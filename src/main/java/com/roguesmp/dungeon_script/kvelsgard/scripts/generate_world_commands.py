#!/usr/bin/env python3
"""
Kvelsgard — sinh lệnh FAWE: barrier, dán 4 schem theo dải Y, biome.
Chạy (từ thư mục kvelsgard/):  python3 scripts/generate_world_commands.py
Kết quả: commands/*.txt. Mọi lệnh <= 256 ký tự. Khối đảo nằm hết trong schem (scripts/generate_world.py).
"""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import kv_model as M

OUT = HERE.parent / "commands"
MAXLEN = 256
PREFIX = "kvelsgard"
RIN, ROUT = M.BARRIER_R
B = int(ROUT) + 1

phases = {}
def phase(name):
    phases[name] = []
    return phases[name]

def sel(lst, a, b):
    lst.append(f"//pos1 {a[0]},{a[1]},{a[2]}")
    lst.append(f"//pos2 {b[0]},{b[1]},{b[2]}")

def check(lst):
    for c in lst:
        if not c.startswith("#") and len(c) > MAXLEN:
            raise SystemExit(f"Lệnh quá dài ({len(c)}): {c}")

p = phase("00_barrier")
p.append(f"# Tường barrier r {RIN}-{ROUT} quanh đảo (mép đảo xa nhất r117), từ Y-64 đến 319 + sàn và nắp")
sel(p, (-B, -64, -B), (B, 319, B))
p.append(f"//g -r barrier r=sqrt(x*x+z*z);r>{RIN}&&r<{ROUT}")
p.append(f"//g -r barrier y==-64&&x*x+z*z<{int(ROUT * ROUT)}")
p.append(f"//g -r barrier y==319&&x*x+z*z<{int(ROUT * ROUT)}")

p = phase("01_paste_island")
p.append("# Toàn bộ khối đảo, sinh bằng scripts/generate_world.py -> output/kvelsgard_l1..l5.schem")
p.append("# Chép 5 file .schem vào plugins/FastAsyncWorldEdit/schematics/ rồi chạy (đứng đâu cũng được, -o dán đúng gốc lưu trong file).")
p.append("# 5 dải Y không chồng nhau nên thứ tự nào cũng được. Dán lại 1 dải sau khi sửa script cũng không đụng dải khác.")
for key, label in (("l5", f"Y{M.Y0}..{M.SEAL[4] - 1}: phòng boss Cổ Thành Câm Lặng + vỏ bedrock + đáy đảo"),
                   ("l4", f"Y{M.SEAL[4]}..{M.SEAL[3] - 1}: Vực Deepslate (tầng 4) + tấm bedrock Y{M.SEAL[4]}"),
                   ("l3", f"Y{M.SEAL[3]}..{M.SEAL[2] - 1}: Rừng Thạch Nhũ (tầng 3) + tấm bedrock Y{M.SEAL[3]}"),
                   ("l2", f"Y{M.SEAL[2]}..{M.SEAL1_MAX}: Hầm Mỏ Cũ (tầng 2) + tấm bedrock Y{M.SEAL[2]} và tấm dưới Layer 1"),
                   ("l1", f"Y{M.SEAL1_MAX + 1}..đỉnh: núi, bãi mỏ, đường leo, hang dẫn, nửa trên hố")):
    p.append(f"# {label}")
    p.append(f"//schem load {PREFIX}_{key}")
    p.append("//paste -o -m !structure_void")

p = phase("02_biomes")
p.append("# Tùy chọn: biome cho màu nước, sương và âm thanh. Thoát/vào lại server để thấy.")
sel(p, (-119, M.SEAL1_MAX + 1, -119), (119, 200, 119))
p.append("//setbiome stony_peaks")
sel(p, (-119, M.SEAL[2], -119), (119, M.SEAL1_MAX, 119))
p.append("//setbiome windswept_gravelly_hills")
sel(p, (-119, M.SEAL[3], -119), (119, M.SEAL[2] - 1, 119))
p.append("//setbiome dripstone_caves")
sel(p, (-119, M.Y0, -119), (119, M.SEAL[3] - 1, 119))
p.append("//setbiome deep_dark")

p = phase("03_remove_ores")
p.append("# CHỈ cần nếu đã dán schem bản cũ có quặng (trước 2026-10-10 chiều). Quặng do plugin tự sinh khi load map.")
p.append("# Quặng thường -> stone, quặng deepslate -> deepslate, toàn bộ đảo.")
sel(p, (-119, M.Y0, -119), (119, 200, 119))
p.append("//replace coal_ore,iron_ore,copper_ore,gold_ore,redstone_ore,lapis_ore,diamond_ore,emerald_ore stone")
p.append("//replace deepslate_coal_ore,deepslate_iron_ore,deepslate_copper_ore,deepslate_gold_ore,"
         "deepslate_redstone_ore,deepslate_lapis_ore,deepslate_diamond_ore,deepslate_emerald_ore deepslate")

for v in phases.values():
    check(v)

if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    for name, lst in phases.items():
        with open(OUT / (name + ".txt"), "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(lst) + "\n")
    print("Số lệnh:", {k: sum(1 for c in v if not c.startswith("#")) for k, v in phases.items()})
    print("Lệnh dài nhất:", max(len(c) for v in phases.values() for c in v if not c.startswith("#")))
