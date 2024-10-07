/*
 * (c) 2004 Humberto Martinez Barbera
 */

package tclib.planning.htopol.gui;

import java.awt.*;

import javax.swing.*;

import tclib.utils.graphs.visualization.*;
import tclib.navigation.mapbuilding.visualization.*;
import tclib.planning.htopol.*;
import tclib.planning.htopol.visualization.*;

import tc.vrobot.*;

import devices.pos.*;
import wucore.utils.geom.Point2;
import wucore.widgets.*;
import wucore.gui.*;

public class HTopolWindow extends JFrame implements Runnable
{
	protected ChildWindowListener		parent;
	protected HTopolMap				map;
	
	protected JSplitPane				rootSP		= new JSplitPane ();

	protected JPanel					firstPA		= new JPanel ();
	protected JPanel					secondPA		= new JPanel ();
	protected JPanel					gridPA		= new JPanel ();
	
	protected JPanel					toolsPA		= new JPanel ();
	protected JPanel					topolPA		= new JPanel ();
	protected JSplitPane				graphsSP		= new JSplitPane ();
	protected JComboBox				zoneCB		= new JComboBox ();
	protected JCheckBox				wgtsCB		= new JCheckBox ("Draw arc weights");
	protected JCheckBox				relaxCB		= new JCheckBox ("Relax graphs");
	protected JCheckBox				gridaCB		= new JCheckBox ("Draw grid artifacts");
	
	protected Component2D 			firstCO 		= new Component2D ();
	protected Component2D 			secondCO 	= new Component2D ();
	protected Component2D 			gridCO 		= new Component2D ();

	// Interface widgets
	protected Graph2D				mfst;
	protected HTopol2D				msnd;
	protected Grid2D					mgrid;
	
	private boolean					relaxing;
	private int 						selnode 		= -1;

	public HTopolWindow (String name, HTopolMap map, RobotDesc rdesc)
	{
		this (name, map, rdesc, null);
	}
	
	public HTopolWindow (String name, HTopolMap map, RobotDesc rdesc, ChildWindowListener parent)
	{
		int			i;
		
		this.map		= map;
		this.parent	= parent;
				
		// Initialise widgets		
		mfst 		= new Graph2D (firstCO.getModel ());
		msnd 		= new HTopol2D (secondCO.getModel ());
		mgrid 		= new Grid2D (gridCO.getModel (), rdesc);
		gridCO.setBackground (mgrid.getMiddleColor ());

		for (i = 0; i < map.numNodes (); i++)
			zoneCB.addItem (((GNodeFL) map.getNode (i)).getLabel ());

		updateRoot ();
		updateZone (0);
		updateGrid (null);
			
		try { initComponents (); } catch (Exception e) { e.printStackTrace (); }
		
		setTitle ("[" + name +"] Hierarchical Topological Map");
		setVisible (true);
	}
	
	public void initComponents() throws Exception
	{
		setLocation (new Point(20, 300));
		setSize (new Dimension(550, 450));

		zoneCB.setMinimumSize (new Dimension(150, 30));
		firstCO.setMinimumSize (new Dimension(100, 100));
		secondCO.setMinimumSize (new Dimension(100, 100));
		gridCO.setMinimumSize (new Dimension(100, 100));

		firstPA.setLayout (new GridLayout (1, 1));
		firstPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "First Level Graph", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		firstPA.add (firstCO);
		
		secondPA.setLayout (new GridLayout (1, 1));
		secondPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Second Level Graph", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		secondPA.add (secondCO);

		gridPA.setLayout (new GridLayout (1, 1));
		gridPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Grid Map", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		gridPA.add (gridCO);

		wgtsCB.setSelected (true);
		toolsPA.setLayout (new GridLayout (2, 2));
		toolsPA.add (zoneCB);
		toolsPA.add (wgtsCB);
		toolsPA.add (relaxCB);
		toolsPA.add (gridaCB);

		graphsSP.setOrientation(JSplitPane.VERTICAL_SPLIT);
		graphsSP.setOneTouchExpandable(true);
		graphsSP.setTopComponent(firstPA);
		graphsSP.setBottomComponent(secondPA);
		graphsSP.setDividerLocation(0.5);

		topolPA.setLayout (new BorderLayout(0, 0));
		topolPA.add (toolsPA, "North");
		topolPA.add (graphsSP, "Center");
		
		rootSP.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		rootSP.setOneTouchExpandable(true);
		rootSP.setLeftComponent(topolPA);
		rootSP.setRightComponent(gridPA);
		rootSP.setDividerLocation(0.5);

		getContentPane().setLayout (new GridLayout (1, 1));
		getContentPane().add (rootSP);

		// event handling
		firstCO.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			public void mouseDragged(java.awt.event.MouseEvent e) {
				firstCOMouseDragged(e);
			}
		});
		firstCO.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent e) {
				firstCOMousePressed(e);
			}
			public void mouseReleased(java.awt.event.MouseEvent e) {
				firstCOMouseReleased(e);
			}
		});
		secondCO.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			public void mouseDragged(java.awt.event.MouseEvent e) {
				secondCOMouseDragged(e);
			}
		});
		secondCO.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent e) {
				secondCOMousePressed(e);
			}
			public void mouseReleased(java.awt.event.MouseEvent e) {
				secondCOMouseReleased(e);
			}
		});
		zoneCB.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				zoneCBActionPerformed(e);
			}
		});		
		wgtsCB.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				wgtsCBActionPerformed(e);
			}
		});		
		relaxCB.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				relaxCBActionPerformed(e);
			}
		});		
		gridaCB.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				gridaCBActionPerformed(e);
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
	public void zoneCBActionPerformed(java.awt.event.ActionEvent e) 
	{
		updateZone (zoneCB.getSelectedIndex ());
		updateGrid (null);
	}

	public void wgtsCBActionPerformed(java.awt.event.ActionEvent e) 
	{
		mfst.drawWeights (wgtsCB.isSelected ());
		msnd.drawWeights (wgtsCB.isSelected ());
		
		mfst.drawGraph ();
		msnd.drawGraph ();

		firstCO.repaint ();
		secondCO.repaint ();
	}

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

	public void gridaCBActionPerformed(java.awt.event.ActionEvent e) 
	{
		mgrid.drawartifacts (gridaCB.isSelected ());
	}

	// Close the window when the close box is clicked
	protected void thisWindowClosing(java.awt.event.WindowEvent e)
	{
		close ();
	}
	
	protected void firstCOMouseDragged(java.awt.event.MouseEvent e)
	{
		Point2		pt;
		
		pt		= firstCO.mouseClick (e.getX (), e.getY ());
		mfst.moveNode (selnode, pt.x (), pt.y ());
		mfst.drawGraph ();
		firstCO.repaint ();
	}
	
	protected void firstCOMousePressed(java.awt.event.MouseEvent e)
	{
		Point2		pt;
		
		pt		= firstCO.mouseClick (e.getX (), e.getY ());
		selnode	= mfst.findNode (pt.x (), pt.y ());
		mfst.fixNode (selnode, true);
	}

	protected void firstCOMouseReleased(java.awt.event.MouseEvent e) 
	{
		mfst.fixNode (selnode, false);
		mfst.drawGraph ();
		firstCO.repaint ();
	}

	protected void secondCOMouseDragged(java.awt.event.MouseEvent e)
	{
		Point2		pt;
		
		pt		= secondCO.mouseClick (e.getX (), e.getY ());
		msnd.moveNode (selnode, pt.x (), pt.y ());
		msnd.drawGraph ();
		secondCO.repaint ();
	}
	
	protected void secondCOMousePressed(java.awt.event.MouseEvent e)
	{
		Point2		pt;
		
		pt		= secondCO.mouseClick (e.getX (), e.getY ());
		selnode	= msnd.findNode (pt.x (), pt.y ());
		msnd.fixNode (selnode, true);
	}

	protected void secondCOMouseReleased(java.awt.event.MouseEvent e) 
	{
		msnd.fixNode (selnode, false);
		msnd.drawGraph ();
		secondCO.repaint ();
	}

	public void close ()
	{
		if (parent != null)
			parent.childClosed (this); 

		setVisible (false);
		dispose ();
	}

	public void changeZone (String zone)
	{
		zoneCB.setSelectedItem (zone);
	}
	

	public void updateZone (int index)
	{
		GNodeFL			node;
		
		node = (GNodeFL) map.getNode (index);
		msnd.update (node.getGraph (), (String) zoneCB.getSelectedItem (), node.isRealized ());
		secondCO.repaint ();
	}
	
	public void updateRoot ()
	{
		mfst.update (map);
		firstCO.repaint ();
	}

	public void updateGrid (Position pos)
	{
		GNodeFL			node;
		
		node = (GNodeFL) map.getNode (zoneCB.getSelectedIndex ());
		mgrid.update (node.getGrid (), node.getPath (), pos, Grid2D.NAVIGATION);
		gridCO.repaint ();
	}
	
    public void run ()
    {
    		while (relaxing) 
    		{
    			mfst.relax ();
   			msnd.relax ();
   			
   			firstCO.repaint ();
   			secondCO.repaint ();

   			try { Thread.sleep (300); } catch (Exception e) { }
    		}
    	}
 }