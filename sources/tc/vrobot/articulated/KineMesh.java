/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot.articulated;

import java.util.ArrayList;
import java.util.List;

/**
 * The solids of an articulated robot as faces, in the frame of the robot
 * with the joints as they are now: what a drawing that is not Java 3D (the
 * views of the robot editor, an outline) works from, the way it works from
 * the faces of a 3D Studio model. Each face is its corners, {x, y, z, x, y,
 * z, ...}, three for a triangle and four for a quad, in metres. The robot is
 * given with its body at the origin; {@link #faces(KineModel, double)} lifts
 * it so that its feet are on the floor (z = 0), as the models of the robots
 * stand.
 */
public class KineMesh
{
	/** How many sides the round solids are drawn with. */
	static public final int			SIDES		= 16;
	static public final int			RINGS		= 8;

	/** The faces of the model with the joints as they are, the body at the origin. */
	static public List<double[]> faces (KineModel model)
	{
		return faces (model, 0.0);
	}

	/** The same, the whole robot lifted so much (m): its height over the floor, to stand it on z = 0. */
	static public List<double[]> faces (KineModel model, double lift)
	{
		List<double[]>	out = new ArrayList<double[]> ();

		if (model == null)				return out;
		model.forward ();
		for (KineNode n : model.nodes ())
		{
			if (n.world () == null)		continue;
			for (KineShape s : n.shapes)
			{
				Frame	f = n.world ().times (s.frame ());

				if (KineShape.BOX.equals (s.type) && (s.size != null) && (s.size.length >= 3))
					box (f, s.size[0] / 2.0, s.size[1] / 2.0, s.size[2] / 2.0, lift, out);
				else if (KineShape.SPHERE.equals (s.type))
					sphere (f, s.radius, 0.0, 0.0, lift, out);
				else if (KineShape.CYLINDER.equals (s.type))
					cylinder (f, s.radius, s.height, s.along (), false, lift, out);
				else if (KineShape.CAPSULE.equals (s.type))
					cylinder (f, s.radius, s.height, s.along (), true, lift, out);
			}
		}
		return out;
	}

	/* ------------------------------------------------------------------ */

	static private void add (Frame f, double lift, List<double[]> out, double[]... corners)
	{
		double[]	face = new double[3 * corners.length];

		for (int i = 0; i < corners.length; i++)
		{
			double[]	p = f.apply (corners[i][0], corners[i][1], corners[i][2]);

			face[3 * i]		= p[0];
			face[3 * i + 1]	= p[1];
			face[3 * i + 2]	= p[2] + lift;
		}
		out.add (face);
	}

	static private void box (Frame f, double hx, double hy, double hz, double lift, List<double[]> out)
	{
		double[][]	c = { { -hx, -hy, -hz }, { hx, -hy, -hz }, { hx, hy, -hz }, { -hx, hy, -hz },
						  { -hx, -hy,  hz }, { hx, -hy,  hz }, { hx, hy,  hz }, { -hx, hy,  hz } };

		add (f, lift, out, c[0], c[1], c[2], c[3]);
		add (f, lift, out, c[4], c[5], c[6], c[7]);
		add (f, lift, out, c[0], c[1], c[5], c[4]);
		add (f, lift, out, c[2], c[3], c[7], c[6]);
		add (f, lift, out, c[1], c[2], c[6], c[5]);
		add (f, lift, out, c[3], c[0], c[4], c[7]);
	}

	/** A point of a circle of radius r at angle a in the plane across the axis, at h along it. */
	static private double[] ring (char along, double r, double a, double h)
	{
		double	u = r * Math.cos (a), v = r * Math.sin (a);

		switch (along)
		{
		case 'x':	return new double[] { h, u, v };
		case 'y':	return new double[] { v, h, u };
		default:	return new double[] { u, v, h };
		}
	}

	static private void cylinder (Frame f, double r, double h, char along, boolean capsule, double lift, List<double[]> out)
	{
		double	half = h / 2.0;

		for (int i = 0; i < SIDES; i++)
		{
			double	a0 = 2 * Math.PI * i / SIDES, a1 = 2 * Math.PI * (i + 1) / SIDES;

			add (f, lift, out, ring (along, r, a0, -half), ring (along, r, a1, -half), ring (along, r, a1, half), ring (along, r, a0, half));
			if (!capsule)												// the lids, as fans from the centre
			{
				add (f, lift, out, ring (along, 0.0, 0.0, half), ring (along, r, a0, half), ring (along, r, a1, half));
				add (f, lift, out, ring (along, 0.0, 0.0, -half), ring (along, r, a1, -half), ring (along, r, a0, -half));
			}
		}
		if (capsule)
		{
			double[]	top = ring (along, 0.0, 0.0, half), bottom = ring (along, 0.0, 0.0, -half);

			sphere (f, r, top[0], top[1], lift, out, top[2], along, 1);
			sphere (f, r, bottom[0], bottom[1], lift, out, bottom[2], along, -1);
		}
	}

	static private void sphere (Frame f, double r, double cx, double cy, double lift, List<double[]> out)
	{
		sphere (f, r, cx, cy, lift, out, 0.0, 'z', 0);
	}

	/**
	 * A sphere (or, with a side, the half of it on that side of the axis it is
	 * drawn along) centred at (cx, cy, cz): rings of quads, with triangles at the
	 * poles.
	 */
	static private void sphere (Frame f, double r, double cx, double cy, double lift, List<double[]> out, double cz, char along, int side)
	{
		int		from = (side > 0) ? RINGS / 2 : 0, to = (side < 0) ? RINGS / 2 : RINGS;

		for (int j = from; j < to; j++)
		{
			double	b0 = -Math.PI / 2.0 + Math.PI * j / RINGS, b1 = -Math.PI / 2.0 + Math.PI * (j + 1) / RINGS;

			for (int i = 0; i < SIDES; i++)
			{
				double		a0 = 2 * Math.PI * i / SIDES, a1 = 2 * Math.PI * (i + 1) / SIDES;
				double[]	p00 = on (along, r, a0, b0, cx, cy, cz), p10 = on (along, r, a1, b0, cx, cy, cz);
				double[]	p11 = on (along, r, a1, b1, cx, cy, cz), p01 = on (along, r, a0, b1, cx, cy, cz);

				if (j == 0)						add (f, lift, out, p00, p10, p11);
				else if (j == RINGS - 1)		add (f, lift, out, p00, p10, p01);
				else							add (f, lift, out, p00, p10, p11, p01);
			}
		}
	}

	/** A point of a sphere: longitude a round the axis, latitude b from the plane across it. */
	static private double[] on (char along, double r, double a, double b, double cx, double cy, double cz)
	{
		double[]	p = ring (along, r * Math.cos (b), a, r * Math.sin (b));

		return new double[] { p[0] + cx, p[1] + cy, p[2] + cz };
	}
}
