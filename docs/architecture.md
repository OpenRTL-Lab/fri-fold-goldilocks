<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/architecture.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 架构说明

[English](architecture.en.md)

本设计使用每 lane 两条21级模乘流水线和并行半和差路径实现二元折叠，活动流的发射间隔为1周期；输入寄存、有限输出队列与弹性输出寄存器共同支持原生握手和背压。

一个任务以固定挑战 alpha 处理声明数量的点对。lane 是一次输入握手中的一个点对槽。对非零点 x，每槽输入 a=f(x)、b=f(-x)、u=x 的逆元，输出 (a+b)/2 + alpha*u*(a-b)/2；除以二指乘域内 2 的逆元。域元素采用 p = 2^64 - 2^32 + 1 下的规范值 0 ≤ v < p，调用方提供与点正确对应的逆元，见 [数学模型](mathematical-model.md)。

## 数据通路

| 单元 | 每 lane 数量 | 功能流水级数 | 作用 |
| --- | ---: | ---: | --- |
| 半和/半差 | 各1 | 10 | `(a+b)/2` 与 `(a-b)/2` |
| 第一模乘 | 1 | 21 | `α·u` |
| 第二模乘 | 1 | 21 | `(α·u)·(a-b)/2` |
| 最终模加 | 1 | 5 | 合并半和与乘积 |

半差与第一模乘对齐进入第二模乘，半和通过延迟寄存器对齐最终模加。lane 内功能延迟47周期，入口再有1级寄存，总算术延迟48周期；输出可立即被下游接受时，从输入接受到外部输出接受为50周期；输出背压会增加该外部延迟。任务性能见 [性能指标](performance.md)。

每条模乘使用16个16×16部分积、6级寄存进位保存压缩、分块进位选择加法与专用 Goldilocks 约减。综合产品不包含 FPGA 原语，也不依赖通用取模运算符或 ROM 初始化文件。

## 缓冲与任务控制

输出队列深52，单 lane 每项113位，后接1级弹性输出寄存器。保留计数覆盖已接受、流水计算及尚未被外部消费的结果，满额时停止接受输入；控制不依赖外部 `output.ready` 的组合传播。队列用可综合存储描述，典型 DC 配置映射为寄存器。

同时执行一个任务；任务之间通过 completion 握手衔接。任务内部大量点对可在流水线中并行飞行，因此任务延迟与点对发射间隔分别测量。通过 `lanes=1/2` 生成相应的独立 lane；`maxPairs` 配置计数与索引位宽，不分配整张求值表存储。

## 验证与产品分离

产品顶层为 `GoldilocksFriFold`。仿真环境 `FriFoldSimTop` 包含参考向量、检查和周期日志；生成脚本提取产品模块闭包并单独 lint。性能观测使用功能有效标记，公开产品不额外导出探针端口或计数器。接口细节见 [接口说明](interface.md)。
