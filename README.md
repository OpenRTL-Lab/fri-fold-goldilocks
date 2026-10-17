<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: README.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Goldilocks FRI Folding Hardware Accelerator

[简体中文](README.zh-CN.md)

This repository provides a standalone hardware accelerator for binary FRI folding over the Goldilocks field, developed in Chisel. Its native streaming interface supports integration into ASIC or FPGA designs to accelerate evaluation-table folding in FRI-based proof systems, including STARKs, and the corresponding folding operations on the verifier side.

It is licensed under the [MIT License](LICENSE), permitting free commercial use, modification, and redistribution, provided the copyright and license notices are retained; see [Third-party notices](THIRD_PARTY_NOTICES.en.md) for tool and generated-code information.

[Release v1.1.0](https://github.com/OpenRTL-Lab/fri-fold-goldilocks/releases/tag/v1.1.0) · [Interface](docs/interface.en.md) · [Reproduction](docs/reproduction.en.md) · [Performance](docs/performance.en.md) · [Synthesis results](docs/synthesis-summary.en.md)

## Computation and task units

One task folds a declared number of pairs using one caller-supplied challenge alpha. One input pair is the triple f(x), f(-x), x inverse, and produces one field element g(x squared). Thus 32,768 pairs represent 65,536 evaluations and yield 32,768 folded values; the inverse is additional input, not another evaluation or a value computed by this block. All field words use unsigned 64-bit canonical values 0 <= v < p, where p = 2^64 - 2^32 + 1.

A lane is one pair position in a data beat. The bundled one-lane interface transfers at most one pair per accepted beat; that does not mean a whole task finishes in one cycle. The output tag identifies the task and index identifies its pair position. A task is a folding batch, not the commitments, queries and challenge generation of a complete FRI round.

## At a glance

| Integration question | Released hardware |
| --- | --- |
| Computation | Binary Fast Reed–Solomon Interactive Oracle Proof of Proximity (FRI) evaluation-table folding |
| Field | Goldilocks p = 2^64 - 2^32 + 1 |
| Bundled configuration | One lane, up to 32,768 pairs per task; top `GoldilocksFriFold` |
| Input/output | Each pair supplies f(x), f(-x) and x inverse; each output is one folded field element |
| Native boundary | Task/challenge, ordered input, output and completion ready/valid channels; returned tag/index |

## Quick start

Download the source archive from [Release v1.1.0](https://github.com/OpenRTL-Lab/fri-fold-goldilocks/releases/tag/v1.1.0), or clone the current release snapshot:

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/fri-fold-goldilocks.git
cd fri-fold-goldilocks
```

Install the exact prerequisites in [Toolchain](docs/toolchain.en.md), then run from the repository root with a fresh output directory:

```bash
python3 tools/emit_sv.py --out build/emit --lanes 1 --pairs 32768 --tasks 16
```

This emission command is not a complete RTL regression. The documented verification entry is:

```bash
python3 tools/verify.py --out build/verify --jobs 8
```

Follow [Reproduction](docs/reproduction.en.md) for expected results, negative-control interpretation and the distinction between short functional and full performance runs.

## Verification and integration boundary

The reproduction workflow compares independent C/Python references and generated/bundled SV, and documents eight simulation modes. Functional checks cover handshakes and malformed frames separately from ideal-interface timing; reset recovery and an intentional reference error are explicit gates. Read the mode table and expected completion status before interpreting a simulator return code.

The caller supplies alpha and point inverses in the documented pair order. This block does not generate challenges, compute inverse tables, commit a Merkle tree or implement an entire FRI/STARK protocol. Reset discards an active task without issuing its completion; task status determines whether the complete output is usable.

### Reading performance correctly

Under ideal native interfaces, one 32,768-pair task takes 32,819 cycles; successive tasks complete 32,820 cycles apart, yielding 0.998415600 pairs/cycle. Each of the two modular-multiplication pipelines has at least one valid stage in 99.902498% of the measured steady-window cycles. This is pipeline occupancy, not per-stage issue utilization or the task completion rate.

These are documented RTL cycle measurements under ideal native interfaces. MHz-based time/rate conversions are not board or silicon measurements. The reported Design Compiler results apply only to the documented configuration, library corner and constraints; they do not establish place-and-route signoff or system throughput. See [Performance](docs/performance.en.md) and [Synthesis results](docs/synthesis-summary.en.md) for conditions.

## Repository contents

- [Chisel design](operators/fri-fold/src/main/scala/zk/fri/): folding pipelines, field arithmetic, native interfaces, and verification environment.
- [Emission entry point](operators/fri-fold/src/tool/scala/zk/fri/Emit.scala) and [SV emission script](tools/emit_sv.py): lane-count and maximum-task-size configuration.
- [Generated SV](rtl/v1.1.0/lane1-m32768/): single-lane product supporting up to 32768 pairs per task, with top module `GoldilocksFriFold`.
- [C reference model](operators/fri-fold/cmodel/reference.c), [Python cross-check](operators/fri-fold/tools/check_reference.py), and [simulation driver](operators/fri-fold/sim/driver.cpp): the complete functional and performance verification chain.
- [Architecture](docs/architecture.en.md), [Interface](docs/interface.en.md), and [Toolchain](docs/toolchain.en.md): integration and reproduction information.

## Applications

For binary FRI folding over Goldilocks, this module can implement bulk evaluation-table reduction on the prover side, query-point folding on the verifier side, or a streaming compute unit inside a multi-round FRI scheduler. The caller supplies paired values, the challenge, and point inverses, and organizes commitment and query processing.

## Performance

With ideal interfaces, a single-lane task of 32768 pairs has a latency of 32819 cycles, sustained throughput of 0.998416 pairs/cycle, and 99.9025% occupancy in each modular-multiplication pipeline; see [Performance](docs/performance.en.md) for definitions, methodology, and results.

## Mathematical model

The circuit computes `g(x²) = (f(x)+f(-x))/2 + α·x⁻¹·(f(x)-f(-x))/2` modulo `2⁶⁴−2³²+1`; see [Mathematical model](docs/mathematical-model.en.md) for exact input conventions, equations, and a known-answer example.

## Reproduction

Reproduce functionality and performance by emitting Chisel RTL and running independent C/Python references, eight simulation modes, and equivalence checks against the supplied SV; see [Reproduction](docs/reproduction.en.md).

## Synthesis results

The single-lane configuration passes DC setup at 500 MHz with 10% setup clock uncertainty in TSMC 28 HPC+, SSG 0.81 V −40°C. All 22719 examined endpoint paths have strictly fewer than 50 logic levels; see [Synthesis results](docs/synthesis-summary.en.md) for conditions, timing, and area.

## Publication scope

Published materials include reproducible RTL design, emission and simulation workflows, and written summaries of synthesis conditions and results. We apologize that foundry licensing and commercial-tool confidentiality requirements prevent us from distributing the process library, original DC Tcl, raw reports, or synthesis snapshots. The delivered component is a standalone folding operator; the integrator organizes commitments, query scheduling, inverse generation, and system interconnects.

## OpenRTL and custom integration

Browse [OpenRTL](https://github.com/OpenRTL-Lab), the [ZK accelerator catalogue](https://github.com/OpenRTL-Lab/zk-accelerators) and [OpenRTL website](https://openrtl.top/) for related modules and the cooperation/contact entry. Custom datapaths, interfaces and target-platform adaptation are optional services; MIT commercial use does not require buying a service. Do not submit confidential specifications in public issues.

## Release sources and integration

The version tag identifies the current source tree, including source notices and documentation. Release downloads provide hardware sources and bundled RTL for this version. Archive annotations and documentation can differ from the online tree. For RTL integration, use the product file list rather than verification environments; use the verification and synthesis documents for supported configurations and measurement conditions.
