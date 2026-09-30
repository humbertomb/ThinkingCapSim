/*
 * Created on 16-jun-2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.umu.soccer.gm.data;

import tc.shared.lps.*;
import tc.shared.lps.lpo.*;
import tcrob.umu.soccer.lpo.*;

public class LocLps
{
	public final static int			INIT_LMS	= 1;
	public final static int			NUM_LMS		= 2;
	public final static int			INIT_NETS	= 3;
	public final static int			NUM_NETS	= 2;
	public final static int			LPS_SIZE	= (1 + NUM_LMS + NUM_NETS);

	static public final int[]		TYPE		= { LocLpo.BALL, LocLpo.LANDMARK, LocLpo.LANDMARK, LocLpo.NET, LocLpo.NET };

	protected LocLpo 				lpo[];			// the standard LPS objects


	
	public LocLps ()
	{
		lpo = new LocLpo[LPS_SIZE];
		for (int i = 0; i < lpo.length; i++)
			lpo[i] = new LocLpo (i, TYPE[i]);
	}
	
	public LocLpo getLpo (int index)			{ return lpo[index]; }
	
	/**
	 * The reduced LPS of the localisation methods from the LPS of the robot: the
	 * ball, the two landmarks and the two nets the vision keeps there (Ball,
	 * Landmark1, Landmark2, Net1, Net2), each as far (mm) and as turned (rad, to
	 * the left of the heading of the robot) as the LPS has it now, and as sure
	 * (anchored, 0 to 1). The methods take an object in only when it has been
	 * seen again since they last did (last_anchored grows): that is when the
	 * vision has placed it again where it saw it (its sightings went up; the
	 * anchor and the ageing cannot tell, as the LPS ages every object before it
	 * hands itself over and keeps an anchoring whole for a while). An object the
	 * LPS does not have, or has never seen, is not anchored at all.
	 */
	public void updateFromLps (LPS lps)
	{
		LPO				ball;
		LPO				net1;
		LPO				net2;
		LPO				landmark1;
		LPO				landmark2;

		if (lps == null)		return;
		synchronized (lps)
		{
			ball		= lps.find ("Ball");
			net1		= lps.find ("Net1");
			net2		= lps.find ("Net2");
			landmark1	= lps.find ("Landmark1");
			landmark2	= lps.find ("Landmark2");

			clock++;
			update (0, (ball instanceof LPOBall) ? ball : null);
			update (INIT_LMS, (landmark1 instanceof LPOLandmark) ? landmark1 : null);
			update (INIT_LMS + 1, (landmark2 instanceof LPOLandmark) ? landmark2 : null);
			update (INIT_NETS, (net1 instanceof LPONet) ? net1 : null);			// a zone of the world called Net1 is not the net seen
			update (INIT_NETS + 1, (net2 instanceof LPONet) ? net2 : null);
		}
	}

	/* The count of updates (what last_anchored is), and the sightings of each object when last taken in */
	protected int					clock;
	protected int[]					sightings	= new int[LPS_SIZE];

	/** One object of the reduced LPS from the LPO of the LPS that stands for it (null: none). */
	protected void update (int i, LPO o)
	{
		LocLpo			l = lpo[i];

		if ((o == null) || !o.active () || !o.anchored ())
		{
			l.anchored	= 0.0;
			return;
		}

		l.rho		= o.rho () * 1000.0;									// the LPS is in m, the methods in mm
		l.theta		= o.theta ();
		l.anchored	= o.anchor ();
		if (o.sightings () != sightings[i])									// seen again since it was last taken in
		{
			l.last_anchored	= clock;
			sightings[i]	= o.sightings ();
		}
	}
}
