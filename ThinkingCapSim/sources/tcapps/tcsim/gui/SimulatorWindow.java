/*
 * (c) 2002 Humberto Martinez Barbera
 * (c) 2002-2003 Bernardo Canovas Segura
 * (c) 2004 Humberto Martinez Barbera
 */
 
package tcapps.tcsim.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Properties;

import javax.media.j3d.Canvas3D;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.plaf.BorderUIResource;

import tc.shared.world.World;
import tc.vrobot.RobotData;
import tc.vrobot.RobotDesc;
import tcapps.tcsim.ExecArchMultiPallet;
import tcapps.tcsim.ExecArchReplay;
import tcapps.tcsim.ExecArchSim;
import tcapps.tcsim.gui.visualization.Model3D;
import tcapps.tcsim.gui.visualization.Scene3D;
import tcapps.tcsim.simul.Simulator;
import tcapps.tcsim.simul.SimulatorDesc;
import tcapps.tcsim.simul.objects.SimObject;
import wucore.utils.geom.Point3;

import com.sun.j3d.utils.universe.SimpleUniverse;

public class SimulatorWindow extends JFrame
{
	public static final int 		ADMIN = 0;
	public static final int 		CLIENT = 1;
	// member declarations
	protected JToolBar 				optionsTB		= new JToolBar();
	protected JTabbedPane 			optionsTP		= new JTabbedPane(SwingConstants.TOP);
	
	// ***************************
	// 3D VISUALIZATION
	// ***************************
	
	protected Canvas3D				canvas3d;
	protected Model3D				model3d;
	protected int					vmode			= Scene3D.M_MOVE;

	// ***************************
	// TOOLS
	// ***************************

	protected JButton 				moveBU			= new JButton ();
	protected JButton 				rotateBU			= new JButton ();
	protected JButton 				zoomBU			= new JButton ();
	protected JButton 				movieBU			= new JButton ();
	protected JButton 				pictureBU		= new JButton ();
	protected JButton 				printBU			= new JButton ();

	// ***************************
	// VISUALIZATION OPTIONS
	// ***************************

	// Lighting panel
	protected JCheckBox 				lightFrontCB		= new JCheckBox();
	protected JScrollBar 				lightFrontSB		= new JScrollBar(JScrollBar.HORIZONTAL);
	protected JCheckBox 				lightBackCB		= new JCheckBox();
	protected JScrollBar 				lightBackSB		= new JScrollBar(JScrollBar.HORIZONTAL);
	protected JCheckBox 				lightTopCB		= new JCheckBox();
	protected JScrollBar 				lightTopSB		= new JScrollBar(JScrollBar.HORIZONTAL);
	protected JCheckBox 				lightAmbientCB	= new JCheckBox();
	protected JScrollBar 				lightAmbientSB	= new JScrollBar(JScrollBar.HORIZONTAL);

	// Sensor displaying panel
	protected JCheckBox 				showIrCB			= new JCheckBox();
	protected JCheckBox 				showSonarCB		= new JCheckBox();
	protected JCheckBox 				showLaserCB		= new JCheckBox();

	// Object focusing panel
	protected JCheckBox 				focusOnRobotCB	= new JCheckBox();
	protected JCheckBox 				focusOnObjectCB	= new JCheckBox();
	protected JComboBox 				focusOnRobotCO	= new JComboBox();
	protected JComboBox 				focusOnObjectCO	= new JComboBox();

	// ***************************
	// EXECUTION OPTIONS
	// ***************************

	protected JTextField 				robotTF			= new JTextField();
	protected JTextField 				worldTF			= new JTextField();
	protected JTextField 				sceneTF			= new JTextField();
	protected JComboBox 				robotCombo		= new JComboBox();
	protected JComboBox 				worldCombo		= new JComboBox();
	protected JComboBox 				sceneCombo		= new JComboBox();
	protected JButton 				playBU			= new JButton();
	protected JButton 				replayBU			= new JButton();
	protected JButton 				worldBU			= new JButton();
	protected JButton 				sceneBU			= new JButton();

	// ***************************
	// SIMULATION STUFF
	// ***************************

	protected Properties				pdefs;
	protected Simulator				simulator;
	protected World 					map;
	protected boolean				man_pallet;

	public SimulatorWindow ()
	{
		this (new Simulator(),null,ADMIN);
	}
	
	public SimulatorWindow(Simulator simulator,int mode){
		this(simulator,null,mode);
	}
	public SimulatorWindow (Properties pdefs){
		this(new Simulator(),pdefs,ADMIN);
	}
	
	public SimulatorWindow(Simulator simulator,Properties pdefs,int mode){
		this.pdefs	= pdefs;
			
		try { initComponents (mode); } catch (Exception e) { e.printStackTrace (); }

		this.simulator = simulator;
//		simulator = new Simulator ();	
		simulator.setVisualization (this);
		populateCombos ();
		
		setVisible (true);
		
		showIrCB.setSelected(model3d.isIrActivated());
		showSonarCB.setSelected(model3d.isSonarActivated());
		showLaserCB.setSelected(model3d.isLaserActivated());
		lightFrontSB.setValue((int)(100.0f*model3d.getFrontLightIntensity()));
		lightBackSB.setValue((int)(100.0f*model3d.getBackLightIntensity()));		
		lightAmbientSB.setValue((int)(100.0f*model3d.getAmbientLightIntensity()));	
	}
	

  	public void initComponents(int mode) throws Exception
  	{
		setLocation(new Point(10, 200));
		setTitle("Simulated 3D World");
		setFont(new Font("Dialog", 0, 12));
		getContentPane().setLayout(new BorderLayout());
		setSize(new Dimension(700, 700));

		optionsTP.addTab ("Execution", createExecutionOpts ());
		optionsTP.addTab ("Visualization", createVisualizationOpts ());
		optionsTB.setLayout (new GridLayout (1,1));
		optionsTB.add(optionsTP);
		optionsTB.setVisible((mode==ADMIN?true:false));
		
		getContentPane().add(optionsTB, BorderLayout.NORTH);
		getContentPane().add(create3DPanel (), BorderLayout.CENTER);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				thisWindowClosing(e);
			}
		});
 	}
  
  	protected JPanel createVisualizationOpts ()
  	{
 		JPanel		panel = new JPanel ();
 		JPanel 		lightPanel = new JPanel();
 		JPanel 		sensorPanel = new JPanel();
 		JPanel 		focusPanel = new JPanel();
		
		showIrCB.setText("Show IR sensor cones");
		showSonarCB.setText("Show sonar sensor cones");
		showLaserCB.setText("Show laser range scanner");

		lightFrontCB.setText("Front light");
		lightFrontCB.setSelected(true);
		lightBackCB.setText("Back light");
		lightBackCB.setSelected(true);
		lightTopCB.setText("Top light");
		lightTopCB.setSelected(true);
		lightAmbientCB.setText("Ambient light");
		lightAmbientCB.setSelected(true);

		lightFrontSB.setMinimum(0);
		lightFrontSB.setValue((int)(100.0f*Scene3D.FRONT_INT));
		lightFrontSB.setMaximum(100);
		lightFrontSB.setUnitIncrement(1);

		lightBackSB.setMinimum(0);
		lightBackSB.setValue((int)(100.0f*Scene3D.BACK_INT));
		lightBackSB.setMaximum(100);
		lightBackSB.setUnitIncrement(1);

		lightTopSB.setMinimum(0);
		lightTopSB.setValue((int)(100.0f*Scene3D.TOP_INT));
		lightTopSB.setMaximum(100);
		lightTopSB.setUnitIncrement(1);

		lightAmbientSB.setMinimum(0);
		lightAmbientSB.setValue((int)(100.0f*Scene3D.AMBIENT_INT));
		lightAmbientSB.setMaximum(100);
		lightAmbientSB.setUnitIncrement(1);

		focusOnRobotCB.setEnabled(false);
		focusOnRobotCB.setText("Focus on robot ID");
		focusOnRobotCB.setSelected(false);

		focusOnObjectCB.setEnabled(false);
		focusOnObjectCB.setText("Focus on object ID");
		focusOnObjectCB.setSelected(false);

		focusOnRobotCO.setEnabled(false);
		focusOnObjectCO.setEnabled(false);

		lightPanel.setBorder(new BorderUIResource.TitledBorderUIResource(new LineBorder(new Color(153, 153, 153), 1, false), "Lighting Options", 4, 2, new Font("Application", 1, 12), new Color(102, 102, 153)));
		lightPanel.setLayout(new GridLayout(4,2));

		sensorPanel.setBorder(new BorderUIResource.TitledBorderUIResource(new LineBorder(new Color(153, 153, 153), 1, false), "Sensor Visualization", 4, 2, new Font("Application", 1, 12), new Color(102, 102, 153)));
		sensorPanel.setLayout(new GridLayout(3,1));

		focusPanel.setBorder(new BorderUIResource.TitledBorderUIResource(new LineBorder(new Color(153, 153, 153), 1, false), "Object Focus Control", 4, 2, new Font("Application", 1, 12), new Color(102, 102, 153)));
		focusPanel.setLayout(new GridLayout(4,1));

		lightPanel.add(lightFrontCB);
		lightPanel.add(lightFrontSB);
		lightPanel.add(lightBackCB);
		lightPanel.add(lightBackSB);
		lightPanel.add(lightTopCB);
		lightPanel.add(lightTopSB);
		lightPanel.add(lightAmbientCB);
		lightPanel.add(lightAmbientSB);

		sensorPanel.add(showIrCB);
		sensorPanel.add(showSonarCB);
		sensorPanel.add(showLaserCB);

		focusPanel.add(focusOnRobotCB);
		focusPanel.add(focusOnRobotCO);
		focusPanel.add(focusOnObjectCB);
		focusPanel.add(focusOnObjectCO);

		showIrCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showIrCBActionPerformed(e);
			}
		});
		showSonarCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showSonarCBActionPerformed(e);
			}
		});		
		showLaserCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showLaserCBActionPerformed(e);
			}
		});		
		lightFrontCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lightsActionPerformed(e);
			}
		});		
		lightBackCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lightsActionPerformed(e);
			}
		});		
		lightTopCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lightsActionPerformed(e);
			}
		});		
		lightAmbientCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lightsActionPerformed(e);
			}
		});			
		lightFrontSB.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				lightsAdjustmentValueChanged(e);
			}
		});
		lightBackSB.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				lightsAdjustmentValueChanged(e);
			}
		});
		lightTopSB.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				lightsAdjustmentValueChanged(e);
			}
		});
		lightAmbientSB.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				lightsAdjustmentValueChanged(e);
			}
		});
		focusOnRobotCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				focusOnRobotCBActionPerformed(e);
			}
		});	
		focusOnRobotCO.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				focusOnRobotComboActionPerformed(e);
			}
		});	
		focusOnObjectCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				focusOnObjectCBActionPerformed(e);
			}
		});	
		focusOnObjectCO.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				focusOnObjectComboActionPerformed(e);
			}
		});	
		
		panel.setLayout(new GridLayout(1,3));
		panel.add(lightPanel);
		panel.add(sensorPanel);
		panel.add(focusPanel);

		return panel;
  	}

  	protected JPanel createExecutionOpts ()
  	{
 		JPanel		panel = new JPanel ();
		JPanel 		scenarioPA = new JPanel();	
		JPanel 		robotPA = new JPanel();
		JPanel 		worldPA = new JPanel();	
		
		// Robot panel		
		robotTF.setVisible(true);
		robotTF.setText ("Robotillo-1");	
		
		robotCombo.setVisible(true);
		
		playBU.setVisible(true);
		playBU.setText("Simulate Robot");
		
		replayBU.setVisible(true);
		replayBU.setText("Replay Robot");	
		
		JPanel robotButPanel = new JPanel();
		robotButPanel.setLayout(new GridLayout(2,1));
		robotButPanel.add(playBU);
		robotButPanel.add(replayBU);
		
		robotPA.setLayout(new GridLayout(3,1));
		robotPA.setBorder(new BorderUIResource.TitledBorderUIResource(new LineBorder(new Color(153, 153, 153), 1, false), "Robot Simulation", 4, 2, new Font("Application", 1, 12), new Color(102, 102, 153)));
		robotPA.add(robotTF);
		robotPA.add(robotCombo);
		robotPA.add(robotButPanel);
		
		// World panel		
		worldTF.setVisible(true);
		worldTF.setEditable(false);
		
		worldCombo.setVisible(true);
		
		worldBU.setVisible(true);
		worldBU.setText("Load World");
		
		worldPA.setLayout(new GridLayout(3,1));		
		worldPA.setBorder(new BorderUIResource.TitledBorderUIResource(new LineBorder(new Color(153, 153, 153), 1, false), "World Simulation", 4, 2, new Font("Application", 1, 12), new Color(102, 102, 153)));
		worldPA.add(worldTF);
		worldPA.add(worldCombo);
		worldPA.add(worldBU);
		
		// Scenario panel		
		sceneTF.setVisible(true);
		sceneTF.setEditable(false);
		
		sceneCombo.setVisible(true);
		
		sceneBU.setVisible(true);
		sceneBU.setText("Load Scene");
		
		scenarioPA.setLayout(new GridLayout(3,1));
		scenarioPA.setBorder(new BorderUIResource.TitledBorderUIResource(new LineBorder(new Color(153, 153, 153), 1, false), "Scenario Simulation", 4, 2, new Font("Application", 1, 12), new Color(102, 102, 153)));
		scenarioPA.add(sceneTF);
		scenarioPA.add(sceneCombo);
		scenarioPA.add(sceneBU);
						
		// Main panel
		panel.setLayout(new GridLayout(1,3));
		panel.add(robotPA);
		panel.add(worldPA);
		panel.add(scenarioPA);
		
		// event handling
		playBU.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				playBUActionPerformed(e);
			}
		});	
		replayBU.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				replayBUActionPerformed(e);
			}
		});	
		worldBU.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				worldBUActionPerformed(e);
			}
		});
		sceneBU.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				sceneBUActionPerformed(e);
			}
		});
		
 		return panel;
  	}
  	
  	protected JToolBar createToolsPanel ()
  	{
  		JToolBar		toolbar = new JToolBar (JToolBar.VERTICAL);
  		ImageIcon	moveIC = new ImageIcon (getToolkit().getImage (ClassLoader.getSystemResource ("tcapps/tcsim/gui/icons/move.gif")));
 		ImageIcon	rotateIC = new ImageIcon (getToolkit().getImage (ClassLoader.getSystemResource ("tcapps/tcsim/gui/icons/rotate.gif")));
 		ImageIcon	zoomIC = new ImageIcon (getToolkit().getImage (ClassLoader.getSystemResource ("tcapps/tcsim/gui/icons/zoom.gif")));
 		ImageIcon	movieIC = new ImageIcon (getToolkit().getImage (ClassLoader.getSystemResource ("tcapps/tcsim/gui/icons/movie.gif")));
 		ImageIcon	pictureIC = new ImageIcon (getToolkit().getImage (ClassLoader.getSystemResource ("tcapps/tcsim/gui/icons/picture.gif")));
 		ImageIcon	printIC = new ImageIcon (getToolkit().getImage (ClassLoader.getSystemResource ("tcapps/tcsim/gui/icons/print.gif")));
  		
  	    moveBU.setToolTipText("Move 3D View");
  	    moveBU.setIcon (moveIC);
  	    moveBU.setSelected (true);
  
  	    rotateBU.setToolTipText("Rotate 3D View");
  	    rotateBU.setIcon (rotateIC);

  	    zoomBU.setToolTipText("Zoom 3D View");
  	    zoomBU.setIcon (zoomIC);

  	    movieBU.setEnabled (false);
 	    movieBU.setToolTipText("Create a movie");
 	    movieBU.setIcon (movieIC);

 	    pictureBU.setEnabled (false);
 	    pictureBU.setToolTipText("Create a snapshoot with current scene");
 	    pictureBU.setIcon (pictureIC);

  	    printBU.setEnabled (false);
  	    printBU.setToolTipText("Print current scene");
  	    printBU.setIcon (printIC);

  	    toolbar.setFloatable (false);
  	    toolbar.setRollover (true);
  		toolbar.add (moveBU);
 		toolbar.add (rotateBU);
 		toolbar.add (zoomBU);
 		toolbar.add (movieBU);
 		toolbar.add (pictureBU);
 		toolbar.add (printBU);

 		moveBU.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveBUActionPerformed(e);
			}
		});	
 		rotateBU.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rotateBUActionPerformed(e);
			}
		});	
 		zoomBU.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				zoomBUActionPerformed(e);
			}
		});			
 		movieBU.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				movieBUActionPerformed(e);
			}
		});	
 		pictureBU.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pictureBUActionPerformed(e);
			}
		});	
 		printBU.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				printBUActionPerformed(e);
			}
		});	

  		return toolbar;
  	}
  	
  	protected JPanel create3DPanel ()
  	{
 		JPanel		panel = new JPanel ();
		canvas3d = new Canvas3D (SimpleUniverse.getPreferredConfiguration ());
		model3d = new Model3D(canvas3d);
		if (map!=null) model3d.addMap(map);
		
		panel.setLayout (new BorderLayout (0, 0));
		panel.add (canvas3d, BorderLayout.CENTER);
		panel.add(createToolsPanel (),BorderLayout.WEST);

		canvas3d.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				canvasMousePressed(e);
			}
		});
		canvas3d.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				canvasMouseDragged(e);
			}
		});
		canvas3d.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				
				if(e.isActionKey()){
					switch(e.getKeyCode()){
					case 37:	model3d.keypress(Scene3D.M_MOVE,-Scene3D.KEYMOVE,0); //cursor izquierda    
						break;
					case 39:	model3d.keypress(Scene3D.M_MOVE,Scene3D.KEYMOVE,0); //cursor derecha 
						break;
					case 38: 	model3d.keypress(Scene3D.M_MOVE,0,Scene3D.KEYMOVE); //cursor arriba
						break;
					case 40: 	model3d.keypress(Scene3D.M_MOVE,0,-Scene3D.KEYMOVE); //cursor abajo
						break;
					}	
				}else{
					switch(e.getKeyCode()){
					case 65:	model3d.keypress(Scene3D.M_MOVE,-Scene3D.KEYMOVE,0); //'a' izquierda    
						break;
					case 68:	model3d.keypress(Scene3D.M_MOVE,Scene3D.KEYMOVE,0); //'d' derecha 
						break;
					case 87: 	model3d.keypress(Scene3D.M_MOVE,0,Scene3D.KEYMOVE); //'w' arriba
						break;
					case 83: 	model3d.keypress(Scene3D.M_MOVE,0,-Scene3D.KEYMOVE); //'s' abajo
						break;
						
					case 82:	model3d.keypress(Scene3D.M_ZOOM,0,-Scene3D.KEYZOOM); //'r' zoom adelante    
						break;
					case 70:	model3d.keypress(Scene3D.M_ZOOM,0,Scene3D.KEYZOOM); //'f' zoom atras 
						break;
					
					case 71:	model3d.keypress(Scene3D.M_ROTATE,-Scene3D.KEYROTATE,0); //'g' rotar izquierda    
						break;
					case 72:	model3d.keypress(Scene3D.M_ROTATE,0,Scene3D.KEYROTATE); //'h' rotar abajo 
						break;
					case 74: 	model3d.keypress(Scene3D.M_ROTATE,Scene3D.KEYROTATE,0); //'j' rotar derecha
						break;
					case 89: 	model3d.keypress(Scene3D.M_ROTATE,0,-Scene3D.KEYROTATE); //'y' rotar arriba
						break;
					}
				}
			}
		});
		
 		return panel;
  	}
  	
	protected void populateCombos ()
	{
		int i;
		File dir;
		String files[];
		
		FilenameFilter filter = new FilenameFilter ()
		{
			public boolean accept (File dir, String name)
			{
				if (name.endsWith (".world")) return true;
				return false;
			}
		};
		
		
		dir = new File ("./conf/maps/");
		files = dir.list (filter);
		
		worldCombo.addItem ("none");
		worldCombo.setSelectedItem ("none");
		for (i=0; i < files.length; i++)
			worldCombo.addItem (files[i]);
		
		//worldCombo.setSelectedItem("dulzem3d.world");
		
		filter = new FilenameFilter ()
		{
			public boolean accept (File dir, String name)
			{
				if (name.endsWith (".arch")) return true;
				return false;
			}
		};
		
		dir = new File ("./conf/archs/");
		files = dir.list (filter);
		
		robotCombo.addItem ("none");
		robotCombo.setSelectedItem ("none");
		for (i=0; i < files.length; i++)
			robotCombo.addItem (files[i]);
		
		//robotCombo.setSelectedItem("ifork.arch");
		
		
		
		filter = new FilenameFilter ()
		{
			public boolean accept (File dir, String name)
			{
				if (name.endsWith (".scn")) return true;
				return false;
			}
		};
		
		dir = new File ("./conf/scenes/");
		files = dir.list (filter);
		
		sceneCombo.addItem ("none");
		sceneCombo.setSelectedItem ("none");
		for (i=0; i < files.length; i++)
			sceneCombo.addItem (files[i]);
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

	// Close the window when the close box is clicked
	protected void thisWindowClosing(WindowEvent e)
	{
		setVisible(false);
		dispose();
	}
	
	protected void playBUActionPerformed(ActionEvent e) 
	{
		String			filename = null;
		JFileChooser	chooser;
		int				code;
		
		if (robotCombo.getSelectedItem ().equals ("none"))
		{
			chooser	= new JFileChooser ();
			chooser.setCurrentDirectory (new File ("./conf/archs/"));
			chooser.setDialogTitle ("Load arch file");
			code	= chooser.showOpenDialog (this);
			
			if (code == JFileChooser.APPROVE_OPTION) 
				filename = chooser.getCurrentDirectory().getPath() + File.separator + chooser.getSelectedFile().getName();
		}
		else
			filename = "." + File.separator + "conf" + File.separator + "archs" + File.separator + (String)robotCombo.getSelectedItem();
		
		if (filename != null)
		{
			new ExecArchSim (robotTF.getText(), filename, pdefs, simulator).start ();
			worldTF.setText (simulator.getWorldName());
		}
		if(!man_pallet){
			System.out.println("SimulatorWindow: lanza ExecArchMultiPallet");
			new ExecArchMultiPallet("."+File.separator+"conf"+File.separator+"pallet"+File.separator+"pallet.arch","."+File.separator+"conf"+File.separator+"pallet"+File.separator+"typepallet.cfg",simulator).start();
			man_pallet=true;
		}
	}
	
	protected void replayBUActionPerformed(ActionEvent e) 
	{
		String			filename = null;
		JFileChooser	chooser;
		int				code;
		
		if (robotCombo.getSelectedItem ().equals ("none"))
		{
			chooser	= new JFileChooser ();
			chooser.setCurrentDirectory (new File ("./conf/archs/"));
			chooser.setDialogTitle ("Load arch file");
			code	= chooser.showOpenDialog (this);
			
			if (code == JFileChooser.APPROVE_OPTION) 
				filename = chooser.getCurrentDirectory().getPath() + File.separator + chooser.getSelectedFile().getName();
		}
		else
			filename = "." + File.separator + "conf" + File.separator + "archs" + File.separator + (String)robotCombo.getSelectedItem();
		
		if (filename != null)
		{
			new ExecArchReplay (robotTF.getText(),filename,simulator).start ();
			worldTF.setText (simulator.getWorldName());
		}
	}
	
	protected void worldBUActionPerformed(ActionEvent e) 
	{
		String			filename = null;
		String			mapname = null;
		JFileChooser	chooser;
		int				code;
		
		if (worldCombo.getSelectedItem ().equals ("none"))
		{
			chooser	= new JFileChooser ();
			chooser.setCurrentDirectory (new File ("./conf/maps/"));
			chooser.setDialogTitle ("Load world file");
			code	= chooser.showOpenDialog (this);
			
			if (code == JFileChooser.APPROVE_OPTION) 
			{
				filename = chooser.getCurrentDirectory().getPath() + File.separator + chooser.getSelectedFile().getName();
				mapname = chooser.getSelectedFile().getName();
			}
		}
		else
		{
			filename = "." + File.separator + "conf" + File.separator + "maps" + File.separator + (String)worldCombo.getSelectedItem();
			mapname = (String)worldCombo.getSelectedItem();
		}
		
		if (simulator != null && filename != null)
		{
			simulator.setWorld (filename);
			worldTF.setText (mapname);
			
			sceneTF.setText ("");
			sceneCombo.setSelectedItem ("none");
		}
	}
	
	protected void sceneBUActionPerformed(ActionEvent e) 
	{
		String			filename = null;
		String			scenename = null;
		JFileChooser	chooser;
		int				code;
		
		if (sceneCombo.getSelectedItem ().equals ("none"))
		{
			chooser	= new JFileChooser ();
			chooser.setCurrentDirectory (new File ("./conf/scenes/"));
			chooser.setDialogTitle ("Load scene file");
			code	= chooser.showOpenDialog (this);
			
			if (code == JFileChooser.APPROVE_OPTION) 
			{
				filename = chooser.getCurrentDirectory().getPath() + File.separator + chooser.getSelectedFile().getName();
				scenename = chooser.getSelectedFile().getName();
			}
		}
		else
		{
			filename = "." + File.separator + "conf" + File.separator + "scenes" + File.separator + (String)sceneCombo.getSelectedItem();
			scenename = (String)sceneCombo.getSelectedItem();
		}
		
		if (simulator != null && filename != null)
		{
			try
			{
				simulator.setScene (filename);
				sceneTF.setText (scenename);
			} catch (Exception exc) { exc.printStackTrace (); }
		}
	}

	protected void moveBUActionPerformed(ActionEvent e) 
	{
		vmode	= Scene3D.M_MOVE;
		moveBU.setSelected (true);
		rotateBU.setSelected (false);
		zoomBU.setSelected (false);
	}
	
	protected void rotateBUActionPerformed(ActionEvent e) 
	{
		vmode	= Scene3D.M_ROTATE;
		moveBU.setSelected (false);
		rotateBU.setSelected (true);
		zoomBU.setSelected (false);
	}
	
	protected void zoomBUActionPerformed(ActionEvent e) 
	{
		vmode	= Scene3D.M_ZOOM;
		moveBU.setSelected (false);
		rotateBU.setSelected (false);
		zoomBU.setSelected (true);
	}
	
	protected void movieBUActionPerformed(ActionEvent e) 
	{
	}
	
	protected void pictureBUActionPerformed(ActionEvent e) 
	{
	}
	
	protected void printBUActionPerformed(ActionEvent e) 
	{
	}
	
	protected void canvasMouseDragged(MouseEvent e)
	{
		model3d.mouseDrag (vmode, e.getX (), e.getY ());
	}
	
	protected void canvasMousePressed(MouseEvent e)
	{
		model3d.mouseDown (e.getX (), e.getY ());
	}

	protected void showIrCBActionPerformed(ActionEvent e)
	{
		model3d.showIr(showIrCB.isSelected());
	}

	protected void showSonarCBActionPerformed(ActionEvent e)
	{
		model3d.showSonar(showSonarCB.isSelected());
	}

	protected void showLaserCBActionPerformed(ActionEvent e)
	{
		model3d.showLaser(showLaserCB.isSelected());
	}

	protected void lightsActionPerformed(ActionEvent e)
	{
		model3d.enableAmbientLight(lightAmbientCB.isSelected());
		model3d.enableFrontLight(lightFrontCB.isSelected());
		model3d.enableBackLight(lightBackCB.isSelected());
		model3d.enableTopLight(lightTopCB.isSelected());
	}

	protected void lightsAdjustmentValueChanged (AdjustmentEvent e)
	{ 
		float intensity;

		intensity = ((float)lightBackSB.getValue())/100.0f;
		model3d.setBackLightIntensity(intensity);

		intensity = ((float)lightFrontSB.getValue())/100.0f;
		model3d.setFrontLightIntensity(intensity);

		intensity = ((float)lightTopSB.getValue())/100.0f;
		model3d.setTopLightIntensity(intensity);

		intensity = ((float)lightAmbientSB.getValue())/100.0f;
		model3d.setAmbientLightIntensity(intensity);
	}

	protected void focusOnRobotCBActionPerformed(ActionEvent e)
	{
		if (model3d.focusedRobot()!=-1)
		{
			model3d.focusOnRobot(-1);
			focusOnRobotCB.setSelected(false);
		}
		else
		{
			if (model3d.focusedObject()!=-1)
			{
				model3d.focusOnObject(-1);
				focusOnObjectCB.setSelected(false);
			}
			model3d.focusOnRobot(((Integer)focusOnRobotCO.getSelectedItem()).intValue());
			focusOnRobotCB.setSelected(true);
		}
		model3d.setViewpoint ();
	}

	protected void focusOnRobotComboActionPerformed(ActionEvent e)
	{
		if (model3d.focusedRobot()!=-1)
			model3d.focusOnRobot(((Integer)focusOnRobotCO.getSelectedItem()).intValue());
		model3d.setViewpoint ();
	}
	
	protected void focusOnObjectCBActionPerformed(ActionEvent e)
	{
		if (model3d.focusedObject()!=-1)
		{
			model3d.focusOnObject(-1);
			focusOnObjectCB.setSelected(false);
		}
		else
		{
			if (model3d.focusedRobot()!=-1)
			{
				model3d.focusOnRobot(-1);
				focusOnRobotCB.setSelected(false);
			}
			model3d.focusOnObject(((Integer)focusOnObjectCO.getSelectedItem()).intValue());
			focusOnObjectCB.setSelected(true);
		}	
		model3d.setViewpoint ();
	}

	protected void focusOnObjectComboActionPerformed(ActionEvent e)
	{
		if (model3d.focusedObject()!=-1)
			 model3d.focusOnObject(((Integer)focusOnObjectCO.getSelectedItem()).intValue());
		model3d.setViewpoint ();
	}
	
	public void setWorldmap (World map)
	{ 
		this.map = map;
		
		if (model3d!=null) 
		{
			model3d.addMap (map); 
			model3d.setViewpoint ();
		}
	}

	/** Adds a new object to the world 3D representation 
      * @returns index of the new object in the world. Must be used to update its data
      */	
	public int addObject (SimObject object)
	{
		return addObject(object,new Point3(),0.0);
	}
	public int addObject (SimObject object,Point3 pos,double a)
	{
		int id;
		id = model3d.addObject(object, pos, a);
		model3d.setViewpoint ();

		if (!focusOnObjectCB.isEnabled())
		{
			focusOnObjectCB.setEnabled(true);
			focusOnObjectCO.setEnabled(true);
		}
		focusOnObjectCO.addItem(new Integer(id));
		return id;
	}
	/** Remove the object with index id from the world 3D representation */
	public void removeObject(int id){
		if(model3d!=null) model3d.removeObject(id);
		model3d.setViewpoint();
		
		focusOnObjectCO.removeItem(new Integer(id));
		
		if(focusOnObjectCO.getComponentCount()<1){
			focusOnObjectCB.setEnabled(false);
			focusOnObjectCO.setEnabled(false);
		}
		
	}

	/** Removes all objects (not robots) that the scene contains */
	public void removeAllObjects()
	{
		if (model3d!=null) model3d.removeAllObjects();

		focusOnObjectCB.setEnabled(false);
		focusOnObjectCO.setEnabled(false);
		focusOnObjectCO.removeAllItems();
	}

	/** Adds a new robot to the world 3D representation 
      * @returns index of the new robot in the world. Must be used to update its data
      */	
	public int addRobot (RobotDesc robot, SimulatorDesc sdesc)
	{
		int id;
		id = model3d.addRobot(robot,sdesc);
		model3d.setViewpoint ();

		if (!focusOnRobotCB.isEnabled())
		{
			focusOnRobotCB.setEnabled(true);
			focusOnRobotCO.setEnabled(true);
		}
		focusOnRobotCO.addItem(new Integer(id));

		return id;
	}

	/** Updates the specified robot position using the new data available */
	public void updateData (int roboindex, RobotData data)
	{
		if (model3d != null)		model3d.updateRobot (roboindex, data);
	}	

	/** Updates the specified object position */
	public void updateObjectData(int objindex, Point3 pt, double a)
	{
		if (model3d != null) 		model3d.updateObject (objindex, pt, a);
	}
	public Simulator getSimulator(){
		return simulator; 
	}
	
	/**
	 * Determine if Java 3D is installed.
	 */
	public static boolean isJ3DInstalled() {
		try {
			Class.forName("javax.media.j3d.Canvas3D");
		}
		catch(Throwable ex) {
			return false;
		}
		return true;
	}
	
}
