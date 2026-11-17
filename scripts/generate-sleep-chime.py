#!/usr/bin/env python3
"""Original mathematical sine chime, no sampled third-party material; 6 seconds with fades."""
from pathlib import Path
import math,struct,wave
out=Path(__file__).resolve().parent.parent/'Frontend2/src/main/resources/sounds/sleep_gentle.wav'
with wave.open(str(out),'wb') as f:
 f.setparams((1,2,22050,0,'NONE','not compressed'))
 for n in range(6*22050):
  t=n/22050;envelope=min(1,t/.7)*max(0,1-t/6)**1.4
  signal=.22*envelope*(math.sin(2*math.pi*440*t)+.45*math.sin(2*math.pi*660*t))/1.45
  f.writeframesraw(struct.pack('<h',round(signal*32767)))
print(out)
