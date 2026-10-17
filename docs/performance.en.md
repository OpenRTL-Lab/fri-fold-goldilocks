<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/performance.en.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Performance

[简体中文](performance.md)

With ideal interfaces, the single-lane, 32768-pair configuration measures 32819 cycles of task latency, a 32820-cycle task interval, and 0.998415600 pairs/cycle sustained throughput. Each of the two principal modular-multiplication pipelines contains valid work in 99.902498% of cycles; definitions, windows, and counts below make these results reproducible.

## Methodology

Run the complete workflow in [Reproduction](reproduction.en.md), using 16 valid tasks of 32768 pairs each, with continuously available inputs and continuously ready output/completion consumers. Performance measurements introduce no external bandwidth, access-latency, or shared-port constraints. Protocol tests check backpressure separately.

Record rising-edge handshake cycles: A for task acceptance, F/I for first/last input, O₀/O for first/last output, and C for completion acceptance. Task latency is `L=C−A`, independently checked by the isolated-task mode. Consecutive task interval is `T=C_next−C`; throughput is measured across actual completion intervals rather than inferred from pipeline depth.

Exclude the first and last batches and measure `(C₂,C₁₃]`: 11 task intervals, 361020 cycles, and 360448 completed pairs. Task throughput is `11/361020`; pair throughput is `360448/361020`.

## Latency and throughput

| Metric | Cycles / cycle-normalized result | At 500 MHz |
| --- | ---: | ---: |
| Task latency C−A, 32768 pairs | 32819 cycles | 65.638 µs |
| Consecutive task completion interval | 32820 cycles | 65.640 µs |
| First output from first input O₀−F | 50 cycles | 100 ns |
| Last output from task acceptance O−A | 32818 cycles | 65.636 µs |
| Sustained task throughput | 0.000030469226 tasks/cycle | 15234.613 tasks/s |
| Sustained pair throughput | 0.998415600 pairs/cycle | 499.207800 million pairs/s |
| Active-stream pair initiation interval | 1 cycle/pair | 1 pair issued every 2 ns |

All 16 ideal tasks have a minimum and maximum latency of 32819 cycles, matching the isolated task. The 500 MHz column converts cycle measurements using the stated frequency; these are not board or silicon measurements, and the synthesis basis is in [Synthesis results](synthesis-summary.en.md). Here M is the actual number of pairs declared by a task and lanes is the generated number of pair slots per beat. Under ideal interfaces, when M is divisible by lanes and every beat is full, `L=M/lanes+51` and `T=M/lanes+52`. These formulas use the runtime length, not automatically the maxPairs capacity. Additional reproduction gives latency/interval of 59/60 cycles for lane1/M8 and 83/84 cycles for lane2/M64.

The datapath holds multiple different pairs concurrently, while control executes one task at a time. Task throughput therefore follows the measured interval for the chosen size. The 50-cycle pair latency is not a 50-cycle initiation interval.

## Compute-unit utilization

Utilization follows real functional `valid` markers, not switching activity or power. Per-stage utilization is valid-processing cycles divided by window cycles. Pipeline occupancy counts cycles with at least one valid stage. These separately describe productive stage load and whether an entire pipeline is empty.

| Unit | Instances | Stages | Valid / idle cycles per stage | Nonempty / empty pipeline cycles | Stage utilization | Pipeline occupancy |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| First modular multiplier | 1 | 21 | 360448 / 572 | 360668 / 352 | 99.841560% | 99.902498% |
| Second modular multiplier | 1 | 21 | 360448 / 572 | 360668 / 352 | 99.841560% | 99.902498% |
| Half-sum and half-difference | 1 each | 10 each | 360448 / 572 | 360547 / 473 | 99.841560% | 99.868982% |
| Final modular adder | 1 | 5 | 360448 / 572 | 360492 / 528 | 99.841560% | 99.853748% |

Half-sum and half-difference share validity, so counts apply to each instance rather than their sum. Each multiplier consists of sixteen partial-product units plus compression and reduction; all 21 functional stages have the same valid count. The two multipliers account for approximately 51.80% of synthesized cell area, as defined in [Synthesis results](synthesis-summary.en.md). The continuous-input portion issues work in 100% of cycles; the table retains actual task-switching overhead.

## Result files and checks

`results.json` stores every task's handshake cycles, per-stage counts, and isolated-task result. The script checks fixed intervals, valid counts, fill/drain lengths, and calculations from raw cycle logs. Supplied and regenerated SV each execute all eight modes, with complete test and performance data compared.
