/*
 * (c) 1997-2001 Humberto Martinez
 * (c) 2002 Juan Pedro Canovas, Humberto Martinez
 * (c) 2003 Bernardo Canovas, Humberto Martinez
 * (c) 2004 Humberto Martinez
 */

package tcapps.tcsimulator.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import tc.shared.world.WMBeacon;
import tc.shared.world.WMCBeacon;
import tc.shared.world.World;
import tc.vrobot.RobotData;
import tc.vrobot.RobotDataCtrl;
import tc.vrobot.RobotDesc;
import tc.vrobot.RobotModel;
import tc.vrobot.SensorPos;
import tc.vrobot.TrackerData;
import tc.vrobot.models.TricycleDrive;
import tcapps.tcsimulator.simulator.objects.SimCargo;
import tcapps.tcsimulator.simulator.objects.SimObject;
import tcapps.tcsimulator.simulator.objects.SimObjects;
import tclib.utils.pos.Position;
import tclib.utils.pos.UTMPos;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;
import wucore.utils.math.Angles;
import wucore.utils.math.stat.RandomNumberGenerator;
import devices.data.BeaconData;
import devices.data.CompassData;
import devices.data.GPSData;
import devices.data.InsData;

public class Simulator
{
	static public final int			MAX_ROBOTS	= 20;
	
	public static final int			S_GEOM		= 0;		// Geometrical sonar simulation
	public static final int			S_GALLARDO	= 2;		// Gallardo (Watt & Watt) sonar simulation
	public static final int			S_EXACT		= 3;		// Exact geometrical sonar measures
	
	public static final int			I_GEOM		= 0;		// Geometrical ir simulation
	public static final int			I_EXACT		= 1;		// Exact geometrical ir measures
	public static final int			I_SHARP		= 2;		// Sharp GP2D02 ir simulation
	
	public static final int			LRF_GEOM		= 0;		// Geometrical lrf simulation
	public static final int			LRF_EXACT	= 1;		// Exact geometrical lrf measures
	public static final int			LRF_GAUSS	= 2;		// GAUSS lrf measures
	
	public static final int			LSB_GEOM		= 0;		// Geometrical laser beacom simulation
	public static final int			LSB_EXACT	= 1;		// Exact geometrical laser beacom measures
	public static final int			LSB_GAUSS	= 2;		// GAUSS laser beacom measures
		
	protected static final int		MAXDEPTH		= 5;		// Maximum number of ray reflections
	/**
	 * How often what the simulation has (the poses of the robots and the objects)
	 * is handed to the windows to be drawn, plan view and 3D (ms). The robots
	 * move every cycle of their modules (100 ms, as a rule): handed over slower
	 * than that, they were seen to jump from one pose to the one two or three
	 * cycles later; at this rate every pose is drawn.
	 */
	public static final int 		GFX3D_UPD 	= 40;
	private static int				MOVE_OBJECT_TIME	= 200;  //millis

	// Simulation parameters
	private double[]				tof;					// Time-of-flight buffers for sonar
	private Random					rnd;					// Pseudo-random number generator
	protected long					tgfx;				// Time of graphics update
	protected RandomNumberGenerator rndg; 				// Generator of Random Number for LRF
	
	// Simulated world components
	public SimObjects				objects;				// Animated objects of the world (null until a world is set)
	protected World					map;
	protected String				mapfile;
	protected int					roboindex	= -1;
	protected Position				bpos;
	
	private int						iconcount;
	private boolean[]				objects3D;
	public Line2[][]				icons;
	
	// Robots internal data
	public SimulatorDesc[]			SDESC;
	public RobotDesc[]				RDESC;
	public RobotModel[]				MODEL;
	public String[]					NAMES		= new String[MAX_ROBOTS];		// what each robot is called (the deployment's name), or null
	protected double[][][]			BODY		= new double[MAX_ROBOTS][][];	// what each robot collides as when it is not its outline (see body ()), made when first needed
	protected boolean[]				BODYDONE	= new boolean[MAX_ROBOTS];		// ... and whether that was worked out already
	protected double[][]			PREV		= new double[MAX_ROBOTS][];		// where each robot was before the step it is taking (x, y, a)
	protected double[][]			CMD			= new double[MAX_ROBOTS][3];	// the last control action of each robot: vlin, vlat (m/s), vrot (rad/s)

	/** The last control action of a robot, {vlin, vlat, vrot} (m/s, m/s, rad/s): what an articulated robot is seen walking with. */
	public double[] commands (int i)
	{
		return ((i >= 0) && (i < MAX_ROBOTS)) ? CMD[i] : new double[3];
	}

	/**
	 * Whether the actuators of the robots (the fork of a forklift, the arm of a
	 * manipulator) collide with what is in the world. Off, a robot with an
	 * actuator collides as its body alone, and puts its fork under a pallet
	 * without being pushed off it. Set in code; off until it is needed.
	 */
	static public boolean			COLLIDE_ACTUATORS	= false;
	public double[][]				START		= new double[MAX_ROBOTS][];		// where each robot starts (x, y, a), as it was last put there at a reset

	/** Where the i-th robot starts, as {x, y, a}: where it was put at its last reset, or null when it never was. */
	public double[] startPose (int i)
	{
		return ((i >= 0) && (i < MAX_ROBOTS)) ? START[i] : null;
	}

	/**
	 * The i-th start point of the world, as {x, y, a}: the ones past the robots
	 * are where a referee may send a robot to. Null when the world has no such
	 * point (or no world yet).
	 */
	public double[] worldStart (int i)
	{
		if ((map == null) || (i < 0) || (i >= map.n_starts ()))		return null;

		tc.shared.world.WMStart	st = map.start (i);

		return new double[] { st.x (), st.y (), st.orientation };
	}

	/** Puts a robot back where it starts (see {@link #placeRobot}: only where it is changes). Whether it could be. */
	public boolean restartRobot (int i)
	{
		double[]	p = startPose (i);

		if (p == null)					return false;
		placeRobot (i, p[0], p[1], p[2]);
		return true;
	}

	/** What the i-th robot is called, its number when it has no name. */
	public String robotName (int i)
	{
		return ((i >= 0) && (i < numrobots) && (NAMES[i] != null)) ? NAMES[i] : ("robot " + i);
	}
	public RobotDataCtrl[]			DATA_CTRL;
	public int[]					ROBOINDEX;
	protected Line2[][]				OUTLINE;		// what each robot occupies in the simulation (collisions, what the sensors of the others meet): its icon, or the circle of its radius when it has none
	public Position[][]				VISOBJS;
	public Position[]				VISPOS;
	public int						numrobots;
	public RobotData[] 				lastRobotData; // Stores the last 'RobotData' object received from "SimulatedRobot" to allow 3D representation in the "RefreshThread"
	protected double[][]			campan;			// how the cameras of each robot are turned now (rad), by robot and camera; null rows: never turned
	protected double[][]			camtilt;
	
	// Simulated world visualization
	protected SimulatorListener 		win;
	
	public int[] 					objectPicked; // Indexed by robot id, this array contains the id of the object that the robot has picked. -1 if no object has been picked 
	
	private Runnable refreshThread = new Runnable ()
	{
		public void run ()
		{
			int i;
			
			System.out.println ("  [SIM-Refresh] Refresh thread started.");
			while (win != null)
			{
				for (i=0; i < numrobots; i++)
				{
					if (lastRobotData[i]!=null)
						win.updateData(i,lastRobotData[i]);
				}

				SimObjects	objs = objects;
				if (objs != null)
					for (i = 0; i < objs.numobjects; i++)
						win.updateObjectData (objs.OBJS[i].idsimul, objs.OBJS[i].odesc.pos, objs.OBJS[i].odesc.a);
				
				win.repaint ();
				
				try {
					synchronized (this)
					{
						this.wait(GFX3D_UPD);
					}
				} catch (Exception e) { e.printStackTrace(); };
				
			}
		}	
	};
		
	// Constructors
	public Simulator ()
	{
		int			i;
		
		// Initialise additional parameters and data
		rnd 		= new Random ();
		tgfx		= 0;
		rndg		= new RandomNumberGenerator ();
		
		RDESC		= new RobotDesc[MAX_ROBOTS];
		SDESC		= new SimulatorDesc[MAX_ROBOTS];
		MODEL		= new RobotModel[MAX_ROBOTS];
		DATA_CTRL	= new RobotDataCtrl[MAX_ROBOTS];
		ROBOINDEX	= new int[MAX_ROBOTS];
		OUTLINE		= new Line2[MAX_ROBOTS][];
		VISOBJS		= new Position[MAX_ROBOTS][];
//		VISOBJS		= new Hashtable[MAX_ROBOTS];
		VISPOS		= new Position[MAX_ROBOTS];
		numrobots	= 0;
		iconcount 	= 0;	
		objects3D	= new boolean[MAX_ROBOTS];
		icons 		= new Line2[MAX_ROBOTS][];
		bpos		= new Position ();		
		lastRobotData = new RobotData[MAX_ROBOTS];
		campan		= new double[MAX_ROBOTS][];
		camtilt		= new double[MAX_ROBOTS][];
		objectPicked = new int[MAX_ROBOTS];		
		for (i = 0; i < MAX_ROBOTS; i++)
		{
			VISPOS[i]	= new Position ();
			objectPicked[i] = -1;
		}

		map = null;
	}
	
	/* Class methods */
	static protected double sqr (double x)
	{
		return x * x;
	}

	// Accessors
	public World			getWorld ()												{ return map; }
	public String			getWorldName ()											{ return mapfile; }
	public void				set_data_ctrl (int robotind, RobotDataCtrl datactrl)	{ DATA_CTRL[robotind] = datactrl; }
	public void				closeVisualization3D ()									{ this.win = null; }	

	/** Stops the simulation of the animated objects (the simulator is being discarded). */
	public void				dispose ()												{ if (objects != null) objects.stop (); objects = null; win = null; }
	
	// Instance methods
//	public int allocIcon ()
//	{
//		iconcount ++;
//		
//		return iconcount-1;
//	}
	
	public int allocIcon(){
		int index,i;
//		 Assign an index number to object
		index	= -1;
		for (i = 0; i < iconcount; i++)
			if (!objects3D[i])
				index = i;
		if (index == -1){
			index	= iconcount;
			iconcount ++;
		}
		if (index >= icons.length)			// grow: robots plus as many objects as the world has
		{
			Line2[][]	ni = new Line2[index + MAX_ROBOTS][];
			boolean[]	no = new boolean[index + MAX_ROBOTS];
			System.arraycopy (icons, 0, ni, 0, icons.length);
			System.arraycopy (objects3D, 0, no, 0, objects3D.length);
			icons		= ni;
			objects3D	= no;
		}
		objects3D[index]=true;
		return index;
	}

	public void moveIcon (int index, Line2[] icon, double rx, double ry, double alpha) 
	{
		int			i;
		double		x1, y1;
		double		x2, y2;
		double		xa,	ya, xb,yb;
		double		l1, l2, r1, r2;
		Line2		line;
		
		// the sensor threads of the robots read icons[index] concurrently: build the new segments
		// apart and publish the complete array at the end (filling it in place left null entries)
		Line2[]		moved = new Line2[icon.length];
		for (i = 0; i < icon.length; i++)
		{
			line		= icon[i];

			xa	= line.orig().x();
			ya	= line.orig().y();
			xb	= line.dest().x();
			yb	= line.dest().y();
			
			l1	= Math.sqrt (xa * xa + ya * ya);
			r1	= Math.atan2 (ya, xa) + alpha;
			
			l2	= Math.sqrt (xb * xb + yb * yb);
			r2	= Math.atan2 (yb, xb) + alpha;
			
			x1	= rx + l1 * Math.cos (r1); 
			y1	= ry + l1 * Math.sin (r1); 
			x2	= rx + l2 * Math.cos (r2); 
			y2	= ry + l2 * Math.sin (r2); 
			
			moved[i]	= new Line2 (x1,y1,x2,y2);
		}
		icons[index]	= moved;
	}

	/**
	 * The edge closest to an animated object among the walls and the other
	 * objects (not itself, whose icon is index, nor the robots: an object meets
	 * a robot as its outline, see SimObjects).
	 */
	public Line2 closerObstacle (SimObject obj, int index)
	{
		Line2[][]	others = new Line2[iconcount][];
		int			n = 0;

		for (int k = 0; k < iconcount; k++)
		{
			boolean		robot = false;
			for (int i = 0; i < numrobots; i++)
				if (ROBOINDEX[i] == k)		robot = true;
			if ((k != index) && !robot && (icons[k] != null))
				others[n++]	= icons[k];
		}
		return map.closer (obj.odesc.pos.x (), obj.odesc.pos.y (), others, n, -1);
	}
	
	public void setVisualization (SimulatorListener win) 
	{ 
		int			i;
		
		this.win = win;
		
		if (map!=null)
			win.setWorldmap(map);
		for (i=0;i<numrobots;i++)
			win.addRobot(RDESC[i],SDESC[i]);
		reportObjects ();
		win.repaint();	
		new Thread (refreshThread).start ();
	}

	/** Puts an animated object where a hand on the visualisation left it (see SimObjects.place). */
	/**
	 * Takes note of how a camera of a robot is turned on its mount (pan to the
	 * left, tilt upwards, rad), as the robot turns it before taking a frame, for
	 * whoever draws the robot (the prism of the camera in the 3D world).
	 */
	public void cameraTurned (int robot, int dev, double pan, double tilt)
	{
		if ((robot < 0) || (robot >= MAX_ROBOTS) || (dev < 0))		return;
		if ((campan[robot] == null) || (dev >= campan[robot].length))
		{
			int			n = Math.max (dev + 1, ((RDESC[robot] != null) ? RDESC[robot].MAXCAMERA : 0));
			double[]	p = new double[n], t = new double[n];

			if (campan[robot] != null)
			{
				System.arraycopy (campan[robot], 0, p, 0, campan[robot].length);
				System.arraycopy (camtilt[robot], 0, t, 0, camtilt[robot].length);
			}
			campan[robot]	= p;
			camtilt[robot]	= t;
		}
		campan[robot][dev]	= pan;
		camtilt[robot][dev]	= tilt;
	}

	/** How the cameras of a robot are turned now, pan of each (rad), or null when none was ever turned. */
	public double[] cameraPans (int robot)		{ return ((robot >= 0) && (robot < MAX_ROBOTS)) ? campan[robot] : null; }
	/** ... and the tilt of each. */
	public double[] cameraTilts (int robot)		{ return ((robot >= 0) && (robot < MAX_ROBOTS)) ? camtilt[robot] : null; }

	/**
	 * Puts a robot at a pose by hand, as one is picked up and set down while the
	 * simulation runs, or by a referee: only where the simulation has it changes,
	 * and what the visualisation shows of it. Where the robot thinks it is (its
	 * odometry, and so its LPS) goes on as it was: the robot is not told, as a real
	 * one is not, and its localisation has to find it out from what it sees. Its
	 * speed goes on as it was.
	 */
	synchronized public void placeRobot (int i, double x, double y, double a)
	{
		if ((i < 0) || (i >= numrobots) || (MODEL[i] == null))		return;

		RobotData	data = lastRobotData[i];					// the data of the cycle (its real pose goes there too)

		MODEL[i].relocate (data, x, y, a);
		if (data != null)
		{
			data.real_x	= x;
			data.real_y	= y;
			data.real_a	= a;
		}
	}

	public void placeObject (int i, double x, double y, double a)
	{
		SimObjects	objs = objects;

		if (objs != null)		objs.place (i, x, y, a);
	}

	/** Gives the animated objects to the visualisation (all of them, replacing the previous ones). */
	protected void reportObjects ()
	{
		if (win == null)			return;
		win.removeAllObjects ();
		if (objects == null)		return;
		for (int i = 0; i < objects.numobjects; i++)
			objects.OBJS[i].idsimul = win.addObject (objects.OBJS[i]);
	}
	
	/** Segments of the circle of the radius of a robot that has no icon (a robot drawn by its image or its 3D model alone). */
	static public final int			OUTLINE_SIDES	= 24;

	/**
	 * What a robot occupies in the simulation, in its own frame: the segments of its
	 * icon, or, when it has none (a robot shown by its image or its 3D model), the
	 * circle of its radius as a polygon of {@link #OUTLINE_SIDES} sides -- the same
	 * circle the views draw it as then. Nothing when it has neither.
	 */
	static public Line2[] outline (RobotDesc rdesc)
	{
		Line2[]		o;
		double		r;

		if ((rdesc.icon != null) && (rdesc.icon.length > 0))		return rdesc.icon;
		r	= rdesc.RADIUS;
		if (r <= 0.0)							return new Line2[0];
		o	= new Line2[OUTLINE_SIDES];
		for (int i = 0; i < OUTLINE_SIDES; i++)
		{
			double	a0 = 2.0 * Math.PI * i / OUTLINE_SIDES, a1 = 2.0 * Math.PI * (i + 1) / OUTLINE_SIDES;

			o[i]	= new Line2 ();
			o[i].set (r * Math.cos (a0), r * Math.sin (a0), r * Math.cos (a1), r * Math.sin (a1));
		}
		return o;
	}

	synchronized public int add_robot (RobotDesc rdesc, SimulatorDesc sdesc, RobotModel model, RobotDataCtrl datactrl)
	{
		return add_robot (rdesc, sdesc, model, datactrl, null);
	}

	/** Adds a robot with its name (shown by the visualisation). */
	synchronized public int add_robot (RobotDesc rdesc, SimulatorDesc sdesc, RobotModel model, RobotDataCtrl datactrl, String name)
	{
		NAMES[numrobots] = name;
		RDESC[numrobots] = rdesc;
		SDESC[numrobots] = sdesc;
		MODEL[numrobots] = model;
		DATA_CTRL[numrobots] = datactrl;
		ROBOINDEX[numrobots] = allocIcon ();		
		OUTLINE[numrobots] = outline (rdesc);
		moveIcon (ROBOINDEX[numrobots], OUTLINE[numrobots], model.real_x, model.real_y, model.real_a);
		
		if (win!=null)
			win.addRobot (rdesc, sdesc, name);

		return (numrobots++);				
	}
	
	public void setWorld (String wname)
	{
		if (wname == null)
		{
			System.out.println ("  [SIM] Using no world");
			map			= new World ();
		}
		else
		{
			System.out.println ("  [SIM] Loading world-> "+wname);
			try 
			{
				map			= new World (wname);
			} catch (Exception e)
			{
				System.out.println ("--[SIM] Error loading world <"+wname+">");
				e.printStackTrace();
				map			= new World ();
			}
		}
		mapfile = wname;

		// the animated objects of the world are simulated from now on
		if (objects != null)		objects.stop ();
		objects	= new SimObjects (map, this);
		for (int i = 0; i < MAX_ROBOTS; i++)		objectPicked[i] = -1;		// loads of the previous world
						
		if (this.win != null)
		{
			win.setWorldmap(map);		
			reportObjects ();
		}
	}
	
	protected double sonar (SensorPos a1)
	{
		double			son;
		
		if (map == null) 		return 0.0;
		
		switch (SDESC[roboindex].MODESON)
		{
			case S_GALLARDO:
				son = s_gallardo (a1);
				break;
			case S_EXACT:
				son = s_exact (a1);
				break;
			case S_GEOM:
			default:
				son = s_geom (a1);
		}
		
		return son;
	}		
	
	private double s_exact (SensorPos a1)
	{
		double			xx1, yy1;
		double			xx2, yy2;
		int				n;
		double			a, a2, step;
		double			dist, tdist;
		double			rlen;
		Line2			rout, wall;
		Point2			p;
		
		
		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.theta ());
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.theta ());
		
		a2		= RDESC[roboindex].CONESON / 2.0;
		rlen	= RDESC[roboindex].RANGESON * 2.0;
		n		= Math.max (1, SDESC[roboindex].RAYSON);
		step	= (n > 1) ? (a2 * 2.0) / (double) (n - 1) : 0.0;
		dist	= RDESC[roboindex].RANGESON;
		rout	= new Line2 ();
		for (int k = 0; k < n; k++)						// every ray of its cone, both ends included (one ray: the axis)
		{
			a		= (n > 1) ? -a2 + k * step : 0.0;
			xx2		= xx1 + rlen * Math.cos (MODEL[roboindex].real_a + a1.orientation () + a);
			yy2		= yy1 + rlen * Math.sin (MODEL[roboindex].real_a + a1.orientation () + a);	
			tdist	= RDESC[roboindex].RANGESON;
			
			rout.set (xx1, yy1, xx2, yy2);
			wall 	= map.crossline (rout, icons, iconcount, ROBOINDEX[roboindex]);
			if (wall != null)							// a ray that hits nothing reads the range, and the others are still looked at
			{
				p		= rout.intersection (wall);
				if (p != null)	tdist 	= p.distance (xx1, yy1);
			}
			dist 	= Math.min (dist, tdist);
		}
		
		return dist;
	}		
	
	private double s_geom (SensorPos a1)
	{
		double			dist;
		
		dist = s_exact (a1);
		dist	= (1.0 - SDESC[roboindex].ERRORSON) * dist + (2.0 * SDESC[roboindex].ERRORSON * Math.random () - SDESC[roboindex].ERRORSON) * dist;
		return Math.min (Math.max (dist, RDESC[roboindex].MINIMSON), RDESC[roboindex].RANGESON);
	}		
	
	private double s_gallardo (SensorPos a1)
	{
		double			aa1;
		double			xx1, yy1;
		double			xx2, yy2;
		double			rho0, nu0;
		double			rhoi, delta, beta;
		double			dist, a, drhoi;
		double			dout, dk, dn;
		double			min, count, wgt;
		double			rlen;
		Line2			sensor, wall, aux;
		Line2			rout, rin, rref;
		Point2			p;
		int				depth;
		int				i;
		boolean			reach;
		
		tof			= new double[SDESC[roboindex].RAYSON];
		
		sensor	= new Line2 ();
		rout	= new Line2 ();
		rin		= new Line2 ();
		aux		= new Line2 ();
		
		rho0	= RDESC[roboindex].CONESON;
		nu0		= RDESC[roboindex].CONESON;
		delta	= rho0 / (double) (SDESC[roboindex].RAYSON - 1);
		rlen	= RDESC[roboindex].RANGESON * 2.0;
		
		aa1		= a1.orientation ();
		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.theta ());
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.theta ());
		xx2		= xx1 + rlen * Math.cos (MODEL[roboindex].real_a + aa1);	
		yy2		= yy1 + rlen * Math.sin (MODEL[roboindex].real_a + aa1);		
		sensor.set (xx1, yy1, xx2, yy2);
		
		for (rhoi = (aa1 - rho0 / 2.0), i = 0; i < SDESC[roboindex].RAYSON; rhoi += delta, i++)
		{
			reach	= false;
			depth	= 0;
			tof[i]	= 0.0;
			xx2		= xx1 + rlen * Math.cos (MODEL[roboindex].real_a + rhoi);	
			yy2		= yy1 + rlen * Math.sin (MODEL[roboindex].real_a + rhoi);		
			drhoi	= rhoi - aa1;
			rout.set (xx1, yy1, xx2, yy2);
			
			while (depth < MAXDEPTH)
			{		
				wall 	= map.crossline (rout, icons, iconcount, ROBOINDEX[roboindex]);		
				if (wall == null)				
					break;
				
				p		= rout.intersection (wall);
				dout	= wall.angle_norm (rout);
				aux.set (rout.orig (), p);
				beta	= 2.0 * (Math.PI - dout);	
				rref	= rout.reflection (p, beta, rlen);	
				rin.set (xx1, yy1, p.x (), p.y ());
				
				dk		= Angles.radnorm_90 (rref.angle_norm (rin));
				dn		= Angles.radnorm_90 (sensor.angle_norm (rin));
				drhoi	= Angles.radnorm_90 (drhoi);
				a		= Math.exp (-2.0 * (sqr (drhoi / rho0) + sqr (dn / rho0) + sqr (dk / nu0)));
				
				if (a > SDESC[roboindex].SENSIBSON)   
				{
					tof[i]	= (tof[i] + p.distance (rout.orig ()) + p.distance (xx1, yy1)) / 2.0;	
					reach	= true;
					break;
				}
				else
				{
					tof[i]	+= p.distance (rout.orig ());	
					rout.set (rref);
					depth ++;
				} 
			}
			if (!reach) tof[i] = RDESC[roboindex].RANGESON;
		}
		
		// Compute the minimum lenght ray
		min 	= RDESC[roboindex].RANGESON;
		for (i = 0; i < SDESC[roboindex].RAYSON; i++)
			if (tof[i] < min) min = tof[i];
			
			// Average the rays which differ less than 5%
		dist 	= 0.0;
		count	= 0.0;
		for (i = 0; i < SDESC[roboindex].RAYSON; i++)
			if (tof[i] - min < 0.10)
			{
				wgt = 1.0 - Math.abs ((double) SDESC[roboindex].RAYSON / 2.0 - (double) i) * delta * delta;
				dist += wgt * tof[i];
				count += wgt;
			}
		if (count == 0.0)												// Humberto's modified model
			dist = RDESC[roboindex].RANGESON;
		else
			dist = Math.max (dist / count, RDESC[roboindex].MINIMSON);	
//		dist = Math.max (min, rdesc.MINIMSON);							// Gallardo's original model
		
		return dist;
	}		
	
	protected double ir (SensorPos a1)
	{
		double			ir;
		
		if (map == null) 		return 0.0;
		
		switch (SDESC[roboindex].MODEIR)
		{
			case I_SHARP:
				ir = i_sharp (a1);
				break;
			case I_EXACT:
				ir = i_exact (a1);
				break;
			case I_GEOM:
			default:
				ir = i_geom (a1);
		}
		
		return ir;
	}		
	
	private double i_exact (SensorPos a1)
	{
		double			xx1, yy1;
		double			xx2, yy2;
		int				n;
		double			a, a2, step;
		double			dist, tdist;
		double			rlen;
		Line2			rout, wall;
		Point2			p;
		
		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.theta ());
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.theta ());
		
		a2		= RDESC[roboindex].CONEIR / 2.0;
		rlen	= RDESC[roboindex].RANGEIR * 2.0;
		n		= Math.max (1, SDESC[roboindex].RAYIR);
		step	= (n > 1) ? (a2 * 2.0) / (double) (n - 1) : 0.0;
		dist	= RDESC[roboindex].RANGEIR;
		rout	= new Line2 ();
		for (int k = 0; k < n; k++)						// every ray of its cone, both ends included (one ray: the axis)
		{
			a		= (n > 1) ? -a2 + k * step : 0.0;
			xx2		= xx1 + rlen * Math.cos (MODEL[roboindex].real_a + a1.orientation () + a);
			yy2		= yy1 + rlen * Math.sin (MODEL[roboindex].real_a + a1.orientation () + a);	
			tdist	= RDESC[roboindex].RANGEIR;
			
			rout.set (xx1, yy1, xx2, yy2);
			wall 	= map.crossline (rout, icons, iconcount, ROBOINDEX[roboindex]);
			if (wall != null)							// a ray that hits nothing reads the range, and the others are still looked at
			{
				p		= rout.intersection (wall);
				if (p != null)	tdist 	= p.distance (xx1, yy1);
			}
			dist 	= Math.min (dist, tdist);
		}
		
		return dist;
	}		
	
	private double i_geom (SensorPos a1)
	{
		double			dist;
		
		dist = i_exact (a1);
		dist	= (1.0 - SDESC[roboindex].ERRORIR) * dist + (2.0 * SDESC[roboindex].ERRORIR * Math.random () - SDESC[roboindex].ERRORIR) * dist;
		return Math.min (Math.max (dist, RDESC[roboindex].MINIMIR), RDESC[roboindex].RANGEIR);
	}		
	
	private double i_sharp (SensorPos a1)
	{
		double			dec;
		double			dist, rdist;
		
		rdist = i_exact (a1) * 100.0;
		if (rdist < 25.0)									// Sharp GP2D02 IR linearized MODEL[roboindex]
		{
			dec		= Math.rint (-5.3333 * rdist + 253.3333);
			dist	= (253.3333 - dec) / 5.3333;
		}
		else if (rdist < 60.0)
		{
			dec		= Math.rint (-1.1428 * rdist + 148.5714);
			dist	= (148.5714 - dec) / 1.1428;
		}
		else
		{
			dec		= Math.rint (-0.25 * rdist + 95.0);
			dist	= (95.0 - dec) / 0.25;
		}
		
		dist	= dist / 100.0;
		return Math.min (Math.max (dist, RDESC[roboindex].MINIMIR), RDESC[roboindex].RANGEIR);
	}		
	
	protected double[] lrf (SensorPos a1)
	{
		double			lrf[];
		
		if (map == null) 		return null;
		
		switch (SDESC[roboindex].MODELRF)
		{
			case LRF_EXACT:
				lrf = lrf_exact (a1);
				break;
			case LRF_GAUSS:
				lrf = lrf_gauss (a1);
				break;
			case LRF_GEOM:
			default:
				lrf = lrf_geom (a1);
		}

		// what it reads is never beyond its range nor short of its minimum, as for the
		// sonars and the infrared: a wall farther away is not seen, the ray reads the
		// most it can (the rays are traced twice as far as the range, and the noise
		// of the models may take a reading past it either way)
		for (int i = 0; i < lrf.length; i++)
			lrf[i]	= Math.min (Math.max (lrf[i], RDESC[roboindex].MINIMLRF), RDESC[roboindex].RANGELRF);
		return lrf;
	}
	
	private double[] lrf_exact (SensorPos a1)
	{
		int 			i;
		double			xx1, yy1;
		double			xx2, yy2;
		double			a, a2, step;
		double			tdist;
		double			rlen;
		double[]		lrf_measures;
		Line2			rout, wall;
		Point2			p;
		
		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.theta ());
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.theta ());
		
		lrf_measures = new double[RDESC[roboindex].RAYLRF];
		a2		= RDESC[roboindex].CONELRF * 0.5;
		rlen	= RDESC[roboindex].RANGELRF * 2.0;
		step	= RDESC[roboindex].CONELRF / (double) (RDESC[roboindex].RAYLRF - 1);
		rout	= new Line2 ();
		
		for (i = 0, a = -a2; i < RDESC[roboindex].RAYLRF; i++, a += step)
		{
			xx2		= xx1 + rlen * Math.cos (MODEL[roboindex].real_a + a1.orientation () + a);
			yy2		= yy1 + rlen * Math.sin (MODEL[roboindex].real_a + a1.orientation () + a);	
			tdist	= RDESC[roboindex].RANGELRF;
			
			rout.set (xx1, yy1, xx2, yy2);
			wall 	= map.crossline (rout, icons, iconcount, ROBOINDEX[roboindex]);						
			if (wall != null)									
			{				
				p		= rout.intersection (wall);
				if (p != null)	tdist 	= p.distance (xx1, yy1);
			}
			lrf_measures[i] = tdist;					
		}
		
		return lrf_measures;
	}
	
	private double[] lrf_geom (SensorPos a1)
	{
		int 				a;
		double[]			dist;
		
		dist = lrf_exact (a1);
		
		for (a = 0; a < RDESC[roboindex].RAYLRF; a++) {
			dist[a]	= (1.0 - SDESC[roboindex].ERRORLRF) * dist[a] + (2.0 * SDESC[roboindex].ERRORLRF * Math.random () - SDESC[roboindex].ERRORLRF) * dist[a];
		}
		return dist;
	}		
	
	private double[] lrf_gauss (SensorPos a1)
	{
		int 				a;
		double[]			dist;
		
		dist = lrf_exact (a1);
		
		for (a = 0; a < RDESC[roboindex].RAYLRF; a++) {
			dist[a] = rndg.nextGaussian (dist[a],SDESC[roboindex].ERRORLRFGAUSS);
		}
		return dist;
	}
	
	/**
	 * A laser beacon scanner: the reflectors it sees, with the bearing (rad, from
	 * where it looks) and the range (m) of each from where it sits, the nearest
	 * BEACLSB of them; and, as a NAV200 does, the pose of the robot when it sees
	 * three or more (quality 90; 0 when it sees fewer). A reflector is seen when it
	 * is within its range (MINIMLSB to RANGELSB) and its aperture (CONELSB), and
	 * nothing is in between: no wall, no static object, no animated object, no
	 * other robot. A cylinder is aimed at on the side that faces the scanner, and
	 * a strip at its middle, and a strip is seen only when it is not looked at
	 * edgewise (more than REFLSB off its line). The readings are as exact as the
	 * simulation mode of the family says (MODELSB): exact, with a relative error
	 * (ERRORANGLELSB, ERRORRANGELSB) or with a gaussian one (ERRORANGLELSBGAUSS in
	 * degrees, ERRORRANGELSBGAUSS in metres).
	 */
	protected void beacons (SensorPos a1, BeaconData out)
	{
		RobotDesc			rd = RDESC[roboindex];
		SimulatorDesc		sd = SDESC[roboindex];
		RobotModel			m = MODEL[roboindex];
		double				sx = m.real_x + a1.rho () * Math.cos (m.real_a + a1.theta ());
		double				sy = m.real_y + a1.rho () * Math.sin (m.real_a + a1.theta ());
		double				look = m.real_a + a1.orientation ();
		List<double[]>		seen = new ArrayList<double[]> ();		// {bearing, range}

		if (map != null)
		{
			if (map.cbeacons () != null)
				for (WMCBeacon b : map.cbeacons ())
				{
					if (b == null)		continue;

					double	dx = b.x () - sx, dy = b.y () - sy, d = Math.hypot (dx, dy);

					if (d <= b.radius ())		continue;
					see (sx, sy, look, b.x () - b.radius () * dx / d, b.y () - b.radius () * dy / d, seen);
				}
			if (map.beacons () != null)
				for (WMBeacon b : map.beacons ())
				{
					Line2	l = (b != null) ? b.getLine () : null;

					if (l == null)		continue;

					double	mx = (l.orig ().x () + l.dest ().x ()) / 2.0, my = (l.orig ().y () + l.dest ().y ()) / 2.0;
					double	inc = Angles.radnorm_180 (b.getAng () - Math.atan2 (my - sy, mx - sx));

					if (Math.abs (Math.sin (inc)) <= Math.sin (Math.max (0.0, rd.REFLSB)))		continue;		// edgewise: it reflects nothing back
					see (sx, sy, look, mx, my, seen);
				}
		}

		// the nearest ones, as many as it can tell apart
		seen.sort ((u, v) -> Double.compare (u[1], v[1]));
		if ((rd.BEACLSB > 0) && (seen.size () > rd.BEACLSB))		seen = new ArrayList<double[]> (seen.subList (0, rd.BEACLSB));

		double[]		bearings = new double[seen.size ()], ranges = new double[seen.size ()];

		for (int k = 0; k < seen.size (); k++)
		{
			double	b = seen.get (k)[0], r = seen.get (k)[1];

			switch ((sd != null) ? sd.MODELSB : LSB_EXACT)
			{
				case LSB_GEOM:
					b	= b * (1.0 + sd.ERRORANGLELSB * (2.0 * Math.random () - 1.0));
					r	= r * (1.0 + sd.ERRORRANGELSB * (2.0 * Math.random () - 1.0));
					break;
				case LSB_GAUSS:
					b	= b + rnd.nextGaussian () * sd.ERRORANGLELSBGAUSS * Angles.DTOR;
					r	= r + rnd.nextGaussian () * sd.ERRORRANGELSBGAUSS;
					break;
				default:
			}
			bearings[k]	= Angles.radnorm_180 (b);
			ranges[k]	= Math.max (0.0, r);
		}

		bpos.set (m.real_x, m.real_y, m.real_a);
		out.setPosition (bpos);
		out.setSeen (bearings, ranges);
		out.setNumber (seen.size ());
		out.setQuality ((seen.size () >= 3) ? 90 : 0);
		out.setValid (true);
	}

	/** A reflector at (x, y), if the scanner at (sx, sy) looking at look sees it: in its range and aperture, and nothing in between. */
	private void see (double sx, double sy, double look, double x, double y, List<double[]> seen)
	{
		RobotDesc		rd = RDESC[roboindex];
		double			dx = x - sx, dy = y - sy, d = Math.hypot (dx, dy);
		double			bearing = Angles.radnorm_180 (Math.atan2 (dy, dx) - look);

		if ((d < rd.MINIMLSB) || (d <= 1e-6) || ((rd.RANGELSB > 0.0) && (d > rd.RANGELSB)))		return;
		if ((rd.CONELSB < 2.0 * Math.PI - 1e-6) && (Math.abs (bearing) > rd.CONELSB / 2.0))		return;

		// a little short of it: a strip on a wall is not behind the wall
		double			ex = x - 0.02 * dx / d, ey = y - 0.02 * dy / d;

		if (map.crossline (new Line2 (sx, sy, ex, ey), icons, iconcount, ROBOINDEX[roboindex]) != null)		return;
		seen.add (new double[] { bearing, d });
	}

	protected double[] radar (SensorPos a1)
	{
		int 			i;
		double			xx1, yy1;
		double			xx2, yy2;
		double			a, a2, step;
		double			tdist;
		double[]		rdr_measures;
		Line2			rout, wall;
		Point2			p;
		
		if (map == null) 		return null;

		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.theta ());
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.theta ());
		
		rdr_measures = new double[SDESC[roboindex].RAYRAD];
		a2		= RDESC[roboindex].CONETRK * 0.5;
		step	= RDESC[roboindex].CONETRK / (double) (SDESC[roboindex].RAYRAD - 1);
		rout	= new Line2 ();
		
		for (i = 0, a = -a2; i < SDESC[roboindex].RAYRAD; i++, a += step)
		{
			xx2		= xx1 + RDESC[roboindex].RANGETRK * Math.cos (MODEL[roboindex].real_a + a1.orientation () + a);
			yy2		= yy1 + RDESC[roboindex].RANGETRK * Math.sin (MODEL[roboindex].real_a + a1.orientation () + a);	
			tdist	= RDESC[roboindex].RANGETRK;
			
			rout.set (xx1, yy1, xx2, yy2);
			wall 	= map.crossline (rout, icons, iconcount, ROBOINDEX[roboindex]);						
			if (wall != null)									
			{				
				p		= rout.intersection (wall);
				if (p != null)	tdist 	= p.distance (xx1, yy1);
			}
			rdr_measures[i] = tdist;					
		}
		
		return rdr_measures;
	}
	
	/**
	 * What the robots collide with: the walls, the icons of the visible static
	 * objects of the world (open ones, like a net, which a robot can get into
	 * through their mouth), the icons of the animated objects that have no
	 * dynamics of their own (a robot put on the field as a figure, say: it is
	 * there as much as a wall is) and the icons of the other robots. The
	 * objects that move by themselves (a ball) are not: they get out of the way
	 * of the robot themselves (see {@link SimObjects}), which is how a robot
	 * pushes a ball; nor are the loads, which a robot picks.
	 */
	protected java.util.List<Line2> obstacles (int robotind)
	{
		java.util.List<Line2>	edges = new java.util.ArrayList<Line2> ();

		if (map == null)		return edges;
		if (map.walls () != null)
			for (Line2 l : map.walls ().getLines ())		edges.add (l);
		for (tc.shared.world.WMObject ob : map.objects ())
			if (ob.visible)
				for (Line2 l : ob.absIcon ())				edges.add (l);
		SimObjects		objs = objects;
		if (objs != null)
			for (int i = 0; i < objs.numobjects; i++)
			{
				int		k = objs.iconOf (i);

				if ((objs.OBJS[i] != null) && (objs.OBJS[i].getClass () == SimObject.class) && (k >= 0) && (k < icons.length) && (icons[k] != null))
					for (Line2 l : icons[k])				edges.add (l);
			}
		for (int i = 0; i < numrobots; i++)
			if ((i != robotind) && (icons[ROBOINDEX[i]] != null))
				for (Line2 l : icons[ROBOINDEX[i]])			edges.add (l);
		return edges;
	}

	/** The point of an edge closest to (x, y): {x, y, distance}. */
	static protected double[] closestOn (Line2 e, double x, double y)
	{
		double		ex = e.dest ().x () - e.orig ().x (), ey = e.dest ().y () - e.orig ().y ();
		double		len2 = ex * ex + ey * ey;
		double		t = (len2 > 0.0) ? ((x - e.orig ().x ()) * ex + (y - e.orig ().y ()) * ey) / len2 : 0.0;
		double		px, py;

		t	= Math.max (0.0, Math.min (1.0, t));
		px	= e.orig ().x () + t * ex;
		py	= e.orig ().y () + t * ey;
		return new double[] { px, py, Math.sqrt ((x - px) * (x - px) + (y - py) * (y - py)) };
	}

	/**
	 * What a robot collides as when it is not its outline (see {@link #collide}),
	 * in its own frame: discs {dx, dy, r}; null when it is its outline. When its
	 * actuator is not to collide ({@link #COLLIDE_ACTUATORS}) and the robot has
	 * one (a 3D model of it) and its bumpers outline its body, it is that body:
	 * discs of half the width of the outline, side by side along its length,
	 * which leave the fork out of the collision and let it go under a pallet.
	 */
	protected double[][] body (int robotind)
	{
		if (BODYDONE[robotind])				return BODY[robotind];

		RobotDesc		rd = RDESC[robotind];
		double[][]		discs = null;
		boolean			actuator = (SDESC[robotind] != null) && (SDESC[robotind].V3DLIFT != null);

		if (!COLLIDE_ACTUATORS && actuator && (rd.MAXBUMPER > 0) && (rd.bumfeat != null))
		{
			double		x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE;

			for (int i = 0; i < rd.MAXBUMPER; i++)
			{
				Line2	b = rd.bumfeat[i];

				if (b == null)		continue;
				x0 = Math.min (x0, Math.min (b.orig ().x (), b.dest ().x ()));	x1 = Math.max (x1, Math.max (b.orig ().x (), b.dest ().x ()));
				y0 = Math.min (y0, Math.min (b.orig ().y (), b.dest ().y ()));	y1 = Math.max (y1, Math.max (b.orig ().y (), b.dest ().y ()));
			}
			if ((x1 > x0) && (y1 > y0))
			{
				// the shorter side gives the discs, laid along the longer one from end to end
				boolean		alongX = (x1 - x0) >= (y1 - y0);
				double		r = (alongX ? (y1 - y0) : (x1 - x0)) / 2.0;
				double		from = (alongX ? x0 : y0) + r, to = (alongX ? x1 : y1) - r;
				double		mid = alongX ? (y0 + y1) / 2.0 : (x0 + x1) / 2.0;
				int			n = Math.max (1, (int) Math.ceil ((to - from) / r)) + 1;		// no gap wider than the radius between centres

				discs	= new double[n][];
				for (int i = 0; i < n; i++)
				{
					double	c = (n > 1) ? from + (to - from) * i / (n - 1) : (from + to) / 2.0;

					discs[i]	= alongX ? new double[] { c, mid, r } : new double[] { mid, c, r };
				}
			}
		}
		BODY[robotind]		= discs;
		BODYDONE[robotind]	= true;
		return discs;
	}

	/**
	 * The robot against what it can hit: it is the disc of its radius (or the
	 * discs of its body, see {@link #body}), and an edge it overlaps puts it out
	 * of the way (the deepest one first, a few times, so that it comes out of a
	 * corner too), which is what lets it slide along a wall instead of stopping
	 * dead against it. Its bumpers are set from where it was touched. Returns
	 * whether it touched anything.
	 */
	protected boolean collide (int robotind, RobotData data)
	{
		double[][]	discs = body (robotind);

		if (map == null)		return false;
		if (discs == null)		return collideOutline (robotind, data);
		return collideDiscs (robotind, data, discs);
	}

	/**
	 * The robot as its outline (its icon, or the circle of its radius when it has
	 * none: what the others see and hit it as) against what it can hit: a step
	 * that would take the outline across an edge is cut short where it touches,
	 * and what is left of it goes on along that edge (with the turn, if there is
	 * room for it), which is what lets it slide along a wall instead of stopping
	 * dead against it. Its bumpers are set from where it was touched. A robot
	 * that already overlapped something before the step (put there by hand) is
	 * let move, so that it can get out. Returns whether it touched anything.
	 */
	protected boolean collideOutline (int robotind, RobotData data)
	{
		Line2[]			o = OUTLINE[robotind];
		RobotModel		m = MODEL[robotind];
		double[]		p = PREV[robotind];
		double[]		to = { m.real_x, m.real_y, m.real_a };
		double			reach = 0.0;
		java.util.List<Line2>	edges = new java.util.ArrayList<Line2> ();

		if ((o == null) || (o.length == 0) || (p == null))		return false;
		for (Line2 l : o)
			if (l != null)		reach = Math.max (reach, Math.max (Math.hypot (l.orig ().x (), l.orig ().y ()), Math.hypot (l.dest ().x (), l.dest ().y ())));
		// only the edges within its reach on the way: not every wall against every side of it
		double		step = Math.hypot (to[0] - p[0], to[1] - p[1]);
		for (Line2 e : obstacles (robotind))
			if (closestOn (e, (p[0] + to[0]) / 2.0, (p[1] + to[1]) / 2.0)[2] <= reach + step / 2.0 + 0.01)		edges.add (e);
		if (edges.isEmpty () || (hit (o, to, edges) == null) || (hit (o, p, edges) != null))		return false;

		// how far it gets: the last of the step that is free
		double		lo = 0.0, hi = 1.0;
		for (int i = 0; i < 12; i++)
		{
			double	mid = (lo + hi) / 2.0;
			if (hit (o, along (p, to, mid), edges) != null)		hi = mid;
			else												lo = mid;
		}
		double[]	at = along (p, to, lo);
		Line2		e = hit (o, along (p, to, hi), edges);			// what it touched

		// and on along what it touched, with the rest of the turn if there is room for it
		if (e != null)
		{
			double	ex = e.dest ().x () - e.orig ().x (), ey = e.dest ().y () - e.orig ().y (), el = Math.hypot (ex, ey);
			double	s = (el > 0.0) ? ((to[0] - at[0]) * ex + (to[1] - at[1]) * ey) / (el * el) : 0.0;
			double	da = Angles.radnorm_180 (to[2] - at[2]);
			double[][]	tries = { { at[0] + s * ex, at[1] + s * ey, at[2] + da }, { at[0] + s * ex, at[1] + s * ey, at[2] } };

			for (double[] t : tries)
			{
				double	l0 = 0.0, h0 = 1.0;

				if (hit (o, t, edges) == null)		{ at = t;	break; }
				for (int i = 0; i < 10; i++)
				{
					double	mid = (l0 + h0) / 2.0;
					if (hit (o, along (at, t, mid), edges) != null)		h0 = mid;
					else												l0 = mid;
				}
				if (l0 > 0.0)		{ at = along (at, t, l0);	break; }
			}
		}

		// a robot that was stopped short rebounds a little, a bit to one side and a bit
		// turned, by chance: a real one does not stand dead still against what it hit,
		// and a robot stuck on a corner gets a way out of it
		if (e != null)
			at	= bounce (o, edges, e, p, at, step);

		// where it is, and its odometry by as much (it was put there as the real robot is, by what it hit)
		double		dx = at[0] - to[0], dy = at[1] - to[1], da = Angles.radnorm_180 (at[2] - to[2]);

		m.real_x	= at[0];		m.real_y	= at[1];		m.real_a	= Angles.radnorm_180 (at[2]);
		m.odom_x	+= dx;			m.odom_y	+= dy;			m.odom_a	= Angles.radnorm_180 (m.odom_a + da);
		if (e != null)
		{
			double[]	c = closestOn (e, at[0], at[1]);
			bumped (robotind, data, c[0] - at[0], c[1] - at[1]);
		}
		data.location (m.odom_x, m.odom_y, m.odom_a);
		m.backup (data);
		return true;
	}

	/** How much a robot rebounds from what it hits, as a share of the step it lost against it. */
	static public final double		BOUNCE		= 0.3;
	/** How much it may be turned by it, either way (deg). */
	static public final double		BOUNCE_TURN	= 3.0;

	/**
	 * The pose of a robot that hit something, rebounding: when it lost most of its
	 * step (more than half), it is put back from the edge it hit, away from where
	 * it touched, by a random part of what it lost (BOUNCE: 15 to 45 %), and
	 * moved as much to one side or the other and turned up to BOUNCE_TURN degrees,
	 * at random. A rebound that would put it into something else is tried without
	 * the turn, then not at all.
	 */
	protected double[] bounce (Line2[] o, java.util.List<Line2> edges, Line2 e, double[] from, double[] at, double step)
	{
		double		lost = step - Math.hypot (at[0] - from[0], at[1] - from[1]);
		double[]	c;
		double		nx, ny, n, back, side, turn;

		if ((step < 1e-6) || (lost < 0.5 * step))		return at;			// it slid on: no need

		c	= closestOn (e, at[0], at[1]);
		nx	= at[0] - c[0];			ny	= at[1] - c[1];
		n	= Math.hypot (nx, ny);
		if (n < 1e-9)									{ nx = from[0] - at[0];		ny = from[1] - at[1];	n = Math.hypot (nx, ny); }
		if (n < 1e-9)									return at;
		nx	/= n;					ny	/= n;

		back	= BOUNCE * lost * (0.5 + rnd.nextDouble ());
		side	= BOUNCE * lost * (2.0 * rnd.nextDouble () - 1.0);
		turn	= Math.toRadians (BOUNCE_TURN) * (2.0 * rnd.nextDouble () - 1.0);

		double[]	b = { at[0] + nx * back - ny * side, at[1] + ny * back + nx * side, at[2] + turn };

		if (hit (o, b, edges) == null)					return b;
		b[2]	= at[2];
		if (hit (o, b, edges) == null)					return b;
		return at;
	}

	/** A pose a share of the way from one to another (the turn the short way round). */
	static protected double[] along (double[] a, double[] b, double t)
	{
		return new double[] { a[0] + t * (b[0] - a[0]), a[1] + t * (b[1] - a[1]), a[2] + t * Angles.radnorm_180 (b[2] - a[2]) };
	}

	/** The first of some edges an outline (in the frame of the robot) crosses or touches at a pose; null when none. */
	static protected Line2 hit (Line2[] outline, double[] pose, java.util.List<Line2> edges)
	{
		double		ca = Math.cos (pose[2]), sa = Math.sin (pose[2]);

		for (Line2 l : outline)
		{
			if (l == null)		continue;

			double	x0 = pose[0] + l.orig ().x () * ca - l.orig ().y () * sa, y0 = pose[1] + l.orig ().x () * sa + l.orig ().y () * ca;
			double	x1 = pose[0] + l.dest ().x () * ca - l.dest ().y () * sa, y1 = pose[1] + l.dest ().x () * sa + l.dest ().y () * ca;

			for (Line2 e : edges)
				if (crosses (x0, y0, x1, y1, e.orig ().x (), e.orig ().y (), e.dest ().x (), e.dest ().y ()))		return e;
		}
		return null;
	}

	/** Whether two segments cross or touch. */
	static protected boolean crosses (double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy)
	{
		double		d1 = side (cx, cy, dx, dy, ax, ay), d2 = side (cx, cy, dx, dy, bx, by);
		double		d3 = side (ax, ay, bx, by, cx, cy), d4 = side (ax, ay, bx, by, dx, dy);

		if ((((d1 > 0) && (d2 < 0)) || ((d1 < 0) && (d2 > 0))) && (((d3 > 0) && (d4 < 0)) || ((d3 < 0) && (d4 > 0))))		return true;
		return ((d1 == 0) && on (cx, cy, dx, dy, ax, ay)) || ((d2 == 0) && on (cx, cy, dx, dy, bx, by))
				|| ((d3 == 0) && on (ax, ay, bx, by, cx, cy)) || ((d4 == 0) && on (ax, ay, bx, by, dx, dy));
	}

	static private double side (double ax, double ay, double bx, double by, double px, double py)
	{
		return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
	}

	static private boolean on (double ax, double ay, double bx, double by, double px, double py)
	{
		return (Math.min (ax, bx) <= px) && (px <= Math.max (ax, bx)) && (Math.min (ay, by) <= py) && (py <= Math.max (ay, by));
	}

	/** What a robot occupies now, in the world: its outline where it is (null when it has none). */
	public Line2[] robotOutline (int robotind)
	{
		return ((robotind >= 0) && (robotind < numrobots) && (icons != null)) ? icons[ROBOINDEX[robotind]] : null;
	}

	/**
	 * The robot as discs (see {@link #body}) against what it can hit: an edge it
	 * overlaps puts it out of the way (the deepest one first, a few times, so that
	 * it comes out of a corner too). Returns whether it touched anything.
	 */
	protected boolean collideDiscs (int robotind, RobotData data, double[][] discs)
	{
		boolean		hit = false;

		for (int pass = 0; pass < 6; pass++)
		{
			Line2		deepest = null;
			double[]	where = null;
			double		into = 0.0;
			double		cx = 0.0, cy = 0.0;										// the centre of the disc that went deepest
			double		ca = Math.cos (MODEL[robotind].real_a), sa = Math.sin (MODEL[robotind].real_a);
			java.util.List<Line2>	edges = obstacles (robotind);

			for (double[] d : discs)
			{
				double	dx = MODEL[robotind].real_x + d[0] * ca - d[1] * sa;
				double	dy = MODEL[robotind].real_y + d[0] * sa + d[1] * ca;

				for (Line2 e : edges)
				{
					double[]	c = closestOn (e, dx, dy);
					if ((d[2] - c[2] > into) && (d[2] - c[2] > 1e-6))		{ into = d[2] - c[2]; deepest = e; where = c; cx = dx; cy = dy; }
				}
			}
			if (deepest == null)		break;

			double		nx = cx - where[0], ny = cy - where[1];
			double		n = Math.sqrt (nx * nx + ny * ny);

			if (n < 1e-9)											// dead on the edge: out the way it came from
			{
				nx	= -Math.cos (MODEL[robotind].real_a);
				ny	= -Math.sin (MODEL[robotind].real_a);
				n	= 1.0;
			}
			nx	/= n;		ny /= n;
			MODEL[robotind].real_x	+= nx * (into + 0.001);
			MODEL[robotind].real_y	+= ny * (into + 0.001);
			MODEL[robotind].odom_x	+= nx * (into + 0.001);
			MODEL[robotind].odom_y	+= ny * (into + 0.001);
			bumped (robotind, data, -nx, -ny);
			hit	= true;
		}
		if (hit)
		{
			data.location (MODEL[robotind].odom_x, MODEL[robotind].odom_y, MODEL[robotind].odom_a);
			MODEL[robotind].backup (data);
		}
		return hit;
	}

	/** The bumpers the robot was touched on, from the way the touch came (dx, dy in the world). */
	protected void bumped (int robotind, RobotData data, double dx, double dy)
	{
		double		phi = Angles.radnorm_180 (Math.atan2 (dy, dx) - MODEL[robotind].real_a);

		for (int i = 0; i < RDESC[robotind].MAXBUMPER; i++)
		{
			Line2		b = RDESC[robotind].bumfeat[i];
			double		a1, a2;

			if (b == null)		continue;
			a1	= Math.atan2 (b.orig ().y (), b.orig ().x ());
			a2	= Math.atan2 (b.dest ().y (), b.dest ().x ());
			if (Math.abs (Angles.radnorm_180 (phi - a1)) + Math.abs (Angles.radnorm_180 (phi - a2))
					<= Math.abs (Angles.radnorm_180 (a2 - a1)) + 1e-6)
				data.bumpers[i]	= true;
		}
	}

	public void reset (int robotind, RobotData data, World map)
	{
		if (MODEL[robotind] != null) 
		{
			if (map != null)
			{
				tc.shared.world.WMStart	st = map.start (robotind);			// START_i for the i-th robot (the first one when there are fewer)
				MODEL[robotind].position (data, st.x (), st.y (), st.orientation);
				START[robotind]	= new double[] { st.x (), st.y (), st.orientation };
			}
			else
				MODEL[robotind].position (data, 0.0, 0.0, 0.0);		
		}
	}
	
	/** Places a robot at an explicit pose. */
	public void reset (int robotind, RobotData data, double x, double y, double a)
	{
		if (MODEL[robotind] != null)		MODEL[robotind].position (data, x, y, a);
		START[robotind]	= new double[] { x, y, a };
	}

	/** Change the START position for the next added robot */
	public void changeStart (double x, double y)
	{
		if (map != null) map.setStart (x, y, map.start_a());
	}
	
	public void changeStart (double x, double y, double a)
	{
		if (map != null) map.setStart (x, y, a);
	}
	
	
	synchronized public void simulate (int robotind, RobotData data, double vlin, double vlat, double vrot, 
			int cycson, int cycir, int cyclrf, int cyclsb, double dt)
	{
		int			i;
//		boolean		collision;
		
		roboindex = robotind;        
		CMD[robotind][0] = vlin;	CMD[robotind][1] = vlat;	CMD[robotind][2] = vrot;
		
		// Compute model based displacement (from where it was, which collisions go back to)
		PREV[robotind]	= new double[] { MODEL[robotind].real_x, MODEL[robotind].real_y, MODEL[robotind].real_a };
		MODEL[robotind].backup (data);
		MODEL[robotind].simulation (data, vlin, vlat, vrot, dt);		
		
		// Send internal data up to LPS
		if (MODEL[robotind] instanceof TricycleDrive)
		{
			data.vm		= ((TricycleDrive) MODEL[robotind]).vm;
			data.del		= ((TricycleDrive) MODEL[robotind]).del;
		}
		
		// Check for collisions: what it overlaps puts it out of the way
		for (i = 0; i < RDESC[robotind].MAXBUMPER; i++)
			data.bumpers[i] = false;
//		collision = collide (robotind, data);
		collide (robotind, data);
		
		// Update real coordinates
		MODEL[robotind].update (data);
		
		// Simulate sensors
		simulate (robotind, data, cycson, cycir, cyclrf, cyclsb);
		
		// Stores robot data
		lastRobotData[robotind] = data;
		
		// Simulates picked objects by the robot
		if ((objectPicked[robotind] != -1) && (objects != null))
			((SimCargo) objects.OBJS[objectPicked[robotind]]).move (data.real_x,data.real_y, data.fork, data.real_a);
	
	}
	
	synchronized public void simulate (int robotind, RobotData data, double x, double y, double a, 
			int cycson, int cycir, int cyclrf, int cyclsb)
	{
		int			i;
		
		roboindex = robotind;        
		
		// Set log based displacement        
		PREV[robotind]	= new double[] { MODEL[robotind].real_x, MODEL[robotind].real_y, MODEL[robotind].real_a };
		MODEL[robotind].backup (data);
		MODEL[robotind].position (data, x, y, a);
		
		// Send internal data up to LPS
		if (MODEL[robotind] instanceof TricycleDrive)
		{
			data.vm		= ((TricycleDrive) MODEL[robotind]).vm;
			data.del		= ((TricycleDrive) MODEL[robotind]).del;
		}
		
		// Check for collisions
		for (i = 0; i < RDESC[robotind].MAXBUMPER; i++)
			data.bumpers[i] = false;
		collide (robotind, data);
		
		// Update real coordinates
		MODEL[robotind].update (data);
		
		// Simulate sensors
		simulate (robotind, data, cycson, cycir, cyclrf, cyclsb);
		
		// Stores robot data
		lastRobotData[robotind] = data;		
		
		// Simulates picked objects by the robot
		if ((objectPicked[robotind] != -1) && (objects != null))
			((SimCargo) objects.OBJS[objectPicked[robotind]]).move (data.real_x, data.real_y, data.fork, data.real_a);
	}
	
	/**
	 * Whether a sensor reads on this step of the firing cycle of its family: on the
	 * step it says it reads on, or on every one when it says none (0, as a
	 * description that does not care about the firing order leaves it).
	 */
	static protected boolean fires (int step, int cycle)
	{
		return (step <= 0) || (step == cycle);
	}

	synchronized public void simulate (int robotind, RobotData data, int cycson, int cycir, int cyclrf, int cyclsb)
	{
		int			i,j;
		
		roboindex = robotind;        
		
		// Compute simulated SONAR data
		for (i = 0; i < RDESC[robotind].MAXSONAR; i++)
			if (fires (RDESC[robotind].sonfeat[i].step (), cycson) && (DATA_CTRL[robotind].sonar))
			{
				data.sonars[i]		= sonar (RDESC[robotind].sonfeat[i]);  
				data.sonars_flg[i]	= true;
			}  
			else   
				data.sonars_flg[i]	= false;
		
		// Compute simulated INFRARED data
		for (i = 0; i < RDESC[robotind].MAXIR; i++)
			if (fires (RDESC[robotind].irfeat[i].step (), cycir) && (DATA_CTRL[robotind].ir))
			{
				data.irs[i]			= ir (RDESC[robotind].irfeat[i]);
				data.irs_flg[i]		= true;
			}     
			else   
				data.irs_flg[i]	= false;
		
		// Compute simulated LASER RANGE data
		for (i = 0; i < RDESC[robotind].MAXLRF; i++)
			if (fires (RDESC[robotind].lrffeat[i].step (), cyclrf) && (DATA_CTRL[robotind].lrf))
			{
				data.lrfs[i]		= lrf (RDESC[robotind].lrffeat[i]);       
				data.lrfs_flg[i]	= true;
			}     
			else   
				data.lrfs_flg[i]	= false;
		
		// Compute simulated LASER BEACON data
		for (i = 0; i < RDESC[robotind].MAXLSB; i++)
			if (fires (RDESC[robotind].lsbfeat[i].step (), cyclsb) && (DATA_CTRL[robotind].lsb))
				beacons (RDESC[robotind].lsbfeat[i], data.beacon[i]);
			else   
				data.beacon[i].setValid (false);
		
		// Compute simulated GPS data
		for (i = 0; i < RDESC[robotind].MAXGPS; i++)
		{
			data.gps[i]		= new GPSData ();  
			data.gps[i].setPos (new UTMPos (data.real_x, data.real_y, "30-S"));  
			data.gps[i].setFix (GPSData.FIX_WAAS_3D);
			data.gps[i].setNumSat (5);
		}     
		
		// Compute simulated COMPASS data
		for (i = 0; i < RDESC[robotind].MAXCOMPASS; i++)
		{
			data.compass[i]		= new CompassData ();  
			data.compass[i].setHeading (data.real_a);  
			data.compass[i].setPitch (0.0);  
			data.compass[i].setRoll (0.0);  
		}     
		
		// Compute simulated INS data
		for (i = 0; i < RDESC[robotind].MAXINS; i++)
		{
			data.ins[i]		= new InsData ();  
			data.ins[i].setPitch (0.0);  
			data.ins[i].setRoll (0.0);  
			data.ins[i].setRollRate (0.0);
			data.ins[i].setPitchRate (0.0);
			data.ins[i].setYawRate (0.0);
			data.ins[i].setAccX (0.0);
			data.ins[i].setAccY (0.0);
			data.ins[i].setAccZ (0.0);
		}     
		
		// Compute simulated RADAR data
		double[]		rds;
		int			k;
		
		for (i = 0; i < RDESC[robotind].MAXTRACKER; i++)
		{
			rds		= radar (RDESC[robotind].trkfeat[i]);
			
			for (k = 0; k < RDESC[robotind].OBJTRK; k++)
				data.trackers[i].valid[k] = false;
			
			for (j = 0, k = 0; j < SDESC[robotind].RAYRAD; j++)
				if ((rds[j] < RDESC[robotind].RANGETRK) && (k < RDESC[robotind].OBJTRK - 1))
				{
					data.trackers[i].trks[k][TrackerData.RANGE] = rds[j];
					data.trackers[i].trks[k][TrackerData.ALPHA] = (double) j * RDESC[robotind].CONETRK / (double) SDESC[robotind].RAYRAD;
					data.trackers[i].trks[k][TrackerData.SPEED] = 0.0;
					data.trackers[i].valid[k] = true;
					k ++;
				}
		}     
		
		moveIcon (ROBOINDEX[robotind], OUTLINE[robotind], MODEL[robotind].real_x, MODEL[robotind].real_y, MODEL[robotind].real_a);	
	}
}
