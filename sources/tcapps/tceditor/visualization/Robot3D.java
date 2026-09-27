/*
 * Created on 07-dic-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcapps.tceditor.visualization;

import javax.media.j3d.*;
import javax.vecmath.*;

import tc.vrobot.*;
import wucore.utils.geom.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Robot3D extends BranchGroup
{
	protected TransformGroup			robot;
	protected TransformGroup			lift;
	protected Transform3D				trobot;
	protected Transform3D				tlift;

	protected Range3D				irs;
	protected Range3D				sonars;
	protected Scan3D					lasers;
	protected Camera3D				cameras;			// what its cameras see, turned as they are (null: it has none)

	protected boolean				camerasShown = true;	// whether the prisms of the cameras are on the scene
	protected boolean 				sonarActive = false;
	protected boolean 				irActive = false;
	protected boolean 				laserActive = false;

	protected Vector3d				pos;
	protected Vector3d				lpos;
	protected TransformGroup			label;				// robot name floating above the robot (null when unnamed)
	protected Transform3D				tlabel;
	static public final double		LABEL_GAP		= 0.25;	// m between the top of the object and its name
	static public final double		LABEL_HEIGHT	= 2.2;	// m above the floor when the model height is unknown
	/** The name of a robot lies on the floor, this much over it so that it is not lost in it (m). */
	static public final double		NAME_Z			= 0.03;
	/** How wide the name of a robot may be, as a share of the largest dimension of the robot seen from above. */
	static public final double		NAME_SHARE		= 2.0;
	protected double				labelHeight		= LABEL_HEIGHT;
	protected double				nameScale		= 1.0;	// how much the name is shrunk to fit the robot
	protected double				nameW, nameH;			// the name as drawn (m), before shrinking
	private Matrix3d					rot = new Matrix3d ();
	private Transform3D				mov = new Transform3D ();

	// Constructors
	public Robot3D (RobotDesc rdesc, TransformGroup ro, TransformGroup rl, Point3 pt, double fhgt, double a)
	{
		this (rdesc, ro, rl, pt, fhgt, a, null);
	}

	/** Height at which a name floats over a 3D object: just above its top ({@link Scene3D#height}). */
	static public double labelHeight (Node... parts)
	{
		double	h = 0.0;
		for (Node n : parts)
			if (n != null)		h = Math.max (h, Scene3D.height (n));
		return (h > 0.0) ? h + LABEL_GAP : LABEL_HEIGHT;
	}

	/** @param name  robot name shown above the robot (null: none) */
	public Robot3D (RobotDesc rdesc, TransformGroup ro, TransformGroup rl, Point3 pt, double fhgt, double a, String name)
	{
		pos		= new Vector3d (pt.x(), pt.y(), pt.z());
		lpos		= new Vector3d (pt.x(), pt.y(), fhgt);

		setCapability (BranchGroup.ALLOW_DETACH);
		setCapability (BranchGroup.ALLOW_CHILDREN_EXTEND);
		setCapability (BranchGroup.ALLOW_CHILDREN_READ);	
		setCapability (BranchGroup.ALLOW_CHILDREN_WRITE);

		// how big the robot is seen from above, while its parts are still in its own frame
		double	foot = footprint (ro, rl);

		// Create robot's frame structures
		trobot 	= new Transform3D ();
		trobot.setIdentity ();
		trobot.rotZ (a);
		trobot.set (pos);
		
		robot	= ro;
		robot.setTransform (trobot);
		addChild (robot);

		// Create robot's lift structures
		if (rl != null)
		{
			tlift 	= new Transform3D ();
			tlift.setIdentity ();
			tlift.rotZ (a);
			tlift.set (pos);
			
			lift		= rl;
			lift.setTransform (tlift);
			addChild (lift);
		}

		// Robot name: flat text on the floor under the robot, centred on it and no
		// wider than NAME_SHARE times the robot seen from above
		if ((name != null) && (name.length () > 0))
		{
			com.sun.j3d.utils.geometry.Text2D	text = new com.sun.j3d.utils.geometry.Text2D (name, new Color3f (0.1f, 0.1f, 0.6f), "Application", 140, java.awt.Font.BOLD);
			double[]	size = size (text);
			double		across = (foot > 0.0) ? foot : 2.0 * Math.max (0.05, rdesc.RADIUS);

			nameW	= size[0];
			nameH	= size[1];
			if (nameW > 0.0)		nameScale = Math.min (1.0, NAME_SHARE * across / nameW);
			labelHeight	= labelHeight (ro, rl);
			tlabel	= new Transform3D ();
			label	= new TransformGroup ();
			label.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
			label.addChild (text);
			placeName (pt.x (), pt.y ());
			addChild (label);
		}

		// Create sensors structures
		sonars	= new Range3D (rdesc.sonfeat, rdesc.CONESON, Color3D.yellow, rdesc.MAXSONAR);
		irs		= new Range3D (rdesc.irfeat, rdesc.CONEIR, Color3D.orange, rdesc.MAXIR);
		lasers	= new Scan3D (rdesc.lrffeat, rdesc.CONELRF, rdesc.RAYLRF, Color3D.blue, rdesc.MAXLRF);

		// and what the cameras see, always shown: the prism of each one, turned with it
		if (rdesc.MAXCAMERA > 0)
		{
			cameras	= new Camera3D (rdesc, pt, a);
			if (cameras.count () > 0)		addChild (cameras);
			else							cameras = null;
		}
	}
	
	/** The name on the floor, centred under (x, y), the size it was given. */
	protected void placeName (double x, double y)
	{
		tlabel.setIdentity ();
		tlabel.setScale (nameScale);
		tlabel.setTranslation (new Vector3d (x - nameScale * nameW / 2.0, y - nameScale * nameH / 2.0, NAME_Z));
		label.setTransform (tlabel);
	}

	/** How wide and how tall a flat text is drawn (m), from its bounds. */
	static protected double[] size (Node text)
	{
		try
		{
			BoundingBox	box = new BoundingBox (text.getBounds ());
			Point3d		lo = new Point3d (), up = new Point3d ();

			box.getLower (lo);
			box.getUpper (up);
			if ((up.x > lo.x) && (up.y > lo.y))		return new double[] { up.x - lo.x, up.y - lo.y };
		}
		catch (Exception e)		{ }
		return new double[] { 0.0, 0.0 };
	}

	/**
	 * The largest dimension of the robot seen from above (m): the longer side of
	 * the box round its parts, before they are placed anywhere. Zero when unknown.
	 */
	static protected double footprint (Node... parts)
	{
		double	d = 0.0;

		for (Node n : parts)
		{
			if (n == null)		continue;
			try
			{
				BoundingBox	box = new BoundingBox (n.getBounds ());
				Point3d		lo = new Point3d (), up = new Point3d ();

				box.getLower (lo);
				box.getUpper (up);
				if ((up.x > lo.x) && (up.y > lo.y))		d = Math.max (d, Math.max (up.x - lo.x, up.y - lo.y));
			}
			catch (Exception e)		{ }
		}
		return d;
	}

	// Instance methods
	public void move (RobotData data, Point3 pt, double hl, double a)
	{
		move (data, pt, hl, a, null, null);
	}

	/**
	 * Moves the robot to a pose, with its cameras turned as they are now: pan
	 * and tilt of each (rad), or null to leave them as they were.
	 */
	public void move (RobotData data, Point3 pt, double hl, double a, double[] pans, double[] tilts)
	{
		mov.setIdentity ();
		mov.rotZ (a);	
		mov.get (rot);
		pos.set (pt.x(), pt.y(), pt.z());
		lpos.set (pt.x(), pt.y(), hl);
		
		trobot.setIdentity ();
		trobot.set (rot, pos, trobot.getScale ());
		robot.setTransform (trobot);
		
		if (lift != null)
		{
			tlift.setIdentity ();
			tlift.set (rot, lpos, tlift.getScale ());
			lift.setTransform (tlift);
		}
		if (label != null)		placeName (pt.x (), pt.y ());
		
		if (sonarActive)		sonars.move (data.sonars, pt, a);
		if (irActive)		irs.move (data.irs, pt, a);
		if (laserActive)		lasers.move (data.lrfs, pt, a);
		if (cameras != null)	cameras.move (pt, a, pans, tilts);
	}	
		
	/** Whether what the cameras see (the prism of each) is drawn. */
	public void showCameras (boolean show)
	{
		if ((cameras == null) || (camerasShown == show))		return;
		camerasShown	= show;
		if (show)		addChild (cameras);
		else			cameras.detach ();
	}

	/** The outer walls of the world the prisms of the cameras are cut at (xmin, ymin, xmax, ymax; null: none). */
	public void setCameraBounds (double[] bounds)
	{
		if (cameras != null)		cameras.setBounds (bounds);
	}

	public void showLaser (boolean show)
	{
		laserActive = show;
		
		if (laserActive)
			addChild (lasers);
		else
			lasers.detach ();
	}
	
	public void showSonar (boolean show)
	{
		sonarActive = show;
		
		if (sonarActive)
			addChild (sonars);
		else
			sonars.detach ();
	}

	public void showIr (boolean show)
	{
		irActive = show;
		
		if (irActive)
			addChild (irs);
		else
			irs.detach ();
	}
}
