/*
 * (c) 2002 Juan Pedro Canovas, Humberto Martinez, Bernardo Canovas
 * (c) 2003-2004 Humberto Martinez
 */
 
package tc.gui.monitor;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import tc.vrobot.*;
import tc.modules.*;
import tc.coord.*;
import tc.shared.linda.*;
import tc.shared.lps.lpo.*;
import tc.shared.world.*;
import tc.gui.visualization.*;

import tclib.planning.sequence.Sequence;
import tclib.utils.fusion.*;

import devices.pos.*;
import wucore.widgets.*;
import wucore.utils.geom.*;

public class MultiRobotPanelClient extends MultiRobotPanelInterf //implements GUIMonitor
{
	// Global constants
	static public final int 				MAX_ROBOTS 	= 10;		// Maximum number of monitored robots
	static public final String 			TITLE 		= "Robot Group Monitor";
	
	static public final int 				LTIMEOUT 	= 2000;
	static public final int				DTIMEOUT		= 300;
		
	// Robot and environment attributes
	private Monitor						monitor		= null;	
	protected RobotList					robots		= null;
	protected EventList					events		= null;
	protected World 						worldmap		= null;
	protected World2D					map			= null;

	// member declarations
	protected JTable 					robotTB		= new JTable();
	protected JScrollPane 				robotSP;
	protected JTable 					eventTB		= new JTable();
	protected JScrollPane 				eventSP;
	protected JButton 					startBUT 	= new JButton();
	protected JButton 					stopBUT 		= new JButton();
	protected JButton 					stepBUT 		= new JButton();
	protected JButton 					resetBUT 	= new JButton();
	protected JButton 					delBUT 		= new JButton();
	protected JButton 					dumpBUT 		= new JButton();	
	protected Component2D 				map2D 		= new Component2D();
	
	// Main panels
	protected JMenu						menu;		// Parent menu
	protected JSplitPane					rootSP		= new JSplitPane ();
	protected JPanel						topPA		= new JPanel();
	protected JPanel 					mapPA		= new JPanel();
	protected JPanel 					rightPA		= new JPanel();
	protected JTabbedPane					tableTP 		= new JTabbedPane (SwingConstants.LEFT);
	
	// Right side panels and subpanels (tabs)
	protected JTabbedPane					optsTP 		= new JTabbedPane (SwingConstants.TOP);
	protected JPanel 					robotPA		= new JPanel();
	protected JPanel 					execPA		= new JPanel();
	protected JPanel 					taskPA		= new JPanel();
	protected JPanel 					statePA		= new JPanel();
	protected JPanel 					monitorPA	= new JPanel();
	
	// State and task panels
	protected JPanel 					cursorPA		= new JPanel();
	protected JPanel 					lindaPA		= new JPanel();
	protected JPanel 					planPA		= new JPanel();
	protected JPanel 					textPA		= new JPanel();
	
	// Components
	protected JComboBox					gotoCB		= new JComboBox ();	
	protected JComboBox 					robotsCB 	= new JComboBox ();
	protected JComboBox					actionCB		= new JComboBox ();
	protected JTextArea					textplan		= new JTextArea ();
	protected JButton 					sendBUT 		= new JButton();
	protected JButton 					addgBUT	 	= new JButton();
	protected JButton 					addaBUT	 	= new JButton();
	protected JButton 					clearBUT 	= new JButton();
	protected JLabel						cursorLA		= new JLabel ();

	protected JSlider 					ltimeoutSL	= new JSlider();
	protected JRadioButton 				lallRB 		= new JRadioButton();
	protected JRadioButton 				lactualRB	= new JRadioButton();
	
	// Robot dependent GUI variables
	protected Hashtable					rtable		= new Hashtable (MAX_ROBOTS);
	protected Properties					config;
	
	// Internal variables
	public String 						title 		= TITLE;
	private boolean						updating		= false;
	private boolean						gotoadded	= false; 

	// Constructors
	public MultiRobotPanelClient()
	{
		robots		= new RobotList (MAX_ROBOTS);
		events		= new EventList ();
	}

	// Static methods
	static public String getPosString (double pos, int dig)
	{
		String			str;
		
		str		= Double.toString (Math.round (pos * 1000.0) * 0.001);
		while (str.length () - str.indexOf ('.') > dig + 1)
			str		= str.substring (0, str.length () - 1);
		while (str.length () - str.indexOf ('.') < dig + 1)
			str		= str + "0";
		
		return str;
	}

	// Instance methods
	protected void initComponents() throws Exception 
	{
		// Initialise standard GUI components
		mapPA.setVisible(true);
		mapPA.setLayout(new java.awt.GridLayout());
		mapPA.setMinimumSize (new Dimension (100, 100));

		map2D.setVisible(true);
		map2D.setLayout(null);
		map2D.setBackground(java.awt.Color.white);

		mapPA.add(map2D);
		mapPA.setBorder (BorderFactory.createLineBorder (Color.black));

		startBUT.setVisible(true);
		startBUT.setText("Start");
		stopBUT.setVisible(true);
		stopBUT.setText("Stop");
		stepBUT.setVisible(false);
		stepBUT.setText("Step");
		resetBUT.setVisible(false);
		resetBUT.setText("Reset");
		delBUT.setVisible(false);
		delBUT.setText("Del robot");
		dumpBUT.setVisible(false);
		dumpBUT.setText("Dump Linda");

		execPA.setVisible (true);
		execPA.setLayout (new GridLayout(2,3));
		execPA.add (startBUT);
		execPA.add (stopBUT);
		execPA.add (stepBUT);
		execPA.add (resetBUT);
		execPA.add (delBUT);
		execPA.add (dumpBUT);
		execPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Execution control", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		sendBUT.setVisible(true);
		sendBUT.setSize(new java.awt.Dimension(50, 30));
		sendBUT.setText("Send");		
		addgBUT.setVisible(true);
		addgBUT.setSize(new java.awt.Dimension(50, 30));
		addgBUT.setText("Add");	
		addaBUT.setVisible(true);
		addaBUT.setSize(new java.awt.Dimension(50, 30));
		addaBUT.setText("Add");	
		clearBUT.setVisible(true);
		clearBUT.setPreferredSize(new java.awt.Dimension(50, 30));
		clearBUT.setText("Clear");			
		actionCB.addItem ("load");
		actionCB.addItem ("unload");
		actionCB.addItem ("goto");
		actionCB.addItem ("stay");
		
		planPA.setVisible (true);
		planPA.setLayout (new GridLayout(3,3));
		planPA.add (new JLabel ("Go to:"));
		planPA.add (gotoCB);
		planPA.add (addgBUT);
		planPA.add (new JLabel ("Action:"));
		planPA.add (actionCB);
		planPA.add (addaBUT);
		planPA.add (new JLabel ());
		planPA.add (new JLabel ());
		planPA.add (sendBUT);
		planPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Plan definition", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		textPA.setVisible (true);
		textPA.setLayout (new BorderLayout());
		textPA.add (clearBUT,BorderLayout.NORTH);
		textplan.setLineWrap (true);
		textPA.add (textplan,BorderLayout.CENTER);
		textPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Current plan", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		ltimeoutSL.setMinimum(0);
		ltimeoutSL.setMaximum(LTIMEOUT);
		ltimeoutSL.setValue(DTIMEOUT);
		ltimeoutSL.setLabelTable(ltimeoutSL.createStandardLabels(LTIMEOUT/4));
		ltimeoutSL.setPaintLabels(true);
		ButtonGroup butgroup=new ButtonGroup();
		lallRB.setText("Send update to all robots");
		lallRB.setSelected(true);
		lactualRB.setText("Send update to selected robot only");
		lactualRB.setSelected(false);
		butgroup.add(lallRB);
		butgroup.add(lactualRB);
		lindaPA.setVisible (true);
		lindaPA.setLayout (new GridLayout(3,1));
		lindaPA.add(ltimeoutSL);
		lindaPA.add(lallRB);
		lindaPA.add(lactualRB);	
		lindaPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Linda monitor", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		cursorPA.setVisible (true);
		cursorPA.setLayout (new GridLayout(3,1));
		cursorPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Map monitor", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		cursorPA.add (cursorLA);

		statePA.setVisible (true);
		statePA.setLayout (new GridLayout(3, 1));
		statePA.add (cursorPA);
		statePA.add (lindaPA);
		
		taskPA.setVisible (true);
		taskPA.setLayout (new GridLayout(3, 1));
		taskPA.add (planPA);
		taskPA.add (textPA);
		
		monitorPA.setVisible (true);
		monitorPA.setLayout (new GridLayout(1, 1));
		
		robotPA.setVisible (false);
		robotPA.setLayout (new GridLayout(1, 2));
		robotPA.add (new JLabel ("Robot:"));
		robotPA.add (robotsCB);
		robotPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Current Robot", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		optsTP.setVisible (false);
		optsTP.insertTab ("Status", null, (JComponent) statePA, null, 0);
		optsTP.insertTab ("Task", null, (JComponent) taskPA, null, 1);
		optsTP.insertTab ("Monitor", null, (JComponent) monitorPA, null, 2);

		rightPA.setVisible (true);
		rightPA.setLayout (new BorderLayout());
		rightPA.add (robotPA, BorderLayout.NORTH);
		rightPA.add (optsTP, BorderLayout.CENTER);
		rightPA.add (execPA, BorderLayout.SOUTH);

		
		robotTB.setModel (robots);
		robotTB.setPreferredScrollableViewportSize(new Dimension(500, 75));
		robotTB.setGridColor (Color.lightGray);
		robotTB.setShowGrid (true);
		robotTB.setShowHorizontalLines (true);
		robotTB.setShowVerticalLines (false);
		robotTB.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		robotSP = new javax.swing.JScrollPane (robotTB);

		eventTB.setModel (events);
		eventTB.setPreferredScrollableViewportSize(new Dimension(500, 75));
		eventTB.setDefaultRenderer (Object.class, new EventListRenderer (eventTB));
		eventTB.setGridColor (Color.lightGray);
		eventTB.setShowGrid (true);
		eventTB.setShowHorizontalLines (true);
		eventTB.setShowVerticalLines (false);
		eventTB.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
		eventSP = new javax.swing.JScrollPane (eventTB);

		tableTP.setVisible (true);
		tableTP.setMinimumSize (new Dimension (300, 75));
		tableTP.insertTab ("Robots", null, (JComponent) robotSP, null, 0);
		tableTP.insertTab ("Events", null, (JComponent) eventSP, null, 1);

		topPA.setLayout (new BorderLayout ());
		topPA.setMinimumSize (new Dimension (300, 75));
		topPA.add (mapPA, BorderLayout.CENTER);		
		topPA.add (rightPA, BorderLayout.EAST);

		rootSP.setOrientation(JSplitPane.VERTICAL_SPLIT);
		rootSP.setOneTouchExpandable(true);
		rootSP.setTopComponent(topPA);
		rootSP.setBottomComponent(tableTP);
//		rootSP.setDividerLocation(0.8);
		rootSP.setResizeWeight(0.5);
		
		// Create main panel
		setLocation(new java.awt.Point(0, 0));
		setSize(new Dimension (600, 480));
		setMinimumSize (new Dimension (200, 200));
		setLayout(new GridLayout (1, 1));
		add (rootSP);

		// event handling
		robotsCB.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				robotsCBActionPerformed(e);
			}
		});		
		addgBUT.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				addgBUTActionPerformed(e);
			}
		});		
		addaBUT.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				addaBUTActionPerformed(e);
			}
		});			
		sendBUT.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				sendBUTActionPerformed(e);
			}
		});		
		clearBUT.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				clearBUTActionPerformed(e);
			}
		});
		startBUT.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				startBUTActionPerformed(e);
			}
		});
		stopBUT.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				stopBUTActionPerformed(e);
			}
		});
		stepBUT.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				stepBUTActionPerformed(e);
			}
		});
		resetBUT.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				resetBUTActionPerformed(e);
			}
		});	
		delBUT.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				delBUTActionPerformed(e);
			}
		});		
		dumpBUT.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				dumpBUTActionPerformed(e);
			}
		});		
		map2D.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			public void mouseDragged(java.awt.event.MouseEvent e) {
				map2DMouseDragged(e);
			}
			public void mouseMoved(java.awt.event.MouseEvent e) {
				map2DMouseMoved(e);
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
		ltimeoutSL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				ltimeoutSliderChanged(e);
			}
		});
	}

	public void open (Properties config)
	{
		this.config	= config;
		
		// Create and initialise standard components
		try { initComponents (); } catch (Exception e) { e.printStackTrace(); }
		setVisible (true);
	}
	
	public void close ()
	{
		Enumeration			enu;
		MonitorPanel			panel;

		enu		= rtable.keys ();
		while (enu.hasMoreElements ())
		{
			panel	= (MonitorPanel) rtable.get (enu.nextElement ());
			panel.close ();
		}
	}
	
	public void 			setMonitor (Monitor monitor)		{ this.monitor = monitor; }
	public Monitor 		getMonitor () 					{ return monitor; }
	public RobotList		getRobotList () 					{ return robots; }
	public void 			setMonitorMenu (JMenu menu)		{ this.menu = menu; }
	
	public void changeConfiguration (String id, RobotDesc rdesc, FusionDesc fdesc)
	{
		robots.add (id, rdesc, fdesc);
		updateRobotPanel(id, robots);
		addRobotPanel(id);
	}

	public void deleteConfiguration (String id)
	{ 
		robots.delete (id);	
		removeRobotPanel (id, robots);
	}	
	
	public void	updateRobot (String id, MonitorData data, LPO[] lpos) 
	{
		String			posmsg = "unknown";
		
		if (worldmap != null)
			posmsg = worldmap.toString (data.cur.x (), data.cur.y ());
			
		robots.update (id, data, lpos, posmsg);
		updateRobotPanel(id, robots);
	}

	public void	updateGoal (String id, Position goal) 
	{
		robots.update (id, goal);
		updateRobotPanel(id, robots);
	}
	
	public void updateStatus (String id, long tstamp, int type, String message) 
	{
		JScrollBar			scroll;
		
		updateRobotPanel (id, robots);
		
		if (ItemStatus.typeIsReport (type))
		{
			events.addRow (id, tstamp, type, message);
			eventTB.setRowSelectionInterval (events.getRowCount()-1, events.getRowCount()-1);
			scroll	= eventSP.getVerticalScrollBar ();
			scroll.setValue (scroll.getMaximum ());
		}
	
		robots.update (id, ItemStatus.typeToString (type)+". "+message);
	}

	protected void updateRobotPanel (String id, RobotList robots)
	{	
		MonitorPanel			panel;

		robotTB.setModel (robots);
		if (map != null)
			map.update (robots);
		mapPA.repaint ();

		panel	= (MonitorPanel) rtable.get (id);
		if (panel != null)
			panel.setRobotData (robots);
	}

	protected void addRobotPanel (String id)
	{
		int 				i;
		String[] 		ids;
		MonitorPanel		panel;
	
		// Create new Panel (this MUST be done before updating the comboBox)
		panel	= new MonitorPanel (id, this, menu, config);
		rtable.put (id, panel);
		
		// Update the comboBox
		updating		= true;
		ids			= robots.getIDs ();
		robotsCB.removeAllItems ();
		for (i = 0; i < ids.length; i++)
			robotsCB.addItem (ids[i]);
		
		updating		= false;
		robotsCB.setSelectedItem (ids[0]);
	}

	protected void removeRobotPanel (String id, RobotList robots)
	{
		MonitorPanel 	panel;
		
		robotsCB.removeItem (id);
		panel=(MonitorPanel)rtable.remove (id);
		panel.delRobotJMenu(id);
		
		robotTB.setModel (robots);
		map.update (robots);
		mapPA.repaint ();
	}
	
	public void	updateWorldMap (String id, Properties worldprops) 
	{
		Dimension		dim;
		
		if (this.worldmap != null)			return;
		
		dim		= rootSP.getSize ();
		rootSP.setDividerLocation((int) Math.round (dim.getHeight () * 0.8));

		if (worldprops == null)
		{
			JOptionPane.showConfirmDialog (this, "The current robot has no associated map.", "Warning", JOptionPane.DEFAULT_OPTION);
			worldprops	= new Properties ();
		}

		worldmap = new World (worldprops);
		
		map = new World2D (map2D.getModel (), worldmap);
		map.drawsensors (false);
		map.autoscale (false);
		map.simulated (false);
		map.drawdanger (false);
		map.drawwarning (false);
		map.update (robots);		
		mapPA.repaint ();

		if (worldmap != null)
		{
			for (int i=0; i < worldmap.docks().n(); i++)
				gotoCB.addItem (worldmap.docks().at(i).label);
			
			for (int i=0; i < worldmap.zones().n(); i++)
				gotoCB.addItem (worldmap.zones().at(i).label);
		}
	}
	
	public World getWorldMap ()
	{
		return worldmap;
	}

	public void updateBehInfo (String id, ItemBehInfo behInfo)
	{
		MonitorPanel			panel;

		panel	= (MonitorPanel) rtable.get (id);
		if (panel != null)
			panel.setBehaviours (behInfo);
	}

	public void setTitle (String title)
	{
		this.title = TITLE + ": " + title;
	}

	public String getTitle ()
	{
		return title;
	}
	
	//*****************
	// Event handling
	//*****************
	public void robotsCBActionPerformed(java.awt.event.ActionEvent e) 
	{
		String			id;
		
		if (updating)			return;
		
		id		= (String) robotsCB.getSelectedItem ();
		if (id != null)
		{
			monitorPA.removeAll ();
			monitorPA.add ((MonitorPanel) rtable.get (id));			
			monitorPA.repaint ();
		}
	}
	
	public void addaBUTActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (!gotoadded)
			return;

		textplan.append ("<"+actionCB.getSelectedItem()+">|");
		gotoadded = false;
		
	}
	
	public void addgBUTActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (gotoadded)
			return;
		
		textplan.append (">" + gotoCB.getSelectedItem());
		gotoadded = true;	
	}
	
	public void clearBUTActionPerformed(java.awt.event.ActionEvent e) 
	{
		textplan.setText ("");
		gotoadded = false;
	}
	
	public void sendBUTActionPerformed(java.awt.event.ActionEvent e) 
	{
		monitor.setID ((String) robotsCB.getSelectedItem());	
		monitor.setPlan (new Sequence (textplan.getText(), "><|"));
	}

	public void stepBUTActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (monitor != null)
		{
			monitor.setID ((String) robotsCB.getSelectedItem());	
			monitor.setCommand (ItemDebug.STEP);
		}
	}

	public void stopBUTActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (monitor != null)
		{
			monitor.setID ((String) robotsCB.getSelectedItem());			
			monitor.setCommand (ItemDebug.STOP);
		}
	}

	public void startBUTActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (monitor != null)
		{
			monitor.setID ((String) robotsCB.getSelectedItem());	
			monitor.setCommand (ItemDebug.START);
		}
	}

	public void resetBUTActionPerformed(java.awt.event.ActionEvent e) 
	{
		if (monitor != null)
		{
			monitor.setID ((String) robotsCB.getSelectedItem());	
			monitor.setCommand (ItemDebug.RESET);
		}
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

	protected void map2DMouseMoved(java.awt.event.MouseEvent e) 
	{
		Point2		pt;
			
		map2D.mouseAxis (e.getX (), e.getY ());
		pt		= map2D.mouseClick (e.getX (), e.getY ());
		if (pt != null)
			cursorLA.setText ("Cursor X:" + getPosString (pt.x (), 3) + " Y:" + getPosString (pt.y (), 3) + " (m)");
	}

	public void delBUTActionPerformed(java.awt.event.ActionEvent e) 
	{
		String id = (String)robotsCB.getSelectedItem(); 
		if (id!=null)
		{
			monitor.setID(LindaEntryFilter.ANY);
			monitor.setDelRobot(id);
			
			monitor.setID (id);	
			monitor.setLindaCtrl (ItemLindaCtrl.DELETE);
			
		}
	}
	
	public void dumpBUTActionPerformed(java.awt.event.ActionEvent e) 
	{
		String id = (String)robotsCB.getSelectedItem(); 
		if (id!=null)
		{
			monitor.setID (id);	
			monitor.setLindaCtrl (ItemLindaCtrl.DUMPREG);
		}
	}
	
	public void ltimeoutSliderChanged(javax.swing.event.ChangeEvent e)
	{

		if (ltimeoutSL.getValueIsAdjusting()) return;
		
		if (lallRB.isSelected())
		{
			String[] allRobots;
			allRobots=robots.getIDs();
			for (int i=0;i<allRobots.length;i++)
			{
				monitor.setID(allRobots[i]);
				monitor.setLindaTimeout(ltimeoutSL.getValue());
			}
		}
		else
		{
			String robotid;
			robotid=(String)robotsCB.getSelectedItem();
		  	if (robotid==null) 
		  		return;
		  	else
		  	{
				monitor.setID(robotid);
				monitor.setLindaTimeout(ltimeoutSL.getValue());
			}
		}
	}
}








