#!/usr/bin/env python3
"""Moves the soccer packages/classes of tcrob.umu.quaky2 to tcrob.umu.soccer and fixes every reference.
Usage: refactor_soccer.py <repo root> [git]   -- with 'git', moves are done with git mv."""
import os, re, subprocess, sys
root = sys.argv[1]; usegit = len(sys.argv) > 2 and sys.argv[2] == 'git'
SRC = os.path.join(root, 'sources')
OLD, NEW = 'tcrob.umu.quaky2', 'tcrob.umu.soccer'
PKGS = ['gui', 'linda', 'lpo']
CLASSES = ['SoccerLindaRouter', 'SoccerVision', 'SoccerVisionConfig', 'SoccerRefereeSimul', 'SoccerRecognizer']

def mv(a, b):
    os.makedirs(os.path.dirname(b), exist_ok=True)
    if usegit: subprocess.check_call(['git', 'mv', a, b], cwd=root)
    else: os.rename(a, b)

old_dir = os.path.join(SRC, *OLD.split('.')); new_dir = os.path.join(SRC, *NEW.split('.'))
os.makedirs(new_dir, exist_ok=True)
for p in PKGS:
    src = os.path.join(old_dir, p)
    if not os.path.isdir(src): continue
    for dp, dn, fn in os.walk(src):
        for f in fn:
            a = os.path.join(dp, f); b = os.path.join(new_dir, os.path.relpath(a, old_dir))
            mv(a, b)
for c in CLASSES:
    a = os.path.join(old_dir, c + '.java')
    if os.path.exists(a): mv(a, os.path.join(new_dir, c + '.java'))
# empty dirs left behind
for dp, dn, fn in os.walk(old_dir, topdown=False):
    if not os.listdir(dp): os.rmdir(dp)

# names that stay in quaky2
stay = sorted(f[:-5] for f in os.listdir(old_dir) if f.endswith('.java'))
moved_classes = set(CLASSES)
# every class now in soccer (top level), for implicit same-package references from quaky2
soccer_top = sorted(f[:-5] for f in os.listdir(new_dir) if f.endswith('.java'))

rx_pkg = re.compile(r'\btcrob\.umu\.quaky2\.(' + '|'.join(PKGS) + r')\b')
rx_cls = re.compile(r'\btcrob\.umu\.quaky2\.(' + '|'.join(CLASSES) + r')\b')
changed = []
for base in (SRC, os.path.join(root, 'conf')):
    for dp, dn, fn in os.walk(base):
        if '.git' in dp: continue
        for f in fn:
            if not f.endswith(('.java', '.deploy', '.chaos', '.json', '.robot', '.properties', '.xml', '.lua', '.hfsm')): continue
            path = os.path.join(dp, f)
            try: s = open(path, encoding='utf-8').read()
            except UnicodeDecodeError: s = open(path, encoding='latin-1').read()
            t = rx_pkg.sub(lambda m: NEW + '.' + m.group(1), s)
            t = rx_cls.sub(lambda m: NEW + '.' + m.group(1), t)
            if f.endswith('.java'):
                rel = os.path.relpath(path, SRC)
                in_soccer = rel.startswith(os.path.join('tcrob', 'umu', 'soccer') + os.sep) and os.sep not in rel[len('tcrob/umu/soccer/'):]
                in_quaky = rel.startswith(os.path.join('tcrob', 'umu', 'quaky2') + os.sep) and os.sep not in rel[len('tcrob/umu/quaky2/'):]
                if in_soccer and f[:-5] in moved_classes:
                    t = re.sub(r'^package\s+tcrob\.umu\.quaky2\s*;', 'package tcrob.umu.soccer;', t, count=1, flags=re.M)
                # implicit same-package references that are now across packages
                def needs(name, text):
                    return re.search(r'\b' + name + r'\b', re.sub(r'^\s*(package|import)\s.*$', '', text, flags=re.M)) is not None
                def add_import(text, imp):
                    if re.search(r'^import\s+' + re.escape(imp) + r'\s*;', text, flags=re.M): return text
                    m = list(re.finditer(r'^import\s.*;$', text, flags=re.M))
                    if m: pos = m[-1].end(); return text[:pos] + '\nimport ' + imp + ';' + text[pos:]
                    m = re.search(r'^package\s.*;$', text, flags=re.M)
                    return text[:m.end()] + '\n\nimport ' + imp + ';' + text[m.end():]
                if in_quaky:
                    for name in moved_classes:
                        if needs(name, t): t = add_import(t, NEW + '.' + name)
                if in_soccer and f[:-5] in moved_classes:
                    for name in stay:
                        if needs(name, t): t = add_import(t, OLD + '.' + name)
                # a wildcard import of quaky2 that was standing for a moved class: soccer.* beside it
                if re.search(r'^import\s+tcrob\.umu\.quaky2\.\*\s*;', t, flags=re.M) and not in_soccer:
                    if any(needs(name, t) for name in moved_classes): t = add_import(t, NEW + '.*')
                    if not any(needs(name, t) for name in stay):
                        t = re.sub(r'^import\s+tcrob\.umu\.quaky2\.\*\s*;\n', '', t, flags=re.M)
            if t != s:
                open(path, 'w', encoding='utf-8').write(t); changed.append(os.path.relpath(path, root))
print('stay in quaky2:', stay)
print('changed files:', len(changed))
for c in changed: print('  ', c)
