/*
 * (c) 2002-2004 Humberto Martinez Barbera
 */
 
package tclib.planning.sequence;

import tc.runtime.thread.ModuleConfig;
import tc.modules.*;
import tc.shared.linda.*;
import tclib.planning.sequence.gui.*;

import wucore.utils.geom.*;
		
public class SeqPlanner extends Planner
{
	/**
	 * What a plain sequence planner understands: it walks the places of the plan
	 * in turn, setting a goal at each of them, and makes nothing of the action
	 * beyond that -- so going to a place and staying at one is all it does.
	 */
	static public final String[]	ACTIONS		= { "goto", "stay" };

	// Overall plan parameters
	protected Task[]				task;
	protected int					task_n;
	protected int					task_k;
	
	protected boolean				initialised = false;		// The planner has been initialised
	protected boolean				newtask;					// A new overall task is available
	protected boolean				finished;				// Current overall task finished
	
	protected SeqPlannerWindow		swin;
	
	// Constructors
	public SeqPlanner (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
		newtask	= false;
	}
	
	// Instance methods
	protected void close_gfx ()
	{
		if (swin != null)		swin.close ();
		swin		= null;
	}

	public void step (long ctime) 
	{
		// Check if a plan can be computed
		if ((world == null) || (task == null) || (lps == null))
		{
			if(debug)
			{
				System.out.print("  [Pla] Idle planner. Reason:");
				if(world==null) System.out.print(" No world.");
				if(task==null) System.out.print(" No plan.");
				if(lps==null) System.out.print(" No LPS data.");
				System.out.println();
			}
			return;		
		}
		if (finished || !initialised)
		{
			if(debug)
				System.out.println("  [Pla] Idle planner: finished="+finished+" initialized="+initialised);
			return;
		}
		
		// Perform the plan supervision task
		do_plan ();
	}
	
	protected void do_plan ()
	{
		if (newtask)
		{
			task_k		= 0;
			newtask		= false;
			
			setGoal (task[task_k]);
			setStatus (ItemStatus.OCCUPIED, task[task_k].toString ());

			if (debug)		System.out.println (this);

			if (swin != null)		swin.updatePlan (htmlPlan ());
		}
		else if (taskStatus () != ItemBehResult.T_NOTYET)
		{
			task_k ++;
			
			if (task_k >= task_n)
			{
				finished	= true;				
				
				setGoal (task[task_n-1]);
				setStatus (ItemStatus.IDLE, "Task completed");
			}
			else
			{
				setGoal (task[task_k]);
				setStatus (ItemStatus.OCCUPIED, task[task_k].toString ());
			}

			if (debug)		System.out.println (this);

			if (swin != null)		swin.updatePlan (htmlPlan ());
		}
	}
	
	public void setGoal (Task task)
	{
		behtime 		= System.currentTimeMillis ();
		behresult	= ItemBehResult.T_NOTYET;
		
		if (debug)		System.out.println ("  [Pla] Sending task [" + task + "] with ID: " + behtime);

		gitem.set (task, behtime);
		linda.write (gtuple);
	}
	
	public void notify_plan (String space, ItemPlan item) 
	{ 
		int				i, n;
		Point3			pos;
		double			theta;
		Sequence		seq;
		Task[]			tasks;
		
		seq		= item.seq;
		if ((world == null) || (seq == null))		return;
		if (seq.size () == 0)						return;
		
		// the new plan is built aside: a place the world does not have rejects it, and the planner goes on as it was
		n		= seq.size () + 1;
		tasks	= new Task[n];
		for (i = 0; i < n-1; i++)
		{
			pos				= world.getPos (seq.place[i]);
			if (pos == null)
			{
				System.out.println ("--[Pla] Plan " + seq + " rejected: the world has no place <" + seq.place[i] + ">");
				setStatus (ItemStatus.FAILED, "No place " + seq.place[i]);
				return;
			}
			theta			= world.getAngle (seq.place[i]);
			tasks[i]		= new Task ();
			tasks[i].tpos.set (pos.x (), pos.y (), pos.z (), theta);
			tasks[i].plan	= seq.action[i];
			tasks[i].task	= "NAVIGATE";
			tasks[i].place	= seq.place[i];
		}
		
		tasks[i]		= new Task ();	
		tasks[i].tpos	= tasks[i-1].tpos;
		tasks[i].plan	= "STAY";
		tasks[i].task	= "STANDBY";
		tasks[i].place	= seq.place[i-1];

		if (debug)		System.out.println ("  [Pla] Received sequence " + seq);

		task		= tasks;
		task_n		= n;
		task_k		= 0;
		newtask		= true;
		finished	= false;
	}	

	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
		
		// Initialise variables
		newtask		= false;
		initialised	= true;
		
		if (localgfx) 	swin	= new SeqPlannerWindow (space);
	}
	
	public String htmlPlan ()
	{
		int			i;
		String		out;
		
		out	= "<HTML><B>SEQ PLAN</B><BR>";
		out	+= "<FONT SIZE=3>";
		for (i = 0; i < task_n; i++)
		{
			if (i == task_k)			out	+= "<FONT COLOR=#FF0000>";

			out	+= "&nbsp;&nbsp;&nbsp;&nbsp;"+task[i].task+" (";
			if (task[i].place != null)
				out	+= task[i].place;
			out	+= ")<BR>";
			
			if (i == task_k)			out	+= "</FONT>";
		}
		
		return out + "</HTML>";
	}

	public String toString ()
	{
		if (task_k >= task_n)
			return "  [Pla] Task completed.";
			
		return "  [Pla] Task: " + task[task_k] + ". ";
	}
}

