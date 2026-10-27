# Bản vị chỉ số — nền kinh tế định giá theo sức mạnh

> Thiết kế **từ đầu**, không dựa vào giá hay item hiện có của server. Ở thế giới thật, tiền neo vào vàng. Ở server này, tiền neo vào **chỉ số**: mọi vật phẩm có sức mạnh (trang bị, enchant, gem, thuốc, mũi tên thuốc) đều quy về một đơn vị chung rồi mới ra giá tiền.
>
> Tài liệu này thay thế phần định giá trong [economy.md](economy.md) và [item-pricing.md](item-pricing.md). Thu nhập/giờ và thưởng guild rank sẽ được suy ra **sau**, từ bảng giá ở đây (mục 11).

## 1. Nguyên tắc

1. **Một đơn vị chung: ĐSM (Điểm Sức Mạnh).** Mọi chỉ số, effect hay enchant đều quy ra ĐSM. Tiền chỉ là ĐSM nhân với đơn giá.
2. **Giá trị = phần sức mạnh món đó thêm vào so với tay không.** Vũ khí được tính phần DPS vượt quá đấm tay, giáp tính phần phòng thủ vượt quá không mặc gì, công cụ tính phần tốc đào vượt quá tay không.
3. **Có lợi cho người dùng thì cộng ĐSM, có hại thì trừ.** "Người dùng" là người mặc/cầm/uống. Với mũi tên thuốc thì là **người bắn**, nên debuff trên mục tiêu được tính là có lợi.
4. **Giá biên lũy tiến, giống kim cương tính theo carat.** Một viên kim cương 2 carat đắt hơn hai viên 1 carat. Tương tự, 1 ĐSM nằm trên món mạnh đắt hơn 1 ĐSM trên món yếu, vì sức mạnh dồn vào một món là hiếm. Đơn giá mỗi ĐSM tăng theo **bậc sức mạnh** của món.
5. **Bậc suy ra từ chỉ số, không suy ra từ vật liệu hay tên.** Món nào có tổng ĐSM bằng ngân sách bậc 4 thì là bậc 4, bất kể làm từ gì.

## 2. Đơn vị ĐSM

| Loại sức mạnh | 1 ĐSM bằng | Căn cứ |
|---|---|---|
| Phòng thủ | **1 DEF** = +5% máu hiệu dụng (EHP) so với 20 máu gốc | `DEFENSE_FLAT` giảm sát thương theo `20/(20+x)`, nên EHP tăng tuyến tính 5% mỗi điểm |
| Tấn công | **+0.32 DPS** (= 5% DPS kiếm gỗ vanilla 4 × 1.6) | +5% tốc giết quái ≈ +5% EHP (server chỉ có PvE) |
| Khai thác | **+0.4 tốc đào** trên cuốc (giá trị "tốc vật liệu" kiểu vanilla) | 4 ĐSM ≈ nâng cuốc gỗ lên cuốc đá |

**Mốc tay không:** DPS 4 (1 sát thương × tốc 4.0), DEF 0, tốc đào 1.

## 3. Bậc sức mạnh và đơn giá

### 3.1 Ngân sách mỗi bậc

Bậc 1–5 neo theo vanilla. Bậc 6–8 theo `weapon-stat-budget-spec`: mỗi bậc chỉ +5% chỉ số, phần còn lại đến từ trait/độ hiếm.

| Bậc | Mốc vanilla | DPS kiếm | **ĐSM vũ khí** | DEF cả bộ | **ĐSM bộ giáp** | Tốc đào cuốc | **ĐSM cuốc** |
|---|---|---|---|---|---|---|---|
| B1 | gỗ / da | 6.4 | 7.5 | 7 | 7 | 2 | 2.5 |
| B2 | đá / đồng / vàng / xích | 8.0 | 12.5 | 11 | 11 | 4 | 7.5 |
| B3 | sắt | 9.6 | 17.5 | 15 | 15 | 6 | 12.5 |
| B4 | kim cương | 11.2 | 22.5 | 20 | 20 | 8 | 17.5 |
| B5 | netherite | 12.8 | 27.5 | 24 + KB 0.2 | 26 | 9 | 20 |
| B6 | | 13.4 | 29.4 | | 27.3 | 10 | 22.5 |
| B7 | | 14.1 | 31.6 | | 28.6 | 11 | 25 |
| B8 (trần) | | 14.7 | 33.4 | | 29.9 | 12 | 27.5 |

- ĐSM vũ khí = (DPS − 4) / 0.32.
- ĐSM cuốc = 2.5 × (tốc đào − 1).

**Tỷ lệ theo slot giáp** (theo vanilla kim cương 3/8/6/3): mũ 0.15 · áo 0.40 · quần 0.30 · giày 0.15. **Khiên / đồ tay trái**: 0.25 ngân sách bộ giáp.

### 3.2 Đơn giá ĐSM (G)

| Bậc | B1 | B2 | B3 | B4 | B5 | B6 | B7 | B8 |
|---|---|---|---|---|---|---|---|---|
| **G (tiền / ĐSM)** | 10 | 25 | 60 | 150 | 400 | 800 | 1 600 | 3 200 |
| Hệ số so với bậc trước | — | ×2.5 | ×2.4 | ×2.5 | ×2.7 | ×2 | ×2 | ×2 |

- Từ B1 đến B5, G nhân khoảng ×2.5 mỗi bậc, vì chỉ số tăng nhiều (DPS +25%, DEF +35% mỗi bậc).
- Từ B6 trở đi chỉ nhân ×2, vì chỉ số chỉ +5% mỗi bậc. Giá vẫn tăng gấp đôi, nên leo bậc cuối rất tốn tiền. Đây là chủ ý: làm van chống lạm phát ở end game.
- Vượt trần B8: mỗi +5% ĐSM thì G ×2.

### 3.3 Xác định bậc của một món (nội suy)

```
Tỷ lệ  = ĐSM món / tỷ lệ slot                        (giáp; vũ khí và cuốc dùng thẳng ĐSM)
Bậc    = vị trí của Tỷ lệ trong bảng 3.1, nội suy log giữa hai mốc
G(món) = nội suy log của G giữa hai bậc đó
```

Ví dụ: một thanh kiếm có ĐSM 20 nằm giữa B3 (17.5) và B4 (22.5).
- f = ln(20/17.5) / ln(22.5/17.5) = 0.53.
- G = 60 × (150/60)^0.53 ≈ **97**.
- Giá = 20 × 97 ≈ **1 950**.

Với công cụ, bậc tính theo **tốc đào** (cột "Tốc đào cuốc"), không theo ĐSM. Nhờ vậy xẻng và rìu đốn, vốn có hệ số công dụng thấp, không bị tụt bậc.

## 4. Bảng giá trị attribute

### 4.1 Chỉ số chính, dùng để tính sức mạnh gốc

| Attribute | Cách dùng |
|---|---|
| `MELEE_DAMAGE_BASE` × `ATTACK_SPEED_BASE` | DPS cận chiến |
| `STAFF_DAMAGE_BASE` × `ATTACK_SPEED_BASE` | DPS trượng, × (1 + 1% mỗi block `STAFF_RANGE_BASE` trên 16) |
| `PROJECTILE_DAMAGE_BASE` / thời gian nạp | DPS cung (nạp 1.0s) / nỏ (1.25s), × (0.8 + 0.2 × `PROJECTILE_SPEED_BASE`) |
| `THROW_RATE_BASE` | DPS vũ khí ném = sát thương × tốc ném |
| `MINING_EFFICIENCY` | Cộng vào tốc đào của công cụ |
| `BREAK_STRENGTH` | Công cụ: +1 ĐSM mỗi cấp (mở khoá khối custom) |
| `MINING_SPEED` | Công cụ: +0.02 ĐSM mỗi đơn vị (khối custom) |

### 4.2 Chỉ số "cộng": giá trị cố định, nằm ở đâu cũng như nhau

| Attribute | Tên | Đơn vị | **ĐSM** | Căn cứ |
|---|---|---|---|---|
| `DEFENSE_FLAT` | Phòng ngự | +1 | **1.0** | định nghĩa |
| `MAX_HEALTH_FLAT` | Máu tối đa | +1 máu | **1.2** | chống được mọi loại sát thương, còn DEF chỉ chống 4 loại |
| `MAX_HEALTH_PERCENT` | Máu tối đa % | +1% | **0.24** | = 0.2 máu |
| `KNOCKBACK_RESISTANCE` | Kháng đẩy lùi | +0.1 | **1.0** | |
| `BLOCK_REFLECT` | Phản sát thương khi đỡ | +1% | **0.05** | chỉ khi đang đỡ khiên |
| `SPEED_PERCENT` | Tốc chạy % | +1% | **0.25** | +20% tốc (≈ Speed I) ≈ +25% EHP (≈ Resistance I) |
| `SPEED_FLAT` | Tốc chạy | +0.001 | **0.25** | ≈ 1% tốc gốc |
| `SNEAKING_SPEED` | Tốc độ bò | +0.1 | **0.3** | |
| `MOVEMENT_EFFICIENCY` | Hiệu quả di chuyển | +0.1 | **0.2** | |
| `JUMP_STRENGTH` | Sức nhảy | +10% | **0.3** | |
| `GRAVITY` | Trọng lực | −10% | **0.2** | |
| `STEP_HEIGHT` | Bước cao | +0.5 block | **0.5** | |
| `FALL_DAMAGE` | Sát thương rơi | −10% | **0.4** | |
| `SAFE_FALL_DISTANCE` | Độ cao rơi an toàn | +1 block | **0.3** | |
| `WATER_MOVEMENT_EFFICIENCY` | Di chuyển dưới nước | +0.1 | **0.2** | |
| `SUBMERGED_MINING_SPEED` | Đào dưới nước | +1 | **0.1** | |
| `OXYGEN_BONUS` | Phần oxi thêm | +1 | **0.3** | |
| `BLOCK_REACH` | Tầm với khối | +1 block | **0.3** | |
| `MINING_EFFICIENCY` | Tốc độ đào (trên món không phải công cụ) | +1 | **0.3** | |
| `BURNING_TIME` | Thời gian cháy | −10% | **0.12** | |
| `LUCK` | May mắn | +1 | **0.5** | |
| `SCALE` | Kích cỡ | — | **0** | coi là cosmetic |

### 4.3 Chỉ số "nhân": giá trị tính theo % của DPS

Các chỉ số này khuếch đại sát thương, nên giá trị phụ thuộc vào sát thương gốc:
- **Trên vũ khí**: dùng `ĐSM_toàn_phần = DPS / 0.32` của chính món đó.
- **Trên giáp, gem, thuốc**: dùng `ĐSM_toàn_phần` của kiếm chuẩn cùng bậc: B1 20 · B2 25 · B3 30 · B4 35 · B5 40 · B6 41.9 · B7 44.1 · B8 45.9.

| Attribute | Tên | Đơn vị | **% ĐSM toàn phần** | Ví dụ trên kiếm B4 (35) |
|---|---|---|---|---|
| `MELEE_DAMAGE_PERCENT` | Sát thương cận chiến % | +1% | **1%** | 0.35 ĐSM |
| `MAGIC_DAMAGE_PERCENT` | Sát thương phép % | +1% | **1%** | 0.35 |
| `PROJECTILE_DAMAGE_PERCENT` | Sát thương tầm xa % | +1% | **1%** | 0.35 |
| `THROW_RATE_PERCENT` | Tốc ném % | +1% | **1%** | 0.35 |
| `PROJECTILE_SPEED_PERCENT` | Vận tốc đạn % | +1% | **0.2%** | 0.07 |
| `SWEEPING_DAMAGE_RATIO` | Quét cạnh | +0.1 | **2%** | 0.70 |
| `ENTITY_REACH` | Tầm đánh | +0.1 block | **1.2%** | 0.42 |
| `ATTACK_KNOCKBACK` | Đẩy lùi | +1 | **2%** | 0.70 |
| `CRIT_DAMAGE_FLAT` | Sát thương chí mạng | +1 | **0.94 × tốc đánh** ĐSM (cố định, không theo %) | 1.5 ở tốc 1.6 |

`CRIT_DAMAGE_FLAT` giả sử ~30% đòn là chí mạng nhảy (×1.5 vanilla). Tỷ lệ % sát thương theo loại (cận chiến/phép/tầm xa) chỉ tính khi khớp loại vũ khí chính của món. Trên giáp, coi như khớp với lối chơi mà món giáp đó nhắm tới.

### 4.4 Quy tắc dấu

ĐSM = giá trị trong JSON × hệ số ở bảng, sau đó **lấy dấu theo lợi/hại cho người dùng**:
- `FALL_DAMAGE: -0.2` → giảm sát thương rơi → **+0.8 ĐSM**.
- `SPEED_PERCENT: -0.06` trên vũ khí nặng → chậm đi → **−1.5 ĐSM**.
- `MAX_HEALTH_FLAT: -2` → **−2.4 ĐSM**.

## 5. Trang bị

### 5.1 Công thức

```
Giáp / khiên:  ĐSM = Σ chỉ số cộng + Σ chỉ số nhân (theo kiếm chuẩn cùng bậc)
Vũ khí:        ĐSM = (DPS − 4) / 0.32 + Σ chỉ số nhân (theo DPS của món) + Σ chỉ số cộng
Công cụ:       ĐSM = 2.5 × (tốc đào − 1) × hệ số công dụng + 1 × BREAK_STRENGTH + 0.02 × MINING_SPEED + Σ chỉ số cộng
               hệ số công dụng: cuốc 1.0 · rìu đốn 0.75 · xẻng 0.5 · cuốc đất 0.3

Giá = ĐSM × G(bậc)        (bậc nội suy theo mục 3.3)
```

Lợi thế đặc trưng của từng loại vũ khí (rìu đánh nặng, giáo đâm xuyên…) **phải được thể hiện bằng attribute hoặc trait có ĐSM**. Lợi thế nào không nằm trong chỉ số sẽ không được tính giá, tức là món đó đang được cho không.

### 5.2 Bảng giá tham chiếu (món đúng ngân sách bậc)

| Bậc | Kiếm | Áo ngực | Cả bộ giáp | Kit (giáp + kiếm) | Cuốc | Khiên |
|---|---|---|---|---|---|---|
| B1 | 75 | 30 | 70 | **145** | 25 | 20 |
| B2 | 310 | 110 | 275 | **590** | 190 | 70 |
| B3 | 1 050 | 360 | 900 | **1 950** | 750 | 225 |
| B4 | 3 400 | 1 200 | 3 000 | **6 400** | 2 600 | 750 |
| B5 | 11 000 | 4 150 | 10 400 | **21 400** | 8 000 | 2 600 |
| B6 | 23 500 | 8 750 | 21 850 | **45 350** | 18 000 | 5 450 |
| B7 | 50 500 | 18 300 | 45 750 | **96 250** | 40 000 | 11 450 |
| B8 | 107 000 | 38 250 | 95 700 | **202 500** | 88 000 | 23 900 |

Kit tăng khoảng ×4 → ×3.3 → ×3.3 → ×3.4 từ B1 đến B5, rồi ×2.1 mỗi bậc sau đó. Đây là "bậc thang thời gian" để suy ra thu nhập/giờ ở mục 11.

**Làm tròn:** dưới 1 000 → bội số 5 · từ 1 000 → bội số 50 · từ 100 000 → bội số 500.

## 6. Enchant — bảng rank

Enchant chưa đầy đủ, nên định giá theo **rank**. Rank dựa trên sức mạnh quy đổi ra ĐSM. Tiền = ĐSM × G(bậc của món mang enchant), nhưng không thấp hơn giá sàn của rank. Enchant trên đồ mạnh đắt hơn, vì cùng hiệu ứng đó trên đồ endgame đáng giá hơn.

Bạn chỉ nêu F, E, D, C, A, S. Mình thêm **B** cho đủ dải liên tục giữa C và A.

| Rank | Tiêu chí | ĐSM quy đổi | Sàn | B1 | B3 | B5 | B8 |
|---|---|---|---|---|---|---|---|
| **F** | Cosmetic, tiện ích rất hẹp (phát sáng, câu cá nhanh hơn chút) | 0.2–0.5 | 20 | 20 | 20–30 | 80–200 | 650–1 600 |
| **E** | Tiện ích rõ ràng nhưng theo hoàn cảnh (thở dưới nước, giảm sát thương rơi I, đẩy lùi I) | 0.5–1.5 | 50 | 50 | 50–90 | 200–600 | 1 600–4 800 |
| **D** | Chiến đấu theo điều kiện: một loại quái, một loại sát thương (diệt thây ma, chống lửa/nổ/đạn, gai I) | 1.5–3 | 100 | 100 | 100–180 | 600–1 200 | 4 800–9 600 |
| **C** | Chiến đấu dùng thường xuyên (khía cạnh lửa/băng, quét cạnh, hiệu suất III) | 3–5 | 200 | 200 | 200–300 | 1 200–2 000 | 9 600–16 000 |
| **B** | Mạnh: hiệu ứng theo thời gian, lợi nhuận tài nguyên (thổ huyết, kịch độc, nhặt II, gia tài II, bắn nhiều tia) | 5–8 | 400 | 400 | 400–480 | 2 000–3 200 | 16 000–25 600 |
| **A** | Rất mạnh hoặc ảnh hưởng kinh tế (diệt vương, bộc phá, gia tài III, nhặt III, tốc thời) | 8–12 | 800 | 800 | 800 | 3 200–4 800 | 25 600–38 400 |
| **S** | Độc nhất, đổi lối chơi (tu sửa, hút máu, hồi phục) | 12–20 | 2 000 | 2 000 | 2 000 | 4 800–8 000 | 38 400–64 000 |
| **X** (lời nguyền) | Có hại cho người dùng | ĐSM âm | — | trừ thẳng vào giá món | | | |

- Cùng một enchant ở cấp cao hơn có thể lên rank (ví dụ Gai I = D, Gai III = C). Chọn giá trong dải theo cấp.
- Enchant nào cấp attribute (`provideAttributes`) thì **tính theo bảng attribute ở mục 4**, không cần xếp rank.
- **Nhiều enchant trên một món:** cộng giá các enchant, rồi cộng thêm +10% cho mỗi enchant sau enchant đầu tiên.

## 7. Gem

```
Giá gem (đứng riêng) = (ĐSM gem + 2) × G(bậc gem) × (0.6 + 0.4 × success_chance)
Món đã khảm          = Giá món tính lại với chỉ số gem đã cộng vào + 2 × G(bậc gem)
Ô khảm trống         = +1 × G(bậc món) mỗi ô
```

- **ĐSM gem** tính theo slot mà gem cộng chỉ số (giáp dùng bảng 4.2/4.3 theo kiếm chuẩn; vũ khí dùng DPS của món). Gem có chỉ số cho nhiều slot thì lấy **slot có lợi nhất**.
- **"+2"** là giá trị của bản thân viên gem: một vật mang chỉ số tháo lắp được. **Bậc gem** khai báo theo nơi gem rơi ra (vùng/quái bậc mấy).
- Khi khảm, chỉ số gem làm tăng ĐSM của món, nên có thể đẩy món lên bậc cao hơn một chút (hiệu ứng carat). Vì vậy gem trên đồ mạnh tăng giá nhiều hơn.
- `success_chance` thấp làm gem rẻ đi, vì người mua chịu rủi ro.

## 8. Thuốc

### 8.1 Nguyên lý

Thuốc custom (`brewing/`) cộng dồn chỉ số của tối đa 6 nguyên liệu. Mỗi nguyên liệu cho attribute tạm thời, effect, và thay đổi thời gian hiệu lực (gốc 180s, tối thiểu 30s).

**Quy tắc thời gian: dùng thuốc liên tục 60 phút = giá mua vĩnh viễn số chỉ số đó ở cùng bậc.**

```
ĐSM thuốc = Σ ĐSM attribute (bảng 4) + Σ ĐSM effect (bảng 8.2)     — có hại thì âm
Giá thuốc = max(ĐSM thuốc, 0.1 × tổng phần dương) × G(bậc thuốc) × (thời gian giây / 3600)
```

- **Bậc thuốc** = bậc cao nhất trong các nguyên liệu. Mỗi nguyên liệu nên khai báo bậc theo nơi rơi ra.
- **Sàn 10% phần dương:** thuốc lợi ít hại nhiều vẫn có giá nguyên liệu. Thuốc chỉ có hại (ĐSM ≤ 0 và không có phần dương) thì giá **0**, không nên bán.
- **Tính theo người dùng có lợi nhất:** chỉ số % sát thương theo loại được tính lần lượt cho 3 kiểu người chơi (cận chiến, phép, tầm xa), rồi lấy kết quả cao nhất. Ví dụ "−7% sát thương phép" không làm hại người cận chiến, nên không trừ giá khi tính cho kiểu cận chiến.

### 8.2 Bảng ĐSM của effect

| Effect (`BrewStats`) | Ý nghĩa giá trị | **ĐSM** | Ví dụ |
|---|---|---|---|
| `resistance` x | giảm x sát thương (cận chiến, phép, đạn, nổ) | **20 × x / (1 − x)** | 0.1 → 2.2 · 0.2 → 5 · 0.3 → 8.6 |
| `vulnerability` x | nhận thêm x sát thương | **−20 × x / (1 + x)** | 0.1 → −1.8 · 0.2 → −3.3 |
| `damage_increase` x | +x sát thương gây ra | **x × ĐSM kiếm chuẩn của bậc** | 0.1 ở B3 → 3 |
| `regeneration` v | hồi v máu/giây | **12 × v** | 0.25 → 3 · 0.5 → 6 |
| `absorption` v | +v máu hấp thụ (một lần) | **0.6 × v** | 4 → 2.4 |
| `fire_resistance` x | giảm x sát thương lửa | **2 × x** (vùng Nether: ×3) | 1.0 → 2 |
| `damage_over_time` v | **người uống** mất v máu/giây | **−12 × v** | 0.25 → −3 |
| `night_vision` | nhìn đêm | **+0.5** | |
| `water_breathing` | thở dưới nước | **+1** | |
| `invisibility` | tàng hình | **+2** | |
| `glowing` | bị phát sáng | **−0.3** | |
| `nausea` | buồn nôn | **−1.5** | |
| `darkness` | bóng tối | **−2** | |
| `blindness` | mù | **−3** | |

Effect vanilla khác (nếu sau này có) quy đổi theo cùng cách: Speed I = +20% tốc = 5 ĐSM, Slowness I = −15% tốc = −3.75 ĐSM…

### 8.3 Ví dụ với nguyên liệu đang có (giá trị roll trung bình, giả sử nguyên liệu bậc B3, G = 60)

| Công thức | Chỉ số tổng | Thời gian | ĐSM | Giá |
|---|---|---|---|---|
| 2 × Iron Bark + Swiftroot | +6 DEF, +15% kháng, +7.5% tốc | 180s | 6 + 3.53 + 1.88 = **11.4** | 11.4 × 60 × 180/3600 ≈ **35** |
| 2 × Cursed Spore | +25% sát thương, +20% nhận sát thương | 240s | 7.5 − 3.33 = **4.2** | ≈ **15** |
| 3 × Ember Petal + Ancient Heart | cận chiến +4% (9 − 5), phép −7% | 70s | kiểu cận chiến: **1.2** | ≈ **1.5** |
| 2 × Stabilizing Salt | −4 máu tối đa | 660s | **−4.8**, không có phần dương | **0** (không bán) |

**Phát hiện thiết kế:** Ancient Heart (+370s, đổi lại −cận chiến/−phép) **không có hại gì với người chơi tầm xa**. Với họ, đây là nguyên liệu kéo dài thời gian miễn phí. Stabilizing Salt (+240s, −2 máu) cũng hoạt động như "đổi máu lấy thời gian". Nên kiểm tra lại xem các nguyên liệu kéo dài thời gian có đánh đổi công bằng với mọi lối chơi không.

## 9. Mũi tên thuốc

Code hiện **chưa có** hệ thống mũi tên thuốc. Dưới đây là quy tắc định giá cho khi làm.

```
ĐSM mũi tên = Σ ĐSM effect lên MỤC TIÊU, đổi dấu theo góc nhìn người bắn × 0.7 (xác suất trúng)
Giá 1 mũi   = max(0, ĐSM mũi tên) × G(bậc) × (thời gian hiệu lực giây / 3600) + 1 (thân tên)
```

Đổi dấu theo góc nhìn người bắn:

| Effect lên mục tiêu | ĐSM cho người bắn |
|---|---|
| `vulnerability` x | **+ x × ĐSM kiếm chuẩn** (mình đánh mục tiêu đau hơn) |
| `damage_over_time` v | **+ v / 0.32** (cộng thêm DPS trong thời gian hiệu lực) |
| Làm chậm −x% tốc | **+ x × 25 × 0.5** (khống chế; quái không chạy thoát) |
| `blindness`, `darkness`, `nausea` | **0** (gần như vô tác dụng với quái) |
| `regeneration`, `resistance`, `damage_increase` | **âm**: buff cho kẻ địch, không nên tồn tại |

Ví dụ: mũi tên gây `vulnerability` 0.15 trong 8s ở B4 → 0.15 × 35 × 0.7 = 3.7 ĐSM. Giá = 3.7 × 150 × 8/3600 + 1 ≈ **2.2 tiền/mũi**.

Mũi tên thường không có chỉ số, giá cố định **1 tiền/mũi**.

## 10. Điều chỉnh chung

- **Độ bền:** mỗi bậc có độ bền chuẩn (theo vanilla của vật liệu mốc). Lệch > ±30% thì nhân giá với `(độ bền / chuẩn)^0.25`. Vì sửa đồ tốn tài nguyên, độ bền thấp cũng là một khoản chi.
- **Lời nguyền** (rank X): trừ thẳng ĐSM âm vào món. Riêng "không thể sửa" thì nhân giá ×0.75 (đời món ngắn).
- **Giá trị âm toàn phần:** món có ĐSM ≤ 0 thì không bán qua shop.
- **Hiệu chuẩn lại:** muốn cả nền kinh tế đắt hay rẻ hơn thì chỉ đổi G (mục 3.2). Muốn đổi sức mạnh tương đối giữa các chỉ số thì đổi bảng 4 và 8.2. Không sửa giá tay từng món.

## 11. Bước tiếp theo: thu nhập/giờ và guild rank

> ✅ Đã làm ở [income.md](income.md): thu nhập/giờ theo bậc, quest 5 cấp khó, dungeon, guild rank.

Bảng giá ở mục 5.2 là "bậc thang thời gian". Khi chốt được **số giờ chơi để mua được kit mỗi bậc**, thu nhập/giờ suy ra trực tiếp:

```
Thu nhập/giờ ở bậc b = Giá kit(b) / Số giờ mục tiêu để mua kit(b)
```

Ví dụ (chưa chốt, chỉ minh hoạ): nếu kit B3 nên mất ~3h thì thu nhập ở giai đoạn B3 ≈ 1 950 / 3 ≈ 650 tiền/giờ. Từ đó:
- Thưởng quest = thời lượng × thu nhập/giờ × tỷ trọng (như economy.md mục 7).
- Thưởng guild rank = ~15% thu nhập kiếm được giữa hai mốc (như economy.md mục 6).
- Thuốc dùng liên tục 1h ≈ giá vĩnh viễn số chỉ số đó, nên chi phí thuốc tính được theo % thu nhập.

## 12. Khoảng trống trong code

- Chưa có trường **bậc** cho nguyên liệu thuốc và gem. Cần thêm để biết dùng G nào.
- Chưa có hệ thống **mũi tên thuốc**.
- Chưa có **gem** nào trong data.
- `SpeedPercent.getEnumConstant()` trả về `SPEED_FLAT` thay vì `SPEED_PERCENT`, nhiều khả năng là lỗi.
- Nên có **công cụ tính giá** trong plugin (validator hoặc lệnh `/price`), đọc bảng 3, 4 và 8.2 từ JSON. Nên làm chung với validator của `weapon-stat-budget-spec`, vì ngân sách bậc ở mục 3.1 chính là ngân sách của spec đó.

## Lịch sử thay đổi

| Ngày | Thay đổi |
|---|---|
| 2026-10-27 | Tạo bản vị chỉ số từ đầu: đơn vị ĐSM, ngân sách 8 bậc, đơn giá G, bảng 40 attribute, công thức trang bị/công cụ, rank enchant F–S (+B), gem, thuốc (14 effect), mũi tên thuốc. Thay thế phần định giá của economy.md và item-pricing.md. |
