/*
 * (c) 2002 Humberto Martinez
 */
 
package tc.gui;

import java.util.*;

import tc.vrobot.*;
import tclib.utils.fusion.*;
import tc.modules.*;
import tc.shared.lps.lpo.*;
import tc.shared.linda.*;

import devices.pos.*;

public interface GUIMonitor
{
	// Configuration methods
	public void			changeConfiguration (String id, RobotDesc rdesc, FusionDesc fdesc);
	public void			deleteConfiguration (String id);
	public void			close ();
	
	// Robot monitoring methods
	public void			updateWorldMap (String id, Properties worldprops);
	public void 		updateRobot (String id, MonitorData data, LPO[] lpos);
	public void 		updateGoal (String id, Position goal);
	public void 		updateStatus (String id, long tstamp, int type, String msg);
	public void			updateBehInfo (String id, ItemBehInfo item);
}
