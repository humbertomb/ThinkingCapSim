/*
 * (c) 2001 Humberto Martinez, Jose Ma Garcia Nieto
 * (c) 2002-2004 Humberto Martinez
 */

package tc.shared.linda.net;

import java.net.*;

import tc.shared.linda.*;

public abstract class LindaNet
{
	// Communication modes
	static public final int			UDP			= 0;
	static public final int			TCP			= 1;

	// Linda connection data and control
	protected LindaNetPacket			packet;			// Communication packetizer
	protected LindaNetProcessor		processor;		// Processor to dispatch to the packets
	
	// Constructors
	public LindaNet (InetAddress server, int port, LindaNetProcessor processor)
	{
		this.processor	= processor;
		
		client_socket (server, port);
	}

	public LindaNet (int port, LindaNetProcessor processor)
	{
		this.processor	= processor;
		
		server_socket (port);
	}

	// Class methods
	static public final String modeToString (int mode)
	{
		switch (mode)
		{
		case UDP:			return "udp";
		case TCP:			return "tcp";
		default:				return "N/A";
		}
	}
	
	// Abstract methods
	protected abstract void				client_socket (InetAddress peer, int port);
	protected abstract void				server_socket (int port);
	public abstract void 				close ();
	
	public abstract boolean				sendTuple (LindaNetConn conn, int command, Tuple tuple);	
	
	public abstract InetAddress			getLocalAddress ();
	public abstract int 					getLocalPort ();
}