/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import java.io.*;

import tc.shared.lps.*;

public class ItemLPS extends Item implements Serializable
{	
	public LPS						lps;			// Pointer to the LPS
	
	// Constructors
	public ItemLPS () 
	{
		this.set (null, 0);
	}	
	
	// Instance methods
	public void set (LPS lps, long tstamp)
	{
		this.set (tstamp);
		this.lps	= lps;		
	}	
	
	public String toString ()
	{
		return lps.toString ();
	}	
}
