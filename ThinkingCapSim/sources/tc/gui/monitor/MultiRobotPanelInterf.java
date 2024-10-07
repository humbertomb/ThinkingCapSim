/*
 * Created on 10-ago-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tc.gui.monitor;

import java.util.Properties;
import javax.swing.JMenu;
import javax.swing.JPanel;
import tc.coord.RobotList;
import tc.gui.GUIMonitor;
import tc.modules.Monitor;
import tc.shared.world.World;

/**
 * @author SergioPC
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class MultiRobotPanelInterf extends JPanel implements GUIMonitor{
	public abstract Monitor 	getMonitor ();
	public abstract RobotList	getRobotList ();
	public abstract World 		getWorldMap ();
	public abstract String 		getTitle();
	public abstract void 		setTitle(String title);
	public abstract void		setMonitorMenu (JMenu monitorMI);
	public abstract void		setMonitor (Monitor monitor);
	public abstract void 		open (Properties prop);
	public abstract void 		close();
}