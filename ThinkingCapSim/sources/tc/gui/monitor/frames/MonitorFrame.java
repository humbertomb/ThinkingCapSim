/*
 * Created on 04-may-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.gui.monitor.frames;

import java.util.*;
import javax.swing.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class MonitorFrame extends JFrame
{
	static public final int			DEF_LOCX		= 50;
	static public final int			DEF_LOCY		= 100;
	static public final int			DEF_WIDTH	= 260;
	static public final int			DEF_HEIGHT	= 280;
	
	protected String					identifier;

	// Constructors
	public MonitorFrame ()
	{
		super ();
	}
	
	// Accessors
	public final String			getIdentifier ()				{ return identifier; }
	public final void			setIdentifier (String id)	{ identifier = id; }
	
	// Subclasses MUST implement
	public abstract void			close ();
	
	// Instance methods
	public void configure (String preffix, Properties config)
	{
		int					xpos, ypos;
		int					width, height;
		String				aux;
		StringTokenizer		st;

		// Get the frame location from the configuration file
		if (config.containsKey(preffix + "_POS"))
		{
			aux		= config.getProperty(preffix + "_POS");
			st		= new StringTokenizer(aux,", \t");
			xpos		= Integer.parseInt(st.nextToken());
			ypos		= Integer.parseInt(st.nextToken());
			setLocation(new java.awt.Point(xpos, ypos));	
		}
		else
			setLocation(new java.awt.Point(DEF_LOCX, DEF_LOCY));
		
		// Get the frame size from the configuration file
		if (config.containsKey(preffix + "_SIZE"))
		{
			aux		= config.getProperty (preffix + "_SIZE");
			st		= new StringTokenizer(aux,", \t");
			width	= Integer.parseInt(st.nextToken());
			height	= Integer.parseInt(st.nextToken());
			setSize(new java.awt.Dimension(width, height));	
		}
		else
			setSize(new java.awt.Dimension(DEF_WIDTH, DEF_HEIGHT));
	}
}
