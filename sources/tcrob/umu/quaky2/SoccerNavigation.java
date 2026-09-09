/*
 * (c) 2002 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

import java.util.*;

import tc.shared.linda.*;

import tclib.navigation.mapbuilding.*;

import tcrob.umu.indoor.IndoorNavigation;

public class SoccerNavigation extends IndoorNavigation
{	
	// Constructors
	public SoccerNavigation (Properties props, Linda linda)
	{
		super (props, linda);
	}
		
	// Instance methods
	public synchronized void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);

		// Modify inherited structures' behaviours
		grid.setMode (FGrid.SAFE_MOTION);
		
		gpath.setTimeStep (30);

		// Initialise other local stuff
		allow_fmap	= false;
		allow_path	= false;
		allow_gmap	= true;
	}
}

