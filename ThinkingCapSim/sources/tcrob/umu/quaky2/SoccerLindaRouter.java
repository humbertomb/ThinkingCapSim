/*
 * Created on 10-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.umu.quaky2;

import tc.coord.*;
import tc.shared.linda.*;
import tc.shared.lps.*;
import tc.shared.lps.lpo.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SoccerLindaRouter extends LindaRouter
{
	protected LPO[]			lpos = new LPO[4];
	
	public SoccerLindaRouter (String robotid, Linda linda, Linda lindaglobal)
	{
		super (robotid, linda, lindaglobal);

		// Register GLOBAL linda listeners
		lindaglobal.register (new Tuple (robotid, Tuple.CAMERA, null), this);
	}

	public void notify (Tuple tuple)
	{
		LPS			lps;
		
		if ((tuple.key == null) || (tuple.value == null))		return;
		
		if (tuple.key.equals (Tuple.LPS))
		{
			if (System.currentTimeMillis () - ltime < timeout)
				return;
			
			ltime	= System.currentTimeMillis ();
			
			// Check for application specific LPOs information		
			lps			= ((ItemLPS) tuple.value).lps;
			lpos[0]		= lps.find ("Ball");
			lpos[1]		= lps.find ("Net1");
			lpos[2]		= lps.find ("Net2");
			lpos[3]		= lps.find ("Align");

			mdata.update (((ItemLPS) tuple.value).lps);
			mitem.set (mdata, lpos, ltime);		
			mtuple.space	= robotid;
			lindaglobal.write (mtuple);
		}			
		else 
			super.notify (tuple);
	}
}
