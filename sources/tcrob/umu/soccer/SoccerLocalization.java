/*
 * (c) 2026 Humberto Martinez
 */
package tcrob.umu.soccer;

import devices.pos.Position;
import tc.modules.Navigation;
import tc.runtime.thread.ModuleConfig;
import tc.shared.linda.ItemConfig;
import tc.shared.linda.ItemLPS;
import tc.shared.linda.ItemNavigation;
import tc.shared.linda.Linda;
import tc.shared.linda.Tuple;
import tcrob.umu.soccer.gm.*;
import tcrob.umu.soccer.gm.data.*;
import tcrob.umu.soccer.gm.fmk.*;
import wucore.utils.math.Angles;

public class SoccerLocalization extends Navigation
{
	// Navigation structures
	protected Localisation			loc;
	protected Odometry				odom = new Odometry ();
	protected LocLps				loclps = new LocLps ();
	
	protected Position				pos;
	
	// Linda data structures
	protected Tuple					ntuple;
	protected ItemNavigation		nitem;

	// Additional local variables
	protected boolean				initialised		= false;

	// Constructors
	public SoccerLocalization (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}
		
	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{		
		// Initialise Linda related structures
		nitem		= new ItemNavigation ();
		ntuple		= new Tuple (Tuple.NAVIGATION, nitem);
		
		// Initialise other local stuff
		pos			= new Position ();
	}
	
	protected void close_gfx ()
	{
	}

	public void step (long ctime)
	{
		if (!initialised)		return;

	}
	
	public void notify_lps (String space, ItemLPS item) 
	{ 
		super.notify_lps (space, item);
		
		loclps.updateFromLps (item.lps);
		loc.updateMotionAndSensors (odom, loclps);
		
		// TODO
		// Update pos variable
		// Send ItemNavigation
	}	
		
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
								
		loc	= new GridFMarkov ("...", 500, 0.08f, (float) (1.0*Angles.DTOR));

		initialised = true;
	}
}

