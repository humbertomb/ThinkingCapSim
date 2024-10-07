/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera, Juan Pedro Canovas Qui–onero
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import java.io.*;

public class Item extends Object implements Serializable
{
	public Long						timestamp;		// Timestamp of the current update
	
	// Constructors
	public Item () 
	{
		set (0);
	}	
	
	// Initialisers
	public void set (long timestamp)
	{
		this.timestamp = new Long (timestamp);
	}
}
