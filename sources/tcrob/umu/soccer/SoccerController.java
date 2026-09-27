/*
 * Created on 27-sep-2026
 *
 * (c) 2026 Humberto Martinez
 */
package tcrob.umu.soccer;

import tc.runtime.thread.ModuleConfig;
import tc.shared.linda.Linda;
import tclib.behaviours.hfsm.*;
import tcrob.umu.soccer.linda.*;

public class SoccerController extends HFSMController
{
	public SoccerController (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}

	/** What the referee says (REFEREE) goes to the machine through the bridge: chaos.getGameState reads it. */
	public void notify_referee (String space, ItemReferee item)
	{
		if (item == null)			return;
		chaos.referee (item);
		if (debug)					System.out.println ("  [SoccerController] " + item);
	}
}
