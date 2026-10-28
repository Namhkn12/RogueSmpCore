# Ngọc (Gem)

> Hệ thống ngọc theo hướng **Hypixel SkyBlock**: mỗi loại ngọc mang **đúng 1 chỉ số**, 5 phẩm chất chỉ làm con số lớn dần. Khác SkyBlock ở 4 điểm, tận dụng những gì plugin đang có (mục 2). Giá tính theo [stat-standard.md](stat-standard.md) mục 7.

## 1. Ngọc chạy thế nào trong plugin

- Ngọc là item có component **`gem_data`**: chỉ số khai báo **theo từng slot**, ngọc chỉ khảm được vào món có slot nằm trong danh sách, kèm `success_chance` (thất bại thì ngọc vỡ).
- Chỉ số của nhiều ngọc trên cùng món được cộng dồn. Món phải có `"socket": n`.
- Hình: base `PLAYER_HEAD` + `head_skin`. Một texture cho cả 5 phẩm chất. **Tên không có số cấp**, phẩm chất thể hiện bằng **màu tên đậm dần**.
- Lore viết tay gồm 1 dòng phẩm chất (`<dark_gray><!i>Ngọc Thô`) và 2 dòng mô tả in nghiêng, riêng cho từng loại ngọc ở từng phẩm chất. Lore không gợi ý chỗ khảm. Chỉ số và tỷ lệ khảm do `gem_data` tự in.
- Tên phẩm chất tiếng Việt: Rough = **Thô**, Flawed = **Rạn**, Fine = **Tinh**, Flawless = **Vô Khuyết**, Perfect = **Hoàn Mỹ**.

## 2. Giống và khác Hypixel SkyBlock

**Giống:**
- **1 loại ngọc = 1 chỉ số.** Nhìn màu là biết ngọc làm gì. Lore chỉ 1–2 dòng chỉ số.
- **5 phẩm chất:** Rough → Flawed → Fine → Flawless → Perfect. Phẩm chất cao hơn chỉ tăng con số, không thêm chỉ số mới.

**Khác:**
1. **Phẩm chất cao có cái giá.** Rough, Flawed, Fine không có mặt trái. Flawless và Perfect thêm **đúng 1 dòng trừ**, hợp với tính chất của ngọc. Ví dụ ngọc phòng ngự làm chậm người, ngọc phép làm mất máu.
2. **Cặp ngọc "cố định ↔ %".** Ruby cộng sát thương cố định, có lợi cho vũ khí đánh nhanh. Amber cộng sát thương %, có lợi cho vũ khí đòn to. Người chơi phải chọn theo vũ khí của mình.
3. **Ngọc đổi chỉ số theo chỗ khảm** (kiểu Diablo 2). Sapphire khảm vào mũ cho oxi, khảm vào giày cho giảm sát thương rơi. Cùng một chủ đề "thích nghi môi trường", mỗi slot một chỉ số.
4. **Khảm có rủi ro, và mỗi loại ngọc khó khảm khác nhau.** Phẩm chất càng cao tỷ lệ càng thấp (Hoàn Mỹ chỉ 15–32%), thất bại thì ngọc vỡ. Ngọc chiến đấu khó khảm hơn ngọc tiện ích (mục 3.1).

**Không khoá loại đồ.** Ngọc tay chính khảm được vào mọi món tay chính. Chỉ số tự quyết định có hợp hay không (Amethyst trên kiếm chẳng có tác dụng). Vì vậy **không cần thêm `allowed_tags`**.

## 3. Phẩm chất

| Phẩm chất | Tên tiếng Việt | Cấp | Bậc | Tỷ lệ khảm | Dòng trừ | Nguồn |
|---|---|---|---|---|---|---|
| Rough | Thô | 1 | B2 | 90–100% | — | NPC |
| Flawed | Rạn | 2 | B3 | 72–90% | — | NPC |
| Fine | Tinh | 3 | B4 | 52–72% | — | NPC (ít), loot |
| Flawless | Vô Khuyết | 4 | B5 | 32–52% | 1 dòng nhẹ | loot, dungeon |
| Perfect | Hoàn Mỹ | 5 | B6 | 15–32% | 1 dòng nặng hơn | dungeon, boss |

### 3.1 Tỷ lệ khảm theo từng ngọc

Ngọc càng ảnh hưởng mạnh tới chiến đấu thì càng khó khảm. Pearl (tầm đánh) khó nhất, Sapphire và Jade (tiện ích) dễ nhất. Các con số cố ý lệch nhau để không đều đặn.

| Ngọc | Thô | Rạn | Tinh | Vô Khuyết | Hoàn Mỹ |
|---|---|---|---|---|---|
| Ruby | 95% | 80% | 62% | 42% | 22% |
| Amber | 95% | 78% | 60% | 40% | 20% |
| Garnet | 92% | 76% | 58% | 38% | 18% |
| Citrine | 95% | 80% | 62% | 42% | 22% |
| Amethyst | 93% | 78% | 60% | 40% | 20% |
| Pearl | 90% | 72% | 52% | 32% | 15% |
| Jade | 100% | 88% | 70% | 50% | 30% |
| Onyx | 98% | 84% | 66% | 46% | 25% |
| Rose Quartz | 96% | 82% | 64% | 44% | 24% |
| Aquamarine | 100% | 86% | 68% | 48% | 28% |
| Sapphire | 100% | 90% | 72% | 52% | 32% |

Tỷ lệ thấp làm ngọc Hoàn Mỹ thành **chỗ tiêu tiền lớn**: trung bình phải vỡ 3–6 viên mới khảm được 1 viên (mục 7).

### Ngân sách ĐSM ròng mỗi viên

| Nhóm | Căn cứ | Rough | Flawed | Fine | Flawless | Perfect |
|---|---|---|---|---|---|---|
| Vũ khí | ~12% ĐSM vũ khí | 1.5 | 2.1 | 2.7 | 3.3 | 3.5 |
| Công cụ | ~12% ĐSM cuốc | 1.0 | 1.5 | 2.0 | 2.4 | 2.75 |
| Giáp | ~7% ĐSM bộ giáp | 0.8 | 1.1 | 1.5 | 1.9 | 2.2 |

Một số ngọc cố ý thấp hơn ngân sách để chỉ số không cộng dồn quá đà (máu, tốc chạy, tầm đánh). Giá của chúng tính theo ĐSM thật nên cũng rẻ hơn.

## 4. Danh sách ngọc

| Ngọc | Chỉ số | Khảm vào | Head id | Head |
|---|---|---|---|---|
| **Ruby** | Sát thương cận chiến (cố định) | Tay chính | `gem_ruby` | có |
| **Amber** | Sát thương cận chiến (%) | Tay chính | `gem_amber` | có |
| **Garnet** | Sát thương chí mạng | Tay chính | `gem_garnet` | có |
| **Citrine** | Sát thương tầm xa (%) | Tay chính | `gem_citrine` | có |
| **Amethyst** | Sát thương phép (%) | Tay chính | `gem_amethyst` | có |
| **Pearl** | Tầm đánh | Tay chính | `gem_pearl` | có |
| **Jade** | Tốc đào | Tay chính | `gem_jade` | có |
| **Onyx** | Phòng ngự | Áo, quần | `gem_onyx` | có |
| **Rose Quartz** | Máu tối đa | Mũ, áo | `gem_quartz` | có |
| **Aquamarine** | Tốc chạy | Quần, giày | `aquamarine` | có |
| **Sapphire** | Mũ: oxi · Giày: giảm sát thương rơi | Mũ, giày | `gem_sapphire` | có |

Mỗi slot giáp có 2 ngọc để chọn: mũ (Rose Quartz / Sapphire), áo (Onyx / Rose Quartz), quần (Onyx / Aquamarine), giày (Aquamarine / Sapphire).

### Head cần thêm

| Head id | Mô tả | Từ khoá tìm |
|---|---|---|
| `gem_garnet` | Đá cắt giác đỏ rượu sẫm, tối hơn ruby | `garnet`, `dark red gem`, `red crystal` |
| `gem_pearl` | Ngọc trai tròn, trắng sữa ánh hồng nhẹ | `pearl`, `white pearl`, `pearl gem` |
| `gem_quartz` | Tinh thể thạch anh hồng phấn, hơi trong | `rose quartz`, `pink gem`, `pink crystal` |
| `gem_sapphire` | Đá cắt giác xanh dương đậm | `sapphire`, `blue gem`, `blue diamond` |

## 5. Chỉ số theo phẩm chất

### 5.1 Ngọc vũ khí

| Ngọc | Rough | Flawed | Fine | Flawless | Perfect |
|---|---|---|---|---|---|
| **Ruby** sát thương cận chiến | +0.3 | +0.4 | +0.55 | +0.9 · **−0.05 tốc đánh** | +1.2 · **−0.1 tốc đánh** |
| **Amber** sát thương cận chiến | +6% | +7% | +8% | +9.5% · **−2% tốc chạy** | +11% · **−4% tốc chạy** |
| **Garnet** sát thương chí mạng | +1 | +1.4 | +1.8 | +2.5 · **−0.5 phòng ngự** | +3 · **−1 phòng ngự** |
| **Citrine** sát thương tầm xa | +6% | +7% | +8% | +9.5% · **−0.5 phòng ngự** | +11% · **−1 phòng ngự** |
| **Amethyst** sát thương phép | +6% | +7% | +8% | +9.5% · **−0.5 máu** | +11% · **−1 máu** |

- **Ruby và Amber:** Ruby cố định mạnh trên kiếm (nhân với nhiều đòn mỗi giây). Amber % mạnh trên rìu (nhân với đòn to). Rìu khảm Ruby Perfect còn bị trừ 10% tốc đánh, nên tự nhiên không đáng.
  - Kiếm B4 (7 sát thương, tốc 1.6) khảm 2 Ruby Perfect: 9.4 sát thương, tốc 1.4 → DPS +17%.
  - Rìu B4 (9, tốc 1.0) khảm 2 Amber Perfect: +22% mỗi đòn, tốc đánh không đổi, chạy chậm hơn 8%.
- **Garnet** chỉ có tác dụng với đòn chí mạng (nhảy lên đánh), nên hợp với người chơi kiếm hoặc rìu có kỹ năng.

### 5.2 Ngọc tiện ích tay chính

| Ngọc | Rough | Flawed | Fine | Flawless | Perfect |
|---|---|---|---|---|---|
| **Pearl** tầm đánh | +0.15 | +0.2 | +0.25 | +0.3 · **−2% tốc chạy** | +0.4 · **−4% tốc chạy** |
| **Jade** tốc đào | +0.4 | +0.6 | +0.8 | +1.15 · **−2% tốc chạy** | +1.4 · **−3% tốc chạy** |

- Pearl cố ý thấp hơn ngân sách. Tầm đánh rất mạnh trong PvP vì đánh được mà không bị đánh lại, nên 2 Pearl Perfect cũng chỉ +0.8 block.
- +1 tốc đào trên cuốc ≈ lên một bậc vật liệu.

### 5.3 Ngọc giáp

| Ngọc | Khảm vào | Rough | Flawed | Fine | Flawless | Perfect |
|---|---|---|---|---|---|---|
| **Onyx** phòng ngự | Áo, quần | +0.8 | +1.1 | +1.5 | +2.4 · **−2% tốc chạy** | +3.2 · **−4% tốc chạy** |
| **Rose Quartz** máu tối đa | Mũ, áo | +0.5 | +0.75 | +1 | +1.25 · **−0.3 phòng ngự** | +1.5 · **−0.5 phòng ngự** |
| **Aquamarine** tốc chạy | Quần, giày | +2% | +3% | +4% | +6% · **−0.3 phòng ngự** | +8% · **−0.5 phòng ngự** |
| **Sapphire** (mũ) oxi | Mũ | +2 | +3 | +4 | +5 · **−0.3 phòng ngự** | +6 · **−0.5 phòng ngự** |
| **Sapphire** (giày) sát thương rơi | Giày | −15% | −20% | −30% | −40% · **−0.3 phòng ngự** | −50% · **−0.5 phòng ngự** |

- **Máu:** 2 Rose Quartz Perfect = +3 máu, dưới trần 25 và còn chỗ cho enchant.
- **Tốc chạy:** 2 Aquamarine Perfect = +16%, dưới Speed I (+20%).
- **Onyx và Aquamarine đối nhau ở quần:** chắc chắn hay nhanh nhẹn. Onyx cấp cao làm chậm người.

### 5.4 Ví dụ lore (Onyx Vô Khuyết, sau khi sửa lore ở mục 8)

```
Onyx
Ngọc Vô Khuyết
Lớp giáp chắc thêm,
bước chân cũng nặng thêm.
Khi khảm trang bị Ngực, Chân:
 +2.4 Phòng ngự
 -2% Tốc chạy
Tỉ lệ khảm thành công: 46%
```

### 5.5 Lore mô tả

Mỗi ngọc có 5 câu, đi từ "thô ráp, lẫn đất đá" (Thô) tới "hoàn mỹ" (Hoàn Mỹ). Ở Vô Khuyết và Hoàn Mỹ, câu mô tả gợi đến cái giá của ngọc. Nội dung đầy đủ nằm trong các file JSON.

| Ngọc | Thô | Hoàn Mỹ |
|---|---|---|
| Ruby | Mảnh đỏ còn bám đất, ấm như than vừa tắt. | Ngọn lửa hoá đá. Lưỡi nặng thêm, nhưng nhát nào cũng bỏng rát. |
| Amber | Cục nhựa cây đục, bên trong lẫn vụn gỗ. | Sức nặng ngàn năm dồn vào lưỡi rìu, và đè lên cả bước chân. |
| Garnet | Viên đá sẫm màu, chỉ ánh lên khi bị va đập. | Hoàn mỹ và liều lĩnh: nó dạy người ta quên cả giáp trụ. |
| Citrine | Mảnh đá vàng đục, phải soi nắng mới thấy màu. | Mũi tên đã trúng đích trước khi kịp rời dây cung. |
| Amethyst | Tinh thể tím đục, thoang thoảng mùi sấm sét. | Nó lắng nghe từng câu thần chú, và đòi trả giá bằng máu. |
| Pearl | Viên ngọc trai méo mó, còn dính cát biển. | Kẻ thù cứ ngỡ mình đã đứng đủ xa. |
| Jade | Mảnh ngọc bích thô, còn lẫn trong đá vôi. | Núi đá cũng phải mở đường cho nó. |
| Onyx | Mảnh đá đen sần sùi, lạnh và nặng. | Vững như tường thành, và chậm như nó. |
| Rose Quartz | Mảnh thạch anh hồng đục, ấm khi áp vào ngực. | Một trái tim thứ hai, ấm áp và mong manh. |
| Aquamarine | Mảnh đá xanh nhạt, mát như nước suối. | Đến cơn gió đuổi theo cũng không kịp. |
| Sapphire | Mảnh đá xanh thẫm, nhặt được dưới đáy hồ. | Biển sâu hay vực thẳm, đâu cũng như nhà. |

## 6. Màu tên theo phẩm chất

| Ngọc | Rough | Flawed | Fine | Flawless | Perfect |
|---|---|---|---|---|---|
| Ruby | `#FF9AA2` | `#FF5A65` | `#E62635` | `#B3121F` | `#7A0A16` |
| Amber | `#F0C987` | `#E3A857` | `#C98A3A` | `#A35E1A` | `#6E3A0C` |
| Garnet | `#D98A9E` | `#B85570` | `#962E4C` | `#741A35` | `#520D22` |
| Citrine | `#FFF3A6` | `#FFE266` | `#FFCC00` | `#D9A400` | `#A67C00` |
| Amethyst | `#E3C8FA` | `#C89BF0` | `#A86BE0` | `#8040BF` | `#56238A` |
| Pearl | `#FFFFFF` | `#F3EEE6` | `#E6DCCB` | `#D4C4A8` | `#BFA982` |
| Jade | `#B6F0CF` | `#7FE0A8` | `#45C281` | `#249960` | `#136B40` |
| Onyx | `#C4C7CE` | `#9A9DA5` | `#6E7178` | `#45474C` | `#232326` |
| Rose Quartz | `#FFD6E4` | `#FFB3CC` | `#F58BB0` | `#D9668F` | `#B03D6A` |
| Aquamarine | `#D4F7FB` | `#A6EDF5` | `#6FDCE8` | `#33BCCC` | `#17899A` |
| Sapphire | `#A9C8FF` | `#7AA7FF` | `#4A7FF0` | `#2557C9` | `#12358F` |

## 7. Giá và NPC

```
Giá = (ĐSM ròng + 2) × G(bậc) × (0.6 + 0.4 × tỷ lệ khảm) × hệ số NPC
```

Tinh bán ở NPC nhân ×1.25 (số in đậm). Vô Khuyết và Hoàn Mỹ không bán. Mỗi ô ghi **giá 1 viên · chi phí trung bình cho 1 lần khảm thành công** (= giá / tỷ lệ khảm).

| Ngọc | Thô | Rạn | Tinh (NPC) | Vô Khuyết | Hoàn Mỹ |
|---|---|---|---|---|---|
| Ruby | 85 · ~90 | 220 · ~280 | 600 (**760**) · ~970 | 1 600 · ~3 850 | 2 950 · ~13 450 |
| Amber | 85 · ~90 | 220 · ~290 | 600 (**760**) · ~1 000 | 1 600 · ~4 050 | 3 050 · ~15 250 |
| Garnet | 85 · ~90 | 220 · ~290 | 590 (**730**) · ~1 000 | 1 600 · ~4 150 | 2 950 · ~16 450 |
| Citrine | 85 · ~90 | 230 · ~280 | 610 (**760**) · ~980 | 1 650 · ~3 900 | 3 100 · ~14 050 |
| Amethyst | 85 · ~90 | 220 · ~290 | 600 (**760**) · ~1 000 | 1 600 · ~3 950 | 2 950 · ~14 700 |
| Pearl | 60 · ~65 | 140 · ~200 | 370 (**460**) · ~710 | 860 · ~2 700 | 1 600 · ~10 550 |
| Jade | 75 · ~75 | 200 · ~230 | 530 (**660**) · ~750 | 1 400 · ~2 800 | 2 750 · ~9 100 |
| Onyx | 70 · ~70 | 170 · ~210 | 450 (**570**) · ~690 | 1 200 · ~2 650 | 2 350 · ~9 400 |
| Rose Quartz | 65 · ~65 | 160 · ~200 | 410 (**510**) · ~640 | 990 · ~2 250 | 1 850 · ~7 650 |
| Aquamarine | 60 · ~60 | 160 · ~180 | 390 (**490**) · ~580 | 1 000 · ~2 100 | 2 000 · ~7 100 |
| Sapphire | 65 · ~65 | 170 · ~190 | 430 (**530**) · ~590 | 1 050 · ~2 050 | 2 050 · ~6 350 |

- Giá 1 viên giảm so với bản trước, vì tỷ lệ khảm thấp làm ngọc rủi ro hơn.
- Chi phí thật cho 1 lần khảm Hoàn Mỹ thành công là **6 000–16 000**, tức khoảng 1.5–3 giờ thu nhập ở B6 (5 000/giờ, theo income.md). Đây là chỗ tiêu tiền lớn ở end game.

| NPC | Rough, Flawed | Fine |
|---|---|---|
| Roland (giáp) | Onyx, Rose Quartz, Aquamarine, Sapphire (Rough); Onyx, Rose Quartz (Flawed) | Onyx, Rose Quartz |
| Drake (vũ khí) | Ruby, Amber, Garnet, Amethyst, Pearl | Ruby, Amber |
| Hawthorne (cung) | Citrine | Citrine |
| Barrett (công cụ) | Jade | Jade |

## 8. Việc cần làm

**Code:**
- **Sửa lore của `GemDataComponent`:**
  1. Gộp các slot có chỉ số giống nhau vào một tiêu đề ("Khi khảm trang bị Ngực, Chân:").
  2. In giá trị qua `getDisplayText` của từng attribute, để phần trăm hiện đúng (hiện đang in `+0.06` thay vì `+6%`).
  3. Bỏ dòng trống sau slot cuối.
- Kiểm tra lỗi `SpeedPercent.getEnumConstant()` trả về `SPEED_FLAT` (stat-standard.md mục 12). Aquamarine, Amber, Pearl, Jade, Onyx dùng `SPEED_PERCENT`.
- Không cần `allowed_tags` nữa.

**Data:**
- Head đã import đủ (Rose Quartz dùng head `gem_quartz`).
- Đã tạo 55 item JSON trong `server_data/items/gems/`. Id: `gem_<ngọc>_<1..5>`, 1 = Rough … 5 = Perfect.
- Thêm `"socket"` cho đồ ở hub.

## Lịch sử thay đổi

| Ngày | Thay đổi |
|---|---|
| 2026-10-28 | Lore mới: dòng phẩm chất tiếng Việt (Thô, Rạn, Tinh, Vô Khuyết, Hoàn Mỹ) + 2 dòng mô tả cho từng ngọc ở từng phẩm, bỏ dòng "Khảm vào". Tỷ lệ khảm riêng cho từng ngọc, Hoàn Mỹ chỉ 15–32%. Tính lại giá. |
| 2026-10-28 | Tạo 55 item JSON. Xoá 5 file `gem_tigers_eye_*` (thay bằng `gem_amber_*`). |
| 2026-10-28 | Làm lại theo hướng Hypixel SkyBlock: 11 ngọc, mỗi ngọc 1 chỉ số, 5 phẩm chất Rough → Perfect. Flawless và Perfect có 1 dòng trừ. Ruby (cố định) và Amber (%) thành cặp lựa chọn. Sapphire đổi chỉ số theo slot. Bỏ khoá loại đồ và `allowed_tags`. Thêm Garnet (chí mạng). |
| 2026-10-28 | Các bản trước: 10 họ đặc trưng + đa dụng; 7 họ chuyên biệt; 5 họ đa dụng; 7 ngọc giáp ban đầu. |
