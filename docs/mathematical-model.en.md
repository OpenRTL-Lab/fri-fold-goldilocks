<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks FRI Folding Hardware Accelerator
Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
File: docs/mathematical-model.en.md

SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Mathematical model

[简体中文](mathematical-model.md)

This circuit performs binary FRI folding over the Goldilocks prime field: two evaluations of the same polynomial at opposite points are combined with a challenge to produce the folded polynomial evaluation at the squared point. The following definitions specify value ranges, pairing, and responsibility for inverse generation.

## Field and inputs

All arithmetic takes place in the following field. Field elements are transmitted as canonical unsigned 64-bit integers:

$$
p=2^{64}-2^{32}+1,\qquad \mathbb F_p=\mathbb Z/p\mathbb Z,\qquad 0\le a,b,\alpha,u<p.
$$

A task supplies a fixed challenge $\alpha$ and a stream of triples $(a,b,u)$. For a nonzero field point $x$, the caller supplies:

$$
a=f(x),\qquad b=f(-x),\qquad u=x^{-1},\qquad ux=1\pmod p.
$$

The circuit has no `x` input, so the caller establishes that nonzero `u` is the inverse of the paired point. Tags do not participate in arithmetic. The canonical representation of $2^{-1}$ is `0x7fffffff80000001`.

## Exact mapping

Each active pair produces one canonical field element:

$$
s=(a+b)2^{-1},\qquad d=(a-b)2^{-1},\qquad c=\alpha u,\qquad y=s+cd\quad\in\mathbb F_p.
$$

Equivalently:

$$
y=g(x^2)=\frac{f(x)+f(-x)}{2}+\alpha x^{-1}\frac{f(x)-f(-x)}{2}.
$$

Writing $f(X)=\sum_j f_jX^j=f_e(X^2)+Xf_o(X^2)$ gives:

$$
f_e(Y)=\sum_j f_{2j}Y^j,\quad f_o(Y)=\sum_j f_{2j+1}Y^j,\quad g(Y)=f_e(Y)+\alpha f_o(Y).
$$

Input `a` corresponds to the positive point and `b` to the negative point. Swapping them changes the sign of the odd component, so pairing must follow this convention. Canonical triples with nonzero u implement the same algebraic mapping even without a polynomial origin; u=0 is an error under the interface contract.

## Known answer and multi-round integration

For $f(X)=3+5X+7X^2+11X^3$, $x=2$, and $\alpha=13$, the inputs are $a=129$, $b=p-67$, and $u=2^{-1}$. The folded polynomial is $g(Y)=68+150Y$, giving $g(4)=668$. The C and Python references independently check this result and the polynomial-coefficient folding relation.

A task uses one challenge for its declared number of pairs. The caller can update challenges, domains, and pairing between rounds and reuse the operator for multi-round FRI. This interface defines binary prime-field folding; extension fields, other folding arities, and complete proof protocols have their own mappings. Status codes and error handling are specified in [Interface](interface.en.md).
