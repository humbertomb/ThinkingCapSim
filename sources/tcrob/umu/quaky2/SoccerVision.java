/*
 * (c) 2002 Humberto Martinez
 * (c) 2026 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

import tc.modules.*;
import tc.runtime.thread.ModuleConfig;
import tc.shared.lps.lpo.*;
import tc.shared.linda.*;
import tcrob.umu.quaky2.lpo.*;

import wucore.utils.color.*;
import devices.data.*;

public class SoccerVision extends Perception
{
	static public final double		BALL_RADIUS	= 0.11;			// Ball radius (m)
	static public final double		NET_SIZE	= 0.2;			// Net size (m)
	
	protected VisionData[]			vision;
	
	// Application LPOs
	protected Ball					ball;
	protected Net					net1;
	protected Net					net2;
	protected LPOPoint				align;
	
	// Constructors
	public SoccerVision (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}
	
	// Instance methods
	protected void lowlevel_fusion ()
	{
		int				i;
		
	    // Object-level fusion and LPS update
	    if (vision != null)
			for (i = 0; i < vision.length; i++)
				if (vision[i].valid)
					lps.set_lpo (vision[i]);
	    vision		= null;

//		ballSeen	= (ball.anchor () >= 0.1);
//		net1Seen 	= (net1.anchor () >= 0.1);
//		net2Seen 	= (net2.anchor () >= 0.1);
//
//		ballSeen	= true;
//		net1Seen 	= true;
//		net2Seen 	= true;
	    	
	    
		// Update low-level perception & LPS data
//		lps.update (data, fusion, lodom, pos, null);
	}

	public void step (long ctime)
	{
		if (state != RUN)		return;
				
		// Sensor fusion and LPS update
		lowlevel_fusion ();
//				
//		lps.add_time ((double) (System.currentTimeMillis () - ctime));
//		
//		// Update the LPS in the Linda space
//		tupd	 = ctime - stime;		
//		lstore.set (lps, tupd);		
//		linda.write (ltuple);
	}
	

	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
		
		// Add domain specific LPOs to the LPS
		ball	= new Ball (BALL_RADIUS, "Ball", LPO.PERCEPT);
		ball.color (WColor.YELLOW.darker());
		
		net1	= new Net (NET_SIZE, "Net1", LPO.PERCEPT);
		net1.color (WColor.BLUE);
		
		net2	= new Net (NET_SIZE, "Net2", LPO.PERCEPT);
		net2.color (WColor.RED);
		
		align	= new LPOPoint (0.0, 0.0, 0.0, "Align", LPO.ARTIFACT);
		align.color (WColor.MAGENTA);
		
		lps.add (ball);
		lps.add (net1);
		lps.add (net2);
		lps.add (align);
	}
	
	public void notify_camera (String space, ItemCamera item)
	{
//		// Add domain specific LPOs to the LPS
//		ball	= new Ball (BALL_RADIUS, "Ball", LPO.PERCEPT);
//		ball.color (WColor.YELLOW.darker());
//		
//		net1	= new Net (NET_SIZE, "Net1", LPO.PERCEPT);
//		net1.color (WColor.BLUE);
//		
//		net2	= new Net (NET_SIZE, "Net2", LPO.PERCEPT);
//		net2.color (WColor.RED);
//		
//		align	= new LPOPoint (0.0, 0.0, 0.0, "Align", LPO.ARTIFACT);
//		align.color (WColor.MAGENTA);
//		
//		lps.add (ball);
//		lps.add (net1);
//		lps.add (net2);
//		lps.add (align);
	}
}

