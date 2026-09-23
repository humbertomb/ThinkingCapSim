/*
 * (c) 2005 Daniel Garcia Nebot, Elad Rodriguez Alvaro, Miguel Cazorla
 * (c) 2026 Humberto Martinez Barbera (ported to ThinkingCap)
 */

package tclib.behaviours.hfsm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads a machine of states from the <code>.xas</code> file the editor writes:
 *
 * <pre>
 *   &lt;strategy stateCount='4' transitionCount='8' ... &gt;
 *     &lt;metastate name='GoToArea' id='31' x='783' y='831' initial='1' extern='0' &gt;
 *       &lt;state name='GoToNet2' id='32' x='70' y='262' initial='1' /&gt;
 *       &lt;transition name='InAreaGoTo' id='34' x='403' y='97' priority='1' to='33' from='32' /&gt;
 *     &lt;/metastate&gt;
 *     &lt;privatevariable name='BALL_CLOSE' type='int' numElements='1' initValue='600' msname='gklua2' /&gt;
 *   &lt;/strategy&gt;
 * </pre>
 *
 * The states of a level are read first and the transitions of that same level
 * afterwards, so a transition always finds where it comes from and where it goes
 * by id. What a transition cannot find is said out loud and left null, which the
 * verification of the machine then reports.
 */
public class XMLParser extends DefaultHandler
{
	/** How deep a machine may be nested. */
	static public final int			MAXLEVEL	= 64;

	/** One of the constants the machine declares (name, type, elements, value, machine). */
	static public class PrivateVar
	{
		public String				name;
		public String				type;
		public String				numElements;
		public String				initValue;
		public String				msname;

		public String toString ()				{ return name + " = " + initValue + " (" + type + ")"; }
	}

	protected MetaState[]			meta;						// the machine of each level
	protected int					level;

	protected int					stateCount;
	protected int					transitionCount;
	protected List<PrivateVar>		privVars = new ArrayList<PrivateVar> ();
	protected List<String>			problems = new ArrayList<String> ();

	/** Reads into <code>m</code>, which is the machine the file holds. */
	public XMLParser (MetaState m)
	{
		meta		= new MetaState[MAXLEVEL];
		meta[0]		= m;
		level		= 0;
	}

	/* ---------------- what was read ---------------- */

	public final MetaState			root ()				{ return meta[0]; }
	public final int				stateCount ()		{ return stateCount; }
	public final int				transitionCount ()	{ return transitionCount; }
	public final List<PrivateVar>	privateVars ()		{ return privVars; }
	/** What could not be made sense of, if anything. */
	public final List<String>		problems ()			{ return problems; }

	/* ---------------- reading a file ---------------- */

	/**
	 * The machine a <code>.xas</code> file holds, its states and transitions
	 * carrying the Lua scripts of the folder of the file.
	 */
	static public MetaState read (File file) throws Exception
	{
		XMLParser		p = parse (file);
		MetaState		root = p.root ();

		root.loadCode (file.getParent ());
		root.sortAll ();
		return root;
	}

	/** The parser that read a file, so that the counters and the constants can be had. */
	static public XMLParser parse (File file) throws Exception
	{
		String			name = file.getName ();
		int				dot = name.lastIndexOf ('.');
		MetaState		root = new MetaState ((dot > 0) ? name.substring (0, dot) : name, 0);
		XMLParser		handler = new XMLParser (root);
		SAXParserFactory	factory = SAXParserFactory.newInstance ();

		factory.setNamespaceAware (false);
		factory.setValidating (false);
		// the files carry no DTD, and none is to be looked for on the network
		try { factory.setFeature ("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); }
		catch (Exception e) { }

		SAXParser		parser = factory.newSAXParser ();

		parser.parse (file, handler);
		return handler;
	}

	/* ---------------- SAX ---------------- */

	public void startElement (String uri, String localname, String qName, Attributes atts) throws SAXException
	{
		String			tag = ((localname != null) && (localname.length () > 0)) ? localname : qName;

		if (tag.equals ("strategy"))			strategy (atts);
		else if (tag.equals ("metastate"))		metastate (atts);
		else if (tag.equals ("state"))			state (atts);
		else if (tag.equals ("transition"))		transition (atts);
		else if (tag.equals ("privatevariable"))		privateVar (atts);
	}

	public void endElement (String uri, String localname, String qName) throws SAXException
	{
		String			tag = ((localname != null) && (localname.length () > 0)) ? localname : qName;

		if (tag.equals ("metastate") && (level > 0))		level--;
	}

	/* ---------------- the elements ---------------- */

	private void strategy (Attributes atts)
	{
		stateCount		= integer (atts, "stateCount", 0);
		transitionCount	= integer (atts, "transitionCount", 0);
		counters (meta[0], atts);
	}

	private void metastate (Attributes atts)
	{
		MetaState		ms = new MetaState (name (atts), integer (atts, "id", 0), integer (atts, "x", 0), integer (atts, "y", 0));

		counters (ms, atts);
		meta[level].addState (ms);
		if (integer (atts, "initial", 0) == 1)			meta[level].setInitialState (ms);

		ms.setExtern (integer (atts, "extern", 0) == 1);
		if (ms.isExtern ())								ms.setPathExtern (atts.getValue ("pathExtern"));

		if (level < (MAXLEVEL - 1))						meta[++level] = ms;
		else											problems.add ("Meta state '" + ms.getName () + "' is nested too deep");
	}

	private void state (Attributes atts)
	{
		State			s = new State (name (atts), integer (atts, "id", 0), integer (atts, "x", 0), integer (atts, "y", 0));

		meta[level].addState (s);
		if (integer (atts, "initial", 0) == 1)			meta[level].setInitialState (s);
	}

	private void transition (Attributes atts)
	{
		Transition		t = new Transition (null, name (atts), integer (atts, "id", 0), integer (atts, "x", 0), integer (atts, "y", 0));
		int				from = integer (atts, "from", -1);
		int				to = integer (atts, "to", -1);
		State			origin = findState (from);
		State			arrival = findState (to);

		t.setPriority (integer (atts, "priority", 1));
		t.setArrivalState (arrival);
		if (arrival == null)
			problems.add ("Transition '" + t.getName () + "' arrives at state " + to + ", which the machine does not have");
		if (origin == null)
			problems.add ("Transition '" + t.getName () + "' leaves state " + from + ", which the machine does not have");
		else
			origin.addTransition (t);
	}

	private void privateVar (Attributes atts)
	{
		PrivateVar		v = new PrivateVar ();

		v.name			= atts.getValue ("name");
		v.type			= atts.getValue ("type");
		v.numElements	= atts.getValue ("numElements");
		v.initValue		= atts.getValue ("initValue");
		v.msname		= atts.getValue ("msname");
		privVars.add (v);
	}

	/**
	 * The state of the current level with that id, or, when it is not there, the one
	 * with that id anywhere in the machine (the editor writes a transition between
	 * two levels that way).
	 */
	public State findState (int id)
	{
		if (id < 0)										return null;

		State			s = meta[level].findState (id);

		if (s != null)									return s;
		if (meta[level].getId () == id)					return meta[level];
		return findState (meta[0], id);
	}

	static private State findState (MetaState m, int id)
	{
		if (m.getId () == id)							return m;
		for (State s : m.getStatesList ())
		{
			if (s.getId () == id)						return s;
			if (s instanceof MetaState)
			{
				State	found = findState ((MetaState) s, id);

				if (found != null)						return found;
			}
		}
		return null;
	}

	/* ---------------- attributes ---------------- */

	static private void counters (MetaState m, Attributes atts)
	{
		m.metaStateCountToName	= integer (atts, "metaStateCountName", 0);
		m.stateCountToName		= integer (atts, "stateCountName", 0);
		m.transitionCountToName	= integer (atts, "transitionCountName", 0);
	}

	static private String name (Attributes atts)
	{
		String			n = atts.getValue ("name");

		return (n != null) ? n : "";
	}

	static private int integer (Attributes atts, String key, int def)
	{
		String			v = atts.getValue (key);

		if (v == null)									return def;
		try { return Integer.parseInt (v.trim ()); }
		catch (NumberFormatException e) { return def; }
	}
}
