/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import java.io.*;
import tc.vrobot.*;

public class ItemSensorsCtrl extends Item implements Serializable
{
	public RobotDataCtrl				data_ctrl;
	
	// Constructors
	public ItemSensorsCtrl () 
	{
		this.set (0);
	}	
	
	// Instance methods
	public void set (RobotDataCtrl data_ctrl, long tstamp)
	{
		set (tstamp);
		this.data_ctrl	= data_ctrl;
	}

	public Item dup ()
	{
		ItemSensorsCtrl		item = null;
		
		try 
		{ 
			item = (ItemSensorsCtrl) this.clone (); 
			item.data_ctrl = data_ctrl.dup ();
		} catch (Exception e) { }
		
		return item;
	}

	public String toString ()
	{
		return data_ctrl.toString ();
	}	
}
