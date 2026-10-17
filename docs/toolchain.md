<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/toolchain.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 工具链说明

[English](toolchain.en.md)

本仓库固定 Chisel、Scala、Mill 与 firtool 版本，并提供只依赖公开代码的生成和验证脚本；以下版本是功能、协议及性能复现所用的工具组合。

| 工具 | 版本 | 用途 |
| --- | --- | --- |
| Mill | 1.1.7 | Scala构建与生成入口 |
| Scala | 2.13.18 | 设计语言运行时 |
| Chisel / plugin | 7.13.0 | 电路描述与编译 |
| CIRCT firtool | 1.149.0 | SystemVerilog生成 |
| JDK | Temurin 21.0.11 | JVM运行环境 |
| Verilator | 5.050 | 产品lint及逐周期仿真 |
| Python | 3.12.13 | 脚本与独立大整数参考 |
| GCC / G++ | 16.2.0 | C参考与C++仿真驱动编译 |

在 Linux 环境安装工具，将 `mill`、`java`、`verilator`、`python3`、`cc`、`g++` 放入 `PATH`。首次构建需要下载 Maven 依赖与 firtool，后续可复用工具缓存。若 Mill 启动命令不同，可用 `MILL` 环境变量提供命令；C编译器可用 `CC` 指定。无需商业仿真器、工艺库或 DC 即可完成公开复现。

`build.mill` 固定依赖版本，`VERSION` 记录产品版本。生成脚本仅把输出写入仓库的相对 `build/` 子目录，不覆盖已存在的输出目录；重复运行时使用新的目录名。具体步骤见 [快速复现](reproduction.md)。
