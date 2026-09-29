# Webots → ThinkingCapSim

`webots2kine.py` reads the Aibo ERS-7 of Webots (`legacy/other/aibo_ers7.proto`, Cyberbotics) and writes
what the simulator draws it with:

- `conf/3dmodels/aibo/*.3ds`: a part per link (body, neck, head, jaw, ears, tail and the three segments of
  every leg), the meshes of the proto brought into the frame of each link of the kinematic model, Z up as
  the robot, with the colours of the proto.
- `conf/robots/aibo.kine`: the joints where and how the proto has them, every node with `mesh` (its part,
  a file name in the folder of the robot parts) and the bounding objects of the proto as the fallback
  shapes. The limits and defaults of the joints are kept from the `.kine` already there (they come from
  the Model Reference Guide, `legacy/other/aibo_ers7.txt`).

Run it from the root of the project (needs Python 3 and numpy):

    python3 legacy/webots/webots2kine.py

`vrml.py` is the small reader of the VRML of the proto it uses.
