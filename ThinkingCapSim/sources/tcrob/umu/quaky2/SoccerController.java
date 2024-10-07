/*
 * (c) 2002 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

import java.util.*;

import tc.shared.lps.lpo.*;
import tc.shared.linda.*;

import tclib.behaviours.bg.*;
import tclib.navigation.mapbuilding.*;
import tclib.navigation.pathplanning.*;

import tcrob.umu.indoor.linda.*;

import devices.pos.*;
import wucore.utils.math.*;
		
public class SoccerController extends BGController
{	
	static public final double			WORLD_SIZE		= 8.0;					// World extent (m)
	static public final double			CELL_SIZE		= 0.15;					// Cell size (m)
	static public final double			BALL_SIZE		= 0.5;					// Ball size for grid (m)
	static public final double			NET_SIZE		= 0.1;					// Net size for grid (m)	
	static public final double			DEF_DIL			= 1.5;					// Default dilation constant				
	static public final double			ALG_DIST		= 0.75;					// Aligment distance (m)
	static public final double			ALG_HEAD		= 35.0 * Angles.DTOR;	// Aligment heading (rad)	
	static public final int				LOOKA_DIST		= 2;					// Look-ahead distance (cells)
	
	// Navigation structures
	protected Grid						grid;
	protected GridPath					gpath;

	// Linda data structures
	protected Tuple						gmtuple;
	protected ItemGridMap				gmitem;

	// Relevant objects (LPOs)
	protected Position					robot;
	protected Position					ball;
	protected Position					net1;
	protected Position					net2;
	protected Position					align;
	protected Position					obstacle;
	
	// BG predicates
	protected boolean					ballSeen		= false;
	protected boolean					net1Seen		= false;
	protected boolean					net2Seen		= false;
	protected boolean					ballAligned		= false;
	protected boolean					ballHold		= false;
	protected boolean					inNet			= false;
	
	// Most probable LPO locations (for searching)
	protected double					lballPhi		= 0.0;
	protected double					lnet1Phi		= 0.0;
	protected double					lnet2Phi		= 0.0;
	
	// Constructors
	public SoccerController (Properties props, Linda linda) 
	{
		super (props, linda);
	}
	
	// Class methods
	public double boolToDouble (boolean value)
	{
		if (value)
			return 1.0;
		else
			return 0.0;
	}
	
	// Instance methods
	protected void initialise (Properties props)
	{		
		super.initialise (props);

		// Initialise Linda related structures
		gmitem		= new ItemGridMap ();
		gmtuple		= new Tuple ("GRIDMAP", gmitem);
		
		// Local variables
		robot		= new Position (0.0, 0.0, 0.0);
		ball		= new Position ();
		net1		= new Position ();
		net2		= new Position ();
		align		= new Position ();
		obstacle	= new Position ();
	}
	
	protected void controller () 
	{
		int				i;
		int				result;
		double			vr, wr;
		double			xx, yy;
		double			m, n;
		double			k;
		double			lookaPhi;
		double			alignRho;
		double 			dstBallNet;
		LPOSensorGroup	group;
//		LPORangeBuffer	rbuffer;
		LPO				lpo;

		/* -------------------- */
		/* READ LPS INFORMATION */
		/* -------------------- */
		
		// Check for new LPOs information
double banchor;
		lpo			= lps.find ("Ball");
		ball.set_polar (lpo.rho (), lpo.phi ());
		ballSeen	= (lpo.anchor () > 0.6);
banchor = lpo.anchor ();

		lpo			= lps.find ("Net1");
		net1.set_polar (lpo.rho (), lpo.phi ());
		net1Seen 	= (lpo.anchor () > 0.3);

		lpo			= lps.find ("Net2");
		net2.set_polar (lpo.rho (), lpo.phi ());
		net2Seen 	= (lpo.anchor () > 0.2);
		
		/* ----------------- */
		/* COMPUTE POSITIONS */
		/* ----------------- */
		
		// Compute robot-ball-net geometry and positions (local)
		m		= (net1.y () - ball.y ()) / (net1.x () - ball.x ());
		if (Math.abs (m) <= 0.5)
		{
			n		= ball.y () - m * ball.x ();
			
			k		= ALG_DIST;
			if (ball.x () < net1.x ())
				k		= -ALG_DIST;
			
			xx		= ball.x () + k * Math.cos (Math.atan (m));
			yy		= m * xx + n;
		}
		else
		{
			m		= (net1.x () - ball.x ()) / (net1.y () - ball.y ());
			n		= ball.x () - m * ball.y ();
			
			k		= ALG_DIST;
			if (ball.y () < net1.y ())
				k		= -ALG_DIST;

			yy		= ball.y () + k * Math.cos (Math.atan (m));
			xx		= m * yy + n;
		}
		
		alignRho	= Math.sqrt (xx * xx + yy * yy);
		align.set (xx, yy);
		
		// Update current LPS information
		lpo			= lps.find ("Align");
		if (lpo != null)
		{
			lpo.locate (xx, yy, 0.0);
			lpo.active (true);
		}
			
		/* ------------------ */
		/* COMPUTE LOCAL GRID */
		/* ------------------ */
		
		// Update local grid map and compute path to goal
//		rbuffer		= (LPORangeBuffer) lps.find ("RBuffer");
//		rpoint		= rbuffer.buffer ();
		grid.reset ();
/*		for (i = 0; i < rbuffer.getSize (); i++)
			if (rpoint[i].active ())
			{
				obstacle.set_polar (rpoint[i].rho (), rpoint[i].phi ());
				grid.obstacle (obstacle, 0.1);
			}
*/
		grid.obstacle (ball, BALL_SIZE);
		grid.obstacle (net1, NET_SIZE);
		grid.location (pos);
		
		gpath.goal (align);
		gpath.curve (GridPath.POLYLINE, GridPath.GRID);
		gpath.replan (robot);
		while (!gpath.newPath ())
			gpath.replan (robot);
		path	= gpath.path ();

		gmitem.set (grid, gpath, robot, System.currentTimeMillis ());
		linda.write (gmtuple);

		// Compute look-ahead point
		looka.valid (false);
		path.check_lookahead (robot, LOOKA_DIST);
		if (path.lookahead () != null)
		{
			path_dst	= path.distance ();
			looka.set (path.lookahead ());
			looka.valid (true);
		}
		
		// Compute heading to look-ahead
		lookaPhi	= Math.atan2 (looka.y (), looka.x ());

		// Update current LPS information
		lpo		= lps.find ("Looka");
		if (lpo != null)
		{
			lpo.locate (looka.x (), looka.y (), 0.0);
			lpo.active (looka.valid ());
		}

		/* ----------- */
		/* INTERPRETER */
		/* ----------- */
		
		// Set state predicates
		dstBallNet	= Math.sqrt ((ball.x () - net1.x ()) * (ball.x () - net1.x ()) + (ball.y () - net1.y ()) * (ball.y () - net1.y ()));
		ballAligned	= (Math.abs (ball.phi () - net1.phi ()) < ALG_HEAD) && (alignRho < dstBallNet + ALG_DIST);
		ballHold	= lps.dsignals[0];
		inNet		= net1Seen && (net1.rho () < 0.4);

		if (ballSeen)		lballPhi	= ball.phi ();
		if (net1Seen)		lnet1Phi	= net1.phi ();
		if (net2Seen)		lnet2Phi	= net2.phi ();
		
		// Put perception data into BG interpreter
		group	= (LPOSensorGroup) lps.find ("Group");
		for (i = 0; i < fdesc.MAXGROUP; i++)
			interp.access ("group"+i, group.range[i]);

		if (ballSeen)
		{
			double		ang;
			
			ang		= ball.phi () * Angles.RTOD;
			if (ang < -65.0)
				interp.access ("group4", 3.0);
			else if ((ang < 65.0) && (ang >= -65.0))
			{
				interp.access ("group3", 3.0);
				interp.access ("group2", 3.0);
				interp.access ("group1", 3.0);
			}
			else if (ang > 65.0)
				interp.access ("group0", 3.0);
		}
		
		// Put object positions into BG interpreter
		interp.access ("ballPhi", ball.phi () * Angles.RTOD);
		interp.access ("lballPhi", lballPhi * Angles.RTOD);
		interp.access ("netPhi", net1.phi () * Angles.RTOD);
		interp.access ("lnetPhi", lnet1Phi * Angles.RTOD);
		interp.access ("lookaPhi", lookaPhi * Angles.RTOD);

		// Put state predicates into BG interpreter
//		interp.access ("ballSeen", boolToDouble (ballSeen || ballHold));
		interp.access ("ballSeen", banchor);
		interp.access ("netSeen", boolToDouble (net1Seen));
		interp.access ("ballAligned", boolToDouble (ballAligned));
		interp.access ("ballHold", boolToDouble (ballHold));
		interp.access ("inNet", boolToDouble (inNet));
		
		// Run the whole BG program	
		interp.agents (program);

		// Read the specified action from BG interpreter
		vr	= interp.access ("speed");
		wr	= interp.access ("turn") * Angles.DTOR;
		
		/* ------------ */
		/* SEND RESULTS */
		/* ------------ */
		
		// Check if goal position reached
		result	= inGoal ();
		switch (result)
		{
		case ItemBehResult.T_FINISHED:
			vr 	= 0.0;
			wr	= 0.0;
			
			// Notify Linda Space the task has been finished
			setResult (result, ItemBehResult.F_OK, idtask);
			break;
			
		case ItemBehResult.T_FAILED:
			vr 	= 0.0;
			wr	= 0.0;
			
			// Notify Linda Space the task has failed
			setResult (result, ItemBehResult.F_BEHIND, idtask);			// or ItemBehResult.F_SIDE
			break;
			
		case ItemBehResult.T_NOTYET:
		default:
/*
			if (!looka.valid ())
			{
				vr 	= 0.0;
				wr	= 0.0;
			}
*/
		}
		
		// Apply the specified action
		setMotion (vr, wr);
	}

	public void notify_config (String space, ItemConfig item)
	{
		int				w, h;
		int				dil;
		
		super.notify_config (space, item);
	    
		// Create data structures
		w 	= (int) Math.round (WORLD_SIZE / CELL_SIZE) + 4;
		h 	= (int) Math.round (WORLD_SIZE / CELL_SIZE) + 4;
		dil = (int) (Math.round (rdesc.RADIUS * DEF_DIL / CELL_SIZE));

		grid 	= new FGrid (fdesc, rdesc, w, h, CELL_SIZE);		
		grid.setMode (FGrid.SAFE_MOTION);
		grid.setRangeSON (1.5);
		grid.setOffsets (-WORLD_SIZE * 0.5, -WORLD_SIZE * 0.5);	
		
		gpath	= new FGridPathA (grid);
		gpath.setDilation (dil);
		gpath.setTimeStep (300);
	}

	public void notify_goal (String space, ItemGoal goal)
	{
		super.notify_goal (space, goal);
		
		// Reset current interpreter state
		interp.reset ();
	}
}

