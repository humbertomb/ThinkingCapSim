/*
 * (c) 2002 Humberto Martinez
 * (c) 2004 Jose Antonio Marin
 */
 
package tcrob.ingenia.ifork;

import java.io.*;
import java.util.Properties;

import tc.modules.Controller;
import tc.shared.linda.ItemBehResult;
import tc.shared.linda.ItemConfig;
import tc.shared.linda.ItemDebug;
import tc.shared.linda.ItemGoal;
import tc.shared.linda.ItemPath;
import tc.shared.linda.Linda;
import tc.shared.linda.Tuple;
import tc.shared.lps.lpo.LPO;
import tc.shared.lps.lpo.LPOLine;
import tc.shared.lps.lpo.LPORangePBug;
import tc.shared.lps.lpo.LPORangePoint;
import tc.vrobot.RobotDataCtrl;
import tc.vrobot.models.TricycleDrive;
import tclib.tracking.*;
import tcrob.umu.indoor.gui.*;
import tcrob.ingenia.ifork.linda.ItemIForkMotion;
import tcrob.ingenia.ifork.lpo.LPOIForkData;
import devices.pos.*;
import wucore.utils.geom.Polygon2;
import wucore.utils.logs.LogFile;
import wucore.utils.logs.LogPlot;
import wucore.utils.math.Angles;

public class IForkController extends Controller
{	
	static public final String			PREFFIX			= "CNTL_";

	// Task types
	static public final int				STANDBY			= 0;
	static public final int				NAVIGATE		= 1;
	static public final int				CROSSDOOR		= 2;
	static public final int				DOCK			= 3;
	static public final int				UNDOCK			= 4;
	static public final int				FLOAD			= 5;
	static public final int				FUNLOAD			= 6;
	static public final int				FADJUST			= 7;
	static public final int				CHECKPAL		= 8;
	static public final int				COORDWAIT		= 20;
	
	// Follow_path behaviour parameters
	static public final double			FOLLOW_STP		= 3.5;		// Distance to begin stopping the vehicle
	static public final double			FOLLOW_LCS		= 50.0;		// Control cycles to stop the vehicle	
	static public final double			FOLLOW_DANGER	= 10.0;		// Range for collision danger (m)
	
	// Manouver behaviour parameters
	static public final double			MANOUVER_STP	= 0.5;		// Distance to stop the vehicle
	static public final double			MANOUVER_LCS	= 10.0;		// Control cycles to stop the vehicle 

	// Additional constants
	static public final int				MAX_INPUTS		= 5;			// Maximum number of input variables

	// Linda related variables
	protected RobotDataCtrl				robot_ctrl;
	protected Tuple						miftuple;
	protected ItemIForkMotion			mifitem;

	// Debugging and logging tools
	protected LogPlot					c_plot;
	protected LogFile					c_dump;
	protected double[]					c_buffer;
	protected String[]					c_labels;
	protected IndoorLPSWindow			win;
	
	// Goal representation and completion detection
	protected boolean					inZone0;
	protected boolean					inZone1;
	protected boolean					failed;
	protected boolean					finished;
	protected double					lastdist;
	
	protected IForkPlan					iplan;						// Current task
	protected long						idtask;						// Current task ID
	protected IForkPlan					newiplan;					// New task received
	protected IForkPlan					lastiplan;					// Previous task

	protected boolean					new_goal;					// New goal received
	protected long						new_id;						// New task ID received
	
	// Look-ahead related variables
	protected Path						path;						// Desired robot path
	protected Position					pos;							// Current robot location
	protected Position					looka;						// Current look-ahead point
	protected int						looka_pts;					// Current look-ahead distance (points)
	protected double					path_dst;					// Current robot to desired path distance (m)

	// Other internal & control variables
	protected String					robotid;
	protected TricycleDrive				model;
	protected double					vr;							// Desired output linear spped (m/s)
	protected double					wr;							// Desired output rotational speed (rad/s)
	protected double					frk_hgt;					// Fork command control value
	protected int						frk_act;					// Fork action mode
	protected int						horn;						// Horn action state
	protected int						lhorn;						// Last horn state
	protected int						coord;						// Coordination light action state
	protected int						lcoord;						// Last coordination light state
	protected double					lvr;						// Last linear velocity
	protected double					looka_dst;					// Current look-ahead distance (m)
	protected boolean					behavoid;					// Obstacle avoidance status

	// Tracking methods
	public final int	SCREW_CONTROL		= 0;
	public final int	FUZZY_CONTROL		= 1;
	public final int	FLATNESS_CONTROL	= 2;
	
	ScrewControl scontrol = null;				// Screw Control
	FuzzyControl fuzzcontrol = null;			// Fuzzy Control
	FlatnessControl fcontrol = null;			// Flatness Control
	
	double dt, abs_time;
	boolean fcont_time;
	long last_time, cont_time;
	Position initialpos;
	
	// Debug and logs
	boolean fdebug;
	boolean sexecuting, fuzexecuting, fexecuting;
	int scounter, fuzcounter, fcounter;
	PrintWriter pout = null;
	
	// Constructors
	public IForkController (Properties props, Linda linda) 
	{
		super (props, linda);
		
		robotid 		= props.getProperty ("ROBNAME");
	}
	
	// Instance methods
	protected void initialise (Properties props)
	{		
		super.initialise (props);

		// Local variables
		robot_ctrl		= new RobotDataCtrl ();
		mifitem			= new ItemIForkMotion ();
		miftuple			= new Tuple (Tuple.MOTION, mifitem);
		lhorn			= ItemIForkMotion.TS_NONE;
		lcoord			= ItemIForkMotion.TS_NONE;
		behavoid		= false;
		
		// Goal related variables
		inZone0 		= false;
		inZone1 		= false;
		failed 			= false;
		finished 		= false;
		lastdist 		= Double.MAX_VALUE;
		iplan			= new IForkPlan ();
		lastiplan 		= new IForkPlan();
		newiplan		= new IForkPlan ();

		// Initialise structures
		looka			= new Position ();
		pos				= new Position ();

		new_goal		= false;
		new_id			= 0;
		
		idtask			= 0;
		looka_pts		= 4;

		// Initialise debug modules
		c_dump		= new LogFile (PREFFIX, ".log");
		c_plot		= new LogPlot ("Controller Output", "step", "values");

		// Initialise debug variables
		c_buffer		= new double[4];
		c_labels		= new String[4];
		c_labels[0]		= "speed";
		c_labels[1]		= "turn";
		c_labels[2]		= "e_ang";
		c_labels[3]		= "e_pos";
	}
	
	protected boolean inRestrictedArea ()
	{
		if (world == null)		{	return false;	}
		
		Polygon2[] polygons = world.getFAreas();
		
		for (int k = 0; k < polygons.length; k++)
			if(polygons[k].contains(pos.x (), pos.y ()))
			{
				//System.out.println("Warning::robot positioned into a restricted area: FAREA_" + k);
				return true;
			}
		
		return false;
	}
	
	protected int inGoalPos ()
	{
		double			dx, dy;
		double			dist, delta;

		// Check if goal position has been reached
		dx		= iplan.tpos.x () - pos.x ();
		dy		= iplan.tpos.y () - pos.y ();
		dist		= Math.sqrt (dx * dx + dy * dy);											// [m]
		delta	= Math.abs (Angles.radnorm_180 (iplan.tpos.alpha () - pos.alpha ()));		// [rad]	
		
		if( (lastiplan.task != iplan.task) || (lastiplan.tpos.distance(iplan.tpos)>0.0))
		{
			//System.out.println("Haciendo "+iplan.task);
			finished = false;
			failed = false;
			lastiplan.task = iplan.task;
			lastiplan.tpos.set(iplan.tpos);
			inZone0 = false; inZone1 = false;

		}
		else
		{
			if(finished) return ItemBehResult.T_FINISHED;
			if(failed) return ItemBehResult.T_FAILED;
		}
		
		// System.out.println("Tolerances: Pos: " + iplan.tol_pos + " Hea: " + iplan.tol_head);
		
		// Si cumple con las condiciones de tolerancia -> Finished
		if ((dist < iplan.tol_pos) && (delta < iplan.tol_head))
		{	
			if(debug && !finished){
				Position local = Transform2.toLocal(pos,iplan.tpos);
				System.out.println("  [IForkController] Plan "+iplan+" dx="+dx+" dy="+dy+" dist="+dist+" delta="+Math.toDegrees(delta)+" local["+local.x()+", "+local.y()+"]");
			}
			inZone0 = false; inZone1 = false;
			finished = true;										
			return ItemBehResult.T_FINISHED;
		}	
			
		if (dist < iplan.tol_pos){			// ZONA0
			inZone0 = true;			// Cumple tolerancia de posicion pero no en Orientacion (en Zona 0)
			return ItemBehResult.T_NOTYET;
		}

		if ((dist < (iplan.tol_pos+0.5))) {// ZONA1 (mayor tolerancia en posicion)
			
			/*
			// Detecta cuando pasa del punto segun tolorancia en Orientacion (solo para Dock y Push)
			if( (iplan.task==ItemGoal.DOCK)||(iplan.task==ItemGoal.PUSH))
				if(Math.abs(Angles.radnorm_180( iplan.pos.alpha()-Math.atan2(pos.y()-iplan.pos.y(),pos.x()-iplan.pos.x()) )) > (Math.PI-iplan.tol_head)/2 )
				{ 
					inZone0 = false; inZone1 = false;
					System.out.println("FAILED 		Pasado del punto ("+dx+","+dy+","+Angles.RTOD * delta+")");									
					failed = true;										
					return ItemBehResult.T_FAILED;		
				}
			*/

				// Detecta cuando Pasa de Zona0 a Zona1
			if(inZone0 && dist > (iplan.tol_pos+0.1))			
			{ 			
				inZone0 = false; inZone1 = false;
				if(parseTask(iplan.task)!=NAVIGATE && parseTask (iplan.task)!=CROSSDOOR){
					System.out.println("--[IForkCon] FAILED\tPasado de Z0 -> Z1 " + iplan.toString()+" dist="+dist+" tolpos="+iplan.tol_pos);
					failed = true;										
					return ItemBehResult.T_FAILED;
				}
				else							
					return ItemBehResult.T_FINISHED;
			}
			inZone1 = true;		// Cumple tolencia de posicion y pero no en Orientacion (en Zona 1)
			return ItemBehResult.T_NOTYET;
		}
		
		// Detecta cuando pasa de Zona0 o Zona1 hacia fuera
		if(inZone0||inZone1){ 	
			inZone0 = false; inZone1 = false;		
			if(parseTask (iplan.task)!=NAVIGATE && parseTask (iplan.task)!=CROSSDOOR){
				System.out.println("--[IForkCon] FAILED\tPasado de Z0Z1 -> fuera" + iplan.toString()+" dist="+dist+" tolpos="+iplan.tol_pos);
				failed = true;										
				return ItemBehResult.T_FAILED;
			}
			else							
				return ItemBehResult.T_FINISHED;	
		}

		return ItemBehResult.T_NOTYET;
	}
	
	protected int inGoalPosPal (){
		int result = inGoalPos();
		if(result == ItemBehResult.T_NOTYET){
			if(lps.pal_switch == 1){
				return ItemBehResult.T_FINISHED;
			}
		}
		return result;
	}
	
	protected int isPalet(int palet){
		// TODO: Remove comments for working on real robot
//		if(palet == 1)
//			return ItemBehResult.T_FINISHED;
//		return ItemBehResult.T_NOTYET;
		
		return ItemBehResult.T_FINISHED;
	}

	protected int inForkPos (double setp, double curr)
	{
		if (debug)	System.out.println("IForkController. Setpoint:"+setp+"m Current:"+curr+"m"+" dif = "+Math.abs (setp - curr)*100+"cm");

		if (Math.abs (setp - curr) < 0.03){
			if (debug) System.out.println("IForkController: finished!!!");
			return ItemBehResult.T_FINISHED;
		}	
		return ItemBehResult.T_NOTYET;
	}
	
	static public int parseTask (String task)
	{
		String		ltask;
		
		ltask = task.toLowerCase ();
		if (ltask.equals ("standby"))			return STANDBY;
		else if (ltask.equals ("navigate"))		return NAVIGATE;
		else if (ltask.equals ("crossdoor"))	return CROSSDOOR;
		else if (ltask.equals ("dock"))			return DOCK;
		else if (ltask.equals ("undock"))		return UNDOCK;
		else if (ltask.equals ("fload"))		return FLOAD;
		else if (ltask.equals ("funload"))		return FUNLOAD;
		else if (ltask.equals ("fadjust"))		return FADJUST;
		else if (ltask.equals ("checkpal"))		return CHECKPAL;
		else if (ltask.equals ("coordwait"))	return COORDWAIT;
		
		return STANDBY;
	}

	protected void controller () 
	{
		LPOIForkData	l_robot;
		LPO				l_looka;
		boolean			avoid;
		
		// Get internal robot status and compute look-ahead distance
		l_robot	= (LPOIForkData) lps.find ("IForkData");
		looka_selection (iplan.spd_vmax);
		
		// System.out.println("Controller LookAhead Distance: " + iplan.spd_vmax);
		
		// Compute look-ahead point
		pos.set (lps.cur);
		looka.set (pos);
		looka.valid (false);
		
		if(new_goal && (path != null))
		{
			abs_time	= 0.0;
			cont_time	= System.currentTimeMillis();
			fcont_time	= false;
			
			sexecuting		= false;
			fuzexecuting	= false;
			fexecuting		= false;
			
			if (fdebug && (pout != null))
			{
				//System.out.println("XXX - Closing file - XXX");
				pout.close();
				pout = null;
			}
			
			initialpos = new Position(pos.x(), pos.y(), pos.alpha());
			
		}
		
		if (!new_goal && (path != null))
		{
			path.check_lookahead (pos, looka_dst);
			if (path.lookahead () != null)
			{
				path_dst	= path.distance ();
				looka.set (path.lookahead ());
				looka.valid (true);
			}
			else if(debug){
				System.out.println("  [IforkController] pos="+pos+" looka_dst="+looka_dst+" path = "+path.toString());
			}
		}
		else if(debug){
			System.out.println("  [IforkController] newgoal="+new_goal+" path is null="+(path==null)+" pos="+pos);
		}

		// Update LPS
		l_looka		= lps.find ("Looka");
		if (l_looka != null)
		{
			l_looka.locate (looka.x () - pos.x (), looka.y () - pos.y (), pos.alpha ());
			l_looka.active (looka.valid ());
		}

		// Execute current active task/behaviour	
		frk_hgt		= 0.0;
		frk_act		= ItemIForkMotion.FRK_NONE;
		horn		= ItemIForkMotion.TS_DISABLE;
		coord		= ItemIForkMotion.TS_DISABLE;	
		avoid		= true;
		
		if(inRestrictedArea())
		{
			// Stop AGV due to safe reasons
			beh_stop ();
			System.out.println("iForkController:: iFork stopped due to restricted area");
			beh_motion (ItemBehResult.T_FAILED);
		} else {
			switch (parseTask (iplan.task))
			{
			case CROSSDOOR:
			case UNDOCK:
				avoid	= false;
			case NAVIGATE:
				if (iplan.tpos.distance (pos) < iplan.tol_pos)
					beh_stop ();
				else
					//beh_follow_path (avoid);	// Controlador Fuzzy
					beh_pure_pursuit(avoid);	// Controlador Pure Pursuit
				
				// White Ligth turn on when avoid is active
				if(avoid && behavoid){
					//horn		= ItemIForkMotion.TS_ENABLE;
					coord = ItemIForkMotion.TS_ENABLE;
				}
				beh_motion (inGoalPos ());
				break;
							
			case DOCK:
				try {
					beh_manouver ();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				horn		= ItemIForkMotion.TS_ENABLE;				// Play horn
				if(IForkPlanner.parsePlan (iplan.plan) == IForkPlanner.LOAD)
					beh_motion (inGoalPosPal());
				else
					beh_motion(inGoalPos());
				break;
							
			case FADJUST:
				beh_stop ();
				frk_hgt		= iplan.tpos.z ();
				frk_act		= ItemIForkMotion.FRK_ADJUST;
				beh_motion (inForkPos (frk_hgt, l_robot.fork ()));
				break;
				
			case FLOAD:
				beh_stop ();
				frk_hgt		= iplan.tpos.z ();
				frk_act		= ItemIForkMotion.FRK_LOAD;
				beh_motion (inForkPos (frk_hgt, l_robot.fork ()));
				break;
				
			case FUNLOAD:
				beh_stop ();
				frk_hgt		= iplan.tpos.z ();
				frk_act		= ItemIForkMotion.FRK_UNLOAD;
				beh_motion (inForkPos (frk_hgt, l_robot.fork ()));
				break;
			case CHECKPAL:
				coord	= ItemIForkMotion.TS_ENABLE;
				beh_motion(isPalet(l_robot.pal_switch()));
				break;
				
			case COORDWAIT:
				beh_stop ();
				coord	= ItemIForkMotion.TS_ENABLE;				// Set coordination light
				beh_motion (ItemBehResult.T_NOTYET);
				break;
				
			case STANDBY:
			default:
				beh_stop ();
				beh_motion (ItemBehResult.T_FINISHED);
			}
		}
		
		if (win != null)		win.update (lps, path);
	}
	
	protected void sensor_selection () 
	{
		// Activate or deactivate the desired sensors	
		switch (parseTask (newiplan.task))
		{
		case NAVIGATE:
		case UNDOCK:			robot_ctrl.lrf	= true; break;
		
		case CROSSDOOR:		robot_ctrl.lrf	= true; break;
		
		case DOCK:			robot_ctrl.lrf	= false; break;
		
		case FLOAD:
		case FUNLOAD:
		case FADJUST:		robot_ctrl.lrf	= false; break;
		
		case STANDBY:
		default:				robot_ctrl.lrf	= false;
		}
		
		setRobotCtrl (robot_ctrl);
	}
	
	protected void looka_selection (double speed) 
	{
		// Select the appropiate look-ahead distance depending on the task	
		switch (parseTask (newiplan.task))
		{
		case NAVIGATE:
			if (speed != IForkPlan.NA)
			{
				if (speed > 0.7)
					looka_dst	= 5.0; 
				else if (speed > 0.4)
					looka_dst	= 2.5; 
				else
					looka_dst	= 1.0;			
			}
			else
				looka_dst	= 5.0; 
			break;
			
		case UNDOCK:
			//looka_dst	= 5.0;
			looka_dst	= 2.0;
			break;
		case CROSSDOOR:
			//looka_dst	= 10.0;
			looka_dst	= 2.0;
			break;
		case DOCK:
			looka_dst	= 0.5;	// Old Fuzzy controler = 0.8;
			break;
		case FLOAD:
		case FUNLOAD:
		case FADJUST:
		case STANDBY:
		default:
			looka_dst	= 0.0;
		}
	}
	
	protected void beh_follow_path (boolean avoid)
	{
		double			dx, dy, dt;
		double			delta;
		double			dist, heading;	
		double			vmax, rmax;
		double 			del, vm;
		LPORangePBug		l_pbug;
		LPOLine			l_avoid = null;
		LPORangePoint	l_pt;

		// Compute heading to look-ahead point 
		dx		= looka.x () - pos.x ();
		dy		= looka.y () - pos.y ();
		dist		= Math.sqrt (dx * dx + dy * dy);										// [m]
		heading	= Math.atan2 (dy, dx);								// [rad]
		delta	= Angles.radnorm_180 (heading - pos.alpha ());		// [rad]
	
		// Collision avoidance
		if (avoid)
		{
			l_pbug	= (LPORangePBug) lps.find ("PolarBug");
			l_avoid	= (LPOLine) lps.find ("Avoid");
			dx		= dist * Math.cos (delta);
			dy		= dist * Math.sin (delta);
			l_pt		= l_pbug.collision (dx, dy, pos); // Devuelve el punto al cual evitar colision (si hay colision)
			l_avoid.active (l_pt != null);
			behavoid = l_avoid.active();
			if (l_avoid.active ())
			{
				//l_avoid.locate_polar (l_pt.rho (), l_pt.phi ());
				l_avoid.locate_polar (1.0, l_pt.phi ());
				delta	= Angles.radnorm_180 (l_avoid.phi ());
				dist = 2;
			}
		}
		
		if (avoid && l_avoid.active ()){
		    // Use Pure Pursuit to calculate Steering angle
			del = Math.atan2(2 * model.l * Math.sin(delta), dist);
			vm = 0.3; // Speed limit
		}
		else{	
			// Apply the fuzzy logic controller (angle and velocity of wheel)
			double foutput[];
			
			foutput = fuzzcontrol.pathFollowingFuzzyController(delta);
			
			del	= foutput[0];	// [rad/s]
			vm	= foutput[1];	// [m/s]
		}
		
		// Set up an upper boundary for the linear velocity (depending on the distance)
		double			distgoal;
		
		distgoal	= iplan.tpos.distance (pos);
		if (iplan.spd_end)
		{
			if(distgoal  <= FOLLOW_STP){
				dt		= rdesc.DTIME * FOLLOW_LCS / 1000.0;	// [s]
				vm		= Math.min (distgoal / dt, vm);			// [m/s]
			}
		}
		else{
			if(distgoal  <= 1.5){
				vm		= Math.min ( 0.2, vm);
			}
		}
		
		// Check for maximum planned velocities 
		vmax	= model.MOTmax;
					
		if (iplan.spd_vmax != IForkPlan.NA)
			vmax	= iplan.spd_vmax;													// [m/s]
		rmax	= model.STRmax;
		
		// Apply speed limits (vm, del)
		del		= Math.max (Math.min (del, rmax), -rmax);								// [rad/s]
		vm		= Math.max (Math.min (vm, vmax), -vmax);								// [m/s]
		
		// Compute direct kynematics
		wr		= Math.sin (del) * vm / (model.l - model.r * Math.sin (del));
		vr		= vm * Math.cos (del) + wr * (model.b + model.r * Math.cos (del));			

		// Apply angular speed limits (wr)
		wr	= Math.max (Math.min (wr, model.Rmax), -model.Rmax);
		
		// Plot current motion commands
		if (debug)
		{
		    System.out.println("Controller INPUT  -  dist: "+dist+" delta: "+Math.toDegrees(delta)+" PlanVmax: "+iplan.spd_vmax+" looka: "+looka_dst);
		    System.out.println("Controller OUTPUT -  vm: "+vm+" del: "+Math.toDegrees(del)+" vr: "+vr+" wr: "+Math.toDegrees(wr));
			c_buffer[0] 	= Math.max (Math.min (vr, 1.0), -1.0);
			c_buffer[1] 	= Math.max (Math.min (wr / rdesc.model.Rmax, 1.0), -1.0);
			c_buffer[2] 	= delta * Angles.RTOD;
			c_buffer[3] 	= Math.max (Math.min (path_dst, 1.0), -1.0);
			
			if (localgfx)
				c_plot.draw (c_buffer);	
			else
				c_dump.write (c_buffer);
		}
	}
	
	protected void beh_pure_pursuit (boolean avoid){
		double dx,dy,dist,distgoal;
		double dxl,dyl;
		double delta;
		double del, vm;
		double dt;
		double vmax, rmax;
		double p;
		LPORangePBug	l_pbug;
		LPOLine			l_avoid = null;
		LPORangePoint	l_pt;
		
		vm = 0;
		
		distgoal	= iplan.tpos.distance (pos);
		
		if(distgoal<iplan.tol_pos){
			wr = 0;
			vr = 0;
			if(debug)
				System.out.println("  [IforkController] purepursuit() InGoal. dist="+distgoal+" pos ="+pos+" goal="+iplan.tpos);
			return;
		}
		
		// Distancias globales
		dx		= looka.x () - pos.x ();
		dy		= looka.y () - pos.y ();
		dist	= Math.sqrt (dx * dx + dy * dy);
		delta	= Angles.radnorm_180 (Math.atan2 (dy, dx) - pos.alpha ());
		
		dxl		= dist * Math.cos (delta);
		dyl		= dist * Math.sin (delta);
		
		// Collision avoidance
		if (avoid)
		{
			l_pbug	= (LPORangePBug) lps.find ("PolarBug");
			l_avoid	= (LPOLine) lps.find ("Avoid");
			l_pt		= l_pbug.collision (dxl, dyl, pos); // Devuelve el punto al cual evitar colision (si hay colision)
			l_avoid.active (l_pt != null);
			behavoid = l_avoid.active();
			if (l_avoid.active ())
			{
				//l_avoid.locate_polar (l_pt.rho (), l_pt.phi ());
				l_avoid.locate_polar (l_pt.rho (), l_pt.phi ());
				delta	= Angles.radnorm_180 (l_avoid.phi ());
				dist = l_pt.rho ();
				if(debug) System.out.println("[IForkController] Avoid. rho="+dist+" delta="+Math.toDegrees(delta));
			}
		}
		
		if (avoid && l_avoid.active ()) // Evitacion obstaculo
		{
		    // Use Pure Pursuit to calculate Steering angle
			del = Math.atan2(2 * model.l * Math.sin(delta), dist);
			vm = 0.3; // Speed limit
		}
		else
		{	
			// Controlador Pure Pursuit //
			
			// Giro maximo derecha
			if(Math.toDegrees(delta) > 60){
				if(debug) System.out.println("[IForkController] giro maximo a derecha");
				del = Math.toRadians(60);
				vm = 0.3;
			}
			// Giro maximo izquierda
			else if(Math.toDegrees(delta) < -60){
				if(debug) System.out.println("[IForkController] giro maximo a izquierda");
				del = Math.toRadians(-60);
				vm = 0.3;
			}
			else {
				del = Math.atan(2*dyl*model.l/(dist*dist));
				if(debug){
					System.out.println("del="+Math.toDegrees(del)+" dyl="+dyl+" L="+model.l+" dist="+dist+" alpha="+Math.toDegrees(pos.alpha())+" p1="+(dx * Math.sin(-pos.alpha()))+" p2="+(dy * Math.cos(-pos.alpha())));
				}
				del	= Math.max (Math.min (del, Math.toRadians(60)), Math.toRadians(-60));
				
				// Velocidad en funcion del angulo.
				//if		(Math.abs(del)<Math.toRadians(15))	vm = 1.0;
				//else if	(Math.abs(del)<Math.toRadians(30))	vm = 0.5;
				//else if	(Math.abs(del)<Math.toRadians(45))	vm = 0.4;
				//else if	(Math.abs(del)<Math.toRadians(60))	vm = 0.3;
				//else											vm = 0.3;
				
				p = 0.7; //Variable que determina la proporcion de velocidad.
				if(Math.abs(del)<Math.toRadians(10))
					vm = (p*1) - (p*0.04) * Math.abs(Math.toDegrees(del));
				else if(Math.abs(del)<Math.toRadians(25))
					vm = (p*0.8) - (p*0.02) * Math.abs(Math.toDegrees(del)); 
				else
					vm = (p*0.3);
			}
		}
		
		if (iplan.spd_end)
		{
			if(distgoal  <= FOLLOW_STP){
				dt		= rdesc.DTIME * FOLLOW_LCS / 1000.0;								// [s]
				vm		= Math.min (distgoal / dt, vm);									// [m/s]
			}
		}
		else{
			if(distgoal  <= 1.5){
				vm		= Math.min ( (distgoal/1.5) * 0.3 + 0.1 , vm); // rampa de 0.4 a 0.1
			}
		}
		
		// Check for maximum planned velocities 
		vmax	= model.MOTmax;
		
		if (iplan.spd_vmax != IForkPlan.NA)
			vmax	= iplan.spd_vmax;													// [m/s]
		rmax	= model.STRmax;
		
		// Apply speed limits (vm, del)
		del		= Math.max (Math.min (del, rmax), -rmax);								// [rad/s]
		vm		= Math.max (Math.min (vm, vmax), -vmax);	
		
		//		// Compute direct kynematics
		wr = Math.sin(del) * vm / (model.l - model.r * Math.sin(del));
		vr = vm * Math.cos(del) + wr * (model.b + model.r * Math.cos(del));
		//System.out.println("wr = "+Math.toDegrees(wr)+" L="+model.l);
		
		// Apply angular speed limits (wr)
		wr	= Math.max (Math.min (wr, model.Rmax), -model.Rmax);
		
		// Plot current motion commands
		if (debug)
		{
		    System.out.println("Controller INPUT  -  dist: "+dist+" delta: "+Math.toDegrees(delta)+" PlanVmax: "+iplan.spd_vmax+" looka: "+looka_dst);
		    System.out.println("Controller OUTPUT -  vm: "+vm+" del: "+Math.toDegrees(del)+" vr: "+vr+" wr: "+Math.toDegrees(wr)+" d["+dx+","+dy+"] dyl="+dyl);
			c_buffer[0] 	= Math.max (Math.min (vr, 1.0), -1.0);
			c_buffer[1] 	= Math.max (Math.min (wr / rdesc.model.Rmax, 1.0), -1.0);
			c_buffer[2] 	= delta * Angles.RTOD;
			c_buffer[3] 	= Math.max (Math.min (path_dst, 1.0), -1.0);
			
			if (localgfx)
				c_plot.draw (c_buffer);	
			else
				c_dump.write (c_buffer);
		}
	}
	
	protected void beh_manouver () throws FileNotFoundException
	{
		int choice;
		
		choice = SCREW_CONTROL;
		//choice = FUZZY_CONTROL;
		//choice = FLATNESS_CONTROL;
		
		switch(choice)
		{
		case SCREW_CONTROL:
			if(fdebug && !sexecuting)
			{
				Position pos;
				pout = new PrintWriter(new FileOutputStream("path_screw_".concat(String.valueOf(scounter)).concat(".txt")));
				for(int i = 0; i < path.num(); i++)
				{
					pos = path.getPosition(i);
					pout.println(pos.x() + "\t" + pos.y());
				}
				pout.close();
				pout = new PrintWriter(new FileOutputStream("screw_".concat(String.valueOf(scounter)).concat(".txt")));
				sexecuting = true;
				++scounter;
			}
			beh_manouverScrew ();
			break;
		case FUZZY_CONTROL:
			if(fdebug && !fuzexecuting)
			{
				Position pos;
				pout = new PrintWriter(new FileOutputStream("path_fuzzy_".concat(String.valueOf(fuzcounter)).concat(".txt")));
				for(int i = 0; i < path.num(); i++)
				{
					pos = path.getPosition(i);
					pout.println(pos.x() + "\t" + pos.y());
				}
				pout.close();
				pout = new PrintWriter(new FileOutputStream("fuzzy_".concat(String.valueOf(fuzcounter)).concat(".txt")));
				fuzexecuting = true;
				++fuzcounter;
			}
			beh_manouverFuzzy ();
			break;
		case FLATNESS_CONTROL:
			if(fdebug && !fexecuting)
			{
				Position pos;
				pout = new PrintWriter(new FileOutputStream("path_flatness_".concat(String.valueOf(fcounter)).concat(".txt")));
				for(int i = 0; i < path.num(); i++)
				{
					pos = path.getPosition(i);
					pout.println(pos.x() + "\t" + pos.y());
				}
				pout.close();
				pout = new PrintWriter(new FileOutputStream("flatness_".concat(String.valueOf(fcounter)).concat(".txt")));
				fexecuting = true;
				++fcounter;
			}
			beh_manouverFlatness ();
			break;
		default:
			System.out.println("Warning:: unknown control id: " + choice);
		}
		
		// Plot current motion commands
		if (debug)
		{
		    //System.out.println("Controller INPUT  -  dist: " + dist + " delta: " + Math.toDegrees(delta) + " PlanVmax: " + iplan.spd_vmax + " looka: " + looka_dst);
		    //System.out.println("Controller OUTPUT -  vm: " + vm + " del: " + Math.toDegrees(del) + " vr: " + vr + " wr: " + Math.toDegrees(wr));
			c_buffer[0] 	= Math.max (Math.min (vr, 1.0), -1.0);
			c_buffer[1] 	= Math.max (Math.min (wr / model.Rmax, 1.0), -1.0);
			c_buffer[2] 	= Math.max (Math.min (path_dst, 1.0), -1.0);
			
			if (localgfx)
				c_plot.draw (c_buffer);	
			else
				c_dump.write (c_buffer);			 
		}
	}
	
	protected void beh_stop ()
	{
		vr		= 0.0;
		wr		= 0.0;
		
		// Plot current motion commands
		if (debug)
		{
			c_buffer[0] 	= vr;
			c_buffer[1] 	= wr;

			if (localgfx)
				c_plot.draw (c_buffer);	
			else
				c_dump.write (c_buffer);
		}
	}
	
	protected void beh_manouverScrew ()
	{
		double[] velocities;
		
		if (!fcont_time)
		{
			cont_time = System.currentTimeMillis();
			last_time = cont_time;
			
			fcont_time = true;
		}
		
		long cur_time;
		cur_time	= System.currentTimeMillis();
		abs_time	= ((double)(cur_time - cont_time))/1000.0;
		
		scontrol.setTolPosition(iplan.tol_pos);
		scontrol.setLookaDistance(looka_dst);
		scontrol.setPath(path);
		velocities = scontrol.step(lps.cur, initialpos, iplan.tpos);
		
		vr = velocities[0];
		wr = velocities[1];
		
		if(fdebug && (lps != null) && (path != null))
			pout.println(abs_time + "\t" + lps.cur.x() + "\t" + lps.cur.y() + "\t" +
				scontrol.getWheelVelocity() + "\t" + scontrol.getWheelAngle() + "\t" +
				scontrol.getVelocity() + "\t" + scontrol.getAngularVelocity() + "\t" +
				getAbsoluteError(lps.cur, path) + "\t" + getAbsoluteAngleError(lps.cur, path)
			);
	}
	
	protected void beh_manouverFuzzy ()
	{
		double[] velocities;
		
		if (!fcont_time)
		{
			cont_time = System.currentTimeMillis();
			last_time = cont_time;
			
			fcont_time = true;
		}
		
		long cur_time;
		cur_time	= System.currentTimeMillis();
		abs_time	= ((double)(cur_time - cont_time))/1000.0;
		
		fuzzcontrol.setTolPosition(iplan.tol_pos);
		fuzzcontrol.setLookaDistance(looka_dst);
		fuzzcontrol.setPath(path);
		velocities = fuzzcontrol.maneuverStep(lps.cur, initialpos, iplan.tpos);
		
		vr = velocities[0];
		wr = velocities[1];
		
		if(fdebug && (lps != null) && (path != null))
			pout.println(abs_time + "\t" + lps.cur.x() + "\t" + lps.cur.y() + "\t" +
				fuzzcontrol.getWheelVelocity() + "\t" + fuzzcontrol.getWheelAngle() + "\t" +
				fuzzcontrol.getVelocity() + "\t" + fuzzcontrol.getAngularVelocity() + "\t" +
				getAbsoluteError(lps.cur, path) + "\t" + getAbsoluteAngleError(lps.cur, path)
			);
	}
	
	protected void beh_manouverFlatness ()
	{
		double[] velocities;
		
		if (!fcont_time)
		{
			cont_time = System.currentTimeMillis();
			last_time = cont_time;
			
			fcontrol.reset();
			fcontrol.setPath(path);
			fcontrol.init();
			
			fcont_time = true;
		}
		
		long cur_time;
		cur_time	= System.currentTimeMillis();
		abs_time	= ((double)(cur_time - cont_time))/1000.0;
		
		if(!fcontrol.hasFinished(abs_time))
		{
			//double dt_ = ((double)(cur_time - last_time))/1000.0;
			
			last_time	= cur_time;
			
			fcontrol.setPath(path);
			
			//velocities = fcontrol.step(lps.cur, dt_, abs_time);
			velocities = fcontrol.step(lps.cur, abs_time);
		} else {
			velocities = new double[2];
			
			velocities[0] = 0.0;
			velocities[1] = 0.0;
		}
		
		vr = velocities[0];
		wr = velocities[1];
		
		//System.out.println("Time: " + abs_time + " Wheel Angle: " + Math.toDegrees(fcontrol.getWheelAngle()));
		
		if(fdebug && (lps != null) && (path != null))
			pout.println(abs_time + "\t" + lps.cur.x() + "\t" + lps.cur.y() + "\t" +
				fcontrol.getWheelVelocity() + "\t" + fcontrol.getWheelAngle() + "\t" +
				fcontrol.getVelocity() + "\t" + fcontrol.getAngularVelocity() + "\t" +
				getAbsoluteError(lps.cur, path) + "\t" + getAbsoluteAngleError(lps.cur, path)
			);
	}
	
	double getAbsoluteError(Position pos_vehicle, Path exp_path)
	{
		double aerror;
		
		double dist;
		double dist_min = Double.MAX_VALUE;
		
		Position aux_path;
		aux_path = exp_path.first();
		
		while(aux_path != null)
		{
			dist = pos_vehicle.distance(aux_path);
			
			if (dist < dist_min)
				dist_min = dist;
			
			aux_path = exp_path.next();
		}
		
		aerror = dist_min;
		
		return aerror;
	}
	
	double getAbsoluteAngleError(Position pos_vehicle, Path exp_path)
	{
		double aerror;
		
		int index, counter;
		double dist;
		double dist_min = Double.MAX_VALUE;
		
		index = 0;
		counter = 0;
		Position aux_path;
		aux_path = exp_path.first();
		while(aux_path != null)
		{
			dist = pos_vehicle.distance(aux_path);
			
			if (dist < dist_min)
			{
				dist_min = dist;
				index = counter;
			}
			
			aux_path = exp_path.next();
			++counter;
		}
		
		double pangle;
		Position pos1, pos2;
		if(index > 0)
		{
			pos1 = exp_path.at(index);
			pos2 = exp_path.at(index-1);
			pangle = Math.atan2( pos2.y()-pos1.y(), pos2.x()-pos1.x() );
			//pangle = Math.atan2( pos1.y()-pos2.y(), pos1.x()-pos2.x() );
			//pangle = Math.atan2( pos1.x()-pos2.x(), pos1.y()-pos2.y() );
		} else {
			pangle = exp_path.at(0).alpha() + Math.PI;
		}
		
		aerror = pos_vehicle.alpha() - pangle;
		
		// Angle normalisation
		while(aerror >= Math.toRadians(180))	aerror -= Math.toRadians(360);
		while(aerror < Math.toRadians(-180))	aerror += Math.toRadians(360);
		
		return aerror;
	}
	
	protected void beh_motion (int result)
	{
		boolean			brk;
		
		// Check if task completed
		switch (result)
		{
		case ItemBehResult.T_FINISHED:
			vr 	= 0.0;
			wr	= 0.0;
			brk	= true;
			
			// Notify Linda Space the task has been finished
			setResult (result, ItemBehResult.F_OK, idtask);
			break;
			
		case ItemBehResult.T_FAILED:
			vr 	= 0.0;
			wr	= 0.0;
			brk	= true;

			// Notify Linda Space the task has failed
			setResult (result, ItemBehResult.F_BEHIND, idtask);			// or ItemBehResult.F_SIDE
			break;
			
		case ItemBehResult.T_NOTYET:
		default:
			brk	= false;
			
			if (!looka.valid ())
			{
				vr 	= 0.0;
				wr	= 0.0;
				brk	= true;
			}
		}
		
/*		// Do not repeat the same command
		if (horn == lhorn)
			horn		= ItemIForkMotion.TS_NONE;
		else
			lhorn	= horn;
		if (coord == lcoord)
			coord	= ItemIForkMotion.TS_NONE;
		else
			lcoord	= coord;*/
		
		// Send current control values
		mifitem.set (vr, wr, brk, System.currentTimeMillis ());
		mifitem.setFork (frk_hgt, frk_act);
		mifitem.setHorn (horn);
		mifitem.setCoordLight (coord);
		
		if(debug)
			System.out.println("  [IForkController] Item:"+mifitem+" look_valid="+looka.valid());
		linda.write (miftuple);
	}
	
	public void step (long ctime) 
	{
		if (state != RUN)						return;
		
		if (!auto || (lps == null))				return;
		
		// Set last goal received as the current one
		if (idtask != new_id)
		{
			idtask		= new_id;
			iplan.set (newiplan);
				
			if (debug)		System.out.println ("  [IForkCon] Working with task [" + iplan + "] and ID: " + idtask);
		}
		
		// Run BG program
		controller ();

		// Print controller execution information 
		if (debug)
			System.out.println ("  [IForkCon] Control cycle: " + (System.currentTimeMillis () - ctime) + " ms");
	}

	public void notify_config (String space, ItemConfig config)
	{
		super.notify_config (space, config);
		
		model	= (TricycleDrive) rdesc.model;
		
		// Controler
		abs_time = 0.0;		// Init time
		cont_time = System.currentTimeMillis();
		fcont_time = false;
		dt = 0.115;			// Sampling period [s]
		
		// Control methods
		scontrol = new ScrewControl (dt, model);
		fuzzcontrol = new FuzzyControl(dt, model);
		fcontrol = new FlatnessControl (dt, model);
		
		// File dump
		fdebug = false;
		
		if (localgfx)
			win		= new IndoorLPSWindow (robotid);
	}

	public void notify_debug (String space, ItemDebug item)
	{
		super.notify_debug (space, item);
	    	    
		if (debug)
		{
			if (localgfx)
				c_plot.open (c_labels);
			else
				c_dump.open (c_labels);
		}
		else
			c_dump.close ();
	}
	
	public void notify_goal (String space, ItemGoal goal)
	{
		newiplan.set ((IForkPlan) goal.task);		
		new_id		= goal.timestamp.longValue ();		
		new_goal		= true;

		sensor_selection ();

		if (debug)		System.out.println ("  [IForkCon] New task [" + iplan + "] and ID: " + new_id);
	}

	public void notify_path (String space, ItemPath item)
	{
		path		= item.path;
		
		new_goal	= false;
	}
}

