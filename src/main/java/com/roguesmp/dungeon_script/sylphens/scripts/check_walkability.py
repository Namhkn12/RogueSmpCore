"""Mô phỏng người chơi đi bộ có trọng lực (bước lên 1 khối, rơi, bơi) từ Sảnh Chính.
In số bước tới mỗi thang/Thủ Vệ và danh sách phòng không tới được (phải rỗng).
Chạy: python3 scripts/check_walkability.py [seed]"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
_seed_args = sys.argv[1:]
sys.argv = [sys.argv[0]] + _seed_args   # cho phép truyền seed: python3 scripts/xxx.py 777
import generate_layer2 as g   # import = chạy lại bộ sinh (cùng seed) để có mảng khối W trong RAM
import numpy as np, json
from collections import deque
W=g.W; PAL=g.PAL; Y0=g.Y0
inv={v:k for k,v in PAL.items()}
soft=set()
for n,i in PAL.items():
    if any(t in n for t in ("air","water","lily_pad","moss_carpet","red_mushroom","brown_mushroom","amethyst_cluster","large_amethyst_bud","soul_lantern","chain","lantern","pointed_dripstone","structure_void","torch","cobweb","pressure_plate")) and "block" not in n:
        soft.add(i)
soft_arr=np.isin(W,list(soft))
water=(W==PAL["minecraft:water"])
NX,NY,NZ=W.shape
def stand(i,j,k):
    if not(0<=i<NX and 1<=j<NY-1 and 0<=k<NZ): return False
    if not(soft_arr[i,j,k] and soft_arr[i,j+1,k]): return False
    return (not soft_arr[i,j-1,k]) or water[i,j,k] or water[i,j-1,k]
def walk(start):
    seen={start:0}; dq=deque([start])
    while dq:
        c=dq.popleft(); i,j,k=c; d0=seen[c]
        nb=[]
        for di,dk in ((1,0),(-1,0),(0,1),(0,-1)):
            for dj in (0,1,-1):
                n=(i+di,j+dj,k+dk)
                if dj==1 and not soft_arr[i,j+2,k]: continue
                if stand(*n): nb.append(n); break
            else:
                # rơi: đi ngang vào ô trống rồi rơi xuống
                ii,kk=i+di,k+dk
                if 0<=ii<NX and 0<=kk<NZ and soft_arr[ii,j,kk] and soft_arr[ii,j+1,kk]:
                    jj=j
                    while jj>1 and soft_arr[ii,jj-1,kk] and not water[ii,jj-1,kk]: jj-=1
                    if stand(ii,jj,kk): nb.append((ii,jj,kk))
        if water[i,j,k] and stand(i,j+1,k): nb.append((i,j+1,k))
        for n in nb:
            if n not in seen: seen[n]=d0+1; dq.append(n)
    return seen
st=g.I(0,g.F[1]+1,8)
seen=walk(st)
print("ô đứng được tới:",len(seen))
def best_in_room(r, y):
    m=r.mask2d(); best=None
    for (i,j,k),d in seen.items():
        if j==y-Y0 and m[i,k]:
            if best is None or d<best: best=d
    return best
for r in g.ROOMS:
    if r.kind in ("guardian","stair","arrival") :
        y = r.F+1 if r.kind!="stair" else g.F[r.fl]+1
        print(f"T{r.fl} {r.name:<28} bước đi từ sảnh: {best_in_room(r,y)}")
miss=[r.name for r in g.ROOMS if r.kind not in("maze","atrium_bot","stair") and best_in_room(r, (g.F[r.fl] if r.kind=="atrium" else r.F)+1) is None]
print("phòng không đi bộ tới được:",miss)
