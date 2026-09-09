/*
 * Created on 04-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.ingenia.ifork.linda;

import java.io.*;

import tc.shared.linda.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ItemSync extends Item implements Serializable
{
	// Values
	public static final int			VOID			= 50;
	public static final int		 	WAITING		= 51;
	public static final int		 	CONTINUE		= 52;
	public static final int			AGVIN		= 53;
	public static final int		 	BYE			= 66;		
	
	
	public int 		message;
	public String	dockid;
	
	
	// Constructors
	public ItemSync () 
	{
		this.set (VOID, "", 0);
	}	
	
	// Initialisers
	public void set (int message, String dockid, long tstamp)
	{
		set (tstamp);
		
		this.message		= message;
		this.dockid			= dockid;
	}
	
	public String toString ()
	{
		String			str = "null";
		
		switch (message)
		{
		case VOID:
			str = "VOID";
			break;
		case WAITING:
			str = "WAITING on "+dockid;
			break;
		case CONTINUE:
			str = "CONTINUE on "+dockid;
			break;
		case AGVIN:
			str = "AGVIN on "+dockid;
			break;
		case BYE:
			str = "BYE on "+dockid;
			break;
		default:
		}
		
		return str;
	}
}
