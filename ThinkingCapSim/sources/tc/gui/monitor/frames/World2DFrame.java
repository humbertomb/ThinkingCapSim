/*
 * (c) 1997-2001 Humberto Martinez
 * (c) 2004 Humberto Martinez
 */
 
package tc.gui.monitor.frames;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import tc.shared.world.*;
import tc.vrobot.*;
import tc.coord.*;
import tc.gui.visualization.*;

import wucore.gui.*;
import wucore.widgets.*;

public class World2DFrame extends MonitorFrame
{
	// Attributes for 2D representation
	protected World2D						model;
	protected ChildWindowListener			parent;
	protected Component2D 					map2D = new Component2D();

	private World2DFrame ()
	{
	}

	public World2DFrame (ChildWindowListener parent)
	{
		this.parent = parent;
		
		setResizable(true);
		try
		{
			initComponents ();	
		} catch (Exception e) { e.printStackTrace (); }
	}

	public void initComponents() throws Exception
	{
		// the following code sets the frame's initial state
		map2D.setVisible(true);
		map2D.setLayout(null);
		setLocation(new java.awt.Point(20, 300));
		getContentPane().setLayout(new GridLayout(1, 1));
		getContentPane().add(map2D);

		setSize(new java.awt.Dimension(500, 500));

		// event handling
		map2D.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			public void mouseDragged(java.awt.event.MouseEvent e) {
				map2DMouseDragged(e);
			}
		});		
		map2D.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent e) {
				map2DMousePressed(e);
			}		
			public void mouseReleased(java.awt.event.MouseEvent e) {
				map2DMouseReleased(e);
			}
		});	
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

	public void map2DMousePressed(java.awt.event.MouseEvent e) 
	{
		if (e.isControlDown () || ((e.getModifiers () & MouseEvent.BUTTON2_MASK) != 0))
			map2D.setCursor (Cursor.getPredefinedCursor (Cursor.N_RESIZE_CURSOR));
		else
			map2D.setCursor (Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR));			
		map2D.mouseDown (e.getX (), e.getY ());
	}

	public void map2DMouseDragged(java.awt.event.MouseEvent e) 
	{
		if (e.isControlDown () || ((e.getModifiers () & MouseEvent.BUTTON2_MASK) != 0))
		{
			map2D.setCursor (Cursor.getPredefinedCursor (Cursor.N_RESIZE_CURSOR));
			map2D.mouseZoom (e.getX (), e.getY ());
		}
		else
		{
			map2D.setCursor (Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR));
			map2D.mousePan (e.getX (), e.getY ());
		}
	}

	public void map2DMouseReleased(java.awt.event.MouseEvent e) 
	{
		map2D.setCursor (Cursor.getPredefinedCursor (Cursor.DEFAULT_CURSOR));
	}

	public void open (World map, RobotDesc robot) 
	{			
		setVisible (true);
		
		model		= new World2D (map2D.getModel (), robot, map);
		model.onplace (true);		
		model.drawartifacts (true);
		model.drawlpos (true);
		model.autoscale (map == null);
		map2D.repaint ();
	}

	public void close ()
	{
		if (parent != null)
			parent.childClosed (this); 

		setVisible (false);
	}

	public void updateData (RobotData data)
	{
		if (model != null)
		{
			model.update (data);			
			map2D.repaint ();
		}
	}	
	
	public void updateData (String id, RobotList robots)
	{
		if (model != null)
		{
			model.update (id, robots);			
			map2D.repaint ();
		}
	}	
}
