/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor.visualization;

import javax.media.j3d.BranchGroup;
import javax.vecmath.Color3f;

import tc.shared.lps.LPS;
import tc.shared.lps.lpo.LPORangeBuffer;
import tc.shared.lps.lpo.LPOSource;
import tc.vrobot.FeaturePos;
import tc.vrobot.RobotData;
import tc.vrobot.RobotDesc;
import tc.vrobot.SensorPos;
import tclib.utils.fusion.Fusion;
import tclib.utils.fusion.FusionDesc;

/**
 * What the virtual sensors of a robot are reading now, drawn where they read it,
 * as {@link Profiles3D} draws the real ones:
 * <ul>
 * <li>a fused sensor (sonar and infrared looking the same way), the sector of
 *     its aperture out to the distance it reads;</li>
 * <li>a reduced laser scan, the polygon of its fan, a vertex at the end of every
 *     ray;</li>
 * <li>a sensor of an area, its sector out to the distance it reads, no
 *     farther than its range.</li>
 * </ul>
 * They are worked out here from what the real sensors read, as the perception
 * of the robot works them out (the fusion of its description). The range buffer
 * the sensors of an area are taken from holds what was read on the last cycle
 * only: the perception remembers older sonar readings as the robot moves, and a
 * sensor of an area here may see a little less than the one of the robot.
 */
public class VirtualProfiles3D extends Profiles3D
{
	static public final Color3f		C_FUSED		= new Color3f (1.0f, 0.25f, 0.25f);
	static public final Color3f		C_SCAN		= new Color3f (0.0f, 0.8f, 0.8f);
	static public final Color3f		C_GROUP		= new Color3f (0.55f, 0.25f, 1.0f);

	/** How high the virtual sensors are drawn when they say no height (m): just over the floor. */
	static public final double		FLOOR		= 0.03;
	/** How far the range buffer takes readings (m), as the perception of the robot has it. */
	static public final double		BUFFER_RANGE	= 2.5;

	protected FusionDesc			fdesc;
	protected Fusion				fusion;
	protected LPS					lps;
	protected LPORangeBuffer		buffer;

	public VirtualProfiles3D (RobotDesc rdesc, FusionDesc fdesc)
	{
		super (rdesc);
		this.fdesc	= fdesc;
		this.fusion	= new Fusion (rdesc, fdesc);
		this.lps	= new LPS (rdesc, fdesc);
		this.buffer	= new LPORangeBuffer (Math.max (1, fusion.readings ()), BUFFER_RANGE, 0.1, "RBuffer", LPOSource.PERCEPT);
		lps.add (buffer);
	}

	/** Whether the robot has any virtual sensor to draw. */
	public boolean any ()
	{
		return (fdesc.MAXVIRTU > 0) || (fdesc.MAXSCAN > 0) || (fdesc.MAXGROUP > 0);
	}

	/** Draws what the virtual sensors read, the robot at (x, y, heading a). */
	public void update (RobotData data, double x, double y, double a)
	{
		long			now = System.currentTimeMillis ();
		BranchGroup		bg;

		if ((data == null) || (now - last < PERIOD))		return;
		last	= now;

		// what the perception would make of what the sensors read now
		try
		{
			fusion.fuse_signal (data);
			buffer.resetBuffer ();
			fusion.fill (buffer, data);
			fusion.fuse_feature (lps, data);
		}
		catch (RuntimeException e)		{ return; }			// a description the fusion cannot work with (a group listing a fused sensor it has not)

		bg		= new BranchGroup ();
		bg.setCapability (BranchGroup.ALLOW_DETACH);
		fused (bg, x, y, a);
		reduced (bg, x, y, a);
		groups (bg, x, y, a);

		if (drawn != null)		drawn.detach ();
		drawn	= bg;
		addChild (bg);
	}

	/* ------------------------------------------------------------------ */

	/** Where a virtual sensor is in the world, just over the floor if it says no height: {x, y, z, heading}. */
	static protected double[] spot (SensorPos s, double x, double y, double a)
	{
		double[]	p = place (s, x, y, a);

		if (!(p[2] > FLOOR))		p[2] = FLOOR;
		return p;
	}

	/** The sector of each fused sensor out to what it reads. */
	protected void fused (BranchGroup bg, double x, double y, double a)
	{
		for (int i = 0; i < fdesc.MAXVIRTU; i++)
			if ((fdesc.virtufeat[i] != null) && (fusion.virtuals[i] > 0.0))
				sector (bg, spot (fdesc.virtufeat[i], x, y, a), fdesc.CONEVIRTU, fusion.virtuals[i], C_FUSED);
	}

	/** The polygon of each reduced scan, a vertex at the end of every ray of its fan. */
	protected void reduced (BranchGroup bg, double x, double y, double a)
	{
		for (int k = 0; k < fdesc.MAXSCAN; k++)
		{
			double[]	read = fusion.allscans[k];
			int			n = (read != null) ? read.length : 0;

			if ((n < 2) || (fdesc.scanfeats[k] == null))		continue;

			double[]	p = spot (fdesc.scanfeats[k], x, y, a);
			double[][]	pts = new double[n][];
			double		step = fdesc.scancones[k] / (n - 1);

			for (int j = 0; j < n; j++)
			{
				double	b = p[3] - fdesc.scancones[k] / 2.0 + step * j;
				pts[j]	= new double[] { p[0] + read[j] * Math.cos (b), p[1] + read[j] * Math.sin (b) };
			}
			fan (bg, p, pts, C_SCAN);
		}
	}

	/** The sector of each sensor of an area out to what it reads. */
	protected void groups (BranchGroup bg, double x, double y, double a)
	{
		for (int i = 0; i < fdesc.MAXGROUP; i++)
		{
			FeaturePos	f = fdesc.groupfeat[i];

			if (f == null)		continue;

			double[]	p = spot (f, x, y, a);
			double		d = Math.min (fusion.groups[i], f.range ());

			if (d > 0.0)
				sector (bg, p, f.cone (), d, C_GROUP);
		}
	}

	/** A sector from a point (x, y, z, heading), of an aperture, out to a distance. */
	protected void sector (BranchGroup bg, double[] p, double cone, double d, Color3f c)
	{
		int			steps = Math.max (4, (int) Math.ceil (Math.toDegrees (cone) / 5.0));
		double[][]	pts = new double[steps + 1][];

		for (int k = 0; k <= steps; k++)
		{
			double	b = p[3] - cone / 2.0 + cone * k / steps;
			pts[k]	= new double[] { p[0] + d * Math.cos (b), p[1] + d * Math.sin (b) };
		}
		fan (bg, p, pts, c);
	}
}
