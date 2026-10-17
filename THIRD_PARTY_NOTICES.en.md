<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: THIRD_PARTY_NOTICES.en.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Third-party notices

[简体中文](THIRD_PARTY_NOTICES.md)

The original design, reference models, scripts, and documentation in this project are licensed under MIT. RTL generation uses Chisel/CIRCT; this notice distinguishes the project deliverables from external tool licenses.

The mathematical definitions of the Goldilocks field and binary FRI folding are given in [Mathematical model](docs/mathematical-model.en.md). This operator does not use a proof-system-specific round-constant table or embed an external software package implementation.

Emission and verification depend on Chisel, CIRCT, Mill, Scala, JDK, Verilator, Python, and C/C++ compilers. These tools are not distributed with this repository and retain their own licenses; Chisel uses Apache-2.0 and CIRCT uses Apache-2.0 with LLVM exceptions. Generated SV retains the generator identification. Versions are listed in [Toolchain](docs/toolchain.en.md).

Users must retain the copyright and permission notices required by the [MIT License](LICENSE). Foundry libraries and DC software are not public deliverables of this repository.
