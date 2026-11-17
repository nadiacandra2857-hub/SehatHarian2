#!/usr/bin/env python3
"""Reviewable JSON -> bounded Java Properties XML; no network or database writes."""
from pathlib import Path
import json,csv,hashlib,argparse,xml.etree.ElementTree as ET
p=argparse.ArgumentParser();p.add_argument('--source',default='content/menus/awal-2026.json');p.add_argument('--output',default='.runtime/menus.xml');a=p.parse_args()
root=Path(__file__).resolve().parent.parent
raw=Path(a.source).read_bytes();data=json.loads(raw)
assert data['format_version']==1
catalog=(root/'datashetmakanan/tempolife-foods.csv').read_bytes();assert hashlib.sha256(catalog).hexdigest()==data['catalog_version_sha256'],'Catalogue changed; review package mapping before preparing'
rows={r['id']:r for r in csv.DictReader(catalog.decode('utf-8-sig').splitlines())}
assert 1<=len(data['menus'])<=50 and 1<=len(data['ingredients'])<=100
assert len({i['id']for i in data['ingredients']})==len(data['ingredients'])
assert len({i['key']for i in data['menus']})==len(data['menus'])
props=ET.Element('properties');ET.SubElement(props,'comment').text='Explicit curated import. CMS DRAFT only; not a startup seed.'
def put(k,v):
 if v is not None:ET.SubElement(props,'entry',key=k).text=str(v).lower() if isinstance(v,bool)else str(v)
def canonical(v):return json.dumps(v,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode()
put('format',1);put('package',data['package']);put('version',data['version']);put('ingredients',len(data['ingredients']));put('menus',len(data['menus']));put('package_sha256',hashlib.sha256(raw).hexdigest())
ingredients={i['id']:i for i in data['ingredients']}
for n,i in enumerate(data['ingredients']):
 for key in ['id','name','origin','assessment','source','basis']:put(f'i.{n}.{key}',i[key])
 put(f'i.{n}.allergens',','.join(sorted(i['allergens'])));put(f'i.{n}.aliases','\n'.join(i['aliases']))
for n,m in enumerate(data['menus']):
 assert m['key'].startswith('curated:') and m['source_version'] and m['url'].startswith('https://')
 if m['completeness']=='COMPLETE':assert m['servings'] and all(l['ingredient_id']and not l['unresolved_name'] for l in m['lines'])
 image=root/'Frontend2/src/main/resources'/m['image'].lstrip('/');assert image.is_file(),image
 used=[ingredients[l['ingredient_id']] for l in m['lines']if l['ingredient_id']]
 signature={'menu':m,'definitions':used,'image_sha256':hashlib.sha256(image.read_bytes()).hexdigest(),'catalog_sha256':data['catalog_version_sha256']}
 source=f"{m['publisher']} — {m['original_title']}\n{m['url']}\nDiakses {m['accessed']} · versi kurasi {m['source_version']}\nHasil sumber: {m['source_yield']}\nAdaptasi: {m['adaptation']}\n{m['limitations']}"
 body=f"{m['meal']} · ringkasan cara memasak\n{m['method']}\n\n{source}\n\nTakaran sumber / adaptasi:\n"+'\n'.join((ingredients[l['ingredient_id']]['name']if l['ingredient_id']else l['unresolved_name'])+': '+l['source_amount']for l in m['lines'])+'\n\nRingkasan/adaptasi Indonesia: SehatHarian, CC-BY-4.0. Bukan afiliasi atau dukungan penerbit sumber.'
 for k,v in dict(key=m['key'],version=m['source_version'],hash=hashlib.sha256(canonical(signature)).hexdigest(),title=m['title'],body=body,source=source,servings=m['servings'],completeness=m['completeness'],image=m['image'],caption=m['caption'],lines=len(m['lines'])).items():put(f'm.{n}.{k}',v)
 for j,l in enumerate(m['lines']):
  for k,v in l.items():
   if k!='source_amount':put(f'm.{n}.l.{j}.{k}',v)
  if l['food_id']:
   r=rows[l['food_id'].split(':',1)[1]];i=ingredients[l['ingredient_id']];assert r['name']==i['food_name']and l['food_version']==i['food_version']
   put(f'm.{n}.l.{j}.food_name',r['name']);put(f'm.{n}.l.{j}.food_source',r['source'])
   keys=['calories','protein','carbs','fat','fiber','sugar','saturated_fat','salt'];put(f'm.{n}.l.{j}.food_nutrients',';'.join(k+'='+str(float(r[k])) for k in keys if r[k]))
out=Path(a.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text('<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">\n'+ET.tostring(props,encoding='unicode'))
print(json.dumps({'package':data['package'],'version':data['version'],'menus':len(data['menus']),'ingredients':len(data['ingredients']),'complete':sum(m['completeness']=='COMPLETE'for m in data['menus']),'status':'prepared locally; no database writes','output':str(out)},indent=2))
