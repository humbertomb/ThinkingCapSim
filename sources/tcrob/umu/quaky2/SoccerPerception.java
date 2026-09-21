/*
 * (c) 2002 Humberto Martinez
 * (c) 2026 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

import tc.runtime.thread.ModuleConfig;
import tc.shared.linda.*;
import tc.shared.lps.lpo.*;
import tcrob.umu.indoor.IndoorPerception;
import tcrob.umu.quaky2.lpo.*;

import devices.data.*;
import wucore.utils.color.*;

/**
 * The perception of the soccer robot: the one of an indoor robot, whose LPS
 * also has the ball and the two nets, placed where the vision (SoccerVision)
 * says it sees them (OBJECT). It is this LPS that the controller is given and
 * that the LPS window shows.
 */
public class SoccerPerception extends IndoorPerception
{
	// Application LPOs (named as the objects the vision sends: Ball, Net1, Net2)
	protected Ball					ball;
	protected Net					net1;
	protected Net					net2;
	protected LPOPoint				align;

	// What the vision saw last, not yet in the LPS
	protected VisionData[]			vdata;
	
	// Constructors
	public SoccerPerception (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}
	
	// Instance methods
	protected void lowlevel_fusion ()
	{
		VisionData[]		seen;

		super.lowlevel_fusion ();

		// Object-level fusion: what the vision saw, where it saw it
		synchronized (this)		{ seen = vdata; vdata = null; }
		if (seen != null)
			for (VisionData d : seen)
				if ((d != null) && d.valid)
					lps.set_lpo (d);
	}

	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
		if (item.props_robot == null)		return;					// only a new robot makes a new LPS
		
		// Add domain specific LPOs to the LPS
		ball	= new Ball (SoccerVision.BALL_RADIUS, "Ball", LPO.PERCEPT);
		ball.color (WColor.YELLOW.darker());
		
		net1	= new Net (SoccerVision.NET_SIZE, "Net1", LPO.PERCEPT);
		net1.color (WColor.BLUE);
		
		net2	= new Net (SoccerVision.NET_SIZE, "Net2", LPO.PERCEPT);
		net2.color (WColor.RED);
		
		align	= new LPOPoint (0.0, 0.0, 0.0, "Align", LPO.ARTIFACT);
		align.color (WColor.MAGENTA);
		
		lps.add (ball);
		lps.add (net1);
		lps.add (net2);
		lps.add (align);
	}

	/** What the vision recognised (ball, nets), kept until the next step puts it in the LPS. */
	public void notify_object (String space, ItemObject item)
	{
		if ((state != RUN) || (item.data == null))		return;
		synchronized (this)		{ vdata = item.data; }
	}
}
