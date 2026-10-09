"""Vẽ bản đồ 5 tầng Layer 2 (lát cắt ở sàn+2) -> images/layer2_floor_maps.png
Chạy: python3 scripts/render_layer2_maps.py [seed]"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
_seed_args = sys.argv[1:]
sys.argv = [sys.argv[0]] + _seed_args   # cho phép truyền seed: python3 scripts/xxx.py 777
import generate_layer2 as g   # import = chạy lại bộ sinh (cùng seed) để có mảng khối W trong RAM
import math
import numpy as np
import matplotlib; matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.colors import to_rgb
W=g.W; AIR=g.AIR; WATER=g.PAL["minecraft:water"]
COL={"combat":"#d9534f","arena":"#e8743b","barracks":"#b5523b","chest":"#f0ad4e","trap":"#8e44ad","res":"#2e9e5b",
     "checkpoint":"#3a87ad","teleport":"#6f42c1","cavern":"#9c8b6e","hall":"#c9a227","guardian":"#b03060",
     "stair":"#ff7f0e","arrival":"#ffd08a","atrium":"#5bc0de","atrium_bot":"#5bc0de","maze_combat":"#e05570"}
TRAP_MK={"pit":("v","#ff2d2d","Bẫy: hố chông"),"arrow":("x","#ffd400","Bẫy: tấm áp suất + dispenser"),
         "web":("*","#dddddd","Bẫy: mạng nhện")}
LBL={"combat":"Phòng Canh (combat)","arena":"Đấu Trường (combat)","barracks":"Hầm Lính (combat)","chest":"Rương",
     "trap":"Hành lang bẫy","res":"Phòng tài nguyên","checkpoint":"Checkpoint","teleport":"Dịch chuyển thoát",
     "cavern":"Hang đá tự nhiên","atrium":"Giếng trời (cao 2 tầng)","stair":"Thang xoắn ↓","arrival":"Chân thang (từ trên xuống)",
     "hall":"Sảnh Chính","guardian":"Thủ Vệ Rễ","maze_combat":"Phòng combat trong mê cung"}
fig,axs=plt.subplots(2,3,figsize=(21,14.5))
for idx,fl in enumerate(range(1,6)):
    ax=axs.flat[idx]
    F=g.F[fl]
    img=np.ones((g.NX,g.NZ,3))*0.16
    sl=W[:,F+2-g.Y0,:]; sl1=W[:,F+1-g.Y0,:]
    open_=(sl==AIR)|(sl1==AIR)|(sl1==WATER)|(sl==WATER)
    img[open_]=to_rgb("#a7b0b8")
    net=g.FLOORNETS.get(fl)
    mz=g.MAZES.get(fl)
    if mz is not None:
        img[open_]=to_rgb("#7a6a53")
    for c in (net.cells if net else []):
        x,z=g.cell_xy(*c); st=net.style.get(c,"corr")
        if st=="maze": img[x-g.X0:x-g.X0+3,z-g.X0:z-g.X0+3][open_[x-g.X0:x-g.X0+3,z-g.X0:z-g.X0+3]]=to_rgb("#7a6a53")
        if st=="cave": img[x-g.X0:x-g.X0+3,z-g.X0:z-g.X0+3][open_[x-g.X0:x-g.X0+3,z-g.X0:z-g.X0+3]]=to_rgb("#c7b48a")
    rooms=[r for r in g.ROOMS if r.kind!="maze" and (r.fl==fl or (r.kind=="atrium" and r.span==fl))]
    for r in rooms:
        m=r.mask2d()&open_
        if r.kind=="stair": m=r.mask2d()
        img[m]=to_rgb(COL["maze_combat"] if getattr(r,"in_maze",False) else COL[r.kind])
    for r in [r for r in g.ROOMS if r.kind=="stair" and r.fl+1==fl]:
        pass
    ax.imshow(np.transpose(img,(1,0,2)),extent=(g.X0-.5,g.X1+.5,g.X1+.5,g.X0-.5),interpolation='nearest')
    for r in rooms:
        t=None
        if r.kind=="stair": t=("A" if " A " in r.name else "B")+"↓"
        elif r.kind=="arrival": t="đến "+("A" if " A " in r.name else "B")
        elif r.kind in("res",): t=r.name
        elif r.kind in("checkpoint","teleport","hall","guardian"): t={"checkpoint":"CP","teleport":"TP thoát","hall":"Sảnh","guardian":"THỦ VỆ"}[r.kind]
        elif r.kind=="atrium": t="Giếng trời" + (" (ban công)" if r.fl==fl else " (đáy, hồ)")
        if t: ax.text(r.cx,r.cz,t,ha='center',va='center',fontsize=7.5,color='white',weight='bold',
                      bbox=dict(boxstyle='round,pad=0.15',fc='black',alpha=0.45,lw=0))
    if mz is not None:
        for t,x,y,z in mz.trap_pos:
            ax.plot(x,z,TRAP_MK[t][0],color=TRAP_MK[t][1],ms=9,mew=2.5)
    ax.add_patch(plt.Circle((0,0),100,fill=False,ls='--',color='#888'))
    ax.set_xlim(-100,100); ax.set_ylim(100,-100)
    nroom=len([r for r in rooms if r.kind not in("arrival","stair")])
    sub=(f"MÊ CUNG {len(mz.cells)} ô · {len(mz.rooms)} phòng combat · {len(mz.trap_pos)} bẫy" if mz is not None
         else f"{nroom} phòng")
    ax.set_title(f"Tầng {fl} — sàn Y{F} · {sub}",fontsize=13,weight='bold')
    ax.text(0,-96,"BẮC",ha='center',fontsize=8,color='#555'); ax.tick_params(labelsize=7)
ax=axs.flat[5]; ax.axis('off')
STEP=0.041
items=list(LBL.items())+[("_corr","Hành lang gạch"),("_maze","Mê cung (tường bedrock phủ đá)"),("_cave","Đường hang tự nhiên")]
extra={"_corr":"#a7b0b8","_maze":"#7a6a53","_cave":"#c7b48a"}
for j,(k,l) in enumerate(items):
    ax.add_patch(plt.Rectangle((0.03,0.95-j*STEP),0.06,0.038,color=COL.get(k,extra.get(k)),transform=ax.transAxes))
    ax.text(0.12,0.969-j*STEP,l,transform=ax.transAxes,va='center',fontsize=11)
for j,(t,(mk,c,l)) in enumerate(TRAP_MK.items()):
    yy=0.95-(len(items)+j)*STEP+0.02
    ax.plot([0.06],[yy],mk,color=c,ms=10,mew=2.5,transform=ax.transAxes)
    ax.text(0.12,yy,l,transform=ax.transAxes,va='center',fontsize=11)
ax.text(0.03,0.02,f"Seed {g.SEED}. Lát cắt ở độ cao sàn+2 mỗi tầng. Trục ngang x, dọc z.",transform=ax.transAxes,fontsize=10)
plt.tight_layout(); plt.savefig(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'images', 'layer2_floor_maps.png'),dpi=100)
print("ok")
