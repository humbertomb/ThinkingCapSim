/*
 * (c) 2003 Humberto Martinez
 */

package tcapps.tcsim;

import tcapps.tcsim.replay.*;
import tcapps.tcsim.simul.*;

import tc.*;
import tc.runtime.thread.*;

public class ExecArchReplay extends ExecArch
{
	Simulator			sim;
	
	public ExecArchReplay (String robotid, String name, Simulator sim)
	{
		super (robotid, name);
		
		this.sim = sim;

		if (vrdesc != null)
		{
			// Load a world description if none available
			if (sim.getWorld () == null)
			{
				try  { sim.setWorld (this.props.getProperty (vrdesc.preffix + "WORLD")); }
				catch (Exception e)
				{
					System.out.println ("[ExecArchSim]: Exception loading world: "+e);
					return;
				}
			}
			
			if (sim.getWorldName () != null)
				this.props.setProperty (vrdesc.preffix + "WORLD", sim.getWorldName ());
		
			// Set the simulator as the virtual robot
			vrdesc.classn = "tcsim.replay.ReplayRobot";
		}
	} 
	
	public void virtual_robot ()
	{
		StdThread 	thread;

		if (vrdesc != null)
		{
			System.out.println (">> Starting Replayed Robot [" + vrdesc.preffix + "@" + robotid + "]");

			thread	= new ReplayRobot (robotid, props, linda_loc, sim);
			thread.setTDesc (vrdesc);
			thread.start ();	
		}
	}
}