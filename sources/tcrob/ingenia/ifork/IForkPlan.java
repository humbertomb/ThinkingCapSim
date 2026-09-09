/*
 * Created on 06-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.ingenia.ifork;

import java.io.*;

import tclib.planning.sequence.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class IForkPlan extends Task implements Serializable
{
	// Constants
	static public final double		NA	= Double.MAX_VALUE;		// N/A speed info
	
	// Local task (low level)
	public String[]					t_labels;	// Task goal point labels (options)
	public int						t_target;	// Solved task goal point index
	public boolean					t_solved;	// The goal position has been "solved"
	
	// Speed limits
	public double					spd_vmax;	// Maximum allowed linear speed
	public double					spd_rmax;	// Maximum allowed rotation speed
	public boolean					spd_end;		// Decay speed reaching the goal

	// Constructors
	public IForkPlan ()
	{
		super ();
	
		t_labels	= null;
		t_target	= -1;
		t_solved	= true;

		spd_vmax	= NA;
		spd_rmax	= NA;
		spd_end		= false;
	}

	// Instance methods
	public void set (IForkPlan plan)
	{
		super.set (plan);
		
		this.t_labels	= plan.t_labels;
		this.t_target	= plan.t_target;
		this.t_solved	= plan.t_solved;
		
		this.spd_vmax	= plan.spd_vmax;
		this.spd_rmax	= plan.spd_rmax;
		this.spd_end		= plan.spd_end;
	}
	
	public boolean containsLabel (String label)
	{
		int			i;
		
		if (t_labels == null)		return false;
		
		for (i = 0; i < t_labels.length; i++)
			if (label.equals (t_labels[i]))
				return true;
			
		return false;
	}
		
/*	public String taskToString ()
	{
	    String label;
	    if(t_labels!=null && t_labels.length!= 0 && t_labels[0] != null)
		    label = " "+t_labels[0];
		else
			label = "";
			
		switch (task)
		{
		case DOCK:				return "Docking"+label;
		case UNDOCK:			return "Undocking"+label;
		case FLOAD:				return "Loading fork"+label;
		case FUNLOAD:			return "Unloading fork"+label;
		case FADJUST:			return "Adjusting fork"+label;
		case CHECKPAL:			return "Check palet"+label;
		case COORDWAIT:			return "Waiting (Coordination)"+label;
		default:				return super.taskToString ()+label;
		}
	}
	
	public String taskToTag ()
	{
		switch (task)
		{
		case DOCK:				return "DOCK";
		case UNDOCK:			return "UNDOCK";
		case FLOAD:				return "FLOAD";
		case FUNLOAD:			return "FUNLOAD";
		case FADJUST:			return "FADJUST";
		case CHECKPAL:			return "CHECKPAL";
		case COORDWAIT:			return "COORDWAIT";
		default:				return super.taskToTag ();
		}
	}*/
}
