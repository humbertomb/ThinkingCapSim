/*
 * Created on 12-dic-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcapps.tcsim.simul.objects;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import tcapps.tcsim.simul.*;

import devices.pos.*;
import wucore.utils.geom.*;
import devices.data.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class SimScene 
{
	static public final int			MAX_OBJECTS	= 20;
	private static int				MOVE_TIME	= 200;  // millis

	protected Simulator				simul;
	protected SimSceneUpdater			updater;

	public int	 					numobjects 		= 0;
	public SimObject 				OBJS[];
	protected int					OBJICONS[];

	// Constructors
	public SimScene (String fname, Simulator simul)
	{
		Properties		props;
		File				file;
		FileInputStream	stream;
		String			prop;
		StringTokenizer	st;
		int 				i, j;		

		this.simul	= simul;
		
		props		= new Properties ();
		try
		{
			file 		= new File (fname);
			stream 		= new FileInputStream (file);
			props.load (stream);
			stream.close ();
		} catch (Exception e) { e.printStackTrace (); }
		
		numobjects = Integer.parseInt (props.getProperty ("NOBJECTS","0"));	
		System.out.println ("# Parsing scene-> "+fname+" ("+numobjects+" objects)");
		
		OBJS = new SimObject[numobjects];
		OBJICONS = new int[numobjects];
		simul.VISDATA = new VisionData[numobjects];
		
		for (i=0; i < Simulator.MAX_ROBOTS; i++)
		{
			simul.VISOBJS[i]	= new Position[numobjects];
			for (j=0; j < numobjects; j++)
				simul.VISOBJS[i][j] = new Position ();
		}
				
		for (i=0; i < numobjects; i++)
		{
			Class			tclass;
			Constructor		cons;
			Class[]			types;
			Object[]			params;
			SimObject 		obj = null;		

			prop		= props.getProperty ("OBJECT"+i);
			st		= new StringTokenizer (prop,", \t");
			
			try
			{
				tclass		= Class.forName (st.nextToken ());
				types		= new Class[1];
				types[0]		= Class.forName ("java.lang.String");        
				cons			= tclass.getConstructor (types);
				params		= new Object[1];
				params[0]	= st.nextToken ();        		
				obj			= (SimObject) cons.newInstance (params);		
			} catch (Exception e) { e.printStackTrace (); }

			obj.odesc.label = st.nextToken();
			obj.odesc.pos.x (Double.parseDouble (st.nextToken()));
			obj.odesc.pos.y (Double.parseDouble (st.nextToken()));
			obj.odesc.a = Math.toRadians (Double.parseDouble (st.nextToken()));
//			so.SPEED= Double.parseDouble (st.nextToken());			
			
			OBJS[i]	= obj;
			OBJICONS[i] = simul.allocIcon ();
			simul.moveIcon (OBJICONS[i], obj.odesc.icon,obj.odesc.pos.x(), obj.odesc.pos.y(), obj.odesc.a);
			
			simul.VISDATA[i]	= new VisionData ();		// ESTO SERIA INTERESANTE INICIALIZARLO	
		}	
				
		updater = new SimSceneUpdater ();
	}

	class SimSceneUpdater implements Runnable
	{
		private boolean			running;	
		
		public SimSceneUpdater ()
		{
			running	= true;
			new Thread (this).start ();
		}

		public void run ()
		{
			long			ct, lt, dt;
			
			System.out.println ("  [SIM-Objs] Objects updater thread started.");
			
			lt		= System.currentTimeMillis ();
			while (running)
			{
				int				i;
				Line2			wall;
				double			dist;
				int				robot;

				// Compute timing
				ct		= System.currentTimeMillis();
				dt		= ct - lt;
				lt		= ct;	
				
				for (i=0; i < numobjects; i++)
				{
					if (OBJS[i] instanceof SimMobileObject)				
					{		
						SimMobileObject		mobj;
						
						mobj		= (SimMobileObject) OBJS[i];
						mobj.move (dt/1000.0);
						
						wall		= simul.closerIcon (OBJS[i], OBJICONS[i]);
						dist		= wall.distance (OBJS[i].odesc.pos.x(),OBJS[i].odesc.pos.y());
						
						if (dist > mobj.radius)
							simul.moveIcon (OBJICONS[i], OBJS[i].odesc.icon,OBJS[i].odesc.pos.x(),OBJS[i].odesc.pos.y(),OBJS[i].odesc.a);	
						else 		// Collision with another object
						{
							robot	= simul.collisionIcon (wall);
							if (robot !=- 1)
								mobj.object_pushed (wall, simul.MODEL[robot].vr);
							else
								mobj.wall_collision (wall);
						}
					}
				}				

				try { Thread.sleep (MOVE_TIME); } catch (Exception e) { };			
			}				
		}	
	}
	
	/** Indicates that the robot with id "robotid" has executed a pick operation. 
	 The nearest object will be marked as picked by the robot */
	public void pick_object(int robotid, double z)
	{
		double dx,dy;
		for (int i = 0;i<numobjects;i++)
		{
			if (OBJS[i] instanceof SimCargo)
			{
				dx = simul.MODEL[robotid].real_x-OBJS[i].odesc.pos.x();
				dy = simul.MODEL[robotid].real_y-OBJS[i].odesc.pos.y();

				if (Math.sqrt(dx*dx+dy*dy)<=(OBJS[i].radius+simul.RDESC[robotid].RADIUS))
				{
					((SimCargo) OBJS[i]).pick(simul.lastRobotData[robotid].real_x,simul.lastRobotData[robotid].real_y,z,simul.lastRobotData[robotid].real_a);
					simul.objectPicked[robotid]=i;
					break;
				}
			}
		}
	}
	
	/** Indicates that the robot with id "robotid" has executed a drop operation.
	 The object picked by the robot will be marked as "not picked" and its position
	 will be its current position */
	public void drop_object(int robotid, double z)
	{
		if (simul.objectPicked[robotid]!=-1)
		{
			((SimCargo) OBJS[simul.objectPicked[robotid]]).drop(z);
			simul.objectPicked[robotid]=-1;
		}
	}
} 