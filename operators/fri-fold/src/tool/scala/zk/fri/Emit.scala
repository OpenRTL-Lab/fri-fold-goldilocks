/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks FRI Folding Hardware Accelerator
 * Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
 * Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
 * File: operators/fri-fold/src/tool/scala/zk/fri/Emit.scala
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

package zk.fri
import circt.stage.ChiselStage
import java.nio.file.{Files,Paths}
object Emit extends App {
  require(args.length == 5)
  val path = Paths.get(args(0)).normalize()
  require(!path.isAbsolute && path.startsWith(Paths.get("build")) && !Files.exists(path))
  Files.createDirectories(path)
  ChiselStage.emitSystemVerilog(new FriFoldSimTop(FriConfig(args(1).toInt,args(2).toInt),args(3),args(4).toInt),
    firtoolOpts=Array("-O=release","-disable-all-randomization",
      "--lowering-options=disallowLocalVariables","-split-verilog",s"-o=$path"))
}
