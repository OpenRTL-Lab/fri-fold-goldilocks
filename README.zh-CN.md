<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: README.zh-CN.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# fri-fold-goldilocks 硬件加速器

[English](README.md)

本仓库提供使用Chisel开发的Goldilocks域二元FRI折叠独立硬件加速器。原生流接口支持ASIC或FPGA集成，可加速STARK等FRI证明系统中的求值表折叠及验证端的对应折叠运算。

采用 [MIT 许可证](LICENSE)，允许免费商用、修改和再分发，须保留版权及许可声明；工具与生成代码说明见 [第三方声明](THIRD_PARTY_NOTICES.md)。

[发布 v1.1.0](https://github.com/OpenRTL-Lab/fri-fold-goldilocks/releases/tag/v1.1.0) · [接口说明](docs/interface.md) · [快速复现](docs/reproduction.md) · [性能指标](docs/performance.md) · [综合结果](docs/synthesis-summary.md)

## 计算与任务单位

一个任务用同一个调用方挑战alpha折叠声明数量的点对。每对输入f(x)、f(-x)和x的逆元，输出一个g(x²)。因此32,768对对应65,536个求值和32,768个折叠结果；逆元是额外输入，不是另一个求值，也不是本模块算出的值。域元素采用p = 2^64 - 2^32 + 1下的无符号64位规范值0 ≤ v < p。

lane是一个数据拍中的点对槽位。随包单lane接口每次握手至多接收一对，不表示每拍完成整个任务。输出tag标识任务，index标识任务内点对位置。任务是折叠批次，不包含完整FRI轮的承诺、查询与挑战生成。

## 配置速览

| 集成问题 | 已发布硬件 |
| --- | --- |
| 计算 | 二元快速Reed–Solomon交互式预言机邻近性证明（FRI）的求值表折叠 |
| 数域 | Goldilocks p = 2^64 - 2^32 + 1 |
| 随包配置 | 单通道，每任务最多32,768点对；顶层`GoldilocksFriFold` |
| 输入／输出 | 每对提供f(x)、f(-x)及x的逆元；每个输出是一个折叠域元素 |
| 原生边界 | 任务／挑战、有序输入、输出和完成ready/valid通道；返回tag/index |

## 快速开始

从 [发布 v1.1.0](https://github.com/OpenRTL-Lab/fri-fold-goldilocks/releases/tag/v1.1.0) 下载源码包，或克隆当前发布快照：

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/fri-fold-goldilocks.git
cd fri-fold-goldilocks
```

按 [工具链说明](docs/toolchain.md) 安装精确版本的前置工具，再在仓库根目录使用全新输出目录运行：

```bash
python3 tools/emit_sv.py --out build/emit --lanes 1 --pairs 32768 --tasks 16
```

此生成命令不是完整RTL回归；文档规定的验证入口为：

```bash
python3 tools/verify.py --out build/verify --jobs 8
```

预期结果、负控判读及短功能运行与完整性能运行的区别见 [快速复现](docs/reproduction.md)。

## 验证与集成边界

复现流程比较独立C/Python参考和生成／随包SV，并记录八种仿真模式。功能检查将握手及错误帧与理想接口计时分开；复位恢复和故意参考错误是明确门禁。解释模拟器返回码前，先读模式表及预期完成状态。

调用方按规定点对顺序提供alpha及点逆元。本模块不生成挑战、不计算逆元表、不提交Merkle树，也不实现完整FRI/STARK协议。复位丢弃在途任务而不返回其完成通知；任务状态决定完整输出是否可用。

### 正确理解性能

理想原生接口下，一个32,768点对任务耗时32,819周期，相邻任务完成间隔32,820周期，吞吐为0.998415600点对／周期。两条模乘流水线各有99.902498%的实测稳态窗口周期至少一级包含有效工作；这是流水线占用率，不是逐级发射忙率或任务完成率。

这些是理想原生接口下已记录的RTL周期测量。按MHz换算的时间／速率不是板级或硅后测量；Design Compiler结果仅适用于文档所列配置、库角及约束，不构成布局布线签核或系统吞吐结论。条件见 [性能指标](docs/performance.md) 和 [综合结果](docs/synthesis-summary.md)。

## 仓库内容

- [Chisel 设计](operators/fri-fold/src/main/scala/zk/fri/)：折叠流水线、域运算、原生接口与验证环境。
- [生成入口](operators/fri-fold/src/tool/scala/zk/fri/Emit.scala)与 [SV 生成脚本](tools/emit_sv.py)：配置 lane 数及最大任务规模。
- [生成好的 SV](rtl/v1.1.0/lane1-m32768/)：单 lane、最大32768点对的产品电路，顶层为 `GoldilocksFriFold`。
- [C 参考模型](operators/fri-fold/cmodel/reference.c)、[Python 交叉检查](operators/fri-fold/tools/check_reference.py)与 [仿真驱动](operators/fri-fold/sim/driver.cpp)：完整功能与性能验证链路。
- [架构说明](docs/architecture.md)、[接口说明](docs/interface.md)与 [工具链说明](docs/toolchain.md)：集成及复现所需资料。

## 应用场景

在需要 Goldilocks 域二元 FRI 折叠时，本模块可承担证明端批量求值表缩减、验证端查询点折叠、或多轮 FRI 调度器中的流水计算单元；调用方提供点对、挑战和点的逆元，并组织承诺与查询流程。

## 性能指标

单 lane 理想接口下，32768点对任务延迟为32819周期，连续任务吞吐率为0.998416点对/周期，两条模乘流水线的占用率均为99.9025%；定义、测试方法与完整数值见 [性能指标](docs/performance.md)。

## 数学模型

电路计算 `g(x²) = (f(x)+f(-x))/2 + α·x⁻¹·(f(x)-f(-x))/2`，模数为 `2⁶⁴−2³²+1`；严格输入约定、公式与已知答案见 [数学模型](docs/mathematical-model.md)。

## 快速复现

从 Chisel 生成电路，再通过独立 C/Python 模型、八种仿真模式和已提供 SV 的一致性检查复现功能与性能；操作步骤见 [快速复现](docs/reproduction.md)。

## 综合结果

单 lane 配置在 TSMC 28 HPC+、SSG 0.81 V −40°C、500 MHz及10% setup时钟不确定度条件下通过 DC setup，全部22719条受检端点路径严格小于50级；完整条件、时序及面积见 [综合结果](docs/synthesis-summary.md)。

## 公开边界

公开内容包括可复现的 RTL 设计、生成与仿真链路，以及综合条件和结果的文字转述。抱歉，由于工艺库授权与商业工具资料的保密要求，无法提供工艺库、原始 DC Tcl、原始报告或综合快照。项目交付独立折叠算子，承诺、查询调度、逆元生成和系统互连由集成方组织。

## OpenRTL与定制集成

通过[OpenRTL组织主页](https://github.com/OpenRTL-Lab)、[ZK加速器集合](https://github.com/OpenRTL-Lab/zk-accelerators)及[OpenRTL网站](https://openrtl.top/)查找相关模块与合作联系入口。需要定制数据通路、接口或目标平台适配时可联系维护者；MIT商用权不附加购买服务的要求，不要在公开Issue提交保密规格。

## 发布源码与集成

版本标签标识包含来源声明和文档的当前源码树；Release 下载提供本版本硬件源码与随包 RTL。源码包与在线目录的注释及文档可能不同。RTL 集成使用产品文件清单，不把验证环境作为产品电路；支持配置、验证范围和测量条件见验证及综合文档。
