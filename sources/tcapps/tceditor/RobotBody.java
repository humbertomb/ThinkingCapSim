/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.io.File;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;

import com.sun.j3d.utils.geometry.Box;

import tc.vrobot.RobotDesc;
import tc.vrobot.articulated.KineJson;
import tc.vrobot.articulated.KineModel;
import tc.vrobot.articulated.WalkingModel;
import tcapps.tceditor.visualization.Articulated3D;
import tcapps.tceditor.visualization.Scene3D;
import tcapps.tcsimulator.simulator.SimulatorDesc;

/**
 * The body a robot is drawn with in a 3D scene, as its simulator description
 * says: its kinematic model when it is articulated (an AIBO), moved by its
 * walking model; its 3D model (and the one of its lift) when it has one; and a
 * box the size of it when it has neither. What the 3D world of the simulator
 * draws and what the cameras of the simulated robots see are built here, so
 * that a robot looks the same in both.
 */
public class RobotBody
{
	public final TransformGroup		body;				// what is put in a Robot3D
	public final TransformGroup		lift;				// its lift, null for none
	public final Articulated3D		art;				// the articulated body, null for a rigid one
	public final WalkingModel		walker;				// what moves its joints, null for none
	protected long					last;				// when it was last moved on [ns], for the time step of its walk

	protected RobotBody (TransformGroup body, TransformGroup lift, Articulated3D art, WalkingModel walker)
	{
		this.body	= body;
		this.lift	= lift;
		this.art	= art;
		this.walker	= walker;
	}

	/** The body of a robot, with its shapes taken from a scene (which keeps the 3D models it has read). */
	static public RobotBody build (Scene3D scene, RobotDesc rdesc, SimulatorDesc sdesc)
	{
		Articulated3D		art = null;
		WalkingModel		walker = null;
		TransformGroup		body, lift;

		// an articulated robot: its kinematic model in the place of the 3D model, and
		// its walking model to move the joints as it goes (a robot with a .kine and no
		// walking model stands articulated at rest)
		if ((sdesc != null) && (sdesc.KINEFILE != null) && (sdesc.KINEFILE.trim ().length () > 0))
		{
			try
			{
				KineModel	km = KineJson.read (new File (sdesc.KINEFILE.trim ()));

				art		= new Articulated3D (km, sdesc.V3DPARTS, (sdesc.V3DTEAM != null) ? sdesc.V3DTEAM.toLowerCase () : null);
				walker	= WalkingModel.create (sdesc.WALKMODEL, km);
				if (walker != null)		{ walker.stand ();	art.update (); }

				double[]	st = ShapeLines.standing (km, walker, sdesc.V3DPARTS);			// pitched onto its feet, the lowest of them on the floor

				art.move (0.0, 0.0, st[1], 0.0, st[0]);
			}
			catch (Exception e)
			{
				System.out.println ("  [RobotBody] Cannot read the kinematic model " + sdesc.KINEFILE + ": " + e);
				art		= null;
				walker	= null;
			}
		}

		body	= ((art != null) || (sdesc == null) || (sdesc.V3DFILE == null)) ? null : scene.getCachedObject (sdesc.V3DFILE, null);
		lift	= ((art != null) || (sdesc == null) || (sdesc.V3DLIFT == null)) ? null : scene.getCachedObject (sdesc.V3DLIFT, null);
		if (art != null)
		{
			body	= new TransformGroup ();
			body.addChild (art);
		}
		else if (body == null)
		{
			// no 3D model: a box the size of the robot
			double	r = Math.max (0.1, (rdesc != null) ? rdesc.RADIUS : 0.0);

			body	= new TransformGroup ();
			body.addChild (new Box ((float) r, (float) r, (float) (r / 2.0), appearance (new Color3f (0.2f, 0.4f, 0.9f))));
		}
		body.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		if (lift != null)		lift.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		return new RobotBody (body, lift, art, walker);
	}

	/**
	 * Moves the joints of an articulated robot on by the time gone since it was
	 * last moved, walking with the control action it is carrying out ({vlin,
	 * vlat, vrot}; null for none) and with its head turned where its first camera
	 * looks (pan and tilt of each camera, rad; null: as it was). A rigid robot has
	 * nothing to move.
	 */
	public void step (double[] pans, double[] tilts, double[] vel)
	{
		long		now;
		double		dt;

		if ((art == null) || (walker == null))		return;
		now		= System.nanoTime ();
		dt		= (last == 0L) ? 0.04 : Math.min (0.2, (now - last) / 1e9);
		last	= now;
		if (vel != null)		walker.setVelocities (vel[0], vel[1], vel[2]);
		if ((pans != null) && (pans.length > 0) && (tilts != null) && (tilts.length > 0) && Double.isFinite (pans[0]) && Double.isFinite (tilts[0]))
			walker.setHead (pans[0], tilts[0]);
		walker.step (dt);
		art.update ();
	}

	static private Appearance appearance (Color3f color)
	{
		Appearance		app = new Appearance ();
		Material		mat = new Material (color, new Color3f (0f, 0f, 0f), color, new Color3f (0.3f, 0.3f, 0.3f), 32f);

		mat.setLightingEnable (true);
		app.setMaterial (mat);
		app.setColoringAttributes (new ColoringAttributes (color, ColoringAttributes.SHADE_GOURAUD));
		return app;
	}
}
