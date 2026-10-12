"""
Kvelsgard — mô hình bố cục dùng chung (port từ bản thiết kế artifact "Kvelsgard – bản thiết kế").

Quy ước tọa độ: x đông(+)/tây(-), z nam(+)/bắc(-), tâm đảo (0, 0). "floor" của một khu = Y của KHỐI sàn
(người đứng ở floor+1). Phòng [x1, z1, x2, z2] phủ x1..x2-1, z1..z2-1.
"""
import math
import numpy as np

SEED = 2026

# ------------------------------------------------------------------ hộp thế giới
R_BASE = 110                        # bán kính đảo trung bình, mép thật 104..117 theo noise
X0, X1 = -119, 119
Z0, Z1 = -119, 119
Y0 = -60                            # đáy đảo thấp nhất ~-56
BARRIER_R = (119.5, 122)            # tường barrier (trong, ngoài)

# ------------------------------------------------------------------ cao độ các tầng
YARD_Y = 120                        # bãi mỏ + cửa hang
TERRACE_Y = 98                      # thềm xuất hiện của player (đứng Y99)
TUNNEL_END_Y = 106                  # sàn hang dẫn ở mép hố
F = {2: 72, 3: 40, 4: 8}            # khối sàn tầng 2, 3, 4
CAV_H = 26                          # chiều cao hang khai thác (khí cao nhất = F + 26)
SEAL1_MAX = 101                     # tấm bedrock dưới Layer 1 (thấp hơn ở chỗ mặt đất thấp: mặt đất - 3)
SEAL = {2: 69, 3: 37, 4: 3}         # tấm bedrock dưới tầng 2, 3, 4
SKIRT_IN = 3.0                      # vách bedrock đứng nằm trong mép đảo 1.6..3 khối
SKIRT_OUT = 1.6

# ------------------------------------------------------------------ noise
def cl(x):
    return np.clip(x, 0.0, 1.0)

def ss(a, b, x):
    t = cl((x - a) / (b - a))
    return t * t * (3 - 2 * t)

def _frac(v):
    return v - np.floor(v)

def hsh(i, j, seed=0):
    return _frac(np.sin(i * 127.1 + j * 311.7 + seed * 17.31) * 43758.5453)

def vn(x, z, s, seed=0):
    """value noise 2D 0..1 (giống hàm vn trong artifact khi seed=0)"""
    x = np.asarray(x, dtype=float) / s
    z = np.asarray(z, dtype=float) / s
    i, j = np.floor(x), np.floor(z)
    fx, fz = x - i, z - j
    fx = fx * fx * (3 - 2 * fx)
    fz = fz * fz * (3 - 2 * fz)
    a, b = hsh(i, j, seed), hsh(i + 1, j, seed)
    c, d = hsh(i, j + 1, seed), hsh(i + 1, j + 1, seed)
    return a + (b - a) * fx + (c - a) * fz + (a - b - c + d) * fx * fz

def hsh3(i, j, k, seed):
    return _frac(np.sin(i * 127.1 + j * 311.7 + k * 74.7 + seed * 13.13) * 43758.5453)

def vn3(x, y, z, s, seed):
    """value noise 3D 0..1"""
    x = np.asarray(x, dtype=float) / s
    y = np.asarray(y, dtype=float) / s
    z = np.asarray(z, dtype=float) / s
    i, j, k = np.floor(x), np.floor(y), np.floor(z)
    fx, fy, fz = x - i, y - j, z - k
    fx = fx * fx * (3 - 2 * fx)
    fy = fy * fy * (3 - 2 * fy)
    fz = fz * fz * (3 - 2 * fz)
    out = 0.0
    for di in (0, 1):
        wx = fx if di else 1 - fx
        for dj in (0, 1):
            wy = fy if dj else 1 - fy
            for dk in (0, 1):
                wz = fz if dk else 1 - fz
                out = out + wx * wy * wz * hsh3(i + di, j + dj, k + dk, seed)
    return out

# ------------------------------------------------------------------ hình đảo
def _wrap(a):
    return (a + np.pi) % (2 * np.pi) - np.pi

def edge_r(x, z):
    """bán kính mép đảo theo hướng (x, z): 104..117, phình về phía nam (thềm xuất hiện)"""
    th = np.arctan2(z, x)
    n = vn(np.cos(th) * 14 + 40, np.sin(th) * 14 + 40, 4, seed=3)
    e = R_BASE + (n - 0.5) * 12 + 2.5 * np.sin(2 * th + 0.7) + 5 * np.exp(-(_wrap(th - np.pi / 2) / 0.45) ** 2)
    return np.clip(e, 104, 117)

def bottom_y(x, z):
    """đáy đảo: sâu nhất ~-59 ở tâm, lên ~-7 sát mép; dưới phòng boss (r<=70) luôn <= -56"""
    r = np.hypot(x, z)
    q = r / edge_r(x, z)
    b = np.floor(-56 + q ** 3 * 50 + (vn(x, z, 11, seed=4) - 0.5) * 6)
    return np.minimum(b, np.floor(-56 + 45 * ss(70, 96, r))).astype(int)

# ------------------------------------------------------------------ Layer 5: phòng boss "Cổ Thành Câm Lặng" (port từ artifact)
BOSS = dict(
    R=60, floor=-44, apex=-2, wall_top=-14,
    spawn=dict(x=0, z=50, r=6, top=-41),
    road=dict(x1=-3, x2=3, z1=-13, z2=44),
    hall=dict(x=0, z=-27, r=14),
    diag=[((-10, -19), (-50, 21)), ((10, -19), (50, 21))],
    dais=dict(x1=-29, z1=-51, x2=29, z2=-27),
    gate=dict(x1=-20, x2=20, z1=-48, z2=-44),
    plinth=-36,                     # mặt gờ bậc 3 dưới chân cổng; hàng y=0 của bản vẽ cổng là Y-35
    fire=dict(x1=-13, x2=13, z=-40),
    colZ=[38, 30, 22, 14, 6, -2], colX=7, colBroken=[1, 8, 10],
    big=[(-8, -9), (8, -9), (-19, -23), (19, -23)],
    lamps=[(-25, -11), (-34, -3), (-48, 12), (25, -11), (34, -3), (48, 12), (-30, 28), (40, 24)],
    pillars=[(-34, -33, 4), (-45, 3, 4), (36, -14, 4), (-20, 30, 4), (26, 36, 4)],
    S=[("T1", -47, -23, -37, -13), ("T2", 33, -35, 43, -25), ("T3", -44, 25, -34, 35),
       ("H1", -25, 4, -13, 12), ("H2", -36, 14, -24, 22), ("H3", -22, 38, -14, 48), ("H4", 44, -12, 56, -4),
       ("M", 13, 12, 35, 28), ("N", 12, 40, 18, 44), ("N", 38, 30, 44, 34), ("N", -50, -6, -44, -2),
       ("G", -54, 18, -46, 24), ("G", 46, 18, 54, 24)],
    walls=[(216, 234), (172, 188), (2, 13), (58, 75)],     # cung tường thành (độ, atan2(z, x)) ở r 54..58
    seal_floor=-52,                 # tấm bedrock dưới phòng boss
    seal_r=(66.5, 68.2),            # vách bedrock trụ quanh phòng boss, nối lên tấm Y3
)

def boss_wob(a):
    return 60 + 1.6 * np.sin(5 * a + 0.7) + 1.1 * np.sin(11 * a)

def boss_ceil(r):
    """vòm thấp kiểu deep dark: Y-2 ở giữa, Y-14 sát vách"""
    return -14 + 12 * np.cos(np.minimum(r, 60) / 60 * np.pi / 2) ** 0.6

def z_top(x):
    """mép bắc của bãi mỏ hình chuông"""
    return 78 - 48 * np.exp(-((x + 4) / 26) ** 2)

NOTCH_X, NOTCH_Z = 35, 84           # hõm đường leo: |x| < 35, z > 84

def zone(x, z):
    """'notch' (hõm đường leo + thềm), 'yard' (bãi mỏ), 'mountain'"""
    x, z = np.asarray(x), np.asarray(z)
    out = np.where(z >= z_top(x), "yard", "mountain").astype(object)
    out[(z > NOTCH_Z) & (np.abs(x) < NOTCH_X)] = "notch"
    return out

def mountain_h(x, z):
    """mặt núi 5 bậc thềm + khối giữa + 2 khe chéo + vành rìa (port từ artifact, R theo mép thật)"""
    x, z = np.asarray(x, dtype=float), np.asarray(z, dtype=float)
    r = np.hypot(x, z)
    d = (z_top(x) - z) * 0.8
    p = cl((78 - z) / 170)
    k = np.minimum(p * 5, 4.999)
    i = np.floor(k)
    f = k - i
    base = 126 + 8 * (i + ss(0.72, 1, f))
    den = np.maximum(((z + 92) / 167) * 58 + 4, 1.0)
    au = np.abs((x + 3) / den)
    spine = 8 * (1 - 0.5 * p) * (1 - ss(0.6, 1, au))
    gully = -5 * np.exp(-((au - 1.05) / 0.12) ** 2)
    rf = ss(0.70, 0.96, r / edge_r(x, z))
    rim = rf * (10 + 14 * p)
    n = (vn(x, z, 14) - 0.5) * 2 * (1 + 4.5 * rf) + (vn(x, z, 5) - 0.5) * 1.2
    return np.round(120 + (base + spine + gully + rim + n - 120) * ss(0, 3, d)).astype(int)

def yard_h(x, z):
    return (YARD_Y + np.round((vn(x, z, 6) - 0.5) * 1.6)).astype(int)

# ------------------------------------------------------------------ đường leo chữ chi trong hõm
# 4 nhánh rộng 3 chạy theo trục x, mỗi nhánh cao dần; giữa 2 nhánh là 1 hàng lan can (tường thấp).
#   thềm z>=101 (Y98) | lan can z=100 | nhánh0 z97-99 | lan can 96 | nhánh1 z93-95 | 92 | nhánh2 z89-91 | 88 | nhánh3 z85-87 | bãi mỏ z<=84
LEG_Z = [(97, 99), (93, 95), (89, 91), (85, 87)]
RAIL_Z = [100, 96, 92, 88]
LEG_X = 30                          # nhánh chạy x -30..30, chỗ quay đầu |x| 31..34
PATH_LEN = 180                      # tổng quãng đường leo
PATH_RISE = YARD_Y - TERRACE_Y

def path_y(d):
    return TERRACE_Y + int(math.floor(PATH_RISE * d / PATH_LEN + 1e-9))

def leg_d(k, x):
    """quãng đường đã leo tại ô x của nhánh k (None nếu ô không thuộc nhánh)"""
    if k == 0:
        return -x if -LEG_X <= x <= 0 else (0 if x > 0 else None)
    if k == 1:
        return 30 + (x + 30) if -LEG_X <= x <= LEG_X else None
    if k == 2:
        return 90 + (30 - x) if -LEG_X <= x <= LEG_X else None
    if k == 3:
        return 150 + (x + 30) if -LEG_X <= x <= 0 else (180 if x > 0 else None)

LEG_ASC = {0: "west", 1: "east", 2: "west", 3: "east"}
TURNS = [  # (x lo, x hi, z lo, z hi, d)
    (-34, -31, 93, 99, 30),
    (31, 34, 89, 95, 90),
    (-34, -31, 85, 91, 150),
]

def notch_cell(x, z):
    """('terrace'|'leg'|'turn'|'rail', y khối mặt, nhánh k hoặc None)"""
    for (xa, xb, za, zb, d) in TURNS:
        if xa <= x <= xb and za <= z <= zb:
            return "turn", path_y(d), None
    if z >= 101:
        return "terrace", TERRACE_Y, None
    for k, (za, zb) in enumerate(LEG_Z):
        if za <= z <= zb:
            return "leg", path_y(leg_d(k, x)), k
    return "rail", None, None

# ------------------------------------------------------------------ hang dẫn
TUNNEL = [(-4, 30), (-10, 14), (6, -2), (12, -20), (-8, -34), (-12, -48), (0, -62)]
PIT = dict(x=0, z=-62, r=12, top=118, dome=6, bottom=F[2])

def tunnel_len():
    return sum(math.hypot(TUNNEL[i][0] - TUNNEL[i - 1][0], TUNNEL[i][1] - TUNNEL[i - 1][1]) for i in range(1, len(TUNNEL)))

def tunnel_floor(s):
    return YARD_Y - (YARD_Y - TUNNEL_END_Y) * s

def tunnel_height(s):
    return 13 - 3 * np.sin(np.pi * s)

def tunnel_halfw(s):
    return 8.5 - 2.0 * ss(0, 0.15, s)

def nearest_on_polyline(px, pz, pts):
    """trả (s 0..1 theo chiều dài, khoảng cách, chỉ số đoạn, hướng đoạn) cho mảng điểm"""
    px, pz = np.asarray(px, dtype=float), np.asarray(pz, dtype=float)
    best_d = np.full(px.shape, np.inf)
    best_s = np.zeros(px.shape)
    best_seg = np.zeros(px.shape, dtype=int)
    L = sum(math.hypot(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1]) for i in range(1, len(pts)))
    acc = 0.0
    for i in range(1, len(pts)):
        ax, az = pts[i - 1]
        bx, bz = pts[i]
        vx, vz = bx - ax, bz - az
        l2 = vx * vx + vz * vz
        t = np.clip(((px - ax) * vx + (pz - az) * vz) / l2, 0, 1)
        dx, dz = px - (ax + t * vx), pz - (az + t * vz)
        dist = np.hypot(dx, dz)
        m = dist < best_d
        best_d[m] = dist[m]
        best_s[m] = (acc + t[m] * math.sqrt(l2)) / L
        best_seg[m] = i - 1
        acc += math.sqrt(l2)
    return best_s, best_d, best_seg

# ------------------------------------------------------------------ 3 tầng mỏ (port nguyên từ artifact)
# rooms: [x1, z1, x2, z2, cao, loại, tên, ghi chú]
LAYERS = [
    dict(id=2, key="l2", name="Hầm Mỏ Cũ", floor=F[2],
         rooms=[[-14, -40, 14, -18, 9, "combat", "Trạm Tời", "Sảnh đầu tầng, tời kéo và lồng thang gãy giữa phòng."],
                [-52, -46, -28, -26, 7, "rest", "Lán Công Nhân", "Giường tầng, bàn ăn, lò sưởi tắt."],
                [30, -44, 54, -26, 7, "store", "Kho Dụng Cụ", "Giá cuốc xẻng, thùng, dây thừng."],
                [-50, -8, -24, 14, 9, "combat", "Xưởng Rèn Nguội", "Lò rèn tắt, đe, máng nước."],
                [24, -10, 52, 14, 10, "combat", "Phòng Sàng Quặng", "Băng chuyền gỗ và phễu sàng."],
                [-10, 2, 10, 18, 7, "reward", "Kho Quản Đốc", "Phòng rương thưởng."]],
         cors=[[(0, -62), (0, -40)], [(-14, -30), (-28, -34)], [(14, -30), (30, -34)], [(-40, -26), (-44, -16), (-38, -8)],
               [(42, -26), (46, -18), (38, -10)], [(-36, 14), (-34, 26), (-26, 36)], [(38, 14), (36, 26), (28, 36)],
               [(-24, 8), (-10, 10)], [(0, -18), (0, -7)]],
         dead=[(0, -7)],
         cavern=dict(x=0, z=52, rx=46, rz=28, ph=0.6, name="Hang Mạch Lộ"),
         down=dict(x=-22, z=62, kind="well", name="Giếng thang gỗ chữ chi")),
    dict(id=3, key="l3", name="Rừng Thạch Nhũ", floor=F[3],
         rooms=[[-30, 54, -14, 70, 8, "entry", "Chân Thang", "Điểm tới của giếng thang, ngã ba đi 3 hướng."],
                [-62, 30, -36, 52, 9, "combat", "Trạm Bơm Nước", "Nửa phòng ngập, máy bơm và ống đồng."],
                [4, 44, 26, 64, 7, "store", "Kho Thuốc Nổ", "Thùng xếp hàng, tường gia cố."],
                [-20, 14, 6, 34, 8, "rest", "Nhà Ăn Tập Thể", "Bàn dài, bếp, kệ bát. Checkpoint giữa tầng."],
                [36, 14, 64, 40, 10, "combat", "Hầm Sập", "Trần sập một góc, đá tảng làm vật cản."],
                [-66, -6, -44, 12, 7, "reward", "Phòng Khảo Sát", "Bàn bản đồ, mẫu đá, két."],
                [-24, -16, 14, 2, 10, "combat", "Bãi Xe Goòng", "Ba tuyến ray, xe goòng. Chắn cửa hang."]],
         cors=[[(-30, 58), (-36, 46)], [(-14, 62), (4, 56)], [(-20, 54), (-16, 44), (-10, 34)], [(-52, 30), (-56, 20), (-54, 12)],
               [(26, 50), (40, 48), (48, 40)], [(-6, 14), (-4, 2)], [(-44, -2), (-24, -6)], [(50, 14), (54, -8), (44, -38)],
               [(-4, -16), (0, -28)]],
         dead=[],
         cavern=dict(x=8, z=-50, rx=50, rz=26, ph=2.1, name="Động Nhũ Thạch"),
         down=dict(x=30, z=-56, kind="sinkhole", name="Hố sụt có thác")),
    dict(id=4, key="l4", name="Vực Deepslate", floor=F[4],
         rooms=[[20, -66, 40, -46, 10, "entry", "Hồ Rơi", "Hồ sâu 4 block đón thác từ tầng 3."],
                [48, -40, 72, -18, 8, "combat", "Trạm Gác Sâu", "Chòi canh, rào chắn, giá vũ khí."],
                [-14, -44, 14, -20, 10, "combat", "Lò Luyện", "Lò nung lớn, máng dung nham giữa phòng."],
                [-58, -50, -34, -30, 7, "rest", "Hầm Trú Ẩn", "Giường, nhật ký. Checkpoint trước thủ vệ."],
                [-66, -12, -44, 8, 8, "reward", "Kho Niêm Phong", "Rương thưởng."],
                [30, -4, 54, 18, 12, "combat", "Giếng Thang Gãy", "Phòng cao, lồng thang rơi vỡ giữa nền."],
                [-12, -4, 12, 12, 9, "store", "Cổng Niêm Phong", "Tiền sảnh, kho tiếp tế trước thủ vệ."]],
         cors=[[(40, -52), (54, -48), (60, -40)], [(20, -54), (8, -50), (4, -44)], [(-14, -36), (-34, -40)],
               [(-48, -30), (-52, -20), (-54, -12)], [(58, -18), (52, -10), (44, -4)], [(0, -20), (0, -4)],
               [(-44, 2), (-12, 4)], [(30, 6), (12, 4)], [(0, 12), (0, 26)]],
         dead=[],
         cavern=dict(x=0, z=50, rx=48, rz=28, ph=4.0, name="Lõi Kvelsgard"),
         down=None),
]

def blob_rho(c, x, z):
    """khoảng cách chuẩn hóa tới tâm hang (<=1 là bên trong), theo đúng hàm blob của artifact"""
    dx, dz = (np.asarray(x, dtype=float) - c["x"]) / c["rx"], (np.asarray(z, dtype=float) - c["z"]) / c["rz"]
    t = np.arctan2(dz, dx)
    k = 1 + 0.07 * np.sin(3 * t + c["ph"]) + 0.04 * np.sin(5 * t + 1.3 * c["ph"]) + 0.03 * np.sin(9 * t)
    return np.hypot(dx, dz) / k
