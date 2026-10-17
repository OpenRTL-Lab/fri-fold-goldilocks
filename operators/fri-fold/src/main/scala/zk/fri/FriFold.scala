/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks FRI Folding Hardware Accelerator
 * Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
 * Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
 * File: operators/fri-fold/src/main/scala/zk/fri/FriFold.scala
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

package zk.fri

import chisel3._
import chisel3.util._

case class FriConfig(lanes: Int = 1, maxPairs: Int = 32768) {
  require(Set(1, 2).contains(lanes))
  require(maxPairs >= 8 && maxPairs <= 32768 && isPow2(maxPairs))
  val countBits = log2Ceil(maxPairs) + 1
  val indexBits = log2Ceil(maxPairs)
  val depth = FriArithmetic.TotalLatency + 4
}
object FriStatus {
  val OK = 0.U(3.W); val BAD_PARAM = 1.U(3.W)
  val ZERO_DENOM = 2.U(3.W); val BAD_FRAME = 3.U(3.W)
}
object FriField { val P: BigInt = BigInt("ffffffff00000001", 16) }
class FriTask(cfg: FriConfig) extends Bundle {
  val tag = UInt(32.W); val mode = UInt(2.W); val profile = UInt(2.W)
  val pairs = UInt(cfg.countBits.W); val alpha = UInt(64.W)
}
class FriPair extends Bundle {
  val a = UInt(64.W); val b = UInt(64.W); val u = UInt(64.W)
}
class FriInput(cfg: FriConfig) extends Bundle {
  val pairs = Vec(cfg.lanes, new FriPair)
  val last = Bool(); val keep = UInt(cfg.lanes.W)
}
class FriOutput(cfg: FriConfig) extends Bundle {
  val values = Vec(cfg.lanes, UInt(64.W)); val tag = UInt(32.W)
  val index = UInt(cfg.indexBits.W); val last = Bool(); val keep = UInt(cfg.lanes.W)
}
class FriCompletion extends Bundle { val tag = UInt(32.W); val status = UInt(3.W) }

/** 每lane两条21级模乘、10级半和差旁路、5级最终模加。 */
class FriFoldLane(laneId: Int) extends Module {
  val io = IO(new Bundle {
    val valid = Input(Bool()); val pair = Input(new FriPair)
    val alpha = Input(UInt(64.W)); val tag = Input(UInt(32.W))
    val outValid = Output(Bool()); val value = Output(UInt(64.W)); val outTag = Output(UInt(32.W))
  })
  private val first = Module(new FriGoldilocksMul)
  private val second = Module(new FriGoldilocksMul)
  first.io.a := io.alpha; first.io.b := io.pair.u
  private val sum2 = FriArithmetic.add(io.pair.a, io.pair.b, 65)._1
  private val sum5 = FriArithmetic.correct(sum2, 65)(63,0)
  private val base2 = FriArithmetic.add(io.pair.a, FriField.P.U(65.W), 65)._1
  private val diff4 = FriArithmetic.add(base2, ShiftRegister(io.pair.b, 2), 65, subtract = true)._1
  private val diff7 = FriArithmetic.correct(diff4, 65)(63,0)
  private val halfSum10 = FriArithmetic.half(ShiftRegister(sum5, 2))
  private val halfDiff10 = FriArithmetic.half(diff7)
  private val alignedSum = ShiftRegister(halfSum10, FriArithmetic.MulLatency - FriArithmetic.HalfLatency)
  private val alignedDiff = ShiftRegister(halfDiff10, FriArithmetic.MulLatency - FriArithmetic.HalfLatency)
  second.io.a := first.io.product; second.io.b := alignedDiff
  private val finalSum = FriArithmetic.add(ShiftRegister(alignedSum, FriArithmetic.MulLatency), second.io.product, 65)._1
  io.value := FriArithmetic.correct(finalSum, 65)(63,0)
  val validFirst = VecInit((1 to FriArithmetic.MulLatency).map(n => ShiftRegister(io.valid, n, false.B, true.B)))
  val validSecond = VecInit((1 to FriArithmetic.MulLatency).map(n => ShiftRegister(validFirst.last, n, false.B, true.B)))
  val validFinal = VecInit((1 to FriArithmetic.FinalLatency).map(n => ShiftRegister(validSecond.last, n, false.B, true.B)))
  io.outValid := validFinal.last
  io.outTag := ShiftRegister(io.tag, FriArithmetic.LaneLatency)
  // 验证层日志，不增加产品探针端口/计数器。有效标记也用于功能结果有效性。
  when(io.valid || validFirst.asUInt.orR || validSecond.asUInt.orR || validFinal.asUInt.orR) {
    printf(p"UNIT tag=${io.tag} lane=${laneId.U} launch1=${io.valid} launch2=${validFirst.last} mask1=${validFirst.asUInt} mask2=${validSecond.asUInt} maskHalf=${validFirst.asUInt(FriArithmetic.HalfLatency-1,0)} maskFinal=${validFinal.asUInt}\n")
  }
}

class GoldilocksFriFold(val cfg: FriConfig) extends Module {
  val io = IO(new Bundle {
    val task = Flipped(Decoupled(new FriTask(cfg)))
    val input = Flipped(Decoupled(new FriInput(cfg)))
    val output = Decoupled(new FriOutput(cfg))
    val completion = Decoupled(new FriCompletion)
    val busy = Output(Bool())
  })
  private val idle = 0.U(2.W); private val running = 1.U(2.W); private val finishing = 2.U(2.W)
  val state = RegInit(idle)
  val alpha = Reg(UInt(64.W)); val tag = Reg(UInt(32.W))
  val pairs = Reg(UInt(cfg.countBits.W)); val beats = Reg(UInt(cfg.countBits.W))
  val received = RegInit(0.U(cfg.countBits.W))
  val status = RegInit(FriStatus.OK)
  val reserved = RegInit(0.U(log2Ceil(cfg.depth + 1).W))
  val queue = Module(new Queue(new FriOutput(cfg), cfg.depth, pipe = false, flow = false))
  // 隔离异步队列读选择与外部输出1ns预算；单级弹性寄存支持II=1及任意背压。
  val outputValid = RegInit(false.B)
  val outputBits = Reg(new FriOutput(cfg))
  queue.io.deq.ready := !outputValid || io.output.ready
  when(queue.io.deq.ready) {
    outputValid := queue.io.deq.valid
    when(queue.io.deq.valid) { outputBits := queue.io.deq.bits }
  }
  io.output.valid := outputValid
  io.output.bits := outputBits
  io.task.ready := state === idle
  io.input.ready := state === running && received < beats && reserved < cfg.depth.U
  io.busy := state =/= idle
  io.completion.valid := state === finishing
  io.completion.bits.tag := tag; io.completion.bits.status := status
  val finalBeat = received === beats - 1.U
  val finalKeep = if(cfg.lanes == 1) 1.U(1.W) else Mux(pairs(0), 1.U(2.W), 3.U(2.W))
  val keep = Mux(finalBeat, finalKeep, ((1 << cfg.lanes) - 1).U(cfg.lanes.W))
  when(io.task.fire) {
    tag := io.task.bits.tag; alpha := io.task.bits.alpha; pairs := io.task.bits.pairs
    beats := (if(cfg.lanes == 1) io.task.bits.pairs else (io.task.bits.pairs +& 1.U) >> 1)
    received := 0.U
    val bad = io.task.bits.mode =/= 0.U || io.task.bits.profile =/= 0.U ||
      io.task.bits.pairs === 0.U || io.task.bits.pairs > cfg.maxPairs.U || io.task.bits.alpha >= FriField.P.U
    status := Mux(bad, FriStatus.BAD_PARAM, FriStatus.OK)
    state := Mux(bad, finishing, running)
    assert(reserved === 0.U && !queue.io.deq.valid && !outputValid)
  }
  val units = (0 until cfg.lanes).map(i => Module(new FriFoldLane(i)))
  val invalid = (0 until cfg.lanes).map(i => keep(i) &&
    (io.input.bits.pairs(i).a >= FriField.P.U || io.input.bits.pairs(i).b >= FriField.P.U ||
     io.input.bits.pairs(i).u >= FriField.P.U))
  val zeros = (0 until cfg.lanes).map(i => keep(i) && io.input.bits.pairs(i).u === 0.U)
  when(io.input.fire) {
    received := received + 1.U
    when(io.input.bits.last =/= finalBeat || io.input.bits.keep =/= keep || invalid.reduce(_ || _)) {
      status := FriStatus.BAD_FRAME
    }.elsewhen(zeros.reduce(_ || _) && status =/= FriStatus.BAD_FRAME) {
      status := FriStatus.ZERO_DENOM
    }
  }
  for(i <- 0 until cfg.lanes) {
    units(i).io.valid := RegNext(io.input.fire && keep(i), false.B)
    units(i).io.alpha := alpha; units(i).io.tag := tag
    // 无使能输入寄存器隔离1ns IO预算；坏帧仍报错，其结果按接口整任务丢弃。
    units(i).io.pair := RegNext(io.input.bits.pairs(i))
  }
  queue.io.enq.valid := units.head.io.outValid
  queue.io.enq.bits.tag := units.head.io.outTag
  queue.io.enq.bits.index := ShiftRegister((received << log2Ceil(cfg.lanes))(cfg.indexBits-1,0), FriArithmetic.TotalLatency)
  queue.io.enq.bits.last := ShiftRegister(finalBeat, FriArithmetic.TotalLatency)
  queue.io.enq.bits.keep := ShiftRegister(keep, FriArithmetic.TotalLatency)
  for(i <- 0 until cfg.lanes) queue.io.enq.bits.values(i) := Mux(units(i).io.outValid, units(i).io.value, 0.U)
  assert(!queue.io.enq.valid || queue.io.enq.ready, "reservation protects finite output queue")
  assert(reserved <= cfg.depth.U)
  when(io.input.fire =/= io.output.fire) { reserved := Mux(io.input.fire, reserved + 1.U, reserved - 1.U) }
  when(io.output.fire && io.output.bits.last) { state := finishing }
  when(io.completion.fire) { state := idle }
}
