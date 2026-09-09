/*
 * (c) 2003 Humberto Martinez
 */
 
package tc.runtime.thread;

import java.util.*;
import java.lang.reflect.*;

import tc.shared.linda.*;

public class RouterDesc extends ThreadDesc
{
	// Variables to help executing the modules
	public int					gmode;				// Global Linda client execution mode
	protected int				gport		= 0;		// Global current local port (for UDP/TCP mode only)

	// Constructors
	public RouterDesc (String preffix, Properties props)
	{
		super (preffix, props);
	}
	
	// Instance methods
	protected void initialise (String preffix, Properties props)
	{
		super.initialise (preffix, props);
		
		// Parse properties to set instance variables
		gmode		= parse_mode (props.getProperty (preffix + "GMODE"));
	}

	public void start_thread (String robotid, Properties props, LindaDesc ldesc_loc, LindaServer server_loc, LindaDesc ldesc_glob, LindaServer server_glob)
	{
		Linda	 			client_loc;
		Linda				client_glob;
		Class				pclass;
		Constructor			cons;
		Class[]				types;
		Object[]				params;

		try
		{
			System.out.print (">> Starting Linda Router [" + preffix + "@" + robotid + "]");
			client_glob 	= linda_client (gmode, null, ldesc_glob, server_glob);
			gport		= port;
			client_loc	= linda_client (mode, null, ldesc_loc, server_loc);
			System.out.println (" Linda=" + lindaToString ());
			
			pclass		= Class.forName (classn);
			types		= new Class[3];
			types[0]		= Class.forName ("java.lang.String");        
			types[1]		= Class.forName ("tc.shared.linda.Linda");        
			types[2]		= Class.forName ("tc.shared.linda.Linda");        
			cons			= pclass.getConstructor (types);
			params		= new Object[3];
			params[0]	= robotid;        
			params[1]	= client_loc;        
			params[2]	= client_glob;        
			cons.newInstance (params);
		}
		catch (Exception e) { e.printStackTrace (); }
	}
	
	protected String lindaToString ()
	{
		String 			str;
		String			sport = "", lport = "";
		
		if (port != 0)
			lport = ":" + Integer.valueOf (port).toString ();
		
		str		= "<LOCAL=";
		
		switch (mode)
		{
		case ThreadDesc.M_UDP:		str += "UDP" + lport;		break;
		case ThreadDesc.M_TCP:		str += "TCP" + lport;		break;
		case ThreadDesc.M_SHARED:		str += "SHARED";			break;
		default:						str += "N/A";
		}

		if (gport != 0)
			sport = ":" + Integer.valueOf (gport).toString ();
		
		str		+= ",GLOBAL=";

		switch (gmode)
		{
		case ThreadDesc.M_UDP:		str += "UDP" + sport;		break;
		case ThreadDesc.M_TCP:		str += "TCP" + sport;		break;
		case ThreadDesc.M_SHARED:		str += "SHARED";			break;
		default:						str += "N/A";
		}
		
		str		+= ">";

		return str;
	}
}

