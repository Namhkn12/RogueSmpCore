#!/usr/bin/env python3
"""
Sylphens — Layer 3: phòng boss "sảnh fantasy" -> output/sylphens_boss.schem + output/boss_room.json

  python3 scripts/generate_boss_room.py [seed]

Bố cục (bắc = z âm): pad đến ở đầu nam -> thảm đỏ giữa 2 hàng cột -> sông nông cắt ngang -> bậc thang
lên bệ lớn (tế đàn, 2 tượng người lùn) -> hồ + thác đổ từ tường bắc sau bệ. Trần vòm cuốn có sườn,
đèn chùm; cây cỏ ở bồn dọc tường, dây leo, tán lá trên dầm.
Dán: //schem load sylphens_boss   rồi   //paste -o   (gốc lưu trong file = -23,-58,-51, vỏ bedrock kín)
"""
import sys, os, json, math, random
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT.parent / "tools"))
from schem_io import write_schem

SEED = int(sys.argv[1]) if len(sys.argv) > 1 and sys.argv[1].lstrip("-").isdigit() else 2026
OUT = ROOT / "output"
SCHEM_NAME = "sylphens_boss"
rng = random.Random(SEED)
nrng = np.random.default_rng(SEED)

# ------------------------------------------------------------------ hộp (khớp BOSS trong generate_world_commands.py)
BX1, BX2, BZ1, BZ2 = -23, 23, -51, 47       # vỏ bedrock ngoài cùng
BY1, BY2 = -58, -28
AX, AZ1, AZ2 = 21, -48, 45                  # lòng phòng: x -21..21, z -48..45
FLOOR = -56                                 # khối sàn, người đứng ở Y-55
SPRING, APEX = -42, -30                     # chân vòm / khí cao nhất ở đỉnh vòm
NX, NY, NZ = BX2 - BX1 + 1, BY2 - BY1 + 1, BZ2 - BZ1 + 1

# bố cục
COL_X = 13                                  # 2 hàng cột ở x = ±13
COL_Z = [-6, 2, 10, 18, 26, 34]             # cột + sườn vòm
RIB_Z = [-42, -30, -18] + COL_Z             # sườn vòm (phía trước bệ chỉ có trụ áp tường)
DAIS = dict(x1=-14, x2=14, z1=-44, z2=-19, top=FLOOR + 4)
STEP_X = 4                                  # bậc lên bệ rộng x -4..4, ở z -15..-18
RIVER = (-14, -10)                          # sông nông cắt ngang (z)
BRIDGE_X = 4                                # cầu cho thảm đi qua sông: x -4..4
CHAN_X = (16, 17)                           # 2 kênh dọc 2 bên bệ (|x|)
BASIN = dict(x=17, z1=-48, z2=-45)          # hồ sau bệ
FALL = dict(x=5, y=-34)                     # thác: rộng x -5..5, nguồn ở Y-34 trong tường bắc
ALTAR = (0, -33)
STATUES = [(-11, -40), (11, -40)]
PAD = (0, 41)
BOSS_SPAWN = (0, DAIS["top"] + 1, -25)
CHANDELIER_Z = [-2, 14, 30]

PAL = {}
def bid(name):
    name = name if name.startswith("minecraft:") else "minecraft:" + name
    if name not in PAL: PAL[name] = len(PAL)
    return PAL[name]
AIR, STONE, BEDROCK = bid("air"), bid("stone"), bid("bedrock")
W = np.full((NX, NY, NZ), STONE, dtype=np.uint16)

def I(x, y, z): return x - BX1, y - BY1, z - BZ1
def setb(x, y, z, name):
    i, j, k = I(x, y, z)
    if 0 <= i < NX and 0 <= j < NY and 0 <= k < NZ: W[i, j, k] = bid(name)
def getb(x, y, z):
    i, j, k = I(x, y, z); return W[i, j, k]
def is_air(x, y, z): return getb(x, y, z) == AIR
def pick(spec):
    """'60%a,40%b' -> 1 tên theo trọng số (dùng rng có seed)"""
    if "%" not in spec: return spec
    parts = [p.split("%") for p in spec.split(",")]
    return rng.choices([n for _, n in parts], weights=[float(w) for w, _ in parts])[0]
def fill_box(x1, x2, y1, y2, z1, z2, spec):
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                setb(x, y, z, pick(spec))

def ceil_y(x):
    """khí cao nhất ở cột x (vòm cuốn elip từ SPRING lên APEX)"""
    return math.floor(SPRING + (APEX - SPRING) * math.sqrt(max(0.0, 1 - (x / (AX + 1)) ** 2)))

# ------------------------------------------------------------------ 1) vỏ + lòng phòng
W[0, :, :] = W[-1, :, :] = BEDROCK
W[:, 0, :] = W[:, -1, :] = BEDROCK
W[:, :, 0] = W[:, :, -1] = BEDROCK
for x in range(-AX, AX + 1):
    i = x - BX1
    W[i, FLOOR + 1 - BY1:ceil_y(x) + 1 - BY1, AZ1 - BZ1:AZ2 + 1 - BZ1] = AIR

# ------------------------------------------------------------------ 2) vật liệu bề mặt (sàn, tường, vòm)
air = W == AIR
adj = np.zeros_like(air)
adj[1:] |= air[:-1]; adj[:-1] |= air[1:]
adj[:, 1:] |= air[:, :-1]; adj[:, :-1] |= air[:, 1:]
adj[:, :, 1:] |= air[:, :, :-1]; adj[:, :, :-1] |= air[:, :, 1:]
surf = adj & ~air & (W != BEDROCK)
WALL = "55%stone_bricks,25%mossy_stone_bricks,20%cracked_stone_bricks"
for i, j, k in zip(*np.nonzero(surf)):
    x, y, z = i + BX1, j + BY1, k + BZ1
    if y == FLOOR:
        if x % 6 == 0 or z % 6 == 0: n = "stone_bricks"
        else: n = "polished_diorite" if (x // 2 + z // 2) % 2 else "polished_andesite"
    elif y > SPRING:
        n = "quartz_bricks" if x in (0, -8, 8) else "calcite"
    elif y <= FLOOR + 3: n = "polished_deepslate"
    elif y == SPRING: n = "chiseled_stone_bricks"
    else: n = pick(WALL)
    W[i, j, k] = bid(n)

# ------------------------------------------------------------------ 3) sườn vòm, trụ áp tường, đèn đỉnh vòm
for zc in RIB_Z:
    for x in range(-AX, AX + 1):
        yc = ceil_y(x)
        for z in (zc - 1, zc, zc + 1):
            setb(x, yc + 1, z, "quartz_bricks")
        if yc > SPRING: setb(x, yc, zc, "quartz_bricks")          # sườn lồi xuống 1 khối
    for sx in (-1, 1):
        fill_box(sx * (AX + 1), sx * (AX + 1), FLOOR + 1, SPRING, zc - 1, zc + 1, "smooth_quartz")
for zc, zn in zip(RIB_Z[:-1], RIB_Z[1:]):
    zm = (zc + zn) // 2
    for x in (-8, 0, 8):
        setb(x, ceil_y(x) + 1, zm, "pearlescent_froglight")

# ------------------------------------------------------------------ 4) cột, dầm, đèn treo dưới đầu cột
for zc in COL_Z:
    for sx in (-1, 1):
        cx = sx * COL_X
        fill_box(cx - 2, cx + 2, FLOOR + 1, FLOOR + 1, zc - 2, zc + 2, "chiseled_stone_bricks")
        fill_box(cx - 1, cx + 1, FLOOR + 2, SPRING - 1, zc - 1, zc + 1, "quartz_pillar[axis=y]")
        fill_box(cx - 2, cx + 2, SPRING, SPRING, zc - 2, zc + 2, "chiseled_quartz_block")
        x1, x2 = sorted((cx + sx * 3, sx * AX))
        fill_box(x1, x2, SPRING, SPRING, zc - 1, zc + 1, "smooth_quartz")          # dầm ra tường
        for dx, dz in ((2, 0), (-2, 0), (0, 2), (0, -2)):
            setb(cx + dx, SPRING - 1, zc + dz, "lantern[hanging=true]")
        for x in range(x1, x2 + 1):                                               # tán lá trên dầm
            for z in (zc - 1, zc, zc + 1):
                if rng.random() < 0.45 and is_air(x, SPRING + 1, z):
                    setb(x, SPRING + 1, z, pick("70%azalea_leaves[persistent=true],30%flowering_azalea_leaves[persistent=true]"))

# ------------------------------------------------------------------ 5) nước: hồ sau bệ, 2 kênh, sông nông, thác
BED = "45%clay,35%sand,20%gravel"
def water_cell(x, z):
    setb(x, FLOOR, z, "water"); setb(x, FLOOR - 1, z, pick(BED))
for x in range(-BASIN["x"], BASIN["x"] + 1):
    for z in range(BASIN["z1"], BASIN["z2"] + 1):
        water_cell(x, z)
for sx in (-1, 1):
    for ax_ in CHAN_X:
        for z in range(BASIN["z2"] + 1, RIVER[0]):
            water_cell(sx * ax_, z)
for x in range(-AX, AX + 1):
    if abs(x) <= BRIDGE_X: continue
    for z in range(RIVER[0], RIVER[1] + 1):
        water_cell(x, z)
water = [(x, z) for x in range(-AX, AX + 1) for z in range(AZ1, AZ2 + 1) if getb(x, FLOOR, z) == bid("water")]
for x, z in water:
    r = rng.random()
    if r < 0.10: setb(x, FLOOR, z, "seagrass")
    elif r < 0.17 and is_air(x, FLOOR + 1, z): setb(x, FLOOR + 1, z, "lily_pad")
for z in range(RIVER[0], RIVER[1] + 1):                    # lan can cầu
    for sx in (-1, 1):
        setb(sx * BRIDGE_X, FLOOR + 1, z, "stone_brick_wall")
# thác: nguồn trong khe tường bắc (z = AZ1-1), chảy ra 1 khối rồi rơi thẳng xuống hồ
for x in range(-FALL["x"], FALL["x"] + 1):
    setb(x, FALL["y"], AZ1 - 1, "water")
    setb(x, FALL["y"], AZ1, "water[level=1]")
    for y in range(FLOOR + 1, FALL["y"]):
        setb(x, y, AZ1, "water[level=8]")
fill_box(-FALL["x"] - 1, FALL["x"] + 1, FALL["y"] + 1, FALL["y"] + 1, AZ1 - 1, AZ1 - 1, "chiseled_stone_bricks")

# ------------------------------------------------------------------ 6) bệ, bậc thang, tế đàn, lửa
d = DAIS
fill_box(d["x1"], d["x2"], FLOOR + 1, d["top"] - 1, d["z1"], d["z2"], "polished_deepslate")
fill_box(d["x1"], d["x2"], d["top"], d["top"], d["z1"], d["z2"], "deepslate_tiles")
for x in range(d["x1"], d["x2"] + 1):
    for z in range(d["z1"], d["z2"] + 1):
        if x in (d["x1"], d["x2"]) or z in (d["z1"], d["z2"]):
            setb(x, d["top"], z, "polished_blackstone_bricks")
        if 4.5 <= math.hypot(x - ALTAR[0], z - ALTAR[1]) < 5.5:
            setb(x, d["top"], z, "gilded_blackstone")
for c in ((d["x1"], d["z1"]), (d["x1"], d["z2"]), (d["x2"], d["z1"]), (d["x2"], d["z2"])):
    setb(c[0], d["top"], c[1], "chiseled_deepslate")
for k in range(4):                                         # bậc từ z -15 lên tới mặt bệ ở z -19
    z = d["z2"] + 4 - k
    for x in range(-STEP_X, STEP_X + 1):
        for y in range(FLOOR + 1, FLOOR + 1 + k):
            setb(x, y, z, "polished_deepslate")
        setb(x, FLOOR + 1 + k, z, "polished_deepslate_stairs[facing=north,half=bottom]")
ax_, az_ = ALTAR
fill_box(ax_ - 2, ax_ + 2, d["top"] + 1, d["top"] + 1, az_ - 2, az_ + 2, "polished_blackstone_bricks")
fill_box(ax_ - 1, ax_ + 1, d["top"] + 2, d["top"] + 2, az_ - 1, az_ + 1, "chiseled_polished_blackstone")
setb(ax_, d["top"] + 3, az_, "crying_obsidian")
for dx in (-2, 2):
    for dz in (-2, 2):
        setb(ax_ + dx, d["top"] + 2, az_ + dz, "candle[candles=4,lit=true]")
for x in (d["x1"] + 1, d["x2"] - 1):
    setb(x, d["top"] + 1, d["z2"] - 1, "campfire[lit=true,signal_fire=false]")

# ------------------------------------------------------------------ 7) 2 tượng người lùn (nhìn về nam), búa ở phía trong
def statue(cx, cz, by, hand):
    """hand = +1: búa ở phía +x; mô hình (dx, dy, dz) -> khối, dz dương = phía trước (nam)"""
    fill_box(cx + min(-3, 5 * hand), cx + max(3, 5 * hand), by, by, cz - 2, cz + 2, "chiseled_stone_bricks")
    m = {}
    for dx in (-1, 1):
        for dy in (0, 1): m[(dx, dy, 0)] = "polished_blackstone"
    for dx in range(-2, 3):
        for dz in (-1, 0, 1):
            for dy in (2, 3, 4): m[(dx, dy, dz)] = "polished_andesite"
            m[(dx, 2, dz)] = "polished_blackstone"
    m[(0, 2, 1)] = "gold_block"
    for sx in (-1, 1):
        for dy in (2, 3, 4): m[(3 * sx, dy, 0)] = "polished_andesite"
    for dx in (-1, 0, 1):
        for dy in (2, 3, 4): m[(dx, dy, 2)] = "calcite"                 # râu
    m[(0, 1, 2)] = "calcite"
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for dy in (5, 6): m[(dx, dy, dz)] = "polished_diorite"     # đầu
            m[(dx, 7, dz)] = "polished_blackstone_bricks"              # mũ
    m[(0, 8, 0)] = "polished_blackstone"
    for sx in (-1, 1):
        m[(2 * sx, 7, 0)] = "bone_block[axis=y]"; m[(2 * sx, 8, 0)] = "bone_block[axis=y]"
    for dy in range(0, 6): m[(4, dy, 1)] = "stripped_dark_oak_log[axis=y]"   # cán búa
    for dx in (3, 4, 5):
        for dz in (0, 1, 2):
            for dy in (6, 7): m[(dx, dy, dz)] = "polished_deepslate"    # đầu búa
    for (dx, dy, dz), n in m.items():
        setb(cx + dx * hand, by + 1 + dy, cz + dz, n)
for (sx, sz) in STATUES:
    statue(sx, sz, d["top"] + 1, 1 if sx < 0 else -1)

# ------------------------------------------------------------------ 8) thảm, pad đến
for z in range(RIVER[0], PAD[1] - 4):
    for x in range(-2, 3):
        setb(x, FLOOR + 1, z, "red_carpet" if abs(x) <= 1 else "yellow_carpet")
for z in range(ALTAR[1] + 3, d["z2"] + 1):
    for x in (-1, 0, 1):
        setb(x, d["top"] + 1, z, "red_carpet")
px, pz = PAD
for x in range(px - 4, px + 5):
    for z in range(pz - 4, pz + 5):
        r = math.hypot(x - px, z - pz)
        if r <= 2.2: setb(x, FLOOR, z, "polished_blackstone")
        elif r <= 3.3: setb(x, FLOOR, z, "crying_obsidian")
setb(px, FLOOR, pz, "lodestone")
for dx in (-4, 4):
    for dz in (-4, 4):
        setb(px + dx, FLOOR + 1, pz + dz, "polished_blackstone_wall")
        setb(px + dx, FLOOR + 2, pz + dz, "polished_blackstone_wall")
        setb(px + dx, FLOOR + 3, pz + dz, "lantern")

# ------------------------------------------------------------------ 9) đèn chùm giữa sảnh
for zc in CHANDELIER_Z:
    top = ceil_y(0)
    for y in range(-37, top + 1): setb(0, y, zc, "chain[axis=y]")
    for t in (1, 2, 3):
        setb(t, -37, zc, "chain[axis=x]"); setb(-t, -37, zc, "chain[axis=x]")
        setb(0, -37, zc + t, "chain[axis=z]"); setb(0, -37, zc - t, "chain[axis=z]")
    for dx, dz in ((0, 0), (3, 0), (-3, 0), (0, 3), (0, -3)):
        setb(dx, -38, zc + dz, "lantern[hanging=true]")

# ------------------------------------------------------------------ 10) cây cỏ: bồn dọc tường, cây góc, dây leo, rêu
PLANT = "30%azalea,15%flowering_azalea,25%fern,30%short_grass"
def planter(sx, z1, z2):
    xa, xb = sorted((sx * (AX - 2), sx * AX))
    for x in range(xa, xb + 1):
        for z in range(z1, z2 + 1):
            rim = x == sx * (AX - 2) or z in (z1, z2)
            setb(x, FLOOR + 1, z, "mossy_stone_bricks" if rim else "moss_block")
            if not rim:
                if rng.random() < 0.2 and is_air(x, FLOOR + 3, z):
                    setb(x, FLOOR + 2, z, "large_fern[half=lower]"); setb(x, FLOOR + 3, z, "large_fern[half=upper]")
                else:
                    setb(x, FLOOR + 2, z, pick(PLANT))
for sx in (-1, 1):
    for zc in COL_Z[:-1]:
        planter(sx, zc + 2, zc + 6)
    for z1 in (-40, -28):
        planter(sx, z1, z1 + 4)
    # đuốc tường giữa 2 trụ áp tường
    for zc, zn in zip(RIB_Z[:-1], RIB_Z[1:]):
        setb(sx * AX, FLOOR + 4, (zc + zn) // 2, f"wall_torch[facing={'east' if sx < 0 else 'west'}]")
def tree(cx, cz):
    for y in range(FLOOR + 1, FLOOR + 5): setb(cx, y, cz, "oak_log[axis=y]")
    for x in range(cx - 3, cx + 4):
        for y in range(FLOOR + 3, FLOOR + 8):
            for z in range(cz - 3, cz + 4):
                if math.hypot(x - cx, (y - FLOOR - 5) * 1.3, z - cz) <= 2.8 and is_air(x, y, z) and abs(x) <= AX and AZ1 <= z <= AZ2:
                    setb(x, y, z, pick("65%azalea_leaves[persistent=true],35%flowering_azalea_leaves[persistent=true]"))
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            if is_air(x, FLOOR + 1, z) and rng.random() < 0.6: setb(x, FLOOR + 1, z, "moss_carpet")
for sx in (-1, 1):
    tree(sx * (AX - 3), AZ2 - 3)
# dây leo trên tường dọc, tường bắc và thân cột
def vine_run(x, z, face, ytop, n):
    for y in range(ytop, ytop - n, -1):
        if not is_air(x, y, z) or y <= FLOOR + 1: break
        setb(x, y, z, f"vine[{face}=true]")
for z in range(AZ1, AZ2 + 1):
    for sx in (-1, 1):
        if rng.random() < 0.3:
            vine_run(sx * AX, z, "west" if sx < 0 else "east", SPRING - 1, rng.randint(3, 11))
for x in range(-AX, AX + 1):
    if abs(x) > FALL["x"] + 1 and rng.random() < 0.4:
        vine_run(x, AZ1, "north", ceil_y(x), rng.randint(4, 14))
for zc in COL_Z:
    for sx in (-1, 1):
        cx = sx * COL_X
        for dz in (-1, 0, 1):
            for side, face in ((2, "west"), (-2, "east")):
                if rng.random() < 0.25: vine_run(cx + side, zc + dz, face, SPRING - 2, rng.randint(3, 9))
# dây leo phát sáng rủ từ vòm gần 2 bên tường
for z in range(AZ1, AZ2 + 1):
    for x in list(range(-AX, -14)) + list(range(15, AX + 1)):
        if rng.random() < 0.035:
            yc = ceil_y(x); n = rng.randint(2, 5)
            for t in range(n):
                if not is_air(x, yc - t, z): break
                setb(x, yc - t, z, "cave_vines[berries=true]" if t == n - 1 else "cave_vines_plant[berries=false]")
# thảm rêu sát tường
for x in range(-AX, AX + 1):
    for z in range(AZ1, AZ2 + 1):
        if abs(x) >= AX - 3 and is_air(x, FLOOR + 1, z) and getb(x, FLOOR, z) != bid("water") and rng.random() < 0.3:
            setb(x, FLOOR + 1, z, "moss_carpet")

# ------------------------------------------------------------------ kiểm tra + xuất
PASS = [i for n, i in PAL.items() if any(t in n for t in ("air", "water", "seagrass", "lily", "carpet", "vine", "torch",
        "lantern", "candle", "fern", "grass", "chain"))]
P = np.isin(W, PASS)
nb = np.zeros_like(P)
nb[1:] |= P[:-1]; nb[:-1] |= P[1:]; nb[:, 1:] |= P[:, :-1]; nb[:, :-1] |= P[:, 1:]; nb[:, :, 1:] |= P[:, :, :-1]; nb[:, :, :-1] |= P[:, :, 1:]
for i, j, k in zip(*np.nonzero(nb & (W == STONE))):        # mặt đá độn lộ ra do khoét nước -> gạch rêu
    W[i, j, k] = bid(pick("60%mossy_stone_bricks,40%mossy_cobblestone"))
bed_vis = int((nb & (W == BEDROCK)).sum())
stone_vis = int((nb & (W == STONE)).sum())
# đường đi: từ pad tới chân bậc và lên mặt bệ (BFS đi bộ đơn giản: 2 ô trống, bước lên 1)
WATER = bid("water")
def walkable(x, y, z):
    """đứng được ở ô (x,y,z): 2 ô trống, dưới chân là khối đặc hoặc nước"""
    return P[I(x, y, z)] and P[I(x, y + 1, z)] and (not P[I(x, y - 1, z)] or getb(x, y - 1, z) == WATER)
from collections import deque
start = (PAD[0], FLOOR + 1, PAD[1] - 5)
seen = {start}; dq = deque([start])
while dq:
    x, y, z = dq.popleft()
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for dy in (0, 1, -1, -2):
            n = (x + dx, y + dy, z + dz)
            if not (-AX <= n[0] <= AX and AZ1 <= n[2] <= AZ2 and FLOOR < n[1] < APEX): continue
            if dy == 1 and not P[I(x, y + 2, z)]: continue
            if walkable(*n):
                if n not in seen: seen.add(n); dq.append(n)
                break
reach_dais = any(p[1] == DAIS["top"] + 1 and DAIS["z1"] <= p[2] <= DAIS["z2"] for p in seen)
reach_boss = (BOSS_SPAWN[0], BOSS_SPAWN[1], BOSS_SPAWN[2]) in seen

if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    print(f"SEED {SEED} · {NX}×{NY}×{NZ} · palette {len(PAL)}")
    print("bedrock lộ ra ô đi qua được:", bed_vis)
    print("đá độn (stone) lộ ra:", stone_vis)
    print("đi bộ từ pad lên mặt bệ:", reach_dais, "| tới điểm boss:", reach_boss, "| ô đứng được:", len(seen))
    write_schem(OUT / f"{SCHEM_NAME}.schem", W, PAL, (BX1, BY1, BZ1))
    minions = [(sx * 17, FLOOR + 1, z) for sx in (-1, 1) for z in (-2, 14, 30)] + \
              [(sx * 7, FLOOR + 1, z) for sx in (-1, 1) for z in (6, 24)]
    data = dict(seed=SEED, schem=SCHEM_NAME, box=dict(x=[BX1, BX2], y=[BY1, BY2], z=[BZ1, BZ2]),
                floor_y=FLOOR, stand_y=FLOOR + 1,
                arrival=dict(x=PAD[0], y=FLOOR + 1, z=PAD[1], note="pad lodestone; cũng là điểm thoát sau khi thắng"),
                boss_spawn=dict(x=BOSS_SPAWN[0], y=BOSS_SPAWN[1], z=BOSS_SPAWN[2]),
                altar=dict(x=ALTAR[0], y=DAIS["top"] + 3, z=ALTAR[1]),
                minion_spawns=[dict(x=x, y=y, z=z) for x, y, z in minions],
                arena=dict(x=[-11, 11], z=[RIVER[1] + 1, PAD[1] - 5], note="sân giữa 2 hàng cột"))
    json.dump(data, open(OUT / "boss_room.json", "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=1)
    print("schem xong", os.path.getsize(OUT / f"{SCHEM_NAME}.schem"))
