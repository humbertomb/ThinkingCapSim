/* * (c) 2003-2004 Humberto Martinez */ package tcapps.tcmonitor;import java.awt.*;import java.io.*;import javax.swing.*;import javax.swing.table.*;import tc.*;import wucore.gui.*;import wucore.utils.geom.*;import tcapps.tcsim.*;import tcapps.tcsim.gui.*;import tcapps.tcsim.simul.*;class LoginServers extends AbstractTableModel{	static public final int			NUM_COLUMNS	= 4;	static public final String[]		labels		= { "Group name" , "Address", "Port", "Server ID"};		public String[][]				data;						// Constructors	public LoginServers ()	{		data		= new String[NUM_COLUMNS][0];	}		// Accessors    public String	getColumnName (int col) 			{ return labels[col]; }    public int		getRowCount () 					{ return data[0].length; }    public int		getColumnCount () 				{ return NUM_COLUMNS; }    public boolean	isCellEditable (int row, int col)	{ return false; }        // Instance methods    public void setRows (int rows)    {    		data		= new String [NUM_COLUMNS][rows];    }        public void setValueAt (Object value, int row, int col)     {    		data[col][row]	= (String) value;    }    public Object getValueAt (int row, int col)		    {     		return data[col][row];    }}public class LaunchWindow extends JFrame {	static public final String			TITTLE		= "BGenWeb Login Window";		protected LoginServers				servers;		JPanel cmdsPA = new JPanel();	JPanel buttonsPA = new JPanel();	JPanel serverPA = new JPanel();	JButton ls_stdBU = new JButton();	JButton ls_thsBU = new JButton();	JPanel clientPA = new JPanel();	JButton bg_stdBU = new JButton();	JButton sim_stdBU = new JButton();	JButton real_stdBU = new JButton ();	JPanel customPA = new JPanel ();	JButton miforkBU = new JButton ();	JButton siforkBU = new JButton ();	JButton siboatBU = new JButton ();	JButton spioneerBU = new JButton ();	JPanel jiniPA = new JPanel();	JScrollPane serversSP = new JScrollPane();	JTable serversTB = new JTable();	public LaunchWindow () 	{		try		{			initComponents ();			servers	= new LoginServers ();			serversTB.setModel (servers);			setVisible (true);		} catch (Exception e) { e.printStackTrace (); }	}	public static void main (String argv[])	{		new LaunchWindow ();	}		public void initComponents() throws Exception 	{		cmdsPA.setVisible(true);		cmdsPA.setLayout(new BorderLayout(0, 0));		buttonsPA.setVisible(true);		buttonsPA.setLayout(new GridLayout(1, 2, 0, 0));		serverPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new Color(153, 153, 153), 1, false), "Linda Space Server", 4, 2, new Font("Application", 1, 12), new Color(102, 102, 153)));		serverPA.setVisible(true);		serverPA.setLayout(new GridLayout(5, 1, 0, 0));		ls_stdBU.setVisible(true);		ls_stdBU.setText("Create standard service");		ls_thsBU.setVisible(true);		ls_thsBU.setText("Monitor active threads");		clientPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new Color(153, 153, 153), 1, false), "BGenWeb Client", 4, 2, new Font("Application", 1, 12), new Color(102, 102, 153)));		clientPA.setVisible(true);		clientPA.setLayout(new GridLayout(5, 1, 0, 0));		bg_stdBU.setVisible(true);		bg_stdBU.setText("Open standard client");		sim_stdBU.setVisible(true);		sim_stdBU.setText("Open standard simulator");		real_stdBU.setVisible(true);		real_stdBU.setText("Open real robot");		real_stdBU.setEnabled(false);			// -- HMB 20241003 no es muy practico ene sta distribucion		customPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new Color(153, 153, 153), 1, false), "Custom Executors", 4, 2, new Font("Application", 1, 12), new Color(102, 102, 153)));		customPA.setVisible(true);		customPA.setLayout(new GridLayout(1, 1));		siforkBU.setVisible(true);		siforkBU.setText("Single iFork");		miforkBU.setVisible(true);		miforkBU.setText("Multi iFork");		siboatBU.setVisible(true);		siboatBU.setText("Rasmus");		spioneerBU.setVisible(true);		spioneerBU.setText("Pioneer3-AT");		jiniPA.setBorder(new javax.swing.plaf.BorderUIResource.TitledBorderUIResource(new javax.swing.border.LineBorder(new Color(153, 153, 153), 1, false), "Available JINI Linda Spaces", 4, 2, new Font("Application", 1, 12), new Color(102, 102, 153)));		jiniPA.setVisible(true);		jiniPA.setLayout(new GridLayout(1, 1));		serversSP.setVisible(true);		serversTB.setVisible(true);		setLocation(new Point(0, 22));		getContentPane().setLayout(new GridLayout(2, 1, 0, 0));		setTitle(TITTLE);				setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);		cmdsPA.add(buttonsPA, BorderLayout.CENTER);		cmdsPA.add(customPA, BorderLayout.SOUTH);		buttonsPA.add(serverPA);		buttonsPA.add(clientPA);		serverPA.add(ls_stdBU);		serverPA.add(ls_thsBU);		clientPA.add(bg_stdBU);		clientPA.add(sim_stdBU);		clientPA.add(real_stdBU);		customPA.add(siforkBU);		customPA.add(miforkBU);		customPA.add(siboatBU);		customPA.add(spioneerBU);		jiniPA.add(serversSP);		serversSP.getViewport().add(serversTB);		getContentPane().add(cmdsPA);		getContentPane().add(jiniPA);		setSize(new Dimension(520, 375));		// event handling		ls_stdBU.addActionListener(new java.awt.event.ActionListener() {			public void actionPerformed(java.awt.event.ActionEvent e) {				ls_stdBUActionPerformed(e);			}		});		ls_thsBU.addActionListener(new java.awt.event.ActionListener() {			public void actionPerformed(java.awt.event.ActionEvent e) {				ls_thsBUActionPerformed(e);			}		});		bg_stdBU.addActionListener(new java.awt.event.ActionListener() {			public void actionPerformed(java.awt.event.ActionEvent e) {				bg_stdBUActionPerformed(e);			}		});		sim_stdBU.addActionListener(new java.awt.event.ActionListener() {			public void actionPerformed(java.awt.event.ActionEvent e) {				sim_stdBUActionPerformed(e);			}		});		real_stdBU.addActionListener(new java.awt.event.ActionListener() {			public void actionPerformed(java.awt.event.ActionEvent e) {				real_stdBUActionPerformed(e);			}		});		siforkBU.addActionListener(new java.awt.event.ActionListener() {			public void actionPerformed(java.awt.event.ActionEvent e) {				siforkBUActionPerformed(e);			}		});		miforkBU.addActionListener(new java.awt.event.ActionListener() {			public void actionPerformed(java.awt.event.ActionEvent e) {				miforkBUActionPerformed(e);			}		});		siboatBU.addActionListener(new java.awt.event.ActionListener() {			public void actionPerformed(java.awt.event.ActionEvent e) {				siboatBUActionPerformed(e);			}		});		spioneerBU.addActionListener(new java.awt.event.ActionListener() {			public void actionPerformed(java.awt.event.ActionEvent e) {				spioneerBUActionPerformed(e);			}		});		addWindowListener(new java.awt.event.WindowAdapter() {			public void windowClosing(java.awt.event.WindowEvent e) {				thisWindowClosing(e);			}		});	}    	private boolean mShown = false;  		public void addNotify() {		super.addNotify();				if (mShown)			return;					// resize frame to account for menubar		JMenuBar jMenuBar = getJMenuBar();		if (jMenuBar != null) {			int jMenuBarHeight = jMenuBar.getPreferredSize().height;			Dimension dimension = getSize();			dimension.height += jMenuBarHeight;			setSize(dimension);						// move down components in layered pane			Component[] components = getLayeredPane().getComponentsInLayer(JLayeredPane.DEFAULT_LAYER.intValue());			for (int i = 0; i < components.length; i++) {				Point location = components[i].getLocation();				location.move(location.x, location.y + jMenuBarHeight);				components[i].setLocation(location);			}		}		mShown = true;	}	// Close the window when the close box is clicked	void thisWindowClosing (java.awt.event.WindowEvent e) 	{		attemptQuit ();	}	public void updateServices ()	{/*		servers.setRows (client.num);				for (int i = 0; i < client.num; i++)		{			servers.setValueAt (client.groups[i], i, 0);			servers.setValueAt (client.addrs[i].getHostAddress (), i, 1);			servers.setValueAt (new Integer (client.ports[i]).toString (), i, 2);			servers.setValueAt (client.ids[i].toString (), i, 3);		}			servers.fireTableDataChanged ();*/	}			public void ls_stdBUActionPerformed(java.awt.event.ActionEvent e) 	{		new ExecArchMulti ("conf/archs/glinda.arch").start ();	}			public void ls_thsBUActionPerformed(java.awt.event.ActionEvent e) 	{		ThreadsWindow			thwin;				thwin	= new ThreadsWindow ();		thwin.start ();	}		public void bg_stdBUActionPerformed(java.awt.event.ActionEvent e) 	{		new ExecArchMulti ("conf/archs/gmon.arch").start ();	}			public void sim_stdBUActionPerformed(java.awt.event.ActionEvent e) 	{		if(SimulatorWindow.isJ3DInstalled())			new tcapps.tcsim.gui.SimulatorWindow ();	}			public void real_stdBUActionPerformed(java.awt.event.ActionEvent e) 	{		new ExecArch ("PIONEER3-AT", "conf/archs/pioneer3.arch").start ();			}			public void siforkBUActionPerformed(java.awt.event.ActionEvent ev) 	{		Simulator		simul;		ExecArchSim		exec;				// Create linda space and open monitor		new ExecArchMulti ("conf/archs/glinda.arch").start ();		new ExecArchMulti ("conf/archs/gmon.arch").start ();				// Create simulador and open an iFork instance		try { Thread.sleep (2000); } catch (Exception e) { }		simul	= new Simulator ();				if(SimulatorWindow.isJ3DInstalled())			new SimulatorWindow(simul,SimulatorWindow.ADMIN);		new ExecArchMultiPallet("."+File.separator+"conf"+File.separator+"pallet"+File.separator+"pallet.arch","."+File.separator+"conf"+File.separator+"pallet"+File.separator+"typepallet.cfg",simul).start();				exec		= new ExecArchSim ("IFORK-1", "conf/archs/ifork.arch", null, simul);		exec.start ();				exec.setStart (new Point3 (75.0, 65.0, Math.PI));	}			public void miforkBUActionPerformed(java.awt.event.ActionEvent ev) 	{		Simulator		simul;		ExecArchSim		exec;				// Create linda space and open monitor		new ExecArchMulti ("conf/archs/glinda.arch").start ();		new ExecArchMulti ("conf/archs/gmon.arch").start ();				// Create simulador and open some iFork instances		try { Thread.sleep (2000); } catch (Exception e) { }		simul	= new Simulator ();				if(SimulatorWindow.isJ3DInstalled())			new SimulatorWindow(simul,SimulatorWindow.ADMIN);		new ExecArchMultiPallet("."+File.separator+"conf"+File.separator+"pallet"+File.separator+"pallet.arch","."+File.separator+"conf"+File.separator+"pallet"+File.separator+"typepallet.cfg",simul).start();						exec		= new ExecArchSim ("IFORK-1", "conf/archs/ifork.arch", null, simul);		exec.start ();				exec.setStart (new Point3 (73.0, 77.0, Math.PI));		try { Thread.sleep (2000); } catch (Exception e) { }				exec		= new ExecArchSim ("IFORK-2", "conf/archs/ifork.arch", null, simul);		exec.start ();		exec.setStart (new Point3 (73.0, 73.0, Math.PI));		try { Thread.sleep (2000); } catch (Exception e) { }				exec		= new ExecArchSim ("IFORK-3", "conf/archs/ifork.arch", null, simul);		exec.start ();		exec.setStart (new Point3 (73.0, 70.0, Math.PI));		try { Thread.sleep (2000); } catch (Exception e) { }				exec		= new ExecArchSim ("IFORK-4", "conf/archs/ifork.arch", null, simul);		exec.start ();		exec.setStart (new Point3 (73.0, 67.0, Math.PI));	}			public void siboatBUActionPerformed(java.awt.event.ActionEvent ev) 	{		Simulator		simul;		ExecArchSim		exec;				// Create linda space and open monitor		new ExecArchMulti ("conf/archs/glinda.arch").start ();		new ExecArchMulti ("conf/archs/gmon.arch").start ();				// Create simulador and open an iFork instance		try { Thread.sleep (2000); } catch (Exception e) { }		simul	= new Simulator ();				exec = new ExecArchSim ("RASMUS", "conf/archs/rasmus.arch", null, simul);		exec.start ();				exec.setStart (new Point3 (0.0, 0.0, Math.PI));	}			public void spioneerBUActionPerformed(java.awt.event.ActionEvent ev) 	{		Simulator		simul;		ExecArchSim		exec;				// Create linda space and open monitor		new ExecArchMulti ("conf/archs/glinda.arch").start ();		new ExecArchMulti ("conf/archs/gmon.arch").start ();				// Create simulador and open an iFork instance		try { Thread.sleep (2000); } catch (Exception e) { }		simul	= new Simulator ();			if (SimulatorWindow.isJ3DInstalled ())			new SimulatorWindow (simul, SimulatorWindow.ADMIN);		exec = new ExecArchSim ("PIONEER3-AT", "conf/archs/pioneer3.arch", null, simul);//		exec = new ExecArchSim ("QUAKY2", "conf/archs/quaky2.arch", null, simul);		exec.start ();	}			/***************************/	/** OSApplication Methods **/	/***************************/	public void attemptQuit ()	{		if (JOptionPane.showConfirmDialog (this, "This action will stop all the services. Do you really want to quit?", "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)		{			setVisible (false);			dispose ();						System.exit (0);		}		else			setVisible (true);	}}		