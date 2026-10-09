#!/usr/bin/env python3
"""
Sylphens — mặt đảo: hang xuất hiện (spawn) ở sườn núi phía nam + đường gấp khúc xuống thung lũng,
và con sông phía bắc từ chân núi chảy vào miệng hố.

  python3 scripts/generate_surface.py

Đầu ra: output/sylphens_surface.schem (chỉ chứa ô thay đổi, còn lại structure_void),
        output/surface.json (điểm spawn, đường, sông cho plugin), images/surface_plan.png
Địa hình tính lại CHÍNH XÁC từ công thức H của 01_terrain bằng ../tools/fawe_noise.py (khớp perlin của FAWE).
Dán: //schem load sylphens_surface  rồi  //paste -o -m !structure_void
"""
import sys, os, json, math, random
from collections import deque
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT.parent / "tools"))
from fawe_noise import perlin
from schem_io import write_schem

SEED = 2026
rng = random.Random(SEED)
OUT = ROOT / "output"
SCHEM_NAME = "sylphens_surface"

# ------------------------------------------------------------------ địa hình (khớp biến H trong generate_world_commands.py)
def terrain_h(x, z):
    r = np.sqrt(x * x + z * z)
    n = perlin(7, x, 0, z, 0.03, 3, 0.5)
    q = r + perlin(9, x, 0, z, 0.012, 2, 0.5) * 6
    m = np.minimum(np.minimum(np.maximum((q - 68) / 12, 0), np.maximum((100 - q) / 12, 0)), 1)
    return 88 + r * 0.2 + n * 2 + m ** 0.7 * (62 + n * 18)

G = np.arange(-100, 101)
GX, GZ = np.meshgrid(G, G, indexing="ij")
HTOP = np.floor(terrain_h(GX, GZ)).astype(int)          # khối mặt đất cao nhất của mỗi cột
def top(x, z): return int(HTOP[x + 100, z + 100])

from functools import lru_cache
@lru_cache(maxsize=None)
def crater_air(x, y, z):
    """ô bị lệnh 03_crater_shaft khoét"""
    if not (66 <= y <= 130 and abs(x) <= 24 and abs(z) <= 24): return False
    r = math.hypot(x, z)
    return r <= 13 + max(0, y - 80) * 0.25 + float(perlin(5, x, y, z, 0.08, 2, 0.5)) * 1.5

def ground_name(x, y, z):
    """khối địa hình gốc ở (x,y,z) theo 01_terrain (để vẽ ảnh / mô phỏng đi bộ)"""
    t = top(x, z)
    if y > t or crater_air(x, y, z): return None
    if t < 128:
        return "grass_block" if y == t else ("dirt" if y > t - 4 else "stone")
    return "snow_block" if (y == t and t >= 160) else "stone"

# ------------------------------------------------------------------ bảng sửa đổi
MOD = {}                          # (x,y,z) -> tên khối
def put(x, y, z, name): MOD[(x, y, z)] = name
def pick(spec):
    if "%" not in spec: return spec
    parts = [p.split("%") for p in spec.split(",")]
    return rng.choices([n for _, n in parts], weights=[float(w) for w, _ in parts])[0]

# ================================================================== 1) hang spawn (nam, cửa nhìn vào tâm)
YE = 121                          # khối sàn hang và sân trước cửa (người đứng Y122)
ZM = next(z for z in range(55, 95) if top(0, z) >= YE + 8)   # mặt vách đầu tiên đủ cao: cửa cao 5 + lanh tô ≥ 3 khối
TUN = 4                           # đường hầm dài 4 từ cửa vào buồng
RX, RZ = 9, 7                     # bán kính buồng (nông theo z để vách sau tới sườn ngoài núi còn dày)
ZC = ZM + TUN + RZ - 1            # tâm buồng
SPAWN = (0, YE + 1, ZC)
ROCK = "60%stone,25%andesite,15%cobblestone"
FLOOR_CAVE = "55%stone,20%moss_block,15%gravel,10%andesite"

cave_air = set()
for x in range(-RX - 2, RX + 3):
    for z in range(ZC - RZ - 2, ZC + RZ + 3):
        phi = math.atan2(z - ZC, x)
        wob = 1 + 0.12 * math.sin(3 * phi + 1.3) + 0.08 * math.sin(5 * phi + 0.4)
        rho2 = (x / (RX * wob)) ** 2 + ((z - ZC) / (RZ * wob)) ** 2
        if rho2 > 1: continue
        ytop = YE + 2 + int(round(6 * math.sqrt(1 - rho2)))
        for y in range(YE + 1, ytop + 1): cave_air.add((x, y, z))
for z in range(ZM, ZC - RZ + 2):
    for x in range(-2, 3):
        for y in range(YE + 1, YE + 6):
            if abs(x) == 2 and y == YE + 5: continue               # bo góc trần đường hầm
            cave_air.add((x, y, z))
for p in cave_air: put(*p, "air")
# sàn, trang trí trong hang
floor_cells = {(x, z) for (x, y, z) in cave_air if y == YE + 1}
for x, z in floor_cells:
    put(x, YE, z, pick(FLOOR_CAVE))
    if rng.random() < 0.18 and (x, z) != (SPAWN[0], SPAWN[2]): put(x, YE + 1, z, "moss_carpet")
for dx in (-1, 0, 1):
    for dz in (-1, 0, 1):
        put(dx, YE, ZC + dz, "chiseled_stone_bricks" if (dx, dz) == (0, 0) else "polished_andesite")
        MOD.pop((dx, YE + 1, ZC + dz), None); put(dx, YE + 1, ZC + dz, "air")
for ang in (45, 135, 225, 315):                                   # 4 đèn lồng quanh điểm spawn
    lx, lz = round(4.5 * math.cos(math.radians(ang))), ZC + round(4.5 * math.sin(math.radians(ang)))
    put(lx, YE + 1, lz, "lantern")
ceil_cells = {}
for (x, y, z) in cave_air:
    ceil_cells[(x, z)] = max(ceil_cells.get((x, z), y), y)
for (x, z), yt in ceil_cells.items():
    if z <= ZM + 1: continue
    r_ = rng.random()
    if r_ < 0.06:
        n = rng.randint(1, 3)
        for t in range(n):
            put(x, yt - t, z, "cave_vines[berries=true]" if t == n - 1 else "cave_vines_plant[berries=false]")
    elif r_ < 0.10:
        put(x, yt, z, "pointed_dripstone[vertical_direction=down]")
# cửa hang: dây leo rủ trên vách phía trên miệng hang
for x in range(-4, 5):
    if rng.random() < 0.55:
        for y in range(YE + 9, YE + 9 - rng.randint(2, 5), -1):
            if y <= top(x, ZM) and (x, y, ZM) not in cave_air:              # chỉ đặt khi có vách để bám
                put(x, y, ZM - 1, "vine[south=true]")

# ================================================================== 2) đường gấp khúc (zig-zag) men chân vách
# Dải đường theo vòng tròn (bán kính r), mỗi dải rộng 4 + 1 lan can, dải sau thấp hơn và gần tâm hơn 5 khối.
# Góc θ: 90° = nam (+z), giảm về phía đông.
PATH = "50%cobblestone,30%andesite,20%gravel"
RAIL = "cobblestone_wall"
STAIR = "cobblestone_stairs"
SLOPE = 3                          # xuống 1 khối mỗi 3 khối đi
R1 = ZM - 0.5                       # mép ngoài dải 1 (sát vách)
BANDS = [(R1 - 5 * k - 4, R1 - 5 * k) for k in range(3)]          # (r trong, r ngoài) của 3 dải
RAILS = [(R1 - 5 * k - 5, R1 - 5 * k - 4) for k in range(3)]
TERRACE = 3.5                       # sân trước cửa: |θ-90| ≤ 3.5°
# (dải, θ bắt đầu, θ kết thúc): đi từ θ bắt đầu sang θ kết thúc, xuống dần
LEGS = [(0, 90 - TERRACE, 74.5), (1, 74.5, 106.0), (2, 106.0, 60.0)]
TURN_W = 3.5                        # chỗ quay đầu rộng 3.5° nối 2 dải

def band_of(r):
    for k, (a, b) in enumerate(BANDS):
        if a <= r < b: return k
    return None
def rail_of(r):
    for k, (a, b) in enumerate(RAILS):
        if a <= r < b: return k
    return None

path_floor = {}                     # (x,z) -> y sàn đường
path_kind = {}                      # (x,z) -> 'path' | 'rail' | 'turn' | 'terrace'
levels = [YE]
for k, (band, t0, t1) in enumerate(LEGS):
    a, b = BANDS[band]
    rm = (a + b) / 2
    y0 = levels[-1]
    sgn = 1 if t1 > t0 else -1
    end_y = None
    for x in range(-90, 91):
        for z in range(20, 95):
            r = math.hypot(x, z)
            if band_of(r) != band: continue
            th = math.degrees(math.atan2(z, x))
            s_deg = (th - t0) * sgn
            if k == 0 and abs(th - 90) <= TERRACE:
                path_floor[(x, z)] = YE; path_kind[(x, z)] = "terrace"; continue
            if not (0 <= s_deg <= (t1 - t0) * sgn): continue
            s = math.radians(s_deg) * rm
            y = y0 - int(s // SLOPE)
            if k == len(LEGS) - 1 and y <= top(x, z):                # nhánh cuối: chạm đất thì dừng
                continue
            path_floor[(x, z)] = y; path_kind[(x, z)] = "path"
    span = math.radians(abs(t1 - t0)) * rm
    levels.append(y0 - int(span // SLOPE))
    if k < len(LEGS) - 1:
        # chỗ quay đầu: phẳng, phủ dải hiện tại + lan can + dải kế tiếp, nằm ngoài đầu nhánh
        ty = levels[-1]
        lo, hi = sorted((t1, t1 + sgn * TURN_W))
        for x in range(-90, 91):
            for z in range(20, 95):
                r = math.hypot(x, z)
                th = math.degrees(math.atan2(z, x))
                if lo <= th <= hi and BANDS[band + 1][0] <= r < BANDS[band][1]:
                    path_floor[(x, z)] = ty; path_kind[(x, z)] = "turn"
# lan can: vòng giữa 2 dải, dọc theo nhánh (không ở chỗ quay đầu / sân)
for x in range(-90, 91):
    for z in range(20, 95):
        r = math.hypot(x, z)
        k = rail_of(r)
        if k is None or (x, z) in path_floor: continue
        th = math.degrees(math.atan2(z, x))
        # lấy cao độ ô đường liền kề phía ngoài (dải k)
        best = None
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                q = (x + dx, z + dz)
                if q in path_floor and band_of(math.hypot(*q)) == k and path_kind[q] in ("path", "terrace"):
                    best = max(best or -999, path_floor[q])
        if best is not None:
            path_floor[(x, z)] = best; path_kind[(x, z)] = "rail"
# dựng: khoét khí phía trên, đắp đá phía dưới, mặt đường, bậc thang ở chỗ chuyển cao độ, lan can
for (x, z), y in path_floor.items():
    t = top(x, z)
    for yy in range(y + 1, max(t, y + 4) + 1):
        if (x, yy, z) not in cave_air: put(x, yy, z, "air")
    for yy in range(min(t + 1, y), y):
        put(x, yy, z, pick(ROCK))
    put(x, y, z, pick(PATH))
    if path_kind[(x, z)] == "rail":
        put(x, y + 1, z, RAIL)
for (x, z), y in path_floor.items():
    if path_kind[(x, z)] == "rail": continue
    for dx, dz, face in ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")):
        q = (x + dx, z + dz)
        if q in path_floor and path_kind[q] != "rail" and path_floor[q] == y + 1:
            put(x, y + 1, z, f"{STAIR}[facing={face},half=bottom]")
            break
# sân trước cửa: vài đèn lồng trên cọc ở 2 mép
for sx in (-1, 1):
    x, z = 3 * sx, ZM - 1
    if (x, z) in path_floor:
        put(x, YE + 1, z, "mossy_cobblestone_wall"); put(x, YE + 2, z, "lantern")

# ================================================================== 3) sông phía bắc: chân núi -> miệng hố
RIVER_HALF = 2.5                    # nửa bề rộng lòng nước (rộng ~6)
def river_cx(z): return 4.0 * math.sin(z / 9.0)
Z_SRC = next(z for z in range(-100, 0) if top(round(river_cx(z)), z) < 106)    # chân núi phía bắc
z_end = -8
yw = {}
lvl = 999
for z in range(Z_SRC, z_end + 1):
    lvl = min(lvl, top(round(river_cx(z)), z) - 1)
    yw[z] = lvl
BED = "45%sand,30%gravel,25%clay"
river_cells = []
for z in range(Z_SRC - 4, z_end + 1):
    zc = max(z, Z_SRC)
    for x in range(-14, 15):
        d = abs(x - river_cx(zc))
        if z < Z_SRC:                                               # hồ nhỏ ở chân núi (chỗ đón thác sau này)
            d = math.hypot(x - river_cx(Z_SRC), z - Z_SRC) * 0.9
        if d > 5: continue
        w = yw[zc]; t = top(x, z)
        if t > w + 6: continue                                      # không khoét rãnh lên sườn núi
        if d <= RIVER_HALF:
            for yy in range(w + 1, t + 1): put(x, yy, z, "air")
            if crater_air(x, w, z) or crater_air(x, w - 1, z): continue
            for yy in range(t + 1, w - 2): put(x, yy, z, "dirt")       # đắp nếu đất thấp hơn lòng sông
            put(x, w - 2, z, pick(BED)); put(x, w - 1, z, "water"); put(x, w, z, "water")
            river_cells.append((x, w, z))
        elif d <= RIVER_HALF + 1:                                   # bờ thấp sát nước
            if crater_air(x, w, z): continue
            for yy in range(w + 1, t + 1): put(x, yy, z, "air")
            for yy in range(t + 1, w - 1): put(x, yy, z, "dirt")
            put(x, w, z, "grass_block"); put(x, w - 1, z, "dirt")
            if rng.random() < 0.25:
                for yy in range(w + 1, w + 1 + rng.randint(2, 3)): put(x, yy, z, "sugar_cane")
        else:                                                       # bờ thoải
            if t > w + 1 and not crater_air(x, w + 1, z):
                for yy in range(w + 2, t + 1): put(x, yy, z, "air")
                put(x, w + 1, z, "grass_block")
for (x, w, z) in river_cells:
    r_ = rng.random()
    if r_ < 0.08: put(x, w - 1, z, "seagrass")
    elif r_ < 0.13: put(x, w + 1, z, "lily_pad")

# ================================================================== 4) rừng thông + nền đất + cỏ hoa (dựng thẳng vào schem)
# FAWE cho MC 26.x gọi generateTree ngay tại khối mặt đất nên //forest không trồng được cây nào -> tự dựng cây ở đây.
frng = random.Random(SEED + 1)
LEAF_S = "spruce_leaves[persistent=true]"
LOG_S = "spruce_log[axis=y]"
FOREST_KEEP_OUT = dict(x=(-30, 30), z=(45, 100))    # trước hang spawn: để trống, giữ tầm nhìn + đường
TREE_KINDS = [("mega_spruce", 9, 26), ("mega_pine", 9, 12), ("spruce", 6, 45), ("pine", 6, 30)]   # (loại, khoảng cách tối thiểu, số cây)

def is_mod_solid(x, y, z):
    n = MOD.get((x, y, z))
    return n is not None and n != "air"
def free_air(x, y, z):
    """ô đang là khí (tự nhiên hoặc đã khoét) và chưa bị gì chiếm"""
    if (x, y, z) in MOD: return MOD[(x, y, z)] == "air"
    return y > top(x, z) and abs(x) <= 100 and abs(z) <= 100
def near_river(x, z):
    if not (Z_SRC - 8 <= z <= z_end + 2): return False
    return abs(x - river_cx(max(z, Z_SRC))) < 7
def ground_ok(x, z):
    """mặt cỏ tự nhiên, phẳng vừa, ngoài vùng cấm"""
    t = top(x, z)
    if t >= 126 or crater_air(x, t, z) or math.hypot(x, z) < 19 or math.hypot(x, z) > 90: return False
    if FOREST_KEEP_OUT["x"][0] <= x <= FOREST_KEEP_OUT["x"][1] and FOREST_KEEP_OUT["z"][0] <= z <= FOREST_KEEP_OUT["z"][1]: return False
    if near_river(x, z): return False
    if any((x, y, z) in MOD for y in range(t - 1, t + 3)): return False
    hs = [top(x + dx, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1)]
    return max(hs) - min(hs) <= 2

def leaf(x, y, z):
    if free_air(x, y, z): put(x, y, z, LEAF_S)
def disc(cx, cz, y, R, center2x2=False):
    ox = 0.5 if center2x2 else 0.0
    rr = int(math.ceil(R)) + 1
    for x in range(int(cx - rr), int(cx + rr) + 2):
        for z in range(int(cz - rr), int(cz + rr) + 2):
            d = math.hypot(x - (cx + ox), z - (cz + ox))
            if d <= R + 0.15 and not (d > R - 0.6 and frng.random() < 0.35):   # mép tán lởm chởm
                leaf(x, y, z)

def podzol_patch(cx, cz, R):
    for x in range(cx - R, cx + R + 2):
        for z in range(cz - R, cz + R + 2):
            if math.hypot(x - cx, z - cz) <= R + 0.5 and frng.random() < 0.75 and ground_ok(x, z):
                put(x, top(x, z), z, "podzol")

def trunk(cells, base, H):
    for (x, z) in cells:
        for y in range(top(x, z) + 1, base + H + 1):
            put(x, y, z, LOG_S)
        put(x, top(x, z), z, "podzol")

def mega_spruce(x, z, pine=False):
    cells = [(x, z), (x + 1, z), (x, z + 1), (x + 1, z + 1)]
    tops = [top(*c) for c in cells]
    if max(tops) - min(tops) > 1 or not all(ground_ok(*c) for c in cells): return False
    base = max(tops); H = frng.randint(18, 27) if not pine else frng.randint(20, 28)
    trunk(cells, base, H)
    crown = int(H * (0.62 if not pine else 0.32))
    tip = base + H
    for y in range(tip - crown, tip + 2):
        d = tip + 1 - y
        if pine:
            R = min(3.2, 0.6 + d * 0.38)
        else:
            R = min(5.2, 0.8 + d * 0.3)
            if d % 3 == 2: R -= 1.0                                          # tầng tán so le
        disc(x, z, y, max(R, 0.6), center2x2=True)
    podzol_patch(x, z, 3)
    return True

def spruce(x, z, pine=False):
    if not ground_ok(x, z): return False
    base = top(x, z); H = frng.randint(9, 14) if not pine else frng.randint(12, 17)
    trunk([(x, z)], base, H)
    tip = base + H
    lo = base + (3 if not pine else H - 5)
    for y in range(lo, tip + 2):
        d = tip + 1 - y
        if pine:
            R = 0.6 if d <= 1 else (1.6 if d % 2 == 0 else 1.1)
        else:
            R = 0.6 if d <= 1 else min(3.0, 1.0 + (d % 2) + d // 4)
        disc(x, z, y, R)
    return True

cands = [(x, z) for x in range(-90, 91) for z in range(-90, 91) if ground_ok(x, z)]
frng.shuffle(cands)
placed = []                                      # (x, z, khoảng cách tối thiểu)
tree_count = {}
for kind, gap, want in TREE_KINDS:
    n = 0
    for (x, z) in cands:
        if n >= want: break
        if any(math.hypot(x - px, z - pz) < max(gap, pg) for px, pz, pg in placed): continue
        ok = mega_spruce(x, z, pine=(kind == "mega_pine")) if kind.startswith("mega") else spruce(x, z, pine=(kind == "pine"))
        if ok:
            placed.append((x, z, gap)); n += 1
    tree_count[kind] = n

# nền đất + cỏ hoa trên mọi ô cỏ còn trống của thung lũng
FLORA_GRASS = [("short_grass", 11), ("fern", 3), ("dandelion", 1), ("poppy", 1), ("cornflower", 1),
               ("oxeye_daisy", 1), ("azure_bluet", 1), ("lily_of_the_valley", 1)]
FLORA_PODZOL = [("fern", 22), ("sweet_berry_bush[age=3]", 3), ("brown_mushroom", 3)]
def roll(table):
    r = frng.random() * 100; acc = 0
    for n, w in table:
        acc += w
        if r < acc: return n
    return None
flora_n = 0
for x in range(-92, 93):
    for z in range(-92, 93):
        t = top(x, z)
        if t >= 128 or math.hypot(x, z) > 95 or crater_air(x, t, z): continue
        g = MOD.get((x, t, z))
        if g is None:
            if (x, t + 1, z) in MOD: continue                       # đường, sông, hang... đã dùng ô này
            r_ = frng.random()
            g = "podzol" if r_ < 0.09 else ("coarse_dirt" if r_ < 0.14 else "grass_block")
            if g != "grass_block": put(x, t, z, g)
        if g not in ("grass_block", "podzol") or not free_air(x, t + 1, z): continue
        p = roll(FLORA_PODZOL if g == "podzol" else FLORA_GRASS)
        if p: put(x, t + 1, z, p); flora_n += 1

# ================================================================== kiểm tra: đi bộ từ điểm spawn ra thung lũng
def block_at(x, y, z):
    if (x, y, z) in MOD: return MOD[(x, y, z)]
    if abs(x) > 100 or abs(z) > 100: return None
    return ground_name(x, y, z)
SOFT = ("air", "water", "lily", "moss_carpet", "vine", "lantern", "sugar_cane", "seagrass", "cave_vines", "pointed",
        "short_grass", "fern", "dandelion", "poppy", "cornflower", "daisy", "bluet", "valley", "berry", "mushroom")
def soft(x, y, z):
    b = block_at(x, y, z)
    return b is None or any(t in b for t in SOFT)
def stand(x, y, z):
    return soft(x, y, z) and soft(x, y + 1, z) and not soft(x, y - 1, z)
start = (SPAWN[0], SPAWN[1], SPAWN[2] - 2)
seen = {start}; dq = deque([start]); reach = None
while dq:
    x, y, z = dq.popleft()
    if math.hypot(x, z) < 50 and y <= 105 and reach is None: reach = (x, y, z)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for dy in (0, 1, -1, -2, -3):
            n = (x + dx, y + dy, z + dz)
            if dy == 1 and not soft(x, y + 2, z): continue
            if stand(*n):
                if n not in seen and math.hypot(n[0], n[2]) < 99: seen.add(n); dq.append(n)
                break
    if len(seen) > 60000: break
# bậc cao 1 khối không có bậc thang trên đường (phải nhảy)
jumps = 0
for (x, z), y in path_floor.items():
    if path_kind[(x, z)] == "rail": continue
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        q = (x + dx, z + dz)
        if q in path_floor and path_kind[q] != "rail" and path_floor[q] == y + 2: jumps += 1
roof = min(top(x, z) - yt for (x, z), yt in ceil_cells.items() if z > ZM + 1)

# ================================================================== xuất
def build_schem():
    xs, ys, zs = zip(*MOD)
    x0, y0, z0 = min(xs), min(ys), min(zs)
    nx, ny, nz = max(xs) - x0 + 1, max(ys) - y0 + 1, max(zs) - z0 + 1
    pal = {"minecraft:structure_void": 0}
    W = np.zeros((nx, ny, nz), dtype=np.uint16)
    for (x, y, z), n in MOD.items():
        n = "minecraft:" + n
        W[x - x0, y - y0, z - z0] = pal.setdefault(n, len(pal))
    return W, pal, (x0, y0, z0)

def render(path):
    import matplotlib; matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    fig, axs = plt.subplots(1, 3, figsize=(24, 9), gridspec_kw=dict(width_ratios=[1.05, 1, 1]))
    COL = {"water": (0.24, 0.43, 0.86), "lily_pad": (0.2, 0.5, 0.2), "sugar_cane": (0.5, 0.75, 0.35),
           "spruce_leaves": (0.13, 0.3, 0.17), "spruce_log": (0.35, 0.25, 0.15), "podzol": (0.45, 0.32, 0.18),
           "coarse_dirt": (0.5, 0.38, 0.27), "poppy": (0.85, 0.15, 0.1), "dandelion": (0.95, 0.85, 0.2),
           "cornflower": (0.3, 0.4, 0.9), "short_grass": (0.38, 0.6, 0.3), "fern": (0.3, 0.55, 0.28),
           "grass_block": (0.42, 0.66, 0.33), "cobblestone_wall": (0.35, 0.35, 0.35), "lantern": (1, 0.8, 0.3)}
    def col(n):
        for k, c in COL.items():
            if k in n: return c
        if "cobble" in n or "andesite" in n or "gravel" in n or "stone" in n: return (0.62, 0.6, 0.56)
        return (0.55, 0.5, 0.45)
    # toàn cảnh
    img = np.ones((201, 201, 3))
    sh = (HTOP - 88) / 100
    base = np.where((HTOP < 128)[..., None], np.array([0.42, 0.66, 0.33]), np.array([0.6, 0.6, 0.6]))
    base = np.where((HTOP >= 160)[..., None], np.array([0.95, 0.95, 0.97]), base)
    img[:] = base * (0.65 + 0.5 * sh[..., None])
    img[np.hypot(GX, GZ) > 98] = 1
    topmod = {}
    for (x, y, z), n in MOD.items():
        if n != "air" and (topmod.get((x, z), (-999,))[0] < y): topmod[(x, z)] = (y, n)
    for (x, z), (y, n) in topmod.items():
        img[x + 100, z + 100] = col(n)
    for (x, z) in floor_cells: img[x + 100, z + 100] = (0.2, 0.18, 0.16)
    ax = axs[0]; ax.imshow(np.transpose(img, (1, 0, 2)), extent=(-100.5, 100.5, 100.5, -100.5), interpolation="nearest")
    ax.add_patch(plt.Circle((0, 0), 14, color="k", alpha=0.5))
    ax.plot(SPAWN[0], SPAWN[2], "r*", ms=14); ax.text(SPAWN[0] + 3, SPAWN[2] + 3, "spawn", color="r")
    ax.set_title("Mặt đảo nhìn từ trên: hang + đường (nam), sông (bắc)"); ax.set_xlabel("x"); ax.set_ylabel("z (bắc = âm)")
    # cận cảnh đường zig-zag: cao độ sàn đường
    ax = axs[1]
    pts = [(x, z, y) for (x, z), y in path_floor.items()]
    sc = ax.scatter([p[0] for p in pts], [p[1] for p in pts], c=[p[2] for p in pts], cmap="viridis", marker="s", s=22)
    ax.scatter([x for (x, z) in floor_cells], [z for (x, z) in floor_cells], c="#3a302a", marker="s", s=22)
    fig.colorbar(sc, ax=ax, label="Y sàn đường")
    ax.set_xlim(-45, 45); ax.set_ylim(95, 40); ax.set_aspect("equal")
    ax.set_title("Đường gấp khúc: cao độ sàn (Y)"); ax.set_xlabel("x"); ax.set_ylabel("z")
    # mặt cắt x = 0 (nam)
    ax = axs[2]
    zz = np.arange(30, 100)
    ax.bar(zz, [top(0, z) - 59 for z in zz], bottom=59.5, width=1.0, color="#b9a98f", label="địa hình gốc")
    for (x, y, z), n in MOD.items():
        if x != 0: continue
        if n == "air": ax.add_patch(plt.Rectangle((z - .5, y - .5), 1, 1, color="white"))
    for (x, y, z), n in MOD.items():
        if x != 0 or n == "air": continue
        ax.add_patch(plt.Rectangle((z - .5, y - .5), 1, 1, color=col(n)))
    ax.plot(SPAWN[2], SPAWN[1], "r*", ms=14)
    ax.set_xlim(40, 98); ax.set_ylim(90, 185); ax.set_aspect("equal")
    ax.set_title("Mặt cắt x = 0: vách núi, sân, đường hầm, buồng hang"); ax.set_xlabel("z"); ax.set_ylabel("Y")
    plt.tight_layout(); plt.savefig(path, dpi=90); plt.close()

if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    W, pal, off = build_schem()
    write_schem(OUT / f"{SCHEM_NAME}.schem", W, pal, off)
    print(f"cửa hang z={ZM}, sàn Y{YE}, buồng tâm (0,{ZC}); đá phủ trên trần hang ít nhất {roof} khối")
    print(f"đường: {len(path_floor)} ô, cao độ các chặng {levels}, bậc cao 1 khối chưa có bậc thang: {jumps}")
    print(f"sông: z {Z_SRC} -> {z_end}, mặt nước Y{yw[Z_SRC]} -> Y{yw[z_end]}, {len(river_cells)} ô nước")
    print(f"đi bộ từ spawn xuống thung lũng (r<50, Y<=105): {reach}")
    print(f"rừng: {tree_count} · cỏ hoa {flora_n} ô")
    print(f"schem {W.shape} gốc {off}, {len(MOD)} ô thay đổi, palette {len(pal)}")
    json.dump(dict(spawn=dict(x=SPAWN[0], y=SPAWN[1], z=SPAWN[2], yaw=180, note="giữa buồng hang, nhìn về bắc (ra cửa)"),
                   cave_mouth=dict(x=0, y=YE + 1, z=ZM), path_end=list(reach) if reach else None,
                   river=dict(source_pool=dict(x=round(river_cx(Z_SRC)), y=yw[Z_SRC], z=Z_SRC - 2,
                                               note="hồ nhỏ ở chân núi bắc, chỗ đón thác"),
                              end=dict(x=round(river_cx(z_end)), y=yw[z_end], z=z_end))),
              open(OUT / "surface.json", "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=1)
    render(ROOT / "images" / "surface_plan.png")
    print("xong")
