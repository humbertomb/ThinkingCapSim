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

	public void notify_referee (String space, ItemReferee item)
	{
	}
}
