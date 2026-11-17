#!/usr/bin/env python3
"""Verify CSV/JSON equivalence, write normalized import properties. No network or database writes."""
import csv,json,hashlib,argparse,xml.etree.ElementTree as ET
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--source',default='datashetmakanan');p.add_argument('--output',default='.runtime/foods.xml');a=p.parse_args()
root=Path(a.source); raw=(root/'tempolife-foods.csv').read_bytes();rows=list(csv.DictReader(raw.decode('utf-8-sig').splitlines()));other=json.loads((root/'tempolife-foods.json').read_text()) if (root/'tempolife-foods.json').exists() else None
keys={'calories':'kcal_per_100g','protein':'protein_g','carbs':'carbs_g','fat':'fat_g','fiber':'fiber_g','sugar':'sugar_g','saturated_fat':'saturated_fat_g','salt':'salt_g'}
if other is not None:assert len(rows)==len(other)
assert len({r['id'] for r in rows})==len(rows)
for r,j in zip(rows,other or []):
 for key in ['name','brand','category','source']:
  assert (r[key] or None)==j[key],(r['id'],key)
 for c,k in keys.items():
  value=float(r[c]) if r[c] else None
  assert value==j[k],(r['id'],c)
  assert value is None or value>=0
props=ET.Element('properties');ET.SubElement(props,'comment').text='Food nutrition data from TempoLife (tempolife.app), CC-BY-4.0.'
def entry(k,v):ET.SubElement(props,'entry',key=k).text=str(v)
entry('count',len(rows));entry('version',hashlib.sha256(raw).hexdigest());entry('source','https://github.com/tempolife-app/tempo-food-db/tree/main/data');entry('license','CC-BY-4.0')
for i,r in enumerate(rows):
 for k,v in r.items():entry(f'{i}.{k}',v)
out=Path(a.output);out.parent.mkdir(parents=True,exist_ok=True)
out.write_text('<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">\n'+ET.tostring(props,encoding='unicode'))
print(json.dumps({'count':len(rows),'equivalent':True if other is not None else 'not compared: CSV-only release','version_sha256':hashlib.sha256(raw).hexdigest(),'output':str(out),'units':'kcal/100g; other nutrients g/100g (JSON keys)','null_source':sum(not r['source'] for r in rows)},indent=2))
