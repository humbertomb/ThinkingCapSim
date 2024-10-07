/*
 * (c) 2004 Humberto Martinez
 */
 
package tcrob.ingenia.ifork.linda;

import java.io.*;

import tc.shared.linda.*;

public class ItemIForkMotion extends ItemMotion implements Serializable
{
	// Tristate actions
	static public final int		 	TS_NONE		= 0;	
	static public final int		 	TS_DISABLE	= 1;	
	static public final int		 	TS_ENABLE	= 2;	

	// Fork control actions
	static public final int		 	FRK_NONE		= 0;
	static public final int		 	FRK_LOAD		= 1;
	static public final int		 	FRK_UNLOAD	= 2;
	static public final int		 	FRK_ADJUST	= 3;
	
	// Fork height control
	public double					frk_height;			// Desired fork Height (m)
	public int						frk_action;			// Current fork action
	
	// Other control actions (tristate)
	public int						horn;				// Current horn action
	public int						lgt_coord;			// Current coordination light action
	
	// Constructors
	public ItemIForkMotion () 
	{
		this.horn			= TS_NONE;
		this.lgt_coord		= TS_NONE;
		this.frk_height		= 0.0;
		this.set (0.0, 0.0, 0);
	}	
	
	// Class methods
	static public String actionToString (int action)
	{
		switch (action)
		{
		case FRK_NONE:		return "N/A";
		case FRK_LOAD:		return "Loading";
		case FRK_UNLOAD:		return "Unloading";
		case FRK_ADJUST:		return "Adjusting";
		default:				return "Error";
		}
	}

	static public String tristToString (int action)
	{
		switch (action)
		{
		case TS_NONE:		return "N/A";
		case TS_DISABLE:		return "Disable";
		case TS_ENABLE:		return "Enable";
		default:				return "Error";
		}
	}

	// Accessors
	public void	setHorn (int horn) 				{ this.horn = horn; }
	public void	setCoordLight (int lgt_coord) 	{ this.lgt_coord = lgt_coord; }
		
	// Instance methods
	public void setFork (double fork, int action)
	{
		this.frk_height		= fork;
		this.frk_action		= action;
	}
	
	public String toString ()
	{
		String			str;
		
		str	= super.toString ();
		
		if (frk_action != FRK_NONE)		str	+= ", fork=" + frk_height+"/"+actionToString (frk_action);
		if (horn != TS_NONE)				str	+= ", horn=" + tristToString (horn);
		if (lgt_coord != TS_NONE)			str	+= ", coord_light=" + tristToString (lgt_coord);
		
		return str;
	}	
}
