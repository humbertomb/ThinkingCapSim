/**
 * Created on 23-abr-2006
 *
 * @author Humberto Martinez Barbera
 */
package tclib.planning.sequence;

import java.io.*;
import tclib.navigation.pathplanning.*;
import devices.pos.*;

public class Task implements Serializable
{
	// Default tolerances	
	static public final  double		TOL_DIST		= 0.25;				// Tolerance in position (m)
	static public final  double		TOL_HEAD		= Math.PI;			// Tolerance in heading (rad)

	// Global plan (high level)
	public String					plan;		// Current plan action
	public String					place;		// Current plan place
	
	// Local task (low level)
	public String					task;		// Current task
	public Position					tpos;		// Current task destination (m, m, m, rad)

	// Tolerances
	public double					tol_pos;		// Tolerance for position
	public double					tol_head;	// Tolerance for heading
	
	// Path generation
	public int						path_mode;	// Type of path generation method
	public int						path_src;	// Where to apply the path planner


	// Constructors
	public Task ()
	{
		// Default values
		plan			= "STAY";
		place		= "none";
		
		task			= "STANDBY";
		tpos		= new Position ();

		tol_pos		= TOL_DIST;
		tol_head		= TOL_HEAD;
		
		path_mode	= GridPath.POLYLINE;
		path_src		= GridPath.GRID;
	}

	// Instance methods
	public void set (Task plan)
	{
		this.tpos.set (plan.tpos);
		this.place		= plan.place;
		
		this.plan		= plan.plan;
		this.task		= plan.task;
		
		this.tol_pos		= plan.tol_pos;
		this.tol_head	= plan.tol_head;
		
		this.path_mode	= plan.path_mode;
		this.path_src	= plan.path_src;
	}

	public String toString ()
	{
		return plan.toUpperCase ()+"/"+task.toUpperCase ()+"("+place+")";
	}
}
