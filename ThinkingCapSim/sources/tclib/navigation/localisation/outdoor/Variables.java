/*
 * (c) 2003-2004 Rafael Toledo Moreo
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.navigation.localisation.outdoor;

public class Variables
{
	// ins
	public double rollins, pitchins, yawins;
	public double rollrateins, pitchrateins, yawrateins;
	public double axins, ayins, azins;
	public double tempins;
	public double tstampins;	// ins sample time
	public boolean qins;
	public double t1ins;		// previous ins time
	public double tsins;		// ins sample time
	
	// gps
	public double xgps, ygps, zgps;
	public double xvgps, yvgps, zvgps;
	public int qgps;
	public double tstampgps;
	public double t1gps;		// previous gps time
	public double tsgps;		// gps sample time
	public double towgps;		// time of week
	public double towgps_prev;		// time of week
	
	// compass
	public double yawcmp;
	public double pitchcmp;
	public double rollcmp;
	public double tstampcmp;
	public double t1cmp;		// previous compass time
	public double tscmp;		// compass sample time
		
	public int lastupdate = 0;		// 1=INS, 2=GPS.
	public int lastgpsq   = 0;		// 0 = No position (or not allowed), 1=valid position
	public int penulqgps  = 0;		// 0 = penultimate q = no post. (or not allowed). 1 = valid position
	
	public long tini;		// initial time
		
	// captures counters		
	public int compasscounter			= -1;
	public int compasscounter_prev		= -1;
	public int inscounter				= -1;
	public int inscounter_prev			= -1;
	public int gpscounter				= -1;
	public int gpscounter_prev			= -1;
	
	public Variables ()
	{
	}
}