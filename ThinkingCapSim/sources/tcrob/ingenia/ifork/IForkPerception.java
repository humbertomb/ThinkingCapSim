/*
 * (c) 2002 Humberto Martinez
 */
 
package tcrob.ingenia.ifork;

import java.util.*;


import tc.vrobot.models.*;
import tc.shared.linda.*;
import tc.shared.lps.lpo.*;

import tcrob.ingenia.ifork.linda.*;
import tcrob.ingenia.ifork.lpo.*;
import tcrob.umu.indoor.*;

import devices.pos.*;

public class IForkPerception extends IndoorPerception
{
	static public final double				RBUF_DIST	= 14.0;		// Max distance for range buffer (m)
	static public final int					RBUF_HIST	= 10;		// History depth for range buffer
	static public final int					MAX_MATES	= 10;		// Max number of known robots
	
	// LPS and application LPOs
	protected LPOIForkData					l_robot;					// Robot internal status
	protected LPOMate[]						l_mates;					// Other robots data
	protected LPORangePBug					l_pbug;					// Polar bug algorithm
	protected LPOLine						l_avoid;					// Obstacle avoidance point
	
	// Odometry correction EKF
	protected IForkKLoc						kloc;
	protected Position						kpos;
	protected boolean						kfilter;
	protected Hashtable<String,Long>		agv_runtime;

	// Constructors
	public IForkPerception (Properties props, Linda linda)
	{
		super (props, linda);

		agv_runtime  = new Hashtable<String,Long> ();
	}
	
	// Instance methods
	protected void position_correction ()
	{
		// Set initial robot location
		if (firstime)	
		{
			if (rdesc.MAXLSB <= 0)
			{
				System.out.println ("--[iFrkPer] There should be at least one LSB laser");
				return;
			}
			else if (data.beacon[0].isValid () && (data.beacon[0].getNumber () >= 3))
			{
				lodom.set (data.odom_x, data.odom_y, data.odom_a);
				pos.set (data.beacon[0].getPosition ());
				
				if (kfilter)				kloc.posInit (pos);

				firstime = false;
			}
			else
				return;
		}	
		
		// Update odometry-based location
		dodom.set (data.odom_x, data.odom_y, data.odom_a);
		dodom.delta (lodom);
		
		// Update local odometry-based location
		kpos.set (pos);
		kpos.translate (dodom);
		
		// Apply EKF (using odometry and beacon-based laser)
		if (kfilter)
		{
			kloc.prediction (kpos);
			//kloc.prediction (data.vm,data.del);
			if ((rdesc.MAXLSB > 0) && data.beacon[0].isValid () && (data.beacon[0].getNumber () >= 4))
				kloc.updateLaser (data.beacon[0].getPosition (),data.beacon[0].getQuality());
				//kloc.updateLaser (data.beacon[0].getPosition ()
			
			lps.qlty	= data.beacon[0].getNumber ();

			// Update corrected position
			pos.set (kloc.current ());	
		}
		else
			pos.set (kpos);

		// Update previous odometry-based position
		lodom.set (data.odom_x, data.odom_y, data.odom_a);		
		
		// Update robot internal status
		l_robot.set (data.vm, data.del, data.fork, data.pal_switch);
		if(lps!=null)
			lps.pal_switch = l_robot.pal_switch() ;
		l_robot.active (true);
		
//		System.out.println ("  [IForkPer] Current pos="+pos+", fork="+data.fork);
	}

	protected void lowlevel_fusion ()
	{
		int				i;
		double			delta, alpha;
		
		// Signal-level sensor fusion and LPS update
		fusion.fuse_signal (data);	
		lps.update_anchors ();
		lps.clamp (pos);
				
		if (fusion.scans_flg)
		{
			delta	= fdesc.CONESCAN / (double) (fdesc.RAYSCAN - 1);
			alpha	= -fdesc.CONESCAN / 2.0;		
			
			for (i = 0; i < fdesc.RAYSCAN; i++, alpha += delta)
			{
				l_rbuffer.add_range (fdesc.scanfeat, i, fusion.scans[i], alpha);
				l_pbug.add_range (fdesc.scanfeat, i, fusion.scans[i], alpha);
			}
		}
		l_pbug.setRobots(l_mates);
		l_scan.update (fusion.scans, fusion.scans_flg);
		
		// Feature-level sensor fusion and LPS update
		fusion.fuse_feature (lps, data);	
		l_group.update (fusion.groups, fusion.groups_flg);
				
		// Update low-level perception & LPS data
		lps.update (data, fusion, lodom, pos, null);
		
		if (win != null)		win.update (lps, null);
	}

	public synchronized void notify_coord (String space, ItemCoordination coord)
	{
		if(agv_runtime.contains(space)){
			if((System.currentTimeMillis() - (agv_runtime.get(space)).longValue())<20000){
				//System.out.println("  [IForkPerception]: notify_coord Recibido coord de "+space+" estando borrado.");
				return;
			}else{
				//System.out.println("  [IForkPerception]: notify_coord "+space+" esta eliminado mas de 20 seg. Borrar de agv_runtime");
				agv_runtime.remove(space);
			}
		}

		int				i, k = 0;
		String			name;
		
		if (l_mates==null || robotid.equals (space))				return;
				
		for (i = 0; i < MAX_MATES; i++)
		{
			name		= l_mates[i].label ();
			if ((name != null) && (name.equals (space)))
			{
				l_mates[i].update (pos, coord.position);
				return;
			}
		}
		
		k	= 0;
		for (i = 0; i < MAX_MATES; i++)
			if (l_mates[i].label () == null)
			{
				k = i;
				break;
			}
		
		l_mates[k].label (space);
		l_mates[k].set (rdesc);						// TODO We assume ALL robots are similar
		l_mates[k].update (pos, coord.position);
	}

	public void notify_config (String space, ItemConfig item)
	{
		int			i;
		
		super.notify_config (space, item);
		
		// Modify LPS geometry to accomodate to laser ranges
		l_rbuffer.setRangeLRF (RBUF_DIST);
		l_rbuffer.setSize (fdesc.RAYSCAN * RBUF_HIST);
		l_rbuffer.resetBuffer ();

		// Add domain specific LPOs to the LPS
		l_robot		= new LPOIForkData ("IForkData", LPO.PERCEPT);	
		l_pbug		= new LPORangePBug (fdesc.RAYSCAN, fdesc.CONESCAN, RBUF_DIST, 0.2, "PolarBug", LPO.PERCEPT, world);
		l_avoid		= new LPOLine (0.0, 0.0, 0.0, "Avoid", LPO.ARTIFACT);	
		lps.add (l_robot);
		lps.add (l_pbug);
		lps.add (l_avoid);
		
		l_mates		= new LPOMate[MAX_MATES];
		for (i = 0; i < MAX_MATES; i++)
		{
			l_mates[i]	= new LPOMate (LPO.ANCHOR);
			lps.add (l_mates[i]);
		}
		
		// Initialise Odometry-correction EKF
		kloc			= new IForkKLoc ((TricycleDrive) rdesc.model);
		kpos			= new Position ();

		// Local behaviour configuration
		segments		= false; 
		kfilter		= true;
	}
	
	public synchronized void notify_delrobot(String space,ItemDelRobot item){
		String name;
		
		if(l_mates == null) return;
		
		if(item.cmd==ItemDelRobot.INFO){
//			System.out.println("  [IForkPerception] Recibido tuple INFO "+space+" item="+item);
		}
		else if(item.cmd==ItemDelRobot.DELETE){
//			System.out.println("  [IForkPerception] Recibido tuple delrobot yo="+robotid+" el otro="+item+" space="+space);
			
			agv_runtime.put(item.robotid, Long.valueOf (System.currentTimeMillis()));
			
			if(robotid.equalsIgnoreCase(item.robotid)){
				
				System.out.println("  [IForkPerception] Stop robot "+robotid);
				stop();
				return;
			}
			for (int i = 0; i < MAX_MATES; i++)
			{
				name		= l_mates[i].label ();
				if ((name != null) && (name.equals (item.robotid)))
				{
					System.out.println("  [IForkPerception] Robot "+robotid+": eliminando "+name);
					l_mates[i].active(false);
					l_mates[i].label(null);
				}
			}
		}
	}
}

