/*
 * (c) 2003 Jose Antonio Marin Meseguer
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.utils.graphs.gui;

import java.awt.*;
import javax.swing.*;

import tclib.utils.graphs.*;
import tclib.utils.graphs.visualization.*;

import wucore.widgets.*;
import wucore.gui.*;

public class GraphWindow extends JFrame
{
	private ChildWindowListener		parent		= null;
	protected Component2D 			panel 		= new Component2D ();
	protected Graph2D 				model;

	public GraphWindow ()
	{
		this (null);
	}
	
	public GraphWindow (ChildWindowListener parent)
	{
		this.parent = parent;
		
		model 		= new Graph2D (panel.getModel ());
		
		try { initComponents (); } catch (Exception e) { e.printStackTrace (); }

		setVisible (true);
	}
	
	public void initComponents() throws Exception
	{
		setLocation (new Point(20, 300));
		setSize (new Dimension(400, 314));
		setTitle ("Graph Topology");

		getContentPane().setLayout(new BorderLayout(0, 0));
		getContentPane().add(panel, "Center");

		// event handling
		panel.addComponentListener(new java.awt.event.ComponentAdapter() {
			public void componentResized(java.awt.event.ComponentEvent e) {
				panelComponentResized(e);
			}
		});
		panel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			public void mouseDragged(java.awt.event.MouseEvent e) {
				panelMouseDragged(e);
			}
		});
		panel.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent e) {
				panelMousePressed(e);
			}
		});
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
	
	// Close the window when the close box is clicked
	void thisWindowClosing(java.awt.event.WindowEvent e)
	{
		close ();
	}
	
	public void panelMouseDragged(java.awt.event.MouseEvent e)
	{
		panel.mousePan (e.getX (), e.getY ());
	}
	
	public void panelMousePressed(java.awt.event.MouseEvent e)
	{
		panel.mouseDown (e.getX (), e.getY ());
	}
	
	public void close ()
	{
		if (parent != null)
			parent.childClosed (this); 

		setVisible (false);
		dispose ();
	}
	
	public void panelComponentResized(java.awt.event.ComponentEvent e) 
	{
		panel.setSize (panel.getSize ());
		panel.repaint ();
	}
	
	public void setGraph (Graph graph)
	{
		model.update (graph);
		panel.repaint ();
	}
}