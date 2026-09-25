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
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import tc.vrobot.CameraCtrl;
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
	/**
	 * Pixels across of a frame when the description does not say how large one is;
	 * how many down then follows from the two fields of view.
	 */
	static public final int			WIDTH		= 320;
	/** As many pixels a side as a frame is ever given. */
	static public final int			MAX_SIDE	= 4096;
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
	protected double				pan, tilt;					// how it was turned from there (rad): about its vertical, then its horizontal
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

		hfov	= fov (rdesc.camhfov, dev, rdesc.CONECAM, DEF_HFOV);
		vfov	= fov (rdesc.camvfov, dev, rdesc.VFOVCAM, DEF_VFOV);
		// how large a frame is: what the description says, and otherwise as wide as
		// the simulator draws one and as tall as the two fields of view ask
		if ((rdesc.camwidth != null) && (dev < rdesc.camwidth.length) && (rdesc.camwidth[dev] > 0))
		{
			w	= rdesc.camwidth[dev];
			h	= rdesc.camheight[dev];
		}
		else
		{
			w	= WIDTH;
			h	= (int) Math.round (WIDTH * Math.tan (vfov / 2.0) / Math.tan (hfov / 2.0));
		}
		w		= Math.max (16, Math.min (MAX_SIDE, w));
		h		= Math.max (16, Math.min (MAX_SIDE, h));
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

		// What it sees: the two fields of view of the description, exactly.
		//
		// Not View.setFieldOfView, which says the horizontal one and leaves the
		// vertical one to be worked out of the size Java 3D takes the screen to be
		// physically -- something an off-screen canvas has no say in, and which came
		// out as a vertical field of view of well over a hundred degrees. In
		// compatibility mode the projection is ours to give, so both are what they
		// are said to be and the pixels are square.
		View		v = universe.getViewer ().getView ();
		Transform3D	proj = new Transform3D ();
		Transform3D	eye = new Transform3D ();

		v.setCompatibilityModeEnable (true);
		// both fields of view, whatever the shape of the frame: the aspect asked of
		// perspective is the one of the fields of view and not the one of the frame,
		// so a frame of a size that does not follow from them -- a description that
		// asks for 640x480 of a camera that sees 60 by 50 -- still sees what it says
		// it sees, with pixels that are not square, as such a camera has
		proj.perspective (hfov, Math.tan (hfov / 2.0) / Math.tan (vfov / 2.0), NEAR, FAR);
		v.setLeftProjection (proj);
		eye.setIdentity ();											// the eye sits on the platform of the view, looking down its -z
		v.setVpcToEc (eye);
		v.setFrontClipDistance (NEAR);
		v.setBackClipDistance (FAR);

		// the camera is carried by the platform of the view and not by the transform
		// of the scene, which is what Scene3D does and what this is instead of
		scene.setTransform (new Transform3D ());

		build ();
	}

	/**
	 * How wide this camera sees, as the description says (rad): what it says of
	 * itself, what the platform says of the whole family of them, or what a camera
	 * is taken to see when neither says.
	 */
	static private double fov (double[] own, int dev, double family, double none)
	{
		if ((own != null) && (dev >= 0) && (dev < own.length) && (own[dev] > 0.0))		return own[dev];
		return (family > 0.0) ? family : none;
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
	/** How far it has been panned from where the description points it, to the left (rad). */
	public final double			pan ()			{ return pan; }
	/** How far it has been tilted from where the description points it, upwards (rad). */
	public final double			tilt ()			{ return tilt; }

	/**
	 * Turns the camera on its mount, as a pan-tilt head does: the pan turns it about
	 * the vertical of the mount (to the left when positive, as every angle here),
	 * the tilt about the horizontal it is left with after the pan (upwards when
	 * positive, as the elevation of the description). Both are on top of where the
	 * description points the camera, which is where it is with none; null puts it
	 * back there. The frames taken from then on are of the turned camera.
	 *
	 * @param ctrl the pan and tilt asked for (rad), or null for the fixed position
	 */
	public void control (CameraCtrl ctrl)
	{
		pan		= ((ctrl != null) && Double.isFinite (ctrl.pan)) ? ctrl.pan : 0.0;
		tilt	= ((ctrl != null) && Double.isFinite (ctrl.tilt)) ? ctrl.tilt : 0.0;
	}
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
	 * looks: the robot carries it, so the pose of the robot is added to the one of
	 * the sensor -- the camera turns with the robot and sits where the description
	 * puts it, at its distance and angle from the centre and at its height. On top
	 * of that goes what the camera was turned on its mount ({@link #control}): the
	 * pan with the orientation, the tilt with the elevation, which is what a head
	 * that pans first and tilts then comes to.
	 *
	 * The frame of the view is built here rather than by {@link Transform3D#lookAt}
	 * and an inversion, which come to the same thing: said outright it is one
	 * transform instead of two, it says which way round the picture goes rather
	 * than leaving it to an up vector, and it has nothing to say about a camera
	 * looking straight up or down. A view looks down its own -z, with its x to the
	 * right of the picture and its y up, so that is what is built:
	 *
	 *   where it looks  f = (cos yaw cos pitch, sin yaw cos pitch, sin pitch)
	 *   to its right    r = (sin yaw, -cos yaw, 0)
	 *   and up          u = r x f
	 *
	 * and the transform of the platform is [r, u, -f] with the eye for a
	 * translation. The pitch is held short of straight up or down, where the up of
	 * the world says nothing about which way round the picture goes.
	 */
	protected void aim (RobotData data)
	{
		double			yaw = data.real_a + feat.orientation () + pan;
		double			pitch = Math.max (-MAX_PITCH, Math.min (MAX_PITCH, feat.elevation () + tilt));
		double			cx = data.real_x + feat.rho () * Math.cos (data.real_a + feat.theta ());
		double			cy = data.real_y + feat.rho () * Math.sin (data.real_a + feat.theta ());
		double			cz = feat.z ();
		Vector3d			f = new Vector3d (Math.cos (yaw) * Math.cos (pitch), Math.sin (yaw) * Math.cos (pitch), Math.sin (pitch));
		Vector3d			r = new Vector3d (Math.sin (yaw), -Math.cos (yaw), 0.0);
		Vector3d			u = new Vector3d ();
		Matrix3d			rot = new Matrix3d ();
		Transform3D		cam = new Transform3D ();

		u.cross (r, f);
		rot.setColumn (0, r);
		rot.setColumn (1, u);
		rot.setColumn (2, new Vector3d (-f.x, -f.y, -f.z));
		cam.set (rot, new Vector3d (cx, cy, cz), 1.0);
		universe.getViewingPlatform ().getViewPlatformTransform ().setTransform (cam);
	}

	/**
	 * Where the eye of the view is, where it looks and which way round the picture
	 * goes, in the world, as the platform of the view has it: {x, y, z} of the eye,
	 * then the way it looks, then its right and its up. What the picture was taken
	 * from, for whoever wants to check it against where the camera is.
	 */
	public double[] eye ()
	{
		Transform3D		t = new Transform3D ();
		Point3d			o = new Point3d (0.0, 0.0, 0.0);
		Point3d			f = new Point3d (0.0, 0.0, -1.0);			// a view looks down its own -z
		Point3d			r = new Point3d (1.0, 0.0, 0.0);			// with its x to the right of the picture
		Point3d			u = new Point3d (0.0, 1.0, 0.0);			// and its y up

		universe.getViewingPlatform ().getViewPlatformTransform ().getTransform (t);
		t.transform (o);	t.transform (f);	t.transform (r);	t.transform (u);
		return new double[] { o.x, o.y, o.z, f.x - o.x, f.y - o.y, f.z - o.z,
							  r.x - o.x, r.y - o.y, r.z - o.z, u.x - o.x, u.y - o.y, u.z - o.z };
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
				+ " " + (float) framerate () + "fps"
				+ (((pan != 0.0) || (tilt != 0.0)) ? (" pan=" + (int) Math.toDegrees (pan) + " tilt=" + (int) Math.toDegrees (tilt)) : "") + "]";
	}
}
