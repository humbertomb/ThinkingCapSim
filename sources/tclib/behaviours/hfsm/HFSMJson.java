/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * How a machine of states is kept: one <code>.hfsm</code> file in JSON, holding
 * the whole machine -- its states, its meta states, its transitions and the Lua
 * of every one of them -- so that a machine is one file and not a folder full of
 * them, as the Chaos editor left it (see {@link XMLParser}, which still reads
 * those).
 *
 * <pre>
 *   {
 *     "name": "gk",
 *     "vars": [ { "name": "BALL_CLOSE", "type": "int", "value": "600" } ],
 *     "machine": {
 *       "name": "gk", "id": 0, "initial": "InitialGK",
 *       "states": [
 *         { "name": "GoGkPos", "id": 1, "x": 63, "y": 442, "code": "..." ,
 *           "transitions": [ { "name": "ballNear", "id": 9, "x": 420, "y": 8,
 *                              "to": "Clear", "toId": 2, "priority": 1,
 *                              "test": "...", "action": "..." } ] },
 *         { "meta": true, "name": "Score", "id": 7, "initial": "Approach", "states": [ ... ] }
 *       ]
 *     }
 *   }
 * </pre>
 *
 * A transition says where it arrives both by name and by id, and is read by id
 * first, so that a machine whose states were renamed by hand still holds
 * together.
 */
public class HFSMJson
{
	/** What a file holds: the machine, the constants it declares, and what could not be read. */
	static public class Machine
	{
		public MetaState						root;
		public List<XMLParser.PrivateVar>		vars = new ArrayList<XMLParser.PrivateVar> ();
		public List<String>						problems = new ArrayList<String> ();
	}

	/** How a file of a machine is named. */
	static public final String		SUFFIX		= ".hfsm";

	/** Where the machines are kept. */
	static public final String		FOLDER		= "./conf/programs/hfsm";

	static private final Gson		GSON		= new GsonBuilder ().setPrettyPrinting ().disableHtmlEscaping ().create ();

	/* ------------------------------------------------------------------ */
	/* Writing                                                             */
	/* ------------------------------------------------------------------ */

	/** The machine into a file. */
	static public void write (MetaState root, File file, List<XMLParser.PrivateVar> vars) throws Exception
	{
		File			dir = file.getParentFile ();

		if ((dir != null) && !dir.exists ())	dir.mkdirs ();
		Files.write (file.toPath (), text (root, vars).getBytes (StandardCharsets.UTF_8));
	}

	/** The text of the file. */
	static public String text (MetaState root, List<XMLParser.PrivateVar> vars)
	{
		return GSON.toJson (toJson (root, vars)) + "\n";
	}

	static public JsonObject toJson (MetaState root, List<XMLParser.PrivateVar> vars)
	{
		JsonObject		o = new JsonObject ();

		o.addProperty ("name", root.getName ());
		if ((vars != null) && !vars.isEmpty ())
		{
			JsonArray	arr = new JsonArray ();

			for (XMLParser.PrivateVar v : vars)
			{
				JsonObject	vo = new JsonObject ();

				vo.addProperty ("name", v.name);
				if (v.type != null)			vo.addProperty ("type", v.type);
				if (v.numElements != null)	vo.addProperty ("elements", v.numElements);
				vo.addProperty ("value", v.initValue);
				if (v.msname != null)		vo.addProperty ("machine", v.msname);
				arr.add (vo);
			}
			o.add ("vars", arr);
		}
		o.add ("machine", meta (root));
		return o;
	}

	/** A meta state and everything under it. */
	static private JsonObject meta (MetaState m)
	{
		JsonObject		o = state (m);
		JsonArray		arr = new JsonArray ();

		o.remove ("code");					// a meta state does what its states do, and has no script of its own
		o.addProperty ("meta", true);
		if (m.getInitialState () != null)		o.addProperty ("initial", m.getInitialState ().getName ());
		if (m.isExtern ())
		{
			o.addProperty ("extern", true);
			if (m.getPathExtern () != null)		o.addProperty ("path", m.getPathExtern ());
		}
		for (State s : m.getStatesList ())
			arr.add ((s instanceof MetaState) ? meta ((MetaState) s) : state (s));
		o.add ("states", arr);
		return o;
	}

	/** A state: what it is called, where it is drawn, what it does and what leaves it. */
	static private JsonObject state (State s)
	{
		JsonObject		o = new JsonObject ();

		o.addProperty ("name", s.getName ());
		o.addProperty ("id", Integer.valueOf (s.getId ()));
		o.addProperty ("x", Integer.valueOf (s.getX ()));
		o.addProperty ("y", Integer.valueOf (s.getY ()));
		if (s.getCode ().trim ().length () > 0)		o.addProperty ("code", s.getCode ());
		if (s.getTransitionsSize () > 0)
		{
			JsonArray	arr = new JsonArray ();

			for (Transition t : s.getTransitions ())		arr.add (transition (t));
			o.add ("transitions", arr);
		}
		return o;
	}

	static private JsonObject transition (Transition t)
	{
		JsonObject		o = new JsonObject ();

		o.addProperty ("name", t.getName ());
		o.addProperty ("id", Integer.valueOf (t.getId ()));
		o.addProperty ("x", Integer.valueOf (t.getX ()));
		o.addProperty ("y", Integer.valueOf (t.getY ()));
		if (t.getPriority () != 1)				o.addProperty ("priority", Integer.valueOf (t.getPriority ()));
		if (t.getArrivalState () != null)
		{
			o.addProperty ("to", t.getArrivalState ().getName ());
			o.addProperty ("toId", Integer.valueOf (t.getArrivalState ().getId ()));
		}
		if (t.getTestCode ().trim ().length () > 0)		o.addProperty ("test", t.getTestCode ());
		if (t.getDoCode ().trim ().length () > 0)		o.addProperty ("action", t.getDoCode ());
		return o;
	}

	/* ------------------------------------------------------------------ */
	/* Reading                                                             */
	/* ------------------------------------------------------------------ */

	/** The machine a file holds, its scripts with it. */
	static public Machine read (File file) throws Exception
	{
		return fromJsonText (new String (Files.readAllBytes (file.toPath ()), StandardCharsets.UTF_8));
	}

	static public Machine fromJsonText (String text) throws Exception
	{
		JsonElement		e = JsonParser.parseString (text);

		if ((e == null) || !e.isJsonObject ())
			throw new IllegalArgumentException ("Not a machine of states: the file holds no object");
		return fromJson (e.getAsJsonObject ());
	}

	static public Machine fromJson (JsonObject o)
	{
		Machine				out = new Machine ();
		JsonElement			m = o.get ("machine");

		for (JsonElement e : array (o, "vars"))
		{
			JsonObject				vo = e.getAsJsonObject ();
			XMLParser.PrivateVar	v = new XMLParser.PrivateVar ();

			v.name			= string (vo, "name", "");
			v.type			= string (vo, "type", "float");
			v.numElements	= string (vo, "elements", "1");
			v.initValue		= string (vo, "value", "0");
			v.msname		= string (vo, "machine", null);
			out.vars.add (v);
		}

		if ((m == null) || !m.isJsonObject ())
			throw new IllegalArgumentException ("Not a machine of states: it holds no \"machine\"");

		// the states first, then what joins them, so that a transition finds where it goes
		Map<Integer, State>		byId = new HashMap<Integer, State> ();
		Map<String, State>		byName = new HashMap<String, State> ();
		List<Runnable>			joins = new ArrayList<Runnable> ();

		out.root	= (MetaState) readState (m.getAsJsonObject (), true, byId, byName, joins, out.problems);
		for (Runnable join : joins)			join.run ();
		out.root.sortAll ();
		return out;
	}

	/**
	 * One state (or meta state) and what is under it. The transitions are left for
	 * later, in <code>joins</code>, as they refer to states not read yet.
	 */
	static private State readState (JsonObject o, boolean meta, final Map<Integer, State> byId, final Map<String, State> byName,
									List<Runnable> joins, final List<String> problems)
	{
		String			name = string (o, "name", "");
		int				id = integer (o, "id", 0);
		boolean			isMeta = meta || bool (o, "meta", false);
		final State		s = isMeta ? new MetaState (name, id, integer (o, "x", 0), integer (o, "y", 0))
								   : new State (name, id, integer (o, "x", 0), integer (o, "y", 0));

		s.setCode (string (o, "code", ""));
		byId.put (Integer.valueOf (id), s);
		if (!byName.containsKey (name))			byName.put (name, s);

		if (isMeta)
		{
			final MetaState		ms = (MetaState) s;
			final String		initial = string (o, "initial", null);

			ms.setExtern (bool (o, "extern", false));
			if (ms.isExtern ())					ms.setPathExtern (string (o, "path", null));
			for (JsonElement e : array (o, "states"))
				ms.addState (readState (e.getAsJsonObject (), false, byId, byName, joins, problems));

			if (initial != null)
				joins.add (new Runnable ()
				{
					public void run ()
					{
						State	init = ms.findState (initial);

						if (init != null)		ms.setInitialState (init);
						else					problems.add ("Meta state '" + ms.getName () + "' starts at '" + initial
															  + "', which it does not hold");
					}
				});
			else if (ms.getStatesSize () > 0)
				problems.add ("Meta state '" + ms.getName () + "' says nowhere to start");
		}

		for (JsonElement e : array (o, "transitions"))
		{
			JsonObject			to = e.getAsJsonObject ();
			final Transition	t = new Transition (null, string (to, "name", ""), integer (to, "id", 0),
													integer (to, "x", 0), integer (to, "y", 0));
			final String		dest = string (to, "to", null);
			final int			destId = integer (to, "toId", -1);

			t.setPriority (integer (to, "priority", 1));
			t.setTestCode (string (to, "test", ""));
			t.setDoCode (string (to, "action", ""));
			s.addTransition (t);

			joins.add (new Runnable ()
			{
				public void run ()
				{
					State	arrival = (destId >= 0) ? byId.get (Integer.valueOf (destId)) : null;

					if ((arrival == null) && (dest != null))		arrival = byName.get (dest);
					t.setArrivalState (arrival);
					if ((arrival == null) && ((dest != null) || (destId >= 0)))
						problems.add ("Transition '" + t.getName () + "' arrives at '"
									  + ((dest != null) ? dest : Integer.toString (destId)) + "', which the machine does not have");
				}
			});
		}
		return s;
	}

	/* ---------------- what a file may leave out ---------------- */

	static private JsonArray array (JsonObject o, String key)
	{
		JsonElement		e = (o != null) ? o.get (key) : null;

		return ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
	}

	static private String string (JsonObject o, String key, String def)
	{
		JsonElement		e = (o != null) ? o.get (key) : null;

		return ((e != null) && e.isJsonPrimitive ()) ? e.getAsString () : def;
	}

	static private int integer (JsonObject o, String key, int def)
	{
		JsonElement		e = (o != null) ? o.get (key) : null;

		try { return ((e != null) && e.isJsonPrimitive ()) ? e.getAsInt () : def; }
		catch (NumberFormatException x) { return def; }
	}

	static private boolean bool (JsonObject o, String key, boolean def)
	{
		JsonElement		e = (o != null) ? o.get (key) : null;

		try { return ((e != null) && e.isJsonPrimitive ()) ? e.getAsBoolean () : def; }
		catch (RuntimeException x) { return def; }
	}

	/* ------------------------------------------------------------------ */
	/* From the machines of the Chaos editor                               */
	/* ------------------------------------------------------------------ */

	/**
	 * A machine as the Chaos editor left it (a <code>.xas</code> and the
	 * <code>.acc</code> files beside it), ready to be written as one file.
	 */
	static public Machine importChaos (File xas) throws Exception
	{
		XMLParser		parser = XMLParser.parse (xas);
		Machine			out = new Machine ();

		out.root	= parser.root ();
		out.vars	= parser.privateVars ();
		out.problems.addAll (parser.problems ());
		out.root.loadCode (xas.getParent ());
		out.root.sortAll ();
		renumber (out.root);
		return out;
	}

	/**
	 * Gives every state and transition of a machine an id no other one has. The
	 * files of the Chaos editor number the states of each meta state on their own,
	 * so the machine that holds them all may well have two things numbered the same,
	 * and one file cannot then say which of them a transition arrives at.
	 */
	static public void renumber (MetaState root)
	{
		int				id = 0;

		root.setId (id++);
		for (State s : states (root))
		{
			if (s != root)						s.setId (id++);
			for (Transition t : s.getTransitions ())
				t.setId (id++);
		}
	}

	/** Every state and meta state of a machine, the root first. */
	static private List<State> states (MetaState root)
	{
		List<State>		out = new ArrayList<State> ();

		out.add (root);
		gather (root, out);
		return out;
	}

	static private void gather (MetaState m, List<State> out)
	{
		for (State s : m.getStatesList ())
		{
			out.add (s);
			if (s instanceof MetaState)			gather ((MetaState) s, out);
		}
	}

	/** Where a machine of that name is kept. */
	static public File fileOf (String name)
	{
		return new File (FOLDER, name + SUFFIX);
	}
}
