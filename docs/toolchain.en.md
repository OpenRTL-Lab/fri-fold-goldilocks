<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/toolchain.en.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Toolchain

[简体中文](toolchain.md)

This repository pins Chisel, Scala, Mill, and firtool versions and provides emission and verification scripts that depend only on public code. The following tool combination was used for functional, protocol, and performance reproduction.

| Tool | Version | Purpose |
| --- | --- | --- |
| Mill | 1.1.7 | Scala build and emission entry point |
| Scala | 2.13.18 | Design-language runtime |
| Chisel / plugin | 7.13.0 | Circuit description and compilation |
| CIRCT firtool | 1.149.0 | SystemVerilog generation |
| JDK | Temurin 21.0.11 | JVM runtime |
| Verilator | 5.050 | Product lint and cycle simulation |
| Python | 3.12.13 | Scripts and independent big-integer reference |
| GCC / G++ | 16.2.0 | C reference and C++ simulation-driver compilation |

Install these tools on Linux and make `mill`, `java`, `verilator`, `python3`, `cc`, and `g++` available through `PATH`. The first build downloads Maven dependencies and firtool; subsequent builds can reuse tool caches. Set `MILL` if the launcher command differs, or `CC` to select the C compiler. Public reproduction requires no commercial simulator, process library, or DC installation.

`build.mill` pins dependencies and `VERSION` records the product version. Scripts write only to relative directories under the repository's `build/` directory and do not overwrite existing output directories. Use a new directory name for each run. See [Reproduction](reproduction.en.md) for instructions.
