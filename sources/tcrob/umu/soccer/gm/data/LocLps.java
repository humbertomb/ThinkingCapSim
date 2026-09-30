/*
 * Created on 16-jun-2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.umu.soccer.gm.data;

import tc.shared.lps.*;
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
	
	public void updateFromLps (LPS lps)
	{
		LPOBall			ball;
		LPONet			net1;
		LPONet			net2;
		LPOLandmark		landmark1;
		LPOLandmark		landmark2;

		ball		= (LPOBall) lps.find ("Ball");
		net1		= (LPONet) lps.find ("Net1");
		net2		= (LPONet) lps.find ("Net2");
		landmark1	= (LPOLandmark) lps.find ("Landmark1");
		landmark2	= (LPOLandmark) lps.find ("Landmark2");

	}
}
