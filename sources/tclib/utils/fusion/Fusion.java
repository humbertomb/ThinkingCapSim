/*
 * (c) 1997-2001 Humberto Martinez
 */
 
package tclib.utils.fusion;

import tc.vrobot.*;
import tc.shared.lps.*;
import tc.shared.lps.lpo.*;

public class Fusion extends Object
{
	public double[]						virtuals;				// Virtual sensor values
	public boolean[]					virtuals_flg;			// Virtual sensor update flag
	public double[]						groups;					// Group sensor values
	public boolean[]					groups_flg;				// Group sensor update flag
	public double[] 					scans;					// Scanner sensor values
	public boolean						scans_flg;				// Scanner sensor update flags
	public boolean[] 					dsignals;				// Digital inputs values
	public boolean[]					dsignals_flg;			// Digital inputs update flags
	
	protected RobotDesc					rdesc;					// Robot description
	protected FusionDesc				fdesc;					// Fusion method description
	
	/* Constructors */
	protected Fusion ()
	{
	}

	public Fusion (RobotDesc rdesc, FusionDesc fdesc)
	{
		this.rdesc		= rdesc;
		this.fdesc		= fdesc;

		virtuals		= new double [fdesc.MAXVIRTU];
		virtuals_flg	= new boolean [fdesc.MAXVIRTU];
		groups			= new double [fdesc.MAXGROUP];
		groups_flg		= new boolean [fdesc.MAXGROUP];
		dsignals		= new boolean [fdesc.MAXDSIG];
		dsignals_flg	= new boolean [fdesc.MAXDSIG];
		scans			= new double [fdesc.RAYSCAN];
	}
		
	/* Instance methods */
	protected void virtual (int s, RobotData data, SensorPos a1)
	{
		double			virtual;
		double			sonar, ir;
		int             mode;
		double			min, t;
		int				i, k1, k2;
		boolean			s_flg, i_flg;
		
		// Select fusion mode
		mode    = a1.mode ();
		if (mode == FusionDesc.V_UNDEF)    mode = fdesc.MODEVIRTU;
		
		// Find nearest sonar sensor
		if (rdesc.MAXSONAR > 0)
		{
			min = Double.MAX_VALUE;
			for (i = 0, k1 = 0; i < rdesc.MAXSONAR; i++)
			{
			    t = a1.distance (rdesc.sonfeat[i]);
				if (t < min)
				{
					min = t;
					k1 = i;
				}
			}
			sonar	= data.sonars[k1];
			s_flg	= data.sonars_flg[k1];
		}
		else
		{
			sonar	= 0.0;
			s_flg	= false;
		}
		
		// Find nearest ir sensor
		if (rdesc.MAXIR > 0)
		{
			min = Double.MAX_VALUE;
			for (i = 0, k2 = 0; i < rdesc.MAXIR; i++)
			{
			    t = a1.distance (rdesc.irfeat[i]);
				if (t < min)
				{
					min = t;
					k2 = i;
				}
			}
			ir		= data.irs[k2];
			i_flg	= data.irs_flg[k2];
		}
		else
		{
			ir		= 0.0;
			i_flg	= false;
		}

		// Apply the fusion method depending on the available sensor data
		if (s_flg && i_flg)												// Both sensors available
		{
			switch (mode)
			{
				case FusionDesc.V_SONAR:
					virtual = sonar;
					break;
				case FusionDesc.V_IR:
					virtual = ir;
					break;
				case FusionDesc.V_FILTER:
					if ((sonar >= 0.95 * rdesc.RANGEIR) && (ir >= 0.95 * rdesc.RANGEIR))
						virtual = sonar;
					else
						virtual = fdesc.vfilter.filter (sonar, ir);
					break;
				case FusionDesc.V_FLYNN:
					if (ir >= 0.7 * rdesc.RANGEIR)
						virtual = sonar;
					else
						virtual = ir;					
					break;
				case FusionDesc.V_MIN:
				default:
					virtual = Math.min (sonar, ir);
			}
			
			virtuals[s]			= virtual; 
			virtuals_flg[s]		= true;
		}      
		else if (s_flg && (mode != FusionDesc.V_IR))					// Only sonar available
		{
			virtuals[s]			= sonar; 
			virtuals_flg[s]		= true;
		}
		else if (i_flg && (mode != FusionDesc.V_SONAR))					// Only infrared available
		{
			virtuals[s]			= ir; 
			virtuals_flg[s]		= (ir < 0.9 * rdesc.RANGEIR);
		}
		else															// No sensor available
		{
			virtuals[s]			= fdesc.RANGEVIRTU; 
			virtuals_flg[s]		= false;
		}

		// Put virtual sensor into limit
		virtuals[s]		= Math.max (Math.min (virtuals[s], fdesc.RANGEVIRTU), 0.0); 
	}		

	protected void group (int s, LPS lps, RobotData data, FeaturePos f)
	{
		double			out;
		double			t;
		int				i;
		LPORangeBuffer	rbuffer;
		
		rbuffer	= (LPORangeBuffer) lps.find ("RBuffer");
		switch (f.mode ())
		{
			case FusionDesc.G_BUF_ARC:
				out	= rbuffer.occupied_arc (f, f.cone () * 0.5, f.range (), false);
				break;
			case FusionDesc.G_WBUF_ARC:
				out = rbuffer.occupied_arc (f, f.cone () * 0.5, f.range (), true);
				break;
			case FusionDesc.G_BUF_RECT:
				out = rbuffer.occupied_rect (f, f.base (), f.range (), false);
				break;
			case FusionDesc.G_WBUF_RECT:
				out = rbuffer.occupied_rect (f, f.base (), f.range (), true);
				break;
			case FusionDesc.G_WEIGHT:
				out = 0.0;
				for (i = 0; i < f.n (); i++)
					out += virtuals[f.ndx (i)] * f.wgt (i);
				break;
			case FusionDesc.G_BWEIGHT:
				out = 0.0;
				for (i = 0; i < f.n (); i++)
				{
					t = virtuals[f.ndx (i)];
					if (t > f.range ())		t = f.range ();
					out += t * f.wgt (i);
				}
				break;
			case FusionDesc.G_MIN:
			default:
				out = Double.MAX_VALUE;
				for (i = 0; i < f.n (); i++)
				{
					t = virtuals[f.ndx (i)] * f.wgt (i);
					if (t < out)	out = t;
				}
		}

		groups[s]		= Math.max (out, 0.0);
		groups_flg[s]	= (groups[s] < f.range ());
	}		
	
	/**
	 * The virtual scanner: the rays of the first laser scanner of the robot
	 * (RAYLRF) brought down to the ones of the fusion (RAYSCAN), each the mean or
	 * the least of the ones it takes in. Nothing when the robot has no laser, the
	 * fusion asks for no scanner, or there is no scan in the data: then no scan is
	 * there (scans_flg false).
	 * OJO: this does not take into account more than one LRF sensor.
	 */
	protected void scanner (RobotData data, SensorPos f)
	{
		int				i, j, k;
		int				ratio;
		double			out;
		double[]		lrf;
		
		scans_flg	= false;
		if ((rdesc.MAXLRF <= 0) || (rdesc.RAYLRF <= 0) || (fdesc.RAYSCAN <= 0) || (scans == null))		return;
		if ((data.lrfs == null) || (data.lrfs.length == 0) || ((lrf = data.lrfs[0]) == null))			return;

		ratio	= Math.max (1, rdesc.RAYLRF / fdesc.RAYSCAN);			// a fusion with more rays than the laser takes one each
		for (i = 0; i < Math.min (fdesc.RAYSCAN, scans.length); i++)
		{
			boolean		avg = (f != null) && (f.mode () == FusionDesc.S_AVG);
			int			n = 0;

			out		= avg ? 0.0 : Double.MAX_VALUE;
			for (j = 0; j < ratio; j++)
			{
				k	= Math.min (i * ratio + j, lrf.length - 1);
				if (k < 0)		break;
				if (avg)		out += lrf[k];
				else			out = Math.min (out, lrf[k]);
				n ++;
			}
			scans[i]	= (n == 0) ? 0.0 : (avg ? out / n : out);
		}
		scans_flg	= (data.lrfs_flg != null) && (data.lrfs_flg.length > 0) && data.lrfs_flg[0];
	}		
	
	public void fuse_signal (RobotData data)
	{
		int			i;
		boolean		btmp;
		
		// Perform signal-level sensor fusion (virtual sensors)
		for (i = 0; i < fdesc.MAXVIRTU; i++)
			virtual (i, data, fdesc.virtufeat[i]);       

		// Perform signal-level sensor fusion (virtual scanner)
		scanner (data, fdesc.scanfeat);       
		
		// Perform signal-level sensor fusion (digital signals)
		btmp	= false;	
		for (i = 0; i < rdesc.MAXBUMPER; i++)				// THIS IS A TERRIBLE HACK !!!!!!
			btmp |= data.bumpers[i];						// TO BE CHANGED (SOON)
		for (i = 0; i < fdesc.MAXDSIG; i++)
		{
			dsignals[i]		= btmp;
			dsignals_flg[i]	= true;
		}		
	}
	
	public void fuse_feature (LPS lps, RobotData data)
	{
		int			i;
        
        // Perform feature-level sensor fusion
		for (i = 0; i < fdesc.MAXGROUP; i++)
			group (i, lps, data, fdesc.groupfeat[i]); 
	}
} 