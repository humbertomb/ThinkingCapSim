/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import devices.pos.*;

public class ItemPath extends Item
{
	public Path						path;			// Path obtained from PathPlanner
	
	// Constructors
	public ItemPath () 
	{
	}	
	
	// Initialisers
	public void set (Path path, long tstamp)
	{
		set (tstamp);
		
		this.path	= path;
	}
	
	public String toString ()
	{
		return "path=" + path;
	}	
}
