#!/usr/bin/env python3
"""Run independent, resumable Chromium/JPro fixture matrix segments. Server and CDP must already run."""
import argparse, subprocess
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--start',type=int,default=0)
a=p.parse_args()
root=Path(__file__).resolve().parent.parent
for start in range(a.start,14,2):
    end=min(start+2,14)
    print(f'Browser matrix {start}:{end}',flush=True)
    subprocess.run(['node','scripts/visual/browser-ui3-matrix.mjs',str(start),str(end)],cwd=root,check=True)
