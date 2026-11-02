# Kinh tế server — tài liệu thiết kế & đối chiếu

> ⚠ **Đã thiết kế lại từ đầu.** Giá vật phẩm xem [stat-standard.md](stat-standard.md). Thu nhập/giờ, quest, dungeon, guild rank xem [income.md](income.md). Các con số dưới đây là bản cũ, chỉ để tham khảo.

> Tài liệu sống. Mọi giá trong shop, thưởng quest, thưởng cấp Mạo hiểm và thưởng dungeon phải quy được về các tham số ở mục 2. Khi đổi một tham số, cập nhật mục **Lịch sử thay đổi** ở cuối file.

## 1. Nguyên tắc

1. **Tiền chỉ sinh ra từ 2 nguồn:** quest (bảng nhiệm vụ, quest cốt truyện, thưởng cấp Mạo hiểm) và dungeon. Không có NPC thu mua đồ. Trao đổi giữa người chơi chỉ **chuyển** tiền, không tạo thêm tiền.
2. **Giá đo bằng giờ chơi, không đo bằng chỉ số.** Mỗi món có một ngân sách "số giờ thu nhập" ở giai đoạn người chơi mua nó. Giá = số giờ × thu nhập/giờ của giai đoạn đó.
3. **Chỉ số neo theo vanilla** (xem `weapon-stat-budget-spec`). Giá mỗi điểm chỉ số tăng nhanh dần qua các tier. Đó là chủ ý: tier cao là mốc tiến trình, không phải món mua theo tỷ lệ.
4. **Mỗi đồng vào phải có chỗ ra.** Nguồn tiền định kỳ (quest hằng ngày) cần khoản tiêu định kỳ (thức ăn, giấy phép dungeon, sửa đồ…), không chỉ khoản mua một lần.

## 2. Tham số gốc

### 2.1 Giai đoạn và thu nhập/giờ (`I`)

"Giờ" = giờ chơi chủ động (đi quest/dungeon), không tính AFK hay xây đảo. Mốc gốc: **S1 = 700 tiền/giờ**. Các giai đoạn sau giữ tỷ lệ 1 : 2 : 3.6 : 4.8.

| Giai đoạn | Cấp Mạo hiểm | Giờ chơi tích luỹ (mục tiêu) | Tier đồ mua | `I` (tiền/giờ) | `E` (exp Mạo hiểm/giờ) |
|---|---|---|---|---|---|
| S1 — Tân binh | 1–5 | 0 → 3h | Recruit (đồng) | **700** | ~250 |
| S2 — Vệ binh | 6–15 | 3 → 15h | Guard (sắt) | **1 400** | ~450 |
| S3 — Azure | 16–30 | 15 → 50h | Azure (kim cương) | **2 500** | ~500 |
| S4 — Hắc thiết | 31–50 | 50 → 130h | Blackiron (netherite) | **3 400** | ~500 |

Thu nhập tích luỹ dự kiến: 2 100 (3h) · 18 900 (15h) · 106 400 (50h) · ~378 400 (130h).

### 2.2 Tỷ trọng nguồn thu (mục tiêu)

| Nguồn | Tỷ trọng `I` | Ghi chú |
|---|---|---|
| Quest bảng nhiệm vụ (reset theo chu kỳ) | ~55% | Nguồn chính, bị giới hạn bởi số quest trong pool mỗi chu kỳ |
| Dungeon (phần thưởng cuối / treasure) | ~35% | **Hiện chưa thưởng `money`**, xem mục 7 |
| Thưởng cấp Mạo hiểm | ~10% | Đề xuất ở mục 6 |
| Quest cốt truyện một lần | ngoài `I` | Thưởng một lần, tính như "bonus", không lặp |

## 3. Đường cong cấp Mạo hiểm (guild rank)

Nguồn: `quest_board.json` → `QuestLevelCurve`. Exp để lên từ cấp `n` lên `n+1` = `baseExp + increment × (n−1)` = `100 + 50(n−1)`.

| Cấp | Tổng exp cần | Giờ mục tiêu | Exp/giờ ngầm định |
|---|---|---|---|
| 5 | 700 | 3h | ~233 |
| 10 | 2 700 | 8h | ~400 |
| 15 | 5 950 | 15h | ~464 |
| 20 | 10 450 | 25h | ~450 |
| 25 | 16 200 | 37h | ~480 |
| 30 | 23 200 | 50h | ~540 |
| 35 | 31 450 | 67h | ~485 |
| 40 | 40 950 | 85h | ~530 |
| 45 | 51 700 | 107h | ~490 |
| 50 | 63 700 | 130h | ~520 |

→ Exp từ quest nên giữ ~250 exp/giờ ở S1 và ~450–500 exp/giờ từ S2 trở đi. Muốn rút ngắn hoặc kéo dài hành trình, chỉnh exp của quest, **không** chỉnh `baseExp`/`increment` (để bảng thưởng cấp ở mục 6 vẫn đúng mốc).

## 4. Công thức định giá

> Từ 2026-10-27, giá từng món trang bị (giáp, vũ khí, công cụ, enchant, gem) tính **theo chỉ số**, xem [item-pricing.md](item-pricing.md). Ngân sách kit ở 4.1 vẫn là mốc để hiệu chuẩn đơn giá. Các cách chia theo thỏi và hệ số loại ở 4.2–4.4 chỉ còn để tham khảo.

### 4.1 Ngân sách theo tier ("kit" = bộ giáp 4 món + 1 vũ khí chính)

| Tier | Giai đoạn mua | Ngân sách kit (giờ) | Giá kit = giờ × `I` |
|---|---|---|---|
| Recruit | S1 | ~1.15h | ~800 |
| Guard | S2 | ~1.45h | ~2 020 |
| Azure | S3 | ~2.75h | ~6 840 |
| Blackiron | S4 | ~7h (áo + kiếm) / ~14.6h (cả bộ + kiếm) | 24 100 / 49 600 |

Trong một kit: **bộ giáp ≈ 75–80%**, **vũ khí ≈ 20–25%**.

**Quy tắc làm tròn:** dưới 1 000 làm tròn tới bội số 5; từ 1 000 trở lên làm tròn tới bội số 50.

### 4.2 Giáp — chia theo số thỏi trong công thức vanilla

`Giá món = Giá bộ × số thỏi / 24`, với mũ 5 · áo 8 · quần 7 · giày 4.

| Tier | Giá bộ | Giá/thỏi | Mũ | Áo | Quần | Giày | Tổng DEF | Giá/điểm DEF |
|---|---|---|---|---|---|---|---|---|
| Recruit | 630 | 26 | 130 | 210 | 185 | 105 | 7.8 | 81 |
| Guard | 1 600 | 67 | 335 | 535 | 465 | 265 | 17.1 | 94 |
| Azure | 5 040 | 210 | 1 050 | 1 700 | 1 450 | 840 | 22.0 | 229 |
| Blackiron (Bulwark) | 38 250* | 1 594 | — | 12 750 | — | — | 10.9 (áo) | 1 170 |

\* Roland chỉ bán áo Bulwark. Giá bộ 38 250 là giá quy đổi, dùng nếu sau này bán thêm món.

Hệ số giá giữa các tier: ×2.5 → ×3.2 → ×7.6. Có thêm thuộc tính (Fire Protection, KB resistance, speed…) thì **không cộng giá riêng**: thuộc tính đã nằm trong ngân sách chỉ số của tier.

### 4.3 Vũ khí — hệ số theo loại (so với kiếm cùng tier)

| Loại | Hệ số | Recruit | Guard | Azure | Blackiron |
|---|---|---|---|---|---|
| Kiếm | 1.00 | 170 | 420 | 1 800 | 11 350 |
| Kiếm ngắn | ~0.92 | 155 | 390 | — | — |
| Rìu chiến | ~1.15–1.20 | — | 500 | 2 100 | — |
| Rìu cầm tay (hatchet) | ~1.05 (Blackiron 0.95) | 180 | 450 | — | 10 750 |

### 4.4 Công cụ — theo độ hữu dụng, không theo số thỏi

Tỷ lệ cuốc chim : rìu đốn : xẻng ≈ **1 : 0.75 : 0.5**.

| Tier | Cuốc | Rìu đốn | Xẻng | Giờ (cuốc) |
|---|---|---|---|---|
| Miner | 475 | 390 | 250 | 0.7h ở S1 |
| Azure | 2 500 | 1 950 | 1 250 | 1.0h ở S3 |
| Blackiron | 14 150 | 9 900 | 7 100 | 4.2h ở S4 |

### 4.5 Thức ăn (khoản tiêu định kỳ)

Giá/món ≈ **1.26 × (điểm đói + bão hoà)**, cộng thêm giá cho đồ có vàng. Bán theo stack 8/16.

| Món | Đói + bão hoà | Giá/stack | Giá/món |
|---|---|---|---|
| Bít tết ×16 | 20.8 | 450 | 28 |
| Sườn heo ×16 | 20.8 | 450 | 28 |
| Thịt cừu ×16 | 15.6 | 335 | 21 |
| Cá hồi ×16 | 15.6 | 315 | 20 |
| Gà ×16 | 13.2 | 270 | 17 |
| Cá tuyết ×16 | 11 | 200 | 12.5 |
| Bánh mì ×16 | 11 | 200 | 12.5 |
| Khoai nướng ×16 | 11 | 200 | 12.5 |
| Bánh bí ngô ×8 | 12.8 | 135 | 17 |
| Cà rốt vàng ×8 | 20.4 | 670 | 84 (đồ có vàng, cao hơn công thức) |
| Dưa hấu lấp lánh ×8 | — | 540 | 67 (nguyên liệu pha thuốc) |

Người chơi đánh liên tục ăn khoảng 2–4 bít tết/giờ, tức 55–110 tiền/giờ (~4–8% `I` ở S2). Khoản tiêu này **cố ý nhỏ**, chỉ để hút tiền đều đặn.

## 5. Bảng giá shop: hiện tại → đề xuất

Giá hiện tại được đặt theo mốc cũ (S1 = 250/giờ). Với `I` mới, giữ nguyên ngân sách giờ thì giá đề xuất ≈ giá cũ × 2.8.

**Roland (giáp)**

| Món | Hiện tại | Đề xuất | | Món | Hiện tại | Đề xuất |
|---|---|---|---|---|---|---|
| recruit_helmet | 45 | 130 | | guard_helmet | 120 | 335 |
| recruit_chestplate | 75 | 210 | | guard_chestplate | 190 | 535 |
| recruit_leggings | 65 | 185 | | guard_leggings | 165 | 465 |
| recruit_boots | 40 | 105 | | guard_boots | 100 | 265 |
| azure_helmet | 380 | 1 050 | | bulwark_chestplate | 4 500 | 12 750 |
| azure_chestplate | 600 | 1 700 | | | | |
| azure_leggings | 520 | 1 450 | | | | |
| azure_boots | 320 | 840 | | | | |

**Drake (vũ khí)**: recruit_sword 60→170 · recruit_shortsword 55→155 · recruit_hatchet 65→180 · guard_sword 150→420 · guard_shortsword 140→390 · guard_axe 180→500 · guard_hatchet 160→450 · azure_sword 650→1 800 · azure_axe 750→2 100 · blackiron_sword 4 000→11 350 · blackiron_hatchet 3 800→10 750.

**Barrett (công cụ)**: miner_pickaxe 170→475 · miner_felling_axe 140→390 · miner_shovel 90→250 · azure_pickaxe 900→2 500 · azure_felling_axe 700→1 950 · azure_shovel 450→1 250 · blackiron_pickaxe 5 000→14 150 · blackiron_felling_axe 3 500→9 900 · blackiron_shovel 2 500→7 100.

**Thức ăn**: xem bảng 4.5 (giá stack cũ × 2.8).

**Kiểm tra theo giờ:**

| Món | Giá đề xuất | Giai đoạn | Số giờ `I` | Đánh giá |
|---|---|---|---|---|
| Bộ Recruit + kiếm | 800 | S1 | 1.14h | ✅ |
| Bộ Guard + kiếm | 2 020 | S2 | 1.44h | ✅ |
| Bộ Azure + kiếm | 6 840 | S3 | 2.74h | ✅ |
| Áo Bulwark + kiếm Blackiron | 24 100 | S4 | 7.1h | ✅ |
| Cuốc Blackiron | 14 150 | S4 | 4.2h | ⚠ cao so với cuốc Azure (×5.7). Chấp nhận nếu coi đây là món "flex" |
| Thưởng cấp 50 hiện tại | 30 000 | S4 | 8.8h | ⚠ hơn cả áo Bulwark + kiếm Blackiron, xem mục 6 |

## 6. Thưởng cấp Mạo hiểm — đề xuất chỉnh

**Vấn đề:** tổng thưởng tiền hiện tại tới cấp 50 là **111 000**, bằng ~29% toàn bộ thu nhập dự kiến trong 130h, gấp đôi mục tiêu 10–15%. Thưởng phân bổ cũng lệch:
- **Đầu game quá thấp so với `I` mới:** cấp 2–3 chỉ cho 200–300, chưa tới nửa giờ thu nhập.
- **Cuối game quá cao:** cấp 45–50 cho 20 000–30 000, bằng 6–9 giờ chơi ở S4.

**Quy tắc đề xuất:** thưởng ở mốc `L` ≈ **15% thu nhập kiếm được từ mốc trước tới `L`**. Các mốc đầu (2, 3, 5) cho thưởng cố định để onboarding. Cấp 50 có thêm phần thưởng "capstone".

| Cấp | Hiện tại | Thu nhập trong khoảng | **Đề xuất** | % khoảng | Tương đương |
|---|---|---|---|---|---|
| 2 | 200 | ~280 | **300** | onboarding | mũ + giày Recruit |
| 3 | 300 | ~420 | **400** | onboarding | áo Recruit + kiếm |
| 5 | 1 000 | ~1 400 | **850** | onboarding | ~đủ kit Recruit |
| 10 | 2 500 | 7 000 | **1 000** | 14% | mũ + áo Guard |
| 15 | 4 000 | 9 800 | **1 500** | 15% | |
| 20 | 6 000 | 25 000 | **3 750** | 15% | áo + quần Azure |
| 25 | 8 000 | 30 000 | **4 500** | 15% | |
| 30 | 10 000 | 32 500 | **4 900** | 15% | |
| 35 | 13 000 | 57 800 | **8 700** | 15% | |
| 40 | 16 000 | 61 200 | **9 200** | 15% | |
| 45 | 20 000 | 74 800 | **11 200** | 15% | |
| 50 | 30 000 | 78 200 | **14 000** | 18% (capstone) | ~áo Bulwark |
| **Tổng** | **111 000** | ~378 400 | **60 300** | **~16%** | |

Bản JSON cho `quest_board.json → level.rewards`:

```json
"rewards": {
  "2":  [ { "type": "money", "amount": 300 } ],
  "3":  [ { "type": "money", "amount": 400 } ],
  "5":  [ { "type": "money", "amount": 850 } ],
  "10": [ { "type": "money", "amount": 1000 } ],
  "15": [ { "type": "money", "amount": 1500 } ],
  "20": [ { "type": "money", "amount": 3750 } ],
  "25": [ { "type": "money", "amount": 4500 } ],
  "30": [ { "type": "money", "amount": 4900 } ],
  "35": [ { "type": "money", "amount": 8700 } ],
  "40": [ { "type": "money", "amount": 9200 } ],
  "45": [ { "type": "money", "amount": 11200 } ],
  "50": [ { "type": "money", "amount": 14000 } ]
}
```

**Gợi ý thêm (không bắt buộc):** để các mốc 10/20/30 "đáng nhớ" hơn mà không thêm tiền, nên thưởng vật phẩm hoặc mở khoá (gem, giấy phép dungeon miễn phí, quyền mua tier mới) thay vì tăng tiền.

## 7. Quest bảng nhiệm vụ & dungeon — định mức thưởng

### 7.1 Quest bảng nhiệm vụ

Thưởng 1 quest = `thời lượng (phút) / 60 × I × 0.55`. Exp = `thời lượng / 60 × E / 0.55` (exp chỉ đến từ quest, nên dồn hết vào phần thời gian làm quest).

| Độ khó | Thời lượng | S1 tiền / exp | S2 | S3 | S4 |
|---|---|---|---|---|---|
| Dễ | ~10 phút | 65 / 75 | 130 / 135 | 230 / 150 | 310 / 150 |
| Thường | ~20 phút | 130 / 150 | 255 / 275 | 460 / 300 | 625 / 300 |
| Khó | ~40 phút | 255 / 300 | 515 / 550 | 915 / 600 | 1 250 / 600 |

Thời lượng tính cả thời gian di chuyển. Dùng `requirements` để quest chỉ xuất hiện đúng giai đoạn.

**Giới hạn theo chu kỳ:** một quest đã hoàn thành sẽ không quay lại trong cùng chu kỳ reset (`ResetSchedule`). Vì vậy thu nhập từ bảng/ngày bị chặn bởi **số quest trong pool cho mỗi giai đoạn**. Pool hằng ngày (`P1D`) nên có đủ quest cho ~2h chơi, tức khoảng 6–8 quest Thường mỗi giai đoạn. Chơi quá mức đó thì thu nhập chuyển sang dungeon. Đây là van chống cày.

> ⚠ Hiện tại `server_data/quests` **chưa có quest nào có trường `board`**, nên pool bảng nhiệm vụ đang trống. Thưởng tiền của các quest cốt truyện hiện có (200 / 1 000) cũng nên xem lại theo `I` mới.

### 7.2 Dungeon

- **Hiện trạng:** phần thưởng dungeon chỉ là loot table (`lootTableId`) và treasure room, chưa trả `money`.
- **Đề xuất:** một lượt dungeon dài `t` phút trả `t/60 × I × 0.35 / 0.45` (giả sử ~45% thời gian chơi nằm trong dungeon). Ví dụ một lượt 30 phút ở S3: 0.5 × 2 500 × 0.78 ≈ **970**.
- **Giấy phép dungeon (Rowan):** khoản tiêu chính, đặt bằng **~20–25% thưởng tiền dự kiến** của lượt đó (ví dụ trên: ~200–250). Như vậy thua dungeon vẫn mất tiền thật, còn thắng thì lãi rõ.
- Vật phẩm tiền vật lý (`copper_ingot_currency`, `copper_block_currency`) cần chốt: có quy đổi ra `money` không? Nếu có, tỷ giá phải tính vào tỷ trọng 35% ở trên.

## 8. Khoản tiêu (sink) — hiện trạng & cần bổ sung

| Khoản tiêu | Loại | Hiện trạng |
|---|---|---|
| Shop trang bị (Roland, Drake, Barrett) | một lần / tier | ✅ |
| Thức ăn | định kỳ, nhỏ | ✅ |
| Giấy phép dungeon | định kỳ, lớn | NPC Rowan chưa có action |
| Sửa đồ (Blacksmith) | định kỳ | cần kiểm tra xem có tốn tiền không |
| Phù phép / thuốc (Aldric, Elric, Magnus) | định kỳ | NPC chưa có shop |

Sau cấp ~30, người chơi đã mua xong đồ Azure, nên thu nhập S4 (~3 400/h) cần sink định kỳ ≥ 40–50% để không lạm phát. Ứng viên: giấy phép dungeon tier cao, phù phép, thuốc, vật phẩm trang trí đảo.

## 9. Khoảng trống trong code

- **Chưa có requirement theo cấp Mạo hiểm.** `"type": "level"` (`LevelRequirement`) kiểm tra **cấp đảo**, không phải cấp bảng nhiệm vụ. Muốn khoá tier shop theo guild rank (ví dụ Azure cần cấp 15, Blackiron cần cấp 30) thì cần thêm một requirement mới (ví dụ `quest_level`).
- `TradeOffer` đã hỗ trợ `requirements`, nên sau khi có requirement trên thì chỉ cần thêm vào JSON shop.
- `MoneyResult` (NPC trả tiền) có sẵn nhưng theo nguyên tắc 1 thì **không dùng** để thu mua đồ.

## 10. Checklist khi thêm/chỉnh một món có giá

1. Món thuộc tier nào, giai đoạn mua là gì?
2. Ngân sách giờ của món = phần của nó trong kit (giáp theo thỏi, vũ khí theo hệ số loại, công cụ theo tỷ lệ 1 : 0.75 : 0.5).
3. Giá = giờ × `I` của giai đoạn, rồi làm tròn.
4. So "giá/điểm chỉ số" với các món cùng tier: lệch quá ±20% thì xem lại chỉ số trước khi xem lại giá.
5. Nếu món mở ra một nguồn thu mới (công cụ đào nhanh hơn, vũ khí clear dungeon nhanh hơn), kiểm tra lại `I` của giai đoạn sau.
6. Ghi vào **Lịch sử thay đổi**.

## Lịch sử thay đổi

| Ngày | Thay đổi |
|---|---|
| 2026-10-27 | Tạo tài liệu. Suy ra công thức từ shop Roland/Drake/Barrett/thức ăn. `I` ban đầu 250/500/900/1 200 (suy ngược từ giá shop). |
| 2026-10-27 | Nâng mốc thu nhập: S1 = 700/giờ, giữ tỷ lệ giai đoạn → 700/1 400/2 500/3 400. Giá shop đề xuất ≈ giá cũ × 2.8 (giữ nguyên ngân sách giờ). Thưởng cấp Mạo hiểm đề xuất 60 300 (từ 111 000). Thưởng quest/dungeon tính lại theo `I` mới. |
| 2026-10-27 | Giá trang bị chuyển sang tính theo chỉ số ([item-pricing.md](item-pricing.md)). Ngân sách kit ở 4.1 giữ nguyên làm mốc. |
