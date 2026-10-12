# 04 — Trạng thái và việc còn dở

## Trạng thái (2026-10-10)
- Layer 1 + 3 tầng mỏ + phòng boss + tấm bedrock, sinh ra 5 schem (không có quặng). Chưa dựng thử trong game.
- `report.txt`: bedrock lộ 0, đúng 3 lỗ tấm (hố, giếng, hố sụt), đi bộ tới mọi phòng + tâm 3 hang khai thác; phòng boss: từ bệ xuất hiện tới sảnh, bậc 2 bệ, gờ chân cổng, cuối đường chéo, hố mộ, tầng 2 nhà H1, tháp T1 (trệt + mái).

## Nhật ký
| Ngày | Thay đổi |
|---|---|
| 2026-10-10 | Tạo thư mục, port bố cục artifact, bộ sinh `generate_world.py`, lệnh FAWE, tài liệu. Đảo r104–117, tấm bedrock giữa các layer, bỏ cửa khoá. |
| 2026-10-10 | Bỏ sinh quặng (plugin tự sinh khi load map). Thêm `commands/03_remove_ores.txt` để xoá quặng ở bản đã dán. |
| 2026-10-10 | Layer 5 phòng boss Cổ Thành Câm Lặng theo artifact (`boss_room.py`), tách schem `kvelsgard_l5` (Y−60..2), `l4` còn Y3..36. Sửa lỗi bộ kiểm tra coi `stairs` là ô trống. |
| 2026-10-10 | Phòng boss bản 2 theo artifact (ảnh mẫu Ancient City): cổng + bệ đúng bản vẽ từng block, hang deep dark, công trình T/H/M/N/G, cột vừa/lớn/nấm, tường thành, sculk. Sửa `schem_view.py` (stairs bị coi là khí). |

## Việc còn dở
- [ ] Dựng thử trong game, ghi góp ý vào `request.json` → `feedback`.
- [ ] Chốt cách xuống phòng boss: teleport (hiện tại) hay giếng rơi thật 52 khối từ đài Lõi (cần thêm hồ ở bệ xuất hiện).
- [ ] Kích cỡ / kiểu đánh của boss: quyết định độ cao trần trên bệ và độ trống của sảnh.
- [ ] Trang trí thêm nếu cần: cầu treo trong Hang Mạch Lộ, xe goòng (entity, plugin đặt), biển tên mỏ (block entity), lều vải.
- [ ] Kiểm tra `//setbiome deep_dark` cho tầng 4 có gây hiệu ứng không mong muốn không (bỏ dòng đó nếu có).
