"""webots2kine: the Aibo ERS-7 of Webots (AiboErs7.proto) -> the parts and the kinematic model of ThinkingCapSim.

    python3 webots2kine.py [--proto legacy/other/aibo_ers7.proto] [--parts conf/3dmodels/aibo] [--kine conf/robots/aibo.kine]

Run from the root of the project (the defaults are relative to it). It writes one .3ds per link into the folder of
the parts (the meshes of the proto, in the frame of the link, Z up as the robot) and the .kine file: the joints
where and how the proto has them, each node with "mesh" (its part) and the bounding objects of the proto as the
fallback shapes. The limits and defaults of the joints are kept from the .kine that is there already, when there is
one (they come from the Model Reference Guide, legacy/other/aibo_ers7.txt); otherwise the joints get no limits and
the default of the proto's position.

Kine frames: every link frame is its parent's frame translated to the joint anchor and turned by the joint alone
(no fixed rotation), so a mesh of the Webots solid is brought into it through the forward kinematics at the pose
the proto is written in (its joint positions). Needs numpy; vrml.py (beside this file) reads the proto.
"""
import json, struct, math, sys, os, argparse
import numpy as np
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import vrml

ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
ap.add_argument('--proto', default='legacy/other/aibo_ers7.proto')
ap.add_argument('--parts', default='conf/3dmodels/aibo')
ap.add_argument('--kine', default='conf/robots/aibo.kine')
args = ap.parse_args()
PROTO = args.proto
OUT3DS = args.parts
KINE_IN = args.kine if os.path.isfile(args.kine) else None
KINE_OUT = args.kine

def rot(r):
    if r is None: return np.eye(3)
    x, y, z, a = r; v = np.array([x, y, z], float); v /= np.linalg.norm(v)
    K = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    return np.eye(3) + math.sin(a) * K + (1 - math.cos(a)) * K @ K

def T(t, r):
    M = np.eye(4); M[:3, :3] = rot(r)
    if t is not None: M[:3, 3] = t
    return M

def axis_angle(R):
    a = math.acos(max(-1.0, min(1.0, (np.trace(R) - 1) / 2)))
    if a < 1e-9: return None
    if abs(a - math.pi) < 1e-6:
        # axis from the symmetric part
        w, v = np.linalg.eigh(R)
        ax = v[:, np.argmin(abs(w - 1))]
    else:
        ax = np.array([R[2, 1] - R[1, 2], R[0, 2] - R[2, 0], R[1, 0] - R[0, 1]]) / (2 * math.sin(a))
    ax = ax / np.linalg.norm(ax)
    return [round(float(ax[0]), 6), round(float(ax[1]), 6), round(float(ax[2]), 6), round(a, 6)]

root, defs = vrml.load(PROTO)

def children(n):
    out = []
    for k, v in n.fields.items():
        if isinstance(v, vrml.Node): out.append((k, v))
        elif isinstance(v, list):
            for x in v:
                if isinstance(x, vrml.Node): out.append((k, x))
    return out

# ---- 1. the joints of the proto, in the body frame at the pose it is written in
links = {}      # name -> dict(M, anchor, axis, pos, parent)
order = []
shapes = {}     # link -> [(M_body<-shape, geom, color, bounding)]
sensors = {}    # kine name -> (link, M_body<-sensor)
TOUCH = {'RIGHT_FORELEG_J3': 'RIGHT_FOREPAW_SENSOR', 'LEFT_FORELEG_J3': 'LEFT_FOREPAW_SENSOR',
         'RIGHT_HINDLEG_J3': 'RIGHT_HINDPAW_SENSOR', 'LEFT_HINDLEG_J3': 'LEFT_HINDPAW_SENSOR'}

def color_of(app):
    if app is None: return (1.0, 1.0, 1.0)
    c = app.fields.get('baseColor')
    if c is None: c = (app.fields.get('diffuseColor') if app.type == 'Material' else None)
    if app.type == 'Appearance':
        m = app.fields.get('material'); c = m.fields.get('diffuseColor') if m else None
    return tuple(float(x) for x in c) if c else (1.0, 1.0, 1.0)

def walk(n, M, link, bounding=False):
    if n is None: return
    if n.type == 'HingeJoint':
        name = n.defname
        jp = n.fields.get('jointParameters'); ep = n.fields.get('endPoint')
        anchor = np.array(jp.fields.get('anchor', [0, 0, 0]), float); axis = np.array(jp.fields.get('axis', [0, 0, 1]), float)
        pos = jp.fields.get('position', 0.0) or 0.0
        Ms = M @ T(ep.fields.get('translation'), ep.fields.get('rotation'))
        links[name] = dict(M=Ms, anchor=(M @ np.append(anchor, 1))[:3], axis=M[:3, :3] @ axis, pos=float(pos), parent=link)
        order.append(name)
        for k, c in children(ep):
            if k == 'physics': continue
            walk(c, Ms, name, k == 'boundingObject')
        return
    if n.type == 'Shape':
        g = n.fields.get('geometry'); a = n.fields.get('appearance')
        if g is not None: shapes.setdefault(link, []).append((M, g, color_of(a), bounding))
        return
    if n.type in ('IndexedFaceSet', 'Box', 'Cylinder', 'Sphere', 'Capsule'):
        shapes.setdefault(link, []).append((M, n, (1.0, 1.0, 1.0), bounding))
        return
    if n.type in ('Camera', 'DistanceSensor', 'TouchSensor'):
        Mn = M @ T(n.fields.get('translation'), n.fields.get('rotation'))
        if n.type == 'Camera': sensors['HEAD_CAM'] = (link, Mn)
        elif n.type == 'TouchSensor': sensors[TOUCH[link]] = (link, Mn)
        elif link == 'ERS7': sensors['CHEST_DISTANCE_SENSOR'] = (link, Mn)
        return
    if n.type in ('LED', 'RotationalMotor', 'PositionSensor', 'Physics', 'HingeJointParameters'):
        return
    if n.type in ('Pose', 'Transform', 'Solid', 'Robot', 'Group'):
        Mn = M @ T(n.fields.get('translation'), n.fields.get('rotation')) if n.type != 'Group' else M
        for k, c in children(n):
            if k == 'physics': continue
            walk(c, Mn, link, bounding or k == 'boundingObject')

walk(root, np.eye(4), 'ERS7')

# ---- 2. the kine tree: frames at the proto pose
oldnodes = {}
if KINE_IN:
    old = json.load(open(KINE_IN))
    def collect(n):
        oldnodes[n['name']] = n
        for c in n.get('children', []): collect(c)
    collect(old['root'])

kine = {}   # name -> dict(T (body<-link at proto pose), t, axis, node)
kine['ERS7'] = dict(T=np.eye(4), parent=None)
for name in order:
    L = links[name]; P = kine[L['parent']]
    Rp = P['T'][:3, :3]; op = P['T'][:3, 3]
    t = Rp.T @ (L['anchor'] - op)
    ax = Rp.T @ L['axis']
    ax = np.array([round(float(v), 6) for v in ax]); ax[abs(ax) < 1e-4] = 0.0
    for i in range(3):
        if abs(abs(ax[i]) - 1.0) < 1e-4: ax[i] = math.copysign(1.0, ax[i])
    ax = ax / np.linalg.norm(ax)
    Tl = P['T'] @ T(t, None) @ T(None, [ax[0], ax[1], ax[2], L['pos']])
    kine[name] = dict(T=Tl, t=t, axis=ax, pos=L['pos'], parent=L['parent'])

def local_frame(link, M):
    return np.linalg.inv(kine[link]['T']) @ M

# ---- 3. meshes and fallback primitives per link
def triangles(g):
    coord = g.fields.get('coord'); pts = np.array(coord.fields['point'], float).reshape(-1, 3)
    idx = [int(i) for i in g.fields['coordIndex']]
    ccw = g.fields.get('ccw', True)
    tris = []; poly = []
    for i in idx + [-1]:
        if i < 0:
            if len(poly) >= 3:
                for k in range(1, len(poly) - 1):
                    tris.append((poly[0], poly[k], poly[k + 1]) if ccw is not False else (poly[0], poly[k + 1], poly[k]))
            poly = []
        else: poly.append(i)
    return pts, tris

def write_3ds(path, parts):
    """parts: [(name, verts Nx3 in robot frame, tris, color)]. A .3ds is Z up like the robot (the loader turns it Y up and the scene back): written as is."""
    def chunk(cid, body): return struct.pack('<HI', cid, 6 + len(body)) + body
    def cstr(s): return s.encode('ascii', 'replace') + b'\0'
    mats = {}
    for _, _, _, c in parts:
        key = tuple(round(v, 3) for v in c)
        if key not in mats: mats[key] = 'm%d' % len(mats)
    mat_chunks = b''
    for c, mname in mats.items():
        rgb = bytes(int(round(v * 255)) for v in c)
        amb = bytes(int(round(v * 255 * 0.5)) for v in c)
        body = chunk(0xA000, cstr(mname)) + chunk(0xA010, chunk(0x0011, amb)) + chunk(0xA020, chunk(0x0011, rgb)) \
             + chunk(0xA030, chunk(0x0011, bytes([60, 60, 60]))) + chunk(0xA040, chunk(0x0030, struct.pack('<H', 20)))
        mat_chunks += chunk(0xAFFF, body)
    objs = b''
    for n, (name, verts, tris, c) in enumerate(parts):
        v3 = np.asarray(verts, dtype='<f4')
        pa = struct.pack('<H', len(v3)) + v3.tobytes()
        fa = struct.pack('<H', len(tris)) + b''.join(struct.pack('<HHHH', a, b, cc, 7) for a, b, cc in tris)
        mg = chunk(0x4130, cstr(mats[tuple(round(v, 3) for v in c)]) + struct.pack('<H', len(tris)) + struct.pack('<%dH' % len(tris), *range(len(tris))))
        sm = chunk(0x4150, struct.pack('<%dI' % len(tris), *([1] * len(tris))))
        mtx = chunk(0x4160, struct.pack('<12f', 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0))
        tri = chunk(0x4100, chunk(0x4110, pa) + chunk(0x4120, fa + mg + sm) + mtx)
        objs += chunk(0x4000, cstr(('%s_%d' % (name, n))[:10]) + tri)
    mdata = chunk(0x3D3D, chunk(0x0100, struct.pack('<f', 1.0)) + mat_chunks + objs)
    data = chunk(0x4D4D, chunk(0x0002, struct.pack('<I', 3)) + mdata)
    open(path, 'wb').write(data)

FILE = {'ERS7': 'ers7_body', 'NECK_TILT': 'ers7_neck', 'HEAD_PAN': 'ers7_head_base', 'HEAD_TILT': 'ers7_head', 'JAW': 'ers7_jaw',
        'LEFT_EAR': 'ers7_ear_left', 'RIGHT_EAR': 'ers7_ear_right', 'TAIL_TILT': 'ers7_tail_base', 'TAIL_PAN': 'ers7_tail',
        'LEFT_FORELEG_J1': 'ers7_shoulder_lf', 'LEFT_FORELEG_J2': 'ers7_upperleg_lf', 'LEFT_FORELEG_J3': 'ers7_lowerleg_lf',
        'RIGHT_FORELEG_J1': 'ers7_shoulder_rf', 'RIGHT_FORELEG_J2': 'ers7_upperleg_rf', 'RIGHT_FORELEG_J3': 'ers7_lowerleg_rf',
        'LEFT_HINDLEG_J1': 'ers7_shoulder_lh', 'LEFT_HINDLEG_J2': 'ers7_upperleg_lh', 'LEFT_HINDLEG_J3': 'ers7_lowerleg_lh',
        'RIGHT_HINDLEG_J1': 'ers7_shoulder_rh', 'RIGHT_HINDLEG_J2': 'ers7_upperleg_rh', 'RIGHT_HINDLEG_J3': 'ers7_lowerleg_rh'}

os.makedirs(OUT3DS, exist_ok=True)
meshfile = {}; fallback = {}
for link in ['ERS7'] + order:
    parts = []; prims = []
    for M, g, c, bounding in shapes.get(link, []):
        F = local_frame(link, M)
        if g.type == 'IndexedFaceSet' and not bounding:
            pts, tris = triangles(g)
            v = (F[:3, :3] @ pts.T).T + F[:3, 3]
            parts.append((link, v, tris, c))
        elif g.type in ('Box', 'Cylinder', 'Sphere', 'Capsule'):
            s = {'translation': [round(float(x), 5) for x in F[:3, 3]]}
            aa = axis_angle(F[:3, :3])
            if aa: s['rotation'] = aa
            if g.type == 'Box': s['type'] = 'box'; s['size'] = [float(x) for x in g.fields['size']]
            elif g.type == 'Sphere': s['type'] = 'sphere'; s['radius'] = float(g.fields.get('radius', 1.0))
            else:
                s['type'] = g.type.lower(); s['radius'] = float(g.fields.get('radius', 1.0)); s['height'] = float(g.fields.get('height', 2.0)); s['axis'] = 'y'
            prims.append(s)
    if parts:
        fn = FILE[link] + '.3ds'
        write_3ds(os.path.join(OUT3DS, fn), parts)
        meshfile[link] = fn
        nv = sum(len(p[1]) for p in parts); nt = sum(len(p[2]) for p in parts)
        print(f'{fn:24s} {len(parts)} shapes, {nv} vertices, {nt} triangles')
    fallback[link] = prims

# ---- 4. the kine file
def num(v): return round(float(v), 6)
def fmt(v): return [num(x) for x in v]

# the dominant colour of a link's mesh for its fallback solids
def link_color(link):
    best = None; area = -1
    for M, g, c, bounding in shapes.get(link, []):
        if g.type == 'IndexedFaceSet' and not bounding:
            n = len(g.fields['coord'].fields['point'])
            if n > area: area = n; best = c
    return [round(x, 3) for x in best] if best else [0.9, 0.9, 0.9]

def node_json(name):
    n = {'name': name}
    if name == 'ERS7': n['translation'] = [0.0, 0.0, 0.0]
    else:
        k = kine[name]; n['translation'] = [0.0 if abs(x) < 1e-6 else num(x) for x in k['t']]
        oj = oldnodes.get(name, {}).get('joint', {})
        j = {'type': 'revolute', 'axis': [int(a) if float(a).is_integer() else num(a) for a in k['axis']]}
        if 'min' in oj: j['min'] = oj['min']
        if 'max' in oj: j['max'] = oj['max']
        j['def'] = oj.get('def', round(k['pos'], 6))
        n['joint'] = j
    if name in meshfile: n['mesh'] = meshfile[name]
    col = link_color(name)
    sh = []
    for p in fallback.get(name, []):
        q = dict(p); q['color'] = col; sh.append(q)
    if sh: n['shapes'] = sh
    ch = [node_json(c) for c in order if links[c]['parent'] == name]
    # sensors of this link
    for sname, (link, Ms) in sensors.items():
        if link != name: continue
        F = local_frame(link, Ms)
        s = {'name': sname, 'translation': [round(float(x), 5) for x in F[:3, 3]]}
        aa = axis_angle(F[:3, :3])
        if aa and (sname == 'HEAD_CAM') and (aa[3] > 1e-4): s['rotation'] = aa
        ch.append(s)
    if ch: n['children'] = ch
    return n

model = {'name': 'aibo',
         'description': 'Sony AIBO ERS-7 from the Webots model (AiboErs7.proto, Cyberbotics): the joints where the proto has them, '
                        'its meshes as one .3ds per link (mesh: the file, in the folder of the robot parts, Z up in the frame of the link) and its bounding solids '
                        'as the fallback drawing. Frame x forward, y left, z up; m and rad. Every link frame is its parent\'s, '
                        'translated to the joint and turned by the joint alone. Joint limits and defaults from the Model Reference '
                        'Guide. Legs: a positive J1 swings a foreleg forward and a hind leg back; a positive J3 folds the forearm '
                        'forward (elbow back) and the hind shank back (knee forward). Neck: 0 is vertical, negative leans forward; '
                        'head tilt: positive looks up, and +30 deg on a -30 deg neck is level.',
         'root': node_json('ERS7')}

def dumps(o, ind=0):
    pad = '  ' * ind
    if isinstance(o, dict):
        items = []
        for k, v in o.items():
            items.append(f'{pad}  "{k}": {dumps(v, ind + 1)}')
        return '{\n' + ',\n'.join(items) + f'\n{pad}}}'
    if isinstance(o, list):
        if all(isinstance(x, (int, float)) for x in o):
            return '[' + ', '.join(json.dumps(x) for x in o) + ']'
        return '[\n' + ',\n'.join(f'{pad}  ' + dumps(x, ind + 1) for x in o) + f'\n{pad}]'
    return json.dumps(o)

open(KINE_OUT, 'w').write(dumps(model) + '\n')
print('kine written')
for name in ['NECK_TILT', 'HEAD_PAN', 'HEAD_TILT', 'JAW', 'LEFT_EAR', 'LEFT_FORELEG_J1', 'LEFT_FORELEG_J2', 'LEFT_FORELEG_J3', 'LEFT_HINDLEG_J3', 'TAIL_TILT', 'TAIL_PAN']:
    k = kine[name]; print(f'{name:18s} t={np.round(k["t"],4)} axis={k["axis"]} pos={k["pos"]:.4f}')
for s, (l, M) in sensors.items():
    F = local_frame(l, M); print(f'{s:24s} in {l}: t={np.round(F[:3,3],4)} rot={axis_angle(F[:3,:3])}')
