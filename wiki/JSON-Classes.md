# JSON Reference — Classes (lớp nhân vật)

Thư mục: `classes/` (đệ quy, id = đường dẫn bỏ `.json`). Quy tắc chung: [JSON-Overview](JSON-Overview.md).

1 file class quyết định: **ability nào thuộc lớp này** (roster), **ability nào được tặng khi chọn lớp**, và **vũ khí nào được cầm**. Mọi khác biệt giữa các lớp đến từ danh sách ability và tag vũ khí — không có hành vi Java riêng theo lớp.

## Cấu trúc file

```json
{
  "id": "warrior",
  "display_name": "<red>Warrior</red>",
  "icon": "minecraft:iron_sword",
  "description": ["Chiến binh tuyến đầu,", "chuyên cận chiến và giáp nặng."],
  "default_abilities": {
    "shield_bash": 1,
    "brute_force": 1,
    "indomitable": 1,
    "glorious_battle": 0
  },
  "allowedWeapons": ["swords", "axes"]
}
```

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | string | bắt buộc | Id class. Phải bằng đường dẫn file. |
| `display_name` | string MiniMessage | optional, `""` | Tên hiển thị; rỗng → dùng `id`. |
| `icon` | string | optional, `"minecraft:barrier"` | **Key model item** (dùng làm `item_model` của icon trong GUI chọn lớp), không phải tên Material. |
| `description` | mảng string MiniMessage | optional, `[]` | Mô tả trong GUI chọn lớp. |
| `default_abilities` | object: **id ability → level** (int) | optional, `{}` | Roster + bảng tặng (xem dưới). **Lenient**: entry lỗi bị bỏ + log. |
| `allowedWeapons` | mảng string | optional, `[]` | **Id tag item** (không phải id item) mà lớp được phép cầm. (camelCase — đúng như code.) |

## `default_abilities` — 2 vai trò

1. **Roster**: chỉ ability có id trong map này mới được **trang bị** khi đang ở lớp này, và chỉ chúng hiện trong GUI chọn ability. Id không phải ability đã đăng ký bị bỏ khỏi danh sách im lặng. Đổi lớp sẽ gỡ các ability không thuộc roster lớp mới.
2. **Tặng khi chọn lớp**: giá trị **> 0** là level được tặng ngay khi người chơi chọn lớp, **nếu** level hiện tại của họ ≤ 0 (không bao giờ hạ level ability đã có). Giá trị **`0`** = "thuộc lớp nhưng không tặng" — hiện trong catalogue ở trạng thái khóa.

Level ability đã mở là **vĩnh viễn**: đổi lớp qua lại không mất level, chỉ tạm thời không trang bị được ability ngoài roster. Muốn ability **không thuộc** lớp thì **không liệt kê** nó (đừng đặt `0`). Id ability hợp lệ xem [JSON-Abilities](JSON-Abilities.md#ability-có-sẵn).

## `allowedWeapons` — giới hạn vũ khí

Giá trị là **id tag item** viết **chữ thường, không có `#`**, tag nằm ở `items/tags/<tag>.json` (xem [Tag](JSON-Overview.md#tag)). Cách kiểm tra khi người chơi cầm vũ khí ở tay chính:

1. Item phải thuộc tag có sẵn **`weapons`** (`items/tags/weapons.json`). Item không thuộc `weapons` thì **không bao giờ bị hạn chế**.
2. Lấy mọi tag mà item thuộc về. Nếu **bất kỳ** tag nào nằm trong `allowedWeapons` → được phép. Ngược lại → bị hạn chế.
3. `allowedWeapons` rỗng → **mọi** vũ khí thuộc tag `weapons` đều bị hạn chế.

Hạn chế gồm: không cast được ability, hiện cảnh báo đỏ trên action bar mỗi tick, và chỉ số của vũ khí đó không được cộng.

Vì vậy để 1 vũ khí dùng được: (a) có mặt trong tag `weapons` **và** (b) có mặt trong tag nào đó mà lớp cho phép:
```json
// items/tags/weapons.json
["#swords", "#axes", "#bows"]
// items/tags/swords.json
["iron_sword", "weapons/fire_sword"]
```

## Dành cho dev

Không cần đăng ký gì trong code — `Registries.PLAYER_CLASS` tự load. Logic nằm ở `SmpPlayer.applyClassRoster`/`syncClassRoster`, `AbilityLoadout.isAllowedForCurrentClass`, `ClassRestrictionMechanic`. `SmpPlayer` giữ lớp hiện tại dưới dạng `Holder<PlayerClass>` nên reload `classes/` không làm lệch lớp đang chọn (xem [Registry System](Registry-System.md)). Hot reload thư mục này tự đồng bộ lại roster của người chơi đang online.

---
◀ [JSON-Abilities](JSON-Abilities.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Quests](JSON-Quests.md)
