/*
 * (c) 2002 Juan Pedro Canovas, Humberto Martinez
 */

package tcapps.tcsim;

import java.util.*;

import tcapps.tcsim.simul.*;
import tcapps.tcsim.simul.objects.SimRobot;

import tc.*;
import tc.runtime.thread.*;
import tc.shared.linda.*;
import tc.shared.linda.net.*;

import wucore.utils.geom.*;

public class ExecArchSim extends ExecArch
{
	protected Simulator			sim;
	protected Point3			start;
	
	public ExecArchSim (String robotid, String name, Properties pdefs, Simulator sim)
	{
		super (robotid, name, pdefs);
		
		this.sim	= sim;
	
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
			vrdesc.classn = "tcsim.simul.SimulatedRobot";
		}
	} 
	
	public void 		setStart (Point3 start)		{ this.start = start; }
	
	public void virtual_robot ()
	{
		StdThread 	thread;

		if (vrdesc != null)
		{
			System.out.println (">> Starting Simulated Robot [" + vrdesc.preffix + "@" + robotid + "]");

			Linda linda = null;	
			
			if (linda_loc != null)
				linda = linda_loc;
			else if (vrdesc.mode == ThreadDesc.M_UDP)
			{
				try
				{
					System.out.println("ExecArchSim: creating LINDAUDP");
					linda = new LindaNetClient (LindaNet.UDP, null, lldesc.addr, lldesc.port);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					linda = null;
				}
			}		
			else if (vrdesc.mode == ThreadDesc.M_TCP)
			{
				try
				{
					System.out.println("ExecArchSim: creating LINDATCP");
					linda = new LindaNetClient (LindaNet.TCP, null, lldesc.addr, lldesc.port);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					linda = null;
				}
			}
			
			if (linda != null)
			{
				thread	= new SimRobot (robotid, props, linda, sim);
				thread.setTDesc (vrdesc);
				if (start != null)	((SimRobot) thread).reset (start);
				
				thread.start ();	
			}
		}
	}
}