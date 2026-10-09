"""Kiểm tra tầng mê cung không đào tắt được:
 1) tính khoảng cách đi bộ (BFS qua ô khí / ô đi xuyên được) tới mọi ô trong khối mê cung
 2) với mọi khối đào được (không phải bedrock), lan qua tối đa DEPTH khối đào được liền nhau,
    xem các ô khí chạm vào có khoảng cách đi bộ chênh nhau quá LIMIT không (= đào xuyên tường để đi tắt)
 Phải in: "ô khí chưa tới được 0, đường tắt qua tường: 0".
Chạy: python3 scripts/check_maze_walls.py [seed]"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
if __name__ == "__main__":
    sys.argv = [sys.argv[0]] + sys.argv[1:]
import generate_layer2 as g   # import = chạy lại bộ sinh (cùng seed)
import numpy as np
from collections import deque

DEPTH = 3       # tường dày 3 (đá | bedrock | đá): lõi bedrock thủng thì 3 khối đá liền là xuyên được
LIMIT = 24      # chênh khoảng cách đi bộ lớn hơn mức này = đường tắt
INF = 1 << 30

def walk_dist(fl):
    """(D, walk, dig, ya): khoảng cách đi bộ trong khối mê cung tính từ các chân thang vào tầng"""
    f = g.F[fl]
    ya, yb = f + g.MZ_BOT - g.Y0, f + g.MZ_TOP - g.Y0
    sub = g.W[:, ya:yb + 1, :]
    walk = np.isin(sub, g.PASS_IDS())
    dig = ~walk & (sub != g.BEDROCK) & (sub != g.VOID)
    D = np.full(sub.shape, INF, dtype=np.int64)
    dq = deque()
    for e in [r for r in g.ROOMS if r.kind == "arrival" and r.fl == fl]:
        for x in range(e.x1, e.x2 + 1):
            for z in range(e.z1, e.z2 + 1):
                c = (x - g.X0, f + 1 - g.Y0 - ya, z - g.X0)
                if walk[c] and D[c] == INF:
                    D[c] = 0; dq.append(c)
    if not dq:
        raise SystemExit("không tìm được ô xuất phát trong chân thang")
    while dq:
        c = dq.popleft()
        for d in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
            n = (c[0]+d[0], c[1]+d[1], c[2]+d[2])
            if 0 <= n[1] < sub.shape[1] and walk[n] and D[n] == INF:
                D[n] = D[c] + 1; dq.append(n)
    unreached = int((walk & (D == INF) & (sub == g.AIR)).sum())
    return D, walk, dig, ya, unreached

def _shift(a, fn, fill):
    out = a.copy()
    for ax in range(3):
        for s in (1, -1):
            b = np.roll(a, s, axis=ax)
            idx = [slice(None)] * 3
            idx[ax] = slice(0, 1) if s == 1 else slice(-1, None)
            b[tuple(idx)] = fill
            out = fn(out, b)
    return out

def shortcuts(fl):
    D, walk, dig, ya, unreached = walk_dist(fl)
    Dmin = np.where(walk & (D < INF), D, INF)
    Dmax = np.where(walk & (D < INF), D, -1)
    lo = np.where(dig, _shift(Dmin, np.minimum, INF), INF)
    hi = np.where(dig, _shift(Dmax, np.maximum, -1), -1)
    for _ in range(DEPTH - 1):
        lo = np.where(dig, np.minimum(lo, _shift(lo, np.minimum, INF)), INF)
        hi = np.where(dig, np.maximum(hi, _shift(hi, np.maximum, -1)), -1)
    bad = dig & (hi >= 0) & (lo < INF) & (hi - lo > LIMIT)
    return bad, hi - lo, ya, unreached

if __name__ == "__main__":
    for fl in g.MAZES:
        bad, gap, ya, unreached = shortcuts(fl)
        print(f"T{fl} mê cung: ô khí chưa tới được {unreached}, đường tắt qua tường: {int(bad.sum())}")
        for i, j, k in list(zip(*np.nonzero(bad)))[:5]:
            print("   ví dụ", i + g.X0, j + ya + g.Y0, k + g.X0, "chênh", int(gap[i, j, k]))
