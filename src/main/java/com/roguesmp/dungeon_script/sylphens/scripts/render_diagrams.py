#!/usr/bin/env python3
"""
Vẽ 3 sơ đồ tổng quan (không cần chạy bộ sinh Layer 2, chỉ đọc output/layer2_rooms.json):
  images/cross_section.png     mặt cắt đứng toàn đảo (Y thật)
  images/surface_heightmap.png sơ đồ địa hình mặt đảo nhìn từ trên (theo công thức H, CHƯA tính noise)
  images/layer1_plan.png       sơ đồ Layer 1 nhìn từ trên (noise MINH HỌA, trong game FAWE perlin sẽ khác)
Chạy: python3 scripts/render_diagrams.py
Mọi thông số lấy từ generate_world_commands.py; nếu đổi công thức ở đó thì sửa tương ứng ở đây.
"""
import json, math
from pathlib import Path
import numpy as np
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle, Circle

ROOT = Path(__file__).resolve().parents[1]
IMG = ROOT / "images"; IMG.mkdir(exist_ok=True)
rooms = json.load(open(ROOT / "output" / "layer2_rooms.json", encoding="utf-8"))
F = {int(k): v for k, v in rooms["floors"].items()}
MAZE_FL = rooms.get("maze_floor")
SEAL = [F[k] - 4 if k == MAZE_FL else F[k] - 2 for k in (1, 2, 3, 4)]   # khớp SEAL_Y trong generate_layer2.py
BOSS = dict(x1=-22, x2=22, z1=-50, z2=46, floor=-56, top=-29)

def surf_h(r, n=0.0):
    m = np.minimum(np.minimum(np.maximum((r - 68) / 12, 0), np.maximum((100 - r) / 12, 0)), 1)
    return 88 + r * 0.2 + n * 2 + m ** 0.7 * (62 + n * 18)

def island_bottom(r):
    return -60 + (r / 100) ** 3 * 55

# ------------------------------------------------------------------ 1) mặt cắt
fig, ax = plt.subplots(figsize=(15, 9.5))
r = np.linspace(0, 99, 400)
hh = surf_h(r); bot = island_bottom(r)
for s in (1, -1):
    ax.fill_between(s * r, bot, hh, color="#d8cfc0")                       # đá thường
    ax.fill_between(s * r, bot, np.minimum(hh, 60), color="#8c8c94")        # deepslate
ax.text(-96, 52, "Y ≤ 60: deepslate\n(plugin cấm phá)", fontsize=9, color="white")
# hố sụt
ys = np.arange(80, 101)
fr = lambda y: 13 + max(0, y - 80) * 0.25
ax.fill_betweenx(ys, [-fr(y) for y in ys], [fr(y) for y in ys], color="#e6f4ff")
ax.text(0, 90, "hố sụt", ha="center", fontsize=9)
# Layer 1
ax.fill_between([-82, 82], [64, 64], [80, 80], color="#a6d9a0")
ax.text(-78, 74, "Layer 1 — hang Lush (sàn ~64, trần ~80)", fontsize=9)
ax.add_patch(Rectangle((-19, 61), 38, 3.5, color="#4a90e2"))
ax.add_patch(Rectangle((-7, 61), 14, 5, color="#777"))
ax.add_patch(Rectangle((-3.5, 47), 7, 19, color="#4a90e2", alpha=0.55))
ax.text(5, 56, "ống r3.5", fontsize=8)
# Layer 2
RMAX = {1: 84, 2: 84, 3: 82, 4: 78, 5: 66}
for k, y in F.items():
    if k == MAZE_FL:
        ax.add_patch(Rectangle((-89, y - 4), 178, 14, color="#222"))
        ax.add_patch(Rectangle((-80, y + 1), 160, 5, color="#7a6a53"))
        ax.text(RMAX[k] + 2, y + 3, f"Tầng {k}: MÊ CUNG sàn Y{y} (đường cao 5, khối bedrock Y{y-4}..Y{y+9})", fontsize=8.5, va="center")
        continue
    tallest = max((rm["y_ceil"] for rm in rooms["rooms"] if rm["floor"] == k and rm["kind"] not in ("atrium",)), default=y + 6)
    ax.add_patch(Rectangle((-RMAX[k], y + 1), 2 * RMAX[k], 4, color="#e8a87c"))
    ax.add_patch(Rectangle((-RMAX[k] * 0.55, y + 1), RMAX[k] * 0.3, tallest - y - 1, color="#e8a87c", alpha=0.45))
    ax.text(RMAX[k] + 2, y + 3, f"Tầng {k}: sàn Y{y} (hành lang cao 4, phòng tới Y{tallest})", fontsize=8.5, va="center")
for y in SEAL:
    ax.plot([-100, 100], [y, y], color="#111", lw=2)
    ax.text(-99, y - 3.2, f"bedrock Y{y}", fontsize=7.5)
for rm in rooms["rooms"]:
    if rm["kind"] == "atrium":
        ax.add_patch(Rectangle((-60, rm["y_floor"] + 1), 14, rm["y_ceil"] - rm["y_floor"] - 1, color="#5bc0de", alpha=0.8))
for rm in rooms["rooms"]:
    if rm["kind"] == "atrium":
        ax.text(-53, rm["y_ceil"] + 2, "giếng\ntrời", ha="center", fontsize=7.5)
ax.add_patch(Rectangle((-15, F[1] + 1), 30, 10, color="#c9a227")); ax.text(0, F[1] + 5, "Sảnh Chính", ha="center", fontsize=8)
ax.add_patch(Rectangle((-14, F[5] + 1), 28, 12, color="#b03060")); ax.text(0, F[5] + 6, "Thủ Vệ", ha="center", fontsize=8, color="w")
# boss
bc = json.load(open(ROOT / "output" / "boss_cavern.json", encoding="utf-8"))
rr = np.linspace(-46, 46, 200)
dome = bc["floor_y"] + 7 + (-25 - bc["floor_y"] - 7) * np.clip(1 - (rr / 46) ** 2, 0, 1) ** 0.45
ax.fill_between(rr * 1.13, bc["floor_y"] - 2, dome + 3, color="#111")
ax.fill_between(rr, bc["floor_y"], dome, color="#6b1d1d")
ax.add_patch(Rectangle((-bc["platform"]["r"], bc["floor_y"]), 2 * bc["platform"]["r"], 3, color="#3c8a3c"))
ax.text(0, -40, "Layer 3 — Boss (hang vòm r46)\nnền Y-61, sàn tròn Y-58, vòm Y-25\nvỏ deepslate + bedrock, chỉ vào bằng dịch chuyển",
        ha="center", va="center", color="w", fontsize=8.5)
# barrier
for s in (1, -1):
    ax.add_patch(Rectangle((s * 101.75 - 1.25, -64), 2.5, 330, color="#e74c3c", alpha=0.35))
ax.axhline(-64, color="#e74c3c", lw=2, alpha=0.6)
ax.text(-100, -70, "barrier: tường r100.5–103 từ Y−64 → 319, sàn Y−64, nắp Y319", color="#c0392b", fontsize=9)
ax.annotate("núi vây kín (đỉnh ~150–185)", xy=(84, 160), xytext=(30, 185), arrowprops=dict(arrowstyle="->"), fontsize=9)
ax.annotate("hang spawn (sườn nam, Y121)", xy=(70, 121), xytext=(105, 135), arrowprops=dict(arrowstyle="->"), fontsize=9)
ax.set_xlim(-108, 160); ax.set_ylim(-76, 200)
ax.set_xlabel("khoảng cách từ tâm (khối)"); ax.set_ylabel("Y")
ax.set_title("Sylphens — mặt cắt đứng (tỉ lệ thật, chưa tính noise)", weight="bold")
plt.tight_layout(); plt.savefig(IMG / "cross_section.png", dpi=105); plt.close()

# ------------------------------------------------------------------ 2) địa hình mặt đảo
N = 401
xs = np.linspace(-104, 104, N)
X, Z = np.meshgrid(xs, xs)
R = np.hypot(X, Z)
Hm = surf_h(R)
Hm = np.where(R <= 99, Hm, np.nan)
crater = R <= 16
Hm = np.where(crater, np.nan, Hm)
fig, ax = plt.subplots(figsize=(11, 10))
im = ax.imshow(Hm, extent=(-104, 104, 104, -104), cmap="terrain", vmin=60, vmax=200)
cs = ax.contour(X, Z, np.where(np.isnan(Hm), 0, Hm), levels=[95, 100, 110, 130, 150], colors="k", linewidths=0.5, alpha=0.5)
ax.clabel(cs, fontsize=7, fmt="Y%d")
ax.add_patch(Circle((0, 0), 16, color="#1f3b5c")); ax.text(0, 0, "HỐ SỤT\nr13–16", ha="center", va="center", color="w", fontsize=9, weight="bold")
surf = json.load(open(ROOT / "output" / "surface.json", encoding="utf-8"))
rv = surf["river"]
zr = np.arange(rv["source_pool"]["z"], rv["end"]["z"] + 1)
ax.plot(4 * np.sin(zr / 9.0), zr, color="#2a7fff", lw=5)              # khớp river_cx() trong generate_surface.py
ax.text(8, rv["source_pool"]["z"], "sông (chờ thác từ núi)", fontsize=8.5)
ax.add_patch(Circle((0, 0), 100.5, fill=False, color="#e74c3c", lw=2))
ax.add_patch(Circle((0, 0), 103, fill=False, color="#e74c3c", lw=1, ls="--"))
sp = surf["spawn"]
ax.plot(sp["x"], sp["z"], "*", color="gold", ms=18, mec="k"); ax.text(4, sp["z"] + 4, f"hang spawn ({sp['x']},{sp['y']},{sp['z']})", fontsize=8.5)
ax.text(0, -96, "BẮC (z−)", ha="center", fontsize=9); ax.text(97, 0, "ĐÔNG", ha="right", fontsize=9)
cb = plt.colorbar(im, ax=ax, shrink=0.7); cb.set_label("Y mặt đất")
ax.set_title("Mặt đảo — địa hình nhìn từ trên (minh họa, ảnh chính xác: surface_plan.png)\nxanh: sông phía bắc, sao vàng: hang spawn, đỏ: barrier",
             fontsize=11, weight="bold")
ax.set_xlabel("x"); ax.set_ylabel("z")
plt.tight_layout(); plt.savefig(IMG / "surface_heightmap.png", dpi=100); plt.close()

# ------------------------------------------------------------------ 3) Layer 1 (minh họa)
def vnoise(scale, seed):
    rng = np.random.default_rng(seed)
    n = int(210 / scale) + 3
    g = rng.uniform(-1, 1, (n, n))
    fx = (X + 105) / scale; fz = (Z + 105) / scale
    ix, iz = fx.astype(int), fz.astype(int); tx, tz = fx - ix, fz - iz
    tx = tx * tx * (3 - 2 * tx); tz = tz * tz * (3 - 2 * tz)
    return (g[ix, iz] * (1 - tx) + g[ix + 1, iz] * tx) * (1 - tz) + (g[ix, iz + 1] * (1 - tx) + g[ix + 1, iz + 1] * tx) * tz
b = vnoise(40, 31) * 0.6        # thu biên độ cho gần phân bố perlin (Rêu nhiều nhất, Pha Lê ít nhất)
small = vnoise(9, 13); big = vnoise(24, 14); wall = vnoise(45, 15)
img = np.ones(X.shape + (3,)) * 0.15
inside = R <= 80 + vnoise(20, 11) * 6
col = {"moss": (0.42, 0.66, 0.33), "drip": (0.62, 0.52, 0.40), "mush": (0.62, 0.35, 0.55), "crys": (0.68, 0.55, 0.88)}
img[inside] = col["moss"]
img[inside & (b < -0.25)] = col["drip"]
img[inside & (b > 0.15)] = col["mush"]
img[inside & (b > 0.42)] = col["crys"]
pillars = inside & (R > 27) & ((small > 0.58) | (big > 0.62))
walls = inside & (R > 30) & (np.abs(wall) < 0.04)
img[pillars | walls] = (0.35, 0.35, 0.38)
center = R <= 24
img[center] = col["moss"]
img[(R > 7.5) & (R <= 19)] = (0.25, 0.55, 0.9)
img[R <= 7] = (0.5, 0.5, 0.5)
img[R <= 3.5] = (0.05, 0.05, 0.05)
fig, ax = plt.subplots(figsize=(11, 10))
ax.imshow(img, extent=(-104, 104, 104, -104))
for t in range(6):
    a = t * math.pi / 3 + math.pi / 6
    ax.plot(14.5 * math.cos(a), 14.5 * math.sin(a), "o", color="#2e7d32", ms=7, mec="w")
ax.add_patch(Circle((0, 0), 100.5, fill=False, color="#e74c3c", lw=2))
leg = [("Rêu (mặc định, lớn nhất)", col["moss"]), ("Thạch Nhũ  b < −0.25", col["drip"]), ("Nấm  b > 0.15", col["mush"]),
       ("Pha Lê  b > 0.42 (nhỏ nhất)", col["crys"]), ("Cột đá / vách ngăn", (0.35, 0.35, 0.38)), ("Hồ tâm r7.5–19 (nước Y61–64)", (0.25, 0.55, 0.9)),
       ("Đảo đá r≤7, ống xuống r≤3.5", (0.5, 0.5, 0.5))]
for i, (t, c) in enumerate(leg):
    ax.add_patch(Rectangle((-102, 58 + i * 6.3), 4.5, 4.5, color=c))
    ax.text(-96, 60.3 + i * 6.3, t, fontsize=7.5, va="center", color="w")
ax.plot([], [], "o", color="#2e7d32", label="dây leo cave vines (6 hướng, r≈14.5)"); ax.legend(loc="lower right", fontsize=8)
ax.set_title("Layer 1 — nhìn từ trên (MINH HỌA bố cục; noise thật trong game do FAWE perlin quyết định)\n"
             "b = perlin(31,x,0,z,0.018,2,0.5); cột: 2 lớp perlin seed 13/14; vách: |perlin(15)| < 0.04", fontsize=10, weight="bold")
ax.set_xlabel("x"); ax.set_ylabel("z")
plt.tight_layout(); plt.savefig(IMG / "layer1_plan.png", dpi=100); plt.close()
print("đã vẽ:", [p.name for p in IMG.iterdir()])
