/*
 * Created on 25-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.umu.indoor.gui;

import java.awt.*;
import javax.swing.*;

import tc.shared.lps.*;
import tc.gui.visualization.*;

import wucore.gui.*;
import devices.pos.*;
import wucore.widgets.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class IndoorLPSWindow extends JFrame
{
	protected ChildWindowListener		parent;
	
	protected Component2D 			lpsCO 		= new Component2D ();
	
	// Interface widgets
	protected LPS2D					mlps;

	public IndoorLPSWindow (String name)
	{
		this (name, null);
	}
	
	public IndoorLPSWindow (String name, ChildWindowListener parent)
	{				
		// Initialise widgets
		mlps 		= new LPS2D (lpsCO.getModel ());
			
		try { initComponents (); } catch (Exception e) { e.printStackTrace (); }
		
		setTitle ("[" + name +"] Local Perceptual Space");
		setVisible (true);
	}
	
	public void initComponents() throws Exception
	{
		setLocation (new Point(50, 50));
		setSize (new Dimension(300, 300));

		getContentPane().setLayout (new GridLayout (1, 1));
		getContentPane().add (lpsCO);

		// event handling
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				thisWindowClosing(e);
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
	
	//*****************
	// Event handling
	//*****************
	// Close the window when the close box is clicked
	protected void thisWindowClosing(java.awt.event.WindowEvent e)
	{
		close ();
	}
	
	public void close ()
	{
		if (parent != null)
			parent.childClosed (this); 

		setVisible (false);
		dispose ();
	}

	public void update (LPS lps, Path path)
	{
		mlps.update (lps, path);
		lpsCO.repaint ();
	}
}
