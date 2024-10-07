/*
 * (c) 2002 Humberto Martinez
 */
 
package tc.shared.linda;

import java.io.*;

public class ItemLindaCtrl extends Item implements Serializable
{
	// Control commands
	public static final int		 	DELETE			= 0;
	public static final int		 	TIMEOUT			= 1;
	public static final int		 	DUMPREG			= 2;	
	public static final int		 	DUMPSPC			= 3;	
	
	// Linda control actions
	public int						cmd;				// Linda control command
	public long						param;				// Command parameter

	// Constructors
	public ItemLindaCtrl () 
	{
		this.set (DUMPREG, 0);
	}	
	
	// Initialisers
	public void set (int cmd, long tstamp)
	{
		set (cmd, 0, tstamp);
	}
	
	public void set (int cmd, long param, long tstamp)
	{
		set (tstamp);
		
		this.cmd		= cmd;
		this.param		= param;
	}
	
	public String toString ()
	{
		String			str = "null";
		
		switch (cmd)
		{
		case DELETE:
			str = "DELETE";
			break;
		case TIMEOUT:
			str = "TIMEOUT=" + param;
			break;
		case DUMPREG:
			str = "DUMP_REGISTRY";
			break;
		case DUMPSPC:
			str = "DUMP_SPACE";
			break;
		default:
		}
		
		return str;
	}	
}
