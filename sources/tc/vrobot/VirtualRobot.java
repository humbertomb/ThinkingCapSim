/*
 * (c) 1997-2002 Humberto Martinez
 */
 
package tc.vrobot;

import java.io.File;
import java.util.Properties;
import java.awt.image.*;

import tc.runtime.thread.ModuleConfig;
import tc.runtime.thread.StdThread;
import tc.shared.linda.ItemCamera;
import tc.shared.linda.ItemConfig;
import tc.shared.linda.ItemSensors;
import tc.shared.linda.ItemSensorsCtrl;
import tc.shared.linda.ItemCameraCtrl;
import tc.shared.linda.ItemExecution;
import tc.shared.linda.ItemMotion;
import tc.shared.linda.ItemObject;
import tc.shared.linda.Linda;
import tc.shared.linda.Tuple;
import tc.shared.world.World;


import wucore.gui.ChildWindowListener;
import wucore.gui.PlotWindow;

public abstract class VirtualRobot extends StdThread implements ChildWindowListener
{
	// General constants
	static protected final String[]	labels		= {"vlin", "vlat", "vrot"};
	
	protected RobotDesc				rdesc;				// Robot description
	protected PlotWindow			plot;				// Window to plot current motion command

	// Parameters for robot connection and environment settings
	protected Properties			rprops;				// Contents of robot description file
	protected String				wname;				// Description of robot environment
	protected String				wtext;				// Contents (JSON text) of the world description file
	
	// Time calculation and correction
	protected long					ltime;				// Previous time mark (ms)

	// Sensor update scheduling
	protected int					cycson;
	protected int					cycir;
	protected int					cyclsb;
	protected int					cyclrf;
	protected int					cycvis;
	
	// Data sent by the robot
	protected Tuple					tdata;
	protected ItemSensors			sdata;
	protected RobotData				data;
	protected Tuple					tobj;
	protected ItemObject			sobj;
	protected Tuple					tcam;
	protected ItemCamera			scam;
	protected BufferedImage			cdata;			// frame of the current cycle (null: no camera, or nothing taken)
	protected int					cdev;					// which camera of the robot took it
	protected RobotDataCtrl			data_ctrl;
	protected CameraCtrl			camera_ctrl;

	private double[]				buffer;				// Buffer to store curve points

	// Constructors
	public VirtualRobot (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}

	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{		
		String			rname;
		String			wdesc;

		// Load robot environment description and parameters
		rname			= cfg.get ("DESC");
		wname			= cfg.get ("WORLD");

		// Load robot description and parameters (JSON or the legacy properties format)
		rprops			= new Properties ();
		try
		{
			rprops		= RobotDef.load (new File (rname)).toProperties ();
		} catch (Exception e) { e.printStackTrace (); }

		// The control cycle of the platform is the one this module is run at, and
		// not a time of its own written in the description: it is said here, in the
		// properties everybody else is given, so that the modules of the robot and
		// its kinematics reckon with the same cycle the robot is actually run at
		rprops.setProperty ("DTIME", String.valueOf (tdesc.exectime));

		// Load world description and parameters (only when the world grants "a priori" knowledge to the robots)
		wtext			= null;
		if (wname != null)
		{
			try
			{
				wdesc	= new String (java.nio.file.Files.readAllBytes (java.nio.file.Paths.get (wname)), java.nio.charset.StandardCharsets.UTF_8);
				if (World.fromJsonText (wdesc).apw)		wtext = wdesc;
			}
			catch (Exception e) { e.printStackTrace (); }
		}

		// Prepare Linda data structures
		sdata		= new ItemSensors ();
		tdata		= new Tuple (Tuple.SENSORS, sdata);
		sobj		= new ItemObject ();
		tobj		= new Tuple (Tuple.OBJECT, sobj);
		scam		= new ItemCamera ();
		tcam		= new Tuple (Tuple.CAMERA, scam);
		
		// Setup robot description and data structures		
		rdesc 		= new RobotDesc (rprops);
		data		= new RobotData (rdesc);
		data_ctrl	= new RobotDataCtrl ();

		// Additional initialisations
		buffer		= new double[3];
	}
	
	protected void configure ()
	{		
		Tuple		tuple;

		System.out.println ("  [VRob] Sending new RDF");
		
		// Send robot description to Linda space
		tuple	= new Tuple (Tuple.CONFIG, new ItemConfig (rprops, wtext, 0));
		linda.write (tuple);
	}
	
	public void run ()
	{
		System.out.println ("  [VRob] Running with " + rdesc);
		
		// Configure the robot
		configure ();
		
       	// Initialise sensor update cycles
		cycson		= 1;
		cycir		= 1;
		cyclrf		= 1;
		cyclsb		= 1;
		cycvis		= 1;
		       	
		// Initialise time computations
    	ltime		= System.currentTimeMillis () - rdesc.DTIME;

		// Run the robot program
		super.run ();
	}
	
	public void open_plot ()
	{
		// Swing components must be created on the event thread (creating the plot from the
		// module thread deadlocks against the AWT tree lock when other windows are laying out)
		Runnable	open = new Runnable ()
		{
			public void run ()
			{
				if (plot == null)			plot	= new PlotWindow (VirtualRobot.this, "Motion Commands");
				
				// what it was asked for, as it was asked for: the two speeds against the
				// left scale in metres a second and the turn rate against the right one in
				// degrees a second, each as far as the platform goes that way
				double		vmax = Math.max (rdesc.model.Vmax, rdesc.model.Umax);
				double		rmax = Math.toDegrees (rdesc.model.Rmax);

				plot.setLegend (labels);
				plot.setLabels ("time", "m/s");
				plot.setRightAxis (2, "deg/s");
				plot.setYRange (-vmax, vmax);
				plot.setRightRange (-rmax, rmax);
				plot.open ();
			}
		};
		if (javax.swing.SwingUtilities.isEventDispatchThread ())
			open.run ();
		else
			try { javax.swing.SwingUtilities.invokeAndWait (open); } catch (Exception e) { e.printStackTrace (); }
	}
	
	protected void close_gfx ()
	{
		if (plot != null)		plot.close ();
		plot = null;
	}

	public void childClosed (Object window)
	{
		if (window instanceof PlotWindow)
			plot = null;
	}
	
	public final void step (long ctime) 
	{
		cycson++;		if (cycson > rdesc.CYCLESON)	cycson	= 1;
	   	cycir++;		if (cycir > rdesc.CYCLEIR)		cycir	= 1;
	   	cyclrf++;		if (cyclrf > rdesc.CYCLELRF)	cyclrf	= 1;
	   	cyclsb++;		if (cyclsb > rdesc.CYCLELSB)	cyclsb	= 1;
	    cycvis++;		if (cycvis > rdesc.CYCLEVIS)	cycvis	= 1;
			
		process_sensors (ctime - ltime);
		ltime	= ctime;
		
		// Write sensor data to the Linda space
		sdata.set (data, ctime);
		if(linda==null) return;
		linda.write (tdata);
				
		// Write the frame of the cameras to the Linda space
		if (cdata != null)
		{
			scam.set (ItemCamera.copy (cdata), cdev, ctime);
			cdata	= null;										// one frame is written once
			if(linda==null) return;
			linda.write (tcam);
		}
	}	
	
	// Default instance methods. Subclasses SHOULD not implement
	public void notify_config (String space, ItemConfig item)
	{
		// This is the configuration just sent
	}

	// Template instance methods. Subclasses MAY implement
	public void notify_motion (String space, ItemMotion item)
	{
		// Plot current motion command: the three velocities it was asked for, as they
		// were asked for, each in its own unit
		if (plot != null)
		{
			buffer[0] 	= item.vlin;								// [m/s]
			buffer[1] 	= item.vlat;								// [m/s]
			buffer[2] 	= Math.toDegrees (item.vrot);				// [deg/s]
			plot.updateData (buffer);	
		}
	}


	public void notify_execution (String space, ItemExecution item) 
	{		
		switch (item.operation)
		{
		case ItemExecution.COMMAND:
			switch (item.command)
			{
			case ItemExecution.START:
				running	= true;
				step	= false;
				break;
			case ItemExecution.STOP:
				running	= false;
				step	= false;
				break;
			case ItemExecution.STEP:
				running	= true;
				step	= true;
				break;
			case ItemExecution.RESET:
				configure ();
				reset ();
				
				running	= false;
				step	= false;
				break;
			default:
			}	
			break;
		case ItemExecution.DEBUG:
			debug	= item.dbg_vrobot;
			break;
		default:
		}	
	}	
	
	public void notify_sensors_ctrl (String space, ItemSensorsCtrl item)
	{
		data_ctrl.set (item.data_ctrl);
	}

	public void notify_camera_ctrl (String space, ItemCameraCtrl item)
	{
		camera_ctrl.set (item.camera_ctrl);
	}

	// Abstract instance methods. Subclasses MUST implement
	public abstract void reset ();
	public abstract void process_sensors (long dtime);
}

