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
public class LindaLocalListener implements LindaEntryListener
{
	protected LindaLocalConn				connection;

	// Constructors
	public LindaLocalListener (LindaListener listener)
	{
		this.connection	= new LindaLocalConn (listener);	
	}
	
	public LindaLocalListener (LindaLocalConn connection)
	{
		this.connection	= connection;	
	}
	
	// Accessors
	public LindaConnection			getConnection ()		{ return connection; }
	
	// Instance methods
	public boolean notify (Tuple tuple, LindaConnection connection)
	{
		this.connection.shared.notify (tuple);
		
		return true;
	}
	
	public boolean answer (Tuple tuple, LindaConnection connection)
	{
		this.connection.shared.notify (tuple);
		
		return true;
	}
	
	public boolean matches (LindaConnection connection)
	{
		if (connection instanceof LindaLocalConn)
			return this.connection.isEqual ((LindaLocalConn) connection);
		
		return false;
	}
	
	public String toString ()
	{
		return connection.toString ();
	}
}
