#!/usr/bin/env python3
"""
Plan-view icon of the RWI B21 for the simulator: 500x500, transparent outside
the platform, the robot facing +x (the right of the image), which is what a
plan view takes the width of the image for.

What is drawn is the platform seen from above: the 22" upper enclosure and the
seams of its six door panels, the ring of the base showing past it with its
sonar and infrared, the lid, and the pan and tilt with its two cameras looking
forward.  The front panel is marked so that the heading can be read at a
glance.

Everything is drawn at SS times the size and scaled down, which is what gives
the edges; the light from the upper left and the shade at the rim are what
keeps it from reading flat.
"""

import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

SIZE = 500
SS = 4                                          # supersampling
N = SIZE * SS
C = N / 2.0
R = N * 0.492                                   # the enclosure fills the frame

BODY = (176, 178, 182, 255)                     # bare aluminium
EDGE = (60, 62, 66, 255)
SEAM = (126, 128, 133, 255)
LID = (191, 193, 197, 255)
LID_EDGE = (146, 148, 153, 255)
BOLT = (154, 156, 161, 255)
DARK = (78, 80, 84, 255)
DARKER = (46, 48, 52, 255)
SONAR = (108, 110, 115, 255)
IR = (64, 66, 70, 255)
GLASS = (104, 138, 162, 255)
FRONT = (196, 122, 58, 255)                     # the front panel


def disc (d, cx, cy, r, fill = None, outline = None, width = 0):
    d.ellipse ([cx - r, cy - r, cx + r, cy + r], fill = fill, outline = outline, width = width)


def at (r, a):
    return C + r * math.cos (a), C + r * math.sin (a)


def build ():
    im = Image.new ("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw (im)
    inner = R * 0.775                           # where the lid begins

    # the enclosure
    disc (d, C, C, R, BODY)

    # the front panel, as a slice of the ring
    d.pieslice ([C - R, C - R, C + R, C + R], -16, 16, fill = FRONT)
    disc (d, C, C, inner, BODY)

    # the seams between the six door panels
    for i in range (6):
        a = math.pi / 6 + 2.0 * math.pi * i / 6
        d.line ([*at (inner * 0.99, a), *at (R * 0.995, a)], fill = SEAM, width = int (N * 0.0055))

    # what the ring of the base carries: 16 sonar, and 16 infrared between them
    for i in range (16):
        a = 2.0 * math.pi * i / 16
        x, y = at (R * 0.885, a)
        s = N * 0.0135
        d.ellipse ([x - s, y - s, x + s, y + s], fill = SONAR)
        x, y = at (R * 0.885, a + math.pi / 16)
        s = N * 0.0075
        d.ellipse ([x - s, y - s, x + s, y + s], fill = IR)

    # the lid, and the bolts that hold it down
    disc (d, C, C, inner, LID)
    disc (d, C, C, inner, outline = LID_EDGE, width = int (N * 0.0045))
    for i in range (8):
        a = math.pi / 8 + 2.0 * math.pi * i / 8
        x, y = at (inner * 0.88, a)
        s = N * 0.0075
        d.ellipse ([x - s, y - s, x + s, y + s], fill = BOLT)

    # the pan and tilt: its base, and the head with the two cameras looking forward
    disc (d, C, C, R * 0.33, DARK)
    disc (d, C, C, R * 0.33, outline = DARKER, width = int (N * 0.0045))
    disc (d, C, C, R * 0.13, DARKER)
    d.rounded_rectangle ([C - R * 0.15, C - R * 0.19, C + R * 0.27, C + R * 0.19],
                         radius = R * 0.05, fill = DARK, outline = DARKER, width = int (N * 0.0045))
    for s in (-1, 1):
        cy = C + s * R * 0.095
        disc (d, C + R * 0.20, cy, R * 0.058, DARKER)
        disc (d, C + R * 0.20, cy, R * 0.040, GLASS)         # the lens
    # the two yokes the head turns in
    d.rectangle ([C - R * 0.03, C - R * 0.30, C + R * 0.03, C - R * 0.19], fill = DARKER)
    d.rectangle ([C - R * 0.03, C + R * 0.19, C + R * 0.03, C + R * 0.30], fill = DARKER)

    # the rim of the enclosure, over everything, so that it closes the shape
    disc (d, C, C, R, outline = EDGE, width = int (N * 0.007))

    # the light from the upper left and the shade at the rim
    yy, xx = np.mgrid[0:N, 0:N]
    nx = (xx - C) / R
    ny = (yy - C) / R
    rr = np.hypot (nx, ny)
    lit = np.clip (0.55 - 0.40 * (nx + ny), 0.0, 1.0) ** 1.4
    shade = np.clip ((rr - 0.70) / 0.32, 0.0, 1.0) ** 1.6

    body = np.array (im.split ()[3], dtype = np.float32) / 255.0
    white = Image.new ("RGBA", (N, N), (255, 255, 255, 255))
    white.putalpha (Image.fromarray ((lit * body * 70).astype (np.uint8)))
    black = Image.new ("RGBA", (N, N), (0, 0, 0, 255))
    black.putalpha (Image.fromarray ((shade * body * 90).astype (np.uint8)))
    im = Image.alpha_composite (Image.alpha_composite (im, white), black)

    return im.resize ((SIZE, SIZE), Image.LANCZOS)


if __name__ == "__main__":
    import sys
    out = sys.argv[1] if len (sys.argv) > 1 else "b21.png"
    build ().save (out)
    print (out)
