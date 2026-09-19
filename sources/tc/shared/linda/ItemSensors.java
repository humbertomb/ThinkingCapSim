/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import java.io.*;
import tc.vrobot.*;

public class ItemSensors extends Item implements Serializable
{
	public RobotData				data;
	
	// Constructors
	public ItemSensors () 
	{
		this.set (0);
	}	
	
	// Instance methods
	public void set (RobotData data, long tstamp)
	{
		set (tstamp);
		this.data = data;
	}
	
	public String toString ()
	{
		return data.toString ();
	}	
}
