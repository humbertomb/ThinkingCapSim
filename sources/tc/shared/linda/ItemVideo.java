/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import java.io.*;

public class ItemVideo extends Item implements Serializable
{	
	public String			url;
	
	// Constructors
	public ItemVideo () 
	{
		set (0);
		this.url	= null;
	}	
	
	public ItemVideo (String url) 
	{
		this.set (url, 0);
	}	
	
	public ItemVideo (String url, long tstamp) 
	{
		this.set (url, tstamp);
	}	
	
	// Initialisers
	public void set (String url, long tstamp)
	{
		set (tstamp);
		
		this.url	= url;
	}

	public Item dup ()
	{
		ItemVideo		item = null;
		
		try { item = (ItemVideo) this.clone (); } catch (Exception e) { }
		return item;
	}
	
	public String toString ()
	{
		return url;
	}	
}

