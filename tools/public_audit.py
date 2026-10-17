#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks FRI Folding Hardware Accelerator
# Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
# Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
# File: tools/public_audit.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""只读检查公开白名单、敏感载荷及双语文档链接。"""
import os,posixpath,re,sys
from pathlib import Path
from urllib.parse import unquote,urlsplit
ROOT=Path(__file__).resolve().parents[1]
ROOT_FILES={'.gitignore','VERSION','build.mill','LICENSE','README.md','README.zh-CN.md','THIRD_PARTY_NOTICES.md','THIRD_PARTY_NOTICES.en.md'}
DOCS={'architecture','interface','mathematical-model','toolchain','reproduction','performance','synthesis-summary','publication-checklist'}
OP='operators/fri-fold/'
RTL={'FriFoldLane.sv','FriGoldilocksMul.sv','GoldilocksFriFold.sv','Queue52_FriOutput.sv','ram_52x113.sv'}
PRESET='rtl/v1.1.0/lane1-m32768/'
EXPECTED=(ROOT_FILES|{'docs/'+n+s for n in DOCS for s in ('.md','.en.md')}
    |{'tools/'+n+'.py' for n in ('emit_sv','metrics','verify','public_audit')}
    |{OP+'src/main/scala/zk/fri/'+n+'.scala' for n in ('FriArithmetic','FriFold','Environment')}
    |{OP+'src/tool/scala/zk/fri/Emit.scala',OP+'cmodel/reference.c',OP+'tools/check_reference.py',OP+'sim/driver.cpp'}
    |{PRESET+n for n in RTL|{'files.f'}})
EXPECTED |= {'docs/journal.en.md', 'docs/source-headers.en.md', 'tools/source_headers.py', 'tools/test_source_headers.py', 'docs/source-headers.md', 'docs/journal.md', 'VERSION.license', 'rtl/v1.1.0/lane1-m32768/files.f.license'}
IGNORED={'.git','.private','build','out','__pycache__','.cache'}
PATTERNS=[('机器绝对目录',r'\x2f(?:home|Users|root|opt|mnt|media|tmp|var|work|workspace|private|etc|[A-Za-z0-9_.-]+-data)\x2f'),
 ('Windows目录',r'\b[A-Z]:\\'),('内部摘要',r'\b[0-9a-fA-F]{40,64}\b'),
 ('内部追溯字段',r'\b(?:sha(?:1|256|512)|source[_-]commit|provenance[_-]id|runner[_-]digest)\b'),
 ('凭证模式',r'(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|AKIA[A-Z0-9]{16})'),
 ('私钥头',r'BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY'),
 ('IP地址',r'\b(?:\d{1,3}\.){3}\d{1,3}\b'),
 ('内部身份',r'\b(?:v\d{2}-[0-9a-f]{12}|agent-[0-9a-f]{16,})\b'),
 ('源码定位',r'\.scala:\d+:\d+')]

def text_failures(name,text):
    return [name+'：'+label for label,pattern in PATTERNS if re.search(pattern,text)]

def document_failures(documents,targets):
    failures=[];labels={}
    for name,content in sorted(documents.items()):
        other=('README.zh-CN.md' if name=='README.md' else 'README.md' if name=='README.zh-CN.md'
               else name[:-6]+'.md' if name.endswith('.en.md') else name[:-3]+'.en.md')
        english=name=='README.md' or name.endswith('.en.md')
        if other not in documents:failures.append(name+'：缺少语言配对')
        paragraphs=re.split(r'\n\s*\n',re.sub(r'\A<!--\nOpenRTL-Header-Begin.*?-->\n\n', '', content, count=1, flags=re.S).strip())
        if len(paragraphs)<2 or not paragraphs[0].startswith('# ') or re.match(r'(?:#|\||```|[-*] |\d+\. )',paragraphs[1]):
            failures.append(name+'：缺少首段摘要')
        if '\\operatorname' in content:failures.append(name+'：不支持的数学宏')
        for label,target in re.findall(r'(?<!!)\[([^\[\]\n]+)\]\(([^\s)]+)\)',content):
            parts=urlsplit(target)
            key=target if parts.scheme else posixpath.normpath(posixpath.join(posixpath.dirname(name),unquote(parts.path)))
            if not parts.scheme and key not in targets:failures.append(name+'：链接不存在：'+target)
            if label==('简体中文' if english else 'English') and key==other:continue
            target_english=key=='README.md' or key.endswith('.en.md')
            if english and key.endswith('.md') and not target_english:failures.append(name+'：英文指向中文')
            if target_english and re.search(r'[\u4e00-\u9fff]',label):failures.append(name+'：英文入口不是英文')
            lang='zh' if re.search(r'[\u4e00-\u9fff]',label) else 'en'
            previous=labels.setdefault((lang,key),label)
            if label!=previous:failures.append(name+'：链接名不一致：'+label+' / '+previous)
    return failures

def main():
    files=set();targets=set();documents={};failures=[]
    for folder,dirs,names in os.walk(ROOT):
        dirs[:]=[n for n in dirs if n not in IGNORED]
        for n in dirs:targets.add((Path(folder)/n).relative_to(ROOT).as_posix())
        for n in names:
            p=Path(folder)/n;name=p.relative_to(ROOT).as_posix();files.add(name);targets.add(name)
            if p.is_symlink():failures.append(name+'：符号链接');continue
            if name not in EXPECTED:failures.append(name+'：不在白名单')
            try:text=p.read_text(encoding='utf-8')
            except (UnicodeError,OSError):failures.append(name+'：非UTF-8文本');continue
            if '\x00' in text:failures.append(name+'：二进制字节')
            failures.extend(text_failures(name,text))
            if name.endswith('.md'):documents[name]=text
    failures.extend(n+'：缺少文件' for n in sorted(EXPECTED-files))
    failures.extend(document_failures(documents,targets))
    listing=(ROOT/PRESET/'files.f').read_text().splitlines()
    if set(listing)!=RTL or len(listing)!=len(RTL):failures.append('产品清单与五个模块不一致')
    if failures:print('\n'.join(failures));return 1
    print(f'公开载荷审查通过：{len(files)}个文本文件、{len(documents)}份Markdown，双语链接有效且名称一致。')
    return 0

if __name__=='__main__':sys.exit(main())
