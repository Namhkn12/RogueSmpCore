"""Đọc / ghi schematic dùng chung cho mọi dungeon.

  read_schem(path)  -> Schem(W, names, offset)   hỗ trợ Sponge .schem v2/v3 và Litematica .litematic
  write_schem(path, W, pal, offset)              ghi Sponge .schem v2 (không cần thư viện)

W là mảng numpy [x, y, z] chứa id khối; names[id] = "minecraft:stone[...]".
"""
import gzip, math, struct
from dataclasses import dataclass
import numpy as np


@dataclass
class Schem:
    W: np.ndarray          # [x, y, z] -> id
    names: list            # id -> tên khối đầy đủ (kèm trạng thái)
    offset: tuple          # tọa độ thật của ô W[0, 0, 0] (nếu file có lưu), mặc định (0, 0, 0)

    @property
    def size(self):
        return self.W.shape

    def ids_matching(self, *keys):
        return [i for i, n in enumerate(self.names) if any(k in n for k in keys)]


# ------------------------------------------------------------------ đọc
def _varints(buf, count):
    out = np.empty(count, dtype=np.int64)
    i = n = 0
    data = bytes(buf)
    while n < count:
        v = shift = 0
        while True:
            b = data[i]; i += 1
            v |= (b & 0x7F) << shift
            if not b & 0x80: break
            shift += 7
        out[n] = v; n += 1
    return out


def _read_sponge(root):
    s = root["Schematic"] if "Schematic" in root else root
    w, h, l = int(s["Width"]), int(s["Height"]), int(s["Length"])
    if "Blocks" in s:                       # v3
        pal, data = s["Blocks"]["Palette"], s["Blocks"]["Data"]
    else:                                   # v1/v2
        pal, data = s["Palette"], s["BlockData"]
    names = [None] * (max(int(v) for v in pal.values()) + 1)
    for k, v in pal.items():
        names[int(v)] = str(k)
    ids = _varints(np.asarray(data, dtype=np.uint8), w * h * l)
    W = ids.reshape(h, l, w).transpose(2, 0, 1)            # (y, z, x) -> (x, y, z)
    off = tuple(int(v) for v in s["Offset"]) if "Offset" in s else (0, 0, 0)
    return Schem(W.astype(np.uint16), [n or "minecraft:air" for n in names], off)


def _state_name(c):
    n = str(c["Name"])
    if "Properties" in c and len(c["Properties"]):
        n += "[" + ",".join(f"{k}={v}" for k, v in sorted(c["Properties"].items())) + "]"
    return n


def _read_litematic(root):
    regions = root["Regions"]
    boxes = []
    for name, r in regions.items():
        p = [int(r["Position"][k]) for k in "xyz"]
        sz = [int(r["Size"][k]) for k in "xyz"]
        lo = [p[i] + min(sz[i] + 1, 0) for i in range(3)]          # kích thước âm = mọc về phía âm
        boxes.append((r, lo, [abs(v) for v in sz]))
    gmin = [min(b[1][i] for b in boxes) for i in range(3)]
    gmax = [max(b[1][i] + b[2][i] for b in boxes) for i in range(3)]
    names = ["minecraft:air"]; index = {"minecraft:air": 0}
    W = np.zeros([gmax[i] - gmin[i] for i in range(3)], dtype=np.uint16)
    for r, lo, (sx, sy, sz) in boxes:
        pal = [_state_name(c) for c in r["BlockStatePalette"]]
        remap = np.array([index.setdefault(n, len(index)) for n in pal], dtype=np.uint16)
        for n in pal:
            if index[n] >= len(names): names.append(n)
        bits = max(2, math.ceil(math.log2(len(pal))))
        longs = np.asarray(r["BlockStates"], dtype=np.int64).view(np.uint64)
        total = sx * sy * sz
        idx = np.arange(total, dtype=np.uint64) * np.uint64(bits)
        word, off = idx // np.uint64(64), idx % np.uint64(64)
        mask = np.uint64((1 << bits) - 1)
        lo_part = (longs[word.astype(np.int64)] >> off)
        nxt = np.minimum(word + np.uint64(1), np.uint64(len(longs) - 1)).astype(np.int64)
        spill = off + np.uint64(bits) > np.uint64(64)
        hi_part = np.where(spill, longs[nxt] << (np.uint64(64) - off) % np.uint64(64), np.uint64(0))
        vals = ((lo_part | hi_part) & mask).astype(np.int64)
        blk = remap[vals].reshape(sy, sz, sx).transpose(2, 0, 1)    # (y, z, x) -> (x, y, z)
        o = [lo[i] - gmin[i] for i in range(3)]
        W[o[0]:o[0] + sx, o[1]:o[1] + sy, o[2]:o[2] + sz] = blk
    return Schem(W, names, tuple(gmin))


def read_schem(path):
    import nbtlib
    f = nbtlib.load(str(path))
    root = f.get("", f) if hasattr(f, "get") else f
    if "Regions" in root:
        return _read_litematic(root)
    if "Schematic" in root or "Palette" in root or "Width" in root:
        return _read_sponge(root)
    if "Blocks" in root and "Materials" in root:
        raise ValueError("định dạng MCEdit .schematic cũ (id số) chưa hỗ trợ: mở bằng WorldEdit rồi //schem save lại dạng .schem")
    raise ValueError("không nhận ra định dạng schematic")


# ------------------------------------------------------------------ ghi
def write_schem(path, W, pal, offset, data_version=3953):
    """W: [x, y, z] id; pal: dict tên -> id; offset: tọa độ thật của W[0,0,0] (dán bằng //paste -o)"""
    def tag_str(s):
        b = s.encode(); return struct.pack(">H", len(b)) + b
    def named(t, name):
        return bytes([t]) + tag_str(name)
    NX, NY, NZ = W.shape
    order = np.transpose(W, (1, 2, 0)).reshape(-1)           # Sponge: x + z*W + y*W*L
    out = bytearray()
    for v in order.tolist():
        while v & ~0x7F:
            out.append((v & 0x7F) | 0x80); v >>= 7
        out.append(v)
    body = bytearray()
    body += named(3, "Version") + struct.pack(">i", 2)
    body += named(3, "DataVersion") + struct.pack(">i", data_version)
    body += named(2, "Width") + struct.pack(">h", NX)
    body += named(2, "Height") + struct.pack(">h", NY)
    body += named(2, "Length") + struct.pack(">h", NZ)
    body += named(11, "Offset") + struct.pack(">i", 3) + struct.pack(">iii", *offset)
    meta = b"".join(named(3, k) + struct.pack(">i", 0) for k in ("WEOffsetX", "WEOffsetY", "WEOffsetZ")) + b"\x00"
    body += named(10, "Metadata") + meta
    body += named(3, "PaletteMax") + struct.pack(">i", len(pal))
    body += named(10, "Palette") + b"".join(named(3, n) + struct.pack(">i", i) for n, i in pal.items()) + b"\x00"
    body += named(7, "BlockData") + struct.pack(">i", len(out)) + bytes(out)
    body += named(9, "BlockEntities") + bytes([10]) + struct.pack(">i", 0)
    with gzip.open(path, "wb") as f:
        f.write(named(10, "Schematic") + bytes(body) + b"\x00")
