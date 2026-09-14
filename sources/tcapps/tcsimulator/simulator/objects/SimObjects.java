/*
 * (c) 2004-2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator.simulator.objects;

import java.util.ArrayList;
import java.util.List;

import tc.shared.world.WMAObject;
import tc.shared.world.World;

import tcapps.tcsimulator.simulator.Simulator;

import devices.data.VisionData;
import devices.pos.Position;
import wucore.utils.geom.Line2;

/**
 * The animated objects of the simulated world ({@link World#aobjects}), each
 * one wrapped in the {@link SimObject} that implements its dynamics. An
 * updater thread moves the mobile ones and resolves their collisions with the
 * walls and the robots; the cargo ones can be picked and dropped by the
 * robots (fork operations).
 */
public class SimObjects
{
	static private final int		MOVE_TIME	= 200;		// period of the updater (ms)

	protected Simulator				simul;
	protected Updater				updater;

	public int	 					numobjects;
	public SimObject[]				OBJS;
	protected int[]					OBJICONS;

	// Constructors
	public SimObjects (World world, Simulator simul)
	{
		List<WMAObject>		aobjs = (world != null) ? world.aobjects () : new ArrayList<WMAObject> ();
		int					i, j;

		this.simul	= simul;
		numobjects	= aobjs.size ();
		System.out.println ("# Animated objects of the world: " + numobjects);

		OBJS		= new SimObject[numobjects];
		OBJICONS	= new int[numobjects];
		simul.VISDATA = new VisionData[numobjects];
		for (i = 0; i < Simulator.MAX_ROBOTS; i++)
		{
			simul.VISOBJS[i]	= new Position[numobjects];
			for (j = 0; j < numobjects; j++)
				simul.VISOBJS[i][j] = new Position ();
		}

		for (i = 0; i < numobjects; i++)
		{
			WMAObject	ao = aobjs.get (i);
			OBJS[i]		= SimObject.create (ao);
			OBJICONS[i]	= simul.allocIcon ();
			simul.moveIcon (OBJICONS[i], ao.getLocalIcon (), ao.pos.x (), ao.pos.y (), ao.a);
			simul.VISDATA[i]	= new VisionData ();
			System.out.println ("  [SIM-Objs] " + ao.label + " -> " + OBJS[i].getClass ().getSimpleName () + ((ao.movement == WMAObject.Movement.STATIC) ? "" : " (" + ao.movement.name ().toLowerCase () + ")"));
		}

		updater = new Updater ();
	}

	/** Stops the updater thread (the objects keep their last pose). */
	public void stop ()
	{
		if (updater != null)		updater.running = false;
		updater = null;
	}

	public SimObject at (int i)					{ return ((i >= 0) && (i < numobjects)) ? OBJS[i] : null; }

	class Updater implements Runnable
	{
		volatile boolean			running;

		public Updater ()
		{
			running	= true;
			Thread	t = new Thread (this, "SIM-Objs");
			t.setDaemon (true);
			t.start ();
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
				ct		= System.currentTimeMillis ();
				dt		= ct - lt;
				lt		= ct;

				for (i = 0; i < numobjects; i++)
				{
					if (!(OBJS[i] instanceof SimMobileObject))		continue;

					SimMobileObject		mobj = (SimMobileObject) OBJS[i];
					mobj.move (dt / 1000.0);

					wall		= simul.closerIcon (OBJS[i], OBJICONS[i]);
					dist		= (wall != null) ? wall.distance (OBJS[i].odesc.pos.x (), OBJS[i].odesc.pos.y ()) : Double.MAX_VALUE;

					if (dist > mobj.radius)
						simul.moveIcon (OBJICONS[i], OBJS[i].odesc.getLocalIcon (), OBJS[i].odesc.pos.x (), OBJS[i].odesc.pos.y (), OBJS[i].odesc.a);
					else 		// Collision with a robot or a wall
					{
						robot	= simul.collisionIcon (wall);
						if (robot != -1)
							mobj.object_pushed (wall, simul.MODEL[robot].vr);
						else
							mobj.wall_collision (wall);
					}
				}

				try { Thread.sleep (MOVE_TIME); } catch (Exception e) { }
			}
			System.out.println ("  [SIM-Objs] Objects updater thread stopped.");
		}
	}

	/** Indicates that the robot with id "robotid" has executed a pick operation.
	 The nearest object will be marked as picked by the robot */
	public void pick_object (int robotid, double z)
	{
		double		dx, dy;
		for (int i = 0; i < numobjects; i++)
		{
			if (OBJS[i] instanceof SimCargo)
			{
				dx = simul.MODEL[robotid].real_x - OBJS[i].odesc.pos.x ();
				dy = simul.MODEL[robotid].real_y - OBJS[i].odesc.pos.y ();

				if (Math.sqrt (dx * dx + dy * dy) <= (OBJS[i].radius + simul.RDESC[robotid].RADIUS))
				{
					((SimCargo) OBJS[i]).pick (simul.lastRobotData[robotid].real_x, simul.lastRobotData[robotid].real_y, z, simul.lastRobotData[robotid].real_a);
					simul.objectPicked[robotid] = i;
					break;
				}
			}
		}
	}

	/** Indicates that the robot with id "robotid" has executed a drop operation.
	 The object picked by the robot will be marked as "not picked" and its position
	 will be its current position */
	public void drop_object (int robotid, double z)
	{
		if (simul.objectPicked[robotid] != -1)
		{
			((SimCargo) OBJS[simul.objectPicked[robotid]]).drop (z);
			simul.objectPicked[robotid] = -1;
		}
	}
}
