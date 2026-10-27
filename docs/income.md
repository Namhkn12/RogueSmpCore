# Thu nhập người chơi — dòng tiền, quest, dungeon, guild rank

> Xây trên [stat-standard.md](stat-standard.md) (bản vị chỉ số). Thay thế mục 2, 3, 6, 7 của [economy.md](economy.md).

**Nhắc lại "bậc" (B1–B8):** bậc là các nấc tiến trình của người chơi. B1 ≈ đồ gỗ, B2 ≈ đá/đồng, B3 ≈ sắt, B4 ≈ kim cương, B5 ≈ netherite, B6–B8 là end game (mỗi bậc mạnh hơn +5%). Bậc của một món suy ra từ chỉ số của nó.

## 1. Hướng đi của server (đầu vào của mô hình)

- **Trang bị do người chơi tự chế tạo** (vanilla plus). Người chơi tự enchant, tự khảm gem, chọn vũ khí theo class. Tiền **không** dùng để mua trang bị.
- **Tài nguyên vanilla chỉ đủ làm đồ bậc đầu (B1–B3).** Từ B4 trở lên phải dùng **quặng cao cấp**, chỉ lấy được qua 3 đường:
  1. **Máy móc**: đường chính, tốn nhiên liệu.
  2. **Loot**: ít.
  3. **Villager**: chỉ bán quặng bậc đầu. Quặng cao cấp thì bán với giá cắt cổ.
- **Nguyên liệu thuốc** phải mua (loot ít).
- **Thức ăn** tự trồng/nuôi được, nhưng đảo giới hạn entity và redstone phần lớn bị tắt, nên vẫn phải mua một phần.

→ **Tiền là "giá thành sản xuất".** Người chơi kiếm tiền từ quest/dungeon, rồi đổi tiền thành nhiên liệu → quặng → trang bị, cùng với thuốc và thức ăn để chiến đấu.

## 2. Dòng tiền

```
            NGUỒN TIỀN                                KHOẢN TIÊU
  Quest bảng nhiệm vụ  55% ─┐               ┌─► Nhiên liệu máy → quặng cao cấp   (chính)
  Dungeon (sau phí)    35% ─┼─► Ví người chơi ┼─► Nguyên liệu thuốc
  Guild rank           10% ─┘      ▲    │     ├─► Thức ăn (phần không tự nuôi được)
                                   │    │     ├─► Quặng villager (đường tắt, đắt)
                 Trao đổi giữa người chơi    ├─► Đầu tư xưởng (lõi máy, xúc tác) B1–B3
                 (chỉ chuyển tiền, không tạo thêm) └─► Giấy phép dungeon
```

Server không có NPC thu mua đồ. Chế tạo, enchant và khảm gem không tốn tiền trực tiếp, mà tốn **nguyên liệu**, và nguyên liệu cao cấp thì tốn **nhiên liệu**.

## 3. Chuỗi giá trị: quặng và nhiên liệu

### 3.1 Giá thành quặng neo theo bản vị chỉ số

Món đồ bậc b có giá trị ĐSM (stat-standard mục 5.2). Một bộ "kit + cuốc" tốn khoảng **29 thỏi** (24 giáp + 2 kiếm + 3 cuốc), giống vanilla.

```
Giá trị 1 thỏi bậc b       = (Giá kit + Giá cuốc) / 29
Giá thành qua máy          = 70% giá trị         (30% còn lại là công sức người chơi: đào, chế tạo, thời gian)
Giá villager (quặng cao)   = 3 × giá thành máy    ("giá cắt cổ": đường tắt cho người có tiền)
Giá villager (quặng đầu)   = 1 × giá trị          (chỉ quặng vanilla bậc đầu)
```

| Bậc quặng | Nguồn chính | Giá trị/thỏi | **Giá thành qua máy** | Villager bán |
|---|---|---|---|---|
| B1 (gỗ, đá) | tự khai thác | 6 | — | 5 |
| B2 (đồng, vàng) | tự khai thác | 27 | — | 25 |
| B3 (sắt) | tự khai thác | 93 | — | 95 |
| B4 | **máy** | 310 | **217** | 650 |
| B5 | **máy** | 1 014 | **710** | 2 150 |
| B6 | **máy** | 2 184 | **1 529** | 4 600 |
| B7 | **máy** | 4 698 | **3 289** | 9 850 |
| B8 | **máy** | 10 017 | **7 012** | 21 000 |

Loot cung cấp khoảng **15%** số quặng cao cấp cần dùng.

### 3.1b Trang bị bán sẵn ở NPC (hub)

Trang bị chủ yếu do người chơi chế tạo. NPC chỉ bán vài món làm **đường tắt**, giá = giá trị bản vị chỉ số × hệ số:

| Cấp hàng | Bậc | Hệ số | Lý do |
|---|---|---|---|
| Cấp thấp | B1–B2 | ×1.0 | Giúp người mới vào nhanh. Tự làm cũng gần như miễn phí |
| Cấp trung | B3–B4 | ×1.25 | Tiện hơn tự làm một chút, nhưng chế tạo vẫn rẻ hơn |
| Cấp cao | B5 | ×2.0 | Tự làm qua máy chỉ tốn ~0.9× giá trị. Mua sẵn đắt gấp ~2 |

Mỗi NPC bán 1–2 món cấp thấp, 2 món cấp trung, 1 món cấp cao. Giáp tính theo bộ, công cụ theo bộ cuốc/rìu đốn/xẻng.

### 3.2 Bản vị năng lượng

Một giá chung cho mọi loại nhiên liệu: **1 000 năng lượng (1 kNL) = 2 tiền.** Nhiên liệu cao cấp chỉ khác ở **mật độ** (nhiều NL hơn mỗi vật phẩm, đốt nhanh hơn), không khác giá mỗi NL. Nhờ vậy giá quặng luôn tính được thẳng từ năng lượng.

Đề xuất bậc thang nhiên liệu (`energy_source`: NL/tick × burn ticks):

| Nhiên liệu | Nguồn | NL/tick | Burn ticks | Tổng NL | Giá |
|---|---|---|---|---|---|
| Than cốc | tự làm (máy đào than → coke oven) | 5 | 1 600 | 8 kNL | tự làm, bị giới hạn bởi sản lượng than |
| Lõi năng lượng I | NPC | 10 | 5 000 | 50 kNL | **100** |
| Lõi năng lượng II | NPC | 20 | 10 000 | 200 kNL | **400** |
| Lõi năng lượng III | NPC (cần nâng cấp lò) | 40 | 25 000 | 1 000 kNL | **2 000** |

Than cốc tự làm cho phép người chơi B3–B4 chạy máy gần như miễn phí ở quy mô nhỏ. Muốn chạy nhanh hoặc chạy quặng cao cấp thì phải mua lõi.

### 3.3 Thông số máy tạo quặng (đề xuất)

```
NL mỗi quặng = Giá thành máy / 2 tiền × 1 000
Thời gian đào = NL mỗi quặng / NL mỗi tick
```

`arcane_reactor` hiện truyền tối đa **20 NL/tick** (`transfer_rate`), nên NL/tick của máy được giữ ≤ 20.

| Máy quặng | `energy_per_tick` | NL mỗi quặng | `break_delay` | Quặng/giờ | Tiền nhiên liệu/giờ (1 máy chạy liên tục) |
|---|---|---|---|---|---|
| B4 | 5 | 108 500 | 21 700 ticks (18 phút) | 3.3 | 720 |
| B5 | 10 | 355 000 | 35 500 ticks (30 phút) | 2.0 | 1 440 |
| B6 | 15 | 764 500 | 51 000 ticks (42 phút) | 1.4 | 2 160 |
| B7 | 20 | 1 644 500 | 82 200 ticks (68 phút) | 0.9 | 2 880 |
| B8 | 20 | 3 506 000 | 175 300 ticks (2.4 giờ) | 0.4 | 2 880 |

Người chơi ở bậc b cần khoảng **1–1.5 máy** quặng bậc b+1 chạy liên tục để lên bậc đúng nhịp ở mục 5. Chất xúc tác (`catalyst_types`: pure, volatile) và module nên **đổi hình dạng** sản lượng (nhanh hơn nhưng tốn NL hơn, tăng tỷ lệ quặng hiếm…), chứ không giảm giá thành mỗi quặng quá ~20%. Nếu không, bản vị sẽ bị phá.

## 4. Thuốc và thức ăn

**Thuốc:** giá nguyên liệu = giá thuốc theo stat-standard mục 8 ("dùng 1 giờ liên tục = giá vĩnh viễn số chỉ số đó"). Mỗi nguyên liệu có giá bằng phần ĐSM nó đóng góp. Mô hình giả định một thuốc trung bình +6 ĐSM, dùng trong:

| Bậc | B1 | B2 | B3 | B4–B8 |
|---|---|---|---|---|
| % thời gian dùng thuốc | 0% | 10% | 30% | 35% (dungeon, boss, quest khó) |

**Thức ăn:** giá NPC cố định, **2 tiền mỗi điểm (đói + bão hoà)**. Ví dụ bít tết ≈ 40, bánh mì ≈ 22, cà rốt vàng ≈ 60. Chiến đấu liên tục tiêu ~60 điểm/giờ. Tự nuôi được khoảng một nửa, nên **mua ~60 tiền/giờ**. Khoản này đáng kể ở B1 (11% thu nhập) và gần như không đáng kể từ B4.

## 5. Thu nhập mỗi giờ và nhịp tiến trình

```
Chi tiêu để lên bậc b+1 = 29 thỏi × giá thành máy(b+1) × (1 − 15% loot) × 1.3 (enchant/gem/nâng cấp)
                        + đầu tư xưởng (B1–B3)
Giờ ở bậc b             = Chi tiêu lên bậc b+1 / (I(b) − thuốc − thức ăn)
```

Lên B2–B3 chỉ cần quặng vanilla. Tiền ở giai đoạn này dùng cho **đường tắt** (mua quặng villager, 20% giá trị kit) và **đầu tư xưởng**: tổng 6 000 cho lõi máy, nhiên liệu dự trữ và xúc tác, chia ra 600 / 1 800 / 3 600 ở B1 / B2 / B3. Nhờ vậy khi chạm B4, người chơi đã có xưởng chạy.

| Bậc | **Thu nhập/giờ I** | Nhiên liệu, quặng, xưởng | Thuốc | Thức ăn | Giờ ở bậc | Vào bậc lúc | Cấp Mạo hiểm |
|---|---|---|---|---|---|---|---|
| B1 | **550** | 490 (89%) | 0 | 60 (11%) | 1.5h | 0h | 1–3 |
| B2 | **800** | 725 (91%) | 15 (2%) | 60 (8%) | 3.2h | 1.5h | 4–7 |
| B3 | **1 500** | 1 330 (89%) | 110 (7%) | 60 (4%) | 7.9h | 4.8h | 8–13 |
| B4 | **2 300** | 1 925 (84%) | 315 (14%) | 60 (3%) | 11.8h | 12.7h | 14–20 |
| B5 | **3 300** | 2 400 (73%) | 840 (25%) | 60 (2%) | 20.4h | 24.5h | 21–28 |
| B6 | **5 000** | 3 260 (65%) | 1 680 (34%) | 60 (1%) | 32.3h | 44.9h | 29–38 |
| B7 | **7 500** | 4 080 (54%) | 3 360 (45%) | 60 (1%) | 55.1h | 77.3h | 39–50 |
| B8 | **11 000** | gem/enchant/nâng cấp | 6 720 (61%) | 60 | không giới hạn | 132h | 50 |

- Người mới tới bậc trần: **~132 giờ chơi chủ động**. Cấp Mạo hiểm 50 đạt ở ~130h.
- **Nhiên liệu là khoản tiêu chính** cho tới B6. Từ B6, thuốc tăng dần, vì người chơi end game đánh nội dung khó hơn và dùng thuốc nhiều hơn.
- **Ở B8 không còn bậc để lên.** Tiền đi vào thuốc, gem/enchant tốt hơn và (nếu có) vật phẩm trang trí. Nên có thêm sink end game để khỏi lạm phát.

### 5.1 Một giờ chơi gồm những gì

Mô hình: 30 phút quest + 24 phút dungeon + 6 phút di chuyển, chế tạo, quản lý xưởng. Máy chạy song song trong lúc người chơi đi quest.

| Bậc | Quest | Dungeon (gộp) | − Giấy phép | Guild rank | **Tổng = I** | Exp Mạo hiểm/giờ |
|---|---|---|---|---|---|---|
| B1 | 300 | 240 | −50 | 55 | **550** | 250 |
| B2 | 440 | 350 | −70 | 80 | **800** | 350 |
| B3 | 825 | 660 | −130 | 150 | **1 500** | 450 |
| B4 | 1 265 | 1 010 | −200 | 230 | **2 300** | 500 |
| B5 | 1 815 | 1 450 | −290 | 330 | **3 300** | 500 |
| B6 | 2 750 | 2 200 | −440 | 500 | **5 000** | 500 |
| B7 | 4 125 | 3 300 | −660 | 750 | **7 500** | 500 |
| B8 | 6 050 | 4 840 | −970 | 1 100 | **11 000** | 500 |

## 6. Quest — 5 cấp độ khó

| Cấp | Tên | Thời lượng chuẩn | Hệ số rủi ro f | Đặc điểm |
|---|---|---|---|---|
| **I** | Việc vặt | ~5 phút | 0.9 | Giao đồ, nói chuyện, nhặt vài vật phẩm |
| **II** | Thường | ~10 phút | 1.0 | Diệt quái thường, thu thập ở vùng an toàn |
| **III** | Khó | ~20 phút | 1.1 | Quái đông/khó, vùng nguy hiểm, có thể chết |
| **IV** | Rất khó | ~35 phút | 1.25 | Elite, chuỗi mục tiêu, nên đi nhóm |
| **V** | Huyền thoại | ~60 phút | 1.5 | Boss, vùng cao nhất của bậc, cần nhóm và đồ đủ |

```
Tiền = thời lượng/60 × I(bậc quest) × f
Exp  = thời lượng/60 × E(bậc quest) × 1.45 × f
```

**Thưởng (tiền / exp Mạo hiểm):**

| Cấp | B1 | B2 | B3 | B4 | B5 | B6 | B7 | B8 |
|---|---|---|---|---|---|---|---|---|
| **I** | 40 / 25 | 60 / 40 | 110 / 50 | 170 / 55 | 250 / 55 | 375 / 55 | 560 / 55 | 825 / 55 |
| **II** | 90 / 60 | 135 / 85 | 250 / 110 | 385 / 120 | 550 / 120 | 835 / 120 | 1 250 / 120 | 1 850 / 120 |
| **III** | 200 / 135 | 295 / 185 | 550 / 240 | 845 / 265 | 1 200 / 265 | 1 850 / 265 | 2 750 / 265 | 4 050 / 265 |
| **IV** | 400 / 265 | 585 / 370 | 1 100 / 475 | 1 700 / 530 | 2 400 / 530 | 3 650 / 530 | 5 450 / 530 | 8 000 / 530 |
| **V** | 825 / 545 | 1 200 / 760 | 2 250 / 980 | 3 450 / 1 100 | 4 950 / 1 100 | 7 500 / 1 100 | 11 250 / 1 100 | 16 500 / 1 100 |

- Thời lượng tính cả di chuyển, cho người chơi có đồ đúng bậc. Bậc quest = bậc nội dung, dùng `requirements` để quest chỉ hiện đúng giai đoạn.
- **Cấp khó theo bậc:** B1 có I–III (bậc này chỉ ~1.5h), B2 có I–IV, từ B3 có đủ I–V.
- **Pool mỗi chu kỳ ngày:** cần 8–10 quest cùng bậc. Gợi ý từ B3: 1×I, 3×II, 3×III, 2×IV, và 1×V reset theo tuần. Hết quest thì chỉ còn dungeon, thu nhập còn ~0.9 × I. Đây là van chống cày.
- **Quest cốt truyện** (làm một lần): × 1.5 so với bảng, tổng không quá 10% thu nhập của bậc đó.
- **Phần thưởng vật phẩm:** quest có thể thưởng quặng cao cấp thay cho một phần tiền. Quy đổi theo **giá thành máy** ở mục 3.1. Ví dụ quest IV ở B5 = 2 400 tiền, có thể đổi thành 900 tiền + 1 thỏi quặng B6 (≈ 1 500 theo giá thành máy).

## 7. Dungeon

```
Thưởng gộp   = thời lượng/60 × I(bậc) × 1.1      (mỗi thành viên nhận đủ)
Giấy phép    = 20% thưởng gộp
Exp Mạo hiểm = thời lượng/60 × E × 0.5
Thất bại     = 30% thưởng gộp theo số phòng đã qua, mất giấy phép
```

Mỗi ô ghi: thưởng gộp / giấy phép / exp.

| Loại | B1 | B2 | B3 | B4 | B5 | B6 | B7 | B8 |
|---|---|---|---|---|---|---|---|---|
| **Ngắn** 15′ | 150 / 30 / 30 | 220 / 45 / 45 | 415 / 85 / 55 | 630 / 125 / 60 | 910 / 180 / 60 | 1 400 / 275 / 60 | 2 050 / 410 / 60 | 3 050 / 605 / 60 |
| **Vừa** 25′ | 250 / 50 / 50 | 365 / 75 / 75 | 690 / 140 / 95 | 1 050 / 210 / 105 | 1 500 / 305 / 105 | 2 300 / 460 / 105 | 3 450 / 690 / 105 | 5 050 / 1 000 / 105 |
| **Dài** 40′ (boss) | 405 / 80 / 85 | 585 / 115 / 115 | 1 100 / 220 / 150 | 1 700 / 335 / 165 | 2 400 / 485 / 165 | 3 650 / 735 / 165 | 5 500 / 1 100 / 165 | 8 050 / 1 600 / 165 |

- Thời lượng tính cho **nhóm 2–4 người** có đồ đúng bậc. Đi một mình chậm hơn, nên lời ít hơn mỗi giờ.
- **Loot dungeon** là một trong 3 nguồn quặng cao cấp (mục 3.1). Tổng quặng từ loot (dungeon + quest + mob) nên giữ quanh **15%** nhu cầu. Nếu tăng tỷ lệ rơi quặng, hãy giảm hệ số 1.1 tương ứng, theo giá thành máy.
- Thắng lãi 80% thưởng gộp. Thua: nhận 30%, mất phí 20%, còn lãi 10%.

## 8. Guild rank (cấp Mạo hiểm)

Đường cong exp giữ như `quest_board.json`: `100 + 50 × (cấp − 1)`, tổng 63 700 exp tới cấp 50.

**Quy tắc:** thưởng khi lên cấp L = **10% thu nhập** kiếm được trong lúc cày từ L−1 lên L. Mọi cấp đều có thưởng.

| Cấp | Thưởng | Cấp | Thưởng | Cấp | Thưởng | Cấp | Thưởng | Cấp | Thưởng |
|---|---|---|---|---|---|---|---|---|---|
| 2 | 20 | 12 | 200 | 22 | 725 | 32 | 1 600 | 42 | 3 150 |
| 3 | 35 | 13 | 215 | 23 | 760 | 33 | 1 650 | 43 | 3 200 |
| 4 | 45 | 14 | 250 | 24 | 790 | 34 | 1 700 | 44 | 3 300 |
| 5 | 55 | 15 | 345 | 25 | 825 | 35 | 1 750 | 45 | 3 400 |
| 6 | 70 | 16 | 370 | 26 | 860 | 36 | 1 800 | 46 | 3 450 |
| 7 | 80 | 17 | 390 | 27 | 890 | 37 | 1 850 | 47 | 3 500 |
| 8 | 115 | 18 | 415 | 28 | 925 | 38 | 1 900 | 48 | 3 600 |
| 9 | 150 | 19 | 435 | 29 | 1 150 | 39 | 2 750 | 49 | 3 700 |
| 10 | 165 | 20 | 460 | 30 | 1 500 | 40 | 3 000 | 50 | 3 750 |
| 11 | 185 | 21 | 585 | 31 | 1 550 | 41 | 3 100 | | |

**Tổng: 66 710.** Các bước nhảy ở cấp 8, 14, 21, 29, 39 là lúc người chơi sang bậc mới.

**Cấp mốc** (5, 10, 15…): nên thưởng thêm **vật phẩm thay vì tiền**, như lõi năng lượng, chất xúc tác, quặng bậc kế tiếp, giấy phép dungeon miễn phí. Đây đúng là thứ người chơi đang cần mua, nên có cảm giác giá trị cao mà không phá tỷ lệ 10%.

**Giới hạn nhận quest** (`acceptLimits`): cấp 1 → 2 · cấp 8 → 3 · cấp 14 → 4 · cấp 21 → 5 · cấp 29 → 6.

<details>
<summary>JSON cho <code>quest_board.json → level.rewards</code></summary>

```json
"rewards": {
  "2": [{"type": "money", "amount": 20}], "3": [{"type": "money", "amount": 35}], "4": [{"type": "money", "amount": 45}],
  "5": [{"type": "money", "amount": 55}], "6": [{"type": "money", "amount": 70}], "7": [{"type": "money", "amount": 80}],
  "8": [{"type": "money", "amount": 115}], "9": [{"type": "money", "amount": 150}], "10": [{"type": "money", "amount": 165}],
  "11": [{"type": "money", "amount": 185}], "12": [{"type": "money", "amount": 200}], "13": [{"type": "money", "amount": 215}],
  "14": [{"type": "money", "amount": 250}], "15": [{"type": "money", "amount": 345}], "16": [{"type": "money", "amount": 370}],
  "17": [{"type": "money", "amount": 390}], "18": [{"type": "money", "amount": 415}], "19": [{"type": "money", "amount": 435}],
  "20": [{"type": "money", "amount": 460}], "21": [{"type": "money", "amount": 585}], "22": [{"type": "money", "amount": 725}],
  "23": [{"type": "money", "amount": 760}], "24": [{"type": "money", "amount": 790}], "25": [{"type": "money", "amount": 825}],
  "26": [{"type": "money", "amount": 860}], "27": [{"type": "money", "amount": 890}], "28": [{"type": "money", "amount": 925}],
  "29": [{"type": "money", "amount": 1150}], "30": [{"type": "money", "amount": 1500}], "31": [{"type": "money", "amount": 1550}],
  "32": [{"type": "money", "amount": 1600}], "33": [{"type": "money", "amount": 1650}], "34": [{"type": "money", "amount": 1700}],
  "35": [{"type": "money", "amount": 1750}], "36": [{"type": "money", "amount": 1800}], "37": [{"type": "money", "amount": 1850}],
  "38": [{"type": "money", "amount": 1900}], "39": [{"type": "money", "amount": 2750}], "40": [{"type": "money", "amount": 3000}],
  "41": [{"type": "money", "amount": 3100}], "42": [{"type": "money", "amount": 3150}], "43": [{"type": "money", "amount": 3200}],
  "44": [{"type": "money", "amount": 3300}], "45": [{"type": "money", "amount": 3400}], "46": [{"type": "money", "amount": 3450}],
  "47": [{"type": "money", "amount": 3500}], "48": [{"type": "money", "amount": 3600}], "49": [{"type": "money", "amount": 3700}],
  "50": [{"type": "money", "amount": 3750}]
}
```

</details>

## 9. Kiểm tra chéo

| Câu hỏi | Kết quả |
|---|---|
| Nguồn tiền có ra đúng I? | 55% + 35% + 10% = 100% ở mọi bậc |
| Ở B4, 1 giờ thu nhập mua được gì? | 2 300 ≈ 3.2 thỏi B5 qua máy, hoặc ~1 thỏi B5 từ villager (2 150). Đường tắt villager đắt gấp 3, đúng ý "cắt cổ" |
| Số máy cần chạy có hợp lý? | ~1–1.5 máy quặng bậc kế tiếp mỗi bậc. Tiền nhiên liệu/giờ (mục 3.3) khớp với ngân sách ở mục 5 |
| Người chơi bỏ qua máy, chỉ mua villager được không? | Được, nhưng mỗi bậc lâu gấp ~2.5–3 lần. Máy là con đường hiệu quả |
| Guild rank có lấn át không? | 10% thu nhập. Cấp lớn nhất 3 750 ≈ 30 phút thu nhập ở B7 |
| Hết quest trong ngày? | Chỉ còn dungeon, ~0.9 × I |

## 10. Tham số để tinh chỉnh

| Muốn… | Chỉnh |
|---|---|
| Rút ngắn hoặc kéo dài toàn hành trình | I ở mục 5 (nhân hoặc chia đều), hoặc 70% "giá thành máy" ở mục 3.1 |
| Máy rẻ hơn hoặc đắt hơn để chạy | Giá 1 kNL ở mục 3.2 (hiện 2 tiền). Giữ I cố định thì giờ mỗi bậc đổi theo |
| Villager bớt "cắt cổ" | Hệ số ×3 ở mục 3.1 |
| Khuyến khích dungeon hơn quest | Tỷ trọng 55/35 và hệ số 1.1 |
| Thuốc chiếm ít/nhiều hơn | % thời gian dùng thuốc ở mục 4 |
| Thêm sink end game | Gem/enchant cấp cao tốn quặng B8, lõi năng lượng III, vật phẩm trang trí đảo |

## 11. Khoảng trống trong code/data

- Chưa có vật phẩm **nhiên liệu** nào có `energy_source`, và chưa có **chất xúc tác** nào (`catalyst_types` mới có pure/volatile, chưa có item).
- `arcane_reactor` `transfer_rate` 20 NL/tick giới hạn máy quặng cao cấp. Nếu muốn máy B7–B8 nhanh hơn, cần thêm nâng cấp lò hoặc relay.
- Chưa có định nghĩa máy tạo **quặng cao cấp**. Hiện chỉ có máy đào than, đá cuội, quặng sắt thô.
- Nguyên liệu thuốc và quặng chưa có trường **bậc**, cần để tính giá theo stat-standard.

## Lịch sử thay đổi

| Ngày | Thay đổi |
|---|---|
| 2026-10-27 | Bản đầu: thu nhập suy từ việc mua trang bị (800 → 13 500/giờ). |
| 2026-10-27 | **Làm lại theo hướng đi server:** trang bị tự chế tạo; tiền đi vào nhiên liệu máy (quặng cao cấp), thuốc, thức ăn, quặng villager. Thêm giá thành quặng, bản vị năng lượng (1 kNL = 2 tiền), thông số máy quặng, bậc thang nhiên liệu. I mới 550 → 11 000/giờ, ~132h tới B8, guild rank tổng 66 710. |
