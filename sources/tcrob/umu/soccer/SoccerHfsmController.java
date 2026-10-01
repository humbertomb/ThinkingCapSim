/*
 * Created on 27-sep-2026
 *
 * (c) 2026 Humberto Martinez
 */
package tcrob.umu.soccer;

import tc.runtime.thread.ModuleConfig;
import tc.shared.linda.ItemBehNeeds;
import tc.shared.linda.ItemNavigation;
import tc.shared.linda.Linda;
import tc.shared.linda.Tuple;
import tclib.behaviours.hfsm.HFSMController;
import tclib.behaviours.lua.Chaos;
import tclib.behaviours.lua.LuaBridge;
import tcrob.umu.soccer.linda.ItemReferee;

/**
 * The controller of a soccer robot driven by a machine of states: an
 * {@link HFSMController} whose scripts speak to the robot through
 * <code>chaos</code> ({@link Chaos}), the bridge of the Chaos robots. They read
 * the objects of the LPS and where the robot is, what the referee says
 * (REFEREE, chaos.getGameState), and they command the three velocities of the
 * platform (vlin, vlat, vrot), a behaviour, and what they need of the vision
 * (the scan of the camera and the objects to keep seeing), which is told to
 * the vision as BEH_NEEDS.
 *
 * Settings, besides those of {@link HFSMController}:
 * <pre>
 *   LPOS        the objects of the LPS the scripts ask for by number,
 *               separated by commas (default Ball, Net1, Net2, Align, Looka, Landmark1, Landmark2)
 * </pre>
 */
public class SoccerHfsmController extends HFSMController
{
	protected Chaos					chaos;
	protected Tuple					ntuple;						// what the scripts need of the vision (BEH_NEEDS): the scan of the camera and the objects
	protected String				nsaid;						// what the vision was last told (scan and needs, as text); null: nothing yet

	public SoccerHfsmController (ModuleConfig cfg, Linda linda)
	{
		super (cfg, linda);
	}

	/** The bridge is chaos, with the objects of the LPS the settings name (LPOS). */
	protected LuaBridge bridge (ModuleConfig cfg)
	{
		String		lpos = cfg.get ("LPOS");

		chaos	= new Chaos ();
		if (lpos != null)						chaos.lpoNames (lpos.split ("[,;\\s]+"));
		ntuple	= new Tuple (Tuple.BEHNEEDS, null);
		nsaid	= null;
		return chaos;
	}

	protected volatile boolean		penalized;			// sent off by the referee: the machine is held until it says otherwise

	public final Chaos				chaos ()			{ return chaos; }

	/** What the scripts are to read: the LPS and where the robot is told to go (where it is comes from the localisation, see notify_navigation). */
	protected void before ()
	{
		chaos.lps (lps);
		if (has_plan && (plan.tpos != null))
			chaos.desired ().set (plan.tpos);
	}

	/** What the scripts commanded: the vision is told what they need, and the velocities are answered. */
	protected double[] after ()
	{
		needs ();
		return new double[] { chaos.linear (), chaos.lateral (), chaos.rotation () };
	}

	/** Another machine, or the same one afresh: the vision is told again what it needs. */
	protected void machineChanged ()
	{
		nsaid	= null;
	}

	/** The description of the robot says where it starts (START_X, START_Y, START_A), which the scripts read with chaos.getStartPos. */
	public void notify_config (String space, tc.shared.linda.ItemConfig item)
	{
		super.notify_config (space, item);
		if ((item.props_robot == null) || (chaos == null))		return;
		try
		{
			String	x = item.props_robot.getProperty ("START_X"), y = item.props_robot.getProperty ("START_Y"), a = item.props_robot.getProperty ("START_A");

			if ((x != null) && (y != null) && (a != null))
				chaos.start (Double.parseDouble (x), Double.parseDouble (y), Double.parseDouble (a));
		}
		catch (NumberFormatException e)		{ }
	}

	/** What the referee says (REFEREE) goes to the machine through the bridge: chaos.getGameState reads it. */
	/**
	 * What the referee says. A tuple about one player (its player is set) changes
	 * the state of the game for that player alone: PENALIZED stops this robot when
	 * it is the one named, and is not undone by what is said of the others or of
	 * the game while it lasts -- only by the referee telling this robot it is back,
	 * or by the game leaving PLAYING (a kick-off, the end). The scripts read all of
	 * it through chaos.getGameState ().
	 */
	public void notify_referee (String space, ItemReferee item)
	{
		if (item == null)			return;
		chaos.referee (item);

		String		me = robotName ();
		boolean		mine = (item.player < 0) || (item.robot == null) || (me == null)
						   || item.robot.equals (me) || item.robot.startsWith (me + " ");		// the messages name the team after the robot

		if (!mine)											// about another player: the game as it stands for this one does not change
			;
		else if (item.state == ItemReferee.GameStates.PENALIZED)
		{
			penalized	= true;
			chaos.gameState (item.state);
		}
		else if ((item.player < 0) && penalized && (item.state == ItemReferee.GameStates.PLAYING))
			;												// the game goes on without this robot
		else
		{
			penalized	= false;
			chaos.gameState (item.state);
		}
		if (debug)					System.out.println ("  [SoccerHfsm] " + item + (penalized ? "  (penalised)" : ""));
	}

	/** Where the localisation (SoccerLocalization) makes the robot to be, as it is, to the scripts (chaos.getCurrentPos). */
	public void notify_navigation (String space, ItemNavigation item)
	{
		if ((item == null) || (item.robot == null) || (chaos == null))		return;

		chaos.pose (item.robot);
		if (debug)					System.out.println ("  [SoccerHfsm] position " + item.robot + " quality " + item.robot.quality);
	}

	/** The name of this robot, as the referee names the players. */
	protected String robotName ()
	{
		return (tdesc != null) ? tdesc.robotid : null;
	}

	/**
	 * Tells the vision what the scripts of the machine need of it (BEH_NEEDS): the
	 * scan of the camera asked for on this cycle (chaos.setScanType), SCAN_NONE when
	 * none was, and the objects it needs to keep seeing and how much
	 * (chaos.setNeeded), by the names the LPS knows them by. It is written when it
	 * is not what the vision was last told, and on the first cycle, so that a
	 * vision that starts scanning on its own is told to stop unless a script says
	 * otherwise. It is the same the program of a {@link tclib.behaviours.lua.LuaController} does.
	 */
	protected void needs ()
	{
		ItemBehNeeds.ScanTypes	scan = chaos.scanType ();
		String[]				names = chaos.lpoNames ();
		StringBuilder			said = new StringBuilder (scan.name ());
		java.util.List<Integer>	idx = new java.util.ArrayList<Integer> (chaos.needed ().keySet ());

		java.util.Collections.sort (idx);
		for (Integer i : idx)
			if ((i >= 0) && (i < names.length))
				said.append (' ').append (names[i]).append ('=').append (chaos.needed ().get (i));
		if (said.toString ().equals (nsaid))		return;

		// a new item every time: a shared Linda hands the reader the very object, and
		// one filled in again underneath it could be read half done
		ItemBehNeeds	nitem = new ItemBehNeeds ();

		nitem.changeScan (scan);
		for (Integer i : idx)
			if ((i >= 0) && (i < names.length))
				nitem.addNeed (names[i], chaos.needed ().get (i), System.currentTimeMillis ());
		nitem.set (System.currentTimeMillis ());
		ntuple.value	= nitem;
		linda.write (ntuple);
		nsaid	= said.toString ();
	}
}
