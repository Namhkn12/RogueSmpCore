# Chuỗi sản xuất — vật liệu, máy móc và các điểm chặn tiêu tiền

> Xây trên [stat-standard.md](stat-standard.md) (giá trị V của món = ĐSM × G) và [income.md](income.md) (thu nhập/giờ, bản vị năng lượng). File này trả lời: **một món đồ được làm ra từ những gì, mỗi thứ giá bao nhiêu, và tiền của người chơi đi vào đâu** trên đường từ quặng tới món đồ hoàn chỉnh.
>
> Sửa lại một phần income.md §3.1 và §3.3 (xem mục 12).

## 1. Vấn đề

- ~80% trang bị do người chơi tự chế tạo, tự enchant, tự khảm gem.
- Hiện tại tiền chỉ đi vào **nhiên liệu máy → quặng cao cấp**. Các bước khác gần như miễn phí:

| Bước | Hiện tại tốn gì | Tốn tiền? |
|---|---|---|
| Chế tạo | quặng (+ nguyên liệu vanilla) | chỉ qua nhiên liệu |
| Enchant (`EnchantingGui`) | cấp XP (cấp enchant × 5) | ✗ |
| Khảm gem (`GemSocketingGui`) | chỉ viên gem, có tỷ lệ thành công | ✗ |
| Tháo gem (`GemSocketUtil.removeGem`) | không gì | ✗ |
| Infusion (`InfusionGui`) | exp + `copper_ingot_currency` | gián tiếp |
| Sửa đồ (`ItemRepairGui`) | nguyên liệu sửa khai báo theo món | tuỳ nguyên liệu |

→ Cần **chặn giữa các bước bằng vật phẩm phải mua**: linh kiện khi chế tạo, vật tư khi hoàn thiện. Tiền không bị trừ thẳng trong GUI, mà đi qua vật phẩm. Nhờ vậy vật phẩm đó vẫn trao đổi được, rơi được từ loot/event, và định giá được.

## 2. Nguyên tắc

1. **Mọi món đều chia ngân sách theo giá trị V** (stat-standard §5): **Quặng 40% · Linh kiện 30% · Công sức 30%**. Hai phần đầu là tiền, phần công sức là thời gian tự kiếm (đào, nhặt nguyên liệu vanilla, chế tạo), không thu tiền.
2. **Quặng là "khung", linh kiện là "phần tiêu tiền".** Quặng tốn nhiên liệu máy. Linh kiện tốn vật liệu cao cấp mua ở NPC. Hai đường tiêu khác nhau, nên không có một nút thắt duy nhất.
3. **Hoàn thiện tốn 35% phần giá trị nó thêm vào.** Enchant, gem hay đục ô làm món đắt lên ΔV, thì người chơi trả 35% ΔV bằng vật tư. 65% còn lại là "lãi" của việc tự làm.
4. **Đơn vị công thức tính theo T(b), không theo số cụ thể.** Tỷ lệ giữa các slot (kiếm, áo, cuốc…) không đổi giữa các bậc, nên một công thức mẫu dùng được cho mọi bậc từ B4 đến B8.
5. **Bậc đầu (B1–B3) gần như miễn phí.** Quặng và vật liệu là vanilla, tiền chỉ đi vào vật tư hoàn thiện, đường tắt villager và đầu tư xưởng (income §5).

## 3. Bản đồ chuỗi

```
TẦNG 0  Năng lượng ── nhiên liệu (mua NPC / than cốc tự làm) ──► lò phản ứng
           │
TẦNG 1  Máy khai thác ──► tài nguyên vanilla (thỏi, gỗ, da, tơ…)            [B1–B3, công sức]
           │
TẦNG 2  Máy sinh tài nguyên: thỏi vanilla ─► Nén ─► Chất lượng cao ─► Nguyên chất ─► Ma thuật
           │                                      (mỗi bước tốn năng lượng)              [QUẶNG 40%]
           │
TẦNG 3  Linh kiện = vật liệu bậc thấp (tự kiếm) + vật liệu bậc cao (mua NPC)        [LINH KIỆN 30%]
           │
TẦNG 4  Trang bị (khung) = thỏi nâng cấp + linh kiện (+ thỏi vanilla làm khung, miễn phí)
           │
TẦNG 5  Hoàn thiện: enchant · đục ô · khảm/tháo gem · infusion · sửa     [VẬT TƯ = 35% ΔV]
```

## 4. Phân loại vật phẩm

| Nhóm | Loại | Vai trò | Nguồn | NPC bán? | Ví dụ hiện có |
|---|---|---|---|---|---|
| **Quặng** (không phải material) | Thỏi vanilla | Đầu vào của máy, khung đồ B1–B3 | khai thác | 1× giá trị (B1–B3) | `minecraft:iron_ingot` |
| | Thỏi nâng cấp (4 cấp) | Khung đồ B4–B8 | máy sinh tài nguyên | 3× giá thành | `iron_compressed_ingot`… `netherite_magic_ingot` |
| **Material** | Bậc thấp | Đầu vào của linh kiện, phần "công sức" | vanilla, mob thường | 1× (tiện lợi) | tơ, da, gỗ, que |
| | Bậc cao | **Nơi tiêu tiền chính.** Đầu vào của linh kiện | **NPC**, ít từ loot | ✓ | *chưa có*, đề xuất ở §6.3 |
| | Linh kiện (thành phẩm) | Ghép vào công thức trang bị | tự ghép, hoặc mua sẵn ×1.25 | ✓ | `crafting_parts/*` (Fine/Superior/Masterwork) |
| | Đặc biệt | Mở công thức biến thể có trait | boss, dungeon | ✗ (chỉ trao đổi) | *chưa có* |
| | Event | Công thức thay thế / cosmetic | event | ✗ | *chưa có* |
| **Máy** | Vật liệu máy | Chế tạo máy (đầu tư một lần) | NPC + thỏi | ✓ | *chưa có* |
| | Buff máy | Module (vĩnh viễn), xúc tác (tiêu hao) | NPC, loot | ✓ | `catalyst_types/pure`, `volatile` |
| | Nhiên liệu | Năng lượng | NPC, than cốc | ✓ | `coke_oven`, tag `pure_fuel`, `volatile_fuel` |
| **Vật tư hoàn thiện** (đề xuất mới) | Mực, keo khảm, kìm tháo | Chặn giữa các bước hoàn thiện | NPC | ✓ | *chưa có*, xem §8 |

"Vật tư hoàn thiện" có thể coi là một nhánh của material bậc cao. Mình tách riêng vì nó không nằm trong công thức chế tạo mà nằm trong GUI hoàn thiện.

## 5. Đơn vị công thức T(b)

`T(b)` = giá trị trung bình của **1 thỏi** ở bậc b = (giá kit + cuốc) / 29 (income §3.1).

| Bậc | B1 | B2 | B3 | B4 | B5 | B6 | B7 | B8 |
|---|---|---|---|---|---|---|---|---|
| **T(b)** | 6 | 27 | 93 | 310 | 1 014 | 2 184 | 4 698 | 10 017 |

Giá trị mỗi slot tính theo T **gần như không đổi qua các bậc** (từ stat-standard §5.2):

| Slot | V / T | Slot | V / T |
|---|---|---|---|
| Vũ khí chính (kiếm, trượng, cung, nỏ) | ~10.8 | Áo ngực | ~4.0 |
| Cuốc | ~8.3 | Quần | ~3.0 |
| Rìu đốn (× 0.75) | ~6.2 | Mũ / Giày | ~1.5 mỗi món |
| Xẻng (× 0.5) | ~4.2 | Khiên | ~2.4 |

→ Công thức mẫu viết theo T thì dùng lại được cho mọi bậc. Chỉ cần đổi cấp thỏi và cấp linh kiện.

## 6. Quặng và linh kiện

### 6.1 Thang thỏi nâng cấp

**Đề xuất (cần chốt, xem §13):** cấp tinh luyện quyết định bậc. Kim loại quyết định **dòng đồ** (hương vị chỉ số), không quyết định bậc.

| Cấp | Bậc | Đầu vào | **Giá thành** (0.4 T) | Villager (3×) |
|---|---|---|---|---|
| Nén | B4 | thỏi vanilla: đồng 8 · sắt 4 · vàng 4 · kim cương 2 · netherite 1 | **125** | 375 |
| Chất lượng cao | B5 | 1 thỏi Nén | **405** | 1 200 |
| Nguyên chất | B6 | 1 thỏi CLC | **875** | 2 600 |
| Ma thuật | B7 | 1 thỏi Nguyên chất | **1 880** | 5 650 |
| *(B8)* | B8 | dùng thỏi Ma thuật, **số lượng ×2** | — | — |

- **Nén** là bước gộp nhiều → 1. Các bước sau là tinh luyện 1 → 1. Thỏi vanilla là công sức nên không tính tiền. Số lượng khác nhau giữa các kim loại chỉ để cân bằng độ khó kiếm.
- Gợi ý dòng đồ theo kim loại: đồng = nhẹ/nhanh · sắt = cân bằng/thủ · vàng = phép/may mắn · kim cương = chí mạng/sát thương · netherite = nặng/kháng. Mọi dòng cùng bậc có **cùng tổng ĐSM**, chỉ khác cách phân bổ.

### 6.2 Linh kiện

Các linh kiện trong `items/crafting_parts/` có 3 cấp. Mỗi cấp phủ 1–2 bậc. Bậc cao hơn trong cấp dùng **số lượng ×2**.

| Cấp | Bậc | Giá gốc tính theo |
|---|---|---|
| Fine | B4 | T(B4) |
| Superior | B5 (×1) · B6 (×2) | T(B5) |
| Masterwork | B7 (×1) · B8 (×2) | T(B7) |

Có hai loại linh kiện theo trọng số:

| Loại | Trọng số | Họ linh kiện | Fine | Superior | Masterwork |
|---|---|---|---|---|---|
| **Lõi** | 1.5 T | `energy_rod` (cận chiến) · `mana_core` (trượng) · `heartwood_shaft` (cung, nỏ, cán công cụ) | **465** | **1 500** | **7 050** |
| **Phụ** | 0.5 T | `silk_thread` (dây cung) · `bow_grip` (tay cầm) · `leather_lining` (lót giáp nhẹ) · `chain_lining` (lót giáp nặng) | **155** | **500** | **2 350** |

NPC bán linh kiện làm sẵn với giá ×1.25 (đường tắt, bỏ qua phần công sức).

### 6.3 Linh kiện làm từ gì

```
Linh kiện Phụ = vật liệu bậc thấp ×2–4 (tự kiếm)  + 1 vật liệu bậc cao cùng cấp
Linh kiện Lõi = vật liệu bậc thấp ×4–8 (tự kiếm)  + 3 vật liệu bậc cao cùng cấp
```

Giá 1 vật liệu bậc cao = 0.5 T, tức **155 / 500 / 2 350** theo cấp. Vật liệu bậc thấp là công sức, không tính tiền.

Đề xuất 3 nhóm vật liệu bậc cao × 3 cấp = 9 vật phẩm. Tên chỉ để minh hoạ:

| Nhóm | Dùng cho | Vật liệu bậc thấp đi kèm |
|---|---|---|
| Nhựa kết sợi | `silk_thread`, `bow_grip`, `leather_lining` | tơ, da, len |
| Thuốc hàn | `chain_lining`, `energy_rod` | xích, thỏi vanilla, blaze/breeze rod |
| Tinh chất bí thuật | `mana_core`, `heartwood_shaft` | gỗ, lapis, amethyst, magma cream |

## 7. Công thức mẫu theo slot (B4–B8)

Giá thành 1 thỏi = 0.4 T, Lõi = 1.5 T, Phụ = 0.5 T. Mục tiêu: tổng tiền ≈ **70% V** (±10%).

| Slot | V | Thỏi nâng cấp | Linh kiện | Tiền (T) | % V |
|---|---|---|---|---|---|
| Kiếm / vũ khí cận chiến | 10.8 T | 8 | `energy_rod` ×3 | 7.7 | 71% |
| Trượng | 10.8 T | 4 | `mana_core` ×4 | 7.6 | 70% |
| Cung | 10.8 T | 4 | `heartwood_shaft` ×2 · `silk_thread` ×3 · `bow_grip` ×3 | 7.6 | 70% |
| Nỏ | 10.8 T | 5 | `heartwood_shaft` ×2 · `silk_thread` ×3 · `bow_grip` ×2 | 7.5 | 69% |
| Cuốc | 8.3 T | 6 | `heartwood_shaft` ×2 · `leather_lining` ×1 | 5.9 | 71% |
| Rìu đốn | 6.2 T | 4 | `heartwood_shaft` ×1 · `leather_lining` ×2 | 4.1 | 66% |
| Xẻng | 4.2 T | 2 | `heartwood_shaft` ×1 · `leather_lining` ×1 | 2.8 | 67% |
| Áo ngực | 4.0 T | 3 | lót ×3 | 2.7 | 68% |
| Quần | 3.0 T | 3 | lót ×2 | 2.2 | 73% |
| Mũ | 1.5 T | 2 | lót ×1 | 1.3 | 87% |
| Giày | 1.5 T | 1 | lót ×1 | 0.9 | 60% |
| Khiên | 2.4 T | 2 | lót ×2 | 1.8 | 75% |

"Lót" = `leather_lining` (giáp nhẹ) hoặc `chain_lining` (giáp nặng). Mũ và giày lệch ngược chiều nhau, nên cả bộ giáp vẫn ra 71%.

**Quy tắc đi kèm:**
- **Bậc dùng cấp gì:** B4 = Nén + Fine · B5 = CLC + Superior · B6 = Nguyên chất + Superior ×2 · B7 = Ma thuật + Masterwork · B8 = Ma thuật ×2 + Masterwork ×2.
- **Thỏi vanilla làm khung:** được thêm tự do (ví dụ áo = 3 lót + 3 thỏi nâng cấp + 4 thỏi sắt). Phần này là công sức, không tính tiền, chỉ để công thức có "cảm giác" vanilla: lót bên trong, quặng bọc bên ngoài.
- **Kế thừa món bậc dưới** (tuỳ chọn): công thức có thể yêu cầu món cùng slot của bậc b−1. Khi đó trừ V(b−1) ≈ V(b)/3 khỏi phần quặng.
- **Linh kiện đặc biệt** (§9) thay 1 Lõi hoặc thêm vào công thức. Giá trị trait cộng vào V của món.

**Kiểm tra (kiếm):** B4 = 8×125 + 3×465 = 2 395 / 3 400 = 70% · B5 = 7 740 / 11 000 = 70% · B6 = 16 000 / 23 500 = 68% · B7 = 36 190 / 50 500 = 72% · B8 = 72 380 / 107 000 = 68%.

**B1–B3:** dùng cùng bảng, nhưng thỏi là thỏi vanilla và linh kiện là vật liệu bậc thấp. Tiền chế tạo ≈ 0.

## 8. Điểm chặn hoàn thiện

```
Số vật tư = round( 0.35 × ĐSM thêm vào × G(bậc món) / giá vật tư của dải ),  tối thiểu 1
```

Vật tư bán theo 4 dải. Giá = G của bậc thấp trong dải:

| Dải | B1–B2 | B3–B4 | B5–B6 | B7–B8 |
|---|---|---|---|---|
| Giá 1 vật tư | 10 | 60 | 400 | 1 600 |

| Bước | Vật tư | ĐSM dùng trong công thức | Ghi chú |
|---|---|---|---|
| **Enchant** | Mực phù phép | ĐSM của enchant (stat-standard §6) | Giữ chi phí XP như cũ, cộng thêm mực |
| **Đục ô khảm** | Keo khảm | 1 mỗi ô, **không nhân 0.35** | stat-standard tính ô trống = +1 × G. Đây là mở khoá nên thu đủ |
| **Khảm gem** | Keo khảm | ĐSM gem + 2 | **Tiêu mỗi lần thử, kể cả khi thất bại** |
| **Tháo gem** | Kìm tháo | 0.15 × (ĐSM gem + 2), thay cho 0.35 | Không có kìm thì gem vỡ khi tháo |
| **Sửa đồ** | Thỏi nâng cấp cùng bậc | — | Sửa đầy = 6% V. Đặt `percent` của nguyên liệu sửa = giá thành thỏi / (6% V) × 100. Không cần vật phẩm mới, `DurabilityRepairComponent` đã hỗ trợ |
| **Infusion** | `copper_ingot_currency` | — | Đã có. Cần chốt giá: đề xuất coi như vật tư dải B1–B2 (10) |
| **Bản vẽ** (tuỳ chọn, B6+) | Bản vẽ theo dòng đồ | — | Mua một lần mỗi dòng đồ mỗi bậc, giá 0.25 V. Đây là sink end game. `gui/interfaces/IHaveBlueprint` đã có sẵn |

**Vì sao 35%:** income §5 tính chi tiêu "enchant/gem/nâng cấp" bằng ×1.3 trên phần quặng 70%, tức ~21% V. Một món hoàn thiện điển hình thêm ~60% V (vài enchant và 1–2 gem). 35% × 60% = 21%, khớp với mô hình.

## 9. Material đặc biệt và event

**Đặc biệt** (boss, dungeon):
- Chỉ dùng cho **công thức biến thể** có trait (stat-standard §5.1: lợi thế phải có ĐSM).
- Giá trị = ĐSM trait × G(bậc). NPC không bán, người chơi trao đổi với nhau được.
- Tỷ lệ rơi: thời gian farm kỳ vọng = giá trị / I(bậc). Ví dụ trait 3 ĐSM ở B5 = 1 200 → ~22 phút farm ở B5 (I = 3 300).

**Event:**
- Không bao giờ đẩy ĐSM vượt ngân sách bậc.
- Dùng vào một trong hai việc: **công thức thay thế** (thay tối đa 50% linh kiện trong công thức) hoặc **cosmetic** (skin).
- Giá trị = giá phần nó thay thế. Giới hạn 50% để event không xoá mất khoản tiêu linh kiện.
- Hết event thì công thức đóng, vật liệu vẫn giữ.

## 10. Máy móc

### 10.1 Ba loại máy

| Loại | Đầu vào → đầu ra | Bậc | Code hiện có |
|---|---|---|---|
| **Khai thác** | năng lượng → tài nguyên vanilla (đặt khối, rơi đồ) | B1–B3 | `generator`: `coal_generator`, `raw_iron_generator`, `cobblestone_generator` |
| **Sinh tài nguyên** | thỏi + năng lượng → thỏi cấp cao hơn | B4–B8 | **chưa có loại block** |
| **Tạo năng lượng** | nhiên liệu → NL | mọi bậc | `energy_generator` (`arcane_reactor`), `wireless_node` (`energy_relay`), `coke_oven` |

### 10.2 Thông số máy sinh tài nguyên

1 kNL = 2 tiền (income §3.2). `arcane_reactor` truyền tối đa 20 NL/tick.

| Máy | NL mỗi thỏi | NL/tick | Thời gian | Thỏi/giờ | Tiền NL/giờ (chạy liên tục) |
|---|---|---|---|---|---|
| Lò nén (→ Nén) | 62 500 | 5 | 12 500 ticks (10.4′) | 5.8 | 720 |
| Lò tinh luyện (→ CLC) | 140 000 | 10 | 14 000 ticks (11.7′) | 5.1 | 1 440 |
| Lò thanh tẩy (→ Nguyên chất) | 235 000 | 15 | 15 700 ticks (13.1′) | 4.6 | 2 160 |
| Lò ma thuật (→ Ma thuật) | 500 000 | 20 | 25 000 ticks (20.8′) | 2.9 | 2 880 |

NL mỗi thỏi = (giá thành cấp này − giá thành thỏi đầu vào) / 2 × 1 000.

### 10.3 Giá máy và vật liệu máy

```
Giá máy sinh tài nguyên bậc b = 5 × T(b)   →  1 550 · 5 050 · 10 900 · 23 500
Thành phần: ~30% thỏi bậc dưới + ~70% vật liệu máy (khung máy, mạch truyền năng) mua NPC
```

So với mua thỏi ở villager, máy hoàn vốn sau ~6 thỏi (ví dụ B5: tiết kiệm 1 200 − 405 = 795 mỗi thỏi). Máy khai thác B1–B3 nằm trong khoản "đầu tư xưởng" 6 000 của income §5.

### 10.4 Buff máy

- **Module** (vĩnh viễn, cắm vào `module_slots`): giảm giá thành tối đa **20%** (income §3.3), hoặc tăng tốc với NL/tick tăng tương ứng. Giá = tiền tiết kiệm được trong **20 giờ** chạy ở mức dùng chuẩn (~1.5–2 thỏi/giờ).
- **Xúc tác** (tiêu hao, `catalyst_types`): đổi hình dạng sản lượng (nhanh hơn, x2 thỏi có xác suất…). Giá = **50% lợi ích kỳ vọng** mỗi lần dùng.
- **Nhiên liệu:** giữ bậc thang ở income §3.2.

## 11. Quy trình thiết kế một món mới

1. Chọn **bậc** và **slot**. Lấy V từ stat-standard §5.2 (hoặc tính ĐSM × G nếu chỉ số lệch chuẩn).
2. Lấy công thức mẫu ở §7. Chọn kim loại theo dòng đồ, chọn lót nhẹ hay nặng.
3. Có trait? Thêm 1 material đặc biệt và cộng giá trị trait vào V.
4. Tính tiền công thức = số thỏi × giá thành + Σ linh kiện. Nằm trong 60–80% V là đạt.
5. Thêm thỏi vanilla làm khung nếu muốn, không tính tiền.
6. Đặt nguyên liệu sửa: thỏi cùng bậc, `percent` theo §8.
7. NPC có bán sẵn không? Nếu có: B1–B2 ×1.0 · B3–B4 ×1.25 · B5+ ×2.0 (income §3.1b).
8. Ghi vào lịch sử thay đổi của file liên quan.

**Ví dụ: kiếm sắt (dòng cân bằng) B5, có Fire Aspect và 1 ô gem**

| Bước | Chi tiết | Tiền |
|---|---|---|
| Công thức | 8 thỏi sắt CLC (8 × 405) + `energy_rod_superior` ×3 (3 × 1 500) | 7 740 |
| Đục 1 ô | 1 keo khảm dải B5–B6 | 400 |
| Enchant Fire Aspect (rank C, ~4 ĐSM) | round(0.35 × 4 × 400 / 400) = 1 mực | 400 |
| Khảm gem 2 ĐSM, tỷ lệ thành công 0.8 | round(0.35 × 4) = 1 keo/lần, kỳ vọng 1.25 lần | 500 |
| **Tổng** | | **9 040** ≈ 3.9 giờ thu nhập B4 |

## 12. Ảnh hưởng lên income.md

| Mục | Thay đổi |
|---|---|
| §3.1 Giá thành quặng | 70% → **40%** giá trị. 30% chuyển sang linh kiện. Villager vẫn = 3× giá thành **mới** (B4 375 thay cho 650) |
| §3.3 Máy quặng | NL mỗi thỏi giảm theo §10.2. Tiền NL/giờ mỗi máy giữ nguyên |
| §5 Chi tiêu mỗi bậc | Tổng không đổi (quặng + linh kiện vẫn = 70% V). Hệ số ×1.3 nay chính là vật tư hoàn thiện (§8) |
| **Khoản tiêu mới chưa có trong mô hình** | Sửa đồ (~0.75% giá kit/giờ, B5 ≈ 160/giờ ≈ 5% I) · máy sinh tài nguyên B4+ (5 T mỗi máy, ≈ 20% chi tiêu của bậc trước) · bản vẽ B6+. Cần cộng vào income §5: tăng I hoặc chấp nhận mỗi bậc lâu hơn |

## 13. Cần chốt

1. **Cấp tinh luyện ↔ bậc.** Đề xuất ở §6.1: cấp tinh luyện = bậc, kim loại = dòng đồ. Nếu muốn kim loại cũng góp vào bậc, chỉ cần sửa bảng §6.1. Các mục khác tính theo T nên không đổi.
2. Tỷ lệ **40 / 30 / 30** (quặng / linh kiện / công sức).
3. Phí hoàn thiện **35%**, và khảm gem có tiêu vật tư khi thất bại không.
4. Sửa đồ bằng thỏi nâng cấp hay bằng vật tư riêng.
5. Có làm **bản vẽ** không.
6. Giá máy 5 T có được tính vào income §5 không.

## 14. Khoảng trống trong code / data

- **Chưa có loại block máy sinh tài nguyên** (thỏi → thỏi). `generator` hiện chỉ đặt khối rồi rơi đồ.
- **`EnchantingGui`** chỉ trừ cấp XP. Cần thêm yêu cầu vật tư.
- **`GemSocketingGui` / `GemSocketUtil`** không tiêu gì khi khảm, tháo gem miễn phí, chưa có thao tác đục ô.
- **`crafting_parts/*`** chưa có công thức chế tạo, chưa có vật liệu bậc cao, chưa có trường bậc.
- **20 file thỏi nâng cấp** (`items/*_ingot.json`) khai báo `"id": "ores/..."` nhưng nằm ở gốc `items/`. Registry dùng ID theo path và log cảnh báo. Nên sửa `id` thành tên file.
- `crafting_recipes/iron_compressed_block.json` và `copper_claymore_1.json` là công thức test (9 thỏi → khối, 9 khối → claymore), chưa theo bảng §7.

## Lịch sử thay đổi

| Ngày | Thay đổi |
|---|---|
| 2026-10-27 | Tạo tài liệu: phân loại material/máy, đơn vị T(b), chia ngân sách 40/30/30, thang thỏi Nén → Ma thuật, giá linh kiện Lõi/Phụ theo 3 cấp, công thức mẫu 12 slot, vật tư hoàn thiện 35% ΔV, thông số và giá máy sinh tài nguyên. |
