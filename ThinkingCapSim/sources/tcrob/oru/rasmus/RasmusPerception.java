/*
 * (c) 2003 Humberto Martinez
 */
 
package tcrob.oru.rasmus;

import java.util.*;

import tc.shared.linda.*;

import tcrob.umu.indoor.IndoorPerception;
		
public class RasmusPerception extends IndoorPerception
{
	public static final double		RBUF_SON_DIST	= 1.5;			// Max distance for sonar range buffer (m)
	public static final double		RBUF_LRF_DIST	= 10.0;			// Max distance for lrf range buffer (m)
	public static final int			RBUF_HIST		= 10;			// History depth for range buffer
	
	// Constructors
	public RasmusPerception (Properties props, Linda linda)
	{
		super (props, linda);
	}
	
	// Instance methods
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
		
		// Modify LPS geometry to accomodate to laser ranges
		l_rbuffer.setRange (RBUF_SON_DIST, RBUF_LRF_DIST);
		l_rbuffer.setSize (fdesc.RAYSCAN * RBUF_HIST);
		l_rbuffer.resetBuffer ();

		// Local behaviour configuration
		segments	= true; 
	}
}

/*
 * THIS WAS USED BY DENIS. IT IS A HACK, BUT IT IS KEPT AS A HINT
 * 
 * 
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
		
		if (fdesc.MAXVIRTU > 0)
			max_buffer	= LPS.PPR_BUFFER * fdesc.MAXVIRTU;

		// Add domain specific LPOs to the LPS
		l_home		= new LPOPoint (0.0, 0.0, 0.0, "Home", LPO.MAP);
//		l_goal		= new LPOPoint (0.0, 0.0, 0.0, "Goal",  LPO.MAP);	

		wucore.utils.math.Matrix3D rotm = new wucore.utils.math.Matrix3D();
		
		// this was the position for the aass_corr.world map
		//rotm.toFrame(0,-2,90.0/180*Math.PI);
		
		// this is the robot position for the real experiment in the aass corridor
		rotm.toFrame(0,0,90.0/180*Math.PI);
		
		// here we use the absolute coordinates of the door1 as in the aass_coor.world
		//double xx = 1.0;
		//double yy = 6.425;
		//double alpha = 0;
		
		// here we use the absolute coordinates of the door4 in the aass_corr.world
//		double xx = -1.5;
//		double yy = 4.825;
//		double alpha = 180/180*Math.PI;
		
		// here we use the absolute coordinates for the real experiment in the corridor
		// of the aass to test the complexTask behaviour
		double xx = -0.60;
		double yy = 10.15;
		double alpha = 170.0/180*Math.PI;
		double x		= (rotm.mat[0][0] * xx) + (rotm.mat[0][1] * yy) + rotm.mat[0][2];
		double y		= (rotm.mat[1][0] * xx) + (rotm.mat[1][1] * yy) + rotm.mat[1][2];	
		alpha	= wucore.utils.math.Angles.radnorm_180 (alpha + rotm.mat[2][2]);

		// here we use the coordinates for the real experiment in the corridor
		// of the aass
		l_door		= new LPOPoint(x,y,alpha,"Door4", LPO.MAP);
		
		// here we use the coordinates for the real experiment in the corridor
		// of the aass to test only the cross behaviour
//		l_door = new LPOPoint(1,-0.2,15.0/180*Math.PI,"Door4",LPO.MAP);
		lps.add(l_door);
		l_door.active(true);
		
//		Use this only with aass_corr and the simulator
//		xx = -3.8;
//		yy = 5.5;
//		alpha = 0;
		
		// This was the absolute position of the goal in the real experiment
		// made in the aass corridor
		xx = -2.60;
		yy = 10.15;
		alpha = 0;
		
		x		= (rotm.mat[0][0] * xx) + (rotm.mat[0][1] * yy) + rotm.mat[0][2];
		y		= (rotm.mat[1][0] * xx) + (rotm.mat[1][1] * yy) + rotm.mat[1][2];	
		alpha	= wucore.utils.math.Angles.radnorm_180 (alpha + rotm.mat[2][2]);
		l_goal		= new LPOPoint (x,y,alpha, "Goal",  LPO.MAP);	

		//Activate it only if you are using the complexTask behaviour, because
		//in that case you want to have a predefined goal to achieve
		l_goal.active(true);

//		Use this only with BPlan stuff
//		xx = -3.0;
//		yy = 5;
//		alpha = 180/180*Math.PI;
//		x		= (rotm.mat[0][0] * xx) + (rotm.mat[0][1] * yy) + rotm.mat[0][2];
//		y		= (rotm.mat[1][0] * xx) + (rotm.mat[1][1] * yy) + rotm.mat[1][2];	
//		alpha	= wucore.utils.math.Angles.radnorm_180 (alpha + rotm.mat[2][2]);
//		LPOPoint l_room = new LPOPoint(x,y,alpha,"Room1",LPO.MAP);
//		lps.add(l_room);
//		l_room.active(true);
//		
//		LPOPoint l_corr = new LPOPoint(2.0,0,0.0/180*Math.PI,"Corridor1",LPO.MAP);
//		lps.add(l_corr);
//		l_corr.active(true);

 */

