<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/publication-checklist.en.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Publication checks

[简体中文](publication-checklist.md)

Repository publication checks cover the file allowlist, bilingual documentation links, sensitive content, and product-module closure. Together with full functional and performance reproduction, they support independent use of the downloaded source package.

Deliverables include Chisel, product SV, C/Python models, the C++ simulation driver, emission and verification scripts, pinned tool versions, and bilingual documentation. Synthesis is published only as the written [Synthesis results](synthesis-summary.en.md).

Run the following command to check public files, links, and payload content:

```bash
python3 tools/public_audit.py
```

The audit rejects local absolute paths, credential patterns, internal provenance digests, and files outside the allowlist. It checks that every project Markdown has an English counterpart, links to the same target use consistent labels, and English prose does not link to Chinese documentation. Build outputs, logs, private publishing materials, and Git directories are excluded from attachments. Mathematical field constants and tool versions are design content, semantically distinct from credentials and internal digests.

Functional and performance methodology is described in [Reproduction](reproduction.en.md). The Release attachment contains the complete public repository file set without private working records.
