# 01 — Thiết kế

Nguồn: artifact "Kvelsgard – bản thiết kế" (https://claude.ai/artifact/QprVqxGdLMNagJ8iAjPDkz) + các quyết định Abx chốt ngày 2026-10-10:
- Đảo bán kính 100–120 → dùng mép tự nhiên r 104–117 (`edge_r`), phình về phía nam để thềm xuất hiện rộng hơn.
- Giữa các layer dưới có tấm bedrock ngăn đào thẳng xuống.
- Không làm cơ chế cửa khoá (Kho Quản Đốc, Cổng Niêm Phong vẫn mở).
- Số phòng mỗi tầng giữ đúng artifact (6/7/7).

## Luồng người chơi
```
Thềm xuất hiện (0, 99, 105) → đường leo chữ chi 4 nhánh → bãi mỏ Y120 → cửa hang 17×13 (-4, 30)
→ hang dẫn 5 khúc, nền Y120→106 → hố r12 (0, -62): gờ xoắn bám vách xuống 34 khối
→ Tầng 2 Hầm Mỏ Cũ (đáy hố → Trạm Tời → ... → Hang Mạch Lộ) → giếng thang gỗ chữ chi (-22, 62)
→ Tầng 3 Rừng Thạch Nhũ (Chân Thang → ... → Động Nhũ Thạch) → hố sụt có thác (30, -56): gờ đá xoắn hoặc nhảy xuống hồ
→ Tầng 4 Vực Deepslate (Hồ Rơi → ... → Lõi Kvelsgard) → teleport (plugin) → Layer 5 bệ xuất hiện (0, -40, 50)
→ đại lộ → sảnh bán nguyệt → bệ triệu hồi → cổng triệu hồi boss
```
Ba tầng mỏ đổi chiều: tầng 2 đi bắc→nam, tầng 3 nam→bắc, tầng 4 lại về nam, nên lối xuống tầng trên rơi đúng điểm vào tầng dưới.

## Layer 1 — mặt núi
- **Mặt núi**: port nguyên hàm `height` của artifact: 5 bậc thềm (Y126→158, bậc sau lùi về bắc), khối núi giữa nhô cao phủ hang dẫn, 2 khe chéo, vành rìa cao dần về mũi bắc (đỉnh Y197). Tuyết trên Y~176, mảng sỏi/đá cuội rải theo noise.
- **Bãi mỏ**: hình chuông Y120 ±1, hở ra mép đảo ở 2 góc nam. Ray đôi từ cửa hang chạy về nam, đống đá thải, cần trục gỗ (tây), lán gác (đông), cột đèn lồng, lửa trại tắt, thùng.
- **Đường leo** (đã đổi so với artifact): hõm |x| < 35, z > 84 hạ xuống. Thềm Y98 từ z101 tới mép đảo; 4 nhánh rộng 3 chạy ngang theo trục x (z 97–99, 93–95, 89–91, 85–87), quay đầu ở |x| 31–34, mỗi nhánh lên 1 khối mỗi ~8 khối đi (bậc thang ở chỗ lên). Giữa 2 nhánh là 1 hàng lan can cobblestone_wall. Artifact vẽ 4 khúc trong dải z 84–96 quá hẹp cho đường rộng 3, nên dải được nới ra z 85–100.
- **Hang dẫn**: polyline artifact, rộng 17 ở cửa thu về 13, cao 13−3·sin(πs), nền dốc đều 120→106, gồ ±1 ở hai bên (giữa phẳng cho ray). Khung gỗ chống mỗi 7 khối, đèn lồng treo, ray bỏ hoang trên các đoạn gần bắc-nam, cột đá dày dần về cuối. Đá phủ mỏng nhất 9 khối.
- **Hố**: trụ r12 từ Y72 lên Y118 + vòm 6. Gờ đá rộng ~2.5 bám vách, xoắn 2 vòng ngược chiều kim đồng hồ từ cửa hang dẫn (Y106) xuống đúng cửa hành lang phía nam ở đáy (Y72), bậc thang ở mỗi bậc, không lan can.

## Layer 2 — Hầm Mỏ Cũ (sàn Y72)
- Phòng: hộp đá thô, cột góc + xà trần gỗ thông, đèn lồng treo dưới xà. Sàn ván cho phòng nghỉ/kho/thưởng, đá-sỏi cho phòng quái. Chi tiết: lồng thang gãy + tời (Trạm Tời), đe/lò/vạc nước (Xưởng Rèn), phễu sàng (Phòng Sàng Quặng), giường tầng (Lán), giá thùng (Kho).
- Hành lang rộng 5 cao 5, khung chống mỗi 6 khối (xà ở Y77 nên lối đi cao 4), ray trên các đoạn thẳng, mạng nhện góc trần. Hành lang (0,−18)→(0,−7) kết thúc ở đống sập.
- **Hang Mạch Lộ** (r 46×28, cao 26): nền 3 bậc **cao dần vào giữa** (ngoài Y72 nối hành lang, giữa Y75, tâm Y78), 4 dốc chéo nối các bậc; trụ đá đỡ trần lưới 15×13; giàn giáo ván quanh 3 trụ ở 2 tầng + thang leo; thác mảnh từ khe trần (24, 72) rơi vào vũng nước; cột đèn lồng.
- Giếng 9×9 tại (−22, 62): gờ ván rộng 2 xoắn vuông bám vách, 14 khối mỗi vòng, xuống phòng Chân Thang; giá đầu giếng bằng gỗ.

## Layer 3 — Rừng Thạch Nhũ (sàn Y40)
- Phòng lót gạch đá rêu/nứt, cột + xà dark oak. Trạm Bơm Nước ngập nửa phòng + ống đồng oxy hoá; Hầm Sập đống đá sập 1 góc; Bãi Xe Goòng 3 tuyến ray.
- Hành lang trần cao 4–6 xen kẽ, vũng nước, glow lichen trên trần, khung dark oak thưa.
- **Động Nhũ Thạch** (r 50×26): nền ruộng bậc thang đá vôi 5 bậc mỗi bậc 1 khối, hồ nông trên các bậc; 10 cột thạch nhũ dáng đồng hồ cát; thạch nhũ/măng đá nhọn, glow lichen, dây glow berries.
- Hố sụt r7 tại (30, −56): gờ đá xoắn ~18 khối/vòng; thác chảy trong khe vách ngoài gờ, đổ xuống hồ.

## Layer 4 — Vực Deepslate (sàn Y8)
- Phòng + hành lang lót gạch deepslate, sàn deepslate đánh bóng/gạch lát, đèn soul. Lò Luyện có máng dung nham ngang phòng (cầu 3 ô giữa) + lò lớn; Trạm Gác Sâu rào song sắt + chòi canh có thang; Giếng Thang Gãy lồng song sắt đổ.
- **Hồ Rơi**: hồ tròn r9.5 sâu 4 giữa phòng, gờ xoắn tầng trên đi tiếp thành tháp dốc đặc trong phòng.
- **Lõi Kvelsgard** (r 48×28): đài giữa Y10 (rho < 0.36), hào dung nham 2 sâu, 4 cầu đá hướng chính, vành sàn Y8 có magma, bậc ngoài Y11 để khai thác, 12 cột bazan, 12 đèn soul quanh đài. Lối xuống boss để trống.

## Layer 5 — Cổ Thành Câm Lặng (phòng boss bản 2, nền Y−44)
Theo mục "Layer 5 · bản 2" của artifact: bố cục đường đi giữ như ảnh phác, công trình lấy từ ảnh mẫu Ancient City rồi biến thể (gãy, sập, phủ sculk), cả hang là deep dark. Thông số ở `BOSS` trong `kv_model.py`, dựng trong `scripts/boss_room.py`.
- **Hang**: tròn R60 mép lượn sóng, vòm thấp Y−2 ở giữa, Y−14 sát vách (cao 42 → 30). Đá quanh hang là deepslate trơn. Nền deepslate/cobbled gồ ±1.
- **Sculk**: thảm nền theo khoảng cách tới cổng (dày quanh cổng, loang ở tàn tích, thưa trên đại lộ ~1/5), sculk vein viền quanh mảng sculk, lưỡi sculk bò lên vách cao 4–8. 20 sensor, 14 catalyst cạnh công trình, 8 shrieker (4 quanh bệ xuất hiện, 4 quanh cổng; đặt bằng schem nên không gọi Warden).
- **Ánh sáng**: chỉ soul lantern, lửa soul, nến. Không đuốc, không đèn lồng vàng.
- **1 Bệ xuất hiện** (0, z50): tròn r6 deepslate tiles cao 3 (mặt Y−41, đứng Y−40), 3 bậc xuống đại lộ.
- **2 Đại lộ** x −3..3, z −13..44: deepslate tiles, dải len xám 3 khối ở giữa.
- **3 Cột vừa** (mẫu 1): 12 cột x ±7, z 38/30/22/14/6/−2; đế 5×5, thân 3×3 tiles xen chiseled, mũ cobbled 5×5, cao 13; tay dark oak fence treo đèn soul hướng vào đường. 3 cột gãy (z38 đông, z6 tây, z−2 tây), mũ rơi nằm cạnh chân.
- **Cột lớn** (mẫu 2): 4 cột (±8, −9) và (±19, −23); đế 7×7, thân 5×5, mũ loe 9×9, cao 16, đèn soul treo 2 mặt.
- **4 Sảnh bán nguyệt** tâm (0, −27) r14: nền vòng đồng tâm, lan can thấp theo cung hở ở đại lộ và 2 đường chéo.
- **5 Bệ 3 bậc** (đúng bản vẽ từng block của artifact): bậc 1 59×25 cao 5 (Y−43..−39) mặt trước có 6 hốc vòm 3×3 đặt nến/sọ; bậc 2 lùi 3, cao 2 (mặt Y−37) có rãnh lửa soul 27 khối ở z−40, bàn triệu hồi (0, −36) và 2 bàn nến bậc thang (±7, −36); bậc 3 gờ 45×8 (Y−36) dưới chân cổng. Cầu thang giữa rộng 9 và cầu thang góc tây x −29..−25. Sculk phủ gần kín mặt bệ, tràn xuống mặt trước.
- **6 Cổng triệu hồi**: khối tường 41×21×5 (x −20..20, z −48..−44, Y−35..−15) bằng gạch deepslate loang cracked/cobbled, 2 dải tiles, hàng chiseled gần đỉnh, đỉnh sứt mẻ; vành reinforced deepslate 29×11 nằm ở lớp giữa (z−46); lòng cổng 27×9 (x −13..13, Y−29..−21) mép trên/dưới răng cưa bằng stairs; ụ hông 2 khối; hàng đèn soul treo dưới mép trên + hàng đặt trên ngưỡng.
- **7 Hai đường chéo** rộng 4 lát cobbled/cracked, ô vỡ sụt; 6 **cột nấm** (mẫu 3, mũ cobbled lệch tâm, đèn soul) dọc đường + 2 trong tàn tích.
- **Tháp canh** (mẫu 4) 11×11 cao 20: cột góc dark oak log, thang gỗ, sàn tiles, vương miện loe có 4 đèn soul. T1 tây còn nguyên, T2 đông bắc mất tầng trên (vương miện đổ cạnh tháp), T3 tây nam chỉ còn tầng trệt.
- **Nhà hai tầng** (mẫu 5) 13×9 cao 10: tầng trệt hàng cột, sàn tầng 2 lót len xám, mái bằng nhô 1, thang leo. H1 nguyên, H2 mất mái, H3 sập một nửa, H4 chỉ còn khung trệt.
- **Điện mộ M** (mẫu 7) 23×17: tường sứt cao 4–8, góc cao 7–10 có vòm gãy, 2 lối vào có vòm; bể nước sâu 3 (phía tây), hố mộ sâu 3 với 6 quách đá + bậc xuống (phía đông), bàn nến bậc thang ở giữa.
- **Trại nến N** ×3: vòm thấp 7×5 có nến. **Cổng thành đổ G** ×2 ở cuối đường chéo, bịt đá vụn.
- **Tường thành** (mẫu 6): 4 cung đứt quãng ở r 51..55, cao 7, 2 tầng lối đi lót len, răng cưa, hàng chiseled.
- **Trụ đá**: 5 trụ deepslate r4 thắt giữa nối nền với trần, sculk vein quanh chân. **Gai đá deepslate** 4–10 khối thành cụm trên các tàn tích, đầu bọc sculk; trên đại lộ, sảnh, bệ để trống. Xích thả đèn soul thưa.
- **Vào**: teleport từ đài Lõi Kvelsgard (plugin). Muốn giếng rơi thật 52 khối thì cần thêm hồ nước ở bệ xuất hiện.

## Ngăn đào tắt
- Tấm bedrock: dưới L1 `min(101, mặt đất thấp nhất 5x5 − 3)` (bậc thang + màn đứng), Y69, Y37, Y3. Phòng boss: tấm đáy Y−52 + vách trụ r 66.5–68.2 từ Y−52 lên Y3; đáy đảo dưới r ≤ 70 hạ xuống ≤ Y−56. Vách đứng dày ~1.5 nằm trong mép đảo 1.6–3 khối, từ Y3 lên tới tấm L1.
- Bedrock chỉ đặt ở ô không kề ô trống → không bao giờ lộ; tự hở ở hố, giếng, hố sụt.
- Đào từ sàn xuống gặp bedrock sau: tầng 2 và 3 là 3 khối, tầng 4 là 5 khối, bãi mỏ là 19 khối.

## Quặng
**Không sinh quặng trong schem.** Quặng do plugin tự sinh khi load map. Đá nền chỉ gồm stone/andesite/tuff/diorite (L1, L2), stone/granite/dripstone/calcite (L3), deepslate/tuff/basalt/blackstone (L4).
