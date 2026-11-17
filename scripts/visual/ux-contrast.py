#!/usr/bin/env python3
"""Read-only measurement of exported JPro DOM colors plus screenshot backgrounds."""
import json, math, re
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'docs/verification/ux-latihan-20261106'
SHOTS=ROOT/'docs/screenshots/ux-latihan-20261106'
def rgb(value):
    return tuple(map(float,re.findall(r'[\d.]+',value)[:3]))
def luminance(color):
    values=[v/255 for v in color]
    values=[v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in values]
    return sum(v*w for v,w in zip(values,[.2126,.7152,.0722]))
def ratio(a,b):
    x,y=sorted([luminance(a),luminance(b)])
    return (y+.05)/(x+.05)
def hx(c):return '#'+''.join(f'{round(v):02X}' for v in c)
rows=[]
cases=[('catalog-secondary','Cari nama latihan (bahasa Inggris)',False),
       ('catalog-secondary','Pemula · Sepeda',False),
       ('dropdown','Pemula',False),('dropdown-selected','Pemula',False),('combo-selected','Pemula',False),
       ('spinner','5',False),('saved-disabled','Simpan aktivitas',True)]
for state,text,disabled in cases:
    path=OUT/f'styles-{state}.json'
    if not path.exists():continue
    data=json.loads(path.read_text());found=[r for r in data if r['text']==text]
    if not found:continue
    r=found[-1] if state=='dropdown-selected' else found[0];rect=r['rect'];im=Image.open(SHOTS/f'audit-{state}.png').convert('RGB')
    point=(max(0,round(rect['x']-4)),min(im.height-1,round(rect['y']+rect['h']/2)))
    bg=im.getpixel(point);alpha=math.prod(float(a['opacity']) for a in r['chain'])
    color=rgb(r['style']['color']);canvas=(17,24,39)
    effective=tuple(alpha*v+(1-alpha)*u for v,u in zip(color,canvas))
    rows.append(dict(state=state,text=text,source=hx(color),opacity=alpha,background=hx(bg),sample=point,effective=hx(effective),ratio=round(ratio(effective,bg),2),disabled=disabled))
# Historical captures are intentionally excluded from this run.
controls=[dict(foreground=a,background=b,ratio=round(ratio(tuple(bytes.fromhex(a[1:])),tuple(bytes.fromhex(b[1:]))),2)) for a,b in [('#E5E7EB','#1F2937'),('#111827','#22C55E'),('#FDE68A','#451A03'),('#22C55E','#1F2937')]]
(OUT/'contrast.json').write_text(json.dumps(dict(method='Current-run JPro DOM color and product of ancestor opacity; screenshot background sampled four pixels left of label. Opaque text in a JavaFX opacity group is composited over the #111827 page canvas (separately reported disabled state). Solid-color estimate, not text-edge pixels or a full accessibility certification.',controls=controls,measurements=rows),indent=2))
print(json.dumps(dict(controls=controls,measurements=rows),indent=2))
