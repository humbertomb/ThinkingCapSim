/*
 * (c) 2002 Juan Pedro Canovas
 * (c) 2003 Bernardo Canovas Segura
 */

package tcapps.tcsimulator.simulator.objects;

import tc.runtime.thread.ModuleConfig;

import tcapps.tcsimulator.simulator.*;
import tcrob.ingenia.ifork.linda.*;

import tc.vrobot.*;
import tc.shared.linda.*;
import tc.shared.world.*;


import wucore.utils.geom.*;

public class SimRobot extends VirtualRobot
{
	// Robot status internal data
	protected double				speed;					// Current motion control commands
	protected double				turn;
	
	// Simulation parameters
	protected Simulator				simul;
	protected RobotModel			model;
	protected long					tgfx;					// Time of graphics update
	
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
		
		// Create robot motion-command window (if ROBGFX selected)
		if (localgfx)
			open_plot ();
	}
	
	public void reset ()
	{
		// Initialise default motion commands		
		turn	= 0.0;
		speed	= 0.0;
		
		simul.reset (r_index, data, map);
	}
	
	/** Places the robot at a given pose (x, y, angle) instead of the START of the world; the world START is left untouched. */
	public void reset (Point3 start)
	{
		turn	= 0.0;
		speed	= 0.0;
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
		simul.simulate (r_index, data, speed, turn, cycson, cycir, cyclrf, cyclsb, cycvis, dt);    
		if (rdesc.MAXVISION > 0)
			odata = simul.getVisionData ();
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
	
	public void notify_debug (String space, ItemDebug item)
	{
		super.notify_debug (space, item);
	}	  
	
	public void notify_motion (String space, ItemMotion item)
	{
		double		kspeed, kturn;
		int			ctrlmode;
		
		super.notify_motion (space, item);
		
		kspeed		= item.speed;
		kturn		= item.turn;
		ctrlmode		= item.ctrlmode;		
		
		// Movement commands
		if (ctrlmode != ItemMotion.CTRL_NONE)
		{
			switch (ctrlmode)
			{
			case ItemMotion.CTRL_MANUAL:
				speed	= kspeed * model.Vmax;
				turn		= kturn * model.Rmax;
				break;
			case ItemMotion.CTRL_AUTO:
				speed	= kspeed;
				turn		= kturn;
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
	public void notify_pallet (String space, ItemPallet item){
		
//		System.out.println("  [SimRobot] Recibido tuple PALLETCTRL space="+space+" "+item);
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
	
	public void notify_delrobot (String space, ItemDelRobot item){
		//System.out.println("  [SimRobot] Recibido tuple DELROBOT space="+space+" "+item);
		if(r_id.equalsIgnoreCase(item.robotid)){
			System.out.println("  [SimRobot] Stop robot "+r_id);
			stop();
		}
	}
}
