# Làm lại shop Roland, Drake, Barrett

> Ba shop giờ chỉ bán **đồ bậc thấp** (low tier stuff) và **nguyên liệu bậc thấp** (low tier material) cho công thức Nén và Chất lượng cao. Giá tính theo [stat-standard.md](stat-standard.md): ĐSM × G(bậc), +1 × G cho mỗi ô khảm, NPC B1–B2 ×1.0 · B3 ×1.25. Giá ngọc lấy từ [gems.md](gems.md) mục 7.

## 1. Thư mục

| Thư mục | Nội dung |
|---|---|
| `items/low_tier_stuff/` | Giáp, kiếm, công cụ bán ở shop. Tên = tên NPC + loại đồ. Mỗi món có 1 ô khảm (`"socket": 1`) |
| `items/low_tier_materials/` | Nguyên liệu công thức, base `PAPER` nên không mặc/cầm dùng được |
| `trades/armor_roland.json`, `weapon_drake.json`, `tool_barrett.json` | Shop mới |

## 2. Armorer Roland

Giáp model sắt, tổng **13 DEF** (giáp sắt vanilla là 15, giáp đồng của server 9.2).

| Item | Slot | DEF | Độ bền | Bậc | Giá |
|---|---|---|---|---|---|
| `rolands_helmet` | Mũ | 2.0 | 420 | B2.6 | 130 |
| `rolands_chestplate` | Áo | 5.2 | 600 | B2.5 | 250 |
| `rolands_leggings` | Quần | 3.8 | 570 | B2.5 | 180 |
| `rolands_boots` | Giày | 2.0 | 480 | B2.6 | 130 |

| Ngọc | Giá |
|---|---|
| `gem_onyx_1` Rough Onyx (phòng ngự) | 70 |
| `gem_rose_quartz_1` Rough Rose Quartz (máu) | 65 |
| `gem_onyx_2` Flawed Onyx (cấp trung) | 170 |

Nguyên liệu (khung giáp, không mặc được):

| Giáp da | Giá | Giáp xích | Giá |
|---|---|---|---|
| `raw_leather_cap` | 10 | `loose_chain_coif` | 20 |
| `raw_leather_tunic` | 25 | `loose_chain_hauberk` | 50 |
| `raw_leather_pants` | 20 | `loose_chain_leggings` | 40 |
| `raw_leather_boots` | 10 | `loose_chain_boots` | 20 |

Giá nguyên liệu = tỷ lệ slot (1.5 / 4 / 3 / 1.5) × T, với T ≈ 6 (da, B1) và ≈ 13 (xích).

## 3. Weaponsmith Drake

Không yếu hơn kiếm gỗ (DPS 6.5) và kiếm đá (7.1). Kiếm ngắn đá 6.9, kiếm dài đá 6.6.

| Item | Model | Sát thương | Tốc | DPS | Tầm | Quét | Độ bền | Bậc | Giá |
|---|---|---|---|---|---|---|---|---|---|
| `drakes_shortsword` | kiếm ngắn đồng | 3.7 | 2.0 | 7.40 | +0.2 | 0.2 | 160 | B1.9 | 310 |
| `drakes_longsword` | kiếm dài sắt | 4.9 | 1.5 | 7.35 | +0.7 | 0.25 | 210 | B2.2 | 450 |

| Ngọc | Giá |
|---|---|
| `gem_ruby_1` Rough Ruby | 85 |
| `gem_amber_1` Rough Amber | 85 |
| `gem_garnet_1` Rough Garnet | 85 |
| `gem_ruby_2` Flawed Ruby (cấp trung) | 220 |

| Nguyên liệu | Model | Giá |
|---|---|---|
| `sword_core` Sword Core | end rod | 40 |
| `bow_grip_crude` Crude Bow Grip | `smp:bow_grip` | 15 |

## 4. Toolsmith Barrett

Công cụ base sắt nên đào khối vanilla với tốc độ của sắt (tốc đào 6 = **B3**). Đây là lý do giá cao hơn hai shop kia. Chỉ số custom thấp hơn cuốc sắt của server (BREAK_STRENGTH 3, MINING_SPEED 160, độ bền 250).

| Item | Sát thương | Tốc | Chỉ số khác | Độ bền | Giá |
|---|---|---|---|---|---|
| `barretts_pickaxe` | 3.5 | 1.2 | BREAK_STRENGTH 2 · MINING_SPEED 140 | 220 | 1 350 |
| `barretts_axe` | 6.5 | 0.95 | | 220 | 780 |
| `barretts_shovel` | 3.0 | 1.0 | | 220 | 545 |

| Ngọc | Giá |
|---|---|
| `gem_jade_1` Rough Jade (tốc đào) | 75 |
| `gem_jade_2` Flawed Jade | 200 |

Jade là ngọc công cụ duy nhất hiện có, nên shop chỉ bán 2 ngọc.

| Nguyên liệu | Model | Giá |
|---|---|---|
| `hemp_rope` Hemp Rope | dây dắt (lead) | 10 |
| `hardwood_stick` Hardwood Stick (cán ngắn) | stick | 5 |
| `bamboo_pole` Bamboo Pole (cán dài) | bamboo | 8 |

## 5. Giao diện

Một lớp viền kính đen bao quanh, item xếp lần lượt từ trái sang phải bên trong, ô thừa là kính xám. Thứ tự: đồ → ngọc → nguyên liệu.

```
Roland (5 hàng)     Drake, Barrett (4 hàng)
#########           #########
#OOOOOOO#           #OOOOOOO#
#OOOOOOO#           #OOOOOOO#
#OOOOOOO#           #########
#########
```

## 6. Item cũ đã xoá

Xoá khỏi `server_data`. Trên server thật cần xoá các file sau (49 file). Không có công thức, quest hay loot table nào dùng chúng.

**`items/hub_shop/`** (37):
`bastion_boots` `bastion_chestplate` `bastion_helmet` `bastion_leggings` `deepdelver_felling_axe` `deepdelver_pickaxe` `deepdelver_shovel` `earthsplitter_felling_axe` `earthsplitter_pickaxe` `earthsplitter_shovel` `hunters_knife` `ironclad_boots` `ironclad_chestplate` `ironclad_helmet` `ironclad_leggings` `ironforge_felling_axe` `ironforge_pickaxe` `ironforge_shovel` `knights_longsword` `militia_sword` `padded_boots` `padded_chestplate` `padded_helmet` `padded_leggings` `prospector_felling_axe` `prospector_pickaxe` `prospector_shovel` `ravager_axe` `scale_boots` `scale_chestplate` `scale_helmet` `scale_leggings` `sentinel_boots` `sentinel_chestplate` `sentinel_helmet` `sentinel_leggings` `sovereign_blade`

**`items/crafting_parts/`** (12):
`chain_lining_fine` `chain_lining_superior` `chain_lining_masterwork` `energy_rod_fine` `energy_rod_superior` `energy_rod_masterwork` `leather_lining_fine` `leather_lining_superior` `leather_lining_masterwork` `mana_core_fine` `mana_core_superior` `mana_core_masterwork`

**Giữ lại:** `heartwood_shaft_fine/superior/masterwork` (shop cung Hawthorne vẫn bán), cùng các item cung trong `hub_shop/` và `bow_grip_*`, `silk_thread_*` trong `crafting_parts/`.
## Lịch sử thay đổi

| Ngày | Thay đổi |
|---|---|
| 2026-10-29 | Đơn giản layout: chỉ còn viền ngoài, item xếp lần lượt bên trong. |
| 2026-10-28 | Làm lại 3 shop Roland, Drake, Barrett chỉ bán đồ và nguyên liệu bậc thấp. Thêm 9 món đồ và 9 nguyên liệu mới, cập nhật mô tả 4 nguyên liệu có sẵn (`sword_core`, `bow_grip_crude`, `hemp_rope`, `hardwood_stick`). Xoá 49 item cũ. |
