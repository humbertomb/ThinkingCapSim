/*
 * Created on 10-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.umu.quaky2;

import tc.modules.*;
import tc.shared.linda.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SoccerLindaRouter extends LindaRouter
{
	public SoccerLindaRouter (String robotid, Linda linda, Linda lindaglobal)
	{
		super (robotid, linda, lindaglobal);

		// Register GLOBAL linda listeners
		lindaglobal.register (new Tuple (robotid, Tuple.CAMERA_CTRL, null), this);
	}
}
