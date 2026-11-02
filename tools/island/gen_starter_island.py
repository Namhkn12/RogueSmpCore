import itertools
import math
import sys
from pathlib import Path

import nbtlib
import numpy as np
from nbtlib import ByteArray, Compound, Int, IntArray, List, Short, String
from PIL import Image, ImageDraw

DEFAULT_SEED = 20261029
ROOT = Path(__file__).resolve().parents[2]
OUT_SCHEM = ROOT / "server_data" / "island_template" / "starter_island.schem"
OUT_PREVIEW = Path(__file__).resolve().parent

SIZE = 32
CHUNK = 16
STEP = 5
DIRT_DEPTH = 3
STONE_DEPTH = 24
DEEPSLATE_DEPTH = 40
BODY_DEPTH = DIRT_DEPTH + STONE_DEPTH + DEEPSLATE_DEPTH
BOTTOM_STEP = 16
CAVE_CEILING = DIRT_DEPTH + 4
WORM_COUNT = 7
CHEESE_COUNT = 170
CHAMBER_COUNT = 3

LOW = 2 * BOTTOM_STEP + BODY_DEPTH + 2
HEIGHT = LOW + 2 * STEP + 1

GRASS = "minecraft:grass_block[snowy=false]"
DEEPSLATE = "minecraft:deepslate[axis=y]"

STONE_TOP = DIRT_DEPTH
DEEP_TOP = DIRT_DEPTH + STONE_DEPTH
BODY_BOTTOM = BODY_DEPTH

KEYFRAMES = [
    (-12, {"minecraft:dirt": 64, "minecraft:coarse_dirt": 22, "minecraft:rooted_dirt": 14}),
    (0, {"minecraft:dirt": 66, "minecraft:coarse_dirt": 22, "minecraft:rooted_dirt": 12}),
    (STONE_TOP - 1, {"minecraft:dirt": 45, "minecraft:coarse_dirt": 30, "minecraft:rooted_dirt": 5, "minecraft:gravel": 15, "minecraft:mossy_cobblestone": 5}),
    (STONE_TOP + 1, {"minecraft:coarse_dirt": 14, "minecraft:gravel": 16, "minecraft:cobblestone": 36, "minecraft:mossy_cobblestone": 20, "minecraft:stone": 10, "minecraft:dirt": 4}),
    (STONE_TOP + STONE_DEPTH * 0.25, {"minecraft:cobblestone": 46, "minecraft:stone": 24, "minecraft:andesite": 14, "minecraft:mossy_cobblestone": 8, "minecraft:gravel": 4}),
    (STONE_TOP + STONE_DEPTH * 0.6, {"minecraft:stone": 38, "minecraft:cobblestone": 30, "minecraft:andesite": 20}),
    (DEEP_TOP - 2, {"minecraft:stone": 28, "minecraft:tuff": 24, "minecraft:andesite": 16, "minecraft:cobblestone": 14, "minecraft:cobbled_deepslate": 14}),
    (DEEP_TOP + 2, {DEEPSLATE: 38, "minecraft:cobbled_deepslate": 26, "minecraft:tuff": 26, "minecraft:stone": 6}),
    (DEEP_TOP + DEEPSLATE_DEPTH * 0.4, {DEEPSLATE: 56, "minecraft:cobbled_deepslate": 28, "minecraft:tuff": 10}),
    (BODY_BOTTOM - 4, {DEEPSLATE: 46, "minecraft:cobbled_deepslate": 22, "minecraft:tuff": 12, "minecraft:dripstone_block": 15, "minecraft:smooth_basalt": 5}),
    (BODY_BOTTOM + 8, {"minecraft:dripstone_block": 42, DEEPSLATE: 22, "minecraft:tuff": 18, "minecraft:cobbled_deepslate": 13, "minecraft:smooth_basalt": 5}),
    (BODY_BOTTOM + 30, {"minecraft:dripstone_block": 62, "minecraft:tuff": 18, DEEPSLATE: 8, "minecraft:calcite": 12}),
]

ORE_VEINS = [
    ("coal", DIRT_DEPTH + 2, DEEP_TOP + 6, 18, (8, 16)),
    ("iron", DIRT_DEPTH + 8, DEEP_TOP + 20, 14, (5, 10)),
    ("gold", DEEP_TOP + 5, BODY_DEPTH + 12, 6, (4, 8)),
    ("diamond", DEEP_TOP + 18, BODY_DEPTH + 2 * 16, 4, (3, 5)),
]
SOFT_BLOCKS = {GRASS, "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt", "minecraft:gravel"}
NEIGHBOURS = [(1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)]

PREVIEW_COLORS = {
    GRASS: (98, 158, 58),
    "minecraft:dirt": (134, 96, 67),
    "minecraft:coarse_dirt": (119, 85, 59),
    "minecraft:rooted_dirt": (144, 104, 77),
    "minecraft:gravel": (131, 127, 126),
    "minecraft:cobblestone": (122, 122, 122),
    "minecraft:mossy_cobblestone": (104, 120, 88),
    "minecraft:stone": (125, 125, 125),
    "minecraft:andesite": (138, 138, 140),
    "minecraft:coal_ore": (40, 40, 40),
    "minecraft:iron_ore": (200, 160, 130),
    "minecraft:gold_ore": (235, 205, 60),
    "minecraft:diamond_ore": (90, 225, 225),
    "minecraft:deepslate_coal_ore": (30, 30, 32),
    "minecraft:deepslate_iron_ore": (190, 150, 125),
    "minecraft:deepslate_gold_ore": (225, 195, 55),
    "minecraft:deepslate_diamond_ore": (80, 215, 215),
    "minecraft:tuff": (108, 109, 102),
    "minecraft:cobbled_deepslate": (77, 77, 80),
    DEEPSLATE: (86, 86, 90),
    "minecraft:dripstone_block": (134, 107, 92),
    "minecraft:smooth_basalt": (72, 72, 78),
    "minecraft:calcite": (215, 216, 212),
}


def value_noise(shape, scale, rng):
    dims = [int(math.ceil(s / scale)) + 2 for s in shape]
    grid = rng.random(dims)
    mesh = np.meshgrid(*[np.arange(s) / scale for s in shape], indexing="ij")
    base = [np.floor(m).astype(int) for m in mesh]
    frac = [m - b for m, b in zip(mesh, base)]
    frac = [t * t * (3 - 2 * t) for t in frac]
    out = np.zeros(shape)
    for corner in itertools.product((0, 1), repeat=len(shape)):
        weight = np.ones(shape)
        index = []
        for axis, c in enumerate(corner):
            weight *= frac[axis] if c else 1 - frac[axis]
            index.append(base[axis] + c)
        out += weight * grid[tuple(index)]
    return out * 2 - 1


def fbm(shape, scale, octaves, rng):
    total, norm, amp = np.zeros(shape), 0.0, 1.0
    for _ in range(octaves):
        total += amp * value_noise(shape, scale, rng)
        norm += amp
        amp *= 0.5
        scale = max(scale / 2, 1.0)
    return total / norm


def surface_heights():
    top = np.full((SIZE, SIZE), LOW + STEP, dtype=int)
    top[:CHUNK, :CHUNK] = LOW + 2 * STEP
    top[CHUNK:, CHUNK:] = LOW
    return top


def bottom_heights():
    bottom = np.full((SIZE, SIZE), LOW - BODY_DEPTH, dtype=int)
    for cx, cz in itertools.product(range(2), range(2)):
        bottom[cx * CHUNK:(cx + 1) * CHUNK, cz * CHUNK:(cz + 1) * CHUNK] -= BOTTOM_STEP * (cx + cz)
    return bottom


class Carver:
    def __init__(self, bottom):
        self.bottom = bottom
        self.air = np.zeros((SIZE, HEIGHT, SIZE), dtype=bool)
        self.grid = np.meshgrid(np.arange(SIZE), np.arange(HEIGHT), np.arange(SIZE), indexing="ij")

    def sphere(self, cx, cy, cz, r):
        lo = [max(int(c - r - 1), 0) for c in (cx, cy, cz)]
        hi = [min(int(c + r + 2), m) for c, m in zip((cx, cy, cz), (SIZE, HEIGHT, SIZE))]
        box = tuple(slice(a, b) for a, b in zip(lo, hi))
        gx, gy, gz = (g[box] for g in self.grid)
        self.air[box] |= (gx - cx) ** 2 + (gy - cy) ** 2 + (gz - cz) ** 2 <= r * r

    def column_floor(self, x, z):
        xi = int(np.clip(round(x), 0, SIZE - 1))
        zi = int(np.clip(round(z), 0, SIZE - 1))
        return self.bottom[xi, zi]

    def face_point(self, rng):
        side = rng.integers(4)
        along = rng.uniform(2, SIZE - 3)
        x, z = [(0.0, along), (SIZE - 1.0, along), (along, 0.0), (along, SIZE - 1.0)][side]
        y = rng.uniform(self.column_floor(x, z) + 4, LOW - CAVE_CEILING - 3)
        return x, y, z

    def worm(self, rng):
        x, y, z = self.face_point(rng)
        centre = (SIZE - 1) / 2
        yaw = math.atan2(centre - z, centre - x) + rng.uniform(-0.6, 0.6)
        pitch = rng.uniform(-0.3, 0.3)
        radius = rng.uniform(1.6, 2.6)
        for step in range(int(rng.integers(35, 75))):
            r = radius * (0.8 + 0.35 * math.sin(step * 0.35))
            self.sphere(x, y, z, r)
            yaw += rng.normal(0, 0.25)
            pitch = float(np.clip(pitch * 0.85 + rng.normal(0, 0.15), -0.6, 0.6))
            x += math.cos(yaw) * math.cos(pitch)
            z += math.sin(yaw) * math.cos(pitch)
            y += math.sin(pitch)
            floor = self.column_floor(x, z) + r + 1
            ceiling = LOW - CAVE_CEILING - r
            if y < floor or y > ceiling:
                pitch = -pitch
                y = float(np.clip(y, floor, ceiling))
            if not (-3 <= x <= SIZE + 2 and -3 <= z <= SIZE + 2):
                break

    def cheese(self, rng):
        r = rng.uniform(1.4, 3.6)
        if rng.random() < 0.75:
            if rng.random() < 0.7:
                x, y, z = self.face_point(rng)
                x += rng.uniform(-0.5, 1.0) * (1 if x == 0 else -1 if x == SIZE - 1 else 0)
                z += rng.uniform(-0.5, 1.0) * (1 if z == 0 else -1 if z == SIZE - 1 else 0)
            else:
                x, z = rng.uniform(1, SIZE - 2, size=2)
                y = self.column_floor(x, z) + rng.uniform(-0.5, 1.0)
        else:
            x, z = rng.uniform(2, SIZE - 3, size=2)
            y = rng.uniform(self.column_floor(x, z) + 3, LOW - CAVE_CEILING - r)
        self.sphere(x, y, z, r)

    def chamber(self, rng):
        x, z = rng.uniform(8, SIZE - 9, size=2)
        r = rng.uniform(3.5, 5.5)
        y = rng.uniform(self.column_floor(x, z) + r + 3, LOW - CAVE_CEILING - r - 2)
        for _ in range(3):
            self.sphere(x + rng.uniform(-2, 2), y + rng.uniform(-1, 1), z + rng.uniform(-2, 2), r * rng.uniform(0.7, 1.0))

    def carve(self, rng):
        for _ in range(CHAMBER_COUNT):
            self.chamber(rng)
        for _ in range(WORM_COUNT):
            self.worm(rng)
        for _ in range(CHEESE_COUNT):
            self.cheese(rng)
        self.air[:, LOW - CAVE_CEILING + 1:, :] = False
        return self.air


def connected_to_surface(solid, top):
    reach = np.zeros_like(solid)
    for x, z in itertools.product(range(SIZE), range(SIZE)):
        reach[x, top[x, z], z] = True
    while True:
        grown = reach.copy()
        grown[1:] |= reach[:-1]
        grown[:-1] |= reach[1:]
        grown[:, 1:] |= reach[:, :-1]
        grown[:, :-1] |= reach[:, 1:]
        grown[:, :, 1:] |= reach[:, :, :-1]
        grown[:, :, :-1] |= reach[:, :, 1:]
        grown &= solid
        if (grown == reach).all():
            return reach
        reach = grown


def pick_materials(depth, rng):
    names = sorted({n for _, w in KEYFRAMES for n in w})
    keys = np.array([k for k, _ in KEYFRAMES], dtype=float)
    weights = np.stack([np.interp(depth, keys, [w.get(n, 0) for _, w in KEYFRAMES]) for n in names], axis=-1)
    cumulative = np.cumsum(weights, axis=-1)
    roll = rng.random(depth.shape) * cumulative[..., -1]
    choice = (cumulative < roll[..., None]).sum(axis=-1)
    return names, np.minimum(choice, len(names) - 1)


def build(rng):
    top = surface_heights()
    bottom = bottom_heights()
    ys = np.arange(HEIGHT)[None, :, None]
    solid = (ys <= top[:, None, :]) & (ys >= bottom[:, None, :])
    solid &= ~Carver(bottom).carve(rng)
    solid = connected_to_surface(solid, top)

    shape = (SIZE, HEIGHT, SIZE)
    jitter = 2.6 * fbm(shape, 5, 2, rng) + 1.6 * fbm((SIZE, 1, SIZE), 8, 2, rng)
    depth = (LOW - ys) + jitter
    names, choice = pick_materials(np.broadcast_to(depth, shape), rng)

    blocks = np.full(shape, "minecraft:air", dtype=object)
    blocks[solid] = np.array(names, dtype=object)[choice[solid]]
    for x, z in itertools.product(range(SIZE), range(SIZE)):
        blocks[x, top[x, z], z] = GRASS
    unsupported = (blocks[:, 1:, :] == "minecraft:gravel") & ~solid[:, :-1, :]
    blocks[:, 1:, :][unsupported] = "minecraft:cobblestone"
    print("ores", place_ores(blocks, solid, np.broadcast_to(depth, shape), rng))
    return blocks


def exposed_mask(solid):
    padded = np.pad(solid, 1, constant_values=False)
    covered = np.ones_like(solid)
    for dx, dy, dz in NEIGHBOURS:
        covered &= padded[1 + dx:1 + dx + SIZE, 1 + dy:1 + dy + HEIGHT, 1 + dz:1 + dz + SIZE]
    return solid & ~covered


def place_ores(blocks, solid, depth, rng):
    hard = solid & ~np.isin(blocks, list(SOFT_BLOCKS))
    exposed = exposed_mask(solid)
    placed = {}
    for ore, shallow, deep, count, (min_size, max_size) in ORE_VEINS:
        band = hard & (depth >= shallow) & (depth <= deep)
        candidates = np.argwhere(band)
        visible = np.argwhere(band & exposed)
        for _ in range(count):
            pool = visible if len(visible) and rng.random() < 0.5 else candidates
            cluster = [tuple(pool[rng.integers(len(pool))])]
            target = int(rng.integers(min_size, max_size + 1))
            for _ in range(target * 4):
                if len(cluster) >= target:
                    break
                bx, by, bz = cluster[rng.integers(len(cluster))]
                dx, dy, dz = NEIGHBOURS[rng.integers(6)]
                point = (bx + dx, by + dy, bz + dz)
                if all(0 <= c < m for c, m in zip(point, band.shape)) and band[point] and point not in cluster:
                    cluster.append(point)
            for point in cluster:
                prefix = "deepslate_" if depth[point] > DEEP_TOP else ""
                blocks[point] = f"minecraft:{prefix}{ore}_ore"
            placed[ore] = placed.get(ore, 0) + len(cluster)
    return placed


def trim(blocks):
    filled = np.where((blocks != "minecraft:air").any(axis=(0, 2)))[0]
    return blocks[:, filled.min():filled.max() + 1, :], filled.min()


def varint(value):
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return out


def write_schem(blocks, origin_y):
    width, height, length = blocks.shape
    palette = {"minecraft:air": 0}
    data = bytearray()
    for y in range(height):
        for z in range(length):
            for x in range(width):
                name = blocks[x, y, z]
                data += varint(palette.setdefault(name, len(palette)))
    schematic = Compound({
        "Version": Int(2),
        "DataVersion": Int(4671),
        "Metadata": Compound({
            "WEOffsetX": Int(-SIZE // 2),
            "WEOffsetY": Int(-origin_y),
            "WEOffsetZ": Int(-SIZE // 2),
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


def colour(name):
    return PREVIEW_COLORS[name]


def render(blocks, path, from_below=False, scale=7):
    if from_below:
        blocks = blocks[:, ::-1, :]
    width, height, length = blocks.shape
    cos30 = math.cos(math.radians(30))
    img_w = int((width + length) * cos30 * scale) + 40
    img_h = int((width + length) * 0.5 * scale + height * scale) + 40
    ox = length * cos30 * scale + 20
    oy = height * scale + 20
    img = Image.new("RGB", (img_w, img_h), (235, 240, 245))
    draw = ImageDraw.Draw(img)

    def project(x, y, z):
        return (ox + (x - z) * cos30 * scale, oy + (x + z) * 0.5 * scale - y * scale)

    filled = blocks != "minecraft:air"
    above = np.flip(np.logical_or.accumulate(np.flip(filled, axis=1), axis=1), axis=1)
    below = np.logical_or.accumulate(filled, axis=1)
    enclosed = ~filled & above & below

    def is_air(x, y, z):
        if not (0 <= x < width and 0 <= y < height and 0 <= z < length):
            return True
        return not filled[x, y, z]

    def shade_for(x, y, z, base):
        if 0 <= x < width and 0 <= y < height and 0 <= z < length and enclosed[x, y, z]:
            return base * 0.4
        return base

    solid = [(x, y, z) for x, y, z in zip(*np.where(blocks != "minecraft:air"))]
    solid.sort(key=lambda p: (p[0] + p[1] + p[2], p[1]))
    for x, y, z in solid:
        base = colour(blocks[x, y, z])
        top_colour = base
        side_colour = base
        if blocks[x, y, z] == GRASS:
            side_colour = (134, 96, 67) if not from_below else base
            if from_below:
                top_colour = (134, 96, 67)
        faces = []
        if is_air(x, y + 1, z):
            faces.append(([(x, y + 1, z), (x + 1, y + 1, z), (x + 1, y + 1, z + 1), (x, y + 1, z + 1)], top_colour, shade_for(x, y + 1, z, 1.0)))
        if is_air(x + 1, y, z):
            faces.append(([(x + 1, y, z), (x + 1, y + 1, z), (x + 1, y + 1, z + 1), (x + 1, y, z + 1)], side_colour, shade_for(x + 1, y, z, 0.8)))
        if is_air(x, y, z + 1):
            faces.append(([(x, y, z + 1), (x + 1, y, z + 1), (x + 1, y + 1, z + 1), (x, y + 1, z + 1)], side_colour, shade_for(x, y, z + 1, 0.62)))
        for corners, rgb, shade in faces:
            fill = tuple(int(c * shade) for c in rgb)
            draw.polygon([project(*c) for c in corners], fill=fill, outline=tuple(int(c * 0.85) for c in fill))
    if from_below:
        img = img.transpose(Image.FLIP_TOP_BOTTOM)
    img.save(path)


def main():
    seed = int(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_SEED
    rng = np.random.default_rng(seed)
    blocks, min_y = trim(build(rng))
    mid_surface = LOW + STEP - min_y
    palette = write_schem(blocks, mid_surface + 1)
    render(blocks, OUT_PREVIEW / "preview_top.png")
    render(blocks, OUT_PREVIEW / "preview_below.png", from_below=True)
    render(blocks[:, :, ::-1], OUT_PREVIEW / "preview_diag.png")
    render(blocks[:, :, ::-1], OUT_PREVIEW / "preview_diag_below.png", from_below=True)
    for cut in (10, 22):
        render(blocks[:, :, :cut], OUT_PREVIEW / f"preview_cut_z{cut}.png")
    print(f"seed={seed} size={blocks.shape} palette={len(palette)} -> {OUT_SCHEM}")


if __name__ == "__main__":
    main()
