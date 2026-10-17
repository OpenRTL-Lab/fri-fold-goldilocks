#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks FRI Folding Hardware Accelerator
# Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
# Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
# File: tools/verify.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""独立参考、产品一致性、八模式仿真与理想接口性能验证。"""
import argparse,json,os,re,subprocess,sys,traceback
from pathlib import Path
from emit_sv import ROOT,OP,BUNDLED,generate,run,code,product_closure,simulation_list
from metrics import fields,performance

MODES=[(0,'ideal'),(1,'protocol'),(2,'reset'),(3,'negative-reference'),(4,'negative-inverse'),(5,'errors'),(6,'isolated'),(7,'queued-reset')]

def simulate(rtl,output,lanes,pairs,tasks,jobs,bundled=False):
    output.mkdir();listing=output/'simulation.f';simulation_list(rtl,listing,bundled)
    obj=output/'obj'
    env_previous=os.environ.get('CCACHE_DISABLE');os.environ['CCACHE_DISABLE']='1'
    try:
        run(['verilator','-O3','--assert','--cc','--exe','--build','-j',jobs,'-DENABLE_INITIAL_MEM_',
             '--top-module','FriFoldSimTop','-Mdir',obj,'-CFLAGS','-O2 -std=c++17',
             '-f',listing,ROOT/OP/'sim/driver.cpp'],output,'verilator')
    finally:
        if env_previous is None:os.environ.pop('CCACHE_DISABLE',None)
        else:os.environ['CCACHE_DISABLE']=env_previous
    tests={}
    for mode,label in MODES:
        run([obj/'VFriFoldSimTop',mode],output,label,1 if mode in (3,4) else 0)
        log=(output/(label+'.log')).read_text()
        matches=re.findall(r'^RESULT (.*)$',log,re.M);assert len(matches)==1
        measured=fields(matches[0])
        assert measured['errors']==(1 if mode in (3,4) else 0)
        assert measured['pass']==(0 if mode in (3,4) else 1)
        checked=pairs*tasks
        if mode==1:checked=(2*pairs+lanes+1)*(tasks//4)
        if mode==5:checked=pairs*(tasks-13)
        if mode==6:checked=pairs
        assert measured['checked']==checked,(label,measured,checked)
        assert measured['completed']==(1 if mode==6 else tasks)
        if mode in (2,7):assert measured['resets']==1
        tests[label]=measured
    measured=performance((output/'ideal.log').read_text(),lanes,pairs,tasks)
    isolated=[fields(x) for x in re.findall(r'^COMPLETE (.*)$',(output/'isolated.log').read_text(),re.M)]
    assert len(isolated)==1 and isolated[0]['latency']==measured['latency_min_cycles']
    measured['isolated']=isolated[0]
    return {'tests':tests,'performance':measured}

def verify(args):
    rtl=generate(args.out,args.lanes,args.pairs,args.tasks);output=rtl.parent
    report={'version':'1.1.0','status':1,'ideal_interfaces':True,'lanes':args.lanes,'pairs':args.pairs,'tasks':args.tasks}
    try:
        report['tools']={t:subprocess.check_output([t,'-version' if t=='java' else '--version'],stderr=subprocess.STDOUT,text=True).strip() for t in ['java','verilator',os.environ.get('CC','cc')]}
        run(['verilator','--lint-only','--assert','--top-module','GoldilocksFriFold','-f',rtl/'product.f'],output,'product-lint')
        report['generated']=simulate(rtl,output/'generated',args.lanes,args.pairs,args.tasks,args.jobs)
        if args.lanes==1 and args.pairs==32768:
            product=product_closure(rtl);bundled={p.name:p for p in BUNDLED.glob('*.sv')}
            assert set(bundled)=={p.name for p in product},'Product module set changed'
            for p in product:
                assert re.sub(r'\s+','',code(p.read_text()))==re.sub(r'\s+','',code(bundled[p.name].read_text())),f'Circuit mismatch: {p.name}'
            report['bundled_product_matches']=True
            report['bundled']=simulate(rtl,output/'bundled',args.lanes,args.pairs,args.tasks,args.jobs,True)
            assert report['generated']==report['bundled'],'Bundled and generated simulation results differ'
        report['status']=0
    except Exception:
        report['error']=traceback.format_exc();print(report['error'],file=sys.stderr)
    (output/'results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'status':report['status'],'results':str(output/'results.json')}))
    return report['status']

if __name__=='__main__':
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--out',default='build/verify');ap.add_argument('--lanes',type=int,default=1)
    ap.add_argument('--pairs',type=int,default=32768);ap.add_argument('--tasks',type=int,default=16)
    ap.add_argument('--jobs',type=int,default=8)
    a=ap.parse_args()
    if not 1<=a.jobs<=32:ap.error('jobs must be in 1..32')
    raise SystemExit(verify(a))
