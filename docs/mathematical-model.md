<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/mathematical-model.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 数学模型

[English](mathematical-model.en.md)

本电路在 Goldilocks 素域上完成二元 FRI 折叠：将同一多项式在相反点的两个求值与一个挑战结合，输出折叠多项式在平方点的求值；以下完整定义数值范围、配对方式及逆元责任。

## 域与输入

所有算术在以下域内执行，外部以64位无符号整数的规范表示传输域元素：

$$
p=2^{64}-2^{32}+1,\qquad \mathbb F_p=\mathbb Z/p\mathbb Z,\qquad 0\le a,b,\alpha,u<p.
$$

任务携带固定挑战 $\alpha$，每个点对输入 $(a,b,u)$。对非零域元素 $x$，调用方应提供：

$$
a=f(x),\qquad b=f(-x),\qquad u=x^{-1},\qquad ux=1\pmod p.
$$

电路没有 `x` 输入，故非零 `u` 与点的逆元关系由调用方建立；标签不参与计算。$2^{-1}$ 的规范表示为 `0x7fffffff80000001`。

## 精确映射

每个有效点对输出一个规范域元素：

$$
s=(a+b)2^{-1},\qquad d=(a-b)2^{-1},\qquad c=\alpha u,\qquad y=s+cd\quad\in\mathbb F_p.
$$

等价地：

$$
y=g(x^2)=\frac{f(x)+f(-x)}{2}+\alpha x^{-1}\frac{f(x)-f(-x)}{2}.
$$

定义 $f(X)=\sum_j f_jX^j=f_e(X^2)+Xf_o(X^2)$，则：

$$
f_e(Y)=\sum_j f_{2j}Y^j,\quad f_o(Y)=\sum_j f_{2j+1}Y^j,\quad g(Y)=f_e(Y)+\alpha f_o(Y).
$$

输入 `a` 对应正点、`b` 对应负点，交换二者会改变奇数项的符号；点对必须遵守这一约定。对于无多项式来源但 u 非零的规范三元组，电路仍严格计算相同的代数映射；接口契约将 u=0 判为错误。

## 已知答案与多轮集成

取 $f(X)=3+5X+7X^2+11X^3$、$x=2$、$\alpha=13$。此时 $a=129$、$b=p-67$、$u=2^{-1}$，折叠多项式为 $g(Y)=68+150Y$，输出 $g(4)=668$。C 与 Python 参考均独立检查这一答案以及多项式系数折叠关系。

一个任务以同一挑战处理指定数量的点对。调用方可逐轮更新挑战、点域和配对关系，复用本算子组织多轮 FRI。此接口定义的是素域二元折叠；扩域、其他折叠阶数和完整证明协议具有各自的数学映射。状态及错误处置见 [接口说明](interface.md)。
