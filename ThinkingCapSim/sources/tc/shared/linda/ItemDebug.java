/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

public class ItemDebug extends Item
{
	// Operation modes
	public static final int 		COMMAND		= 0;
	public static final int 		DEBUG		= 1;
	public static final int 		MODE		= 2;
	
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
	
	// Ececution parameters commands
	public int						mode_vrobot;
	public int						mode_perception;
	public int						mode_controller;
	public int						mode_navigation;
	public int						mode_planner;
	
	// Debug commands
	public boolean					dbg_vrobot;
	public boolean					dbg_perception;
	public boolean					dbg_controller;
	public boolean					dbg_navigation;
	public boolean					dbg_planner;
	
	// Constructors
	public ItemDebug () 
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
		case MODE:		return "MODE";
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
	
	public void mode (int rob, int per, int con, int nav, int pla, long tstamp)
	{
		set (tstamp);
		
		this.operation			= MODE;

		this.mode_vrobot		= rob;
		this.mode_perception	= per;
		this.mode_controller	= con;
		this.mode_navigation	= nav;
		this.mode_planner		= pla;
	}

	
	public String toString ()
	{
		return operToString (operation) + ", " + cmdToString (command);
	}	
}
