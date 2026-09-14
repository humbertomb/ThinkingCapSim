/*
 * (c) 2003 Humberto Martinez
 */
 
package tc.runtime.thread;

import java.net.*;

import tc.shared.linda.*;

public class LindaDesc extends Object
{
	// Types of Linda servers
	static public final int			L_LOCAL		= 0;
	static public final int			L_GLOBAL		= 1;
	static public final String[]		l_mode		= { "LOCAL", "GLOBAL" };
	
	// Parameters of the Linda servers
	public String					classn;						// Implementation of tc.shared.linda.Linda
	public boolean					create;						// Create an instance of the Linda server?
	public String					preffix;					// Preffix for denoting the class (and parsing files)
	public int						mode;						// Linda server type
	public String					addr;						// Linda server address
	public int						port;						// Local server port

	// Constructors
	public LindaDesc (String preffix, int mode, String addr, int port, boolean create)
	{
		this.preffix	= preffix;
		this.mode		= mode;
		this.classn		= "tc.shared.linda.LindaServer";
		this.addr		= (addr != null) ? addr : "localhost";
		this.port		= port;
		this.create		= create;

		// If creating the Linda space, override the IP address
		if (create)
			try { this.addr	= InetAddress.getLocalHost ().getHostAddress (); } 						catch (Exception e)		{ this.addr	= "localhost"; }
	}

	public LindaServer start_server () throws Exception
	{
		LindaServer			server;
		
		// Create the Linda Space
		System.out.print (">> Creating a <"+ l_mode[mode] + "> Linda Space server thread ");
		
		server	= new LindaServer (port);

		System.out.println (server);
		
		// Update the description with the available port
		port		= server.port ();
		
		return server;
	}
	
	public String toString ()
	{
		String 			str;
		
		str		= "[" + preffix + "/" + l_mode[mode] + "] CLASS=" + classn;
		
		str		+= " ADDR=" + addr + ":" + port;
				
		str		+= " FLAGS=<";
		if (create)			str += "CREATE ";		
		str		+= ">";
		
		return str;
	}
}

