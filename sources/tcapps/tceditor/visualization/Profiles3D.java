/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor.visualization;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import tc.shared.world.WMBeacon;
import tc.shared.world.WMCBeacon;
import tc.shared.world.World;
import tc.vrobot.RobotData;
import tc.vrobot.RobotDesc;
import tc.vrobot.SensorPos;
import wucore.utils.geom.Line2;

/**
 * What the sensors of a robot are measuring now, drawn where they measure it, as
 * the robot editor draws what they cover but as far as what each one reads:
 * <ul>
 * <li>a sonar or an infrared, the sector of its aperture out to the distance it
 *     reads;</li>
 * <li>a laser range finder, the polygon of its scan, a vertex at the end of
 *     every ray;</li>
 * <li>a laser beacon scanner, a line from it to every reflector of the world it
 *     sees (in its range and aperture, and with no wall in between).</li>
 * </ul>
 * Everything is flat, at the height of each sensor, and drawn again whenever
 * the robot moves ({@link #update}), at most every {@link #PERIOD} ms.
 */
public class Profiles3D extends BranchGroup
{
	/** How often the profiles are drawn again, at most [ms]. */
	static public final long		PERIOD		= 100;

	static public final Color3f		C_SONAR		= new Color3f (1.0f, 0.85f, 0.0f);
	static public final Color3f		C_IR		= new Color3f (1.0f, 0.45f, 0.0f);
	static public final Color3f		C_LRF		= new Color3f (0.15f, 0.45f, 1.0f);
	static public final Color3f		C_LSB		= new Color3f (1.0f, 0.0f, 1.0f);

	protected RobotDesc				rdesc;
	protected World					world;				// where the reflectors are, and the walls that hide them
	protected BranchGroup			drawn;				// what is drawn now
	protected long					last;

	public Profiles3D (RobotDesc rdesc)
	{
		this.rdesc	= rdesc;
		setCapability (BranchGroup.ALLOW_DETACH);
		setCapability (BranchGroup.ALLOW_CHILDREN_EXTEND);
		setCapability (BranchGroup.ALLOW_CHILDREN_WRITE);
	}

	/** The world the reflectors the beacon scanners look for are in (null: none). */
	public void setWorld (World world)			{ this.world = world; }

	/** Draws what the sensors read, the robot at (x, y, heading a). */
	public void update (RobotData data, double x, double y, double a)
	{
		long			now = System.currentTimeMillis ();
		BranchGroup		bg;

		if ((data == null) || (now - last < PERIOD))		return;
		last	= now;

		bg		= new BranchGroup ();
		bg.setCapability (BranchGroup.ALLOW_DETACH);
		ranges (bg, rdesc.sonfeat, rdesc.MAXSONAR, data.sonars, rdesc.CONESON, C_SONAR, x, y, a);
		ranges (bg, rdesc.irfeat, rdesc.MAXIR, data.irs, rdesc.CONEIR, C_IR, x, y, a);
		scans (bg, data, x, y, a);
		beacons (bg, x, y, a);

		if (drawn != null)		drawn.detach ();
		drawn	= bg;
		addChild (bg);
	}

	/* ------------------------------------------------------------------ */

	/** Where a sensor is in the world, at its height: {x, y, z, heading}. */
	static protected double[] place (SensorPos s, double x, double y, double a)
	{
		return new double[] { x + s.rho () * Math.cos (a + s.theta ()), y + s.rho () * Math.sin (a + s.theta ()), s.z (), a + s.orientation () };
	}

	/** The sector of each sensor of a family out to what it reads. */
	protected void ranges (BranchGroup bg, SensorPos[] feat, int n, double[] read, double cone, Color3f c,
						   double x, double y, double a)
	{
		if ((feat == null) || (read == null))		return;
		for (int i = 0; (i < n) && (i < feat.length) && (i < read.length); i++)
		{
			if ((feat[i] == null) || !(read[i] > 0.0))		continue;

			double[]	p = place (feat[i], x, y, a);
			int			steps = Math.max (4, (int) Math.ceil (Math.toDegrees (cone) / 5.0));
			double[][]	pts = new double[steps + 1][];

			for (int k = 0; k <= steps; k++)
			{
				double	b = p[3] - cone / 2.0 + cone * k / steps;
				pts[k]	= new double[] { p[0] + read[i] * Math.cos (b), p[1] + read[i] * Math.sin (b) };
			}
			fan (bg, p, pts, c);
		}
	}

	/** The polygon of the scan of each laser range finder, a vertex at the end of every ray. */
	protected void scans (BranchGroup bg, RobotData data, double x, double y, double a)
	{
		int			rays = rdesc.RAYLRF;
		double		cone = rdesc.CONELRF;

		if ((rdesc.lrffeat == null) || (data.lrfs == null) || (rays < 2))		return;
		for (int i = 0; (i < rdesc.MAXLRF) && (i < rdesc.lrffeat.length) && (i < data.lrfs.length); i++)
		{
			double[]	read = data.lrfs[i];

			if ((rdesc.lrffeat[i] == null) || (read == null))		continue;

			double[]	p = place (rdesc.lrffeat[i], x, y, a);
			double[][]	pts = new double[Math.min (rays, read.length)][];
			double		step = cone / (rays - 1);

			for (int k = 0; k < pts.length; k++)
			{
				double	b = p[3] - cone / 2.0 + step * k;
				pts[k]	= new double[] { p[0] + read[k] * Math.cos (b), p[1] + read[k] * Math.sin (b) };
			}
			fan (bg, p, pts, C_LRF);
		}
	}

	/** A line from each laser beacon scanner to every reflector it sees. */
	protected void beacons (BranchGroup bg, double x, double y, double a)
	{
		List<double[]>	refl;
		List<Line2>		walls;

		if ((rdesc.lsbfeat == null) || (rdesc.MAXLSB <= 0) || (world == null))		return;
		refl	= reflectors ();
		if (refl.isEmpty ())			return;
		walls	= new ArrayList<Line2> ();
		if (world.walls () != null)
			for (Line2 l : world.walls ().getLines ())		if (l != null)		walls.add (l);

		List<double[]>	segs = new ArrayList<double[]> ();

		for (int i = 0; (i < rdesc.MAXLSB) && (i < rdesc.lsbfeat.length); i++)
		{
			if (rdesc.lsbfeat[i] == null)		continue;

			double[]	p = place (rdesc.lsbfeat[i], x, y, a);

			for (double[] r : refl)
			{
				double	dx = r[0] - p[0], dy = r[1] - p[1], d = Math.hypot (dx, dy);
				double	off = Math.abs (wucore.utils.math.Angles.radnorm_180 (Math.atan2 (dy, dx) - p[3]));

				if ((d <= 1e-6) || ((rdesc.RANGELSB > 0.0) && (d > rdesc.RANGELSB)))		continue;
				if ((rdesc.CONELSB < 2.0 * Math.PI - 1e-6) && (off > rdesc.CONELSB / 2.0))	continue;
				if (hidden (p[0], p[1], r[0] - 0.02 * dx / d, r[1] - 0.02 * dy / d, walls))	continue;		// short of the reflector: a plate on a wall is not behind it
				segs.add (new double[] { p[0], p[1], p[2], r[0], r[1], p[2] });
			}
		}
		if (segs.isEmpty ())			return;

		LineArray		la = new LineArray (2 * segs.size (), GeometryArray.COORDINATES);

		for (int k = 0; k < segs.size (); k++)
		{
			double[]	s = segs.get (k);
			la.setCoordinate (2 * k, new Point3d (s[0], s[1], s[2]));
			la.setCoordinate (2 * k + 1, new Point3d (s[3], s[4], s[5]));
		}
		bg.addChild (new Shape3D (la, lines (C_LSB, 2.0f)));
	}

	/** The reflectors of the world, where a scanner aims at: the cylinders, and the middle of the plates. */
	protected List<double[]> reflectors ()
	{
		List<double[]>	out = new ArrayList<double[]> ();

		if (world.cbeacons () != null)
			for (WMCBeacon b : world.cbeacons ())		if (b != null)		out.add (new double[] { b.x (), b.y () });
		if (world.beacons () != null)
			for (WMBeacon b : world.beacons ())
			{
				Line2	l = (b != null) ? b.getLine () : null;
				if (l != null)		out.add (new double[] { (l.orig ().x () + l.dest ().x ()) / 2.0, (l.orig ().y () + l.dest ().y ()) / 2.0 });
			}
		return out;
	}

	/** Whether a wall is in the way from one point to another. */
	static protected boolean hidden (double x0, double y0, double x1, double y1, List<Line2> walls)
	{
		for (Line2 w : walls)
		{
			double	ax = w.orig ().x (), ay = w.orig ().y (), bx = w.dest ().x (), by = w.dest ().y ();
			double	d1 = side (ax, ay, bx, by, x0, y0), d2 = side (ax, ay, bx, by, x1, y1);
			double	d3 = side (x0, y0, x1, y1, ax, ay), d4 = side (x0, y0, x1, y1, bx, by);

			if ((d1 * d2 < 0.0) && (d3 * d4 < 0.0))		return true;
		}
		return false;
	}

	static private double side (double ax, double ay, double bx, double by, double px, double py)
	{
		return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
	}

	/**
	 * A flat fan from a point (at its height) through some others in order: filled,
	 * see-through, and with its outline from the point round to the point again.
	 */
	protected void fan (BranchGroup bg, double[] o, double[][] pts, Color3f c)
	{
		if (pts.length < 2)			return;

		TriangleArray	ta = new TriangleArray (3 * (pts.length - 1), GeometryArray.COORDINATES);
		LineArray		la = new LineArray (2 * (pts.length + 1), GeometryArray.COORDINATES);
		Point3d			po = new Point3d (o[0], o[1], o[2]);

		for (int k = 0; k + 1 < pts.length; k++)
		{
			ta.setCoordinate (3 * k, po);
			ta.setCoordinate (3 * k + 1, new Point3d (pts[k][0], pts[k][1], o[2]));
			ta.setCoordinate (3 * k + 2, new Point3d (pts[k + 1][0], pts[k + 1][1], o[2]));
		}
		la.setCoordinate (0, po);
		la.setCoordinate (1, new Point3d (pts[0][0], pts[0][1], o[2]));
		for (int k = 0; k + 1 < pts.length; k++)
		{
			la.setCoordinate (2 + 2 * k, new Point3d (pts[k][0], pts[k][1], o[2]));
			la.setCoordinate (3 + 2 * k, new Point3d (pts[k + 1][0], pts[k + 1][1], o[2]));
		}
		la.setCoordinate (2 * pts.length, new Point3d (pts[pts.length - 1][0], pts[pts.length - 1][1], o[2]));
		la.setCoordinate (2 * pts.length + 1, po);

		bg.addChild (new Shape3D (ta, fill (c)));
		bg.addChild (new Shape3D (la, lines (c, 1.5f)));
	}

	/** See-through, of a colour, seen from above and from below. */
	static protected Appearance fill (Color3f c)
	{
		Appearance		app = new Appearance ();
		PolygonAttributes	pa = new PolygonAttributes ();

		pa.setCullFace (PolygonAttributes.CULL_NONE);
		app.setPolygonAttributes (pa);
		app.setColoringAttributes (new ColoringAttributes (c, ColoringAttributes.SHADE_FLAT));
		app.setTransparencyAttributes (new TransparencyAttributes (TransparencyAttributes.NICEST, 0.7f));
		RenderingAttributes	ra = new RenderingAttributes ();
		ra.setDepthBufferWriteEnable (false);				// what is behind a see-through fill is still drawn
		app.setRenderingAttributes (ra);
		return app;
	}

	static protected Appearance lines (Color3f c, float width)
	{
		Appearance		app = new Appearance ();

		app.setColoringAttributes (new ColoringAttributes (c, ColoringAttributes.SHADE_FLAT));
		app.setLineAttributes (new LineAttributes (width, LineAttributes.PATTERN_SOLID, true));
		return app;
	}
}
