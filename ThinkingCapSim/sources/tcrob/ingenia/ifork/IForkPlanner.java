/*
 * (c) 2002,2003 Humberto Martinez Barbera
 *          2003 Juan Pedro Canovas
 * (c) 2004 Humberto Martinez Barbera
 * 	    
 */
 
package tcrob.ingenia.ifork;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.util.*;

import tc.shared.linda.*;
import tc.shared.world.*;

import tclib.navigation.pathplanning.GridPath;
import tclib.planning.htopol.*;
import tclib.planning.sequence.*;
import tclib.utils.petrinets.*;
import tcrob.ingenia.ifork.gui.*;
import tcrob.ingenia.ifork.linda.*;

import devices.pos.*;
import wucore.utils.geom.*;
import wucore.utils.math.*;
		
public class IForkPlanner extends SeqPlanner
{
	// Plan types
	static public final int			STAY				= 0;
	static public final int			GOTO				= 1;
	static public final int			LOAD				= 2;
	static public final int			UNLOAD			= 3;
	
	// Manouvering distances
	static public final double		UNDOCK_DIST		= 2.25;		// Undocking distance (m)
	
	// Manouvering linear velocities
	static public final double		UNDOCK_SPD		= 0.85;		// Undocking velocity (m/s)
	static public final double		DOCK_SPD			= 0.35;		// Docking velocity (m/s)
	static public final double		DOOR_SPD			= 0.75;		// Door crossing velocity (m/s)
	static public final double		NAV_SPD			= 1.5;		// Navigating velocity (m/s)
	static public final double		WRN_SPD			= 0.5;		// Max. warning speed (m/s)

	// Manouvering rotation velocities
	static public final double		DOCK_TRN			= 80.0;		// Docking velocity (deg/s)
	static public final double		DOOR_TRN			= 50.0;		// Door crossing velocity (deg/s)
	static public final double		NAV_TRN			= 120.0;		// Navigating velocity (deg/s)

	// Manouvering tolerances
	static public final  double		TOL_DOCK_DIST	= 0.04;		// Tolerance in docking operations (m)
	static public final  double		TOL_DOCK_HEAD	= 15.0;		// Tolerance in docking operations (deg)
	static public final  double		TOL_NAV_DIST		= 0.30;		// Tolerance in navigation operations (m)
	static public final  double		TOL_NAV_HEAD		= 180.0;		// Tolerance in navigation operations (deg)

	// Fork commands and heights
	static public final double		FORK_NAVIG		= 0.240;		// Fork height navigation (m)
	static public final double		FORK_OFFSET		= 0.22;		// Offset over current height setpoint (m)

	// Current sub-plan parameters
	protected HTopolMap				topol;						// Topologic Map of world
	protected IForkPlan[]				subplan;
	protected int					subplan_n;
	protected int					subplan_k;
	
	protected boolean				completed;					// Current subtask finished
	protected boolean				failed;						// Current subtask has failed	

	// Current petri net for coordination
	protected PetriNet				pnet;
	private Vector					lnodes;						// Last inserted set of nodes

	// Coordination with warehouse
	protected boolean 				doSecCoord;					// Coordination active
	protected boolean				firstwaiting;				// WAIT msg has to be sent
	protected ItemSync 				syncitem;					// Warehouse item
	protected Tuple 		  			synctuple;					// Warehouse tuple
	
	// Coordination with AGVs
	static public final double 		AGV_DIST			= 4.0;		// Coordination Distance
	static public final double		WARN_DIST			= 6.0;		// Warning distance
	static public final double		POINT_DIST			= 3.0;		// Distance to ocuped wp
	
	protected Hashtable				agvinfo;						// Coord info from other robots
	protected long					myPriority;					// Own priority value
	protected ItemCoordination		coorditem;	
	protected Tuple					coordtuple;
	protected boolean				givenway;
	protected boolean				stopped;
	protected String					task_backup;
	
	protected String				booked;						// Current topo node locked
	protected LinkedList			locks;
	protected Hashtable				asoclocks;					// Topo nodes locked with the AGVs
	protected boolean				lmodified		= false;		// A lock has been modified
	protected boolean				begindock		= true;
	protected String				lastwp 			= null;			// Ultimo Punto de espera visitado WP o DOOR
	protected boolean				warning			= false;
	protected Line2[]				limits;
	protected int					givencause		= -1;
	protected boolean				laserexception	=false;
	// Private and debug variables
	protected IForkPlanWindow			win;
	protected Hashtable				agv_runtime;
	
	// Debug
	PrintWriter pout = null;
	long reftimer;
	boolean fdebug;
	
	// Constructors
	public IForkPlanner (Properties props, Linda linda)
	{
		super (props, linda);

		int			i;
		
		subplan		= new IForkPlan[50];
		for (i = 0; i < 50; i++)
			subplan[i]	= new IForkPlan ();
		pnet			= new PetriNet ();
			
		// Coordination stuff
		agvinfo		= new Hashtable ();
		myPriority	= 0;
		coorditem 	= new ItemCoordination();
		coordtuple 	= new Tuple (IForkTuple.COORD, coorditem); 
		givenway 	= false;
		stopped		= true;
		robotid 	= props.getProperty ("ROBNAME");
		
		asoclocks	= new Hashtable ();
		locks 		= new LinkedList ();
		agv_runtime  = new Hashtable ();
		
		fdebug = false;
		
		if(fdebug)
		{
			reftimer = System.currentTimeMillis();
			try {
				pout = new PrintWriter(new FileOutputStream(robotid.concat(".txt")));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// Instance methods
	protected void initialise (Properties props)
	{
		super.initialise(props);
		
		// Calcule ifork limits min=[-1.45,-0.5025] max[1.035,0.5025]
		limits = new Line2[4];
		Point2 pt1 = new Point2(1.035,0.5025);
		Point2 pt2 = new Point2(1.035,-0.5025);
		Point2 pt3 = new Point2(-1.45,-0.5025);
		Point2 pt4 = new Point2(-1.45,0.5025);
		limits[0] = new Line2(pt1.x(), pt1.y(), pt2.x(), pt2.y());
		limits[1] = new Line2(pt2.x(), pt2.y(), pt3.x(), pt3.y());
		limits[2] = new Line2(pt3.x(), pt3.y(), pt4.x(), pt4.y());
		limits[3] = new Line2(pt4.x(), pt4.y(), pt1.x(), pt1.y());
		
		// This is REQUIRED for correct IForkPlan management
		gitem.task	= new IForkPlan ();
		
		// Set-up security coordination stuff
		try { doSecCoord = new Boolean (props.getProperty ("WHCOORD")).booleanValue (); } 	
		catch (Exception e) 	{ doSecCoord = false; }	
		
		if (doSecCoord)
		{
			syncitem		= new ItemSync ();
			synctuple 	= new Tuple (IForkTuple.SYNC, syncitem);
			
			firstwaiting = true;
			System.out.println ("\t>> Plant Security Coordination ACTIVE");
		}
		else
			System.out.println ("\t>> Plant Security Coordination INACTIVE");
	}	
	
	static public int parsePlan (String plan)
	{
		String		lplan;
		
		lplan = plan.toLowerCase ();
		if (lplan.equals ("stay"))				return STAY;
		else if (lplan.equals ("goto"))			return GOTO;
		else if (lplan.equals ("load"))			return LOAD;
		else if (lplan.equals ("unload"))			return UNLOAD;
		
		return STAY;
	}
	
	public void step (long ctime) 
	{
		// Check if a plan can be computed
		if ((world == null) || (task == null) || (lps == null) || finished || !initialised){
			if(debug){
				System.out.print("Planner: Plan no computed.");
				if(world==null) System.out.print(" world null.");
				if(task==null) System.out.print(" plan null.");
				if(lps==null) System.out.print(" lps null.");
				if(finished) System.out.print(" plan finished.");
				if(!initialised) System.out.print(" no initialised.");
				System.out.println();
			}
			if(lps!=null){
				calcPriority();
				sendCoordInfo();
			}
			
			if(fdebug)	dumpInfo();
			
			return;		
		}
		
		// Perform the plan supervision task
		do_plan ();
		
		if(fdebug)	dumpInfo();
	}
	
	private void dumpInfo(){
		long gtime;
		long curtime;
		int igivenway, ilaserexception, istopped;

		curtime = System.currentTimeMillis();
		gtime = curtime - reftimer;
		
		if(givenway)		igivenway = 1;			else	igivenway = 0;
		if(stopped)			istopped = 1;			else	istopped = 0;
		if(laserexception)	ilaserexception = 1;	else	ilaserexception = 0;

		if(lps!=null)
		{
			pout.println(gtime + "\t" + lps.cur.x() + "\t" + lps.cur.y() + "\t" + Math.toDegrees(lps.cur.alpha()) + "\t" + myPriority + "\t" + igivenway + "\t" + istopped + "\t" + ilaserexception);
			pout.flush();
		}
	}
	
	protected void do_plan () 
	{		
		// Perform coordination tasks
		calcPriority ();
		
		// Print coordination debug info
		if(debug){
			System.out.println(robotid+" myPriority = "+myPriority+" stopped="+stopped+" booked="+booked);
		    printLocks();
		    printAgvInfos();
		}
		sendCoordInfo ();
		checkCoordStatus ();
		
		// Compute and perform own plan
		if (newtask)
		{
			if(debug)	System.out.println("IforkPlanner.do_plantasks");
			do_plantasks ();
		}
		else
		{
			if(debug)	System.out.println("IforkPlanner.do_checktasks. plan "+task_k+" of "+(task_n-1)+" task  "+subplan_k+" of "+(subplan_n-1));
			do_checktask ();
		}
		
		if (completed)
		{
			task_k ++;
			newtask	= true;
			
			if (task_k == task_n)
			{
				finished	= true;				
				
				calcPriority ();
				sendCoordInfo ();
				if(debug)
				    System.out.println("  [IforkPlanner] Plan Finished. Prio="+myPriority+" booked="+booked);
				if (!failed)
					setStatus (ItemStatus.IDLE, "Task completed");

				if (debug)		System.out.println (this);
			}
		}
	}
	
	protected void do_checktask () 
	{
		if (lmodified)
		{
			if (win != null)		win.updateLocks (htmlLocks ());
			lmodified	= false;
		}
		
		switch (taskStatus ())
		{
		case ItemBehResult.T_FINISHED:
			subplan_k ++;
			
			if (subplan_k == subplan_n)
			{
				completed	= true;				
				setStatus (ItemStatus.COMPLETED, "Task " +  task[task_k] + " completed");
			}
			else
				do_sendtask ();

			if (debug)		System.out.println (this);
			break;

		case ItemBehResult.T_FAILED:
			failed 		= true;
			completed	= true;

			subplan_k	= 0;
			subplan_n	= 0;
			sub_stay ();

			setGoal (subplan[subplan_k]);
			setStatus (ItemStatus.FAILED, "Task " +  task[task_k] + " failed");

			task_k 		= task_n - 1;
				
			stopped 		= true;
			
			if (debug)		System.out.println (this);				
			break;

		case ItemBehResult.T_NOTYET:
		    if (debug)		System.out.println (this);
		default:
		}
	}
	
	protected void do_plantasks ()
	{		
		net_init ();
		
		subplan_n	= 0;
		stopped		= false;
		givenway	= false;
		switch (parsePlan (task[task_k].plan))
		{
		case LOAD:
			sub_navigate (true);
			sub_load ();
			sub_undock ();
			break;
		
		case UNLOAD:				
			sub_navigate (true);
			sub_unload ();
			sub_undock ();
			break;
				
		case GOTO:				
			sub_navigate (false);
			break;
				
		case STAY:
		default:		
			sub_stay ();
						
			stopped		= true;				
		}
		
		newtask		= false;
		failed 		= false;
		completed	= false;
		subplan_k	= 0;
		
		do_sendtask ();
		
		if (debug)		printPlan ();
	}
	
	protected void do_sendtask ()
	{
		if (!subplan[subplan_k].t_solved && debug)
		{
			subplan[subplan_k].t_target = 0;
			
			System.out.println ("--[Pla] Current task has not been solved yet!!");
			printTask (subplan_k);
		}
		if(subplan[subplan_k].t_labels != null){
			lastwp = subplan[subplan_k].t_labels[0];
		}
		
		if(debug){
		    System.out.println(subplan[subplan_k].toString());
		}
		
		setGoal (subplan[subplan_k]);
		setStatus (ItemStatus.OCCUPIED, subplan[subplan_k].toString ());

		if (win != null)		win.updatePlan (htmlPlan (), pnet);
	}
		
	private void net_init ()
	{
		PNNode			node;
		
		pnet		= new PetriNet ();
		node		= new PNNode (robotid);
		node.setTokens (1);
		pnet.addNode (node);
		lnodes	= new Vector ();
		lnodes.add (node);
	}
	
	private void net_places (String[] places)
	{
		int				i, j;
		Vector			cnodes;
		PNNode			snode, dnode;
		PNTransition		t;

		cnodes	= new Vector ();
		for (j = 0; j < places.length; j++)
		{
			dnode	= new PNNode (places[j]);
			dnode.setCapacity (1);
			
			pnet.addNode (dnode);
			cnodes.add (dnode);
			
			for (i = 0; i < lnodes.size (); i++)
			{			
				snode	= (PNNode) lnodes.elementAt (i);
				t		= new PNTransition ();
				
				pnet.addTransition (t);
				pnet.addEdge (snode, t);
				pnet.addEdge (t, dnode);
			}
		}
		lnodes	= cnodes;
	}
	
	private void net_single (String label, String reason)
	{
		int				i;
		PNNode			snode, dnode;
		PNTransition		t;

		dnode	= new PNNode (label);
		dnode.setCapacity (1);	
		pnet.addNode (dnode);
		
		for (i = 0; i < lnodes.size (); i++)
		{			
			snode	= (PNNode) lnodes.elementAt (i);
			t		= new PNTransition ();
			t.setName (reason);
			
			pnet.addTransition (t);
			pnet.addEdge (snode, t);
			pnet.addEdge (t, dnode);
		}

		lnodes	= new Vector ();
		lnodes.add (dnode);
	}
	
	private void sub_stay ()
	{
		// Stay on place
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan		= task[task_k].plan;				
		subplan[subplan_n].task		= "STANDBY";
		subplan_n ++;
	}
	
	private void sub_navigate (boolean isdock)
	{
		int				i, j, aux;
		double			a;
		Point2			paux, p1;
		Vector 			nodes;
		Position			tpos;
		GNodeSL			node;
		String			goal;
		String[]			labels;
		Vector			wps = null;

		// Set the fork for navigation
		tpos		= new Position (task[task_k].tpos);
		tpos.z (FORK_NAVIG);
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan			= task[task_k].plan;				
		subplan[subplan_n].task			= "FADJUST";
		subplan[subplan_n].tpos			= tpos;
		subplan_n ++;

		// Navigate the vehicle
		if (isdock)
		{
			wps		= topol.getInNodesSL (task[task_k].place);
			goal		= ((GNodeSL) wps.firstElement ()).getLabel ();
		}
		else
			goal		= task[task_k].place;
			
		nodes	= topol.calcPath (new Point2 (lps.cur.x (),lps.cur.y ()), goal);
		for (i = 0; i < nodes.size()-1; i++)
		{
			String			zone;
			GNodeSL			next;
					
			node		= (GNodeSL) nodes.get (i);
			next		= (GNodeSL) nodes.get (i+1);
			zone		= node.getFather ();
			
			paux		= world.getPos (node.getLabel(), zone);		
			aux		= world.doors ().index (node.getLabel ());
			
			if (aux != -1)
			{				
				Line2 l = world.doors ().at (aux).edge;
				if (l.orig().x() == paux.x() && l.orig().y() == paux.y())
					p1 = l.dest ();
				else 
					p1 = l.orig ();
				
				a = Math.atan2 (paux.y()-p1.y(),paux.x()-p1.x());
			}
			else 
				a = world.getAngle (node.getLabel ());
				
			GNodeFL		parent;
			Vector		doors;
			
			parent	= (GNodeFL) topol.getNode (zone);
			doors	= parent.getDoors (next.getFather ());
			labels	= new String[doors.size ()];
			for (j = 0; j < doors.size (); j++)
				labels[j]	= (String) doors.elementAt (j);
			
			subplan[subplan_n].place		= task[task_k].place;				
			subplan[subplan_n].plan			= task[task_k].plan;				
			subplan[subplan_n].task			= "NAVIGATE";
			subplan[subplan_n].t_labels		= labels;				
			subplan[subplan_n].tpos			= new Position (paux.x (), paux.y (), Math.PI+a);
			subplan[subplan_n].t_solved		= (labels.length == 1);
			subplan[subplan_n].tol_pos		= TOL_NAV_DIST;
			subplan[subplan_n].tol_head		= TOL_NAV_HEAD * Angles.DTOR * 2.0;
			subplan[subplan_n].path_mode		= GridPath.POLYLINE;
			subplan[subplan_n].path_src		= GridPath.GRID;
			subplan[subplan_n].spd_vmax		= NAV_SPD;
			subplan[subplan_n].spd_rmax		= NAV_TRN * Angles.DTOR;
			subplan[subplan_n].spd_end		= false;
			subplan_n ++;
			
			if (aux != -1)
			{
				paux = world.getPos (node.getLabel(), next.getFather ());
									
				subplan[subplan_n].plan			= task[task_k].plan;				
				subplan[subplan_n].place		= task[task_k].place;				
				subplan[subplan_n].task			= "CROSSDOOR";
				subplan[subplan_n].t_labels		= labels;				
				subplan[subplan_n].tpos			= new Position (paux.x (), paux.y (), Math.PI+a);
				subplan[subplan_n].t_solved		= (labels.length == 1);
				subplan[subplan_n].tol_pos		= TOL_NAV_DIST;
				subplan[subplan_n].tol_head		= TOL_NAV_HEAD * Angles.DTOR * 2.0;
				subplan[subplan_n].path_mode		= GridPath.POLYLINE;
				subplan[subplan_n].path_src		= GridPath.SEGMENTS;
				subplan[subplan_n].spd_vmax		= DOOR_SPD;
				subplan[subplan_n].spd_rmax		= DOOR_TRN * Angles.DTOR;
				subplan[subplan_n].spd_end		= false;			
				subplan_n ++;
				
				net_places (labels);
			}			
		}

		// Navigate to desired waypoint set
		node = (GNodeSL) nodes.get(i);
		paux = world.getPos (node.getLabel(),node.getFather());
		a = world.getAngle (node.getLabel ());

		if (isdock)
		{
			labels		= new String[wps.size ()];
			for (j = 0; j < wps.size (); j++)
				labels[j]	= ((GNodeSL) wps.elementAt (j)).getLabel ();
		}
		else
		{
			labels		= new String[1];
			labels[0]	= goal;
		}
		
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan			= task[task_k].plan;				
		subplan[subplan_n].task			= "NAVIGATE";
		subplan[subplan_n].t_labels		= labels;				
		subplan[subplan_n].tpos			= new Position (paux.x (), paux.y (), Math.PI+a);
		subplan[subplan_n].t_solved		= (labels.length == 1);
		subplan[subplan_n].tol_pos		= TOL_NAV_DIST;
		subplan[subplan_n].tol_head		= TOL_NAV_HEAD * Angles.DTOR * 2.0;
		subplan[subplan_n].path_mode		= GridPath.POLYLINE;
		subplan[subplan_n].path_src		= GridPath.GRID;
		subplan[subplan_n].spd_vmax		= NAV_SPD;
		subplan[subplan_n].spd_rmax		= NAV_TRN * Angles.DTOR;
		subplan[subplan_n].spd_end		= isdock;
		subplan_n ++;

		net_places (labels);
	}

	private void sub_load ()
	{
		Position			tpos;

		// Position the forks
		tpos	= new Position (subplan[subplan_n-1].tpos);
		tpos.z (task[task_k].tpos.z ());
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan			= task[task_k].plan;				
		subplan[subplan_n].task			= "FADJUST";
		subplan[subplan_n].tpos			= tpos;
		subplan_n ++;
					 
		// Wait for docking permission
		if (doSecCoord)
		{
			//subplan[subplan_n].label		= plan[plan_k].label;	
			subplan[subplan_n].place		= "checkWHMessage"+"|"+task[task_k].place; //this method will check the condition			
			subplan[subplan_n].plan			= task[task_k].plan;				
			subplan[subplan_n].task			= "COORDWAIT";
			subplan[subplan_n].tpos			= subplan[subplan_n-1].tpos;					
			subplan_n++;				
		}
		
		// Dock the vehicle
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan			= task[task_k].plan;				
		subplan[subplan_n].task			= "DOCK";
		subplan[subplan_n].t_labels		= new String[] { task[task_k].place };				
		subplan[subplan_n].tpos			= task[task_k].tpos;
		subplan[subplan_n].tol_pos		= TOL_DOCK_DIST;
		subplan[subplan_n].tol_head		= TOL_DOCK_HEAD * Angles.DTOR;
		subplan[subplan_n].path_mode	= GridPath.BSPLINE;
		subplan[subplan_n].path_src		= GridPath.POINTS;
		//subplan[subplan_n].path_src		= GridPath.MINLENGHT;
		subplan[subplan_n].spd_vmax		= DOCK_SPD;
		subplan[subplan_n].spd_rmax		= DOCK_TRN * Angles.DTOR;
		subplan[subplan_n].spd_end		= false;
		subplan_n ++;
		
		
		// Check palet
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan			= task[task_k].plan;				
		subplan[subplan_n].task			= "CHECKPAL";
		subplan[subplan_n].tpos		= task[task_k].tpos;
		subplan_n ++;
		
		// Send docking finished message AGVIN
		if (doSecCoord)
		{
			//subplan[subplan_n].label		= plan[plan_k].label;	
			subplan[subplan_n].place		= "sendAGVINMessage"+"|"+task[task_k].place; //this method will check the condition			
			subplan[subplan_n].plan			= task[task_k].plan;				
			subplan[subplan_n].task			= "COORDWAIT";
			subplan[subplan_n].tpos			= subplan[subplan_n-1].tpos;
			subplan_n++;				
		}

		// Lift the load
		tpos	= new Position (task[task_k].tpos);
		tpos.z (task[task_k].tpos.z () + FORK_OFFSET);
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan			= task[task_k].plan;				
		subplan[subplan_n].task			= "FLOAD";
		subplan[subplan_n].tpos			= tpos;
		subplan_n ++;
		
		net_single (task[task_k].place, "free");
	}
	
	private void sub_unload ()
	{
		Position			tpos;

		// Position the forks
		tpos	= new Position (task[task_k].tpos);
		tpos.z (task[task_k].tpos.z () + FORK_OFFSET);
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan			= task[task_k].plan;				
		subplan[subplan_n].task			= "FADJUST";
		subplan[subplan_n].tpos			= tpos;
		subplan_n ++;
		
		// Wait for docking permission
		if (doSecCoord)
		{
			//subplan[subplan_n].label		= plan[plan_k].label;	
			subplan[subplan_n].place		= "checkWHMessage"+"|"+task[task_k].place; //this method will check the condition			
			subplan[subplan_n].plan			= task[task_k].plan;				
			subplan[subplan_n].task			= "COORDWAIT";
			subplan[subplan_n].tpos			= subplan[subplan_n-1].tpos;					
			subplan_n++;				
		}
		
		// Dock the vehicle
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan			= task[task_k].plan;				
		subplan[subplan_n].task			= "DOCK";
		subplan[subplan_n].t_labels		= new String[] { task[task_k].place };				
		subplan[subplan_n].tpos			= task[task_k].tpos;
		subplan[subplan_n].tol_pos		= TOL_DOCK_DIST;
		subplan[subplan_n].tol_head		= TOL_DOCK_HEAD * Angles.DTOR;
		subplan[subplan_n].path_mode		= GridPath.BSPLINE;
		subplan[subplan_n].path_src		= GridPath.POINTS;
		//subplan[subplan_n].path_src		= GridPath.MINLENGHT;
		subplan[subplan_n].spd_vmax		= DOCK_SPD;
		subplan[subplan_n].spd_rmax		= DOCK_TRN * Angles.DTOR;
		subplan[subplan_n].spd_end		= false;
		subplan_n ++;
		
		// Send docking finished message AGVIN
		if (doSecCoord)
		{
			//subplan[subplan_n].label		= plan[plan_k].label;	
			subplan[subplan_n].place		= "sendAGVINMessage"+"|"+task[task_k].place; //this method will check the condition			
			subplan[subplan_n].plan			= task[task_k].plan;				
			subplan[subplan_n].task			= "COORDWAIT";
			subplan[subplan_n].tpos			= subplan[subplan_n-1].tpos;
			subplan_n++;				
		}

		// Unlift the load
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan			= task[task_k].plan;				
		subplan[subplan_n].task			= "FUNLOAD";
		subplan[subplan_n].tpos			= task[task_k].tpos;
		subplan_n ++;
		
		net_single (task[task_k].place, "free");
	}
	
	private void sub_undock ()
	{
		double			x, y, a;
		Position			tpos;

		// Undock the vehicle
		a	= task[task_k].tpos.alpha ();
		x	= task[task_k].tpos.x () + UNDOCK_DIST * Math.cos (a);
		y	= task[task_k].tpos.y () + UNDOCK_DIST * Math.sin (a);			
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan			= task[task_k].plan;				
		subplan[subplan_n].task			= "UNDOCK";
		subplan[subplan_n].t_labels		= new String[] { task[task_k].place };				
		subplan[subplan_n].tpos			= new Position (x, y, a);
		subplan[subplan_n].tol_pos		= TOL_NAV_DIST;
		subplan[subplan_n].tol_head		= TOL_NAV_HEAD * Angles.DTOR * 2.0;
		subplan[subplan_n].path_mode		= GridPath.POLYLINE;
		subplan[subplan_n].path_src		= GridPath.SEGMENTS;
		subplan[subplan_n].spd_vmax		= UNDOCK_SPD;
		subplan[subplan_n].spd_rmax		= IForkPlan.NA;
		subplan[subplan_n].spd_end		= false;
		subplan_n ++;
	
		// Set the fork for navigation
		tpos	= new Position (subplan[subplan_n-1].tpos);
		tpos.z (FORK_NAVIG);
		subplan[subplan_n].place		= task[task_k].place;				
		subplan[subplan_n].plan			= task[task_k].plan;				
		subplan[subplan_n].task			= "FADJUST";
		subplan[subplan_n].tpos			= tpos;
		subplan_n ++;
		
		// Send the operation finished message
		if (doSecCoord)
		{
			//subplan[subplan_n].label		= plan[plan_k].label;	
			subplan[subplan_n].place		= "sendByeMessage"+"|"+task[task_k].place; //this method will check the condition			
			subplan[subplan_n].plan			= task[task_k].plan;				
			subplan[subplan_n].task			= "COORDWAIT";
			subplan[subplan_n].tpos			= subplan[subplan_n-1].tpos;
			subplan_n++;				
		}
		
		net_single ("u" + task[task_k].place, "finished");
	}

	protected void sendCoordInfo ()
	{
		String			lbooked;
		
		coorditem.set (myPriority, lps.cur, System.currentTimeMillis());
		coorditem.bk_action = ItemCoordination.ACTION_NONE;
		lbooked		= booked;
		if(subplan_k < subplan_n){
		    if(subplan[subplan_k].t_labels != null)
		    	coorditem.goal = subplan[subplan_k].t_labels[0];
			else 
		    	coorditem.goal = null;
			coorditem.task = IForkController.parseTask (subplan[subplan_k].task);
		
		
			switch (IForkController.parseTask (subplan[subplan_k].task))
			{
				
				// Cuando cruza una puerta ocupa el wp (lo desocupa cuando se aleja 3m navegando) 
				case IForkController.CROSSDOOR:
					if(booked == null && lastwp != null && (!locks.contains(lastwp))){
						
						booked = lastwp;
						coorditem.booked = booked;
						coorditem.bk_action = ItemCoordination.ACTION_OCCUPED;
						//if(debug) 
							System.out.println("***"+ robotid+"*** ocupa puerta "+booked+" (CROSSDOOR)");
					}
					break;
									
				// Cuando navega y esta a una distancia elimina el WP ocupado (para que no quede en medio)
				case IForkController.NAVIGATE:
					if(subplan[subplan_k].t_labels == null || subplan[subplan_k].t_labels[0] == null){
						System.out.println("[IforkPlaner] Navegando a wp null: plan = "+subplan[subplan_k].toString());
						break;  
					}
					String wp = subplan[subplan_k].t_labels[0];
					if (booked == null)
					{
						// Ocupa un WP (que no sea buf*) que no este ocupado y a menos de 3m
						if ( (!locks.contains(wp)) && distTo(wp)<3 && !wp.startsWith("buf")){
						//if (isnearto(wp,3)){
							booked = wp;
							coorditem.booked = booked;
							coorditem.bk_action = ItemCoordination.ACTION_OCCUPED;
							//if(debug) 
								System.out.println("***"+ robotid+"*** ocupa wp "+booked+" (NAVIGATE)");
							//System.out.println("["+robotid+"] ocupa "+booked);
						}
						break;
					}
					else if (distTo(booked)>3) // cuando se aleja 3 metros se elimina el booked
					{
						// Desocupa un WP cuando se aleja 3m
						//if(debug) 
							System.out.println("***"+ robotid+"*** libera wp "+booked+" (NAVIGATE)");
						coorditem.booked = booked;	// Si esta a una distancia se elimina el booked (da igual si esta en warning)
						coorditem.bk_action = ItemCoordination.ACTION_FREE;
						booked = null;
					}
					else if ( !booked.equals(wp) && (!locks.contains(wp)) && distTo(wp)<3 && !wp.startsWith("buf")){
						System.out.println("***"+ robotid+"*** libera wp "+booked+" por estar mas cerca de "+wp);
						coorditem.booked = booked;	// Si esta a una distancia se elimina el booked (da igual si esta en warning)
						coorditem.bk_action = ItemCoordination.ACTION_FREE;
						booked = null;
					}
					break;
				default:
			}
		
		}
		else{
		    coorditem.goal = null;
		    coorditem.task = IForkController.parseTask ("STANDBY");
		}
		
		if(debug)
		    System.out.println("["+robotid+"] sendCoordInfo: booked="+booked+" Priority="+myPriority+" Pos="+lps.cur);
		
		coordtuple.space = LindaEntryFilter.ANY;
		linda.write (coordtuple);
		
		if (booked != lbooked)			lmodified = true;
	}
	
	protected boolean enableGivenway(int cause){
	    if (!givenway)
		{
			task_backup = subplan[subplan_k].task;
			subplan[subplan_k].task = "COORDWAIT";
			setGoal (subplan[subplan_k]);
			givenway = true;
			givencause = cause;
		    return true;
		}
		return false;
	}
	
	protected void disableGivenway(){
	    if (givenway)
		{
		    //if(debug){ 
		    	System.out.println("["+robotid+"] Recuperado Paso. Prio="+myPriority);
		    	printAgvInfos();
		    	printLocks();
		    //}
			subplan[subplan_k].task	= task_backup;	
			setGoal (subplan[subplan_k]);
			setStatus (ItemStatus.OCCUPIED, subplan[subplan_k].toString ());
			givenway = false;
			givencause = -1;
		}
	}
	
	
	
	protected synchronized void checkCoordStatus ()
	{
		ItemCoordination		item;
		Enumeration			infos = agvinfo.keys();
		String				wpname, rname;
		String				wpgoal = null;
		boolean				warn =false;
		double 				dist;
		
		if(subplan[subplan_k].t_labels != null && subplan[subplan_k].t_labels.length > 0)
			wpgoal = subplan[subplan_k].t_labels[0]; // Wp al que se dirige
		if(debug)
			System.out.println("wpgoal = "+wpgoal+" givenway = "+givenway+" warning = "+warning+" stopped = "+stopped);
		if (stopped)				return;
		
		// Check for waypoint booking status
		for (ListIterator l = locks.listIterator(); l.hasNext();)
		{
			wpname = (String)l.next();  // wp ocupado
			
			// Si el WP ocupado es el suyo lo ignora
			if ((booked != null) && booked.equalsIgnoreCase (wpname)) 			continue;
			
			//System.out.println("["+robotid+"] dist a "+wpname+" = "+world.wps().at(wpname).getPos());
			
			// Si esta Navegando hacia un WP ocupado cede el paso (Prioridad = 0)
			if ( ((IForkController.parseTask (subplan[subplan_k].task) == IForkController.NAVIGATE) || (givenway && givencause==1) ) ) // TODO esto es una chapuza
			{
				// Si se acerca a un wp ocupado una distancia minima cede el paso
				if(distTo(wpname) < POINT_DIST || (distTo(wpname) < 4 && world.getType(wpname)==World.DOCK)){
				    rname = (String)asoclocks.get(wpname); // robot que ha ocupado el WP
				    item		= (ItemCoordination)agvinfo.get (rname);
				    if(item!=null && debug) 
				    	System.out.println("  [IforkPlanner] "+wpname+" esta ocupado por "+rname+" myprio="+myPriority+" supri="+item.priority);
				    //if(item == null || item.priority <= myPriority){
					if(!priority(wpgoal,wpname,item)){
				    	myPriority = 2;
				    	if(enableGivenway(1)){
				    	    setStatus (ItemStatus.WAIT, "Resource <" +  wpname + "> booked by "+rname);
				    	    //if(debug)
				    	    	System.out.println("["+robotid+"] Cediendo Paso por "+rname+" (booked) 3m: dist a "+wpname+" = "+distTo(wpname)+ " suPrio="+item.priority+" myPrio="+myPriority);
				    	}
						else 
						    if(debug) 
						        System.out.println("["+robotid+"] Continua Cediendo Paso (booked) 3m: dist a "+wpname+" = "+distTo(wpname) );
						return;
				    }
				}
				
				// Si esta cerca (menor de 8m) de un WP ocupado cede el paso en 2 casos:
				if(wpgoal!=null && wpname != null){
					// Si el wp al que se dirige esta ocupado
					if(wpgoal.equals(wpname) && ( (world.getType(wpname)==World.DOOR && distTo(wpname)<7) || (world.getType(wpname)!=World.DOOR && distTo(wpname)<8)) ){
						myPriority = 2;
						
						if(enableGivenway(1)){
						    setStatus (ItemStatus.WAIT, "Resource <" +  wpname + "> booked.");
						    //if(debug)
						        System.out.println("["+robotid+"] Cediendo Paso (booked) destino "+wpgoal+" ocupado 5m: dist a "+wpname+" = "+distTo(wpname));				    	}
						else 
						    if(debug) 
						        System.out.println("["+robotid+"] Continua Cediendo Paso (booked) destino "+wpgoal+" ocupado 5m: dist a "+wpname+" = "+distTo(wpname));
						return;
						
					}
					// El wp al que se dirige esta cerca de otro ocupado
					else if(distwps(wpname,wpgoal)<4.0 && (distTo(wpname)<8 || distTo(wpgoal)<8)){	
					    myPriority = 2;
					    if(enableGivenway(1)){
					        setStatus (ItemStatus.WAIT, "Resource <" +  wpname + "> booked");
							//if(debug)
					        	System.out.println("["+robotid+"] Cediendo Paso (booked) destino "+wpgoal+ " cercano a un wp ocupado "+wpname+" dist1="+distwps(wpname,wpgoal)+" dist2="+distTo(wpname)+" dist3="+distTo(wpgoal));
					    }
						else 
						    if(debug) 
						        System.out.println("["+robotid+"] Continua Cediendo Paso (booked) destino "+wpgoal+ " cercano a un wp ocupado "+wpname+" dist1="+distwps(wpname,wpgoal)+" dist2="+distTo(wpname)+" dist3="+distTo(wpgoal));
					    return;						
					}
				}
			}
			
		}	
		
		// Check for robot proximity conditions
		while (infos.hasMoreElements())
		{
			rname	= (String)infos.nextElement();	// Nombre de la carretilla
			item		= (ItemCoordination)agvinfo.get (rname); // Toda la informacion de la carretilla	
			dist	= iforkdist(lps.cur, item.position);
			if(item !=null) wpname = item.goal;
			else			wpname = null;
			
			
			// Si dos carretillas van al mismo sitio
			if((IForkController.parseTask (subplan[subplan_k].task) == IForkController.NAVIGATE || (givenway && givencause==2)) && (wpname!=null && wpgoal!=null) && (!wpname.equals(booked))){
			    if(wpgoal.equals(wpname)){
			    	if((distTo(wpname) <7) && !priority(wpgoal,wpname,item)){
				        myPriority = 3;
				        if(enableGivenway(2)){
				            setStatus (ItemStatus.WAIT, "Giving way to " +  rname+" by "+wpname);
				            //if(debug) 
				            	System.out.println("["+robotid+"] Cediendo Paso. Al wp "+wpgoal+" se dirige "+rname +" con mas prioridad. Mydist="+distTo(wpname));
				        }
				        else 
				            if(debug) 
				                System.out.println("["+robotid+"] Continua Cediendo Paso. Al wp "+wpgoal+" se dirige "+rname +" con mas prioridad. Mydist="+distTo(wpname));
				        return;
			    	}
			        
			    }
			    else if(distwps(wpname,wpgoal)<4.0 && ((distTo(wpname)<5 || distTo(wpgoal)<5) && distTo(item.position, wpname)<5) && !priority1(wpgoal,wpname,item)){
					myPriority = 3;
					if(enableGivenway(2)){
						setStatus (ItemStatus.WAIT, "Giving way to " +  rname+" by "+wpname);
						//if(debug) 
							System.out.println("["+robotid+"] Cediendo Paso. Al wp "+wpgoal+" esta cerca de "+wpname +" y se dirige "+rname +" con mas prioridad. Mydist="+distTo(wpname));
			        }
			        else 
			            if(debug) 
							System.out.println("["+robotid+"] Continua Cediendo Paso. Al wp "+wpgoal+" esta cerca de "+wpname +" y se dirige "+rname +" con mas prioridad. Mydist="+distTo(wpname));
			        return;	
				}
			}
			
//			 Si esta cruzando una puerta y se encuentra una carretilla a menos de 3m, se para.
			if( (IForkController.parseTask (subplan[subplan_k].task) == IForkController.CROSSDOOR || (givenway && givencause==3)) && (distTo(item.position, wpgoal) < POINT_DIST) ){
			    if(enableGivenway(3)){
					setStatus (ItemStatus.WAIT, "Giving way. " +  rname +" block door.");
					//if(debug) 
						System.out.println("["+robotid+"] Cediendo Paso. CROSSDOOR. Dist a "+rname+" = "+dist+", Dist to door "+wpgoal+"="+ distTo(item.position, wpgoal) + " Myprio="+myPriority+" SuPrio="+item.priority);
		        }
		        else 
		            if(debug) 
						System.out.println("["+robotid+"] Continua Cediendo Paso CROSSDOOR. dist a "+rname+" = "+dist+"("+lps.cur.distance(item.position)+") Myprio="+myPriority+" SuPrio="+item.priority);
				myPriority = 5;
				return;	
			}
			
			//	Si esta cerca de una carretilla cede el paso segun prioridad y nombre			
			if(securityDist(lps.cur,item.position))
			//if (dist < AGV_DIST)
			{
				if (	(IForkController.parseTask (subplan[subplan_k].task) == IForkController.NAVIGATE || (givenway && givencause==4) ) &&
						(item.priority < myPriority ||(item.priority == myPriority && !priority(rname)))
					)
				{
				    if(enableGivenway(4)){
						setStatus (ItemStatus.WAIT, "Giving way to " +  rname);
						//if(debug) 
							System.out.println("["+robotid+"] Cediendo Paso. dist a "+rname+" = "+dist+"("+lps.cur.distance(item.position)+") Myprio="+myPriority+" SuPrio="+item.priority);
			        }
			        else 
			            if(debug) 
							System.out.println("["+robotid+"] Continua Cediendo Paso. dist a "+rname+" = "+dist+"("+lps.cur.distance(item.position)+") Myprio="+myPriority+" SuPrio="+item.priority);
					myPriority = 1;
					return;	
				}
				
			}
			// Si esta navegando a una distancia cercana a otra carretilla -> warning
			if((dist < (WARN_DIST) && IForkController.parseTask (subplan[subplan_k].task) == IForkController.NAVIGATE)){   
				if(IForkController.parseTask (subplan[subplan_k].task) == IForkController.NAVIGATE){
					warn = true;
				}
			}
						
		}
		
		if(IForkController.parseTask (subplan[subplan_k].task) == IForkController.NAVIGATE){
			if(!warning && warn){	// Envia plan con velocidad warning
				warning = true;
				subplan[subplan_k].spd_vmax = Math.min(WRN_SPD, subplan[subplan_k].spd_vmax);
				setGoal (subplan[subplan_k]);
				if(debug) System.out.println("["+robotid+"] Velocidad limitada (WARNING)");
			}
			else if(warning && !warn){	// Envia plan con velocidad normal
				warning = false;
				subplan[subplan_k].spd_vmax = NAV_SPD;
				setGoal (subplan[subplan_k]);
				if(debug) System.out.println("["+robotid+"] Velocidad NORMAL");
			}
		}else{
			warning = false;
		}
				
		disableGivenway();
	}

	protected void calcPriority ()
	{
		//if (givenway || stopped){
		// Si ha terminado las tareas, prioridad muy baja (si no ocupa ningun wp que no sea de espera).
		if (subplan == null || laserexception || (stopped && (booked == null))){
			myPriority = 10;
		    return;
		}
		
	    // Si esta cediendo el paso, su prioridad no cambia
		if(givenway) return;
		if(subplan_k >= subplan_n) return;
		
		switch (IForkController.parseTask (subplan[subplan_k].task))
		{
			// Tendria que tener baja, pero como se utiliza cuando atraca tiene alta
			case IForkController.STANDBY:		myPriority = -4;		break;
			
			// Navegando. Prioridad media
			case IForkController.NAVIGATE:	myPriority = -3;	break;
			
			// Esperando Coordinacion. Prioridad alta
			case IForkController.COORDWAIT:	myPriority = -5;	break;
			
			// Tareas de atraque. Prioridad  muy alta
			case IForkController.DOCK:
			case IForkController.UNDOCK:
			case IForkController.FLOAD:
			case IForkController.FUNLOAD:
			case IForkController.FADJUST:		myPriority = -7;		break;
			
			// Cruzando puerta. Prioridad maxima
			case IForkController.CROSSDOOR:		myPriority = -10;		break;	
			
			default:						myPriority = 0;
		}
		
		// Si esta bloqueando un wp la prioridad es medio alta (como minimo)
		if (booked != null) 
			myPriority = Math.min (myPriority , -3);
	}		
			
	public int taskStatus ()
	{	
		Class myclass = this.getClass();
		//System.out.println ("\t[IFORKPLANNER]--->taskStatus");
		if (doSecCoord)
		{
			//System.out.print ("\t[IFORKPLANNER]--->taskStatus whCoordination active for "+ItemGoal.toString(subplan[subplan_k].task));
			if (IForkController.parseTask (subplan[subplan_k].task) == IForkController.COORDWAIT) 
			{
			   if( subplan[subplan_k].place.indexOf('|') == -1 ){
			   		if(debug) System.out.println("IForkPlanner: taskStatus CoordWaiting givenway. Return NOTYET");
			   		return ItemBehResult.T_NOTYET; // Esta parada por coordinacion entre carretillas 
			   }
				/* check condition */
				try
				{	String method=null;
					try{
						method = subplan[subplan_k].place.substring(0,subplan[subplan_k].place.indexOf('|'));
					}catch(StringIndexOutOfBoundsException strex){
						method=new String(subplan[subplan_k].place);
					}

					Method checkMethod = null;
					try
					{
						checkMethod = myclass.getMethod(method,null);
					} catch (Exception e)
					{
						System.out.println ("  [IForkPlanner]: WARNING!! "+e+" method="+method);
						return ItemBehResult.T_NOTYET;
					}
					//System.out.print ("\t[IFORKPLANNER]--->Checking condition");
					if (((Boolean)checkMethod.invoke(this,null)).booleanValue())
					{
						firstwaiting = true; //Next time will be the first waiting
						//System.out.println ("TRUE");
						if(debug) System.out.println("IForkPlanner: taskStatus CoordWaiting Check. Return FINISHED ("+method);
						return ItemBehResult.T_FINISHED;
					}
					else
					{
						//System.out.print (".");
						if(debug) System.out.println("IForkPlanner: taskStatus CoordWaiting Check. Return NOTYET ("+method);
						return ItemBehResult.T_NOTYET;
					}
				
				} catch (Exception e)
				{
					System.out.println ("[IFORKPLANNER]: while checking WAITUNTIL condition");
					e.printStackTrace();
					return ItemBehResult.T_NOTYET;
				}			
			}
		}
		
		return super.taskStatus ();	
	}
	
	/* "Check Condition" Implementations */	
	public Boolean checkWHMessage ()
	{
		//System.out.print ("\t[IFORKPLANNER]--->inGoal whCoordination active");
		/* send ItemWareHouse throught linda */
		if (firstwaiting)
		{
			//System.out.println("Voy a enviar WAIT "+System.currentTimeMillis());
			String label  = subplan[subplan_k].place.substring(subplan[subplan_k].place.indexOf("|")+1);
			
//			System.out.println ("IForkPlanner: [Sending WAITING on "+label+"] ");
			synctuple.space = LindaEntryFilter.ANY;
			syncitem.dockid = label;
			syncitem.message = ItemSync.WAITING;
			linda.write (synctuple);
			firstwaiting = false;
			//System.out.println("He enviado WAIT sycntuple="+synctuple+" tiempo="+System.currentTimeMillis());
			return new Boolean(false);
		}
		//return (new Boolean(true));
		
		Tuple t = new Tuple(IForkTuple.SYNC,syncitem);
		Tuple t2 = linda.take(t);
		
		if (t2 != null){
			ItemSync item = (ItemSync)t2.value;
			if(item!=null){
				//System.out.println("IForkPlanner: recibido continue Sync"+item);
				return (new Boolean (item.message == ItemSync.CONTINUE));
			}
		}			
		return new Boolean(false);
	}

	public Boolean sendByeMessage ()
	{
		String label  = subplan[subplan_k].place.substring(subplan[subplan_k].place.indexOf("|")+1);
		
		//System.out.println (" [Sending BYE on "+label+"] ");
		synctuple.space = LindaEntryFilter.ANY;
		syncitem.dockid = label;
		syncitem.message = ItemSync.BYE;
		linda.write (synctuple);
		return new Boolean(true);	
	}
	
	public Boolean sendAGVINMessage ()
	{
		String label  = subplan[subplan_k].place.substring(subplan[subplan_k].place.indexOf("|")+1);
		
		//System.out.println (" [Sending AGVIN on "+label+"] ");
		synctuple.space = LindaEntryFilter.ANY;
		syncitem.dockid = label;
		syncitem.message = ItemSync.AGVIN;
		linda.write (synctuple);
		return new Boolean(true);	
	}

	public void setGoal (IForkPlan plan)
	{
		behtime 	= System.currentTimeMillis ();
		behresult	= ItemBehResult.T_NOTYET;
		
		if (debug)		System.out.println ("  [Pla] Sending task [" + plan + "] with ID: " + behtime);

		((IForkPlan) gitem.task).set (plan);
		gitem.set (behtime);
		linda.write (gtuple);
	}
	
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
				
		if (item.props_topol != null)
			topol = new HTopolMap (world, item.props_topol);
		
		if ((world == null) || (topol == null))
		{
			System.err.println ("--[iFrkPla] World and topological maps MUST be specified. Aborting module.");
			initialised	= false;		
			return;
		}
		
		if (localgfx) 	win	= new IForkPlanWindow (space);
	}	
	
	public void notify_coord (String space, ItemCoordination coord)
	{
		
		if(agv_runtime.contains(space)){
			if((System.currentTimeMillis() - ((Long)agv_runtime.get(space)).longValue())<20000){
				//System.out.println("  [IForkPlanner]: notify_coord Recibido coord de "+space+" estando borrado.");
				return;
			}else{
				//System.out.println("  [IForkPlanner]: notify_coord "+space+" esta eliminado mas de 20 seg. Borrar de agv_runtime");
				agv_runtime.remove(space);
			}
		}

		if (robotid!=null && !robotid.equalsIgnoreCase(space))					{
			agvinfo.put(space,coord);	
			
			if (coord.booked != null && !coord.booked.startsWith("buf"))
			{			
				if(coord.bk_action == ItemCoordination.ACTION_FREE){
					locks.remove (coord.booked);
					asoclocks.remove (coord.booked);
				}
				else if(coord.bk_action == ItemCoordination.ACTION_OCCUPED){
					if(!locks.contains(coord.booked))
						locks.add (coord.booked);
					asoclocks.put (coord.booked, space);
				}
				else{
					System.out.println("  [IforkPlanner] Comando desconocido (ItemCoordination):"+coord);
				}
				
				lmodified	= true;
			}
		}
//		System.out.println ("  [iFrkPla] Received message from <"+space+"> self=<"+robotid+">");
	}

	public void notify_plan (String space, ItemPlan item)
	{
		super.notify_plan (space, item);
		
//		System.out.println ("  [iFrkPla] Plan received: "+item);
	}
		
	public void notify_status(String space, ItemStatus item){	
		if(item.type==ItemStatus.ALARM){
			laserexception = item.message.indexOf("<LASER>") != -1;
			if(debug)
				System.out.println("[IForkPlanner:]notify_status ALARM received. item="+item+" laserexc="+laserexception);
		}
	}
	
	public synchronized void notify_delrobot(String space,ItemDelRobot item){
		Enumeration enum1;
		String wp;
		
		if(item.cmd==ItemDelRobot.INFO){
			//System.out.println("  [IForkPlanner] Recibido tuple INFO "+space+" item="+item);
		}
		else if(item.cmd==ItemDelRobot.DELETE){
			//System.out.println("  [IForkPlanner] Recibido tuple delrobot "+item+" space="+space+" robotid="+robotid);
			
			agv_runtime.put(item.robotid,new Long(System.currentTimeMillis()));
			
			if(robotid!=null && robotid.equalsIgnoreCase(item.robotid)){
				System.out.println("  [IForkPlanner] Stop robot "+robotid);
				stop();
				return;
			}
			if(asoclocks.containsValue(item.robotid)){
				for(enum1 = asoclocks.keys();enum1.hasMoreElements();){
					wp = (String)enum1.nextElement();
					if(item.robotid.equalsIgnoreCase((String)asoclocks.get(wp))){
						System.out.println("  [IForkPlanner] Robot "+robotid+": eliminando "+wp+" del robot "+item.robotid);
						asoclocks.remove(wp);
						locks.remove(wp);
					}
				}
			}
			agvinfo.remove(item.robotid);
		}
	}
//	public synchronized void notify_delrobot(String space,ItemDelRobot item){
//		System.out.println("  [IForkPlanner] Recibido tuple delrobot "+item+" space="+space+" myrobotid="+robotid);
//	
//	}

	
	public void printPlan ()
	{
		int			i;
		
		System.out.println ("  [iFrkPla] Generated full plan for <"+task[task_k]+">");
		for (i = 0; i < subplan_n; i++)
			printTask (i);
	}

	public void printTask (int i)
	{
		int			j;
		
		System.out.print ("\t"+subplan[i].task+" (");
		if (subplan[i].t_labels != null)
			for (j = 0; j < subplan[i].t_labels.length; j++)
			{
				System.out.print (subplan[i].t_labels[j]);
				if (j+1 < subplan[i].t_labels.length)
					System.out.print (" ");
			}
		System.out.println (")");
	}

	public String htmlPlan ()
	{
		int			i, j;
		String		out;
		
		out	= "<HTML><B>"+task[task_k]+"</B><BR>";
		out	+= "<FONT SIZE=3>";
		for (i = 0; i < subplan_n; i++)
		{
			if (i == subplan_k)			out	+= "<FONT COLOR=#FF0000>";

			out	+= "&nbsp;&nbsp;&nbsp;&nbsp;"+subplan[i].task+" (";
			if (subplan[i].t_labels != null)
				for (j = 0; j < subplan[i].t_labels.length; j++)
				{
					if (j == subplan[i].t_target)				out	+= "<U>";
					
					out	+= subplan[i].t_labels[j];
					
					if (j == subplan[i].t_target)				out	+= "</U>";			
					if (j+1 < subplan[i].t_labels.length)		out	+= " ";
				}
			out	+= ")<BR>";
			
			if (i == subplan_k)			out	+= "</FONT>";
		}
		
		return out + "</HTML>";
	}

	public String htmlLocks ()
	{
		int			i;
		boolean		mlock;
		String		label;
		String		out;
		
		out	= "<HTML><FONT SIZE=3>";
		
		if (booked != null)
			out	+= "<B>"+booked+"</B>&nbsp;&nbsp;&nbsp;&nbsp;<I>self</I><BR>";	
		
		for (i = 0; i < locks.size (); i++)
		{
			label	= (String) locks.get (i);
			mlock	= subplan[subplan_k].containsLabel (label);
			
			if (mlock)		out	+= "<FONT COLOR=#FF0000>";
			
			out	+= "<B>" + locks.get (i) + "</B>&nbsp;&nbsp;&nbsp;&nbsp;" + asoclocks.get (label) + "<BR>";			

			if (mlock)		out	+= "</FONT>";
		}
		
		return out + "</HTML>";
	}

	public String toString ()
	{
		String			str;
		
		str		= super.toString ();
		
		if (subplan_k >= subplan_n)
			str 	+= "Subtask completed.";
		else
			str	+= "Subtask: " + subplan[subplan_k].task;
		
		return str;
	}
	
	static public Integer toInteger (String id)
	{
		if (id.equalsIgnoreCase("stay"))
			return new Integer(STAY);
		else if (id.equalsIgnoreCase("goto"))
			return new Integer(GOTO);
		else if (id.equalsIgnoreCase("load"))
			return new Integer(LOAD);
		else if (id.equalsIgnoreCase("unload"))
			return new Integer(UNLOAD);
		else {
			System.out.println("iForkPlanner::Warning:unknown id: " + id);
			return null;
		}
	}
	
	static public String toString (int task)
	{
		switch (task)
		{
			case STAY:			return "stay";
			case GOTO:			return "goto";
			case LOAD:			return "load";
			case UNLOAD:		return "unload";
			default:			return "n/a";
		}
	}
	
//	public boolean isnearto(String wpname,double dist){
//		int type = world.getType(wpname);
//		boolean cond = false;
//		if(type == World.WP)
//			cond = lps.cur.distance (world.wps().at(wpname).getPos()) < dist;	
//		else if(type == World.DOOR){
//			Line2 path = world.doors().at(wpname).path;
//			cond = path.segDistance(lps.cur.x(),lps.cur.y()) < dist;
//		}
//		return cond;
//	}
	
	public double distTo(String wpname){
		int type = world.getType(wpname);
		if(type == World.WP)
			return lps.cur.distance (world.wps().at(wpname).getPos());	
		else if(type == World.DOOR){
			Line2 path = world.doors().at(wpname).path;
			return path.segDistance(lps.cur.x(),lps.cur.y());
		}
		return Double.MAX_VALUE;
	}
	
	public double distTo(Position pos, String wpname){
		int type = world.getType(wpname);
		if(type == World.WP)
			return pos.distance (world.wps().at(wpname).getPos());	
		else if(type == World.DOOR){
			Line2 path = world.doors().at(wpname).path;
			return path.segDistance(pos.x(),pos.y());
		}
		return Double.MAX_VALUE;
	}
	
	// Calcula si se tiene prioridad frente a la carretilla "rname"
	// Para ello tiene en cuenta donde van las dos carretillas
	public boolean priority(String rname){
	    if(!agvinfo.containsKey(rname)) return true;
	    if(subplan[subplan_k].t_labels == null) return true;
	    
	    String goal = subplan[subplan_k].t_labels[0];
	    ItemCoordination item = (ItemCoordination) agvinfo.get(rname);
	    if(item==null || item.goal == null) return true;
	    double d1 = distTo(lps.cur, item.goal);	// Distancia de la carretilla al wp al que va otra carretilla
	    double d2 = distTo(item.position, goal); // Distancia de la otra carretilla al wp al que va la carretilla
	    //System.out.println(robotid+" d1 = "+d1+ ", "+rname+" "+" d2 = "+d2);
	    if(d1 < d2) return true;
	    return false;
	}
	
	// Calcula si se tiene prioridad (true tiene mas)
	public boolean priority(String miwp, String suwp, ItemCoordination item){
	    if(myPriority<item.priority) return true;
	    if(myPriority>item.priority) return false;
	    if(suwp == null || miwp == null || item == null) return false;
	    double d1 = distTo(lps.cur, suwp);	// Distancia de la carretilla al wp al que va otra carretilla
	    double d2 = distTo(item.position, miwp); // Distancia de la otra carretilla al wp al que va la carretilla
	    //System.out.println(robotid+" d1 = "+d1+ ", "+rname+" "+" d2 = "+d2);
	    if(d1 < d2) return true;
	    return false;
	}
	
	// Calcula si se tiene prioridad (true tiene mas)
	public boolean priority1(String miwp, String suwp, ItemCoordination item){
	    if(myPriority<item.priority) return true;
	    if(myPriority>item.priority) return false;
	    if(suwp == null || miwp == null || item == null) return false;
	    double d1 = distTo(lps.cur, miwp);	// Distancia de la carretilla al wp al que va otra carretilla
	    double d2 = distTo(item.position, suwp); // Distancia de la otra carretilla al wp al que va la carretilla
	    //System.out.println(robotid+" d1 = "+d1+ ", "+rname+" "+" d2 = "+d2);
	    if(d1 < d2) return true;
	    return false;
	}
	
	// Esta a una distancia de seguridad, cuando estan a menos de 2 metros,
	// o a menos de AGVDIST en el caso de que 
	public boolean securityDist(Position pos1, Position pos2){
	    double dist = iforkdist(pos1,pos2);
	    if(dist < 2) return true;
	    if(dist < AGV_DIST){
	        double ang1 = Math.atan2(pos2.y()-pos1.y(), pos2.x()-pos1.x());
	        double ang2 = ang1 + Math.PI;
	        
	        // Si pos2 esta por delante de pos1
	        if(Math.abs(Angles.radnorm_180(ang1-pos1.alpha())) < Math.PI/2) return true;
	        // Si pos1 esta por delante de pos2
	        if(Math.abs(Angles.radnorm_180(ang2-pos2.alpha())) < Math.PI/2) return true;
	        
	        return false; // Estan las dos por detras
	    }
	    return false; // Continua cuando esta alejado
	}
	
	public double iforkdist(Position pos1, Position pos2){
	    Line2[] ifork1 = Transform2.rotTrans(limits, pos1.x(), pos1.y(), pos1.alpha());
	    Line2[] ifork2 = Transform2.rotTrans(limits, pos2.x(), pos2.y(), pos2.alpha());
	    double d, mind;
	    mind = Double.MAX_VALUE;
	    for(int i = 0; i<limits.length; i++){
	        for(int j = 0; j<limits.length; j++){
	            d = ifork1[i].segDistance(ifork2[j]);
	            if(d<mind) mind = d;
	        }
	    }	    
	    return mind;
	}
	
	public double distwps(String wp1,String wp2){
		int type1 = world.getType(wp1);
		int type2 = world.getType(wp2);
		
		if(type1 == World.WP && type2 == World.WP){
			Point3 pos1 = world.wps().at(wp1).getPos();
			Point3 pos2 = world.wps().at(wp2).getPos();
			return pos1.distance(pos2);
		}
		else if(type1 == World.DOOR && type2 == World.WP){
			Line2 path = world.doors().at(wp1).path;
			Point3 pos2 = world.wps().at(wp2).getPos();
			return path.segDistance(pos2.x(),pos2.y());
		}
		else if(type1 == World.WP && type2 == World.DOOR){
			Line2 path = world.doors().at(wp2).path;
			Point3 pos1 = world.wps().at(wp1).getPos();
			return path.segDistance(pos1.x(),pos1.y());
		}
		return Double.MAX_VALUE;
	}
	
	public void printLocks(){
	    if(locks.size()>0){
			System.out.print("["+robotid+"] Looks = { ");
			for(int i = 0; i<locks.size();i++){
				System.out.print((String)locks.get(i)+" ");
			}
			System.out.println("}");
		}
	    if(asoclocks.size()>0){
	    	Enumeration			infos = asoclocks.keys();
	    	String rname;
	    	String bockedname;
	    	System.out.print("["+robotid+"] AsocLooks = { ");
	    	while (infos.hasMoreElements()){
	    		rname	= (String)infos.nextElement();
	    		bockedname = (String)asoclocks.get (rname);
	    		System.out.print(rname+"->"+bockedname+" ");
	    	}
			System.out.println("}");
		}
	}
	
	public void printAgvInfos(){
	    if(agvinfo.isEmpty()) return;
	    Enumeration			infos = agvinfo.keys();
	    String rname;
	    ItemCoordination item;
	    
	    System.out.println("["+robotid+"] Agvinfos:");
	    while(infos.hasMoreElements()){
	        rname	= (String)infos.nextElement();
			item		= (ItemCoordination)agvinfo.get (rname);
			System.out.println("\tNAME= "+rname+" ITEM= "+item+" dist= "+lps.cur.distance(item.position)+ " dist2="+iforkdist(item.position,lps.cur));
	    }
		
	}
	
	
}
