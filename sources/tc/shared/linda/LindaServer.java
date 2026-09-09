/*
  * (c) 2002-2004 Humberto Martinez
 */

package tc.shared.linda;

import tc.shared.linda.local.*;
import tc.shared.linda.net.*;
import tc.shared.linda.gui.*;

public class LindaServer implements Linda, LindaNetProcessor
{
	// Linda servers
	protected LindaNetUDP    				userver;
	protected LindaNetTCP    				tserver;

	// Linda server implementation
	protected LindaSpace						space;
	protected LindaServerWindow				window;
	private boolean							debug		= false;

	// Constructors
	public LindaServer (int port) 
	{		
		// Create remote accessors
		if (port > 0)
		{
			userver	= new LindaNetUDP (port, this);
			tserver	= new LindaNetTCP (port, this);
		}
		
		// Create local data structures
		space	= new LindaSpace ();
		if (debug)
		{
			window	= new LindaServerWindow (toString (), space);
			window.open ();			
		}
	}
		
	// Accessors
	public final int		port ()				{ return userver.getLocalPort (); }
	
	// Instance methods
	public void stop ()
	{
		if (userver != null)		userver.close ();
		if (tserver != null)		tserver.close ();
		if (window != null)			window.close ();
	}
	
	public synchronized void process (LindaNet linda, LindaNetPacket packet)
	{
		switch (packet.getCommand ()) 
		{
		case LindaNetPacket.CMD_WRITE: 
			space.write (packet.getTuple (), packet.getConnection ());
			break;

		case LindaNetPacket.CMD_READ:
			space.read (packet.getTuple (), new LindaNetListener (linda, packet.getConnection ()));
			break;

		case LindaNetPacket.CMD_TAKE:
			space.take (packet.getTuple (), new LindaNetListener (linda, packet.getConnection ()));
			break;

		case LindaNetPacket.CMD_REGISTER:
			space.register (packet.getTuple (), new LindaNetListener (linda, packet.getConnection ()));
			break;
			
		case LindaNetPacket.CMD_UNREGISTER:
			space.unregister (packet.getTuple (), new LindaNetListener (linda, packet.getConnection ()));
			break;

		default:
		}
	}

	public boolean write (Tuple tuple)
	{
		return space.write (tuple, null);
	}

	public Tuple take (Tuple template)
	{
		return space.take (template, null);
	}

	public Tuple read (Tuple template)
	{
		return space.read (template, null);
	}

	public void register (Tuple template, LindaListener listener)
	{
		space.register (template, new LindaLocalListener (listener));
	}
	
	public void unregister (Tuple template, LindaListener listener)
	{
		space.unregister (template, new LindaLocalListener (listener));
	}
	
	public void manage(int command,LindaNetListener con,String robotid){
		ItemDelRobot 	dritem;
		Tuple			drtuple;
		
		switch(command){
			case LindaNetProcessor.DELETE:
//				Enviar tupla DELROBOT
				dritem 	= new ItemDelRobot();
				drtuple 	= new Tuple (Tuple.DELROBOT, dritem);
				
				dritem.set(ItemDelRobot.DELETE,robotid,System.currentTimeMillis());
				write(drtuple);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				dritem.set(ItemDelRobot.INFO,robotid,System.currentTimeMillis());
				write(drtuple);
//				Eliminar todas las conexiones con del LindaSpace
				space.unregister(con);
			
			break;
			default:
				System.out.println("  [LindaSpace] manage("+command+","+con+","+robotid+") Unknown command "+command);
			break;
		}
		
	}
	
	public String toString ()
	{
		String			str;
		
		str		= "[LOCAL";
		
		if (userver != null)		str += ",UDP:" + userver.getLocalPort ();
		if (tserver != null)		str += ",TCP:" + tserver.getLocalPort ();
		
		str 	+= "]";
		
		return str;
	}
}