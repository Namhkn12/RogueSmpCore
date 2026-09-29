# Player Class System

Package: [`com.roguesmp.player.classes`](../src/main/java/com/roguesmp/player/classes)

Hệ "lớp nhân vật" (Warrior/Mage/Archer/Assassin, ...) — 1 `PlayerClass` là dữ liệu JSON thuần, giữ 2 việc: **roster ability** (ability nào thuộc lớp này, và ability nào được cấp miễn phí + ở level bao nhiêu lúc chọn lớp), và **hạn chế vũ khí** (lớp này được cầm loại vũ khí nào). Không có class Java hành vi nào theo lớp — mọi khác biệt giữa các lớp đến từ chính danh sách ability trong roster của nó (xem [Player Ability System](Player-Ability-System.md)).

## `PlayerClass` — định nghĩa JSON

[`player/classes/PlayerClass.java`](../src/main/java/com/roguesmp/player/classes/PlayerClass.java), registry `Registries.PLAYER_CLASS = new Registry<>("classes", PlayerClass.CODEC)`, thư mục `classes/`:

| JSON key | Kiểu | Bắt buộc/mặc định |
| :--- | :--- | :--- |
| `id` | String | bắt buộc |
| `display_name` | String | optional, mặc định `""` (rơi về chính `id` nếu rỗng) |
| `icon` | String | optional, mặc định `"minecraft:barrier"` |
| `description` | mảng String | optional, mặc định `[]` |
| `default_abilities` | `Map<String, Integer>` — id ability → level | optional, mặc định `{}` |
| `allowedWeapons` | mảng String — id **tag** trên `Registries.ITEM`, không phải id item | optional, mặc định `[]` |

> Lưu ý chính tả trong source: `display_name`/`default_abilities` viết snake_case, nhưng `allowedWeapons` viết camelCase — không phải lỗi đánh máy khi bạn thấy điều này, JSON thật đúng như vậy.

```json
{
  "id": "warrior",
  "display_name": "<red>Warrior</red>",
  "icon": "minecraft:iron_sword",
  "description": [
    "Một chiến binh tuyến đầu, chuyên",
    "cận chiến và giáp nặng."
  ],
  "default_abilities": {
    "shield_bash": 1,
    "battle_cry": 1,
    "whirlwind": 0
  },
  "allowedWeapons": ["weapons", "swords"]
}
```

`"weapons"`/`"swords"` ở đây là **id của 1 tag** khai trong `items/tags/weapons.json`/`items/tags/swords.json` (xem [Registry System → Tag](Registry-System.md#tag-system)) — không phải id item trực tiếp.

## `default_abilities` — vừa là roster, vừa là bảng cấp phát

Map này làm 2 việc cùng lúc:
- **Roster đầy đủ**: mọi id nằm trong map này (kể cả value `0`) được coi là "thuộc về lớp" — `hasAbility(id)` trả `true`, `AbilityLoadout.isAllowedForCurrentClass(id)` cho phép trang bị nó (miễn ability đó đã unlock ở level > 0 theo cách nào đó, vd. quest/thành tựu).
- **Bảng cấp phát tự động**: giá trị **> 0** là level được cấp **miễn phí** ngay khi player chọn lớp lần đầu — `SmpPlayer.applyClassRoster(playerClass)` chỉ set level nếu ability đó hiện đang ở level ≤ 0 (idempotent, không bao giờ hạ level 1 ability đã unlock bằng cách khác).
- Giá trị `0` nghĩa là "có trong roster nhưng không cấp miễn phí" — vẫn hiện trong catalogue ability của lớp (ở trạng thái khóa) cho tới khi unlock qua đường khác.

## Liên kết với hệ Ability — `AbilityLoadout.isAllowedForCurrentClass`

```java
// player/ability/AbilityLoadout.java
public boolean isAllowedForCurrentClass(String abilityId) {
    String classId = smpPlayer.getPlayerData().getClassId();
    PlayerClass playerClass = Registries.PLAYER_CLASS.get(classId);
    return playerClass != null && playerClass.hasAbility(abilityId);
}
```

`equip(...)` từ chối trang bị 1 ability không nằm trong roster của lớp hiện tại. **Ability chỉ có thể trang bị khi lớp sở hữu nó đang active — nhưng level đã unlock là vĩnh viễn**, đổi lớp qua lại không mất level đã lên, chỉ tạm thời không trang bị được cho đến khi đổi lại đúng lớp đó (hoặc 1 lớp khác cũng liệt kê ability này trong roster).

`SmpPlayer` ([`player/SmpPlayer.java`](../src/main/java/com/roguesmp/player/SmpPlayer.java)) giữ `Holder<PlayerClass> playerClass` — 1 [tham chiếu ổn định theo id](Registry-System.md#holdert--tham-chiếu-ổn-định-qua-id), không phải id chuỗi trần — nên reload registry `classes/` không làm mất/lệch lớp player đang chọn. `setPlayerClass(...)` ghi id vào `PlayerData` rồi gọi `applyClassRoster(...)`; `syncClassRoster()` chạy lại bước cấp phát này sau 1 lần hot-reload (xem [Registry System → hot reload](Registry-System.md)).

## Cách thêm 1 lớp nhân vật mới

1. Viết `classes/<id>.json` với `display_name`, `icon`, `description`, `default_abilities` (chọn ability nào cấp miễn phí ở level mấy, ability nào chỉ nằm trong roster để unlock sau), `allowedWeapons` (id các tag vũ khí — tạo tag mới trong `items/tags/` nếu cần 1 nhóm vũ khí riêng cho lớp này).
2. Không cần đăng ký gì thêm trong code — `Registries.PLAYER_CLASS` tự load, và `AbilityLoadout`/`SmpPlayer` tra theo id lúc runtime.
3. Đảm bảo mọi ability id trong `default_abilities` thực sự tồn tại trong [`AbilityInfos`](../src/main/java/com/roguesmp/player/ability/AbilityInfos.java) — id sai chỉ khiến `applyClassRoster` bỏ qua entry đó, không có exception nào báo.

## Lưu ý & lỗi thường gặp

- **`allowedWeapons` là id tag, không phải id item** — viết id item trực tiếp vào đây sẽ không có tác dụng gì (không match được tag nào tên như vậy), cần bọc item vào 1 tag trước.
- **`default_abilities` giá trị `0` vẫn cho vào roster** — dễ nhầm với "loại bỏ khỏi lớp." Muốn ability không thuộc lớp này chút nào thì đơn giản là không liệt kê nó trong map, chứ không đặt `0`.
- **`applyClassRoster` không bao giờ hạ level** — nếu 1 ability đã unlock ở level 3 bằng cách khác (nâng cấp, phần thưởng, ...), chọn lại 1 class có `default_abilities` ghi level 1 cho ability đó **không** hạ nó về 1.

---
◀ [Player Ability System](Player-Ability-System.md) · Về [Trang chủ](Home.md) · Tiếp theo: [Dungeon System](Dungeon-System.md)
