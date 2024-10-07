/*
 * (c) 1997-2002 Humberto Martinez
 */
 
package tc.vrobot;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Properties;

import tc.runtime.thread.StdThread;
import tc.shared.linda.ItemConfig;
import tc.shared.linda.ItemData;
import tc.shared.linda.ItemDataCtrl;
import tc.shared.linda.ItemDebug;
import tc.shared.linda.ItemMotion;
import tc.shared.linda.ItemObject;
import tc.shared.linda.ItemStatus;
import tc.shared.linda.Linda;
import tc.shared.linda.Tuple;

import tcrob.ingenia.ifork.*;

import wucore.gui.ChildWindowListener;
import wucore.gui.PlotWindow;
import devices.data.VisionData;

public abstract class VirtualRobot extends StdThread implements ChildWindowListener
{
	// General constants
	static protected final String[]	labels		= {"speed", "turn"};
	
	protected RobotDesc				rdesc;				// Robot description
	protected PlotWindow				plot;				// Window to plot current motion command

	// Parameters for robot connection and environment settings
	protected String					raddress;			// Remote robot address (VR to robot driver)
	protected int					rport;				// Remote robot port (VR to robot driver)
	protected int					lport;				// Local robot port (VR to robot driver)
	protected Properties				rprops;				// Contents of robot description file
	protected String					wname;				// Description of robot environment
	protected Properties				wprops;				// Contents of world description file
	protected String					tname;				// Description of topologic map
	protected Properties				tprops;				// Contents of topologic description file
	
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
	protected ItemData				sdata;
	protected RobotData				data;
	protected Tuple					tobj;
	protected ItemObject				sobj;
	protected VisionData[]			odata;
	protected RobotDataCtrl			data_ctrl;

	private double[]					buffer;				// Buffer to store curve points

	// Constructors
	public VirtualRobot (Properties props, Linda linda)
	{
		super (props, linda);
	}

	// Instance methods
	protected void initialise (Properties props)
	{		
		String			rname;
		String			rcname, name;
		File				file;
		FileInputStream	stream;
		boolean			wapriori;
		
		// Load robot environment description and parameters
		rname			= props.getProperty ("ROBDESC");
		rcname			= props.getProperty ("ROBCUST");
		raddress			= props.getProperty ("ROBRADDR");
		try { rport 		= new Integer (props.getProperty ("ROBRPORT")).intValue (); } 	catch (Exception e) 		{ rport		= 0; }
		try { lport 		= new Integer (props.getProperty ("ROBLPORT")).intValue (); } 	catch (Exception e) 		{ lport		= 0; }
		wname			= props.getProperty ("ROBWORLD");
		tname			= props.getProperty ("ROBTOPOL");
		try { wapriori	= new Boolean (props.getProperty ("ROBAPW")).booleanValue (); } catch (Exception e) 		{ wapriori	= false; }
		
		// Load robot description and parameters
		rprops			= new Properties ();
		try
		{
			file 		= new File (rname);
			stream 		= new FileInputStream (file);
			rprops.load (stream);
			stream.close ();
		} catch (Exception e) { e.printStackTrace (); }

		// Load robot custom description and parameters (these parameters will overwrite the generic ones)
		if (rcname != null)
		{
			Properties		rcprops;
			Enumeration		keys;
			String			key;
			
			rcprops			= new Properties ();
			try
			{
				file 		= new File (rcname);
				stream 		= new FileInputStream (file);
				rcprops.load (stream);
				stream.close ();
			} catch (Exception e) { e.printStackTrace (); }
			
			// Load customised parameters
			name		= rcprops.getProperty ("NAME");
			
			keys		= rcprops.keys ();
			while (keys.hasMoreElements ())
			{
				key		= (String) keys.nextElement ();
				rprops.put (key, rcprops.get (key));
			}
			
			System.out.println ("  [VRob] Loaded customization for robot <"+name+">");
		}

		// Load world description and parameters (in case "a priori" world is selected)
		wprops			= null;
		tprops			= null;
		if (wapriori)
		{
			if (wname != null)
			{
				wprops			= new Properties ();
				try
				{
					file 		= new File (wname);
					stream 		= new FileInputStream (file);
					wprops.load (stream);
					stream.close ();
				} catch (Exception e) { e.printStackTrace (); }
			}
			
			if (tname != null)
			{
				tprops 			= new Properties ();
				try
				{
					file 		= new File (tname);
					stream 		= new FileInputStream (file);
					tprops.load (stream);
					stream.close ();
				} catch (Exception e) { e.printStackTrace (); }
			}
		}
		
		// Prepare Linda data structures
		sdata		= new ItemData ();
		tdata		= new Tuple (Tuple.DATA, sdata);
		sobj			= new ItemObject ();
		tobj			= new Tuple (Tuple.OBJECT, sobj);
		
		// Setup robot description and data structures		
		rdesc 		= new RobotDesc (rprops, tdesc.exectime);
		data			= new RobotData (rdesc);
		data_ctrl	= new RobotDataCtrl ();

		// Additional initialisations
		buffer		= new double[2];
	}
	
	protected void configure ()
	{		
		Tuple		tuple;

		System.out.println ("  [VRob] Sending new RDF");
		
		// Send robot description to Linda space
		tuple	= new Tuple (Tuple.CONFIG, new ItemConfig (rprops, wprops, tprops, 0));
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
		if (plot == null)			plot	= new PlotWindow (this, "Motion Commands");
		
		plot.setLegend (labels);
		plot.setLabels ("time", "values");
		plot.setYRange (-1.0, 1.0);
		plot.open ();
	}
	
	public void childClosed (Object window)
	{
		if (window instanceof PlotWindow)
			plot		= null;
	}
	
	public final void step (long ctime) 
	{
		int				i;
		VisionData[]		vobj;
    	    	 				
	    	cycson++;		if (cycson > rdesc.CYCLESON)	cycson	= 1;
	    	cycir++;			if (cycir > rdesc.CYCLEIR)	cycir	= 1;
	    	cyclrf++;		if (cyclrf > rdesc.CYCLELRF)	cyclrf	= 1;
	    	cyclsb++;		if (cyclsb > rdesc.CYCLELSB)	cyclsb	= 1;
	    	cycvis++;		if (cycvis > rdesc.CYCLEVIS)	cycvis	= 1;
			
		process_sensors (ctime - ltime);
		ltime	= ctime;
		
		// Write sensor data to the Linda space
		sdata.set (data, ctime);
		if(linda==null) return;
		linda.write (tdata);
		
		// Write object data to the Linda space
		if (odata != null)
		{
			vobj	= new VisionData[odata.length];
			for (i = 0; i < odata.length; i++)
				vobj[i]	= odata[i].dup ();
				
			sobj.set (vobj, ctime);
			if(linda==null) return;
			linda.write (tobj);
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
    		double		speed, turn;
    	
		speed	= item.speed;
		turn		= item.turn;
		
		// Plot current motion command
		if (debug && (plot != null))
		{
			buffer[0] 	= Math.max (Math.min (speed, 1.0), -1.0);
			buffer[1] 	= Math.max (Math.min (turn / rdesc.model.Rmax, 1.0), -1.0);
			plot.updateData (buffer);	
		}
	}

	public void notify_debug (String space, ItemDebug item) 
	{		
		switch (item.operation)
		{
		case ItemDebug.COMMAND:
			switch (item.command)
			{
			case ItemDebug.START:
				running	= true;
				step	= false;
				break;
			case ItemDebug.STOP:
				running	= false;
				step	= false;
				break;
			case ItemDebug.STEP:
				running	= true;
				step	= true;
				break;
			case ItemDebug.RESET:
				configure ();
				reset ();
				
				running	= false;
				step	= false;
				break;
			default:
			}	
			break;
		case ItemDebug.DEBUG:
			debug	= item.dbg_vrobot;
			break;
		case ItemDebug.MODE:
			mode	= item.mode_vrobot;
			break;
		default:
		}	
	}	
	
	public void notify_data_ctrl (String space, ItemDataCtrl item)
	{
		data_ctrl.set (item.data_ctrl);
	}

	public void notify_status (String space, ItemStatus item)
	{
		String itemstr;

		String task_mensaje = "";

		itemstr=item.toString();

		if(itemstr.indexOf("/")>0) {
			task_mensaje=itemstr.substring(itemstr.indexOf("/")+1,itemstr.indexOf("("));
		}
		
		//if(item.toString().indexOf("Unloading fork")!=-1){

		// OCCUPIED. UNLOAD/FUNLOAD(Coutx) 

		if (IForkController.parseTask(task_mensaje) == IForkController.FUNLOAD) {
			
			data.pal_switch=0;
			
		//}else if(item.toString().indexOf("Loading fork")!=-1){

		//OCCUPIED. LOAD/FLOAD(Coutx) 

		} else if (IForkController.parseTask(task_mensaje) == IForkController.FLOAD) {

			data.pal_switch=1;

		}
		
		/*
		if(item.toString().indexOf("Unloading fork")!=-1){
			data.pal_switch=0;
		}else if(item.toString().indexOf("Loading fork")!=-1){
			data.pal_switch=1;
		}
		*/
	}
	
	// Abstract instance methods. Subclasses MUST implement
	public abstract void reset ();
	public abstract void process_sensors (long dtime);
}

