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

import tclib.navigation.mapbuilding.visualization.*;

import tc.shared.lps.lpo.*;
import tc.vrobot.*;

import wucore.widgets.*;
import wucore.utils.geom.*;
import wucore.utils.math.*;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */

public class LaserMeasureWindow extends Object implements Runnable
{
	public static int 			RAYSCAN		= 361;
	public static double		CONESCAN	= 180.0 * Angles.DTOR;
	public static double		RANGESCAN	= 7.0;
	
	static public final String 	START_PROC	= "Start Processing";
	static public final String 	STOP_PROC	= "Stop Processing ";
	static public final String 	START_LOG	= "Start Logging";
	static public final String 	STOP_LOG	= "Stop Logging ";
	static public final String 	SINGLE_LOG	= " Log Data  ";

	static public final String 	DEF_DIR		= "logs";
	static public final String 	DEF_FILE	= "laserscan.log";
	static public final boolean AUTOPORTDETECT = true;

	Laser laser;
	double[] values;
	SensorPos senpos;
	LPOSensorScanner scanLRF;
	boolean running = false;
	
	// Laser visualization
	Component2D laserCO;
	Scanner2D mlaser;
	Point2 cursor;
		
	JTextField angle;
	JTextField posx;
	JTextField posy;
	JTextField posz;
	
	// Tools
	JFrame frame;
	JToggleButton logBU;
	JToggleButton startBU;
	JComboBox portsCB;
	JComboBox modelsCB;
	JTextField logTF;
	JSlider waitSL;
	JFileChooser chooser;
	String curfpath;
		
	public LaserMeasureWindow()
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
		configPA.add (createCtrlPanel ());
				
		// Create the frame
		frame = new JFrame("Laser Scanner Measurement Tool");
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
		portsCB.addItem ("/dev/tty.USA1962P1.1");
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
		
		logPA = new JPanel ();
		logPA.setLayout (new BoxLayout (logPA, BoxLayout.X_AXIS));
		logPA.add (flogBU);
		logPA.add (logTF);
		logPA.add (logBU);
		//logPA.add (looplogCB);
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

		return topPA;
	}

	private JPanel createCtrlPanel ()
	{
		JPanel mainpanel;
		JPanel row1, row2, row3, row4;
		
		angle = new JTextField ("0");
		posx = new JTextField ("0");
		posy = new JTextField ("0");
		posz = new JTextField ("0");
		
		row1 = new JPanel ();
		row1.setLayout(new BoxLayout (row1, BoxLayout.X_AXIS));
		row1.add (new JLabel ("Angle"));
		row1.add (angle);
		row1.add (new JLabel ("deg"));
		
		row2 = new JPanel ();
		row2.setLayout(new BoxLayout (row2, BoxLayout.X_AXIS));
		row2.add (new JLabel ("Pos X"));
		row2.add (posx);
		row2.add (new JLabel ("m"));
		
		row3 = new JPanel ();
		row3.setLayout(new BoxLayout (row3, BoxLayout.X_AXIS));
		row3.add (new JLabel ("Pos Y"));
		row3.add (posy);
		row3.add (new JLabel ("m"));
		
		row4 = new JPanel ();
		row4.setLayout(new BoxLayout (row4, BoxLayout.X_AXIS));
		row4.add (new JLabel ("Pos Z"));
		row4.add (posz);
		row4.add (new JLabel ("m"));
		
		mainpanel = new JPanel();
		mainpanel.setLayout (new BoxLayout (mainpanel, BoxLayout.Y_AXIS));
		mainpanel.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Scan Parameters", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		mainpanel.add (row1);
		mainpanel.add (row2);
		mainpanel.add (row3);
		mainpanel.add (row4);
		mainpanel.add (new JLabel ("- - - - - - - - - - - - - -"));

		return mainpanel;
	}
		
	private void initialiseLPOs()
	{
		senpos = new SensorPos ();
		senpos.set_xy (0.0, 0.0, 90.0 * Angles.DTOR);

		scanLRF = new LPOSensorScanner (senpos, RAYSCAN, CONESCAN, "Scan", LPO.PERCEPT);
		scanLRF.setMode (LPOSensorScanner.RAYS);
		
		mlaser.update (null, scanLRF, null);
		laserCO.repaint ();
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
//			if (logging)
//				saveLaserData (logTF.getText (), values);
			
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
			
			out.write (posx.getText()+" ");
			out.write (posy.getText()+" ");
			out.write (posz.getText()+" ");
			out.write (angle.getText()+" ");
			out.write (new Integer (RAYSCAN).toString ());
			for(i = 0; i < RAYSCAN; i++)
				out.write (" " + format (values[i], 2));
			out.write("\n");			
			
			out.close();
		} catch (IOException e) { e.printStackTrace(); }
	}

	private void processLaserData (double[] values)
	{
		if (values == null)					return;
		
		// Put raw scan
		scanLRF.update (values, true);
								
		// Update graphic components
		mlaser.update (null, scanLRF, null);
		laserCO.repaint ();
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
		saveLaserData (logTF.getText (), values);
		logBU.setSelected (false);
	}

	protected void looplogCBActionPerformed (java.awt.event.ActionEvent e) 
	{
		logBU.setText (SINGLE_LOG);
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
			mlaser.update (null, scanLRF, null);
			laserCO.repaint ();		
		}
	}

	public static void main(String[] args)
	{
		new LaserMeasureWindow();
	}
}
