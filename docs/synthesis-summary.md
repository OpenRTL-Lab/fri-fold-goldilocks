<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/synthesis-summary.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 综合结果

[English](synthesis-summary.en.md)

单lane、最大32768点对的 `GoldilocksFriFold` 在 TSMC 28 HPC+ 的 SSG 0.81 V −40°C 条件下通过500 MHz、10% setup时钟不确定度的 DC setup 检查；总cell面积0.063527324 mm²，22719条受检端点路径的 Z/ZN 逻辑级数最大36级。

## 条件

| 项目 | 条件 |
| --- | --- |
| 产品版本 | 1.1.0，lane1，maxPairs32768 |
| 综合工具 | Synopsys Design Compiler NXT，X-2025.06-SP3 |
| 工艺 | TSMC 28 HPC+ 标准单元 |
| 工艺角 | SSG，0.81 V，−40°C |
| 时钟周期 | 2.0 ns，即500 MHz |
| setup时钟不确定度 | 0.2 ns，即周期的10% |
| 输入/输出延迟 | 最大1.0 ns，最小0 ns |
| 最大数据转换时间 | 0.25 ns |
| 输出负载 | 0.02 pF |
| 最大扇出约束 | 32 |
| reset | 同步高有效，纳入时序检查 |
| 存储实现 | 寄存器映射；物理SRAM宏0个 |

WNS 表示最差 setup 裕量，TNS 是负 setup 裕量之和。下文逻辑级数统计数据路径中特定单元输出引脚，不是算术流水线周期数。

## Setup 与逻辑级数

| 指标 | 结果 |
| --- | ---: |
| setup WNS | +0.000020981 ns |
| setup TNS | 0 ns |
| setup违例端点 | 0 |
| 已约束setup端点 | 22719 |
| 最大Z/ZN逻辑级数 | 36 |
| 严格小于50级的受检路径 | 22719，100% |
| 大于30级 | 28，0.1232% |
| 大于40/50/60级 | 各0 |

每个已约束setup端点取一条最差max路径，分母为22719；其中内部到内部22065、内部到输出150、输入到内部504。逐条完整路径与工具实时timing-points交叉核对：仅数据路径中叶引脚名严格等于 `Z` 或 `ZN` 时计一级，不按报告行数计级，不统计net行、输入引脚、起点clock-to-Q或时钟树。累计阈值不能相加。

## 标准单元面积

| 类别 | 面积µm² | 占比 |
| --- | ---: | ---: |
| 组合逻辑，不含Buf/Inv | 25006.660 | 39.3636% |
| Buf/Inv | 920.710 | 1.4493% |
| 非组合逻辑 | 37599.954 | 59.1871% |
| 物理存储宏/黑盒 | 0 | 0% |
| 总cell面积 | 63527.324 | 100% |

该面积为标准单元面积，不是芯片面积。第一/第二模乘分别为16658.530/16250.752 µm²，合计约51.80%；整个lane为45828.426 µm²，输出队列为15345.820 µm²。层次面积包含子层次，父子不能叠加。

## 资料公开方式

本页转述已完成综合的约束和结果；公开RTL与仿真可按 [快速复现](reproduction.md)复现。抱歉，由于工艺库授权及商业资料保密要求，我们无法公开工艺库、原始DC Tcl、原始报告、门级网表或综合快照。
