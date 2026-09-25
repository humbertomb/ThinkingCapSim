/*
 * (c) 2004-2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator.simulator.objects;

import java.util.ArrayList;
import java.util.List;

import tc.shared.world.WMAObject;
import tc.shared.world.World;

import tcapps.tcsimulator.simulator.Simulator;

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
	static private final int		MOVE_TIME	= 50;		// period of the updater (ms)

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

	/** Puts the collision outline of an object at its current pose (sensors and other objects see it there). */
	public void updateIcon (int i)
	{
		if ((i < 0) || (i >= numobjects))		return;
		simul.moveIcon (OBJICONS[i], OBJS[i].odesc.getLocalIcon (), OBJS[i].odesc.pos.x (), OBJS[i].odesc.pos.y (), OBJS[i].odesc.a);
	}

	/**
	 * Puts an object where it is told, as it is: at a pose, facing a way and, if it
	 * was moving, stopped. It is what a hand on the visualisation does to the
	 * simulation while it runs -- to see what a module makes of a ball put in
	 * front of the robot, say -- and it changes nothing of the world the object
	 * came from, only where the simulation has it now.
	 */
	public void place (int i, double x, double y, double a)
	{
		if ((i < 0) || (i >= numobjects))		return;

		WMAObject	o = OBJS[i].odesc;

		o.pos.set (x, y, o.pos.z ());
		o.a		= a;
		o.invalidate ();
		if (OBJS[i] instanceof SimMobileObject)		((SimMobileObject) OBJS[i]).v = 0.0;
		updateIcon (i);
	}

	/** Removes the collision outline of an object (a load carried by a robot is not an obstacle). */
	public void clearIcon (int i)
	{
		if ((i < 0) || (i >= numobjects))		return;
		simul.moveIcon (OBJICONS[i], new Line2[0], 0.0, 0.0, 0.0);
	}

	class Updater implements Runnable
	{
		volatile boolean			running;

		// Where each robot was, and how it moves (m, m/s)
		protected double[]			rx	= new double[Simulator.MAX_ROBOTS];
		protected double[]			ry	= new double[Simulator.MAX_ROBOTS];
		protected boolean[]			rknown	= new boolean[Simulator.MAX_ROBOTS];
		protected double[]			rvx	= new double[Simulator.MAX_ROBOTS];
		protected double[]			rvy	= new double[Simulator.MAX_ROBOTS];

		/** The velocity of each robot, from where it is now and where it was dt seconds ago. */
		protected void robots (double dt)
		{
			for (int r = 0; (r < simul.numrobots) && (r < rx.length); r++)
			{
				if (simul.MODEL[r] == null)		continue;
				double	x = simul.MODEL[r].real_x, y = simul.MODEL[r].real_y;
				if (rknown[r] && (dt > 0.0))		{ rvx[r] = (x - rx[r]) / dt; rvy[r] = (y - ry[r]) / dt; }
				else								{ rvx[r] = 0.0; rvy[r] = 0.0; }
				rx[r]		= x;
				ry[r]		= y;
				rknown[r]	= true;
			}
		}

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

				// Compute timing
				ct		= System.currentTimeMillis ();
				dt		= ct - lt;
				lt		= ct;

				// how the robots move (from where they were the last time), which is what they push the objects with
				robots (dt / 1000.0);

				for (i = 0; i < numobjects; i++)
				{
					if (!(OBJS[i] instanceof SimMobileObject))		continue;

					SimMobileObject		mobj = (SimMobileObject) OBJS[i];
					mobj.move (dt / 1000.0);

					// the robots: discs (of their radius) the object cannot get into
					for (int r = 0; r < simul.numrobots; r++)
						if ((simul.MODEL[r] != null) && (simul.RDESC[r] != null))
							mobj.robot_collision (simul.MODEL[r].real_x, simul.MODEL[r].real_y, simul.RDESC[r].RADIUS, rvx[r], rvy[r]);

					// the walls (and the other objects)
					wall		= simul.closerObstacle (OBJS[i], OBJICONS[i]);
					dist		= (wall != null) ? wall.distance (OBJS[i].odesc.pos.x (), OBJS[i].odesc.pos.y ()) : Double.MAX_VALUE;
					if (dist <= mobj.radius)
						mobj.wall_collision (wall);

					simul.moveIcon (OBJICONS[i], OBJS[i].odesc.getLocalIcon (), OBJS[i].odesc.pos.x (), OBJS[i].odesc.pos.y (), OBJS[i].odesc.a);
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
		double		rx = simul.MODEL[robotid].real_x, ry = simul.MODEL[robotid].real_y, ra = simul.MODEL[robotid].real_a;
		if (simul.lastRobotData[robotid] != null)		// pose the robot last reported (the model may be a step ahead)
		{
			rx = simul.lastRobotData[robotid].real_x;
			ry = simul.lastRobotData[robotid].real_y;
			ra = simul.lastRobotData[robotid].real_a;
		}
		if (simul.objectPicked[robotid] != -1)			// already carrying one
		{
			System.out.println ("  [SIM-Objs] " + OBJS[simul.objectPicked[robotid]].odesc.label + " is already loaded on robot " + robotid);
			return;
		}
		for (int i = 0; i < numobjects; i++)
		{
			if (!(OBJS[i] instanceof SimCargo) || ((SimCargo) OBJS[i]).isPicked ())		continue;

			dx = rx - OBJS[i].odesc.pos.x ();
			dy = ry - OBJS[i].odesc.pos.y ();

			if (Math.sqrt (dx * dx + dy * dy) <= (OBJS[i].radius + simul.RDESC[robotid].RADIUS))
			{
				((SimCargo) OBJS[i]).pick (rx, ry, z, ra);
				simul.objectPicked[robotid] = i;
				clearIcon (i);						// carried: no longer an obstacle where it was
				System.out.println ("  [SIM-Objs] Robot " + robotid + " loads " + OBJS[i].odesc.label);
				return;
			}
		}
		System.out.println ("  [SIM-Objs] Robot " + robotid + " found no load to pick up");
	}

	/** Indicates that the robot with id "robotid" has executed a drop operation.
	 The object picked by the robot will be marked as "not picked" and its position
	 will be its current position */
	public void drop_object (int robotid, double z)
	{
		int		i = simul.objectPicked[robotid];
		if ((i < 0) || (i >= numobjects))		return;

		((SimCargo) OBJS[i]).drop (z);
		simul.objectPicked[robotid] = -1;
		updateIcon (i);								// an obstacle again, at the place where it was left
		System.out.println ("  [SIM-Objs] Robot " + robotid + " unloads " + OBJS[i].odesc.label);
	}
}
