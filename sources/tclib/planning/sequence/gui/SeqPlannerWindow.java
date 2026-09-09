/*
 * Created on 07-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tclib.planning.sequence.gui;

import java.awt.*;
import javax.swing.*;

import wucore.gui.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SeqPlannerWindow extends JFrame
{
	protected ChildWindowListener		parent;
	
	protected JSplitPane				rootSP		= new JSplitPane ();

	protected JEditorPane				planTA		= new JEditorPane ();
	protected JScrollPane				planPA		= new JScrollPane (planTA);
		
	protected JEditorPane				locksTA		= new JEditorPane ();
	protected JScrollPane				locksPA		= new JScrollPane (locksTA);
	
	protected JPanel					leftPA		= new JPanel ();
	protected JSplitPane				planSP		= new JSplitPane ();
	
	public SeqPlannerWindow (String name)
	{
		this (name, null);
	}
	
	public SeqPlannerWindow (String name, ChildWindowListener parent)
	{				
		try { initComponents (); } catch (Exception e) { e.printStackTrace (); }
		
		setTitle ("[" + name +"] Planning and Coordination Status");
		setVisible (true);
	}
	
	public void initComponents() throws Exception
	{
		setLocation (new Point(500, 300));
		setSize (new Dimension(500, 450));

		planTA.setMinimumSize (new Dimension(100, 300));
		locksTA.setMinimumSize (new Dimension(100, 100));

		planPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Current Task Set (Plan)", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));	
		planTA.setContentType ("text/html");
		planTA.setEditable (false);
		
		locksPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Locked Resources", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));	
		locksTA.setContentType ("text/html");
		locksTA.setEditable (false);
		

		planSP.setOrientation(JSplitPane.VERTICAL_SPLIT);
		planSP.setOneTouchExpandable(true);
		planSP.setTopComponent(locksPA);
		planSP.setBottomComponent(new JPanel ());
		planSP.setDividerLocation(125);

		leftPA.setLayout (new BorderLayout(0, 0));
		leftPA.add (planSP, "Center");
		
		rootSP.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		rootSP.setOneTouchExpandable(true);
		rootSP.setLeftComponent(leftPA);
		rootSP.setRightComponent(planPA);
		rootSP.setDividerLocation(0.5);

		getContentPane().setLayout (new GridLayout (1, 1));
		getContentPane().add (rootSP);

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

	public void updatePlan (String plan)
	{
		planTA.setText (plan);
	}
	
	public void updateLocks (String locks)
	{
		locksTA.setText (locks);
	}
}
