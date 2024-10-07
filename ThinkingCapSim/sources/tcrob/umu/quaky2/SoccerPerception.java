/*
 * (c) 2002 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

import java.util.*;
//import java.awt.*;

import tc.shared.lps.lpo.*;
import tc.shared.linda.*;
		
import tcrob.umu.indoor.IndoorPerception;
import tcrob.umu.quaky2.lpo.*;

import wucore.utils.math.*;
import wucore.utils.color.*;

public class SoccerPerception extends IndoorPerception
{
	static public final double		BALL_RADIUS	= 0.1;			// Ball radius (m)
	static public final double		NET_SIZE	= 0.2;			// Net size (m)
	
	// Application LPOs
	protected Ball					ball;
	protected Net					net1;
	protected Net					net2;
	protected LPOPoint				align;
	
	// Constructors
	public SoccerPerception (Properties props, Linda linda)
	{
		super (props, linda);
	}
	
	// Instance methods
	protected void lowlevel_fusion ()
	{
		int				i;
		double			delta, alpha;
		double			arc1;
		boolean			ballSeen, net1Seen, net2Seen;
		
		// Update LPOs and clamp LPS
		lps.update_anchors ();
		lps.clamp (pos);
		
	    // Object-level fusion and LPS update
	    if (vdata != null)
			for (i = 0; i < vdata.length; i++)
				if (vdata[i].valid)
					lps.set_lpo (vdata[i]);
		vdata		= null;

		ballSeen	= (ball.anchor () >= 0.1);
		net1Seen 	= (net1.anchor () >= 0.1);
		net2Seen 	= (net2.anchor () >= 0.1);

		ballSeen	= true;
		net1Seen 	= true;
		net2Seen 	= true;

		// Signal-level sensor fusion and LPS update
		fusion.fuse_signal (data);	

		arc1		= fdesc.CONEVIRTU * 0.5 + 55.0 * Angles.DTOR;
//		arc2		= fdesc.CONEVIRTU * 0.5 + 55.0 * Angles.DTOR;
//		len			= 0.45;
		for (i = 0; i < fdesc.MAXVIRTU; i++)
		{
			alpha		= fdesc.virtufeat[i].alpha ();
	    	if (fusion.virtuals_flg[i])
	    	{
	    		// Check sensors when the object is in direct view
	    		if (ballSeen && (alpha - arc1 < ball.phi ()) && (alpha + arc1 >= ball.phi ()))
					fusion.virtuals_flg[i]	= false;

	    		if (net1Seen && (alpha - arc1 < net1.phi ()) && (alpha + arc1 >= net1.phi ()))
					fusion.virtuals_flg[i]	= false;

	    		if (net2Seen && (alpha - arc1 < net2.phi ()) && (alpha + arc1 >= net2.phi ()))
					fusion.virtuals_flg[i]	= false;

	    		// Check sensors when the object is not in direct view
/*	    		if (!ballSeen && (alpha - arc2 < ball.phi ()) && (alpha + arc2 >= ball.phi ())
	    			&& (fusion.virtuals[i] - len < ball.rho ()) && (fusion.virtuals[i] + len >= ball.rho ()))
						fusion.virtuals_flg[i]	= false;

	    		if (!net1Seen && (alpha - arc2 < net1.phi ()) && (alpha + arc2 >= net1.phi ())
	    			&& (fusion.virtuals[i] - len < net1.rho ()) && (fusion.virtuals[i] + len >= net1.rho ()))
						fusion.virtuals_flg[i]	= false;

	    		if (!net2Seen && (alpha - arc2 < net2.phi ()) && (alpha + arc2 >= net2.phi ())
	    			&& (fusion.virtuals[i] - len < net2.rho ()) && (fusion.virtuals[i] + len >= net2.rho ()))
						fusion.virtuals_flg[i]	= false;
*/	    	}
		
	    	if (fusion.virtuals_flg[i])
	    		l_rbuffer.add_range (fdesc.virtufeat[i], i, fusion.virtuals[i]);
	    }
	    l_virtual.update (fusion.virtuals, fusion.virtuals_flg);

    	if (fusion.scans_flg)
    	{
    		delta	= fdesc.CONESCAN / (double) (fdesc.RAYSCAN - 1);
			alpha	= -fdesc.CONESCAN * 0.5;		

    		for (i = 0; i < fdesc.RAYSCAN; i++, alpha += delta)
    			l_rbuffer.add_range (fdesc.scanfeat, i, fusion.scans[i], alpha);
    	}
    	l_scan.update (fusion.scans, fusion.scans_flg);
	    	
	    // Feature-level sensor fusion and LPS update
		fusion.fuse_feature (lps, data);	
		l_group.update (fusion.groups, fusion.groups_flg);
	    
		// Update low-level perception & LPS data
		lps.update (data, fusion, lodom, pos, null);
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
		
		// Do not generate segments
		segments = false;
	}
}

