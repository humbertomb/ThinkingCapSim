package tcrob.oru.rasmus;

import tc.runtime.thread.ModuleConfig;
import java.util.*;

import tc.shared.linda.*;
import tclib.planning.sequence.*;

public class RasmusDeliberation extends SeqPlanner
{
	// Constructors
	public RasmusDeliberation (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}
		
	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{
		//System.out.println ("RamsusDelibaration() /initialise  (Jesper)");		
	}
	
	public void step ()
	{
		//System.out.println("RasmusDeliberation /step()  (Jesper)");
		if (state != RUN) 					return;
		
		// Write your own MOPLANNING code here
			
		// Write whatever you want to the Linda space
	}	
	
	public void notify_config (String space, ItemConfig item)
	{
		//System.out.println("RasmusDeliberation() /notify_config() (Jesper)");
		super.notify_config (space, item);
		
		// Write your map initialisation code here
	}
}
