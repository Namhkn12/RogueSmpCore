# CLAUDE.md — Dungeon Sylphens (tên cũ: Root Island / Đảo Rễ Cây Thế Giới v2)

Hướng dẫn cho Claude Code khi làm việc trong thư mục này. Quy ước chung cho mọi dungeon (người dùng, ngôn ngữ, bẫy FAWE) nằm ở `../CLAUDE.md`. Đọc file đó, rồi file này, rồi `docs/` theo nhu cầu.

Mọi lệnh dưới đây chạy từ thư mục `sylphens/`.

## Dự án là gì
Dungeon đảo nổi trong void world, tâm (0, 0), bán kính 100, chạy theo instance (mỗi team một bản). Người chơi được phá khối có kiểm soát.
- Mặt đảo: thung lũng lõm về tâm, núi dốc vây kín, rừng thông; **hang spawn** trong sườn nam + đường gấp khúc; sông bắc chảy vào **hố sụt tâm**.
- Layer 1 (Y~64–80): hang Lush, cột đá, vách ngăn, 4 biome không đều. Hồ tâm + ống thẳng xuống Layer 2.
- Layer 2 (5 tầng, sàn Y 46/30/18/−2/−18): **sinh thủ tục theo seed** ra file `.schem`. 4 tầng phòng (phòng ngẫu nhiên, hành lang có đuốc + vòm cửa, giếng trời) và **tầng 3 là một mê cung lớn** bọc bedrock (phòng combat nhỏ, bẫy). Tấm bedrock ngăn tầng.
- Layer 3: phòng boss hang vòm r46 (nền Y−61, vòm Y−25) phình dưới đáy đảo, sinh bằng `generate_boss_cavern.py` ra `sylphens_boss_cavern.schem`; chỉ vào bằng dịch chuyển từ tâm phòng Thủ Vệ. Phương án cũ sảnh fantasy (`generate_boss_room.py`) vẫn giữ.
- Toàn đảo bọc barrier r100.5–103 từ Y−64 tới 319.

Xem `images/cross_section.png` để nắm toàn cảnh trong 10 giây.

## Cấu trúc thư mục
```
CLAUDE.md                  file này
README.md                  tổng quan cho người đọc
requirements.txt           numpy, matplotlib (nbtlib tùy chọn để kiểm tra schem)
scripts/
  generate_world_commands.py  -> commands/*.txt  (barrier, mặt đảo, Layer 1, boss, lệnh dán Layer 2)
  generate_layer2.py          -> output/sylphens_layer2.schem + layer2_rooms.json/.md + layer2_report.txt
  check_walkability.py        mô phỏng đi bộ có trọng lực, mọi phòng phải tới được
  check_bedrock_seal.py       bedrock không lộ + không đào xuyên tầng được
  check_maze_walls.py         tầng mê cung: không đào xuyên tường để đi tắt
  generate_boss_room.py       -> output/sylphens_boss.schem + boss_room.json (tự kiểm bedrock lộ, đá độn lộ, đường lên bệ)
  generate_boss_cavern.py     -> output/sylphens_boss_cavern.schem + boss_cavern.json (ĐANG DÙNG; tự kiểm hở ra ngoài đảo,
                                bedrock lộ, khoảng trống dưới gai, đi bộ hang đến -> tâm sàn)
  generate_surface.py         -> output/sylphens_surface.schem + surface.json + images/surface_plan.png (hang spawn, đường, sông;
                                tự kiểm đá phủ trần, bậc phải nhảy, đi bộ spawn -> thung lũng). Địa hình tính bằng ../tools/fawe_noise.py
  render_layer2_maps.py       -> images/layer2_floor_maps.png
  render_diagrams.py          -> images/cross_section.png, surface_heightmap.png, layer1_plan.png
commands/                  lệnh FAWE dán vào chat theo thứ tự tên file (KHÔNG sửa tay, sửa script rồi sinh lại)
output/                    schematic + dữ liệu phòng (sinh tự động)
images/                    sơ đồ (sinh tự động)
docs/                      thiết kế, thông số, runbook, thuật toán, tích hợp plugin, ghi chú FAWE, việc còn dở
docs/legacy/               bố cục v1 (bán kính 170) và quyết định cũ về phòng boss, chỉ để tham khảo
```

## Lệnh thường dùng (chạy từ thư mục gốc)
```bash
pip install -r requirements.txt            # nếu thiếu: pip install --break-system-packages ...
python3 scripts/generate_world_commands.py # ~1s, sinh commands/
python3 scripts/generate_layer2.py [seed]  # ~10s, mặc định seed 2026
python3 scripts/check_walkability.py [seed]
python3 scripts/check_bedrock_seal.py [seed]
python3 scripts/check_maze_walls.py [seed]
python3 scripts/render_layer2_maps.py [seed]
python3 scripts/render_diagrams.py
python3 scripts/generate_boss_room.py      # <1s (phương án 1, không dùng)
python3 scripts/generate_boss_cavern.py    # ~2s
python3 ../tools/schem_view.py output/sylphens_boss_cavern.schem images/boss_cavern.png --cut -44 --open --x 0 --z 0
python3 scripts/generate_surface.py        # ~3s
python3 ../tools/schem_view.py output/sylphens_boss.schem images/boss_room.png --cut -43 --open --x 0 --z -33
```
Các script check/render **import generate_layer2**, tức là chạy lại toàn bộ bộ sinh trong RAM (~10s mỗi lần). Seed phải giống nhau giữa các lệnh.

## Bất biến KHÔNG được phá
1. **Mọi lệnh trong `commands/` ≤ 256 ký tự** (giới hạn chat Minecraft). `generate_world_commands.py` tự kiểm (`check()`); giữ nguyên.
2. **Tọa độ thật**: lệnh dùng `//g -r`, schem dán bằng `//paste -o` (gốc schem = −88, −21, −88). Không dùng tọa độ tương đối.
3. **Sàn Layer 2 khai báo ở 2 nơi**: `F`, `MAZE_FL`, `MZ_BOT` trong `generate_layer2.py` và trong `generate_world_commands.py` (lệnh 05b tính Y tấm bedrock = F−2, riêng tầng mê cung = F+MZ_BOT). Đổi một chỗ phải đổi cả hai, cộng `BOSS` nếu hạ thấp. Khối bedrock mê cung (F−4..F+9) không được chạm phòng tầng trên/dưới: hiện T2 vỏ thấp nhất Y28, T4 trần cao nhất Y13.
4. **Y ≤ 60 là deepslate** (vỏ không cho phá). Layer 2 và boss phải nằm hoàn toàn dưới Y60.
5. **Tính tất định**: cùng seed → cùng schem từng khối. Chỉ dùng `rng`/`nrng` có seed và `zlib.crc32`; KHÔNG dùng `hash()` của Python (bị random hóa mỗi lần chạy).
6. **Không để bedrock lộ trong phòng** và **không đào xuyên tầng được** ngoài thang/giếng trời. `check_bedrock_seal.py` phải in `0` và 4 dòng `None`. **Không đào tắt qua tường mê cung**: `check_maze_walls.py` phải in `ô khí chưa tới được 0, đường tắt qua tường: 0`. Tường mê cung luôn dày 3 (đá | bedrock | đá) nhờ đường đi nằm trọn trong lõi ô (`mz_strip`).
7. **Mọi phòng đi bộ tới được** từ Sảnh Chính. `layer2_report.txt` phải có `TẤT CẢ PHÒNG THÔNG: True`, `check_walkability.py` phải in danh sách rỗng.
8. Muốn xuống dưới tầng mê cung thì bắt buộc đi qua mê cung: không giếng trời nào nối vào/ra tầng `MAZE_FL`. Mỗi tầng có 2 thang xuống (A, B), không giấu, đặt xa lối vào và xa nhau.

## Quy trình sau mỗi thay đổi Layer 2
1. `python3 scripts/generate_layer2.py` và đọc phần cuối (mỗi tầng, mê cung, thông đường, tấm bedrock).
2. `python3 scripts/check_walkability.py`, `python3 scripts/check_bedrock_seal.py`, `python3 scripts/check_maze_walls.py`.
3. `python3 scripts/render_layer2_maps.py`, rồi **mở ảnh ra xem** (Read tool) trước khi báo xong.
4. Nếu đổi thông số chung: chạy lại `generate_world_commands.py` và `render_diagrams.py`, cập nhật `docs/02_parameters.md`.
5. Ghi thay đổi vào `docs/07_status_and_todo.md`.

## Bẫy đã gặp
- Hố bẫy / hồ sâu quá sẽ ăn thủng trần tầng dưới. Hố bẫy hiện sâu 3, hồ giếng trời sâu 3. Hàm `seal_floors()` bọc bedrock quanh vùng trũng chạm tấm.
- Hố bẫy T2 ăn xuống Y27 (đỉnh khối mê cung), nên phòng mê cung chỉ cao 6 (`MZ_RH`) để trên trần còn 2 lớp bedrock.
- Thứ tự dựng: vỏ/khí phòng thường → `build_maze` (đổ bedrock rồi khoét) → thang → nội thất → vòm cửa → đuốc → đèn trần → `seal_floors` → `face_maze` (phủ đá lên bedrock lộ). Đổi thứ tự dễ làm lộ bedrock hoặc phủ đá lên tấm ngăn.
- `hash()` làm nội thất khác nhau mỗi lần chạy (đã sửa sang `zlib.crc32`).
- Bẫy `perlin` 0..1 và vùng chọn vuông: xem `../CLAUDE.md`. Trong `generate_world_commands.py`, noise có dấu dùng helper `sgn()`.
- Đáy đảo thu nhỏ dần nên tầng thấp có bán kính dùng được nhỏ hơn (`RMAX`). Ô ngoài vùng an toàn được ghi `structure_void` để không dán ra ngoài không khí.
- Trong schem, tầng 1 cao tối đa 10 (`HMAX[1]`) vì phía trên là sàn Layer 1 (Y61+).
