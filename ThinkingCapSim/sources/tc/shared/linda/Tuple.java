/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import java.io.*;

public class Tuple implements Serializable
{
	// Key values (general purpose)
	public static final String			DEBUG		= "DEBUG";
	public static final String			LINDACTRL	= "LINDACTRL";
	public static final String			GUISVC		= "GUISVC";
	public static final String			GUIDATA		= "GUIDATA";
	public static final String			GUICTRL		= "GUICTRL";
	public static final String			DELROBOT	= "DELROBOT";

	// Key values (data structures)
	public static final String			DATA		= "DATA";
	public static final String			CONFIG		= "CONFIG";
	public static final String			LPS			= "LPS";
	public static final String			VIDEO		= "VIDEO";
	public static final String			MONITOR		= "MONITOR";
	public static final String			STATUS		= "STATUS";
	public static final String			OBJECT		= "OBJECT";
	
	// Key values (behaviours)
	public static final String			BEHRESULT	= "BEHRESULT";
	public static final String			BEHINFO 	= "BEHINFO";
	public static final String			BEHRULES	= "BEHRULES";
	public static final String			BEHNAME		= "BEHNAME";
	public static final String 			BEHDEBUG	= "BEHDEBUG";

	// Key values (actions)
	public static final String			MOTION		= "MOTION";
	public static final String			GOAL		= "GOAL";
	public static final String			NAVIGATION	= "NAVIGATION";
	public static final String			PLAN		= "PLAN";
	public static final String			PATH		= "PATH";
	public static final String			CAMERA		= "CAMERA";

	// Key values (control of actions)
	public static final String			DATACTRL	= "DATACTRL";
	
	public static final String			PALLETCTRL	= "PALLETCTRL";

	// Instance variables
	public String						space		= LindaEntryFilter.ANY;
	public String						key;
	public Item							value		= null;
		
	// Constructors
	public Tuple (String key) 
	{
		this.key		= key;
	}	
	
	public Tuple (String key, Item value) 
	{
		this.key		= key;
		this.value	= value;
	}	
	
	public Tuple (String space, String key, Item value) 
	{
		set (space, key, value);
	}	
	
	// Initialisers
	public void set (String space, String key, Item value)
	{
		this.space	= space;
		this.key		= key;
		this.value	= value;
	}
	
	public String toString ()
	{
		return "Tuple: [" + space + ", " + key + ", " + value + "]";
	}
}
