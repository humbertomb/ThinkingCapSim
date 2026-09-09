/*
 * (c) 2001 Humberto Martinez
 */

package tcrob.umu.indoor;

import java.util.*;

import tc.shared.linda.*;
import tc.shared.lps.*;
import tc.shared.lps.lpo.*;
import tc.shared.world.*;
import tc.modules.*;

import tclib.navigation.mapbuilding.lpo.*;
import tcrob.umu.indoor.gui.*;

import devices.pos.*;
import wucore.utils.geom.*;
import wucore.utils.math.*;
import wucore.utils.math.jama.*;

import devices.data.*;

public class IndoorPerception extends Perception
{
	// Fuzzy segments map generation mode
	public static final int			UPD_RATE		= 10;			// Segments update rate
	
	// Initial uncertainty parameters
	static public final double		INIT_VAR_R		= 0.0001218;	// Initial rotation variance (rad2) 	<= 2 deg
	static public final double		INIT_VAR_T		= 0.00009;		// Initial translation variance (m2)	<= 0.003 m
	
	// Robot and perception structures
	protected VisionData[]			vdata			= null;
	
	// Localisation related structures
	protected Position				pos;							// Current position (corrected)
	protected Position				cpos;							// Last corrected position
	protected Position				lodom;							// Last odometric position
	protected Position				dodom;							// Current odometric displacement
	protected long					tupd;							// Time of the last update (sent)
	protected long					pos_upd;						// Time of the last position update (received)
	
	// Fuzzy segments map parameters
	protected int					count_upd		= 0;
	
	// LPS configuration and application LPOs
	protected double					max_range		= 2.5;			// Range buffer maximum length
	protected int					max_buffer		= 200;			// Maximum number of range points
	
	protected LPOPoint				l_home;							// Home position
	protected LPOPoint				l_goal;							// Goal position
	protected LPOLine				l_looka;						// Look-ahead point
	protected LPORangeBuffer			l_rbuffer;						// Range buffer
	protected LPOSensorRange			l_virtual;						// Virtual ranges sensor
	protected LPOSensorGroup			l_group;						// Group sensor
	protected LPOSensorScanner		l_scan;							// Scanner sensor
	protected LPOSensorFSeg			l_fsegs;						// Fuzzy segments
	
	// Robot perception subsystems. Enable status
	protected boolean				segments		= true;
	protected boolean				firstime		= true;
	
	// Debugging tools and windows
	protected IndoorLPSWindow			win;
	
	// Constructors
	public IndoorPerception (Properties props, Linda linda)
	{
		super (props, linda);
	}
	
	// Instance methods
	protected void initialise (Properties props)
	{		
		Matrix		uncert;
		
		// Create local structures
		uncert		= new Matrix (3, 3);
		pos			= new Position ();
		cpos		= new Position ();
		lodom		= new Position ();
		dodom		= new Position ();
		
		// Initialise local structures
		uncert.diagonal (INIT_VAR_T);
		uncert.set (2, 2, INIT_VAR_R);
		pos.set (uncert);
		
		super.initialise (props);
	}
	
	protected void position_correction ()
	{
		// Update current robot location
		if (firstime)
		{
			pos.set (data.odom_x, data.odom_y, data.odom_a);
			firstime	= false;
		}
		else
		{
			dodom.set (data.odom_x, data.odom_y, data.odom_a);
			dodom.delta (lodom);
			if (tupd == pos_upd)
			{
				pos.set (cpos);
				if (debug)		System.out.println ("  [Per]: correction update: ="+ tupd);
			}
			pos.translate (dodom);
		}
		lodom.set (data.odom_x, data.odom_y, data.odom_a);		
	}
	
	protected void lowlevel_fusion ()
	{
		int				i;
		double			delta, alpha;
		
		// Signal-level sensor fusion and LPS update
		fusion.fuse_signal (data);	
		lps.update_anchors ();
		lps.clamp (pos);
		
		for (i = 0; i < fdesc.MAXVIRTU; i++)
		{
			if (fusion.virtuals_flg[i])
				l_rbuffer.add_range (fdesc.virtufeat[i], i, fusion.virtuals[i]);
		}
		l_virtual.update (fusion.virtuals, fusion.virtuals_flg);
		
		if (fusion.scans_flg)
		{
			delta	= fdesc.CONESCAN / (double) (fdesc.RAYSCAN - 1);
			alpha	= -fdesc.CONESCAN / 2.0;		
			
			for (i = 0; i < fdesc.RAYSCAN; i++, alpha += delta)
				l_rbuffer.add_range (fdesc.scanfeat, i, fusion.scans[i], alpha);
		}
		l_scan.update (fusion.scans, fusion.scans_flg);
		
		// Feature-level sensor fusion and LPS update
		fusion.fuse_feature (lps, data);	
		l_group.update (fusion.groups, fusion.groups_flg);
		
		// Object-level fusion and LPS update
		if (vdata != null)
			for (i = 0; i < vdata.length; i++)
				if (vdata[i].valid)
					lps.set_lpo (vdata[i]);
		vdata		= null;
		
		// Update low-level perception & LPS data
		lps.update (data, fusion, lodom, pos, null);
		
		if (win != null)		win.update (lps, null);
	}
	
	protected void maplevel_fusion ()
	{
		l_fsegs.active (segments);
		
		if (!segments)				return;
		
		// Map-level sensor fusion (sonar generated)
		if (fdesc.MAXVIRTU > 0)
		{
			count_upd ++;
			
			if (count_upd % UPD_RATE == 0)
			{
				l_rbuffer.sort_buffer ();
				l_fsegs.reset_inviewSON ();
				l_fsegs.do_segments (l_rbuffer, pos.getMatrix ());
			}	
		}
		
		// Map-level sensor fusion (laser generated)
		if (fdesc.RAYSCAN > 0)
		{
			//			l_fsegs.reset_inviewLRF ();
			if (l_scan.active ())
			{
				l_fsegs.reset_inviewLRF ();
				l_fsegs.do_segments (l_scan, pos.getMatrix ());
			}
		}
		
		// Fuse maps (sonar and laser) and update LPS
		l_fsegs.merge_inviews ();
	}
	
	public void step (long ctime)
	{
		if ((state != RUN) || (data == null))		return;
		
		// Update current robot location
		position_correction ();
		
		if (firstime)
		{
			data	= null;
			return;
		}
		
		// Sensor fusion and LPS update
		lowlevel_fusion ();
		
		// Map-level sensor fusion
		maplevel_fusion ();
		
		lps.add_time ((double) (System.currentTimeMillis () - ctime));
		
		// Update the LPS in the Linda space
		tupd	 = ctime - stime;		
		lstore.set (lps, tupd);		
		linda.write (ltuple);
		
		// Finish processing by clearing flags
		data		= null;
	}
	
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
		
		if (fdesc.MAXVIRTU > 0)
			max_buffer	= LPS.PPR_BUFFER * fdesc.MAXVIRTU;
		
		// Add domain specific LPOs to the LPS
		l_home		= new LPOPoint (0.0, 0.0, 0.0, "Home", LPO.MAP);	
		l_goal		= new LPOPoint (0.0, 0.0, 0.0, "Goal",  LPO.ARTIFACT);	
		l_looka		= new LPOLine (0.0, 0.0, 0.0, "Looka", LPO.ARTIFACT);	
		l_rbuffer	= new LPORangeBuffer (max_buffer, max_range, 0.1, "RBuffer", LPO.PERCEPT);
		l_virtual	= new LPOSensorRange (fdesc.virtufeat, "Virtual", LPO.PERCEPT);		
		l_group		= new LPOSensorGroup (fdesc.groupfeat, "Group", LPO.PERCEPT);	
		l_scan		= new LPOSensorScanner (fdesc.scanfeat, fdesc.RAYSCAN, fdesc.CONESCAN, "Scanner", LPO.PERCEPT);	
		l_fsegs		= new LPOSensorFSeg (max_buffer, fdesc.RAYSCAN, "FSegs", LPO.PERCEPT);	
		
		lps.add (l_home);
		lps.add (l_goal);
		lps.add (l_looka);
		lps.add (l_rbuffer);
		lps.add (l_virtual);
		lps.add (l_group);
		lps.add (l_scan);
		lps.add (l_fsegs);
		
		// Add map-related LPOs to the LPS
		if (world != null)
		{
			int				i;
			WMDoor			door;
			LPOPoint		l_door;	
			Point2			pdoor;		
			WMZone			zone;
			LPOPoint		l_zone;	
			Matrix3D		rotm;
			double			xx, yy;
			double			x, y, a;
			
			rotm = new Matrix3D ();
			rotm.toFrame (world.start_x (), world.start_y (), world.start_a ());
			
			// Add doors to the LPS
			for (i = 0; i < world.doors ().n (); i++)
			{
				door	= world.doors ().at (i);
				pdoor	= door.edge.center ();
				
				xx		= pdoor.x ();
				yy		= pdoor.y ();
				
				x		= (rotm.mat[0][0] * xx) + (rotm.mat[0][1] * yy) + rotm.mat[0][2];
				y		= (rotm.mat[1][0] * xx) + (rotm.mat[1][1] * yy) + rotm.mat[1][2];	
				a		= Angles.radnorm_180 (rotm.mat[2][2]);
				
				l_door		= new LPOPoint (x, y, a, door.label, LPO.MAP);
				l_door.active (true);
				
				lps.add (l_door);
			}
			
			// Add zones (rooms) to the LPS
			for (i = 0; i < world.zones ().n (); i++)
			{
				zone	= world.zones ().at (i);
				
				xx		= zone.area.getCenterX ();
				yy		= zone.area.getCenterY ();
				
				x		= (rotm.mat[0][0] * xx) + (rotm.mat[0][1] * yy) + rotm.mat[0][2];
				y		= (rotm.mat[1][0] * xx) + (rotm.mat[1][1] * yy) + rotm.mat[1][2];	
				a		= Angles.radnorm_180 (rotm.mat[2][2]);
				
				l_zone		= new LPOPoint (x, y, a, zone.label, LPO.MAP);
				l_zone.active (true);
				
				lps.add (l_zone);
			}
		}
		
		
		if (localgfx)
			win		= new IndoorLPSWindow (robotid);

		//		lps.dump ();
		firstime	= true;
	}
	
	public void notify_object (String space, ItemObject item)
	{
		// Quit if still processing previous data
		if (vdata != null)		
		{
			System.out.println ("--[Per]: Discarding visual data. Unprocessed event");
			return;
		}
		
		// Get data structures from Linda space
		vdata	= item.data;
	}
	
	public void notify_navigation (String space, ItemNavigation item) 
	{ 
		// Update corrected robot position and its uncertainty matrix
		if (item.robot.valid ())
		{
			pos_upd		= item.timestamp.longValue ();
			cpos.set (item.robot);
		}
	}	
}

