#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks FRI Folding Hardware Accelerator
# Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
# Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
# File: tools/emit_sv.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""生成独立FRI产品及验证环境，并核对C/Python参考。"""
from source_headers import stamp_generated
import argparse,json,os,re,shlex,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OP=Path('operators/fri-fold')
BUNDLED=Path('rtl/v1.1.0/lane1-m32768')

def relative_build(value):
    p=Path(value)
    if p.is_absolute() or '..' in p.parts or not p.parts or p.parts[0]!='build':
        raise ValueError('Output must be a relative directory under build')
    return p

def run(command,output,label,expected=0):
    with (output/(label+'.log')).open('x') as log:
        result=subprocess.run([str(x) for x in command],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
    if result.returncode!=expected:
        raise RuntimeError(f'{label}: expected exit {expected}, got {result.returncode}; see its log')

def code(text):
    return re.sub(r'/\*.*?\*/|//[^\n]*','',text,flags=re.S)

def product_closure(rtl):
    stamp_generated(rtl)
    definitions,texts={},{}
    for p in sorted(rtl.glob('*.sv')):
        text=re.sub(r'[ \t]*//[^\n]*\.scala:\d+:\d+[^\n]*','',p.read_text())
        p.write_text(text);texts[p]=code(text)
        for name in re.findall(r'\bmodule\s+(\w+)',texts[p]):
            assert name not in definitions,'Duplicate module'
            definitions[name]=p
    chosen=set();todo=['GoldilocksFriFold']
    while todo:
        p=definitions[todo.pop()]
        if p in chosen:continue
        chosen.add(p)
        instances=set(re.findall(r'^[ \t]*(\w+)\b[ \t]+(?:#\s*\([^;]*?\)\s*)?\w+\s*\(',texts[p],re.M|re.S))
        todo.extend(instances & definitions.keys())
    for p in chosen:
        stripped=re.sub(r'"(?:[^"\\]|\\.)*"','""',texts[p])
        assert not re.search(r'\$readmem|%|\b(?:DSP48\w*|RAMB\w*|URAM\w*|BUFG\w*|IOBUF)\b',stripped)
        assert 'SimTop' not in stripped
    return sorted(chosen)

def simulation_list(rtl,output,bundled=False):
    sources=sorted(rtl.rglob('*.sv'))
    if bundled:
        product=set(product_closure(rtl))
        sources=[p for p in sources if p not in product]+sorted(BUNDLED.glob('*.sv'))
    includes=sorted({p.parent for p in sources})
    output.write_text(''.join('+incdir+'+str(p)+'\n' for p in includes)+''.join(str(p)+'\n' for p in sources))

def generate(value,lanes=1,pairs=32768,tasks=16):
    os.chdir(ROOT);output=relative_build(value)
    if output.exists():raise ValueError('Output already exists; choose a new directory')
    if lanes not in (1,2) or not 8<=pairs<=32768 or pairs&(pairs-1) or tasks<16 or tasks%16:
        raise ValueError('lanes=1/2; pairs a power of two in 8..32768; tasks a multiple of 16')
    output.mkdir(parents=True);vectors=output/'vectors';vectors.mkdir()
    run([os.environ.get('CC','cc'),'-O2','-std=c11','-Wall','-Wextra','-Werror',OP/'cmodel/reference.c','-o',output/'reference'],output,'c-compile')
    run([output/'reference',vectors,pairs,tasks],output,'c-reference')
    run([sys.executable,OP/'tools/check_reference.py',vectors,pairs],output,'python-reference')
    rtl=output/'rtl'
    run(shlex.split(os.environ.get('MILL','mill'))+['-i','friFold.tool.runMain','zk.fri.Emit',rtl,lanes,pairs,vectors,tasks],output,'emit')
    product=product_closure(rtl)
    (rtl/'product.f').write_text(''.join(str(p)+'\n' for p in product))
    simulation_list(rtl,rtl/'simulation.f')
    (output/'configuration.json').write_text(json.dumps({'version':'1.1.0','lanes':lanes,'max_pairs':pairs,'tasks':tasks},indent=2)+'\n')
    return rtl

if __name__=='__main__':
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--out',default='build/emit');ap.add_argument('--lanes',type=int,default=1)
    ap.add_argument('--pairs',type=int,default=32768);ap.add_argument('--tasks',type=int,default=16)
    a=ap.parse_args();generate(a.out,a.lanes,a.pairs,a.tasks)
    print('Emission and references passed; product closure: output rtl/product.f')
