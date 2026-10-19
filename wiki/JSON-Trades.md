# JSON Reference — Trades (cửa hàng NPC)

Thư mục: `trades/` (đệ quy, id = đường dẫn bỏ `.json`). Quy tắc chung: [JSON-Overview](JSON-Overview.md). Mỗi file là **1 cửa hàng**; NPC mở cửa hàng bằng action `open_trade` (xem [JSON-Npcs](JSON-Npcs.md#action)).

## Cấu trúc file

```json
{
  "title": "<dark_gray>Thợ Rèn",
  "layout": {
    "pattern": [
      "#########",
      "#O.O.O.O#",
      "#.O.O.O.#",
      "#O.O.O.O#",
      "#########",
      "##<#A#>##"
    ],
    "keys": { "#": "BLACK_STAINED_GLASS_PANE", "A": "ANVIL" },
    "fill": "GRAY_STAINED_GLASS_PANE"
  },
  "offers": [
    {
      "name": "<gold>Kiếm Thép",
      "costs": [
        { "type": "money", "amount": 500 },
        { "type": "item", "item_id": "steel_ingot", "amount": 3 }
      ],
      "requirements": [
        { "type": "level", "min": 10 },
        { "type": "quest_completed", "quest": "boss_hunter_1" }
      ],
      "results": [ { "type": "item", "item_id": "explosive_bow" } ]
    }
  ]
}
```

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | string | tự lấy từ đường dẫn | Id cửa hàng, dùng trong `open_trade`. Không cần viết. |
| `title` | string MiniMessage | `"<dark_gray>Cửa hàng"` | Tiêu đề GUI. |
| `layout` | object | bố cục mặc định (xem dưới) | Vị trí các ô trong GUI. |
| `offers` | mảng offer | **bắt buộc** | Danh sách giao dịch, xếp vào các ô `O` theo thứ tự. |

## Offer

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `name` | string MiniMessage | optional | Tên hiển thị của ô. Không có → dùng tên item nhận được đầu tiên. |
| `costs` | mảng cost | `[]` | Giá — **bị lấy đi** khi giao dịch. |
| `requirements` | mảng requirement | `[]` | Điều kiện — **chỉ kiểm tra, không lấy đi**. |
| `results` | mảng result | **bắt buộc** | Thứ người chơi nhận được. |

Icon của ô = item của result đầu tiên (nếu là `item`), nếu không thì là rương. Lore tự sinh 3 mục **Mua / Giá / Yêu cầu**, mỗi dòng có dấu ✔/✘ theo trạng thái hiện tại của người chơi.

### Khi click một offer

1. Kiểm tra mọi `requirements` và `costs`. Thiếu bất kỳ → báo *"Bạn không đủ điều kiện để giao dịch!"*, không mất gì.
2. Lấy item của `costs` và thêm item của `results` vào túi đồ (36 ô chính, **không** tính giáp/tay trái). Chỗ trống do cost giải phóng được tính.
3. Nếu không chứa đủ → túi đồ được **trả nguyên trạng**, báo *"Túi đồ không đủ chỗ trống!"*.
4. Thành công → mới trừ xu và chạy result không phải item (`money`, `command`).

⚠️ Hai cost cùng 1 item được kiểm tra **riêng rẽ**, không cộng dồn. Gộp thành 1 cost với tổng `amount`.

## Cost (`costs`)

Field chọn loại: `"type"`.

| `type` | Key | Ý nghĩa |
| :--- | :--- | :--- |
| `money` | `amount` (long, bắt buộc) | Trừ xu. |
| `item` | `item_id` (id item, bắt buộc), `amount` (int, mặc định `1`) | Lấy item tùy chỉnh khỏi túi đồ. |

## Requirement (`requirements`)

| `type` | Key | Ý nghĩa |
| :--- | :--- | :--- |
| `level` | `min` (int, bắt buộc) | Cấp người chơi ≥ `min`. |
| `quest_completed` | `quest` (id quest, bắt buộc) | Đã hoàn thành quest này. |
| `permission` | `permission` (string, bắt buộc), `display` (string MiniMessage, mặc định `"Không có quyền"`) | Có permission. `display` là dòng hiện trong lore. |
| `money` | `amount` (long, bắt buộc) | Đang **sở hữu** ít nhất `amount` xu (không bị trừ). |
| `item` | `item_id` (bắt buộc), `amount` (mặc định `1`) | Đang **sở hữu** item (không bị lấy). |

⚠️ `money`/`item` có ở cả `costs` lẫn `requirements` với cùng tên `type` — **mảng chứa nó** quyết định có bị lấy đi hay không.

## Result (`results`)

| `type` | Key | Ý nghĩa |
| :--- | :--- | :--- |
| `item` | `item_id` (bắt buộc), `amount` (mặc định `1`) | Cho item tùy chỉnh. Tính vào kiểm tra chỗ trống. |
| `money` | `amount` (long, bắt buộc) | Cho xu (dùng để làm offer **bán lại**). |
| `command` | `command` (string, bắt buộc), `display` (string MiniMessage, mặc định `""`) | Chạy lệnh bằng **console**, không có `/` đầu. `%player%` → tên, `%uuid%` → UUID. `display` là dòng hiện trong lore. |

## Layout

Vẽ GUI bằng lưới ký tự, giống `pattern` của công thức shaped.

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `pattern` | mảng string | **bắt buộc** | 1–6 dòng, **mỗi dòng đúng 9 ký tự**. Số dòng = số hàng GUI. |
| `keys` | map ký tự → `Material` | `{}` | Ô trang trí. Key phải đúng 1 ký tự. |
| `fill` | `Material` | `LIGHT_GRAY_STAINED_GLASS_PANE` | Mọi ô còn lại. |

| Ký tự | Ý nghĩa |
| :--- | :--- |
| `O` | Ô offer. Điền theo thứ tự trái → phải, trên → dưới. Số ô `O` = số offer mỗi trang. |
| `<` / `>` | Nút trang trước / sau. Chỉ hiện khi có trang tương ứng; nếu không thì là `fill`. |
| ký tự trong `keys` | Trang trí bằng material đó. |
| ký tự khác (vd. `.`) | `fill`. Ô `O` không có offer cũng là `fill`. |

GUI không có ô trống (air): mọi ô đều có pane, và người chơi không thể shift-click hay kéo item vào cửa hàng.

Bỏ `layout` → dùng bố cục mặc định: viền kính đen, 28 ô offer (4 hàng × 7), nút trang ở hàng dưới cùng.

```json
"pattern": [
  "#########",
  "#OOOOOOO#",
  "#OOOOOOO#",
  "#OOOOOOO#",
  "#OOOOOOO#",
  "###<#>###"
],
"keys": { "#": "BLACK_STAINED_GLASS_PANE" }
```

## Lỗi thường gặp

Báo trong log lúc khởi động, cửa hàng đó không được load:
- `item_id` / `quest` / `shop` sai id.
- `pattern` rỗng hoặc hơn 6 dòng, dòng không đủ 9 ký tự, key trong `keys` dài hơn 1 ký tự, không có ô `O` nào.

## Dành cho dev

Code nằm trong [`trade/`](../src/main/java/com/roguesmp/trade). Thêm loại mới: implement interface, khai báo `TYPE_KEY` + `CODEC`, đăng ký 1 dòng trong `TradeCosts` / `TradeRequirements` / `TradeResults`.

- **`TradeCost`**: `canAfford`, `getDisplay`; `takeFrom(Inventory)` cho phần **item** (được hoàn tác khi túi đầy), `take(player)` cho phần còn lại (chỉ chạy khi thành công).
- **`TradeRequirement`**: `test`, `getDisplay`.
- **`TradeResult`**: `getDisplay`; `createItems(player)` cho phần **item** (tính vào kiểm tra chỗ trống), `give(player)` cho phần còn lại; `getIcon` tùy chọn.

Luồng giao dịch: `TradeOffer.trade` (snapshot túi đồ → `takeFrom` → `addItem` → hoàn tác nếu dư → `take`/`give`). GUI: [`TradeGui`](../src/main/java/com/roguesmp/trade/gui/TradeGui.java); bố cục: [`TradeLayout`](../src/main/java/com/roguesmp/trade/TradeLayout.java).

---
◀ [JSON-Npcs](JSON-Npcs.md) · Về [Trang chủ](Home.md) · Tiếp theo: [JSON-Loot](JSON-Loot.md)
