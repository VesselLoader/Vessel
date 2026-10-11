#!/usr/bin/env python3
"""Rebuilds repo-starter/README.md from MASTER-SPEC-STATUS.md (run after every pass).
Usage: python3 tools/build_readme.py <package root> <readme version>
All numbers come from the status file tables; nothing is typed by hand."""
import re,subprocess,sys,json
root=sys.argv[1]; rver=sys.argv[2]
S=open(f'{root}/MASTER-SPEC-STATUS.md',encoding='utf-8').read().split('\n')
now=subprocess.check_output(['date','-u','+%Y-%m-%d']).decode().strip()
sver=[l for l in S if l.startswith('**Document version')][0].split(':** ')[1].strip()
def sect(start,end):
    a=next(i for i,l in enumerate(S) if l.startswith(start)); b=next(i for i,l in enumerate(S) if l.startswith(end)); return S[a:b]
ph=[]
for l in sect('## 3.','## 4.'):
    m=re.match(r'^\| (\d+)\. ([^|]+)\| ([^|]+)\| (\d+)% \|',l)
    if m: ph.append((int(m.group(1)),m.group(2).strip(),m.group(3).strip(),int(m.group(4))))
assert len(ph)==23
overall=round(sum(p[3] for p in ph)/23,1)
rows=[m for m in (re.match(r'^\| (\d+) \| ([^|]+) \| (.*)\|$',l) for l in sect('### 4.1','### 4.3')) if m]
fdone=sum(1 for m in rows if m.group(3).strip().startswith('DONE'))
st=[m for m in (re.match(r'^\| (\d+) \| .*? \| (deep|medium) \| (DONE|PARTIAL|NOT STARTED) \|',l) for l in sect('### 4.4','## 5.')) if m]
c={k:sum(1 for m in st if m.group(3)==k) for k in ['DONE','PARTIAL','NOT STARTED']}
apct=round(100*(c['DONE']+0.5*c['PARTIAL'])/len(st),1)
ndec=len([l for l in sect('## 2.','## 3.') if re.match(r'^\| \d+ \| ',l)])
phase_rows="\n".join(f"| {n} | {name} | {stt} | {pct}% |" for n,name,stt,pct in ph)
tpl=open(f'{root}/tools/README.template.md',encoding='utf-8').read()
out=tpl
for k,v in {'{{README_VERSION}}':rver,'{{DATE}}':now,'{{STATUS_VERSION}}':sver,'{{NDEC}}':str(ndec),'{{FDONE}}':str(fdone),'{{FROWS}}':str(len(rows)),
            '{{ADONE}}':str(c['DONE']),'{{APART}}':str(c['PARTIAL']),'{{AROWS}}':str(len(st)),'{{APCT}}':str(apct),'{{OVERALL}}':str(overall),'{{PHASE_ROWS}}':phase_rows}.items():
    out=out.replace(k,v)
assert '{{' not in out, re.findall(r'\{\{[A-Z_]+\}\}',out)
open(f'{root}/repo-starter/README.md','w',encoding='utf-8').write(out)
print('README built:',sver,rver,'phases',len(ph),'overall',overall,'forge',fdone,len(rows),'stageA',c,apct,'decisions',ndec)
