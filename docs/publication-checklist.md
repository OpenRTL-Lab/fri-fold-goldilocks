<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/publication-checklist.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 发布内容检查

[English](publication-checklist.en.md)

本仓库的发布检查覆盖文件白名单、双语文档链接、敏感信息与产品模块闭包，配合完整功能性能复现，保证下载源码包后能够独立使用设计和验证链路。

发布内容包括 Chisel、产品SV、C/Python模型、C++仿真驱动、生成与验证脚本、固定工具版本和双语说明；综合仅提供 [综合结果](synthesis-summary.md)的文字转述。

执行以下命令检查公开文件集、链接和载荷：

```bash
python3 tools/public_audit.py
```

检查拒绝本地绝对路径、凭证模式、内部追溯摘要和未列入白名单的文件；检查每份项目Markdown具有英文配对、同一目标的链接名一致、英文正文不链接中文文档。构建产物、运行日志、私有发布资料及Git目录不进入发布附件。数学域常量及工具版本属于设计内容，按语义区别于凭证或内部摘要。

功能与性能验证方法见 [快速复现](reproduction.md)。Release附件是完整公开仓库文件集的源码包，不包含私有工作记录。
