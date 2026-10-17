<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/architecture.en.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Architecture

[简体中文](architecture.md)

Each lane implements binary folding with two 21-stage modular-multiplication pipelines and parallel half-sum/half-difference paths, accepting one active beat per cycle. Input registers, a finite output queue, and an elastic output register support native handshaking and backpressure.

One task applies a fixed challenge alpha to a declared number of pairs. A lane is one pair slot in an accepted data beat. For each nonzero point x, its inputs are a=f(x), b=f(-x), and u=x inverse; the result is (a+b)/2 + alpha*u*(a-b)/2. Division by two is field multiplication by the inverse of 2. Words are canonical values 0 <= v < p for p = 2^64 - 2^32 + 1. The caller supplies the correctly associated inverse; see [Mathematical model](mathematical-model.en.md).

## Datapath

| Unit | Instances per lane | Functional stages | Operation |
| --- | ---: | ---: | --- |
| Half-sum / half-difference | 1 each | 10 | `(a+b)/2` and `(a-b)/2` |
| First modular multiplier | 1 | 21 | `α·u` |
| Second modular multiplier | 1 | 21 | `(α·u)·(a-b)/2` |
| Final modular adder | 1 | 5 | Combine half-sum and product |

The half-difference is aligned with the first multiplication for the second multiplier; the half-sum is delayed to the final adder. Lane latency is 47 cycles, plus one input register, giving 48 arithmetic cycles and 50 cycles from input acceptance to external output acceptance when the output consumer is ready; output backpressure can increase this external latency. Task-level results are in [Performance](performance.en.md).

Each multiplier uses sixteen 16×16 partial products, six registered carry-save compression levels, block carry-select addition, and dedicated Goldilocks reduction. The synthesis product contains no FPGA primitives and depends on neither a generic remainder operator nor ROM initialization files.

## Buffering and task control

The output queue has 52 entries, each 113 bits in the single-lane configuration, followed by one elastic output register. Reservation accounting covers accepted inputs, in-flight computations, and results awaiting external consumption. Input acceptance stops when reservations fill, without combinational dependence on external `output.ready`. The synthesizable storage description maps to registers in the typical DC configuration.

One task executes at a time, with completion handshakes separating tasks. Many pairs from one task can be in flight concurrently, so task latency and pair initiation interval are measured separately. Generation supports `lanes=1/2`; `maxPairs` sets counter and index widths rather than allocating an entire evaluation table.

## Verification and product separation

The product top is `GoldilocksFriFold`. The simulation environment `FriFoldSimTop` holds reference vectors, checks, and cycle logs. The emission script extracts the product module closure and lints it separately. Performance observation follows functional valid markers; the public product exposes no additional probe ports or counters. See [Interface](interface.en.md) for integration details.
