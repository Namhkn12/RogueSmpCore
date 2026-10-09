# 03 — Runbook dựng trong game

## Chuẩn bị
- Một **void world** riêng cho template. Đảo dựng ở tâm (0, 0).
- FAWE đã cài. Tài khoản admin cần limit thay đổi đủ lớn: giai đoạn 01 xử lý ~10 triệu khối, giai đoạn 04 khoảng 3 triệu.
- Bay creative hoặc spectator. Đứng đâu cũng được, mọi lệnh đều dùng tọa độ thật.
- Mỗi dòng không bắt đầu bằng `#` trong `commands/*.txt` là **một lệnh chat**, dán theo đúng thứ tự từ trên xuống.

## Thứ tự
| # | File | Nội dung | Ghi chú |
|---|---|---|---|
| 1 | `00_barrier.txt` | tường, sàn, nắp barrier | |
| 2 | `01_terrain.txt` | khối đảo, lớp mặt, đổi Y ≤ 60 thành deepslate | lệnh đầu nặng nhất |
| 3 | `03_crater_shaft.txt` | phễu hố sụt | |
| 5a | `04a_reset_layer1.txt` | **chỉ khi** đã chạy `04` bản cũ (trước 2026-10-09): xóa khối tràn ngoài đảo, dựng lại barrier, lấp đá Y61–85 | sau đó chạy lại `03` rồi `04` |
| 5 | `04_layer1_cave.txt` | hang, hồ, ống, cột, vách, quặng, biome, trang trí, dây leo | có `//gmask`, phải chạy tới lệnh `//gmask` trống cuối cùng |
| 6 | `05a_reset_old_layer2.txt` | **chỉ khi** world đã có Layer 2 hoặc boss của bản cũ (sàn 44/34/24/14/4) | |
| 7 | `05_layer2_paste.txt` | dán schem Layer 2 | xem bên dưới |
| 8 | `05b_bedrock_seal_outer_ring.txt` | nối tấm bedrock ngăn tầng ra mép đảo (r 87–100.5) | chạy **sau** khi dán |
| 9 | `06_layer3_boss.txt` | dán `sylphens_boss_cavern.schem` (hang vòm boss + vỏ) | chép schem vào thư mục schematics trước; dán đè là xóa luôn phòng boss cũ (`-m !structure_void`) |
| 10 | `07_surface_spawn_river.txt` | dán `sylphens_surface.schem`: hang spawn, đường gấp khúc, sông bắc, rừng thông, podzol, cỏ hoa | chép schem vào thư mục schematics trước; chỉ chứa ô thay đổi (`-m !structure_void`) |
| 11 | `08_valley_biome.txt` | `//setbiome` taiga cổ thụ (cây, podzol, cỏ hoa đã có trong schem ở bước 10) | chạy sau cùng; thoát/vào lại để thấy màu biome |

> File `02_waterfalls_streams.txt` (7 suối cũ) đã bỏ từ v2.8.

## Dán Layer 2
1. Chép `output/sylphens_layer2.schem` vào `plugins/FastAsyncWorldEdit/schematics/`.
2. Chạy:
   ```
   //schem load sylphens_layer2
   //paste -o -m !structure_void
   ```
   - Không cần chọn vùng, không cần đứng ở đâu cụ thể. Cờ `-o` dán vào đúng gốc lưu trong file (−88, −21, −88).
   - `-m !structure_void` bỏ qua các ô ngoài vùng an toàn gần đáy đảo.
3. Nếu FAWE không nhận `-m`, dán bằng `//paste -o`. Hậu quả chỉ là vài mảng deepslate lòi dưới đáy đảo, vẫn nằm trong barrier.
4. **Dự phòng nếu `-o` dán lệch:** `//undo`, rồi `/tp @s -88 -21 -88`, rồi `//paste` (không có `-o`).
5. Dán lại (sau khi sinh seed mới hoặc đổi bộ sinh) thì đè trực tiếp lên bản cũ, không cần reset. Vùng schem phủ toàn bộ Layer 2.
6. Sau khi dán lại phải chạy lại `05b` nếu danh sách tấm bedrock đổi. Từ v2.6 tấm thứ 3 là Y14 (trước là Y12). Vòng bedrock Y12 cũ ở mép đảo (r 87–100.5) còn lại cũng không sao: nó nằm ngoài mọi phòng.

> Tên cũ của file: `dao_re_cay_l2.schem`, rồi `root_island_layer2.schem`. Nội dung giống hệt `sylphens_layer2.schem` (seed 2026), chỉ khác tên file.

## Kiểm tra sau khi dựng
| Lệnh | Phải thấy |
|---|---|
| `/tp @s 0 100 0` | đứng trên miệng hố, nhìn thấy sông phía bắc đổ vào hố |
| `/tp @s 0 122 80 180 0` | điểm spawn trong hang, nhìn ra cửa thấy thung lũng |
| `/tp @s 5 67 0` | trên đảo đá giữa hồ Layer 1, cạnh lỗ ống r3.5 ở tâm |
| `/tp @s 0 50 0` | giữa Sảnh Chính, bên dưới là hồ, có 4 cửa |
| `/tp @s 6 19 -72` | trong mê cung tầng 3, cạnh chân thang A từ T2 (seed 2026; tọa độ thang ở `docs/02` bảng vị trí) |
| `/tp @s 0 -15 0` | giữa phòng Thủ Vệ, sàn tâm là lodestone |
| `/tp @s 0 -53 55 180 0` | trong hang đến của phòng boss, nhìn ra hang vòm: sàn tròn có vết nứt xanh, gai trần, cột |

Kiểm tra tấm ngăn: đứng ở hành lang bất kỳ, đào thẳng xuống. Sau khoảng 2 khối phải gặp bedrock (trừ khi đang ở thang hoặc giếng trời).

Kiểm tra mê cung: đào vào tường, sàn hoặc trần bất kỳ trong mê cung. Sau đúng 1 lớp đá phải gặp bedrock.

## Hoàn tác
- Mỗi giai đoạn chạy được `//undo` ngay sau đó, nếu FAWE còn lưu history.
- Làm lại từ đầu: tạo void world mới là nhanh nhất.
