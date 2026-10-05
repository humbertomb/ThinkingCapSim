/*
 * (c) 2026 Humberto Martinez
 */

package tcrob.umu.soccer;

import tc.modules.LindaRouter;
import tc.shared.linda.*;
import tcrob.umu.soccer.linda.SoccerTuple;

/**
 * The Linda router of a soccer player: it brings to the local space of the
 * robot what the referee says (REFEREE), which is written on the global space
 * by a supervisor that runs outside every robot ({@link SoccerRefereeSimul}),
 * and the execution commands (EXECUTION) addressed to the robot.
 *
 * For now that is all it routes: nothing goes from the local space to the
 * global one, and none of the rest of what the base router carries (PLAN,
 * MOTION, the behaviours, CONFIG, STATUS, ...) is registered.
 */
public class SoccerLindaRouter extends LindaRouter
{
	public SoccerLindaRouter (String robotid, Linda lindalocal, Linda lindaglobal)
	{
		super (robotid, lindalocal, lindaglobal);
	}

	protected void initialise (LindaListener listener)
	{
		// Register GLOBAL linda listeners: the commands to this robot, and what
		// the referee says to everyone
		lindaglobal.register (new Tuple (robotid, Tuple.EXECUTION, null),	listener);
		lindaglobal.register (new Tuple (SoccerTuple.REFEREE),				listener);
	}

	public void notify (Tuple tuple)
	{
		if ((tuple.key == null) || (tuple.value == null))		return;

		if (debug) System.out.print ("  [SoccerRouter] <=" + tuple);

		// Tuples from global to local: the commands to this robot, and the referee (broadcast)
		if (tuple.key.equals (Tuple.EXECUTION) || tuple.key.equals (SoccerTuple.REFEREE))
		{
			lindalocal.write (tuple);
			if (debug) System.out.println (" L=>" + tuple);
		}
		else
			System.out.println ("--[SoccerRouter] Un-requested tuple KEY received");
	}
}
