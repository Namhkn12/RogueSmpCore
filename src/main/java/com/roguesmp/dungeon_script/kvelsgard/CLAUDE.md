# CLAUDE.md — Dungeon Kvelsgard

Quy ước chung cho mọi dungeon (người dùng, ngôn ngữ, bẫy FAWE) nằm ở `../CLAUDE.md`. Đọc file đó, rồi file này, rồi `docs/` theo nhu cầu.
Mọi lệnh dưới đây chạy từ thư mục `kvelsgard/`.

## Dự án là gì
Dungeon mỏ quặng bỏ hoang trong núi, đảo nổi trong void world, tâm (0, 0), mép đảo r 104–117. Người chơi được phá khối (chủ đề khai thác).
- Layer 1: thềm xuất hiện Y98 ở mép nam → đường leo chữ chi 4 nhánh → bãi mỏ Y120 → hang dẫn 5 khúc (Y120→106) → hố r12 có gờ xoắn xuống 34 khối.
- Layer 2 Hầm Mỏ Cũ (sàn Y72), Layer 3 Rừng Thạch Nhũ (sàn Y40), Layer 4 Vực Deepslate (sàn Y8): mỗi tầng 6–7 phòng, hành lang, một hang khai thác lớn (cao 26).
- Tấm bedrock ngăn tầng (Y≤101 / 69 / 37 / 3) + vách bedrock đứng sát mép đảo: không đào tắt sang tầng khác hay ra ngoài đảo. Chỉ hở ở 3 lối xuống.
- Layer 5 phòng boss "Cổ Thành Câm Lặng" (`scripts/boss_room.py`): bản 2 theo ảnh mẫu Ancient City: hang deep dark R60 nền Y-44 vòm Y-2 (Y-14 sát vách), cổng 41×21×5 + bệ 3 bậc dựng đúng bản vẽ từng block của artifact, sculk phủ nền, bọc vỏ bedrock riêng; chỉ vào bằng teleport (plugin) tới bệ xuất hiện (0, -40, 50).
- Bố cục gốc: artifact "Kvelsgard – bản thiết kế" (https://claude.ai/artifact/QprVqxGdLMNagJ8iAjPDkz), đã port vào `scripts/kv_model.py`.

Xem `images/sections.png` + `images/details.png` để nắm toàn cảnh.

## Cấu trúc thư mục
```
scripts/
  kv_model.py                 bố cục: cao độ, mép đảo, mặt núi, đường leo, hang dẫn, hố, phòng/hành lang/hang 3 tầng
  boss_room.py                Layer 5 phòng boss (gọi từ generate_world.py)
  generate_world.py           -> output/kvelsgard_l1..l5.schem + layout.json + report.txt + images/ (~25s)
  render_maps.py              ảnh kiểm tra (gọi từ generate_world.py)
  generate_world_commands.py  -> commands/*.txt (barrier, dán schem, biome)
commands/  output/  images/  docs/
```

## Lệnh thường dùng
```bash
PY=C:/Users/Admin/AppData/Local/Programs/Python/Python312/python.exe   # máy dev: xem memory no-python-on-dev-machine
PYTHONIOENCODING=utf-8 $PY scripts/generate_world.py
PYTHONIOENCODING=utf-8 $PY scripts/generate_world_commands.py
PYTHONIOENCODING=utf-8 $PY ../tools/schem_view.py output/kvelsgard_l3.schem images/l3_view.png --cut 42
```

## Bất biến KHÔNG được phá
1. **Cao độ chỉ khai báo ở `kv_model.py`** (`F`, `SEAL`, `SEAL1_MAX`, `CAV_H`, `YARD_Y`, `TERRACE_Y`, `TUNNEL_END_Y`). "floor" = Y của **khối sàn**, người đứng floor+1. 5 schem cắt đúng theo tấm: l5 `Y-60..2`, l4 `3..36`, l3 `37..68`, l2 `69..101`, l1 `102..đỉnh`.
2. **Nội dung tầng cách tấm bedrock ≥ 2 khối**: tầng 2 khí ≥ Y73 và ≤ Y98; tầng 3 khí ≤ Y66, nước/sàn ≥ Y39; tầng 4 khí ≤ Y34, dung nham/hồ ≥ Y5; phòng boss khí ≤ Y-2, đáy bể/hố mộ điện mộ ≥ Y-47 (tấm đáy Y-52), mọi khoảng trống trong r ≤ 64 (vách trụ bedrock r 66.5–68.2, `BOSS["seal_r"]`). Tấm chỉ đặt ở ô không kề ô trống, nên vi phạm sẽ hiện thành **lỗ tấm** trong `report.txt`.
3. `output/report.txt` phải có: `lộ ra ô trống: 0`, **đúng 3 lỗ tấm** (L1 quanh hố x-13..13 z-75..-49, Y69 quanh giếng, Y37 quanh hố sụt), `TẤT CẢ KHU THÔNG: True` (phòng boss kiểm riêng, đi bộ từ bệ xuất hiện).
4. Tấm dưới Layer 1 là mặt bậc thang `min(101, mặt đất thấp nhất trong ô 5x5 - 3)` + màn đứng nối các bậc. Bỏ min-filter thì tấm lộ ở vách hõm đường leo.
5. Tính tất định: chỉ `random.Random(SEED)` / `np.random.default_rng(SEED)` và noise trong `kv_model`; không dùng `hash()`.
6. Mọi lệnh trong `commands/` ≤ 256 ký tự (`check()` tự kiểm).
7. Không dùng bedrock ở chỗ người chơi nhìn thấy; vách đứng nằm trong mép đảo 1.6–3 khối.
8. **Không đặt khối quặng** (`*_ore`) trong schem: Abx có cơ chế sinh quặng riêng khi load map.

## Quy trình sau mỗi thay đổi
1. `generate_world.py`, đọc `report.txt` (bất biến 3).
2. **Mở ảnh ra xem** (Read tool): `images/layer2..4.png`, `sections.png`, `details.png`, `surface.png`.
3. Đổi cao độ / mép đảo → chạy lại `generate_world_commands.py`, cập nhật `docs/02_parameters.md`.
4. Ghi thay đổi vào `docs/04_status_and_todo.md`.

## Bẫy đã gặp
- Hành lang trong artifact dừng ở mép hang theo bán kính trung bình; mép thật lượn sóng (`blob_rho`) nên có chỗ hụt vài khối. `into_cavern()` kéo dài hành lang cuối thêm 7 khối vào hang.
- Gờ xoắn trong hố phải kết thúc đúng ở cửa hành lang phía nam (góc 90°), nếu không vòng thấp nhất chắn ngang lối ra.
- Thác nước: nguồn nước 1 ô có khí bên dưới chỉ chảy thẳng xuống (không loang ngang). Thác hố sụt nằm trong khe r7.6 ngoài gờ xoắn (r ≤ 7) để không đổ lên gờ.
- So tên khối với `"air"` bằng `in` sẽ khớp cả `stairs`: dùng `n == "minecraft:air"` (đã sửa cả ở `../tools/schem_view.py`).
- Kiểm tra công trình phòng boss bằng ảnh isometric: cắt vùng từ `kvelsgard_l5.schem` ra schem nhỏ rồi chạy `../tools/schem_view.py` (vùng to quá thì ảnh không đọc được).
- Phòng boss dùng `chain` (tên trước 1.21.9); schem mang DataVersion 1.21 nên FAWE tự nâng thành `iron_chain` khi dán. Nếu xích biến mất trong game thì báo lại.
- `pick()` cần dạng `100%x` khi chỉ có 1 khối trong `surface_pick`.
