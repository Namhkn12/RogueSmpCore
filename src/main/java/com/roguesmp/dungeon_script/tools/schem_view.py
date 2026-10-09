"""Xem nhanh một schematic: in thông tin + vẽ ảnh 4 khung (nhìn từ trên, 2 mặt cắt đứng, isometric).

  python tools/schem_view.py <file.schem|.litematic> [ảnh.png] [--cut Y] [--open] [--x X] [--z Z]

  --cut Y   chỉ xét khối có y <= Y (tọa độ thật nếu file có Offset), để nhìn xuyên mái/trần
  --open    bóc 2 lớp tường phía trước (phía +x, +z) trong hình isometric để thấy bên trong
  --x/--z   vị trí mặt cắt đứng (mặc định: giữa công trình)
"""
import sys, os, zlib, argparse
from collections import Counter
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from schem_io import read_schem

# ------------------------------------------------------------------ màu theo tên khối
COLORS = [  # (từ khóa, màu) — khớp từ khóa đầu tiên
    ("air", None), ("structure_void", None), ("cave_air", None), ("barrier", None), ("light[", None),
    ("water", (60, 110, 220)), ("lava", (240, 110, 20)), ("bedrock", (40, 40, 40)),
    ("red_carpet", (170, 30, 35)), ("yellow_carpet", (225, 190, 40)), ("carpet", (120, 140, 70)),
    ("gold", (240, 200, 60)), ("quartz", (236, 230, 222)), ("calcite", (225, 225, 220)),
    ("diorite", (200, 200, 200)), ("andesite", (140, 142, 140)), ("smooth_stone", (165, 165, 165)),
    ("froglight", (245, 235, 200)), ("sea_lantern", (190, 225, 220)), ("shroomlight", (240, 150, 70)),
    ("glowstone", (250, 215, 120)), ("lantern", (250, 190, 90)), ("torch", (255, 200, 80)), ("candle", (250, 220, 150)),
    ("crying_obsidian", (90, 30, 150)), ("obsidian", (30, 20, 45)), ("lodestone", (120, 120, 130)),
    ("blackstone", (50, 45, 55)), ("deepslate", (75, 75, 82)), ("tuff", (110, 112, 100)),
    ("mossy", (100, 125, 85)), ("moss", (90, 130, 50)), ("cobblestone", (120, 120, 120)),
    ("stone_brick", (125, 125, 125)), ("brick", (150, 80, 65)), ("stone", (128, 128, 128)),
    ("azalea_leaves", (85, 130, 45)), ("leaves", (60, 120, 40)), ("azalea", (95, 140, 50)),
    ("vine", (50, 110, 30)), ("fern", (70, 130, 50)), ("grass", (90, 150, 60)), ("lily", (40, 120, 40)),
    ("seagrass", (40, 120, 60)), ("cave_vines", (90, 120, 40)), ("roots", (110, 85, 60)),
    ("log", (100, 75, 45)), ("planks", (160, 120, 75)), ("wood", (110, 80, 50)),
    ("dirt", (125, 90, 60)), ("mud", (80, 70, 60)), ("clay", (160, 165, 180)), ("sand", (220, 210, 160)),
    ("gravel", (135, 128, 125)), ("iron", (215, 215, 215)), ("chain", (70, 75, 85)),
    ("wool", (220, 220, 220)), ("glass", (200, 230, 240)), ("prismarine", (90, 160, 150)),
    ("amethyst", (150, 100, 200)), ("dripstone", (140, 110, 90)), ("chest", (160, 110, 40)),
]
def color_of(name):
    n = name.replace("minecraft:", "")
    for k, c in COLORS:
        if k in n: return c
    h = zlib.crc32(n.split("[")[0].encode())
    return (80 + h % 150, 80 + (h >> 8) % 150, 80 + (h >> 16) % 150)

def palette_rgb(names):
    rgb = np.zeros((len(names), 3), np.float32); solid = np.zeros(len(names), bool)
    for i, n in enumerate(names):
        c = color_of(n)
        if c is not None:
            rgb[i] = np.array(c) / 255.0; solid[i] = True
    return rgb, solid

# ------------------------------------------------------------------ các góc nhìn
def top_view(W, solid, rgb, cut_j):
    """nhìn từ trên xuống, chỉ xét y <= cut_j: màu khối đặc cao nhất, tối dần theo độ sâu"""
    sub = solid[W[:, :cut_j + 1, :]]
    has = sub.any(axis=1)
    top = sub.shape[1] - 1 - np.argmax(sub[:, ::-1, :], axis=1)
    ids = np.take_along_axis(W[:, :cut_j + 1, :], top[:, None, :], axis=1)[:, 0, :]
    img = rgb[ids] * (0.55 + 0.45 * (top / max(1, cut_j)))[..., None]
    img[~has] = 1.0
    return np.transpose(img, (1, 0, 2))           # hàng = z, cột = x

def section(W, solid, rgb, axis, k):
    """mặt cắt đứng: axis 'x' -> lát x=k (trục ngang z), 'z' -> lát z=k (trục ngang x)"""
    sl = W[k, :, :] if axis == "x" else W[:, :, k].T
    img = rgb[sl].copy(); img[~solid[sl]] = 1.0     # [y, ngang]
    return img[::-1]                                  # y lớn ở trên

def iso_view(W, solid, rgb, cut_j, open_front):
    """isometric nhìn từ phía +x, +z, trên cao xuống; mỗi khối 4x4 px (mặt trên sáng, 2 mặt bên tối)"""
    S = solid[W].copy()
    S[:, cut_j + 1:, :] = False
    if open_front:
        S[-2:, :, :] = False; S[:, :, -2:] = False
    nx, ny, nz = S.shape
    # chỉ vẽ khối có ít nhất 1 mặt lộ về phía người xem (+x, +y, +z)
    vis = S.copy()
    exp = np.zeros_like(S)
    exp[:-1] |= ~S[1:]; exp[-1] = True
    exp[:, :-1] |= ~S[:, 1:]; exp[:, -1] = True
    exp[:, :, :-1] |= ~S[:, :, 1:]; exp[:, :, -1] = True
    vis &= exp
    xs, ys, zs = np.nonzero(vis)
    d = xs + zs + ys
    o = np.argsort(d, kind="stable")
    xs, ys, zs = xs[o], ys[o], zs[o]
    col = rgb[W[xs, ys, zs]]
    sx = (xs - zs) * 2 + 2 * nz
    sy = (xs + zs) - ys * 2 + 2 * ny
    H, Wd = 2 * ny + nx + nz + 4, 2 * (nx + nz) + 4
    img = np.ones((H, Wd, 3), np.float32)
    for dy in range(4):
        for dx in range(4):
            shade = 1.0 if dy < 2 else (0.72 if dx < 2 else 0.55)
            img[sy + dy, sx + dx] = col * shade
    return img

# ------------------------------------------------------------------ main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("file"); ap.add_argument("out", nargs="?")
    ap.add_argument("--cut", type=int); ap.add_argument("--open", action="store_true")
    ap.add_argument("--x", type=int); ap.add_argument("--z", type=int)
    a = ap.parse_args()
    s = read_schem(a.file)
    W, names, (ox, oy, oz) = s.W, s.names, s.offset
    nx, ny, nz = W.shape
    rgb, solid = palette_rgb(names)
    cnt = Counter()
    for i, c in zip(*np.unique(W, return_counts=True)):
        cnt[names[i]] += int(c)
    print(f"{os.path.basename(a.file)}: {nx}×{ny}×{nz} (x×y×z), gốc {ox},{oy},{oz}, {len(names)} trạng thái khối")
    print("khối nhiều nhất:")
    for n, c in cnt.most_common(25):
        print(f"  {c:>8}  {n}")
    cut_j = ny - 1 if a.cut is None else max(0, min(ny - 1, a.cut - oy))
    kx = nx // 2 if a.x is None else a.x - ox
    kz = nz // 2 if a.z is None else a.z - oz
    import matplotlib; matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    fig = plt.figure(figsize=(18, 13))
    ax = fig.add_subplot(2, 2, 1); ax.imshow(top_view(W, solid, rgb, cut_j), interpolation="nearest",
        extent=(ox - .5, ox + nx - .5, oz + nz - .5, oz - .5))
    ax.set_title(f"Nhìn từ trên (y ≤ {oy + cut_j}) · ngang x, dọc z"); ax.set_xlabel("x"); ax.set_ylabel("z")
    ax = fig.add_subplot(2, 2, 2); ax.imshow(section(W, solid, rgb, "x", kx), interpolation="nearest",
        extent=(oz - .5, oz + nz - .5, oy - .5, oy + ny - .5))
    ax.set_title(f"Mặt cắt đứng x = {ox + kx} · ngang z, dọc y"); ax.set_xlabel("z"); ax.set_ylabel("y")
    ax = fig.add_subplot(2, 2, 3); ax.imshow(section(W, solid, rgb, "z", kz), interpolation="nearest",
        extent=(ox - .5, ox + nx - .5, oy - .5, oy + ny - .5))
    ax.set_title(f"Mặt cắt đứng z = {oz + kz} · ngang x, dọc y"); ax.set_xlabel("x"); ax.set_ylabel("y")
    ax = fig.add_subplot(2, 2, 4); ax.imshow(iso_view(W, solid, rgb, cut_j, a.open), interpolation="nearest")
    ax.set_title("Isometric (nhìn từ +x +z)" + (" · bóc tường trước" if a.open else "")); ax.axis("off")
    plt.tight_layout()
    out = a.out or os.path.splitext(a.file)[0] + "_view.png"
    plt.savefig(out, dpi=90); print("ảnh:", out)

if __name__ == "__main__":
    main()
