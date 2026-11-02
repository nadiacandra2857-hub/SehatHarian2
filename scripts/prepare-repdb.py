#!/usr/bin/env python3
"""Explicit local in-app RepDB preparation. Never downloads premium assets or touches SQL.
Requires Pillow with WebP for deterministic PNG conversion. Output is NOT for dataset redistribution.
"""
import argparse, collections, hashlib, io, json, re, shutil, tempfile, urllib.request
from pathlib import Path
import xml.etree.ElementTree as ET

COMMIT = '66c4298af8d99b9dc397dad6699e508fb871d249'
BASE = f'https://raw.githubusercontent.com/RepDB/exercise-dataset/{COMMIT}/'
SCHEMA = json.loads(Path(__file__).with_name('repdb-schema.json').read_text())
ID = re.compile(r'[a-z0-9]+(?:-[a-z0-9]+)*\Z')
ASSET = re.compile(r'images/flat/[a-z0-9][a-z0-9-]*\.webp\Z')


def text(value):
    if not isinstance(value, str) or not value.strip() or len(value) > 16000:
        raise ValueError('teks wajib tidak valid')
    if any(ord(c) < 32 and c not in '\n\r\t' for c in value):
        raise ValueError('karakter kontrol tidak valid')
    return value.strip()


def strings(e, key, required=False):
    value = e.get(key, [])
    if not isinstance(value, list) or (required and not value):
        raise ValueError(f'{key}: daftar tidak valid')
    return [text(x) for x in value]


def validate(e):
    if not isinstance(e, dict) or not ID.fullmatch(text(e.get('id'))):
        raise ValueError('ID tidak valid')
    row = {k: text(e.get(k)) for k in ['id', 'name_en', 'difficulty', 'body_part', 'category']}
    if len(row['name_en']) > 100:
        raise ValueError('nama melebihi kapasitas catatan aktivitas')
    for key in SCHEMA:
        if key in e and e[key] not in SCHEMA[key]:
            raise ValueError(f'{key}: enum tidak dikenal')
    row['equipment'] = e.get('equipment', '_unspecified')
    row['description'] = text(e['description_en']) if e.get('description_en') else ''
    for key in ['instructions_en', 'tips_en', 'primary_muscles', 'secondary_muscles']:
        row[key] = strings(e, key, key == 'instructions_en')
    flat = e.get('images', {}).get('flat', {})
    if not isinstance(flat, dict) or not (('start' in flat and 'peak' in flat) or 'main' in flat):
        raise ValueError('gambar memerlukan start/peak atau main')
    row['images'] = {}
    for pose in ['start', 'peak', 'main']:
        if pose in flat:
            if not isinstance(flat[pose], str) or not ASSET.fullmatch(flat[pose]):
                raise ValueError('path aset tidak aman')
            row['images'][pose] = flat[pose]
    return row


def parse(data):
    if not isinstance(data, dict) or data.get('schema_version') != 3 or not isinstance(data.get('exercises'), list):
        raise ValueError('schema RepDB tidak didukung')
    if data.get('count') != len(data['exercises']):
        raise ValueError('count tidak sesuai')
    ids = collections.Counter(e.get('id') for e in data['exercises'] if isinstance(e, dict) and isinstance(e.get('id'), str))
    rows, errors = [], []
    for index, e in enumerate(data['exercises']):
        try:
            row = validate(e)
            if ids[row['id']] != 1:
                raise ValueError('duplikasi ID')
            rows.append(row)
        except (ValueError, TypeError, AttributeError) as exc:
            errors.append({'index': index, 'reason': str(exc)})
    return rows, errors


def fetch(path):
    with urllib.request.urlopen(BASE + path, timeout=45) as response:
        return response.read()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output', type=Path, default=Path('.runtime/repdb'))
    p.add_argument('--source', type=Path, help='JSON lokal dari commit yang dipatok; hash tetap diperiksa')
    p.add_argument('--limit', type=int, default=24, help='jumlah latihan lokal, default 24; bukan seluruh dataset')
    args = p.parse_args()
    if not 1 <= args.limit <= 701:
        p.error('limit harus 1–701; pastikan izin pengemasan sebelum menyertakan seluruh dataset')
    from PIL import Image
    raw = args.source.read_bytes() if args.source else fetch('exercises.json')
    # Pinned content hash prevents mislabeled local source and silent upstream drift.
    expected = 'ecaeece74163382d74d0082639cee83b466dec5c3e2430efaf327e3dd5357443'
    if hashlib.sha256(raw).hexdigest() != expected:
        raise ValueError('SHA256 JSON tidak sesuai commit sumber')
    rows, errors = parse(json.loads(raw))
    # Representative deterministic subset across category/difficulty; not a suitability ranking.
    groups = collections.defaultdict(list)
    for row in sorted(rows, key=lambda r: r['id']):
        groups[(row['category'], row['difficulty'])].append(row)
    selected = []
    while len(selected) < min(args.limit, len(rows)):
        for key in sorted(groups):
            if groups[key] and len(selected) < args.limit:
                selected.append(groups[key].pop(0))
    target = args.output.resolve()
    target.parent.mkdir(parents=True, exist_ok=True)
    previous = json.loads((target / 'manifest.json').read_text()) if (target / 'manifest.json').is_file() else {}
    with tempfile.TemporaryDirectory(prefix='repdb-', dir=target.parent) as directory:
        stage = Path(directory)
        hashes, asset_errors, imported = {}, [], []
        for row in selected:
            converted = {}
            for pose, path in row['images'].items():
                png = path.removesuffix('.webp') + '.png'
                try:
                    if path not in hashes:
                        local = target / path
                        image = local.read_bytes() if local.is_file() and previous.get('commit') == COMMIT else b''
                        if not image or hashlib.sha256(image).hexdigest() != previous.get('assets', {}).get(path, {}).get('source_sha256'):
                            image = fetch(path)
                        dest = stage / png
                        dest.parent.mkdir(parents=True, exist_ok=True)
                        with Image.open(io.BytesIO(image)) as im:
                            im.convert('RGBA').save(dest, format='PNG', optimize=False, compress_level=9)
                        # Keep the original locally to test the actual JavaFX WebP decoder.
                        (stage / path).write_bytes(image)
                        hashes[path] = {'source_sha256': hashlib.sha256(image).hexdigest(),
                                        'png_sha256': hashlib.sha256(dest.read_bytes()).hexdigest(), 'png': png}
                    converted[pose] = png
                except Exception as exc:
                    asset_errors.append({'id': row['id'], 'path': path, 'reason': str(exc)})
                    converted[pose] = png  # Missing file intentionally renders an explicit placeholder.
            row['images'] = converted
            imported.append(row)
        props = {'format': '1', 'source': 'RepDB Free Tier', 'commit': COMMIT, 'license': 'RepDB Free Tier v1.0',
                 'language': 'en', 'count': str(len(imported)), 'schema': '3'}
        for i, row in enumerate(imported):
            for key, value in row.items():
                prefix = f'exercise.{i}.{key}'
                if isinstance(value, dict):
                    props.update({prefix + '.' + k: v for k, v in value.items()})
                elif isinstance(value, list):
                    props[prefix] = '\n'.join(value)
                else:
                    props[prefix] = value
        root = ET.Element('properties')
        for k, v in sorted(props.items()):
            ET.SubElement(root, 'entry', key=k).text = v
        xml = b'<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">\n' + ET.tostring(root, encoding='utf-8')
        (stage / 'catalog.xml').write_bytes(xml)
        for filename in ['README.md', 'LICENSE-DATA.md', 'ATTRIBUTION.md']:
            (stage / filename).write_bytes(fetch(filename))
        manifest = {'source': BASE, 'commit': COMMIT, 'license': 'RepDB Free Tier v1.0', 'schema_version': 3,
                    'json_sha256': expected, 'available': len(rows), 'invalid': errors,
                    'imported': len(imported), 'ids': [r['id'] for r in imported], 'assets': hashes,
                    'asset_errors': asset_errors, 'conversion': 'Pillow RGBA PNG, compress_level=9, no resize, no generative AI'}
        (stage / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
        # Publish catalog last; readers never see half-written XML. No source or database mutation.
        target.mkdir(parents=True, exist_ok=True)
        for file in stage.rglob('*'):
            if file.is_file() and file.name != 'catalog.xml':
                dest = target / file.relative_to(stage); dest.parent.mkdir(parents=True, exist_ok=True); shutil.copy2(file, dest)
        (target / 'catalog.xml.tmp').write_bytes(xml)
        (target / 'catalog.xml.tmp').replace(target / 'catalog.xml')
    print(json.dumps({'source': BASE, 'valid': len(rows), 'invalid': len(errors), 'imported': len(imported),
                      'assets': len(hashes), 'asset_failures': len(asset_errors), 'output': str(target)}))
    if errors or asset_errors:
        raise SystemExit(1)

if __name__ == '__main__':
    main()
