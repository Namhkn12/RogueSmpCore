# Fx System

Package: [`com.roguesmp.fx`](../src/main/java/com/roguesmp/fx)

Hệ thống dựng hiệu ứng hình ảnh (particle và/hoặc block/item display entity) từ các **shape** tĩnh, gộp lại thành **part**, rồi di chuyển/biến đổi cả nhóm hoặc từng part riêng lẻ bằng **motion**. Bốn khái niệm tách rời rõ ràng:

- `FxShape` — hình học tĩnh: tập điểm cục bộ.
- `FxTransform` — vị trí + xoay + scale, bất biến (immutable).
- `FxMotion` — cách 1 transform thay đổi qua từng tick.
- `FxRenderer` — cách 1 tập điểm được vẽ ra (particle hay display entity thật).

`FxPart` ghép 1 shape + 1 renderer + transform/motion cục bộ lại thành 1 phần tử hình ảnh. `FxEffect` gom nhiều `FxPart` dưới 1 root transform/motion chung. `FxEngine` tick mọi `FxEffect` đang chạy.

**Fx thuần hiển thị — không bao giờ tự chạy gameplay logic (damage, v.v...) bên trong package này.** Xem mục [Pattern read-only accessor](#pattern-đọc-vị-trí-hiện-tại--read-only-accessor) bên dưới để biết cách code gọi effect tự làm hit-detection của riêng nó.

## `FxShape` — hình học tĩnh

[`fx/shape/FxShape.java`](../src/main/java/com/roguesmp/fx/shape/FxShape.java):

```java
@FunctionalInterface
public interface FxShape {
    List<Vector> points(int tick);
    default FxShape combine(FxShape other) { ... } // gộp 2 tập điểm làm 1
}
```

Tham số `tick` tồn tại để hỗ trợ shape đổi hình dạng theo thời gian (vd. mật độ điểm tăng dần), nhưng phần lớn implement có sẵn **bỏ qua nó** — shape chỉ nên là hình học, việc di chuyển/biến đổi qua thời gian nên để `FxMotion` lo. Có sẵn: `PointShape`, `LineShape`, `CircleShape`, `SphereShape` (Fibonacci lattice), `HelixShape`.

## `FxTransform` — vị trí + xoay + scale bất biến

[`fx/FxTransform.java`](../src/main/java/com/roguesmp/fx/FxTransform.java) — bọc `Vector3f position`, `Quaternionf rotation`, `Vector3f scale` (JOML, giống `Transformation` của `Display`).

```java
Vector3f apply(Vector local);          // biến 1 điểm cục bộ (scale → rotate → translate) sang không gian của transform này
FxTransform combine(FxTransform child); // ghép transform con (tương đối) vào transform cha → ra transform thế giới
static FxTransform lerp(a, b, t);
```

`combine` là cách 1 part "kế thừa" chuyển động của root: mỗi tick, `FxEffect` tính `rootTransform`, còn `FxPart` tính `root.combine(localTransform)` để ra vị trí/xoay/scale cuối cùng trong thế giới.

## `FxMotion` — biến transform hiện tại thành transform kế tiếp

[`fx/motion/FxMotion.java`](../src/main/java/com/roguesmp/fx/motion/FxMotion.java):

```java
@FunctionalInterface
public interface FxMotion {
    FxTransform step(FxTransform current, int tick);
    default FxMotion andThen(FxMotion next) { ... } // nối 2 motion, chạy cùng tick
}
```

**Cố tình không có khái niệm "path" khai báo trước (không from/to, không control point cố định).** Mỗi `step` chỉ biết "đang ở đâu", nên có thể phản ứng với state sống (mục tiêu đang di chuyển, input ngoài, v.v.) thay vì cam kết trước 1 quỹ đạo/điểm đến cố định. Có sẵn trong `fx/motion/`:

| Motion | Hành vi |
| :--- | :--- |
| `VelocityMotion` | Cộng dồn 1 vận tốc/tick; `setVelocity(...)` để lái sống. |
| `SpinMotion` | Xoay liên tục quanh 1 trục với tốc độ góc cố định. |
| `OrbitMotion` | Bay vòng quanh tâm lấy từ `Supplier<Vector3f>` (tâm có thể tự di chuyển). |
| `SeekMotion` | Đuổi theo mục tiêu sống (`Supplier<Vector3f>`) với tốc độ cố định. |
| `GravityMotion` | Vận tốc nội bộ bị kéo xuống mỗi tick — quỹ đạo mảnh vỡ rơi tự nhiên ra từ việc step từng tick, không phải tính sẵn 1 đường cong tới điểm rơi. |

Motion cũng có thể là lambda tại chỗ (xem `shockwave()` trong `ExplosionEffectDemo` — tăng dần `scale` mỗi tick tới 1 giới hạn rồi giữ nguyên).

## `FxRenderer` — cách 1 tập điểm được vẽ

[`fx/render/FxRenderer.java`](../src/main/java/com/roguesmp/fx/render/FxRenderer.java):

```java
void render(World world, FxTransform transform, List<Vector> localPoints, int tick);
void remove(); // dọn dẹp entity/state do renderer này sở hữu
```

- **`ParticleRenderer`** — không có state, spawn `ParticleBuilder` tại từng điểm mỗi tick.
- **`BlockDisplayRenderer`** / **`ItemDisplayRenderer`** — extend `AbstractDisplayRenderer`, giữ **1 entity `Display` thật cho mỗi điểm**, teleport/reorient nó mỗi tick thay vì respawn. Số entity tự tăng/giảm theo số điểm shape trả về mỗi tick (`ensureCapacity`); `setPersistent(false)` + `setInterpolationDuration(...)` để entity không lưu vào chunk và di chuyển mượt giữa 2 lần cập nhật.

Cả 2 loại renderer đều nhận tham số `Collection<Player> viewers` (`null` = mọi người xung quanh đều thấy, mặc định) — xem [scope người xem](#giới-hạn-người-xem--fxeffectviewers) bên dưới. `ParticleRenderer` gọi `builder.receivers(viewers)`/`builder.allPlayers()` mỗi tick; `AbstractDisplayRenderer` chỉ set `setVisibleByDefault(false)` + `viewer.showEntity(...)` **1 lần lúc spawn** entity (không lặp lại mỗi tick), nên giả định `viewers` không đổi trong suốt vòng đời effect.

## `FxPart` — 1 phần tử hình ảnh

[`fx/FxPart.java`](../src/main/java/com/roguesmp/fx/FxPart.java) — ghép shape + renderer, cộng thêm transform/motion cục bộ (tương đối với root):

```java
new FxPart(shape, renderer)
        .transform(FxTransform.ofPosition(0, 1, 0)) // offset cố định so với root
        .motion(someMotion)                          // di chuyển độc lập, chồng lên root
        .activeTicks(tick -> tick <= 15)              // chỉ render ở những tick khớp predicate
        .once();                                      // tiện ích: activeTicks(tick -> tick == 0)
```

`activeTicks`/`once()` chỉ chặn bước **render** — motion vẫn step mỗi tick, nên `currentTransform()` (xem bên dưới) không bao giờ đứng hình dù part đang tạm ẩn. Dùng để tránh spam particle: 1 flash/impact nên `once()` thay vì vẽ lại suốt vòng đời effect; 1 ring toả ra nên tắt render sau khi hết giai đoạn phóng to thay vì tiếp tục vẽ 1 hình đã đứng yên.

## `FxEffect` + `FxEngine` + `FxHandle`

[`fx/FxEffect.java`](../src/main/java/com/roguesmp/fx/FxEffect.java) — gom nhiều `FxPart` dưới 1 `Location` gốc:

```java
FxEffect effect = FxEffect.builder(origin)
        .part(partA)
        .part(partB)
        .rootMotion(someMotion)   // toàn nhóm cùng di chuyển/xoay, tương đối với vị trí hiện tại
        .duration(45)             // tick sau đó tự dừng + dọn dẹp; -1 (mặc định) = chạy tới khi bị stop() từ ngoài
        .onComplete(() -> {...})
        .viewer(caster)           // hoặc .viewers(collection) — không gọi thì mặc định ai cũng thấy
        .build();

FxHandle handle = FxEngine.getInstance().play(effect);
handle.stop();        // dừng sớm, dọn dẹp entity của mọi part
handle.isActive();    // còn đang chạy không
```

[`fx/FxEngine.java`](../src/main/java/com/roguesmp/fx/FxEngine.java) — singleton theo đúng convention Registry/Manager của plugin (`init(plugin)` gọi 1 lần từ `RogueSmpCore.init()`, `getInstance()`), giữ 1 `BukkitTask` duy nhất tick mọi effect đang active (không phải 1 task/effect). `shutdown()` được gọi từ `RogueSmpCore.onDisable()`.

### Giới hạn người xem — `FxEffect.viewers`

Mặc định mọi part trong 1 effect hiện với **tất cả** người chơi ở gần (behavior gốc, không đổi nếu không gọi gì thêm). Gọi `.viewer(player)`/`.viewers(collection)` trên builder để giới hạn cả effect (mọi part) chỉ hiện cho đúng những người đó — dùng cho hiệu ứng riêng tư kiểu "chỉ người cast thấy" (preview ability, chế độ ngắm, ...). Cùng ý tưởng với cơ chế `viewer` sẵn có ở [`FloatingItemAnimation`](../src/main/java/com/roguesmp/dungeon/itemdisplay/FloatingItemAnimation.java) (`setVisibleByDefault(false)` + `Player#showEntity`), chỉ khác là áp dụng cho cả nhóm thay vì 1 entity đơn.

`viewers` được truyền xuống `FxRenderer.render(..., Collection<Player> viewers)` mỗi tick, nhưng 2 renderer built-in xử lý khác nhau:
- `ParticleRenderer` gọi lại `builder.receivers(viewers)` (hoặc `.allPlayers()` nếu `viewers == null`) **mỗi lần spawn particle**, vì `Particle`/`ParticleBuilder` không có khái niệm "entity sống lâu" để set 1 lần.
- `AbstractDisplayRenderer` chỉ set `setVisibleByDefault(false)` + gọi `showEntity` cho từng viewer **đúng 1 lần lúc entity được spawn** (trong `ensureCapacity`) — không lặp lại ở các tick sau. Hệ quả: đổi `viewers` giữa chừng vòng đời effect **không** ảnh hưởng các display entity đã spawn từ trước, chỉ áp dụng cho entity mới (vd. khi shape tăng số điểm). Coi `viewers` là cấu hình cố định lúc build, không phải state có thể đổi runtime.

Renderer tự viết thêm nên tôn trọng cùng quy ước: `viewers == null` → hiện cho mọi người; khác `null` → chỉ hiện cho đúng tập đó.

## Pattern đọc vị trí hiện tại — read-only accessor

Ban đầu có cân nhắc để `FxPart` tự làm hit-test (kiểu `onTouch(radius, callback)` gọi thẳng `DamageUtils`), nhưng bị bỏ: nó buộc `fx` — vốn chỉ nên thuần hiển thị, giống `ParticleShape`/`DungeonParticle` ở chỗ khác trong plugin — phải phụ thuộc vào hệ damage, và bắt mọi effect (kể cả effect không cần hit-test) gánh thêm 1 cơ chế + state (`Set<UUID>` hit-once) mà nhiều caller đã tự có sẵn cho gameplay logic của riêng họ (cooldown, ability state, ...).

Thay vào đó, `FxPart`/`FxEffect`/`FxHandle` chỉ phơi ra **1 accessor đọc-only** cho vị trí/xoay/scale hiện tại trong thế giới, được cập nhật mỗi tick (và **prime sẵn ngay khi build**, trước cả tick đầu tiên, nên không bao giờ trả về `FxTransform.IDENTITY` giả):

```java
part.currentTransform();          // FxPart      — world transform của riêng part này
effect.currentRootTransform();    // FxEffect    — world transform của root cả nhóm
handle.currentTransform();        // FxHandle    — pass-through tới currentRootTransform() của effect nó bọc
```

Code gọi effect (1 `Ability`, 1 boss spell, 1 command demo, ...) **giữ lại tham chiếu tới `FxPart` mình quan tâm** (hoặc chỉ cần `FxHandle` nếu chỉ cần vị trí root), rồi tự chạy vòng lặp tick của riêng nó (thường là 1 `BukkitRunnable` song song với `FxEngine`) để đọc `currentTransform()`, tự làm phép khoảng cách, tự giữ state hit-once/cooldown, và tự gọi `DamageUtils`/API gameplay khác — hoàn toàn ở tầng gọi, không đụng vào `fx`.

### Ví dụ thật: [`FxCommand.watchShockwaveForHits`](../src/main/java/com/roguesmp/fx/FxCommand.java)

`ExplosionEffectDemo.create(...)` trả về `record Explosion(FxEffect effect, FxPart shockwave)` thay vì chỉ `FxEffect` trần — để caller giữ được tham chiếu tới đúng part (vòng shockwave) cần theo dõi. `FxCommand` sau đó tự chạy 1 `BukkitRunnable` riêng, mỗi tick đọc `shockwave.currentTransform().scale()` để suy ra bán kính vòng shockwave **đang** to tới đâu (không phải tính 1 lần lúc cast), rồi tự damage người chơi đúng lúc vòng sóng thật sự quét qua họ:

```java
FxTransform ring = shockwave.currentTransform();
Location center = ring.toLocation(world);
double radius = ExplosionEffectDemo.SHOCKWAVE_BASE_RADIUS * ring.scale().x();

for (Player nearby : center.getNearbyPlayers(radius + BLAST_HIT_BAND)) {
    if (alreadyHit.contains(nearby.getUniqueId())) continue;
    if (Math.abs(nearby.getLocation().distance(center) - radius) > BLAST_HIT_BAND) continue;
    alreadyHit.add(nearby.getUniqueId());
    DamageUtils.damage(nearby, null, BLAST_DAMAGE, new DamageEvent.Metadata(DamageType.BLAST));
}
```

Runnable này tự `cancel()` khi `handle.isActive()` trả `false` — vòng đời hit-test đi theo vòng đời effect nhưng vẫn là 1 task hoàn toàn tách biệt, sống ngoài `fx`.

## Ví dụ đầy đủ: `/fx explosion`

[`fx/demo/ExplosionEffectDemo.java`](../src/main/java/com/roguesmp/fx/demo/ExplosionEffectDemo.java) — 1 flash `once()`, 1 vòng shockwave (particle `DUST`, mật độ điểm đủ dày để trông liền mạch tới tận bán kính lớn nhất, tắt render sau khi hết phóng to), 10 tia lửa (`GravityMotion`), 6 mảnh vỡ block display (`GravityMotion.andThen(SpinMotion)`) — tất cả gộp thành 1 `FxEffect`. [`fx/FxCommand.java`](../src/main/java/com/roguesmp/fx/FxCommand.java) đăng ký `/fx explosion`, raytrace điểm nhìn của player (`player.rayTraceBlocks(...)`) làm origin, `play()` effect, rồi gắn hit-detection như mục trên.

## Cách thêm 1 shape/motion/renderer mới

1. **Shape mới**: implement `FxShape`, trả `List<Vector>` (điểm cục bộ, gốc `(0,0,0)`) từ `points(tick)`. Bỏ qua `tick` trừ khi hình dạng thật sự cần đổi theo thời gian (mật độ, v.v.) — việc *di chuyển* nên là `FxMotion`, không phải shape.
2. **Motion mới**: implement `FxMotion`, chỉ nhận `(current, tick)` rồi trả transform kế tiếp — không nhận điểm đến/duration cố định trong constructor nếu tránh được, để giữ đúng tinh thần "không cam kết trước quỹ đạo".
3. **Renderer mới**: implement `FxRenderer` trực tiếp cho particle-only; extend `AbstractDisplayRenderer<T extends Display>` nếu cần entity thật (chỉ cần override `configure(T display)`).

## Lưu ý & lỗi thường gặp

- **`FxTransform.at(Location)` chỉ lấy `yaw`, bỏ qua `pitch`** — root xoay theo hướng nhìn ngang, không nghiêng lên/xuống theo pitch của `Location` gốc.
- **Số điểm shape trả ra nên tương đối ổn định giữa các tick** khi dùng display renderer — `AbstractDisplayRenderer` tự spawn/remove entity để khớp số điểm mỗi tick, số điểm dao động liên tục sẽ làm entity bị tạo/xoá liên tục thay vì di chuyển mượt.
- **Đừng bọc gameplay logic vào `fx`** (xem mục pattern accessor ở trên) — nếu thấy mình sắp import `DamageUtils`/`DamageEvent` vào trong package `com.roguesmp.fx`, đó là dấu hiệu nên chuyển logic đó ra tầng gọi (ability/spell/command) và dùng `currentTransform()` thay vì thêm hook mới vào `FxPart`/`FxEffect`.
- **World/chunk unload đã được xử lý an toàn, không cần tự check ở tầng gọi**: nếu `origin.getWorld()` trả `null` (world đã unload, vd. qua Multiverse), `FxEffect.tick()` tự `stop()` thay vì ném NPE giữa vòng lặp dùng chung của `FxEngine`. Mỗi `ParticleRenderer`/`AbstractDisplayRenderer` tự kiểm tra `world.isChunkLoaded(...)` **1 lần mỗi part mỗi tick** (tại vị trí root của part, không phải từng điểm — đánh đổi để tránh chi phí check theo từng điểm) trước khi spawn/teleport, bỏ qua tick đó thay vì force-load chunk. `AbstractDisplayRenderer` cũng tự lọc bỏ entity đã bị server dọn (`!isValid()`) trước khi đếm capacity, để không gọi `teleport`/`setTransformation` lên 1 entity đã chết.

---
◀ [Dungeon System](Dungeon-System.md) · Về [Trang chủ](Home.md)
