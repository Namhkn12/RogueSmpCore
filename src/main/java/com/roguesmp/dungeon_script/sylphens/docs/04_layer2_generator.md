# 04 — Bộ sinh Layer 2 (`scripts/generate_layer2.py`)

Toàn bộ Layer 2 nằm trong một mảng numpy `W[x, y, z]` gồm 177×82×177 khối (uint16, mỗi giá trị là id trong bảng `PAL`). Bộ sinh khởi tạo mảng toàn deepslate, đục và đặt phòng vào, rồi ghi ra file `.schem` (Sponge v2, DataVersion 3953). Thời gian chạy khoảng 20 giây. Cùng một seed luôn cho ra cùng một kết quả.

## Các bước (theo thứ tự trong file)
| Bước | Chỗ trong code | Làm gì |
|---|---|---|
| 0 | `F`, `RMAX`, `HMAX`, `MAT`, `RES`, `RES_BY_FLOOR` | hằng số và vật liệu |
| 1 | vòng `for fl in range(1, 6):` | **đặt phòng** cho từng tầng thường (xem chi tiết bên dưới); tầng `MAZE_FL` gọi `plan_maze` |
| 2 | `class FloorNet`, `link_rooms`, `astar` | nối phòng bằng hành lang trên lưới thô `CELL=4` |
| 3 | `plan_maze` | tầng mê cung: lưới ô `MZ_P=8`, chân thang + thang xuống + phòng combat thành nút, DFS + vòng lặp, chọn bẫy và rương |
| 4 | `# 1) vỏ` | đổ vỏ tường theo vật liệu từng loại phòng và hành lang |
| 5 | `# 2) khí` | đục khí cho phòng và hành lang. Đường hang có trần gợn và vách loang |
| 5b | `# 2b) build_maze` | đổ khối bedrock F−4..F+9 rồi khoét đường, phòng, hố chông, đặt tấm áp suất/dispenser/mạng nhện, rương |
| 6 | `# 3) thang xoắn` | thang xoắn ốc: 6 khối mỗi vòng, cột tâm `chiseled_deepslate` |
| 7 | `# 4) atrium` | giếng trời: ban công, lan can có 4 chỗ hở, hồ đáy sâu 3, xích và đèn |
| 8 | `# 5) nội thất` | nội thất theo loại phòng (cột, rương, hố bẫy, hồ, kệ sách, nấm, pha lê, đá quặng…) |
| 9 | `# 6) rương`, `# 6b)`, `# 6c)` | rương ở ~40% ngõ cụt; vòm cửa phòng (`ARCH`); vòm sườn hành lang (`RIB_EVERY`) |
| 10 | `# 6d) đuốc`, `# 7) đèn trần` | đuốc tường ở hành lang/phòng nhỏ/mê cung (`place_torches`); đèn trần lưới 6 chỉ cho phòng lớn `CEIL_LIT` |
| 11 | `# 7b) seal_floors()`, `face_maze` | tấm bedrock ngăn tầng (xem bên dưới); sau đó phủ 1 lớp `MZ_MAT` lên mọi mặt bedrock mê cung lộ ra |
| 12 | `# 8) safe` | ô ngoài vùng an toàn chuyển thành `structure_void` |
| 13 | `__main__` | kiểm tra thông đường, ghi report/json/md/schem |

## Đặt phòng mỗi tầng (bước 1)
Thứ tự đặt quan trọng: phòng đặt trước sẽ giữ được vị trí tốt hơn.

1. **Phòng cố định**
   - Tầng 1: Sảnh Chính tròn ở tâm.
   - Tầng 5: Thủ Vệ Rễ tròn ở tâm.
   - Từ tầng 2 trở đi: các "chân thang" và đáy giếng trời từ tầng trên trở thành lối vào của tầng.
2. **Thang A, B**: không giấu; mỗi thang chọn chỗ xa lối vào nhất và xa thang kia (`try_place(..., score=...)`).
3. **Giếng trời** (xác suất 85%, không nối vào/ra tầng mê cung, nên hiện chỉ T1→T2 hoặc T4→T5): chiếm chỗ trên cả 2 tầng.
5. Theo thứ tự: phòng tài nguyên (`RES_BY_FLOOR`), checkpoint (tầng 2–5), dịch chuyển thoát (tầng 2 và 4), 1–2 hang đá.
6. **Phòng thử thách**: `want` phòng mỗi tầng, chọn theo trọng số 5:3:3 giữa `templates` Phòng Canh / Đấu Trường / Hầm Lính. Thêm 3–4 hầm rương và 2 hành lang bẫy.

Mọi phòng đều qua `free_for()`, nghĩa là không đè lên phòng khác (có lề `margin`) và nằm trong `RMAX`. Phòng dùng chung chỗ trên nhiều tầng (thang, giếng trời) chiếm chỗ ở tất cả các tầng đó.

## Nối hành lang (bước 2–3)
- Lưới thô: mỗi ô 4×4 khối, khí 3×3. Ô bị chặn nếu đè phòng (lề 2) hoặc nằm ngoài `RMAX − 3`.
- **Cây khung nhỏ nhất** (MST) giữa các phòng, rồi mỗi phòng có 35% cơ hội thêm một cạnh tới phòng gần thứ 3–4. Các cạnh thừa này tạo vòng lặp.
- A* đi trên lưới có chi phí nhiễu (`cost = 1 + 2.5·noise`), nên hành lang ngoằn ngoèo. Đi trên ô đã có hành lang thì rẻ (0.35), nên các đường hay nhập vào nhau.
- 25% cạnh là **đường hang** (kiểu `cave`).
- **Vá liên thông**: union-find. Cụm nào còn rời thì nối vào cụm chính bằng cặp phòng gần nhất nối được.
- Sảnh Chính bị ép đủ 4 cửa theo 4 hướng.
- Thang và Thủ Vệ nối vào mạng như mọi phòng khác (MST).
- Cuối cùng thêm 5–16 **ngõ cụt** (3–9 ô) rẽ từ hành lang, cho cảm giác mê cung.

`maxdoors` giới hạn số cửa mỗi loại phòng: rương 1, thang 1, Thủ Vệ 2, Sảnh 4, mặc định 3.

## Tầng mê cung (`plan_maze`, `build_maze`, `face_maze`)
- Lưới ô gốc `MGX0 = −84`, bước `MZ_P = 8`. Ô hợp lệ khi cả lõi 5×5 nằm trong `RMAX[3] − 1`.
- **Nút**: mỗi ô thường là 1 nút. Chân thang từ T2 thành phòng chữ nhật trùm hộp 9×9 (`mz_span`). 2 thang xuống là khối 2×2 ô, chọn xa lối vào và xa nhau. 7 phòng combat 2×2 hoặc 2×3 ô, cách nhau ≥ 1 ô.
- **DFS** trên đồ thị nút cho cây khung (mọi nút tới được), thêm `MZ_LOOP` cạnh tạo vòng lặp. Mỗi cạnh có độ rộng 3/4/5.
- **Hình học**: đường đi `mz_strip` luôn nằm trong lõi ô, nên tường giữa 2 ô không nối luôn dày ≥ 3. Sau khi `face_maze` phủ 1 lớp đá mỗi bên, lõi bedrock vẫn còn ≥ 1. `check_maze_walls.py` kiểm tra điều này.
- **Bẫy** đặt ở đoạn xuyên tường (`mz_gap`) giữa 2 ô thường: hố chông cần đường rộng ≥ 4 để chừa gờ 1 khối.
- **Rương**: `MZ_CHESTS` ô cụt (bậc 1) xa lối vào nhất.

## Tấm bedrock (`seal_floors`, bước 11)
- Mỗi cặp tầng k|k+1 có một tấm ở `Y = F[k] − 2`. Mọi ô **không phải khí** tại Y đó đổi thành bedrock.
- Lỗ cho phép: `foot(2)` của thang và giếng trời đi từ tầng k xuống.
- Ô khí khác chạm tấm (hồ, hố bẫy): BFS vùng trũng đó. Lớp trong giữ vật liệu cũ, lớp ngoài thành bedrock. Nếu lớp trong giáp một khoảng trống khác (ví dụ trần phòng dưới) thì chính nó cũng thành bedrock.
- `SEAL_REPORT` ghi số ô đã bọc. Dòng bắt đầu bằng `!!` nghĩa là vùng trũng sâu quá giới hạn, cần sửa nội thất.

## Thông số hay chỉnh
| Muốn | Sửa |
|---|---|
| Bố cục khác hoàn toàn | chạy với seed khác: `python3 scripts/generate_layer2.py 777` |
| Thêm hoặc bớt phòng thử thách | `want = {1: 13, 2: 12, 3: 0, 4: 11, 5: 8}[fl]` (tầng mê cung = 0) |
| Tỉ lệ loại phòng thử thách | `rng.choices(templates, weights=[5, 3, 3])` |
| Thêm loại phòng thử thách mới | thêm tuple vào `templates`, thêm vật liệu vào `MAT`, nội thất ở `# 5)`, màu và nhãn ở `render_layer2_maps.py` |
| Phòng tài nguyên mỗi tầng | `RES_BY_FLOOR`; vật liệu, độ dày vỏ, chiều cao ở `RES` |
| Tầng nào là mê cung | `MAZE_FL` (+ `F`, `MAZE_FL` trong `generate_world_commands.py`; kiểm tra khối bedrock F−4..F+9 không chạm phòng tầng trên/dưới) |
| Đường mê cung rộng/hẹp hơn | `MZ_WIDTH` (tối đa 5 = `MZ_CORE`) |
| Số phòng combat / bẫy / rương trong mê cung | `MZ_ROOMS`, `MZ_TRAPS`, `MZ_CHESTS` |
| Mê cung ít ngõ cụt hơn | tăng `MZ_LOOP` |
| Mật độ đuốc | `TORCH_GAP` |
| Phòng nào giữ đèn trần | `CEIL_LIT` |
| Vật liệu vòm cửa | `ARCH`, `ARCH_KIND` |
| Nhiều vòng lặp hơn | `if rng.random() < 0.35:` |
| Nhiều đường hang hơn | `"cave" if rng.random() < 0.25` |
| Nhiều ngõ cụt hơn | `range({1: 14, 2: 16, 3: 0, 4: 14, 5: 8}[fl])` |
| Có hoặc không giếng trời | `if MAZE_FL not in (fl, fl + 1) and rng.random() < 0.85` |
| Độ cao tầng | `F` (+ `HMAX`, + `F` trong `generate_world_commands.py`, + `BOSS` nếu cần) |

Sau khi sửa, luôn chạy đủ quy trình kiểm tra trong `CLAUDE.md`.

## Định dạng `.schem`
- Viết tay NBT (hàm `write_schem`, không cần thư viện): Sponge Schematic v2.
- `Offset = [−88, −21, −88]`, `BlockData` mã hóa varint, thứ tự `x + z·W + y·W·L`.
- `BlockEntities` rỗng: rương được đặt nhưng chưa có loot. Plugin tự điền loot.
- Kiểm tra lại file: `nbtlib.load(path)` (xem `requirements.txt`).
