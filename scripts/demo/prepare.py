#!/usr/bin/env python3
"""Compile explicit demo tool using existing build and cached dependencies."""
import os, pathlib, subprocess, sys
root=pathlib.Path(__file__).resolve().parents[2]
os.chdir(root)
env=dict(os.environ,DB_HOST='127.0.0.1',DB_PORT='13306',DB_NAME='sehatharian_demo_db')
cache=pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1'
jars=[]
for group,name in [('com.mysql','mysql-connector-j'),('org.mindrot','jbcrypt')]:
    jars.append(str(next((cache/group/name).rglob('*.jar'))))
out=root/'.runtime/demo-tool';out.mkdir(parents=True,exist_ok=True)
cp=os.pathsep.join([str(root/'Backend2/build/classes/java/main'),str(root/'Backend2/src/main/resources'),*jars])
subprocess.run(['javac','--release','27','-cp',cp,'-d',str(out),'scripts/demo/Demo90.java'],check=True)
subprocess.run(['java','-cp',str(out)+os.pathsep+cp,'Demo90',*(sys.argv[1:] or ['--dry-run'])],check=True,env=env)
