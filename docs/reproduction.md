<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/reproduction.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 快速复现

[English](reproduction.en.md)

按照本说明可从公开 Chisel 代码重新生成电路，独立核对 C/Python 参考，执行八种逐周期仿真，并复现延迟、吞吐率与计算单元忙率；典型配置还会验证仓库提供的 SV 与重新生成电路一致。

## 获取版本快照

[发布 v1.1.0](https://github.com/OpenRTL-Lab/fri-fold-goldilocks/releases/tag/v1.1.0)

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/fri-fold-goldilocks.git
cd fri-fold-goldilocks
```

版本标签标识包含来源声明和文档的当前源码树；Release 下载提供本版本硬件源码与随包 RTL。源码包与在线目录的注释及文档可能不同。RTL 集成使用产品文件清单，不把验证环境作为产品电路；支持配置、验证范围和测量条件见验证及综合文档。

## 1. 准备工具

先按 [工具链说明](toolchain.md)安装生成器、编译器与仿真工具，然后在仓库根目录执行以下检查，确认脚本可找到它们：

```bash
mill --version
java -version
verilator --version
python3 --version
cc --version
g++ --version
```

生成依赖由 Mill 首次解析；提前准备网络或工具缓存。上述命令只检查环境，不执行功能验证。

使用 `build/` 下全新的相对输出目录，脚本拒绝已存在目录。命令中的 `--lanes` 固定硬件每拍点对槽数，`--pairs` 固定生成容量 maxPairs，`--tasks` 是验证请求数、不是硬件复制数。接口任务可选择不超过 maxPairs 的实际长度。

## 2. 一次复现完整链路

以下命令生成单lane、最大32768点对电路，运行16项任务，并核对独立参考、产品lint、八种仿真与提供的SV；`--jobs` 仅设置编译并行度：

```bash
python3 tools/verify.py --out build/verify --jobs 8
```

成功时 `build/verify/results.json` 中 `status=0`、`bundled_product_matches=true`。`generated` 与 `bundled` 的测试及性能结果一致。主要输出：

| 路径 | 内容 |
| --- | --- |
| `build/verify/rtl/product.f` | 从仓库根目录使用的产品模块清单 |
| `build/verify/rtl/simulation.f` | 含参考ROM及验证环境的仿真清单 |
| `build/verify/vectors/` | 独立C模型生成、Python核对的向量 |
| `build/verify/generated/` | 生成SV的编译和八模式运行日志 |
| `build/verify/bundled/` | 仓库SV的八模式运行日志 |
| `build/verify/results.json` | 工具版本、验证结果及逐任务性能 |

负控制模式预期退出码1，完整验证脚本将正确检测到错误视为通过；其余模式预期退出码0。不要单以负控制日志中的 `pass=0` 判断整个验证失败。

## 3. 八种测试模式

| 模式 | 检查内容 |
| --- | --- |
| ideal | 理想接口、16任务、逐结果精确比较与性能 |
| protocol | 不同长度、有效掩码、背压及等待期间稳定性 |
| reset | 运行中复位并重新执行完整任务 |
| negative-reference | 破坏参考答案，确认检测到一个数值错误 |
| negative-inverse | 破坏点逆元配对，确认参考比较检测到一个错误 |
| errors | 13项参数/数据错误任务，再执行3项正常任务 |
| isolated | 单独一个任务的完整延迟 |
| queued-reset | 队列与输出处于等待状态时复位并恢复 |

C参考使用128位软件乘法取模，Python参考使用任意精度整数，两者与硬件专用约减结构独立。检查已知答案668、常数/纯奇/纯偶多项式、挑战0/1/p−1、域边界与确定性样本；逐项核对数值、tag、index、keep、last和完成状态。

## 4. 配置验证与单独生成

以下命令分别验证最小规模和双lane配置，确认生成参数与部分有效末拍协议：

```bash
python3 tools/verify.py --out build/verify-small --pairs 8 --jobs 8
python3 tools/verify.py --out build/verify-lane2 --lanes 2 --pairs 64 --jobs 8
```

只需生成电路及参考文件时，执行以下命令，不运行 Verilator：

```bash
python3 tools/emit_sv.py --out build/emit --lanes 1 --pairs 32768 --tasks 16
```

`--lanes` 为1或2，`--pairs` 为8..32768中的2的幂；`--tasks` 为至少16且为16的倍数。这里 `--pairs` 是生成时的 `maxPairs`，接口允许每项任务选择1..maxPairs的实际长度。输出目录存在时脚本拒绝覆盖，重新运行请换目录。

## 5. 读取性能与集成

`results.json` 的 `generated.performance` 给出任务接收/完成周期、点对吞吐率以及逐级功能有效计数；定义和典型数值见 [性能指标](performance.md)。时钟周期换算与综合条件分别见 [综合结果](synthesis-summary.md)。产品集成使用 `GoldilocksFriFold`，不要把 `FriFoldSimTop` 或参考ROM作为产品顶层；完整协议见 [接口说明](interface.md)。
