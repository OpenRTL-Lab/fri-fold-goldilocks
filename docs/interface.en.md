<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/interface.en.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Interface

[简体中文](interface.md)

The module exposes four native `valid/ready` interfaces: task, input stream, output stream, and completion. A task declares its pair count and challenge; ordered data flows through the pipelines, and completion status determines whether the entire task is usable.

## Clock, reset, and handshaking

Top module `GoldilocksFriFold` uses a single `clock` and synchronous active-high `reset`. Reset cancels the active task and unconsumed results without issuing its completion; a new task can then be submitted. A transfer occurs on a rising edge with `valid && ready`. The sender holds `valid` and all payload fields stable while stalled. `io_busy` indicates an active task or an unconsumed completion.

Generation fixes `lanes` (1 or 2 pair slots per beat) and `maxPairs` (a power of two from 8 to 32,768). The bundled lane1/M32768 name means one lane and maxPairs=32,768, not a mandatory runtime length. Each task declares its actual `pairs` count and one challenge alpha. For each nonzero x, a=f(x), b=f(-x), u=x inverse; the output is (a+b)/2 + alpha*u*(a-b)/2. Canonical words are unsigned integers 0 <= v < p, p = 2^64 - 2^32 + 1.

## Payloads

Flattened SV ports use the prefixes `io_task_bits_`, `io_input_bits_`, `io_output_bits_`, and `io_completion_bits_`; Vec elements use `_0` or `_1` suffixes.

| Interface | Field | Width and meaning |
| --- | --- | --- |
| task | tag | 32 bits, returned unchanged with outputs and completion |
| task | mode, profile | 2 bits each; both must be 0 |
| task | pairs | `log₂(maxPairs)+1` bits; actual pair count 1..maxPairs |
| task | alpha | 64-bit canonical Goldilocks challenge |
| input | pairs[i].a, b, u | Three 64-bit field elements per lane |
| input | keep, last | Active-lane mask and final-beat flag |
| output | values[i] | One 64-bit canonical result per lane |
| output | tag, index | 32-bit task tag; `log₂(maxPairs)`-bit first-pair index |
| output | keep, last | Corresponding active mask and final-beat flag |
| completion | tag, status | 32-bit tag and 3-bit status code |

For lane1/M32768, `pairs` is 16 bits and `index` is 15 bits. With two lanes, beat indices are 0, 2, 4, …; an odd-length final beat activates only lane0 with `keep=01`, and the inactive result is zero. All other beats use an all-ones `keep`; only the declared final beat sets `last`. Actual task lengths need not be powers of two.

## Status and lifecycle

| status | Name | Behavior |
| ---: | --- | --- |
| 0 | OK | All results in the task are valid |
| 1 | BAD_PARAM | Nonzero mode/profile (not 0), invalid pairs, or noncanonical alpha; no data accepted, direct completion |
| 2 | ZERO_DENOM | An active lane supplies u=0 |
| 3 | BAD_FRAME | Incorrect keep/last, or noncanonical a/b/u on an active lane |

BAD_FRAME takes precedence over ZERO_DENOM. For data errors, the declared frame is still consumed and results drained. Some results may have been emitted before the error was detected; callers wait for completion and discard the entire task on non-OK status. The caller is responsible for associating a nonzero `u` with the correct point, as specified in [Mathematical model](mathematical-model.en.md).

`task.ready` is asserted only while idle. Completion becomes available one cycle after the final output is consumed. Task acceptance resumes one cycle after completion is consumed. Input acceptance is protected by 52 reservations and responds to output backpressure. Performance benchmarks use continuously available inputs and continuously ready consumers.
