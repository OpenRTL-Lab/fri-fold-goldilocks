<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: THIRD_PARTY_NOTICES.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 第三方声明

[English](THIRD_PARTY_NOTICES.en.md)

本项目自行编写的设计、参考模型、脚本与文档采用 MIT 许可证；生成代码使用 Chisel/CIRCT 工具链，本说明区分项目交付内容与外部工具的授权。

Goldilocks 域和二元 FRI 折叠的数学定义见 [数学模型](docs/mathematical-model.md)。本算子不使用特定证明系统的轮常量表，不内嵌外部软件包的实现。

生成与验证依赖 Chisel、CIRCT、Mill、Scala、JDK、Verilator、Python 和 C/C++ 编译器。工具不随本仓库分发，各工具按自身许可证提供；Chisel 采用 Apache-2.0，CIRCT 采用 Apache-2.0 with LLVM exceptions。已生成 SV 保留生成器标识。工具版本见 [工具链说明](docs/toolchain.md)。

使用本项目代码须保留 [MIT 许可证](LICENSE)要求的版权及许可声明。工艺库和 DC 软件不是本仓库的公开交付内容。
