#!/usr/bin/env python3
"""
Sylphens — Layer 2 thủ tục (procedural, có seed) -> file .schem (Sponge v2)

  python3 scripts/generate_layer2.py            # seed mặc định 2026
  python3 scripts/generate_layer2.py 777        # seed khác -> bố cục khác

Đầu ra: output/sylphens_layer2.schem, output/layer2_rooms.json, output/layer2_report.txt
Paste trong game (đứng đâu cũng được):
  //schem load sylphens_layer2
  //paste -o -m !structure_void
"""
import sys, json, math, heapq, gzip, struct, os, random, zlib
from collections import defaultdict, deque
import numpy as np

SEED = int(sys.argv[1]) if len(sys.argv) > 1 and sys.argv[1].lstrip("-").isdigit() else 2026
from pathlib import Path
OUT = str(Path(__file__).resolve().parents[1] / "output")
SCHEM_NAME = "sylphens_layer2"

# ------------------------------------------------------------------ hằng số
F = {1: 46, 2: 30, 3: 18, 4: -2, 5: -18}      # sàn (khối đứng) của 5 tầng; T3 là tầng mê cung nên sát T2 hơn
MAZE_FL = 3                                     # tầng mê cung (cả tầng là 1 mê cung lớn bọc bedrock)
RMAX = {1: 84, 2: 84, 3: 82, 4: 78, 5: 66}      # bán kính dùng được mỗi tầng (đáy đảo thu nhỏ dần)
HMAX = {1: 10, 2: 12, 3: 12, 4: 12, 5: 12}      # trần phòng cao nhất (T1 bị giới hạn bởi Layer 1)
CH = 4                                          # hành lang cao 4
X0, X1 = -88, 88
Y0, Y1 = -21, 60
NX, NY, NZ = X1 - X0 + 1, Y1 - Y0 + 1, X1 - X0 + 1
CELL = 4                                        # lưới thô cho hành lang (3 khí + 1 tường)
NC = (X1 - X0 + 1) // CELL                      # 44

rng = random.Random(SEED)
nrng = np.random.default_rng(SEED)

# ------------------------------------------------------------------ bảng khối
PAL = {}
def bid(name):
    if name not in PAL:
        PAL[name] = len(PAL)
    return PAL[name]
VOID = bid("minecraft:structure_void")
DEEP = bid("minecraft:deepslate")
AIR = bid("minecraft:air")
BEDROCK = bid("minecraft:bedrock")

W = np.full((NX, NY, NZ), DEEP, dtype=np.uint16)

def I(x, y, z):
    return x - X0, y - Y0, z - X0

def pat(spec):
    """'60%a,40%b' -> (ids, probs)"""
    ids, ws = [], []
    for part in spec.split(","):
        w, n = part.split("%")
        ids.append(bid("minecraft:" + n)); ws.append(float(w))
    ws = np.array(ws); return np.array(ids, dtype=np.uint16), ws / ws.sum()

def fill(mask, spec):
    """mask: bool 3D cùng shape W"""
    n = int(mask.sum())
    if n == 0: return
    if "%" not in spec:
        W[mask] = bid("minecraft:" + spec); return
    ids, p = pat(spec)
    W[mask] = nrng.choice(ids, size=n, p=p)

def setb(x, y, z, name):
    i, j, k = I(x, y, z)
    if 0 <= i < NX and 0 <= j < NY and 0 <= k < NZ:
        W[i, j, k] = bid("minecraft:" + name)

def getb(x, y, z):
    i, j, k = I(x, y, z); return W[i, j, k]

XS = np.arange(X0, X1 + 1)
GX, GZ = np.meshgrid(XS, XS, indexing="ij")        # 2D tọa độ thật
GR = np.sqrt(GX ** 2 + GZ ** 2)

def col3(mask2d, ya, yb):
    """2D mask -> 3D mask cho y trong [ya, yb]"""
    m = np.zeros((NX, NY, NZ), dtype=bool)
    ja, jb = max(0, ya - Y0), min(NY - 1, yb - Y0)
    if ja <= jb:
        m[:, ja:jb + 1, :] = mask2d[:, None, :]
    return m

def dilate(m, n=1):
    for _ in range(n):
        o = m.copy()
        o[1:, :] |= m[:-1, :]; o[:-1, :] |= m[1:, :]
        o[:, 1:] |= m[:, :-1]; o[:, :-1] |= m[:, 1:]
        m = o
    return m

def smooth_noise(scale, seed):
    """noise 2D mượt đơn giản (value noise nội suy) trong [-1,1]"""
    r = np.random.default_rng(seed)
    n = int(NX / scale) + 3
    g = r.uniform(-1, 1, (n, n))
    fx = (GX - X0) / scale; fz = (GZ - X0) / scale
    ix, iz = fx.astype(int), fz.astype(int)
    tx, tz = fx - ix, fz - iz
    tx = tx * tx * (3 - 2 * tx); tz = tz * tz * (3 - 2 * tz)
    a = g[ix, iz]; b = g[ix + 1, iz]; c = g[ix, iz + 1]; d = g[ix + 1, iz + 1]
    return (a * (1 - tx) + b * tx) * (1 - tz) + (c * (1 - tx) + d * tx) * tz

# ------------------------------------------------------------------ vật liệu
BR = "45%stone_bricks,30%mossy_stone_bricks,25%cracked_stone_bricks"
DBR = "50%deepslate_bricks,25%cracked_deepslate_bricks,25%deepslate_tiles"
MAT = {
    "hall": "40%stone_bricks,30%mossy_stone_bricks,30%polished_andesite",
    "guardian": "45%deepslate_tiles,30%mossy_cobblestone,25%cracked_deepslate_tiles",
    "combat": BR, "arena": "50%polished_andesite,30%stone_bricks,20%andesite",
    "barracks": DBR, "chest": "70%stone_bricks,30%chiseled_stone_bricks",
    "trap": "60%cracked_stone_bricks,40%stone_bricks",
    "checkpoint": "70%polished_deepslate,30%deepslate_tiles",
    "teleport": "60%polished_blackstone_bricks,30%polished_blackstone,10%crying_obsidian",
    "stair": DBR, "atrium": "50%stone_bricks,30%polished_andesite,20%mossy_stone_bricks",
    "arrival": DBR, "cavern": "60%tuff,25%deepslate,15%dripstone_block",
    "corr": BR, "maze": "60%mossy_cobblestone,40%cobblestone", "cave": "70%tuff,30%deepslate",
}
RES = {  # tên: (vỏ, độ dày vỏ, cao)
    "Mỏ Quặng Sâu": ("55%stone,10%deepslate_coal_ore,9%deepslate_iron_ore,8%deepslate_copper_ore,6%deepslate_gold_ore,6%deepslate_redstone_ore,4%deepslate_lapis_ore,2%deepslate_diamond_ore", 3, 12),
    "Hồ Ngầm": ("50%prismarine,25%clay,25%mud", 2, 9),
    "Vườn Rễ Cổ": ("50%rooted_dirt,30%mud_bricks,20%dirt", 2, 10),
    "Thư Khố": ("60%bookshelf,25%dark_oak_planks,15%deepslate_lapis_ore", 2, 10),
    "Vườn Nấm": ("60%mycelium,40%dirt", 2, 11),
    "Hang Pha Lê": ("50%amethyst_block,30%calcite,20%smooth_basalt", 2, 12),
    "Mỏ Cổ": ("50%stone,12%deepslate_iron_ore,12%deepslate_gold_ore,10%deepslate_redstone_ore,8%deepslate_emerald_ore,8%deepslate_diamond_ore", 3, 10),
}
RES_BY_FLOOR = {1: ["Vườn Rễ Cổ", "Thư Khố"], 2: ["Mỏ Quặng Sâu", "Hồ Ngầm"], 3: [],
                4: ["Hang Pha Lê", "Vườn Nấm"], 5: ["Mỏ Cổ"]}

# ------------------------------------------------------------------ mê cung (tầng MAZE_FL)
# Lưới ô bước MZ_P: lõi MZ_CORE khối (đường đi rộng 3-5 nằm trong lõi) + tường 3 khối (đá | bedrock | đá).
# Mặt cắt đứng (F = sàn): F-4..F-1 bedrock (đáy, F-4 cũng là tấm ngăn tầng), F sàn đá, đường F+1..F+5,
# phòng F+1..F+6, trên cùng tới F+9 là bedrock. Mọi mặt bedrock lộ ra khí được phủ 1 lớp MZ_MAT.
MZ_P, MZ_CORE = 8, 5
MZ_H, MZ_RH = 5, 6                  # cao đường đi / phòng trong mê cung
MZ_BOT, MZ_TOP = -4, 9              # khối bedrock của tầng: F+MZ_BOT .. F+MZ_TOP
MZ_MAT = "45%cobblestone,30%mossy_cobblestone,25%stone"
MZ_WIDTH = ([3, 4, 5], [3, 4, 3])   # độ rộng đường và trọng số
MZ_ROOMS = 7                        # phòng combat nhỏ trong mê cung
MZ_TRAPS = {"pit": 5, "arrow": 5, "web": 4}
MZ_CHESTS = 8                       # rương ở ngõ cụt
MZ_LOOP = 0.06                      # tỉ lệ cạnh thêm để có vòng lặp

# ------------------------------------------------------------------ ánh sáng, vòm cửa
TORCH = "wall_torch"
TORCH_GAP = {"corr": 7, "maze": 9}               # khoảng cách tối thiểu giữa 2 đuốc
CEIL_LIT = ("hall", "guardian", "res", "cavern", "atrium")   # phòng lớn giữ đèn trần, còn lại dùng đuốc
ARCH = {  # kind -> (khối bậc thang vòm, khối khung 2 bên, đá đỉnh vòm)
    "default": ("stone_brick_stairs", "polished_andesite", "chiseled_stone_bricks"),
    "deep": ("deepslate_brick_stairs", "polished_deepslate", "chiseled_deepslate"),
    "teleport": ("polished_blackstone_brick_stairs", "polished_blackstone", "chiseled_polished_blackstone"),
    "Vườn Rễ Cổ": ("mud_brick_stairs", "mud_bricks", "packed_mud"),
    "Thư Khố": ("dark_oak_stairs", "stripped_dark_oak_log", "dark_oak_planks"),
}
ARCH_KIND = {"barracks": "deep", "stair": "deep", "arrival": "deep", "guardian": "deep", "checkpoint": "deep",
             "teleport": "teleport"}
RIB_EVERY = 2                       # vòm sườn trong hành lang thẳng: mỗi 2 khe ô (= 8 khối)

# ------------------------------------------------------------------ phòng
ROOMS = []          # mọi phòng (mọi tầng)
class Room:
    def __init__(s, kind, name, fl, cx, cz, w, l, H, shape="rect", base=None, span=None, mat=None, pad=1):
        s.kind, s.name, s.fl = kind, name, fl
        s.cx, s.cz, s.w, s.l, s.H, s.shape = cx, cz, w, l, H, shape
        s.F = F[fl] if base is None else base       # sàn
        s.span = span                                # atrium: tầng dưới
        s.mat = mat or MAT.get(kind, BR)
        s.pad = pad
        s.x1, s.x2 = cx - w // 2, cx - w // 2 + w - 1
        s.z1, s.z2 = cz - l // 2, cz - l // 2 + l - 1
        s.doors = []
        s.res = None
    def mask2d(s):
        if s.shape == "round":
            R = s.w / 2
            return (GX - s.cx) ** 2 + (GZ - s.cz) ** 2 <= R * R
        if s.shape == "blob":
            nz = smooth_noise(6, hash((s.cx, s.cz)) & 0xffff)
            dx = (GX - s.cx) / (s.w / 2); dz = (GZ - s.cz) / (s.l / 2)
            return dx * dx + dz * dz <= 0.75 + 0.35 * nz
        return (GX >= s.x1) & (GX <= s.x2) & (GZ >= s.z1) & (GZ <= s.z2)
    def top(s):
        return s.F + s.H
    def foot(s, margin):
        return dilate(s.mask2d(), margin)

# chiếm chỗ 2D theo tầng
OCC = {k: np.zeros((NX, NZ), dtype=bool) for k in F}

def free_for(room, fls, margin):
    m = room.foot(margin)
    if (GR[m] > RMAX[min(fls)] - 2).any():
        return False
    for k in fls:
        if (OCC[k] & m).any(): return False
    return True

def claim(room, fls, margin=2):
    m = room.foot(margin)
    for k in fls: OCC[k] |= m

def try_place(kind, name, fl, w, l, H, shape="rect", rmin=0, rmax=None, ang=None, angspread=math.pi,
              fls=None, margin=5, tries=600, score=None, samples=60, **kw):
    """đặt ngẫu nhiên không đè; nếu có score: lấy mẫu nhiều vị trí hợp lệ, chọn điểm cao nhất"""
    fls = fls or [fl]
    rmax = rmax or RMAX[fl]
    best, bs, found = None, -1e9, 0
    for _ in range(tries * (3 if score else 1)):
        a = (ang if ang is not None else 0) + rng.uniform(-angspread, angspread)
        r = rng.uniform(rmin, rmax)
        cx, cz = int(round(r * math.cos(a))), int(round(r * math.sin(a)))
        rm = Room(kind, name, fl, cx, cz, w, l, H, shape, **kw)
        if free_for(rm, fls, margin):
            if not score:
                best = rm; break
            sc = score(rm) + rng.uniform(0, 4)
            if sc > bs: best, bs = rm, sc
            found += 1
            if found >= samples: break
    if best:
        claim(best, fls, 3); ROOMS.append(best)
    return best

def mind(rm, pts):
    return min((math.hypot(rm.cx - p[0], rm.cz - p[1]) for p in pts), default=0)

def angle_of(x, z): return math.atan2(z, x)

# ------------------------------------------------------------------ hành lang (lưới thô)
def cell_xy(i, j): return X0 + CELL * i, X0 + CELL * j       # góc tây-bắc của ô
def cell_of(x, z): return (x - X0) // CELL, (z - X0) // CELL

class FloorNet:
    def __init__(s, fl):
        s.fl = fl
        s.blocked = np.zeros((NC, NC), dtype=bool)
        s.cells = set()           # ô có hành lang
        s.edges = set()           # cạnh nối 2 ô
        s.style = {}              # ô -> 'corr' | 'maze' | 'cave'
        s.maze = set()
        s.fake = []               # (x,z,axis) tường giả
        s.dead = []               # ô cuối ngõ cụt
        noise = np.random.default_rng(SEED + fl).uniform(0, 1, (NC, NC))
        s.cost = 1 + 2.5 * noise
        for i in range(NC):
            for j in range(NC):
                x, z = cell_xy(i, j)
                if math.hypot(x + 1, z + 1) > RMAX[fl] - 3:
                    s.blocked[i, j] = True
    def block_room(s, room):
        m = room.foot(2)
        for i in range(NC):
            for j in range(NC):
                x, z = cell_xy(i, j)
                if m[x - X0:x - X0 + 3, z - X0:z - X0 + 3].any():
                    s.blocked[i, j] = True
    def ok(s, c):
        i, j = c
        return 0 <= i < NC and 0 <= j < NC and not s.blocked[i, j] and c not in s.maze
    def add_edge(s, a, b, style="corr"):
        s.cells.add(a); s.cells.add(b)
        s.edges.add((min(a, b), max(a, b)))
        for c in (a, b): s.style.setdefault(c, style)
    def astar(s, starts, goals, avoid=()):
        goals = set(goals)
        pq = [(0, c) for c in starts]
        g = {c: 0 for c in starts}; prev = {}
        heapq.heapify(pq)
        gx = [c[0] for c in goals]; gz = [c[1] for c in goals]
        tx, tz = sum(gx) / len(gx), sum(gz) / len(gz)
        while pq:
            f, c = heapq.heappop(pq)
            if c in goals:
                path = [c]
                while c in prev: c = prev[c]; path.append(c)
                return path[::-1]
            for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (c[0] + d[0], c[1] + d[1])
                if not s.ok(n) or n in avoid: continue
                step = 0.35 if n in s.cells else s.cost[n]
                ng = g[c] + step
                if ng < g.get(n, 1e9):
                    g[n] = ng; prev[n] = c
                    heapq.heappush(pq, (ng + 0.5 * (abs(n[0] - tx) + abs(n[1] - tz)), n))
        return None

def door_cells(net, room):
    """các ô tự do nằm sát 1 cạnh phòng -> (ô, cạnh)"""
    out = []
    m = room.mask2d()
    for i in range(NC):
        for j in range(NC):
            if not net.ok((i, j)): continue
            x, z = cell_xy(i, j)
            cx, cz = x + 1, z + 1
            # 4 hướng: tia từ tâm ô vào phòng, khoảng cách tới phần trong <= 6
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                for t in range(2, 8):
                    px, pz = cx + dx * t, cz + dz * t
                    if not (X0 <= px <= X1 and X0 <= pz <= X1): break
                    # cả dải rộng 3 phải trúng phần trong
                    hit = all(m[px + (dz != 0) * o - X0, pz + (dx != 0) * o - X0] for o in (-1, 0, 1))
                    if hit:
                        out.append(((i, j), (dx, dz), t)); break
                    if any(OCC_ROOMS_AT(px, pz, room)): break
    return out

ROOM_AT = {}
def OCC_ROOMS_AT(px, pz, room):
    return []

BANDS = []     # (fl, x1,x2,z1,z2, sàn, dx,dz, phòng) dải nối cửa vào phòng

def connect_door(net, room, cand):
    (i, j), (dx, dz), t = cand
    x, z = cell_xy(i, j)
    cx, cz = x + 1, z + 1
    xa, xb = sorted((cx, cx + dx * (t + 1)))
    za, zb = sorted((cz, cz + dz * (t + 1)))
    if dx == 0: xa, xb = cx - 1, cx + 1
    else: za, zb = cz - 1, cz + 1
    BANDS.append((net.fl, xa, xb, za, zb, F[net.fl], dx, dz, room))
    room.doors.append(((i, j), (dx, dz)))
    net.cells.add((i, j)); net.style.setdefault((i, j), "corr")
    return (i, j)

def pick_door(net, room, toward):
    cands = door_cells(net, room)
    if not cands: return None
    tx, tz = toward
    # ưu tiên cửa sẵn có gần hướng đích, giới hạn số cửa
    existing = [c for c in cands if c[0] in [d[0] for d in room.doors]]
    pool = existing if (existing and len(room.doors) >= room.maxdoors) else cands
    best = min(pool, key=lambda c: math.hypot(cell_xy(*c[0])[0] - tx, cell_xy(*c[0])[1] - tz) + rng.uniform(0, 6))
    if best[0] not in [d[0] for d in room.doors]:
        connect_door(net, room, best)
    return best[0]

def link_rooms(net, a, b, style="corr"):
    da = pick_door(net, a, (b.cx, b.cz))
    db = pick_door(net, b, (a.cx, a.cz))
    if da is None or db is None: return False
    path = net.astar([da], [db])
    if not path: return False
    st = style
    for p, q in zip(path, path[1:]):
        net.add_edge(p, q, st)
    return True

# ------------------------------------------------------------------ mê cung: bố cục trên lưới ô
MGX0 = -84                                   # gốc lưới mê cung (dùng chung cho x và z)
MNC = (X1 - MGX0) // MZ_P + 1
def mz_core(i): return MGX0 + i * MZ_P       # khối đầu tiên của lõi ô i
def mz_mid(i): return mz_core(i) + MZ_CORE // 2

class MazeFloor:
    def __init__(s, fl):
        s.fl = fl
        s.cells = set()      # ô thuộc mê cung
        s.node = {}          # ô -> id phòng (ô không thuộc phòng thì nút là chính ô đó)
        s.rects = {}         # id phòng -> (i0, i1, j0, j1)
        s.kind = {}          # id phòng -> 'arrival' | 'stair' | 'combat'
        s.edges = []         # (ô a, ô b, độ rộng)
        s.traps = []         # (loại, ô a, ô b, độ rộng) -> dựng ở build_maze
        s.trap_pos = []      # (loại, x, y, z) cho plugin
        s.chests = []        # ô cụt có rương
        s.rooms = []         # Room combat trong mê cung
        s.failed = []

def mz_valid(i, j, fl):
    if not (0 <= i < MNC and 0 <= j < MNC): return False
    x0, z0 = mz_core(i), mz_core(j)
    return all(math.hypot(x, z) <= RMAX[fl] - 1 for x in (x0, x0 + MZ_CORE - 1) for z in (z0, z0 + MZ_CORE - 1))

def mz_span(a, b):
    """dải ô [i0, i1] có lõi phủ trọn đoạn khối [a, b]"""
    i0 = (a - MGX0) // MZ_P
    i1 = i0
    while mz_core(i1) + MZ_CORE - 1 < b: i1 += 1
    return i0, i1

def mz_rect(i0, i1, j0, j1):
    """hộp khối (x1, x2, z1, z2) của phòng phủ các ô i0..i1 × j0..j1"""
    return mz_core(i0), mz_core(i1) + MZ_CORE - 1, mz_core(j0), mz_core(j1) + MZ_CORE - 1

def mz_strip(a, b, w):
    """hộp khối của dải đường rộng w nối tâm ô a với tâm ô b (nằm trọn trong lõi -> tường luôn dày 3)"""
    lo, hi = (w - 1) // 2, w // 2
    xa, za, xb, zb = mz_mid(a[0]), mz_mid(a[1]), mz_mid(b[0]), mz_mid(b[1])
    return min(xa, xb) - lo, max(xa, xb) + hi, min(za, zb) - lo, max(za, zb) + hi

def mz_gap(a, b, w):
    """hộp khối của đoạn đường đi xuyên tường (3 khối) giữa 2 ô kề nhau"""
    x1, x2, z1, z2 = mz_strip(a, b, w)
    if a[1] == b[1]:
        g = mz_core(min(a[0], b[0])) + MZ_CORE
        return g, g + MZ_P - MZ_CORE - 1, z1, z2
    g = mz_core(min(a[1], b[1])) + MZ_CORE
    return x1, x2, g, g + MZ_P - MZ_CORE - 1

def plan_maze(fl, entries):
    mz = MazeFloor(fl)
    cells = {(i, j) for i in range(MNC) for j in range(MNC) if mz_valid(i, j, fl)}

    def add_room(rid, kind, i0, i1, j0, j1):
        mz.rects[rid] = (i0, i1, j0, j1); mz.kind[rid] = kind
        for i in range(i0, i1 + 1):
            for j in range(j0, j1 + 1):
                cells.add((i, j)); mz.node[(i, j)] = rid

    def free(i0, i1, j0, j1, gap):
        if any((i, j) in mz.node for i in range(i0 - gap, i1 + gap + 1) for j in range(j0 - gap, j1 + gap + 1)):
            return False
        return all((i, j) in cells for i in range(i0, i1 + 1) for j in range(j0, j1 + 1))

    # 1) chân thang từ tầng trên: phòng chữ nhật trùm hộp 9x9
    for e in entries:
        i0, i1 = mz_span(e.x1, e.x2); j0, j1 = mz_span(e.z1, e.z2)
        add_room(e.name, "arrival", i0, i1, j0, j1)
    ent = [(e.cx, e.cz) for e in entries]

    # 2) 2 thang xuống tầng dưới: khối 2x2 ô (13x13), xa lối vào và xa nhau
    if fl < 5:
        downs = []
        for tag in ("A", "B"):
            best, bs = None, -1e9
            for _ in range(500):
                i0, j0 = rng.randrange(MNC - 1), rng.randrange(MNC - 1)
                if not free(i0, i0 + 1, j0, j0 + 1, 2): continue
                x1, x2, z1, z2 = mz_rect(i0, i0 + 1, j0, j0 + 1)
                rm = Room("stair", f"Thang Xoắn {tag} ↓T{fl+1}", fl, (x1 + x2) // 2, (z1 + z2) // 2, 9, 9, 6)
                if math.hypot(rm.cx, rm.cz) < 20 or not free_for(rm, [fl + 1], 3): continue
                sc = min(mind(rm, ent), mind(rm, downs) if downs else 1e9) + rng.uniform(0, 4)
                if sc > bs: best, bs = (rm, i0, j0), sc
            if not best:
                mz.failed.append(f"thang {tag}"); continue
            rm, i0, j0 = best
            claim(rm, [fl, fl + 1], 3); ROOMS.append(rm); STAIRS.append(rm)
            add_room(rm.name, "stair", i0, i0 + 1, j0, j0 + 1)
            downs.append((rm.cx, rm.cz))
            arr = Room("arrival", f"Chân Thang {tag} (từ T{fl})", fl + 1, rm.cx, rm.cz, 9, 9, 6)
            ROOMS.append(arr); arrivals[fl + 1].append(arr)

    # 3) phòng combat nhỏ rải trong mê cung
    for _ in range(MZ_ROOMS):
        for _ in range(300):
            si, sj = rng.choice([(2, 2), (2, 2), (2, 3), (3, 2)])
            i0, j0 = rng.randrange(MNC - si + 1), rng.randrange(MNC - sj + 1)
            if not free(i0, i0 + si - 1, j0, j0 + sj - 1, 1): continue
            x1, x2, z1, z2 = mz_rect(i0, i0 + si - 1, j0, j0 + sj - 1)
            w, l = x2 - x1 + 1, z2 - z1 + 1
            rm = Room("combat", nm("Ổ Canh Mê Cung"), fl, x1 + w // 2, z1 + l // 2, w, l, MZ_RH, mat=MZ_MAT)
            rm.in_maze = True
            ROOMS.append(rm); mz.rooms.append(rm)
            add_room(rm.name, "combat", i0, i0 + si - 1, j0, j0 + sj - 1)
            break

    # 4) DFS trên đồ thị nút (ô thường + phòng), rồi thêm vài cạnh tạo vòng lặp
    def nid(c): return mz.node.get(c, c)
    adj = {}
    for c in sorted(cells):
        for d in ((1, 0), (0, 1)):
            n = (c[0] + d[0], c[1] + d[1])
            if n in cells and nid(c) != nid(n):
                adj.setdefault(nid(c), {}).setdefault(nid(n), []).append((c, n))
                adj.setdefault(nid(n), {}).setdefault(nid(c), []).append((c, n))
    start = entries[0].name if entries else next(iter(adj))
    seen = {start}; stack = [start]; used = set()
    while stack:
        cur = stack[-1]
        nb = [n for n in adj.get(cur, {}) if n not in seen]
        if not nb: stack.pop(); continue
        n = rng.choice(nb); seen.add(n); stack.append(n)
        mz.edges.append(rng.choice(adj[cur][n]) + (rng.choices(*MZ_WIDTH)[0],))
        used.add(frozenset((str(cur), str(n))))
    mz.failed += [str(k) for k in adj if k not in seen]
    spare = [(u, v) for u in adj for v in adj[u] if str(u) < str(v) and frozenset((str(u), str(v))) not in used]
    for k in rng.sample(range(len(spare)), int(len(mz.edges) * MZ_LOOP)):
        u, v = spare[k]
        mz.edges.append(rng.choice(adj[u][v]) + (rng.choices(*MZ_WIDTH)[0],))
    mz.cells = cells

    # 5) bẫy trên các đoạn xuyên tường giữa 2 ô thường; hố chông cần đường rộng >= 4 để chừa gờ
    plain = [e for e in mz.edges if e[0] not in mz.node and e[1] not in mz.node]
    rng.shuffle(plain)
    want = dict(MZ_TRAPS)
    for e in plain:
        for t in ("pit", "arrow", "web"):
            if want[t] > 0 and (t != "pit" or e[2] >= 4):
                mz.traps.append((t,) + e); want[t] -= 1; break
        if not any(want.values()): break

    # 6) rương ở ô cụt (bậc 1), ưu tiên ô xa lối vào
    deg = defaultdict(int)
    for a, b, _ in mz.edges:
        deg[a] += 1; deg[b] += 1
    trapped = {c for t in mz.traps for c in t[1:3]}
    dead = [c for c in cells if c not in mz.node and deg[c] == 1 and c not in trapped]
    dead.sort(key=lambda c: -min((math.hypot(mz_mid(c[0]) - p[0], mz_mid(c[1]) - p[1]) for p in ent), default=0))
    mz.chests = dead[:MZ_CHESTS]
    return mz

# ------------------------------------------------------------------ dựng từng tầng
FLOORNETS = {}     # tầng thường: mạng hành lang
MAZES = {}         # tầng mê cung
STAIRS = []
ATRIA = []
names_count = defaultdict(int)
def nm(base):
    names_count[base] += 1
    return f"{base} {names_count[base]}"

arrivals = {k: [] for k in F}

def room_door_limit(r):
    r.maxdoors = {"chest": 1, "teleport": 1, "stair": 1, "dead": 1, "hall": 4, "guardian": 2,
                  "atrium": 3, "arrival": 2}.get(r.kind, 3)

for fl in range(1, 6):
    if fl == MAZE_FL:
        MAZES[fl] = plan_maze(fl, arrivals[fl])
        continue
    floor_rooms = []
    # --- phòng cố định
    if fl == 1:
        hall = Room("hall", "Sảnh Chính", 1, 0, 0, 31, 31, 10, "round")
        ROOMS.append(hall); claim(hall, [1], 2); floor_rooms.append(hall)
        entries = [hall]
    else:
        entries = list(arrivals[fl])
        floor_rooms += entries
    if fl == 5:
        g = Room("guardian", "Thủ Vệ Rễ", 5, 0, 0, 29, 29, 12, "round")
        ROOMS.append(g); claim(g, [5], 2); floor_rooms.append(g)
    entry_pts = [(e.cx, e.cz) for e in entries]
    net = FloorNet(fl); FLOORNETS[fl] = net

    # --- 2 thang xuống: xa lối vào và xa nhau, không giấu
    if fl < 5:
        rlo = 45 if fl == 4 else 20
        downs = []
        for tag in ("A", "B"):
            s_ = try_place("stair", f"Thang Xoắn {tag} ↓T{fl+1}", fl, 9, 9, 6, rmin=rlo, fls=[fl, fl + 1], margin=4,
                           score=lambda rm: min(mind(rm, entry_pts), mind(rm, downs) if downs else 1e9))
            if s_ is None: continue
            downs.append((s_.cx, s_.cz)); STAIRS.append(s_); floor_rooms.append(s_)
            arr = Room("arrival", f"Chân Thang {tag} (từ T{fl})", fl + 1, s_.cx, s_.cz, 9, 9, 6)
            ROOMS.append(arr); arrivals[fl + 1].append(arr)
        # giếng trời không được nối vào/ra tầng mê cung (sẽ đi tắt qua mê cung)
        if MAZE_FL not in (fl, fl + 1) and rng.random() < 0.85:
            at = try_place("atrium", "Giếng Trời", fl, rng.choice([19, 21, 23]), 0, 9, "round",
                           rmin=25, rmax=RMAX[fl + 1] - 14, fls=[fl, fl + 1], margin=5)
            if at:
                at.l = at.w; at.z1, at.z2 = at.cz - at.w // 2, at.cz - at.w // 2 + at.w - 1
                at.F = F[fl + 1]; at.span = fl + 1; at.H = F[fl] - F[fl + 1] + 9
                ATRIA.append(at); floor_rooms.append(at)
                bot = Room("atrium_bot", "Đáy Giếng Trời", fl + 1, at.cx, at.cz, at.w, at.w, 6, "round")
                bot.parent = at; arrivals[fl + 1].append(bot)

    # --- phòng tài nguyên (to, cao)
    for rname in RES_BY_FLOOR[fl]:
        mat, pad, H = RES[rname]
        H = min(H, HMAX[fl])
        w, l = rng.randint(20, 27), rng.randint(16, 22)
        if rng.random() < 0.5: w, l = l, w
        rr = try_place("res", rname, fl, w, l, H, rmin=20, margin=6, mat=mat, pad=pad)
        if rr: rr.res = rname; floor_rooms.append(rr)
    # --- checkpoint, dịch chuyển thoát
    if fl >= 2:
        cp = try_place("checkpoint", "Trạm Nghỉ", fl, 11, 11, 6, rmin=20, margin=5)
        if cp: floor_rooms.append(cp)
    if fl in (2, 4):
        tp = try_place("teleport", "Trạm Dịch Chuyển", fl, 9, 9, 7, rmin=40, margin=5)
        if tp: floor_rooms.append(tp)
    # --- hang tự nhiên
    for _ in range(rng.randint(1, 2)):
        cv = try_place("cavern", "Hang Đá", fl, rng.randint(16, 24), rng.randint(14, 20),
                       min(HMAX[fl], rng.choice([9, 11, 12])), "blob", rmin=15, margin=5)
        if cv: floor_rooms.append(cv)
    # --- phòng thử thách (lặp mẫu), rương, bẫy
    want = {1: 13, 2: 12, 3: 0, 4: 11, 5: 8}[fl]
    templates = [("combat", "Phòng Canh", (11, 15), (11, 15), [6, 7, 8], "rect"),
                 ("arena", "Đấu Trường", (13, 17), None, [8, 10, 12], "round"),
                 ("barracks", "Hầm Lính", (9, 13), (14, 19), [5, 6], "rect")]
    for _ in range(want):
        kind, base, wr, lr, Hs, shape = rng.choices(templates, weights=[5, 3, 3])[0]
        w = rng.randint(*wr); l = w if lr is None else rng.randint(*lr)
        if rng.random() < 0.5 and shape == "rect": w, l = l, w
        H = min(rng.choice(Hs), HMAX[fl])
        r_ = try_place(kind, nm(base), fl, w, l, H, shape, rmin=14, margin=5)
        if r_: floor_rooms.append(r_)
    for _ in range(rng.randint(3, 4)):
        c_ = try_place("chest", nm("Hầm Rương"), fl, rng.randint(5, 7), rng.randint(5, 7), 5, rmin=14, margin=4)
        if c_: floor_rooms.append(c_)
    for _ in range(2):
        w, l = 5, rng.randint(15, 21)
        if rng.random() < 0.5: w, l = l, w
        t_ = try_place("trap", nm("Hành Lang Bẫy"), fl, w, l, 5, rmin=18, margin=5)
        if t_: floor_rooms.append(t_)

    for r_ in floor_rooms: room_door_limit(r_)
    # --- chặn ô hành lang bởi mọi phòng có mặt ở tầng này
    for r_ in ROOMS:
        on = (r_.fl == fl) or (r_.span == fl) or (r_ in arrivals[fl])
        if r_.kind == "stair" and r_.fl + 1 == fl: on = True
        if on: net.block_room(r_)

    # --- mạng nối: MST + cạnh thêm (mọi phòng kể cả thang và Thủ Vệ)
    nodes = list(floor_rooms)
    def dist(a, b): return math.hypot(a.cx - b.cx, a.cz - b.cz)
    inT = {0}; edges = []
    while len(inT) < len(nodes):
        best = min(((dist(nodes[i], nodes[j]), i, j) for i in inT for j in range(len(nodes)) if j not in inT))
        edges.append((best[1], best[2])); inT.add(best[2])
    for i, a in enumerate(nodes):
        if rng.random() < 0.35:
            near = sorted(range(len(nodes)), key=lambda j: dist(a, nodes[j]))[2:4]
            if near: edges.append((i, rng.choice(near)))
    rng.shuffle(edges)
    failed = []
    par = list(range(len(nodes)))
    def fnd(a):
        while par[a] != a: par[a] = par[par[a]]; a = par[a]
        return a
    for i, j in edges:
        st = "cave" if rng.random() < 0.25 else "corr"
        if link_rooms(net, nodes[i], nodes[j], st): par[fnd(i)] = fnd(j)
    # vá: nối các cụm còn rời bằng cặp gần nhất nối được
    for _ in range(len(nodes)):
        comps = {fnd(i) for i in range(len(nodes))}
        if len(comps) == 1: break
        main = fnd(0)
        pairs = sorted(((dist(nodes[i], nodes[j]), i, j) for i in range(len(nodes)) for j in range(len(nodes))
                        if fnd(i) != main and fnd(j) == main))
        done = False
        for _, i, j in pairs[:60]:
            nodes[i].maxdoors = max(nodes[i].maxdoors, len(nodes[i].doors) + 1)
            if link_rooms(net, nodes[i], nodes[j], "corr"):
                par[fnd(i)] = fnd(j); done = True; break
        if not done:
            failed += [nodes[i].name for i in range(len(nodes)) if fnd(i) != main]; break
    # --- Sảnh Chính đủ 4 cửa 4 hướng
    if fl == 1:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if any(d[1] == (dx, dz) for d in hall.doors): continue
            cand = sorted([r_ for r_ in nodes if r_ is not hall],
                          key=lambda r_: math.hypot(r_.cx + dx * 60, r_.cz + dz * 60))
            for tgt in cand[:5]:
                dcs = [c for c in door_cells(net, hall) if c[1] == (dx, dz)]
                if not dcs: break
                c0 = min(dcs, key=lambda c: abs(cell_xy(*c[0])[0] + 1) + abs(cell_xy(*c[0])[1] + 1))
                d0 = connect_door(net, hall, c0)
                db = pick_door(net, tgt, (0, 0))
                path = net.astar([d0], [db]) if db else None
                if path:
                    for p, q in zip(path, path[1:]): net.add_edge(p, q, "corr")
                    break
    # --- ngõ cụt rẽ nhánh
    for _ in range({1: 14, 2: 16, 3: 0, 4: 14, 5: 8}[fl]):
        base = list(net.cells)
        if not base: break
        c = rng.choice(base); L = rng.randint(3, 9); prev = c; ok_ = 0
        for _ in range(L):
            nbs = [(prev[0] + d[0], prev[1] + d[1]) for d in ((1,0),(-1,0),(0,1),(0,-1))]
            nbs = [n for n in nbs if net.ok(n) and n not in net.cells]
            if not nbs: break
            n = rng.choice(nbs); net.add_edge(prev, n, rng.choice(["corr", "cave"])); prev = n; ok_ += 1
        if ok_ >= 3: net.dead.append(prev)
    net.failed = failed
    net.rooms = floor_rooms

# ------------------------------------------------------------------ dựng khối
def room_air3d(r):
    m2 = r.mask2d()
    return col3(m2, r.F + 1, r.F + r.H)

def room_shell3d(r):
    m2 = dilate(r.mask2d(), r.pad)
    return col3(m2, r.F - (r.pad - 1), r.F + r.H + r.pad)

def corr_masks(net):
    """2D mask khí cho từng kiểu hành lang"""
    ms = {"corr": np.zeros((NX, NZ), bool), "maze": np.zeros((NX, NZ), bool), "cave": np.zeros((NX, NZ), bool)}
    for c in net.cells:
        x, z = cell_xy(*c)
        ms[net.style.get(c, "corr")][x - X0:x - X0 + 3, z - X0:z - X0 + 3] = True
    for a, b in net.edges:
        sa, sb = net.style.get(a, "corr"), net.style.get(b, "corr")
        st = sa if sa == sb else "corr"
        xa, za = cell_xy(*a); xb, zb = cell_xy(*b)
        x1, x2 = min(xa, xb), max(xa, xb) + 2
        z1, z2 = min(za, zb), max(za, zb) + 2
        ms[st][x1 - X0:x2 - X0 + 1, z1 - X0:z2 - X0 + 1] = True
    return ms

cavenoise = smooth_noise(5, SEED + 99)

# 1) vỏ
for r in ROOMS:
    if r.kind == "atrium_bot" or r.fl == MAZE_FL: continue
    fill(room_shell3d(r), r.mat)
for fl, net in FLOORNETS.items():
    ms = corr_masks(net)
    for st, m in ms.items():
        if st == "cave": continue           # hang tự nhiên: vách deepslate/tuff thô
        fill(col3(dilate(m, 1), F[fl], F[fl] + CH + 1), MAT[st])
    m = dilate(ms["cave"], 2)
    fill(col3(m, F[fl] - 1, F[fl] + CH + 2) & (W == DEEP), "70%tuff,30%deepslate")
    for (fl_, xa, xb, za, zb, base, *_) in BANDS:
        if fl_ != fl: continue
        b2 = (GX >= xa) & (GX <= xb) & (GZ >= za) & (GZ <= zb)
        fill(col3(dilate(b2, 1), base, base + CH + 1) & (W == DEEP), BR)

# 2) khí
for r in ROOMS:
    if r.kind == "atrium_bot" or r.fl == MAZE_FL: continue
    W[room_air3d(r)] = AIR
for fl, net in FLOORNETS.items():
    ms = corr_masks(net)
    for st, m in ms.items():
        if st == "cave":
            # trần gợn sóng, vách loang
            mm = dilate(m, 1) & (cavenoise > 0.1) | m
            W[col3(m, F[fl] + 1, F[fl] + CH)] = AIR
            W[col3(mm, F[fl] + 1, F[fl] + CH - 1)] = AIR
            W[col3(m & (cavenoise > -0.2), F[fl] + CH + 1, F[fl] + CH + 1)] = AIR
        else:
            W[col3(m, F[fl] + 1, F[fl] + CH)] = AIR
    for (fl_, xa, xb, za, zb, base, *_) in BANDS:
        if fl_ != fl: continue
        b2 = (GX >= xa) & (GX <= xb) & (GZ >= za) & (GZ <= zb)
        W[col3(b2, base + 1, base + CH)] = AIR

# 2b) tầng mê cung: khối bedrock đặc, khoét đường + phòng, bẫy, rương
SPIKE = "pointed_dripstone[vertical_direction=up]"
def build_maze(mz):
    f = F[mz.fl]
    W[col3(GR <= 89, f + MZ_BOT, f + MZ_TOP)] = BEDROCK
    def box(x1, x2, z1, z2): return (GX >= x1) & (GX <= x2) & (GZ >= z1) & (GZ <= z2)
    for a, b, w in mz.edges:
        W[col3(box(*mz_strip(a, b, w)), f + 1, f + MZ_H)] = AIR
    for (i0, i1, j0, j1) in mz.rects.values():
        W[col3(box(*mz_rect(i0, i1, j0, j1)), f + 1, f + MZ_RH)] = AIR
    for t, a, b, w in mz.traps:
        x1, x2, z1, z2 = mz_gap(a, b, w)
        along_x = a[1] == b[1]
        mx, mz_ = (x1 + x2) // 2, (z1 + z2) // 2
        if t == "pit":
            # hố chông sâu 2 phủ cả đoạn xuyên tường, chừa gờ rộng 1 ở một bên để đi qua
            side = rng.choice((0, 1))
            if along_x: z1, z2 = (z1 + 1, z2) if side == 0 else (z1, z2 - 1)
            else: x1, x2 = (x1 + 1, x2) if side == 0 else (x1, x2 - 1)
            m = box(x1, x2, z1, z2)
            W[col3(m, f - 1, f)] = AIR
            W[col3(m, f - 2, f - 2)] = bid("minecraft:" + SPIKE)
            W[col3(m, f - 3, f - 3)] = bid("minecraft:dripstone_block")
        elif t == "arrow":
            # tấm áp suất giữa đường + 2 dispenser trong 2 vách bắn ngang (plugin nạp tên / xử lý kích hoạt)
            setb(mx, f + 1, mz_, "stone_pressure_plate")
            if along_x:
                setb(mx, f + 2, z1 - 1, "dispenser[facing=south]"); setb(mx, f + 2, z2 + 1, "dispenser[facing=north]")
            else:
                setb(x1 - 1, f + 2, mz_, "dispenser[facing=east]"); setb(x2 + 1, f + 2, mz_, "dispenser[facing=west]")
        elif t == "web":
            m = box(x1, x2, z1, z2) & (nrng.random((NX, NZ)) < 0.55)
            W[col3(m, f + 1, f + 2)] = bid("minecraft:cobweb")
        mz.trap_pos.append((t, mx, f + 1, mz_))
    for c in mz.chests:
        setb(mz_mid(c[0]), f + 1, mz_mid(c[1]), "chest[facing=%s]" % rng.choice(["north", "south", "east", "west"]))

def face_maze(mz):
    """mọi mặt bedrock của khối mê cung lộ ra ô đi qua được -> phủ 1 lớp đá MZ_MAT"""
    f = F[mz.fl]
    ya, yb = f + MZ_BOT - Y0, f + MZ_TOP - Y0
    P = np.isin(W[:, ya - 1:yb + 2, :], PASS_IDS())
    core = P[:, 1:-1, :]
    adj = P[:, :-2, :] | P[:, 2:, :]
    adj[1:] |= core[:-1]; adj[:-1] |= core[1:]
    adj[:, :, 1:] |= core[:, :, :-1]; adj[:, :, :-1] |= core[:, :, 1:]
    m = np.zeros(W.shape, bool)
    m[:, ya:yb + 1, :] = (W[:, ya:yb + 1, :] == BEDROCK) & adj
    fill(m, MZ_MAT)

def PASS_IDS():
    """khối người chơi đi xuyên qua được / không che mặt khối bên cạnh"""
    keys = ("air", "water", "lily_pad", "moss_carpet", "lantern", "chain", "pointed_dripstone", "amethyst_cluster",
            "amethyst_bud", "chest", "barrel", "torch", "cobweb", "pressure_plate")
    return [i for n, i in PAL.items() if any(k in n for k in keys) and "block" not in n and "leaves" not in n]

for mz in MAZES.values():
    build_maze(mz)

# 3) thang xoắn ốc (khoét từ sàn tầng trên xuống tầng dưới)
STEP = bid("minecraft:deepslate_bricks")
PIL = bid("minecraft:chiseled_deepslate")
for s in STAIRS:
    top, bot = F[s.fl], F[s.fl + 1]
    th0 = rng.uniform(0, 2 * math.pi)
    s.theta0 = th0
    for x in range(s.cx - 4, s.cx + 5):
        for z in range(s.cz - 4, s.cz + 5):
            dx, dz = x - s.cx, z - s.cz
            rr = math.hypot(dx, dz)
            i, _, k = I(x, 0, z)
            if rr < 1.3:
                W[i, bot - Y0:top + 6 - Y0, k] = PIL; continue
            if rr > 3.8: continue
            sfrac = ((math.atan2(dz, dx) - th0) % (2 * math.pi)) / (2 * math.pi)
            W[i, bot + 1 - Y0:top + 6 - Y0, k] = AIR
            h = math.floor(top - sfrac * 6)
            while h > bot:
                W[i, h - Y0, k] = STEP
                h -= 6
    # đèn
    setb(s.cx, top + 5, s.cx and s.cz, "shroomlight") if False else None

# 4) atrium: ban công + hồ đáy + xích đèn
for a in ATRIA:
    m2 = a.mask2d()
    inner = (GX - a.cx) ** 2 + (GZ - a.cz) ** 2 <= (a.w / 2 - 3.5) ** 2
    ledge = m2 & ~inner
    W[col3(ledge, F[a.fl], F[a.fl])] = bid("minecraft:polished_deepslate")
    ang = np.arctan2(GZ - a.cz, GX - a.cx)
    rail = ledge & ~dilate(~m2 | ~ledge, 0) & dilate(inner, 1) & (np.abs(np.sin(2 * ang)) > 0.3)
    W[col3(rail, F[a.fl] + 1, F[a.fl] + 1)] = bid("minecraft:deepslate_tile_wall")
    pool = (GX - a.cx) ** 2 + (GZ - a.cz) ** 2 <= (a.w / 2 - 4) ** 2
    W[col3(pool, a.F - 2, a.F)] = bid("minecraft:water")
    for y in range(a.F + a.H - 7, a.F + a.H + 1):
        setb(a.cx, y, a.cz, "chain[axis=y]")
    setb(a.cx, a.F + a.H - 8, a.cz, "lantern[hanging=true]")

# 5) nội thất theo loại
def interior(r, shrink=0):
    m = r.mask2d()
    for _ in range(shrink):
        m = ~dilate(~m, 1)
    return m

CHEST_FACE = ["north", "south", "east", "west"]
for r in ROOMS:
    k = r.kind
    if k in ("maze", "atrium_bot"): continue
    m2 = r.mask2d()
    if k == "hall":
        W[col3((GX ** 2 + GZ ** 2) <= 30, F[1] - 2, F[1])] = bid("minecraft:water")
        for t in range(8):
            a = t * math.pi / 4 + math.pi / 8
            px, pz = round(10.5 * math.cos(a)), round(10.5 * math.sin(a))
            fill(col3((abs(GX - px) <= 1) & (abs(GZ - pz) <= 1), r.F + 1, r.top()), "chiseled_stone_bricks")
        W[col3(GX ** 2 + GZ ** 2 <= 12.5, r.top() + 1, 60)] = AIR     # ống từ Layer 1
    elif k == "guardian":
        for t in range(8):
            a = t * math.pi / 4
            px, pz = round(10 * math.cos(a)), round(10 * math.sin(a))
            fill(col3((GX - px) ** 2 + (GZ - pz) ** 2 <= 2.3, r.F + 1, r.top()), "45%deepslate_tiles,30%mossy_cobblestone,25%cracked_deepslate_tiles")
        fill(col3(GX ** 2 + GZ ** 2 <= 6, r.F, r.F), "crying_obsidian")
        setb(0, r.F, 0, "lodestone")
    elif k == "arena":
        R = r.w / 2
        for t in range(6):
            a = t * math.pi / 3
            px, pz = r.cx + round(R * 0.55 * math.cos(a)), r.cz + round(R * 0.55 * math.sin(a))
            fill(col3((GX - px) ** 2 + (GZ - pz) ** 2 <= 1.2, r.F + 1, r.top()), "polished_andesite")
        fill(col3(m2, r.F, r.F), "50%polished_andesite,50%stone_bricks")
    elif k == "combat":
        for sx in (-1, 1):
            for sz in (-1, 1):
                px, pz = r.cx + sx * (r.w // 4), r.cz + sz * (r.l // 4)
                fill(col3((abs(GX - px) <= 0) & (abs(GZ - pz) <= 0) | ((GX - px) ** 2 + (GZ - pz) ** 2 <= 0.6), r.F + 1, r.top()), "stone_bricks")
    elif k == "barracks":
        fill(col3(m2, r.F, r.F), "spruce_planks")
    elif k == "chest":
        setb(r.cx, r.F + 1, r.cz, "chest[facing=%s]" % rng.choice(CHEST_FACE))
        setb(r.x1, r.F + 1, r.z1, "barrel[facing=up]"); setb(r.x2, r.F + 1, r.z2, "barrel[facing=up]")
    elif k == "trap":
        long_x = r.w > r.l
        if long_x:
            pit = (GX >= r.cx - 1) & (GX <= r.cx) & (GZ >= r.z1) & (GZ <= r.z2)
        else:
            pit = (GZ >= r.cz - 1) & (GZ <= r.cz) & (GX >= r.x1) & (GX <= r.x2)
        # hố nông (không được ăn xuống trần tầng dưới): rơi 3 khối vào măng đá
        W[col3(pit, r.F - 1, r.F)] = AIR
        W[col3(pit, r.F - 3, r.F - 3)] = bid("minecraft:dripstone_block")
        W[col3(pit, r.F - 2, r.F - 2)] = bid("minecraft:pointed_dripstone[vertical_direction=up]")
        fill(col3(dilate(pit, 1) & ~pit, r.F - 3, r.F - 1) & (W == DEEP), "cracked_stone_bricks")
    elif k == "checkpoint":
        fill(col3(m2, r.F, r.F), "polished_deepslate")
        setb(r.cx, r.F + 1, r.cz, "lodestone")
        for dx, dz in ((-3, -3), (3, 3), (-3, 3), (3, -3)):
            setb(r.cx + dx, r.F + 1, r.cz + dz, "soul_lantern")
    elif k == "teleport":
        fill(col3(((GX - r.cx) ** 2 + (GZ - r.cz) ** 2 <= 5), r.F, r.F), "crying_obsidian")
        setb(r.cx, r.F, r.cz, "respawn_anchor")
    elif k == "arrival":
        pass
    elif k == "cavern":
        fill(col3(m2, r.F, r.F), "60%tuff,25%moss_block,15%dripstone_block")
        drip = m2 & (nrng.random((NX, NZ)) < 0.07)
        W[col3(drip, r.top(), r.top())] = bid("minecraft:pointed_dripstone[vertical_direction=down]")
        stal = interior(r, 2) & (nrng.random((NX, NZ)) < 0.04)
        W[col3(stal, r.F + 1, r.F + 1)] = bid("minecraft:pointed_dripstone[vertical_direction=up]")
    elif k == "res":
        inn = interior(r, 2)
        if r.res == "Hồ Ngầm":
            pool = interior(r, 4)
            W[col3(pool, r.F - 2, r.F)] = bid("minecraft:water")
            fill(col3(inn & ~pool, r.F, r.F), "60%sand,40%clay")
            lp = pool & (nrng.random((NX, NZ)) < 0.05)
            W[col3(lp, r.F + 1, r.F + 1)] = bid("minecraft:lily_pad")
        elif r.res == "Vườn Rễ Cổ":
            fill(col3(m2, r.F, r.F), "60%rooted_dirt,40%moss_block")
            logs = inn & (smooth_noise(2.2, zlib.crc32(r.name.encode()) & 0xffff) > 0.55)
            fill(col3(logs, r.F + 1, r.top()), "60%dark_oak_log,25%mangrove_roots,15%oak_log")
            fill(col3(m2, r.top(), r.top()) & (W == AIR), "azalea_leaves[persistent=true]") if False else None
            can = m2 & (smooth_noise(3, 7) > 0.0)
            fill(col3(can, r.top(), r.top()), "70%azalea_leaves[persistent=true],30%flowering_azalea_leaves[persistent=true]")
            mc = inn & ~logs & (nrng.random((NX, NZ)) < 0.3)
            W[col3(mc, r.F + 1, r.F + 1)] = bid("minecraft:moss_carpet")
        elif r.res == "Thư Khố":
            fill(col3(m2, r.F, r.F), "dark_oak_planks")
            rows = inn & (((GX - r.x1) % 4 == 2) if r.w >= r.l else ((GZ - r.z1) % 4 == 2))
            rows &= ~(((GZ - r.cz) ** 2 <= 2) if r.w >= r.l else ((GX - r.cx) ** 2 <= 2))
            fill(col3(rows, r.F + 1, r.F + 4), "85%bookshelf,15%chiseled_bookshelf")
        elif r.res == "Vườn Nấm":
            fill(col3(m2, r.F, r.F), "mycelium")
            stems = inn & (smooth_noise(2.5, 31) > 0.62)
            fill(col3(stems, r.F + 1, r.top() - 3), "mushroom_stem")
            caps = dilate(stems, 2) & m2
            fill(col3(caps, r.top() - 2, r.top() - 2), "60%red_mushroom_block,40%brown_mushroom_block")
            sm = inn & ~dilate(stems, 1) & (nrng.random((NX, NZ)) < 0.08)
            fill(col3(sm, r.F + 1, r.F + 1), "50%red_mushroom,50%brown_mushroom")
        elif r.res == "Hang Pha Lê":
            fill(col3(m2, r.F, r.F), "60%calcite,40%amethyst_block")
            cl = inn & (nrng.random((NX, NZ)) < 0.07)
            fill(col3(cl, r.F + 1, r.F + 1), "50%amethyst_cluster,50%large_amethyst_bud")
            pil = inn & (smooth_noise(2.5, 41) > 0.65)
            fill(col3(pil, r.F + 1, r.top()), "60%amethyst_block,40%calcite")
        elif r.res in ("Mỏ Quặng Sâu", "Mỏ Cổ"):
            fill(col3(m2, r.F, r.F), "70%stone,30%gravel")
            boul = inn & (smooth_noise(2.0, zlib.crc32(r.name.encode()) & 0xfff) > 0.5)
            m3 = col3(boul, r.F + 1, r.F + 3)
            fill(m3, r.mat)

# 6) rương ở một số ngõ cụt
for fl, net in FLOORNETS.items():
    for c in net.dead:
        if rng.random() < 0.4:
            x, z = cell_xy(*c)
            setb(x + 1, F[fl] + 1, z + 1, "chest[facing=%s]" % rng.choice(CHEST_FACE))

# 6b) vòm cửa phòng: 2 bậc thang úp ở 2 góc trên lỗ cửa, khung 2 bên, đá đỉnh vòm
def arch_spec(r):
    if r.kind == "res" and r.res in ARCH: return ARCH[r.res]
    return ARCH[ARCH_KIND.get(r.kind, "default")]

def solid_at(x, y, z):
    i, j, k = I(x, y, z)
    return 0 <= i < NX and 0 <= k < NZ and W[i, j, k] not in (AIR, BEDROCK, VOID)

def air_at(x, y, z):
    i, j, k = I(x, y, z)
    return 0 <= i < NX and 0 <= k < NZ and W[i, j, k] == AIR

ARCH_COUNT = 0
for (fl, xa, xb, za, zb, base, dx, dz, room) in BANDS:
    if room.kind == "cavern": continue                       # hang tự nhiên: giữ lỗ thô
    st, fr, key = arch_spec(room)
    m = room.mask2d()
    shell = dilate(m, max(room.pad, 1)) & ~m
    y = base + CH
    if dx == 0:   # đi theo z, lỗ cửa rộng theo x
        cx = xa + 1
        cuts = [(cx, z) for z in range(za, zb + 1) if shell[cx - X0, z - X0]]
        sides = ((-1, 0, "west"), (1, 0, "east"))
    else:
        cz = za + 1
        cuts = [(x, cz) for x in range(xa, xb + 1) if shell[x - X0, cz - X0]]
        sides = ((0, -1, "north"), (0, 1, "south"))
    for (x, z) in cuts:
        if not air_at(x, y, z): continue
        for ox, oz, face in sides:
            if air_at(x + ox, y, z + oz):
                setb(x + ox, y, z + oz, f"{st}[facing={face},half=top]")
            for yy in range(base + 1, base + CH + 2):            # khung 2 bên lỗ cửa
                if solid_at(x + 2 * ox, yy, z + 2 * oz):
                    setb(x + 2 * ox, yy, z + 2 * oz, fr)
        if solid_at(x, y + 1, z):
            setb(x, y + 1, z, key)
        ARCH_COUNT += 1

# 6c) vòm sườn trong hành lang gạch thẳng: mỗi RIB_EVERY khe ô
RIB = "stone_brick_stairs"
for fl, net in FLOORNETS.items():
    y = F[fl] + CH
    for a, b in net.edges:
        if net.style.get(a) != "corr" or net.style.get(b) != "corr": continue
        if a[1] == b[1]:
            i = min(a[0], b[0])
            if i % RIB_EVERY: continue
            x = X0 + CELL * i + 3; z0 = X0 + CELL * a[1]
            pts = [(x, z0 + o) for o in range(3)]; out = [(x, z0 - 1), (x, z0 + 3)]; faces = ("north", "south")
        else:
            j = min(a[1], b[1])
            if j % RIB_EVERY: continue
            z = X0 + CELL * j + 3; x0 = X0 + CELL * a[0]
            pts = [(x0 + o, z) for o in range(3)]; out = [(x0 - 1, z), (x0 + 3, z)]; faces = ("west", "east")
        if all(air_at(px, yy, pz) for px, pz in pts for yy in (y - 1, y)) and            all(solid_at(px, yy, pz) for px, pz in out for yy in (y - 1, y)):
            setb(pts[0][0], y, pts[0][1], f"{RIB}[facing={faces[0]},half=top]")
            setb(pts[2][0], y, pts[2][1], f"{RIB}[facing={faces[1]},half=top]")

# 6d) đuốc gắn tường: hành lang, phòng nhỏ, mê cung (phòng lớn giữ đèn trần ở bước 7)
NONFULL = ("air", "water", "stairs", "_wall", "slab", "fence", "chest", "barrel", "torch", "lantern", "chain",
           "pointed", "carpet", "cobweb", "plate", "lily", "cluster", "bud", "structure_void", "leaves", "roots")
def full_ids():
    return [i for n, i in PAL.items() if not any(t in n for t in NONFULL)
            and n not in ("minecraft:red_mushroom", "minecraft:brown_mushroom")]

TORCH_N = 0
def place_torches(mask2d, y, gap):
    global TORCH_N
    j = y - Y0
    air = (W[:, j, :] == AIR) & mask2d
    full = np.isin(W[:, j, :], full_ids())
    cand = []
    for dx, dz, face in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
        nb = np.zeros_like(full)
        if dx == 1: nb[:-1] = full[1:]
        if dx == -1: nb[1:] = full[:-1]
        if dz == 1: nb[:, :-1] = full[:, 1:]
        if dz == -1: nb[:, 1:] = full[:, :-1]
        for i, k in zip(*np.nonzero(air & nb)):
            cand.append((int(i), int(k), face))
    taken = defaultdict(list)
    for idx in nrng.permutation(len(cand)):
        i, k, face = cand[idx]
        if W[i, j, k] != AIR: continue
        key = (i // gap, k // gap)
        if any(math.hypot(i - p, k - q) < gap for gi in (-1, 0, 1) for gk in (-1, 0, 1)
               for p, q in taken[(key[0] + gi, key[1] + gk)]):
            continue
        W[i, j, k] = bid(f"minecraft:{TORCH}[facing={face}]")
        taken[key].append((i, k)); TORCH_N += 1

for fl, net in FLOORNETS.items():
    ms = corr_masks(net)
    reg = ms["corr"] | ms["cave"]
    for (fl_, xa, xb, za, zb, base, *_) in BANDS:
        if fl_ == fl: reg |= (GX >= xa) & (GX <= xb) & (GZ >= za) & (GZ <= zb)
    for r in ROOMS:
        if r.F == F[fl] and r.kind not in CEIL_LIT and r.kind != "atrium_bot" and r.fl == fl:
            reg |= r.mask2d()
    place_torches(reg, F[fl] + 3, TORCH_GAP["corr"])
for mz in MAZES.values():
    place_torches(GR <= 89, F[mz.fl] + 3, TORCH_GAP["maze"])

# 7) đèn trần: chỉ phòng lớn (CEIL_LIT), lưới 6
airm = (W == AIR)
ceil = np.zeros_like(airm); ceil[:, 1:, :] = airm[:, :-1, :] & ~airm[:, 1:, :]
grid6 = ((GX % 6 == 0) & (GZ % 6 == 0))[:, None, :]
big = np.zeros(W.shape, bool)
for r in ROOMS:
    if r.kind in CEIL_LIT:
        big |= col3(r.mask2d(), r.F + 1, r.F + r.H + 1)
lightmask = ceil & grid6 & big
nolight = [PAL[n] for n in PAL if any(t in n for t in ("chain", "lantern", "pointed", "leaves", "water", "mushroom_block", "stem", "bedrock"))]
lightmask &= ~np.isin(W, nolight)
fill(lightmask, "50%sea_lantern,30%shroomlight,20%ochre_froglight")

# 7b) tấm bedrock ngăn giữa các tầng (Y = sàn tầng trên - 2), không lộ ra trong phòng
# tầng mê cung: tấm ngăn chính là đáy khối bedrock của nó (F + MZ_BOT)
SEAL_Y = {k: (F[k] + MZ_BOT if k == MAZE_FL else F[k] - 2) for k in (1, 2, 3, 4)}
SEAL_REPORT = []
def seal_floors():
    passable = np.isin(W, PASS_IDS())
    for k, ys in SEAL_Y.items():
        j = ys - Y0
        # lỗ được phép: thang xoắn và giếng trời đi từ tầng k xuống k+1
        allow = np.zeros((NX, NZ), bool)
        for s in STAIRS:
            if s.fl == k: allow |= s.foot(2)
        for a in ATRIA:
            if a.fl == k: allow |= a.foot(2)
        layer = passable[:, j, :]
        W[:, j, :][~layer & ~allow] = BEDROCK          # tấm phẳng
        # lỗ ngoài ý muốn (hồ, hố bẫy...) -> bọc vùng trũng bằng 2 lớp: lớp trong giữ nguyên, lớp ngoài bedrock
        holes = layer & ~allow
        seen = set(); dq = deque()
        for i, kk in zip(*np.nonzero(holes)):
            seen.add((i, j, kk)); dq.append((i, j, kk))
        ylim = F[k] - 8 - Y0                              # hố/hồ sâu nhất là sàn-6
        while dq:
            c = dq.popleft()
            for d in ((1,0,0),(-1,0,0),(0,-1,0),(0,0,1),(0,0,-1)):
                n = (c[0]+d[0], c[1]+d[1], c[2]+d[2])
                if n in seen or not (0 <= n[0] < NX and 0 <= n[2] < NZ) or n[1] > j: continue
                if passable[n]:
                    if n[1] <= ylim:
                        SEAL_REPORT.append(f"!! tầng {k}: vùng trũng chạm tầng dưới tại {n[0]+X0},{n[1]+Y0},{n[2]+X0}")
                        continue
                    seen.add(n); dq.append(n)
        lining = set()
        for c in seen:
            for d in ((1,0,0),(-1,0,0),(0,-1,0),(0,0,1),(0,0,-1)):
                n = (c[0]+d[0], c[1]+d[1], c[2]+d[2])
                if n not in seen and n[1] <= j and not passable[n]: lining.add(n)
        outer = set()
        for c in lining:
            for d in ((1,0,0),(-1,0,0),(0,-1,0),(0,0,1),(0,0,-1)):
                n = (c[0]+d[0], c[1]+d[1], c[2]+d[2])
                if n not in seen and n not in lining and n[1] <= j and not passable[n]: outer.add(n)
        # lớp trong ở ngang tấm phải giữ vật liệu cũ (đã bị đổi thành bedrock ở trên) -> khôi phục từ bản sao
        for n in outer: W[n] = BEDROCK
        # lớp trong nằm sát khoảng trống khác (vd trần phòng dưới) -> chính nó phải là bedrock
        for n in lining:
            for d in ((1,0,0),(-1,0,0),(0,-1,0),(0,0,1),(0,0,-1)):
                m = (n[0]+d[0], n[1]+d[1], n[2]+d[2])
                if m not in seen and passable[m]:
                    W[n] = BEDROCK; break
        for n in lining:
            if n[1] == j: W[n] = SNAP[n]
        SEAL_REPORT.append(f"tầng {k}|{k+1}: tấm Y{ys}, {int(holes.sum())} ô lỗ được bọc, lớp bọc {len(outer)} khối")
SNAP = W.copy()
seal_floors()
for mz in MAZES.values():
    face_maze(mz)

# 8) ngoài vùng an toàn -> structure_void (không ghi đè khi paste)
YY = np.arange(Y0, Y1 + 1)
safe = (GR[:, None, :] <= 90) & (YY[None, :, None] >= -55 + (GR[:, None, :] / 100) ** 3 * 55 + 3)
W[~safe] = VOID

# ------------------------------------------------------------------ kiểm tra
def bfs_from(x, y, z):
    ids = PASS_IDS() + [PAL[n] for n in ("minecraft:red_mushroom", "minecraft:brown_mushroom") if n in PAL]
    passable = np.isin(W, ids)
    start = I(x, y, z)
    seen = np.zeros_like(passable)
    dq = deque([start]); seen[start] = True
    while dq:
        c = dq.popleft()
        for d in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
            n = (c[0]+d[0], c[1]+d[1], c[2]+d[2])
            if 0 <= n[0] < NX and 0 <= n[1] < NY and 0 <= n[2] < NZ and passable[n] and not seen[n]:
                seen[n] = True; dq.append(n)
    return seen

def reached(seen, r):
    m2 = r.mask2d()
    ys = [r.F + 1, r.F + 2]
    if r.kind == "atrium": ys = [F[r.fl] + 1, F[r.fl] + 2]
    if r.kind == "stair": ys = [F[r.fl] + 1, F[r.fl] + 2]
    for y in ys:
        if seen[:, y - Y0, :][m2].any(): return True
    return False

def write_schem(path):
    def tag_str(s): b = s.encode(); return struct.pack(">H", len(b)) + b
    def named(t, name): return bytes([t]) + tag_str(name)
    inv = {v: k for k, v in PAL.items()}
    # Sponge v2: index = x + z*Width + y*Width*Length
    order = np.transpose(W, (1, 2, 0)).reshape(-1)   # y, z, x
    out = bytearray()
    for v in order.tolist():
        while v & ~0x7F:
            out.append((v & 0x7F) | 0x80); v >>= 7
        out.append(v)
    body = bytearray()
    body += named(3, "Version") + struct.pack(">i", 2)
    body += named(3, "DataVersion") + struct.pack(">i", 3953)
    body += named(2, "Width") + struct.pack(">h", NX)
    body += named(2, "Height") + struct.pack(">h", NY)
    body += named(2, "Length") + struct.pack(">h", NZ)
    body += named(11, "Offset") + struct.pack(">i", 3) + struct.pack(">iii", X0, Y0, X0)
    meta = named(3, "WEOffsetX") + struct.pack(">i", 0) + named(3, "WEOffsetY") + struct.pack(">i", 0) + named(3, "WEOffsetZ") + struct.pack(">i", 0) + b"\x00"
    body += named(10, "Metadata") + meta
    body += named(3, "PaletteMax") + struct.pack(">i", len(PAL))
    pal = b"".join(named(3, name) + struct.pack(">i", i) for name, i in PAL.items()) + b"\x00"
    body += named(10, "Palette") + pal
    body += named(7, "BlockData") + struct.pack(">i", len(out)) + bytes(out)
    body += named(9, "BlockEntities") + bytes([10]) + struct.pack(">i", 0)
    data = named(10, "Schematic") + bytes(body) + b"\x00"
    with gzip.open(path, "wb") as f: f.write(data)

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    seen = bfs_from(0, F[1] + 2, 8)
    rep = [f"SEED {SEED}", f"palette {len(PAL)} khối"]
    allok = True
    for r in ROOMS:
        a = reached(seen, r)
        rep.append(f"{'OK ' if a else '!! '} T{r.fl} {r.kind:<10} {r.name:<28} x{r.x1}..{r.x2} z{r.z1}..{r.z2} sàn {r.F} trần {r.F + r.H + 1}")
        allok &= a
    for fl, net in FLOORNETS.items():
        rep.append(f"T{fl}: {len(net.rooms)} phòng, {len(net.cells)} ô hành lang, ngõ cụt {len(net.dead)}, nối hỏng {net.failed}")
    for fl, mz in MAZES.items():
        tc = defaultdict(int)
        for t in mz.trap_pos: tc[t[0]] += 1
        rep.append(f"T{fl} MÊ CUNG: {len(mz.cells)} ô, {len(mz.edges)} đoạn đường, {len(mz.rooms)} phòng combat, "
                   f"bẫy {dict(tc)}, rương {len(mz.chests)}, nút không nối {mz.failed}")
    rep.append(f"vòm cửa {ARCH_COUNT}, đuốc {TORCH_N}")
    rep.append("TẤT CẢ PHÒNG THÔNG: " + str(allok))
    rep += SEAL_REPORT
    open(os.path.join(OUT, "layer2_report.txt"), "w", encoding="utf-8", newline="\n").write("\n".join(rep))
    print("\n".join(rep[-(len(SEAL_REPORT) + 3 + len(FLOORNETS) + len(MAZES)):]))
    rooms = [dict(name=r.name, kind=r.kind, floor=r.fl, x=[r.x1, r.x2], z=[r.z1, r.z2], y_floor=r.F,
                  y_ceil=r.F + r.H + 1, center=[r.cx, r.cz], shape=r.shape, resource=r.res,
                  in_maze=getattr(r, "in_maze", False))
             for r in ROOMS if r.kind not in ("atrium_bot",)]
    maze = [dict(floor=fl, y_floor=F[fl], traps=[dict(type=t, x=x, y=y, z=z) for t, x, y, z in mz.trap_pos],
                 chests=[dict(x=mz_mid(c[0]), y=F[fl] + 1, z=mz_mid(c[1])) for c in mz.chests])
            for fl, mz in MAZES.items()]
    json.dump(dict(seed=SEED, floors=F, maze_floor=MAZE_FL, rooms=rooms, maze=maze),
              open(os.path.join(OUT, "layer2_rooms.json"), "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=1)
    # bảng markdown dễ đọc (cùng dữ liệu với json)
    KIND_VI = {"hall": "Sảnh", "guardian": "Thủ Vệ", "combat": "combat", "arena": "combat (đấu trường)",
               "barracks": "combat (hầm lính)", "chest": "rương", "trap": "bẫy", "res": "tài nguyên",
               "checkpoint": "checkpoint", "teleport": "dịch chuyển thoát", "cavern": "hang tự nhiên",
               "stair": "thang xoắn ↓", "arrival": "chân thang", "atrium": "giếng trời"}
    TRAP_VI = {"pit": "hố chông (sâu 2, gờ 1 khối)", "arrow": "tấm áp suất + 2 dispenser", "web": "mạng nhện"}
    md = [f"# Layer 2 — danh sách phòng (seed {SEED})", "",
          "Sinh tự động bởi scripts/generate_layer2.py. Đừng sửa tay, sửa bộ sinh rồi chạy lại.", "",
          "Tọa độ thật. y_floor = khối sàn (người đứng ở y_floor+1); y_ceil = khối trần.", ""]
    for fl in range(1, 6):
        title = f"## Tầng {fl} (sàn Y{F[fl]})" + (" — MÊ CUNG" if fl in MAZES else "")
        md += [title, "", "| Tên | Loại | x | z | tâm | sàn / trần | Ghi chú |", "|---|---|---|---|---|---|---|"]
        for r in ROOMS:
            if r.fl != fl or r.kind == "atrium_bot": continue
            note = ""
            if r.kind == "atrium": note = f"ban công tầng {fl}, đáy hồ tầng {fl + 1}"
            if r.kind == "res": note = r.res
            if getattr(r, "in_maze", False): note = "trong mê cung"
            md.append(f"| {r.name} | {KIND_VI.get(r.kind, r.kind)} | {r.x1}..{r.x2} | {r.z1}..{r.z2} | {r.cx},{r.cz} | {r.F} / {r.F + r.H + 1} | {note} |")
        if fl in MAZES:
            mz = MAZES[fl]
            md += ["", "| Bẫy | x, y, z |", "|---|---|"]
            md += [f"| {TRAP_VI[t]} | {x}, {y}, {z} |" for t, x, y, z in mz.trap_pos]
            md += ["", "Rương ở ngõ cụt: " + ", ".join(f"({mz_mid(c[0])}, {F[fl] + 1}, {mz_mid(c[1])})" for c in mz.chests)]
        md.append("")
    open(os.path.join(OUT, "layer2_rooms.md"), "w", encoding="utf-8", newline="\n").write("\n".join(md))
    write_schem(os.path.join(OUT, SCHEM_NAME + ".schem"))
    print("schem xong", os.path.getsize(os.path.join(OUT, SCHEM_NAME + ".schem")))
