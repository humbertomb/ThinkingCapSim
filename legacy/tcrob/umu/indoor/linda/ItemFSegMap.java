/*
 * (c) 2003 Humberto Martinez
 */
 
package tcrob.umu.indoor.linda;

import tc.shared.linda.*;

import tclib.navigation.mapbuilding.*;

import devices.pos.*;

public class ItemFSegMap extends Item
{
	public FSegMap					fmap;			// Current fuzzy segments map
	public Paths					paths;			// Corrected and odometry based trajectories
	public Position					pos;			// Current robot position
	
	// Constructors
	public ItemFSegMap () 
	{
		super ();
	}	
	
	// Initialisers
	public void set (FSegMap fmap, Paths paths, Position pos, long tstamp)
	{
		set (tstamp);
		
		this.fmap	= fmap;
		this.paths	= paths;
		this.pos	= pos;
	}
	
	public String toString ()
	{
		return "ItemFSegMap=" + fmap;
	}	
}
