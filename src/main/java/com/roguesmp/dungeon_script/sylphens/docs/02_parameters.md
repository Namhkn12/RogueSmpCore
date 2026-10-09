# 02 — Thông số

Hệ tọa độ: tâm đảo (0, 0). Bắc = z âm, Đông = x dương. Góc 0° = +x (đông), 90° = +z (nam). Mọi số dưới đây là **tọa độ thật**.

## Độ cao tổng
| Phần | Giá trị | Nguồn trong code |
|---|---|---|
| Barrier tường | r 100.5–103, Y −64 → 319 | `00_barrier` |
| Barrier sàn / nắp | Y −64 / Y 319, r² < 10700 | `00_barrier` |
| Khối đảo | r ≤ 97–100 (noise), Y từ đáy tới mặt H | `01_terrain` |
| Đáy đảo | `b = −60 + (r/100)³·55 + n·5`: r0 → −60, r50 → −53, r90 → −20 | `01_terrain` |
| Ranh deepslate | Y ≤ 60 thay `stone` → `deepslate` | `01_terrain`, `DEEP_TOP` |
| Layer 1 | sàn ~64 (±2), trần ~80 (±4), r ≤ 80±6 | `04_layer1_cave` |
| Layer 2 sàn tầng | **46 / 30 / 18 / −2 / −18**, T3 là tầng mê cung | `F`, `MAZE_FL` (cả 2 script) |
| Bedrock ngăn tầng | **44 / 28 / 14 / −4** (= F − 2; tầng mê cung = F − 4, đáy khối bedrock) | `SEAL_Y`, `SEAL`, lệnh `05b` |
| Khối bedrock mê cung | Y 14..27 (F−4..F+9), r ≤ 89, khoét đường + phòng, phủ đá lên mặt lộ | `build_maze`, `face_maze` |
| Layer 3 boss (đang dùng) | hang vòm r46: nền Y−61, sàn tròn Y−58, đỉnh vòm Y−25; vỏ dày 6 tới Y−22, phình xuống dưới đáy đảo | `generate_boss_cavern.py` |

## Mặt đảo (`01_terrain`, `07_surface_spawn_river`, `08_valley_biome`)
Công thức độ cao dùng chung (biến `H`). Mọi lệnh cần độ cao đều phải dùng đúng công thức này:
```
r = sqrt(x²+z²)
n = perlin(7, x,0,z, 0.03, 3, 0.5)
q = r + perlin(9, x,0,z, 0.012, 2, 0.5)·6        # méo vành núi
m = min(max((q−68)/12,0), max((100−q)/12,0), 1)  # 0 ở thung lũng, 1 trên sống núi
h = 88 + 0.2·r + 2n + m^0.7·(62 + 18n)
```
Khi không có noise:
- Thung lũng cao ~91 sát hố, ~102 ở chân núi (r 68).
- Sống núi cao ~150–185 ở r 80–88.
- Sườn trong lên ~60 khối trên 12 khối ngang.

Lớp mặt:
- h < 128: lớp trên là cỏ, dưới là 3 lớp đất.
- h ≥ 128: đá hỗn hợp (stone/andesite/tuff/cobble).
- h ≥ 160: tuyết.


Hố sụt (`03_crater_shaft`):
- Phễu `r ≤ 13 + max(0, y−80)·0.25 + perlin·1.5`, từ Y66 tới Y130.

Lưu ý: trong công thức, `perlin()` trả về 0..1 (xem 06), nên địa hình thực lệch dương so với khi viết. Độ cao **chính xác** từng cột tính bằng `../tools/fawe_noise.py` (khớp jlibnoise của FAWE tới 1e-16), dùng trong `generate_surface.py`.

### Hang spawn + đường gấp khúc (`generate_surface.py` -> `sylphens_surface.schem`, lệnh `07`)
| Thành phần | Thông số |
|---|---|
| Điểm spawn | (0, 122, 80), yaw 180 (nhìn về bắc, ra cửa) — giữa buồng hang |
| Cửa hang | sườn trong phía nam, mặt vách z = 70, sàn Y121 (đứng Y122), cửa rộng 5 cao 5; lanh tô ≥ 3 khối đá (`YE`, `ZM`) |
| Buồng hang | elip 18 × 14, vòm cao tới ~Y129, trên trần ≥ 19 khối đá, vách sau tới sườn ngoài ≥ 7 khối (`RX`, `RZ`, `TUN`) |
| Trong hang | sàn đá/rêu/sỏi, thảm rêu, bệ 3×3 ở điểm spawn, 4 đèn lồng, glow berries + nhũ đá trên vòm |
| Đường gấp khúc | 3 chặng men theo vòng chân vách, mỗi chặng rộng 4 + lan can cobblestone_wall; dốc 1 khối xuống mỗi 3 khối, mọi bậc có cobblestone_stairs (không phải nhảy); chặng: Y121 → 117 → 106 → chạm đất ~Y99 (`BANDS`, `LEGS`, `SLOPE`) |
| Cách dựng đường | phía núi: khoét khí tới mặt núi; phía thung lũng: đắp đá (stone/andesite/cobblestone) từ mặt đất lên sàn đường |

### Sông phía bắc
| Thành phần | Thông số |
|---|---|
| Hồ đầu nguồn | chân núi bắc, quanh (-2, 100, -64) — chỗ Abx tự thêm thác từ núi |
| Lòng sông | rộng ~6, sâu 2, đáy cát/sỏi/đất sét; tâm uốn lượn `x = 4·sin(z/9)`; mặt nước = mặt đất tâm − 1, không bao giờ cao lên khi xuôi dòng (Y100 → Y89) |
| Bờ | bờ thấp sát nước (cỏ, ~25% có mía), ra ngoài thoải dần; không khoét chỗ địa hình cao hơn mặt nước > 6 |
| Cuối sông | chảy tới mép hố sụt (z ≈ -8); ô nằm trong phễu hố bị bỏ qua để nước đổ thẳng xuống hố |

### Thung lũng (trong `sylphens_surface.schem`, biome bằng lệnh `08`)
| Thành phần | Thông số |
|---|---|
| Biome | `old_growth_spruce_taiga` cho x,z −100..100, Y86..200 |
| Nền | 9% podzol, 5% coarse dirt rải trên cỏ; vạt podzol bán kính 3 quanh gốc thông khổng lồ |
| Cây | tự dựng (không dùng `//forest`): 26 thông khổng lồ 2×2 cao 18–27, 12 thông khổng lồ ngọn thưa, 45 thông cao 9–14, 30 thông thân mảnh 12–17; cách nhau ≥ 6–9 khối; chỉ trên cỏ phẳng vừa, r 19–90; chừa x −30..30, z 45..100 (trước hang) và 2 bờ sông (`TREE_KINDS`, `FOREST_KEEP_OUT`) |
| Cỏ hoa | trên cỏ: 11% cỏ ngắn, 3% dương xỉ, 6 loại hoa mỗi loại 1%; trên podzol: 22% dương xỉ, 3% bụi dâu, 3% nấm nâu |

## Layer 1 (`04_layer1_cave`)
| Thành phần | Thông số |
|---|---|
| Khoang hang | `r ≤ 80 + 6n`, `64+2n < y < 80 + 4·perlin(12)` |
| Tâm phẳng | r ≤ 24: sàn 64, khí 65–79 |
| Hồ tâm | r 7.5–19(+2 noise), nước Y61–64 |
| Đảo đá giữa hồ | r ≤ 7.5, đá tới Y65, rêu Y66 |
| Ống xuống Layer 2 | r ≤ 3.5, Y61–66 (L1) và Y59–60 + trần Sảnh (schem) |
| Lưu ý | `perlin()` trả về 0..1 (xem 06). Khoang hang, hồ dùng `sgn(p) = p·2−1` để có noise −1..1 |
| Cột đá | 27 < r ≤ 88, `perlin(13, f0.09) > 0.84−k` hoặc `perlin(14, f0.035) > 0.78−k`, k = |y−72|·0.01. Chiếm ~8% hang ở Y72, ~15% sát sàn/trần (`PIL_S`, `PIL_G`, `PIL_K`) |
| Vách ngăn | 30 < r ≤ 88, `|m−0.5| < 0.9·(|∂x m|+|∂z m|)` với m = perlin(15, f0.02): đồng mức chia độ dốc nên dày đều ~2 khối; khe khi `perlin(16, f0.05) ≤ 0.4`. Chiếm ~5% (`WALL_W`, `WALL_GAP`) |
| Quặng (trong stone) | coal 0.86, iron 0.89, copper 0.89, gold 0.92 (ngưỡng perlin 3D, ~1–3% khối đá mỗi loại) |
| Biome `b = perlin(31, f0.018)` | Rêu mặc định ~46% · Thạch Nhũ b < 0.30 ~19% · Nấm b > 0.60 ~20% · Pha Lê b > 0.76 ~15% (`B_DRIP`, `B_MUSH`, `B_CRYS`) |
| Dây leo | 6 hướng (`|sin 3θ| < 0.07`), bám vách phễu Y81–91, treo r14–15 Y67–79 |

## Layer 2 (`generate_layer2.py`)
| Hằng số | Giá trị | Ý nghĩa |
|---|---|---|
| `F` | {1:46, 2:30, 3:18, 4:−2, 5:−18} | sàn (khối đứng) |
| `MAZE_FL` | 3 | tầng mê cung |
| `RMAX` | {1:84, 2:84, 3:82, 4:78, 5:66} | bán kính dùng được (đáy đảo thu nhỏ) |
| `HMAX` | {1:10, 2–5:12} | trần phòng cao nhất (không tính giếng trời) |
| `CH` | 4 | hành lang cao 4 |
| `CELL` | 4 | lưới hành lang: 3 khí + 1 tường |
| Vùng schem | x,z −88..88, Y −21..60 (177×82×177), gốc (−88,−21,−88) | |
| Vùng an toàn | r ≤ 90 và Y ≥ −55 + (r/100)³·55 + 3, ngoài vùng là `structure_void` | |

## Tầng mê cung (`MAZE_FL` = 3)
| Hằng số | Giá trị | Ý nghĩa |
|---|---|---|
| `MZ_P`, `MZ_CORE` | 8, 5 | lưới ô bước 8: lõi 5 (đường nằm trong lõi) + tường 3 (đá \| bedrock \| đá) |
| `MZ_WIDTH` | 3 / 4 / 5, trọng số 3:4:3 | độ rộng mỗi đoạn đường |
| `MZ_H`, `MZ_RH` | 5, 6 | cao đường đi / phòng trong mê cung |
| `MZ_BOT`, `MZ_TOP` | −4, +9 | khối bedrock của tầng: F−4..F+9 (Y14..27) |
| `MZ_MAT` | cobblestone / mossy_cobblestone / stone | lớp đá phủ mặt bedrock lộ |
| `MZ_ROOMS` | 7 | phòng combat nhỏ trong mê cung, 2×2 ô (13×13) hoặc 2×3 ô (13×21) |
| `MZ_TRAPS` | hố chông 5, mũi tên 5, mạng nhện 4 | đặt ở đoạn xuyên tường giữa 2 ô thường |
| `MZ_CHESTS` | 8 | rương ở ngõ cụt xa lối vào nhất |
| `MZ_LOOP` | 0.06 | tỉ lệ cạnh thêm (vòng lặp) ngoài cây DFS |

Mặt cắt đứng (F = 18): F−4..F−1 bedrock · F sàn đá · F+1..F+5 đường (phòng tới F+6) · lớp đá trần · bedrock tới F+9.

Bẫy:
| Loại | Dựng | Plugin |
|---|---|---|
| `pit` hố chông | hố sâu 2 phủ đoạn xuyên tường (dài 3), măng đá trên dripstone_block, chừa gờ rộng 1 | có thể thêm sát thương/hiệu ứng |
| `arrow` | `stone_pressure_plate` giữa đường + 2 `dispenser` trong 2 vách ở sàn+2 (chưa nối redstone, chưa có tên) | nạp tên / tự xử lý khi giẫm tấm |
| `web` | ~55% `cobweb` ở 2 lớp dưới trong đoạn dài 3 | có thể spawn nhện |

## Ánh sáng và vòm
| Phần | Thông số |
|---|---|
| Đuốc tường (`wall_torch`) | ở sàn+3, gắn vào khối đặc đầy đủ; cách nhau ≥ 7 (hành lang, phòng nhỏ) và ≥ 9 (mê cung) (`TORCH_GAP`) |
| Đèn trần | chỉ phòng lớn `CEIL_LIT` = Sảnh, Thủ Vệ, tài nguyên, hang đá, giếng trời; lưới 6 |
| Vòm cửa | mọi cửa phòng (trừ hang đá): 2 bậc thang úp ở 2 góc trên lỗ cửa → vòm; khung 2 bên + đá đỉnh vòm theo vật liệu phòng (`ARCH`, `ARCH_KIND`) |
| Vòm sườn hành lang | hành lang gạch thẳng, mỗi 2 khe ô (8 khối) một cặp `stone_brick_stairs` úp ở trần (`RIB_EVERY`) |

Kích thước phòng:
| Loại | Kích thước | Cao |
|---|---|---|
| Sảnh Chính | tròn Ø31, tâm (0,0) | 10 |
| Thủ Vệ Rễ | tròn Ø29, tâm (0,0) | 12 |
| Phòng Canh | 11–15 × 11–15 | 6–8 |
| Đấu Trường | tròn Ø13–17 | 8–12 |
| Hầm Lính | 9–13 × 14–19 | 5–6 |
| Hầm Rương | 5–7 × 5–7 | 5 |
| Hành Lang Bẫy | 5 × 15–21 | 5, hố giữa sâu 3, đáy măng đá |
| Phòng tài nguyên | 16–27 × 16–22 | 9–12 |
| Hang Đá | blob 16–24 × 14–20 | 9–12 |
| Trạm Nghỉ (checkpoint) | 11 × 11 | 6 |
| Trạm Dịch Chuyển | 9 × 9 | 7 |
| Thang xoắn | 9 × 9, xoắn r ≤ 3.8, cột tâm r < 1.3, 6 khối/vòng | xuyên 12–20 |
| Giếng Trời | tròn Ø19–23 | 25 (2 tầng) |

## Vị trí then chốt (seed 2026)
Bảng sinh từ `output/layer2_rooms.json`. Danh sách đầy đủ (kể cả bẫy và rương mê cung) ở `output/layer2_rooms.md`.

| Tầng | Tên | x | z | tâm | sàn / trần |
|---|---|---|---|---|---|
| T1 | Sảnh Chính | -15..15 | -15..15 | 0,0 | 46 / 57 |
| T1 | Thang Xoắn A ↓T2 | 69..77 | 1..9 | 73,5 | 46 / 53 |
| T1 | Thang Xoắn B ↓T2 | -73..-65 | 18..26 | -69,22 | 46 / 53 |
| T1→T2 | Giếng Trời | -35..-15 | 35..55 | -25,45 | ban công 46, đáy 30 |
| T1 | Vườn Rễ Cổ | 29..45 | -28..-6 | 37,-17 | 46 / 57 |
| T1 | Thư Khố | 45..62 | 17..37 | 54,27 | 46 / 57 |
| T2 | Thang Xoắn A ↓T3 | -3..5 | -76..-68 | 1,-72 | 30 / 37 |
| T2 | Thang Xoắn B ↓T3 | -39..-31 | -29..-21 | -35,-25 | 30 / 37 |
| T2 | Mỏ Quặng Sâu | 8..23 | -27..-6 | 16,-16 | 30 / 43 |
| T2 | Hồ Ngầm | 2..20 | 8..27 | 11,18 | 30 / 40 |
| T2 | Trạm Nghỉ (CP) | -25..-15 | 16..26 | -20,21 | 30 / 37 |
| T2 | Trạm Dịch Chuyển | 22..30 | -48..-40 | 26,-44 | 30 / 38 |
| T3 | Thang Xoắn A ↓T4 | 54..62 | 30..38 | 58,34 | 18 / 25 |
| T3 | Thang Xoắn B ↓T4 | -26..-18 | 54..62 | -22,58 | 18 / 25 |
| T4 | Thang Xoắn A ↓T5 | -21..-13 | -61..-53 | -17,-57 | -2 / 5 |
| T4 | Thang Xoắn B ↓T5 | -70..-62 | -11..-3 | -66,-7 | -2 / 5 |
| T4 | Hang Pha Lê | 15..37 | 17..34 | 26,26 | -2 / 11 |
| T4 | Vườn Nấm | -21..-4 | -37..-17 | -12,-27 | -2 / 10 |
| T4 | Trạm Nghỉ (CP) | -16..-6 | 18..28 | -11,23 | -2 / 5 |
| T4 | Trạm Dịch Chuyển | 10..18 | 44..52 | 14,48 | -2 / 6 |
| T5 | **Thủ Vệ Rễ** (pad boss tâm 0,−18,0 = lodestone) | -14..14 | -14..14 | 0,0 | -18 / -5 |
| T5 | Mỏ Cổ | -49..-24 | 6..22 | -36,14 | -18 / -7 |
| T5 | Trạm Nghỉ (CP) | 46..56 | -5..5 | 51,0 | -18 / -11 |

Khối đánh dấu trong phòng (plugin có thể quét để lấy vị trí):
| Khối | Vị trí |
|---|---|
| `lodestone` | tâm Trạm Nghỉ (sàn+1), và tâm Thủ Vệ (ở sàn) |
| `respawn_anchor` | tâm Trạm Dịch Chuyển (ở sàn) |
| `chest` | tâm Hầm Rương, ~40% ngõ cụt hành lang, và `MZ_CHESTS` ngõ cụt mê cung |
| `stone_pressure_plate` + `dispenser` | bẫy mũi tên trong mê cung (vị trí cũng có trong json `maze[].traps`) |

## Layer 3 — phòng boss "hang vòm lớn" (ĐANG DÙNG, `generate_boss_cavern.py` -> `sylphens_boss_cavern.schem`)
Hang không vừa trong hộp boss cũ (rộng 45) nên phình xuống dưới đáy đảo như một khối rễ đá: vẫn nằm trong barrier (r < 100, Y ≥ −63), không chạm tầng 5 (Y ≥ −21). Schem phủ trọn hộp boss cũ: phần hộp cũ nằm trong đảo thành deepslate, phần lòi ra dưới đáy đảo thành khí.

| Thành phần | Thông số | Hằng số |
|---|---|---|
| Lòng hang | tròn r46 (méo ±3 theo góc), nền khối Y−61 (đứng −60), tường đứng ~6 rồi cuốn vòm tới Y−25, trần gợn noise | `R`, `FL`, `APEX`, `WALL_H` |
| Vỏ | dày 6: lớp 1–3 deepslate tự nhiên (deepslate / cobbled / polished + quặng emerald, coal deepslate lấm tấm), lớp 4 bedrock (Y ≤ −24), lớp 5–6 deepslate; dưới nền là sàn barrier Y−64 | `SHELL` |
| Sàn tròn | r24, cao hơn nền 3 (mặt Y−58, đứng −57), thân deepslate bricks, viền chiseled / polished deepslate, mặt polished deepslate / tiles | `RP`, `PT` |
| Vết nứt lưới | 2 họ đường lượn sóng cách ~7; mỗi ô nứt sâu 6: blue concrete (Y−63) · green glass ×2 · lime glass ×3 (mặt Y−58); ~28% ô có `light[level=15]` ngay trên | `CRACK_GAP`, `CRACK_LAYERS` |
| Vòng triệu hồi | tâm (0,0): vòng nứt r8 (light 45%), vòng chiseled r5, 8 tia lime glass, tâm reinforced deepslate 3×3, 4 light quanh tâm | `CIRCLE_R` |
| Bậc thang | 4 hướng (Đ, T, N, B), rộng 7, 3 bậc polished deepslate stairs | |
| Gai trần | gai lớn ở tâm dài 17 (gốc r6.5), vòng 7 gai nghiêng tủa ra ở r10, ~38 gai rải khắp trần (dài 5–13, nghiêng ra ngoài dần); mũi 3 khối: green/lime glass bọc lõi verdant froglight; mũi thấp nhất cách mặt sàn tròn ≥ 17 | |
| Cột | 7 cột đồng hồ cát ở r36 (bỏ hướng nam), có gân kính xanh lá | `PILLAR_R`, `PILLAR_ANG` |
| Măng đá | 26 khối tự dựng ở vòng ngoài (cao 3–10), chừa lối tới 4 bậc thang và đường xuống | |
| Hang đến | phía nam, sàn Y−54, buồng elip 15×13 + đường hầm 5×5 ra vách; 4 soul lantern; điểm đến (0, -53, 55) yaw 180 | `ARR_DIR`, `AF` |
| Đường xuống | gấp khúc 2 chặng men vách (Y−54 → −59 → nền), lan can polished deepslate wall, bậc cobbled deepslate stairs | |
| Điểm boss | (0, -57, 0) tâm vòng triệu hồi | |

Ảnh: `images/boss_cavern.png`. Dữ liệu cho plugin: `output/boss_cavern.json`.

## Layer 3 — phương án 1 (không dùng nữa): phòng boss "sảnh fantasy" (`generate_boss_room.py` -> `sylphens_boss.schem`)
Hộp bedrock −23..23 × −58..−28 × −51..47 (giữ nguyên như trước). Lòng phòng x −21..21, z −48..45. Sàn khối Y−56 (người đứng Y−55; trước là −57, nâng 1 để có lòng sông/hồ mà không chạm vỏ bedrock).

| Thành phần | Vị trí / thông số | Hằng số |
|---|---|---|
| Trần vòm cuốn | tường thẳng tới Y−42, cuốn elip lên đỉnh Y−30; vòm calcite, sườn quartz_bricks ở z −42, −30, −18 và mỗi cột; đèn froglight giữa các sườn | `SPRING`, `APEX`, `RIB_Z` |
| 2 hàng cột | x ±13, z −6, 2, 10, 18, 26, 34; thân quartz_pillar 3×3, đế/đầu cột 5×5, dầm thạch anh ra tường; 4 lồng đèn treo dưới mỗi đầu cột | `COL_X`, `COL_Z` |
| Pad đến (cũng là điểm thoát) | (0, -55, 41) đứng; `lodestone` ở sàn, vòng crying_obsidian, 4 cột đèn | `PAD` |
| Thảm | đỏ x −1..1, viền vàng x ±2, từ pad tới chân bậc; thảm đỏ tiếp trên bệ tới tế đàn | |
| Sông nông | z −14..−10, sâu 1, cắt ngang hết sảnh; cầu x −4..4 có lan can cho thảm đi qua | `RIVER`, `BRIDGE_X` |
| Kênh + hồ | 2 kênh x ±16..17 dọc 2 bên bệ nối hồ sau bệ (x −17..17, z −48..−45) với sông | `CHAN_X`, `BASIN` |
| Thác | rộng x −5..5, nguồn trong khe tường bắc ở Y−34, rơi thẳng xuống hồ | `FALL` |
| Bệ | x −14..14, z −44..−19, mặt bệ Y−52 (đứng −51); bậc thang x −4..4 ở z −15..−18; 2 lửa trại ở 2 góc trước | `DAIS`, `STEP_X` |
| Tế đàn | tâm (0, −33): đế 5×5 polished blackstone, crying obsidian ở đỉnh, 4 cụm nến; vòng gilded blackstone r5 quanh | `ALTAR` |
| Tượng người lùn | x ±11, z −40, cao 9 trên bệ đá, nhìn về nam, cầm búa phía trong | `STATUES` |
| Điểm boss | (0, -51, -25) trên mặt bệ | `BOSS_SPAWN` |
| Cây cỏ | bồn rêu dọc 2 tường (azalea, dương xỉ, cỏ), 2 cây azalea ở góc nam, dây leo trên tường/cột/tường bắc, cave vines phát sáng rủ từ vòm, tán lá trên dầm, thảm rêu sát tường, lily pad/seagrass dưới nước | |
| Ánh sáng | 3 đèn chùm (z −2, 14, 30), lồng đèn dưới đầu cột, đuốc tường giữa các trụ, froglight trên vòm, nến + lửa trại trên bệ | `CHANDELIER_Z` |

Toàn bộ vị trí cho plugin ở `output/boss_room.json`. Ảnh xem trước: `images/boss_room.png`.
