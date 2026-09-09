/*
 * Created on 08-dic-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcapps.tcsim.simul.objects;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SimCargo extends SimObject
{
	protected double 		distance;			// Distance from robot center to cargo
	protected boolean		picked = false;
	protected String 		namepallet;

	public SimCargo (String fname)
	{
		super (fname);	
	}
	
	/** Returns true if the object has been picked by a robot */
	public boolean isPicked ()
	{
		return picked;
	}
	
	/** Sets the object state to "picked" */
	public void pick (double x, double y, double z, double a)
	{
		double			dx, dy;
		
		picked = true;
		
		odesc.pos.z (z);
		dx = odesc.pos.x () - x;
		dy = odesc.pos.y () - y;
		distance = Math.sqrt(dx*dx+dy*dy);
	}

	/** Sets the object state to "not picked" */
	public void drop (double z)
	{
		picked = false;
		odesc.pos.z (z);
	}

	/** Moves the object when has been picked by a robot */
	public void move (double x, double y, double z, double a)
	{
		double			dx, dy;

		if (!picked)				return;
		
		dx = distance*Math.cos(a);
		dy = distance*Math.sin(a);
		odesc.pos.x (x-dx);
		odesc.pos.y (y-dy);
		odesc.pos.z (z);
		odesc.a = a;
	}
	public void setNamePallet(String namepallet){
		this.namepallet=namepallet;
	}
}
