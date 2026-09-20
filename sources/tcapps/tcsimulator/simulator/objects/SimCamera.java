/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator.simulator.objects;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import tc.vrobot.RobotData;
import tc.vrobot.RobotDesc;
import tc.vrobot.SensorPos;

import tcapps.tceditor.visualization.Object3D;
import tcapps.tceditor.visualization.Robot3D;
import tcapps.tceditor.visualization.Scene3D;
import tcapps.tceditor.visualization.World3D;

import tcapps.tcsimulator.simulator.Simulator;
import tcapps.tcsimulator.simulator.SimulatorDesc;

import tc.shared.world.WMObject;

import wucore.utils.geom.Point3;

/**
 * One camera of a simulated robot: the 3D world seen from where the camera is
 * mounted, rendered into an image instead of onto a screen.
 *
 * It is a scene of its own, built out of the same pieces the 3D view of the
 * simulator is built of ({@link World3D}, {@link Robot3D}, {@link Object3D}),
 * and rendered by an off-screen {@link Canvas3D}: the world is drawn once, and
 * every frame only moves the robots and the objects that have moved and takes
 * the picture. A scene of its own because the 3D view of the simulator carries
 * its camera in the transform of the scene rather than in the platform of the
 * view, so a second camera cannot be put on it, and because a branch of a scene
 * graph can only hang from one universe at a time.
 *
 * What a camera says of itself is what the description of the platform says --
 * where it sits and where it looks ({@link RobotDesc#camfeat}), how wide it
 * sees (CONECAM and VFOVCAM) and how many frames it takes in a second
 * ({@link RobotDesc#camfps}) -- so a camera is aimed and timed by the
 * description and by nothing here.
 *
 * The robot the camera is mounted on is not drawn: the camera sits on it, so its
 * own body would be most of the picture.
 */
public class SimCamera extends Scene3D
{
	/** Pixels across of a frame; how many down follows from the two fields of view. */
	static public final int			WIDTH		= 320;
	/** Bounds of what it sees along the way it looks (m). */
	static public final double		NEAR		= 0.05;
	static public final double		FAR			= 300.0;
	/** What it sees when the description does not say (rad). */
	static public final double		DEF_HFOV	= Math.toRadians (60.0);
	static public final double		DEF_VFOV	= Math.toRadians (45.0);
	/** How many frames a second when the description does not say (fps). */
	static public final double		DEF_FPS		= 5.0;
	/** How far it can be made to look up or down before the up of the world is of no use (rad). */
	static public final double		MAX_PITCH	= Math.toRadians (89.0);
	/** Metres a pixel of the off-screen canvas stands for; Java 3D asks for a physical size. */
	static private final double		PIXEL		= 0.0254 / 90.0;

	protected Simulator				simul;
	protected int					robot;						// the robot the camera is mounted on
	protected int					dev;						// which camera of that robot it is
	protected SensorPos				feat;						// where it sits and where it looks
	protected double				hfov, vfov;					// how wide it sees (rad)
	protected double				period;						// between one frame and the next (ms)
	protected double				waited;						// since the last one (ms)

	protected Canvas3D				off;						// what it is rendered by
	protected BufferedImage			frame;						// what it is rendered into
	protected ImageComponent2D		buffer;

	protected List<Robot3D>			bodies	= new ArrayList<Robot3D> ();		// the other robots, in the order the simulator has them
	protected List<Object3D>		things	= new ArrayList<Object3D> ();		// the objects the world moves about
	protected BranchGroup			live;						// what moves hangs from here

	/**
	 * A camera of a robot, or null when it cannot be had: no camera of that
	 * number in the description, no world yet, or no 3D to render it with (a
	 * machine with no display, or without the libraries).
	 */
	static public SimCamera create (Simulator simul, int robot, int dev)
	{
		RobotDesc		rdesc;
		SensorPos		feat;
		double			hfov, vfov;
		int				w, h;

		if ((simul == null) || (simul.getWorld () == null))						return null;
		if ((robot < 0) || (robot >= simul.numrobots))							return null;
		rdesc	= simul.RDESC[robot];
		if ((rdesc == null) || (rdesc.camfeat == null) || (dev < 0) || (dev >= rdesc.camfeat.length))		return null;
		feat	= rdesc.camfeat[dev];
		if (feat == null)														return null;

		hfov	= (rdesc.CONECAM > 0.0) ? rdesc.CONECAM : DEF_HFOV;
		vfov	= (rdesc.VFOVCAM > 0.0) ? rdesc.VFOVCAM : DEF_VFOV;
		w		= WIDTH;
		h		= (int) Math.round (WIDTH * Math.tan (vfov / 2.0) / Math.tan (hfov / 2.0));
		h		= Math.max (16, Math.min (4 * WIDTH, h));
		try
		{
			GraphicsConfigTemplate3D	tmpl = new GraphicsConfigTemplate3D ();
			GraphicsConfiguration		gc = GraphicsEnvironment.getLocalGraphicsEnvironment ()
											.getDefaultScreenDevice ().getBestConfiguration (tmpl);
			Canvas3D					off = new Canvas3D (gc, true);				// true: off screen

			off.getScreen3D ().setSize (w, h);
			off.getScreen3D ().setPhysicalScreenWidth (w * PIXEL);
			off.getScreen3D ().setPhysicalScreenHeight (h * PIXEL);
			return new SimCamera (off, w, h, simul, robot, dev, feat, hfov, vfov);
		}
		catch (Throwable t)
		{
			System.out.println ("--[SimCamera] Cannot create camera " + dev + " of robot " + robot + ": " + t);
			return null;
		}
	}

	protected SimCamera (Canvas3D off, int w, int h, Simulator simul, int robot, int dev,
						 SensorPos feat, double hfov, double vfov)
	{
		super (off);

		double		fps;

		this.off	= off;
		this.simul	= simul;
		this.robot	= robot;
		this.dev	= dev;
		this.feat	= feat;
		this.hfov	= hfov;
		this.vfov	= vfov;

		fps			= rate (simul.RDESC[robot], dev);
		period		= 1000.0 / fps;
		waited		= period;										// the first frame is due at once

		frame		= new BufferedImage (w, h, BufferedImage.TYPE_3BYTE_BGR);
		buffer		= new ImageComponent2D (ImageComponent.FORMAT_RGB, frame, true, false);
		off.setOffScreenBuffer (buffer);

		// what it sees: as wide as the description says, and as far
		View		v = universe.getViewer ().getView ();
		v.setFieldOfView (hfov);										// the vertical one follows from the shape of the frame
		v.setFrontClipDistance (NEAR);
		v.setBackClipDistance (FAR);

		// the camera is carried by the platform of the view and not by the transform
		// of the scene, which is what Scene3D does and what this is instead of
		scene.setTransform (new Transform3D ());

		build ();
	}

	/** How many frames a second this camera takes, as the description says (fps). */
	static public double rate (RobotDesc rdesc, int dev)
	{
		double		fps = 0.0;

		if (rdesc == null)									return DEF_FPS;
		if ((rdesc.camfps != null) && (dev >= 0) && (dev < rdesc.camfps.length))		fps = rdesc.camfps[dev];
		if (fps <= 0.0)										fps = rdesc.FPSCAM;
		return (fps > 0.0) ? fps : DEF_FPS;
	}

	/* Accessor methods */
	public final int				device ()		{ return dev; }
	public final BufferedImage		last ()			{ return frame; }
	public final double			framerate ()	{ return 1000.0 / period; }
	public final int				width ()		{ return frame.getWidth (); }
	public final int				height ()		{ return frame.getHeight (); }

	/* Instance methods */

	/** The world and what moves in it, once. */
	protected void build ()
	{
		BranchGroup		world = new BranchGroup ();

		// the animated ones are added live, below; and a camera sees the warehouse
		// and not the marks a map carries for whoever edits it
		world.addChild (new World3D (simul.getWorld (), this, false, false));
		scene.addChild (world);

		live	= new BranchGroup ();
		live.setCapability (BranchGroup.ALLOW_CHILDREN_EXTEND);
		live.setCapability (BranchGroup.ALLOW_CHILDREN_WRITE);
		scene.addChild (live);

		addRobots ();
		addThings ();
	}

	/** A body for every robot of the simulation but the one that carries the camera. */
	protected void addRobots ()
	{
		while (bodies.size () < simul.numrobots)
		{
			int				i = bodies.size ();
			RobotDesc		rdesc = simul.RDESC[i];
			SimulatorDesc	sdesc = simul.SDESC[i];
			Robot3D			body = null;

			if ((i != robot) && (rdesc != null))
			{
				TransformGroup	shape = ((sdesc != null) && (sdesc.V3DFILE != null)) ? getCachedObject (sdesc.V3DFILE, null) : null;
				TransformGroup	lift = ((sdesc != null) && (sdesc.V3DLIFT != null)) ? getCachedObject (sdesc.V3DLIFT, null) : null;

				if (shape != null)
				{
					shape.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
					if (lift != null)		lift.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
					body	= new Robot3D (rdesc, shape, lift, new Point3 (0.0, 0.0, 0.0), 0.0, 0.0, null);
					live.addChild (body);
				}
			}
			bodies.add (body);											// null: the one with the camera, or one with no shape
		}
	}

	/** A shape for every object the world moves about. */
	protected void addThings ()
	{
		SimObjects		objs = simul.objects;

		if (objs == null)					return;
		while (things.size () < objs.numobjects)
		{
			int				i = things.size ();
			WMObject		o = (objs.OBJS[i] != null) ? objs.OBJS[i].odesc : null;
			Object3D		thing = null;

			if ((o != null) && (o.shape != null))
			{
				TransformGroup	shape = getCachedObject (o.shape,
									o.usecolor ? wucore.utils.color.ColorTool.fromWColorToColor (o.color) : null);
				if (shape != null)
				{
					shape.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
					thing	= new Object3D (shape, new Point3 (o.pos.x (), o.pos.y (), o.pos.z ()), o.a);
					live.addChild (thing);
				}
			}
			things.add (thing);											// null: an object with no 3D shape of its own
		}
	}

	/**
	 * True when it is time to take another frame, which it says once: asking
	 * takes the turn. <code>dtime</code> is the time of the cycle just simulated.
	 */
	public boolean due (long dtime)
	{
		waited	+= dtime;
		if (waited < period)				return false;
		waited	= (waited > 4 * period) ? 0.0 : (waited - period);		// a simulation slower than the camera does not pile frames up
		return true;
	}

	/**
	 * The world as this camera sees it with the robot at the pose of
	 * <code>data</code>, or null when it cannot be rendered.
	 *
	 * The image is the one of this camera, drawn over: whoever keeps it keeps a
	 * copy of its own ({@link tc.shared.linda.ItemCamera#copy}).
	 */
	public BufferedImage take (RobotData data)
	{
		if (data == null)					return null;
		try
		{
			addRobots ();				addThings ();
			moveRobots ();				moveThings ();
			aim (data);
			off.renderOffScreenBuffer ();
			off.waitForOffScreenRendering ();
			return frame;
		}
		catch (Throwable t)
		{
			System.out.println ("--[SimCamera] Cannot take a frame of camera " + dev + " of robot " + robot + ": " + t);
			return null;
		}
	}

	/** The other robots, where the simulation has them. */
	protected void moveRobots ()
	{
		for (int i = 0; i < bodies.size (); i++)
		{
			Robot3D			body = bodies.get (i);
			RobotData		d = (simul.lastRobotData != null) ? simul.lastRobotData[i] : null;

			if ((body == null) || (d == null))		continue;
			body.move (d, new Point3 (d.real_x, d.real_y, 0.0), d.fork, d.real_a);
		}
	}

	/** The objects, where the world has them. */
	protected void moveThings ()
	{
		SimObjects		objs = simul.objects;

		if (objs == null)					return;
		for (int i = 0; (i < things.size ()) && (i < objs.numobjects); i++)
		{
			Object3D		thing = things.get (i);
			WMObject		o = (objs.OBJS[i] != null) ? objs.OBJS[i].odesc : null;

			if ((thing == null) || (o == null))		continue;
			thing.move (new Point3 (o.pos.x (), o.pos.y (), o.pos.z ()), o.a);
		}
	}

	/**
	 * Puts the eye of the view where the camera is and points it where the camera
	 * looks: the robot carries it, so its pose is added to the one of the sensor.
	 */
	protected void aim (RobotData data)
	{
		double			yaw = data.real_a + feat.orientation ();
		double			pitch = Math.max (-MAX_PITCH, Math.min (MAX_PITCH, feat.elevation ()));
		double			cx = data.real_x + feat.rho () * Math.cos (data.real_a + feat.theta ());
		double			cy = data.real_y + feat.rho () * Math.sin (data.real_a + feat.theta ());
		double			cz = feat.z ();
		Point3d			eye = new Point3d (cx, cy, cz);
		Point3d			at = new Point3d (cx + Math.cos (yaw) * Math.cos (pitch),
										  cy + Math.sin (yaw) * Math.cos (pitch),
										  cz + Math.sin (pitch));
		Transform3D		cam = new Transform3D ();

		cam.lookAt (eye, at, new Vector3d (0.0, 0.0, 1.0));
		cam.invert ();													// lookAt looks the other way round
		universe.getViewingPlatform ().getViewPlatformTransform ().setTransform (cam);
	}

	/** Lets go of the universe it renders with. */
	public void dispose ()
	{
		try { universe.cleanup (); } catch (Throwable t) { }
	}

	public String toString ()
	{
		return "camera" + dev + " of robot " + robot + " [" + width () + "x" + height ()
				+ " hfov=" + (int) Math.toDegrees (hfov) + " vfov=" + (int) Math.toDegrees (vfov)
				+ " " + (float) framerate () + "fps]";
	}
}
