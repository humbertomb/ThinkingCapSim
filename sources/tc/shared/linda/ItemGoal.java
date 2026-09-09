/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import tclib.planning.sequence.*;

public class ItemGoal extends Item
{	
	public Task						task;
		
	// Constructors
	public ItemGoal () 
	{
		task			= new Task ();
		task.task	= "NAVIGATE";	
	}	
	
	// Instance methods
	public void set (Task task, long tstamp)
	{
		set (tstamp);
		
		this.task.set (task);
	}

	public void set (double gx, double gy, long tstamp)
	{
		set (tstamp);
		
		task.task	= "NAVIGATE";
		task.tpos.set (gx, gy);
	}
	
	public String toString ()
	{
		return task.toString ();
	}
}

