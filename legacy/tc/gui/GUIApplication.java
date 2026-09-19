/*
 * Created on 09-jun-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.gui;

import tc.modules.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface GUIApplication 
{
	// State and flow control methods
	public void			start ();
	public boolean		isReady ();
	
	// Data processing
	public void			setMonitor (Monitor monitor);
	public GUIMonitor	getGUIMonitor ();
}
