# Attribute System

Package: [`com.roguesmp.attribute`](../src/main/java/com/roguesmp/attribute)

`SmpAttribute` là 1 interface đảm nhận 2 nhiệm vụ rất khác nhau qua cùng 1 API: bọc 1 `Attribute` vanilla của Bukkit (tốc độ di chuyển, kháng knockback, ...), và hook vào 1 bề mặt gameplay-event tùy chỉnh rộng cho các stat không có tương đương trong vanilla (% sát thương phép, hiệu ứng on-hit, ...). **Không có `AttributeRegistry`** — mỗi attribute chỉ đơn giản là 1 hằng số cứng trong enum [`Attributes`](../src/main/java/com/roguesmp/attribute/Attributes.java).

## Interface `SmpAttribute`

[`SmpAttribute.java`](../src/main/java/com/roguesmp/attribute/SmpAttribute.java)

Các method bắt buộc (không có default):

```java
@NotNull String getId();
@NotNull Attributes getEnumConstant();
@NotNull String getSimpleName();
@Nullable List<Component> getDisplayText(double value, @Nullable SmpPlayer player, PersistentDataContainerView pdc);
```

Hook bọc vanilla (mặc định no-op — nên override cả cặp):

```java
default void addVanillaAttribute(Player player, double value) { }    // thêm lúc equip; nhớ xóa modifier cũ trước!
default void removeVanillaAttribute(Player player) { }               // xóa lúc unequip
```

Hook gameplay-event tùy chỉnh (mặc định no-op, cùng khuôn mẫu với [`Ability`](Player-Ability-System.md) và [`Spell`](Entity-Boss-Spell-System.md)):

```java
default void tick(@NotNull SmpPlayer player, int periodIncrement, double value) { }
default void onDamageEntity(DamageEvent event, double value, @NotNull SmpPlayer player) { }
default void onKillEntity(EntityDeathEvent event, double value, @NotNull SmpPlayer player) { }
default void onHurt(DamageEvent event, double value, @NotNull SmpPlayer player) { }
default void onHurtFatal(DamageEvent event, double value, @NotNull SmpPlayer player) { }
default void onConsume(PlayerItemConsumeEvent event, double value, @NotNull SmpPlayer player) { }
default void onExpChange(PlayerExpChangeEvent event, double value, @NotNull SmpPlayer player) { }
default void onBlockBreak(BlockBreakEvent event, double value, @NotNull SmpPlayer player) { }
default void onCombust(EntityCombustEvent event, double value, @NotNull SmpPlayer player) { }        // player bị bén lửa
default void onCombustEntity(EntityCombustByEntityEvent event, double value, @NotNull SmpPlayer player) { } // player đốt entity khác
default void onProjectileHit(ProjectileHitEvent event, double value, @NotNull SmpPlayer player) { }
default void onProjectileLaunch(PlayerLaunchProjectileEvent event, double value, @NotNull SmpPlayer player) { }
default void onShootArrow(EntityShootBowEvent event, double value, @NotNull SmpPlayer player) { }
default void onConsumeArrow(ArrowConsumeEvent event, double value, @NotNull SmpPlayer player) { }
```

Ngoài ra còn có các helper lore (`defaultFlatLoreProvider`, `defaultPercentLoreProvider`, `defaultMultLoreProvider`) để dựng `getDisplayText` 1 cách nhất quán.

## Enum `Attributes` — cái "registry" không phải registry

> ⚠️ Class này nằm ở [`attribute/Attributes.java`](../src/main/java/com/roguesmp/attribute/Attributes.java) — **không** phải `constant/Attributes.java` như tên gói có thể gợi ý ở nơi khác.

Có 34 hằng số, chia nhóm theo category bằng comment trong source (combat/offense, defense/vitals, land movement, vertical/physics, aquatic, utility/world, mining). Mỗi hằng số được tạo bằng cách truyền thẳng 1 instance `SmpAttribute` mới:

```java
public enum Attributes {
    // --- COMBAT & OFFENSE ---
    MELEE_DAMAGE_BASE(new MeleeDamageBase()),
    MAGIC_DAMAGE_PERCENT(new MagicDamagePercent()),
    ...
    ;

    private final SmpAttribute attribute;
    Attributes(SmpAttribute attribute) { this.attribute = attribute; }
    public SmpAttribute getAttribute() { return attribute; }
}
```

**Không có `@SerializedName` trên bất kỳ hằng số nào** — enum này được decode qua `Codec.enumOf(Attributes.class)` (xem [Codec System](Codec-System.md)), không qua Gson, nên chuỗi JSON phải khớp (không phân biệt hoa/thường) với **tên hằng số Java**, vd. `"melee_damage_base"` hoặc `"MELEE_DAMAGE_BASE"` đều decode ra `MELEE_DAMAGE_BASE`. Đây cũng chính là codec dùng để serialize map attribute của item (`EquipAttributeComponent`/`GemDataComponent` — xem [JSON-Items](JSON-Items.md#danh-sách-attribute) để có danh sách đầy đủ theo nhóm). **Thêm 1 attribute mới chỉ đơn giản là thêm 1 hằng số enum** — không có bước đăng ký registry nào khác.

## Ví dụ 1 — bọc vanilla đơn giản: `KnockbackResistance`

[`attribute/impl/KnockbackResistance.java`](../src/main/java/com/roguesmp/attribute/impl/KnockbackResistance.java):

```java
public static final NamespacedKey MODIFIER_ID = new NamespacedKey("smp", "knockback_resistance");

@Override
public void addVanillaAttribute(Player player, double value) {
    AttributeInstance ai = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
    if (ai != null) {
        ai.removeModifier(MODIFIER_ID);   // luôn xóa modifier cũ trước
        ai.addTransientModifier(new AttributeModifier(MODIFIER_ID, value, AttributeModifier.Operation.ADD_NUMBER));
    }
}

@Override
public void removeVanillaAttribute(Player player) {
    AttributeInstance ai = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
    if (ai != null) ai.removeModifier(MODIFIER_ID);
}
```

Chỉ override method định danh + cặp bọc vanilla — không có hook gameplay-event nào. Cùng khuôn mẫu này được dùng bởi `MaxHealthFlat`, `SpeedFlat`, `Gravity`, `JumpStrength`, `StepHeight`, `Scale`, và nhiều class khác.

## Ví dụ 2 — hook gameplay tùy chỉnh: `MagicDamagePercent`

[`attribute/impl/MagicDamagePercent.java`](../src/main/java/com/roguesmp/attribute/impl/MagicDamagePercent.java):

```java
@Override
public void onDamageEntity(DamageEvent event, double value, @NotNull SmpPlayer player) {
    if (event.getDamageType() == DamageType.MAGIC) {
        event.addDamageModifier(value, DamageOperation.INCREASE_BASE);
    }
}
```

Không override cặp bọc vanilla nào cả — thuần túy là hook tùy chỉnh. `DamageType` ([`constant/DamageType.java`](../src/main/java/com/roguesmp/constant/DamageType.java)) là 1 enum tùy chỉnh (`MELEE`, `MELEE_ABILITY`, `PROJECTILE`, `PROJECTILE_ABILITY`, `MAGIC`, `THORNS`, `BLAST`, `FIRE`, `FALL`, `AILMENT`, `TRUE`, `OTHER`) được map từ `EntityDamageEvent.DamageCause` của Bukkit. `DamageEvent` ([`event/DamageEvent.java`](../src/main/java/com/roguesmp/event/DamageEvent.java)) là 1 `Event` tùy chỉnh của Bukkit, với các lệnh gọi `addDamageModifier(double value, DamageOperation operation)` theo từng giai đoạn để dựng lên sát thương cuối cùng. `MeleeDamageBase` theo đúng khuôn mẫu này, kiểm tra `DamageType.MELEE` với `DamageOperation.BASE`.

<a id="attribute-thực-sự-được-áp-dụng-như-thế-nào--không-có-attributemanager"></a>
## Attribute thực sự được áp dụng như thế nào — không có `AttributeManager`

Trách nhiệm được chia làm 2 nơi, cả 2 đều duyệt qua 1 `EnumMap<Attributes, Double> activeAttributes` sống trên player:

**Equip/unequip → attribute vanilla**, do `SmpPlayer.updateSlotStat(Player, EquipSlot, SmpItem)` ([`player/SmpPlayer.java`](../src/main/java/com/roguesmp/player/SmpPlayer.java)) điều khiển: so sánh `EquipAttributeComponent.getFinalAttributes()` của item cũ (trừ đi) với item mới (cộng vào) rồi cập nhật vào `activeAttributes`, sau đó với mỗi hằng số `Attributes` bị ảnh hưởng, gọi `removeVanillaAttribute(player)` (nếu tổng về 0) hoặc `addVanillaAttribute(player, total)`.

**Gameplay event → hook tùy chỉnh**, do `AttributeMechanic` ([`player/mechanic/AttributeMechanic.java`](../src/main/java/com/roguesmp/player/mechanic/AttributeMechanic.java)) điều khiển, 1 `PlayerMechanic` (`getPriority() == 100`) mà với mỗi event liên quan sẽ làm:

```java
player.getActiveAttributes().forEach((attributes, value) ->
        attributes.getAttribute().onDamageEntity(event, value, player)); // (hoặc tick/onKillEntity/onHurt/...)
```

`SmpPlayer` phân phối event Bukkit/tùy chỉnh ra cho mọi `PlayerMechanic` đã đăng ký; `AttributeMechanic` chỉ là 1 trong số đó, chuyên để duyệt `activeAttributes`.

## Cách thêm 1 attribute mới

1. Tạo 1 class trong `attribute/impl/` implement `SmpAttribute`. Implement `getId`, `getEnumConstant`, `getSimpleName`, `getDisplayText`.
2. Nếu nó bọc 1 `Attribute` vanilla: override `addVanillaAttribute`/`removeVanillaAttribute`, dùng 1 `NamespacedKey` modifier ổn định (xem `KnockbackResistance`). Nếu là custom: override (các) hook `on*` liên quan (xem `MagicDamagePercent`).
3. Thêm 1 hằng số vào [`attribute/Attributes.java`](../src/main/java/com/roguesmp/attribute/Attributes.java): `YOUR_ATTR(new YourImpl()),` — tên hằng số chính là chuỗi dùng trong JSON (không phân biệt hoa/thường), không cần `@SerializedName`.
4. Xong — `EquipAttributeComponent`/`SmpPlayer.updateSlotStat` và `AttributeMechanic` đã duyệt tổng quát qua mọi giá trị `Attributes`, nên hằng số mới sẽ tự động được xử lý lúc equip/unequip và trong các hook event liên quan. Không có bước đăng ký registry nào cần thiết.

## Lưu ý & lỗi thường gặp

- **Luôn `removeModifier` trước khi `addTransientModifier`** trong `addVanillaAttribute` — nếu bỏ qua bước này khi equip lại/refresh, modifier trùng lặp có thể chồng lên nhau dưới cùng 1 key (vô hại nếu key ổn định, vì Bukkit tự khử trùng theo `NamespacedKey`, nhưng vẫn là thói quen sai cần tránh).
- **`addVanillaAttribute`/`removeVanillaAttribute` được thiết kế để override theo cặp.** 1 attribute thêm modifier vanilla nhưng không bao giờ xóa nó sẽ làm rò rỉ stat bonus sau khi unequip.
- **Hook tùy chỉnh chỉ chạy qua `AttributeMechanic`**, và chỉ chạy cho những attribute có mặt trong `activeAttributes` (tức đang được equip) — 1 attribute chưa bao giờ được equip sẽ không bao giờ có hook `on*` được gọi, dù đã đăng ký trong enum.

---
◀ [Item System](Item-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [GUI Framework](GUI-Framework.md)
