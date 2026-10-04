/*
 * (c) 1997-2001 Humberto Martinez
 */
 
package tclib.utils.fusion;

import wucore.utils.math.Angles;

import tc.vrobot.*;
import tc.shared.lps.*;
import tc.shared.lps.lpo.*;

public class Fusion extends Object
{
	public double[]						virtuals;				// Virtual sensor values
	public boolean[]					virtuals_flg;			// Virtual sensor update flag
	public double[]						groups;					// Group sensor values
	public boolean[]					groups_flg;				// Group sensor update flag
	public double[] 					scans;					// Scanner sensor values (the first virtual scanner: allscans[0])
	public boolean						scans_flg;				// Scanner sensor update flags
	public double[][]					allscans;				// The values of every virtual scanner (one per reduced laser scan)
	public boolean[]					allscans_flg;			// ... and whether each was read on this cycle
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
		allscans		= new double [fdesc.MAXSCAN][];
		allscans_flg	= new boolean [fdesc.MAXSCAN];
		for (int k = 0; k < fdesc.MAXSCAN; k++)
			allscans[k]	= new double [Math.max (0, fdesc.scanrays[k])];
		scans			= (fdesc.MAXSCAN > 0) ? allscans[0] : new double [Math.max (0, fdesc.RAYSCAN)];
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
	 * A virtual scanner: the fan of a reduced laser scan (its rays over its cone,
	 * from where it sits and looking where it looks) made out of the rays of every
	 * laser range finder of the robot that has readings where the fan looks. Each
	 * reading of a laser is the point it hit (or the end of its range), and is seen
	 * from where the fan sits: by its bearing it falls to the rays of the fan within
	 * half a ray of it (half a ray of the fan, or of the laser as it is seen from
	 * there, whichever is wider), and each ray of the fan keeps the nearest of what
	 * falls to it, or the mean, as its mode says. A ray that nothing falls to takes
	 * the nearest one that something did, up to SCAN_GAP rays away, and otherwise
	 * reads the range of the fan, as one that sees nothing. When the fan and a laser sit in the same place this is
	 * the old reduction of that laser, ray by ray.
	 *
	 * The rays of the lasers are as the simulator and the perception take them: from
	 * one end of the cone to the other, both included (the rays of the fan as well),
	 * all of them with the rays and
	 * the cone of the description (RAYLRF, CONELRF). The scan is read on this cycle
	 * when any of the lasers was.
	 */
	protected void scanner (RobotData data, int k)
	{
		SensorPos		f = fdesc.scanfeats[k];
		double[]		out = allscans[k];
		int				n = out.length;
		double			cone = fdesc.scancones[k], range = fdesc.scanranges[k];
		boolean			avg = (f != null) && (f.mode () == FusionDesc.S_AVG);
		double			sStep = (n > 1) ? cone / (double) (n - 1) : 0.0;
		double			sStart = ((f != null) ? f.orientation () : 0.0) - cone * 0.5;
		double			ox = (f != null) ? f.x () : 0.0, oy = (f != null) ? f.y () : 0.0;
		double[]		acc = new double[n];
		int[]			cnt = new int[n];
		boolean			read = false;

		allscans_flg[k]	= false;
		if (n == 0)					return;
		for (int i = 0; i < n; i++)		acc[i] = avg ? 0.0 : Double.MAX_VALUE;

		if ((rdesc.MAXLRF > 0) && (rdesc.RAYLRF > 0) && (data.lrfs != null))
			for (int l = 0; l < Math.min (rdesc.MAXLRF, data.lrfs.length); l++)
			{
				double[]	lrf = data.lrfs[l];
				SensorPos	lf = ((rdesc.lrffeat != null) && (l < rdesc.lrffeat.length)) ? rdesc.lrffeat[l] : null;
				int			nl;
				double		lStep, lStart;

				if ((lrf == null) || (lf == null))		continue;
				nl		= Math.min (rdesc.RAYLRF, lrf.length);
				lStep	= (nl > 1) ? rdesc.CONELRF / (double) (nl - 1) : 0.0;
				lStart	= lf.orientation () - rdesc.CONELRF * 0.5;
				if ((data.lrfs_flg != null) && (l < data.lrfs_flg.length) && data.lrfs_flg[l])		read = true;

				for (int j = 0; j < nl; j++)
				{
					double	r = lrf[j], b = lStart + j * lStep;
					double	px = lf.x () + r * Math.cos (b), py = lf.y () + r * Math.sin (b);
					double	dx = px - ox, dy = py - oy, d = Math.sqrt (dx * dx + dy * dy);
					double	half, c;
					int		i0, i1;

					// how wide the reading is from the fan, and where it falls in it
					half	= 0.5 * Math.max (sStep, (d > 1E-6) ? lStep * r / d : lStep);
					c		= Angles.radnorm_180 (Math.atan2 (dy, dx) - sStart - cone * 0.5) + cone * 0.5;		// from the start of the fan (measured round its middle)
					if (sStep <= 0.0)
					{
						if (Math.abs (c - cone * 0.5) > Math.max (half, cone * 0.5))		continue;
						i0	= i1 = 0;
					}
					else
					{
						i0	= (int) Math.ceil ((c - half) / sStep - 1E-9);
						i1	= (int) Math.floor ((c + half) / sStep + 1E-9);
						if (i1 < i0)			i1 = i0 = (int) Math.round (c / sStep);
					}
					for (int i = i0; i <= i1; i++)
					{
						int		q = i;

						if ((q < 0) || (q >= n))		continue;		// outside the fan
						if (avg)			acc[q] += d;
						else				acc[q] = Math.min (acc[q], d);
						cnt[q] ++;
					}
				}
			}

		for (int i = 0; i < n; i++)
			out[i]	= (cnt[i] == 0) ? Double.NaN : (avg ? acc[i] / cnt[i] : acc[i]);
		// a ray nothing fell to takes the nearest one that something did, a few rays
		// away at most (a fan that sits off the laser sees its edges from aside, and
		// a ray or two at the end get nothing), and otherwise sees nothing
		for (int i = 0; i < n; i++)
		{
			if (cnt[i] > 0)				continue;
			double	v = range;
			for (int g = 1; g <= SCAN_GAP; g++)
			{
				if ((i - g >= 0) && (cnt[i - g] > 0))		{ v = out[i - g];	break; }
				if ((i + g < n) && (cnt[i + g] > 0))		{ v = out[i + g];	break; }
			}
			acc[i]	= v;
		}
		for (int i = 0; i < n; i++)
			if (cnt[i] == 0)			out[i] = acc[i];
		allscans_flg[k]	= read;
	}		
	
	/** How many rays away a ray of a virtual scanner that nothing fell to looks for one that something did. */
	static public final int				SCAN_GAP		= 3;

	public void fuse_signal (RobotData data)
	{
		int			i;
		boolean		btmp;
		
		// Perform signal-level sensor fusion (virtual sensors)
		for (i = 0; i < fdesc.MAXVIRTU; i++)
			virtual (i, data, fdesc.virtufeat[i]);       

		// Perform signal-level sensor fusion (virtual scanners: every reduced laser scan)
		for (i = 0; i < fdesc.MAXSCAN; i++)
			scanner (data, i);
		scans_flg	= (fdesc.MAXSCAN > 0) && allscans_flg[0];
		
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
	
	/**
	 * Puts what the range sensors read on this cycle in a range buffer, which is
	 * what the sensors of an area (the groups of the buffer modes) are worked out
	 * of: the sonars and the infrared through the fused sensors when there are
	 * any, and one by one when there are none (an infrared only when it sees
	 * something); the laser range finders through the reduced scans when there
	 * are any, and ray by ray, every laser, when there is none. Only what was
	 * read on this cycle goes in. The signal-level fusion must have been done
	 * ({@link #fuse_signal}).
	 */
	public void fill (LPORangeBuffer rb, RobotData data)
	{
		int			i, k;
		double		delta, alpha;

		if (fdesc.MAXVIRTU > 0)
		{
			for (i = 0; i < fdesc.MAXVIRTU; i++)
				if (virtuals_flg[i])
					rb.add_range (fdesc.virtufeat[i], i, virtuals[i]);
		}
		else
		{
			for (i = 0; i < rdesc.MAXSONAR; i++)
				if (data.sonars_flg[i])
					rb.add_range (rdesc.sonfeat[i], i, data.sonars[i]);
			for (i = 0; i < rdesc.MAXIR; i++)
				if (data.irs_flg[i])
					rb.add_range (rdesc.irfeat[i], rdesc.MAXSONAR + i, (data.irs[i] < 0.9 * rdesc.RANGEIR) ? data.irs[i] : 0.0);
		}

		if (fdesc.MAXSCAN > 0)
		{
			int		id = 0;

			for (k = 0; k < fdesc.MAXSCAN; id += fdesc.scanrays[k], k++)
			{
				int		n = fdesc.scanrays[k];

				if (!allscans_flg[k])		continue;
				delta	= (n > 1) ? fdesc.scancones[k] / (double) (n - 1) : 0.0;
				alpha	= -fdesc.scancones[k] / 2.0;
				for (i = 0; i < n; i++, alpha += delta)
					rb.add_range (fdesc.scanfeats[k], id + i, allscans[k][i], alpha);
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
					rb.add_range (rdesc.lrffeat[k], k * rdesc.RAYLRF + i, data.lrfs[k][i], alpha);
			}
		}
	}

	/** How many readings one cycle puts in a range buffer at most ({@link #fill}). */
	public int readings ()
	{
		int		n = (fdesc.MAXVIRTU > 0) ? fdesc.MAXVIRTU : rdesc.MAXSONAR + rdesc.MAXIR;

		if (fdesc.MAXSCAN > 0)		for (int k = 0; k < fdesc.MAXSCAN; k++)		n += fdesc.scanrays[k];
		else						n += rdesc.MAXLRF * rdesc.RAYLRF;
		return n;
	}

	public void fuse_feature (LPS lps, RobotData data)
	{
		int			i;
        
        // Perform feature-level sensor fusion
		for (i = 0; i < fdesc.MAXGROUP; i++)
			group (i, lps, data, fdesc.groupfeat[i]); 
	}
} 