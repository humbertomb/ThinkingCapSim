/*
 * (c) 2002 Humberto Martinez, Juan Pedro Canovas
 * (c) 2003 Humberto Martinez
 */

package tc;

import java.io.*;
import java.util.*;

import tc.runtime.thread.*;
import tc.shared.linda.*;

public class ExecArch extends Thread
{
	static public final int 		MAX_THS		= 20;	// Maximum number of modules
	
	// Linda Spaces parameters
	public LindaDesc				lldesc;				// Local Linda Space description
	public LindaDesc				gldesc;				// Global Linda Space description
	
	// Actual Linda Spaces
	protected LindaServer			linda_loc;
	protected LindaServer			linda_glob;

	// Architecture modules and parameters
	public int					num;					// Number of modules
	public ThreadDesc[]			thdesc;				// Actual standard modules
	public ThreadDesc			vrdesc;				// Actual VirtualRobot module
	public RouterDesc			lrdesc;				// Actual LindaRouter module
		
	// Additional execution variables
	protected boolean			initialised	= false;
	protected Properties			props;
	protected String				robotid;

	/* Constructors */
	protected ExecArch ()
	{
	}

	public ExecArch (String robotid, String name)
	{
		this (robotid, name, (Properties) null);
	}

	public ExecArch (String robotid, String name, Properties pdefs)
	{
		Properties		props;
		File			file;
		FileInputStream	stream;

		// Read properties from file
		props			= new Properties ();
		try
		{
			file 		= new File (name);
			stream 		= new FileInputStream (file);
			props.load (stream);
			stream.close ();
		} catch (Exception e) { e.printStackTrace (); }
		
		initialise (robotid, props, pdefs);
	}

	public ExecArch (String robotid, Properties props)
	{
		
		initialise (robotid, props, null);
	}

	public ExecArch (String robotid, Properties props, Properties pdefs)
	{
		initialise (robotid, props, pdefs);
	}

	// Class methods
	public static void main (String[] argv)
	{
		if (argv.length == 2)
				new ExecArch (argv[0], argv[1]).start ();
		else
		{
			System.out.println ("ERROR: wrong number of arguments.");
			System.out.println ("\tUsage: ExecArch <robot_id> <arch> to execute an ADF");
		}
	}
    
	/* Instance methods */
	protected void initialise (String robotid, Properties props, Properties pdefs)
	{
		String			modules, vrmodule, lrmodule;
		String			preffix;
		StringTokenizer	st;
		Enumeration		enu;
		String			pname;
		
		// Setup private local variables
		this.props		= props;
		this.robotid	= robotid;
				
		props.setProperty("ROBNAME",robotid);
		
		// Prepare data structures
		num				= 0;
		thdesc			= new ThreadDesc[MAX_THS];

		// Overwrite default properties
		if (pdefs != null)
		{
			enu	= pdefs.propertyNames ();
			while (enu.hasMoreElements ())
			{
				pname	= (String) enu.nextElement ();
				
				props.setProperty (pname, pdefs.getProperty (pname));
			}
		}

		// Load and parse Linda servers properties
		lldesc		= new LindaDesc ("LLIN", LindaDesc.L_LOCAL, props);			
		gldesc		= new LindaDesc ("GLIN", LindaDesc.L_GLOBAL, props);			

		// Load and parse architecture global properties
		modules		= props.getProperty ("MODULES");
		vrmodule		= props.getProperty ("VROBOT");
		lrmodule		= props.getProperty ("ROUTER");
		if (modules != null)
		{
			st			= new StringTokenizer (modules, ", \t");
			for (num = 0; st.hasMoreTokens (); num++)
			{
				preffix		= st.nextToken ();
				thdesc[num]	= new ThreadDesc (preffix, props);
			}
		}
		if (lrmodule != null)
			lrdesc		= new RouterDesc (lrmodule, props);			
		if (vrmodule != null)
			vrdesc		= new ThreadDesc (vrmodule, props);			
			
		initialised		= true;	
	}
	
	public void run ()
	{
		int				i;
		
		while (!initialised);
		
		this.setName ("TC-Arch-Executor ("+robotid+")");

		System.out.println ("\nExecArch: Executing a TC-II ADF (Architecture Definition File)");
		System.out.println (this);
		System.out.println ();		
		
		try
		{
			// Crate Linda Spaces if required
			if (lldesc.create)			linda_loc	= lldesc.start_server ();
			if (gldesc.create)			linda_glob	= gldesc.start_server ();
			
			// Execute LindaRouter if needed
			if (lrdesc != null)
				lrdesc.start_thread (robotid, props, lldesc, linda_loc, gldesc, linda_glob);

			// Execute required standard modules
			for (i = 0; i < num; i++)
				thdesc[i].start_thread (robotid, props, lldesc, linda_loc);
			
			// Execute VirtualRobot if needed
			virtual_robot ();
		}
		catch (Exception e) { e.printStackTrace (); }
		
		try { Thread.currentThread ().join (); } catch (Exception e) {  e.printStackTrace ();  }
		
		System.out.println ("Program finished.");
	}
	
	protected void virtual_robot ()
	{
		if (vrdesc != null)
			vrdesc.start_thread (robotid, props, lldesc, linda_loc);
	}
	
	public String toString ()
	{
		int				i;
		String			str;
		
		str		= "Architecture Description\n";
		
		str += "\t" + gldesc + "\n";									// Global Linda Space
		str += "\t" + lldesc + "\n";									// Local Linda Space
		if (gldesc != null)		str += "\t" + lrdesc + "\n";			// Linda Router
		
		for (i = 0; i < num; i++)
			str += "\t" + thdesc[i] + "\n";								// Architecture modules
			
		if (gldesc != null)		str += "\t" + vrdesc + "\n";			// Virtual Robot

		return str;
	}
}

