/*
 * (c) 2004 Humberto Martinez
 */

package tc.gui.monitor.frames;

import javax.swing.*;

import wucore.widgets.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RemoteMonitorFrame extends MonitorFrame
{
	// GUI components
	protected JComponent			jlink;
	protected Component2D			component;
	protected Model2D			model;
		
	// Constructors
	public RemoteMonitorFrame ()
	{
		super ();
	}
			
	public final JComponent		getFrameLink ()					{ return jlink; }
	public final void			setFrameLink (JComponent jlink)	{ this.jlink = jlink; }
	
	public final Component2D		getComponent ()					{ return component; }
	public final void			setComponent (Component2D comp)	{ component = comp; }
	
	public final Model2D			getModel ()						{ return model; }
	public final void			setModel (Model2D model)			{ this.model = model; }
	
	// Instance method
	public void close ()
	{
		setVisible (false);
		dispose ();
	}
}
