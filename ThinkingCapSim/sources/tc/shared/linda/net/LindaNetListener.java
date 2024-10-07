/*
 * Created on 09-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.linda.net;

import tc.shared.linda.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LindaNetListener implements LindaEntryListener
{
	protected LindaNet						channel;
	protected LindaNetConn					connection;

	// Constructors
	public LindaNetListener (LindaNet channel, LindaNetConn connection)
	{
		this.channel	= channel;
		this.connection	= new LindaNetConn (connection);
	}
	
	// Accessors
	public LindaConnection			getConnection ()		{ return connection; }
	
	// Instance methods
	public boolean notify (Tuple tuple, LindaConnection connection)
	{
//		if(tuple.key.equals("SYNC") && !tuple.space.equals("any")){
//			System.out.println("  [LindaNetListener] envia tuple "+tuple+" a "+connection );
//		}
		return channel.sendTuple ((LindaNetConn) connection, LindaNetPacket.CMD_EVENT, tuple);
	}
	
	public boolean answer (Tuple tuple, LindaConnection connection)
	{
		return channel.sendTuple ((LindaNetConn) connection, LindaNetPacket.CMD_ANSWER, tuple);
	}
	
	public boolean matches (LindaConnection connection)
	{
		if (connection instanceof LindaNetConn)
			return this.connection.isEqual ((LindaNetConn) connection);
		
		return false;
	}
	
	public String toLongString ()
	{
		return connection.toString () + " (" + channel.getClass ().getName () + "@" + Integer.toHexString (channel.hashCode ()) + ")";
	}
	
	public String toString ()
	{
		return connection.toString ();
	}
}
