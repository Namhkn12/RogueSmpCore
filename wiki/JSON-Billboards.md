# JSON Reference — Billboards

Thư mục: `billboards/` (đệ quy, id = đường dẫn bỏ `.json`, **không** có field `"id"`). Quy tắc chung: [JSON-Overview](JSON-Overview.md).

Billboard là bảng chữ nổi nhiều dòng. Mỗi dòng có thể khác nhau theo từng người xem (placeholder, điều kiện), có thể đổi khung theo thời gian, và billboard có thể click được. Chữ được gửi bằng packet riêng cho từng người chơi trong tầm nhìn.

File JSON mô tả **nội dung**. Billboard hiện trong world qua các [bản đặt](#bản-đặt).

## Cấu trúc file

```json
{
  "view_range": 32,
  "refresh": 10,
  "line_spacing": 0.08,
  "display": { "background": "#80000000", "line_width": 220 },
  "lines": [
    {
      "type": "animated",
      "interval": 10,
      "frames": ["<gold><bold>✦ RogueSMP ✦", "<yellow><bold>✦ RogueSMP ✦"],
      "display": { "scale": 1.6, "shadow": true }
    },
    "<gray>Xin chào, <white>%player_name%<gray>!",
    {
      "type": "text",
      "text": "<yellow>Gõ <white>/is create <yellow>để tạo đảo",
      "condition": { "type": "not", "requirement": { "type": "has_island" } }
    }
  ],
  "actions": [
    { "click": "right", "action": { "type": "open_gui", "gui": "main_menu" } }
  ]
}
```

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `lines` | mảng [dòng](#dòng-lines) | bắt buộc | Các dòng, từ trên xuống. |
| `view_range` | double | `48` | Khoảng cách (block) người chơi thấy billboard. Phải > 0. |
| `refresh` | int (tick) | `10` | Chu kỳ render lại chữ cho mỗi người xem. Phải ≥ 1. Packet chỉ gửi khi chữ thay đổi. |
| `line_spacing` | double | `0.05` | Khoảng trống thêm giữa 2 [đoạn](#đoạn-và-chiều-cao). |
| `display` | [style](#display--style) | trống | Style mặc định cho mọi dòng. |
| `hitbox` | `{ "width": float, "height": float }` | tự tính | Vùng click. Tự tính: rộng `2 × scale lớn nhất`, cao bằng cả billboard. |
| `actions` | mảng action | `[]` | Chạy khi người xem click. Định dạng: [JSON-Actions](JSON-Actions.md). |

### Click

Billboard click được khi có `actions` hoặc `hitbox`.

| Thao tác | `click` khớp |
| :--- | :--- |
| Click phải vào vùng click | `right`, `any` |
| Đánh (click trái) vào vùng click | `left`, `any` |

Mỗi người chơi click tối đa 1 lần / 5 tick.

## Dòng (`lines`)

Mỗi phần tử là 1 trong 2 dạng:

- **Chuỗi** — dòng chữ không style riêng, không điều kiện.
- **Object có `"type"`** — `text` hoặc `animated`.

### `text`

```json
{ "type": "text", "text": "<gold>Chào mừng", "display": { "scale": 1.5 }, "condition": { "type": "has_island" } }
```

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `text` | string MiniMessage | bắt buộc | Nội dung. Placeholder `%...%` được thay theo từng người xem. |
| `display` | [style](#display--style) | trống | Style riêng của dòng. |
| `condition` | [requirement](JSON-Trades.md#requirement-requirements) | không | Dòng chỉ hiện với người xem thoả điều kiện, kiểm tra mỗi `refresh` tick. |

Placeholder cần expansion PlaceholderAPI tương ứng, vd. `%player_name%` cần `/papi ecloud download Player`. Thiếu expansion thì placeholder hiện nguyên văn.

### `animated`

```json
{ "type": "animated", "interval": 10, "frames": ["<red>A", "<gold>B", { "type": "text", "text": "%player_name%" }] }
```

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `frames` | mảng dòng | bắt buộc, ≥ 1 | Các khung. Mỗi khung là 1 dòng bất kỳ. |
| `interval` | int (tick) | `20` | Thời gian mỗi khung. Phải ≥ 1. |
| `display` | [style](#display--style) | trống | Style của cả dòng. `display` bên trong từng khung không có tác dụng. |
| `condition` | [requirement](JSON-Trades.md#requirement-requirements) | không | Như `text`. |

⚠️ Chữ được gửi lại mỗi `refresh` tick. Để không bị nhảy khung, đặt `interval` ≥ `refresh`, tốt nhất là bội số của `refresh`.

## `display` — style

Dùng ở `display` của billboard và của từng dòng. Thứ tự lấy giá trị: **dòng → billboard → mặc định**.

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `billboard` | `center` / `fixed` / `vertical` / `horizontal` | `center` | Cách chữ xoay. `center`: luôn quay về người xem. `fixed`: giữ hướng đặt. |
| `scale` | float | `1` | Cỡ chữ. |
| `shadow` | bool | `false` | Đổ bóng chữ. |
| `see_through` | bool | `false` | Nhìn xuyên tường. |
| `background` | `#RRGGBB` / `#AARRGGBB` | nền vanilla (đen mờ) | Màu nền. `#00000000` = không nền; `#RRGGBB` = nền đặc. |
| `alignment` | `left` / `center` / `right` | `center` | Căn lề trong đoạn. |
| `line_width` | int | `200` | Độ rộng tối đa (pixel) trước khi tự xuống dòng. |

<a id="đoạn-và-chiều-cao"></a>
### Đoạn và chiều cao

- Các dòng **liền nhau** có style giống hệt nhau gộp thành 1 đoạn, chung 1 khung nền.
- Dòng khác style bắt đầu đoạn mới. Mỗi đoạn cao `số dòng × 0.25 × scale`, các đoạn cách nhau `line_spacing`.
- Billboard xếp từ **vị trí bản đặt lên trên**: dòng cuối cùng nằm tại bản đặt.
- Dòng bị ẩn do `condition` làm các dòng cùng đoạn khít lại; vị trí các đoạn giữ nguyên.

Muốn nền liền một khối, cho các dòng cùng style.

## Bản đặt

Mỗi bản đặt là 1 entity `Marker` vô hình mang id billboard, đặt bằng `/billboard place <id>` hoặc GUI.

- 1 billboard có bao nhiêu bản tuỳ ý, ở bất kỳ world nào. Mọi bản dùng chung nội dung.
- Bản đặt lưu trong chunk nên đi theo world: copy world mẫu và paste schematic phòng dungeon đều mang theo billboard.
- Bản đặt chỉ hiện khi chunk của nó đang được nạp.
- Bản đặt có id không tồn tại không hiện gì và log cảnh báo `Placed billboard at <vị trí> references unknown billboard '<id>'`. Marker được giữ nguyên; billboard có lại id đó (sửa file + reload, hoặc tạo lại) thì bản đặt hiện lại.

### Xoá billboard

Xoá billboard xoá file và mọi bản **đang nạp**. Bản trong chunk/world chưa nạp trở thành bản lạc, log cảnh báo như trên khi nạp. Xoá bản lạc bằng tay tại toạ độ trong log:

```
/kill @e[type=marker,distance=..2]
```

Lệnh này xoá mọi Marker trong 2 block, không chỉ Marker của billboard.

## Lệnh

Quyền: `roguesmp.billboard.admin`.

| Lệnh | Việc |
| :--- | :--- |
| `/billboard` | Mở trình duyệt. |
| `/billboard browser [tìm]` | Mở trình duyệt, lọc id chứa chuỗi tìm. |
| `/billboard create <id>` | Tạo billboard và đặt 1 bản tại chỗ đứng. Id: chữ thường, số, `_`, `-`, `/`, tối đa 64 ký tự. |
| `/billboard edit <id>` | Mở editor. |
| `/billboard delete <id>` | Xoá billboard (xem [Xoá billboard](#xoá-billboard)). |
| `/billboard place <id>` | Đặt 1 bản tại chỗ đứng. |
| `/billboard unplace` | Xoá bản gần nhất trong 4 block. |
| `/billboard nudge <x> <y> <z>` | Dịch bản gần nhất trong 4 block. |
| `/billboard near` | Liệt kê bản đặt trong 16 block. |
| `/billboard tp <id>` | Dịch chuyển tới bản đang nạp gần nhất, ưu tiên world hiện tại. |
| `/billboard list` | Liệt kê billboard và số bản đang nạp. |
| `/billboard addline <id> <chữ>` | Thêm dòng chữ. |
| `/billboard setline <id> <số dòng> <chữ>` | Thay dòng (đánh số từ 1). |
| `/billboard removeline <id> <số dòng>` | Xoá dòng. |
| `/billboard reload` | Nạp lại `billboards/`. |

Mọi thay đổi qua lệnh/GUI ghi file ngay và cập nhật mọi bản đang nạp. Lệnh, trình duyệt và danh sách bản đặt chỉ thấy các bản **đang nạp**.

## GUI

### Trình duyệt

| Nút | Thao tác |
| :--- | :--- |
| Phễu (lọc) | Click trái / phải: đổi bộ lọc — tất cả world, world hiện tại, từng world có bản đặt, chưa có bản nào nạp. |
| Name tag (tìm) | Click: nhập chuỗi tìm theo id. Click phải: xoá tìm. |
| Emerald | Tạo billboard. |
| Billboard | Click: mở editor. Shift + click: đặt 1 bản tại chỗ đứng. |

Mỗi billboard hiện số bản đang nạp và các world chứa bản.

### Editor

| Nút | Thao tác |
| :--- | :--- |
| Place here | Đặt 1 bản tại chỗ đứng. |
| Nudge X / Y / Z | Dịch bản gần nhất (trong 4 block). Click trái / phải: ±0.1, Shift: ±1. |
| Remove nearest copy | Shift + click phải: xoá bản gần nhất (trong 4 block). |
| Copies | Mở danh sách bản đặt. |
| View range / Refresh / Line spacing | Click trái / phải để tăng / giảm, Shift để bước lớn. |
| Display defaults | Mở màn hình [style](#màn-hình-style). |
| Edit whole billboard | Sửa toàn bộ bằng hộp JSON. |
| Add text line / Add animated line | Thêm dòng mẫu. |
| Dòng | Click: sửa / xoá dòng bằng hộp JSON. Shift + click trái / phải: chuyển lên / xuống. |
| Delete billboard | Shift + click phải. |

Hộp JSON dùng định dạng dễ dãi: không cần `{}` ngoài cùng, key và chuỗi đơn giản không cần ngoặc kép.

### Danh sách bản đặt

Mỗi bản hiện world, toạ độ, khoảng cách. Bản ở world hiện tại xếp trước, gần trước.

| Thao tác | Việc |
| :--- | :--- |
| Click | Dịch chuyển tới bản. |
| Shift + click phải | Xoá bản. |

<a id="màn-hình-style"></a>
### Màn hình style

Mỗi key của [`display`](#display--style) 1 nút. Nút xanh = đã đặt, xám = dùng mặc định.

| Key | Thao tác |
| :--- | :--- |
| `billboard`, `alignment` | Click trái / phải: giá trị kế / trước. |
| `scale` | ±0.1, Shift ±0.5. |
| `line_width` | ±10, Shift ±50. |
| `shadow`, `see_through` | Click: bật / tắt. |
| `background` | Click: đổi giữa các nền có sẵn. Màu tuỳ ý: nút Edit as JSON. |
| Mọi key | Q: bỏ đặt (dùng mặc định). |

## Dành cho dev

| File | Vai trò |
| :--- | :--- |
| [`billboard/Billboard`](../src/main/java/com/roguesmp/billboard/Billboard.java) | Định nghĩa + `CODEC`, registry `Registries.BILLBOARD` (`billboards/`) |
| [`billboard/line/BillboardLine`](../src/main/java/com/roguesmp/billboard/line/BillboardLine.java) | Interface dòng: `render(viewer, tick)`, `display()`, `condition()`; `BillboardLine.dynamic(...)` tạo dòng bằng code |
| [`billboard/line/BillboardLines`](../src/main/java/com/roguesmp/billboard/line/BillboardLines.java) | Đăng ký loại dòng vào `Registries.BILLBOARD_LINE_CODEC` |
| [`billboard/DisplaySettings`](../src/main/java/com/roguesmp/billboard/DisplaySettings.java) | Style + kế thừa (`merge`) |
| [`billboard/BillboardManager`](../src/main/java/com/roguesmp/billboard/BillboardManager.java) | Tạo / sửa / xoá + ghi file, bản đặt, billboard tạm bằng code (`spawnTemporary`), tick, click |
| [`billboard/BillboardInstance`](../src/main/java/com/roguesmp/billboard/BillboardInstance.java) | 1 billboard tại 1 vị trí: chia đoạn, theo dõi người xem, render theo người |
| [`billboard/PacketEntity`](../src/main/java/com/roguesmp/billboard/PacketEntity.java) | Entity NMS chỉ tồn tại qua packet spawn / data / remove |
| [`billboard/BillboardListener`](../src/main/java/com/roguesmp/billboard/BillboardListener.java) | Marker vào / ra world (xử lý ở tick sau), click qua `PlayerUseUnknownEntityEvent` |

Thêm loại dòng: implement `BillboardLine` (record có `CODEC`), đăng ký trong `BillboardLines`.
