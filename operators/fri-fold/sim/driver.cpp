/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks FRI Folding Hardware Accelerator
 * Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
 * Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
 * File: operators/fri-fold/sim/driver.cpp
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

#include "VFriFoldSimTop.h"
#include "verilated.h"
#include <cstdlib>
#include <iostream>
// 只控制时钟、复位、触发和测试模式；不触碰DUT通道。
int main(int argc,char**argv){
  VerilatedContext context;context.commandArgs(argc,argv);VFriFoldSimTop top{&context};
  auto tick=[&](){top.clock=0;top.eval();context.timeInc(1);top.clock=1;top.eval();context.timeInc(1);};
  top.reset=1;top.io_trigger=0;top.io_mode=argc>1?std::atoi(argv[1]):0;
  for(int i=0;i<8;i++)tick();top.reset=0;tick();top.io_trigger=1;tick();top.io_trigger=0;
  unsigned n=0;while(!top.io_done&&!context.gotFinish()&&n<30000000){tick();n++;}
  bool pass=top.io_done&&top.io_pass&&top.io_errors==0;
  std::cout<<"RESULT pass="<<pass<<" errors="<<top.io_errors<<" checked="<<top.io_checked
    <<" completed="<<top.io_completed<<" cycles="<<top.io_cycles<<" resets="<<top.io_resets<<'\n';
  top.final();return pass?0:1;
}
