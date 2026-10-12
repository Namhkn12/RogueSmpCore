#!/usr/bin/env python3
"""
Kvelsgard — sinh toàn bộ khối đảo (mặt núi, bãi mỏ, đường leo, hang dẫn, hố, 3 tầng mỏ, tấm bedrock; không có quặng, plugin tự sinh)
ra 4 schematic cắt theo dải Y, cộng dữ liệu vị trí cho plugin và báo cáo kiểm tra.

  python3 scripts/generate_world.py            (~1-2 phút)

Đầu ra:
  output/kvelsgard_l1.schem  Y102..đỉnh   mặt núi, bãi mỏ, đường leo, hang dẫn, nửa trên hố
  output/kvelsgard_l2.schem  Y69..101     Hầm Mỏ Cũ + tấm bedrock Y69 + tấm dưới Layer 1
  output/kvelsgard_l3.schem  Y37..68      Rừng Thạch Nhũ + tấm bedrock Y37
  output/kvelsgard_l4.schem  Y-60..36     Vực Deepslate + tấm bedrock Y3 + phần đáy đảo
  output/layout.json         spawn, phòng, hang, đường xuống (tọa độ thật) cho plugin
  output/report.txt          kết quả kiểm tra (bedrock lộ, lỗ tấm ngăn, đi bộ tới mọi phòng)
  images/*.png               bản đồ (scripts/render_maps.py)
Mọi schem dán bằng //paste -o (gốc lưu trong file), ô ngoài đảo là structure_void.
"""
import sys, json, math, random
from collections import deque
from pathlib import Path
import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(ROOT.parent / "tools"))
sys.path.insert(0, str(HERE))
from schem_io import write_schem
import kv_model as M
from kv_model import F, SEAL, X0, X1, Z0, Z1, Y0

rng = random.Random(M.SEED)
OUT = ROOT / "output"
PREFIX = "kvelsgard"

# ================================================================== lưới cột
GX, GZ = np.meshgrid(np.arange(X0, X1 + 1), np.arange(Z0, Z1 + 1), indexing="ij")
NX, NZ = GX.shape
RR = np.hypot(GX, GZ)
EDGE = M.edge_r(GX, GZ)
ISL = RR <= EDGE
FOOT = RR <= EDGE + 1.5
ZONE = M.zone(GX, GZ)

HT = np.where(ZONE == "mountain", M.mountain_h(GX, GZ), M.yard_h(GX, GZ))
# bãi mỏ phẳng tuyệt đối ở đầu đường leo, dải ray và trước cửa hang
flat = (ZONE == "yard") & (((GZ >= 78) & (np.abs(GX) <= 36)) | ((np.abs(GX + 4) <= 9) & (GZ <= 82)))
HT[flat] = M.YARD_Y

# hõm đường leo: thềm, 4 nhánh, chỗ quay đầu, lan can
NOTCH = {}
for x in range(-M.NOTCH_X + 1, M.NOTCH_X):
    for z in range(M.NOTCH_Z + 1, Z1 + 1):
        if not ISL[x - X0, z - Z0]:
            continue
        NOTCH[(x, z)] = list(M.notch_cell(x, z))
def _notch_y(x, z):
    c = NOTCH.get((x, z))
    if c is None:
        return int(HT[x - X0, z - Z0])
    return c[1]
for (x, z), c in NOTCH.items():
    if c[0] == "rail":
        a, b = _notch_y(x, z + 1), _notch_y(x, z - 1)
        a = a if a is not None else b
        b = b if b is not None else a
        c[1] = max(a, b)
        c.append(abs(a - b) >= 1)            # có tường thấp phía trên
for (x, z), c in NOTCH.items():
    HT[x - X0, z - Z0] = c[1]

BOT = M.bottom_y(GX, GZ)
Y1 = int(HT[ISL].max()) + 3
NY = Y1 - Y0 + 1
HT_MIN = HT.copy()                      # mặt đất thấp nhất trong ô 5x5: tấm L1 luôn cách mọi ô trống bên cạnh >= 3 khối
for dx in range(-2, 3):
    for dz in range(-2, 3):
        HT_MIN = np.minimum(HT_MIN, np.roll(np.roll(HT, dx, 0), dz, 1))
SEAL1 = np.minimum(M.SEAL1_MAX, HT_MIN - 3)

# ================================================================== bảng khối + khối thế giới
PAL = {}
NAMES = []
def bid(n):
    n = n if n.startswith("minecraft:") else "minecraft:" + n
    if n not in PAL:
        PAL[n] = len(PAL)
        NAMES.append(n)
    return PAL[n]
VOID, AIR, WATER, LAVA, BED = bid("structure_void"), bid("air"), bid("water"), bid("lava"), bid("bedrock")

W = np.full((NX, NY, NZ), VOID, dtype=np.uint16)
fx, fz = np.nonzero(FOOT)
W[fx, :, fz] = AIR

def inb(x, y, z):
    return X0 <= x <= X1 and Y0 <= y <= Y1 and Z0 <= z <= Z1
def put(x, y, z, name):
    if inb(x, y, z):
        W[x - X0, y - Y0, z - Z0] = bid(name)
def get(x, y, z):
    return NAMES[W[x - X0, y - Y0, z - Z0]] if inb(x, y, z) else "minecraft:structure_void"
def is_air(x, y, z):
    return inb(x, y, z) and W[x - X0, y - Y0, z - Z0] == AIR
def pick(spec):
    if "%" not in spec:
        return spec
    parts = [p.split("%", 1) for p in spec.split(",")]
    return rng.choices([n for _, n in parts], weights=[float(w) for w, _ in parts])[0]
def carve_cols(xs, zs, ylo, yhi, block=None):
    """đặt `block` (mặc định khí) vào mọi ô ylo..yhi của từng cột (xs, zs)"""
    block = AIR if block is None else block
    xs, zs, ylo, yhi = map(np.asarray, (xs, zs, ylo, yhi))
    if xs.size == 0:
        return
    for y in range(int(ylo.min()), int(yhi.max()) + 1):
        m = (ylo <= y) & (y <= yhi)
        if m.any():
            W[xs[m] - X0, y - Y0, zs[m] - Z0] = block

# ------------------------------------------------------------------ dải đá theo tầng
JIT = np.round((M.vn(GX, GZ, 9, seed=6) - 0.5) * 4).astype(int)
def band_of(y):
    if y >= M.SEAL1_MAX + 1: return 1
    if y >= SEAL[2]: return 2
    if y >= SEAL[3]: return 3
    return 4
ROCK_IDS = {}
def rock_slab(y):
    n = M.vn3(GX, y, GZ, 7, seed=11)
    n2 = M.vn3(GX, y, GZ, 13, seed=12)
    yy = y + JIT
    out = np.full(GX.shape, bid("stone"), dtype=np.uint16)
    b1 = yy >= M.SEAL1_MAX + 1
    b2 = (yy >= SEAL[2]) & ~b1
    b3 = (yy >= SEAL[3]) & ~b1 & ~b2
    b4 = yy < SEAL[3]
    out[b1 & (n < 0.30)] = bid("andesite")
    out[b1 & (n > 0.72)] = bid("tuff")
    out[b2 & (n < 0.30)] = bid("andesite")
    out[b2 & (n > 0.70)] = bid("tuff")
    out[b2 & (n2 > 0.78)] = bid("diorite")
    out[b3 & (n < 0.30)] = bid("granite")
    out[b3 & (n > 0.70)] = bid("dripstone_block")
    out[b3 & (n2 > 0.78)] = bid("calcite")
    out[b4] = bid("deepslate")
    out[b4 & (n < 0.26)] = bid("tuff")
    out[b4 & (n > 0.76)] = bid("basalt")
    out[b4 & (n2 > 0.80)] = bid("blackstone")
    return out

for yi in range(NY):
    y = Y0 + yi
    m = ISL & (BOT <= y) & (y <= HT)
    if m.any():
        W[:, yi, :][m] = rock_slab(y)[m]

# ------------------------------------------------------------------ mặt đất
nrng = np.random.default_rng(M.SEED)
def surface_pick(mask, spec):
    parts = [p.split("%", 1) for p in spec.split(",")]
    ids = np.array([bid(n) for _, n in parts], dtype=np.uint16)
    w = np.array([float(p) for p, _ in parts]); w /= w.sum()
    xs, zs = np.nonzero(mask)
    ch = ids[nrng.choice(len(ids), size=xs.size, p=w)]
    W[xs, HT[xs, zs] - Y0, zs] = ch

surface_pick(ISL & (ZONE == "yard"), "35%coarse_dirt,25%gravel,15%stone,10%cobblestone,15%andesite")
snow = ISL & (ZONE == "mountain") & (HT + (M.vn(GX, GZ, 7, seed=8) - 0.5) * 10 >= 176)
surface_pick(snow, "100%snow_block")
loose = ISL & (ZONE == "mountain") & ~snow & (M.vn(GX, GZ, 6, seed=9) > 0.66)
surface_pick(loose, "60%gravel,40%cobblestone")

PATH_MAT = "40%gravel,30%coarse_dirt,20%cobblestone,10%andesite"
for (x, z), c in NOTCH.items():
    y = c[1]
    if c[0] == "rail":
        put(x, y, z, "cobblestone")
        if c[3]:
            put(x, y + 1, z, "cobblestone_wall")
    elif c[0] == "terrace":
        put(x, y, z, pick("45%coarse_dirt,25%gravel,15%podzol,15%stone"))
    else:
        put(x, y, z, pick(PATH_MAT))
# bậc thang ở chỗ nhánh lên 1 khối
STEP = {"east": (1, 0), "west": (-1, 0), "south": (0, 1), "north": (0, -1)}
for (x, z), c in NOTCH.items():
    if c[0] != "leg":
        continue
    asc = M.LEG_ASC[c[2]]
    dx, dz = STEP[asc]
    nxt = NOTCH.get((x + dx, z + dz))
    if nxt and nxt[0] in ("leg", "turn") and nxt[1] == c[1] + 1:
        put(x, c[1] + 1, z, f"cobblestone_stairs[facing={asc}]")
SPAWN = (0, M.TERRACE_Y + 1, 105)
for dx in (-1, 0, 1):
    for dz in (-1, 0, 1):
        put(dx, M.TERRACE_Y, SPAWN[2] + dz, "chiseled_stone_bricks" if dx == dz == 0 else "polished_andesite")

# ================================================================== Layer 1: hang dẫn
TL = M.tunnel_len()
tb = (GX >= -26) & (GX <= 26) & (GZ >= -80) & (GZ <= 36)
txs, tzs = GX[tb], GZ[tb]
ts, td, tseg = M.nearest_on_polyline(txs, tzs, M.TUNNEL)
tw = M.tunnel_halfw(ts) + (M.vn(ts * TL, 0, 6, seed=7) - 0.5) * 2
pit_r = np.hypot(txs - M.PIT["x"], tzs - M.PIT["z"])
keep = (td <= tw) & (pit_r > M.PIT["r"] - 0.5)
txs, tzs, ts, td, tw = txs[keep], tzs[keep], ts[keep], td[keep], tw[keep]
t_floor_c = np.round(M.tunnel_floor(ts)).astype(int)
bump = np.where(td > 3, np.round((M.vn(txs, tzs, 4, seed=5) - 0.5) * 2.4), 0).astype(int)
t_floor = t_floor_c + bump
e = np.clip(td / tw, 0, 1)
t_top = t_floor_c + np.round(M.tunnel_height(ts) * (1 - e ** 3) ** (1 / 3)).astype(int)
ok = t_top > t_floor + 1
carve_cols(txs[ok], tzs[ok], t_floor[ok] + 1, t_top[ok])
for x, z, y in zip(txs[ok], tzs[ok], t_floor[ok]):
    put(int(x), int(y), int(z), pick("45%stone,20%gravel,20%cobblestone,15%andesite"))
TUN_FLOOR = {(int(x), int(z)): int(y) for x, z, y in zip(txs[ok], tzs[ok], t_floor[ok])}
TUN_TOP = {(int(x), int(z)): int(y) for x, z, y in zip(txs[ok], tzs[ok], t_top[ok])}

def polyline_at(pts, a):
    """điểm + hướng đơn vị tại quãng đường a"""
    for i in range(1, len(pts)):
        ax, az = pts[i - 1]; bx, bz = pts[i]
        l = math.hypot(bx - ax, bz - az)
        if a <= l or i == len(pts) - 1:
            t = min(a / l, 1.0)
            return ax + (bx - ax) * t, az + (bz - az) * t, (bx - ax) / l, (bz - az) / l
        a -= l

def frame_line(cx, cz, nx, nz, half):
    cells = []
    o = -half
    while o <= half + 1e-9:
        c = (int(round(cx + nx * o)), int(round(cz + nz * o)))
        if c not in cells:
            cells.append(c)
        o += 0.5
    return cells

# khung gỗ chống, đèn lồng, cột đá
a = 3.0
frames = 0
while a < TL - 16:
    cx, cz, ux, uz = polyline_at(M.TUNNEL, a)
    s = a / TL
    half = float(M.tunnel_halfw(s)) - 1.6
    cells = [c for c in frame_line(cx, cz, -uz, ux, half) if c in TUN_FLOOR]
    if len(cells) >= 5:
        yb = min(TUN_TOP[c] for c in cells)
        axis = "x" if abs(uz) > abs(ux) else "z"
        for c in cells:
            put(c[0], yb, c[1], f"stripped_spruce_log[axis={axis}]")
        for c in (cells[0], cells[-1]):
            for y in range(TUN_FLOOR[c] + 1, yb):
                put(c[0], y, c[1], "spruce_log[axis=y]")
        if frames % 2 == 0 or a < 5:
            for c in (cells[len(cells) // 4], cells[3 * len(cells) // 4]):
                if is_air(c[0], yb - 1, c[1]):
                    put(c[0], yb - 1, c[1], "lantern[hanging=true]")
        frames += 1
    a += 7.0
pillars = 0
for _ in range(400):
    if pillars >= 16:
        break
    a = rng.uniform(0.3 * TL, TL - 18)
    s = a / TL
    if rng.random() > s:
        continue
    cx, cz, ux, uz = polyline_at(M.TUNNEL, a)
    w = float(M.tunnel_halfw(s))
    o = rng.choice((-1, 1)) * rng.uniform(4.0, w - 2.0)
    px, pz, pr = cx - uz * o, cz + ux * o, rng.uniform(0.9, 1.6)
    for x in range(int(px - 2), int(px + 3)):
        for z in range(int(pz - 2), int(pz + 3)):
            if (x - px) ** 2 + (z - pz) ** 2 <= pr * pr and (x, z) in TUN_FLOOR:
                for y in range(TUN_FLOOR[(x, z)] + 1, TUN_TOP[(x, z)] + 1):
                    put(x, y, z, pick("70%stone,30%andesite"))
    pillars += 1
# ray bỏ hoang trên các đoạn gần như bắc-nam
for i in range(1, len(M.TUNNEL)):
    (ax, az), (bx, bz) = M.TUNNEL[i - 1], M.TUNNEL[i]
    if abs(bx - ax) > 0.4 * abs(bz - az):
        continue
    for off in (-2, 2):
        step = 1 if bz > az else -1
        z = az + 3 * step
        while (z - bz) * step < -3:
            run = rng.randint(4, 9)
            t = (z - az) / (bz - az)
            x = int(round(ax + (bx - ax) * t)) + off
            cells = [(x, zz) for zz in range(z, z + run * step, step)]
            ys = {TUN_FLOOR.get(c) for c in cells}
            if None not in ys and len(ys) == 1:
                y = ys.pop()
                for c in cells:
                    if is_air(c[0], y + 1, c[1]):
                        put(c[0], y + 1, c[1], "rail[shape=north_south]")
            z += (run + rng.randint(2, 5)) * step

# ------------------------------------------------------------------ hố xuống tầng 2 + cầu thang vòng cung bám vách
P = M.PIT
pb = (np.hypot(GX - P["x"], GZ - P["z"]) <= P["r"] + 0.5)
pxs, pzs = GX[pb], GZ[pb]
prr = np.hypot(pxs - P["x"], pzs - P["z"])
ptop = P["top"] + np.round(P["dome"] * np.sqrt(np.clip(1 - (prr / (P["r"] + 0.5)) ** 2, 0, 1))).astype(int)
carve_cols(pxs, pzs, np.full(pxs.shape, F[2] + 1), ptop)
for x, z in zip(pxs, pzs):
    put(int(x), F[2], int(z), pick("40%stone,25%gravel,20%cobblestone,15%andesite"))

def spiral(cx, cz, ring, y_top, y_bot, phi0, sweep, sign, mat, stair, corbel=None, support_below=None, support_floor=None):
    """gờ xoắn bám vách: ô (x,z) thỏa `ring` nhận sàn ở mọi vòng; đi theo chiều `sign` (+1 tăng góc) thì xuống dần.
    corbel(x,z): đặt thêm 1 khối đỡ dưới gờ. support_below: gờ có y <= giá trị này được đắp đặc xuống support_floor."""
    drop = y_top - y_bot
    cells = []
    for x in range(int(cx) - 14, int(cx) + 15):
        for z in range(int(cz) - 14, int(cz) + 15):
            if not ring(x, z):
                continue
            phi = math.degrees(math.atan2(z - cz, x - cx))
            psi = ((phi - phi0) * sign) % 360
            while psi <= sweep:
                q = psi * drop / sweep
                k = int(math.floor(q))
                cells.append((x, z, y_top - k, q, q - k, phi))
                psi += 360
    for x, z, y, q, fr, phi in cells:
        put(x, y, z, pick(mat))
        if corbel and corbel(x, z) and is_air(x, y - 1, z):
            put(x, y - 1, z, pick(mat))
        if support_below is not None and y <= support_below:
            for yy in range(support_floor, y):
                put(x, yy, z, pick(mat))
    for x, z, y, q, fr, phi in cells:
        if fr < 0.3 and q >= 1:
            t = math.radians(phi)
            ax, az = math.sin(t) * sign, -math.cos(t) * sign          # hướng đi lên (ngược chiều xuống)
            facing = ("east" if ax > 0 else "west") if abs(ax) >= abs(az) else ("south" if az > 0 else "north")
            if is_air(x, y + 1, z):
                put(x, y + 1, z, f"{stair}[facing={facing}]")
    return cells

phi_in = math.degrees(math.atan2(-48 - P["z"], -12 - P["x"]))      # hướng hang dẫn đổ vào hố
PIT_SWEEP = (phi_in - 90) % 360 + 720                                 # kết thúc ở cửa hành lang phía nam (90°)
spiral(P["x"], P["z"], lambda x, z: 9.7 < math.hypot(x - P["x"], z - P["z"]) <= P["r"] + 0.5,
       M.TUNNEL_END_Y, F[2], phi_in, PIT_SWEEP, -1, "60%cobblestone,40%stone", "cobblestone_stairs",
       corbel=lambda x, z: math.hypot(x - P["x"], z - P["z"]) > 11.2)

# ================================================================== bề mặt: cổng mỏ, ray, cần trục, lán
MOUTH = M.TUNNEL[0]
for z in range(MOUTH[1] + 2, 77):
    for x in (MOUTH[0] - 3, MOUTH[0] + 3):
        if rng.random() < 0.85 and is_air(x, M.YARD_Y + 1, z):
            put(x, M.YARD_Y, z, "gravel")
            put(x, M.YARD_Y + 1, z, "rail[shape=north_south]")
def heap(cx, cz, r, h, spec):
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            d = math.hypot(x - cx, z - cz)
            if d > r or not ISL[x - X0, z - Z0]:
                continue
            top = int(round(h * (1 - d / r) + (rng.random() - 0.5)))
            base = int(HT[x - X0, z - Z0])
            for y in range(base + 1, base + 1 + max(0, top)):
                put(x, y, z, pick(spec))
heap(MOUTH[0] - 3, 80, 4, 3, "50%gravel,30%cobblestone,20%tuff")
heap(MOUTH[0] + 4, 81, 3, 2, "50%gravel,30%cobblestone,20%tuff")
heap(-52, 88, 5, 4, "40%cobblestone,30%gravel,30%tuff")
heap(50, 92, 4, 3, "40%cobblestone,30%gravel,30%tuff")
def log_col(x, z, y0, y1, n="spruce_log[axis=y]"):
    for y in range(y0, y1 + 1):
        put(x, y, z, n)
# cần trục gỗ ở mép tây
cx, cz = -50, 80
gy = int(HT[cx - X0, cz - Z0])
log_col(cx, cz, gy + 1, gy + 11)
for i in range(1, 8):
    put(cx - i, gy + 11, cz, "stripped_spruce_log[axis=x]")
for y in range(gy + 5, gy + 11):
    put(cx - 7, y, cz, "spruce_fence")
put(cx + 1, gy + 1, cz, "grindstone[face=floor]")
put(cx, gy + 1, cz + 1, "barrel[facing=up]")
# lán gác phía đông
sx, sz = 52, 82
sy = int(HT[sx - X0, sz - Z0])
for x in range(sx - 2, sx + 3):
    for z in range(sz - 2, sz + 3):
        put(x, sy, z, "spruce_planks")
        edge_ = abs(x - sx) == 2 or abs(z - sz) == 2
        for y in range(sy + 1, sy + 4):
            if abs(x - sx) == 2 and abs(z - sz) == 2:
                put(x, y, z, "spruce_log[axis=y]")
            elif edge_ and not (z == sz - 2 and x == sx and y < sy + 3):
                put(x, y, z, "spruce_planks" if y != sy + 2 or rng.random() < 0.6 else "air")
            else:
                put(x, y, z, "air")
        put(x, sy + 4, z, "spruce_slab")
        for y in range(sy - 3, sy):
            if get(x, y, z) == "minecraft:air":
                put(x, y, z, "cobblestone")
put(sx, sy + 3, sz, "lantern[hanging=true]")
put(sx + 1, sy + 1, sz + 1, "barrel[facing=up]")
put(sx - 1, sy + 1, sz + 1, "crafting_table")
for (lx, lz) in ((MOUTH[0] - 7, 34), (MOUTH[0] + 7, 34), (-20, 70), (16, 70), (0, 83)):
    ly = int(HT[lx - X0, lz - Z0])
    put(lx, ly + 1, lz, "spruce_fence")
    put(lx, ly + 2, lz, "spruce_fence")
    put(lx, ly + 3, lz, "lantern")
put(-12, M.YARD_Y + 1, 60, "campfire[lit=false]")
for (bx_, bz_) in ((-14, 58), (-10, 62), (10, 50), (12, 52)):
    put(bx_, int(HT[bx_ - X0, bz_ - Z0]) + 1, bz_, "barrel[facing=up]")

# ================================================================== 3 tầng mỏ
THEME = {
    2: dict(floor="45%stone,20%andesite,15%gravel,10%cobblestone,10%tuff", post="stripped_spruce_log[axis=y]",
            beam="spruce_log", plank="spruce_planks", lamp="lantern", stair="spruce_stairs",
            room_floor={"combat": "50%stone,25%cobblestone,25%gravel", "rest": "spruce_planks", "store": "spruce_planks",
                        "reward": "spruce_planks", "entry": "50%stone,50%cobblestone"},
            lining=None),
    3: dict(floor="30%stone,20%dripstone_block,15%calcite,15%mossy_cobblestone,10%granite,10%clay",
            post="dark_oak_log[axis=y]", beam="dark_oak_log", plank="dark_oak_planks", lamp="lantern", stair="dark_oak_stairs",
            room_floor={"combat": "40%mossy_cobblestone,30%stone,20%mud,10%clay", "rest": "dark_oak_planks",
                        "store": "dark_oak_planks", "reward": "40%stone_bricks,30%mossy_stone_bricks,30%cracked_stone_bricks",
                        "entry": "50%mossy_cobblestone,50%stone"},
            lining="45%stone_bricks,30%mossy_stone_bricks,25%cracked_stone_bricks"),
    4: dict(floor="40%deepslate,25%cobbled_deepslate,15%tuff,10%smooth_basalt,10%blackstone",
            post="polished_basalt[axis=y]", beam="polished_basalt", plank="polished_blackstone_bricks", lamp="soul_lantern",
            stair="polished_blackstone_brick_stairs",
            room_floor={"combat": "50%polished_deepslate,30%deepslate_tiles,20%cracked_deepslate_tiles",
                        "rest": "50%polished_deepslate,50%deepslate_tiles", "store": "polished_deepslate",
                        "reward": "50%deepslate_tiles,50%polished_deepslate", "entry": "60%cobbled_deepslate,40%deepslate"},
            lining="50%deepslate_bricks,30%cracked_deepslate_bricks,20%deepslate_tiles"),
}
OPEN_IDS = set()
def is_open_name(n):
    return n == "minecraft:air" or any(k in n for k in ("water", "lava", "rail", "lantern", "torch", "cobweb", "lichen", "pointed_dripstone",
                                "cave_vines", "ladder", "short_grass", "carpet", "structure_void",
                                "sculk_vein", "chain", "candle", "skull", "fire"))

def sub_grid(cx, cz, rx, rz):
    m = (np.abs(GX - cx) <= rx) & (np.abs(GZ - cz) <= rz)
    return GX[m], GZ[m]

LAYOUT = {"spawn": None, "layers": []}

def cavern_shape(L, xs, zs):
    c, Fl, lid = L["cavern"], L["floor"], L["id"]
    rho = M.blob_rho(c, xs, zs) + (M.vn(xs, zs, 5, seed=20 + lid) - 0.5) * 0.08
    ang = np.arctan2((zs - c["z"]) / c["rz"], (xs - c["x"]) / c["rx"])
    ceil = Fl + 6 + (M.CAV_H - 6) * np.sqrt(np.clip(1 - rho ** 2, 0, 1)) + (M.vn(xs, zs, 7, seed=30 + lid) - 0.5) * 4
    ceil = np.minimum(np.round(ceil), Fl + M.CAV_H).astype(int)
    ramp = np.zeros(xs.shape, dtype=bool)
    for a0 in (0.785, 2.356, -2.356, -0.785):
        ramp |= np.abs(np.angle(np.exp(1j * (ang - a0)))) < 0.16
    if lid == 2:
        fl = np.select([rho < 0.45, rho < 0.75], [6, 3], 0)
        rampf = np.round(np.clip((0.82 - rho) / 0.42, 0, 1) * 6)
        fl = np.where(ramp, rampf, fl)
    elif lid == 3:
        fl = np.select([rho < 0.40, rho < 0.55, rho < 0.70, rho < 0.85], [4, 3, 2, 1], 0)
    else:
        fl = np.select([rho < 0.36, rho < 0.46, rho < 0.80], [2, -99, 0], 3)
        rampf = np.round(np.clip((rho - 0.78) / 0.14, 0, 1) * 3)
        fl = np.where(ramp & (rho >= 0.78), rampf, fl)
    return rho, ang, Fl + fl, np.maximum(ceil, Fl + fl + 5)

def carve_room(L, r):
    x1, z1, x2, z2, h = r[:5]
    Fl, th = L["floor"], THEME[L["id"]]
    if th["lining"]:
        for x in range(x1 - 1, x2 + 1):
            for z in range(z1 - 1, z2 + 1):
                for y in range(Fl, Fl + h + 2):
                    if not is_air(x, y, z):
                        put(x, y, z, pick(th["lining"]))
    for x in range(x1, x2):
        for z in range(z1, z2):
            put(x, Fl, z, pick(th["room_floor"][r[5]]))
            for y in range(Fl + 1, Fl + h + 1):
                put(x, y, z, "air")

def cor_cells(seg, radius=2.5):
    (ax, az), (bx, bz) = seg
    out = {}
    for x in range(min(ax, bx) - 4, max(ax, bx) + 5):
        for z in range(min(az, bz) - 4, max(az, bz) + 5):
            vx, vz = bx - ax, bz - az
            t = max(0, min(1, ((x - ax) * vx + (z - az) * vz) / (vx * vx + vz * vz)))
            d = math.hypot(x - ax - t * vx, z - az - t * vz)
            if d <= radius:
                out[(x, z)] = d
    return out

def carve_corridor(L, pts):
    Fl, lid, th = L["floor"], L["id"], THEME[L["id"]]
    cells = {}
    for i in range(1, len(pts)):
        for c, d in cor_cells((pts[i - 1], pts[i])).items():
            cells[c] = min(d, cells.get(c, 9))
    for (x, z), d in cells.items():
        if lid == 3:
            top = Fl + 4 + int(round(float(M.vn(x, z, 6, seed=41)) * 2))
        elif lid == 4:
            top = Fl + (6 if d < 1.5 else 5)
        else:
            top = Fl + 5
        if lid == 4:
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    for y in range(Fl, top + 2):
                        n_ = get(x + dx, y, z + dz)
                        if n_ not in ("minecraft:air", "minecraft:water", "minecraft:lava") and (x + dx, z + dz) not in cells:
                            put(x + dx, y, z + dz, pick(th["lining"]))
            for y in range(Fl, top + 2):
                if not is_air(x, y, z):
                    put(x, y, z, pick(th["lining"]))
        for y in range(Fl + 1, top + 1):
            put(x, y, z, "air")
        if get(x, Fl, z) != "minecraft:water":
            put(x, Fl, z, pick(th["floor"]))
    return cells

def frames_along(L, pts, every):
    Fl, th = L["floor"], THEME[L["id"]]
    n = 0
    for i in range(1, len(pts)):
        (ax, az), (bx, bz) = pts[i - 1], pts[i]
        l = math.hypot(bx - ax, bz - az)
        ux, uz = (bx - ax) / l, (bz - az) / l
        a = 3.0
        while a < l - 2:
            cx, cz = ax + ux * a, az + uz * a
            cells = frame_line(cx, cz, -uz, ux, 2.0)
            top = min((y for y in range(Fl + 1, Fl + 8) if not is_air(cells[len(cells) // 2][0], y, cells[len(cells) // 2][1])),
                      default=Fl + 5) - 1
            axis = "x" if abs(uz) > abs(ux) else "z"
            if L["id"] == 3 and rng.random() < 0.35:
                a += every
                continue
            for c in cells:
                if is_air(c[0], top, c[1]):
                    put(c[0], top, c[1], f"{th['beam']}[axis={axis}]")
            for c in (cells[0], cells[-1]):
                for y in range(Fl + 1, top):
                    if is_air(c[0], y, c[1]):
                        put(c[0], y, c[1], th["post"])
            if n % 2 == 0:
                c = cells[len(cells) // 2]
                if is_air(c[0], top - 1, c[1]):
                    put(c[0], top - 1, c[1], f"{th['lamp']}[hanging=true]")
            n += 1
            a += every

def hanging_lamps(L, x1, z1, x2, z2, ytop, every=6):
    th = THEME[L["id"]]
    for x in range(x1 + 2, x2 - 1, every):
        for z in range(z1 + 2, z2 - 1, every):
            if is_air(x, ytop, z) and not is_air(x, ytop + 1, z):
                put(x, ytop, z, f"{th['lamp']}[hanging=true]")

def bed(x, y, z, facing, color):
    dx, dz = STEP[facing]
    put(x, y, z, f"{color}_bed[facing={facing},part=foot]")
    put(x + dx, y, z + dz, f"{color}_bed[facing={facing},part=head]")

def furnish(L, r):
    x1, z1, x2, z2, h, typ, name = r[:7]
    Fl, lid, th = L["floor"], L["id"], THEME[L["id"]]
    y = Fl + 1
    cx, cz = (x1 + x2) // 2, (z1 + z2) // 2
    def wall_cells():
        for x in range(x1 + 1, x2 - 1):
            yield x, z1, "south"
            yield x, z2 - 1, "north"
        for z in range(z1 + 1, z2 - 1):
            yield x1, z, "east"
            yield x2 - 1, z, "west"
    # cột góc + xà trần
    for (px, pz) in ((x1, z1), (x1, z2 - 1), (x2 - 1, z1), (x2 - 1, z2 - 1)):
        for yy in range(y, Fl + h + 1):
            put(px, yy, pz, th["post"])
    long_x = (x2 - x1) >= (z2 - z1)
    if lid != 4:
        if long_x:
            for x in range(x1 + 4, x2 - 2, 5):
                for z in range(z1, z2):
                    put(x, Fl + h, z, f"{th['beam']}[axis=z]")
        else:
            for z in range(z1 + 4, z2 - 2, 5):
                for x in range(x1, x2):
                    put(x, Fl + h, z, f"{th['beam']}[axis=x]")
    hanging_lamps(L, x1, z1, x2, z2, Fl + h - 1 if lid != 4 else Fl + h)
    if typ == "rest":
        n = 0
        for x in range(x1 + 2, x2 - 2, 3):
            if n >= 4:
                break
            bed(x, y, z1 + 1, "south", "brown" if lid == 2 else ("green" if lid == 3 else "gray"))
            n += 1
        put(x2 - 3, y, z2 - 2, "crafting_table")
        put(x2 - 4, y, z2 - 2, "barrel[facing=up]")
        put(cx, y, z2 - 2, "campfire[lit=false]")
        for x in range(x1 + 2, x1 + 6):
            put(x, y, cz, f"{th['plank']}")
    elif typ == "store":
        for (x, z, f) in wall_cells():
            if rng.random() < 0.55:
                put(x, y, z, "barrel[facing=up]")
                if rng.random() < 0.4:
                    put(x, y + 1, z, "barrel[facing=up]")
        put(cx, y, cz, "crafting_table")
    elif typ == "reward":
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                put(cx + dx, Fl, cz + dz, "chiseled_stone_bricks" if lid != 4 else "chiseled_deepslate")
        for (x, z, f) in wall_cells():
            if rng.random() < 0.25:
                put(x, y, z, "barrel[facing=up]")
    elif typ == "combat":
        for (x, z, f) in wall_cells():
            if rng.random() < 0.12:
                put(x, y, z, "barrel[facing=up]" if lid != 4 else "polished_blackstone_bricks")
    # chi tiết riêng từng phòng
    if name == "Trạm Tời":
        for (px, pz) in ((cx - 2, cz - 2), (cx - 2, cz + 2), (cx + 2, cz - 2), (cx + 2, cz + 2)):
            for yy in range(y, Fl + h + 1):
                put(px, yy, pz, th["post"])
        for x in range(cx - 1, cx + 2):
            for z in (cz - 2, cz + 2):
                for yy in range(y, y + 3):
                    if rng.random() < 0.7:
                        put(x, yy, z, "iron_bars")
        put(cx, y, cz, "grindstone[face=floor]")
    elif name == "Xưởng Rèn Nguội":
        put(cx, y, cz, "chipped_anvil")
        put(cx - 2, y, cz, "blast_furnace[facing=east]")
        put(cx - 2, y + 1, cz, "furnace[facing=east]")
        put(cx + 2, y, cz, "smithing_table")
        put(cx, y, cz + 2, "water_cauldron[level=3]")
        put(cx + 1, y, cz + 2, "water_cauldron[level=3]")
    elif name == "Phòng Sàng Quặng":
        for x in range(x1 + 3, x2 - 3):
            put(x, y, cz, "spruce_planks")
            put(x, y + 1, cz, "hopper" if x % 3 == 0 else "composter")
    elif name == "Bãi Xe Goòng":
        for z in (cz - 4, cz, cz + 4):
            for x in range(x1 + 1, x2 - 1):
                if rng.random() < 0.85 and is_air(x, y, z):
                    put(x, y, z, "rail[shape=east_west]")
    elif name == "Trạm Bơm Nước":
        for x in range(x1, (x1 + x2) // 2):
            for z in range(z1, z2):
                put(x, Fl, z, "water")
        for z in range(z1 + 1, z2 - 1):
            put(cx, Fl + 4, z, "oxidized_cut_copper")
        for yy in range(y, Fl + 5):
            put(cx, yy, z1 + 1, "oxidized_cut_copper")
    elif name == "Hầm Sập":
        for x in range(x2 - 10, x2):
            for z in range(z1, z1 + 10):
                d = math.hypot(x - (x2 - 1), z - z1)
                top = int(round((10 - d) * 0.9))
                for yy in range(y, min(Fl + h, Fl + top)):
                    put(x, yy, z, pick("40%cobblestone,30%stone,20%gravel,10%mossy_cobblestone"))
    elif name == "Lò Luyện":
        for x in range(x1 + 1, x2 - 1):
            if abs(x - cx) > 1:
                for z in (cz, cz + 1):
                    put(x, Fl, z, "lava")
                    put(x, Fl - 1, z, "deepslate")
        for dz in (-2, -1, 0, 1, 2, 3):
            pass
        for yy in range(y, y + 4):
            for z in range(z1 + 1, z1 + 4):
                for x in range(cx - 2, cx + 3):
                    put(x, yy, z, "deepslate_bricks" if yy < y + 3 else "deepslate_tiles")
        put(cx, y, z1 + 4, "blast_furnace[facing=south]")
    elif name == "Trạm Gác Sâu":
        for x in range(x1 + 3, x2 - 3, 4):
            for yy in range(y, y + 2):
                put(x, yy, cz, "iron_bars")
                put(x + 1, yy, cz, "iron_bars")
        for x in range(x2 - 6, x2 - 1):
            for z in range(z1 + 1, z1 + 6):
                put(x, Fl + 3, z, "polished_blackstone_bricks")
        for yy in range(y, Fl + 3):
            put(x2 - 2, yy, z1 + 5, "polished_basalt[axis=y]")
            put(x2 - 6, yy, z1 + 5, "polished_basalt[axis=y]")
        for yy in range(y, Fl + 3):
            put(x2 - 4, yy, z1 + 6, f"ladder[facing=south]")
    elif name == "Giếng Thang Gãy":
        for (px, pz) in ((cx - 2, cz - 2), (cx - 2, cz + 2), (cx + 2, cz - 2), (cx + 2, cz + 2)):
            for yy in range(y, y + rng.randint(3, 7)):
                put(px, yy, pz, "iron_bars")
        heap_local = [(cx + dx, cz + dz) for dx in range(-1, 2) for dz in range(-1, 2)]
        for (x, z) in heap_local:
            put(x, y, z, pick("50%cobbled_deepslate,50%iron_bars"))
    elif name == "Cổng Niêm Phong":
        for x in range(cx - 3, cx + 4):
            for yy in range(y, y + 6):
                if abs(x - cx) == 3 or yy == y + 5:
                    put(x, yy, z2 - 1, "chiseled_deepslate" if yy == y + 5 else "polished_deepslate")
        for (x, z, f) in wall_cells():
            if rng.random() < 0.3 and z != z2 - 1:
                put(x, y, z, "barrel[facing=up]")

def cavern_build(L):
    c, Fl, lid, th = L["cavern"], L["floor"], L["id"], THEME[L["id"]]
    xs, zs = sub_grid(c["x"], c["z"], c["rx"] * 1.2, c["rz"] * 1.2)
    rho, ang, fl, ceil = cavern_shape(L, xs, zs)
    inside = rho <= 1.0
    conn = [tuple(p[-1]) for p in L["cors"]]
    if L.get("down"):
        conn.append((L["down"]["x"], L["down"]["z"]))
    solid_mask = np.zeros(xs.shape, dtype=bool)
    pillars = []
    if lid == 2:                                         # trụ đá giữ trần kiểu buồng-cột
        for i in range(-3, 4):
            for j in range(-2, 3):
                px = c["x"] + i * 15 + rng.uniform(-3, 3)
                pz = c["z"] + j * 13 + rng.uniform(-3, 3)
                pr = float(M.blob_rho(c, px, pz))
                if pr > 0.86 or min(math.hypot(px - a, pz - b) for a, b in conn) < 10:
                    continue
                if any(abs(float(np.angle(np.exp(1j * (math.atan2((pz - c['z']) / c['rz'], (px - c['x']) / c['rx']) - a0))))) < 0.25
                       for a0 in (0.785, 2.356, -2.356, -0.785)):
                    continue
                prad = rng.uniform(2.2, 3.4)
                pillars.append((px, pz, prad))
                solid_mask |= np.hypot(xs - px, zs - pz) <= prad
    elif lid == 3:                                       # cột thạch nhũ khổng lồ
        for _ in range(300):
            if len(pillars) >= 10:
                break
            px = c["x"] + rng.uniform(-c["rx"], c["rx"]) * 0.8
            pz = c["z"] + rng.uniform(-c["rz"], c["rz"]) * 0.8
            if float(M.blob_rho(c, px, pz)) > 0.8 or min(math.hypot(px - a, pz - b) for a, b in conn) < 12:
                continue
            if any(math.hypot(px - a, pz - b) < 11 for a, b, _ in pillars):
                continue
            pillars.append((px, pz, rng.uniform(2.5, 4.2)))
    elif lid == 4:                                       # cột bazan vòng ngoài
        for _ in range(300):
            if len(pillars) >= 12:
                break
            t = rng.uniform(-math.pi, math.pi)
            rr = rng.uniform(0.54, 0.74)
            px, pz = c["x"] + math.cos(t) * c["rx"] * rr, c["z"] + math.sin(t) * c["rz"] * rr
            if min(math.hypot(px - a, pz - b) for a, b in conn) < 9:
                continue
            if any(abs(np.angle(np.exp(1j * (t - a0)))) < 0.22 for a0 in (0, math.pi / 2, math.pi, -math.pi / 2)):
                continue
            if any(math.hypot(px - a, pz - b) < 9 for a, b, _ in pillars):
                continue
            prad = rng.uniform(1.5, 2.6)
            pillars.append((px, pz, prad))
            solid_mask |= np.hypot(xs - px, zs - pz) <= prad
    sel = inside & ~solid_mask
    if lid == 4:
        moat = sel & (fl < Fl - 50)
        sel_n = sel & ~moat
        carve_cols(xs[sel_n], zs[sel_n], fl[sel_n] + 1, ceil[sel_n])
        carve_cols(xs[moat], zs[moat], np.full(moat.sum(), Fl + 1), ceil[moat])
        carve_cols(xs[moat], zs[moat], np.full(moat.sum(), Fl - 1), np.full(moat.sum(), Fl), LAVA)
        for x, z, a in zip(xs[moat], zs[moat], ang[moat]):                       # 4 cầu đá
            ax_, az_ = int(x) - c["x"], int(z) - c["z"]
            if abs(ax_) <= 1 or abs(az_) <= 1:
                for y in range(Fl - 1, Fl + 2):
                    put(int(x), y, int(z), pick("60%polished_deepslate,40%deepslate_tiles"))
    else:
        carve_cols(xs[sel], zs[sel], fl[sel] + 1, ceil[sel])
    for x, z, f_, r_ in zip(xs[sel], zs[sel], fl[sel], rho[sel]):
        x, z, f_ = int(x), int(z), int(f_)
        if f_ < Fl - 50:
            continue
        if lid == 4 and r_ < 0.36:
            put(x, f_, z, pick("50%polished_deepslate,30%deepslate_tiles,20%cracked_deepslate_tiles"))
        else:
            put(x, f_, z, pick(th["floor"]))
    FLOOR = {(int(x), int(z)): int(f_) for x, z, f_ in zip(xs[sel], zs[sel], fl[sel])}
    CEIL = {(int(x), int(z)): int(cc) for x, z, cc in zip(xs[sel], zs[sel], ceil[sel])}
    # trang trí theo tầng
    if lid == 2:
        for k, (px, pz, prad) in enumerate(pillars[:: max(1, len(pillars) // 3)][:3]):
            base = min(FLOOR.get((int(round(px + prad + 1)), int(round(pz))), Fl + 6), Fl + 6)
            for lvl in (base + 5, base + 10):
                for x in range(int(px - prad - 3), int(px + prad + 4)):
                    for z in range(int(pz - prad - 3), int(pz + prad + 4)):
                        d = math.hypot(x - px, z - pz)
                        if prad < d <= prad + 2.0 and is_air(x, lvl, z) and is_air(x, lvl + 1, z):
                            put(x, lvl, z, "spruce_planks")
                            if d > prad + 1.4 and rng.random() < 0.5:
                                put(x, lvl + 1, z, "spruce_fence")
            lx = int(math.floor(px + prad)) + 1
            lz = int(round(pz))
            while not is_air(lx, base + 1, lz) and lx < px + prad + 3:
                lx += 1
            for y in range(base + 1, base + 12):
                if is_air(lx, y, lz) or get(lx, y, lz).endswith("planks"):
                    put(lx, y, lz, "ladder[facing=east]")
        wx, wz = 24, 72                                   # thác nước mảnh rơi từ khe trần
        top = CEIL.get((wx, wz))
        if top:
            put(wx, top, wz, "water")
            for y in range(FLOOR[(wx, wz)] + 1, top):
                put(wx, y, wz, "water[level=8]")
            for x in range(wx - 2, wx + 3):
                for z in range(wz - 2, wz + 3):
                    if math.hypot(x - wx, z - wz) <= 2.3 and (x, z) in FLOOR:
                        put(x, FLOOR[(x, z)], z, "water")
        for (x, z), f_ in FLOOR.items():
            if rng.random() < 0.006 and is_air(x, f_ + 1, z) and is_air(x, f_ + 3, z):
                put(x, f_ + 1, z, "spruce_fence")
                put(x, f_ + 2, z, "lantern")
            elif rng.random() < 0.01 and is_air(x, f_ + 1, z):
                put(x, f_ + 1, z, "rail[shape=north_south]" if rng.random() < 0.5 else "rail[shape=east_west]")
    elif lid == 3:
        for (px, pz, prad) in pillars:
            fy = FLOOR.get((int(round(px)), int(round(pz))), Fl)
            cy = CEIL.get((int(round(px)), int(round(pz))), Fl + 20)
            mid, half = (fy + cy) / 2, max(1, (cy - fy) / 2)
            for x in range(int(px - prad - 2), int(px + prad + 3)):
                for z in range(int(pz - prad - 2), int(pz + prad + 3)):
                    for y in range(fy + 1, cy + 2):
                        rr_ = prad * (0.42 + 0.58 * ((y - mid) / half) ** 2)
                        if math.hypot(x - px, z - pz) <= rr_ and is_air(x, y, z):
                            put(x, y, z, "dripstone_block")
        for (x, z), f_ in FLOOR.items():
            ring = int(round(f_ - Fl))
            nb = [FLOOR.get((x + dx, z + dz)) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if all(v == f_ for v in nb) and M.vn(x, z, 4, seed=52) > 0.68:
                put(x, f_, z, "water")                      # hồ nông trên bậc đá vôi
            elif rng.random() < 0.03 and is_air(x, f_ + 1, z):
                n = rng.randint(1, 3)
                states = ["tip"] if n == 1 else (["frustum", "tip"] if n == 2 else ["base", "frustum", "tip"])
                for i, s_ in enumerate(states):
                    put(x, f_ + 1 + i, z, f"pointed_dripstone[vertical_direction=up,thickness={s_}]")
        for (x, z), cc in CEIL.items():
            if not is_air(x, cc, z):
                continue
            r_ = rng.random()
            if r_ < 0.05:
                n = rng.randint(1, 3)
                states = ["tip"] if n == 1 else (["frustum", "tip"] if n == 2 else ["base", "frustum", "tip"])
                for i, s_ in enumerate(states):
                    if is_air(x, cc - i, z):
                        put(x, cc - i, z, f"pointed_dripstone[vertical_direction=down,thickness={s_}]")
            elif r_ < 0.15:
                put(x, cc, z, "glow_lichen[up=true]")
            elif r_ < 0.18:
                n = rng.randint(1, 4)
                for i in range(n):
                    if is_air(x, cc - i, z):
                        put(x, cc - i, z, "cave_vines[berries=true]" if i == n - 1 else "cave_vines_plant[berries=true]")
    elif lid == 4:
        for (px, pz, prad) in pillars:
            for x in range(int(px - prad - 1), int(px + prad + 2)):
                for z in range(int(pz - prad - 1), int(pz + prad + 2)):
                    if math.hypot(x - px, z - pz) <= prad:
                        for y in range(Fl - 1, Fl + M.CAV_H + 1):
                            if is_air(x, y, z):
                                put(x, y, z, "basalt[axis=y]")
        for (x, z), f_ in FLOOR.items():
            if f_ < Fl - 50:
                continue
            r_ = float(M.blob_rho(c, x, z))
            if 0.46 < r_ < 0.8 and rng.random() < 0.05:
                put(x, f_, z, "magma_block")
        for k in range(12):                              # đèn soul quanh đài
            t = k * math.pi / 6 + math.pi / 12
            x = int(round(c["x"] + math.cos(t) * c["rx"] * 0.33))
            z = int(round(c["z"] + math.sin(t) * c["rz"] * 0.33))
            f_ = FLOOR.get((x, z))
            if f_ is not None:
                put(x, f_ + 1, z, "polished_blackstone_wall")
                put(x, f_ + 2, z, "soul_lantern")
    return dict(floor=FLOOR, ceil=CEIL, pillars=pillars)

def well_connector(top_y, bot_room):
    """giếng vuông 9x9, thang gỗ chữ chi bám vách: từ sàn hang tầng 2 xuống phòng Chân Thang tầng 3"""
    d = M.LAYERS[0]["down"]
    cx, cz = d["x"], d["z"]
    room_top = F[3] + bot_room[4]
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            for y in range(room_top + 1, top_y + 1):
                put(x, y, z, "air")
    spiral(cx, cz, lambda x, z: 3 <= max(abs(x - cx), abs(z - cz)) <= 4, top_y, F[3], 0.0,
           (top_y - F[3] - 1) / 14 * 360, 1, "spruce_planks", "spruce_stairs", support_below=room_top, support_floor=F[3] + 1)
    for (px, pz) in ((cx - 5, cz - 5), (cx - 5, cz + 5), (cx + 5, cz - 5), (cx + 5, cz + 5)):
        for y in range(top_y + 1, top_y + 6):
            put(px, y, pz, "spruce_log[axis=y]")
    for x in range(cx - 5, cx + 6):
        put(x, top_y + 6, cz - 5, "stripped_spruce_log[axis=x]")
        put(x, top_y + 6, cz + 5, "stripped_spruce_log[axis=x]")
    put(cx, top_y + 5, cz - 5, "lantern[hanging=true]")
    put(cx, top_y + 5, cz + 5, "lantern[hanging=true]")
    return dict(x=cx, z=cz, top=top_y, bottom=F[3])

def sinkhole_connector(top_y, room):
    """hố sụt r7 + gờ đá xoắn + thác nước trong khe vách, đổ xuống hồ phòng Hồ Rơi tầng 4"""
    d = M.LAYERS[1]["down"]
    cx, cz = d["x"], d["z"]
    room_top = F[4] + room[4]
    for x in range(cx - 8, cx + 9):
        for z in range(cz - 8, cz + 9):
            if math.hypot(x - cx, z - cz) <= 7.0:
                for y in range(room_top + 1, top_y + 1):
                    put(x, y, z, "air")
    for x in range(cx - 10, cx + 11):                    # hồ: lòng phòng r<=9.5 sâu 4
        for z in range(cz - 10, cz + 11):
            if math.hypot(x - cx, z - cz) <= 9.5:
                for y in range(F[4] - 3, F[4] + 1):
                    put(x, y, z, "water")
                put(x, F[4] - 4, z, "cobbled_deepslate")
    sweep = (top_y - F[4] - 1) / 18 * 360
    spiral(cx, cz, lambda x, z: 4.6 < math.hypot(x - cx, z - cz) <= 7.0, top_y, F[4], 200.0, sweep, -1,
           "60%tuff,40%cobbled_deepslate", "cobbled_deepslate_stairs", support_below=room_top, support_floor=F[4] - 3)
    wt = math.radians(20.0)                              # khe thác ở hướng ~20°
    sx_, sz_ = int(round(cx + math.cos(wt) * 7.6)), int(round(cz + math.sin(wt) * 7.6))
    put(sx_, top_y - 1, sz_, "water")
    for y in range(F[4] + 1, top_y - 1):
        put(sx_, y, sz_, "water[level=8]")
    return dict(x=cx, z=cz, top=top_y, bottom=F[4], waterfall=[sx_, sz_])

def into_cavern(L, pts, extra=7):
    """hành lang kết thúc sát hang: kéo dài thêm `extra` khối theo hướng cuối để chắc chắn thông vào hang"""
    (ax, az), (bx, bz) = pts[-2], pts[-1]
    if float(M.blob_rho(L["cavern"], bx, bz)) > 1.25:
        return pts
    l = math.hypot(bx - ax, bz - az)
    return pts + [(int(round(bx + (bx - ax) / l * extra)), int(round(bz + (bz - az) / l * extra)))]

layer_info = {}
for L in M.LAYERS:
    lid = L["id"]
    th = THEME[lid]
    cav = cavern_build(L)
    for r in L["rooms"]:
        carve_room(L, r)
    corr = {}
    cors = [into_cavern(L, pts) for pts in L["cors"]]
    for pts in cors:
        corr.update(carve_corridor(L, pts))
    for r in L["rooms"]:
        furnish(L, r)
    for pts in cors:
        if lid in (2, 3):
            frames_along(L, pts, 6 if lid == 2 else 7)
        else:
            for i in range(1, len(pts)):
                (ax, az), (bx, bz) = pts[i - 1], pts[i]
                l = math.hypot(bx - ax, bz - az)
                for a in np.arange(4, l - 2, 8):
                    x, z = int(round(ax + (bx - ax) * a / l)), int(round(az + (bz - az) * a / l))
                    for y in range(F[4] + 6, F[4] + 3, -1):
                        if is_air(x, y, z) and not is_air(x, y + 1, z):
                            put(x, y, z, "soul_lantern[hanging=true]")
                            break
    for (x, z), d in corr.items():
        Fl = L["floor"]
        if lid == 2:
            if d > 1.6 and rng.random() < 0.05:
                for y in range(Fl + 5, Fl + 2, -1):
                    if is_air(x, y, z) and not is_air(x, y + 1, z):
                        put(x, y, z, "cobweb")
                        break
        elif lid == 3:
            if rng.random() < 0.06 and get(x, Fl, z) != "minecraft:water":
                put(x, Fl, z, "water")
            for y in range(Fl + 6, Fl + 3, -1):
                if is_air(x, y, z) and not is_air(x, y + 1, z):
                    if rng.random() < 0.15:
                        put(x, y, z, "glow_lichen[up=true]")
                    break
        elif lid == 4:
            if rng.random() < 0.05:
                put(x, Fl, z, "magma_block")
    for pts in L["cors"]:                                # ray bỏ hoang trên đoạn thẳng trục
        if lid != 2:
            break
        for i in range(1, len(pts)):
            (ax, az), (bx, bz) = pts[i - 1], pts[i]
            if ax != bx and az != bz:
                continue
            shape = "north_south" if ax == bx else "east_west"
            n = max(abs(bx - ax), abs(bz - az))
            for t in range(n + 1):
                x = ax + (bx - ax) * t // max(n, 1)
                z = az + (bz - az) * t // max(n, 1)
                if rng.random() < 0.8 and is_air(x, F[2] + 1, z) and not is_air(x, F[2], z):
                    put(x, F[2] + 1, z, f"rail[shape={shape}]")
    for (dx_, dz_) in L["dead"]:                         # hầm sập cụt
        for x in range(dx_ - 3, dx_ + 4):
            for z in range(dz_ - 3, dz_ + 4):
                d = math.hypot(x - dx_, z - dz_)
                if d > 3.2:
                    continue
                for y in range(L["floor"] + 1, L["floor"] + 6):
                    if is_air(x, y, z) and (y - L["floor"]) <= 5 - d * 0.9:
                        put(x, y, z, pick("50%cobblestone,30%gravel,20%stone"))
    layer_info[lid] = dict(cavern=cav, corridors=corr)

ROOM = {r[6]: r for L in M.LAYERS for r in L["rooms"]}
well = well_connector(layer_info[2]["cavern"]["floor"][(M.LAYERS[0]["down"]["x"], M.LAYERS[0]["down"]["z"])],
                      ROOM["Chân Thang"])
sink = sinkhole_connector(layer_info[3]["cavern"]["floor"][(M.LAYERS[1]["down"]["x"], M.LAYERS[1]["down"]["z"])],
                          ROOM["Hồ Rơi"])
import boss_room
BOSS_INFO = boss_room.build(sys.modules[__name__])

# ================================================================== tấm bedrock ngăn tầng + vách bedrock đứng
def open_mask_ids():
    return np.array([is_open_name(n) for n in NAMES], dtype=bool)
OPEN = open_mask_ids()[W]
adj = OPEN.copy()
adj[1:] |= OPEN[:-1]; adj[:-1] |= OPEN[1:]
adj[:, 1:] |= OPEN[:, :-1]; adj[:, :-1] |= OPEN[:, 1:]
adj[:, :, 1:] |= OPEN[:, :, :-1]; adj[:, :, :-1] |= OPEN[:, :, 1:]
SKIRT = ISL & (RR >= EDGE - M.SKIRT_IN) & (RR <= EDGE - M.SKIRT_OUT)
INNER = ISL & (RR < EDGE - M.SKIRT_OUT)
cand = np.zeros(W.shape, dtype=bool)
for k, y in SEAL.items():
    cand[:, y - Y0, :] |= INNER
s1x, s1z = np.nonzero(INNER)
SN = SEAL1.copy()                       # tấm L1 bậc thang: nối mép bậc bằng màn đứng tới tấm cột bên cạnh
for ax_, sh in ((0, 1), (0, -1), (1, 1), (1, -1)):
    SN = np.minimum(SN, np.roll(SEAL1, sh, ax_))
for y in range(int(SN[INNER].min()), M.SEAL1_MAX + 1):
    cand[:, y - Y0, :] |= INNER & (SN <= y) & (y <= SEAL1)
for yi in range(SEAL[4] - Y0, NY):
    y = Y0 + yi
    cand[:, yi, :] |= SKIRT & (y <= SEAL1)
BS = M.BOSS                             # vỏ phòng boss: tấm đáy + vách trụ, nối lên tấm Y3
cand[:, BS["seal_floor"] - Y0, :] |= ISL & (RR <= BS["seal_r"][1])
for y in range(BS["seal_floor"], SEAL[4]):
    cand[:, y - Y0, :] |= ISL & (RR >= BS["seal_r"][0]) & (RR <= BS["seal_r"][1])
holes = cand & (adj | OPEN)
place = cand & ~holes
W[place] = BED
hole_xyz = np.argwhere(holes)

def clusters(points, gap=2):
    """gom các ô lỗ theo (x, z) kề nhau trong khoảng `gap`"""
    by_xz = {}
    for p in points:
        by_xz.setdefault((p[0], p[2]), []).append(p)
    left = set(by_xz)
    out = []
    while left:
        s_ = left.pop()
        comp, dq_ = [s_], deque([s_])
        while dq_:
            px_, pz_ = dq_.popleft()
            for ddx in range(-gap, gap + 1):
                for ddz in range(-gap, gap + 1):
                    q = (px_ + ddx, pz_ + ddz)
                    if q in left:
                        left.discard(q)
                        comp.append(q)
                        dq_.append(q)
        out.append([p for c in comp for p in by_xz[c]])
    return out

# ================================================================== kiểm tra
report = []
def rep(s):
    report.append(s)
    print(s)
NAMES_ARR = np.array(NAMES + [""] * (len(PAL) - len(NAMES)))
OPEN = open_mask_ids()[W]
bed_mask = W == BED
exp = np.zeros(W.shape, dtype=bool)
for ax_ in range(3):
    for sh in (1, -1):
        exp |= bed_mask & np.roll(OPEN, sh, axis=ax_)
rep(f"Hộp {NX}x{NY}x{NZ}, gốc ({X0},{Y0},{Z0}), palette {len(PAL)}, mép đảo r {float(EDGE.min()):.1f}..{float(EDGE.max()):.1f}")
rep(f"Đỉnh núi cao nhất Y{int(HT[ISL].max())}, đáy đảo thấp nhất Y{int(BOT[ISL].min())}")
rep(f"Bedrock: {int(bed_mask.sum())} khối, lộ ra ô trống: {int(exp.sum())}")
by_plate = {}
for x, y, z in hole_xyz:
    wx, wy, wz = x + X0, y + Y0, z + Z0
    key = "vách đứng" if SKIRT[x, z] else ("tấm L1" if wy == SEAL1[x, z] else f"tấm Y{wy}")
    by_plate.setdefault(key, []).append((wx, wy, wz))
for k, v in sorted(by_plate.items()):
    for comp in sorted(clusters(v), key=len, reverse=True):
        xs_ = [p[0] for p in comp]; zs_ = [p[2] for p in comp]
        rep(f"  lỗ {k}: {len(comp)} ô, x {min(xs_)}..{max(xs_)} z {min(zs_)}..{max(zs_)}")
cover = min(int(HT[x - X0, z - Z0]) - TUN_TOP[(x, z)] for (x, z) in TUN_TOP if M.nearest_on_polyline(x, z, M.TUNNEL)[0] > 0.06)
rep(f"Đá phủ mỏng nhất trên hang dẫn (bỏ 6% đầu ở cửa): {cover} khối")

# đi bộ có trọng lực từ điểm xuất hiện
nm = NAMES
PASS = np.array([is_open_name(n) and "lava" not in n and "structure_void" not in n for n in nm], dtype=bool)[W]
WAT = np.array(["water" in n for n in nm], dtype=bool)[W]
LAD = np.array(["ladder" in n for n in nm], dtype=bool)[W]
NOSTAND = np.array([any(k in n for k in ("fence", "_wall", "lava", "structure_void")) for n in nm], dtype=bool)[W]
SUP = ~PASS & ~NOSTAND
stand = np.zeros(W.shape, dtype=bool)
stand[:, 1:-1, :] = PASS[:, 1:-1, :] & PASS[:, 2:, :] & (SUP[:, :-2, :] | WAT[:, 1:-1, :] | WAT[:, :-2, :] | LAD[:, 1:-1, :])
SX, SY, SZ = W.shape
def walk(pos):
    start = (pos[0] - X0, pos[1] - Y0, pos[2] - Z0)
    assert stand[start], f"điểm {pos} không đứng được"
    seen = np.zeros(W.shape, dtype=bool)
    seen[start] = True
    dq = deque([start])
    while dq:
        x, y, z = dq.popleft()
        nbrs = []
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            tx, tz = x + dx, z + dz
            if not (0 <= tx < SX and 0 <= tz < SZ):
                continue
            if y + 2 < SY and stand[tx, y + 1, tz] and PASS[x, y + 2, z]:
                nbrs.append((tx, y + 1, tz))
            if stand[tx, y, tz]:
                nbrs.append((tx, y, tz))
            if PASS[tx, y, tz] and PASS[tx, y + 1, tz]:
                for dy in (1, 2, 3):
                    ty = y - dy
                    if ty < 1:
                        break
                    if stand[tx, ty, tz]:
                        nbrs.append((tx, ty, tz))
                        break
                    if not PASS[tx, ty, tz]:
                        break
        if (WAT[x, y, z] or LAD[x, y, z]) and y + 2 < SY and stand[x, y + 1, z]:
            nbrs.append((x, y + 1, z))
        if y > 1 and stand[x, y - 1, z] and (WAT[x, y - 1, z] or LAD[x, y - 1, z] or WAT[x, y, z]):
            nbrs.append((x, y - 1, z))
        for n in nbrs:
            if not seen[n]:
                seen[n] = True
                dq.append(n)
    return seen
seen = walk(SPAWN)
seen_boss = walk(BOSS_INFO['spawn'])
def reached_box(x1, x2, z1, z2, y1, y2, sn=None):
    sn = seen if sn is None else sn
    return bool(sn[x1 - X0:x2 - X0, y1 - Y0:y2 - Y0 + 1, z1 - Z0:z2 - Z0].any())
rep(f"Ô đứng được đã đi tới: {int(seen.sum())}")
fails = []
checks = [("Bãi mỏ", (-10, 10, 50, 60, 121, 121)), ("Cuối hang dẫn", (-14, -8, -50, -44, 105, 112)),
          ("Đáy hố", (-3, 3, -65, -59, 73, 74))]
for L in M.LAYERS:
    for r in L["rooms"]:
        checks.append((f"T{L['id']} {r[6]}", (r[0], r[2], r[1], r[3], L["floor"] + 1, L["floor"] + r[4])))
    c = L["cavern"]
    checks.append((f"T{L['id']} {c['name']} (tâm)", (c["x"] - 4, c["x"] + 4, c["z"] - 4, c["z"] + 4, L["floor"] - 1, L["floor"] + 12)))
FB = M.BOSS["floor"]
PL = M.BOSS["plinth"]
bchecks = [("Boss: sảnh bán nguyệt", (-6, 6, -26, -16, FB + 1, FB + 2)),
           ("Boss: bậc 2 cạnh bàn triệu hồi", (-6, 7, -38, -33, PL, PL + 1)),
           ("Boss: gờ chân cổng", (-10, 11, -43, -42, PL + 1, PL + 2)),
           ("Boss: cuối đường chéo tây", (-46, -40, 8, 17, FB, FB + 2)),
           ("Boss: hố mộ trong điện mộ", (27, 32, 17, 24, FB - 2, FB - 2)),
           ("Boss: tầng 2 nhà H1", (-24, -13, 5, 12, FB + 6, FB + 6)),
           ("Boss: tầng trệt tháp T1", (-45, -38, -21, -14, FB + 2, FB + 2)),
           ("Boss: mái tháp T1", (-47, -36, -23, -12, FB + 19, FB + 19))]
checks += [(n_, b_, True) for n_, b_ in bchecks]
checks = [c if len(c) == 3 else (c[0], c[1], False) for c in checks]
for name, (x1, x2, z1, z2, y1, y2), is_boss in checks:
    ok_ = reached_box(x1, x2, z1, z2, y1, y2, seen_boss if is_boss else None)
    rep(f"  {'OK ' if ok_ else 'CHƯA'} {name}")
    if not ok_:
        fails.append(name)
rep(f"TẤT CẢ KHU THÔNG: {not fails}")

# ================================================================== dữ liệu cho plugin
def room_json(L, r):
    x1, z1, x2, z2, h, typ, name, note = r
    return dict(name=name, type=typ, note=note, min=[x1, L["floor"] + 1, z1], max=[x2 - 1, L["floor"] + h, z2 - 1],
                center=[(x1 + x2) // 2, L["floor"] + 1, (z1 + z2) // 2])
layout = dict(
    world_note="tọa độ thật; floor = Y khối sàn, người đứng floor+1",
    spawn=dict(pos=list(SPAWN), yaw=180),
    yard=dict(y=M.YARD_Y, mouth=list(M.TUNNEL[0])),
    tunnel=dict(points=[list(p) for p in M.TUNNEL], floor_from=M.YARD_Y, floor_to=M.TUNNEL_END_Y),
    pit=dict(center=[P["x"], P["z"]], r=P["r"], top=M.TUNNEL_END_Y, bottom=F[2]),
    seals=dict(l1=f"min({M.SEAL1_MAX}, mặt đất - 3)", **{f"under_l{k}": v for k, v in SEAL.items()}),
    layers=[],
    connectors=dict(well=well, sinkhole=sink),
    boss=BOSS_INFO,
)
for L in M.LAYERS:
    c = L["cavern"]
    layout["layers"].append(dict(id=L["id"], name=L["name"], floor=L["floor"],
                                 rooms=[room_json(L, r) for r in L["rooms"]],
                                 cavern=dict(name=c["name"], center=[c["x"], L["floor"] + 1, c["z"]], rx=c["rx"], rz=c["rz"],
                                             top=L["floor"] + M.CAV_H)))

if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    slices = [("l5", Y0, SEAL[4] - 1), ("l4", SEAL[4], SEAL[3] - 1), ("l3", SEAL[3], SEAL[2] - 1), ("l2", SEAL[2], M.SEAL1_MAX), ("l1", M.SEAL1_MAX + 1, Y1)]
    for key, ya, yb in slices:
        sub = W[:, ya - Y0:yb - Y0 + 1, :]
        write_schem(OUT / f"{PREFIX}_{key}.schem", sub, PAL, (X0, ya, Z0))
        rep(f"Ghi {PREFIX}_{key}.schem  Y{ya}..{yb}  ({sub.shape[0]}x{sub.shape[1]}x{sub.shape[2]})")
    with open(OUT / "layout.json", "w", encoding="utf-8", newline="\n") as f:
        json.dump(layout, f, ensure_ascii=False, indent=2)
    with open(OUT / "report.txt", "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(report) + "\n")
    import render_maps
    render_maps.render(W, NAMES, dict(X0=X0, Y0=Y0, Z0=Z0, HT=HT, ISL=ISL, layers=M.LAYERS, seen=seen, SPAWN=SPAWN,
                                          seen_boss=seen_boss))
