"""
Kvelsgard — vẽ ảnh kiểm tra từ khối thế giới (gọi ở cuối generate_world.py).

  images/surface.png      độ cao mặt đảo + vùng + đường leo
  images/layer2.png ...   mặt bằng từng tầng mỏ (lát ở sàn+2), khung phòng, ô đã đi bộ tới
  images/sections.png     mặt cắt đứng x=0, x=-22 (giếng), z=-56 (hố sụt)
"""
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle

IMG = Path(__file__).resolve().parents[1] / "images"

CATS = [  # (từ khóa, màu)
    ("structure_void", (1, 1, 1)), (":air", (0.97, 0.96, 0.93)), ("bedrock", (0.05, 0.05, 0.05)),
    ("water", (0.25, 0.5, 0.9)), ("lava", (0.95, 0.45, 0.1)), ("magma", (0.8, 0.3, 0.1)),
    ("sculk", (0.05, 0.3, 0.35)), ("wool", (0.55, 0.55, 0.58)), ("reinforced", (0.15, 0.35, 0.3)), ("ice", (0.6, 0.8, 0.95)),
    ("_ore", (0.95, 0.8, 0.1)), ("snow", (1, 1, 1)), ("grass", (0.4, 0.7, 0.3)), ("podzol", (0.5, 0.4, 0.25)),
    ("dirt", (0.55, 0.42, 0.3)), ("gravel", (0.6, 0.58, 0.56)), ("spruce", (0.45, 0.3, 0.18)), ("dark_oak", (0.3, 0.2, 0.12)),
    ("rail", (0.5, 0.45, 0.4)), ("lantern", (1, 0.85, 0.3)), ("dripstone", (0.65, 0.5, 0.4)), ("calcite", (0.9, 0.9, 0.88)),
    ("granite", (0.65, 0.45, 0.4)), ("brick", (0.45, 0.45, 0.5)), ("tile", (0.3, 0.3, 0.33)), ("polished_deepslate", (0.32, 0.32, 0.35)),
    ("deepslate", (0.25, 0.25, 0.28)), ("basalt", (0.35, 0.35, 0.38)), ("blackstone", (0.18, 0.16, 0.18)), ("tuff", (0.45, 0.47, 0.42)),
    ("andesite", (0.58, 0.58, 0.58)), ("cobble", (0.5, 0.5, 0.5)), ("stone", (0.48, 0.48, 0.48)),
]

def palette_colors(names):
    out = np.zeros((len(names), 3))
    for i, n in enumerate(names):
        out[i] = next((c for k, c in CATS if k in n), (0.6, 0.4, 0.6))
    return out

def render(W, names, ctx):
    IMG.mkdir(exist_ok=True)
    X0, Y0, Z0 = ctx["X0"], ctx["Y0"], ctx["Z0"]
    HT, ISL, seen = ctx["HT"], ctx["ISL"], ctx["seen"]
    col = palette_colors(names)
    is_open = np.array([n == "minecraft:air" or any(k in n for k in ("water", "lava", "rail", "lantern", "cobweb", "lichen", "pointed", "vines",
                                                "ladder", "structure_void")) for n in names])
    ext = [X0 - 0.5, X0 + W.shape[0] - 0.5, Z0 + W.shape[2] - 0.5, Z0 - 0.5]

    # ---- mặt đảo
    fig, ax = plt.subplots(figsize=(10, 10))
    h = np.where(ISL, HT, np.nan).astype(float)
    im = ax.imshow(h.T, extent=ext, cmap="terrain", vmin=80, vmax=210)
    ax.contour(np.arange(X0, X0 + W.shape[0]), np.arange(Z0, Z0 + W.shape[2]), np.where(ISL, HT, 0).T,
               levels=np.arange(96, 210, 8), colors="k", linewidths=0.3)
    plt.colorbar(im, ax=ax, shrink=0.7, label="Y mặt đất")
    sp = ctx["SPAWN"]
    ax.plot(sp[0], sp[2], "r*", ms=14)
    ax.set_title("Kvelsgard — mặt đảo (bắc ở trên, đường đồng mức 8 khối, sao đỏ = điểm xuất hiện)")
    fig.savefig(IMG / "surface.png", dpi=110, bbox_inches="tight")
    plt.close(fig)

    # ---- mặt bằng từng tầng
    for L in ctx["layers"]:
        Fl = L["floor"]
        top = np.zeros((W.shape[0], W.shape[2], 3))
        sl = W[:, Fl + 2 - Y0, :]
        flo = W[:, Fl - Y0, :]
        op = is_open[sl]
        top[:] = 0.22
        img = np.where(op[..., None], col[flo] * 0.6 + 0.4, col[sl] * 0.5)
        for dy in (-1, 0, 1, 2, 3, 4, 5, 6):
            s2 = W[:, Fl + dy - Y0, :]
            liquid = np.isin(s2, [i for i, n in enumerate(names) if "water" in n or "lava" in n])
            img[liquid & op] = col[s2][liquid & op]
        reach = seen[:, Fl - 3 - Y0:Fl + 12 - Y0, :].any(axis=1)
        img[reach] = img[reach] * 0.75 + np.array([0.2, 0.75, 0.3]) * 0.25
        bed = (W[:, Fl - 3 - Y0, :] == names.index("minecraft:bedrock")) if L["id"] in (2, 3) else None
        fig, ax = plt.subplots(figsize=(11, 11))
        ax.imshow(np.transpose(img, (1, 0, 2)), extent=ext)
        for r in L["rooms"]:
            ax.add_patch(Rectangle((r[0] - 0.5, r[1] - 0.5), r[2] - r[0], r[3] - r[1], fill=False, ec="crimson", lw=1.2))
            ax.text((r[0] + r[2]) / 2, (r[1] + r[3]) / 2, r[6], ha="center", va="center", fontsize=7, color="crimson")
        c = L["cavern"]
        ax.text(c["x"], c["z"], c["name"], ha="center", fontsize=9, color="navy", weight="bold")
        ax.set_xlim(-120, 120); ax.set_ylim(120, -120)
        ax.set_title(f"Tầng {L['id']} — {L['name']} (lát Y{Fl + 2}; xanh lá = đã đi bộ tới)")
        fig.savefig(IMG / f"layer{L['id']}.png", dpi=110, bbox_inches="tight")
        plt.close(fig)

    # ---- mặt cắt
    fig, axs = plt.subplots(3, 1, figsize=(14, 19))
    for ax, (label, kind, v) in zip(axs, [("x = 0 (nam ← → bắc)", "x", 0), ("x = -22 (giếng thang)", "x", -22),
                                          ("z = -56 (hố sụt tầng 3→4)", "z", -56)]):
        if kind == "x":
            sl = W[v - X0, :, :]                       # (y, z)
            img = col[sl][::-1, ::-1]                   # z giảm sang phải: nam bên trái
            e2 = [Z0 + W.shape[2] - 0.5, Z0 - 0.5, Y0 - 0.5, Y0 + W.shape[1] - 0.5]
        else:
            sl = W[:, :, v - Z0].T                      # (y, x)
            img = col[sl][::-1]
            e2 = [X0 - 0.5, X0 + W.shape[0] - 0.5, Y0 - 0.5, Y0 + W.shape[1] - 0.5]
        ax.imshow(img, extent=e2, aspect="auto", interpolation="nearest")
        ax.set_title(label)
        ax.set_ylabel("Y")
        ax.grid(alpha=0.25)
    fig.savefig(IMG / "sections.png", dpi=100, bbox_inches="tight")
    plt.close(fig)

    # ---- phóng to: hố, giếng, hố sụt, đường leo
    def cut_x(ax, x, z0, z1, y0, y1, title):
        sl = W[x - X0, y0 - Y0:y1 - Y0 + 1, z0 - Z0:z1 - Z0 + 1]
        ax.imshow(col[sl][::-1, ::-1], extent=[z1 + 0.5, z0 - 0.5, y0 - 0.5, y1 + 0.5], interpolation="nearest")
        ax.set_title(title, fontsize=9); ax.set_xlabel("z"); ax.set_ylabel("Y")
    def cut_z(ax, z, x0, x1, y0, y1, title):
        sl = W[x0 - X0:x1 - X0 + 1, y0 - Y0:y1 - Y0 + 1, z - Z0].T
        ax.imshow(col[sl][::-1], extent=[x0 - 0.5, x1 + 0.5, y0 - 0.5, y1 + 0.5], interpolation="nearest")
        ax.set_title(title, fontsize=9); ax.set_xlabel("x"); ax.set_ylabel("Y")
    def plan(ax, y, x0, x1, z0, z1, title):
        sl = W[x0 - X0:x1 - X0 + 1, y - Y0, z0 - Z0:z1 - Z0 + 1]
        ax.imshow(np.transpose(col[sl], (1, 0, 2)), extent=[x0 - 0.5, x1 + 0.5, z1 + 0.5, z0 - 0.5], interpolation="nearest")
        ax.set_title(title, fontsize=9); ax.set_xlabel("x"); ax.set_ylabel("z")
    fig, axs = plt.subplots(2, 3, figsize=(18, 12))
    cut_x(axs[0, 0], 0, -78, -46, 68, 126, "Hố tầng 1→2, cắt x=0")
    cut_x(axs[0, 1], -22, 50, 74, 36, 82, "Giếng thang tầng 2→3, cắt x=-22")
    cut_z(axs[0, 2], -56, 16, 44, 2, 48, "Hố sụt tầng 3→4, cắt z=-56")
    x0, x1, z0, z1 = -40, 40, 78, 118
    box = W[x0 - X0:x1 - X0 + 1, :, z0 - Z0:z1 - Z0 + 1]
    solid = ~is_open[box]
    topy = box.shape[1] - 1 - np.argmax(solid[:, ::-1, :], axis=1)
    topb = np.take_along_axis(box, topy[:, None, :], axis=1)[:, 0, :]
    shade = 0.55 + 0.45 * (topy - topy.min()) / max(1, topy.max() - topy.min())
    axs[1, 0].imshow(np.transpose(col[topb] * shade[..., None], (1, 0, 2)), extent=[x0 - 0.5, x1 + 0.5, z1 + 0.5, z0 - 0.5],
                     interpolation="nearest")
    axs[1, 0].set_title("Hõm đường leo nhìn từ trên (sáng = cao)", fontsize=9)
    cut_x(axs[1, 1], -20, 80, 112, 94, 126, "Đường leo, cắt x=-20")
    plan(axs[1, 2], 121, -24, 16, 20, 50, "Cửa hang dẫn, lát Y121")
    fig.savefig(IMG / "details.png", dpi=100, bbox_inches="tight")
    plt.close(fig)

    # ---- phòng boss
    import kv_model as KM
    FB = KM.BOSS["floor"]
    fig = plt.figure(figsize=(18, 18))
    ax = fig.add_subplot(2, 2, 1)
    sl, flo = W[:, FB + 2 - Y0, :], W[:, FB - Y0, :]
    op = is_open[sl]
    img = np.where(op[..., None], col[flo] * 0.7 + 0.3, col[sl] * 0.6)
    rb = ctx["seen_boss"][:, FB - 6 - Y0:FB + 12 - Y0, :].any(axis=1)
    img[rb] = img[rb] * 0.75 + np.array([0.2, 0.75, 0.3]) * 0.25
    ax.imshow(np.transpose(img, (1, 0, 2)), extent=ext, interpolation="nearest")
    for (n, x1, z1, x2, z2) in KM.BOSS["S"]:
        ax.add_patch(Rectangle((x1 - 0.5, z1 - 0.5), x2 - x1 + 1, z2 - z1 + 1, fill=False, ec="crimson", lw=1))
        ax.text((x1 + x2) / 2, (z1 + z2) / 2, n, ha="center", va="center", color="crimson", weight="bold")
    ax.set_xlim(-66, 66); ax.set_ylim(66, -66)
    ax.set_title(f"Phòng boss — lát Y{FB + 2} (xanh lá = đi bộ tới từ bệ xuất hiện)")
    ax = fig.add_subplot(2, 2, 2)
    box = W[:, FB - Y0:KM.BOSS["apex"] + 1 - Y0, :]
    sub = np.argmax(is_open[box], axis=1)               # ô trống đầu tiên từ nền lên; khối bên dưới là mặt nền
    ax.imshow(np.transpose(col[np.take_along_axis(box, np.clip(sub - 1, 0, None)[:, None, :], axis=1)[:, 0, :]], (1, 0, 2)),
              extent=[X0 - 0.5, X0 + W.shape[0] - 0.5, Z0 + W.shape[2] - 0.5, Z0 - 0.5], interpolation="nearest")
    ax.set_xlim(-66, 66); ax.set_ylim(66, -66)
    ax.set_title("Phòng boss — mặt nền nhìn từ trên (khối đầu tiên dưới khoảng trống)")
    for i, (label, v) in enumerate((("cắt x=0 (nam ← → bắc)", 0), ("cắt z=-12 (tây → đông, qua lỗ thủng B)", -12))):
        ax = fig.add_subplot(4, 1, 3 + i)
        if i == 0:
            s2 = W[v - X0, FB - 10 - Y0:12 - Y0, :]
            ax.imshow(col[s2][::-1, ::-1], extent=[Z0 + W.shape[2] - 0.5, Z0 - 0.5, FB - 10.5, 11.5], aspect="auto",
                      interpolation="nearest")
            ax.set_xlim(70, -70)
        else:
            s2 = W[:, FB - 10 - Y0:12 - Y0, v - Z0].T
            ax.imshow(col[s2][::-1], extent=[X0 - 0.5, X0 + W.shape[0] - 0.5, FB - 10.5, 11.5], aspect="auto",
                      interpolation="nearest")
            ax.set_xlim(-70, 70)
        ax.set_title(label)
    fig.savefig(IMG / "boss.png", dpi=90, bbox_inches="tight")
    plt.close(fig)
