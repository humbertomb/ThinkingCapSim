/*
 * (c) 2002 Humberto Martinez
 */
 
package tc.runtime.thread;

import java.lang.reflect.*;

import tc.shared.linda.*;
import tc.shared.linda.net.*;

public class ThreadDesc extends Object
{
	// Linda-based execution modes
	static public final int		M_SHARED	= 0;
	static public final int		M_UDP		= 1;
	static public final int		M_TCP		= 2;
	static public String[]		M_MODES		= { "shared", "udp", "tcp" };
	
	// Variables to control the processing mode
	public int					priority;			// Execution priority of the thread
	public boolean				passive;			// Run in pasive mode (event-activated execution) 
	public boolean				queued;				// Queue Linda tuples before processing them 
	public boolean				polled;				// Run in polled mode (polling the Linda space)
	public long					exectime;			// Activation time for active and polled mode (ms)
	public boolean				cangfx;				// Module can display graphics
	
	// Variables to help executing the modules
	public String				classn;				// Subclass of tc.runtime.thread.StdThread
	public String				preffix;			// Preffix for denoting the class (and parsing files)
	public String				info;				// Description of module's purpose
	public String				connects;			// Connection registration parameters
	public int					mode;				// Linda client execution mode
	
	// Runtime data
	protected int				port		= 0;	// Current local port (for UDP/TCP mode only)
	public String				robotid;			// Current robot name
	public StdThread			thread;				// Current execution thread
	public ModuleConfig			config;				// Configuration of the module (what it reads, and what it is given when started)
	
	// Constructors
	public ThreadDesc (String preffix, ModuleConfig config)
	{
		initialise (preffix, config);
	}
	
	// Class methods
	static public int parse_mode (String mode)
	{
		int			code = M_SHARED;
		
		if (mode == null)			return M_SHARED;
		
		if (mode.equals ("udp"))
			code	= M_UDP;
		if (mode.equals ("tcp"))
			code	= M_TCP;
		else if (mode.equals ("local"))
			code	= M_SHARED;
			
		return code;
	}
	
	// Instance methods
	protected void initialise (String preffix, ModuleConfig config)
	{
		this.preffix	= preffix;
		this.config		= config;
		
		// Take the execution parameters from the configuration of the module
		classn			= config.get ("CLASS", "tc.runtime.thread.StdThread");
		info			= config.get ("INFO", (config.name () != null) ? config.name () : "No info");
		connects		= config.get ("CONNECT");
		mode			= parse_mode (config.get ("MODE"));
		passive			= config.getBoolean ("PASSIVE", false);
		queued			= config.getBoolean ("QUEUED", false);
		polled			= config.getBoolean ("POLLED", false);
		exectime		= config.getLong ("EXTIME", 100);
		priority		= config.getInt ("PRI", -1);
		cangfx			= config.getBoolean ("GFX", false);

		if (polled)		passive = false;
	}

	protected Linda linda_client (int mode, String robotid, LindaDesc ldesc, LindaServer server) throws Exception
	{
		Linda	 			client;
		
		switch (mode)
		{
		case ThreadDesc.M_UDP:
			LindaNetClient		uclient;
			
			uclient	= new LindaNetClient (LindaNet.UDP, robotid, ldesc.addr, ldesc.port);
			port		= uclient.getLocalPort ();
			
			client	= (Linda) uclient;
			break;

		case ThreadDesc.M_TCP:
			LindaNetClient		tclient;
			
			tclient	= new LindaNetClient (LindaNet.TCP, robotid, ldesc.addr, ldesc.port);
			port		= tclient.getLocalPort ();
			
			client	= (Linda) tclient;
			break;

		case ThreadDesc.M_SHARED:
		default:
			// modules of a robot see their own tuples with the robot id as space (as with the network clients)
			client	= (robotid != null) ? new LindaSharedClient (server, robotid) : server;
			port		= 0;
		}
		
		return client;
	}
		
	public void start_thread (String robotid, LindaDesc ldesc_loc, LindaServer server_loc)
	{
		Linda	 			client;
		Class<?>				tclass;
		Constructor<?>			cons;
		Class<?>[]				types;
		Object[]			params;

		this.robotid	= robotid;
		
		try
		{
			System.out.print (">> Starting module [" + preffix + "@" + robotid + "]");
			client			= linda_client (mode, robotid, ldesc_loc, server_loc);
			if (client == null)
			{
				System.out.println ("--[ThDesc] Can not create Linda client. Aborting module [" + preffix + "@" + robotid + "]");
				return;
			}
			System.out.println (" Linda=" + lindaToString ());

			tclass		= Class.forName (classn);
			types		= new Class<?>[2];
			types[0]		= ModuleConfig.class;
			types[1]		= Class.forName ("tc.shared.linda.Linda");        
			cons			= tclass.getConstructor (types);
			params		= new Object[2];
			params[0]	= config;        
			params[1]	= client;        
			
			thread		= (StdThread) cons.newInstance (params);		
			thread.setTDesc (this);
			thread.start ();
		}
		catch (Exception e) { e.printStackTrace (); }
	}
	
	protected String lindaToString ()
	{
		String 			str;
		String			sport = "";
		
		if (port != 0)
			sport = ":" + Integer.valueOf (port).toString ();
		
		str		= "<";
		
		switch (mode)
		{
		case ThreadDesc.M_UDP:		str += "UDP" + sport;		break;
		case ThreadDesc.M_TCP:		str += "TCP" + sport;		break;
		case ThreadDesc.M_SHARED:	str += "SHARED";			break;
		default:					str += "N/A";
		}
		
		str		+= ">";

		return str;
	}

	public String toString ()
	{
		String 			str;
		
		str		= "[" + preffix + "/" + info + "] CLASS=" + classn + " LINDA=" + lindaToString ();		
		str		+= " FLAGS=<";
		
		if (passive)		
			str += "PASSIVE";
		else
		{
			str += "ACTIVE:"+exectime;
			if (priority != -1)		str += ", PRIORITY:"+priority;
		}
		
		if (queued)		str += ", QUEUED";
		if (polled)		str += ", POLLED";
		if (cangfx)		str += ", GRAPHICS";
		
		str		+= ">";
		
		return str;
	}
}

