/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor.visualization;

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
 * The pyramid is built once in the frame of the camera (apex at the origin,
 * looking down +x, y to its left, z up) and placed with a transform: the pose of
 * the robot, then where the camera sits on it, then its orientation and pan
 * about the vertical, then its elevation and tilt about the horizontal it is
 * left with -- the same order the simulator renders the frames of the camera in.
 */
public class Camera3D extends BranchGroup
{
	/** How far a camera is drawn to when its description says no range (m). */
	static public final double		DEF_RANGE	= 1.0;
	/** How see-through the pyramid is. */
	static public final float		ALPHA		= 0.7f;
	/** The base of the pyramid, a shade darker than its faces. */
	static public final float		BASE_SHADE	= 0.75f;

	protected RobotDesc				rdesc;
	protected TransformGroup[]		cams;						// one per camera; null where the camera cannot be drawn
	protected double[]				pan, tilt;					// how each camera is turned now (rad)

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

		cams	= new TransformGroup[n];
		pan		= new double[n];
		tilt	= new double[n];
		for (int i = 0; i < n; i++)
		{
			Group		pyr = pyramid (i);

			if (pyr == null)			continue;
			cams[i]	= new TransformGroup ();
			cams[i].setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
			cams[i].addChild (pyr);
			addChild (cams[i]);
		}
		move (pt, a, null, null);
	}

	/** How many cameras there are to draw (some may not be drawable). */
	public int count ()										{ return cams.length; }

	/**
	 * Follows the robot to a pose, with each camera turned as it is now: pan to the
	 * left and tilt upwards from where its description points it (rad); null, or a
	 * camera beyond the arrays, keeps the turn it had.
	 */
	public void move (Point3 pt, double a, double[] pans, double[] tilts)
	{
		for (int i = 0; i < cams.length; i++)
		{
			if (cams[i] == null)		continue;
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
			cams[i].setTransform (t);
		}
	}

	/**
	 * The pyramid of one camera in its own frame: its four faces and, a shade
	 * darker, its base at the range max. Null when the description says nothing of
	 * how wide the camera sees.
	 */
	protected Group pyramid (int i)
	{
		double		hfov = ((rdesc.camhfov != null) && (i < rdesc.camhfov.length) && (rdesc.camhfov[i] > 0.0)) ? rdesc.camhfov[i] : rdesc.CONECAM;
		double		vfov = ((rdesc.camvfov != null) && (i < rdesc.camvfov.length) && (rdesc.camvfov[i] > 0.0)) ? rdesc.camvfov[i] : rdesc.VFOVCAM;
		double		r = ((rdesc.camrange != null) && (i < rdesc.camrange.length) && (rdesc.camrange[i] > 0.0)) ? rdesc.camrange[i] : DEF_RANGE;

		if ((rdesc.camfeat[i] == null) || (hfov <= 0.0) || (vfov <= 0.0))		return null;

		double		hw = r * Math.tan (hfov / 2.0), hh = r * Math.tan (vfov / 2.0);
		Point3d		ap = new Point3d (0.0, 0.0, 0.0);
		Point3d[]	c = new Point3d[4];

		for (int k = 0; k < 4; k++)
		{
			double	sw = ((k == 0) || (k == 3)) ? hw : -hw;			// left (+y), right
			double	sh = (k < 2) ? hh : -hh;						// top, bottom
			c[k]	= new Point3d (r, sw, sh);
		}

		TriangleArray	ta = new TriangleArray (12, TriangleArray.COORDINATES);		// the four faces, from the camera
		for (int k = 0; k < 4; k++)
		{
			ta.setCoordinate (3 * k,     ap);
			ta.setCoordinate (3 * k + 1, c[k]);
			ta.setCoordinate (3 * k + 2, c[(k + 1) % 4]);
		}
		QuadArray		qa = new QuadArray (4, QuadArray.COORDINATES);				// what it sees at its range max
		for (int k = 0; k < 4; k++)		qa.setCoordinate (k, c[k]);

		Group		g = new Group ();

		g.addChild (new Shape3D (ta, appearance (1.0f)));
		g.addChild (new Shape3D (qa, appearance (BASE_SHADE)));
		return g;
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
