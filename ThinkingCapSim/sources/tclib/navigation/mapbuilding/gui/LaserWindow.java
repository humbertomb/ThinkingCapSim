/*
 * (c) 2004 David Herrero Perez, Humberto Martinez Barbera
 *
 *	Laser test with GUI application
 */

package tclib.navigation.mapbuilding.gui;

import java.io.*;
import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;

import javax.comm.CommPortIdentifier;
import javax.swing.*;

import devices.drivers.laser.*;

import tclib.navigation.mapbuilding.lpo.*;
import tclib.navigation.mapbuilding.visualization.*;

import tc.shared.lps.lpo.*;
import tc.vrobot.*;

import wucore.widgets.*;
import wucore.utils.geom.*;
import wucore.utils.math.*;
import wucore.utils.math.jama.*;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */

public class LaserWindow extends Object implements Runnable
{
	public static int 			RAYSCAN		= 361;
	public static double			CONESCAN		= 180.0 * Angles.DTOR;
	public static double			RANGESCAN	= 15.0;
	
	static public final String 	START_PROC	= "Start Processing";
	static public final String 	STOP_PROC	= "Stop Processing ";
	static public final String 	START_LOG	= "Start Logging";
	static public final String 	STOP_LOG		= "Stop Logging ";
	static public final String 	SINGLE_LOG	= " Single Log  ";

	static public final String 	DEF_DIR		= "logs";
	static public final String 	DEF_FILE		= "laserscan.log";
	static public final boolean AUTOPORTDETECT = true;

	Laser laser;
	double[] values;
	SensorPos senpos;
	LPOSensorScanner scanLRF;
	LPOFSegments inviewLRF;	// Curent in-view fuzzy segments (laser generated)
	LPOSensorSignat dataLRF; // Temporal sensor buffer for segment generation
	LPORangeLTG ltgLRF; // Current laser generated Local-Tangent-Graph
	Matrix P = new Matrix (2, 2, 1.0);
	boolean running = false;
	boolean logging = false;
	
	// Laser visualization
	Component2D laserCO;
	Scanner2D mlaser;
	Point2 cursor;
	
	// RIEPFA controls
	JSlider riepfa_pminSL;
	JSlider riepfa_bpmaxSL;
	JSlider riepfa_distSL;	
	JLabel riepfa_pminLA;
	JLabel riepfa_bpmaxLA;
	JLabel riepfa_distLA;	
	JLabel riepfa_segsLA;
	
	// LTG controls
	JSlider ltg_rangeSL;
	JSlider ltg_windowSL;
	JSlider ltg_zeroSL;	
	JSlider ltg_szoneSL;	
	JSlider ltg_bgapSL;	
	JLabel ltg_rangeLA;
	JLabel ltg_windowLA;
	JLabel ltg_zeroLA;
	JLabel ltg_szoneLA;	
	JLabel ltg_bgapLA;	
	JLabel ltg_segsLA;
	JCheckBox ltg_debugCB;

	// Tools
	JFrame frame;
	JToggleButton logBU;
	JToggleButton startBU;
	JComboBox portsCB;
	JComboBox modelsCB;
	JTextField logTF;
	JSlider waitSL;
	JCheckBox looplogCB;
	JFileChooser chooser;
	String curfpath;
		
	public LaserWindow()
	{
		JPanel laserPA;
		JPanel configPA;

		// Set up default directories
		chooser = new JFileChooser (System.getProperty ("user.dir") + File.separator + DEF_DIR);

		// Create custom components		
		laserCO = new Component2D ();
		laserCO.setClipping (false);
		laserCO.setPreferredSize (new Dimension(700, 500));
		mlaser = new Scanner2D (laserCO.getModel ());
		mlaser.range (RANGESCAN+2.0);
		laserPA	= new JPanel ();
		laserPA.setLayout (new GridLayout(1, 1));
		laserPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Laser Scanner", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		laserPA.add (laserCO);
						
		// Create configuration panel
		configPA = new JPanel ();
		configPA.setLayout (new BoxLayout (configPA, BoxLayout.Y_AXIS));
		configPA.add (createRiepfaPanel ());
		configPA.add (createLtgPanel ());
				
		// Create the frame
		frame = new JFrame("Laser Scanner Calibration Tool");
		frame.getContentPane().add(createToolsPanel (), BorderLayout.NORTH);
		frame.getContentPane().add(laserPA, BorderLayout.CENTER);
		frame.getContentPane().add(configPA, BorderLayout.EAST);
		
		// Add component listeners
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) { System.exit(0); }
		});
		laserCO.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
			public void mouseDragged(java.awt.event.MouseEvent e) {
				laserCOMouseDragged(e);
			}
			public void mouseMoved(java.awt.event.MouseEvent e) {
				laserCOMouseMoved(e);
			}
		});		
		laserCO.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent e) {
				laserCOMousePressed(e);
			}		
			public void mouseReleased(java.awt.event.MouseEvent e) {
				laserCOMouseReleased(e);
			}
		});	
		
		initialiseLPOs ();
		
		frame.setVisible(true);
		frame.pack();
	}
	
	// Static methods
	static public String format (double val, int dig)
	{
		String			str;
		
		str		= Double.toString (Math.round (val * 1000.0) * 0.001);
		while (str.length () - str.indexOf ('.') > dig + 1)
			str		= str.substring (0, str.length () - 1);
		while (str.length () - str.indexOf ('.') < dig + 1)
			str		= str + "0";
		
		return str;
	}

	private JPanel createToolsPanel ()
	{
		JPanel topPA;
		JPanel realPA;
		JPanel logPA;
		JButton fportBU;
		JButton flogBU;

		// Create commands panel
		fportBU = new JButton ("Add File/Port");
		portsCB = new JComboBox ();
		portsCB.addItem ("/dev/tty.USA1941P1.1");
		portsCB.addItem ("/dev/tty.usbserial");
		if(AUTOPORTDETECT){
			try{
				CommPortIdentifier portId;
				Enumeration en = CommPortIdentifier.getPortIdentifiers();
				// iterate through the ports.
				while (en.hasMoreElements()) {
					portId = (CommPortIdentifier) en.nextElement();
					if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
						portsCB.addItem(portId.getName());
						System.out.println("Serial port found: "+portId.getName());
					}
				}
			}catch(Exception e){
			}
		}
		modelsCB = new JComboBox ();
		modelsCB.addItem ("devices.drivers.laser.LMS200.LMS200");
		modelsCB.addItem ("devices.drivers.laser.PLS.PLS");
		modelsCB.addItem ("devices.drivers.laser.LaserFile");
		
		realPA = new JPanel ();
		realPA.setLayout (new BoxLayout (realPA, BoxLayout.X_AXIS));
		realPA.add (fportBU);
		realPA.add (new JLabel ("Port/File"));
		realPA.add (portsCB);
		realPA.add (new JLabel ("Driver"));
		realPA.add (modelsCB);
		
		// Create log panel
		startBU = new JToggleButton (START_PROC);
		logBU = new JToggleButton (SINGLE_LOG);
		flogBU = new JButton ("Set Log File");
		logTF = new JTextField (DEF_FILE);
		waitSL = new JSlider (0, 2000, 0);
		waitSL.setMajorTickSpacing(1000);
		waitSL.setMinorTickSpacing(100);
		waitSL.setPaintTicks (true);
		looplogCB = new JCheckBox ("Continuous Logging");
		looplogCB.setSelected (logging);
		
		logPA = new JPanel ();
		logPA.setLayout (new BoxLayout (logPA, BoxLayout.X_AXIS));
		logPA.add (flogBU);
		logPA.add (logTF);
		logPA.add (logBU);
		logPA.add (looplogCB);
		logPA.add(Box.createHorizontalStrut (15));
		logPA.add (new JSeparator (JSeparator.VERTICAL));
		logPA.add(Box.createHorizontalStrut (15));
		logPA.add (new JLabel ("Wait Time (ms)"));
		logPA.add (waitSL);
		logPA.add (startBU);
		
		// Create main panel
		topPA = new JPanel ();
		topPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Execution and Log Control", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		topPA.setLayout (new BoxLayout (topPA, BoxLayout.Y_AXIS));
		topPA.add (realPA);
		topPA.add (new JSeparator ());
		topPA.add (logPA);

		startBU.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				startBUActionPerformed(e);
			}
		});		
		fportBU.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				fportBUActionPerformed(e);
			}
		});		
		flogBU.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				flogBUActionPerformed(e);
			}
		});		
		logBU.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				logBUActionPerformed(e);
			}
		});		
		looplogCB.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				looplogCBActionPerformed(e);
			}
		});		

		return topPA;
	}
	
	private JPanel createRiepfaPanel ()
	{
		JPanel mainpanel;
		Box mainbox;
		JPanel npointspanel;
		JPanel maxbkppanel;
		JPanel distpanel;
		
		mainpanel = new JPanel(new BorderLayout());
		mainpanel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "RIEPFA Configuration", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		// For configuring the number of minimum points on riepfa algorithm
		npointspanel = new JPanel();
		npointspanel.setLayout(new BoxLayout(npointspanel, BoxLayout.Y_AXIS));
		
		JPanel nppanelslider;
		nppanelslider = new JPanel();
		nppanelslider.setLayout(new BoxLayout(nppanelslider, BoxLayout.X_AXIS));
		
		JLabel lbminp = new JLabel("Minimun Number of Points");
		lbminp.setForeground(new Color(29,14,237));
		riepfa_pminSL= new JSlider(4, 50, 6);
		riepfa_pminLA = new JLabel("__ pts");
		riepfa_pminLA.setForeground(new Color(29,14,237));
		riepfa_pminLA.setFont(new Font("values", Font.PLAIN, 12));
		
		npointspanel.add(lbminp);
		nppanelslider.add(riepfa_pminSL);
		nppanelslider.add(riepfa_pminLA);
		npointspanel.add(nppanelslider);
		
		// For configuring the maximum break point on riepfa algorithm
		maxbkppanel = new JPanel();
		maxbkppanel.setLayout(new BoxLayout(maxbkppanel, BoxLayout.Y_AXIS));
		
		JPanel mbpanelslider;
		mbpanelslider = new JPanel();
		mbpanelslider.setLayout(new BoxLayout(mbpanelslider, BoxLayout.X_AXIS));
		
		JLabel lbmaxb = new JLabel("Maximun Break Point Distance");
		lbmaxb.setForeground(new Color(29,14,237));
		riepfa_bpmaxSL = new JSlider(5, 1000, 150);
		riepfa_bpmaxLA = new JLabel("____ m");
		riepfa_bpmaxLA.setForeground(new Color(29,14,237));
		riepfa_bpmaxLA.setFont(new Font("values", Font.PLAIN, 12));
				
		maxbkppanel.add(lbmaxb);
		mbpanelslider.add(riepfa_bpmaxSL);
		mbpanelslider.add(riepfa_bpmaxLA);
		maxbkppanel.add(mbpanelslider);
		
		// For configuring the fit distance of riepfa algorithm
		distpanel = new JPanel();
		distpanel.setLayout(new BoxLayout(distpanel, BoxLayout.Y_AXIS));
		
		JPanel distpanelslider;
		distpanelslider = new JPanel();
		distpanelslider.setLayout(new BoxLayout(distpanelslider, BoxLayout.X_AXIS));
		
		JLabel lbdist = new JLabel("Riepfa Distance");
		lbdist.setForeground(new Color(29,14,237));
		riepfa_distSL= new JSlider(1, 100, 30);
		riepfa_distLA = new JLabel("____ m");
		riepfa_distLA.setForeground(new Color(29,14,237));
		riepfa_distLA.setFont(new Font("values", Font.PLAIN, 12));
				
		distpanel.add(lbdist);
		distpanelslider.add(riepfa_distSL);
		distpanelslider.add(riepfa_distLA);
		distpanel.add(distpanelslider);
		
		riepfa_segsLA = new JLabel("Number of Segments: " + 0);
		riepfa_segsLA.setForeground(new Color(230,43,43));
		riepfa_segsLA.setFont(new Font("Bold", Font.BOLD, 16));
		
		// For layout compnents
		mainbox = new Box(BoxLayout.Y_AXIS);
		mainbox.add(npointspanel);
		mainbox.add(Box.createVerticalStrut(10));
		mainbox.add(maxbkppanel);
		mainbox.add(Box.createVerticalStrut(10));
		mainbox.add(distpanel);
		mainbox.add(Box.createVerticalStrut(10));
		mainbox.add(riepfa_segsLA);
		
		mainpanel.add(mainbox);

		riepfa_pminSL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				riepfa_pminSLStateChanged(e);
			}		
		});	
		riepfa_bpmaxSL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				riepfa_bpmaxSLStateChanged(e);
			}		
		});	
		riepfa_distSL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				riepfa_distSLStateChanged(e);
			}		
		});	

		return mainpanel;
	}

	private JPanel createLtgPanel ()
	{
		JPanel mainpanel;
		Box mainbox;
		JPanel npointspanel;
		JPanel maxbkppanel;
		JPanel distpanel;
		JPanel szonePA;
		JPanel bgapPA;
		
		mainpanel = new JPanel(new BorderLayout());
		mainpanel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "LTG Configuration", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		
		// For configuring the maximum break point on riepfa algorithm
		maxbkppanel = new JPanel();
		maxbkppanel.setLayout(new BoxLayout(maxbkppanel, BoxLayout.Y_AXIS));
		
		JPanel mbpanelslider;
		mbpanelslider = new JPanel();
		mbpanelslider.setLayout(new BoxLayout(mbpanelslider, BoxLayout.X_AXIS));
		
		JLabel lbmaxb = new JLabel("Maximum Range");
		lbmaxb.setForeground(new Color(29,14,237));
		ltg_rangeSL = new JSlider(1, 500);
		ltg_rangeLA = new JLabel("___ m");
		ltg_rangeLA.setForeground(new Color(29,14,237));
		ltg_rangeLA.setFont(new Font("values", Font.PLAIN, 12));
				
		maxbkppanel.add(lbmaxb);
		mbpanelslider.add(ltg_rangeSL);
		mbpanelslider.add(ltg_rangeLA);
		maxbkppanel.add(mbpanelslider);
		
		// For configuring the number of minimum points on riepfa algorithm
		npointspanel = new JPanel();
		npointspanel.setLayout(new BoxLayout(npointspanel, BoxLayout.Y_AXIS));
		
		JPanel nppanelslider;
		nppanelslider = new JPanel();
		nppanelslider.setLayout(new BoxLayout(nppanelslider, BoxLayout.X_AXIS));
		
		JLabel lbminp = new JLabel("Filter Window Size");
		lbminp.setForeground(new Color(29,14,237));
		ltg_windowSL = new JSlider(0, 10);
		ltg_windowLA = new JLabel("__ pts");
		ltg_windowLA.setForeground(new Color(29,14,237));
		ltg_windowLA.setFont(new Font("values", Font.PLAIN, 12));
		
		npointspanel.add(lbminp);
		nppanelslider.add(ltg_windowSL);
		nppanelslider.add(ltg_windowLA);
		npointspanel.add(nppanelslider);
		
		// For configuring the fit distance of riepfa algorithm
		distpanel = new JPanel();
		distpanel.setLayout(new BoxLayout(distpanel, BoxLayout.Y_AXIS));
		
		JPanel distpanelslider;
		distpanelslider = new JPanel();
		distpanelslider.setLayout(new BoxLayout(distpanelslider, BoxLayout.X_AXIS));
		
		JLabel lbdist = new JLabel("Dead Band for 1st Derivative");
		lbdist.setForeground(new Color(29,14,237));
		ltg_zeroSL = new JSlider(0, 100);
		ltg_zeroLA = new JLabel("____ m");
		ltg_zeroLA.setForeground(new Color(29,14,237));
		ltg_zeroLA.setFont(new Font("values", Font.PLAIN, 12));
				
		distpanel.add(lbdist);
		distpanelslider.add(ltg_zeroSL);
		distpanelslider.add(ltg_zeroLA);
		distpanel.add(distpanelslider);
		
		// **************************
		szonePA = new JPanel();
		szonePA.setLayout(new BoxLayout(szonePA, BoxLayout.Y_AXIS));
		
		JPanel szoneslPA;
		szoneslPA = new JPanel();
		szoneslPA.setLayout(new BoxLayout(szoneslPA, BoxLayout.X_AXIS));
		
		JLabel szoneLA = new JLabel("Security Distance");
		szoneLA.setForeground(new Color(29,14,237));
		ltg_szoneSL = new JSlider(0, 100);
		ltg_szoneLA = new JLabel("___ m");
		ltg_szoneLA.setForeground(new Color(29,14,237));
		ltg_szoneLA.setFont(new Font("values", Font.PLAIN, 12));
				
		szoneslPA.add(ltg_szoneSL);
		szoneslPA.add(ltg_szoneLA);
		szonePA.add(szoneLA);
		szonePA.add(szoneslPA);

		// **************************
		bgapPA = new JPanel();
		bgapPA.setLayout(new BoxLayout(bgapPA, BoxLayout.Y_AXIS));
		
		JPanel bgapslPA;
		bgapslPA = new JPanel();
		bgapslPA.setLayout(new BoxLayout(bgapslPA, BoxLayout.X_AXIS));
		
		JLabel bgapLA = new JLabel("Minimum point density");
		bgapLA.setForeground(new Color(29,14,237));
		ltg_bgapSL = new JSlider(0, 100);
		ltg_bgapLA = new JLabel("___ m");
		ltg_bgapLA.setForeground(new Color(29,14,237));
		ltg_bgapLA.setFont(new Font("values", Font.PLAIN, 12));
				
		bgapslPA.add(ltg_bgapSL);
		bgapslPA.add(ltg_bgapLA);
		bgapPA.add(bgapLA);
		bgapPA.add(bgapslPA);

		ltg_segsLA = new JLabel("Number of Boundaries: " + 0);
		ltg_segsLA.setForeground(new Color(230,43,43));
		ltg_segsLA.setFont(new Font("Bold", Font.BOLD, 16));

		ltg_debugCB = new JCheckBox ("Show LTG debug window");

		// For layout compnents
		mainbox = new Box(BoxLayout.Y_AXIS);
		mainbox.add(npointspanel);
		mainbox.add(Box.createVerticalStrut(10));
		mainbox.add(maxbkppanel);
		mainbox.add(Box.createVerticalStrut(10));
		mainbox.add(distpanel);
		mainbox.add(Box.createVerticalStrut(10));
		mainbox.add(szonePA);
		mainbox.add(Box.createVerticalStrut(10));
		mainbox.add(bgapPA);
		mainbox.add(Box.createVerticalStrut(10));
		mainbox.add(ltg_segsLA);
		mainbox.add(ltg_debugCB);
		
		mainpanel.add(mainbox);

		ltg_rangeSL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				ltg_rangeSLStateChanged(e);
			}		
		});	
		ltg_windowSL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				ltg_windowSLStateChanged(e);
			}		
		});	
		ltg_zeroSL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				ltg_zeroSLStateChanged(e);
			}		
		});	
		ltg_szoneSL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				ltg_szoneSLStateChanged(e);
			}		
		});	
		ltg_bgapSL.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				ltg_bgapSLStateChanged(e);
			}		
		});	
		ltg_debugCB.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				ltg_debugCBActionPerformed(e);
			}
		});		

		return mainpanel;
	}
	
	private void initialiseLPOs()
	{
		senpos = new SensorPos ();
		senpos.set_xy (0.0, 0.0, 90.0 * Angles.DTOR);

		scanLRF = new LPOSensorScanner (senpos, RAYSCAN, CONESCAN, "Scan", LPO.PERCEPT);
		scanLRF.setMode (LPOSensorScanner.RAYS);
		inviewLRF = new LPOFSegments (LPOSensorFSeg.MAX_SEGS);
		dataLRF = new LPOSensorSignat(1);
		ltgLRF = new LPORangeLTG (RAYSCAN, CONESCAN, RANGESCAN, 0.05, "LTG", LPO.PERCEPT);
		
		mlaser.update (inviewLRF, scanLRF, ltgLRF);
		laserCO.repaint ();

		riepfa_pminSL.setValue(inviewLRF.getNPointRiepfa());
		riepfa_bpmaxSL.setValue(new Double(inviewLRF.getMaxBreakPointRiepfa() * 100.0).intValue());
		riepfa_distSL.setValue(new Double(inviewLRF.getMaxDistRiepfa() * 100.0).intValue());

		ltg_rangeSL.setValue ((int) (ltgLRF.getRangeLRF () * 10));
		ltg_windowSL.setValue (ltgLRF.getFWindow ());
		ltg_zeroSL.setValue ((int) (ltgLRF.getZDeriv () * 100));
		ltg_szoneSL.setValue ((int) (ltgLRF.getSZone () * 100));
		ltg_bgapSL.setValue ((int) (ltgLRF.getMinPoints () * 10));
	}

	public void run ()
	{		
		try 
		{
			laser = Laser.getLaser (modelsCB.getSelectedItem ()+"|"+portsCB.getSelectedItem ());
		} catch (Exception e) { e.printStackTrace (); }
		
		running = (laser != null);	
		while (running)
		{
			// Read laser scanner
			try 
			{
				values = laser.getLaserData();
			} catch (Exception e) { e.printStackTrace (); }
			
			processLaserData (values);
			if (logging)
				saveLaserData (logTF.getText (), values);
			
			try { Thread.sleep (waitSL.getValue ()); } catch (Exception e) { }
		}
	}
	
	private void saveLaserData (String fname, double[] values)
	{
		int				i;
		BufferedWriter	out;
		
		if ((values == null) || (fname == null))		return;
		
		try
		{
			// Appending to file
			out = new BufferedWriter(new FileWriter (fname, true));
			
			out.write (new Integer (RAYSCAN).toString ());
			for(i = 0; i < RAYSCAN; i++)
				out.write (" " + format (values[i], 2));
			out.write("\n");			
			
			out.close();
		} catch (IOException e) { e.printStackTrace(); }
	}

	private void processLaserData (double[] values)
	{
		int i;
		double xx, yy;
		double alpha, delta;
				
		if (values == null)					return;
		
		// Put raw scan
		scanLRF.update (values, true);
		
		// Update LTG
		delta	= CONESCAN / (double) (RAYSCAN - 1);
		alpha	= -CONESCAN / 2.0;		
		
		for (i = 0; i < RAYSCAN; i++, alpha += delta)
			ltgLRF.add_range (senpos, i, values[i], alpha);
		ltgLRF.build_graph ();
		ltgLRF.collision (cursor);
		ltgLRF.active (true);
		
		// Generate Fuzzy Segments
		dataLRF.n (values.length);		
		dataLRF.locate (0.0, 0.0, 0.0);
		dataLRF.active (true);
		
		alpha = 0.0;
		for (i = 0; i < dataLRF.n(); i++)
		{
			xx	= values[i] * Math.cos(alpha);
			yy	= values[i] * Math.sin(alpha);
			
			dataLRF.d[i].locate (xx, yy, 0.0, 82.0);
			
			alpha += Math.toRadians(0.5);
		}
		
		inviewLRF.reset();
		//inviewLRF.build_segments_RIEPFA(dataLRF, P);
		inviewLRF.build_segments_SAM(dataLRF, P, ltgLRF.getInitBoundaries(), ltgLRF.getEndBoundaries(), ltgLRF.getNumBoundaries());
		
		// Update graphic components
		mlaser.update (inviewLRF, scanLRF, ltgLRF);
		laserCO.repaint ();
		
		riepfa_segsLA.setText ("Number of Segments: " + inviewLRF.numseg());
		ltg_segsLA.setText ("Number of Boundaries: " + ltgLRF.boundaries ());
	}

	protected void startBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		if (startBU.isSelected ())
		{
			startBU.setText (STOP_PROC);
			Thread thread = new Thread (this);
			thread.start ();
		}
		else
		{
			running = false;	
			startBU.setText (START_PROC);
		}
	}

	protected void fportBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		int				val;
		
		val		= chooser.showOpenDialog (frame);
		if (val == JFileChooser.APPROVE_OPTION)
		{
			portsCB.addItem (chooser.getSelectedFile ().getPath ());
			portsCB.setSelectedIndex (portsCB.getItemCount () - 1);
			modelsCB.setSelectedIndex (modelsCB.getItemCount () - 1);
		}
	}

	protected void flogBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		int				val;

		val		= chooser.showSaveDialog (frame);
		if (val == JFileChooser.APPROVE_OPTION)
			logTF.setText (chooser.getSelectedFile ().getPath ());
	}

	protected void logBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		if (looplogCB.isSelected ())
		{
			logging = logBU.isSelected ();
			if (logging)
			{
				logBU.setText (STOP_LOG);
				looplogCB.setEnabled (false);
			}
			else
			{
				logBU.setText (START_LOG);
				looplogCB.setEnabled (true);
			}
		}
		else 
		{
			saveLaserData (logTF.getText (), values);
			logBU.setSelected (false);
		}
	}

	protected void looplogCBActionPerformed (java.awt.event.ActionEvent e) 
	{
		if (looplogCB.isSelected ())
			logBU.setText (START_LOG);
		else
			logBU.setText (SINGLE_LOG);
	}

	protected void riepfa_pminSLStateChanged (javax.swing.event.ChangeEvent e) 
	{
		inviewLRF.setNPointRiepfa (riepfa_pminSL.getValue());
		riepfa_pminLA.setText (Integer.toString(riepfa_pminSL.getValue()) + " pts");
	}

	protected void riepfa_bpmaxSLStateChanged (javax.swing.event.ChangeEvent e) 
	{
		double			val;
		
		val = (double) riepfa_bpmaxSL.getValue() * 0.01;
		inviewLRF.setMaxBreakPointRiepfa (val);
		riepfa_bpmaxLA.setText (format (val, 2) + " m");
	}

	protected void riepfa_distSLStateChanged (javax.swing.event.ChangeEvent e) 
	{
		double			val;
		
		val = (double) riepfa_distSL.getValue() * 0.01;
		inviewLRF.setMaxDistRiepfa (val);
		riepfa_distLA.setText (format (val, 2) + " m");
	}

	protected void ltg_rangeSLStateChanged (javax.swing.event.ChangeEvent e) 
	{
		double			val;
		
		val = (double) ltg_rangeSL.getValue() * 0.1;
		ltgLRF.setRangeLRF (val);
		ltg_rangeLA.setText (format (val, 1) + " m");
	}

	protected void ltg_windowSLStateChanged (javax.swing.event.ChangeEvent e) 
	{
		ltgLRF.setFWindow (ltg_windowSL.getValue());
		ltg_windowLA.setText (Integer.toString(ltg_windowSL.getValue()) + " pts");
	}

	protected void ltg_zeroSLStateChanged (javax.swing.event.ChangeEvent e) 
	{
		double			val;
		
		val = (double) ltg_zeroSL.getValue() * 0.01;
		ltgLRF.setZDeriv (val);
		ltg_zeroLA.setText (format (val, 2) + " m");
	}

	protected void ltg_szoneSLStateChanged (javax.swing.event.ChangeEvent e) 
	{
		double			val;
		
		val = (double) ltg_szoneSL.getValue() * 0.01;
		ltgLRF.setSZone (val);
		ltg_szoneLA.setText (format (val, 2) + " m");
	}

	protected void ltg_bgapSLStateChanged (javax.swing.event.ChangeEvent e) 
	{
		double			val;
		
		val = (double) ltg_bgapSL.getValue() * 0.1;
		ltgLRF.setMinPoints (val);
		ltg_bgapLA.setText (format (val, 1) + " m");
	}

	protected void ltg_debugCBActionPerformed (java.awt.event.ActionEvent e) 
	{
		ltgLRF.setDebug (ltg_debugCB.isSelected ());
	}
	
	protected void laserCOMousePressed(java.awt.event.MouseEvent e) 
	{
		if (e.isControlDown () || ((e.getModifiers () & MouseEvent.BUTTON2_MASK) != 0))
			laserCO.setCursor (Cursor.getPredefinedCursor (Cursor.N_RESIZE_CURSOR));
		else
			laserCO.setCursor (Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR));			
		laserCO.mouseDown (e.getX (), e.getY ());
	}

	protected void laserCOMouseDragged(java.awt.event.MouseEvent e) 
	{
		if (e.isControlDown () || ((e.getModifiers () & MouseEvent.BUTTON2_MASK) != 0))
		{
			laserCO.setCursor (Cursor.getPredefinedCursor (Cursor.N_RESIZE_CURSOR));
			laserCO.mouseZoom (e.getX (), e.getY ());
		}
		else
		{
			laserCO.setCursor (Cursor.getPredefinedCursor (Cursor.MOVE_CURSOR));
			laserCO.mousePan (e.getX (), e.getY ());
		}
	}

	protected void laserCOMouseReleased(java.awt.event.MouseEvent e) 
	{
		laserCO.setCursor (Cursor.getPredefinedCursor (Cursor.DEFAULT_CURSOR));
	}

	protected void laserCOMouseMoved(java.awt.event.MouseEvent e) 
	{
		cursor	= laserCO.mouseClick (e.getX (), e.getY ());
		if (!running)
		{
			ltgLRF.collision (cursor);
			
			mlaser.update (inviewLRF, scanLRF, ltgLRF);
			laserCO.repaint ();		
		}
	}

	public static void main(String[] args)
	{
		new LaserWindow();
	}
}
