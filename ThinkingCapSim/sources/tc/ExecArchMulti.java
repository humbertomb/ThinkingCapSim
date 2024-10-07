/*
 * (c) 2002 Humberto Martinez, Juan Pedro Canovas
 * (c) 2003 Humberto Martinez
 */

package tc;

import java.io.*;
import java.util.*;

import tc.runtime.thread.*;
import tc.shared.linda.*;

public class ExecArchMulti extends Thread
{
	static public final int 		MAX_MTS		= 5;	// Maximum number of monitor
	
	// Linda Spaces parameters
	public LindaDesc				gldesc;				// Global Linda Space description
	
	// Actual Linda Spaces
	protected LindaServer			linda_glob;

	// Architecture modules and parameters
	public int						num;				// Number of modules
	public MonitorDesc[]			mtdesc;				// Actual stabdard modules
		
	// Additional execution variables
	protected Properties			props;
	
	/* Constructors */
	protected ExecArchMulti ()
	{
	}

	public ExecArchMulti (String name)
	{
		this (name, null);
	}
	
	public ExecArchMulti (String name, Properties pdefs)
	{
		Properties		props;
		File			file;
		FileInputStream	stream;

		props			= new Properties ();
		try
		{
			file 		= new File (name);
			stream 		= new FileInputStream (file);
			props.load (stream);
			stream.close ();
		} catch (Exception e) { e.printStackTrace (); }

		initialise (props, pdefs);
	}

	public ExecArchMulti (Properties props, Properties pdefs)
	{
		initialise (props, pdefs);
	}

	// Class methods
	public static void main (String[] argv)
	{
		if (argv.length == 1)
				new ExecArchMulti (argv[0]).start ();
		else
		{
				System.out.println ("ERROR: wrong number of arguments.");
				System.out.println ("\tUsage: ExecArchMulti <arch> to execute an ADF (Architecture Description File)");
		}
	}
    
	/* Instance methods */
	protected void initialise (Properties props, Properties pdefs)
	{
		String			modules;
		String			preffix;
		StringTokenizer	st;
		Enumeration		enu;
		String			pname;
		
		// Setup private local variables
		this.props		= props;
		
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
		
		// Prepare data structures
		num				= 0;
		mtdesc			= new MonitorDesc[MAX_MTS];

		// Load and parse Linda servers properties
		gldesc			= new LindaDesc ("GLIN", LindaDesc.L_GLOBAL, props);			

		// Load and parse architecture global properties
		modules			= props.getProperty ("MONITORS");
		if (modules != null)
		{
			st			= new StringTokenizer (modules, ", \t");
			for (num = 0; st.hasMoreTokens (); num++)
			{
				preffix		= st.nextToken ();
				mtdesc[num]	= new MonitorDesc (preffix, props);
			}
		}
	}
	
	public void run ()
	{
		int				i;
		
		this.setName ("TC-ArchMulti-Executor");

		System.out.println ("\nExecArchMulti: Executing a TC-II ADF (Architecture Definition File)");
		System.out.println (this);
		System.out.println ();		
		
		try
		{
			// Crate Linda Spaces if required
			if (gldesc.create)			linda_glob	= gldesc.start_server ();
			
			// Execute required standard modules
			for (i = 0; i < num; i++)
				mtdesc[i].start_thread (props, gldesc, linda_glob);
		}
		catch (Exception e) { e.printStackTrace (); }

		try { Thread.currentThread ().join (); } catch (Exception e) {  e.printStackTrace ();  }
		
		System.out.println ("Program finished.");
	}
	
	public String toString ()
	{
		int				i;
		String			str;
		
		str		= "Architecture Description\n";
		
		str += "\t" + gldesc + "\n";									// Global Linda Space
		
		for (i = 0; i < num; i++)
			str += "\t" + mtdesc[i] + "\n";								// Architecture monitors
			
		return str;
	}
}