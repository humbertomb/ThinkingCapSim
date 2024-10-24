/*
 * (c) 1997-2001 Humberto Martinez
 * (c) 2002 Juan Pedro Canovas, Humberto Martinez
 * (c) 2003 Bernardo Canovas, Humberto Martinez
 * (c) 2004 Humberto Martinez
 */

package tcapps.tcsim.simul;

import java.util.Enumeration;
import java.util.Random;

import tc.shared.world.World;
import tc.vrobot.RobotData;
import tc.vrobot.RobotDataCtrl;
import tc.vrobot.RobotDesc;
import tc.vrobot.RobotModel;
import tc.vrobot.SensorPos;
import tc.vrobot.TrackerData;
import tc.vrobot.models.TricycleDrive;
import tcapps.tcsim.gui.SimulatorWindow;
import tcapps.tcsim.simul.objects.SimCargo;
import tcapps.tcsim.simul.objects.SimMultiCargo;
import tcapps.tcsim.simul.objects.SimObject;
import tcapps.tcsim.simul.objects.SimScene;
import devices.pos.Position;
import devices.pos.UTMPos;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;
import wucore.utils.math.Angles;
import wucore.utils.math.stat.RandomNumberGenerator;
import devices.data.CompassData;
import devices.data.GPSData;
import devices.data.InsData;
import devices.data.VisionData;

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
	public static final int 		GFX3D_UPD 	= 500;	// Time for 3D graphics update (in milisecs)
	private static int				MOVE_OBJECT_TIME	= 200;  //millis

	// Simulation parameters
	private double[]				tof;					// Time-of-flight buffers for sonar
	private Random					rnd;					// Pseudo-random number generator
	protected long					tgfx;				// Time of graphics update
	protected RandomNumberGenerator rndg; 				// Generator of Random Number for LRF
	
	// Simulated world components
	public SimScene					scene;
	protected World					map;
	protected String				mapfile;
	protected int					roboindex	= -1;
	protected Position				bpos;
	
	private int						iconcount;
	private boolean[]				objects3D;
	public Line2[][]				icons;
	public SimMultiCargo			smc;
	
	// Robots internal data
	public SimulatorDesc[]			SDESC;
	public RobotDesc[]				RDESC;
	public RobotModel[]				MODEL;
	public RobotDataCtrl[]			DATA_CTRL;
	public int[]					ROBOINDEX;
	public VisionData[]			VISDATA;
	public Position[][]			VISOBJS;
//	public Hashtable			VISDATA;
//	public Hashtable[]			VISOBJS;
	public Position[]				VISPOS;
	public int					numrobots;
	public RobotData[] 			lastRobotData; // Stores the last 'RobotData' object received from "SimulatedRobot" to allow 3D representation in the "RefreshThread"
	
	// Simulated world visualization
	protected SimulatorWindow 		win;
	
	public int[] 					objectPicked; // Indexed by robot id, this array contains the id of the object that the robot has picked. -1 if no object has been picked 
	public String[]					palletPicked; // Indexed by robot id, this array contains the id of the pallet that the robot has picked. -1 if no object has been picked
	
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

				if (scene != null)
					for (i=0;i< scene.numobjects;i++)
						win.updateObjectData(i,scene.OBJS[i].odesc.pos,scene.OBJS[i].odesc.a);
					
				SimObject so;
				if(smc != null){
					for (Enumeration enu=smc.pallets.elements();enu.hasMoreElements();){
						so=(SimObject)enu.nextElement();
						win.updateObjectData(so.idsimul,so.odesc.pos,so.odesc.a);
					}
				}
				
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
		VISOBJS		= new Position[MAX_ROBOTS][];
//		VISOBJS		= new Hashtable[MAX_ROBOTS];
		VISPOS		= new Position[MAX_ROBOTS];
		numrobots	= 0;
		iconcount 	= 0;	
		objects3D	= new boolean[MAX_ROBOTS+SimScene.MAX_OBJECTS];
		icons 		= new Line2[MAX_ROBOTS+SimScene.MAX_OBJECTS][];
		bpos		= new Position ();		
		lastRobotData = new RobotData[MAX_ROBOTS];
		objectPicked = new int[MAX_ROBOTS];		
		palletPicked = new String[MAX_ROBOTS];
		for (i = 0; i < MAX_ROBOTS; i++)
		{
			VISPOS[i]	= new Position ();
			objectPicked[i] = -1;
			palletPicked[i] = null;
		}

		map = null;
	}
	
	/* Class methods */
	static protected double sqr (double x)
	{
		return x * x;
	}

	// Accessors
	public World				getWorld ()												{ return map; }
	public String			getWorldName ()											{ return mapfile; }
	public VisionData[]		getVisionData ()										{ return VISDATA; }
//	public VisionData[]		getVisionData (){
//		VisionData vd[];
//		int i=0;
//		
//		vd=new VisionData[VISDATA.size()];
//		for(Enumeration enum=VISDATA.elements();enum.hasMoreElements();)
//			vd[i++]=(VisionData)enum.nextElement();
//		return vd;
//	}
	public void				set_data_ctrl (int robotind, RobotDataCtrl datactrl)	{ DATA_CTRL[robotind] = datactrl; }
	public void				closeVisualization3D ()									{ this.win = null; }	
	
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
		
		icons[index]	= new Line2[icon.length];
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
			
			icons[index][i]	= new Line2 (x1,y1,x2,y2);
		}
	}

	public Line2 closerIcon (SimObject obj, int index)
	{
		return map.closer (obj.odesc.pos.x(),obj.odesc.pos.y(), icons, iconcount, index);
	}
	
	public int collisionIcon (Line2 line)
	{
		int			i, j, k;
		
		for (i = 0; i < numrobots; i++)
		{
			k	= ROBOINDEX[i];
			for (j = 0; j < icons[k].length; j++)
				if (icons[k][j] == line)
					return k;
		}				
				
		return -1;
	}
	
	public void setScene (String fname)
	{		
		int			i;

		System.out.println("Simulator: crear SimScene");
		scene	= new SimScene (fname, this);

		if (win != null)
		{
			win.removeAllObjects ();
			for (i = 0; i < scene.numobjects; i++)
				win.addObject (scene.OBJS[i]);
//			SimObject so;
//			for (Enumeration enum=scene.OBJS.elements();enum.hasMoreElements();){
//				so=(SimObject)enum.nextElement();
//				id=win.addObject (so);
//				so.idsimul=id;
//			}
		}
	}
	
	public void setVisualization (SimulatorWindow win) 
	{ 
		int			i;
		
		this.win = win;
		
		if (map!=null)
			win.setWorldmap(map);
		for (i=0;i<numrobots;i++)
			win.addRobot(RDESC[i],SDESC[i]);
		if (scene != null){
			 for (i=0;i<scene.numobjects;i++)
			 	win.addObject(scene.OBJS[i]);
//			SimObject so;
//			for (Enumeration enum=scene.OBJS.elements();enum.hasMoreElements();){
//				so=(SimObject)enum.nextElement();
//				win.addObject (so);
//			}
		}
		win.repaint();	
		new Thread (refreshThread).start ();
	}
	public void setSimMultiCargo(SimMultiCargo smc){
		this.smc=smc;
	}
	
	synchronized public int add_robot (RobotDesc rdesc, SimulatorDesc sdesc, RobotModel model, RobotDataCtrl datactrl)
	{
		RDESC[numrobots] = rdesc;
		SDESC[numrobots] = sdesc;
		MODEL[numrobots] = model;
		DATA_CTRL[numrobots] = datactrl;
		ROBOINDEX[numrobots] = allocIcon ();		
		moveIcon (ROBOINDEX[numrobots], rdesc.icon,model.real_x, model.real_y, model.real_a);;
		
		if (win!=null)
			win.addRobot (rdesc, sdesc);

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
						
		if (this.win != null)
			win.setWorldmap(map);		
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
		double			a, a2, step;
		double			dist, tdist;
		double			rlen;
		Line2			rout, wall;
		Point2			p;
		
		
		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.phi ());
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.phi ());
		
		a2		= RDESC[roboindex].CONESON / 2.0;
		rlen	= RDESC[roboindex].RANGESON * 2.0;
		step		= (a2 * 2.0) / (double) (SDESC[roboindex].RAYSON - 1);
		dist		= Double.MAX_VALUE;
		rout		= new Line2 ();
		for (a = -a2; a <= a2; a += step)
		{
			xx2		= xx1 + rlen * Math.cos (MODEL[roboindex].real_a + a1.alpha () + a);
			yy2		= yy1 + rlen * Math.sin (MODEL[roboindex].real_a + a1.alpha () + a);	
			tdist	= RDESC[roboindex].RANGESON;
			
			rout.set (xx1, yy1, xx2, yy2);
			wall 		= map.crossline (rout, icons, iconcount, ROBOINDEX[roboindex]);						
			if (wall == null)									
				break;
			else
			{				
				p		= rout.intersection (wall);
				tdist 	= p.distance (xx1, yy1);
			}
			dist 		= Math.min (dist, tdist);
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
		
		aa1		= a1.alpha ();
		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.phi ());
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.phi ());
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
		double			a, a2, step;
		double			dist, tdist;
		double			rlen;
		Line2			rout, wall;
		Point2			p;
		
		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.phi ());
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.phi ());
		
		a2		= RDESC[roboindex].CONEIR / 2.0;
		rlen	= RDESC[roboindex].RANGEIR * 2.0;
		step	= (a2 * 2.0) / (double) (SDESC[roboindex].RAYIR- 1);
		dist	= Double.MAX_VALUE;
		rout	= new Line2 ();
		for (a = -a2; a <= a2; a += step)
		{
			xx2		= xx1 + rlen * Math.cos (MODEL[roboindex].real_a + a1.alpha () + a);
			yy2		= yy1 + rlen * Math.sin (MODEL[roboindex].real_a + a1.alpha () + a);	
			tdist	= RDESC[roboindex].RANGEIR;
			
			rout.set (xx1, yy1, xx2, yy2);
			wall 	= map.crossline (rout, icons, iconcount, ROBOINDEX[roboindex]);						
			if (wall == null)									
				break;
			else
			{				
				p		= rout.intersection (wall);
				tdist 	= p.distance (xx1, yy1);
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
		
		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.phi ());
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.phi ());
		
		lrf_measures = new double[RDESC[roboindex].RAYLRF];
		a2		= RDESC[roboindex].CONELRF * 0.5;
		rlen	= RDESC[roboindex].RANGELRF * 2.0;
		step	= RDESC[roboindex].CONELRF / (double) (RDESC[roboindex].RAYLRF - 1);
		rout	= new Line2 ();
		
		for (i = 0, a = -a2; i < RDESC[roboindex].RAYLRF; i++, a += step)
		{
			xx2		= xx1 + rlen * Math.cos (MODEL[roboindex].real_a + a1.alpha () + a);
			yy2		= yy1 + rlen * Math.sin (MODEL[roboindex].real_a + a1.alpha () + a);	
			tdist	= RDESC[roboindex].RANGELRF;
			
			rout.set (xx1, yy1, xx2, yy2);
			wall 	= map.crossline (rout, icons, iconcount, ROBOINDEX[roboindex]);						
			if (wall != null)									
			{				
				p		= rout.intersection (wall);
				tdist 	= p.distance (xx1, yy1);
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
	
//	Sensor laser de balizas (tres tipos: exacto, gaussiano y geometrico)
	protected double[] lsb (SensorPos a1)
	{
		double			lsb[];
		
		if (map == null) 		return null;
		
		switch (SDESC[roboindex].MODELSB)
		{
			case LSB_EXACT:
				lsb = lsb_exact (a1);
				break;
			case LSB_GAUSS:
				lsb = lsb_gauss (a1);
				break;
			case LSB_GEOM:
			default:
				lsb = lsb_geom (a1);
		}
		
		return lsb;
	}
	
//	Sensor Laser de balizas sin errores que da el angulo de orientacion (en RAD) de la baliza detectada	
	private double[] lsb_exact (SensorPos a1)
	{
		int 			i,longitud;
		double			xx1, yy1; 						// 	Posicion inicial del barrido
		double			xx2, yy2; 						// 	Posicion final del barrido
		double 			distMuro, distBeac, dist, dist1;//	Distancia al muro y distancia a baliza y distancia auxiliar, y distancia de la primera baliza
		double			a2, a, step, angle; 			//  angulo de semiapertura , angulo auxiliar, angulo entre barridos, angulo auxiliar
		double			bearing, bearingFinal; 			// angulo medido por el sensor, y angulo final medido por el sensor
		double			rangeFinal,range;
		double[]		lsb_measures;					//  Medidas calculadas
		Line2			rout, wall;						//	Linea del barrido, linea que intersecta el barrido (del muro o baliza)
		Point2			p;								// 	Punto de interseccion entre barrido y el muro o la baliza.
		int				 index, first_index, last_index;//	Indices de la baliza intersectada (actual, primera y la ultima baliza detectada)
		
		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.phi ());  // Posicion absoluta del sensor laser
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.phi ());
		
		if(RDESC[roboindex].RANGE == true ||RDESC[roboindex].ANGLE == true)
			longitud = RDESC[roboindex].BEACLSB*2;						// longitud del vector de las medidas
		else
			longitud = RDESC[roboindex].BEACLSB;						// longitud del vector de las medidas
		
		lsb_measures = new double[longitud];  					// BEACLSB es el numero maximo de balizas detectables
		
		
		for (i=0;i<longitud;i++) lsb_measures[i]=Double.MAX_VALUE;		// Inicia el array de medida con el maximo valor (no hay medida)
		a2		= (RDESC[roboindex].CONELSB / 2.0);								// CONELSB es el angulo de barrido (seguramente 360�)
		step	= (a2 * 2.0) / (double) (RDESC[roboindex].RAYLSB-1);  					// angulo entre barridos 
		rout	= new Line2 ();
		
		bearingFinal=Double.MAX_VALUE;
		rangeFinal=Double.MAX_VALUE;
		i		= 0;						//	numero de medidas
		
		
		dist		= Double.MAX_VALUE;									// Minima distancia entre sensor y el baliza 	
		dist1		= Double.MAX_VALUE;									// Primera distancia entre sensor y la baliza 	
		index=-1;
		last_index=-1;
		first_index=-1;
		
		for (a = -a2; a < a2; a += step)	// barrido entre -a2 y a2                 
		{
			
			xx2		= xx1 + RDESC[roboindex].RANGELSB * Math.cos (MODEL[roboindex].real_a + a1.alpha () + a);	// punto final del barrido
			yy2		= yy1 + RDESC[roboindex].RANGELSB * Math.sin (MODEL[roboindex].real_a + a1.alpha () + a);
			
			distMuro	= Double.MAX_VALUE;									
			distBeac	= Double.MAX_VALUE;										
			rout.set (xx1, yy1, xx2, yy2);					
			wall 	= map.crossline (rout, icons, iconcount, ROBOINDEX[roboindex]);									
			if(wall != null){													
				p			= rout.intersection (wall);	
				distMuro	= p.distance (xx1, yy1);						// Calculo de la distancia entre sensor y el muro
			}
			
			index = map.beacons().crossline(rout);														
			wall=map.beacons().at(index).getLine();
			if(wall != null){
				p			= rout.intersection (wall);
				distBeac	= p.distance (xx1, yy1);						// Calcula la interseccion entre el sensor y baliza			
				
				if((distBeac<distMuro)&&(distBeac>RDESC[roboindex].MINIMLSB)){			// Si la distancia a la baliza es menor o que la del Muro, y la distancia entre la baliza es mayor a la minima
					
					if(index!=last_index)	dist=Double.MAX_VALUE;			
					
					if (distBeac<dist){										// dist = distancia minima entre entre sensor y baliza para distintos rayos (la mas perpendicular)
						
						range=distBeac; // Para el rango
						bearing=Math.atan2(yy2-yy1,xx2-xx1);				// Calcula el angulo absoluto entre la baliza y sensor(PI a -PI)
						angle=Angles.radnorm_180(map.beacons().at(index).getAng()-bearing);		// Calcula el angulo entre balizas y barrido
						
						//System.out.println(" range ="+ range);
						
						// || (angle>(rdesc.REFLSB)) && (angle<(Math.PI-rdesc.REFLSB))	// a�adir al if para detectar en las dos caras
						if ((angle>RDESC[roboindex].REFLSB) && (angle<(Math.PI-RDESC[roboindex].REFLSB)) )		// Verifica si el rayo reflecta en la baliza
						{	
							if ((last_index>=0) && (last_index!=index))	{
								
								if(RDESC[roboindex].ANGLE == true) lsb_measures[i++]=bearingFinal;	// Guarda la medida (la mas perpendicular) cuando cambia de baliza					
								if(RDESC[roboindex].RANGE == true) lsb_measures[i++]=rangeFinal;	// Guarda la medida (la mas perpendicular) cuando cambia de baliza					
							}
							bearingFinal=Angles.radnorm_180(bearing-a1.alpha()-MODEL[roboindex].real_a);					// Se almacena la medida relativa que se medir� con el sensor
							rangeFinal = range;
							if (last_index<0)	{dist1=distBeac; first_index=index;}				// Guarda la primera distancia (para el caso especial de que el primer rayo y el ultimo del barrido correspondan a la misma baliza)
							last_index=index;			// Se almacena el indice de la ultima baliza
							dist=distBeac;				// Se guarda la distancia mas perpendicular a la baliza
						}
						
					}
					
					
				} 
			}
		}
		
		// Guarda la medida de la ultima baliza (y en el caso de que sea la misma baliza que la primera, guarda la del rayo mas perpendicular
		if (last_index>=0){				
			if(last_index!=first_index){								// Si la primera baliza no corresponde con la ultima guarda la medida
				if(RDESC[roboindex].ANGLE == true) lsb_measures[i++]=bearingFinal;	// Guarda la medida (la mas perpendicular) cuando cambia de baliza					
				if(RDESC[roboindex].RANGE == true) lsb_measures[i++]=rangeFinal;	// Guarda la medida (la mas perpendicular) cuando cambia de baliza					
			}
			else
				if (dist1>dist){
					i=0;	
					if(RDESC[roboindex].ANGLE == true) lsb_measures[i++]=bearingFinal;	// Guarda la medida (la mas perpendicular) cuando cambia de baliza					
					if(RDESC[roboindex].RANGE == true) lsb_measures[i++]=rangeFinal;	// Guarda la medida (la mas perpendicular) cuando cambia de baliza					
				}
		}
		
//		for(i=0;i<3;i++)
//		System.out.println("MEDIDA0="+lsb_measures[2*i]*Angles.+"MEDIDA1="+lsb_measures[2*i+1]);
		
		
//		System.out.println("**POSICION REAL ["+model.real_x+" , "+model.real_y+"]  angulo = "+model.real_a*Angles.);
		
		return lsb_measures;
	}
	
	
//	Sensor Laser de balizas sin errores que da el angulo de orientacion (en RAD) de la baliza detectada	(modo continuo)
	private double[] lsb_exact_CONT (SensorPos a1)
	{
		int 			i,a;
		double			xx1, yy1; 		// Posicion sensor (absolutas)
		double			xx2, yy2; 		// Posiciones del barrido (absolutas)
		double			a2, angle, dist; 	
		double			range, bearing; // rango y angulo medido por el sensor
		double[]		lsb_measures;
		Line2			rout, wall;
		Point2			p;
		
		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.phi ());  // Posicion absoluta del sensor laser
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.phi ());
		
		lsb_measures = new double[RDESC[roboindex].BEACLSB];  			// BEACLSB es el numero maximo de balizas
		a2		= (RDESC[roboindex].CONELSB / 2.0);				// CONELSB es el angulo de barrido (seguramente 360�)
		
		rout	= new Line2 ();
		i		= 0;
		
		for (a = 0; a < map.beacons().n(); a++)			// map.bn() es el numero de balizas                  
		{	
			rout.set(map.beacons().at(a).getLine());
			xx2		=(rout.orig().x()+rout.dest().x())/2;		// Posicion X de la baliza
			yy2		=(rout.orig().y()+rout.dest().y())/2;		// Posicion Y de la baliza
			range =Math.sqrt((xx2-xx1)*(xx2-xx1)+(yy2-yy1)*(yy2-yy1));	// rango entre sensor y balizas
			
			dist	= Double.MAX_VALUE;									// Calculo de la distancia entre sensor y el muro	
			rout.set (xx1, yy1, xx2, yy2);					
			wall 	= map.crossline (rout, icons, iconcount, ROBOINDEX[roboindex]);									
			if(wall != null){
				p		= rout.intersection (wall);
				dist 	= p.distance (xx1, yy1);
			}
			
			
			if (range<=RDESC[roboindex].RANGELSB && range>=RDESC[roboindex].MINIMLSB && range<=dist ){				// Si el barrido alcanza la baliza y no se excede el rango maximo ...
				bearing=Math.atan2(yy2-yy1,xx2-xx1);											// Calcula el angulo absoluto entre la baliza y sensor(PI a -PI)
				
				if(bearing<=a2 & bearing>=(-a2)){												// Si no se supera el angulo de barrido del laser ...
					angle=Angles.radnorm_180(map.beacons().at(a).getAng()-bearing);									// Calcula el angulo entre balizas y barrido
					
					if ((angle>RDESC[roboindex].REFLSB) && (angle<(Math.PI-RDESC[roboindex].REFLSB)))
					{lsb_measures[i++] = Angles.radnorm_180(bearing-a1.alpha()-MODEL[roboindex].real_a);		// Calculo del angulo relativo de la baliza (radianes)
					System.out.println("Medidas beacons = "+lsb_measures[i-1]);
					}
				}
			}
		}
		
		while(i<RDESC[roboindex].BEACLSB)			lsb_measures[i++]=Double.MAX_VALUE;
		
		return lsb_measures;
	}
	
	private double[] lsb_geom (SensorPos a1)	
	{
		int 				a=0;
		int					i=0;
		double[]			measures;
		
		measures = lsb_exact (a1);
		
		for (a = 0; a < RDESC[roboindex].BEACLSB; a++) {
			if(Math.abs(measures[a])<1000){
				if(RDESC[roboindex].ANGLE == true) 
					measures[i]	= (1.0 - SDESC[roboindex].ERRORANGLELSB) * measures[i] + (2.0 * SDESC[roboindex].ERRORANGLELSB * Math.random () - SDESC[roboindex].ERRORANGLELSB) * measures[i++];
				if(RDESC[roboindex].RANGE == true) 		
					measures[i]	= (1.0 - SDESC[roboindex].ERRORRANGELSB) * measures[i] + (2.0 * SDESC[roboindex].ERRORRANGELSB * Math.random () - SDESC[roboindex].ERRORRANGELSB) * measures[i++];
			}
		}
		
		return measures;
	}		
	
	private double[] lsb_gauss (SensorPos a1) // a�ade ruido gausiano de desviacion tipica ERRORGAUSS
	{
		int 				a=0;
		int					i=0;
		double[]			measures;		
		measures = lsb_exact (a1);
		for (a = 0; a < RDESC[roboindex].BEACLSB; a++) {
			if(Math.abs(measures[a])<1000){
				if(RDESC[roboindex].ANGLE == true) 
					measures[i]	= measures[i++] + rnd.nextGaussian()*SDESC[roboindex].ERRORANGLELSBGAUSS; // A�ade ruido gaussiano			
				if(RDESC[roboindex].RANGE == true) 		
					measures[i]	= measures[i++] + rnd.nextGaussian()*SDESC[roboindex].ERRORRANGELSBGAUSS; // A�ade ruido gaussiano
			}
		}
		return measures;
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

		xx1		= MODEL[roboindex].real_x + a1.rho () * Math.cos (MODEL[roboindex].real_a + a1.phi ());
		yy1		= MODEL[roboindex].real_y + a1.rho () * Math.sin (MODEL[roboindex].real_a + a1.phi ());
		
		rdr_measures = new double[SDESC[roboindex].RAYRAD];
		a2		= RDESC[roboindex].CONETRK * 0.5;
		step	= RDESC[roboindex].CONETRK / (double) (SDESC[roboindex].RAYRAD - 1);
		rout	= new Line2 ();
		
		for (i = 0, a = -a2; i < SDESC[roboindex].RAYRAD; i++, a += step)
		{
			xx2		= xx1 + RDESC[roboindex].RANGETRK * Math.cos (MODEL[roboindex].real_a + a1.alpha () + a);
			yy2		= yy1 + RDESC[roboindex].RANGETRK * Math.sin (MODEL[roboindex].real_a + a1.alpha () + a);	
			tdist	= RDESC[roboindex].RANGETRK;
			
			rout.set (xx1, yy1, xx2, yy2);
			wall 	= map.crossline (rout, icons, iconcount, ROBOINDEX[roboindex]);						
			if (wall != null)									
			{				
				p		= rout.intersection (wall);
				tdist 	= p.distance (xx1, yy1);
			}
			rdr_measures[i] = tdist;					
		}
		
		return rdr_measures;
	}
	
	protected boolean bumper (Line2 a1)
	{
		Line2			wall;
		double			xx1, yy1;
		double			xx2, yy2;
		double          len1, len2;
		double          rho1, rho2;
		double			dist;
		
		if ((map == null) || (map.walls () == null))	return false;
		
		wall	= map.walls().closer (MODEL[roboindex].real_x, MODEL[roboindex].real_y, icons, iconcount, ROBOINDEX[roboindex]);
		
		if (wall == null)	return false;
		
		dist	= wall.distance (MODEL[roboindex].real_x, MODEL[roboindex].real_y);
		if (dist > RDESC[roboindex].RADIUS) return false;
		
		len1    = Math.sqrt (a1.orig ().x () * a1.orig ().x () + a1.orig ().y () * a1.orig ().y ());
		len2    = Math.sqrt (a1.dest ().x () * a1.dest ().x () + a1.dest ().y () * a1.dest ().y ());
		rho1    = Math.atan2 (a1.orig ().y (), a1.orig ().x ());
		rho2    = Math.atan2 (a1.dest ().y (), a1.dest ().x ());
		
		xx1		= MODEL[roboindex].real_x + len1 * Math.cos (MODEL[roboindex].real_a + rho1);
		yy1		= MODEL[roboindex].real_y + len1 * Math.sin (MODEL[roboindex].real_a + rho1);
		xx2		= MODEL[roboindex].real_x + len2 * Math.cos (MODEL[roboindex].real_a + rho2);
		yy2		= MODEL[roboindex].real_y + len2 * Math.sin (MODEL[roboindex].real_a + rho2);
		
		if (wall.intersection (xx1, yy1, xx2, yy2) == null) return false;
		
		//*** saca al quaky. quiz� no se deba hacer aqk� sino en el simulate ***/
		
		///// COMENTAR LO SIGUIENTE PARA SEGUIR CON EL ANTERIOR MODELO DE CHOQUES /////
		double xvect, yvect;
		Point2 intersec;
		Line2 l1 = new Line2(MODEL[roboindex].real_x,MODEL[roboindex].real_y,xx1,yy1);
		Line2 l2 = new Line2(MODEL[roboindex].real_x,MODEL[roboindex].real_y,xx2,yy2);
		
		intersec=l1.intersection (wall);
		if (intersec!=null)
		{
			xvect = xx1-intersec.x();
			yvect = yy1-intersec.y();
		}
		else
		{
			intersec=l2.intersection(wall);
			if (intersec!=null)
			{
				xvect = xx2-intersec.x();
				yvect = yy2-intersec.y();
			}
			else //�choca pero no hay intersecci�n?
			{
				xvect = 0.0;
				yvect = 0.0;
			}
		}
		MODEL[roboindex].real_x -= xvect;
		MODEL[roboindex].real_y -= yvect;
		MODEL[roboindex].odom_x -= xvect;
		MODEL[roboindex].odom_y -= yvect;
		RobotData data = new RobotData(RDESC[roboindex]);
		data.location(MODEL[roboindex].odom_x,MODEL[roboindex].odom_y,MODEL[roboindex].odom_a);
		MODEL[roboindex].backup(data);
		/////  /////
		
		return true;
	}		
	
	public void reset (int robotind, RobotData data, World map)
	{
		if (MODEL[robotind] != null) 
		{
			if (map != null)
				MODEL[robotind].position (data, map.start_x (), map.start_y (), map.start_a ());
			else
				MODEL[robotind].position (data, 0.0, 0.0, 0.0);		
		}
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
	
	
	/*OPERACIONES SOBRE PALLETS*/
	
//	public int addPallet(String idpallet,Position pos,int typepallet){
//		SimObject so=null;
//		int id=-1;
//		
//		
//		if(smc!=null){
//			so=smc.addPallet(idpallet,pos,typepallet);
//			System.out.println("Simulator: addPallet() so="+so.toString());
//			if(win!=null){
//				id=win.addObject(so,new Point3(pos.x(),pos.y(),0.0),pos.alpha());
//				so.idsimul=id;
//			}
//		}
//		System.out.println("Simulator: addPallet() so="+so.toString());
//		
//		
//		return id;
//	}
	public int addPallet(SimObject so,Position pos){
		int id=-1;
		
		if(win != null){
			id=win.addObject(so,new Point3(pos.x(),pos.y(),0.0),pos.alpha());
			
		}
		return id;
	}
	public void delPallet(Position pos){
		
	}
	public void delPallet(int id){
		win.removeObject(id);
		objects3D[id]=false;
	}
	public void movePallet(String idpallet,Position pos){
		
	}
	public void changeStatusPallet(String idpallet,int typepallet){
		
	}
	
//	simul.delPallet(item.idpallet);
//	simul.movePallet(item.idpallet,item.position,item.status);
	
	synchronized public void simulate (int robotind, RobotData data, double speed, double turn, 
			int cycson, int cycir, int cyclrf, int cyclsb, int cycvis, double dt)
	{
		int			i;
		boolean		collision;
		
		roboindex = robotind;        
		
		// Compute model based displacement        
		MODEL[robotind].backup (data);
		MODEL[robotind].simulation (data, speed, turn, dt);		
		
		// Send internal data up to LPS
		if (MODEL[robotind] instanceof TricycleDrive)
		{
			data.vm		= ((TricycleDrive) MODEL[robotind]).vm;
			data.del		= ((TricycleDrive) MODEL[robotind]).del;
		}
		
		// Check for collisions
		collision = false;   
		for (i = 0; i < RDESC[robotind].MAXBUMPER; i++)
		{
			data.bumpers[i] = bumper (RDESC[robotind].bumfeat[i]); 
			collision = collision || data.bumpers[i];
		} 
		
		// If robot collided restore previous position
		if (collision)
			MODEL[robotind].restore (data); 
		
		// Update real coordinates
		MODEL[robotind].update (data);
		
		// Simulate sensors
		simulate (robotind, data, cycson, cycir, cyclrf, cyclsb, cycvis);
		
		// Stores robot data
		lastRobotData[robotind] = data;
		
		// Simulates picked objects by the robot
		if (objectPicked[robotind]!=-1)
//			((SimCargo)scene.OBJS.get(new Integer(objectPicked[robotind]))).move (data.real_x,data.real_y, data.fork, data.real_a);
			((SimCargo) scene.OBJS[objectPicked[robotind]]).move (data.real_x,data.real_y, data.fork, data.real_a);
//		 Simulates picked pallets by the robot
		if (palletPicked[robotind]!=null){
			((SimCargo) smc.pallets.get(palletPicked[robotind])).move (data.real_x, data.real_y, data.fork, data.real_a);
		}
	
	}
	
	synchronized public void simulate (int robotind, RobotData data, double x, double y, double a, 
			int cycson, int cycir, int cyclrf, int cyclsb, int cycvis)
	{
		int			i;
		
		roboindex = robotind;        
		
		// Set log based displacement        
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
			data.bumpers[i] = bumper (RDESC[robotind].bumfeat[i]); 
		
		// Update real coordinates
		MODEL[robotind].update (data);
		
		// Simulate sensors
		simulate (robotind, data, cycson, cycir, cyclrf, cyclsb, cycvis);
		
		// Stores robot data
		lastRobotData[robotind] = data;		
		
		// Simulates picked objects by the robot
		if (objectPicked[robotind]!=-1)
		{
			((SimCargo) scene.OBJS[objectPicked[robotind]]).move (data.real_x, data.real_y, data.fork, data.real_a);
		}
//		 Simulates picked pallets by the robot
		if (palletPicked[robotind]!=null){
			((SimCargo) smc.pallets.get(palletPicked[robotind])).move (data.real_x, data.real_y, data.fork, data.real_a);
		}
	}
	
	synchronized public void simulate (int robotind, RobotData data, int cycson, int cycir, int cyclrf, 
			int cyclsb, int cycvis)
	{
		int			i,j;
		double		lx, ly;
		double		ll, aa, na;
		double		xx1, yy1, aa1;
		
		roboindex = robotind;        
		
		// Compute simulated SONAR data
		for (i = 0; i < RDESC[robotind].MAXSONAR; i++)
			if ((RDESC[robotind].sonfeat[i].step () == cycson) && (DATA_CTRL[robotind].sonar))
			{
				data.sonars[i]		= sonar (RDESC[robotind].sonfeat[i]);  
				data.sonars_flg[i]	= true;
			}  
			else   
				data.sonars_flg[i]	= false;
		
		// Compute simulated INFRARED data
		for (i = 0; i < RDESC[robotind].MAXIR; i++)
			if ((RDESC[robotind].irfeat[i].step () == cycir) && (DATA_CTRL[robotind].ir))
			{
				data.irs[i]			= ir (RDESC[robotind].irfeat[i]);
				data.irs_flg[i]		= true;
			}     
			else   
				data.irs_flg[i]	= false;
		
		// Compute simulated LASER RANGE data
		for (i = 0; i < RDESC[robotind].MAXLRF; i++)
			if ((RDESC[robotind].lrffeat[i].step () == cyclrf) && (DATA_CTRL[robotind].lrf))
			{
				data.lrfs[i]		= lrf (RDESC[robotind].lrffeat[i]);       
				data.lrfs_flg[i]	= true;
			}     
			else   
				data.lrfs_flg[i]	= false;
		
		// Compute simulated LASER BEACON data
		for (i = 0; i < RDESC[robotind].MAXLSB; i++)
			if ((RDESC[robotind].lsbfeat[i].step () == cyclsb) && (DATA_CTRL[robotind].lsb))
			{
//				data.beacon[i]		= lsb (RDESC[robotind].lsbfeat[i]);  
				
				bpos.set (data.real_x, data.real_y, data.real_a);
				
				data.beacon[i].setPosition (bpos);  
				data.beacon[i].setNumber (5);
				data.beacon[i].setQuality(90);
				data.beacon[i].setValid (true);
			}     
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
		
		// Compute simulated VISION data
		if (VISDATA != null)
		{
			for (j = 0; j < scene.numobjects; j++)
				VISDATA[j].valid = false;
//			VisionData vd;
//			Object obj;
//			for(Enumeration enum=VISDATA.elements();enum.hasMoreElements();){
//				
//				obj=enum.nextElement();
////				System.out.println("Simulator:simulate obj="+obj.getClass());
//				vd=(VisionData)obj;
//				vd.valid = false;
//			}
			
			for (i = 0; i < RDESC[robotind].MAXVISION; i++)
				if ((RDESC[robotind].visfeat[i].step () == cycvis) && (DATA_CTRL[robotind].vision))
				{
					// Store current robot-perceived absolute position of camera (x, y, a)
					bpos.set (data.odom_x, data.odom_y, data.odom_a);
					
					// Compute sensor absolute position (x, y, a)
					xx1		= data.real_x + RDESC[robotind].visfeat[i].rho () * Math.cos (RDESC[robotind].visfeat[i].phi ());
					yy1		= data.real_y + RDESC[robotind].visfeat[i].rho () * Math.sin (RDESC[robotind].visfeat[i].phi ());
					aa1		= data.real_a + RDESC[robotind].visfeat[i].alpha ();
					
					// Iterate through all available objects
					for (j = 0; j < scene.numobjects; j++)
//					for(Enumeration enum=scene.OBJS.keys();enum.hasMoreElements();)
					{
						SimObject 	so;
//						Integer 	key;
//						Position	p;
//						key=(Integer)enum.nextElement();
						so=(SimObject)scene.OBJS[j];
						
						
						// Get the robot relative position of perceived object (previous image)
						
//						if((((p=(Position)VISOBJS[robotind].get(key)))!=null)){
//							ll=p.rho();
//							aa=p.phi();
//						}else{
//							p=new Position();
//							VISOBJS[robotind].put(key,p);
//							ll = 0.0;
//							aa = 0.0;
//						}
						ll	= VISOBJS[robotind][j].rho ();
						aa	= VISOBJS[robotind][j].phi ();
						
						// Decide if the object was perceived (previous image)
						if (Math.abs (aa) < RDESC[robotind].CONEVIS * 0.5)
						{
							// Add some gaussian noise to the angular position
							na	= rndg.nextGaussian (aa, SDESC[robotind].ERRORVIS);
							
							lx	= ll * Math.cos (na);
							ly	= ll * Math.sin (na);
							
							// Generate visual perception data (previous image and previous position)
//							VISDATA[j].set_dev (i);
//							VISDATA[j]=(VisionData)VISDATA.get(key);
							VISDATA[j].set_dev(i);
//							VISDATA[j].set_blob (so.odesc.label, 60, 80, 10, 10, so.odesc.color);
							VISDATA[j].set_blob (so.odesc.label, 60, 80, 10, 10, wucore.utils.color.ColorTool.fromWColorToColor(so.odesc.color));
							VISDATA[j].percept_pos (ly, 0.0, lx);
							VISDATA[j].sensor_pos (RDESC[robotind].visfeat[i].x (), RDESC[robotind].visfeat[i].y (), RDESC[robotind].visfeat[i].alpha ());
							VISDATA[j].capture_pos (VISPOS[robotind], bpos);
							VISDATA[j].valid = true;
						}
						
						// Compute the robot relative position of perceived object (new image)
						lx	= so.odesc.pos.x() - xx1;
						ly	= so.odesc.pos.y() - yy1;
						
						ll	= Math.sqrt (lx * lx + ly * ly);
						aa	= Angles.radnorm_180 (Math.atan2 (ly, lx) - aa1);
						
//						p.set_polar(ll,aa);
//						VISOBJS[robotind].put(key,p);
						VISOBJS[robotind][j].set_polar (ll, aa);
					}
					
					// Store the robot-perceived position for the image capture (new position)
					VISPOS[robotind].set (bpos);		
				}
		}
		
		moveIcon (ROBOINDEX[robotind], RDESC[robotind].icon,MODEL[robotind].real_x, MODEL[robotind].real_y, MODEL[robotind].real_a);	
	}
}
