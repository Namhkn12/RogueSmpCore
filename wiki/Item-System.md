# Item System

Package: [`com.roguesmp.item`](../src/main/java/com/roguesmp/item)

Mô hình 2 tầng: 1 template JSON bất biến (`BaseItem`) và 1 wrapper runtime sống trên từng `ItemStack` (`SmpItem`), được mở rộng thông qua interface hook kiểu plugin `ItemComponent`.

## `BaseItem` — template

[`BaseItem.java`](../src/main/java/com/roguesmp/item/BaseItem.java) là prototype bất biến, định nghĩa bằng JSON: `id`, `base` (Material), và `Map<String, ItemComponent> components`. Nó thuộc sở hữu của [`Registries.ITEM`](Registry-System.md) và không bao giờ tự thay đổi — `generateItemStack(...)` chỉ đơn giản tạo 1 `new SmpItem(this)` tạm thời rồi ủy quyền:

```java
public static final Codec<BaseItem> CODEC = Codec.composite(
        Codec.STRING.fieldOf("id").forGetter(BaseItem::getId),
        Codec.MATERIAL.fieldOf("base").forGetter(BaseItem::getBase),
        Codec.<ItemComponent>dispatchedMap(Registries.ITEM_COMPONENT_CODEC::getOrThrow)
                .optionalFieldOf("components", Map.of())
                .forGetter(BaseItem::getComponents),
        BaseItem::new
);
```

Xem [Codec System → ví dụ BaseItem](Codec-System.md#full-worked-example--baseitem-dispatch--dispatchedmap--registry-loading) để biết `dispatchedMap` ở đây tra codec của từng component như thế nào, và [Registry System](Registry-System.md) để biết các file `*.json` dưới `plugin.getDataFolder()/items/` được decode vào `Registries.ITEM` lúc khởi động ra sao.

`isUnique()` không còn là field JSON — nó được tính tự động trong constructor: `true` nếu bất kỳ component nào trong `components` implement marker interface [`UniqueTrackingComponent`](../src/main/java/com/roguesmp/item/component/UniqueTrackingComponent.java). Component nào giữ state per-instance (lưu/đọc qua PDC bằng `save`/`load`, vd. `DurabilityComponent`, `EnchantComponent`, `GemSocketComponent`) nên implement interface này thay vì `ItemComponent` — item chứa nó sẽ tự động unique, không cần khai báo `"unique": true` thủ công nữa.

## `SmpItem` — wrapper sống

[`SmpItem.java`](../src/main/java/com/roguesmp/item/SmpItem.java) bọc quanh 1 `ItemStack` thật. **Không bao giờ tự tạo 1 instance cho 1 item đã tồn tại trong game** — luôn đi qua cache:

```java
SmpItem item = SmpItem.wrap(itemStack);                          // không có context player
SmpItem item = SmpItem.wrap(itemStack, smpPlayer);                // có context player
SmpItem item = SmpItem.wrap(itemStack, smpPlayer, onCacheMiss);   // + callback lúc wrap lần đầu
```

Cả 3 đều ủy quyền cho [`ItemManager`](../src/main/java/com/roguesmp/item/ItemManager.java) (singleton, `getInstance()`), class này giữ 1 cache Caffeine (`expireAfterAccess(5s)`, `expireAfterWrite(20s)`) khóa theo UUID của item (lấy từ PDC). Item không unique (không có UUID ổn định) không thể cache được — `wrapItem` tạo 1 `SmpItem` tạm thời mới cho mỗi lần gọi với những item này.

Tự khởi tạo trực tiếp chỉ dành cho 2 trường hợp mà chính `SmpItem` cần: `new SmpItem(ItemStack)` tái tạo lại từ PDC của 1 stack đã có sẵn (đọc `Keys.ITEM_ID`/`Keys.ITEM_UUID`, tra `BaseItem` qua `ItemRegistry`), và `new SmpItem(BaseItem)` tạo 1 item hoàn toàn mới từ template (sinh UUID nếu `unique`).

### Pipeline lắp ráp `generateItemStack`

Đây là method biến dữ liệu component thành 1 `ItemStack` thật sự được render — nên đọc kỹ trước khi viết 1 component mới, vì nó quyết định *thời điểm* các hook của bạn chạy so với nhau:

```java
public ItemStack generateItemStack(@Nullable SmpPlayer player, int stackAmount) {
    if (baseItem == null) return itemStack;

    ItemStack result = ItemStack.of(baseItem.getBase(), stackAmount);
    // dọn bớt tooltip vanilla, ép unbreakable, ẩn hiển thị attribute/enchant mặc định
    result.setData(DataComponentTypes.TOOLTIP_DISPLAY, ...);
    result.unsetData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
    result.setData(DataComponentTypes.UNBREAKABLE);

    PersistentDataContainerView oldData = itemStack.getPersistentDataContainer();
    LoreBuilder loreBuilder = new LoreBuilder();

    result.editPersistentDataContainer(pdc -> {
        oldData.copyTo(pdc, true);
        pdc.set(Keys.ITEM_ID, PersistentDataType.STRING, baseItem.getId());
        ItemDataContext dataContext = new ItemDataContext(this, player, result, pdc);
        ItemLoreContext loreContext = new ItemLoreContext(this, player, loreBuilder, pdc);

        applyModifiers(player);   // 1. các modifier xuyên component chạy trước (gem, enchant, ...)

        for (Map.Entry<String, ItemComponent> entry : componentMap.entrySet()) {
            ItemComponent itemComponent = entry.getValue();
            itemComponent.save(pdc);              // 2. lưu state runtime có thể thay đổi
            itemComponent.modifyStack(dataContext); // 3. đụng vào data component vanilla
            itemComponent.contributeLore(loreContext); // 4. đẩy dòng lore
        }
    });

    result.setData(DataComponentTypes.LORE, ItemLore.lore(loreBuilder.build()));
    this.itemStack = result;
    return result;
}
```

Thứ tự: **tạo stack gốc → dọn data component vanilla → copy PDC cũ sang + gắn `ITEM_ID` → `applyModifiers(player)` → với mỗi component: `save` → `modifyStack` → `contributeLore` → ghi lore đã gom lại.**

`applyModifiers` chạy *trước* vòng lặp component để 1 [`ItemModifier`](../src/main/java/com/roguesmp/item/modifier/ItemModifier.java) (vd. `EnchantAttributeModifier` đọc `EnchantComponent` rồi đẩy giá trị vào `EquipAttributeComponent` qua `putModifier(...)`) có thể thay đổi state của component mà vòng lặp sau đó sẽ render/lưu lại. Các modifier được sắp thứ tự trong [`ModifierRegistry`](../src/main/java/com/roguesmp/registry/ModifierRegistry.java) (`GemModifier`, `EnchantAttributeModifier`, `BrokenModifier`) và chỉ chạy 1 lần cho mỗi instance `SmpItem` (nhờ cờ `loadedModifiers`).

`loadData(pdc)` là phần đối xứng, chạy 1 lần khi 1 `SmpItem` được tạo — với mỗi component trên `BaseItem`, nó gọi `copy()` để lấy 1 bản copy mutable riêng cho instance, rồi `load(pdc)` để hydrate state runtime:

```java
private void loadData(PersistentDataContainerView pdc) {
    for (Map.Entry<String, ItemComponent> entry : baseItem.getComponents().entrySet()) {
        ItemComponent copy = entry.getValue().copy();
        componentMap.put(entry.getKey(), copy);
        copy.load(pdc);
    }
}
```

## `ItemComponent` — điểm mở rộng

[`ItemComponent.java`](../src/main/java/com/roguesmp/item/component/ItemComponent.java), toàn bộ interface:

```java
public interface ItemComponent {
    /** Copy BaseItem component into active SmpItem instance */
    @NotNull ItemComponent copy();

    default void contributeLore(ItemLoreContext context) { }
    /** For modifying vanilla ItemStack stuff */
    default void modifyStack(ItemDataContext context) { }
    /** Load data from pdc */
    default void load(PersistentDataContainerView pdc) { }
    /** Save data to pdc */
    default void save(PersistentDataContainer pdc) { }
}
```

| Hook | Bắt buộc? | Được gọi khi | Dùng để |
| :--- | :--- | :--- | :--- |
| `copy()` | **có** | 1 lần, trong `SmpItem.loadData`, biến component dùng chung của `BaseItem` thành 1 bản copy mutable riêng cho instance | Với component không state: `return this;`. Với component mutable: `return new X(...)`. |
| `load(pdc)` | không | 1 lần, ngay sau `copy()`, trong `SmpItem.loadData` | Hydrate state runtime mutable (vd. độ bền hiện tại) từ PDC sẵn có của item. |
| `save(pdc)` | không | Mỗi lần gọi `generateItemStack`, đầu tiên trong vòng lặp component | Lưu state runtime mutable vào PDC của stack mới, trước khi `modifyStack`/`contributeLore` chạy. |
| `modifyStack(context)` | không | Mỗi lần gọi `generateItemStack`, sau `save` | Thay đổi `DataComponentTypes` vanilla trên `context.newStack()` (tên, max stack size, nội dung potion, ...). |
| `contributeLore(context)` | không | Mỗi lần gọi `generateItemStack`, cuối cùng | Đẩy dòng lore vào `LoreBuilder` dùng chung qua `context.builder().putLines(priority, lines)`. |

## `ComponentKey` và `ComponentKeys` — định danh & đăng ký

`ComponentKey<T extends ItemComponent>` ([`ComponentKey.java`](../src/main/java/com/roguesmp/item/component/ComponentKey.java)) chỉ đơn giản là `record ComponentKey<T>(String id)` — 1 handle đã gõ kiểu, dùng làm key trong map component của `BaseItem`/`SmpItem` (qua `.id()`), và làm tham số cho `baseItem.getComponent(key)` / `smpItem.getComponent(key)`.

> ⚠️ Class đăng ký này **không còn nằm ở `constant/ComponentKeys.java`** — nó đã dọn sang [`item/component/ItemComponentKeys.java`](../src/main/java/com/roguesmp/item/component/ItemComponentKeys.java), cùng lúc đó `registry/ItemComponentCodecRegistry.java` (lớp trung gian cũ) cũng bị xóa. Nếu thấy tài liệu/code cũ nhắc `constant.ComponentKeys` hay `ItemComponentCodecRegistry`, đó là tên đã lỗi thời.

[`ItemComponentKeys.java`](../src/main/java/com/roguesmp/item/component/ItemComponentKeys.java) khai báo mọi hằng số `ComponentKey<T>`, và trong static initializer, đăng ký `Codec` của từng component cùng lúc, gọi thẳng `Registries.ITEM_COMPONENT_CODEC.register(...)` — không qua lớp trung gian nào nữa:

```java
public static final ComponentKey<DurabilityComponent> DURABILITY;
static {
    DURABILITY = register("durability", DurabilityComponent.CODEC);
    ...
    BROKEN = new ComponentKey<>("broken"); // Transient so it has no CODEC.
}

public static void loadClass() { }

public static <T extends ItemComponent> ComponentKey<T> register(String id, Codec<T> codec) {
    Registries.ITEM_COMPONENT_CODEC.register(id, codec);
    return new ComponentKey<>(id);
}
```

`register(id, codec)` vừa tạo key vừa lưu codec vào [`Registries.ITEM_COMPONENT_CODEC`](Registry-System.md) — bảng tra mà `dispatchedMap` của `BaseItem.CODEC` dùng khi decode 1 object `"components"`. Component không có `CODEC` (transient/chỉ tồn tại runtime — hiện tại là `BROKEN` và `ItemTypeComponent`) được tạo trực tiếp `new ComponentKey<>("id")` mà không gọi `register(...)`.

`loadClass()` là 1 no-op cố tình để trống — công việc duy nhất của nó là *được gọi* (`RogueSmpCore.init()`, trước khi bất kỳ thứ gì decode JSON của item) để ép static initializer của class này chạy. Java load class theo kiểu lazy, nên nếu không có lệnh gọi này, việc đăng ký codec component có thể bị trễ đến lần dùng đầu tiên — **gọi nó, đừng xóa nó.** Entity có 1 hệ mirror y hệt ([`EntityComponentKeys`](../src/main/java/com/roguesmp/entity/component/EntityComponentKeys.java) đăng ký vào `Registries.ENTITY_COMPONENT_CODEC`) — xem [Entity, Boss & Spell System](Entity-Boss-Spell-System.md).

## Hai ví dụ minh họa

### Đơn giản — `StackSizeComponent` (không state, không PDC)

```java
public record StackSizeComponent(int size) implements ItemComponent {
    public static final Codec<StackSizeComponent> CODEC = Codec.INT.xmap(StackSizeComponent::new, StackSizeComponent::size);

    @Override public @NotNull ItemComponent copy() { return this; } // không có state mutable, chia sẻ an toàn

    @Override public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.MAX_STACK_SIZE, size);
    }
}
```

### Phức tạp — `DurabilityComponent` (giá trị template bất biến + state runtime mutable backing bởi PDC)

```java
public final class DurabilityComponent implements ItemComponent {
    public static final NamespacedKey DURABILITY_KEY = new NamespacedKey(Keys.GLOBAL_NAMESPACE, "durability");
    public static final Codec<DurabilityComponent> CODEC = Codec.INT.xmap(DurabilityComponent::new, DurabilityComponent::maxDurability);

    private final int maxDurability;       // từ JSON, dùng chung/bất biến
    @GsonIgnore
    private int currentDurability;         // chỉ tồn tại runtime, KHÔNG được round-trip qua JSON của BaseItem

    public DurabilityComponent(int maxDurability) {
        this.maxDurability = maxDurability;
        this.currentDurability = maxDurability;
    }

    @Override public @NotNull ItemComponent copy() { return new DurabilityComponent(maxDurability); }

    @Override public void load(PersistentDataContainerView pdc) {
        Integer current = pdc.get(DURABILITY_KEY, PersistentDataType.INTEGER);
        this.currentDurability = current == null ? maxDurability : current;
    }

    @Override public void save(PersistentDataContainer pdc) {
        pdc.set(DURABILITY_KEY, PersistentDataType.INTEGER, currentDurability);
    }

    @Override public void contributeLore(ItemLoreContext context) {
        // ... dựng 1 dòng lore "Độ bền: X/Y" có màu, dựa trên currentDurability/maxDurability
        context.builder().putLines(110, List.of(Component.empty(), loreComponent));
    }
}
```

`@GsonIgnore` đánh dấu `currentDurability` bị loại khỏi quá trình round-trip JSON của `BaseItem` — đây là state riêng cho từng instance, chỉ sống trong PDC của 1 `ItemStack`, được hydrate qua `load`/`save`.

<a id="tham-khảo-đầy-đủ--mọi-component-đã-đăng-ký-dành-cho-designer-viết-json"></a>
## Tham khảo đầy đủ — mọi component đã đăng ký (dành cho designer viết JSON)

Bảng dưới liệt kê **mọi** key hợp lệ trong object `"components"` của 1 file `items/*.json`, tính đến thời điểm viết trang này. Component không có cột "CODEC" (`broken`, `item_type`) là state runtime thuần — không thể khai trong JSON, bỏ qua khi viết item mới.

| Key JSON | Class | Shape | Bắt buộc/mặc định |
| :--- | :--- | :--- | :--- |
| `name` | `NameComponent` | chuỗi MiniMessage trần | — |
| `stack_size` | `StackSizeComponent` | số nguyên trần | — |
| `description` | `DescriptionComponent` | mảng chuỗi MiniMessage | — |
| `durability` | `DurabilityComponent` | số nguyên trần (độ bền tối đa) | — |
| `durability_repair` | `DurabilityRepairComponent` | số nguyên trần (lượng hồi mỗi lần sửa) | — |
| `enchant` | `EnchantComponent` | `{ "enchants": {"<ENCHANT>": <level>, ...} }` | `enchants` bắt buộc |
| `attribute` | `EquipAttributeComponent` | `{ "attributes": {"<ATTR>": <double>, ...}, "slot": "<EquipSlot>" }` | cả 2 bắt buộc |
| `socket` | `GemSocketComponent` | số nguyên trần (số socket) | — |
| `gem_data` | `GemDataComponent` | `{ "attributes": {"<EquipSlot>": {"<ATTR>": <double>}}, "success_chance": <double, mặc định 1.0> }` | `attributes` bắt buộc |
| `consumable` | `ConsumableComponent` | xem ví dụ bên dưới | tất cả field bắt buộc |
| `potion_content` | `PotionContentComponent` | `{ "color": "A,R,G,B", "effects": [{"type","duration","amplifier"}] }` | cả 2 bắt buộc |
| `head_skin` | `PlayerHeadSkinComponent` | chuỗi trần (id trong `SkinRegistry`) | — |
| `item_model` | `ItemModelComponent` | chuỗi trần (namespaced key, vd. `"roguesmp:fire_sword"`) | — |
| `equippable` | `EquippableComponent` | xem ví dụ bên dưới | `slot` bắt buộc, còn lại optional |
| `wrench` | `WrenchComponent` | `{}` (object rỗng, đánh dấu "item này là cờ lê") | — |
| `block_place` | `BlockPlaceComponent` | `{ "block": "<id trong Registries.BLOCK_PROPERTIES>" }` | bắt buộc |
| `passive_ability` | `PassiveAbilityComponent` | mảng `ItemAbility` đa hình (`"type"`) — xem [mục riêng](#itemability--năng-lực-thụ-động-gắn-trên-item) | — |
| `glint` | `EnchantGlintComponent` | boolean trần (cưỡng chế hiệu ứng lấp lánh) | — |
| `magic_power` | `MagicPowerComponent` | số nguyên trần (mana tối đa) | — |
| `random_stat` | `RandomStatComponent` | `{}` (object rỗng — roll random quality lúc PDC load lần đầu) | — |
| `usage_timer` | `UsageTimerComponent` | số nguyên trần (số tick còn lại trước khi item tự xóa lúc đang mặc) | — |
| `command_executor` | `CommandExecutorComponent` | chuỗi trần (lệnh gốc; click trái mở dialog đổi, click phải chạy) | — |

Enum dùng trong bảng trên: `EquipSlot` — `MAINHAND, OFFHAND, HEAD, CHEST, LEGS, FEET, PROJECTILE`. `Attributes` (viết `"attribute"`/`"gem_data"` bằng đúng tên hằng số, không phân biệt hoa/thường) — **Combat & Offense**: `MELEE_DAMAGE_BASE, ATTACK_SPEED_BASE, PROJECTILE_DAMAGE_BASE, PROJECTILE_SPEED_BASE, BREAK_STRENGTH, THROW_RATE_BASE, MELEE_DAMAGE_PERCENT, MAGIC_DAMAGE_PERCENT, THROW_RATE_PERCENT, PROJECTILE_DAMAGE_PERCENT, PROJECTILE_SPEED_PERCENT, CRIT_DAMAGE_FLAT, ATTACK_KNOCKBACK, SWEEPING_DAMAGE_RATIO`; **Defense & Vitals**: `DEFENSE_FLAT, MAX_HEALTH_FLAT, MAX_HEALTH_PERCENT, KNOCKBACK_RESISTANCE`; **Land Movement**: `SPEED_FLAT, SPEED_PERCENT, SNEAKING_SPEED, MOVEMENT_EFFICIENCY`; **Vertical & Physics**: `JUMP_STRENGTH, GRAVITY, STEP_HEIGHT, FALL_DAMAGE, SAFE_FALL_DISTANCE`; **Aquatic**: `WATER_MOVEMENT_EFFICIENCY, SUBMERGED_MINING_SPEED, OXYGEN_BONUS`; **Utility & World**: `ENTITY_REACH, BLOCK_REACH, MINING_EFFICIENCY, BURNING_TIME`; **Mining** (chỉ block tùy chỉnh): `MINING_SPEED`; và `SCALE`, `LUCK`. `Enchants` (dùng trong `"enchant"`) theo nhóm: bảo vệ (`FIRE_PROTECTION, BLAST_PROTECTION, PROJECTILE_PROTECTION`), tiện ích giáp (`REGENERATION, RESPIRATION, AQUA_AFFINITY, THORNS, FEATHER_FALLING, DEPTH_STRIDER, FROST_WALKER, SOUL_SPEED, SWIFT_SNEAK`), sát thương trực tiếp (`SMITE, BANE_OF_ARTHROPODS, FIRE_SLAYER, IMPALING, REGICIDE`), hiệu ứng vũ khí (`SMASH, BLEEDING, POISONING`), di chuyển (`RIPTIDE, LUNGE`), tiện ích công cụ (`LOOTING, EFFICIENCY, SILK_TOUCH, FORTUNE, LUCK_OF_THE_SEA, LURE, MENDING`), infusion (`VIGOR, FOCUS, FORTITUDE, PERSPICACITY, CELERITY`), lời nguyền (`EXHAUSTION, IRREPARABLE, UNCRITABLE`), thẩm mỹ (`GLOWING`), cùng vài cái khác (`CHANNELING, MULTISHOT, QUICK_CHARGE, PIERCING, DENSITY, WIND_BURST, EPOCH, RETRIEVAL, LIFESTEAL, KNOCKBACK, SWEEPING_EDGE, PUNCH, FLAME, FIRE_ASPECT, ICE_ASPECT`).

### Ví dụ đầy đủ — vũ khí cận chiến với gem socket

```json
{
  "id": "fire_sword",
  "base": "NETHERITE_SWORD",
  "components": {
    "name": "<gold>Fire Sword",
    "description": ["<gray>A blade forged in dragonfire.", "<gray>Handle with care."],
    "durability": 500,
    "attribute": {
      "attributes": { "MELEE_DAMAGE_BASE": 7.0, "ATTACK_SPEED_BASE": -2.4 },
      "slot": "MAINHAND"
    },
    "enchant": { "enchants": { "SHARPNESS": 3, "FIRE_ASPECT": 2 } },
    "socket": 2,
    "gem_data": {
      "attributes": { "MAINHAND": { "MELEE_DAMAGE_PERCENT": 5.0 } },
      "success_chance": 0.8
    },
    "glint": true
  }
}
```

`durability` khiến item này tự động `isUnique() == true` (implement `UniqueTrackingComponent`) — mỗi item đúc ra có UUID riêng, được cache trong `ItemManager`.

### Ví dụ đầy đủ — vật phẩm ăn được (potion + hiệu ứng)

```json
{
  "id": "phoenix_elixir",
  "base": "POTION",
  "components": {
    "name": "<light_purple>Phoenix Elixir",
    "item_model": "roguesmp:phoenix_elixir",
    "consumable": {
      "effects": [ { "id": "speed", "duration": 200 } ],
      "hunger": 0,
      "saturation": 0,
      "canAlwaysEat": true,
      "consumeSeconds": 1.2,
      "animation": "DRINK",
      "sound": "minecraft:entity.generic.drink",
      "hasParticles": true
    },
    "potion_content": {
      "color": "255,0,120,255",
      "effects": [ { "type": "speed", "duration": 1200, "amplifier": 1 } ]
    }
  }
}
```

`consumable.effects` là danh sách `SmpEffect` đa hình (dispatch trên field `"id"` riêng của effect, xem [Codec System](Codec-System.md#polymorphic-dispatch-codecdispatch)) — không nên nhầm với `potion_content.effects` (chỉ là hiệu ứng potion vanilla, `StoredEffect` đơn giản `{type, duration, amplifier}`).

### Ví dụ đầy đủ — trang bị (equippable) + đầu người

```json
{
  "id": "dragon_helm",
  "base": "PLAYER_HEAD",
  "components": {
    "name": "<red>Dragon Helm",
    "head_skin": "dragon_helm_skin",
    "equippable": {
      "slot": "HEAD",
      "asset_id": "roguesmp:dragon_helm",
      "damage_on_hurt": false
    },
    "attribute": {
      "attributes": { "MAX_HEALTH_FLAT": 20.0, "DEFENSE_FLAT": 8.0 },
      "slot": "HEAD"
    }
  }
}
```

`equippable.slot` dùng enum `EquipmentSlot` vanilla của Bukkit (`HAND, OFF_HAND, FEET, LEGS, CHEST, HEAD, BODY`) — **khác** với `EquipSlot` tùy chỉnh của `attribute`/`gem_data` (chú ý 2 enum khác nhau, dễ nhầm khi cùng nằm trong 1 item).

<a id="itemability--năng-lực-thụ-động-gắn-trên-item"></a>
## `ItemAbility` — năng lực thụ động gắn trên item

[`ItemAbility.java`](../src/main/java/com/roguesmp/item/ability/ItemAbility.java) là interface theo đúng khuôn mẫu hook mặc định no-op như `ItemComponent`/`SmpAttribute`/`Ability`: `getTypeId()` (bắt buộc), cộng các hook tùy chọn `provideAttribute(SmpPlayer, SmpItem)` (đóng góp thêm `Map<Attributes, Double>` ngoài `attribute`/`gem_data`), `onTick(SmpPlayer, SmpItem, interval)` (thụ động, định kỳ), `onDamageEntity`/`onHurt`, `onDurabilityChange`, `onInteractEntity` (right-click 1 entity khi đang cầm/mặc item). Không có 1 trigger cố định duy nhất — mỗi impl override đúng hook nó cần. Gắn vào item qua component `passive_ability` (`List<ItemAbility>`, dispatch đa hình trên `"type"`, xem [`Registries.ITEM_ABILITY_CODEC`](Registry-System.md)).

3 kiểu đã đăng ký trong [`ItemAbilities.java`](../src/main/java/com/roguesmp/item/ability/ItemAbilities.java):

| `type` | Ý nghĩa | JSON |
| :--- | :--- | :--- |
| `unyielding_edge` | Sát thương cận chiến tăng dần khi độ bền giảm | `{ "type": "unyielding_edge", "dmgPerUnitLoss": 5.0, "amountLossPerUnit": 200 }` (2 field optional, default như trên) |
| `barking` | Định kỳ phát 1 âm thanh ngẫu nhiên, chỉ người cầm nghe | `{ "type": "barking", "interval": 200, "sounds": [{"key": "entity.wolf.growl", "volume": 1.0, "pitch": 1.0}] }` |
| `entity_zapper` | Click phải xóa entity đang nhắm (từ chối trên player) | `{ "type": "entity_zapper" }` (không field) |

```json
"passive_ability": [
  { "type": "unyielding_edge", "dmgPerUnitLoss": 8.0, "amountLossPerUnit": 150 },
  { "type": "barking", "interval": 400, "sounds": [ { "key": "entity.wolf.growl" } ] }
]
```

## Cách thêm 1 `ItemComponent` mới

1. Tạo 1 class trong `item/component/impl/` implement `ItemComponent`. Khai `public static final Codec<YourComponent> CODEC` — `Codec.INT.xmap(...)`/tương tự cho 1 giá trị đơn, `Codec.composite(...)` cho nhiều field có tên (xem [Codec System](Codec-System.md)).
2. Implement `copy()` (bắt buộc). Chỉ thêm `load`/`save` nếu có state runtime mutable cần lưu vào PDC (định nghĩa 1 `NamespacedKey` dưới `Keys.GLOBAL_NAMESPACE`, theo mẫu `DurabilityComponent.DURABILITY_KEY`). Thêm `modifyStack` để đụng vào `DataComponentTypes` vanilla. Thêm `contributeLore` để đóng góp dòng lore.
3. Đăng ký nó trong [`ItemComponentKeys.java`](../src/main/java/com/roguesmp/item/component/ItemComponentKeys.java): khai `public static final ComponentKey<YourComponent> YOUR_KEY;` và trong static block, `YOUR_KEY = register("your_id", YourComponent.CODEC);`.
4. Không cần nối gì thêm — `ComponentKeys.loadClass()` đã chạy sẵn lúc plugin `init()`.
5. Tham chiếu `"your_id"` trong object `"components"` của 1 item ở `plugins/.../items/*.json`; `BaseItem.CODEC` sẽ tự động decode nó.
6. Truy cập nó ở nơi khác qua `baseItem.getComponent(ComponentKeys.YOUR_KEY)` / `smpItem.getComponent(ComponentKeys.YOUR_KEY)`.

## `ItemModifier` — logic xuyên component

[`ItemModifier.java`](../src/main/java/com/roguesmp/item/modifier/ItemModifier.java): interface 1 method `void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player)`. Dùng cho hành vi trải rộng trên *nhiều* component — vd. gem/enchant đẩy giá trị dẫn xuất vào `EquipAttributeComponent`, hoặc state của `BrokenComponent` phụ thuộc vào `DurabilityComponent`. Được đăng ký theo thứ tự cố định trong `ModifierRegistry` và luôn chạy 1 lần, ở đầu `SmpItem.generateItemStack`, trước khi bất kỳ hook component nào chạy.

## Lưu ý & lỗi thường gặp

- **Không bao giờ `new SmpItem(itemStack)` cho 1 item đã có trong túi đồ/thế giới của player** — dùng `SmpItem.wrap(...)` để tận dụng cache của `ItemManager` và không chạy lại `applyModifiers` 1 cách không cần thiết.
- **Đánh dấu `@GsonIgnore` cho mọi field chỉ tồn tại trong PDC/runtime.** Nếu nó vô tình round-trip qua JSON của `BaseItem`, dữ liệu template và state sống sẽ bị lẫn vào nhau.
- **Thứ tự hook của component là cố định** (`save` → `modifyStack` → `contributeLore`, sau `applyModifiers`) — nếu lore của 1 component phụ thuộc vào state mà `modifyStack` của component khác thiết lập, sự phụ thuộc đó sẽ không thấy được; hãy dùng 1 `ItemModifier` cho các mối quan tâm về thứ tự xuyên component.
- **`ItemRegistry.getInstance()` cũ đã bị `@Deprecated`** — xem [Registry System](Registry-System.md#the-migration-registry-getinstance--registries); dùng `Registries.ITEM` trong code mới.

---
◀ [Registry System](Registry-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Attribute System](Attribute-System.md)
