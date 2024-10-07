/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import devices.pos.*;

public class ItemNavigation extends Item
{
	public Position					robot;			// Current corrected robot position (m, m, rad)
	
	// Constructors
	public ItemNavigation () 
	{
		robot	= new Position ();
	}	
	
	public ItemNavigation (Position robot, long tstamp) 
	{
		this  ();
		this.set (robot, tstamp);
	}	
	
	// Initialisers
	public void set (Position robot, long tstamp)
	{
		set (tstamp);
		
		this.robot.set (robot);
	}
	
	public String toString ()
	{
		return "robot=" + robot;
	}	
}
