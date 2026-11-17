from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json
root=Path(__file__).resolve().parents[2]
base=root/'docs/screenshots/beranda-visual'
try:font=ImageFont.truetype('DejaVuSans.ttf',15)
except OSError:font=ImageFont.load_default(size=15)
reference=root/'Reference'/json.loads((root/'docs/verification/mobile-rapi/reference-map.json').read_text())['beranda']
for width in [390,360]:
 for baseline,label in [('baseline-terakhir','Baseline mobile terakhir'),('before','Sebelum · capture baru')]:
  before=root/'docs/screenshots/mobile-rapi/after'/f'beranda-{width}.png' if baseline=='baseline-terakhir' else base/'before'/f'beranda-{width}.png'
  paths=[reference,before,base/'after'/f'beranda-{width}.png']
  images=[]
  for p in paths:
   im=Image.open(p).convert('RGB');images.append(im.resize((390,round(im.height*390/im.width)),Image.Resampling.LANCZOS))
  sheet=Image.new('RGB',(1202,max(im.height for im in images)+44),'#EAF0F5');draw=ImageDraw.Draw(sheet)
  for i,(im,title) in enumerate(zip(images,['Referensi komposisi',label,'Sesudah · JPro aktual'])):
   x=i*406;sheet.paste(im,(x,44));draw.text((x+8,13),title,font=font,fill='#203047')
  sheet.save(base/f'compare-{baseline}-{width}.png')
