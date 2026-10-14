# JSON Reference — Spawners

Spawner tuỳ chỉnh là **spawner vanilla được cấu hình lại**: chọn mob tuỳ chỉnh nào sinh ra, tốc độ, số lượng, và gắn thêm **behavior** (phải phá nhiều lần, chạy trốn, nguyền người chơi...). Hệ này độc lập với dungeon — dùng được ở bất kỳ đâu có bật flag.

Thư mục: `spawners/`, mỗi spawner 1 file `spawner<id>.json` (vd. `spawners/spawnerskel_nest.json`). Nạp bằng Gson lúc khởi động, **không** reload được bằng `/smpreload`.

## Bật spawner bằng WorldGuard

Spawner tuỳ chỉnh **chỉ hoạt động trong region có flag `smp-spawners allow`** (mặc định `deny`):

```
/rg flag <region> smp-spawners allow
```

Trong region bị `deny`, spawner tuỳ chỉnh không sinh mob và behavior không chạy; spawner vanilla bình thường không bị ảnh hưởng. Với dungeon, world instance phải có region bật flag này; đơn giản nhất là đặt trên `__global__` của template world (cần kiểm tra region có được sao chép sang instance không).

## Đặt spawner

1. `/spawner give <id>` (quyền `roguesmp.spawner.admin`) → nhận item spawner gắn id.
2. Đặt xuống trong template world hoặc schematic phòng. Plugin để lại 1 marker vô hình ghi id.
3. Khi chunk được nạp **trong region bật flag**, marker biến block thành spawner đã cấu hình. Vì vậy spawner trong schematic phòng dungeon tự kích hoạt khi phòng được dán.

Phòng dungeon sạch → mọi spawner tuỳ chỉnh trong phòng bị tắt.

## File spawner

```json
{
  "id": "skel_nest",
  "name": "Ổ xương",
  "mobs": { "crypt/skeleton": 3, "crypt/archer": 1 },
  "delay": 20, "minDelay": 100, "maxDelay": 200,
  "activeRange": 16, "spawnRange": 4,
  "maxNearBy": 6, "spawnCount": 2,
  "behaviors": [ { "type": "wave", "params": { "waves": 3 } } ]
}
```

Key viết **camelCase** (khác dungeon).

| Key | Ý nghĩa |
| :--- | :--- |
| `id` | Id spawner, dùng trong `/spawner give`. |
| `name` | Tên item spawner (thiếu thì dùng `id`). |
| `mobs` | **Bắt buộc.** Id entity (trong `entities/`) → **trọng số** chọn, không phải số lượng. Ví dụ trên: skeleton 3/4, archer 1/4. |
| `delay` | Số tick tới lần sinh đầu tiên. |
| `minDelay`, `maxDelay` | Khoảng tick ngẫu nhiên giữa các lần sinh. |
| `activeRange` | Người chơi phải ở trong bán kính này (block) thì spawner mới chạy. |
| `spawnRange` | Bán kính quanh spawner nơi mob xuất hiện. |
| `maxNearBy` | Không sinh thêm nếu xung quanh đã có chừng này mob. |
| `spawnCount` | Số mob thử sinh mỗi lần. |
| `behaviors` | **Bắt buộc có key**; không cần behavior thì để `[]`. |

## Behavior

Mỗi behavior: `{ "type": ..., "params": { ... } }`. "Người chơi gần" = trong 20 block.

| `type` | `params` (mặc định) | Tác dụng |
| :--- | :--- | :--- |
| `protector` | `requireBreak` (3) | Phải phá N lần mới vỡ; mỗi lần trước báo "còn N lần". |
| `wave` | `waves` (3) | Không phá được cho tới khi đã sinh đủ `waves` đợt. Mỗi đợt sinh thêm mob theo số người chơi gần. |
| `teleport` | `max_teleports` (3), `teleport_radius` (20), `safe_check_radius` (3) | Bị phá thì dịch sang chỗ an toàn ngẫu nhiên cách 10–`teleport_radius` block, tới khi hết lượt mới vỡ. Khi sinh: thay bằng 2–4 mob ngay cạnh mỗi người chơi gần. Lưu ý key **snake_case**. |
| `cursed` | — | Mỗi lần sinh, mỗi người chơi gần nhận 2 lời nguyền ngẫu nhiên khác nhau: buồn nôn, mù, độc, mạng nhện dưới chân, cháy. |

Nhiều behavior trên 1 spawner được kết hợp: 1 lần phá hoặc sinh bị huỷ nếu **bất kỳ** behavior nào từ chối (vd. `protector` + `wave` = phải qua đủ đợt **và** phá đủ lần).

---
◀ [JSON-Dungeons](JSON-Dungeons.md) · Về [Trang chủ](Home.md)
