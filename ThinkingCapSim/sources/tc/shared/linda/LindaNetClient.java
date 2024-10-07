/*
 * (c) 2001-2004 Humberto Martinez
 */

package tc.shared.linda;

import java.net.*;
import java.util.*;

import tc.shared.linda.net.*;

public class LindaNetClient implements Linda, LindaNetProcessor
{
	static protected final int		MAX_KEYS 	= 100;
	static protected final long		TIMEOUT		= 200; 		// Reception timeout (ms)

	// Linda connection data and control
	protected Hashtable				listeners 	= null;	
	protected LindaNet				lclient;
	protected Tuple					ltuple;
	protected Integer				lock;
	protected boolean				answered;
	
	protected String					sourceid;				// Source identification for outgoing messages
	
	// Constructors
	public LindaNetClient (int mode, String sourceid, String addr, int port)
	{
		InetAddress		inet;
		
		this.sourceid	= sourceid;
		
		// Set up local variables
		listeners 	= new Hashtable (MAX_KEYS);
		lock			= new Integer (0);
		answered		= false;

		// Resolve server address 
		inet			= null;
		try
		{
			inet	= InetAddress.getByName (addr);
		} catch (Exception e) { e.printStackTrace (); }
		
		if (inet != null)
		{
			// Stablish connection with server
			switch (mode)
			{
			case LindaNet.TCP:
				lclient = new LindaNetTCP (inet, port, this);
				break;
				
			case LindaNet.UDP:
			default:
				lclient = new LindaNetUDP (inet, port, this);
			}
		}
	}
		
	// Instance methods
	public int getLocalPort ()
	{
		return lclient.getLocalPort ();
	}
	
	public boolean write (Tuple tuple) 
	{
		// Set default source id
		if (sourceid != null)
			tuple.space = sourceid;
		
		return lclient.sendTuple (null, LindaNetPacket.CMD_WRITE, tuple);
	}

	public Tuple read (Tuple template) 
	{
		Tuple			tuple = null;
		
		lclient.sendTuple (null, LindaNetPacket.CMD_READ, template);
		
		synchronized (lock)
		{
			if (!answered)
				try { lock.wait (TIMEOUT); } catch (Exception e) { }
			
			if (answered)
			{
				tuple		= ltuple;
				answered	= false;
			}
		}

		return tuple;
	}

	public Tuple take (Tuple template) 
	{
		Tuple			tuple = null;
		
		lclient.sendTuple (null, LindaNetPacket.CMD_TAKE, template);

		synchronized (lock)
		{
			if (!answered)
				try { lock.wait (TIMEOUT); } catch (Exception e) { }
			
			if (answered)
			{
				tuple		= ltuple;
				answered	= false;
			}
		}

		return tuple;
	}

	public void register (Tuple template, LindaListener listener) 
	{
		listeners.put (template.key, listener);
		lclient.sendTuple (null, LindaNetPacket.CMD_REGISTER, template);
	}
	
	public void unregister (Tuple template, LindaListener listener) 
	{
		listeners.remove (template.key);
		lclient.sendTuple (null, LindaNetPacket.CMD_UNREGISTER, template);
	}
	
	public void process (LindaNet linda, LindaNetPacket packet)
	{
		switch (packet.getCommand ()) 
		{
		case LindaNetPacket.CMD_ANSWER:
			synchronized (lock)
			{
				ltuple		= packet.getTuple ();
				answered	= true;			
				lock.notify ();
			}
			break;
		
		case LindaNetPacket.CMD_EVENT:
			LindaListener	listener;
			Tuple			tuple;

			tuple		= packet.getTuple ();
			listener	= (LindaListener) listeners.get (tuple.key);
			
			if (listener != null) 
				listener.notify (tuple);
			break;
		
		default:
			System.out.println ("--[LindaNetClient] Wrong packet command <" + LindaNetPacket.cmdToString (packet.getCommand ()) + "> received");
		}
	}
	
	public void stop ()
	{
		lclient.close ();
	}
	
	public void manage(int command,LindaNetListener con,String robotid){
		System.out.println("LindaNetClient manage("+command+","+con+","+robotid+")");
	}
}