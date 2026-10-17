#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks FRI Folding Hardware Accelerator
# Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
# Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
# File: operators/fri-fold/tools/check_reference.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""独立任意精度参考；多项式样本用偶奇系数求值，所有样本另核对域点逆元。"""
from pathlib import Path
import sys
P=2**64-2**32+1
def eval_poly(c,x):
    return sum(a*pow(x,k,P) for k,a in enumerate(c))%P
def check(directory,m):
    rows=[[int(x,16) for x in line.split()[2:]] for line in (directory/'polynomials.txt').read_text().splitlines()]
    kinds=[int(line.split()[1]) for line in (directory/'polynomials.txt').read_text().splitlines()]
    values=[int(x,16) for x in (directory/'input.hex').read_text().split()]
    expected=[int(x,16) for x in (directory/'expected.hex').read_text().split()]
    alphas=[int(x,16) for x in (directory/'alpha.hex').read_text().split()]
    assert len(values)==len(expected)*3==len(rows)*m*3
    assert eval_poly([3+13*5,7+13*11],4)==668
    root=pow(7,(P-1)//(2*m),P);assert pow(root,m,P)==P-1
    for t,(alpha,*c) in enumerate(rows):
        assert alpha==alphas[t]
        x=2 if kinds[t]==0 else 7
        for i in range(m):
            a,b,u=values[3*(t*m+i):3*(t*m+i+1)]
            assert x*u%P==1 and all(0<=v<P for v in [a,b,u])
            if kinds[t]<8:
                assert a==eval_poly(c,x) and b==eval_poly(c,-x)
                folded=[(c[2*k]+alpha*c[2*k+1])%P for k in range(4)]
                result=eval_poly(folded,x*x%P)
            else:result=((a+b)+alpha*u*(a-b))*pow(2,-1,P)%P
            assert result==expected[t*m+i],(t,i)
            x=x*root%P
    print(f'PYTHON_REFERENCE pass=1 tasks={len(rows)} pairs={m} checked={len(expected)} kat=668')
if __name__=='__main__':check(Path(sys.argv[1]),int(sys.argv[2]))
