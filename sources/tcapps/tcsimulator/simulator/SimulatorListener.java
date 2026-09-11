/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator.simulator;

import tc.shared.world.World;
import tc.vrobot.RobotData;
import tc.vrobot.RobotDesc;
import tcapps.tcsimulator.simulator.objects.SimObject;
import wucore.utils.geom.Point3;

/**
 * Visualisation of a {@link Simulator}: the simulator notifies the robots and
 * objects it manages and, periodically (from its refresh thread), their
 * current state. Implemented by the simulator windows.
 */
public interface SimulatorListener
{
	/** The simulator world (map) has been set or replaced. */
	public void setWorldmap (World map);

	/** A robot was added; returns the index to be used in {@link #updateData}. */
	public int addRobot (RobotDesc rdesc, SimulatorDesc sdesc);

	/** Current data (pose, sensors) of a robot. Called from the simulator refresh thread. */
	public void updateData (int roboindex, RobotData data);

	/** An object was added to the scene; returns its index for {@link #updateObjectData}. */
	public int addObject (SimObject object);

	/** An object was added at a given pose (pallets created at runtime). */
	public int addObject (SimObject object, Point3 pos, double a);

	public void removeObject (int objindex);

	public void removeAllObjects ();

	/** Current pose of an object. Called from the simulator refresh thread. */
	public void updateObjectData (int objindex, Point3 pt, double a);

	/** End of a refresh cycle: redraw. Called from the simulator refresh thread. */
	public void repaint ();
}
