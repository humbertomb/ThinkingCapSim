/*
 * Created on 07-dic-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcapps.tcsim.gui.visualization.objects;

import javax.media.j3d.*;
import javax.vecmath.*;

import tc.vrobot.*;
import tcapps.tcsim.gui.visualization.Color3D;
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

	protected boolean 				sonarActive = false;
	protected boolean 				irActive = false;
	protected boolean 				laserActive = false;

	protected Vector3d				pos;
	protected Vector3d				lpos;
	private Matrix3d					rot = new Matrix3d ();
	private Transform3D				mov = new Transform3D ();

	// Constructors
	public Robot3D (RobotDesc rdesc, TransformGroup ro, TransformGroup rl, Point3 pt, double fhgt, double a)
	{
		pos		= new Vector3d (pt.x(), pt.y(), pt.z());
		lpos		= new Vector3d (pt.x(), pt.y(), fhgt);

		setCapability (BranchGroup.ALLOW_DETACH);
		setCapability (BranchGroup.ALLOW_CHILDREN_EXTEND);
		setCapability (BranchGroup.ALLOW_CHILDREN_READ);	
		setCapability (BranchGroup.ALLOW_CHILDREN_WRITE);

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

		// Create sensors structures
		sonars	= new Range3D (rdesc.sonfeat, rdesc.CONESON, Color3D.yellow, rdesc.MAXSONAR);
		irs		= new Range3D (rdesc.irfeat, rdesc.CONEIR, Color3D.orange, rdesc.MAXIR);
		lasers	= new Scan3D (rdesc.lrffeat, rdesc.CONELRF, rdesc.RAYLRF, Color3D.blue, rdesc.MAXLRF);
	}
	
	// Instance methods
	public void move (RobotData data, Point3 pt, double hl, double a)
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
		
		if (sonarActive)		sonars.move (data.sonars, pt, a);
		if (irActive)		irs.move (data.irs, pt, a);
		if (laserActive)		lasers.move (data.lrfs, pt, a);
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
