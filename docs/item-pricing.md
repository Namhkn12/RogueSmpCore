# Định giá vật phẩm theo chỉ số

> ⚠ **Đã được thay thế** bởi [stat-standard.md](stat-standard.md): thiết kế lại từ đầu, bậc suy ra từ chỉ số thay vì vật liệu, giá trị tính so với tay không, có thêm thuốc và mũi tên thuốc. Giữ file này để tham khảo cách hiệu chuẩn theo item cũ.

> Bổ sung cho [economy.md](economy.md). File đó quyết định **tổng ngân sách** (giá một bộ đồ mỗi tier = số giờ × thu nhập/giờ). File này quyết định **cách chia ngân sách đó theo chỉ số**: một món mạnh hơn trong cùng tier thì đắt hơn, có enchant hoặc gem thì cộng thêm giá.

## 1. Ý tưởng

```
Giá món = Điểm sức mạnh × Đơn giá điểm (theo tier)
        + Giá enchant
        + Giá gem đã khảm
        ± Điều chỉnh (lời nguyền, ô khảm trống)
```

- **Điểm sức mạnh** tính từ chỉ số. Mỗi attribute có một **giá trị V** cố định, đo bằng "điểm giáp" (1 điểm giáp = 1 DEF = +5% lượng máu hiệu dụng).
- **Đơn giá điểm** tăng theo tier. Chỉ số giữa các tier chênh ít (DEF áo ngực 3.1 → 6.8 → 8.8 → 10.9), nhưng giá phải tăng theo số giờ chơi (economy.md), nên đơn giá phải tăng theo tier. Nếu dùng một đơn giá chung cho mọi tier, đồ Netherite sẽ chỉ đắt hơn đồ Kim cương ~25%, tức người chơi mua được sau vài phút.
- **Tier lấy theo vật liệu gốc** (`base`), không suy ra từ chỉ số. Lý do: một cây rìu kim cương có DPS thấp hơn kiếm kim cương, nhưng vẫn là món của giai đoạn Azure. Nếu suy tier từ DPS, rìu bị tính theo đơn giá tier thấp hơn và lệch tới −90%.

| Tier | Vật liệu `base` | Giai đoạn (economy.md) |
|---|---|---|
| T0 | WOODEN, LEATHER, STONE | trước S1 |
| T1 | COPPER, CHAINMAIL, GOLDEN | S1 — Recruit |
| T2 | IRON | S2 — Guard |
| T3 | DIAMOND | S3 — Azure |
| T4 | NETHERITE | S4 — Blackiron |

Base không có vật liệu (`BOW`, `CROSSBOW`, `SHIELD`, `STICK`, `TRIDENT`, `MACE`…) thì lấy tier từ tiền tố id (`diamond_bow` → T3). Không có tiền tố thì **phải khai báo tier thủ công** (xem mục 9).

## 2. Bảng giá trị attribute (V)

`V` = số điểm giáp mà **một đơn vị** attribute đáng giá. Cột "Đơn vị" là đơn vị ghi trong JSON.

### 2.1 Chỉ số chính (không có V, dùng để tính điểm gốc ở mục 3)

| Attribute | Tên hiển thị | Vai trò |
|---|---|---|
| `MELEE_DAMAGE_BASE` | Sát thương cận chiến | DPS = sát thương × tốc đánh |
| `ATTACK_SPEED_BASE` | Tốc đánh | |
| `STAFF_DAMAGE_BASE` | Sát thương trượng | DPS trượng = sát thương × tốc đánh |
| `STAFF_RANGE_BASE` | Tầm đánh trượng | +1% DPS mỗi block trên 16 |
| `PROJECTILE_DAMAGE_BASE` | Sát thương tầm xa | DPS = sát thương / thời gian nạp (cung 1.0s, nỏ 1.25s) |
| `PROJECTILE_SPEED_BASE` | Vận tốc đạn | DPS × (0.8 + 0.2 × giá trị) |
| `THROW_RATE_BASE` | Tốc ném | DPS = sát thương × tốc ném (chưa có món nào dùng, chưa hiệu chuẩn) |
| `BREAK_STRENGTH` | Sức đào | Công cụ: +0.5 điểm mỗi cấp |
| `MINING_SPEED` | Tốc độ đào (khối custom) | Công cụ: +0.01 điểm mỗi đơn vị |

### 2.2 Chỉ số phụ (có V)

Giá tiền mỗi đơn vị khi nằm trên **giáp/khiên** = V × đơn giá giáp của tier (mục 3.1).

| Nhóm | Attribute | Tên hiển thị | Đơn vị tham chiếu | V (điểm/đơn vị) | T1 | T2 | T3 | T4 |
|---|---|---|---|---|---|---|---|---|
| Phòng thủ | `DEFENSE_FLAT` | Phòng ngự | +1 | 1.0 | 72 | 90 | 235 | 1 370 |
| | `MAX_HEALTH_FLAT` | Máu tối đa | +1 máu (½ tim) | 1.2 | 86 | 108 | 282 | 1 644 |
| | `MAX_HEALTH_PERCENT` | Máu tối đa % | +1% (0.01) | 0.25 | 18 | 22 | 59 | 342 |
| | `KNOCKBACK_RESISTANCE` | Kháng đẩy lùi | +0.01 | 0.1 | 7 | 9 | 24 | 137 |
| | `BLOCK_REFLECT` | Phản sát thương khi đỡ | +1 (=1%) | 0.05 | 4 | 4.5 | 12 | 68 |
| Tấn công | `MELEE_DAMAGE_PERCENT` | Sát thương cận chiến % | +1% (0.01) | 0.2 | 14 | 18 | 47 | 274 |
| | `MAGIC_DAMAGE_PERCENT` | Sát thương phép % | +1% (0.01) | 0.2 | 14 | 18 | 47 | 274 |
| | `PROJECTILE_DAMAGE_PERCENT` | Sát thương tầm xa % | +1% (0.01) | 0.2 | 14 | 18 | 47 | 274 |
| | `THROW_RATE_PERCENT` | Tốc ném % | +1% (0.01) | 0.2 | 14 | 18 | 47 | 274 |
| | `PROJECTILE_SPEED_PERCENT` | Vận tốc đạn % | +1% (0.01) | 0.04 | 3 | 4 | 9 | 55 |
| | `CRIT_DAMAGE_FLAT` | Sát thương chí mạng | +1 | 0.8 | 58 | 72 | 188 | 1 096 |
| | `SWEEPING_DAMAGE_RATIO` | Quét cạnh | +0.1 | 0.8 | 58 | 72 | 188 | 1 096 |
| | `ENTITY_REACH` | Tầm đánh | +0.1 block | 0.2 | 14 | 18 | 47 | 274 |
| | `ATTACK_KNOCKBACK` | Đẩy lùi | +1 | 0.3 | 22 | 27 | 71 | 411 |
| Di chuyển | `SPEED_PERCENT` | Tốc chạy % | +1% (0.01) | 0.25 | 18 | 22 | 59 | 342 |
| | `SPEED_FLAT` | Tốc chạy | +0.001 (≈1%) | 0.25 | 18 | 22 | 59 | 342 |
| | `SNEAKING_SPEED` | Tốc độ bò | +0.1 | 0.3 | 22 | 27 | 71 | 411 |
| | `MOVEMENT_EFFICIENCY` | Hiệu quả di chuyển | +0.1 | 0.2 | 14 | 18 | 47 | 274 |
| | `JUMP_STRENGTH` | Sức nhảy | +10% (0.1) | 0.3 | 22 | 27 | 71 | 411 |
| | `GRAVITY` | Trọng lực | −10% (−0.1) | 0.2 | 14 | 18 | 47 | 274 |
| | `STEP_HEIGHT` | Bước cao | +0.5 block | 0.5 | 36 | 45 | 118 | 685 |
| | `FALL_DAMAGE` | Sát thương rơi | −10% (−0.1) | 0.4 | 29 | 36 | 94 | 548 |
| | `SAFE_FALL_DISTANCE` | Độ cao rơi an toàn | +1 block | 0.3 | 22 | 27 | 71 | 411 |
| Dưới nước | `WATER_MOVEMENT_EFFICIENCY` | Di chuyển dưới nước | +0.1 | 0.2 | 14 | 18 | 47 | 274 |
| | `SUBMERGED_MINING_SPEED` | Đào dưới nước | +1 | 0.1 | 7 | 9 | 24 | 137 |
| | `OXYGEN_BONUS` | Phần oxi thêm | +1 | 0.3 | 22 | 27 | 71 | 411 |
| Tiện ích | `BLOCK_REACH` | Tầm với khối | +1 block | 0.3 | 22 | 27 | 71 | 411 |
| | `MINING_EFFICIENCY` | Tốc độ đào (vanilla) | +1 (trên món không phải công cụ) | 0.3 | 22 | 27 | 71 | 411 |
| | `BURNING_TIME` | Thời gian cháy | −10% (−0.1) | 0.12 | 9 | 11 | 28 | 164 |
| | `LUCK` | May mắn | +1 | 0.5 | 36 | 45 | 118 | 685 |
| | `SCALE` | Kích cỡ | — | 0 | — | — | — | — |

**Quy ước dấu:** V nhân với giá trị trong JSON. Các chỉ số mà giá trị âm có lợi (`FALL_DAMAGE`, `BURNING_TIME`, `GRAVITY`) có V âm trong công thức, nên giá trị âm sẽ **cộng** điểm. Ngược lại, chỉ số có lợi bị âm (ví dụ `SPEED_PERCENT: -0.06` trên vũ khí nặng) sẽ **trừ** điểm.

**Căn cứ chọn V:**
- 1 DEF = +5% máu hiệu dụng, vì `DEFENSE_FLAT` giảm sát thương theo công thức `20/(20+x)`.
- +1 máu = 1.2 DEF, vì máu chống được mọi loại sát thương, còn DEF chỉ chống cận chiến, phép, đạn và nổ.
- +1% sát thương = 0.2 DEF. Coi +5% tốc giết quái ngang +5% máu hiệu dụng (server hiện chỉ có PvE).
- +20% tốc chạy (≈ Speed I) ≈ 25% máu hiệu dụng (≈ Resistance I), nên 1% tốc chạy = 0.25 DEF.
- `CRIT_DAMAGE_FLAT`: giả sử ~30% đòn là chí mạng nhảy (vanilla, ×1.5).

## 3. Điểm sức mạnh và đơn giá theo loại món

### 3.1 Giáp và khiên

```
Điểm = Σ V × giá trị        (tính cả DEF)
Giá  = Điểm × Đơn giá giáp(T)
```

| | T0 | T1 | T2 | T3 | T4 |
|---|---|---|---|---|---|
| **Đơn giá giáp** (tiền/điểm) | 35 | 72 | 90 | 235 | 1 370 |

Không còn chia giá theo số thỏi (5/8/7/4). Mũ có ít DEF hơn thì rẻ hơn, áo ngực nhiều DEF hơn thì đắt hơn. Tổng giá cả bộ vẫn khớp ngân sách ở economy.md.

### 3.2 Vũ khí (cận chiến, trượng, cung/nỏ)

```
DPS gốc = theo bảng 2.1 (cận chiến: sát thương × tốc đánh)
Điểm    = DPS gốc × (1 + 0.05 × Σ V × giá trị của chỉ số phụ)
Giá     = Điểm × Đơn giá vũ khí(T)
```

Nghĩa là trên vũ khí, **mỗi điểm giáp chỉ số phụ = +5% điểm vũ khí**. Ví dụ quét cạnh +0.1 (V 0.8) = +4%, tốc chạy −6% (V −1.5) = −7.5%.

| | T0 | T1 | T2 | T3 | T4 |
|---|---|---|---|---|---|
| **Đơn giá vũ khí** (tiền/điểm) | 12 | 23 | 43 | 170 | 930 |
| Kiếm chuẩn: DPS gốc | — | 6.4 | 9.0 | 10.15 | 11.9 |
| Kiếm chuẩn: giá | — | 170 | 420 | 1 800 | 11 350 |

**Giá biên tham khảo** (trên kiếm chuẩn của tier):

| Thay đổi | T1 | T2 | T3 | T4 |
|---|---|---|---|---|
| +1 `MELEE_DAMAGE_BASE` | ~42 | ~70 | ~260 | ~1 330 |
| +0.1 `ATTACK_SPEED_BASE` | ~10 | ~28 | ~125 | ~810 |
| +1 điểm giáp chỉ số phụ (= +5%) | ~7 | ~19 | ~86 | ~553 |

### 3.3 Công cụ

```
Tốc đào = tốc vật liệu vanilla (gỗ 2 · đá 4 · đồng 5 · sắt 6 · kim cương 8 · netherite 9 · vàng 12) + MINING_EFFICIENCY
Điểm    = Tốc đào × Hệ số công dụng + MINING_SPEED / 100 + 0.5 × BREAK_STRENGTH
          × (1 + 0.05 × Σ V × giá trị của chỉ số phụ khác)
Giá     = Điểm × Đơn giá công cụ(T)
```

| Hệ số công dụng | Cuốc 1.0 · Rìu đốn 0.75 · Xẻng 0.5 · Cuốc đất 0.3 |
|---|---|

Đây là ngoại lệ duy nhất cho nguyên tắc "chỉ tính theo chỉ số". Attribute không thể hiện được món đào loại khối nào, nên cần hệ số công dụng. Sát thương cận chiến của công cụ **không tính giá**.

| | T0 | T1 | T2 | T3 | T4 |
|---|---|---|---|---|---|
| **Đơn giá công cụ** (tiền/điểm) | 15 | 30 | 56 | 190 | 790 |

Đơn giá T2 được hiệu chuẩn từ `miner_pickaxe`. Món này có `base` sắt nhưng Barrett bán ở S1, vì chưa có cuốc đồng.

## 4. Đối chiếu với giá đề xuất ở economy.md

| Món | Tier | Điểm | Đơn giá | **Giá theo chỉ số** | economy.md | Lệch |
|---|---|---|---|---|---|---|
| `recruit_helmet` | T1 | 1.20 | 72 | 85 | 130 | −34% |
| `recruit_chestplate` | T1 | 3.10 | 72 | 225 | 210 | +6% |
| `recruit_leggings` | T1 | 2.55 | 72 | 185 | 185 | −1% |
| `recruit_boots` | T1 | 1.85 | 72 | 135 | 105 | +27% |
| `guard_helmet` | T2 | 2.60 | 90 | 235 | 335 | −30% |
| `guard_chestplate` | T2 | 7.10 | 90 | 640 | 535 | +19% |
| `guard_leggings` | T2 | 5.40 | 90 | 485 | 465 | +5% |
| `guard_boots` | T2 | 2.60 | 90 | 235 | 265 | −12% |
| `azure_helmet` | T3 | 3.30 | 235 | 775 | 1 050 | −26% |
| `azure_chestplate` | T3 | 8.80 | 235 | 2 050 | 1 700 | +22% |
| `azure_leggings` | T3 | 6.35 | 235 | 1 500 | 1 450 | +3% |
| `azure_boots` | T3 | 3.05 | 235 | 715 | 840 | −15% |
| `bulwark_chestplate` | T4 | 11.15 | 1 370 | 15 300 (+615 enchant) | 12 750 | +25% |
| `recruit_sword` | T1 | 7.30 | 23 | 170 | 170 | 0% |
| `recruit_shortsword` | T1 | 6.82 | 23 | 155 | 155 | +1% |
| `recruit_hatchet` | T1 | 6.13 | 23 | 140 | 180 | −22% |
| `guard_sword` | T2 | 9.74 | 43 | 420 | 420 | 0% |
| `guard_shortsword` | T2 | 8.66 | 43 | 370 | 390 | −5% |
| `guard_axe` | T2 | 7.16 | 43 | 310 | 500 | −38% |
| `guard_hatchet` | T2 | 7.79 | 43 | 335 | 450 | −26% |
| `azure_sword` | T3 | 10.69 | 170 | 1 800 | 1 800 | 0% |
| `azure_axe` | T3 | 7.63 | 170 | 1 300 | 2 100 | −38% |
| `blackiron_sword` | T4 | 12.19 | 930 | 11 350 | 11 350 | 0% |
| `blackiron_hatchet` | T4 | 9.85 | 930 | 9 150 | 10 750 | −15% |
| `miner_pickaxe` | T2 | 8.40 | 56 | 470 | 475 | −1% |
| `miner_felling_axe` | T2 | 4.50 | 56 | 250 | 390 | −35% |
| `miner_shovel` | T2 | 3.00 | 56 | 170 | 250 | −33% |
| `azure_pickaxe` | T3 | 13.10 | 190 | 2 500 | 2 500 | 0% |
| `azure_felling_axe` | T3 | 7.50 | 190 | 1 400 | 1 950 | −27% |
| `azure_shovel` | T3 | 5.00 | 190 | 950 | 1 250 | −24% |
| `blackiron_pickaxe` | T4 | 17.90 | 790 | 14 150 | 14 150 | 0% |
| `blackiron_felling_axe` | T4 | 10.50 | 790 | 8 300 | 9 900 | −16% |
| `blackiron_shovel` | T4 | 7.00 | 790 | 5 550 | 7 100 | −22% |

**Đọc bảng:**
- **Tổng bộ giáp vẫn đúng ngân sách:** Recruit 630 · Guard 1 595 · Azure 5 040. Chỉ có cách chia giữa các món thay đổi: mũ rẻ đi, áo đắt lên, vì DEF tập trung ở áo.
- **Rìu chiến / rìu cầm tay rẻ hơn 15–38%**, vì DPS thấp hơn kiếm cùng tier khoảng 20–30% (ví dụ `azure_axe` 7.6 DPS so với `azure_sword` 10.2) mà lại không có quét cạnh. Nếu rìu có lợi thế chưa nằm trong attribute (đòn nặng, phá khiên…), hãy biến lợi thế đó thành attribute hoặc trait có giá. Còn nếu không có, rìu đang **yếu về chỉ số** chứ không phải bị định giá sai.
- **Rìu đốn / xẻng rẻ hơn 16–35%** vì hệ số công dụng thấp. Nếu thấy quá rẻ, tăng hệ số (ví dụ rìu đốn 0.9, xẻng 0.65) thay vì sửa giá tay.

Một số món ngoài shop để tham khảo: `diamond_longbow` ~1 700 · `diamond_crossbow` ~1 550 · `diamond_staff` ~1 950 · `netherite_glaive` ~11 150 · `knight_netherite_chestplate` ~18 350 · `copper_heater_shield` ~270.

## 5. Enchant

```
Giá enchant = [ Σ  Phí độ hiếm × Hệ số cấp(lv) ] × (1 + 0.15 × (số enchant − 1))
            + giá chỉ số mà enchant cung cấp (theo mục 3, đơn giá tier của món)
```

- **Hệ số cấp** = lv^1.5 → I: 1 · II: 2.83 · III: 5.2 · IV: 8 · V: 11.2.
- **Phụ phí số lượng:** mỗi enchant thêm sau enchant đầu tiên cộng +15% phần phí độ hiếm, vì gom được nhiều enchant trên một món là hiếm.
- **Enchant cấp chỉ số** (có `provideAttributes`) tính thêm giá chỉ số theo bảng 2.2. Các enchant này thường xếp vào độ hiếm Thường, vì phần lớn giá trị đã nằm trong chỉ số.

| Độ hiếm | Phí cấp I | Enchant |
|---|---|---|
| **Thường** | 150 | Aqua Affinity¹, Respiration¹, Depth Strider¹, Feather Falling¹, Swift Sneak¹, Sweeping Edge¹, Frost Walker, Soul Speed, Knockback, Punch, Lure, Luck of the Sea, Quick Charge, Piercing, Riptide, Lunge, Glowing (không tính cấp) |
| **Hiếm** | 450 | Fire Protection¹, Blast Protection, Projectile Protection, Thorns, Smite, Bane of Arthropods, Fire Slayer, Impaling, Fire Aspect, Flame, Ice Aspect, Efficiency, Silk Touch, Multishot, Retrieval, Wind Burst, Channeling, Density |
| **Sử thi** | 1 500 | Bleeding, Poisoning, Smash, Regicide, Looting, Fortune, Epoch |
| **Huyền thoại** | 5 000 | Lifesteal, Regeneration, Mending |
| **Infusion** | 150 | Vigor¹, Focus¹, Fortitude¹, Perspicacity¹, Celerity¹. Người chơi tự nâng bằng `copper_ingot_currency` (tối đa cấp 4). Chỉ tính giá khi món được bán kèm sẵn |
| **Lời nguyền** | — | Exhaustion −10%/cấp · Uncritable −20% · Irreparable −25% (nhân vào **tổng giá món**) |
| **Không bán** | — | Greed, Explosive (enchant test) |

¹ Có `provideAttributes`, nên cộng thêm giá chỉ số.

**Ví dụ:**
- `bulwark_chestplate` có Fire Protection I: 450 × 1 + (`BURNING_TIME` −0.1 → 0.12 điểm × 1 370 = 164) ≈ **615**.
- Kiếm T3 có Smite III + Looting II: (450 × 5.2 + 1 500 × 2.83) × 1.15 = (2 340 + 4 245) × 1.15 ≈ **7 550**.

## 6. Gem

Hiện **chưa có gem nào được định nghĩa** trong `server_data/items`. `speed_gem` và `defense_gem` được thưởng trong quest nhưng chưa có file. Công thức dưới đây theo cấu trúc `gem_data` (chỉ số theo từng slot + `success_chance`).

```
Giá gem (đứng riêng)  = (Phí gem + Điểm gem × Đơn giá(tier gem)) × (0.6 + 0.4 × success_chance)
Giá món đã khảm       = Giá món tính lại với chỉ số gem cộng vào (đơn giá tier của món) + Phí gem
```

| Độ hiếm gem | Phí gem | Tier dùng khi gem đứng riêng |
|---|---|---|
| Thường | 200 | T1 |
| Hiếm | 600 | T2 |
| Sử thi | 2 000 | T3 |
| Huyền thoại | 6 000 | T4 |

- **Điểm gem** tính theo loại món của slot mà gem khảm vào (giáp dùng mục 3.1, vũ khí dùng 3.2). Gem có chỉ số cho nhiều slot thì lấy **slot đắt nhất**.
- Khi khảm, chỉ số gem được tính theo **đơn giá tier của món**. Cùng một gem khảm lên đồ T4 sẽ làm món tăng giá nhiều hơn khảm lên đồ T1. Đúng giá trị thực, vì chỉ số trên đồ endgame đáng giá hơn.
- `success_chance` thấp làm gem rẻ đi, vì người mua chịu rủi ro.
- **Ô khảm trống** (`gem_socket`): +5% giá món mỗi ô.

**Ví dụ:** gem Hiếm "+1 DEF, +2% tốc chạy" cho giáp, `success_chance` 0.8.
- Điểm = 1 + 0.5 = 1.5.
- Đứng riêng: (600 + 1.5 × 90) × 0.92 ≈ **675**.
- Khảm vào `azure_chestplate` (T3): +1.5 × 235 + 600 ≈ **+950** vào giá áo.

## 7. Điều chỉnh khác

- **Độ bền:** chưa tính vào giá. Các tier đang dùng độ bền chuẩn (giáp ngực 155 / 840 / 1 200 / 1 560, kiếm 156 / 840 / 1 200 / 1 560). Nếu một món lệch > ±30% so với chuẩn của tier, có thể nhân thêm `(độ bền / độ bền chuẩn)^0.25` (tuỳ chọn).
- **Làm tròn:** như economy.md, dưới 1 000 làm tròn tới bội số 5, từ 1 000 trở lên làm tròn tới bội số 50.
- **Tier sau Netherite** (theo `weapon-stat-budget-spec`, mỗi tier chỉ +5% DPS): đơn giá nhân ×3 mỗi tier. Như vậy +5% chỉ số vẫn tốn nhiều giờ chơi hơn.

## 8. Quy trình định giá một món mới

1. Xác định **tier** từ `base` (hoặc khai báo tay).
2. Xác định **loại món**: giáp/khiên, vũ khí, công cụ.
3. Tính **điểm** theo mục 3, với V lấy từ bảng 2.2.
4. Giá = điểm × đơn giá tier.
5. Cộng giá **enchant** (mục 5) và **gem** (mục 6). Áp **lời nguyền** và **ô khảm trống**.
6. Làm tròn.
7. Kiểm tra: một kit (bộ giáp + vũ khí chính) của tier có nằm trong ngân sách giờ ở economy.md không. Lệch nhiều thì sửa **chỉ số hoặc V**, không sửa giá tay.

## 9. Khoảng trống trong code / dữ liệu

- **`SpeedPercent.getEnumConstant()` trả về `Attributes.SPEED_FLAT`** thay vì `SPEED_PERCENT` (`attribute/impl/SpeedPercent.java`). Có thể là lỗi copy-paste, cần kiểm tra.
- **Chưa có trường tier.** Các món `STICK` (trượng/đũa amethyst, emerald, lapis, redstone), `mace`, `trident` không suy ra được tier. Nên thêm trường (ví dụ `"tier": 3`) vào item JSON nếu muốn tính giá tự động.
- **Chưa có gem nào trong data**, nên mục 6 chưa kiểm chứng được với món thật.
- **Chưa có công cụ tính giá.** Mọi số ở đây được tính bằng script ngoài plugin. Bước tiếp theo hợp lý là thêm validator hoặc lệnh `/price <item>` vào plugin, đọc bảng V và đơn giá từ một file JSON. Nên làm chung với validator của `weapon-stat-budget-spec`.

## Lịch sử thay đổi

| Ngày | Thay đổi |
|---|---|
| 2026-10-27 | Tạo tài liệu: bảng V cho 40 attribute, đơn giá theo tier, công thức giáp/vũ khí/công cụ, enchant theo độ hiếm + số lượng, gem = phí + chỉ số. Hiệu chuẩn theo giá kiếm chuẩn, bộ giáp và cuốc ở economy.md (mốc S1 = 700/giờ). |
