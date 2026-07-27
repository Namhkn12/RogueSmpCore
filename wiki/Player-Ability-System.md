# Player Ability System

Package: [`com.roguesmp.player.ability`](../src/main/java/com/roguesmp/player/ability)

`Ability` là class hành vi cho mỗi lần cast (cùng khuôn mẫu hook mặc định no-op với [`SmpAttribute`](Attribute-System.md)/`Spell`). Mỗi impl đi kèm 1 `AbilityInfo<T>` static chứa factory, scaling theo level, các action có tên, và các trigger được load từ JSON. `AbilityLoadout` là container runtime cho từng player, điều phối phím bấm vào chuỗi ability đang trang bị, bao gồm cả 1 cơ chế interceptor cho các ability cần bắt (capture) vài input tiếp theo của player (chế độ ngắm/tích lực).

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
```

## `AbilityInfo<T>`

[`player/ability/AbilityInfo.java`](../src/main/java/com/roguesmp/player/ability/AbilityInfo.java) — khai báo như 1 field `INFO` static trên từng impl:

```java
public static final AbilityInfo<Fireball> INFO = new AbilityInfo<>(ID, Fireball.class, Fireball::new)
        .registerAction("execute", Fireball::handleExecute);
```

Cấu trúc:
- `AbilityAction<T> { AbilityResponse execute(T ability); }` — 1 hành vi có tên.
- `factory`: `BiFunction<SmpPlayer, Integer, T>` — dựng 1 instance mới (`Fireball::new`).
- `actionRegistry`: `Map<String, AbilityAction<T>>` — populate trong code qua các lệnh gọi nối chuỗi `.registerAction("key", Class::method)`.
- `triggerMap`: `Map<AbilityTrigger, String>` (trigger → action key) — populate **từ JSON lúc load** qua `populate(...)`, không hardcode.
- `scaling`: `Map<String, List<Double>>` — giá trị theo từng level; `getAttributeForLevel(attr, level)` clamp index thành `level - 1`.
- `findMatchingActionKey(Player, AbilityTrigger.Key)` — tìm trigger đầu tiên khớp cả phím vật lý lẫn predicate tùy chọn.
- `executeSpecificAction(T, actionKey)` — chạy action đã khớp, fallback về `AbilityResponse.continueChain()` nếu key không tồn tại.

## `AbilityTrigger` — phím vật lý + predicate tùy chọn

[`player/ability/trigger/AbilityTrigger.java`](../src/main/java/com/roguesmp/player/ability/trigger/AbilityTrigger.java):

```java
public enum Key { LEFT_CLICK, RIGHT_CLICK, SWAP, SNEAK, JUMP }

private final Key key;
private final List<String> options = new ArrayList<>();

public boolean matches(Player player, Key pressedKey) {
    if (this.key != pressedKey) return false;
    for (String optionKey : options) {
        Predicate<Player> condition = TriggerOptionRegistry.get(optionKey);
        if (condition != null && !condition.test(player)) return false;
    }
    return true;
}
```

1 trigger đọc là "phím vật lý X, **và** mọi predicate có tên đều đúng." Predicate được tra theo tên key qua [`TriggerOptionRegistry`](../src/main/java/com/roguesmp/registry/ability/TriggerOptionRegistry.java), vd. `"sneaking" → Player::isSneaking`, `"holding_projectile" → ...`, `"not_holding_consumable" → ...`.

## `AbilityLoadout` — container runtime cho từng player

[`player/ability/AbilityLoadout.java`](../src/main/java/com/roguesmp/player/ability/AbilityLoadout.java):

```java
private final Map<AbilityType, Ability[]> abilityMap = new EnumMap<>(AbilityType.class); // mảng có kích thước theo AbilityType.getMaxSlots()
private Ability contextOwner = null;   // interceptor: bắt input tiếp theo
private int contextTicksLeft = 0;
```

`equip()` ghi vào cả mảng runtime lẫn persistence `PlayerData`. `loadData(PlayerData)` dựng lại instance qua `AbilityRegistry.getInstance().createInstance(id, smpPlayer, level)`. `tick()` giảm dần `contextTicksLeft` và tự động xóa `contextOwner` khi về 0 (nên 1 chế độ capture sẽ tự hết hạn nếu không bao giờ được release rõ ràng).

### Điều phối `cast(key)` → `execute(...)`

```java
public void cast(AbilityTrigger.Key key) {
    if (contextOwner != null) {
        if (execute(contextOwner, key)) return;   // interceptor được ưu tiên trước
    }
    for (Ability ability : abilityMap.get(AbilityType.ACTIVE)) {
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

## Load trigger/scaling từ JSON

[`registry/ability/AbilityRegistry.java`](../src/main/java/com/roguesmp/registry/ability/AbilityRegistry.java) — `loadAll()` đọc mọi file `*.json` dưới `<dataFolder>/ability_info/`, khớp với 1 `AbilityInfo` đã đăng ký sẵn theo id, rồi gọi `info.populate(...)`. Parse trigger:

```java
JsonObject obj = json.getAsJsonObject("trigger");
for (String actionKey : obj.keySet()) {
    JsonObject data = obj.getAsJsonObject(actionKey);
    AbilityTrigger.Key key = AbilityTrigger.Key.valueOf(data.get("key").getAsString().toUpperCase());
    List<String> options = new ArrayList<>();
    if (data.has("options")) data.getAsJsonArray("options").forEach(opt -> options.add(opt.getAsString()));
    map.put(new AbilityTrigger(key, options), actionKey);
}
```

Cấu trúc suy ra được (chưa có file JSON mẫu nào được commit):

```json
{
  "id": "aether_stance", "display_name": "...", "icon": "END_ROD", "type": "ACTIVE",
  "scaling": { "damage": [4, 6, 8], "cooldown": [200, 180, 160] },
  "trigger": {
    "activate": { "key": "RIGHT_CLICK", "options": ["sneaking"] },
    "fire":     { "key": "LEFT_CLICK" },
    "dash":     { "key": "SNEAK" }
  }
}
```

## Cách thêm 1 ability mới

1. Tạo 1 class trong `player/ability/impl/{active|passive|lifeline}` extend `Ability`, với `public static final String ID = "..."`.
2. Khai `public static final AbilityInfo<YourClass> INFO = new AbilityInfo<>(ID, YourClass.class, YourClass::new)`, nối chuỗi `.registerAction("key", YourClass::method)` cho từng hành vi riêng biệt — 1 `"execute"` duy nhất cho ability đơn giản (`Fireball`), hoặc nhiều action có tên cho ability dạng state/capture (`AetherStance` với `activate`/`fire`/`dash`). Ability passive chỉ phản ứng theo event (vd. `Dodging`) có thể không đăng ký action nào cả.
3. Lấy giá trị scaling trong constructor qua `getAbilityInfo().getAttributeForLevel("key", level)`.
4. Override `getAbilityInfo()` trả về `INFO`, cộng với các hook `on*` liên quan cho hành vi phản ứng.
5. Đăng ký `YourClass.INFO` trong constructor của `AbilityRegistry`.
6. Viết `<dataFolder>/ability_info/<id>.json` với `scaling`, `trigger` (action key → `AbilityTrigger.Key` + `options` tùy chọn), `display_name`, `icon`, `type`, `description`.

## Lưu ý & lỗi thường gặp

- **Doc comment của `AbilityResponse.deny()` ghi "dừng chuỗi", nhưng switch trong `processSignal` không có case `DENY` rõ ràng** — nó rơi vào `default -> false`, nghĩa là `execute()` trả `false` và `cast()` vẫn tiếp tục thử ability tiếp theo đang trang bị. Nếu cần 1 điểm dừng cứng thật sự, hãy kiểm tra lại hành vi hiện tại trong `AbilityLoadout.processSignal` trước khi chỉ dựa vào `deny()` — đây có thể là 1 bug tiềm ẩn hơn là hành vi cố ý.
- **1 `AbilityCastEvent` bị cancel vẫn tính là "đã xử lý"** (`execute()` trả `true`), nên việc cancel nó sẽ âm thầm dừng toàn bộ chuỗi cast cho lần bấm phím đó thay vì để ability tiếp theo thử — cần lưu ý nếu 1 plugin/listener khác cancel các lần cast.
- **`CAPTURE` phải được gia hạn ở mỗi lần gọi action trong lúc stance còn active**, nếu không `contextTicksLeft` sẽ tự về 0 qua `AbilityLoadout.tick()` và âm thầm release interceptor — xem cách `AetherStance.handleFire`/`handleDash` trả `AbilityResponse.capture(ticksLeft)` lại mỗi lần.

---
◀ [Entity, Boss & Spell System](Entity-Boss-Spell-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Dungeon System](Dungeon-System.md)
