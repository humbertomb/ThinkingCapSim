/*
 * (c) 2003 Humberto Martinez
 */
 
package tc.runtime.thread;

import java.lang.reflect.*;
import java.util.*;

import tc.gui.*;
import tc.modules.*;
import tc.shared.linda.*;

public class MonitorDesc extends ThreadDesc
{		
	// Variables to help executing the modules
	public String				wclassn;						// Class name of the GUI for the multi-monitor

	// Constructors
	public MonitorDesc (String preffix, Properties props)
	{
		super (preffix, props);
	}
		
	// Instance methods
	protected void initialise (String preffix, Properties props)
	{
		super.initialise (preffix, props);
		
		// Parse properties to set instance variables
		wclassn			= props.getProperty (preffix + "WEBCLASS");	
		if (wclassn == null)			{ wclassn	= "none"; }
	}

	public void start_thread (Properties props, LindaDesc ldesc_loc, LindaServer server_loc)
	{
		Linda	 				client;
		Monitor					monitor;	
		Class					tclass, pclass;
		Constructor				cons;
		Class[]					types;
		Object[]					params;
		GUIApplication			web = null;
		GUIMonitor				gui = null;
		
		if (!wclassn.equalsIgnoreCase("none"))
		{
			System.out.println (">> Starting GUI application [" + preffix + "]");
			try
			{
				pclass			= Class.forName (wclassn);
				web				= (GUIApplication) pclass.newInstance ();		
				web.start ();
				while (!web.isReady ());
				
				gui				= web.getGUIMonitor ();        
			}
			catch (Exception e) { e.printStackTrace (); }
		}
		else
			System.out.println (">> No GUI application [" + preffix + "]");
		
		try
		{
			System.out.print (">> Starting monitor [" + preffix + "]");
			client			= linda_client (mode, null, ldesc_loc, server_loc);
			System.out.println (" Linda=" + lindaToString ());
			
			tclass			= Class.forName (classn);
			types			= new Class[3];
			types[0]			= Class.forName ("java.util.Properties");      
			types[1]			= Class.forName ("tc.gui.GUIMonitor");   
			types[2]			= Class.forName ("tc.shared.linda.Linda");        
			cons				= tclass.getConstructor (types);
			params			= new Object[3];
			params[0]		= props;
			params[1]		= gui;        
			params[2]		= client;        
			monitor			= (Monitor) cons.newInstance (params);		
			monitor.setTDesc (this);
			
			if (!wclassn.equalsIgnoreCase("none"))
				web.setMonitor (monitor);
		}
		catch (Exception e) { e.printStackTrace (); }
	}
				
	public String toString ()
	{
		String 			str;
		
		str		= super.toString ();
		str		+= " GUI=" + wclassn;
				
		return str;
	}
}

