# 02 — Thông số

Mọi giá trị khai báo ở `scripts/kv_model.py`. "floor" = Y khối sàn, người đứng floor+1.

## Cao độ
| Tên | Giá trị | Ghi chú |
|---|---|---|
| `TERRACE_Y` | 98 | thềm xuất hiện |
| `YARD_Y` | 120 | bãi mỏ, sàn cửa hang |
| `TUNNEL_END_Y` | 106 | sàn hang dẫn ở mép hố |
| `F[2]`, `F[3]`, `F[4]` | 72, 40, 8 | sàn 3 tầng mỏ |
| `CAV_H` | 26 | hang khai thác: khí cao nhất F+26 (98 / 66 / 34) |
| `SEAL1_MAX` | 101 | tấm dưới L1 = min(101, mặt đất thấp nhất 5×5 − 3) |
| `SEAL[2]`, `SEAL[3]`, `SEAL[4]` | 69, 37, 3 | tấm dưới tầng 2, 3, 4 |
| Mép đảo | r 104–117 | `edge_r`: noise + phình nam |
| Đáy đảo | −56 ở tâm → ~−7 sát mép | `bottom_y` |
| Đỉnh núi | Y197 | rìa bắc |
| Barrier | r 119.5–122, Y −64..319 | `BARRIER_R` |

## Schematic
| File | Dải Y | Kích thước | Gốc (`//paste -o`) |
|---|---|---|---|
| `kvelsgard_l5.schem` | −60..2 | 239×63×239 | −119, −60, −119 |
| `kvelsgard_l4.schem` | 3..36 | 239×34×239 | −119, 3, −119 |
| `kvelsgard_l3.schem` | 37..68 | 239×32×239 | −119, 37, −119 |
| `kvelsgard_l2.schem` | 69..101 | 239×33×239 | −119, 69, −119 |
| `kvelsgard_l1.schem` | 102..200 | 239×99×239 | −119, 102, −119 |

Ô ngoài mép đảo + 1.5 là `structure_void`; ô trong đảo (kể cả khí trên mặt núi) được ghi đè khi dán.

## Vị trí cố định
| Điểm | Tọa độ | |
|---|---|---|
| Điểm xuất hiện | (0, 99, 105), nhìn bắc | thềm, khối chiseled_stone_bricks |
| Cửa hang dẫn | (−4, 120, 30) | rộng 17, cao 13 |
| Hang dẫn | (−4,30) (−10,14) (6,−2) (12,−20) (−8,−34) (−12,−48) (0,−62) | |
| Hố tầng 1→2 | tâm (0, −62), r12, Y72..124 | gờ xoắn kết thúc ở góc 90° (nam) |
| Giếng tầng 2→3 | tâm (−22, 62), 9×9, Y41..75 | gờ ván 2 ô, 14 khối/vòng |
| Hố sụt tầng 3→4 | tâm (30, −56), r7, Y19..43 | thác trong khe (37, −53) |
| Hồ Rơi | tâm (30, −56), r9.5, nước Y5..8 | |
| Thác Hang Mạch Lộ | (24, 72) | |
| Bệ xuất hiện phòng boss | (0, −40, 50), nhìn bắc | đích teleport |
| Bàn triệu hồi | (0, −36, −36), nến ở Y−35 | trên bậc 2 (mặt Y−37) |
| Rãnh lửa soul | x −13..13, Y−36, z −40 | soul_fire trên soul_sand |
| Cổng triệu hồi | khối x −20..20, Y −35..−15, z −48..−44 | lòng cổng x −13..13, Y −29..−21; vành reinforced ở z −46 |
| Vỏ bedrock phòng boss | đáy Y−52 (r ≤ 68.2), vách r 66.5–68.2 | |

## Đường leo
| Phần | z | Y |
|---|---|---|
| Thềm | ≥ 101 | 98 |
| Nhánh 0 (đi tây) | 97–99 | 98 → 101 |
| Nhánh 1 (đi đông) | 93–95 | 101 → 109 |
| Nhánh 2 (đi tây) | 89–91 | 109 → 116 |
| Nhánh 3 (đi đông) | 85–87 | 116 → 120 tại x=0, phẳng 120 tới x=34 |
| Lan can | 100, 96, 92, 88 | cao độ nhánh cao hơn + tường thấp |
| Quay đầu | x −34..−31 hoặc 31..34 | phẳng |

Độ cao theo quãng đường `d` (0..180): `98 + floor(22·d/180)`.

## Phòng
Bảng đầy đủ (tọa độ thật, loại, ghi chú) ở `output/layout.json`; khai báo gốc ở `LAYERS` trong `kv_model.py` (`[x1, z1, x2, z2, cao, loại, tên, ghi chú]`, phủ x1..x2−1, z1..z2−1).
