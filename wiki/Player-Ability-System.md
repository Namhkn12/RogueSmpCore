# Player Ability System

Package: [`com.roguesmp.player.ability`](../src/main/java/com/roguesmp/player/ability)

`Ability` là class hành vi cho mỗi lần cast (cùng khuôn mẫu hook mặc định no-op với [`SmpAttribute`](Attribute-System.md)/`Spell`). Mỗi impl đi kèm 1 `AbilityInfo<T>` static (factory + action, thuần code) và 1 `AbilityConfig` JSON-loadable riêng (scaling theo level, trigger, mô tả, chi phí nâng cấp), liên kết với nhau qua id. `AbilityLoadout` là container runtime cho từng player, điều phối phím bấm vào chuỗi ability đang trang bị, bao gồm cả 1 cơ chế interceptor cho các ability cần bắt (capture) vài input tiếp theo của player (chế độ ngắm/tích lực). Ability nào 1 player được phép trang bị còn phụ thuộc vào [Player Class System](Player-Class-System.md) đang active.

## `Ability`

[`player/ability/Ability.java`](../src/main/java/com/roguesmp/player/ability/Ability.java) — abstract, field `smpPlayer`, `level`, `cooldownTick`.

```java
public abstract @NotNull AbilityInfo<? extends Ability> getAbilityInfo(); // method bắt buộc duy nhất

// helper cooldown
public int getCooldownTick(); public boolean isOnCooldown();
public void setCooldownTick(int ticks);
public boolean tickCooldown(int reduction); // true đúng lúc cooldown vừa xong
public void onCooldownRefreshed();          // mặc định: âm thanh + action bar

// hook gameplay no-op, override khi cần — cùng bề mặt event với SmpAttribute
public void tick(int ticks);
public void onDamageEntity(DamageEvent event);
public void onKillEntity(EntityDeathEvent event);
public void onHurt(DamageEvent event);
public void onHurtFatal(DamageEvent event);
public void onConsume(PlayerItemConsumeEvent event);
public void onExpChange(PlayerExpChangeEvent event);
public void onBlockBreak(BlockBreakEvent event);
public void onCombust(EntityCombustEvent event);
public void onCombustEntity(EntityCombustByEntityEvent event);
public void onProjectileHit(ProjectileHitEvent event);
public void onProjectileLaunch(PlayerLaunchProjectileEvent event);
public void onShootArrow(EntityShootBowEvent event);
public void onConsumeArrow(ArrowConsumeEvent event);
public void onStartBlocking(PlayerStartBlockAttackEvent event); // player bắt đầu đỡ đòn bằng khiên
public void onTeleport(PlayerTeleportEvent event);
public void onDeath(PlayerDeathEvent event);
public void onEquip();   // lúc ability được equip vào loadout
public void onUnequip(); // lúc ability bị gỡ khỏi loadout
```

## `AbilityInfo<T>` / `AbilityConfig` — nửa code, nửa JSON

> ⚠️ **`AbilityInfo` đã được tách làm 2 nửa.** Trước đây 1 `AbilityInfo` duy nhất vừa giữ code (factory, action) vừa tự `populate(...)` dữ liệu JSON (scaling, trigger) vào chính nó. Giờ **`AbilityInfo`** ([`player/ability/AbilityInfo.java`](../src/main/java/com/roguesmp/player/ability/AbilityInfo.java)) chỉ còn giữ phần **không thể là JSON** (id, class, factory, action registry), còn mọi dữ liệu tunable nằm ở 1 record JSON-loadable riêng, **`AbilityConfig`** ([`player/ability/AbilityConfig.java`](../src/main/java/com/roguesmp/player/ability/AbilityConfig.java)), tra theo `id` từ `Registries.ABILITY_CONFIG` **mỗi lần cần** — không cache vào field của `AbilityInfo`, nên 1 lần reload registry lập tức có hiệu lực.

```java
// AbilityInfo — khai báo như 1 field INFO static trên từng impl, phần Java thuần
public static final AbilityInfo<Fireball> INFO = new AbilityInfo<>(ID, Fireball.class, Fireball::new)
        .registerAction("execute", Fireball::handleExecute);
```

`AbilityInfo` giữ: `id`, `abilityClass`, `factory` (`BiFunction<SmpPlayer, Integer, T>`, dựng 1 instance mới), `actionRegistry` (`Map<String, AbilityAction<T>>`, populate bằng `.registerAction("key", Class::method)` nối chuỗi trong code). Mọi getter tunable (`getDisplayName()`, `getIcon()`, `getScaling()`, `getDescription()`, `getUpgradeRequirement()`, `findMatchingActionKey(...)`) đều **ủy quyền sang `AbilityConfig`** đọc từ `Registries.ABILITY_CONFIG.getOrDefault(id, AbilityConfig.DEFAULT_CONFIG)` bên trong.

```java
// AbilityConfig — record JSON-loadable, load từ ability_info/<id>.json vào Registries.ABILITY_CONFIG
public record AbilityConfig(
        List<String> description,               // "description": mảng chuỗi MiniMessage, hỗ trợ tag <key>/<key:format> tra theo scaling
        Map<String, List<Double>> scaling,       // "scaling": key tùy ý -> giá trị theo từng level (index 0 = level 1)
        String displayName,                      // "display_name"
        Material icon,                           // "icon" — tên Material, mặc định BARRIER
        Map<String, AbilityTrigger> triggers,     // "trigger": action-key -> {key, options, displayName}
        Map<Integer, List<UpgradeRequirement>> upgrades // "upgrades": level (chuỗi "2","3",...) -> list yêu cầu nâng cấp
) { }
```

Cả 2 nửa liên kết với nhau **chỉ qua chuỗi id** — file `ability_info/<id>.json` không có field `"id"`/`"type"` nào cả, chính **id registry** (đường dẫn file, bỏ `.json`) phải khớp tuyệt đối với `AbilityInfo.ID` trong code. Lệch tên → ability chạy với `AbilityConfig.DEFAULT_CONFIG` (rỗng, icon `BARRIER`) mà không có lỗi nào được báo.

`getAttributeForLevel(attr, level)` clamp index thành `level - 1` (level bắt đầu từ 1); `findMatchingActionKey(Player, AbilityTrigger.Key)` duyệt `config().triggers()` tìm entry đầu tiên khớp; `executeSpecificAction(T, actionKey)` chạy action đã đăng ký trong `AbilityInfo`, fallback `AbilityResponse.continueChain()` nếu key không tồn tại.

## `AbilityTrigger` — phím vật lý + predicate tùy chọn

[`player/ability/trigger/AbilityTrigger.java`](../src/main/java/com/roguesmp/player/ability/trigger/AbilityTrigger.java):

```java
public enum Key { LEFT_CLICK, RIGHT_CLICK, SWAP, SNEAK, JUMP } // đúng 5 giá trị, không có thêm

public static final Codec<AbilityTrigger> CODEC = Codec.composite(
        Codec.enumOf(Key.class).fieldOf("key").forGetter(AbilityTrigger::getKey),
        Codec.listOf(Codec.STRING).optionalFieldOf("options", new ArrayList<>()).forGetter(AbilityTrigger::getOptions),
        Codec.STRING.optionalFieldOf("displayName", "").forGetter(AbilityTrigger::getDisplayName),
        AbilityTrigger::new
);

public boolean matches(Player player, Key pressedKey) {
    if (this.key != pressedKey) return false;
    for (String optionKey : options) {
        TriggerOption option = Registries.TRIGGER_OPTION.get(optionKey);
        if (option != null && !option.test(player)) return false;
    }
    return true;
}
```

1 trigger đọc là "phím vật lý X, **và** mọi predicate có tên đều đúng." `displayName` (optional) chỉ dùng để hiển thị — hữu ích khi 1 ability có nhiều trigger và cần phân biệt chúng trong GUI (vd. `"Kích hoạt (Ngắm): Chuột phải"`).

### `TriggerOption` — bảng predicate, [`Registries.TRIGGER_OPTION`](Registry-System.md)

Đăng ký code-driven qua [`TriggerOptions.java`](../src/main/java/com/roguesmp/player/ability/trigger/TriggerOptions.java). **Hiện chỉ có đúng 4 option đã đăng ký** — đừng giả định có nhiều hơn khi viết trigger JSON:

| id | Predicate |
| :--- | :--- |
| `sneaking` | `Player::isSneaking` |
| `not_sneaking` | `p -> !p.isSneaking()` |
| `sprinting` | `Player::isSprinting` |
| `not_sprinting` | `p -> !p.isSprinting()` |

Thêm 1 option mới: khai `public static final TriggerOption YOUR_OPTION = register("your_id", "Tên hiển thị", predicate);` trong `TriggerOptions.java`.

## `AbilityLoadout` — container runtime cho từng player

[`player/ability/AbilityLoadout.java`](../src/main/java/com/roguesmp/player/ability/AbilityLoadout.java):

```java
public static final int SLOT_COUNT = 4;
private final Ability[] slots = new Ability[SLOT_COUNT]; // 1 mảng duy nhất, không còn phân loại Active/Passive/Lifeline
private Ability contextOwner = null;   // interceptor: bắt input tiếp theo
private int contextTicksLeft = 0;
```

`equip()` ghi vào cả mảng runtime lẫn persistence `PlayerData`. `loadData(PlayerData)` dựng lại instance qua `Registries.ABILITY.get(id).createInstance(smpPlayer, level)` (⚠️ không còn qua 1 singleton `AbilityRegistry.getInstance()` — `Registries.ABILITY` là `Registry<AbilityInfo<? extends Ability>>`, code-driven, populate qua [`AbilityInfos.loadClass()`](../src/main/java/com/roguesmp/player/ability/AbilityInfos.java)). `tick()` giảm dần `contextTicksLeft` và tự động xóa `contextOwner` khi về 0 (nên 1 chế độ capture sẽ tự hết hạn nếu không bao giờ được release rõ ràng).

### Điều phối `cast(key)` → `execute(...)`

```java
public void cast(AbilityTrigger.Key key) {
    if (contextOwner != null) {
        if (execute(contextOwner, key)) return;   // interceptor được ưu tiên trước
    }
    for (Ability ability : slots) {   // ability không có trigger nào thì findMatchingActionKey trả null → bỏ qua
        if (ability == null || ability == contextOwner) continue;
        if (execute(ability, key)) return;         // chuỗi: dừng ở ability đầu tiên "xử lý" được
    }
}

private boolean execute(Ability ability, AbilityTrigger.Key key) {
    String actionKey = ability.getAbilityInfo().findMatchingActionKey(smpPlayer.getBukkitPlayer(), key);
    if (actionKey == null) return false;

    AbilityCastEvent event = new AbilityCastEvent(smpPlayer, ability);
    Bukkit.getPluginManager().callEvent(event);
    if (event.isCancelled()) return true; // bị cancel cũng tính là "đã xử lý" — chuỗi dừng ở đây

    AbilityResponse response = ability.getAbilityInfo().executeSpecificAction(ability, actionKey);
    return processSignal(ability, response);
}
```

`AbilityCastEvent` là 1 `Event` cancellable thuần của Bukkit, được bắn trước mỗi lần thực thi action — plugin/hệ thống khác có thể veto 1 lần cast mà bản thân ability không hề biết. `InputSignal` của `AbilityResponse` sau đó quyết định `cast()` làm gì tiếp theo:

| Signal | Hiệu ứng |
| :--- | :--- |
| `CONTINUE` | Ability không xử lý — `execute()` trả `false`, `cast()` thử ability tiếp theo. |
| `CONSUME` | Đã xử lý, dừng — `execute()` trả `true`. |
| `CAPTURE` | Đã xử lý, **và** ability này trở thành `contextOwner` trong `resp.timeoutTicks()` tick tiếp theo — các lần cast sau sẽ route vào đây trước (chế độ ngắm/tích lực). |
| `RELEASE` | Xóa `contextOwner` — kết thúc 1 lần capture. |
| `DENY` | *(xem lưu ý bên dưới — hiện không hoạt động như 1 điểm dừng cứng)* |

`AbilityLoadoutMechanic` ([`player/mechanic/AbilityLoadoutMechanic.java`](../src/main/java/com/roguesmp/player/mechanic/AbilityLoadoutMechanic.java)) là nơi thực sự gọi `cast(...)` từ input vật lý: click trái/phải qua `onInteract`, `SWAP` qua `onSwapHand`, `JUMP`/`SNEAK` qua `onInput`.

## Ví dụ 1 — đơn giản: `Fireball`

[`player/ability/impl/active/Fireball.java`](../src/main/java/com/roguesmp/player/ability/impl/active/Fireball.java) — 1 action đăng ký duy nhất `"execute"`, có cooldown gate, spawn 1 item projectile vật lý với vòng lặp tick `BukkitRunnable`, nổ khi va chạm/hết thời gian:

```java
public static final AbilityInfo<Fireball> INFO = new AbilityInfo<>(ID, Fireball.class, Fireball::new)
        .registerAction("execute", Fireball::handleExecute);

public AbilityResponse handleExecute() {
    if (isOnCooldown()) return AbilityResponse.continueChain();
    // ... spawn + bắn projectile, đặt cooldown
    return AbilityResponse.consume();
}
```

`GravityBomb` theo đúng khuôn mẫu 1 action này.

## Ví dụ 2 — phức tạp, capture/chế độ ngắm: `AetherStance`

[`player/ability/impl/active/AetherStance.java`](../src/main/java/com/roguesmp/player/ability/impl/active/AetherStance.java) — 3 action đăng ký tạo thành 1 state machine nhỏ:

```java
.registerAction("activate", AetherStance::handleActivate)
.registerAction("fire", AetherStance::handleFire)
.registerAction("dash", AetherStance::handleDash);

public AbilityResponse handleActivate() {
    if (isOnCooldown() || isActive) return AbilityResponse.deny();
    startStance();
    return AbilityResponse.capture(durationTicks);   // trở thành contextOwner — input sau đó route vào đây
}

public AbilityResponse handleFire() {
    if (!isActive) return AbilityResponse.continueChain();
    executeBeam(...);
    if (--shotsRemaining <= 0) return endStance();      // trả về AbilityResponse.release()
    return AbilityResponse.capture(ticksLeft);          // gia hạn capture
}

public AbilityResponse handleDash() {
    if (!isActive) return AbilityResponse.continueChain();
    executeDash(...);
    return AbilityResponse.capture(ticksLeft);
}
```

Trong lúc đang capture, mọi lệnh `cast()` đều route vào `AetherStance` trước (bất kể phím nào được bấm, miễn là 1 trong các trigger của nó khớp), cho đến khi nó tự release hoặc `contextTicksLeft` hết hạn qua `AbilityLoadout.tick()`.

## `ability_info/<id>.json` — ví dụ đầy đủ, đã xác minh với code thật

Không còn 1 `AbilityRegistry` singleton nào cả — `Registries.ABILITY_CONFIG = new Registry<>("ability_info", AbilityConfig.CODEC)` tự load đệ quy mọi file dưới `ability_info/`, id = đường dẫn file bỏ `.json` (xem [Registry System](Registry-System.md)). Ví dụ dưới khớp đúng các key `scaling` mà [`Fireball`](../src/main/java/com/roguesmp/player/ability/impl/mage/Fireball.java) (`ID = "fireball"`) thật sự đọc qua `getAttributeForLevel(...)`:

```json
{
  "display_name": "<red>Fireball",
  "icon": "FIRE_CHARGE",
  "description": [
    "Phóng 1 quả cầu lửa gây <damage> sát thương trong bán kính <radius> ô.",
    "Hồi chiêu: <cooldown:second>s"
  ],
  "scaling": {
    "damage": [10.0, 14.0, 18.0, 22.0, 26.0],
    "radius": [2.5, 2.5, 3.0, 3.0, 3.5],
    "velocity": [1.4, 1.5, 1.6, 1.7, 1.8],
    "cooldown": [100.0, 90.0, 80.0, 70.0, 60.0]
  },
  "trigger": {
    "execute": { "key": "RIGHT_CLICK", "options": ["sneaking"] }
  },
  "upgrades": {
    "2": [ { "type": "exp", "level": 10 } ],
    "3": [ { "type": "item", "item_id": "fire_essence", "amount": 5 }, { "type": "exp", "level": 20 } ]
  }
}
```

Vài điểm quan trọng khi viết file này:
- **Không có field `"id"`/`"type"` nào trong file** — id chính là đường dẫn file (`ability_info/fireball.json` → id `"fireball"`), phải khớp tuyệt đối `Fireball.ID` trong code.
- **`trigger` là 1 map, không phải mảng** — key của map chính là **tên action** đã `registerAction(...)` trong code (vd. `"execute"`), không phải tên tự do.
- 1 ability nhiều action (kiểu `AetherStance`: `activate`/`fire`/`dash`) cần 1 entry `trigger` cho **mỗi** action muốn có phím bấm riêng:
  ```json
  "trigger": {
    "activate": { "key": "RIGHT_CLICK", "options": ["sneaking"] },
    "fire":     { "key": "LEFT_CLICK" },
    "dash":     { "key": "SNEAK" }
  }
  ```
- **`options` chỉ chấp nhận 4 id đã đăng ký** (xem bảng `TriggerOption` ở trên) — 1 id không tồn tại bị bỏ qua âm thầm khi kiểm tra (`Registries.TRIGGER_OPTION.get(optionKey)` trả `null` → coi như predicate đó "đúng").
- Ability thuần passive (vd. `Dodging`, chỉ phản ứng theo hook `on*`) có thể bỏ hẳn `"trigger"` — sẽ hiện là "Kích hoạt: Bị động" trong GUI.

<a id="upgraderequirement--chi-phí-nâng-cấp-level"></a>
## `UpgradeRequirement` — chi phí nâng cấp level

`"upgrades"` map level đích (viết dưới dạng chuỗi số, `"2"`, `"3"`, ...) sang 1 danh sách yêu cầu đa hình (dispatch trên `"type"`, qua [`Registries.ABILITY_UPGRADE_REQUIREMENT_CODEC`](../src/main/java/com/roguesmp/player/ability/upgrade/UpgradeRequirements.java)) — tất cả yêu cầu trong list phải thỏa để nâng ability từ level hiện tại lên level đó. 2 kiểu đã đăng ký:

| `type` | Field | Ý nghĩa |
| :--- | :--- | :--- |
| `item` | `item_id` (tham chiếu [`Holder<BaseItem>`](Registry-System.md#holdert--tham-chiếu-ổn-định-qua-id) vào `Registries.ITEM`), `amount` (int) | Cần nộp N item `item_id` |
| `exp` | `level` (int) | Cần đủ kinh nghiệm tương đương N level vanilla (`PlayerUtils.getExpFromLevel`) |

```json
"upgrades": {
  "2": [ { "type": "exp", "level": 10 } ],
  "3": [ { "type": "item", "item_id": "fire_essence", "amount": 5 }, { "type": "exp", "level": 20 } ]
}
```

## Cách thêm 1 ability mới

1. Tạo 1 class trong `player/ability/impl/<lớp nhân vật>` (vd. `warrior/`, `mage/`, `archer/`, `assassin/`) extend `Ability`, với `public static final String ID = "..."`.
2. Khai `public static final AbilityInfo<YourClass> INFO = new AbilityInfo<>(ID, YourClass.class, YourClass::new)`, nối chuỗi `.registerAction("key", YourClass::method)` cho từng hành vi riêng biệt — 1 `"execute"` duy nhất cho ability đơn giản (`Fireball`), hoặc nhiều action có tên cho ability dạng state/capture (`AetherStance` với `activate`/`fire`/`dash`). Ability passive chỉ phản ứng theo event (vd. `Dodging`) có thể không đăng ký action nào cả.
3. Lấy giá trị scaling trong constructor/method qua `getAbilityInfo().getAttributeForLevel("key", level)` (hoặc helper `getBaseAttributeValue("key")` trên chính `Ability`, tự dùng `getLevel()` hiện tại).
4. Override `getAbilityInfo()` trả về `INFO`, cộng với các hook `on*` liên quan cho hành vi phản ứng.
5. Đăng ký `YourClass.INFO` trong [`AbilityInfos.java`](../src/main/java/com/roguesmp/player/ability/AbilityInfos.java): `public static final AbilityInfo<YourClass> YOUR_ABILITY = register(YourClass.INFO);`.
6. Viết `<dataFolder>/ability_info/<id>.json` (id file **phải khớp** `ID` ở bước 1) với `scaling`, `trigger`, `display_name`, `icon`, `description`, `upgrades` — xem ví dụ đầy đủ ở trên.
7. Nếu ability này thuộc về 1 lớp nhân vật (class), thêm id của nó vào `default_abilities` của file `classes/<class>.json` tương ứng — xem [Player Class System](Player-Class-System.md); nếu không, ability sẽ tồn tại trong registry nhưng không class nào cho phép trang bị nó.

## Lưu ý & lỗi thường gặp

- **Doc comment của `AbilityResponse.deny()` ghi "dừng chuỗi", nhưng switch trong `processSignal` không có case `DENY` rõ ràng** — nó rơi vào `default -> false`, nghĩa là `execute()` trả `false` và `cast()` vẫn tiếp tục thử ability tiếp theo đang trang bị. Nếu cần 1 điểm dừng cứng thật sự, hãy kiểm tra lại hành vi hiện tại trong `AbilityLoadout.processSignal` trước khi chỉ dựa vào `deny()` — đây có thể là 1 bug tiềm ẩn hơn là hành vi cố ý.
- **1 `AbilityCastEvent` bị cancel vẫn tính là "đã xử lý"** (`execute()` trả `true`), nên việc cancel nó sẽ âm thầm dừng toàn bộ chuỗi cast cho lần bấm phím đó thay vì để ability tiếp theo thử — cần lưu ý nếu 1 plugin/listener khác cancel các lần cast.
- **`CAPTURE` phải được gia hạn ở mỗi lần gọi action trong lúc stance còn active**, nếu không `contextTicksLeft` sẽ tự về 0 qua `AbilityLoadout.tick()` và âm thầm release interceptor — xem cách `AetherStance.handleFire`/`handleDash` trả `AbilityResponse.capture(ticksLeft)` lại mỗi lần.

---
◀ [Entity, Boss & Spell System](Entity-Boss-Spell-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Player Class System](Player-Class-System.md)
