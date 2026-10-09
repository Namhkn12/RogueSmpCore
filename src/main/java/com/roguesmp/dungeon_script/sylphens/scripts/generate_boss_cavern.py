#!/usr/bin/env python3
"""
Sylphens — Layer 3 phương án 2: phòng boss "hang vòm lớn" -> output/sylphens_boss_cavern.schem + boss_cavern.json

  python3 scripts/generate_boss_cavern.py

Hang vòm tròn đường kính ~92, sàn Y-61, đỉnh vòm Y-25, phình xuống dưới đáy đảo như khối rễ đá
(vỏ deepslate dày 6, lớp thứ 4 là bedrock giấu kín). Giữa: sàn tròn r24 cao hơn nền 3 khối, mặt sàn
có vết nứt dạng lưới (blue concrete / kính xanh lá đậm x2 / kính xanh lá nhạt x3 / light block),
vòng triệu hồi ở tâm, 4 bậc thang 4 hướng. Trần: gai đá chĩa xuống, gai lớn ở giữa, mũi gai kính
xanh lá + verdant froglight. Cột đá quanh sàn, măng đá tự dựng. Hang đến (spawn) ở phía nam, cao hơn
nền, có đường gấp khúc xuống như hang spawn mặt đảo.
Schem phủ trọn hộp boss cũ (x-23..23, y-58..-28, z-51..47) nên dán đè là xóa phòng boss cũ.
Dán: //schem load sylphens_boss_cavern   rồi   //paste -o -m !structure_void
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
nrng = np.random.default_rng(SEED)
OUT = ROOT / "output"
SCHEM_NAME = "sylphens_boss_cavern"

# ------------------------------------------------------------------ hộp schem
X0, X1, Z0, Z1 = -57, 57, -57, 66
Y0, Y1 = -63, -22                   # -64 là sàn barrier; -21 trở lên là tầng 5 Layer 2
NX, NY, NZ = X1 - X0 + 1, Y1 - Y0 + 1, Z1 - Z0 + 1
OLD_BOX = dict(x=(-23, 23), y=(-58, -28), z=(-51, 47))

# ------------------------------------------------------------------ thông số hang
FL = -61                            # khối nền hang (đứng Y-60)
APEX = -25                          # khí cao nhất ở đỉnh vòm
R = 46.0                            # bán kính hang
WALL_H = 6                          # tường gần thẳng đứng cao ~6 trước khi cong thành vòm
RP = 24                             # bán kính sàn tròn giữa
PT = FL + 3                         # khối mặt sàn tròn (đứng Y-57)
CRACK_GAP = 7                       # lưới vết nứt: mỗi ~7 khối một đường
CIRCLE_R = 8                        # vòng triệu hồi
SHELL = 6                           # vỏ: lớp 1-3 deepslate tự nhiên, 4 bedrock, 5-6 deepslate
PILLAR_R, PILLAR_ANG = 36, [0, 45, 135, 180, 225, 270, 315]   # bỏ hướng nam (90°) cho đường xuống
ARR_DIR = 90                        # hang đến ở phía nam (+z)
AF = FL + 7                         # sàn hang đến (Y-54)

PAL = {}
def bid(n):
    n = n if n.startswith("minecraft:") else "minecraft:" + n
    if n not in PAL: PAL[n] = len(PAL)
    return PAL[n]
VOID, AIR, DEEP, BED = bid("structure_void"), bid("air"), bid("deepslate"), bid("bedrock")
W = np.full((NX, NY, NZ), VOID, dtype=np.uint16)
def I(x, y, z): return x - X0, y - Y0, z - Z0
def inb(x, y, z): return X0 <= x <= X1 and Y0 <= y <= Y1 and Z0 <= z <= Z1
def setb(x, y, z, n):
    if inb(x, y, z): W[I(x, y, z)] = bid(n)
def getn(x, y, z):
    return INV[W[I(x, y, z)]] if inb(x, y, z) else None
def pick(spec):
    if "%" not in spec: return spec
    parts = [p.split("%") for p in spec.split(",")]
    return rng.choices([n for _, n in parts], weights=[float(w) for w, _ in parts])[0]

xs, ys, zs = np.arange(X0, X1 + 1), np.arange(Y0, Y1 + 1), np.arange(Z0, Z1 + 1)
GX, GZ = np.meshgrid(xs, zs, indexing="ij")
GR = np.hypot(GX, GZ)
GT = np.degrees(np.arctan2(GZ, GX))
# đáy đảo thật (01_terrain): khối đảo có ở y >= b
ISL_BOT = np.ceil(-60 + (GR / 100) ** 3 * 55 + perlin(7, GX, 0, GZ, 0.03, 3, 0.5) * 5).astype(int)

# ------------------------------------------------------------------ 1) lòng hang
ang = np.radians(GT)
wob = (perlin(41, np.cos(ang) * 9, 0, np.sin(ang) * 9, 0.35, 2, 0.5) * 2 - 1)    # méo tường theo góc
REFF = R + 3.0 * wob
rho = np.clip(GR / REFF, 0, 1)
cnoise = (perlin(42, GX, 0, GZ, 0.07, 3, 0.5) * 2 - 1) * 2.5
CEIL = np.floor(FL + 1 + WALL_H + (APEX - FL - 1 - WALL_H) * (1 - rho ** 2) ** 0.45 + cnoise * (rho < 0.97)).astype(int)
CEIL = np.minimum(CEIL, APEX)
AIRM = np.zeros((NX, NY, NZ), bool)
for j, y in enumerate(ys):
    AIRM[:, j, :] = (GR <= REFF) & (y > FL) & (y <= CEIL)

# ------------------------------------------------------------------ 2) hang đến + đường hầm (phía nam)
th = math.radians(ARR_DIR)
ARR_C = (round(55 * math.cos(th)), round(55 * math.sin(th)))     # tâm buồng hang đến
arr_air = set()
for x in range(-9, 10):
    for z in range(46, 64):
        dx, dz = x - ARR_C[0], z - ARR_C[1]
        p2 = (dx / 7.5) ** 2 + (dz / 6.5) ** 2
        if p2 <= 1:
            for y in range(AF + 1, AF + 3 + int(round(5 * math.sqrt(1 - p2))) + 1):
                arr_air.add((x, y, z))
for z in range(38, 50):                                           # đường hầm cửa hang rộng 5 cao 5
    for x in range(-2, 3):
        for y in range(AF + 1, AF + 6):
            if abs(x) == 2 and y == AF + 5: continue
            arr_air.add((x, y, z))
for (x, y, z) in arr_air:
    if inb(x, y, z): AIRM[I(x, y, z)] = True

# ------------------------------------------------------------------ 3) vỏ: dãn lòng hang 6 lớp
depth = np.zeros(AIRM.shape, np.int8)
cur = AIRM.copy()
for d in range(1, SHELL + 1):
    nxt = cur.copy()
    nxt[1:] |= cur[:-1]; nxt[:-1] |= cur[1:]
    nxt[:, 1:] |= cur[:, :-1]; nxt[:, :-1] |= cur[:, 1:]
    nxt[:, :, 1:] |= cur[:, :, :-1]; nxt[:, :, :-1] |= cur[:, :, 1:]
    depth[nxt & ~cur] = d
    cur = nxt
SHELLM = cur & ~AIRM
W[SHELLM] = DEEP
yy3 = ys[None, :, None]
W[SHELLM & (depth == 4) & (yy3 <= -24)] = BED
# hộp boss cũ: phần không thuộc hang/vỏ -> deepslate nếu nằm trong đảo, khí nếu lòi ra dưới đáy đảo
ox, oy, oz = OLD_BOX["x"], OLD_BOX["y"], OLD_BOX["z"]
oi = slice(ox[0] - X0, ox[1] - X0 + 1); oj = slice(oy[0] - Y0, oy[1] - Y0 + 1); ok = slice(oz[0] - Z0, oz[1] - Z0 + 1)
sub = W[oi, oj, ok]
rest = sub == VOID
inside = ys[oj][None, :, None] >= ISL_BOT[oi, ok][:, None, :]
sub[rest & inside] = DEEP
sub[rest & ~inside] = AIR
OLDAIR = np.zeros(W.shape, bool); OLDAIR[oi, oj, ok] = rest & ~inside      # khí xóa phần hộp cũ lòi ra, không thuộc phòng
W[AIRM] = AIR

# ------------------------------------------------------------------ 4) vật liệu bề mặt tự nhiên
INV = {}
def refresh_inv():
    INV.clear(); INV.update({v: k.replace("minecraft:", "") for k, v in PAL.items()})
air = W == AIR
adj = np.zeros_like(air)
adj[1:] |= air[:-1]; adj[:-1] |= air[1:]; adj[:, 1:] |= air[:, :-1]; adj[:, :-1] |= air[:, 1:]
adj[:, :, 1:] |= air[:, :, :-1]; adj[:, :, :-1] |= air[:, :, 1:]
surf = adj & (W == DEEP)
X3 = xs[:, None, None] + 0 * yy3; Z3 = zs[None, None, :] + 0 * yy3
pn = perlin(43, X3, yy3 + 0 * X3, Z3, 0.11, 2, 0.5)
mat = np.where(pn > 0.64, bid("cobbled_deepslate"), np.where(pn < 0.3, bid("polished_deepslate"), DEEP))
rnd = nrng.random(W.shape)
mat = np.where(rnd < 0.012, bid("deepslate_emerald_ore"), np.where(rnd < 0.03, bid("deepslate_coal_ore"), mat))
floor_surf = surf & (yy3 == FL)
W[surf] = mat[surf]
fl_mat = np.where(pn > 0.58, bid("cobbled_deepslate"), np.where(rnd < 0.08, bid("gravel"), DEEP))
W[floor_surf] = fl_mat[floor_surf]

# ------------------------------------------------------------------ 5) sàn tròn giữa, bậc thang 4 hướng, vết nứt, vòng triệu hồi
plat2 = GR <= RP + 0.4
stairs = []                                    # (x, z, y, facing)
for dirx, dirz, face in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
    for o in (1, 2, 3):
        for q in range(-3, 4):
            a = RP + o
            x, z = (a * dirx + (q if dirz else 0), a * dirz + (q if dirx else 0))
            stairs.append((x, z, PT + 1 - o, face))
    for a in range(int(RP - 3), RP + 1):          # lấp khe giữa mép tròn và bậc
        for q in range(-3, 4):
            x, z = (a * dirx + (q if dirz else 0), a * dirz + (q if dirx else 0))
            plat2[x - X0, z - Z0] = True
for i, j in zip(*np.nonzero(plat2)):
    x, z = int(xs[i]), int(zs[j])
    r = math.hypot(x, z)
    for y in range(FL + 1, PT):
        setb(x, y, z, "cracked_deepslate_bricks" if rng.random() < 0.25 else "deepslate_bricks")
    if r > RP - 1.5:
        setb(x, PT, z, "chiseled_deepslate" if (int(math.degrees(math.atan2(z, x)) // 6) % 2) else "polished_deepslate")
    else:
        setb(x, PT, z, pick("50%polished_deepslate,30%deepslate_tiles,20%cracked_deepslate_tiles"))
for (x, z, y, face) in stairs:
    for yy in range(FL + 1, y): setb(x, yy, z, "deepslate_bricks")
    setb(x, y, z, f"polished_deepslate_stairs[facing={face},half=bottom]")
# vết nứt lưới: 2 họ đường lượn sóng
CRACK_LAYERS = ["blue_concrete", "green_stained_glass", "green_stained_glass",
                "lime_stained_glass", "lime_stained_glass", "lime_stained_glass"]     # từ đáy lên mặt
def crack_col(x, z, light_p):
    for k, n in enumerate(CRACK_LAYERS):
        setb(x, PT - 5 + k, z, n)
    if rng.random() < light_p: setb(x, PT + 1, z, "light[level=15]")
crack_cells = set()
for x in range(-RP, RP + 1):
    for z in range(-RP, RP + 1):
        r = math.hypot(x, z)
        if r > RP - 2.2 or r < CIRCLE_R + 1.6: continue
        kz = round((z - 1.8 * math.sin(x / 6.3)) / CRACK_GAP)
        kx = round((x - 1.8 * math.sin(z / 5.7 + 1.3)) / CRACK_GAP)
        dz_ = abs(z - (kz * CRACK_GAP + 1.8 * math.sin(x / 6.3 + kz * 1.7)))
        dx_ = abs(x - (kx * CRACK_GAP + 1.8 * math.sin(z / 5.7 + 1.3 + kx * 1.1)))
        if min(dz_, dx_) < 0.6: crack_cells.add((x, z))
for (x, z) in crack_cells: crack_col(x, z, 0.28)
# vòng triệu hồi
for x in range(-CIRCLE_R - 1, CIRCLE_R + 2):
    for z in range(-CIRCLE_R - 1, CIRCLE_R + 2):
        r = math.hypot(x, z); a = math.degrees(math.atan2(z, x)) % 360
        if CIRCLE_R - 0.6 <= r <= CIRCLE_R + 0.5: crack_col(x, z, 0.45)
        elif 5.0 <= r <= 5.8: setb(x, PT, z, "chiseled_deepslate")
        elif r < 5.0:
            spoke = min(a % 45, 45 - a % 45) < 6 and r > 1.6
            setb(x, PT, z, "reinforced_deepslate" if r <= 1.6 else ("lime_stained_glass" if spoke else "polished_deepslate"))
            if spoke:
                setb(x, PT - 1, z, "lime_stained_glass"); setb(x, PT - 2, z, "green_stained_glass")
for dx, dz in ((3, 0), (-3, 0), (0, 3), (0, -3)):
    setb(dx, PT + 1, dz, "light[level=12]")

# ------------------------------------------------------------------ 6) khối nón: gai trần, măng đá, cột
CONE_MAT = "60%deepslate,25%cobbled_deepslate,15%deepslate_tiles"
def cone(bx, by, bz, dx, dy, dz, L, R0, glow_tip=False, power=1.25):
    """nón từ gốc (bx,by,bz) theo hướng (dx,dy,dz), dài L, bán kính gốc R0; chỉ ghi vào khí hoặc vỏ"""
    n = math.sqrt(dx * dx + dy * dy + dz * dz); dx, dy, dz = dx / n, dy / n, dz / n
    ext = int(L + R0 + 2)
    tipx, tipy, tipz = bx + dx * L, by + dy * L, bz + dz * L
    cells = []
    for x in range(int(bx - ext), int(bx + ext) + 1):
        for y in range(int(by - ext), int(by + ext) + 1):
            for z in range(int(bz - ext), int(bz + ext) + 1):
                if not inb(x, y, z): continue
                vx, vy, vz = x - bx, y - by, z - bz
                s = vx * dx + vy * dy + vz * dz
                if s < -1 or s > L: continue
                px, py, pz = vx - s * dx, vy - s * dy, vz - s * dz
                d = math.sqrt(px * px + py * py + pz * pz)
                rad = R0 * max(0.0, 1 - max(s, 0) / L) ** power
                if d <= rad + 0.35:
                    cells.append((x, y, z, s, d, rad))
    for (x, y, z, s, d, rad) in cells:
        cur = W[I(x, y, z)]
        if cur == VOID or cur == BED: continue
        if glow_tip and s > L - 3.2:
            n_ = "verdant_froglight" if d < rad - 0.6 else ("lime_stained_glass" if s > L - 1.6 else "green_stained_glass")
        else:
            n_ = pick(CONE_MAT)
        setb(x, y, z, n_)
    if glow_tip:                                   # chắc chắn có lõi phát sáng ở mũi
        setb(round(tipx - dx * 2), round(tipy - dy * 2), round(tipz - dz * 2), "verdant_froglight")

refresh_inv()
def ceil_at(x, z): return int(CEIL[x - X0, z - Z0])
def floor_at(x, z):
    return PT if math.hypot(x, z) <= RP + 3.5 else FL
spikes, mites, pillars = [], [], []
# gai lớn ở giữa + vòng gai nghiêng tủa ra quanh nó
cone(0, ceil_at(0, 0) + 2, 0, 0, -1, 0, 17, 6.5, glow_tip=True, power=1.1)
spikes.append((0, 0, 17))
for k in range(7):
    a = math.radians(k * 360 / 7 + 10)
    bx, bz = 10 * math.cos(a), 10 * math.sin(a)
    L = rng.uniform(9, 12)
    cone(bx, ceil_at(round(bx), round(bz)) + 2, bz, math.cos(a) * 0.45, -1, math.sin(a) * 0.45, L, rng.uniform(2.6, 3.4), glow_tip=True)
    spikes.append((round(bx), round(bz), L))
# gai rải khắp trần
tries = 0
while len(spikes) < 46 and tries < 4000:
    tries += 1
    x, z = rng.uniform(-R + 5, R - 5), rng.uniform(-R + 5, R - 5)
    r = math.hypot(x, z)
    if r > R - 6 or r < 15: continue
    if any(math.hypot(x - sx, z - sz) < 6.5 for sx, sz, _ in spikes): continue
    xi, zi = round(x), round(z)
    room = ceil_at(xi, zi) - floor_at(xi, zi)
    L = min(rng.uniform(5, 13), room - 9)
    if L < 4: continue
    tilt = 0.25 * r / R
    cone(x, ceil_at(xi, zi) + 2, z, x / r * tilt, -1, z / r * tilt, L, rng.uniform(1.6, 3.4), glow_tip=rng.random() < 0.8)
    spikes.append((xi, zi, L))
# cột đá (hình đồng hồ cát, nối nền với trần)
for a_ in PILLAR_ANG:
    a = math.radians(a_ + rng.uniform(-6, 6))
    cx, cz = PILLAR_R * math.cos(a), PILLAR_R * math.sin(a)
    top = ceil_at(round(cx), round(cz)) + 2
    h = top - FL
    rb = rng.uniform(3.4, 4.4)
    for y in range(FL, top + 1):
        t = (y - FL) / h
        rad = rb * (0.62 + 0.38 * (2 * t - 1) ** 2) + 0.4 * math.sin(y * 0.7 + a_)
        for x in range(int(cx - 6), int(cx + 7)):
            for z in range(int(cz - 6), int(cz + 7)):
                if math.hypot(x - cx, z - cz) <= rad and inb(x, y, z) and W[I(x, y, z)] not in (VOID, BED):
                    vein = abs(math.sin(math.atan2(z - cz, x - cx) * 3 + y * 0.25)) < 0.07
                    setb(x, y, z, "green_stained_glass" if vein and math.hypot(x - cx, z - cz) > rad - 1 else pick(CONE_MAT))
    pillars.append((round(cx), round(cz)))
# măng đá trên nền vòng ngoài
def blocked_floor(x, z):
    r = math.hypot(x, z); t = math.degrees(math.atan2(z, x))
    if r < RP + 5 or r > R - 4: return True
    if abs(t - ARR_DIR) < 32: return True                         # chừa đường xuống từ hang đến
    if any(math.hypot(x - px, z - pz) < 8 for px, pz in pillars): return True
    for dxx, dzz in ((1, 0), (0, 1)):                             # chừa lối thẳng tới 4 bậc thang
        if abs(x * dzz - z * dxx) < 6 and (x * dxx + z * dzz) != 0: return True
    return False
tries = 0
while len(mites) < 26 and tries < 4000:
    tries += 1
    x, z = rng.uniform(-R, R), rng.uniform(-R, R)
    if blocked_floor(x, z) or any(math.hypot(x - mx, z - mz) < 6 for mx, mz in mites): continue
    xi, zi = round(x), round(z)
    if not inb(xi, FL + 1, zi) or W[I(xi, FL + 1, zi)] != AIR: continue
    L = min(rng.uniform(3, 10), ceil_at(xi, zi) - FL - 4)
    if L < 3: continue
    cone(x, FL, z, 0, 1, 0, L, rng.uniform(1.3, 3.0), glow_tip=rng.random() < 0.3, power=1.4)
    mites.append((x, z))

# ------------------------------------------------------------------ 7) hang đến: sàn, đèn, đường gấp khúc xuống nền
refresh_inv()
for (x, y, z) in arr_air:
    if y == AF + 1:
        setb(x, AF, z, pick("55%deepslate,30%cobbled_deepslate,15%polished_deepslate"))
for dx in (-1, 0, 1):
    for dz in (-1, 0, 1):
        setb(ARR_C[0] + dx, AF, ARR_C[1] + dz, "reinforced_deepslate" if (dx, dz) == (0, 0) else "polished_deepslate")
for a in (45, 135, 225, 315):
    setb(round(ARR_C[0] + 5 * math.cos(math.radians(a))), AF + 1, round(ARR_C[1] + 4.5 * math.sin(math.radians(a))), "soul_lantern")
PATH = "55%cobbled_deepslate,30%polished_deepslate,15%deepslate_tiles"
SLOPE = 3
R1 = 44.5
BANDS = [(R1 - 4, R1), (R1 - 9, R1 - 5)]
RAILS = [(R1 - 5, R1 - 4)]
LEGS = [(0, ARR_DIR - 4, ARR_DIR - 26), (1, ARR_DIR - 26, ARR_DIR + 40)]
path_floor, path_kind = {}, {}
levels = [AF]
for k, (band, t0, t1) in enumerate(LEGS):
    a_, b_ = BANDS[band]; rm = (a_ + b_) / 2; y0 = levels[-1]; sg = 1 if t1 > t0 else -1
    for x in range(-50, 51):
        for z in range(0, 51):
            r = math.hypot(x, z)
            if not (a_ <= r < b_): continue
            t = math.degrees(math.atan2(z, x))
            if k == 0 and abs(t - ARR_DIR) <= 4:
                path_floor[(x, z)] = AF; path_kind[(x, z)] = "terrace"; continue
            sd = (t - t0) * sg
            if not (0 <= sd <= abs(t1 - t0)): continue
            y = y0 - int(math.radians(sd) * rm // SLOPE)
            if y <= FL: continue
            path_floor[(x, z)] = y; path_kind[(x, z)] = "path"
    levels.append(y0 - int(math.radians(abs(t1 - t0)) * rm // SLOPE))
    if k == 0:
        lo, hi = sorted((t1, t1 - 3.5))
        for x in range(-50, 51):
            for z in range(0, 51):
                r = math.hypot(x, z); t = math.degrees(math.atan2(z, x))
                if lo <= t <= hi and BANDS[1][0] <= r < BANDS[0][1]:
                    path_floor[(x, z)] = levels[-1]; path_kind[(x, z)] = "turn"
for x in range(-50, 51):
    for z in range(0, 51):
        r = math.hypot(x, z)
        if not (RAILS[0][0] <= r < RAILS[0][1]) or (x, z) in path_floor: continue
        best = max((path_floor[q] for q in ((x + a, z + b) for a in (-1, 0, 1) for b in (-1, 0, 1))
                    if q in path_floor and path_kind[q] in ("path", "terrace") and BANDS[0][0] <= math.hypot(*q)), default=None)
        if best is not None: path_floor[(x, z)] = best; path_kind[(x, z)] = "rail"
for (x, z), y in path_floor.items():
    for yy in range(FL + 1, y): setb(x, yy, z, pick("70%deepslate,30%cobbled_deepslate"))
    setb(x, y, z, pick(PATH))
    for yy in range(y + 1, y + 4):
        if inb(x, yy, z) and getn(x, yy, z) not in ("air", None) and (x, yy, z) not in arr_air: setb(x, yy, z, "air")
    if path_kind[(x, z)] == "rail": setb(x, y + 1, z, "polished_deepslate_wall")
for (x, z), y in path_floor.items():
    if path_kind[(x, z)] == "rail": continue
    for ddx, ddz, face in ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")):
        q = (x + ddx, z + ddz)
        if q in path_floor and path_kind[q] != "rail" and path_floor[q] == y + 1:
            setb(x, y + 1, z, f"cobbled_deepslate_stairs[facing={face},half=bottom]"); break

# ------------------------------------------------------------------ kiểm tra
refresh_inv()
SOFTK = ("air", "light", "lantern", "structure_void")
soft_ids = [i for n, i in PAL.items() if any(k in n for k in ("air", "light[", "lantern"))]
P = np.isin(W, soft_ids)
# hở ra ngoài: ô khí của phòng kề một ô void mà ngoài đời là khí (dưới đáy đảo)
roomair = P & ~OLDAIR
outside = (W == VOID) & ((yy3 + 0 * W) < ISL_BOT[:, None, :])
leak = 0
for ax_ in range(3):
    for s_ in (1, -1):
        nb = np.roll(outside, s_, axis=ax_)
        sl = [slice(None)] * 3; sl[ax_] = slice(0, 1) if s_ == 1 else slice(-1, None)
        nb[tuple(sl)] = False
        leak += int((roomair & nb).sum())
bed_vis = 0
for ax_ in range(3):
    for s in (1, -1):
        bed_vis += int(((W == BED) & np.roll(P, s, axis=ax_)).sum())
def stand(x, y, z):
    if not (inb(x, y, z) and inb(x, y + 1, z) and inb(x, y - 1, z)): return False
    return P[I(x, y, z)] and P[I(x, y + 1, z)] and not P[I(x, y - 1, z)]
start = (ARR_C[0], AF + 1, ARR_C[1] - 2)
seen = {start}; dq = deque([start])
while dq:
    x, y, z = dq.popleft()
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for dy in (0, 1, -1, -2, -3):
            n = (x + dx, y + dy, z + dz)
            if dy == 1 and not (inb(x, y + 2, z) and P[I(x, y + 2, z)]): continue
            if stand(*n):
                if n not in seen: seen.add(n); dq.append(n)
                break
reach_floor = any(p[1] == FL + 1 for p in seen)
reach_center = (0, PT + 1, 2) in seen or (2, PT + 1, 0) in seen
jumps = sum(1 for (x, z), y in path_floor.items() if path_kind[(x, z)] != "rail"
            for q in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))
            if q in path_floor and path_kind[q] != "rail" and path_floor[q] == y + 2)
# khoảng trống dưới mũi gai thấp nhất trên sàn tròn
low = 999
for i, j in zip(*np.nonzero(GR <= RP)):
    col = W[i, PT + 2 - Y0:, j]
    solid = np.nonzero(~np.isin(col, soft_ids))[0]
    if len(solid): low = min(low, int(solid[0]) + 1)

if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    print(f"hộp {NX}×{NY}×{NZ} gốc ({X0},{Y0},{Z0}), palette {len(PAL)}")
    print(f"hang: r{R:.0f}, nền Y{FL}, sàn tròn r{RP} mặt Y{PT}, đỉnh vòm Y{APEX}; gai {len(spikes)}, măng {len(mites)}, cột {len(pillars)}, vết nứt {len(crack_cells)} ô")
    print(f"ô khí hở ra ngoài đảo: {leak} · bedrock lộ: {bed_vis} · khoảng trống thấp nhất trên sàn tròn: {low} khối")
    print(f"đường xuống: chặng {levels}, bậc phải nhảy {jumps} · đi bộ từ hang đến: tới nền {reach_floor}, lên tâm sàn tròn {reach_center}")
    write_schem(OUT / f"{SCHEM_NAME}.schem", W, PAL, (X0, Y0, Z0))
    data = dict(schem=SCHEM_NAME, box=dict(x=[X0, X1], y=[Y0, Y1], z=[Z0, Z1]),
                arrival=dict(x=ARR_C[0], y=AF + 1, z=ARR_C[1], yaw=180, note="giữa hang đến phía nam, nhìn về tâm; cũng là điểm thoát"),
                boss_spawn=dict(x=0, y=PT + 1, z=0, note="tâm vòng triệu hồi"),
                summon_circle=dict(x=0, y=PT, z=0, r=CIRCLE_R),
                platform=dict(r=RP, y_top=PT), floor_y=FL,
                minion_spawns=[dict(x=round(15 * math.cos(math.radians(a))), y=PT + 1, z=round(15 * math.sin(math.radians(a))))
                               for a in range(0, 360, 45)] +
                              [dict(x=round(31 * math.cos(math.radians(a))), y=FL + 1, z=round(31 * math.sin(math.radians(a))))
                               for a in (0, 60, 120, 180, 240, 300)],
                pillars=[dict(x=x, z=z) for x, z in pillars])
    json.dump(data, open(OUT / "boss_cavern.json", "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=1)
    print("schem xong", os.path.getsize(OUT / f"{SCHEM_NAME}.schem"))
