from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
root=Path(__file__).resolve().parents[2];base=root/'docs/screenshots/visual-lanjutan';out=base/'compare';out.mkdir(exist_ok=True)
try:font=ImageFont.truetype('DejaVuSans.ttf',16)
except OSError:font=ImageFont.load_default(size=16)
for before in (base/'before').glob('*-390.png'):
 after=base/'after'/before.name
 if not after.exists():continue
 images=[Image.open(p).convert('RGB') for p in [before,after]]
 sheet=Image.new('RGB',(800,884),'#EAF0F5');draw=ImageDraw.Draw(sheet)
 for i,(im,label) in enumerate(zip(images,['Sebelum · JPro aktual','Sesudah · JPro aktual'])):
  sheet.paste(im.resize((390,844),Image.Resampling.LANCZOS),(i*410,40));draw.text((i*410+8,12),label,font=font,fill='#203047')
 sheet.save(out/before.name)
