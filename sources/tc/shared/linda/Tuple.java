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
	public static final String			EXECUTION		= "EXECUTION";

	// Key values (data structures)
	public static final String			SENSORS			= "SENSORS";
	public static final String			SENSORS_CTRL	= "SENSORS_CTRL";
	public static final String			CONFIG			= "CONFIG";
	public static final String			LPS				= "LPS";
	public static final String			STATUS			= "STATUS";
	public static final String			OBJECT			= "OBJECT";
	public static final String			CAMERA			= "CAMERA";
	public static final String			CAMERA_CTRL		= "CAMERA_CTRL";
	
	// Key values (behaviours)
	public static final String			BEHRESULT		= "BEHRESULT";
	public static final String			BEHINFO 		= "BEHINFO";
	public static final String			BEHRULES		= "BEHRULES";
	public static final String			BEHNAME			= "BEHNAME";
	public static final String 			BEHDEBUG		= "BEHDEBUG";

	// Key values (actions)
	public static final String			MOTION			= "MOTION";
	public static final String			GOAL			= "GOAL";
	public static final String			NAVIGATION		= "NAVIGATION";
	public static final String			PLAN			= "PLAN";
	public static final String			PATH			= "PATH";
	
	// Instance variables
	public String						space			= LindaEntryFilter.ANY;
	public String						key;
	public Item							value			= null;
		
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
