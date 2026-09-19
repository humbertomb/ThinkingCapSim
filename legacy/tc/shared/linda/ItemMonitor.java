/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import java.io.*;

import tc.modules.*;
import tc.shared.lps.lpo.*;

public class ItemMonitor extends Item implements Serializable
{	
	// Signal-level and feature-level sensor fusion
	public MonitorData				data;

	// Local perceptual objects
	public LPO[]					lpos;
	
	// Constructors
	public ItemMonitor () 
	{
		this.set (null, null, 0);
	}	
	
	// Instance methods
	public void set (MonitorData data, LPO[] lpos, long tstamp)
	{
		int			i, k;
		int			n;
		
		this.set (tstamp);
		this.data	= data;
				
		// Assign LPOs
		if (lpos == null)
			this.lpos	= new LPO[0];
		else
		{
			n	= 0;
			for (i = 0; i < lpos.length; i++)
				if ((lpos[i] != null) && (lpos[i].active ()))
					n++;
					
			this.lpos	= new LPO[n];
			
			for (i = 0, k = 0; i < lpos.length; i++)
				if ((lpos[i] != null) && (lpos[i].active ()))
				{
					this.lpos[k] = lpos[i];
					k ++;
				}
		}
	}	
	
	public String toString ()
	{
		return data.toString ();
	}	
}
