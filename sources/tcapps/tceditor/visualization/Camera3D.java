/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor.visualization;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.*;
import javax.vecmath.*;

import tc.vrobot.*;
import wucore.utils.geom.*;

/**
 * What the cameras of a simulated robot see, drawn in the 3D world: for each
 * camera, the pyramid from where it sits out to its range max, as wide and as
 * tall as its two fields of view, with its apex on the camera -- the one the
 * robot editor draws, but following the robot as it moves and the camera as it
 * is turned on its mount (pan, tilt: see {@link #move}).
 *
 * The pyramid is built in the frame of the camera (apex at the origin, looking
 * down +x, y to its left, z up) and taken to the world with the pose of the
 * robot, then where the camera sits on it, then its orientation and pan about
 * the vertical, then its elevation and tilt about the horizontal it is left
 * with -- the same order the simulator renders the frames of the camera in.
 * There it is cut to what can be seen: nothing below the floor, and nothing
 * beyond the outer walls of the world when they are known ({@link #setBounds}),
 * so a camera looking down shows the patch of floor it sees and one looking at
 * a wall stops at it. The cut is made again every time it moves.
 */
public class Camera3D extends BranchGroup
{
	/** How far a camera is drawn to when its description says no range (m). */
	static public final double		DEF_RANGE	= 1.0;
	/** How see-through the pyramid is. */
	static public final float		ALPHA		= 0.7f;
	/** The base of the pyramid, a shade darker than its faces. */
	static public final float		BASE_SHADE	= 0.75f;
	/** Where the floor is (m): nothing is drawn below it. */
	static public final double		FLOOR		= 0.0;

	protected RobotDesc				rdesc;
	protected Point3d[][]			pyr;						// the corners of each pyramid in its camera's frame (apex first); null where it cannot be drawn
	protected Shape3D[]				faces;						// the faces of each pyramid and the caps the cuts leave, in the world
	protected Shape3D[]				bases;						// what it sees at its range max, a shade darker
	protected double[]				pan, tilt;					// how each camera is turned now (rad)
	protected double[]				bounds;						// the outer walls: xmin, ymin, xmax, ymax (null: no walls to stop at)

	private Transform3D				t		= new Transform3D ();
	private Transform3D				step	= new Transform3D ();

	public Camera3D (RobotDesc rdesc, Point3 pt, double a)
	{
		this.rdesc	= rdesc;

		setCapability (BranchGroup.ALLOW_DETACH);
		setCapability (BranchGroup.ALLOW_CHILDREN_EXTEND);
		setCapability (BranchGroup.ALLOW_CHILDREN_WRITE);
		setCapability (BranchGroup.ALLOW_CHILDREN_READ);

		int		n = ((rdesc != null) && (rdesc.camfeat != null)) ? rdesc.camfeat.length : 0;

		pyr		= new Point3d[n][];
		faces	= new Shape3D[n];
		bases	= new Shape3D[n];
		pan		= new double[n];
		tilt	= new double[n];
		for (int i = 0; i < n; i++)
		{
			pyr[i]	= pyramid (i);
			if (pyr[i] == null)			continue;
			faces[i]	= shape (1.0f);
			bases[i]	= shape (BASE_SHADE);
			addChild (faces[i]);
			addChild (bases[i]);
		}
		move (pt, a, null, null);
	}

	/** How many cameras there are to draw (some may not be drawable). */
	public int count ()										{ return pyr.length; }

	/**
	 * The outer walls of the world the pyramids are cut at (xmin, ymin, xmax,
	 * ymax, m); null for none. They are cut at the floor whatever this says.
	 */
	public void setBounds (double[] b)
	{
		bounds	= ((b != null) && (b.length >= 4)) ? b.clone () : null;
	}

	/**
	 * Follows the robot to a pose, with each camera turned as it is now: pan to the
	 * left and tilt upwards from where its description points it (rad); null, or a
	 * camera beyond the arrays, keeps the turn it had.
	 */
	public void move (Point3 pt, double a, double[] pans, double[] tilts)
	{
		for (int i = 0; i < pyr.length; i++)
		{
			if (pyr[i] == null)			continue;
			if ((pans != null) && (i < pans.length) && Double.isFinite (pans[i]))		pan[i] = pans[i];
			if ((tilts != null) && (i < tilts.length) && Double.isFinite (tilts[i]))	tilt[i] = tilts[i];

			SensorPos	f = rdesc.camfeat[i];
			double		cx = f.rho () * Math.cos (f.theta ()), cy = f.rho () * Math.sin (f.theta ());

			// the robot in the world, the camera on the robot, and how it is turned
			t.setIdentity ();
			t.setTranslation (new Vector3d (pt.x (), pt.y (), pt.z ()));
			step.rotZ (a);												t.mul (step);
			step.setIdentity ();
			step.setTranslation (new Vector3d (cx, cy, f.z ()));		t.mul (step);
			step.rotZ (f.orientation () + pan[i]);						t.mul (step);
			step.rotY (-(f.elevation () + tilt[i]));					t.mul (step);		// a positive pitch raises +x

			// the pyramid in the world, cut to what can be seen
			Point3d[]	p = new Point3d[pyr[i].length];

			for (int k = 0; k < p.length; k++)		{ p[k] = new Point3d (pyr[i][k]);	t.transform (p[k]); }

			List<Point3d[]>	side = new ArrayList<Point3d[]> ();			// the four faces from the camera
			List<Point3d[]>	base = new ArrayList<Point3d[]> ();			// what it sees at its range max

			for (int k = 0; k < 4; k++)		side.add (new Point3d[] { p[0], p[1 + k], p[1 + (k + 1) % 4] });
			base.add (new Point3d[] { p[1], p[2], p[3], p[4] });
			cut (side, base);
			faces[i].setGeometry (geometry (side));
			bases[i].setGeometry (geometry (base));
		}
	}

	/* ------------------------------------------------------------------ */
	/* Cutting the pyramid                                                 */
	/* ------------------------------------------------------------------ */

	/**
	 * Cuts the pyramid (its side faces and its base, convex polygons of a convex
	 * body) to the half spaces that can be seen: over the floor, and inside the
	 * outer walls when they are known. What each cut leaves on its plane is
	 * closed with a cap, added to the side faces.
	 */
	protected void cut (List<Point3d[]> side, List<Point3d[]> base)
	{
		halve (side, base, new Vector3d (0.0, 0.0, 1.0), -FLOOR);					// z >= FLOOR
		if (bounds == null)			return;
		halve (side, base, new Vector3d ( 1.0, 0.0, 0.0), -bounds[0]);				// x >= xmin
		halve (side, base, new Vector3d (-1.0, 0.0, 0.0),  bounds[2]);				// x <= xmax
		halve (side, base, new Vector3d (0.0,  1.0, 0.0), -bounds[1]);				// y >= ymin
		halve (side, base, new Vector3d (0.0, -1.0, 0.0),  bounds[3]);				// y <= ymax
	}

	/** Keeps of every polygon what is on the side n.p + d >= 0, and caps the cut. */
	protected void halve (List<Point3d[]> side, List<Point3d[]> base, Vector3d n, double d)
	{
		List<Point3d>	rim = new ArrayList<Point3d> ();						// where the polygons cross the plane

		clip (side, n, d, rim);
		clip (base, n, d, rim);

		Point3d[]		cap = cap (rim, n);

		if (cap != null)			side.add (cap);
	}

	/** Sutherland-Hodgman on each polygon, against one half space; the crossing points go to the rim. */
	static protected void clip (List<Point3d[]> polys, Vector3d n, double d, List<Point3d> rim)
	{
		for (int i = polys.size () - 1; i >= 0; i--)
		{
			Point3d[]		in = polys.get (i);
			List<Point3d>	out = new ArrayList<Point3d> ();

			for (int k = 0; k < in.length; k++)
			{
				Point3d		a = in[k], b = in[(k + 1) % in.length];
				double		da = n.x * a.x + n.y * a.y + n.z * a.z + d;
				double		db = n.x * b.x + n.y * b.y + n.z * b.z + d;

				if (da >= 0.0)					out.add (a);
				if ((da >= 0.0) != (db >= 0.0))									// the edge crosses the plane
				{
					double		s = da / (da - db);
					Point3d		c = new Point3d (a.x + s * (b.x - a.x), a.y + s * (b.y - a.y), a.z + s * (b.z - a.z));

					out.add (c);
					rim.add (c);
				}
			}
			if (out.size () < 3)		polys.remove (i);
			else						polys.set (i, out.toArray (new Point3d[out.size ()]));
		}
	}

	/**
	 * The polygon that closes a cut: the crossing points, which lie on the plane
	 * and bound a convex figure, in order around their centre. Null with fewer
	 * than three distinct ones.
	 */
	static protected Point3d[] cap (List<Point3d> rim, Vector3d n)
	{
		List<Point3d>	pts = new ArrayList<Point3d> ();

		for (Point3d p : rim)
		{
			boolean		seen = false;

			for (Point3d q : pts)		if (p.distanceSquared (q) < 1e-10)		{ seen = true; break; }
			if (!seen)					pts.add (p);
		}
		if (pts.size () < 3)			return null;

		Point3d		c = new Point3d ();

		for (Point3d p : pts)			c.add (p);
		c.scale (1.0 / pts.size ());

		// two axes in the plane to measure the angle of each point about the centre
		Vector3d	u = new Vector3d (), v = new Vector3d ();
		Vector3d	any = (Math.abs (n.z) < 0.9) ? new Vector3d (0.0, 0.0, 1.0) : new Vector3d (1.0, 0.0, 0.0);

		u.cross (n, any);		u.normalize ();
		v.cross (n, u);

		final double[]	ang = new double[pts.size ()];
		Integer[]		idx = new Integer[pts.size ()];

		for (int i = 0; i < pts.size (); i++)
		{
			Vector3d	r = new Vector3d (pts.get (i));

			r.sub (c);
			ang[i]	= Math.atan2 (r.dot (v), r.dot (u));
			idx[i]	= i;
		}
		java.util.Arrays.sort (idx, new java.util.Comparator<Integer> ()
		{
			public int compare (Integer a, Integer b)		{ return Double.compare (ang[a], ang[b]); }
		});

		Point3d[]	cap = new Point3d[pts.size ()];

		for (int i = 0; i < cap.length; i++)		cap[i] = pts.get (idx[i]);
		return cap;
	}

	/** Convex polygons as triangles (a fan from the first corner of each); null with none. */
	static protected GeometryArray geometry (List<Point3d[]> polys)
	{
		int		n = 0;

		for (Point3d[] p : polys)		n += 3 * (p.length - 2);
		if (n == 0)						return null;

		TriangleArray	ta = new TriangleArray (n, TriangleArray.COORDINATES);
		int				k = 0;

		for (Point3d[] p : polys)
			for (int i = 1; i + 1 < p.length; i++)
			{
				ta.setCoordinate (k++, p[0]);
				ta.setCoordinate (k++, p[i]);
				ta.setCoordinate (k++, p[i + 1]);
			}
		return ta;
	}

	/* ------------------------------------------------------------------ */
	/* The pyramid                                                         */
	/* ------------------------------------------------------------------ */

	/**
	 * The corners of the pyramid of one camera in its own frame: the apex, then
	 * the four corners of its base at the range max going round (top left, top
	 * right, bottom right, bottom left). Null when the description says nothing
	 * of how wide the camera sees.
	 */
	protected Point3d[] pyramid (int i)
	{
		double		hfov = ((rdesc.camhfov != null) && (i < rdesc.camhfov.length) && (rdesc.camhfov[i] > 0.0)) ? rdesc.camhfov[i] : rdesc.CONECAM;
		double		vfov = ((rdesc.camvfov != null) && (i < rdesc.camvfov.length) && (rdesc.camvfov[i] > 0.0)) ? rdesc.camvfov[i] : rdesc.VFOVCAM;
		double		r = ((rdesc.camrange != null) && (i < rdesc.camrange.length) && (rdesc.camrange[i] > 0.0)) ? rdesc.camrange[i] : DEF_RANGE;

		if ((rdesc.camfeat[i] == null) || (hfov <= 0.0) || (vfov <= 0.0))		return null;

		double		hw = r * Math.tan (hfov / 2.0), hh = r * Math.tan (vfov / 2.0);
		Point3d[]	c = new Point3d[5];

		c[0]	= new Point3d (0.0, 0.0, 0.0);
		for (int k = 0; k < 4; k++)
		{
			double	sw = ((k == 0) || (k == 3)) ? hw : -hw;			// left (+y), right
			double	sh = (k < 2) ? hh : -hh;						// top, bottom
			c[1 + k]	= new Point3d (r, sw, sh);
		}
		return c;
	}

	/** A shape whose geometry is set again every time the camera moves. */
	static protected Shape3D shape (float shade)
	{
		Shape3D		s = new Shape3D ();

		s.setCapability (Shape3D.ALLOW_GEOMETRY_WRITE);
		s.setAppearance (appearance (shade));
		return s;
	}

	/** The faces of the pyramid in the colour the robot editor gives the cameras (yellow), see-through. */
	static protected Appearance appearance (float shade)
	{
		Appearance	app = new Appearance ();
		Color3f		c = new Color3f (0.95f * shade, 0.9f * shade, 0.3f * shade);

		app.setColoringAttributes (new ColoringAttributes (c, ColoringAttributes.SHADE_FLAT));
		app.setTransparencyAttributes (new TransparencyAttributes (TransparencyAttributes.BLENDED, ALPHA));
		app.setPolygonAttributes (new PolygonAttributes (PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0f));
		return app;
	}
}
