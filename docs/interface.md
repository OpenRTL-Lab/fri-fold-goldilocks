<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/interface.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 接口说明

[English](interface.en.md)

本模块提供任务、输入流、输出流及完成通知四组原生 `valid/ready` 接口；任务声明点对数量和挑战，数据按顺序流过流水线，完成状态决定整项任务是否可用。

## 时钟、复位与握手

顶层 `GoldilocksFriFold` 使用单时钟 `clock` 和同步高有效 `reset`。复位取消正在进行的任务及未消费结果，不产生该任务的完成通知；复位后可重新提交。任一接口在时钟上升沿同时 `valid && ready` 时完成传输；发送方在等待期间保持 `valid` 及全部载荷不变。`io_busy` 表示存在进行中任务或待消费完成通知。

生成参数固定 `lanes`（每拍1或2个点对槽）和 `maxPairs`（8～32,768中的2的幂）。随包名称 lane1/M32768 表示单 lane、maxPairs=32,768，不要求每任务都达到该长度。任务字段 `pairs` 指定本次实际点对数，alpha 是整任务共用挑战。每个非零点 x 对应 a=f(x)、b=f(-x)、u=x 的逆元，结果为 (a+b)/2 + alpha*u*(a-b)/2。规范字是无符号整数 0 ≤ v < p，p = 2^64 - 2^32 + 1。

## 载荷

SV 扁平端口以 `io_task_bits_`、`io_input_bits_`、`io_output_bits_`、`io_completion_bits_` 为前缀，Vec 元素以 `_0` 或 `_1` 为后缀。

| 接口 | 字段 | 位宽与含义 |
| --- | --- | --- |
| task | tag | 32位，输出及完成通知原样返回 |
| task | mode、profile | 各2位，均填0 |
| task | pairs | `log₂(maxPairs)+1` 位，实际点对数1..maxPairs |
| task | alpha | 64位规范 Goldilocks 挑战 |
| input | pairs[i].a、b、u | 每 lane 三个64位域元素 |
| input | keep、last | lane有效掩码与任务最后一拍标记 |
| output | values[i] | 每 lane 一个64位规范结果 |
| output | tag、index | 32位任务标签；`log₂(maxPairs)` 位首个点对索引 |
| output | keep、last | 对应输入的有效掩码与最后一拍标记 |
| completion | tag、status | 32位标签与3位状态码 |

lane1/M32768 的 `pairs` 位宽16、`index` 位宽15。lane2 的每拍索引为0、2、4…；奇数长度最后一拍只使用lane0，`keep=01`，inactive输出为0。其他拍 `keep` 全1；`last` 仅在声明长度的末拍为1。实际任务长度无需为2的幂。

## 状态与生命周期

| status | 名称 | 行为 |
| ---: | --- | --- |
| 0 | OK | 任务全部结果有效 |
| 1 | BAD_PARAM | mode/profile非0、pairs非法、或alpha不规范；不接受数据，直接通知完成 |
| 2 | ZERO_DENOM | 有效lane的u为0 |
| 3 | BAD_FRAME | keep/last不符，或有效lane的a/b/u不规范 |

BAD_FRAME 优先于 ZERO_DENOM。数据错误时仍按声明长度消费并排空结果；结果可能在错误发现之前已经输出，调用方应等待完成状态，并在非OK时丢弃整项任务。非零但错误的 `u` 与点的对应关系由调用方负责，见 [数学模型](mathematical-model.md)。

`task.ready` 只在空闲时为1。最后一拍输出被消费后，下周期提供完成通知；完成通知被消费后，下一周期恢复任务接收。接收数据受52项保留额度保护，可响应输出背压。性能基准使用持续可供数与持续可消费结果的理想接口。
