# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks FRI Folding Hardware Accelerator
# Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
# Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
# File: tools/metrics.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""Measure valid issue and stage occupancy from simulation cycle logs."""
import re
MUL_DEPTH=21
HALF_DEPTH=10
FINAL_DEPTH=5
ARITH_DEPTH=48
def fields(s):return {k:int(v) for k,v in re.findall(r'(\w+)=\s*(\d+)',s)}
def performance(log,lanes,m,tasks):
    accepted=[fields(s) for s in re.findall(r'^ACCEPT (.*)$',log,re.M)]
    completed=[fields(s) for s in re.findall(r'^COMPLETE (.*)$',log,re.M)]
    assert len(accepted)==len(completed)==tasks
    assert [x['tag'] for x in completed]==list(range(tasks))
    per={t:[dict(launch1=0,launch2=0,occupied1=0,occupied2=0,stages1=[0]*MUL_DEPTH,stages2=[0]*MUL_DEPTH,
        occupiedHalf=0,occupiedFinal=0,stagesHalf=[0]*HALF_DEPTH,stagesFinal=[0]*FINAL_DEPTH) for _ in range(lanes)] for t in range(tasks)}
    for s in re.findall(r'^UNIT (.*)$',log,re.M):
        v=fields(s);unit=per[v['tag']][v['lane']]
        for n in (1,2):
            unit[f'launch{n}']+=v[f'launch{n}']
            unit[f'occupied{n}']+=bool(v[f'mask{n}'])
            for bit in range(MUL_DEPTH):unit[f'stages{n}'][bit]+=bool(v[f'mask{n}']&(1<<bit))
        for name,depth in [('Half',HALF_DEPTH),('Final',FINAL_DEPTH)]:
            unit['occupied'+name]+=bool(v['mask'+name])
            for bit in range(depth):unit['stages'+name][bit]+=bool(v['mask'+name]&(1<<bit))
    for t,c in enumerate(completed):
        assert c['status']==0 and c['metadataError']==0 and c['A']==accepted[t]['cycle']
        assert c['F']-c['A']==1 and c['I']-c['F']==m//lanes-1
        assert c['O0']-c['F']==ARITH_DEPTH+2 and c['O']-c['I']==ARITH_DEPTH+2 and c['cycle']-c['O']==1
        for lane in range(lanes):
            u=per[t][lane]
            assert u['launch1']==u['launch2']==m//lanes
            assert u['stages1']==u['stages2']==[m//lanes]*MUL_DEPTH
            assert u['occupied1']==u['occupied2']==m//lanes+MUL_DEPTH-1
            for name,depth in [('Half',HALF_DEPTH),('Final',FINAL_DEPTH)]:
                assert u['stages'+name]==[m//lanes]*depth
                assert u['occupied'+name]==m//lanes+depth-1
    first=completed[2];last=completed[tasks-3];duration=last['cycle']-first['cycle'];intervals=last['tag']-first['tag']
    assert duration>0 and all(completed[i+1]['cycle']-completed[i]['cycle']==duration//intervals for i in range(tasks-1))
    steady=[]
    for lane in range(lanes):
        for n in (1,2):
            launches=sum(per[t][lane][f'launch{n}'] for t in range(first['tag']+1,last['tag']+1))
            occupied=sum(per[t][lane][f'occupied{n}'] for t in range(first['tag']+1,last['tag']+1))
            stages=[sum(per[t][lane][f'stages{n}'][i] for t in range(first['tag']+1,last['tag']+1)) for i in range(MUL_DEPTH)]
            steady.append(dict(lane=lane,unit=n,instances=1,depth=MUL_DEPTH,valid_launches=launches,
                valid_launch_cycles=launches,occupied_cycles=occupied,idle_cycles=duration-occupied,
                effective_stage_cycles=stages,effective_stage_busy=launches/duration,
                pipeline_any_busy=occupied/duration,launch_busy=launches/duration))
        for name,depth,instances in [('Half',HALF_DEPTH,2),('Final',FINAL_DEPTH,1)]:
            stages=[sum(per[t][lane]['stages'+name][i] for t in range(first['tag']+1,last['tag']+1)) for i in range(depth)]
            occupied=sum(per[t][lane]['occupied'+name] for t in range(first['tag']+1,last['tag']+1))
            steady.append(dict(lane=lane,unit=name,instances=instances,depth=depth,valid_launches=stages[0],
                valid_launch_cycles=stages[0],occupied_cycles=occupied,idle_cycles=duration-occupied,
                effective_stage_cycles=stages,effective_stage_busy=stages[0]/duration,
                pipeline_any_busy=occupied/duration,launch_busy=stages[0]/duration))
    return dict(accepted=accepted,completed=completed,per_task_units=per,
        latency_min_cycles=min(c['latency'] for c in completed),latency_max_cycles=max(c['latency'] for c in completed),
        first_output_from_first_input_cycles=ARITH_DEPTH+2,last_output_from_task_cycles=completed[0]['O']-completed[0]['A'],
        steady_first_tag=first['tag'],steady_last_tag=last['tag'],steady_intervals=intervals,steady_cycles=duration,
        task_interval_cycles=duration//intervals,tasks_per_cycle=intervals/duration,pairs_per_cycle=m*intervals/duration,
        steady_units=steady,cycles_definition='完成握手C减任务接受A；稳态窗口(C_first,C_last]，暖机/尾批排除',
        occupancy_definition='各真实功能valid流水级逐拍记录，any占用与有效发射分开，不是翻转率或功耗')
