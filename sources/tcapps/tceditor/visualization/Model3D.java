/*
 * (c) 2002 Humberto Martinez Barbera
 * (c) 2002-2003 Bernardo Canovas Segura
 * (c) 2004 Humberto Martinez Barbera
 */

package tcapps.tceditor.visualization;

import java.awt.*;
import javax.media.j3d.*;
import javax.swing.*;

import tc.shared.world.*;
import tc.vrobot.*;

import wucore.utils.geom.*;

import tcapps.tcsimulator.simulator.*;
import tcapps.tcsimulator.simulator.objects.*;

public class Model3D extends Scene3D
{
	protected static final int		MAXROBOTS = 10;
	protected static final int		MAXOBJECTS = 10;
		
	protected World 					map;
	protected Object3D[] 				objects;
	protected Robot3D[] 				robots;
	protected int 					numrobots = 0;
	protected int 					numobjects = 0;	
	protected BranchGroup 			bobjects;
	protected BranchGroup 			brobots;
	
	protected boolean 				sonarActive = false;
	protected boolean 				irActive = false;
	protected boolean 				laserActive = false;
	
	protected int 					focusOnRobot = -1;
	protected int 					focusOnObject = -1;
	
	/* Constructors */
	public Model3D (Canvas3D canvas)
	{
		super (canvas);
		
		brobots = new BranchGroup();
		brobots.setCapability(BranchGroup.ALLOW_DETACH);
		brobots.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		brobots.setCapability(BranchGroup.ALLOW_CHILDREN_READ);	
		brobots.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
				
		bobjects = new BranchGroup();
		bobjects.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		bobjects.setCapability(BranchGroup.ALLOW_CHILDREN_READ);	
		bobjects.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		bobjects.setCapability(BranchGroup.ALLOW_DETACH);
				
		// Update main scene
		scene.addChild (brobots);
		scene.addChild (bobjects);	
			
		robots	= new Robot3D[MAXROBOTS];
		objects	= new Object3D[MAXOBJECTS];
	}
		
	/* Adds a new robot at the start point of the world. If no world was added, it will be (0.0,0.0) *
	 * @return id of the robot in this world representation. Must be stored to actualize robot data correctly
	 */
	public int addRobot (RobotDesc rdesc, SimulatorDesc sdesc)
	{
		int			i;
		int			index;
		Point3		pt;
		double		a = 0.0;

		if (sdesc.V3DFILE == null)
			JOptionPane.showConfirmDialog (null, "The current robot has no associated 3D file.", "Fatal Error", JOptionPane.DEFAULT_OPTION);
		
		// Assign an index number to the robot
		index	= -1;
		for (i = 0; i < numrobots; i++)
			if (robots[i] == null)
				index = i;
		if (index == -1)
		{
			index	= numrobots;
			numrobots ++;
		}

		// Set-up default position
		pt	= new Point3 ();
		if (map != null)
		{
			pt.set (map.start_x(), map.start_y(), 0.0);
			a	= map.start_a ();
		}

		// Initialise object structures
		robots[index]	= new Robot3D (rdesc, getCachedObject (sdesc.V3DFILE, null), getCachedObject (sdesc.V3DLIFT, null), pt, 0.0, a);
		brobots.addChild (robots[index]);
		
		return index;
	}

	public void updateRobot (int index, RobotData data)
	{
		if ((index < 0) || (index >= numrobots))		return;
		
		robots[index].move (data, new Point3 (data.real_x,data.real_y,0.0), data.fork, data.real_a);
		if (focusOnRobot == index) 
		{ 
			focus.set(data.real_x,data.real_y,0.0);
			setViewpoint ();
		}
	}	

	/** Adds a new object at the indicated position *
	 * @return id of the object in this world representation. Must be stored to actualize object data correctly
	 */
	public int addObject (SimObject sobj, Point3 pt, double a)
	{
		int			i;
		int			index;
		Color	color = null;
		
		// Check if default coloring must be overriden
		if (sobj.odesc.usecolor)
			color 	= wucore.utils.color.ColorTool.fromWColorToColor(sobj.odesc.color);
			//color 	= sobj.odesc.color;

		// What it is drawn as: its 3D model, or, with no model and a picture of its
		// own, that picture lying flat over the ground its icon covers. An object
		// with neither is not in this view, and takes no place of it
		TransformGroup	shape = (sobj.odesc.shape != null) ? getCachedObject (sobj.odesc.shape, color)
															: getObjectImage (sobj.odesc);

		if (shape == null)				return -1;

		// Assign an index number to the object
		index	= -1;
		for (i = 0; i < numobjects; i++)
			if (objects[i] == null)
				index = i;
		if (index == -1)
		{
			index	= numobjects;
			numobjects ++;
		}
	
		// Initialise object structures
		objects[index] 	= new Object3D (shape, pt, a);		
		bobjects.addChild (objects[index]);		

		return index;
	}

	public void removeObject (int index)
	{
		if ((index < 0) || (index >= numobjects))		return;
		
		bobjects.removeChild (objects[index]);
		objects[index]	= null;
	}
	
	/** Moves the object indicated to the position and angle indicated */
	public void updateObject (int index, Point3 pt, double a)
	{
		if ((index < 0) || (index >= numobjects))		return;
		if (objects[index] == null)					return;
		
		objects[index].move (pt, a);
		if (focusOnObject == index)
		{
			focus.set (pt.x(),pt.y(),pt.z());
			setViewpoint ();
		}
	}

	/** Show or hide an object */
	public void showObject (int index, boolean show)
	{
		if ((index < 0) || (index >= numobjects))		return;

		objects[index].setVisible (show);
		if (objects[index].isVisible ())
			bobjects.addChild (objects[index]);		
		else
			objects[index].detach ();		
	}
	
	/** Removes all objects (leaving the robots) that were added to the scene */
	public void removeAllObjects ()
	{
		if (numobjects==0) return;
		
		bobjects.removeAllChildren ();
				
		for (int i=0;i<numobjects;i++)
			objects[i] = null;
		
		numobjects=0;
	}

	/** Adds a map to the world */
	public void addMap (World map)
	{
		double			xo, yo;

		this.map = map;

		// Set focus point
		double[]	b = map.bounds ();
		xo	= (b[0] + b[2]) * 0.5;
		yo	= (b[1] + b[3]) * 0.5;
		focus.set (xo, yo, 0.0);
		
		scene.addChild (new World3D (map, this));
	}
	
	/** Show or hide the ir sensor cones */
	public void showIr(boolean show)
	{
		int			i;
		
		irActive = show;
		for (i = 0; i < numrobots; i++)
			robots[i].showIr (show);
	}
	
	/** Show or hide the sonar sensor cones */
	public void showSonar(boolean show)
	{
		int			i;
		
		sonarActive = show;
		for (i = 0; i < numrobots; i++)
			robots[i].showSonar (show);
	}
	
	/** Show or hide the sonar sensor cones */
	public void showLaser(boolean show)
	{
		int			i;
		
		laserActive = show;
		for (i = 0; i < numrobots; i++)
			robots[i].showLaser (show);
	}
	
	/** Returns true if the ir sensor cones are been displayed */
	public boolean isIrActivated()
	{
		return irActive;
	}
	
	/** Returns true if the sonar sensor cones are been displayed */
	public boolean isSonarActivated()
	{
		return sonarActive;
	}
	
	/** Returns true if the sonar sensor cones are been displayed */
	public boolean isLaserActivated()
	{
		return laserActive;
	}
	
	/** Indicates id of robot to focus on. -1 indicates no autofocus */
	public void focusOnRobot(int id)
	{
		focusOnRobot = id;
	}
	
	/** Indicates id of object to focus on. -1 indicates no autofocus */
	public void focusOnObject(int id)
	{
		focusOnObject = id;
	}
	
	/** Returns the robot id's that is being focused. 
	 Returns -1 if no robot is being focused */
	public int focusedRobot()
	{
		return focusOnRobot;
	}
	
	/** Returns the object id's that is being focused.
	 Returns -1 if no object is being focused */
	public int focusedObject()
	{
		return focusOnObject;
	}
}