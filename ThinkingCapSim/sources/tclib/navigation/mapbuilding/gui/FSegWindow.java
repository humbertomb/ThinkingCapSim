/*
 * (c) 2004 Humberto Martinez Barbera
 * (c) 2024 Humberto Martinez Barbera
 */

package tclib.navigation.mapbuilding.gui;

import java.awt.*;
import javax.swing.*;

import tclib.navigation.mapbuilding.*;
import tclib.navigation.mapbuilding.visualization.*;

import tc.vrobot.*;

import devices.pos.*;

import wucore.widgets.*;
import wucore.gui.*;

public class FSegWindow extends JFrame
{
	protected ChildWindowListener		parent;	
	
	protected FSegMap 					fmap;
	
	protected Component2D 				fmapCO 		= new Component2D ();
	protected FMap2D					mfmap;
	
	public FSegWindow (String name, FSegMap grid, RobotDesc rdesc)
	{
		this (name, grid, rdesc, null);
	}
	
	public FSegWindow (String name, FSegMap grid, RobotDesc rdesc, ChildWindowListener parent)
	{
		this.fmap	= grid;
		this.parent	= parent;

		JPanel			panel;

		mfmap = new FMap2D (fmapCO.getModel (), fmapCO, rdesc, null);
		mfmap.drawartifacts (true);

//		gridCO.setBackground (mgrid.getMiddleColor ());
		fmapCO.setMinimumSize (new Dimension(100, 100));
		
		panel = new JPanel ();
		panel.setLayout (new GridLayout (1, 1));
		panel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Fuzzy Segments Map", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		panel.add (fmapCO);

		updateMap (null, null);
					
		setTitle ("[" + name +"] Fuzzy Segments Map");
		setLocation (new Point(50, 500));
		setSize (new Dimension(600, 800));
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

	public void updateMap (Paths paths, Position pos)
	{
		mfmap.update (fmap, paths, pos);
		fmapCO.repaint ();
	}
 }