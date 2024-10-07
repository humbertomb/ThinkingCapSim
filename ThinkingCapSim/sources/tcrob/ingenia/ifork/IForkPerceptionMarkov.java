/*
 * (c) 2001 Humberto Martinez
 */

package tcrob.ingenia.ifork;

import java.util.*;

import tc.shared.linda.*;

import tclib.navigation.localisation.fmarkov.*;
import tclib.navigation.mapbuilding.lpo.*;

public class IForkPerceptionMarkov extends IForkPerception
{
	public static double			RBORDER = 0.3; // Border size (m)
	public static double			RPIXELSIZE = 0.7; // Desired pixelation (m)

	// Markov Fuzzy Self-Localisation 
	protected MK2_5FGrid			mkgrid;
	
	// Constructors
	public IForkPerceptionMarkov (Properties props, Linda linda)
	{
		super (props, linda);		
	}
	
	// Instance methods
	protected void position_correction ()
	{
		super.position_correction ();
		
		mkgrid.convolve (dodom.x(), dodom.y(), dodom.alpha());
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
				
				// Sensor information
				LPOFSegments segslrf = l_fsegs.segsLRF();
				mkgrid.introducePerceptions(segslrf);				
			}
		}
		
		// Fuse maps (sonar and laser) and update LPS
		l_fsegs.merge_inviews ();
		
		// Center of gravitity
		mkgrid.locate ();
	}
	
	public void notify_config (String space, ItemConfig item)
	{
		super.notify_config (space, item);
		
		mkgrid = new MK2_5FGrid (world, RBORDER, RPIXELSIZE);
	}
}

