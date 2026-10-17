/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks FRI Folding Hardware Accelerator
 * Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
 * Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
 * File: operators/fri-fold/src/main/scala/zk/fri/FriArithmetic.scala
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

/** 每块独立寄存两个进位候选，再寄存选择；不允许全宽单拍进位链。 */
object FriArithmetic {
  val MulLatency = 21
  val HalfLatency = 10
  val FinalLatency = 5
  val LaneLatency = 2 * MulLatency + FinalLatency
  val InputLatency = 1
  val TotalLatency = InputLatency + LaneLatency

  def add(a: UInt, b: UInt, width: Int, subtract: Boolean = false): (UInt, Bool) = {
    require(width >= 1 && width <= 128)
    val aa = a.pad(width); val bb = if (subtract) ~b.pad(width) else b.pad(width)
    val candidates = (0 until width by 16).map { lo =>
      val bits = math.min(16, width - lo)
      val x = aa(lo + bits - 1, lo); val y = bb(lo + bits - 1, lo)
      // 单条显式局部进位链，不写(x+y)+1以免被映射成两个串接加法器。
      def local(initial: Bool): UInt = {
        var c: Bool = initial
        val sums = (0 until bits).map { bit =>
          val propagate = x(bit) ^ y(bit)
          val s = propagate ^ c
          c = (x(bit) & y(bit)) | (propagate & c)
          s
        }
        Cat(c, Cat(sums.reverse))
      }
      val zero = RegNext(local(false.B))
      val one = RegNext(local(true.B))
      (bits, zero, one)
    }
    var carry: Bool = subtract.B
    val words = candidates.map { case (bits, zero, one) =>
      val selected = Mux(carry, one, zero)
      carry = selected(bits)
      selected(bits - 1, 0)
    }
    (RegNext(Cat(words.reverse)), RegNext(carry))
  }

  /** 两拍减法借位与候选同时计算，第三拍才选择。输入必须 <2p 才完成规范化。 */
  def correct(x: UInt, width: Int): UInt = {
    val (difference, noBorrow) = add(x, FriField.P.U(width.W), width, subtract = true)
    RegNext(Mux(noBorrow, difference, ShiftRegister(x, 2)))
  }

  def half(x: UInt): UInt = {
    val (plusP, _) = add(x, FriField.P.U(65.W), 65)
    val raw = ShiftRegister(x, 2)
    RegNext(Mux(raw(0), plusP, raw.pad(65))(64, 1))
  }
}

/** 16×16叶乘 + 六个单层CSA寄存 + 分块乘积合并/模约减；21拍、II=1。 */
class FriGoldilocksMul extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(64.W)); val b = Input(UInt(64.W))
    val product = Output(UInt(64.W))
  })
  var rows: Seq[UInt] = for (i <- 0 until 4; j <- 0 until 4) yield {
    val leaf = RegNext(io.a(16*i+15, 16*i) * io.b(16*j+15, 16*j))
    (leaf.pad(128) << (16*(i+j)))(127, 0)
  }
  while (rows.size > 2) {
    rows = rows.grouped(3).flatMap {
      case Seq(a, b, c) => Seq(RegNext(a ^ b ^ c),
        RegNext((((a & b) | (a & c) | (b & c)) << 1)(127, 0)))
      case rest => rest.map(x => RegNext(x))
    }.toSeq
  }
  private val full = FriArithmetic.add(rows.head, rows(1), 128)._1 // 第9拍
  private val highSum = FriArithmetic.add(full(63,32), full(95,64), 33)._1
  private val subtrahend = FriArithmetic.add(full(95,64), full(127,96), 33)._1
  private val low = ShiftRegister(full(31,0), 2)
  private val positive = FriArithmetic.add(Cat(highSum, low), FriField.P.U(66.W), 66)._1
  private val candidate = FriArithmetic.add(positive, ShiftRegister(subtrahend, 2), 66, subtract = true)._1
  private val once = FriArithmetic.correct(candidate, 66)
  private val twice = FriArithmetic.correct(once, 66)
  io.product := twice(63,0)
}
