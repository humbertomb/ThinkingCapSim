/*
 * (c) 2003 Humberto Martinez
 */
 
package tcapps.tcsim.replay;

import java.io.*;
import java.util.*;

import tcapps.tcsim.simul.*;

import tc.vrobot.*;
import tc.shared.linda.*;
import tc.shared.world.*;

public class ReplayRobot extends VirtualRobot
{		
	static public final String		SUFFIX		= ".path";
	
	// Replay parameters
	protected String					rpname;
	protected BufferedReader			rpfile;
	protected double					x, y, a;				// Robot position

	protected Simulator				simul;
	protected RobotModel				model;
	protected World					map;
	protected String					robotid;
	protected int					myindex;
	protected SimulatorDesc			sdesc;

	// Constructors
	public ReplayRobot (String robotid, Properties props, Linda linda, Simulator simul)
	{
		super (props, linda);

		this.simul = simul;
		this.robotid = robotid;
	}
		
	// Instance methods
	protected void initialise (Properties props)
	{		
		super.initialise (props);
		
		// Load robot and world description
		sdesc		= new SimulatorDesc (rprops);
		model		= rdesc.model;		
		
		// Notify the simulator that exists
		myindex 		= simul.add_robot (rdesc, sdesc, model, data_ctrl);		
		map 			= simul.getWorld ();
		System.out.println ("# Setting robot map to: "+map);

		// Load robot environment description and parameters
		rpname		= props.getProperty ("ROBLOG");
		System.out.println ("# Replaying log file: " + rpname);
		
		reset ();
	}

	public void reset ()
	{
		FileReader				file;
		
		// Load path file
		try
		{
			if (!rpname.endsWith (SUFFIX))
				System.out.println ("--[Replay] Reading PATH file: Unknown file-extension. Continue loading ...");

			file	= new FileReader (rpname);
			rpfile	= new BufferedReader (file);
		}
		catch (Exception e) { e.printStackTrace (); }
		
		// Initialise simulation
		simul.reset (myindex, data, map);
	}
	
	public void process_sensors (long dtime)
	{
		String				str;
		StringTokenizer		st;
    	
		// Parse data
		try
		{
			str		= rpfile.readLine ();
			st		= new StringTokenizer (str, ", \t");

			x		= Double.parseDouble (st.nextToken ());
			y		= Double.parseDouble (st.nextToken ());
			a		= Double.parseDouble (st.nextToken ());
		} catch (Exception e) { }

		// Compute simulation
		simul.simulate (myindex, data, x, y, a, cycson, cycir, cyclrf, cyclsb, cycvis);    
		if (rdesc.MAXVISION > 0)
			odata = simul.getVisionData ();
	}
			
	public void notify_data_ctrl (String space, ItemDataCtrl item)
	{
		super.notify_data_ctrl (space, item);
		simul.set_data_ctrl (myindex, data_ctrl);
	}
	
	public void notify_motion (String space, ItemMotion item)
    {
		// A Replayer can not be "manipulated"
	}	  
}
