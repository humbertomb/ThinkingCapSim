/*
 * (c) 2002 Juan Pedro Canovas
 * (c) 2003 Bernardo Canovas Segura
 */

package tcapps.tcsimulator.simulator.objects;

import java.util.*;
import java.awt.image.*;
import javax.swing.*;

import tc.runtime.thread.ModuleConfig;

import tcapps.tcsimulator.simulator.*;
import tcapps.tcsimulator.*;
import tcrob.ingenia.ifork.IForkController;
import tcrob.ingenia.ifork.linda.*;

import tc.vrobot.*;
import tc.shared.linda.*;
import tc.shared.world.*;


import wucore.utils.geom.*;

public class SimRobot extends VirtualRobot
{
	// Robot status internal data
	protected double				vlin;					// Current motion control commands
	protected double				vlat;
	protected double				vrot;
	
	// Simulation parameters
	protected Simulator				simul;
	protected RobotModel			model;
	protected long					tgfx;					// Time of graphics update
	
	// Cameras of the platform, rendered out of the 3D world
	protected SimCamera[]			cams;					// null: none, or no 3D to render them with
	protected int					cnext;					// which one has the next turn
	protected CameraWindow			camwin;					// what they are taking (ROBGFX only)
	
	// Other local stuff
	protected String				r_id;
	protected int					r_index;
	protected World					map;
	protected SimulatorDesc			sdesc;
	
	// Constructors
	public SimRobot (String robotid, ModuleConfig cfg, Linda linda, Simulator simul)
	{
		super (cfg, linda);
		
		this.r_id		= robotid;
		this.simul		= simul;
	}
	
	/* Accessor methods */
	public final int 			sonar_mode ()	 		{ return sdesc.MODESON; }
	public final void 			sonar_mode (int mod)	{ sdesc.MODESON = mod; }
	public final int 			ir_mode ()	 			{ return sdesc.MODEIR; }
	public final void 			ir_mode (int mod)		{ sdesc.MODEIR = mod; }
	public final int 			lsb_mode ()	 			{ return sdesc.MODELSB; }
	public final void 			lsb_mode (int mod)		{ sdesc.MODELSB = mod; }
	
	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{		
		super.initialise (cfg);
		
		// Load robot and world description
		sdesc	= new SimulatorDesc (rprops);
		model	= rdesc.model;
		
		// Notify the simulator of a new robot
		r_index	= simul.add_robot (rdesc, sdesc, model, data_ctrl, r_id);
		map		= simul.getWorld ();
		System.out.println ("# Setting robot map to "+map);
		
		reset ();
		
		// Create the cameras of the platform, and the window showing what they take
		open_cameras ();
		
		// Create robot motion-command window (if ROBGFX selected)
		if (localgfx)
			open_plot ();
	}
	
	public void reset ()
	{
		// Initialise default motion commands		
		vlin	= 0.0;
		vlat	= 0.0;
		vrot	= 0.0;
		
		simul.reset (r_index, data, map);
	}
	
	/** Places the robot at a given pose (x, y, angle) instead of the START of the world; the world START is left untouched. */
	public void reset (Point3 start)
	{
		vlin	= 0.0;
		vlat	= 0.0;
		vrot	= 0.0;
		simul.reset (r_index, data, start.x (), start.y (), start.z ());
		data.location (start.x (), start.y (), start.z ());
	}
	
	public void process_sensors (long dtime)
	{
		double		dt;
		
		// Update simulated delta time
		dt = ((double) dtime) / 1000.0;
		if (dt > (rdesc.DTIME / 1000.0))		
			dt = rdesc.DTIME / 1000.0;							// Non real-time simulation

		// Compute simulation
		simul.simulate (r_index, data, vlin, vlat, vrot, cycson, cycir, cyclrf, cyclsb, cycvis, dt);    
		
		// Take a frame of whichever camera is due for one
		process_cameras (dtime);
	}
	
	/**
	 * Builds a scene of its own for every camera the description of the platform
	 * declares, and, with local graphics, the window that shows what they take.
	 *
	 * A camera that cannot be rendered -- no 3D, no display -- is left out
	 * rather than stopping the robot: the simulation runs, with no frames.
	 */
	protected void open_cameras ()
	{
		java.util.List<SimCamera>	built = new ArrayList<SimCamera> ();
		
		if (rdesc.MAXCAMERA <= 0)				return;
		for (int i = 0; i < rdesc.MAXCAMERA; i++)
		{
			SimCamera	c = SimCamera.create (simul, r_index, i);
			if (c != null)						built.add (c);
		}
		if (built.isEmpty ())					return;
		cams	= built.toArray (new SimCamera[0]);
		for (SimCamera c : cams)
			System.out.println ("  [Sim] Robot " + r_id + ": " + c);
		if (localgfx)							open_camera_window ();
	}
	
	protected void open_camera_window ()
	{
		// Swing components must be created on the event thread, as the plot is
		Runnable	open = new Runnable ()
		{
			public void run ()
			{
				camwin	= new CameraWindow (SimRobot.this, "Cameras of " + r_id);
				for (SimCamera c : cams)
					camwin.add ("camera" + c.device (), c.width (), c.height (), c.framerate ());
				camwin.open ();
			}
		};
		if (SwingUtilities.isEventDispatchThread ())
			open.run ();
		else
			try { SwingUtilities.invokeAndWait (open); } catch (Exception e) { e.printStackTrace (); }
	}
	
	/**
	 * Takes a frame of one camera, when one is due for it, and leaves it where
	 * the virtual robot writes it to the Linda space (CAMERA).
	 *
	 * One frame a cycle, since that is what the robot writes; with more than one
	 * camera due at once they take turns, so none of them is starved by the
	 * first.
	 */
	protected void process_cameras (long dtime)
	{
		if (cams == null)						return;
		for (int k = 0; k < cams.length; k++)
		{
			SimCamera		c = cams[(cnext + k) % cams.length];
			
			if (!c.due (dtime))					continue;
			BufferedImage	im = c.take (data);
			if (im == null)						continue;
			if (camwin != null)					camwin.show (c.device (), im);
			// the robot writes the frame of this cycle: it copies it, so the camera
			// may draw over its own again
			cdata	= im;
			cdev	= c.device ();
			cnext	= (cnext + k + 1) % cams.length;
			return;								// one frame a cycle
		}
	}
	
	protected void close_gfx ()
	{
		super.close_gfx ();
		if (camwin != null)						camwin.close ();
		camwin	= null;
	}
	
	public void childClosed (Object window)
	{
		super.childClosed (window);
		if (window instanceof CameraWindow)		camwin = null;
	}
	
	/** Lets go of the scenes the cameras render with. */
	public void stop ()
	{
		super.stop ();
		if (cams != null)
			for (SimCamera c : cams)			c.dispose ();
		cams	= null;
	}
	
	public void notify_sensors_ctrl (String space, ItemSensorsCtrl item)
	{
		super.notify_sensors_ctrl (space, item);
		simul.set_data_ctrl (r_index, data_ctrl);
	}
	
	public void notify_zone (String space, ItemIForkZone item)
	{
		System.out.println ("  [Sim] Change zone message received <"+item.zone+">");
	}
	
	public void notify_execution (String space, ItemExecution item)
	{
		super.notify_execution (space, item);
	}	  
	
	public void notify_motion (String space, ItemMotion item)
	{
		double		kvlin, kvlat, kvrot;
		int			ctrlmode;
		
		super.notify_motion (space, item);
		
		kvlin		= item.vlin;
		kvlat		= item.vlat;
		kvrot		= item.vrot;
		ctrlmode		= item.ctrlmode;		
		
		// Movement commands
		if (ctrlmode != ItemMotion.CTRL_NONE)
		{
			switch (ctrlmode)
			{
			case ItemMotion.CTRL_MANUAL:
				vlin	= kvlin * model.Vmax;
				vlat	= kvlat * model.Umax;
				vrot	= kvrot * model.Rmax;
				break;
			case ItemMotion.CTRL_AUTO:
				vlin	= kvlin;
				vlat	= kvlat;
				vrot	= kvrot;
				break;
			default:
				System.out.println ("--[Sim] Unrecognised control-mode command");
			}
		}	
		
		// Read specific motion commands
		ItemIForkMotion			ifitem;
		if (item instanceof ItemIForkMotion)
		{
			ifitem		= (ItemIForkMotion) item;
			if (ifitem.frk_action != ItemIForkMotion.FRK_NONE)
			{
				// TODO this hsould be simulated better.
				data.fork	= ifitem.frk_height;
				if (simul.objects != null)
				{
					if (ifitem.frk_action == ItemIForkMotion.FRK_LOAD)
						simul.objects.pick_object (r_index, data.fork);
					else if (ifitem.frk_action == ItemIForkMotion.FRK_UNLOAD)
						simul.objects.drop_object (r_index, data.fork);
				}
			}
		}
	}	  
	/**
	 * The status of the robot (iFork): a task that loads the fork puts a pallet
	 * on it, one that unloads it takes it away, as the switch of the fork would
	 * tell (pal_switch). The task is in the status as "... <kind>/<task>(<place>)",
	 * e.g. "OCCUPIED. LOAD/FLOAD(Coutx)"; a status without it changes nothing.
	 */
	public void notify_status (String space, ItemStatus item)
	{
		String		status = item.toString ();
		int			from, to;
		int			task;

		from	= status.indexOf ("/");
		to		= (from > 0) ? status.indexOf ("(", from) : -1;
		if (to < 0)		return;

		task	= IForkController.parseTask (status.substring (from + 1, to));
		if (task == IForkController.FUNLOAD)
			data.pal_switch	= 0;
		else if (task == IForkController.FLOAD)
			data.pal_switch	= 1;
	}

	public void notify_pallet (String space, ItemPallet item){
		
//		System.out.println("  [SimRobot] Recibido tuple PALLET_CTRL space="+space+" "+item);
		if(item.robotid!=null && r_id.equalsIgnoreCase(item.robotid)){
			if(item.destiny==ItemPallet.AGV){
				if(item.action==ItemPallet.ADD){
					data.pal_switch=1;
//					System.out.println("  [SimRobot] AGV ADD pal_switch=1");
				}else if(item.action==ItemPallet.DEL){
					data.pal_switch=0;
//					System.out.println("  [SimRobot] AGV DEL pal_switch=0");
				}
			}	
		}
	}
}
