/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks FRI Folding Hardware Accelerator
 * Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
 * Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
 * File: operators/fri-fold/src/main/scala/zk/fri/Environment.scala
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
import chisel3.util.experimental.loadMemoryFromFileInline

/** 仅此Chisel环境触碰原生接口，C++不驱动数据。 */
class FriFoldSimTop(cfg: FriConfig, directory: String, tasks: Int) extends Module {
  val io = IO(new Bundle {
    val trigger = Input(Bool()); val mode = Input(UInt(3.W))
    val done = Output(Bool()); val pass = Output(Bool())
    val errors = Output(UInt(32.W)); val checked = Output(UInt(32.W)); val completed = Output(UInt(32.W))
    val cycles = Output(UInt(32.W)); val resets = Output(UInt(32.W))
  })
  val start = RegInit(false.B); val active = RegInit(false.B)
  val cycle = RegInit(0.U(32.W)); val frame = RegInit(0.U(32.W))
  val sent = RegInit(0.U(32.W)); val outputs = RegInit(0.U(32.W))
  val checked = RegInit(0.U(32.W)); val errors = RegInit(0.U(32.W))
  val resetDone = RegInit(false.B)
  val limit = Mux(io.mode === 6.U, 1.U, tasks.U)
  val lateReset = io.mode === 7.U
  val dutOutputPending = Wire(Bool())
  val abort = active && frame === 0.U && !resetDone &&
    ((io.mode === 2.U && sent === 5.U) ||
     (lateReset && sent >= math.min(cfg.depth, cfg.maxPairs/cfg.lanes).U && dutOutputPending))
  // 输出valid来自寄存队列，复位只清寄存状态，不产生组合反馈。
  val dut = withReset(reset.asBool || abort) { Module(new GoldilocksFriFold(cfg)) }
  dutOutputPending := dut.io.output.valid
  val input = Mem(tasks*cfg.maxPairs*3, UInt(64.W)); val expected = Mem(tasks*cfg.maxPairs, UInt(64.W))
  val challenges = Mem(tasks, UInt(64.W))
  loadMemoryFromFileInline(input,directory+"/input.hex")
  loadMemoryFromFileInline(expected,directory+"/expected.hex")
  loadMemoryFromFileInline(challenges,directory+"/alpha.hex")
  val safeFrame = Mux(frame < tasks.U, frame, 0.U)
  val protocol = io.mode === 1.U
  val errorMode = io.mode === 5.U
  val length = Mux(protocol, MuxLookup(frame(1,0), cfg.maxPairs.U)(Seq(
    0.U -> 1.U, 1.U -> (cfg.lanes+1).U, 2.U -> (cfg.maxPairs-1).U)), cfg.maxPairs.U)
  val beatCount = (length + (cfg.lanes-1).U) >> log2Ceil(cfg.lanes)
  val expectedStatus = Mux(errorMode, Mux(frame < 5.U, FriStatus.BAD_PARAM,
    Mux(frame === 5.U, FriStatus.ZERO_DENOM, Mux(frame <= 12.U, FriStatus.BAD_FRAME, FriStatus.OK))), FriStatus.OK)
  val validTask = expectedStatus =/= FriStatus.BAD_PARAM
  val taskLength = Mux(errorMode && frame === 2.U, 0.U,
    Mux(errorMode && frame === 3.U, (cfg.maxPairs+1).U, length))
  dut.io.task.valid := start && !active && frame < limit && !abort
  dut.io.task.bits.tag := frame
  dut.io.task.bits.mode := Mux(errorMode && frame === 0.U, 1.U, 0.U)
  dut.io.task.bits.profile := Mux(errorMode && frame === 1.U, 1.U, 0.U)
  dut.io.task.bits.pairs := taskLength
  dut.io.task.bits.alpha := Mux(errorMode && frame === 4.U, FriField.P.U, challenges(safeFrame))
  val isLast = sent === beatCount - 1.U
  val keep = if(cfg.lanes == 1) 1.U(1.W) else Mux(isLast && length(0), 1.U(2.W), 3.U(2.W))
  val heldInput = RegNext(dut.io.input.valid && !dut.io.input.ready, false.B)
  dut.io.input.valid := active && validTask && sent < beatCount && !abort &&
    (!protocol || heldInput || cycle(2,0) =/= 0.U)
  dut.io.input.bits.last := Mux(errorMode && frame === 7.U, sent === 0.U,
    Mux(errorMode && frame === 8.U, false.B, isLast))
  dut.io.input.bits.keep := Mux(errorMode && (frame === 9.U || frame === 12.U) && sent === 0.U, 0.U, keep)
  for(i <- 0 until cfg.lanes) {
    val index = safeFrame*cfg.maxPairs.U + sent*cfg.lanes.U + i.U
    val pair = dut.io.input.bits.pairs(i)
    pair.a := input(index*3.U); pair.b := input(index*3.U+1.U); pair.u := input(index*3.U+2.U)
    when(!keep(i)) { pair.a := FriField.P.U; pair.b := FriField.P.U; pair.u := FriField.P.U }
    when(sent === 0.U && i.U === 0.U) {
      when(io.mode === 4.U && frame === 0.U) { pair.u := input(index*3.U+2.U) ^ 1.U }
      when(errorMode) {
        when(frame === 5.U || frame === 12.U) { pair.u := 0.U }
        when(frame === 6.U) { pair.a := FriField.P.U }
        when(frame === 10.U) { pair.b := FriField.P.U }
        when(frame === 11.U) { pair.u := FriField.P.U }
      }
    }
  }
  dut.io.output.ready := (!protocol || cycle(5,0) >= 31.U) && (!lateReset || resetDone)
  dut.io.completion.ready := !protocol || cycle(4,0) >= 2.U
  val acceptedAt = Reg(UInt(32.W)); val firstAt = Reg(UInt(32.W)); val lastInputAt = Reg(UInt(32.W))
  val firstOutputAt = Reg(UInt(32.W)); val lastOutputAt = Reg(UInt(32.W))
  when(io.trigger) { start := true.B }
  when(start) { cycle := cycle + 1.U }
  when(dut.io.task.fire) {
    active := true.B; sent := 0.U; outputs := 0.U; acceptedAt := cycle
    firstAt := 0.U; lastInputAt := 0.U; firstOutputAt := 0.U; lastOutputAt := 0.U
    printf(p"ACCEPT tag=${frame} cycle=${cycle} pairs=${length}\n")
  }
  when(dut.io.input.fire) {
    when(sent === 0.U) { firstAt := cycle }
    lastInputAt := cycle; sent := sent + 1.U
  }
  when(dut.io.output.fire) {
    val out = dut.io.output.bits
    val expectedKeep = if(cfg.lanes == 1) 1.U(1.W) else Mux(outputs === beatCount-1.U && length(0), 1.U(2.W), 3.U(2.W))
    val badMetadata = out.tag =/= frame || out.index =/= outputs*cfg.lanes.U ||
      out.last =/= (outputs === beatCount-1.U) || out.keep =/= expectedKeep
    val numerical = (0 until cfg.lanes).map(i => expectedKeep(i) && expectedStatus === FriStatus.OK &&
      out.values(i) =/= (expected(safeFrame*cfg.maxPairs.U + outputs*cfg.lanes.U+i.U) ^
        Mux(io.mode === 3.U && frame === 0.U && outputs === 0.U && i.U === 0.U, 1.U, 0.U)))
    val badInactive = (0 until cfg.lanes).map(i => !expectedKeep(i) && out.values(i) =/= 0.U)
    errors := errors + PopCount(numerical) + PopCount(badInactive) + badMetadata
    when(expectedStatus === FriStatus.OK) { checked := checked + PopCount(expectedKeep) }
    when(outputs === 0.U) { firstOutputAt := cycle }
    lastOutputAt := cycle; outputs := outputs + 1.U
  }
  when(dut.io.completion.fire) {
    val bad = dut.io.completion.bits.tag =/= frame || dut.io.completion.bits.status =/= expectedStatus ||
      outputs =/= Mux(validTask, beatCount, 0.U) || sent =/= Mux(validTask, beatCount, 0.U)
    errors := errors + bad
    printf(p"COMPLETE tag=${frame} cycle=${cycle} A=${acceptedAt} F=${firstAt} I=${lastInputAt} O0=${firstOutputAt} O=${lastOutputAt} latency=${cycle-acceptedAt} status=${dut.io.completion.bits.status} metadataError=${bad}\n")
    active := false.B; frame := frame + 1.U
  }
  when(abort) {
    resetDone := true.B; active := false.B; frame := 0.U; sent := 0.U; outputs := 0.U
    checked := 0.U; errors := 0.U
  }
  val heldOutput = RegNext(dut.io.output.valid && !dut.io.output.ready, false.B)
  val previousOutput = RegNext(dut.io.output.bits.asUInt)
  val heldCompletion = RegNext(dut.io.completion.valid && !dut.io.completion.ready, false.B)
  val previousCompletion = RegNext(dut.io.completion.bits.asUInt)
  when(abort) { heldOutput := false.B; heldCompletion := false.B }
  when(heldOutput && !abort) { assert(dut.io.output.valid && dut.io.output.bits.asUInt === previousOutput) }
  when(heldCompletion && !abort) { assert(dut.io.completion.valid && dut.io.completion.bits.asUInt === previousCompletion) }
  io.done := frame === limit || cycle > (tasks*(cfg.maxPairs+100)*12+1000).U
  io.pass := frame === limit && errors === 0.U && ((io.mode =/= 2.U && !lateReset) || resetDone)
  io.errors := errors; io.checked := checked; io.completed := frame; io.cycles := cycle; io.resets := resetDone
}
