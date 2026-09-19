/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

public class ItemExecution extends Item
{
	// Operation modes
	public static final int 		COMMAND		= 0;
	public static final int 		DEBUG		= 1;
	
	// Command types
	public static final int 		START		= 0;
	public static final int 		STOP		= 1;
	public static final int 		STEP		= 2;
	public static final int 		RESET		= 3;
	public static final int 		MANUAL		= 4;
	public static final int 		AUTO		= 5;
	
	// Operation selection
	public int						operation;

	// Control commands
	public int						command;
		
	// Debug commands
	public boolean					dbg_vrobot;
	public boolean					dbg_perception;
	public boolean					dbg_controller;
	public boolean					dbg_navigation;
	public boolean					dbg_planner;
	
	// Constructors
	public ItemExecution () 
	{
		this.command (STOP, 0);
	}	
	
	// Class methods
	static public String operToString (int operation)
	{
		switch (operation)
		{
		case COMMAND:	return "COMMAND";
		case DEBUG:		return "DEBUG";
		default:		return "N/A";
		}
	}
	
	static public String cmdToString (int command)
	{
		switch (command)
		{
		case START:		return "START";
		case STOP:		return "STOP";
		case STEP:		return "STEP";
		case RESET:		return "RESET";
		case MANUAL:	return "MANUAL";
		case AUTO:		return "AUTO";
		default:		return "N/A";
		}
	}
	
	// Instance methods
	public void command (int command, long tstamp)
	{
		set (tstamp);
		
		this.operation			= COMMAND;
		
		this.command			= command;
	}

	public void debug (boolean rob, boolean per, boolean con, boolean nav, boolean pla, long tstamp)
	{
		set (tstamp);
		
		this.operation			= DEBUG;
		
		this.dbg_vrobot			= rob;
		this.dbg_perception		= per;
		this.dbg_controller		= con;
		this.dbg_navigation		= nav;
		this.dbg_planner		= pla;
	}
	
	public String toString ()
	{
		return operToString (operation) + ", " + cmdToString (command);
	}	
}
