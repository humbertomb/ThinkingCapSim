/*
 * (c) 2002 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

import tc.runtime.thread.ModuleConfig;

import tc.shared.lps.lpo.*;
import tc.shared.linda.*;

import tclib.behaviours.bg.*;
import tclib.navigation.mapbuilding.*;
import tclib.navigation.pathplanning.*;

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
	static public final double			ALG_LAT			= 0.07;					// How far off the ball-net line the robot may be to be aligned (m)
	static public final double			ALG_LAT_KEEP	= 0.20;					// ... and how far it may drift before it aligns again (m)
	static public final double			ALG_LAT_DIST	= 3.0;					// Beyond this distance to the net it may drift the whole ALG_LAT_KEEP (m)
	static public final double			ALG_BACK		= 0.05;					// How far behind the ball, towards the net, the robot has to be (m)
	static public final double			CATCH_DIST		= 0.45;					// The ball is held when it is this close, in front of the robot (m)
	static public final double			CATCH_HEAD		= 30.0 * Angles.DTOR;	// ... within this heading (rad)
	static public final double			PUSH_AHEAD		= 0.25;					// How far beyond the ball, towards the net, the robot aims to push it (m)
	static public final double			FIELD_WAIT		= 20.0;					// How long it looks for the whole field before it gives up (s)
	static public final double			NET_SMOOTH		= 0.3;					// How much of a new sighting of a net goes into where it is taken to be
	static public final double			AXIS_SURE		= 0.9;					// A net just seen (this anchored) says where it is
	static public final double			AXIS_MIN		= 3.0;					// Nets further apart than this give the axis of the field (m)
	static public final double			AIM_SIDE		= 0.80;					// The ball goes straight into the net when it is this close to its axis (m)
	static public final double			AIM_FRONT		= 0.90;					// ... otherwise it is taken to the front of the net, this far from it (m)
	static public final double			AIM_TURN		= 0.3;					// How much of the drift of the ball is aimed off, to the other side
	static public final double			AIM_OVER		= 0.20;					// ... at the most (m)
	static public final double			NET_DEEP		= 0.35;					// How far behind where a net is seen (its mouth) the ball is taken (m)
	static public final double			GOAL_IN			= 0.05;					// How far past the mouth of the net the ball has to be to have gone in (m)
	static public final double			NET_HALF		= 0.30;					// Half the width of the mouth of a net (m)
	static public final double			GOAL_NEAR		= 0.60;					// ... and no further than this from where the net is seen (m)
	static public final double			GOAL_DIST		= 0.20;					// The ball is in the net when it is this close to it (m)
	static public final int				LOOKA_DIST		= 2;					// Look-ahead distance (cells)
	
	// Navigation structures
	protected Grid						grid;
	protected GridPath					gpath;

	// Relevant objects (LPOs)
	protected Position					robot;
	protected Position					ball;
	protected Position					net1;
	protected Position					net2;
	protected Position					net;						// the net of the task (net1 or net2)
	protected Position					align;
	protected Position					push;						// where the robot aims to push the ball from
	protected Position					aim;						// where it takes the ball to (the net, or the front of it)
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
	protected double					lnetPhi			= 0.0;

	// The net of the task, and whether it is seen
	protected String					target			= "Net1";
	protected boolean					netSeen			= false;
	
	// Where each net was when it was last seen, in the frame of the odometry (so that the
	// two can be compared although they are never seen at the same time), the axis of the
	// field that follows from them (the direction from Net2 to Net1, in that same frame)
	// and how far off that axis, at the net, the ball is
	protected Position					anet1;
	protected Position					anet2;
	protected boolean					aseen1			= false;
	protected boolean					aseen2			= false;
	protected double					axis			= Double.NaN;
	protected double					sideways		= 0.0;
	protected double					deep			= 0.0;
	protected boolean					fieldSeen		= false;
	
	// The step being run and when it started, to give up looking for the field
	protected String					laststep		= "";
	protected long						stepstart		= 0;
	
	// Constructors
	public SoccerController (ModuleConfig cfg, Linda linda) 
	{
		super (cfg, linda);
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
	protected void initialise (ModuleConfig cfg)
	{		
		super.initialise (cfg);
		
		// Local variables
		robot		= new Position (0.0, 0.0, 0.0);
		ball		= new Position ();
		net1		= new Position ();
		net2		= new Position ();
		net			= new Position ();
		align		= new Position ();
		push		= new Position ();
		aim			= new Position ();
		anet1		= new Position ();
		anet2		= new Position ();
		obstacle	= new Position ();
	}
	
	/**
	 * Where an object of the LPS is, into <code>p</code>, and how sure the LPS is
	 * of it (its anchor). An object the LPS does not have is not seen (0) and
	 * <code>p</code> keeps where it was last.
	 */
	protected double locate (String name, Position p)
	{
		LPO		lpo = lps.find (name);

		if (lpo == null)		return 0.0;
		p.set_polar (lpo.rho (), lpo.theta ());
		return lpo.anchor ();
	}

	/** The net the task names (Net2 when its place names a 2; Net1 otherwise). */
	protected String targetNet ()
	{
		String		place = ((plan != null) && (plan.place != null)) ? plan.place.toLowerCase () : "";

		return place.contains ("2") ? "Net2" : "Net1";
	}

	/** Where something of the frame of the odometry is, as the robot sees it. */
	protected void relative (Position p, Position out)
	{
		double		aa = lps.cur.alpha ();
		double		dx = p.x () - lps.cur.x (), dy = p.y () - lps.cur.y ();

		out.set ((dx * Math.cos (aa)) + (dy * Math.sin (aa)), (-dx * Math.sin (aa)) + (dy * Math.cos (aa)));
	}

	/** Where a net is, into <code>mem</code>, as the average of the sightings of it (in the frame of the odometry). */
	protected void remember (Position seen, Position mem, boolean known)
	{
		Position	abs = new Position ();

		absolute (seen, abs);
		if (!known)		mem.set (abs.x (), abs.y ());
		else			mem.set (mem.x () + (NET_SMOOTH * (abs.x () - mem.x ())), mem.y () + (NET_SMOOTH * (abs.y () - mem.y ())));
	}

	/** Where something of the LPS is in the frame of the odometry, which does not turn with the robot. */
	protected void absolute (Position p, Position out)
	{
		double		aa = lps.cur.alpha ();

		out.set (lps.cur.x () + (p.x () * Math.cos (aa)) - (p.y () * Math.sin (aa)),
				 lps.cur.y () + (p.x () * Math.sin (aa)) + (p.y () * Math.cos (aa)));
	}

	/**
	 * Which way the net of the task looks, as a unit vector towards its inside: the
	 * axis of the field, learnt the first time both nets were seen together. It is
	 * (0, 0) while the axis is not known.
	 */
	protected void inwards (Position p)
	{
		double		aa;

		if (Double.isNaN (axis))		{ p.set (0.0, 0.0); return; }

		aa	= axis - lps.cur.alpha ();
		if (target.endsWith ("2"))		aa += Math.PI;			// Net2 looks the other way
		p.set (Math.cos (aa), Math.sin (aa));
	}

	/**
	 * Where the ball has to be taken: into the net when it is in front of its mouth,
	 * and to the front of the mouth otherwise, so that it is never pushed into a post.
	 * How far off the axis of the net the ball is stays in <code>sideways</code>.
	 */
	protected void aiming ()
	{
		Position	in = new Position ();
		double		ox, oy;

		inwards (in);
		if ((in.x () == 0.0) && (in.y () == 0.0))				// no axis: straight at the net
		{
			sideways	= 0.0;
			deep		= 0.0;
			aim.set (net.x (), net.y ());
			return;
		}

		double		side, over;

		ox			= ball.x () - net.x ();
		oy			= ball.y () - net.y ();
		side		= (in.x () * oy) - (in.y () * ox);			// which side of the axis of the net the ball is
		sideways	= Math.abs (side);
		deep		= (in.x () * ox) + (in.y () * oy);
		if (sideways > AIM_SIDE)								// beside the net: to the front of it first
			aim.set (net.x () - (AIM_FRONT * in.x ()), net.y () - (AIM_FRONT * in.y ()));
		else
		{
			// in front of it: well inside it (what is seen of a net is its mouth), and towards
			// the other side of its axis, as the ball keeps drifting to the side it comes from
			over	= Math.max (-AIM_OVER, Math.min (AIM_OVER, AIM_TURN * side));
			aim.set (net.x () + (NET_DEEP * in.x ()) + (over * in.y ()),
					 net.y () + (NET_DEEP * in.y ()) - (over * in.x ()));
		}
	}

	/**
	 * Where the robot has to be to push the ball where it is aimed at, and where it
	 * aims while pushing it: the alignment point is on the line from that point through
	 * the ball, ALG_DIST behind the ball, and the point it pushes towards is
	 * PUSH_AHEAD beyond the ball on that same line, so that the robot drives
	 * through the ball rather than at it. Both go into the LPS (Align).
	 */
	protected void alignment ()
	{
		double		bx = ball.x (), by = ball.y (), nx = aim.x (), ny = aim.y ();
		double		dx = nx - bx, dy = ny - by;
		double		d = Math.sqrt (dx * dx + dy * dy);
		LPO			lpo;

		if (d < 1e-3)								// the ball is on the net: aim at the ball itself
		{
			align.set (bx, by);
			push.set (bx, by);
		}
		else
		{
			dx	/= d;		dy /= d;
			align.set (bx - ALG_DIST * dx, by - ALG_DIST * dy);
			push.set (bx + PUSH_AHEAD * dx, by + PUSH_AHEAD * dy);
		}

		lpo		= lps.find ("Align");
		if (lpo != null)
		{
			lpo.locate (align.x (), align.y (), Math.atan2 (by - align.y (), bx - align.x ()));
			lpo.active (ballSeen && netSeen);
		}
	}

	/**
	 * How far the robot is off the line that takes the ball where it is aimed at (m),
	 * always positive, and how far it is past the ball along that line (m, negative when
	 * it is still behind the ball, which is where it has to be to push it).
	 */
	protected double offline ()
	{
		double		bx = ball.x (), by = ball.y (), dx = aim.x () - bx, dy = aim.y () - by;
		double		d = Math.sqrt (dx * dx + dy * dy);

		if (d < 1e-3)		return 0.0;
		return Math.abs (((dx / d) * (-by)) - ((dy / d) * (-bx)));
	}

	protected double aside ()
	{
		double		bx = ball.x (), by = ball.y (), dx = aim.x () - bx, dy = aim.y () - by;
		double		d = Math.sqrt (dx * dx + dy * dy);

		if (d < 1e-3)		return 0.0;
		return (((dx / d) * (-bx)) + ((dy / d) * (-by)));
	}

	protected void controller () 
	{
		int				i;
		int				result;
		double			vr, wr;
		double			lookaPhi;
		double 			dstBallNet;
		double			lateral;
		double			behind;
		double			keep;
		double			nanchor;
		LPOSensorGroup	group;
//		LPORangeBuffer	rbuffer;
		LPO				lpo;

		// without a plan (nobody has said what to do) the robot stays where it is
		if (!has_plan)
		{
			setMotion (0.0, 0.0, 0.0);
			return;
		}

		// how long the step of the plan has been running
		String			step = (plan.task != null) ? plan.task.toUpperCase () : "";
		if (!step.equals (laststep))		{ laststep = step;	stepstart = System.currentTimeMillis (); }
		
		/* -------------------- */
		/* READ LPS INFORMATION */
		/* -------------------- */
		
		// Check for new LPOs information
double banchor;
		// an object the LPS does not have (no module puts it there) is an object not seen
		banchor		= locate ("Ball", ball);
		ballSeen	= (banchor > 0.6);
double n1anchor, n2anchor;
		n1anchor	= locate ("Net1", net1);
		n2anchor	= locate ("Net2", net2);
		net1Seen 	= (n1anchor > 0.3);
		net2Seen 	= (n2anchor > 0.2);
		
		// the net the task is about (Net2 when it names it; Net1 otherwise), which is the one it plays on
		target		= targetNet ();
		nanchor		= locate (target, net);
		netSeen		= (nanchor > 0.3);
		// where it is, as the average of the sightings of it, which is steadier than the last one
		// (a net seen from one side looks displaced towards that side)
		if (target.endsWith ("2"))	{ if (aseen2)	relative (anet2, net); }
		else						{ if (aseen1)	relative (anet1, net); }
		
		// the axis of the field: where each net is, as it is seen, and the line between them
		if (n1anchor > AXIS_SURE)		{ remember (net1, anet1, aseen1);	aseen1 = true; }
		if (n2anchor > AXIS_SURE)		{ remember (net2, anet2, aseen2);	aseen2 = true; }
		if (aseen1 && aseen2)
		{
			double		dx = anet1.x () - anet2.x (), dy = anet1.y () - anet2.y ();
			
			if (Math.sqrt ((dx * dx) + (dy * dy)) > AXIS_MIN)
				axis	= Math.atan2 (dy, dx);
		}
		// the field is known once both nets have been seen (or once it is not worth looking for them)
		fieldSeen	= !Double.isNaN (axis) || ((System.currentTimeMillis () - stepstart) > ((long) (FIELD_WAIT * 1000.0)));
		
		/* ----------------- */
		/* COMPUTE POSITIONS */
		/* ----------------- */
		
		// where the ball has to be taken, the point to push it from, and the one to push it towards
		aiming ();
		alignment ();
		
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
		dstBallNet	= Math.sqrt ((ball.x () - net.x ()) * (ball.x () - net.x ()) + (ball.y () - net.y ()) * (ball.y () - net.y ()));
		// aligned: the robot is behind the ball and on the line that takes the ball to the net,
		// so that pushing the ball sends it there. Where it looks does not matter: the approach
		// turns towards the ball. It may drift off that line a bit before it aligns again, so
		// that pushing does not flicker back to aligning
		lateral		= offline ();
		behind		= -aside ();
		// the closer the ball is to the net, the less it may drift, as the mouth is narrow
		keep		= Math.max (ALG_LAT, Math.min (ALG_LAT_KEEP, (ALG_LAT_KEEP * dstBallNet) / ALG_LAT_DIST));
		ballAligned	= ballSeen && netSeen && (behind > ALG_BACK)
					  && (lateral < (ballAligned ? keep : ALG_LAT));
		// the ball is held when it is right in front of the robot (or the switch of the fork says so)
		ballHold	= (ballSeen && (ball.rho () < CATCH_DIST) && (Math.abs (ball.phi ()) < CATCH_HEAD))
					  || ((lps.dsignals != null) && (lps.dsignals.length > 0) && lps.dsignals[0]);
		// scored: the ball has gone past the mouth of the net, within its width. Without the
		// axis of the field there is no telling where the mouth is, and being close does
		inNet		= ballSeen && netSeen
					  && (Double.isNaN (axis) ? (dstBallNet < GOAL_DIST)
											  : ((deep > GOAL_IN) && (sideways < NET_HALF) && (dstBallNet < GOAL_NEAR)));

		if (ballSeen)		lballPhi	= ball.phi ();
		if (netSeen)		lnetPhi		= net.phi ();
		
		// Put perception data into BG interpreter
		lpo		= lps.find ("Group");
		if (lpo instanceof LPOSensorGroup)
		{
			group	= (LPOSensorGroup) lpo;
			for (i = 0; (i < fdesc.MAXGROUP) && (i < group.range.length); i++)
				interp.access ("group"+i, group.range[i]);
		}

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
		interp.access ("netPhi", net.phi () * Angles.RTOD);
		interp.access ("lnetPhi", lnetPhi * Angles.RTOD);
		interp.access ("pushPhi", push.phi () * Angles.RTOD);
		interp.access ("lookaPhi", lookaPhi * Angles.RTOD);

		// Put state predicates into BG interpreter
//		interp.access ("ballSeen", boolToDouble (ballSeen || ballHold));
		interp.access ("ballSeen", banchor);
		interp.access ("netSeen", boolToDouble (netSeen));
		interp.access ("fieldSeen", boolToDouble (fieldSeen));
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
		setMotion (vr, 0.0, wr);				// a differential drive does not go sideways
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

	/**
	 * Whether the step of the plan the robot is at is done, from what it
	 * perceives: SEARCH, when it sees the ball and the net; ALIGN, when the ball
	 * is aligned with the net; KICK, when it is in the net (scored); STANDBY at
	 * once (it stays still). Any other, as any controller: at its place.
	 */
	protected int inGoal ()
	{
		if (!has_plan)						return ItemBehResult.T_NOTYET;

		switch ((plan.task != null) ? plan.task.toUpperCase () : "")
		{
		case "SEARCH":		return (ballSeen && netSeen && fieldSeen) ? ItemBehResult.T_FINISHED : ItemBehResult.T_NOTYET;
		case "ALIGN":		return ballAligned ? ItemBehResult.T_FINISHED : ItemBehResult.T_NOTYET;
		case "KICK":		return inNet ? ItemBehResult.T_FINISHED : ItemBehResult.T_NOTYET;
		case "STANDBY":		return ItemBehResult.T_FINISHED;
		default:			return super.inGoal ();
		}
	}

	public void notify_goal (String space, ItemGoal goal)
	{
		String		step = ((goal.task != null) && (goal.task.task != null)) ? goal.task.task.toUpperCase () : "";

		super.notify_goal (space, goal);
		
		// a plan starts over (SEARCH) or ends (STANDBY): the program starts over; the steps in between go on from where it is
		if (step.equals ("SEARCH") || step.equals ("STANDBY"))
			interp.reset ();
	}
}

