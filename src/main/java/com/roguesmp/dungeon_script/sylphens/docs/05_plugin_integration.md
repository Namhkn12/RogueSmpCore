# 05 — Tích hợp plugin

Abx tự viết phần plugin. Tài liệu này mô tả những gì bản dựng mong đợi từ plugin, và dữ liệu có sẵn để plugin dùng.

## Quy tắc phá khối (đề xuất)
**Cấm phá**, đây là vỏ dungeon:
- `deepslate`, `tuff`
- mọi `*_bricks` trừ tường giả
- `deepslate_tiles`, `polished_*`, `chiseled_*`, `cobblestone` và `mossy_cobblestone` của mê lộ
- `bedrock`, `barrier`

**Cho phá:**
- Mặt đảo và Layer 1: `stone`, `dirt`, `grass_block`, `andesite`, gỗ, lá, quặng, `moss_block`, `dripstone_block`, `calcite`, `amethyst_block`…
- Khối trong phòng tài nguyên: quặng deepslate, `rooted_dirt`, `mud`, `clay`, `prismarine`, `bookshelf`, `mycelium`, `mushroom_stem`, `amethyst_block`…
- Mê cung (tầng 3): lớp đá phủ (`cobblestone`, `mossy_cobblestone`, `stone`) phá được cũng không sao, sau 1 khối là bedrock. Nên cấm để giữ cảnh quan.

Lưu ý:
- Mặt đảo và Layer 1 nằm ở Y > 60 nên là đá thường. Từ Y ≤ 60 trở xuống toàn bộ là deepslate.
- Vỏ phòng tài nguyên dày 2–3 khối khai thác được. Bên ngoài vỏ vẫn là deepslate, nên người chơi không đào thoát ra được.
- Kể cả khi plugin cho phá nhầm, tấm bedrock vẫn chặn không cho đào xuyên tầng.

Cách gọn nhất là dùng **danh sách trắng** (chỉ cho phá các khối liệt kê ở trên) cho vùng Y ≤ 60, và danh sách đen nhẹ (barrier, bedrock) cho vùng phía trên.

## Điểm xuất hiện khi vào dungeon
- `output/surface.json` → `spawn`: (0, 122, 80), yaw 180 (nhìn ra cửa hang về phía bắc). Hang nằm trong sườn núi nam, có đường gấp khúc xuống thung lũng.
- `river.source_pool`: chỗ đón thác (Abx tự làm). `river.end`: chỗ sông đổ vào hố.

## Boss và Thủ Vệ
- Phòng Thủ Vệ: tâm (0, −18, 0). Khối `lodestone` ở tâm sàn là **pad dịch chuyển xuống boss**.
- Phòng boss (hang vòm, `output/boss_cavern.json`):
  - `arrival` (0, -53, 55), yaw 180: điểm dịch chuyển đến trong hang nhỏ phía nam; cũng là điểm thoát sau khi thắng.
  - `boss_spawn` (0, -57, 0): tâm vòng triệu hồi trên sàn tròn.
  - `minion_spawns`: 8 điểm trên sàn tròn (r15) + 6 điểm trên nền vòng ngoài (r31).
  - `platform` (r24, mặt Y−58), `summon_circle` (r8), `pillars` (7 cột).
  - Phương án cũ `boss_room.json` (sảnh fantasy) không còn dùng.
- Điều kiện:
  - Pad chỉ kích hoạt khi Thủ Vệ của instance đã chết.
  - Boss chỉ spawn khi Thủ Vệ đã chết.
  - Trong phòng boss thì chỉ thoát được bằng cách thắng (plugin dịch chuyển ra) hoặc chết.

## Checkpoint và dịch chuyển thoát
- `lodestone` ở tâm mỗi Trạm Nghỉ (ở sàn+1): điểm checkpoint/respawn.
- `respawn_anchor` ở tâm Trạm Dịch Chuyển (tầng 2 và 4): thoát khỏi dungeon.

## Dữ liệu phòng: `output/layer2_rooms.json`
```json
{
  "seed": 2026,
  "floors": {"1": 46, "2": 30, "3": 14, "4": -2, "5": -18},
  "rooms": [
    {"name": "Phòng Canh 1", "kind": "combat", "floor": 1,
     "x": [20, 31], "z": [-33, -22], "y_floor": 46, "y_ceil": 55,
     "center": [26, -27], "shape": "rect", "resource": null, "in_maze": false}
  ],
  "maze_floor": 3,
  "maze": [{"floor": 3, "y_floor": 18,
            "traps": [{"type": "arrow", "x": -20, "y": 19, "z": -70}],
            "chests": [{"x": 54, "y": 19, "z": 70}]}]
}
```
- `kind` nhận các giá trị: `hall`, `guardian`, `combat`, `arena`, `barracks`, `chest`, `trap`, `res`, `checkpoint`, `teleport`, `cavern`, `stair`, `arrival`, `atrium`.
- `x`, `z` là hộp bao. Phòng `round`/`blob` thật sự nhỏ hơn hộp này.
- Không gian người chơi đứng được: Y từ `y_floor + 1` tới `y_ceil − 1`.
- Riêng `atrium`: `floor` là tầng có ban công, `y_floor` là sàn đáy (tầng dưới).
- `in_maze: true` đánh dấu phòng combat nằm trong mê cung tầng 3.
- `maze[].traps`: `type` là `pit` (hố chông, đã dựng sẵn), `arrow` (tấm áp suất ở `x,y,z` + 2 dispenser trong 2 vách ở y+1, chưa nối redstone và chưa có tên), `web` (mạng nhện).
- `maze[].chests`: rương ở ngõ cụt mê cung (chưa có loot).

Gợi ý dùng trong plugin:
| Loại phòng | Việc plugin làm |
|---|---|
| combat / arena / barracks | spawn đợt mob khi người chơi bước vào hộp phòng |
| chest | điền loot vào rương ở tâm phòng |
| trap | gắn bẫy (dispenser, tripwire…). Hố măng đá đã dựng sẵn |
| res | có thể tăng tỉ lệ rơi đồ |
| combat (`in_maze`) | spawn mob khi vào phòng; phòng nhỏ 13×13 hoặc 13×21 |
| bẫy mê cung `arrow` | khi người chơi giẫm tấm áp suất: bắn tên từ 2 dispenser (nạp tên hoặc tự spawn projectile) |

## Instance
- Template là world chứa đảo. Mỗi team được một bản sao (world copy, hoặc schematic paste vào lưới slot, hoặc định dạng nhẹ như Slime). Cách nhân bản chưa được chốt.
- Đảo nằm gọn trong barrier r ≤ 103, Y −64..319, nên mỗi slot cần ít nhất 208 × 208 khối.
- Cần điều kiện đóng instance: boss chết, cả team chết hoặc rời đi, hoặc hết giờ.
