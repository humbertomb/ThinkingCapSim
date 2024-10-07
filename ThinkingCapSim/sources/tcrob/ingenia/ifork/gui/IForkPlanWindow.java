/*
 * Created on 07-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.ingenia.ifork.gui;

import java.awt.*;
import javax.swing.*;

import tclib.utils.petrinets.*;
import tclib.utils.petrinets.visualization.*;

import wucore.gui.*;
import wucore.widgets.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class IForkPlanWindow extends JFrame implements Runnable
{
	protected ChildWindowListener		parent;
	
	protected JSplitPane				rootSP		= new JSplitPane ();

	protected JEditorPane				planTA		= new JEditorPane ();
	protected JScrollPane				planPA		= new JScrollPane (planTA);
	
	protected JPanel					pnetPA		= new JPanel ();
	protected Component2D 				pnetCO 		= new Component2D ();
	
	protected JEditorPane				locksTA		= new JEditorPane ();
	protected JScrollPane				locksPA		= new JScrollPane (locksTA);
	
	protected JPanel					toolsPA		= new JPanel ();
	protected JPanel					leftPA		= new JPanel ();
	protected JSplitPane				planSP		= new JSplitPane ();
	protected JCheckBox					relaxCB		= new JCheckBox ("Relax graph");
	protected JButton					saveCB		= new JButton ("Save net");
	
	// Interface widgets
	protected PetriNet2D				mpnet;
	private PetriNet					pnet;
	private boolean						relaxing;

	public IForkPlanWindow (String name)
	{
		this (name, null);
	}
	
	public IForkPlanWindow (String name, ChildWindowListener parent)
	{				
		// Initialise widgets
		mpnet 		= new PetriNet2D (pnetCO.getModel ());
			
		try { initComponents (); } catch (Exception e) { e.printStackTrace (); }
		
		setTitle ("[" + name +"] Planning and Coordination Status");
		setVisible (true);
	}
	
	public void initComponents() throws Exception
	{
		setLocation (new Point(500, 300));
		setSize (new Dimension(500, 450));

		planTA.setMinimumSize (new Dimension(100, 300));
		pnetCO.setMinimumSize (new Dimension(150, 100));
		locksTA.setMinimumSize (new Dimension(100, 100));

		planPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Current Task Set (Plan)", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));	
		planTA.setContentType ("text/html");
		planTA.setEditable (false);
		
		locksPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Locked Resources", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));	
		locksTA.setContentType ("text/html");
		locksTA.setEditable (false);
		
		pnetPA.setLayout (new GridLayout (1, 1));
		pnetPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Petri Net", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		pnetPA.add (pnetCO);

		relaxCB.setSelected (false);
		toolsPA.setLayout (new GridLayout (2, 1));
		toolsPA.add (relaxCB);
		toolsPA.add (saveCB);

		planSP.setOrientation(JSplitPane.VERTICAL_SPLIT);
		planSP.setOneTouchExpandable(true);
		planSP.setTopComponent(locksPA);
		planSP.setBottomComponent(pnetPA);
		planSP.setDividerLocation(125);

		leftPA.setLayout (new BorderLayout(0, 0));
		leftPA.add (toolsPA, "North");
		leftPA.add (planSP, "Center");
		
		rootSP.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		rootSP.setOneTouchExpandable(true);
		rootSP.setLeftComponent(leftPA);
		rootSP.setRightComponent(planPA);
		rootSP.setDividerLocation(0.5);

		getContentPane().setLayout (new GridLayout (1, 1));
		getContentPane().add (rootSP);

		// event handling
		relaxCB.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				relaxCBActionPerformed(e);
			}
		});		
		saveCB.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				saveCBActionPerformed(e);
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
	
	//*****************
	// Event handling
	//*****************
	public void relaxCBActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (relaxCB.isSelected ())
		{
			Thread		relaxer;
			
			relaxing		= true;
			relaxer 		= new Thread (this);
			relaxer.start();
		 }
		else
			relaxing		= false;
	}

	public void saveCBActionPerformed(java.awt.event.ActionEvent e) 
	{
		int				val;
		JFileChooser		chooser;
		
		chooser	= new JFileChooser ();
		val 		= chooser.showSaveDialog (this);
		if (val == JFileChooser.APPROVE_OPTION)
			pnet.toFile(chooser.getSelectedFile().getPath ());
	}
	
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

	public void updatePlan (String plan, PetriNet pnet)
	{
		this.pnet = pnet;
		
		planTA.setText (plan);
		mpnet.update (pnet);
		pnetCO.repaint ();
	}
	
	public void updateLocks (String locks)
	{
		locksTA.setText (locks);
	}
	
    public void run ()
    {
    		while (relaxing) 
    		{
   			mpnet.relax (pnet);
    			pnetCO.repaint ();

   			try { Thread.sleep (300); } catch (Exception e) { }
    		}
    	}
}
