import importlib.util
import sys
from pathlib import Path

import nbtlib
import numpy as np
from nbtlib import ByteArray, Compound, Int, IntArray, List, Short

HERE = Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("base_island", HERE / "gen_starter_island.py")
base = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(base)

CUBE = 16
LAYOUTS = {
    "cubes": {
        "black": (6, 0, 15),
        "blue": (17, 3, 10),
        "grey": (0, 6, 5),
        "brown": (9, 13, 0),
    },
    "grid": {
        "black": (0, 0, 5),
        "blue": (16, 0, 3),
        "grey": (0, 16, 1),
        "brown": (16, 16, 0),
    },
}
LAYOUTS["round"] = LAYOUTS["grid"]
ROUND_LAYOUTS = {"round"}

LAYOUT = sys.argv[1] if len(sys.argv) > 1 else "cubes"
SEED = int(sys.argv[2]) if len(sys.argv) > 2 else 20261030
CUBES = LAYOUTS[LAYOUT]
OUT_SCHEM = base.ROOT / "server_data" / "island_template" / f"starter_island_{LAYOUT}.schem"

GRASS = base.GRASS
MOSS = "minecraft:moss_block"
DEEPSLATE = base.DEEPSLATE
DIRT_LAYER = 2
DEEP_TOP = 10.5

KEYFRAMES = [
    (0, {"minecraft:dirt": 70, "minecraft:coarse_dirt": 20, "minecraft:rooted_dirt": 10}),
    (1.5, {"minecraft:dirt": 50, "minecraft:coarse_dirt": 30, "minecraft:gravel": 10, "minecraft:mossy_cobblestone": 10}),
    (2.5, {"minecraft:cobblestone": 35, "minecraft:mossy_cobblestone": 20, "minecraft:stone": 25, "minecraft:andesite": 10, "minecraft:gravel": 10}),
    (5, {"minecraft:stone": 40, "minecraft:cobblestone": 30, "minecraft:andesite": 25, "minecraft:tuff": 5}),
    (8.5, {"minecraft:stone": 30, "minecraft:tuff": 25, "minecraft:andesite": 15, "minecraft:cobblestone": 15, "minecraft:cobbled_deepslate": 15}),
    (DEEP_TOP + 0.5, {DEEPSLATE: 40, "minecraft:cobbled_deepslate": 30, "minecraft:tuff": 25, "minecraft:stone": 5}),
    (13, {DEEPSLATE: 55, "minecraft:cobbled_deepslate": 30, "minecraft:tuff": 15}),
    (16, {DEEPSLATE: 50, "minecraft:cobbled_deepslate": 25, "minecraft:tuff": 15, "minecraft:smooth_basalt": 10}),
]

ORE_VEINS = [
    ("coal", 3, 11, 10, (6, 12)),
    ("iron", 5, 15, 8, (4, 8)),
    ("gold", 10, 30, 3, (3, 6)),
    ("diamond", 12, 30, 2, (2, 4)),
]
SOFT = {GRASS, MOSS, "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt", "minecraft:gravel"}
NEIGHBOURS = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]
SIDES = [(1, 0), (-1, 0), (0, 1), (0, -1)]

base.PREVIEW_COLORS[MOSS] = (84, 112, 40)


def layout():
    width = max(x for x, _, _ in CUBES.values()) + CUBE
    length = max(z for _, z, _ in CUBES.values()) + CUBE
    height = max(h for _, _, h in CUBES.values()) + CUBE
    cube_top = np.full((width, height, length), -1, dtype=int)
    for x0, z0, lift in sorted(CUBES.values(), key=lambda c: c[2]):
        top = lift + CUBE - 1
        cube_top[x0:x0 + CUBE, lift:lift + CUBE, z0:z0 + CUBE] = top
    if LAYOUT in ROUND_LAYOUTS:
        xs, zs = np.meshgrid(np.arange(width) + 0.5, np.arange(length) + 0.5, indexing="ij")
        radius = min(width, length) / 2
        outside = np.hypot(xs - width / 2, zs - length / 2) > radius
        cube_top[outside[:, None, :].repeat(height, axis=1)] = -1
    return cube_top


def pick(depth, rng):
    names = sorted({n for _, w in KEYFRAMES for n in w})
    keys = [k for k, _ in KEYFRAMES]
    weights = np.stack([np.interp(depth, keys, [w.get(n, 0) for _, w in KEYFRAMES]) for n in names], axis=-1)
    cumulative = np.cumsum(weights, axis=-1)
    roll = rng.random(depth.shape) * cumulative[..., -1]
    choice = np.minimum((cumulative < roll[..., None]).sum(axis=-1), len(names) - 1)
    return np.array(names, dtype=object)[choice]


def neighbour(mask, dx, dy, dz, fill=False):
    padded = np.pad(mask, 1, constant_values=fill)
    w, h, l = mask.shape
    return padded[1 + dx:1 + dx + w, 1 + dy:1 + dy + h, 1 + dz:1 + dz + l]


def edge_distance(heights, footprint):
    w, l = heights.shape
    padded = np.pad(heights, 1, constant_values=-10)
    edge = np.zeros_like(footprint)
    for dx, dz in SIDES:
        edge |= padded[1 + dx:1 + dx + w, 1 + dz:1 + dz + l] < heights
    edge &= footprint
    dist = np.where(edge, 0, 99)
    frontier = edge
    for step in range(1, 40):
        grown = np.zeros_like(frontier)
        for dx, dz in SIDES + [(1, 1), (1, -1), (-1, 1), (-1, -1)]:
            grown |= np.pad(frontier, 1)[1 + dx:1 + dx + w, 1 + dz:1 + dz + l]
        grown &= footprint & (dist == 99)
        if not grown.any():
            break
        dist[grown] = step
        frontier = grown
    return dist


def dress_surface(blocks, solid, depth, rng):
    w, h, l = solid.shape
    heights = np.where(solid.any(axis=1), h - 1 - np.argmax(solid[:, ::-1, :], axis=1), -1)
    footprint = heights >= 0
    dist = edge_distance(heights, footprint)
    noise = base.fbm((w, l), 4, 2, rng)
    grass_chance = np.clip((dist - 1.5) / 3 + 0.35 * noise, 0, 1)
    for x, z in zip(*np.where(footprint)):
        y = heights[x, z]
        blocks[x, y, z] = GRASS if rng.random() < grass_chance[x, z] else MOSS

    open_side = np.zeros_like(solid)
    for dx, dz in SIDES:
        open_side |= ~neighbour(solid, dx, 0, dz)
    open_side &= solid
    soft_face = open_side & (depth < DIRT_LAYER + 0.8)
    blocks[soft_face] = MOSS
    mossy = open_side & (depth >= DIRT_LAYER + 0.8) & (depth < 5) & (rng.random(solid.shape) < np.clip((5 - depth) / 3, 0, 0.8))
    blocks[mossy] = np.where(rng.random(int(mossy.sum())) < 0.35, MOSS, "minecraft:mossy_cobblestone")


def place_ores(blocks, solid, depth, rng):
    hard = solid & ~np.isin(blocks, list(SOFT | {"minecraft:mossy_cobblestone"}))
    covered = np.ones_like(solid)
    for d in NEIGHBOURS:
        covered &= neighbour(solid, *d)
    exposed = solid & ~covered
    placed = {}
    for ore, shallow, deep, count, (lo, hi) in ORE_VEINS:
        band = hard & (depth >= shallow) & (depth <= deep)
        candidates, visible = np.argwhere(band), np.argwhere(band & exposed)
        if not len(candidates):
            continue
        for _ in range(count):
            pool = visible if len(visible) and rng.random() < 0.5 else candidates
            cluster = [tuple(pool[rng.integers(len(pool))])]
            target = int(rng.integers(lo, hi + 1))
            for _ in range(target * 4):
                if len(cluster) >= target:
                    break
                bx, by, bz = cluster[rng.integers(len(cluster))]
                dx, dy, dz = NEIGHBOURS[rng.integers(6)]
                p = (bx + dx, by + dy, bz + dz)
                if all(0 <= c < m for c, m in zip(p, band.shape)) and band[p] and p not in cluster:
                    cluster.append(p)
            for p in cluster:
                blocks[p] = f"minecraft:{'deepslate_' if depth[p] > DEEP_TOP else ''}{ore}_ore"
            placed[ore] = placed.get(ore, 0) + len(cluster)
    return placed


def build(rng):
    cube_top = layout()
    solid = cube_top >= 0
    ys = np.arange(cube_top.shape[1])[None, :, None]
    jitter = 1.1 * base.fbm(solid.shape, 4, 2, rng)
    depth = np.where(solid, cube_top - ys + jitter, 0)
    blocks = np.full(solid.shape, "minecraft:air", dtype=object)
    blocks[solid] = pick(depth[solid], rng)
    dress_surface(blocks, solid, depth, rng)
    unsupported = (blocks[:, 1:, :] == "minecraft:gravel") & ~solid[:, :-1, :]
    blocks[:, 1:, :][unsupported] = "minecraft:cobblestone"
    print("ores", place_ores(blocks, solid, depth, rng))
    return blocks


def write_schem(blocks):
    width, height, length = blocks.shape
    palette = {"minecraft:air": 0}
    data = bytearray()
    for y in range(height):
        for z in range(length):
            for x in range(width):
                data += base.varint(palette.setdefault(blocks[x, y, z], len(palette)))
    lowest_top = min(lift for _, _, lift in CUBES.values()) + CUBE
    schematic = Compound({
        "Version": Int(2),
        "DataVersion": Int(4671),
        "Metadata": Compound({
            "WEOffsetX": Int(-(width // 2)),
            "WEOffsetY": Int(-lowest_top),
            "WEOffsetZ": Int(-(length // 2)),
        }),
        "Width": Short(width),
        "Height": Short(height),
        "Length": Short(length),
        "Offset": IntArray([0, 0, 0]),
        "PaletteMax": Int(len(palette)),
        "Palette": Compound({k: Int(v) for k, v in palette.items()}),
        "BlockData": ByteArray(np.frombuffer(bytes(data), dtype=np.int8)),
        "BlockEntities": List[Compound]([]),
    })
    OUT_SCHEM.parent.mkdir(parents=True, exist_ok=True)
    nbtlib.File(schematic, root_name="Schematic").save(OUT_SCHEM, gzipped=True)
    return palette


def main():
    blocks = build(np.random.default_rng(SEED))
    palette = write_schem(blocks)
    base.render(blocks, HERE / f"{LAYOUT}_top.png", scale=9)
    base.render(blocks[:, :, ::-1], HERE / f"{LAYOUT}_diag.png", scale=9)
    base.render(blocks, HERE / f"{LAYOUT}_below.png", from_below=True, scale=9)
    print(f"seed={SEED} size={blocks.shape} palette={len(palette)} -> {OUT_SCHEM}")


if __name__ == "__main__":
    main()
