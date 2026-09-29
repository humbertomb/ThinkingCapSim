"""A small VRML/Webots PROTO reader: tokens -> Node(type, fields, defname), lists, numbers, strings."""
import re, sys

class Node:
    def __init__(self, type_, defname=None):
        self.type = type_; self.defname = defname; self.fields = {}
    def __repr__(self): return f"{self.type}({self.defname})"

TOKEN = re.compile(r'''\s*(?:(#[^\n]*)|("(?:[^"\\]|\\.)*")|([\[\]{}])|([^\s\[\]{}"]+))''', re.S)

def tokenize(text):
    pos = 0; n = len(text); out = []
    while pos < n:
        m = TOKEN.match(text, pos)
        if not m or m.end() == pos: break
        pos = m.end()
        c, s, b, w = m.groups()
        if c: continue
        if s: out.append(('str', s[1:-1]))
        elif b: out.append(('br', b))
        else:
            w = w.strip(',')
            if w: out.append(('w', w))
    return out

class Parser:
    def __init__(self, toks): self.t = toks; self.i = 0; self.defs = {}
    def peek(self): return self.t[self.i] if self.i < len(self.t) else (None, None)
    def next(self): tok = self.t[self.i]; self.i += 1; return tok
    def parse_value(self):
        k, v = self.peek()
        if k == 'br' and v == '[':
            self.next(); items = []
            while self.peek()[1] != ']':
                items.append(self.parse_value())
            self.next(); return items
        if k == 'str': self.next(); return v
        if k == 'w':
            if v == 'DEF':
                self.next(); name = self.next()[1]; type_ = self.next()[1]; node = self.parse_node(type_, name); self.defs[name] = node; return node
            if v == 'USE':
                self.next(); name = self.next()[1]; return self.defs.get(name)
            if v == 'IS':
                self.next(); self.next(); return None
            if v in ('TRUE', 'FALSE'): self.next(); return v == 'TRUE'
            if v == 'NULL': self.next(); return None
            try:
                f = float(v); self.next(); return f
            except ValueError:
                pass
            # node
            self.next(); return self.parse_node(v)
        raise ValueError(f"unexpected {k} {v} at {self.i}")
    def parse_node(self, type_, defname=None):
        node = Node(type_, defname)
        k, v = self.next()
        assert v == '{', (type_, k, v, self.i)
        while self.peek()[1] != '}':
            fk, fname = self.next()
            # value: could be a list of numbers, node, IS ...
            k2, v2 = self.peek()
            if k2 == 'w' and v2 == 'IS':
                self.next(); self.next(); node.fields[fname] = None; continue
            vals = [self.parse_value()]
            # numbers may continue (SFVec3f etc.)
            while self.peek()[0] == 'w' and self._isnum(self.peek()[1]):
                vals.append(self.parse_value())
            node.fields[fname] = vals[0] if len(vals) == 1 else vals
        self.next()
        return node
    @staticmethod
    def _isnum(s):
        try: float(s); return True
        except ValueError: return False

def load(path):
    text = open(path, encoding='utf-8', errors='replace').read()
    # skip up to the body '{' after the PROTO header ']'
    i = text.index(']\n{')
    body = text[i+2:]
    toks = tokenize(body)
    p = Parser(toks)
    assert p.next()[1] == '{'
    root = p.parse_value()
    return root, p.defs
