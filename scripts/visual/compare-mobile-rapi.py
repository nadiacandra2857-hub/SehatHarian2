#!/usr/bin/env python3
"""Compose actual screenshots beside the supplied reference; no pixels are generated or retouched."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json
root=Path(__file__).resolve().parents[2]
base=root/'docs/screenshots/mobile-rapi';out=base/'compare';out.mkdir(exist_ok=True)
refs=json.loads((root/'docs/verification/mobile-rapi/reference-map.json').read_text())
try:font=ImageFont.truetype('DejaVuSans.ttf',16)
except OSError:font=ImageFont.load_default()
for screen,key in [('beranda','beranda'),('latihan-aktif','latihan'),('latihan-jeda','latihan'),('nutrisi','nutrisi'),('hidrasi','hidrasi')]:
 for width in [390,360]:
  paths=[root/'Reference'/refs[key],base/'before'/f'{screen}-{width}.png',base/'after'/f'{screen}-{width}.png']
  images=[]
  for p in paths:
   image=Image.open(p).convert('RGB');images.append(image.resize((360,round(image.height*360/image.width))))
  sheet=Image.new('RGB',(1112,max(i.height for i in images)+44),'#eef2f7');draw=ImageDraw.Draw(sheet)
  for i,(im,label) in enumerate(zip(images,['Reference','Sebelum (JPro aktual)','Sesudah (JPro aktual)'])):
   x=i*376;sheet.paste(im,(x,40));draw.text((x+8,10),label,fill='#122034',font=font)
  sheet.save(out/f'{screen}-{width}.png')
