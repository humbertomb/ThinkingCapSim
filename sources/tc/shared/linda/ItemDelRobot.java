/* 
Title:			Thinking Cap 
Author:			Jose Antonio Marin y Sergio Aleman
Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import java.io.*;

public class ItemDelRobot extends Item implements Serializable
{	
	
//	 Control commands
	public static final int		 	INFO			= 0;
	public static final int		 	DELETE			= 1;
	
	
	// Linda control actions
	public String					robotid;
	public int						cmd;				// Linda control command
	
	
	// Constructors
	public ItemDelRobot () 
	{
		set (0);
		this.robotid	= null;
		this.cmd		=INFO;
	}	
	
	public ItemDelRobot (String robotid) 
	{
		this.set (INFO, robotid, 0);
	}	
	
	public ItemDelRobot (String robotid, long tstamp) 
	{
		this.set (INFO, robotid, tstamp);
	}	
	public ItemDelRobot (int cmd, String robotid, long tstamp) 
	{
		this.set (cmd, robotid, tstamp);
	}
	
	// Initialisers
	public void set (String robotid, long tstamp)
	{
		set (INFO, robotid, tstamp);
	}
	
	public void set (int cmd, String robotid, long tstamp)
	{
		set(tstamp);
		this.cmd=cmd;
		this.robotid=robotid;
	}
	
	public Item dup ()
	{
		ItemDelRobot		item = null;
		
		try { item = (ItemDelRobot) this.clone (); } catch (Exception e) {e.printStackTrace(); }
		return item;
	}
	
	public String toString ()
	{
		return "ItemDelRobot[cmd="+cmd+" robotid="+robotid+"]";
	}	
}