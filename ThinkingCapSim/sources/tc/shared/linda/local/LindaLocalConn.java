/*
 * Created on 09-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.linda.local;

import tc.shared.linda.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LindaLocalConn extends LindaConnection
{
	protected LindaListener				shared;
	
	// Constructors
	public LindaLocalConn (LindaListener shared)
	{
		this.shared		= shared;
	}
	
	// Instance methods
	public boolean isEqual (LindaLocalConn remote)
	{
		return shared.equals (remote.shared);
	}
	
	public String toString ()
	{
		return "shared://" + shared.getClass ().getName () + "@" + Integer.toHexString (shared.hashCode ());
	}
}
