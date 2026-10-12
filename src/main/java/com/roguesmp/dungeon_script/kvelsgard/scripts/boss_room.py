"""
Kvelsgard — Layer 5: phòng boss "Cổ Thành Câm Lặng" bản 2 (artifact mục Layer 5, dựng theo ảnh mẫu Ancient City).
Gọi từ generate_world.py: build(g) với g là module generate_world (put/get/pick/carve_cols/...).

Hang deep dark R60 (nền khối Y-44, vòm Y-2 ở giữa, Y-14 sát vách), nền phủ sculk, chỉ ánh sáng soul + nến.
Trục chính: bệ xuất hiện (0, z50) → đại lộ + 12 cột vừa → 2 cột lớn → sảnh bán nguyệt → bệ 3 bậc → cổng 41×21×5.
Công trình theo mẫu: tháp canh T1–T3, nhà hai tầng H1–H4, điện mộ M, trại nến N, cổng thành đổ G, tường thành,
cột nấm làm cột đèn, 5 trụ đá, gai đá deepslate trên trần.
"""
import math
import numpy as np
import kv_model as M

B = M.BOSS
FL = B["floor"]
GY0 = B["plinth"] + 1               # hàng y=0 của bản vẽ cổng/bệ
BRICK = "60%deepslate_bricks,20%cracked_deepslate_bricks,20%deepslate_tiles"
RUBBLE = "40%cobbled_deepslate,30%cracked_deepslate_bricks,20%deepslate_bricks,10%deepslate"
STAIR = "deepslate_brick_stairs"


def hs(i, j):
    v = math.sin(i * 127.1 + j * 311.7) * 43758.5453
    return v - math.floor(v)


# ------------------------------------------------------------------ bản vẽ cổng + bệ (port từ B.front / B.side)
def gate_front(x, y):
    a, h, h2 = abs(x), hs(x + 70, y + 40), hs(x + 9, y + 90)
    if y >= 0:
        if a <= 13 and 6 <= y <= 14:
            if (y == 6 or y == 14) and (x & 1):
                return "tooth"
            if y == 13 and x % 4 == 0:
                return "lantern_hang"
            if y == 7 and x % 4 == 2:
                return "lantern"
            return None
        if (a == 14 and 5 <= y <= 15) or ((y == 5 or y == 15) and a <= 14):
            return "reinf"
        in_body = a <= 20 and y <= 20
        butt = (a == 21 and 7 <= y <= 12) or (a == 22 and 8 <= y <= 11)
        foot = a == 21 and y == 0
        bump = y == 21 and a <= 20 and h < 0.14
        if y == 20 and a <= 20 and h < 0.28:
            return None
        if not (in_body or butt or foot or bump):
            return None
        if y <= 2 and h2 < 0.4 - 0.1 * y:
            return "sculk"
        if y in (3, 17):
            return "tile"
        if y == 18 and x % 4 == 0:
            return "chis"
        return "crack" if h < 0.14 else ("cobble" if h < 0.26 else "brick")
    return None


def dais_block(x, z, y):
    """khối của bệ 3 bậc tại (x, z, y) với y < 0 theo bản vẽ (Y = GY0 + y)"""
    a, h, h2 = abs(x), hs(x + 70, y + 40), hs(x + 9, y + 90)
    center, west = a <= 4, -29 <= x <= -25
    if y <= -4:                                         # bậc 1: 59×25 cao 5
        if a > 29:
            return None
        if (center or west) and z >= -31:
            top = -8 + (-27 - z)
            return None if y > top else ("stair" if y == top else "brick")
        if z == -27 or z == -28:                        # hốc vòm 3×3 ở mặt trước
            for c in (10, 17, 24):
                d = abs(a - c)
                if d <= 1 and -7 <= y <= -5:
                    if y == -5 and d == 1:
                        break
                    if y == -7 and d == 0 and z == -28:
                        return "niche_item"
                    return None
        if y == -8:
            return "cobble"
        if y == -4 and h2 < 0.45:
            return "sculk"
        return "crack" if h < 0.14 else ("cobble" if h < 0.24 else "brick")
    if y >= -3 and y <= -2:                             # bậc 2: lùi 3, cao 2
        if a > 26 or z > -32:
            return None
        if center:
            if y == -3:
                return "stair" if z == -32 else "brick"
            if z == -32:
                return None
            if z == -33:
                return "stair"
        if y == -2 and B["fire"]["x1"] <= x <= B["fire"]["x2"] and z == B["fire"]["z"]:
            return "soulsand"
        if y == -2 and h2 < 0.5:
            return "sculk"
        return "brick"
    if y == -1:                                         # bậc 3: gờ 45×8 dưới chân cổng
        if a <= 22 and -50 <= z <= -43:
            return "sculk" if h2 < 0.55 else "tile"
        if B["fire"]["x1"] <= x <= B["fire"]["x2"] and z == B["fire"]["z"]:
            return "fire"
    return None


NAMES_GATE = {"brick": "deepslate_bricks", "crack": "cracked_deepslate_bricks", "cobble": "cobbled_deepslate",
              "tile": "deepslate_tiles", "chis": "chiseled_deepslate", "reinf": "reinforced_deepslate", "sculk": "sculk",
              "soulsand": "soul_sand", "fire": "soul_fire"}


def build(g):
    rng, put, get, pick, is_air = g.rng, g.put, g.get, g.pick, g.is_air

    # ------------------------------------------------------------------ đá quanh hang: deepslate trơn (bỏ tuff/basalt/blackstone của dải tầng 4)
    rx0, rx1 = -68 - g.X0, 68 - g.X0 + 1
    ry0, ry1 = B["seal_floor"] - g.Y0, M.SEAL[4] - g.Y0
    box = g.W[rx0:rx1, ry0:ry1, rx0:rx1]
    for n in ("tuff", "basalt", "blackstone"):
        box[box == g.bid(n)] = g.bid("deepslate")

    # ------------------------------------------------------------------ hang
    xs, zs = g.sub_grid(0, 0, 66, 66)
    r = np.hypot(xs, zs)
    k = M.boss_wob(np.arctan2(zs, xs))
    inside = r <= k
    ceil = M.boss_ceil(r) + (M.vn(xs, zs, 6, seed=60) - 0.5) * 3
    ceil = np.minimum(np.round(ceil), B["apex"]).astype(int)
    g.carve_cols(xs[inside], zs[inside], np.full(inside.sum(), FL + 1), ceil[inside])
    CEIL = {(int(x), int(z)): int(c) for x, z, c in zip(xs[inside], zs[inside], ceil[inside])}
    def in_cave(x, z):
        return (x, z) in CEIL
    for (x, z) in CEIL:
        put(x, FL, z, pick("65%deepslate,35%cobbled_deepslate"))

    # vùng lát (không gồ, không công trình đè)
    rd, hl, sp, dz_ = B["road"], B["hall"], B["spawn"], B["dais"]
    def seg_d(px, pz, a_, b_):
        vx, vz = b_[0] - a_[0], b_[1] - a_[1]
        t = max(0.0, min(1.0, ((px - a_[0]) * vx + (pz - a_[1]) * vz) / (vx * vx + vz * vz)))
        return math.hypot(px - a_[0] - t * vx, pz - a_[1] - t * vz)
    def zone(x, z):
        if dz_["x1"] <= x <= dz_["x2"] and dz_["z1"] <= z <= dz_["z2"]:
            return "dais"
        if rd["x1"] <= x <= rd["x2"] and rd["z1"] <= z <= rd["z2"]:
            return "road"
        if math.hypot(x - sp["x"], z - sp["z"]) <= sp["r"]:
            return "pad"
        if z >= hl["z"] and math.hypot(x - hl["x"], z - hl["z"]) <= hl["r"]:
            return "hall"
        if any(seg_d(x, z, a_, b_) <= 2 for a_, b_ in B["diag"]):
            return "diag"
        return None
    ZONE = {c: zone(*c) for c in CEIL}
    struct = set()
    def mark(x1, z1, x2, z2, pad=0):
        for x in range(x1 - pad, x2 + pad + 1):
            for z in range(z1 - pad, z2 + pad + 1):
                struct.add((x, z))
    for (_, x1, z1, x2, z2) in B["S"]:
        mark(x1, z1, x2, z2, 1)
    for (x, z) in B["big"]:
        mark(x - 5, z - 5, x + 5, z + 5)
    for (x, z) in B["lamps"]:
        mark(x - 4, z - 4, x + 4, z + 4)
    for (x, z), zn in ZONE.items():                     # nền gồ ±1 ngoài vùng lát và công trình
        if zn is None and (x, z) not in struct and M.vn(x, z, 5, seed=63) > 0.68:
            put(x, FL + 1, z, pick("60%deepslate,40%cobbled_deepslate"))

    # ------------------------------------------------------------------ 1 bệ xuất hiện, 2 đại lộ, 4 sảnh, 7 đường chéo
    for x in range(sp["x"] - sp["r"], sp["x"] + sp["r"] + 1):
        for z in range(sp["z"] - sp["r"], sp["z"] + sp["r"] + 1):
            d = math.hypot(x - sp["x"], z - sp["z"])
            if d <= sp["r"] + 0.3:
                for y in range(FL + 1, sp["top"]):
                    put(x, y, z, "deepslate_tiles" if d > sp["r"] - 1 else "deepslate_bricks")
                put(x, sp["top"], z, "chiseled_deepslate" if d < 1 else "deepslate_tiles")
    for i, z in enumerate((43, 42, 41)):
        for x in range(-3, 4):
            for yy in range(FL + 1, sp["top"] - i):
                put(x, yy, z, "deepslate_tiles")
            put(x, sp["top"] - i, z, f"{STAIR}[facing=south]")
    for x in range(rd["x1"], rd["x2"] + 1):
        for z in range(rd["z1"], rd["z2"] + 1):
            put(x, FL, z, "gray_wool" if abs(x) <= 1 else pick("70%deepslate_tiles,30%cracked_deepslate_tiles"))
    for (x, z), zn in ZONE.items():
        if zn == "hall":
            d = math.hypot(x - hl["x"], z - hl["z"])
            put(x, FL, z, "chiseled_deepslate" if d < 1.5 else ("polished_deepslate" if int(d // 2) % 2 else "deepslate_tiles"))
        elif zn == "diag":
            put(x, FL, z, pick("50%cobbled_deepslate,30%cracked_deepslate_tiles,20%deepslate_tiles"))
            if rng.random() < 0.06:
                put(x, FL, z, "air")                    # ô lát vỡ sụt 1
                put(x, FL - 1, z, "cobbled_deepslate")
    for x in range(hl["x"] - hl["r"] - 1, hl["x"] + hl["r"] + 2):    # lan can thấp theo cung, hở 3 lối
        for z in range(hl["z"], hl["z"] + hl["r"] + 2):
            d = math.hypot(x - hl["x"], z - hl["z"])
            if hl["r"] - 0.5 < d <= hl["r"] + 0.5 and abs(x) > rd["x2"] + 1 \
                    and all(seg_d(x, z, a_, b_) > 2.6 for a_, b_ in B["diag"]):
                put(x, FL + 1, z, "deepslate_brick_wall")

    # ------------------------------------------------------------------ 5 bệ 3 bậc + 6 cổng triệu hồi
    for x in range(-29, 30):
        for z in range(dz_["z1"], dz_["z2"] + 1):
            for y in range(-8, 0):
                b_ = dais_block(x, z, y)
                Y = GY0 + y
                if b_ is None:
                    continue
                if b_ == "stair":
                    put(x, Y, z, f"{STAIR}[facing=north]")
                elif b_ == "niche_item":
                    put(x, Y, z, "candle[candles=3,lit=true]" if (abs(x) // 7) % 2 else "skeleton_skull[rotation=0]")
                else:
                    put(x, Y, z, NAMES_GATE[b_])
    gt = B["gate"]
    for x in range(-22, 23):
        for y in range(0, 22):
            b_ = gate_front(x, y)
            for z in range(gt["z1"], gt["z2"] + 1):
                Y = GY0 + y
                if b_ is None:
                    if abs(x) <= 13 and 6 <= y <= 14:
                        put(x, Y, z, "air")
                    continue
                if b_ == "tooth":
                    put(x, Y, z, f"{STAIR}[facing={'south' if z >= -46 else 'north'},half={'bottom' if y == 6 else 'top'}]")
                elif b_ in ("lantern", "lantern_hang"):
                    put(x, Y, z, ("soul_lantern[hanging=true]" if b_ == "lantern_hang" else "soul_lantern") if z == -46 else "air")
                elif b_ == "reinf":
                    put(x, Y, z, "reinforced_deepslate" if z == -46 else pick(BRICK))
                else:
                    put(x, Y, z, NAMES_GATE[b_])
    # bàn triệu hồi + 2 bàn nến bậc thang trên bậc 2
    ty = GY0 - 1
    put(-1, ty, -36, "polished_deepslate"); put(0, ty, -36, "chiseled_deepslate"); put(1, ty, -36, "polished_deepslate")
    put(0, ty + 1, -36, "candle[candles=4,lit=true]")
    for sx in (-7, 7):
        for dx in (-1, 0, 1):
            put(sx + dx, ty, -36, "deepslate_tiles")
        put(sx, ty + 1, -36, "polished_deepslate")
        put(sx - 1, ty + 1, -36, "candle[candles=2,lit=true]")
        put(sx + 1, ty + 1, -36, "candle[candles=2,lit=true]")
        put(sx, ty + 2, -36, "candle[candles=3,lit=true]")
    for x in range(-29, 30):                            # sculk chảy tràn xuống mặt trước bệ
        for y in range(FL + 1, GY0 - 4):
            if get(x, y, -27).replace("minecraft:", "") in ("deepslate_bricks", "cracked_deepslate_bricks", "cobbled_deepslate") \
                    and rng.random() < 0.4 * (y - FL) / (GY0 - 4 - FL):
                put(x, y, -27, "sculk")

    # ------------------------------------------------------------------ 3 cột vừa, cột lớn, cột nấm
    def column_mid(xc, zc, broken):
        s = 1 if xc > 0 else -1
        for x in range(xc - 2, xc + 3):
            for z in range(zc - 2, zc + 3):
                put(x, FL + 1, z, "polished_deepslate")
                put(x, FL + 2, z, "deepslate_bricks")
        top = FL + 11
        for x in range(xc - 1, xc + 2):
            for z in range(zc - 1, zc + 2):
                hh = top if not broken else FL + rng.randint(4, 7)
                for y in range(FL + 3, hh + 1):
                    put(x, y, z, "chiseled_deepslate" if (y - FL) % 4 == 0 else "deepslate_tiles")
        if broken:
            ox = xc + s * 4                              # mũ rơi nằm cạnh chân, phía ngoài
            for x in range(ox - 1, ox + 2):
                for z in range(zc - 2, zc + 3):
                    if in_cave(x, z):
                        put(x, FL + 1, z, "cobbled_deepslate")
                        if rng.random() < 0.5:
                            put(x, FL + 2, z, "cobbled_deepslate")
            return
        for x in range(xc - 2, xc + 3):
            for z in range(zc - 2, zc + 3):
                put(x, FL + 12, z, "cobbled_deepslate")
                put(x, FL + 13, z, "cobbled_deepslate")
        put(xc - s * 2, FL + 9, zc, "dark_oak_fence")    # tay fence hướng vào đường + đèn soul treo
        put(xc - s * 2, FL + 8, zc, "soul_lantern[hanging=true]")

    def column_big(xc, zc):
        for x in range(xc - 3, xc + 4):
            for z in range(zc - 3, zc + 4):
                put(x, FL + 1, z, "polished_deepslate")
                put(x, FL + 2, z, "deepslate_bricks")
        for x in range(xc - 2, xc + 3):
            for z in range(zc - 2, zc + 3):
                for y in range(FL + 3, FL + 14):
                    put(x, y, z, "chiseled_deepslate" if (y - FL) % 4 == 0 else
                        ("deepslate_tiles" if abs(x - xc) < 2 and abs(z - zc) < 2 else "deepslate_bricks"))
        for x in range(xc - 3, xc + 4):
            for z in range(zc - 3, zc + 4):
                put(x, FL + 14, z, "cobbled_deepslate")
        for x in range(xc - 4, xc + 5):
            for z in range(zc - 4, zc + 5):
                put(x, FL + 15, z, "cobbled_deepslate")
                put(x, FL + 16, z, "cobbled_deepslate")
        s = -1 if xc > 0 else 1
        put(xc + 4 * s, FL + 14, zc, "soul_lantern[hanging=true]")
        put(xc, FL + 14, zc + 4, "soul_lantern[hanging=true]")

    def column_mush(xc, zc):
        for x in range(xc - 2, xc + 3):
            for z in range(zc - 2, zc + 3):
                if abs(x - xc) == 2 and abs(z - zc) == 2 and rng.random() < 0.5:
                    continue
                put(x, FL + 1, z, "deepslate_bricks")
        for x in range(xc - 1, xc + 2):
            for z in range(zc - 1, zc + 2):
                for y in range(FL + 2, FL + 8):
                    put(x, y, z, "deepslate_tiles" if (x, z) != (xc, zc) else "polished_deepslate")
        ox, oz = rng.choice([(-1, 0), (1, 0), (0, -1), (0, 1), (1, 1), (-1, -1)])
        for x in range(xc - 2 + ox, xc + 3 + ox):
            for z in range(zc - 2 + oz, zc + 3 + oz):
                put(x, FL + 8, z, "cobbled_deepslate")
        for x in range(xc - 3 + ox, xc + 4 + ox):
            for z in range(zc - 3 + oz, zc + 4 + oz):
                corner = abs(x - xc - ox) == 3 and abs(z - zc - oz) == 3
                if not corner and rng.random() < 0.9:
                    put(x, FL + 9, z, "cobbled_deepslate")
        for (lx, lz) in ((xc + 3 + ox, zc + oz), (xc - 3 + ox, zc + oz)):
            if is_air(lx, FL + 8, lz) and not is_air(lx, FL + 9, lz):
                put(lx, FL + 8, lz, "soul_lantern[hanging=true]")
                break

    for i, zc in enumerate(B["colZ"]):
        for j, xc in enumerate((-B["colX"], B["colX"])):
            column_mid(xc, zc, (i * 2 + j) in B["colBroken"])
    for (xc, zc) in B["big"]:
        column_big(xc, zc)
    for (xc, zc) in B["lamps"]:
        column_mush(xc, zc)

    # ------------------------------------------------------------------ tháp canh, nhà hai tầng, điện mộ, trại nến, cổng thành
    def tower(x1, z1, x2, z2, state):
        cx, cz = (x1 + x2) // 2, (z1 + z2) // 2
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                put(x, FL + 1, z, "deepslate_bricks" if x in (x1, x2) or z in (z1, z2) else "deepslate_tiles")
        levels = {"full": 3, "top_lost": 2, "ground": 1}[state]
        for lv in range(levels):
            y0 = FL + 2 + lv * 6
            hmax = 5 if lv < 2 else 3
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    ring = abs(dx) == 3 or abs(dz) == 3
                    x, z = cx + dx, cz + dz
                    if not ring:
                        if lv > 0:
                            put(x, y0 - 1, z, "deepslate_tiles")
                        continue
                    corner = abs(dx) == 3 and abs(dz) == 3
                    face_mid = (abs(dx) == 3 and abs(dz) <= 1) or (abs(dz) == 3 and abs(dx) <= 1)
                    top = y0 + hmax - 1
                    if state == "ground":
                        top = y0 + rng.randint(0, 3)
                    elif state == "top_lost" and lv == 1:
                        top = y0 + rng.randint(1, hmax - 1)
                    for y in range(y0, top + 1):
                        if corner:
                            put(x, y, z, "dark_oak_log[axis=y]")
                        elif face_mid and (y - y0) <= 2 and (lv > 0 or dx > 0 or abs(dz) == 3):
                            put(x, y, z, "air")
                        else:
                            put(x, y, z, pick(BRICK))
        for y in range(FL + 2, FL + 2 + 6 * levels - (1 if levels == 3 else 0)):    # thang gỗ
            if levels == 1:
                break
            put(cx - 2, y, cz + 2, "ladder[facing=east]")
        if levels >= 2:
            put(cx - 2, FL + 7, cz + 2, "ladder[facing=east]")
        if levels == 3:
            put(cx - 2, FL + 13, cz + 2, "ladder[facing=east]")
            for dx in range(-4, 5):
                for dz in range(-4, 5):
                    if abs(dx) == 4 or abs(dz) == 4:
                        put(cx + dx, FL + 17, cz + dz, "deepslate_bricks")
            for dx in range(-5, 6):
                for dz in range(-5, 6):
                    edge = abs(dx) == 5 or abs(dz) == 5
                    put(cx + dx, FL + 18, cz + dz, "deepslate_bricks" if edge else "deepslate_tiles")
                    if edge and (dx + dz) % 2 == 0:
                        put(cx + dx, FL + 19, cz + dz, "deepslate_bricks")
            for dx in (-4, 4):
                for dz in (-4, 4):
                    put(cx + dx, FL + 16, cz + dz, "soul_lantern[hanging=true]")
            for y in (FL + 17, FL + 18):
                put(cx - 2, y, cz + 2, "ladder[facing=east]")
        if state == "top_lost":                         # vương miện đổ nằm cạnh tháp
            for dx in range(-5, 6):
                for dz in (6, 7):
                    if rng.random() < 0.75 and in_cave(cx + dx, cz - dz):
                        put(cx + dx, FL + 1, cz - dz, pick("70%deepslate_bricks,30%cracked_deepslate_bricks"))
            put(cx + 1, FL + 2, cz - 6, "soul_lantern")
        if state == "ground":
            for _ in range(10):
                x, z = rng.randint(x1, x2), rng.randint(z1, z2)
                if is_air(x, FL + 2, z):
                    put(x, FL + 2, z, pick(RUBBLE))

    def house(x1, z1, x2, z2, state):
        long_x = (x2 - x1) >= (z2 - z1)
        mid = (x1 + x2) // 2 if long_x else (z1 + z2) // 2
        def collapsed(x, z):
            return state == "half" and ((x if long_x else z) > mid)
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                per = x in (x1, x2) or z in (z1, z2)
                pillar = (((x - x1) % 3 == 0 or x == x2) and z in (z1, z2)) or \
                    (((z - z1) % 3 == 0 or z == z2) and x in (x1, x2))
                if pillar:
                    for y in range(FL + 1, FL + 5):
                        put(x, y, z, "deepslate_bricks")
                elif per:
                    put(x, FL + 4, z, "deepslate_bricks")
                if collapsed(x, z):
                    continue
                if state == "frame" and rng.random() < 0.6:
                    continue
                put(x, FL + 5, z, "deepslate_tiles" if per else "gray_wool")
                if state == "frame":
                    continue
                if per:
                    win = (x - x1) % 3 == 2 or (z - z1) % 3 == 2
                    hh = FL + 9 if state in ("full", "half") else FL + rng.randint(6, 9)
                    for y in range(FL + 6, hh + 1):
                        put(x, y, z, "air" if (win and y in (FL + 7, FL + 8)) else "deepslate_bricks")
        if state in ("full", "half"):
            for x in range(x1 - 1, x2 + 2):
                for z in range(z1 - 1, z2 + 2):
                    if not collapsed(x, z):
                        put(x, FL + 10, z, "polished_deepslate" if x in (x1 - 1, x2 + 1) or z in (z1 - 1, z2 + 1) else "deepslate_tiles")
        if state == "half":
            for x in range(x1, x2 + 1):
                for z in range(z1, z2 + 1):
                    if collapsed(x, z) and rng.random() < 0.55:
                        for y in range(FL + 1, FL + 1 + rng.randint(1, 3)):
                            if is_air(x, y, z):
                                put(x, y, z, pick(RUBBLE))
        if state != "frame":                            # thang lên tầng 2 bám cột góc
            for y in range(FL + 1, FL + 7):
                put(x1 + 1, y, z1, "ladder[facing=east]")
            put(x1 + 1, FL + 7, z1, "air")
        put((x1 + x2) // 2, FL + 1, (z1 + z2) // 2, "candle[candles=2,lit=true]")

    def mausoleum(x1, z1, x2, z2):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                put(x, FL, z, "deepslate_tiles")
                if x in (x1, x2) or z in (z1, z2):
                    corner = (x - x1 < 3 or x2 - x < 3) and (z - z1 < 3 or z2 - z < 3)
                    hh = rng.randint(7, 10) if corner else rng.randint(4, 8)
                    if not corner and abs(x - (x1 + x2) // 2) <= 1:
                        for y in (FL + 6, FL + 7):      # lối vào giữa 2 cạnh, vòm phía trên
                            put(x, y, z, pick(BRICK))
                        continue
                    for y in range(FL + 1, FL + 1 + hh):
                        if corner and y in (FL + 2, FL + 3, FL + 4) and (x - x1 == 1 or x2 - x == 1 or z - z1 == 1 or z2 - z == 1):
                            continue                    # vòm gãy ở góc
                        put(x, y, z, pick(BRICK))
        cz = (z1 + z2) // 2
        pool = (x1 + 3, cz - 4, x1 + 9, cz + 4)
        pit = (x2 - 9, cz - 4, x2 - 3, cz + 4)
        for (px1, pz1, px2, pz2) in (pool, pit):
            for x in range(px1, px2 + 1):
                for z in range(pz1, pz2 + 1):
                    if x in (px1, px2) or z in (pz1, pz2):
                        put(x, FL + 1, z, "deepslate_brick_wall" if rng.random() < 0.8 else "air")
        for x in range(pool[0] + 1, pool[2]):
            for z in range(pool[1] + 1, pool[3]):
                for y in range(FL - 2, FL + 1):
                    put(x, y, z, "water")
                put(x, FL - 3, z, "deepslate_tiles")
        for x in range(pit[0] + 1, pit[2]):
            for z in range(pit[1] + 1, pit[3]):
                put(x, FL - 3, z, "deepslate_tiles")
                for y in range(FL - 2, FL + 1):
                    put(x, y, z, "air")
        sx = pit[0] + 1                                  # bậc xuống hố mộ, đi lên về phía bắc
        put(sx, FL + 1, pit[1], "air")
        for i, z in enumerate(range(pit[1] + 1, pit[1] + 4)):
            put(sx, FL - i, z, f"{STAIR}[facing=north]")
            for y in range(FL - 3, FL - i):
                put(sx, y, z, "deepslate_tiles")
        for (qx, qz) in [(px, pz) for px in (pit[0] + 2, pit[0] + 4) for pz in (pit[1] + 2, pit[1] + 4, pit[1] + 6)]:
            put(qx, FL - 2, qz, "polished_deepslate")
            put(qx + 1, FL - 2, qz, "chiseled_deepslate")
            put(qx, FL - 1, qz, "skeleton_skull[rotation=4]" if rng.random() < 0.4 else "candle[candles=1,lit=true]")
        tx = (x1 + x2) // 2
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                put(tx + dx, FL + 1, cz + dz, "deepslate_tiles")
                if dx or dz:
                    if (dx + dz) % 2:
                        put(tx + dx, FL + 2, cz + dz, "candle[candles=2,lit=true]")
        put(tx, FL + 2, cz, "polished_deepslate")
        put(tx, FL + 3, cz, "candle[candles=4,lit=true]")
        put(tx, FL + 1, z2 - 1, "sculk_catalyst")

    def candle_camp(x1, z1, x2, z2):
        for x in range(x1, x2 + 1):
            for z in (z1, z2):
                for y in range(FL + 1, FL + 4):
                    put(x, y, z, pick(BRICK))
                if rng.random() < 0.5:
                    put(x, FL + 4, z, "candle[candles=%d,lit=true]" % rng.randint(1, 4))
        for x in range(x1 + 1, x2):
            for z in range(z1, z2 + 1):
                put(x, FL + 4, z, "deepslate_tiles" if z in (z1, z2) or rng.random() < 0.6 else "air")
        for x in range(x1 + 1, x2):
            for z in range(z1 + 1, z2):
                if rng.random() < 0.4:
                    put(x, FL + 1, z, "candle[candles=%d,lit=true]" % rng.randint(1, 4))

    def city_gate(x1, z1, x2, z2):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                end = x - x1 < 3 or x2 - x < 3
                for y in range(FL + 1, FL + 10):
                    if end and y <= FL + 9 - (1 if rng.random() < 0.3 else 0):
                        put(x, y, z, "chiseled_deepslate" if y == FL + 7 else pick(BRICK))
                    elif not end and y >= FL + 8 and rng.random() < 0.7:
                        put(x, y, z, pick(BRICK))
                    elif not end and y <= FL + 7 - abs(z - (z1 + z2) / 2) and rng.random() < 0.9:
                        put(x, y, z, pick(RUBBLE))

    for (name, x1, z1, x2, z2) in B["S"]:
        kind = name[0]
        if name == "T1": tower(x1, z1, x2, z2, "full")
        elif name == "T2": tower(x1, z1, x2, z2, "top_lost")
        elif name == "T3": tower(x1, z1, x2, z2, "ground")
        elif name == "H1": house(x1, z1, x2, z2, "full")
        elif name == "H2": house(x1, z1, x2, z2, "noroof")
        elif name == "H3": house(x1, z1, x2, z2, "half")
        elif name == "H4": house(x1, z1, x2, z2, "frame")
        elif kind == "M": mausoleum(x1, z1, x2, z2)
        elif kind == "N": candle_camp(x1, z1, x2, z2)
        elif kind == "G": city_gate(x1, z1, x2, z2)

    # ------------------------------------------------------------------ tường thành: 4 cung đứt quãng ở r 51..55 (tách khỏi vách), 2 tầng lối đi lót len
    for (x, z) in list(CEIL):
        rr = math.hypot(x, z) + 3
        if not (54 <= rr <= 58.2):
            continue
        ang = math.degrees(math.atan2(z, x)) % 360
        if not any(a0 <= ang <= a1 for a0, a1 in B["walls"]) or M.vn(ang * 3, 0, 4, seed=64) < 0.22:
            continue
        if ZONE.get((x, z)) == "diag":
            continue
        cren = (int(ang * rr * math.pi / 180) % 2) == 0
        if rr < 55.3:
            for y in range(FL + 1, FL + 5):
                put(x, y, z, "chiseled_deepslate" if y == FL + 3 and cren else pick(BRICK))
            if cren:
                put(x, FL + 5, z, "deepslate_bricks")
        elif rr < 56.6:
            for y in range(FL + 1, FL + 4):
                put(x, y, z, pick(BRICK))
            put(x, FL + 4, z, "gray_wool")
        else:
            for y in range(FL + 1, FL + 6):
                put(x, y, z, pick(BRICK))
            put(x, FL + 6, z, "gray_wool")
            if rr > 57.4 and cren:
                put(x, FL + 7, z, "deepslate_bricks")

    # ------------------------------------------------------------------ 5 trụ đá tự nhiên thắt giữa
    for (px, pz, pr) in B["pillars"]:
        for x in range(px - pr - 3, px + pr + 4):
            for z in range(pz - pr - 3, pz + pr + 4):
                if not in_cave(x, z):
                    continue
                top = CEIL[(x, z)]
                mid, half = (FL + top) / 2, max(1.0, (top - FL) / 2)
                for y in range(FL + 1, top + 1):
                    rad = pr * (0.62 + 0.38 * ((y - mid) / half) ** 2) + 1.2 * (y < FL + 3) \
                        + (M.vn(x * 3 + y, z * 3, 4, seed=61) - 0.5)
                    if math.hypot(x - px, z - pz) <= rad:
                        put(x, y, z, pick("70%deepslate,30%cobbled_deepslate"))
                d = math.hypot(x - px, z - pz)
                if pr + 1 < d <= pr + 3 and is_air(x, FL + 1, z) and rng.random() < 0.6:
                    put(x, FL + 1, z, "sculk_vein[down=true]")

    # ------------------------------------------------------------------ trần: gai deepslate trên khu tàn tích, xích đèn soul
    def open_axis(x, z):
        return ZONE.get((x, z)) in ("road", "pad", "hall", "dais") or (abs(x) <= 9 and z > -20) \
            or (dz_["x1"] - 4 <= x <= dz_["x2"] + 4 and z < -24)
    anchors = [((x1 + x2) / 2, (z1 + z2) / 2) for (_, x1, z1, x2, z2) in B["S"]] + [(x, z) for (x, z) in B["lamps"]]
    for (cx, cz) in anchors:
        for _ in range(rng.randint(2, 4)):
            px, pz = cx + rng.uniform(-6, 6), cz + rng.uniform(-6, 6)
            ix, iz = int(round(px)), int(round(pz))
            if not in_cave(ix, iz) or open_axis(ix, iz):
                continue
            L, rb = rng.randint(4, 10), rng.uniform(1.3, 2.4)
            for x in range(int(px - rb - 1), int(px + rb + 2)):
                for z in range(int(pz - rb - 1), int(pz + rb + 2)):
                    if not in_cave(x, z):
                        continue
                    d = math.hypot(x - px, z - pz)
                    if d > rb:
                        continue
                    n = max(1, int(L * (1 - d / rb) ** 1.2))
                    top = CEIL[(x, z)]
                    for i in range(n):
                        if is_air(x, top - i, z):
                            put(x, top - i, z, "sculk" if i == n - 1 else pick("60%deepslate,40%cobbled_deepslate"))
    chains = 0
    for (x, z), c in sorted(CEIL.items(), key=lambda kv: hs(kv[0][0] + 3, kv[0][1] + 5)):
        if chains >= 14:
            break
        if open_axis(x, z) or (x, z) in struct or not is_air(x, c, z):
            continue
        n = rng.randint(4, 12)
        if all(is_air(x, c - i, z) for i in range(n + 4)):
            for i in range(n):
                put(x, c - i, z, "chain")
            put(x, c - n, z, "soul_lantern[hanging=true]")
            chains += 1

    # ------------------------------------------------------------------ sculk: thảm nền theo khoảng cách tới cổng, viền vein, lưỡi bò lên vách
    surf = {}
    for (x, z) in CEIL:
        for y in range(FL - 3, B["plinth"] + 2):
            if not is_air(x, y, z) and get(x, y, z) != "minecraft:water" and is_air(x, y + 1, z):
                surf[(x, z)] = y
                break
    SURFACE_OK = ("deepslate", "cobbled_deepslate", "deepslate_tiles", "cracked_deepslate_tiles", "polished_deepslate",
                  "gray_wool", "deepslate_bricks", "cracked_deepslate_bricks", "chiseled_deepslate")
    sk = set()
    for (x, z), y in surf.items():
        if get(x, y, z).replace("minecraft:", "") not in SURFACE_OK:
            continue
        zn = ZONE.get((x, z))
        near = max(0.0, 1 - math.hypot(x, z + 46) / 75)
        n = hs(x // 4 + 50, z // 4 + 50) * 0.6 + hs(x // 9 + 9, z // 9 + 9) * 0.4
        p = 0.34 + 0.6 * near + 0.7 * (n - 0.5)
        p = {"dais": 0.82, "road": 0.2, "pad": 0.2, "hall": 0.4, "diag": 0.3}.get(zn, p)
        if hs(x + 31, z + 77) < p:
            put(x, y, z, "sculk")
            sk.add((x, z))
    for (x, z), y in surf.items():
        if (x, z) in sk or not is_air(x, y + 1, z):
            continue
        if any((x + dx, z + dz) in sk and surf.get((x + dx, z + dz)) == y for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))) \
                and rng.random() < 0.6 and get(x, y, z).replace("minecraft:", "") in SURFACE_OK:
            put(x, y + 1, z, "sculk_vein[down=true]")
    for (x, z) in CEIL:                                 # lưỡi sculk bò lên vách cao 4-8
        if math.hypot(x, z) < 52:
            continue
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            wx, wz = x + dx, z + dz
            if in_cave(wx, wz):
                continue
            ang = math.degrees(math.atan2(wz, wx))
            v = M.vn(ang * 2.5, 0, 3, seed=65)
            if v < 0.55:
                continue
            for y in range(FL + 1, FL + 1 + int(4 + (v - 0.55) / 0.45 * 4)):
                if not is_air(wx, y, wz):
                    put(wx, y, wz, "sculk")
    sk_cells = sorted(sk, key=lambda c: hs(c[0] + 11, c[1] + 17))
    placed = {"sculk_sensor": 0, "sculk_catalyst": 0}
    for (x, z) in sk_cells:
        y = surf[(x, z)]
        if placed["sculk_sensor"] < 20 and ZONE.get((x, z)) is None and (x, z) not in struct and is_air(x, y + 1, z):
            put(x, y + 1, z, "sculk_sensor")
            placed["sculk_sensor"] += 1
    for (name, x1, z1, x2, z2) in B["S"]:
        for (x, z) in ((x1 - 1, z1 - 1), (x2 + 1, z2 + 1)):
            if placed["sculk_catalyst"] < 14 and (x, z) in surf and is_air(x, surf[(x, z)] + 1, z):
                put(x, surf[(x, z)], z, "sculk_catalyst")
                placed["sculk_catalyst"] += 1
    for t in (45, 135, 225, 315):                       # 4 shrieker quanh bệ xuất hiện + 4 quanh cổng
        x = int(round(sp["x"] + math.cos(math.radians(t)) * (sp["r"] + 2)))
        z = int(round(sp["z"] + math.sin(math.radians(t)) * (sp["r"] + 2)))
        if (x, z) in surf:
            put(x, surf[(x, z)] + 1, z, "sculk_shrieker")
    for x in (-24, -21, 21, 24):
        put(x, GY0 - 1, -46, "sculk_shrieker")

    return dict(
        name="Cổ Thành Câm Lặng",
        spawn=[sp["x"], sp["top"] + 1, sp["z"]], yaw=180,
        teleport_from=[0, M.F[4] + 3, 50],
        floor=FL, apex=B["apex"], wall_top=B["wall_top"], radius=B["R"],
        hall=[hl["x"], FL + 1, hl["z"] + 6],
        summon_table=[0, GY0, -36],
        fire_trench=dict(x=[B["fire"]["x1"], B["fire"]["x2"]], y=GY0 - 1, z=B["fire"]["z"]),
        gate=dict(min=[gt["x1"], GY0, gt["z1"]], max=[gt["x2"], GY0 + 20, gt["z2"]],
                  opening_min=[-13, GY0 + 6, gt["z1"]], opening_max=[13, GY0 + 14, gt["z2"]]),
        structures=[dict(id=n, min=[x1, FL + 1, z1], max=[x2, FL + 1, z2]) for (n, x1, z1, x2, z2) in B["S"]],
        sculk_blocks=placed,
    )
