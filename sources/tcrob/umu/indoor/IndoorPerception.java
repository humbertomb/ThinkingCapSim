/*
 * (c) 2001 Humberto Martinez
 */

package tcrob.umu.indoor;

import tc.runtime.thread.ModuleConfig;
import tc.shared.linda.*;
import tc.shared.lps.*;
import tc.shared.lps.lpo.*;
import tc.shared.world.*;
import tc.modules.*;

import tclib.navigation.mapbuilding.lpo.*;
import tc.shared.lps.gui.LPSWindow;

import tclib.utils.pos.*;
import wucore.utils.geom.*;
import wucore.utils.math.*;
import wucore.utils.math.jama.*;

public class IndoorPerception extends Perception
{
	// Fuzzy segments map generation mode
	public static final int			UPD_RATE		= 10;			// Segments update rate
	
	// Initial uncertainty parameters
	static public final double		INIT_VAR_R		= 0.0001218;	// Initial rotation variance (rad2) 	<= 2 deg
	static public final double		INIT_VAR_T		= 0.00009;		// Initial translation variance (m2)	<= 0.003 m
	
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
	protected double				max_range		= 2.5;			// Range buffer maximum length
	protected int					max_buffer		= 200;			// Maximum number of range points
	
	protected LPOPoint				l_home;							// Home position
	protected LPOPoint				l_goal;							// Goal position
	protected LPOLine				l_looka;						// Look-ahead point
	protected LPORangeBuffer		l_rbuffer;						// Range buffer
	protected LPOSensorRange		l_virtual;						// Virtual ranges sensor
	protected LPOSensorGroup		l_group;						// Group sensor
	protected LPOSensorScanner		l_scan;							// Scanner sensor
	protected LPOSensorFSeg			l_fsegs;						// Fuzzy segments
	
	// Robot perception subsystems. Enable status
	protected boolean				segments		= true;
	protected boolean				firstime		= true;
	
	// Debugging tools and windows
	protected LPSWindow				win;
	
	// Constructors
	public IndoorPerception (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}
	
	// Instance methods
	protected void initialise (ModuleConfig cfg)
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
		
		super.initialise (cfg);
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
		// Signal-level sensor fusion and LPS update
		fusion.fuse_signal (data);	
		lps.update_anchors ();
		lps.clamp (pos);
		
		// what the robot sees around it, which the sensors of an area are worked out of
		fill_buffer ();
		l_virtual.update (fusion.virtuals, fusion.virtuals_flg);
		l_scan.update (fusion.scans, fusion.scans_flg);
		
		// Feature-level sensor fusion and LPS update
		fusion.fuse_feature (lps, data);	
		l_group.update (fusion.groups, fusion.groups_flg);
				
		// Update low-level perception & LPS data
		lps.update (data, fusion, lodom, pos, null);
		
		if (win != null)		win.update (lps, null);
	}
	
	/**
	 * Puts what the range sensors read on this cycle in the range buffer, which is
	 * what the sensors of an area (the groups of the buffer modes) are worked out
	 * of. Every source the robot has is taken, each in the most worked-out form it
	 * is described in:
	 * <ul>
	 * <li>the sonars and the infrared, through the fused sensors when there are
	 *     any (one reading per direction, fused as each says), and one by one when
	 *     there are none: each sonar, and each infrared that sees something (one
	 *     near the end of its range does not, as the fusion takes it);</li>
	 * <li>the laser range finders, through the reduced scans when there are any
	 *     (each a fan made of the rays of every laser that reads where it looks),
	 *     and ray by ray, every laser of the robot, when there is none.</li>
	 * </ul>
	 * Only what was read on this cycle goes in, as the fused sensors always did.
	 */
	protected void fill_buffer ()
	{
		int				i, k;
		double			delta, alpha;

		// sonars and infrared: the fused sensors, or each of them when there are none
		if (fdesc.MAXVIRTU > 0)
		{
			for (i = 0; i < fdesc.MAXVIRTU; i++)
				if (fusion.virtuals_flg[i])
					l_rbuffer.add_range (fdesc.virtufeat[i], i, fusion.virtuals[i]);
		}
		else
		{
			for (i = 0; i < rdesc.MAXSONAR; i++)
				if (data.sonars_flg[i])
					l_rbuffer.add_range (rdesc.sonfeat[i], i, data.sonars[i]);
			for (i = 0; i < rdesc.MAXIR; i++)
				if (data.irs_flg[i])			// one at the end of its range saw nothing (0: no point)
					l_rbuffer.add_range (rdesc.irfeat[i], rdesc.MAXSONAR + i, (data.irs[i] < 0.9 * rdesc.RANGEIR) ? data.irs[i] : 0.0);
		}

		// laser range finders: the reduced scans, each made of every laser, or every
		// ray of every laser when there is none
		if (fdesc.MAXSCAN > 0)
		{
			int		id = 0;

			for (k = 0; k < fdesc.MAXSCAN; id += fdesc.scanrays[k], k++)
			{
				int		n = fdesc.scanrays[k];

				if (!fusion.allscans_flg[k])		continue;
				delta	= (n > 1) ? fdesc.scancones[k] / (double) (n - 1) : 0.0;
				alpha	= -fdesc.scancones[k] / 2.0;
				for (i = 0; i < n; i++, alpha += delta)
					l_rbuffer.add_range (fdesc.scanfeats[k], id + i, fusion.allscans[k][i], alpha);
			}
		}
		else if ((rdesc.RAYLRF > 0) && (data.lrfs != null))
		{
			delta	= (rdesc.RAYLRF > 1) ? rdesc.CONELRF / (double) (rdesc.RAYLRF - 1) : 0.0;
			for (k = 0; k < Math.min (rdesc.MAXLRF, data.lrfs.length); k++)
			{
				if ((data.lrfs[k] == null) || (data.lrfs_flg == null) || !data.lrfs_flg[k] || (rdesc.lrffeat[k] == null))		continue;
				alpha	= -rdesc.CONELRF / 2.0;
				for (i = 0; i < Math.min (rdesc.RAYLRF, data.lrfs[k].length); i++, alpha += delta)
					l_rbuffer.add_range (rdesc.lrffeat[k], k * rdesc.RAYLRF + i, data.lrfs[k][i], alpha);
			}
		}
	}

	/**
	 * How many points the range buffer keeps: PPR_BUFFER readings of each of the
	 * sonar-like sources (the fused sensors, or the sonars and infrared one by
	 * one), which are slow and sparse and are remembered as the robot moves, and a
	 * couple of cycles of the laser ones (the reduced scans, or every ray of every
	 * laser), which are dense and fresh on every cycle; never fewer than the 200
	 * of old.
	 */
	protected int buffer_size ()
	{
		int		sparse = (fdesc.MAXVIRTU > 0) ? fdesc.MAXVIRTU : rdesc.MAXSONAR + rdesc.MAXIR;
		int		dense = 0;

		for (int k = 0; k < fdesc.MAXSCAN; k++)		dense += fdesc.scanrays[k];
		if (fdesc.MAXSCAN == 0)						dense = rdesc.MAXLRF * rdesc.RAYLRF;

		return Math.max (200, LPS.PPR_BUFFER * sparse + LASER_CYCLES * dense);
	}

	/** How many cycles of laser readings the range buffer has room for. */
	static public final int			LASER_CYCLES	= 2;

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
	
	protected void close_gfx ()
	{
		if (win != null)		win.close ();
		win		= null;
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
		
		// other modules of the robot may work on this LPS too (guests, as SoccerVision)
		synchronized (lps)
		{
			// Sensor fusion and LPS update
			lowlevel_fusion ();
			
			// Map-level sensor fusion
			maplevel_fusion ();
			
			lps.add_time ((double) (System.currentTimeMillis () - ctime));
			
			// Update the LPS in the Linda space
			tupd	 = ctime - stime;		
			lstore.set (lps, tupd);		
			linda.write (ltuple);
		}
		
		// Finish processing by clearing flags
		data		= null;
	}
	
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
		
		max_buffer	= buffer_size ();
		
		// Add domain specific LPOs to the LPS
		l_home		= new LPOPoint (0.0, 0.0, 0.0, "Home", LPOSource.MAP);	
		l_goal		= new LPOPoint (0.0, 0.0, 0.0, "Goal",  LPOSource.ARTIFACT);	
		l_looka		= new LPOLine (0.0, 0.0, 0.0, "Looka", LPOSource.ARTIFACT);	
		l_rbuffer	= new LPORangeBuffer (max_buffer, max_range, 0.1, "RBuffer", LPOSource.PERCEPT);
		l_virtual	= new LPOSensorRange (fdesc.virtufeat, "Virtual", LPOSource.PERCEPT);		
		l_group		= new LPOSensorGroup (fdesc.groupfeat, "Group", LPOSource.PERCEPT);	
		l_scan		= new LPOSensorScanner (fdesc.scanfeat, fdesc.RAYSCAN, fdesc.CONESCAN, "Scanner", LPOSource.PERCEPT);	
		l_fsegs		= new LPOSensorFSeg (max_buffer, fdesc.RAYSCAN, "FSegs", LPOSource.PERCEPT);	
		
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
			WMConnector			door;
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
			for (i = 0; i < world.connectors ().n (); i++)
			{
				door	= world.connectors ().at (i);
				pdoor	= door.edge.center ();
				
				xx		= pdoor.x ();
				yy		= pdoor.y ();
				
				x		= (rotm.mat[0][0] * xx) + (rotm.mat[0][1] * yy) + rotm.mat[0][2];
				y		= (rotm.mat[1][0] * xx) + (rotm.mat[1][1] * yy) + rotm.mat[1][2];	
				a		= Angles.radnorm_180 (rotm.mat[2][2]);
				
				l_door		= new LPOPoint (x, y, a, door.label, LPOSource.MAP);
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
				
				l_zone		= new LPOPoint (x, y, a, zone.label, LPOSource.MAP);
				l_zone.active (true);
				
				lps.add (l_zone);
			}
		}
		
		
		if (localgfx)
			win		= new LPSWindow (robotid);

		//		lps.dump ();
		firstime	= true;
	}
		
	public void notify_navigation (String space, ItemNavigation item) 
	{ 
		// Update corrected robot position and its uncertainty matrix
		if (item.robot.valid)
		{
			pos_upd		= item.timestamp.longValue ();
			cpos.set (item.robot);
		}
	}	
}

