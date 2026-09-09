/**
 * @version March 16, 2006
 * @author Humberto Martinez Barbera
 */

package tclib.navigation.localisation.outdoor.gui;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.comm.*;
import javax.swing.*;

import devices.data.*;
import devices.drivers.ins.*;
import devices.drivers.gps.*;
import devices.gui.*;

import devices.pos.*;
import wucore.widgets.*;
import tclib.navigation.localisation.outdoor.visualization.*;
import tclib.navigation.localisation.outdoor.*;

public class PositioningWindow extends Object implements Runnable
{
	static public final String 		START_PROC	= "Start Processing";
	static public final String 		STOP_PROC	= "Stop Processing ";
	static public final String 		START_LOG	= "Start Logging";
	static public final String 		STOP_LOG	= "Stop Logging ";
	static public final String 		REAL_MODE	= "From Sensor";
	static public final String 		LOG_MODE	= "From Logger";

	static public final String 		NO_PORT		= "---";
	static public final String 		DEF_DIR		= "logs";
	static public final String 		DEF_FILE	= "insgps.log";
	static public final String 		DEF_FILTER_FILE	= "kalman.log";
	static public final boolean		AUTODETECT	= true;

	// Sensor reading and logging
	protected InsData				insdata;
	protected PollINS				insth;
	protected GPSData				gpsdata;
	protected SatelliteData[] 		sats;			
	protected PollGPS				gpsth;
	protected CompassData			cmpdata;
	protected PollFile				fileth;
	protected PollNet				fileth2;
	protected BufferedWriter		logfile;
	protected long					stime;
	protected long					GPSref;
	protected boolean				running		= false;
	protected FilterFactory			kloc;		// Kalman based position correction filter
	protected BufferedWriter 		output;	
	protected Position				pos = new Position ();;		// Current position (GPS corrected)
	protected UTMPos				posfilter = new UTMPos();
	
	// Linda data sharing
	protected PositioningProducer	lprod;

	// GUI
	protected JFrame 				frame;
	protected JToggleButton 		logBU;
	protected JButton	 			clearBU;
	protected JToggleButton 		modeBU;
	protected JToggleButton 		startBU;
	protected JComboBox 			insportsCB;
	protected JComboBox 			insmodelsCB;
	protected JTextField 			insdtTF;
	protected JComboBox 			gpsportsCB;
	protected JComboBox 			gpsmodelsCB;
	protected JComboBox			filtermodelsCB;
	protected JTextField			filterlogTF;
	protected JButton				filterlogBU;
	protected JTextField 			gpsdtTF;
	protected JTextField 			logTF;
	protected JFileChooser 		chooser;
	protected String 				curfpath;
	protected GPSDataPanel 		gpsdPA;
	protected InsDataPanel 		insdPA;
	protected CompassDataPanel 	cmpdPA;
	protected FilterDataPanel	    filterdPA;
	protected JTextField 			laddrTF;
	protected JTextField 			lportTF;
	protected JButton	 			connectBU;
	
	// Own Widgets
	protected Component2D			pathC2D;
	protected Position2D			pathP2D;
	
	// Inner classes
	class PollGPS extends Thread
	{
		protected GPS				dev;
		protected long				dtime;
		protected boolean			running = true;
		
		// Constructors
		public PollGPS (String driver, String port, long dtime)
		{
			try { this.dev = GPS.getGPS (driver+"|"+port); } catch (Exception e) { e.printStackTrace (); }
			this.dtime	= dtime;
		}

		public void halt ()
		{
			running = false;
		}
		
		public void run () 
		{
			long	ctime;
			
		    while (running)
		    {
		    		try { gpsdata = dev.getData (); } catch (Exception e) { e.printStackTrace (); }
		    		
		    		if (gpsdata != null)
		    		{
			    		ctime	= (int) (System.currentTimeMillis () - stime);
			    		
			    		if (logfile != null)
			    			logData ("GPS", gpsdata.toDatalog(), ctime);

			    		kloc.update (gpsdata, ctime);
		    			kloc.filter (ctime);
		    			pos.set (kloc.getPosition ());

		    			GPSref++;
						if (GPSref > 1)
		    			try {
		    				output.write(ctime + "," + pos.x() + "," + pos.y() + "," + pos.y() + "," + insdata.getRoll()+","+insdata.getPitch()+","+insdata.getYaw()+"\r\n");
		    			} 
		    			catch (IOException e1) {
		    				e1.printStackTrace();
		    			}
		    		}
		    		try { sats = dev.getSatellites(); } catch (Exception e) { e.printStackTrace (); }
		    				    		
		    		try { Thread.sleep (dtime); } catch (Exception e) { }
				Thread.yield ();
			}
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		    try { dev.close (); } catch (Exception e) { e.printStackTrace (); }
		}
	}

	class PollINS extends Thread
	{
		protected Ins				dev;
		protected long				dtime;
		protected boolean			running = true;
		
		// Constructors
		public PollINS (String driver, String port, long dtime)
		{
			try { this.dev = Ins.getIns (driver+"|"+port); } catch (Exception e) { e.printStackTrace (); }
			this.dtime	= dtime;
		}

		public void halt ()
		{
			running = false;
		}
		
		public void run () 
		{ 
			long	ctime;
						
		    while (running)
		    {
		    		try { insdata = dev.getData (); } catch (Exception e) { e.printStackTrace (); }

		    		if (insdata != null)
		    		{
			    		ctime	= (int) (System.currentTimeMillis () - stime);
			    		
			    		if (logfile != null)
			    			logData ("INS", insdata.toDatalog(), ctime);

		    			kloc.update (insdata, ctime);
		    			kloc.filter (ctime);
		    			pos.set (kloc.getPosition ());
						if (GPSref > 1)
		    			try {
		    				output.write(ctime + "," + pos.x() + "," + pos.y() + "," + pos.y() + "," + insdata.getRoll()+","+insdata.getPitch()+","+insdata.getYaw()+"\r\n");
		    			} 
		    			catch (IOException e1) {
		    				e1.printStackTrace();
		    			}

		    		}
		    		
		    		try { Thread.sleep (dtime); } catch (Exception e) { }
				Thread.yield ();
			}
				
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try { dev.close (); } catch (Exception e) { e.printStackTrace (); }
		}
	}
	
	class PollFile extends Thread
	{
		protected BufferedReader		reader;
		protected boolean			running = true;

		public PollFile (String name)
		{
			try
			{
				reader	= new BufferedReader (new FileReader (name));		
			} catch (Exception e) { e.printStackTrace(); }
		}
		
		public void halt ()
		{
			running = false;
		}
		
		public void run ()
		{
			int				i;
			long				ptime, ctime;
			StringTokenizer	st;
			String			sensor;
			double[]		inslog = new double[InsData.DATALOG];
			double[]		gpslog = new double[GPSData.DATALOG];
			double[]		cmplog = new double[CompassData.DATALOG];
			boolean			firstime = true;

			if (reader == null)			return;
			
			ptime = 0;
			try
			{
				while (reader.ready () && running)
				{
					st	= new StringTokenizer (reader.readLine (), " ");
					
					// Wait for time
					ctime = Integer.parseInt (st.nextToken ());
					if (!firstime)
						try { Thread.sleep (ctime - ptime); } catch (Exception e) { e.printStackTrace(); }
					else
						firstime = false;
					ptime = ctime;
					
					// Parse data
					sensor = st.nextToken ();
					if (sensor.equals ("INS"))
					{
						for (i = 0; i < InsData.DATALOG; i++)
							inslog[i] = Double.parseDouble (st.nextToken ());
						
						if (insdata == null)			insdata = new InsData ();
						insdata.fromDatalog (inslog);
		    			kloc.update (insdata, ctime);
		    			kloc.filter (ctime);
		    			pos.set (kloc.getPosition ());
		    		}
					else if (sensor.equals ("GPS"))
					{
						for (i = 0; i < GPSData.DATALOG; i++)
							gpslog[i] = Double.parseDouble (st.nextToken ());
						
						if (gpsdata == null)			gpsdata = new GPSData ();
						gpsdata.fromDatalog (gpslog);
			    		kloc.update (gpsdata, ctime);
		    			kloc.filter (ctime);
		    			GPSref++;
					}
					else if (sensor.equals ("CMP"))
					{
						for (i = 0; i < CompassData.DATALOG; i++)
							cmplog[i] = Double.parseDouble (st.nextToken ());
						
						if (cmpdata == null)			cmpdata = new CompassData ();
						cmpdata.fromDatalog (cmplog);
			    		kloc.update (cmpdata, ctime);
		    			kloc.filter (ctime);
					}
					else
						System.out.println ("--ParseFile Unrecognised sensor type <"+sensor+">");

					if (GPSref > 1)
					try {
	    				output.write(ctime + "," + pos.x() + "," + pos.y() + "," + pos.y() + "," + insdata.getRoll()+","+insdata.getPitch()+","+insdata.getYaw()+"\r\n");
	    			} 
	    			catch (IOException e1) {
	    				e1.printStackTrace();
	    			}
				}
				reader.close ();
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	class PollNet extends Thread
	{
		protected BufferedReader		reader;
		protected boolean running = true;
		Socket socket;

		InetAddress address;
		int port;

		int datapacketsize = 256;
		byte[] datapacket = new byte[datapacketsize];

		public PollNet (InetAddress address, int port)
		{
			this.address = address;
			this.port = port;

			try{
				socket = new Socket(address, port);
				//socket.connect(new InetSocketAddress(address, port));
				socket.setReceiveBufferSize(256000);
				socket.setSoTimeout(2000);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try
			{
				reader	= new BufferedReader (new InputStreamReader (socket.getInputStream()));
			} catch (Exception e) { e.printStackTrace(); }
		}
		
		public void halt ()
		{
			running = false;
		}
		
		public void run ()
		{
			int			i;
			long			ctime;
			StringTokenizer	st;
			String			sensor;
			double[]		inslog = new double[InsData.DATALOG];
			double[]		gpslog = new double[GPSData.DATALOG];
			double[]		cmplog = new double[CompassData.DATALOG];

			if (reader == null)			return;
			
			try
			{
				//while (reader.ready () && running)
				while (running)
				{
					
					st	= new StringTokenizer (reader.readLine (), " ");
		
					// Wait for time
					
					ctime = Integer.parseInt (st.nextToken ());
					
					// Parse data
					sensor = st.nextToken ();
					if (sensor.equals ("INS"))
					{
						for (i = 0; i < InsData.DATALOG; i++)
							inslog[i] = Double.parseDouble (st.nextToken ());
						
						if (insdata == null)			insdata = new InsData ();
						insdata.fromDatalog (inslog);
		    			kloc.update (insdata, ctime);
		    			kloc.filter (ctime);
		    			pos.set (kloc.getPosition ());
			    		if (logfile != null)
			    			logData ("INS", insdata.toDatalog(), ctime);
					}
					else if (sensor.equals ("GPS"))
					{
						for (i = 0; i < GPSData.DATALOG; i++)
							gpslog[i] = Double.parseDouble (st.nextToken ());
						
						if (gpsdata == null)			gpsdata = new GPSData ();
						gpsdata.fromDatalog (gpslog);
			    		kloc.update (gpsdata, ctime);
		    			kloc.filter (ctime);
			    		if (logfile != null)
			    			logData ("GPS", gpsdata.toDatalog(), ctime);
			    		GPSref++;
					}
					else if (sensor.equals ("CMP"))
					{
						for (i = 0; i < CompassData.DATALOG; i++)
							cmplog[i] = Double.parseDouble (st.nextToken ());
						
						if (cmpdata == null)			cmpdata = new CompassData ();
						cmpdata.fromDatalog (cmplog);
			    		kloc.update (cmpdata, ctime);
		    			kloc.filter (ctime);
		    			if (logfile != null)
			    			logData ("CMP", cmpdata.toDatalog(), ctime);
					}
					else
						System.out.println ("--ParseFile Unrecognised sensor type <"+sensor+">");

					if (GPSref > 1)
						try {
							output.write(ctime + "," + pos.x() + "," + pos.y() + "," + pos.y() + "," + insdata.getRoll()+","+insdata.getPitch()+","+insdata.getYaw()+"\r\n");
						} 
	    				catch (IOException e1) {
	    					e1.printStackTrace();
	    				}
				}
				reader.close ();
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (Exception e) { e.printStackTrace(); }
		}
	}

	
	public PositioningWindow()
	{
		
		JPanel sensorsPA;
		JPanel insPA, gpsPA, pathPA, bottomPA, filterPA, cmpPA;

		// Set up default directories
		chooser = new JFileChooser (System.getProperty ("user.dir") + File.separator + DEF_DIR);

		// Create custom components		
		gpsdPA		= new GPSDataPanel ();
		gpsPA		= new JPanel ();
		gpsPA.setLayout (new GridLayout(1, 1));
		gpsPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "GPS Device", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		gpsPA.add (gpsdPA);
		insdPA		= new InsDataPanel ();
		insPA		= new JPanel ();
		insPA.setLayout (new GridLayout(1, 1));
		insPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "INS Device", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		insPA.add (insdPA);
		cmpdPA		= new CompassDataPanel ("Compass");
		cmpPA		= new JPanel ();
		cmpPA.setLayout (new GridLayout(1, 1));
		cmpPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Compass Device", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		cmpPA.add (cmpdPA);
		
		filterdPA 	= new FilterDataPanel ();
		filterPA 	= new JPanel ();
		filterPA.setLayout (new GridLayout(1,1));
		filterPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Filter", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		filterPA.add (filterdPA);
		
		sensorsPA	= new JPanel ();
		sensorsPA.setLayout (new GridLayout(1,2));
		sensorsPA.add (gpsPA);
		sensorsPA.add (insPA);
		
		// Create configuration panel
		pathC2D		= new Component2D ();
		pathP2D		= new Position2D (pathC2D);
		pathPA		= new JPanel ();
		pathPA.setLayout (new GridLayout(1, 1));
		pathPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Paths", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		pathPA.add (pathC2D);
		
		bottomPA	= new JPanel ();
		bottomPA.setLayout (new GridLayout(1, 2));
		bottomPA.add (cmpPA);
		bottomPA.add (filterPA);
		
		
		// Create the frame
		frame = new JFrame("GPS + INS Localisation Tool");
		frame.setLocation (new Point (0, 0));
		frame.setSize (new Dimension (1024, 650));
		frame.getContentPane().add(createToolsPanel (), BorderLayout.NORTH);
		frame.getContentPane().add(sensorsPA, BorderLayout.WEST);
		frame.getContentPane().add(pathPA, BorderLayout.CENTER);
		frame.getContentPane().add(bottomPA, BorderLayout.SOUTH);
		
		// Add component listeners
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) { System.exit(0); }
		});
		
		frame.setVisible(true);
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
		JPanel insdevPA;
		JLabel insLA;
		JPanel gpsdevPA;
		JLabel gpsLA;
		JPanel logPA;
		JLabel filterLA;
		JPanel filterPA;
		JButton flogBU;

		// Create INS commands panel
		insportsCB = new JComboBox ();
		insportsCB.addItem (NO_PORT);
		insportsCB.addItem ("/dev/ttyS0");

		insmodelsCB = new JComboBox ();
		insmodelsCB.addItem ("devices.drivers.ins.xsens.XSens");
		insmodelsCB.addItem ("devices.drivers.ins.crossbow.Crossbow");
		insdtTF = new JTextField ("100");
		
		insLA	= new JLabel ("INS   ");
		insLA.setFont (new Font ("Application", 1, 12));
		insLA.setForeground (new Color (102, 102, 153));
		insdevPA = new JPanel ();
		insdevPA.setLayout (new BoxLayout (insdevPA, BoxLayout.X_AXIS));
		insdevPA.add (insLA);
		insdevPA.add (new JLabel ("Port"));
		insdevPA.add (insportsCB);
		insdevPA.add (new JLabel ("Driver"));
		insdevPA.add (insmodelsCB);
		insdevPA.add (new JLabel ("Polling"));
		insdevPA.add (insdtTF);
		
		// Create GPS commands panel
		gpsportsCB = new JComboBox ();
		gpsportsCB.addItem (NO_PORT);
		gpsportsCB.addItem ("/dev/ttyS0");
		gpsmodelsCB = new JComboBox ();
		gpsmodelsCB.addItem ("devices.drivers.gps.Garmin.Garmin");
		gpsmodelsCB.addItem ("devices.drivers.gps.NMEA0183.NMEA0183");
		gpsmodelsCB.addItem ("devices.drivers.gps.Trimble.Trimble");
		gpsdtTF = new JTextField ("1000");
		
		gpsLA	= new JLabel ("GPS   ");
		gpsLA.setFont (new Font ("Application", 1, 12));
		gpsLA.setForeground (new Color (102, 102, 153));
		gpsdevPA = new JPanel ();
		gpsdevPA.setLayout (new BoxLayout (gpsdevPA, BoxLayout.X_AXIS));
		gpsdevPA.add (gpsLA);
		gpsdevPA.add (new JLabel ("Port"));
		gpsdevPA.add (gpsportsCB);
		gpsdevPA.add (new JLabel ("Driver"));
		gpsdevPA.add (gpsmodelsCB);
		gpsdevPA.add (new JLabel ("Polling"));
		gpsdevPA.add (gpsdtTF);
		
		// Create filter commands panel
		filtermodelsCB = new JComboBox ();
		filtermodelsCB.addItem ("tclib.navigation.localisation.outdoor.KalmanFusion");
		filtermodelsCB.addItem ("tclib.navigation.localisation.outdoor.KalmanFilter");
		filtermodelsCB.addItem ("tclib.navigation.localisation.outdoor.ExtKalmanFilter");
		filtermodelsCB.addItem ("tclib.navigation.localisation.outdoor.TripleKalmanFilter");
		filterlogBU = new JButton ("Set Log File");
		filterlogTF = new JTextField (DEF_FILTER_FILE);
		
		filterLA	= new JLabel ("Filter   ");
		filterLA.setFont (new Font ("Application", 1, 12));
		filterLA.setForeground (new Color (102, 102, 153));
		filterPA 	= new JPanel ();
		filterPA.setLayout (new BoxLayout (filterPA, BoxLayout.X_AXIS));
		filterPA.add (filterLA);
		filterPA.add (new JLabel ("Type:"));
		filterPA.add (filtermodelsCB);
		filterPA.add (filterlogBU);
		filterPA.add (filterlogTF);
		
		// Detect available serial ports
		if (AUTODETECT)
			try
			{
				CommPortIdentifier portId;
				Enumeration en = CommPortIdentifier.getPortIdentifiers();
				while (en.hasMoreElements())
				{
					portId = (CommPortIdentifier) en.nextElement();
					if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL)
					{
						insportsCB.addItem(portId.getName());
						gpsportsCB.addItem(portId.getName());
					}
				}
			} catch(Exception e) { }

		// Create log panel
		startBU = new JToggleButton (START_PROC);
		modeBU = new JToggleButton (REAL_MODE);
		logBU = new JToggleButton (START_LOG);
		flogBU = new JButton ("Set Log File");
		clearBU = new JButton ("Clear Log");
		logTF = new JTextField (DEF_FILE);
		laddrTF = new JTextField ("localhost");
		lportTF = new JTextField ("5500");
		connectBU = new JButton ("Connect");
		
		logPA = new JPanel ();
		logPA.setLayout (new BoxLayout (logPA, BoxLayout.X_AXIS));
		logPA.add (flogBU);
		logPA.add (logTF);
		logPA.add (logBU);
		logPA.add (clearBU);
		logPA.add(Box.createHorizontalStrut (15));
		logPA.add (new JSeparator (JSeparator.VERTICAL));
		logPA.add(Box.createHorizontalStrut (15));
		logPA.add (new JLabel ("Address"));
		logPA.add (laddrTF);
		logPA.add (new JLabel ("Port"));
		logPA.add (lportTF);
		logPA.add (connectBU);
		logPA.add(Box.createHorizontalStrut (15));
		logPA.add (new JSeparator (JSeparator.VERTICAL));
		logPA.add(Box.createHorizontalStrut (15));
		logPA.add (modeBU);
		logPA.add (startBU);
		
		// Create main panel
		topPA = new JPanel ();
		topPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, false), "Execution and Log Control", 4, 2, new java.awt.Font("Application", 1, 12), new java.awt.Color(102, 102, 153)));
		topPA.setLayout (new BoxLayout (topPA, BoxLayout.Y_AXIS));
		topPA.add (insdevPA);
		topPA.add (new JSeparator ());
		topPA.add (gpsdevPA);
		topPA.add (new JSeparator ());
		topPA.add (filterPA);
		topPA.add (new JSeparator ());
		topPA.add (logPA);

		startBU.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				startBUActionPerformed(e);
			}
		});		
		flogBU.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				flogBUActionPerformed(e);
			}
		});	
		filterlogBU.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				filterlogBUActionPerformed(e);
			}
		});	
		clearBU.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				clearBUActionPerformed(e);
			}
		});		
		logBU.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				logBUActionPerformed(e);
			}
		});		
		modeBU.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				modeBUActionPerformed(e);
			}
		});	
		connectBU.addActionListener(new java.awt.event.ActionListener() {
			public void  actionPerformed(java.awt.event.ActionEvent e) {
				connectBUActionPerformed(e);
			}
		});	

		return topPA;
	}
	
	public void run ()
	{			
		long			ctime, ptime;
		
		running = (insth != null) || (gpsth != null) || (fileth != null) || (fileth2 != null);
		ptime = System.currentTimeMillis ();
		while (running)
		{
			if (insdata != null)
				insdPA.update (insdata);
			if (gpsdata != null){
				gpsdPA.update (gpsdata, sats);
			}
			if (cmpdata != null)
				cmpdPA.update (cmpdata);
			ctime = System.currentTimeMillis ();

			filterdPA.update(insdata, pos);
			
			if (lprod != null)
				lprod.setPositionData (gpsdata, insdata, (int) (ctime - ptime));

			if (GPSref > 1){
				// Update GIS map
				pathP2D.autoscale (false);
				//pathP2D.autocenter (true);
				posfilter.setNorth(kloc.getPosition().y());
				posfilter.setEast(kloc.getPosition().x());
				//posfilter.setAltitude(kloc.getPosition().z());
				pathP2D.update (posfilter);		
				pathC2D.repaint ();
			}

			try { Thread.sleep (100); } catch (Exception e) { }
			Thread.yield ();
			
			ptime = ctime;
		}
	}
		
	private void openDevices ()
	{		
		insdata = null;
		gpsdata = null;
		insth = null;
		gpsth = null;
		stime = System.currentTimeMillis ();

		// Initialise positioning filter
		String fclass = filtermodelsCB.getSelectedItem().toString();
		kloc		= new FilterFactory (fclass);
		try	{
			output	= new BufferedWriter(new FileWriter ( filterlogTF.getText()));
		} catch (Exception e) { e.printStackTrace (); }

		try 
		{
			if (!insportsCB.getSelectedItem ().equals (NO_PORT))
			{
				insth = new PollINS ((String) insmodelsCB.getSelectedItem (), (String) insportsCB.getSelectedItem (), 100);
				insth.start ();
			}
			if (!gpsportsCB.getSelectedItem ().equals (NO_PORT))
			{
				gpsth = new PollGPS ((String) gpsmodelsCB.getSelectedItem (), (String) gpsportsCB.getSelectedItem (), 1000);
				gpsth.start ();
			}
		} catch (Exception e) { e.printStackTrace (); }
		
		Thread thread = new Thread (this);
		thread.start ();
	}
	
	private void closeDevices ()
	{
		if (insth != null)
			insth.halt ();
		if (gpsth != null)
			gpsth.halt ();
		
		running = false;
	}
	
	private void openFile ()
	{		
		insdata = null;
		gpsdata = null;
		fileth = null;
		stime = System.currentTimeMillis ();

		// Initialise positioning filter
		String fclass = filtermodelsCB.getSelectedItem().toString();
		kloc		= new FilterFactory (fclass);
		try	{
			output	= new BufferedWriter(new FileWriter (filterlogTF.getText()));
		} catch (Exception e) { e.printStackTrace (); }

		try 
		{
			fileth = new PollFile (logTF.getText ());
			fileth.start ();
		} catch (Exception e) { e.printStackTrace (); }
		

		Thread thread = new Thread (this);
		thread.start ();
	}
	
	private void closeFile ()
	{
		if (fileth != null)
			fileth.halt ();
		
		running = false;
	}
	
	private synchronized void logData (String preffix, double[] data, long time)
	{
		int			i;
		
		try
		{
			logfile.write (new Long(time).toString () + " " + preffix);
			for (i = 0; i < data.length; i++)
				logfile.write (" " + data[i]);
			logfile.write("\n");
			logfile.flush ();
		} catch (Exception e) { e.printStackTrace(); }
	}

	protected void modeBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		if (modeBU.isSelected ())
		{
			modeBU.setText (LOG_MODE);
		}
		else
		{
			modeBU.setText (REAL_MODE);
		}
	}
	
	protected void startBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		if (startBU.isSelected ())
		{
			startBU.setText (STOP_PROC);
			if (modeBU.isSelected ())
				openFile ();
			else
				openDevices ();
		}
		else
		{
			if (modeBU.isSelected ())
				closeFile ();
			else
				closeDevices ();
			startBU.setText (START_PROC);
		}
	}

	protected void fportBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		int				val;
		
		val		= chooser.showOpenDialog (frame);
		if (val == JFileChooser.APPROVE_OPTION)
		{
			insportsCB.addItem (chooser.getSelectedFile ().getPath ());
			insportsCB.setSelectedIndex (insportsCB.getItemCount () - 1);
			insmodelsCB.setSelectedIndex (insmodelsCB.getItemCount () - 1);
		}
	}

	protected void flogBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		int				val;

		val		= chooser.showSaveDialog (frame);
		if (val == JFileChooser.APPROVE_OPTION)
			logTF.setText (chooser.getSelectedFile ().getPath ());
	}

	protected void filterlogBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		int				val;

		val		= chooser.showSaveDialog (frame);
		if (val == JFileChooser.APPROVE_OPTION)
			filterlogTF.setText (chooser.getSelectedFile ().getPath ());
	}

	protected void logBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		if (logBU.isSelected ())
		{
			clearBU.setEnabled (false);
			logBU.setText (STOP_LOG);
			try
			{
				logfile = new BufferedWriter (new FileWriter (logTF.getText (), true));
			} catch (Exception ex) { ex.printStackTrace (); logfile = null; }
		}
		else
		{
			clearBU.setEnabled (true);
			logBU.setText (START_LOG);
			try
			{
				logfile.flush ();
				logfile.close ();
			} catch (Exception ex) { }
			logfile = null;
		}
	}

	protected void clearBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		try
		{
			logfile = new BufferedWriter (new FileWriter (logTF.getText (), false));
			logfile.close ();
			logfile = null; 
		} catch (Exception ex) { ex.printStackTrace (); }
	}

	protected void connectBUActionPerformed (java.awt.event.ActionEvent e) 
	{
		//connectBU.setEnabled (false);
		//lprod = PositioningProducer.createProducer (laddrTF.getText(), lportTF.getText());

		if (connectBU.getText().equals("Connect")){
			connectBU.setText("Disconnect");
			
			insdata = null;
			gpsdata = null;
			fileth = null;
			stime = System.currentTimeMillis ();
	
			// Initialise positioning filter
			String fclass = filtermodelsCB.getSelectedItem().toString();
			kloc		= new FilterFactory (fclass);
	
			try	{
				output	= new BufferedWriter(new FileWriter (filterlogTF.getText()));
			} catch (Exception ex) { ex.printStackTrace (); }
	
			try 
			{
				fileth2 = new PollNet (InetAddress.getByName(laddrTF.getText()), new Integer(lportTF.getText()).intValue());
				fileth2.start ();
			} catch (Exception ex) { ex.printStackTrace (); }
			
	
			Thread thread = new Thread (this);
			thread.start ();
		}
		else{
			connectBU.setText("Connect");			
			fileth2.halt();
		}
	}

	public static void main(String[] args)
	{
		new PositioningWindow();
	}
}
