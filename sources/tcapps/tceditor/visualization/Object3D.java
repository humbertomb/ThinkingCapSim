/*
 * Created on 07-dic-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcapps.tceditor.visualization;

import javax.media.j3d.*;
import javax.vecmath.*;

import wucore.utils.geom.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Object3D extends BranchGroup
{
	protected TransformGroup 			object;
	protected Transform3D 			transform;	
	protected boolean				visible = true;
	
	protected Vector3d				pos;
	/** The largest dimension of the object seen from above (m), in its own frame; 0 when it cannot be told. */
	public double					across;
	private Matrix3d					rot = new Matrix3d ();
	private Transform3D				mov = new Transform3D ();

	// Constructor
	public Object3D (TransformGroup obj, Point3 pt, double a)
	{
		// Initialise variables
		pos			= new Vector3d ();
		transform 	= new Transform3D ();
		object		= obj;
		across		= FloorName.footprint (obj);					// while it is still in its own frame
		
		// Set group properties
		setCapability (BranchGroup.ALLOW_CHILDREN_READ);
		setCapability (BranchGroup.ALLOW_DETACH);
		addChild (object);
		
		// Set object initial position
		move (pt, a);
	}
	
	// Instance methods
	public void move (Point3 pt, double a)
	{
		mov.setIdentity ();
		mov.rotZ (a);	
		mov.get (rot);
		
		pos.set (pt.x(), pt.y(), pt.z());
		
		transform.setIdentity ();
		transform.set (rot, pos, transform.getScale ());
		object.setTransform (transform);
	}
	
	/**
	 * @return Returns the visible.
	 */
	public boolean isVisible () {
		return visible;
	}
	/**
	 * @param visible The visible to set.
	 */
	public void setVisible (boolean visible) {
		this.visible = visible;
	}
}
