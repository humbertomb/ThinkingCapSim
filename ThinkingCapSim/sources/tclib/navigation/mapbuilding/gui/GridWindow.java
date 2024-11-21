/*
 * (c) 2004 Humberto Martinez Barbera
 * (c) 2024 Humberto Martinez Barbera
 */

package tclib.navigation.mapbuilding.gui;

import java.awt.*;
import javax.swing.*;

import tclib.navigation.mapbuilding.*;
import tclib.navigation.mapbuilding.visualization.*;
import tclib.navigation.pathplanning.*;

import tc.vrobot.*;

import devices.pos.*;

import wucore.widgets.*;
import wucore.gui.*;

public class GridWindow extends JFrame
{
	protected ChildWindowListener		parent;
	
	
	protected Grid 						grid;
	
	protected Component2D 				gridCO 		= new Component2D ();

	// Interface widgets
	protected Grid2D					mgrid;
	
	private int 						selnode 		= -1;

	public GridWindow (String name, Grid grid, RobotDesc rdesc)
	{
		this (name, grid, rdesc, null);
	}
	
	public GridWindow (String name, Grid grid, RobotDesc rdesc, ChildWindowListener parent)
	{
		this.grid	= grid;
		this.parent	= parent;

		JPanel			panel;

		mgrid = new Grid2D (gridCO.getModel (), rdesc);
		mgrid.drawartifacts (true);

		gridCO.setBackground (mgrid.getMiddleColor ());
		gridCO.setMinimumSize (new Dimension(100, 100));
		
		panel = new JPanel ();
		panel.setLayout (new GridLayout (1, 1));
		panel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Grid Map", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		panel.add (gridCO);

		updateGrid (null, null);
					
		setTitle ("[" + name +"] Grid Map");
		setLocation (new Point(20, 300));
		setSize (new Dimension(300, 300));
		getContentPane().setLayout (new GridLayout (1, 1));
		getContentPane().add (panel);
		setVisible (true);
		
		// event handling
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				close ();
			}
		});
	}
		
	private boolean mShown = false;
  	
	public void addNotify() 
	{
		super.addNotify();
		
		if (mShown)
			return;
			
		// resize frame to account for menubar
		JMenuBar jMenuBar = getJMenuBar();
		if (jMenuBar != null) {
			int jMenuBarHeight = jMenuBar.getPreferredSize().height;
			Dimension dimension = getSize();
			dimension.height += jMenuBarHeight;
			setSize(dimension);
		}
		mShown = true;
	}
		
	public void close ()
	{
		if (parent != null)
			parent.childClosed (this); 

		setVisible (false);
		dispose ();
	}

	public void updateGrid (GridPath path, Position pos)
	{
		mgrid.update (grid, path, pos, Grid2D.NAVIGATION);
		gridCO.repaint ();
	}
 }