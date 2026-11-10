# JSON Reference — Infusions

Thư mục: `infusions/` (id = đường dẫn bỏ `.json`, **không có** field `"id"`). Quy tắc chung: [JSON-Overview](JSON-Overview.md).

Infusion (Ấn Ký) là enchant người chơi tự nâng trên trang bị qua `InfusionGui` (mở từ thợ rèn). Có 2 loại: **Cơ Bản** (`basic`, tăng chỉ số) và **Cao Cấp** (`advanced`, enchant có hiệu ứng hành vi qua các hook của `SmpEnchant`). Mỗi trang bị có tối đa **1 Cơ Bản + 1 Cao Cấp**, nâng/hạ độc lập. GUI chia 2 tab theo loại. Chỉ cấp do người chơi thêm (lưu trong PDC) mới tính là infusion; enchant có sẵn trong định nghĩa item thì không.

## Cấu trúc file

`infusions/vigor.json`:

```json
{
  "enchant": "vigor",
  "type": "basic",
  "order": 0,
  "levels": [
    { "costs": [ { "type": "exp", "level": 30 }, { "type": "item", "item": "copper_ingot_currency", "amount": 5 } ], "success_chance": 1.0 },
    { "costs": [ { "type": "exp", "level": 60 }, { "type": "item", "item": "copper_ingot_currency", "amount": 10 } ], "success_chance": 0.9 }
  ]
}
```

| Key | Kiểu | Bắt buộc / mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `enchant` | id enchant | **bắt buộc** | Enchant được nâng (id trong `Enchants`, vd. `vigor`, `focus`). Sai id → file lỗi khi nạp. |
| `type` | `basic` / `advanced` | `basic` | Loại infusion, quyết định ô (tab) trên trang bị. |
| `levels` | mảng [cấp](#cấp-levels) | **bắt buộc** | Phần tử thứ N là cấp N. Số phần tử = cấp tối đa. |
| `order` | int | `0` | Thứ tự trong GUI, nhỏ đứng trước. Bằng nhau thì xếp theo id. |

## Cấp (`levels`)

| Key | Kiểu | Mặc định | Ý nghĩa |
| :--- | :--- | :--- | :--- |
| `costs` | mảng cost | `[]` | Chi phí mỗi lần **thử** nâng lên cấp này. Dùng chung loại cost với trade/island: `money`, `item`, `exp`. |
| `success_chance` | double `0`–`1` | `1.0` | Tỉ lệ thành công. Thất bại **mất toàn bộ chi phí**, cấp giữ nguyên. |

## Hạ cấp

Hạ 1 cấp hoàn trả **100%** `costs` của cấp đang có (theo file hiện tại, không phải giá lúc mua). Chi phí của các lần thử thất bại không được hoàn. Nhờ vậy người chơi đổi build không bị lỗ, chỉ chịu rủi ro lúc nâng.

⚠️ Đổi `costs` khi người chơi đã nâng sẽ thay đổi số tiền họ được hoàn. Giảm số cấp trong file thì các item đang vượt cấp vẫn giữ cấp cũ, và hạ cấp sẽ hoàn theo cấp cuối cùng trong file.

Reload: `infusions` (xem [JSON-Overview](JSON-Overview.md)).
