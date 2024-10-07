/*
 * (c) 2003 Humberto Martinez
 */
 
package tcrob.umu.indoor.linda;

import tc.shared.linda.*;

import tclib.navigation.mapbuilding.*;
import tclib.navigation.pathplanning.*;

import devices.pos.*;

public class ItemGridMap extends Item
{
	public Grid						grid;			// Current grid map
	public GridPath					path;			// Current path to goal
	public Position					pos;			// Current robot position
	
	// Constructors
	public ItemGridMap () 
	{
		super ();
	}	
	
	// Initialisers
	public void set (Grid grid, GridPath path, Position pos, long tstamp)
	{
		set (tstamp);
		
		this.grid	= grid;
		this.path	= path;
		this.pos	= pos;
	}
	
	public String toString ()
	{
		return "ItemGridMap =" + grid;
	}	
}
