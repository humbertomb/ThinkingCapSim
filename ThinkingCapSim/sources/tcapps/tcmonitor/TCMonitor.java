/*
 * (c) 2003 Bernardo Canovas Segura, Humberto Martinez
 * (c) 2004 Humberto Martinez Barbera
 */

package tcapps.tcmonitor;

import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;

import tc.modules.*;
import tc.gui.*;
import tc.gui.monitor.*;

import wucore.gui.*;

public class TCMonitor extends JFrame implements GUIApplication
{
	// Global constants
	static public final String			VERSION				= "BGenWeb 2.4b - MultiRobot Remote Monitoring and Control";

	// Global & Common structures
	protected Monitor	 				monitor				= null;
	protected Properties					confProps 			= new Properties();
	protected MultiRobotPanelInterf		multiPanel			= null;
	protected boolean					ready				= false;

	// GUI components
	protected JMenuBar 					mainMB 				= new JMenuBar();
	protected JMenu 						monitorMI 			= new JMenu();
	protected JMenu 						windowMI				= new JMenu();

	protected JMenuItem 					newThreadsMI 		= new JMenuItem();
	protected JMenu						changePanelMI		= new JMenu();
	protected JMenu 						applicationsMI 		= new JMenu();
	protected JTabbedPane 				robotsTP 			= new JTabbedPane(SwingConstants.TOP);

	public TCMonitor() 
	{
	}

	protected void initComponents() throws Exception 
	{
		mainMB.setVisible(true);
		monitorMI.setVisible(true);
		monitorMI.setText("Monitor");
		windowMI.setVisible(true);
		windowMI.setText("Windows");
		newThreadsMI.setVisible(true);
		newThreadsMI.setText("Add new Threads window");
		changePanelMI.setText("Change multirobot panel");
		changePanelMI.setVisible(true);
		changePanelMI.setEnabled(false);
		applicationsMI.setVisible(true);
		applicationsMI.setEnabled(false);
		applicationsMI.setText("Applications");

		setLocation(new java.awt.Point(0, 0));
		setJMenuBar(mainMB);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		setTitle(VERSION);

		windowMI.add(changePanelMI);
		windowMI.add(newThreadsMI);
		mainMB.add(monitorMI);
		mainMB.add(windowMI);
		mainMB.add(applicationsMI);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(robotsTP,BorderLayout.CENTER);		

		// event handling
		robotsTP.addChangeListener(new javax.swing.event.ChangeListener() {
			public void  stateChanged(javax.swing.event.ChangeEvent e) {
				robotsTBChanged(e);
			}
		});		
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				thisWindowClosing(e);
			}
		});
		newThreadsMI.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				newThreadsMIActionPerformed(e);
			}
		});		
	}
  
  	private boolean mShown = false;
  	
	public void addNotify() {
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
	protected void thisWindowClosing(java.awt.event.WindowEvent e)
	{
		if (JOptionPane.showConfirmDialog (this, "This action will stop all the services. Do you really want to quit?", "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
		{
			// Close all GUI windows
			if (multiPanel != null)
			{
				multiPanel.close ();
				multiPanel = null;
			}

			// Stop connection to the Linda space
			if (monitor != null)
			{
				monitor.stop ();
				monitor = null;
			}
			
			// Close this GUI
			setVisible (false);
			dispose ();
		}
		else
			setVisible (true);
	}
	
	/** Connects with a new robot by RMI and reconfigures GUI */
	/*	private void connect () 
		{		
			// Adds the robot tab
			actualRobot=newRobotId;
			tabPanel.setLayout(new BorderLayout());
			tabPanel.add(toolBarsPanel,BorderLayout.NORTH);
			framesPanel.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
			tabPanel.add(framesPanel,BorderLayout.CENTER);
			robotsTP.addTab(actualRobot,tabPanel);
			robotsTP.setSelectedComponent(tabPanel);
			robotGraphicStates.put(actualRobot,new BGenWebAdminMemento());
		}		

		optsTP.addChangeListener(new javax.swing.event.ChangeListener() {
			public void  stateChanged(javax.swing.event.ChangeEvent e) {
				optsTBStateChanged(e);
			}
		});		

		public void optsTBStateChanged(javax.swing.event.ChangeEvent e)
	{
		if (connected==false) return;
		connected=false;
		if (actualRobot!=null)
			this.saveState(actualRobot);
		actualRobot=robotsTP.getTitleAt(robotsTP.getSelectedIndex());
		if (actualRobot.equals(multiPanel.getTitle()))
		{
			actualRobot=null;
			disconnectBUT.setEnabled(false);
		} else {
			this.retreiveState(actualRobot);
			disconnectBUT.setEnabled(true);
		}
		connected=true;
	}
	*/


    /**************/
	/* listeners  */
	/**************/
		
	/** Listener for the robots tab pane */
	protected void robotsTBChanged(javax.swing.event.ChangeEvent e) 
	{
	}

	/** Listener for the 'windows->add new Threads window' option */
	protected void newThreadsMIActionPerformed(java.awt.event.ActionEvent e) 
	{
		ThreadsWindow			thwin;
		
		thwin	= new ThreadsWindow ();
		thwin.start ();
	}

	/** Listener for the items of the "applications" menu */
	protected void applicationItemsActionPerformed(java.awt.event.ActionEvent e) {
		JMenuItem mItem;
		String className;
		mItem=(JMenuItem) e.getSource();
		className=(String)mItem.getClientProperty("ClassToExecute");
		try {
			Class.forName (className).newInstance ();
		} catch (Exception exc)
		{
			JOptionPane.showMessageDialog(this, "Error executing application "+className+": "+exc.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
			exc.printStackTrace();
		}	
	}

	/** Listener for the items of the "change Multi Robot Panel" menu */
	protected void changePanelItemsActionPerformed(java.awt.event.ActionEvent e)
	{
		JMenuItem mItem;
		String className;
		mItem=(JMenuItem) e.getSource();
		className=(String)mItem.getClientProperty("ClassToLoad");
		try {
			Class mrpclass=  Class.forName(className);
 robotsTP.removeTabAt(robotsTP.indexOfTab(multiPanel.getTitle())); // ESTO A VECES DA FALLOS!!!
			multiPanel = (MultiRobotPanel) mrpclass.newInstance();
			multiPanel.setMonitorMenu (monitorMI);
			if (monitor!=null)
				multiPanel.setMonitor (monitor);
			robotsTP.insertTab(multiPanel.getTitle(),null,(JComponent)multiPanel,null,0);
			multiPanel.open (confProps);
//			ready = true; /** �hace falta ya? */
		} catch (Exception exc) { }
	}

	/****************************/
	/** GUIApplication Methods **/
	/****************************/
 
	/** Opens and configures the window */
	public void start ()
	{		
		// Create and initialise graphics widgets
		try { initComponents (); } catch (Exception e) { e.printStackTrace(); }

		// Create and initialize another structures
		loadConfiguration();

		// 'Maximizes' the window
//		GraphicsConfiguration gc=getGraphicsConfiguration();
//		if (gc!=null) setBounds(gc.getBounds());
		GraphicsConfiguration gc=getGraphicsConfiguration();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize() ;
		Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(gc);
		this.setBounds(insets.left,insets.top, Math.min (d.width-insets.right, 1200), Math.min (d.height-insets.bottom-300, 900)) ;
		
//		System.out.println("BGenWebAdmin insets "+insets);
//		System.out.println("BGenWebAdmin dimension "+d);
//		System.out.println("insets.left="+insets.left+",insets.top="+insets.top+", d.width-insets.right="+(d.width-insets.right)+", d.height-insets.bottom-20="+(d.height-insets.bottom-20));
//		System.out.println("BGenWebAdmin cambia bounds "+this.getBounds());
		
		setVisible (true);
	}

	public void setMonitor (Monitor monitor)
	{	
		this.monitor		= monitor;
		
		if (multiPanel != null)
		{
			multiPanel.setMonitor (monitor);
			
			robotsTP.setTitleAt (0, multiPanel.getTitle ());
		}
	}
	
	public GUIMonitor	getGUIMonitor ()		{ return multiPanel; }
	public boolean		isReady () 			{ return ready; }
	
	/**************************/
	/* Other internal methods */
	/**************************/
	/** Creates a GridBagConstraints object for a GridBagLayout */
	private static java.awt.GridBagConstraints makeGridBagConstraints(int gridx, int gridy,
					int gridwidth, int gridheight,
					double weightx, double weighty,
					int anchor, int fill,
					java.awt.Insets insets, int ipadx, int ipady) {
	    java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
	    c.gridx = gridx;
	    c.gridy = gridy;
	    c.gridwidth = gridwidth;
	    c.gridheight = gridheight;
	    c.weightx = weightx;
	    c.weighty = weighty;
	    c.anchor  = anchor;
	    c.fill = fill;
	    c.insets = insets;
	    c.ipadx = ipadx;
	    c.ipady = ipady;
	    
	   	return c;
	}
				
	/** Reads the configuration file */
	private void loadConfiguration()
	{
		File			file;
		FileInputStream	stream;

		confProps			= new Properties ();
		try
		{
			file 		= new File ("conf/apps/bgwclient.cfg");
			stream 		= new FileInputStream (file);
			confProps.load (stream);
			stream.close ();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error reading configuration file: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}
		
		// Gets available multi-robot classes
		Hashtable multiPanels = new Hashtable();
		if ((confProps.getProperty("MULTIROBOTPANEL0_NAME")==null) ||	(confProps.getProperty("MULTIROBOTPANEL0_CLASS")==null))
			
			JOptionPane.showMessageDialog(this, "Error parsing configuration file: properties MULTIROBOTPANEL0_NAME or MULTIROBOTPANEL0_CLASS not found","Error",JOptionPane.ERROR_MESSAGE);
		else
		{
			int i=0;
			String mpName;
			String mpClassname;

			JMenuItem auxItem;
			while (confProps.getProperty("MULTIROBOTPANEL"+i+"_NAME")!=null)
			{
				mpName = confProps.getProperty("MULTIROBOTPANEL"+i+"_NAME");
				auxItem = new JMenuItem();
				auxItem.setText(mpName);
				auxItem.setEnabled(true);
				if (confProps.getProperty("MULTIROBOTPANEL"+i+"_CLASS")==null)
					JOptionPane.showMessageDialog(this, "Error parsing configuration file: multi robot panel "+auxItem.getText()+" has no class declared with _CLASS option","Error",JOptionPane.ERROR_MESSAGE);
				else
				{
					mpClassname = confProps.getProperty("MULTIROBOTPANEL"+i+"_CLASS");
					auxItem.putClientProperty("ClassToLoad",mpClassname);
					multiPanels.put(mpName,mpClassname);
//					auxItem.putClientProperty("ClassToExecute",confProps.getProperty("APP"+i+"_CLASS"));
					auxItem.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent e) {
							changePanelItemsActionPerformed(e);
						}
					});
					changePanelMI.add(auxItem);
				}
				i++;
			}
		}

		// Gets multi-robot panel class
		if (confProps.getProperty("MULTIROBOTPANEL_DEFAULT")==null)
			JOptionPane.showMessageDialog(this, "Error parsing configuration file: property MULTIROBOTPANEL_DEFAULT not found","Error",JOptionPane.ERROR_MESSAGE);		
		else
		{
			try {
				if (!multiPanels.containsKey(confProps.getProperty("MULTIROBOTPANEL_DEFAULT")))
					JOptionPane.showMessageDialog(this, "Error parsing configuration file: value of MULTIROBOTPANEL_DEFAULT is not a declared multi robot panel NAME","Error",JOptionPane.ERROR_MESSAGE);
				else
				{
					Class mrpclass=  Class.forName((String)multiPanels.get(confProps.getProperty("MULTIROBOTPANEL_DEFAULT")));
					multiPanel=(MultiRobotPanelInterf)mrpclass.newInstance();
					multiPanel.setMonitorMenu (monitorMI);
					if (monitor != null)
						multiPanel.setMonitor (monitor);
					robotsTP.addTab(multiPanel.getTitle(),(JComponent)multiPanel);
					multiPanel.open (confProps);
				}
			} catch (Exception e) {
				System.out.println("Error loading panel class <"+(String)multiPanels.get(confProps.getProperty("MULTIROBOTPANEL_DEFAULT"))+">: "+e.getMessage());
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Error loading panel class <"+(String)multiPanels.get(confProps.getProperty("MULTIROBOTPANEL_DEFAULT"))+">: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
			}
		}

		// Gets the applications menu options
		if (confProps.getProperty("APP0_NAME")==null)
			applicationsMI.setEnabled(false);
		else
		{
			int i=0;
			JMenuItem auxItem;
			applicationsMI.setEnabled(true);
			while (confProps.getProperty("APP"+i+"_NAME")!=null)
			{
				auxItem = new JMenuItem();
				auxItem.setText(confProps.getProperty("APP"+i+"_NAME"));
				auxItem.setEnabled(true);
				if (confProps.getProperty("APP"+i+"_CLASS")==null)
					JOptionPane.showMessageDialog(this, "Error parsing configuration file: application "+auxItem.getText()+" has no class declared with _CLASS option","Error",JOptionPane.ERROR_MESSAGE);
				else
				{
					auxItem.putClientProperty("ClassToExecute",confProps.getProperty("APP"+i+"_CLASS"));
					auxItem.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent e) {
							applicationItemsActionPerformed(e);
						}
					});
					applicationsMI.add(auxItem);
				}
				i++;
			}
		}
		
		ready	= true;
	}
}


