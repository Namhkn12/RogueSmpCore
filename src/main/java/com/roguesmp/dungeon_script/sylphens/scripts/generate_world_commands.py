#!/usr/bin/env python3
"""
Sylphens — sinh lệnh FAWE cho: barrier, mặt đảo, hố sụt, Layer 1, dán Layer 2, boss, hang spawn + sông, cây cối.
Chạy (từ thư mục gốc dự án):  python3 scripts/generate_world_commands.py
Kết quả: commands/*.txt (mỗi giai đoạn 1 file). Mọi lệnh <= 256 ký tự (giới hạn chat). Dùng //g -r (tọa độ thật).
Layer 2 KHÔNG sinh ở đây mà bằng scripts/generate_layer2.py (ra file .schem).
"""
import math, os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "commands"
SCHEM_NAME = "sylphens_layer2"
MAXLEN = 256

# ---------------------------------------------------------------- hằng số Y
F = {1: 46, 2: 30, 3: 18, 4: -2, 5: -18}   # sàn Layer 2 (phải khớp generate_layer2.py)
MAZE_FL, MZ_BOT = 3, -4                     # tầng mê cung + đáy khối bedrock của nó (khớp generate_layer2.py)
SEAL = [F[k] + MZ_BOT if k == MAZE_FL else F[k] - 2 for k in (1, 2, 3, 4)]   # Y tấm bedrock ngăn tầng
L1_FLOOR, L1_CEIL = 64, 80                   # Layer 1: sàn ~64, trần ~80 (15 khối)
DEEP_TOP = 60                                # Y<=60 là deepslate (không cho phá)
BOSS = dict(x1=-22, x2=22, z1=-50, z2=46, floor=-56, top=-29)   # lớp trong vỏ bedrock; sàn đứng ở floor+1 (khớp generate_boss_room.py)
BOSS_SCHEM = "sylphens_boss_cavern"   # phòng boss đang dùng (phương án 2: hang vòm). Phương án 1 sảnh fantasy: sylphens_boss

# công thức mặt đất dùng chung (phải giống hệt ở mọi lệnh cần độ cao h)
H = ("r=sqrt(x*x+z*z);n=perlin(7,x,0,z,0.03,3,0.5);"
     "q=r+perlin(9,x,0,z,0.012,2,0.5)*6;"
     "m=min(max((q-68)/12,0),max((100-q)/12,0),1);"
     "h=88+r*0.2+n*2+m^0.7*(62+n*18);")

# perlin() của WorldEdit/FAWE trả về 0..1 (clamp(libnoise/2+0.5)), trung vị ~0.5, KHÔNG phải -1..1.
# Biến H/terrain ở trên viết theo giả định cũ nên noise bị lệch dương; giữ nguyên vì mặt đảo đã dựng và ổn.
# Lệnh mới cần noise có dấu thì bọc bằng sgn(...) -> -1..1.
def sgn(e):
    return f"({e}*2-1)"
P7 = "perlin(7,x,0,z,0.03,3,0.5)"            # noise n của terrain (dùng để lấy đúng mép đảo)

BR = "50%stone_bricks,30%mossy_stone_bricks,20%cracked_stone_bricks"

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

# ================================================================ 00 barrier
p = phase("00_barrier")
p.append("# Tường barrier dày 2.5 khối quanh đảo, từ Y-64 đến 319 + sàn và nắp")
sel(p, (-104, -64, -104), (104, 319, 104))
p.append("//g -r barrier r=sqrt(x*x+z*z);r>100.5&&r<103")
p.append("//g -r barrier y==-64&&x*x+z*z<10700")
p.append("//g -r barrier y==319&&x*x+z*z<10700")

# ================================================================ 01 terrain
p = phase("01_terrain")
p.append("# Khối đảo: thung lũng lõm về tâm, vành núi dốc r 68-100, đáy đảo thu nhỏ dần")
sel(p, (-100, -60, -100), (100, 200, 100))
p.append("//g -r stone " + H + "b=-60+(r/100)^3*55+n*5;y<=h&&y>=b&&r<=min(99,97+n*3)")
p.append("# Lớp mặt: cỏ/đất ở thung lũng, đá hỗn hợp trên núi, tuyết đỉnh")
sel(p, (-100, 80, -100), (100, 200, 100))
p.append("//g -r dirt " + H + "y>h-4&&y<=h-1&&h<128")
p.append("//g -r grass_block " + H + "y>h-1&&y<=h&&h<128")
p.append("//g -r 55%stone,25%andesite,12%tuff,8%cobblestone " + H + "y>h-3&&y<=h&&h>=128")
p.append("//g -r snow_block " + H + "y>h-1&&y<=h&&h>=160")
p.append("# Phần dưới Y60 thành deepslate (lớp không cho phá, bọc Layer 2 + boss)")
sel(p, (-100, -60, -100), (100, DEEP_TOP, 100))
p.append("//replace stone deepslate")

# ================================================================ 03 crater
p = phase("03_crater_shaft")
p.append("# Hố sụt tâm: phễu mở rộng lên trên, thông xuống trần Layer 1")
sel(p, (-24, 66, -24), (24, 130, 24))
p.append("//g -r air r=sqrt(x*x+z*z);r<=13+max(0,y-80)*0.25+perlin(5,x,y,z,0.08,2,0.5)*1.5")
# ================================================================ 04 layer1
p = phase("04a_reset_layer1")
p.append("# CHỈ chạy nếu đã chạy 04_layer1_cave bản cũ (ngưỡng sai, cột tràn ra khung vuông ngoài đảo).")
p.append("# Sau file này chạy lại 03_crater_shaft rồi 04_layer1_cave.")
sel(p, (-104, 61, -104), (104, 85, 104))
p.append("# xóa khối tràn ngoài mép đảo, dựng lại barrier, lấp lại đá trong đảo (Y61-85)")
p.append(f"//g -r air r=sqrt(x*x+z*z);n={P7};r>min(99,97+n*3)&&(r<=100.5||r>=103)")
p.append("//g -r barrier r=sqrt(x*x+z*z);r>100.5&&r<103")
p.append(f"//g -r stone r=sqrt(x*x+z*z);n={P7};r<=min(99,97+n*3)")

# Ngưỡng Layer 1 đặt theo phân phối thật của perlin() (giá trị 0..1, trung vị ~0.5), dò bằng mô phỏng jlibnoise:
# hang trống ~80-87%, cột ~8% giữa hang (~15% sát sàn/trần), vách ~5%; biome rêu 46 / thạch nhũ 19 / nấm 20 / pha lê 15 (%)
PIL_S, PIL_G, PIL_K = 0.84, 0.78, 0.010      # cột nhỏ, cột khổng lồ, độ phình chân/đỉnh mỗi khối lệch khỏi Y72
WALL_W, WALL_GAP = 0.9, 0.40                  # độ dày vách (~2 khối), ngưỡng khe hở (lớn hơn = nhiều khe hơn)
B_DRIP, B_MUSH, B_CRYS = 0.30, 0.60, 0.76     # b<B_DRIP thạch nhũ, b>B_MUSH nấm, b>B_CRYS pha lê
L1_R = 88                                     # cột/vách không vượt quá bán kính này (hang tối đa r86)

p = phase("04_layer1_cave")
p.append("# Khoét hang Layer 1 (sàn ~64±2, trần ~80±4, r 80±6)")
sel(p, (-90, 61, -90), (90, 85, 90))
p.append(f"//g -r air r=sqrt(x*x+z*z);n={sgn('perlin(11,x,0,z,0.04,2,0.5)')};"
         f"c=80+{sgn('perlin(12,x,0,z,0.03,2,0.5)')}*4;r<=80+n*6&&y>64+n*2&&y<c")
p.append("# Tâm hang phẳng r<=24: sàn 64, không gian 65-79")
p.append("//g -r stone x*x+z*z<=576&&y>=61&&y<=64")
p.append("//g -r air x*x+z*z<=576&&y>=65&&y<=79")
p.append("# Hồ đáy hố (r 8-19, sâu 4) + đảo đá giữa hồ (r<=7) + ống xuống Sảnh Chính (r<=3.5)")
p.append(f"//g -r water r=sqrt(x*x+z*z);r>7.5&&r<=19+{sgn('perlin(3,x,0,z,0.1,1,0.5)')}*2&&y>=61&&y<=64")
p.append("//g -r stone x*x+z*z<=56&&y>=61&&y<=65")
p.append("//g -r moss_block x*x+z*z<=56&&y==66")
p.append("//g -r air x*x+z*z<=12.5&&y>=61&&y<=66")
p.append("# Cột đá đủ cỡ (2 lớp noise: cột nhỏ + cột khổng lồ), phình ở chân và đỉnh. Giới hạn r để không tràn ra ngoài đảo")
p.append(f"//g -r stone r=sqrt(x*x+z*z);k=abs(y-72)*{PIL_K};r>27&&r<={L1_R}&&"
         f"(perlin(13,x,0,z,0.09,2,0.5)>{PIL_S}-k||perlin(14,x,0,z,0.035,2,0.5)>{PIL_G}-k)")
p.append("# Vách ngăn: đường đồng mức noise=0.5, chia cho độ dốc để dày đều ~2 khối; noise thứ 2 cắt khe hở")
p.append(f"//g -r stone r=sqrt(x*x+z*z);m=perlin(15,x,0,z,0.02,2,0.5);a=perlin(15,x+1,0,z,0.02,2,0.5)-m;"
         f"b=perlin(15,x,0,z+1,0.02,2,0.5)-m;r>30&&r<={L1_R}&&abs(m-0.5)<{WALL_W}*(abs(a)+abs(b))&&"
         f"perlin(16,x,0,z,0.05,1,0.5)>{WALL_GAP}")
p.append("# Quặng trong vách/cột Layer 1 (chỉ thay stone), mỗi loại ~1-3% khối đá")
p.append("//gmask stone")
p.append("//g -r coal_ore perlin(51,x,y,z,0.12,1,0.5)>0.86")
p.append("//g -r iron_ore perlin(52,x,y,z,0.13,1,0.5)>0.89")
p.append("//g -r copper_ore perlin(53,x,y,z,0.11,1,0.5)>0.89")
p.append("//g -r gold_ore perlin(54,x,y,z,0.14,1,0.5)>0.92")
p.append("//gmask")
# Mặt nạ <air / >air chỉ xét khối TRÊN / DƯỚI, khớp cả chính ô khí -> không bao giờ dùng làm mặt nạ "from" của //replace
# (sẽ lấp đầy hang). Cách làm: //gmask <air|>air + //replace <danh sách đá> để đánh dấu sàn = moss_block, trần = MARK_CEIL,
# rồi mỗi biome chỉ cần //gmask theo noise và thay 2 khối đánh dấu đó.
ROCK = "stone,coal_ore,iron_ore,copper_ore,gold_ore"
MARK_CEIL = "smooth_stone"                    # khối tạm cho trần, không có sẵn trong vùng Y61-85
BIOME = "perlin(31,x,0,z,0.018,2,0.5)"
p.append("# --- Biome nền: đánh dấu sàn (moss_block) và trần (smooth_stone tạm) — chỉ thay đá, không đụng khí/nước")
p.append("//gmask <air")
p.append(f"//replace {ROCK} moss_block")
p.append("//gmask >air")
p.append(f"//replace {ROCK} {MARK_CEIL}")
p.append("# --- phủ Thạch Nhũ / Pha Lê / Nấm theo noise b (Pha Lê trước Nấm vì vùng Pha Lê nằm trong vùng Nấm)")
p.append(f"//gmask ={BIOME}<{B_DRIP}")
p.append("//replace moss_block 70%dripstone_block,30%tuff")
p.append(f"//replace {MARK_CEIL} dripstone_block")
p.append(f"//gmask ={BIOME}>{B_CRYS}")
p.append("//replace moss_block 60%calcite,40%amethyst_block")
p.append(f"//replace {MARK_CEIL} 70%calcite,20%smooth_basalt,10%amethyst_block")
p.append(f"//gmask ={BIOME}>{B_MUSH}")
p.append("//replace moss_block mycelium")
p.append(f"//replace {MARK_CEIL} 85%stone,15%shroomlight")
p.append("# --- trần còn lại (vùng Rêu mặc định)")
p.append("//gmask")
p.append(f"//replace {MARK_CEIL} 70%stone,30%moss_block")
p.append("# --- Trang trí (đặt vào khối khí sát sàn/trần)")
p.append("//gmask >moss_block")
p.append("//replace air 62%air,30%moss_carpet,4%azalea,2%flowering_azalea,2%short_grass")
p.append("//gmask <moss_block")
p.append("//replace air 93%air,6%cave_vines[berries=true],1%spore_blossom")
p.append("//gmask >dripstone_block")
p.append("//replace air 92%air,8%pointed_dripstone[vertical_direction=up]")
p.append("//gmask <dripstone_block")
p.append("//replace air 88%air,12%pointed_dripstone[vertical_direction=down]")
p.append("//gmask >mycelium")
p.append("//replace air 80%air,10%red_mushroom,10%brown_mushroom")
p.append("//gmask >amethyst_block")
p.append("//replace air 85%air,8%amethyst_cluster,7%large_amethyst_bud")
p.append("//gmask")
p.append("# --- Dây leo xuống hố")
sel(p, (-24, 60, -24), (24, 95, 24))
p.append("# Dây leo cave vines (leo được) ở 6 hướng:")
p.append("# đoạn 1 bám vách phễu Y81-91, đoạn 2 treo từ mép trần Layer 1 xuống sát mặt hồ")
p.append("//g -r cave_vines_plant r=sqrt(x*x+z*z);t=atan2(z,x);k=(y-80)*0.25;abs(sin(t*3))<0.07&&r>11.8+k&&r<12.8+k&&y>=81&&y<=91")
p.append("//g -r cave_vines_plant r=sqrt(x*x+z*z);t=atan2(z,x);abs(sin(t*3))<0.07&&r>14&&r<15&&y>=67&&y<=79")
p.append("//g -r cave_vines[berries=true] r=sqrt(x*x+z*z);t=atan2(z,x);abs(sin(t*3))<0.07&&r>14&&r<15&&y==66")


# ================================================================ 05 layer2 (schematic)
p = phase("05_layer2_paste")
p.append(f"# Layer 2 sinh bằng scripts/generate_layer2.py -> output/{SCHEM_NAME}.schem (5 tầng, sàn {'/'.join(map(str, F.values()))}, T{MAZE_FL} là mê cung)")
p.append("# Bỏ file .schem vào plugins/FastAsyncWorldEdit/schematics/ rồi chạy (đứng đâu cũng được, -o dán đúng gốc -88,-21,-88):")
p.append(f"//schem load {SCHEM_NAME}")
p.append("//paste -o -m !structure_void")
p.append("# Nếu bản FAWE không nhận cờ -m: dùng mặt nạ toàn cục thay thế")
p.append("#   //gmask deepslate")
p.append("#   //paste -o")
p.append("#   //gmask")

p = phase("05b_bedrock_seal_outer_ring")
p.append(f"# Tấm bedrock ngăn tầng đã nằm sẵn trong schem (Y{', Y'.join(map(str, SEAL))}) cho vùng r<=88.")
p.append("# Lệnh dưới đây nối tấm ra tới mép đảo (r 87-100.5) để không đào vòng ra ngoài rồi xuống được. Chạy SAU khi paste.")
sel(p, (-101, min(SEAL), -101), (101, max(SEAL), 101))
p.append("//gmask !air")
p.append("//g -r bedrock (" + "||".join(f"y=={y}" for y in SEAL) + ")&&x*x+z*z>7569&&x*x+z*z<=10100")
p.append("//gmask")

p = phase("05a_reset_old_layer2")
p.append("# CHỈ chạy nếu trước đó đã chạy file 05_layer2 hoặc 06 boss của bản cũ (trả vùng về deepslate)")
sel(p, (-90, -2, -90), (90, 60, 90))
p.append("//g -r deepslate x*x+z*z<=7921")
sel(p, (-72, -26, -72), (72, -3, 72))
p.append("//g -r deepslate x*x+z*z<=4900")
sel(p, (-23, -42, -51), (23, -12, 47))
p.append("//set deepslate")

# ================================================================ 06 boss
p = phase("06_layer3_boss")
b = BOSS
p.append("# Phòng boss 'hang vòm lớn' (r46, nền Y-61, vòm Y-25) phình xuống dưới đáy đảo, vỏ deepslate + bedrock giấu,")
p.append("# chỉ vào bằng dịch chuyển. Sinh bằng scripts/generate_boss_cavern.py -> output/sylphens_boss_cavern.schem.")
p.append("# Schem phủ trọn hộp boss cũ (x-23..23, y-58..-28, z-51..47): dán đè là xóa phòng boss cũ. Vị trí: output/boss_cavern.json")
p.append(f"//schem load {BOSS_SCHEM}")
p.append("//paste -o -m !structure_void")

# ================================================================ 07 hang spawn + đường + sông (schem)
p = phase("07_surface_spawn_river")
p.append("# Hang spawn ở sườn núi nam (cửa nhìn vào tâm) + đường gấp khúc xuống thung lũng + sông phía bắc vào miệng hố.")
p.append("# Sinh bằng scripts/generate_surface.py -> output/sylphens_surface.schem (chỉ chứa ô thay đổi). Chạy SAU 01 và 03.")
p.append("//schem load sylphens_surface")
p.append("//paste -o -m !structure_void")

# ================================================================ 08 thung lũng: biome, cây thông, cỏ hoa
p = phase("08_valley_biome")
p.append("# Chạy SAU 07. Cây thông, podzol, cỏ hoa đã nằm sẵn trong sylphens_surface.schem (FAWE cho MC 26.x: //forest trồng 0 cây).")
p.append("# Ở đây chỉ đổi biome taiga cổ thụ cho cả mặt đảo (thoát/vào lại server để thấy màu cỏ, lá mới).")
sel(p, (-100, 86, -100), (100, 200, 100))
p.append("//setbiome old_growth_spruce_taiga")

for v in phases.values():
    check(v)

if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    for name, lst in phases.items():
        with open(OUT / (name + ".txt"), "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(lst) + "\n")
    n = sum(1 for v in phases.values() for c in v if not c.startswith("#"))
    print("Số lệnh:", n, {k: sum(1 for c in v if not c.startswith('#')) for k, v in phases.items()})
    print("Lệnh dài nhất:", max(len(c) for v in phases.values() for c in v if not c.startswith('#')))
