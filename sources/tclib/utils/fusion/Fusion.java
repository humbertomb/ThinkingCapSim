/*
 * (c) 1997-2001 Humberto Martinez
 */
 
package tclib.utils.fusion;

import wucore.utils.math.Angles;

import tc.vrobot.*;
import tc.shared.lps.*;
import tc.shared.lps.lpo.*;

/**
 * The fusion of the sensors of a robot, as its description ({@link FusionDesc})
 * says, worked out every cycle from what the real sensors read:
 * <ul>
 * <li>the fused sensors (signal level): the nearest sonar and infrared to each,
 *     the sonar, the infrared, the nearer of the two or Flynn's rules;</li>
 * <li>the virtual scanners (signal level): every reduced laser scan, out of the
 *     rays of every laser that reads where its fan looks;</li>
 * <li>the sensors of an area (feature level): the nearest of what falls in their
 *     arc of the range buffer, as it is or weighted by how old it is.</li>
 * </ul>
 */
public class Fusion extends Object
{
	public double[]						virtuals;				// Virtual sensor values
	public boolean[]					virtuals_flg;			// Virtual sensor update flag
	public double[]						groups;					// Group sensor values
	public boolean[]					groups_flg;				// Group sensor flag: it sees something (under its range)
	public boolean						fresh_flg;				// Whether anything the range buffer is filled with was read on this cycle
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

		// until they are first read, they see nothing
		java.util.Arrays.fill (virtuals, fdesc.RANGEVIRTU);
		for (int i = 0; i < fdesc.MAXGROUP; i++)
			groups[i]	= (fdesc.groupfeat[i] != null) ? fdesc.groupfeat[i].range () : fdesc.RANGEGROUP;
	}
		
	/* Instance methods */
	/**
	 * A fused sensor: the nearest sonar and the nearest infrared to it, fused as
	 * its mode says (or the mode of the description, when it says none). Before
	 * anything else, a sensor that reads its maximum range (or beyond) sees
	 * nothing and is not used, nor is one not read on this cycle. Of the ones
	 * left:
	 * <ul>
	 * <li>sonar only: the sonar;</li>
	 * <li>infrared only: the infrared;</li>
	 * <li>the minimum: the nearer of the two;</li>
	 * <li>Flynn's rules: the infrared while it is close enough to be trusted
	 *     (under 70% of its range), and the sonar beyond that.</li>
	 * </ul>
	 * When either of the two is left alone, it is what the rules that can use it
	 * read; when the mode has none to use, the fused sensor reads its maximum
	 * range. It is read on this cycle when any of the two was; when neither was,
	 * it keeps what it read last (it does not jump to its maximum range because a
	 * sensor did not fire).
	 *
	 * The sensors fused are the ones that look the same way as the fused sensor
	 * (the nearest of them, when several do), and each sits where it sits on the
	 * robot: what it reads is the point it hit, taken as the distance from where
	 * the fused sensor sits (see {@link #from}). Whether a sensor sees something
	 * and Flynn's rule go by what the sensor itself reads.
	 */
	protected void virtual (int s, RobotData data, SensorPos a1)
	{
		int				mode = a1.mode ();
		int				ks = nearest (a1, rdesc.sonfeat, rdesc.MAXSONAR);
		int				ki = nearest (a1, rdesc.irfeat, rdesc.MAXIR);
		boolean			s_read = (ks >= 0) && data.sonars_flg[ks];
		boolean			i_read = (ki >= 0) && data.irs_flg[ki];
		double			sonar = s_read ? data.sonars[ks] : 0.0;
		double			ir = i_read ? data.irs[ki] : 0.0;
		boolean			s_ok = s_read && sees (sonar, rdesc.RANGESON);		// what can be used: what sees something
		boolean			i_ok = i_read && sees (ir, rdesc.RANGEIR);
		boolean			near = i_ok && (ir < 0.7 * rdesc.RANGEIR);			// Flynn: an infrared near enough to be trusted
		double			out = fdesc.RANGEVIRTU;

		virtuals_flg[s]	= s_read || i_read;
		if (!virtuals_flg[s])				return;			// nothing new: as it was

		// from where the fused sensor sits
		if (s_ok)		sonar	= from (a1, rdesc.sonfeat[ks], sonar, 0.0);
		if (i_ok)		ir		= from (a1, rdesc.irfeat[ki], ir, 0.0);

		if (mode == FusionDesc.V_UNDEF)		mode = fdesc.MODEVIRTU;
		switch (mode)
		{
			case FusionDesc.V_SONAR:
				if (s_ok)				out = sonar;
				break;
			case FusionDesc.V_IR:
				if (i_ok)				out = ir;
				break;
			case FusionDesc.V_FLYNN:
				if (s_ok && i_ok)		out = near ? ir : sonar;
				else if (s_ok)			out = sonar;
				else if (i_ok)			out = ir;
				break;
			case FusionDesc.V_MIN:
			default:
				if (s_ok && i_ok)		out = Math.min (sonar, ir);
				else if (s_ok)			out = sonar;
				else if (i_ok)			out = ir;
		}

		virtuals[s]		= Math.max (Math.min (out, fdesc.RANGEVIRTU), 0.0);
	}

	/** Whether a reading sees something: it is under the maximum range of its sensor (when the sensor says one). */
	static protected boolean sees (double r, double range)
	{
		return (range <= 0.0) || (r < range);
	}

	/**
	 * The sensor of a family a fused sensor is made of: the one that looks the
	 * most the same way, and the nearest of them when several look alike (within
	 * a degree), -1 when the family has none. Where they sit alone does not tell:
	 * two sensors side by side may look forty degrees apart.
	 */
	static protected int nearest (SensorPos a1, SensorPos[] feats, int n)
	{
		double			bestA = Double.MAX_VALUE, bestD = Double.MAX_VALUE;
		int				k = -1;

		if (feats == null)		return -1;
		for (int i = 0; i < Math.min (n, feats.length); i++)
		{
			if (feats[i] == null)		continue;

			double	a = Math.abs (Angles.radnorm_180 (feats[i].orientation () - a1.orientation ()));
			double	d = a1.distance (feats[i]);

			if ((k < 0) || (a < bestA - LOOK_ALIKE))						{ bestA = a;	bestD = d;	k = i; }	// looks more the same way
			else if ((a <= bestA + LOOK_ALIKE) && (d < bestD))				{ bestA = Math.min (bestA, a);	bestD = d;	k = i; }	// as much, and nearer
		}
		return k;
	}

	/** How far apart two sensors may look and still look the same way (rad). */
	static public final double			LOOK_ALIKE		= Math.toRadians (1.0);

	/**
	 * What a sensor reads, seen from somewhere else on the robot: the distance from
	 * where o sits to the point the sensor s hit, reading r along its own direction
	 * (turned alpha, the ray of a laser). With o and s in the same place it is r.
	 */
	static public double from (SensorPos o, SensorPos s, double r, double alpha)
	{
		double	b = s.orientation () + alpha;
		double	dx = s.x () + r * Math.cos (b) - o.x ();
		double	dy = s.y () + r * Math.sin (b) - o.y ();

		return Math.sqrt (dx * dx + dy * dy);
	}

	/**
	 * A sensor of an area: the nearest of what falls in its arc of the range
	 * buffer, out to its range (range when there is nothing), as it is or, in the
	 * weighted mode, an older reading counting as farther when choosing the
	 * nearest (LPORangeBuffer.buffer_wgt, by how many cycles old it is). It is only
	 * worked out again when anything the range buffer is filled with was read on
	 * this cycle; otherwise it keeps its value, so that it does not jump on the
	 * cycles the sensors do not fire. It sees something when it reads short of its
	 * range.
	 */
	protected void group (int s, LPS lps, RobotData data, FeaturePos f)
	{
		LPORangeBuffer	rbuffer;
		double			out;

		if (!fresh_flg)		return;

		rbuffer	= (LPORangeBuffer) lps.find ("RBuffer");
		out		= rbuffer.occupied_arc (f, f.cone () * 0.5, f.range (), f.mode () == FusionDesc.G_WBUF_ARC);

		groups[s]		= Math.max (Math.min (out, f.range ()), 0.0);
		groups_flg[s]	= (groups[s] < f.range () - EPSILON);
		if (!groups_flg[s])		groups[s] = f.range ();
	}

	/** How much short of its range a sensor of an area must read to see something (m). */
	static public final double			EPSILON			= 1E-3;

	/** Whether anything the range buffer is filled with ({@link #fill}) was read on this cycle. */
	protected boolean fresh (RobotData data)
	{
		if (fdesc.MAXVIRTU > 0)
		{
			for (int i = 0; i < fdesc.MAXVIRTU; i++)		if (virtuals_flg[i])		return true;
		}
		else
		{
			for (int i = 0; i < rdesc.MAXSONAR; i++)		if (data.sonars_flg[i])		return true;
			for (int i = 0; i < rdesc.MAXIR; i++)			if (data.irs_flg[i])		return true;
		}
		if (fdesc.MAXSCAN > 0)
		{
			for (int k = 0; k < fdesc.MAXSCAN; k++)			if (allscans_flg[k])		return true;
		}
		else if ((rdesc.RAYLRF > 0) && (data.lrfs != null) && (data.lrfs_flg != null))
		{
			for (int k = 0; k < Math.min (rdesc.MAXLRF, data.lrfs_flg.length); k++)		if (data.lrfs_flg[k])		return true;
		}
		return false;
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
	 * reads the range of the fan, as one that sees nothing. When the fan and a
	 * laser sit in the same place this is the old reduction of that laser, ray by
	 * ray.
	 *
	 * The rays of the lasers are as the simulator and the perception take them:
	 * from one end of the cone to the other, both included (the rays of the fan as
	 * well), all of them with the rays and the cone of the description (RAYLRF,
	 * CONELRF). The scan is read on this cycle when any of the lasers was.
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
					for (int i = Math.max (0, i0); i <= Math.min (n - 1, i1); i++)		// what falls outside the fan is left out
					{
						if (avg)			acc[i] += d;
						else				acc[i] = Math.min (acc[i], d);
						cnt[i] ++;
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
		fresh_flg	= fresh (data);
		
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