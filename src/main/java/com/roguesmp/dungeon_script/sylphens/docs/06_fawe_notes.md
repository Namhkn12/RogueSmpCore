# 06 — Ghi chú FAWE / WorldEdit

Nguồn: https://worldedit.enginehub.org/en/latest/usage/other/expressions/ và trang masks cùng site.

## Lệnh dùng trong dự án
| Lệnh | Ý nghĩa |
|---|---|
| `//pos1 x,y,z` / `//pos2 x,y,z` | chọn vùng bằng tọa độ thật (mọi giai đoạn đều chọn vùng trước) |
| `//g -r <pattern> <expr>` | `//generate` với `-r` dùng **tọa độ thật** (raw). Đặt khối ở mọi ô trong vùng chọn mà `expr > 0` |
| `//set`, `//replace <mask> <pattern>` | thay khối |
| `//gmask <mask>` / `//gmask` | mặt nạ toàn cục cho mọi lệnh sau. **Phải xóa bằng `//gmask` trống** |
| `//schem load <tên>` + `//paste -o` | dán schem vào gốc lưu trong file |

## Cú pháp biểu thức
- Giống Java. `^` là **lũy thừa** (không phải xor). `&&`, `||`, `? :` dùng được. Gán biến tạm bằng `a=...;` phân cách bởi `;`.
- Giá trị trả về là biểu thức cuối cùng. Kết quả > 0 nghĩa là đặt khối.
- Hàm dùng ở đây: `sqrt`, `abs`, `min`/`max` (2–3 tham số), `sin`, `atan2`, `perlin`.
- Vòng lặp tối đa 256 lần (không dùng trong dự án).

## Mặt nạ
| Mặt nạ | Ý nghĩa |
|---|---|
| `<air` | ô có khối phía **trên** là khí — **kể cả chính ô khí** |
| `>air` | ô có khối phía **dưới** là khí — **kể cả chính ô khí** |
| `!air` | mọi khối trừ khí |
| `=expr` | mặt nạ biểu thức (dùng trong `//gmask =perlin(...)<-0.25`) |
| `%50` | ngẫu nhiên 50% |

⚠ Mặt nạ offset `<m` / `>m` (`OffsetsMask.single` trong source WorldEdit) chỉ xét ô kề trên/dưới, **không xét chính ô đó**. `//replace <air X` vì vậy thay luôn mọi ô khí có khí ở trên, tức lấp đầy hang (lỗi đã gặp 2026-10-09). Cách dùng đúng:
- Trang trí đặt vào khí: `//gmask >moss_block` rồi `//replace air ...` (from = `air`, an toàn).
- Đổi vật liệu bề mặt: `//gmask <air` rồi `//replace stone,..._ore X` (from = danh sách đá cụ thể).
- Cần thêm điều kiện noise: đánh dấu bề mặt bằng khối tạm trước (xem `04_layer1_cave`: sàn = moss_block, trần = smooth_stone), rồi `//gmask =perlin(...)` + `//replace <khối tạm> X`.

Pattern có trọng số: `60%stone,40%andesite`. Pattern có trạng thái khối: `pointed_dripstone[vertical_direction=up]`.

## `perlin` — ĐÃ XÁC NHẬN TỪ SOURCE (2026-10-09)
- Thứ tự: `perlin(seed, x, y, z, frequency, octaves, persistence)` (`Functions.perlin` trong worldedit-core 7.4.2 và FAWE main).
- **Giá trị trả về nằm trong 0..1, không phải −1..1.** Code: `JLibNoiseGenerator.forceRange` = `clamp(GetValue/2 + 0.5, 0, 1)`. Trung vị ~0.5.
- Phân phối thực (2 octave, persistence 0.5, đo bằng jlibnoise 1.0.0 trên vùng r ≤ 88):

  | phân vị | 5% | 10% | 20% | 50% | 80% | 90% | 95% | 99% |
  |---|---|---|---|---|---|---|---|---|
  | giá trị | ~0.15–0.21 | ~0.23–0.28 | ~0.30–0.36 | ~0.50 | ~0.64–0.70 | ~0.73–0.79 | ~0.79–0.85 | ~0.91–1.0 |

  1 octave 3D (quặng, f≈0.12): p95 ≈ 0.82, p98 ≈ 0.89, p99 ≈ 0.93.
- Hệ quả: mọi ngưỡng viết theo giả định −1..1 đều lệch. Ví dụ Layer 1 cũ: `>0.58` cho cột phủ 45–64% hang, `>0.15` cho nấm phủ ~94% sàn, `<-0.25` không bao giờ đúng.
- Cần noise có dấu thì dùng `(perlin(...)*2-1)` (helper `sgn()` trong `generate_world_commands.py`).
- Biến `H` (mặt đảo) và hố sụt vẫn viết theo giả định cũ (noise lệch dương), nhưng giữ nguyên vì đã dựng và ổn.
- Layer 2 (schem) không phụ thuộc `perlin`.

## Server Minecraft 26.x (từ 2026-10-09 Abx dùng 26.2)
- `//forest <loại> <mật độ>`: loại cây là **id `PLACED_FEATURE` của game** (adapter 26.x đăng ký mọi placed feature là TreeFeature / FallenTreeFeature / CoralTreeFeature). Tên cũ của WorldEdit (`megaredwood`, `tallspruce`, `redwood`…) **không còn dùng được**. Ví dụ: `mega_spruce_checked`, `mega_pine_checked`, `spruce_checked`, `pine_checked`, `jungle_tree`, `mega_jungle_tree_checked`, `fancy_oak_checked`, `cherry_checked`. Gõ `//forest ` rồi Tab để xem danh sách thật trên server.
- Bản `_checked` có kiểm tra "cây non sống được" nên chỉ mọc trên nền đất/cỏ/podzol.
- **Thực tế trên server 26.2: `//forest mega_spruce_checked 0.2` báo thành công nhưng tạo 0 cây.** Lý do (source FAWE `main`): `function/generator/TreeGenerator.apply(position)` gọi `generateTree` ngay tại khối mặt đất do `GroundFunction` tìm ra, không phải ô khí phía trên như `ForestGenerator` cũ (`position.add(0, 1, 0)`), nên feature vanilla thấy gốc cây đè lên khối cỏ và từ chối. → Không dùng `//forest`; cây được dựng thẳng trong schem (`generate_surface.py`).
- `//forest`, `//flora`, `//replace`… có `@Confirm(REGION)`: khi diện tích 2D vùng chọn > 524 288 thì FAWE dừng chờ `//confirm`. Các vùng chọn trong `commands/` đều nhỏ hơn mức này.
- Đổi tên khối từ 1.21.9: `chain` → `iron_chain` (thêm xích đồng). Schem do script ghi vẫn mang DataVersion 3953 (1.21) để FAWE tự nâng cấp tên khối khi load; nếu thấy xích biến mất sau khi dán thì báo lại.

## Giới hạn
- Chat Minecraft giới hạn **256 ký tự**. Script tự kiểm và báo lỗi khi vượt.
- Lệnh `//g` trên vùng lớn cần limit FAWE đủ cao.
- `//g -r` tính biểu thức cho mọi ô trong vùng chọn. Vùng chọn càng sát thì càng nhanh, nên mỗi giai đoạn đều chọn vùng hẹp nhất có thể.

## Học được từ trước (ghi chú dự án)
- Địa hình ở quy mô phòng cần noise tần số cao hơn, ví dụ `noise(x/8, z/8)`.
- Làm trần khó hơn làm sàn. Dùng `//flip up` hoặc `//deform y=maxY-y`.
