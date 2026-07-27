# Entity, Boss & Spell System

Package: [`com.roguesmp.entity`](../src/main/java/com/roguesmp/entity)

Theo cùng mô hình định nghĩa/runtime với [Item System](Item-System.md): `BaseEntity` là định nghĩa JSON tĩnh, `SmpEntity` là wrapper runtime quanh 1 `LivingEntity` sống. Boss extend `SmpEntity` trực tiếp và tự điều khiển logic phase/spell riêng; mob thường thì hoàn toàn do JSON điều khiển thông qua 1 bảng tra `Spell` tổng quát.

> **Lưu ý:** không giống `BaseItem`/`BaseEffect`, `BaseEntity` **không** được decode qua [Codec System](Codec-System.md) — nó là 1 POJO Gson thuần túy (`Utils.GSON.fromJson(reader, BaseEntity.class)`), được load bởi `EntityRegistry.loadFromFile()`. Đừng mặc định rằng mọi class được định nghĩa bằng JSON trong dự án đều dùng `Codec` — hãy kiểm tra trước khi copy 1 pattern.

## `BaseEntity` — định nghĩa tĩnh

[`entity/BaseEntity.java`](../src/main/java/com/roguesmp/entity/BaseEntity.java) — field: `id`, `entityType`, `displayName`, `noAi`, `invulnerable`, `persistent`, `isBoss`, `isElite`, `detectionRange`, `baseStat` (`Map<EntityAttribute, Double>`), `equipments` (`Map<EquipmentSlot, EntityEquipment>`), `activeSpell`/`passiveSpell` (`List<String>` chứa id spell), `passiveInterval`, `canCastSameSpellTwice`, `spellParams` (`Map<String, Map<String, Object>>`, tham số riêng cho từng spell id). Được dựng bằng constructor all-args private cộng với setter kiểu fluent.

Method chính:
- `spawn(Location)` — spawn entity Bukkit, gọi `EntityRegistry.getInstance().wrap(this, living)` để lấy 1 `SmpEntity`, rồi `initialize()` và đăng ký với `EntityManager`.
- `processSpell(SmpEntity)` — biến các list id `activeSpell`/`passiveSpell` cộng `spellParams` thành instance `Spell` qua `EntitySpellRegistry.createSpell(...)`, rồi gọi `smpEntity.startSpell(...)`.
- `processEntity(Entity)` — áp dụng cờ AI/bất tử/persistent, attribute từ `baseStat`, và `equipments` lên entity Bukkit đã spawn. Việc này vẫn chạy ngay cả với các boss subclass hardcode (xem bên dưới), vì nó cung cấp base stat/trang bị cho chúng.

## `SmpEntity` — wrapper runtime, lịch chạy an toàn với Folia

[`entity/SmpEntity.java`](../src/main/java/com/roguesmp/entity/SmpEntity.java) chạy các tick spell định kỳ thông qua **entity scheduler** của Paper (`entity.getScheduler().runAtFixedRate(...)`), giúp việc tick được gắn đúng vào region đang sở hữu entity đó — bắt buộc để tương thích Folia.

```java
// startSpell(...)
if (passiveSpells != null && !passiveSpells.isEmpty()) {
    taskPassive = entity.getScheduler().runAtFixedRate(
            plugin,
            task -> runPassiveSpellTask(passiveIntervalTicks),
            this::unload,   // callback retired — tự cancel gọn gàng nếu region của entity unload/entity chết
            1L,
            passiveIntervalTicks
    );
}

if (activeSpells != null && !activeSpells.isEmpty()) {
    taskActive = entity.getScheduler().runAtFixedRate(
            plugin,
            task -> runActiveSpellTask(ACTIVE_RUN_INTERVAL_DEFAULT),
            this::unload,
            spellDelay,
            ACTIVE_RUN_INTERVAL_DEFAULT
    );
}
```

`runActiveSpellTask` đếm ngược `nextActiveTimer`; về 0 thì gọi `activeSpells.runNextSpell(...)` (ủy quyền cho `SpellManager`, xem bên dưới), giá trị trả về là `cooldownTicks()` của spell vừa cast, dùng làm timer mới. `runPassiveSpellTask` tick `run(interval)` cho mọi spell passive mỗi chu kỳ, refresh boss bar, và bỏ qua hoàn toàn nếu không có player nào trong `detectionRange`.

`changePhase(SpellManager newActive, List<Spell> newPassive, @Nullable Consumer<LivingEntity> phaseAction)` — cancel `SpellManager` hiện tại (tôn trọng `persistOnPhaseChange()` của từng spell), chạy `phaseAction` tùy chọn, rồi thay vào list spell mới (khởi động lại task đã lên lịch nếu cần). Đây là primitive cốt lõi mà boss dùng để chuyển pha.

## `EntityRegistry` — đăng ký entity đặc biệt/boss

[`registry/entity/EntityRegistry.java`](../src/main/java/com/roguesmp/registry/entity/EntityRegistry.java):

```java
private final Map<String, BiFunction<BaseEntity, LivingEntity, SmpEntity>> factories = new HashMap<>();

private EntityRegistry(RogueSmpCore plugin) {
    registerSpecial("primordial_slime", PrimordialSlime::new);
    registerSpecial("hell_knight", HellKnight::new);
    registerSpecial(HellKnightCompanion.ID, HellKnightCompanion::new);
    // ...
}

public SmpEntity wrap(BaseEntity base, LivingEntity living) {
    return factories.getOrDefault(base.getId(), SmpEntity::new).apply(base, living); // fallback: SmpEntity thường
}

private void registerSpecial(String id, BiFunction<BaseEntity, LivingEntity, SmpEntity> factory) {
    factories.put(id, factory);
}
```

Bất kỳ id entity nào **chưa** đăng ký qua `registerSpecial` sẽ wrap thành 1 `SmpEntity` thường, hoàn toàn do list `activeSpell`/`passiveSpell` trong JSON điều khiển (tra qua `EntitySpellRegistry`). Id đã đăng ký sẽ được dựng bằng subclass riêng của nó — đây là cách boss có thể dùng logic phase/spell tự viết tay mà vẫn dùng chung pipeline base stat/trang bị từ JSON.

## Ví dụ boss — `HellKnight`

[`entity/boss/hellknight/HellKnight.java`](../src/main/java/com/roguesmp/entity/boss/hellknight/HellKnight.java) extend `SmpEntity`. Constructor của nó dựng `phase1Actives/Passives` cho tới `phase4Active/Passive` như các field `List<Spell>` thuần (instance `new XSpell(...)`, hardcode — không do JSON điều khiển). Nó override `initialize()` để chạy 1 đoạn cinematic mở màn thay vì bắt đầu spell ngay, kết thúc bằng `startCombat()` dựng 1 `BossBarManager` khóa theo ngưỡng % máu.

Chuyển pha (`setupPhaseTrigger()`):

```java
phaseEvents.put(80, boss -> {
    this.changePhase(SpellManager.EMPTY, Collections.emptyList(), null); // đóng băng spell trong lúc cutscene
    setAi(false);
    Utils.runLater(() -> {
        setAi(true);
        this.changePhase(new SpellManager(phase2Active), phase2Passive,
                living -> dialogue("<red><b>Để xem các ngươi xử lí thế nào..."));
        this.forceCastSpell(ShadowCloneSpell.class);
    }, 20);
});

phaseEvents.put(70, boss -> this.changePhase(new SpellManager(phase3Active), phase3Passive, null));
```

Pha ở ngưỡng 30% còn triệu hồi và cưỡi 1 companion trước khi vào pha cuối. Ngưỡng boss bar đến từ `BossBarManager` và được `SmpEntity.onHurt` tham chiếu để giới hạn sát thương đúng bằng ngưỡng trước khi kích hoạt phase action — nên boss không bao giờ bị "vượt qua" 1 phase trigger chỉ vì ăn 1 đòn quá to.

`entity/boss/primordialslime/PrimordialSlime.java` theo đúng pattern này (list spell theo pha, map `phaseEvents` ở 70%/40%, gọi `changePhase(...)` kiểu cinematic) — khác biệt duy nhất: nó trì hoãn `startSpell` đến khi đoạn mở màn riêng của nó kết thúc, và hủy toàn bộ sát thương trong lúc mở màn.

## Spell

### `Spell` — lớp cơ sở abstract

[`entity/spell/Spell.java`](../src/main/java/com/roguesmp/entity/spell/Spell.java) — `abstract class Spell implements Cloneable` (không phải interface trần):

```java
public boolean canRun();                 // mặc định true — gate trước khi SpellManager chọn nó
public abstract void run(int interval);  // hiệu ứng thật sự
public void cancel();                    // cancel các BukkitRunnable đang theo dõi
public abstract int cooldownTicks();     // độ trễ trước lần chọn active-spell tiếp theo
public int castTicks();                  // mặc định 0
public boolean onlyForceCasted();        // mặc định false — bị loại khỏi vòng xoay active-spell ngẫu nhiên
public boolean persistOnPhaseChange();   // mặc định false — sống sót qua changePhase() nếu true
public void onDamage(DamageEvent event);
public void onHurt(DamageEvent event);
public void onDeath(EntityDeathEvent event);
public void onProjectileLaunch(ProjectileLaunchEvent event);
public void onProjectileHit(ProjectileHitEvent event);
public void onCastSpell(SpellCastEvent event);
public void onTargetEntity(EntityTargetLivingEntityEvent event);
public void onNearbyPlayerDeath(PlayerDeathEvent event); // gate bởi hasNearbyPlayerDeathTrigger()
```

`SpellParamReader { Spell fromParams(Map<String,Object> params, LivingEntity owner) }` là cách các spell **do JSON điều khiển** (`entity/spell/impl/*`) đăng ký 1 factory vào [`EntitySpellRegistry`](../src/main/java/com/roguesmp/registry/entity/EntitySpellRegistry.java), vd. `register("self_destruct_spell", SelfDestructSpell::readParam)`. Spell hardcode riêng cho boss (trong các package `entity/boss/*`) bỏ qua registry này hoàn toàn — chúng được `new` trực tiếp ngay trong constructor của boss.

### `SpellManager` — vòng xoay active-spell

[`entity/spell/SpellManager.java`](../src/main/java/com/roguesmp/entity/spell/SpellManager.java) bọc 1 `List<Spell>`, tính độ sâu cooldown là `floor((số-spell-1)/2)` để 1 spell không bị chọn lại ngay lập tức, và expose `runNextSpell(boolean preventSameSpellTwiceInARow)` cùng `forceCastSpell(Class<? extends Spell>)`.

### Ví dụ spell — `TeleportBehindSpell`

[`entity/boss/hellknight/TeleportBehindSpell.java`](../src/main/java/com/roguesmp/entity/boss/hellknight/TeleportBehindSpell.java): extend `Spell`, override `cooldownTicks()` (trả về 300) và `run(int interval)`. Vì spell passive tick mỗi chu kỳ thay vì được `SpellManager` lên lịch theo cooldown, nó tự theo dõi 1 biến đếm `currentCooldown` nội bộ. Bọc hiệu ứng trong 1 `BukkitRunnable`, thêm vào tập `activeRunnables` kế thừa từ `Spell` để việc đổi pha / `cancel()` dọn dẹp đúng cách.

## Cách thêm 1 boss mới với phase và spell

1. Viết 1 định nghĩa entity JSON (`entities/<id>.json`) khớp với các field Gson của `BaseEntity` — vẫn cần thiết ngay cả với boss hardcode, vì `base.processEntity()` cung cấp attribute/trang bị cơ bản lúc `initialize()`.
2. Tạo `MyBoss extends SmpEntity` với constructor `(BaseEntity base, LivingEntity entity)`. Dựng `phaseNActive`/`phaseNPassive` như các `List<Spell>` chứa instance `new XSpell(...)`.
3. Dựng 1 `Map<Integer, BossBarManager.BossHealthAction> phaseEvents` khóa theo % máu, mỗi entry gọi `this.changePhase(new SpellManager(phaseNActive), phaseNPassive, optionalConsumer)`.
4. Override `initialize()` nếu cần 1 cinematic spawn tùy chỉnh; dựng 1 `BossBarManager` rồi gọi `startSpell(...)` để khởi động các vòng lặp đã lên lịch Folia-safe.
5. Viết class spell extend `Spell` trực tiếp trong package của boss — không cần entry `EntitySpellRegistry` cho spell riêng của boss.
6. Đăng ký boss trong constructor của `EntityRegistry`: `registerSpecial("my_boss_id", MyBoss::new);` — id phải khớp chính xác với `id` trong file JSON. Bỏ qua bước này thì boss chỉ wrap thành 1 `SmpEntity` thường, không có logic phase nào.

## Lưu ý & lỗi thường gặp

- **`BaseEntity` dùng Gson thô, không dùng `Codec`** — đừng dùng `Codec.composite`/`dispatch` ở đây; pattern đó thuộc về `BaseItem`/`SmpEffect`.
- **Id truyền vào `registerSpecial` phải khớp chính xác với field `id` trong JSON**, nếu không entity sẽ âm thầm rơi về `SmpEntity` thường, không có logic phase nào — không hề có lỗi nào được báo.
- **`persistOnPhaseChange()` mặc định là `false`** — 1 spell mà bạn muốn sống sót qua `changePhase()` (vd. 1 DoT kéo dài) cần được override rõ ràng thành `true`, nếu không nó sẽ bị hủy cùng với mọi thứ khác trong pha cũ.
- **Dùng `this::unload` làm callback retired**, đừng tự cancel bằng tay — callback retired của `runAtFixedRate` chính là thứ giữ cho việc dọn dẹp lúc region unload/entity chết trên Folia hoạt động đúng; bỏ qua nó có nguy cơ để lại 1 task đã lên lịch bị treo.

---
◀ [GUI Framework](GUI-Framework.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Player Ability System](Player-Ability-System.md)
