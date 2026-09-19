/*
 * (c) 2001-2003 Humberto Martinez
 */
 
package tc.modules;

import java.io.*;

import tc.fleet.*;
import tc.shared.lps.*;
import tc.shared.lps.lpo.*;

import tclib.utils.fusion.*;

import devices.pos.*;

public class MonitorData extends VehicleData implements Serializable 
{
	// Robot corrected position
	public Position				odom;					// Odometry-based coordinates (m, m, rad)
	public Position				real;					// Real coordinates for ground truth (m, m, rad)

	// Signal-level and feature-level sensor fusion
	public double[]				virtuals;				// Virtual sensor values
	public boolean[]			virtuals_flg;			// Virtual sensor update flags
	public double[]				groups;					// Group sensor values
	public boolean[]			groups_flg;				// Group sensor update flags
	public double[] 			scans;					// Scanner sensor values
	public boolean				scans_flg;				// Scanner sensor update flags
	public boolean[] 			dsignals;				// Digital inputs values
	public boolean[]			dsignals_flg;			// Digital inputs update flags
	public int					pal_switch			= -1;
	
	/* Constructors */
	public MonitorData ()
	{
		super ();
	}
	
	// Instance methods
	public void update (LPS lps)
	{
		int					i;
		FusionDesc			fdesc;
		LPOSensorRange		virtual;
		LPOSensorGroup		group;
		LPOSensorScanner	scan;
		boolean				doinitialise;

		// Update superclass and check if an initialisation is needed
		doinitialise	= !initialised;
		update (lps.cur, lps.pose, lps.qlty);
		pal_switch = lps.pal_switch;
		
		fdesc	= lps.fdesc ();
		
		if (doinitialise)
		{
			if (fdesc != null)
			{
				virtuals		= new double [fdesc.MAXVIRTU];
				virtuals_flg	= new boolean [fdesc.MAXVIRTU];
					
				groups			= new double [fdesc.MAXGROUP];
				groups_flg		= new boolean [fdesc.MAXGROUP];

				dsignals		= new boolean [fdesc.MAXDSIG];
				dsignals_flg	= new boolean [fdesc.MAXDSIG];
					
				scans			= new double [fdesc.RAYSCAN];
			}
			
			odom			= new Position ();
			real			= new Position ();
		}

		virtual	= (LPOSensorRange) lps.find ("Virtual");
		if (virtual != null)
			for (i = 0; i < fdesc.MAXVIRTU; i++)
			{
				virtuals[i]		= virtual.range[i];
				virtuals_flg[i]	= virtual.valid[i];
			}
		
		group	= (LPOSensorGroup) lps.find ("Group");
		if (group != null)
			for (i = 0; i < fdesc.MAXGROUP; i++)
			{
				groups[i]		= group.range[i];
				groups_flg[i]	= group.valid[i];
			}

		if (fdesc != null)
			for (i = 0; i < fdesc.MAXDSIG; i++)
			{
				dsignals[i]		= lps.dsignals[i];
				dsignals_flg[i]	= lps.dsignals_flg[i];
			}

		scan	= (LPOSensorScanner) lps.find ("Scanner");
		if (scan != null)
		{
			for (i = 0; i < fdesc.RAYSCAN; i++)
				scans[i]		= scan.range[i];
			scans_flg		= scan.active ();
		}
		
		odom.set (lps.odom);
		real.set (lps.real);
	}
} 