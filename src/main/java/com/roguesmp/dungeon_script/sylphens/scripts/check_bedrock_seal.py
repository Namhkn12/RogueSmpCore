"""Kiểm tra tấm bedrock ngăn tầng:
 1) bedrock không lộ ra khoảng khí nào (phải = 0)
 2) mô phỏng đào mọi khối trừ bedrock từ tầng k: không được tới tầng k+1 trừ qua thang/giếng trời (phải None)
Chạy: python3 scripts/check_bedrock_seal.py [seed]"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
_seed_args = sys.argv[1:]
sys.argv = [sys.argv[0]] + _seed_args   # cho phép truyền seed: python3 scripts/xxx.py 777
import generate_layer2 as g   # import = chạy lại bộ sinh (cùng seed) để có mảng khối W trong RAM
import json, numpy as np
from collections import deque
W=g.W; PAL=g.PAL; inv={v:k for k,v in PAL.items()}
vis=0
# 2) bedrock lộ ra cạnh ô khí?
air=np.isin(W,[PAL["minecraft:air"]]); bed=W==PAL["minecraft:bedrock"]
for d in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]:
    vis+= (bed & np.roll(air,d,axis=(0,1,2))).sum()
print("mặt bedrock lộ ra khí:",vis)
# 3) thử đào xuyên: mọi khối trừ bedrock đều đào được; vòng ngoài r>88 tại tấm coi như bedrock (lệnh bổ sung)
GR=g.GR
for k,ys in g.SEAL_Y.items():
    j=ys-g.Y0
    dig=(W!=PAL["minecraft:bedrock"])
    dig[:,j,:][(GR>88)]=False             # lệnh vòng ngoài
    block=np.zeros(W.shape[::2],bool)
    for s in g.STAIRS:
        if s.fl==k: block|=s.foot(3)
    for a in g.ATRIA:
        if a.fl==k: block|=a.foot(3)
    dig[:, :, :][np.broadcast_to(block[:,None,:],W.shape)]=False
    # nguồn: toàn bộ ô khí tầng trên (sàn+1), đích: ô khí tầng dưới
    src=np.argwhere(air[:, g.F[k]+1-g.Y0, :] & ~block)
    seen=np.zeros(W.shape,bool); dq=deque()
    for i,kk in src[:4000]:
        c=(i,g.F[k]+1-g.Y0,kk)
        if dig[c]: seen[c]=True; dq.append(c)
    hit=None
    lo=g.F[k+1]+1-g.Y0
    while dq and hit is None:
        c=dq.popleft()
        for d in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
            n=(c[0]+d[0],c[1]+d[1],c[2]+d[2])
            if 0<=n[0]<W.shape[0] and lo<=n[1]<=g.F[k]+13-g.Y0 and 0<=n[2]<W.shape[2] and dig[n] and not seen[n]:
                seen[n]=True; dq.append(n)
                if n[1]<j-6: hit=(n[0]+g.X0,n[1]+g.Y0,n[2]+g.X0)
    print(f"tầng {k}->{k+1}: đào xuyên tấm được? ", hit)
