<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/reproduction.en.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Reproduction

[简体中文](reproduction.md)

Follow this guide to regenerate RTL from public Chisel sources, cross-check independent C/Python references, execute eight cycle-accurate simulation modes, and reproduce latency, throughput, and compute-unit utilization. The typical configuration also checks the supplied SV against regenerated circuitry.

## Obtain the release snapshot

[Release v1.1.0](https://github.com/OpenRTL-Lab/fri-fold-goldilocks/releases/tag/v1.1.0)

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/fri-fold-goldilocks.git
cd fri-fold-goldilocks
```

The version tag identifies the current source tree, including source notices and documentation. Release downloads provide hardware sources and bundled RTL for this version. Archive annotations and documentation can differ from the online tree. For RTL integration, use the product file list rather than verification environments; use the verification and synthesis documents for supported configurations and measurement conditions.

## 1. Prepare the tools

Install the generators, compilers, and simulator described in [Toolchain](toolchain.en.md), then run the following checks from the repository root to confirm that scripts can find them:

```bash
mill --version
java -version
verilator --version
python3 --version
cc --version
g++ --version
```

Mill resolves generation dependencies on the first build, so prepare network access or tool caches. These commands check the environment; they do not perform functional verification.

Use a fresh relative output directory under `build/`; scripts reject existing output directories. In these commands, `--lanes` fixes hardware pair slots per beat, `--pairs` fixes generation-time maxPairs, and `--tasks` is the verification request count, not a hardware replication count. Runtime tasks choose their own lengths up to maxPairs.

## 2. Reproduce the complete workflow

The following command emits a single-lane circuit for up to 32768 pairs, runs 16 tasks, and checks independent references, product lint, eight simulations, and supplied SV. `--jobs` controls compilation parallelism only:

```bash
python3 tools/verify.py --out build/verify --jobs 8
```

On success, `build/verify/results.json` contains `status=0` and `bundled_product_matches=true`. The test and performance results in `generated` and `bundled` match. Main outputs:

| Path | Contents |
| --- | --- |
| `build/verify/rtl/product.f` | Product module list used from the repository root |
| `build/verify/rtl/simulation.f` | Simulation list including reference ROMs and environment |
| `build/verify/vectors/` | C-generated, Python-checked vectors |
| `build/verify/generated/` | Generated-SV build and eight-mode logs |
| `build/verify/bundled/` | Eight-mode logs for supplied SV |
| `build/verify/results.json` | Tool versions, test results, and per-task performance |

Negative-control modes expect exit code 1; the full verification script treats correct error detection as success. Other modes expect exit code 0. A negative-control log containing `pass=0` does not by itself mean that full verification failed.

## 3. Eight simulation modes

| Mode | Checks |
| --- | --- |
| ideal | Ideal interfaces, 16 tasks, exact per-result comparison, performance |
| protocol | Variable lengths, active masks, backpressure, and stable stalled payloads |
| reset | Mid-task reset followed by complete task recovery |
| negative-reference | Corrupt an expected answer and detect one numerical error |
| negative-inverse | Corrupt inverse pairing and detect one error against the reference |
| errors | 13 parameter/data-error tasks followed by 3 valid tasks |
| isolated | End-to-end latency of one isolated task |
| queued-reset | Reset and recover while the queue and output are stalled |

The C reference uses 128-bit software multiplication and remainder; Python uses arbitrary-precision integers. Both are independent of the hardware reduction structure. Tests cover known answer 668, constant/odd/even polynomials, challenges 0/1/p−1, field boundaries, and deterministic samples, with exact checks of values, tags, indices, keep/last, and completion status.

## 4. Configuration checks and emission only

The following commands verify the minimum size and a two-lane configuration, including the partially active final-beat protocol:

```bash
python3 tools/verify.py --out build/verify-small --pairs 8 --jobs 8
python3 tools/verify.py --out build/verify-lane2 --lanes 2 --pairs 64 --jobs 8
```

To generate RTL and reference files without running Verilator, use:

```bash
python3 tools/emit_sv.py --out build/emit --lanes 1 --pairs 32768 --tasks 16
```

`--lanes` is 1 or 2; `--pairs` is a power of two in 8..32768; `--tasks` is a multiple of 16 and at least 16. Here `--pairs` sets generation-time `maxPairs`; actual tasks may declare any length in 1..maxPairs. Existing output directories are not overwritten; choose a new directory for each run.

## 5. Read performance and integrate

`generated.performance` in `results.json` contains task acceptance/completion cycles, pair throughput, and per-stage functional-valid counts. Definitions and typical results are in [Performance](performance.en.md). Clock conversion and synthesis conditions are in [Synthesis results](synthesis-summary.en.md). Integrate the product top `GoldilocksFriFold`, not `FriFoldSimTop` or reference ROMs; the complete protocol is described in [Interface](interface.en.md).
