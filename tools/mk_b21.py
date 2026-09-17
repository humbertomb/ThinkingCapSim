#!/usr/bin/env python3
"""
Build a .3ds model of the RWI B21 from its published dimensions (CYCOGS sheet,
inches): 2" ground clearance, 12" base of 21" diameter, the lidar ring up to
21", a 29" upper enclosure of 22" diameter, 58" overall to the top of the
cameras.  Four 4.5" wheels of the synchro drive.

Z is up and the model rests on z = 0, centred on the turning axis, in metres:
that is what tc's loader expects (checked against pioneer3.3ds and quaky2.3ds).
"""

import math, struct, sys

IN = 0.0254                                     # inch -> metre


# ---------------------------------------------------------------- geometry

def prism (n, r, z0, z1, phase = 0.0, caps = True):
    """A regular n-sided prism about the z axis, r the circumradius."""
    vs = []
    fs = []
    for i in range (n):
        a = phase + 2.0 * math.pi * i / n
        vs.append ((r * math.cos (a), r * math.sin (a), z0))
        vs.append ((r * math.cos (a), r * math.sin (a), z1))
    for i in range (n):
        b0, t0 = 2 * i, 2 * i + 1
        b1, t1 = 2 * ((i + 1) % n), 2 * ((i + 1) % n) + 1
        fs.append ((b0, b1, t1))
        fs.append ((b0, t1, t0))
    if caps:
        cb = len (vs);		vs.append ((0.0, 0.0, z0))
        ct = len (vs);		vs.append ((0.0, 0.0, z1))
        for i in range (n):
            fs.append ((cb, 2 * ((i + 1) % n), 2 * i))
            fs.append ((ct, 2 * i + 1, 2 * ((i + 1) % n) + 1))
    return vs, fs


def wheel (n, r, width, cx, cy, ang):
    """A wheel of radius r rolling about a horizontal axle, at (cx, cy)."""
    vs = []
    fs = []
    ax, ay = -math.sin (ang), math.cos (ang)    # the axle, across the rolling plane
    dx, dy = math.cos (ang), math.sin (ang)     # the way it rolls
    for side in (-1.0, 1.0):
        ox = cx + ax * width * 0.5 * side
        oy = cy + ay * width * 0.5 * side
        for i in range (n):
            a = -0.5 * math.pi + 2.0 * math.pi * i / n
            vs.append ((ox + dx * r * math.cos (a), oy + dy * r * math.cos (a), r + r * math.sin (a)))
    for i in range (n):
        a0, a1 = i, (i + 1) % n
        b0, b1 = n + i, n + (i + 1) % n
        fs.append ((a0, b1, a1))
        fs.append ((a0, b0, b1))
    for side in (0, n):
        c = len (vs);	vs.append ((cx + ax * width * 0.5 * (1 if side else -1),
                                    cy + ay * width * 0.5 * (1 if side else -1), r))
        for i in range (n):
            fs.append ((c, side + (i + 1) % n, side + i) if side else (c, side + i, side + (i + 1) % n))
    return vs, fs


def merge (*parts):
    vs = []
    fs = []
    for pv, pf in parts:
        o = len (vs)
        vs += pv
        fs += [(a + o, b + o, c + o) for a, b, c in pf]
    return vs, fs


# ------------------------------------------------------------------- 3ds

def chunk (cid, body):
    return struct.pack ('<HI', cid, 6 + len (body)) + body


def asciiz (s):
    return s.encode ('latin1') + b'\0'


def colour (cid, rgb):
    return chunk (cid, chunk (0x0011, bytes (rgb)))


def percent (cid, pc):
    return chunk (cid, chunk (0x0030, struct.pack ('<H', pc)))


def material (name, diffuse, shine = 25):
    dim = tuple (int (c * 0.35) for c in diffuse)
    lit = tuple (min (255, int (c * 1.25)) for c in diffuse)
    body = chunk (0xA000, asciiz (name))
    body += colour (0xA010, dim)                    # ambient
    body += colour (0xA020, diffuse)                # diffuse
    body += colour (0xA030, lit)                    # specular
    body += percent (0xA040, shine)                 # shininess
    body += percent (0xA041, 20)                    # shine strength
    body += percent (0xA050, 0)                     # transparency
    body += chunk (0xA100, struct.pack ('<H', 3))   # shading: phong
    return chunk (0xAFFF, body)


def mesh (name, vs, fs, mat):
    pts = struct.pack ('<H', len (vs))
    for x, y, z in vs:
        pts += struct.pack ('<fff', x, y, z)

    fac = struct.pack ('<H', len (fs))
    for a, b, c in fs:
        fac += struct.pack ('<HHHH', a, b, c, 7)
    grp = asciiz (mat) + struct.pack ('<H', len (fs))
    for i in range (len (fs)):
        grp += struct.pack ('<H', i)
    fac += chunk (0x4130, grp)

    axes = struct.pack ('<9f', 1, 0, 0, 0, 1, 0, 0, 0, 1) + struct.pack ('<3f', 0, 0, 0)

    tri = chunk (0x4110, pts) + chunk (0x4160, axes) + chunk (0x4120, fac)
    return chunk (0x4000, asciiz (name) + chunk (0x4100, tri))


def write (path, mats, meshes):
    mdata = chunk (0x3D3E, struct.pack ('<I', 3))
    mdata += colour (0x2100, (26, 26, 26))          # ambient light
    for m in mats:
        mdata += m
    for m in meshes:
        mdata += m
    out = chunk (0x4D4D, chunk (0x0002, struct.pack ('<I', 3)) + chunk (0x3D3D, mdata))
    open (path, 'wb').write (out)
    return len (out)


# ------------------------------------------------------------------ model

BODY = 'b21 body'
DARK = 'b21 dark'
TYRE = 'b21 tyre'

CLEAR = 2.0 * IN                                # ground clearance
BASE_T = 12.0 * IN                              # top of the base
LIDAR_T = 21.0 * IN                             # top of the lidar ring
ENCL_T = 50.0 * IN                              # top of the upper enclosure (29" of it)
TOP = 58.0 * IN                                 # top of the cameras

R_BASE = 10.5 * IN                              # 21" diameter
R_ENCL = 11.0 * IN                              # 22" diameter
R_LIDAR = 9.75 * IN                             # the ring is set in a little
R_WHEEL = 2.25 * IN                             # 4.5" wheels
W_WHEEL = 1.6 * IN
D_WHEEL = 7.0 * IN                              # from the axis to the wheel

def build (path):
    meshes = []

    # the base: eight panels, four sonar, four infrared and four bumpers on them
    meshes.append (mesh ('base', *prism (8, R_BASE, CLEAR, BASE_T, math.pi / 8), mat = BODY))
    # the skirt under it, so that the wheels sit in a shadow rather than in the air
    meshes.append (mesh ('skirt', *prism (8, R_BASE * 0.92, 0.5 * CLEAR, CLEAR, math.pi / 8), mat = DARK))
    # the ring the Sick lidar looks out of
    meshes.append (mesh ('lidar', *prism (24, R_LIDAR, BASE_T, LIDAR_T), mat = DARK))
    meshes.append (mesh ('lidar rim', *prism (24, R_BASE, BASE_T, BASE_T + 0.6 * IN), mat = BODY))
    # the upper enclosure: six door panels
    meshes.append (mesh ('enclosure', *prism (24, R_ENCL, LIDAR_T + 1.0 * IN, ENCL_T), mat = BODY))
    meshes.append (mesh ('collar', *prism (24, R_ENCL, LIDAR_T, LIDAR_T + 1.0 * IN), mat = DARK))
    # the lid and the pan and tilt with its two cameras
    meshes.append (mesh ('lid', *prism (24, R_ENCL * 0.80, ENCL_T, ENCL_T + 1.5 * IN), mat = BODY))
    meshes.append (mesh ('mast', *prism (12, 1.6 * IN, ENCL_T + 1.5 * IN, TOP - 3.0 * IN), mat = DARK))
    for i, dy in enumerate ((-2.6 * IN, 2.6 * IN)):
        meshes.append (mesh ('camera %d' % (i + 1),
                             *shift (prism (10, 1.0 * IN, TOP - 3.5 * IN, TOP), 0.0, dy), mat = DARK))

    # the four wheels of the synchro drive, all pointing the same way
    ws = []
    for i in range (4):
        a = math.pi / 4 + i * math.pi / 2
        ws.append (wheel (16, R_WHEEL, W_WHEEL, D_WHEEL * math.cos (a), D_WHEEL * math.sin (a), 0.0))
    meshes.append (mesh ('wheels', *merge (*ws), mat = TYRE))

    mats = [material (BODY, (176, 178, 182)),       # bare aluminium panels
            material (DARK, (72, 74, 78)),
            material (TYRE, (38, 38, 40), shine = 8)]
    return write (path, mats, meshes)


def shift (part, dx, dy):
    vs, fs = part
    return [(x + dx, y + dy, z) for x, y, z in vs], fs


if __name__ == '__main__':
    path = sys.argv[1] if len (sys.argv) > 1 else 'b21.3ds'
    n = build (path)
    print ('%s: %d bytes' % (path, n))
