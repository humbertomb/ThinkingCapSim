/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Writes a machine of states back as the <code>.xas</code> file
 * {@link XMLParser} reads, with the Lua script of every state and transition in
 * the <code>.acc</code> files beside it.
 *
 * The file is written in the same shape the editor of the Chaos robots wrote, so
 * that a machine can go back and forth between the two.
 */
public class XMLWriter
{
	protected StringBuffer			out;

	/** The whole machine (the .xas and the .acc files) into the folder of the file. */
	static public void write (MetaState root, File file, List<XMLParser.PrivateVar> vars) throws Exception
	{
		File			dir = (file.getParentFile () != null) ? file.getParentFile () : new File (".");

		if (!dir.exists ())						dir.mkdirs ();
		Files.write (file.toPath (), text (root, vars).getBytes (StandardCharsets.UTF_8));
		writeCode (root, dir);
	}

	/** The text of the .xas file. */
	static public String text (MetaState root, List<XMLParser.PrivateVar> vars)
	{
		XMLWriter		w = new XMLWriter ();

		w.out	= new StringBuffer ();
		w.strategy (root, vars);
		return w.out.toString ();
	}

	/* ------------------------------------------------------------------ */
	/* The .xas file                                                       */
	/* ------------------------------------------------------------------ */

	protected void strategy (MetaState root, List<XMLParser.PrivateVar> vars)
	{
		out.append ("\n<strategy")
		   .append (attr ("stateCount", count (root, false)))
		   .append (attr ("transitionCount", root.getTransitionCount () + deepTransitions (root)))
		   .append (attr ("metaStateCountName", root.metaStateCountToName))
		   .append (attr ("stateCountName", root.stateCountToName))
		   .append (attr ("transitionCountName", root.transitionCountToName))
		   .append (" >\n");
		contents (root);
		for (Transition t : root.getTransitions ())			// the root has no level above it
			transition (t, root);
		if (vars != null)
			for (XMLParser.PrivateVar v : vars)
				out.append ("<privatevariable").append (attr ("name", v.name)).append (attr ("type", v.type))
				   .append (attr ("numElements", v.numElements)).append (attr ("initValue", v.initValue))
				   .append (attr ("msname", v.msname)).append (" ></privatevariable>\n");
		out.append ("</strategy>\n");
	}

	/**
	 * The states and then the transitions of a level, as the parser expects to find
	 * them. What leaves a state is written at the level of the state, so the
	 * transitions of a meta state go with the level that holds it and not inside it.
	 */
	protected void contents (MetaState m)
	{
		for (State s : m.getStatesList ())
			if (s instanceof MetaState)			metastate ((MetaState) s, m);
		for (State s : m.getStatesList ())
			if (!(s instanceof MetaState))		state (s, m);
		for (State s : m.getStatesList ())
			for (Transition t : s.getTransitions ())
				transition (t, s);
	}

	protected void metastate (MetaState ms, MetaState parent)
	{
		out.append ("<metastate").append (attr ("name", ms.getName ())).append (attr ("id", ms.getId ()))
		   .append (attr ("x", ms.getX ())).append (attr ("y", ms.getY ()))
		   .append (attr ("metaStateCountName", ms.metaStateCountToName))
		   .append (attr ("stateCountName", ms.stateCountToName))
		   .append (attr ("transitionCountName", ms.transitionCountToName))
		   .append (attr ("initial", (parent.getInitialState () == ms) ? 1 : 0))
		   .append (attr ("extern", ms.isExtern () ? 1 : 0));
		if (ms.isExtern () && (ms.getPathExtern () != null))
			out.append (attr ("pathExtern", ms.getPathExtern ()));
		out.append (" >\n");
		contents (ms);
		out.append ("</metastate>\n");
	}

	protected void state (State s, MetaState parent)
	{
		out.append ("<state").append (attr ("name", s.getName ())).append (attr ("id", s.getId ()))
		   .append (attr ("x", s.getX ())).append (attr ("y", s.getY ()))
		   .append (attr ("initial", (parent.getInitialState () == s) ? 1 : 0))
		   .append (" >\n</state>\n");
	}

	protected void transition (Transition t, State from)
	{
		out.append ("<transition").append (attr ("name", t.getName ())).append (attr ("id", t.getId ()))
		   .append (attr ("x", t.getX ())).append (attr ("y", t.getY ()))
		   .append (attr ("priority", t.getPriority ()))
		   .append (attr ("to", (t.getArrivalState () != null) ? t.getArrivalState ().getId () : -1))
		   .append (attr ("from", from.getId ()))
		   .append (" ></transition>\n");
	}

	/* ------------------------------------------------------------------ */
	/* The .acc files                                                      */
	/* ------------------------------------------------------------------ */

	/** The script of every state and transition under a machine, into a folder. */
	static public void writeCode (MetaState m, File dir) throws Exception
	{
		code (new File (m.getPathCode (dir.getPath ())), m.getCode ());
		for (State s : m.getStatesList ())
		{
			if (s instanceof MetaState)			writeCode ((MetaState) s, dir);
			else								code (new File (s.getPathCode (dir.getPath ())), s.getCode ());
			for (Transition t : s.getTransitions ())
			{
				code (new File (t.getPathTestCode (dir.getPath ())), t.getTestCode ());
				code (new File (t.getPathDoCode (dir.getPath ())), t.getDoCode ());
			}
		}
		for (Transition t : m.getTransitions ())
		{
			code (new File (t.getPathTestCode (dir.getPath ())), t.getTestCode ());
			code (new File (t.getPathDoCode (dir.getPath ())), t.getDoCode ());
		}
	}

	/**
	 * One script into its file. A script that says nothing is not written, and its
	 * file, if there is one, is emptied rather than left as it was.
	 */
	static protected void code (File f, String text) throws Exception
	{
		boolean			empty = (text == null) || (text.trim ().length () == 0);

		if (empty && !f.exists ())				return;
		Files.write (f.toPath (), (empty ? "" : text).getBytes (StandardCharsets.UTF_8));
	}

	/* ------------------------------------------------------------------ */
	/* Helpers                                                             */
	/* ------------------------------------------------------------------ */

	/** How many states a machine holds, the meta states counted only when asked. */
	static public int count (MetaState m, boolean metas)
	{
		int				n = 0;

		for (State s : m.getStatesList ())
			if (s instanceof MetaState)			n += (metas ? 1 : 0) + count ((MetaState) s, metas);
			else								n++;
		return n;
	}

	static private int deepTransitions (MetaState m)
	{
		int				n = 0;

		for (State s : m.getStatesList ())
			if (s instanceof MetaState)			n += ((MetaState) s).getTransitionCount () + deepTransitions ((MetaState) s);
		return n;
	}

	static private String attr (String name, int value)		{ return attr (name, Integer.toString (value)); }

	static private String attr (String name, String value)
	{
		return " " + name + "='" + escape ((value != null) ? value : "") + "'";
	}

	static private String escape (String s)
	{
		return s.replace ("&", "&amp;").replace ("<", "&lt;").replace (">", "&gt;").replace ("'", "&apos;").replace ("\"", "&quot;");
	}
}
