#!/usr/bin/env python3
"""Stop only a verified SehatHarian sleep-test JPro from an explicit isolated directory."""
import argparse
import os
from pathlib import Path
import signal

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('instance',type=Path)
p.add_argument('database')
a=p.parse_args()
instance=a.instance.resolve()
if not str(instance).startswith('/tmp/') or not a.database.startswith('sehatharian_test_sleep_'):
    p.error('An isolated /tmp instance and sehatharian_test_sleep_* database are required')
marker=instance/'Frontend2/RUNNING_PID'
if not marker.exists():raise SystemExit('Test process already stopped; no RUNNING_PID marker')
pid=int(marker.read_text().strip())
try:
    fields=(Path('/proc')/str(pid)/'environ').read_bytes().split(b'\0')
    cwd=(Path('/proc')/str(pid)/'cwd').resolve()
except FileNotFoundError:
    raise SystemExit('Test process already stopped; marker retained for launcher inspection')
if f'DB_NAME={a.database}'.encode() not in fields or not cwd.is_relative_to(instance):
    p.error('PID identity or working directory differs; process preserved')
os.kill(pid,signal.SIGTERM)
print(f'Stopped verified sleep-test JPro {pid} in {instance}')
