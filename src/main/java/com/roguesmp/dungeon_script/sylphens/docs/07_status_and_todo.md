# 07 — Trạng thái và việc còn dở

## Trạng thái (2026-10-09)
| Phần | Trạng thái |
|---|---|
| Barrier, mặt đảo, hố sụt | lệnh đã sinh (`commands/00, 01, 03`), Abx hài lòng phần lớn |
| Hang spawn, đường gấp khúc, sông bắc, rừng thông | `sylphens_surface.schem` + lệnh `07`, `08` (v2.8), chờ Abx xem trong game |
| Layer 1 | lệnh đã sinh (`commands/04`) |
| Layer 2 | schem seed 2026, kiểm tra thông đường + bedrock đều đạt |
| Bedrock ngăn tầng | trong schem + lệnh `05b` |
| Layer 3 boss | hang vòm `sylphens_boss_cavern.schem` (v2.9) đang dùng; sảnh fantasy (v2.7) giữ làm phương án 1 |
| Plugin | phía Abx |

Kết quả kiểm tra (seed 2026):
- 93 phòng không tính thang, chân thang và mê lộ (tầng 1–5: 23/22/16/19/13). Report đếm cả thang và chân thang nên ra 25/26/21/24/15.
- Mọi phòng đi bộ tới được.
- Đường ngắn nhất từ Sảnh tới Thủ Vệ khoảng 654 bước.
- Bedrock lộ ra khí: 0. Đào xuyên tầng: không có đường nào.

Abx: "khá hài lòng đa phần, vẫn cần một số điều chỉnh". **Chưa nêu cụ thể điều chỉnh nào. Hỏi Abx trước khi sửa.**

## Việc còn dở / ý tưởng
- [x] Xác nhận thứ tự tham số `perlin`: đúng `seed, x, y, z, …`; giá trị trả về 0..1 (xem 06).
- [ ] Vẽ lại `images/layer1_plan.png`: `render_diagrams.py` vẫn dùng noise minh họa + ngưỡng cũ.
- [ ] Cửa khóa / cửa một chiều / chìa khóa trong Layer 2. Bản v1 có hệ Chìa Đồng, Chìa Sắt, 3 Ấn, xem `legacy/`. Bản v2 chưa có.
- [ ] Cơ chế bẫy cụ thể (dispenser, tripwire, áp suất) trong Hành Lang Bẫy và bẫy `arrow` của mê cung (dispenser chưa nối redstone, chưa có tên).
- [ ] Loot rương (`BlockEntities` đang rỗng).
- [ ] Thác từ núi đổ xuống hồ đầu nguồn sông bắc (Abx tự làm).
- [ ] Phòng boss: Abx xem trong game và góp ý (cột, sông, tượng, cây cỏ).
- [x] (v2.9 thay bằng hang vòm có vỏ đá tự nhiên phình dưới đáy đảo) Đáy hộp boss (Y−58) thấp hơn đáy đảo ở phần lớn diện tích (đáy đảo ≈ −60 + 5·noise ở tâm, cao dần ra ngoài): vỏ bedrock lộ ra mặt dưới đảo. Chỉ thấy được từ khoảng trống trong barrier dưới đảo, người chơi không tới được. Nếu muốn giấu: nâng cả hộp (tối đa trần −22) hoặc đắp deepslate dưới đáy đảo.
- [ ] Cổng dịch chuyển lên mặt đảo (stargate) của v1: nay người chơi xuất hiện trong hang spawn; nếu vẫn muốn cổng thì đặt ở sân trước cửa hang.
- [ ] Chưa có tấm bedrock giữa Layer 1 và tầng 1, và dưới tầng 5. Hiện chỉ có deepslate, người chơi đào được nếu plugin không cấm.
- [ ] Tối ưu nhân bản instance (chọn cách sao chép world).

## Nhật ký thay đổi
- **2026-10-08 v2.0**:
  - Thu đảo xuống r100.
  - Mặt đảo thành thung lũng vây kín, 7 thác, hố sụt tâm.
  - Layer 1 Lush với cột đá và vách ngăn.
  - Layer 2 bản đầu: 4 tuyến đối xứng, tầng cách 10 (sàn 44/34/24/14/4).
- **2026-10-08 v2.1**: Layer 2 viết lại thành bộ sinh thủ tục:
  - Tầng cách 16 (46/30/14/−2/−18).
  - Phòng ngẫu nhiên, mê lộ, thang A và B, giếng trời, 7 loại phòng tài nguyên.
  - Boss hạ xuống sàn −57.
  - Thêm file reset `05a`.
- **2026-10-08 v2.2**:
  - Tấm bedrock ngăn tầng ở Y 44/28/12/−4, lệnh `05b` cho vành ngoài.
  - Hố bẫy còn sâu 3 (trước đó sâu 5–6, ăn thủng trần tầng dưới).
  - Hồ giếng trời còn sâu 3.
  - Sửa nội thất không tất định (`hash` → `zlib.crc32`).
- **2026-10-09 v2.3**:
  - Đóng gói dự án cho Claude Code: tên thư mục và file tiếng Anh, script chạy theo đường dẫn tương đối.
  - Đổi tên schem thành `root_island_layer2`.
  - Thêm `layer2_rooms.md` và sơ đồ mặt đảo, Layer 1.
  - Xóa code Layer 2 cũ khỏi script lệnh thế giới.
- **2026-10-09 v2.4** (Layer 1):
  - Phát hiện `perlin()` trả về 0..1 → ngưỡng cũ lệch: cột phủ 45–64% hang, nấm ~94% sàn, không có thạch nhũ, quặng ~25% đá.
  - Cột/vách thiếu giới hạn r nên tràn ra 4 góc vùng chọn vuông (−90..90) và đè lên barrier.
  - Sửa: ngưỡng mới dò bằng mô phỏng jlibnoise; cột/vách giới hạn r ≤ 88; vách dày đều ~2 khối; khoang hang/hồ dùng noise có dấu.
  - Thêm `04a_reset_layer1.txt` để dọn bản cũ.
- **2026-10-09 v2.5**:
  - Đặt tên dungeon là **Sylphens**, chuyển vào `dungeon_script/sylphens/` để sau này chứa thêm dungeon khác.
  - Đổi tên schem thành `sylphens_layer2`. Nội dung không đổi; file cũ `root_island_layer2.schem` trên server có thể xóa.
- **2026-10-09 v2.6** (Layer 2):
  - Bỏ mê lộ nhỏ ở mỗi tầng (đào phá được để đi tắt) và tường giả / thang ẩn B.
  - **Tầng 3 thành một mê cung lớn** (sàn 14 → 18): tường, nền, trần là bedrock, phủ 1 lớp đá; đường rộng 3–5; 7 phòng combat nhỏ, 14 bẫy (hố chông, mũi tên, mạng nhện), 8 rương ngõ cụt. Tấm ngăn T3|T4 thành Y14.
  - Thư Khố chuyển lên T1, Vườn Nấm xuống T4 (thay Mỏ Quặng Sâu thứ 2). Giếng trời không nối vào/ra tầng mê cung.
  - Mỗi tầng 2 thang xuống thường, đặt xa lối vào và xa nhau.
  - Hành lang + phòng nhỏ + mê cung dùng đuốc gắn tường; đèn trần chỉ còn ở phòng lớn. Vòm cửa ở mọi cửa phòng, vòm sườn trong hành lang gạch.
  - Thêm `check_maze_walls.py` (đã thử âm: đục lõi bedrock 1 bức tường thì báo lỗi).
  - Kết quả seed 2026: mọi phòng thông, bedrock lộ 0, đào xuyên tầng None ×4, đường tắt qua tường mê cung 0.
- **2026-10-09 v2.7** (Layer 3):
  - Phòng boss sảnh fantasy theo phương án đầu trong `legacy/` + cây cỏ: `scripts/generate_boss_room.py` -> `sylphens_boss.schem`, `boss_room.json`. Lệnh `06` giờ dán schem.
  - Sàn boss nâng từ khối −57 lên −56 (đứng −55) để có lòng sông/hồ trên vỏ bedrock.
  - Thêm công cụ chung `../tools/schem_io.py` (đọc .schem v2/v3, .litematic; ghi .schem) và `../tools/schem_view.py` (ảnh xem trước).
- **2026-10-09 v2.7.1** (Layer 1, sửa lỗi):
  - `//replace <air moss_block` lấp đầy hang tới miệng hố: mặt nạ `<air` khớp cả ô khí (đã xác nhận trong source WorldEdit `OffsetMaskParser`).
  - Đổi sang: `//gmask <air|>air` + `//replace <danh sách đá>` để đánh dấu sàn (moss_block) / trần (smooth_stone tạm), rồi biome chỉ thay khối đánh dấu. Mô phỏng: bản cũ lấp 94% khí, bản mới giữ nguyên khí và nước.
- **2026-10-09 v2.8** (mặt đảo):
  - Bỏ 7 suối/thác (`02_waterfalls_streams`, chưa từng chạy trên world).
  - Hang spawn trong sườn núi nam (cửa nhìn vào tâm, sàn Y121) + đường gấp khúc 3 chặng xuống thung lũng; sông bắc từ chân núi vào miệng hố: `scripts/generate_surface.py` -> `sylphens_surface.schem`, `surface.json`, lệnh `07`.
  - Biome taiga cổ thụ, podzol, thông khổng lồ + thông cao (chừa vùng trước hang), cỏ hoa: lệnh `08`.
  - Thêm `../tools/fawe_noise.py`: perlin của FAWE bằng numpy, khớp jar jlibnoise tới 1e-16 → script Python tính đúng độ cao địa hình thật.
- **2026-10-09 v2.8.1**: server lên MC 26.2. `//forest megaredwood/tallspruce` không chạy vì FAWE 26.x lấy loại cây từ placed feature của game → đổi sang `mega_spruce_checked`, `mega_pine_checked`, `spruce_checked`, `pine_checked`. Lệnh cỏ hoa (`//gmask >grass_block` + `//replace air ...`) cũng báo không chạy, chưa rõ lỗi: chờ Abx gửi thông báo lỗi.
- **2026-10-09 v2.8.2**: `//forest` trên FAWE 26.2 tạo 0 cây (FAWE đặt cây vào chính khối mặt đất). Chuyển rừng thông (113 cây 4 loại), podzol, coarse dirt, cỏ hoa vào `sylphens_surface.schem`; lệnh `08` đổi tên `08_valley_biome.txt`, chỉ còn `//setbiome`. Lệnh cỏ hoa cũ cũng bỏ nên không cần tìm lỗi nữa.
- **2026-10-09 v2.9** (Layer 3 phương án 2):
  - Phòng boss hang vòm r46 (nền Y−61, vòm Y−25), phình dưới đáy đảo với vỏ deepslate dày 6 (lớp 4 bedrock): sàn tròn r24 cao 3 với vết nứt lưới phát sáng xanh (blue concrete / green glass ×2 / lime glass ×3 / light), vòng triệu hồi, 4 bậc thang; gai trần (gai lớn giữa + 7 gai tủa ra + ~38 gai), mũi kính xanh + verdant froglight; 7 cột, 26 măng đá; hang đến phía nam + đường gấp khúc.
  - `scripts/generate_boss_cavern.py` -> `sylphens_boss_cavern.schem`, `boss_cavern.json`; lệnh `06` dán schem này (xóa luôn phòng boss cũ).
  - Tự kiểm: không ô khí nào hở ra ngoài đảo, bedrock lộ 0, mũi gai cách sàn ≥ 17, đi bộ hang đến → nền → tâm sàn tròn, không bậc phải nhảy.
