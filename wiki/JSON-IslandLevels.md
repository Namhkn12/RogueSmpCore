# JSON Reference — Island Levels (cấp đảo)

Thư mục: `island_levels/`. Quy tắc chung: [JSON-Overview](JSON-Overview.md). Mỗi file là **1 cấp**, **tên file là số cấp** (vd. `3.json` = cấp 3). File có tên không phải số bị bỏ qua.

- Đảo mới bắt đầu ở cấp `0`. **Luôn cần `0.json`**: thiếu nó thì đảo cấp 0 dùng giá trị mặc định, tức sức chứa khối **không giới hạn**.
- Cấp không có file dùng giá trị của **cấp có file gần nhất bên dưới**.
- Nâng cấp luôn đưa đảo lên **cấp có file kế tiếp**, nên cấp không có file sẽ bị bỏ qua.

## Cấu trúc file

```json
{
  "block_capacity": 6,
  "max_energy_networks": 1,
  "energy_network_capacity": 5000,
  "requirements": [],
  "costs": [
    { "type": "item", "item": "minecraft:gravel", "amount": 256 },
    { "type": "item", "item": "minecraft:iron_nugget", "amount": 64 },
    { "type": "item", "item": "copper_compressed_ingot", "amount": 16 },
    { "type": "money", "amount": 500 }
  ]
}
```

Item vanilla viết `minecraft:<tên>`, item tuỳ chỉnh viết id trong `items/` (xem [JSON-Overview](JSON-Overview.md)).

| Key | Mặc định | Ý nghĩa |
| :--- | :--- | :--- |
| `block_capacity` | `-1` (không giới hạn) | Tổng sức chứa khối custom của đảo. Mỗi khối tốn `capacity_cost` (xem [JSON-Blocks](JSON-Blocks.md)). Đặt khối vượt sức chứa sẽ bị chặn. |
| `max_energy_networks` | `1` | Số mạng năng lượng tối đa. |
| `energy_network_capacity` | `10000` | Dung lượng mỗi mạng năng lượng. |
| `requirements` | `[]` | Requirement để **nâng lên** cấp này. ⚠️ Chỉ kiểm tra **người bấm nâng cấp**, không phải cả đảo. Đảo co-op nên tránh requirement cá nhân (cấp Mạo hiểm, quest...). |
| `costs` | `[]` | Chi phí để **nâng lên** cấp này. `item` và `money` được [góp dần](#đóng-góp), `exp` trả 1 lần khi [nâng cấp](#nâng-cấp). |

## GUI

Mở từ nút **Cấp Độ Đảo** trong menu đảo. Tooltip của nút đó hiện cấp hiện tại và chỉ số hiện tại (sức chứa đã dùng/tổng, mạng năng lượng).

Màn hình chính có 2 ô:
- **Cấp hiện tại:** chỉ số của cấp đang có.
- **Cấp tiếp theo:** vừa là thông tin vừa là nút nâng cấp. Tooltip hiện:
  - mở khoá gì, dạng `cũ → mới`,
  - tiến độ góp (% chung = trung bình tiến độ từng mục) và từng mục,
  - chi phí trả khi nâng cấp (`exp`) và requirement,
  - trạng thái. Ô phát sáng khi đã có thể nâng cấp.

Click vào **cấp tiếp theo**:
- **chưa góp đủ** → mở màn hình [Đóng góp](#đóng-góp),
- **đã góp đủ** và đủ điều kiện → mở GUI [xác nhận nâng cấp](#nâng-cấp),
- trường hợp khác → báo lỗi.

## Đóng góp

Cost `item` và `money` **không** bị lấy 1 lần khi nâng cấp. Thành viên đảo **góp dần** trong màn hình Đóng góp:

- Mỗi mục là 1 ô, 21 ô mỗi trang, có phân trang. Ô tiền (nếu có) nằm đầu tiên. Ô đã đủ hiện kính xanh lá ✔. Đầu màn hình hiện % tiến độ chung. Nút quay lại về màn hình chính.
- **Ô item:**
  - **Click:** góp tối đa 64.
  - **Shift + Click:** góp tất cả trong túi.
- **Ô tiền:**
  - **Click:** mở hộp thoại nhập số tiền (dấu `,` `.` được bỏ qua).
  - **Shift + Click:** góp toàn bộ số dư.
- Luôn chỉ lấy **đúng phần còn thiếu**.
- Mọi thành viên đều góp được, bao nhiêu lần cũng được. Tiến độ lưu theo đảo: `levelContributions` (item) và `moneyContribution` (tiền) trong dữ liệu đảo.
- Nhiều cost `item` cùng 1 item, hoặc nhiều cost `money`, được cộng thành 1 mục tiêu.
- Đồ và tiền đã góp **không hoàn lại**, kể cả khi người góp rời đảo.

⚠️ Đổi `costs` của 1 cấp khi đảo đang góp dở: tiến độ được giữ theo **id item**. Item bị bỏ khỏi file thì phần đã góp không còn tác dụng. Giảm `amount` thì phần góp dư mất khi lên cấp.

## Nâng cấp

GUI xác nhận liệt kê chi phí sẽ trả (`exp`). Bấm **XÁC NHẬN**:
1. Kiểm tra lại requirement, tiến độ góp và chi phí.
2. Người bấm trả cost `exp`.
3. Đảo lên cấp, **tiến độ góp bị xoá**.
4. Mọi thành viên đang online nhận thông báo có khung, so sánh chỉ số cũ → mới (chỉ số thay đổi được tô xanh), kèm âm thanh.
5. Objective quest `island_level` của các thành viên được kiểm tra lại.

Lệnh admin đổi cấp đảo cũng xoá tiến độ góp.

### Nhiều thành viên cùng lúc

- Mọi thao tác góp và nâng cấp mang theo **cấp mà màn hình đang hiện**. Nếu đảo đã đổi cấp (thành viên khác vừa nâng cấp), thao tác bị từ chối, không lấy đồ hay tiền. Người bấm xác nhận nhận thông báo *"Đảo đã được nâng cấp bởi thành viên khác."*
- Sau mỗi lần góp, nâng cấp hoặc admin đổi cấp, GUI cấp đảo của **mọi người đang xem đảo đó** được vẽ lại. Ai đang ở GUI xác nhận đã lỗi thời sẽ được đưa về màn hình chính.

## Quest liên quan

Objective `island_level` hoàn thành khi đảo đạt cấp yêu cầu, tính cho mọi thành viên. Xem [JSON-Quests](JSON-Quests.md#objective).

---
◀ [JSON-Infusions](JSON-Infusions.md) · Về [Trang chủ](Home.md)
