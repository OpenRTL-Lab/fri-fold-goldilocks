<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/synthesis-summary.en.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Synthesis results

[简体中文](synthesis-summary.md)

The single-lane `GoldilocksFriFold` supporting up to 32768 pairs passes DC setup at 500 MHz with 10% setup clock uncertainty in TSMC 28 HPC+, SSG 0.81 V −40°C. Total cell area is 0.063527324 mm², and the maximum Z/ZN logic depth across 22719 examined endpoint paths is 36 levels.

## Conditions

| Item | Condition |
| --- | --- |
| Product version | 1.1.0, lane1, maxPairs32768 |
| Synthesis tool | Synopsys Design Compiler NXT, X-2025.06-SP3 |
| Process | TSMC 28 HPC+ standard cells |
| Corner | SSG, 0.81 V, −40°C |
| Clock period | 2.0 ns, or 500 MHz |
| Setup clock uncertainty | 0.2 ns, or 10% of the period |
| Input/output delay | Maximum 1.0 ns, minimum 0 ns |
| Maximum data transition | 0.25 ns |
| Output load | 0.02 pF |
| Maximum fanout constraint | 32 |
| Reset | Synchronous active-high, included in timing checks |
| Storage implementation | Register mapping; 0 physical SRAM macros |

WNS denotes the minimum setup slack; TNS is the sum of negative setup slacks. Logic depth below counts selected data-path cell output pins, not arithmetic pipeline cycles.

## Setup and logic depth

| Metric | Result |
| --- | ---: |
| Setup WNS | +0.000020981 ns |
| Setup TNS | 0 ns |
| Violating setup endpoints | 0 |
| Constrained setup endpoints | 22719 |
| Maximum Z/ZN logic levels | 36 |
| Examined paths strictly below 50 levels | 22719, 100% |
| More than 30 levels | 28, 0.1232% |
| More than 40/50/60 levels | 0 each |

One worst max path is examined per constrained setup endpoint, giving 22719 paths: 22065 internal-to-internal, 150 internal-to-output, and 504 input-to-internal. Full path reports are cross-checked against live timing points. A level is counted only for a data-path pin whose leaf name is exactly `Z` or `ZN`, not by report line count. Net lines, input pins, startpoint clock-to-Q, and clock trees are excluded. Cumulative threshold counts must not be added.

## Standard-cell area

| Category | Area µm² | Share |
| --- | ---: | ---: |
| Combinational logic excluding Buf/Inv | 25006.660 | 39.3636% |
| Buf/Inv | 920.710 | 1.4493% |
| Noncombinational logic | 37599.954 | 59.1871% |
| Physical memory macros / black boxes | 0 | 0% |
| Total cell area | 63527.324 | 100% |

These are standard-cell areas, not die area. The first and second multipliers occupy 16658.530 and 16250.752 µm² respectively, approximately 51.80% together. The complete lane occupies 45828.426 µm² and the output queue 15345.820 µm². Hierarchical areas include children, so parent and child rows must not be added.

## Publication policy

This page summarizes the completed synthesis conditions and results; public RTL and simulation are reproducible through [Reproduction](reproduction.en.md). We apologize that process-library licensing and commercial confidentiality prevent publication of the process library, original DC Tcl, raw reports, gate-level netlist, or synthesis snapshots.
