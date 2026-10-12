# 03 — Runbook dựng trong game

## Chuẩn bị
- Void world riêng tên `Kvelsgard` (Multiverse-Core + VoidGenerator), đảo dựng ở tâm (0, 0).
- FAWE trên server MC 26.2. Limit thay đổi của tài khoản admin phải đủ cho ~15 triệu khối (schem l1 ~5.6 triệu, l4 ~5.5 triệu).
- Chép 5 file `output/kvelsgard_l1..l5.schem` vào `plugins/FastAsyncWorldEdit/schematics/`.
- Mỗi dòng không bắt đầu bằng `#` trong `commands/*.txt` là **một lệnh chat**, dán theo thứ tự.

## Thứ tự
| # | File | Nội dung |
|---|---|---|
| 1 | `00_barrier.txt` | tường, sàn, nắp barrier r119.5–122 |
| 2 | `01_paste_island.txt` | dán 5 schem (`//schem load` + `//paste -o -m !structure_void`), thứ tự nào cũng được |
| 3 | `02_biomes.txt` | tùy chọn: stony_peaks / windswept_gravelly_hills / dripstone_caves / deep_dark theo dải Y |
| 4 | `03_remove_ores.txt` | chỉ khi đã dán schem bản cũ có quặng: thay mọi quặng bằng stone / deepslate |

- Không cần chọn vùng hay đứng chỗ nào khi dán: `-o` dán vào gốc lưu trong file.
- Nếu FAWE không nhận `-m`: dán bằng `//paste -o`. Hậu quả là khối khí `structure_void` đè lên barrier ở 4 góc hộp: chạy lại `00_barrier.txt`.
- Dự phòng nếu `-o` dán lệch: `//undo`, `/tp @s <gốc trong docs/02>`, rồi `//paste`.
- Sửa script rồi sinh lại: chỉ cần dán lại các dải đã đổi, dán đè trực tiếp.

## Kiểm tra sau khi dựng
| Lệnh | Phải thấy |
|---|---|
| `/tp @s 0 99 105 180 0` | đứng trên khối chiseled ở thềm, trước mặt là đường leo chữ chi lên bãi mỏ |
| `/tp @s -4 121 40 180 0` | bãi mỏ, cửa hang 17×13 trước mặt, ray đôi chạy vào hang |
| `/tp @s -10 110 -45` | cuối hang dẫn, phía trước là hố, gờ xoắn bắt đầu ở mép |
| `/tp @s 0 73 -62` | đáy hố, cửa hành lang phía nam dẫn tới Trạm Tời |
| `/tp @s 6 79 52` | bậc giữa Hang Mạch Lộ, trụ đá + giàn giáo, thác mảnh phía đông nam |
| `/tp @s -22 76 54` | mép giếng thang 9×9, giá đầu giếng bằng gỗ |
| `/tp @s -17 41 57` | phòng Chân Thang tầng 3, chân thang giếng |
| `/tp @s 8 45 -50` | bậc cao nhất Động Nhũ Thạch, hố sụt phía đông |
| `/tp @s 22 9 -64` | góc phòng Hồ Rơi, hồ tròn + tháp dốc, thác đổ từ trên |
| `/tp @s 0 11 50` | đài giữa Lõi Kvelsgard, hào dung nham + 4 cầu |
| `/tp @s 0 -40 50 180 0` | bệ xuất hiện phòng boss: nhìn dọc đại lộ 12 cột vừa, 2 cột lớn tới bệ 3 bậc và cổng |
| `/tp @s 0 -36 -38 180 0` | bậc 2 của bệ: rãnh lửa soul, bàn triệu hồi và cổng reinforced phía trước |
| `/tp @s 29 -46 21` | hố mộ trong điện mộ M, 6 quách đá |
| `/tp @s -42 -25 -18` | tầng 3 tháp canh T1, leo thang lên vương miện |

Kiểm tra tấm ngăn: đứng ở hành lang tầng 2 hoặc 3 đào thẳng xuống, phá 3 khối (kể cả khối sàn) thì gặp bedrock; tầng 4 là 5 khối. Đào ngang ra phía mép đảo phải gặp vách bedrock trước khi ra ngoài.

## Dữ liệu cho plugin (`output/layout.json`)
- `spawn.pos` + `yaw`: điểm dịch chuyển tới.
- `layers[].rooms[]`: `name`, `type` (`combat` / `rest` / `store` / `reward` / `entry`), `min`/`max` (khối khí của phòng), `center` (ô đứng giữa sàn).
- `layers[].cavern`: tâm, bán trục, Y khí cao nhất.
- `connectors.well` / `connectors.sinkhole`: tâm, Y đỉnh / đáy; `sinkhole.waterfall` là ô cột thác.
- `boss`: `spawn` + `yaw` (đích teleport), `teleport_from` (đài Lõi Kvelsgard), `summon_table`, `fire_trench`, `gate` (khối + lòng cổng), `hall`, `structures[]` (T/H/M/N/G), `sculk_blocks`.
- `seals`: Y các tấm bedrock (khối bedrock không cho phá: plugin nên chặn phá bedrock, còn lại tùy luật khai thác).

## Hoàn tác
- Mỗi lần dán chạy được `//undo` ngay sau đó nếu FAWE còn lưu history.
- Làm lại từ đầu: tạo void world mới.
