/*
 * Created on 05-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.linda.net;

import java.net.*;

import tc.shared.linda.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LindaNetConn extends LindaConnection
{
	public int				mode;
	public InetAddress		peer;
	public int				port;
	
	// Constructors
	public LindaNetConn (LindaNetConn connection)
	{
		this (connection.mode, connection.peer, connection.port);
	}
		
	public LindaNetConn (int mode)
	{
		this.mode		= mode;
	}
	
	public LindaNetConn (int mode, InetAddress peer, int port)
	{
		this.mode		= mode;
		
		set (peer, port);
	}
	
	// Instance methods	
	public boolean isEqual (LindaNetConn remote)
	{
		return (mode == remote.mode) && peer.equals (remote.peer) && (port == remote.port);
	}
	
	public void set (InetAddress peer, int port)
	{
		this.peer		= peer;
		this.port		= port;
	}
		
	public String toString ()
	{
		return LindaNet.modeToString (mode) + "://" + peer + ":" + port;
	}
}
