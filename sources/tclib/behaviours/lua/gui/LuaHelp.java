/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.gui;

import java.util.ArrayList;
import java.util.List;

import tclib.behaviours.lua.Chaos;
import tclib.behaviours.lua.interpreter.Lua;
import tclib.behaviours.lua.interpreter.LuaFunction;
import tclib.behaviours.lua.interpreter.LuaState;
import tclib.behaviours.lua.interpreter.LuaTable;

/**
 * The pages of help of the Lua editor, written as HTML in the way the help of
 * the deployment editor is ({@link tcapps.tceditor.HelpWindow} shows them).
 *
 * The page of the classes is written out of the tables themselves -- a fresh
 * bridge and a fresh interpreter are asked what they hold -- so it says what
 * there is and not what there was when it was written down: a function that is
 * there and has nothing said about it is listed all the same, and says so.
 */
public class LuaHelp
{
	/** The style of a page of help, the one the deployment editor uses. */
	static public final String		CSS	=
		"body { font-family: sans-serif; font-size: 11pt; color: #202020; margin: 12px 18px 18px 18px; }"
		+ "h1 { font-size: 17pt; color: #12324d; margin-bottom: 2px; }"
		+ "h2 { font-size: 12pt; color: #24507a; margin-top: 16px; margin-bottom: 4px; }"
		+ "p { margin-top: 3px; margin-bottom: 7px; }"
		+ "li { margin-bottom: 3px; }"
		+ ".lead { color: #555555; }"
		+ ".mono { font-family: monospaced; font-size: 10pt; color: #24507a; }"
		+ ".cls { font-family: monospaced; font-size: 9pt; color: #666666; }"
		+ ".sym { font-family: monospaced; font-size: 12pt; font-weight: bold; color: #12324d; }"
		+ ".wire { font-size: 9pt; color: #4a4a4a; }"
		+ ".none { font-size: 9pt; color: #999999; }";

	static public final String		LANGUAGE	= "The Lua of the Behaviours";
	static public final String		CLASSES		= "What a Script Can Call";

	/* ------------------------------------------------------------------ */
	/* The language                                                        */
	/* ------------------------------------------------------------------ */

	/** A short manual of the Lua the interpreter of the behaviours reads. */
	static public String language ()
	{
		StringBuilder	h = new StringBuilder ();

		h.append ("<html><head><style>").append (CSS).append ("</style></head><body>");
		h.append ("<h1>The Lua of the Behaviours</h1>");
		h.append ("<p class=\"lead\">A behaviour is a Lua script run once on every cycle of the controller, which is every ")
		 .append ("few tens of milliseconds. It reads where things are, works out what the robot is to do and says so through ")
		 .append ("the table it knows as <span class=\"mono\">chaos</span>. It is not a program that runs to its end: it starts ")
		 .append ("and finishes on every cycle, and what it wants to remember it leaves in a global.</p>");
		h.append ("<p class=\"lead\">The interpreter (<span class=\"mono\">tclib.behaviours.lua.interpreter</span>) is ours, and ")
		 .append ("reads Lua 5.1 but for what a behaviour never needs. What follows is what it does read.</p>");

		h.append ("<h2>Values</h2>");
		h.append ("<ul>");
		h.append ("<li><b>nil</b>, <b>boolean</b> (<span class=\"mono\">true</span>, <span class=\"mono\">false</span>), ")
		 .append ("<b>number</b> (every number is a real one: <span class=\"mono\">3</span> and <span class=\"mono\">3.0</span> are the same), ")
		 .append ("<b>string</b> and <b>table</b>.</li>");
		h.append ("<li>Only <span class=\"mono\">nil</span> and <span class=\"mono\">false</span> are false; ")
		 .append ("<span class=\"mono\">0</span> and the empty string are true, as in Lua.</li>");
		h.append ("<li>A table is written <span class=\"mono\">{ 1, 2, 3 }</span> or <span class=\"mono\">{ x = 1, [\"y\"] = 2 }</span>, ")
		 .append ("and read <span class=\"mono\">t[1]</span>, <span class=\"mono\">t.x</span>. The first index is 1.</li>");
		h.append ("</ul>");

		h.append ("<h2>What is written</h2>");
		code (h, "-- a comment, and --[[ a long one ]]\n"
				 + "local rho = ball.rho              -- a local of this script\n"
				 + "count = (count or 0) + 1          -- a global: it is there on the next cycle\n"
				 + "if rho < 300 and ball.anchored > 0.9 then\n"
				 + "\tchaos.setBehavior (\"dokick\")\n"
				 + "elseif rho < 1000 then\n"
				 + "\tchaos.setVlin (200)\n"
				 + "else\n"
				 + "\tchaos.setVlin (400)\n"
				 + "end");
		h.append ("<ul>");
		h.append ("<li><b>Statements</b>: <span class=\"mono\">local</span>, assignment (several at once: ")
		 .append ("<span class=\"mono\">a, b = b, a</span>), a call, <span class=\"mono\">if / elseif / else / end</span>, ")
		 .append ("<span class=\"mono\">while</span>, <span class=\"mono\">repeat / until</span>, ")
		 .append ("<span class=\"mono\">for i = 1, 10, 2</span>, <span class=\"mono\">for k, v in pairs (t)</span>, ")
		 .append ("<span class=\"mono\">do / end</span>, <span class=\"mono\">break</span>, <span class=\"mono\">return</span>.</li>");
		h.append ("<li><b>Functions</b>: <span class=\"mono\">function f (a, b) ... end</span>, ")
		 .append ("<span class=\"mono\">local function</span>, <span class=\"mono\">function t.f ()</span>, ")
		 .append ("a function as a value, and several values returned at once ")
		 .append ("(<span class=\"mono\">return x, y</span>); where one value is wanted, the first is taken.</li>");
		h.append ("<li><b>Operators</b>: <span class=\"mono\">+ - * / % ^</span>, <span class=\"mono\">.. </span> (joining text), ")
		 .append ("<span class=\"mono\">== ~= &lt; &lt;= &gt; &gt;=</span>, <span class=\"mono\">and or not</span>, ")
		 .append ("<span class=\"mono\">#t</span> (how long), unary <span class=\"mono\">-</span>.</li>");
		h.append ("<li><b>Calls</b>: the brackets can be left out of a call with one string or one table, as in Lua ")
		 .append ("(<span class=\"mono\">print \"hello\"</span>).</li>");
		h.append ("</ul>");

		h.append ("<h2>What is not there</h2>");
		h.append ("<p>A behaviour has no use for these, and the interpreter does without them:</p>");
		h.append ("<ul>");
		h.append ("<li><b>Metatables</b> (<span class=\"mono\">setmetatable</span>, <span class=\"mono\">__index</span>): ")
		 .append ("a table is a table, and inheritance is written by hand if it is wanted at all.</li>");
		h.append ("<li><b>goto</b> and its labels: they are read and skipped, so an old script still loads.</li>");
		h.append ("<li><b>Coroutines</b>, <b>modules</b> (<span class=\"mono\">require</span>) and anything that reaches outside ")
		 .append ("the robot: no files, no processes, no network. A script asks the robot for what it needs and nothing else.</li>");
		h.append ("<li>Of the standard library, what is there is in the other page of this help ")
		 .append ("(<i>What a Script Can Call</i>): the whole of <span class=\"mono\">math</span> and a part of ")
		 .append ("<span class=\"mono\">string</span>, <span class=\"mono\">table</span>, <span class=\"mono\">io</span> and ")
		 .append ("<span class=\"mono\">os</span>.</li>");
		h.append ("</ul>");

		h.append ("<h2>Locals, globals and cycles</h2>");
		h.append ("<p>Every script of a robot runs in the same interpreter, one after another, cycle after cycle:</p>");
		h.append ("<ul>");
		h.append ("<li>A <b>local</b> lives for one run of one script. Whatever it was worth is gone on the next cycle.</li>");
		h.append ("<li>A <b>global</b> (a name written with no <span class=\"mono\">local</span>) is shared by every script of ")
		 .append ("the robot and stays between cycles: that is where a timer, a count or a state of its own is kept.</li>");
		h.append ("<li><span class=\"mono\">chaos.setGlobal</span> and <span class=\"mono\">chaos.getGlobal</span> keep values ")
		 .append ("in the bridge instead, which is how the machines of states pass a value from one state to another.</li>");
		h.append ("<li>The monitor of the controller shows all of them while the robot runs, and says which is which.</li>");
		h.append ("</ul>");

		h.append ("<h2>When something is wrong</h2>");
		h.append ("<p>A script that does not read, or that fails while running, is said out loud on the console ")
		 .append ("(<span class=\"mono\">[LUA] lookForBall.lua:34: attempt to perform arithmetic on a nil value</span>) and the ")
		 .append ("cycle carries on: a robot that stops is worse than a robot that misses a cycle. <b>Code &gt; Verify Code</b> ")
		 .append ("(F5) reads what is written with the very interpreter that will run it and points at the line.</p>");
		h.append ("<p>A command that is not a number is refused by the bridge and said once ")
		 .append ("(<span class=\"mono\">[CHAOS] chaos.setVrot was given NaN and ignored</span>): it usually comes of dividing by ")
		 .append ("the angle of an object that was never seen.</p>");

		h.append ("</body></html>");
		return h.toString ();
	}

	/* ------------------------------------------------------------------ */
	/* The classes                                                         */
	/* ------------------------------------------------------------------ */

	/** What every function of a table is for, as <code>name</code>, <code>arguments</code>, <code>what it does</code>. */
	static private final String[][]	CHAOS_HELP	=
	{
		{ "getLpo", "index", "The object of the LPS of that number, as a table (see <i>an object</i> below). "
					+ "The numbers are the ones the module was given in LPOS: 0 the ball, 1 and 2 the nets, 3 the align point, 4 the look-ahead." },
		{ "setNeeded", "index, weight", "Says that the behaviour needs to keep seeing that object, and how much (0 to 1). "
					+ "The vision of the simulation looks everywhere at once, so it is taken note of and no more." },
		{ "getMyPos", "", "Where the robot thinks it is: x and y in mm, theta in radians, in the field." },
		{ "gsGetMyPos", "", "The same, as the Chaos robots asked for it when the position came of the sight of the landmarks. "
					+ "<span class=\"mono\">quality</span> says how sure it is (1 in the simulation)." },
		{ "getBallVel", "", "How fast the ball is going, x and y in mm a second." },
		{ "lps_getAstray", "", "Whether the robot is lost: <span class=\"mono\">astray</span> is always 0, as the simulation knows where it is." },
		{ "setVlin", "mm/s", "How fast to go forward. Backwards is negative." },
		{ "setVlat", "mm/s", "How fast to go sideways, for a robot that can (a wheeled one cannot, and it is ignored)." },
		{ "setVrot", "deg/s", "How fast to turn. To the left is positive." },
		{ "setVelocities", "vlin, vlat, vrot", "The three at once: along, across and around, in mm/s and deg/s." },
		{ "setBehavior", "name", "The behaviour to run: the file <span class=\"mono\">&lt;name&gt;.lua</span> of the folder of the "
					+ "behaviours (BEH), which is run right after the program on the same cycle." },
		{ "getBehaviorInfo", "", "About the behaviour running: <span class=\"mono\">name</span>, "
					+ "<span class=\"mono\">isNew</span> (1 on its first cycle, 0 afterwards), <span class=\"mono\">timer</span> "
					+ "and <span class=\"mono\">time</span> (ms since it started), <span class=\"mono\">finished</span> and "
					+ "<span class=\"mono\">failed</span>. A behaviour that sets itself up does it when isNew is 1." },
		{ "setDesiredPos", "x, y, theta", "Where the robot is to end up: mm and radians." },
		{ "setTargetPos", "x, y, theta", "The same as setDesiredPos, under the name some scripts use." },
		{ "getDesiredPos", "", "Where it was told to end up, as a point." },
		{ "setKick", "", "Kick now." },
		{ "setSynchroKick", "on", "Kick when the ball is where it should be (true or false)." },
		{ "setSurround", "", "Go round the ball instead of at it." },
		{ "trackLandMarks", "", "Point the camera at the landmarks. The camera of the simulation sees everywhere, so it does nothing." },
		{ "getRole", "", "The part this robot plays in the team, in <span class=\"mono\">role</span>." },
		{ "getOptimalPose", "", "Where the team would have this robot be, as a point." },
		{ "getDefPose", "", "Where it defends from, as a point." },
		{ "bookBall", "", "Asks the team for the ball, and says whether it was given." },
		{ "haveBookedBall", "", "Whether this robot has the ball booked." },
		{ "releaseBookedBall", "", "Gives the ball back to the team." },
		{ "setGlobal", "name, index, value", "Keeps a value in the bridge, where every script finds it. With two arguments "
					+ "(<span class=\"mono\">name, value</span>) there is no index." },
		{ "getGlobal", "name [, index]", "What was kept under that name, or nil. Without an index, the last value written." },
	};

	static private final String[][]	MATH_HELP	=
	{
		{ "pi", "", "3.14159..." },
		{ "huge", "", "As big as a number gets." },
		{ "abs", "x", "Without its sign." },
		{ "sign", "x", "Which way it goes: -1 when it is negative, 1 when it is positive or zero "
					+ "(and nothing at all -- not a number -- when it was given nothing at all). "
					+ "This one is ours, and is what <span class=\"mono\">x / math.abs (x)</span> was meant to be, "
					+ "which answers nothing when x is zero." },
		{ "sqrt", "x", "The square root." },
		{ "sin", "x", "Of an angle in radians." },
		{ "cos", "x", "Of an angle in radians." },
		{ "tan", "x", "Of an angle in radians." },
		{ "asin", "x", "In radians." },
		{ "acos", "x", "In radians." },
		{ "atan", "y [, x]", "In radians; with two arguments, of y/x in the right quadrant." },
		{ "atan2", "y, x", "In radians, in the right quadrant." },
		{ "exp", "x", "e to the x." },
		{ "log", "x [, base]", "The natural logarithm, or the one of that base." },
		{ "pow", "x, y", "x to the y (the same as <span class=\"mono\">x ^ y</span>)." },
		{ "floor", "x", "The whole number below." },
		{ "ceil", "x", "The whole number above." },
		{ "fmod", "x, y", "What is left of x divided by y." },
		{ "modf", "x", "Two values: the whole part and what is left." },
		{ "max", "x, ...", "The biggest." },
		{ "min", "x, ...", "The smallest." },
		{ "deg", "radians", "As degrees." },
		{ "rad", "degrees", "As radians, as Lua has it: nothing is normalised." },
		{ "radians", "degrees", "As radians brought into -pi .. pi, which is what an angle of the robot is. "
					+ "This one is ours, and is the one to use on an angle." },
		{ "random", "[m [, n]]", "A number between 0 and 1, between 1 and m, or between m and n." },
		{ "randomseed", "x", "There to be called; the numbers are the ones of the machine." },
	};

	static private final String[][]	STRING_HELP	=
	{
		{ "len", "s", "How many characters." },
		{ "sub", "s, i [, j]", "From the i-th character to the j-th one (the last by default). Counting from the end is negative." },
		{ "upper", "s", "In capitals." },
		{ "lower", "s", "In small letters." },
		{ "rep", "s, n", "The text n times over." },
		{ "format", "fmt, ...", "As <span class=\"mono\">string.format (\"%s is %.2f m away\", o.name, o.rho / 1000)</span>." },
	};

	static private final String[][]	TABLE_HELP	=
	{
		{ "insert", "t, [pos,] value", "Puts a value at the end, or at that place." },
		{ "remove", "t [, pos]", "Takes the last one out, or the one at that place, and answers it." },
		{ "getn", "t", "How many there are (the same as <span class=\"mono\">#t</span>)." },
		{ "concat", "t [, sep [, i [, j]]]", "The values one after another as one text." },
	};

	static private final String[][]	IO_HELP		=
	{
		{ "write", "...", "Writes on the console of the simulator, with no line ending. "
					+ "A behaviour that writes on every cycle is a behaviour nobody can read the log of." },
		{ "read", "", "There is nothing to read from: it answers nil." },
	};

	static private final String[][]	OS_HELP		=
	{
		{ "clock", "", "Seconds of processor time, as a number." },
		{ "time", "", "The time of the machine, in seconds." },
	};

	static private final String[][]	BASE_HELP	=
	{
		{ "print", "...", "Writes on the console, with a line ending." },
		{ "tostring", "v", "The value as text." },
		{ "tonumber", "v", "The value as a number, or nil when it is not one." },
		{ "type", "v", "\"nil\", \"boolean\", \"number\", \"string\", \"table\" or \"function\"." },
		{ "assert", "v [, message]", "Stops the script when v is false." },
		{ "error", "message", "Stops the script, saying that." },
		{ "ipairs", "t", "To walk 1, 2, 3 ... of a table in a <span class=\"mono\">for</span>." },
		{ "pairs", "t", "To walk everything a table holds in a <span class=\"mono\">for</span>." },
		{ "unpack", "t", "The values of a table as several values." },
		{ "_VERSION", "", "Which Lua this is." },
	};

	/** The page of what a script can call: the bridge and the library, as they are. */
	static public String classes ()
	{
		StringBuilder	h = new StringBuilder ();
		LuaState		lua = new LuaState ();				// a fresh one, to say what there is
		Chaos			chaos = new Chaos ();

		h.append ("<html><head><style>").append (CSS).append ("</style></head><body>");
		h.append ("<h1>What a Script Can Call</h1>");
		h.append ("<p class=\"lead\">A behaviour sees one table of its own, <span class=\"mono\">chaos</span>, which is the robot, ")
		 .append ("and the part of the standard library of Lua that is there. This page is written out of those tables themselves, ")
		 .append ("so it says what there is now.</p>");
		h.append ("<p class=\"lead\">Units, as the Chaos robots had them: <b>distances in millimetres</b>, ")
		 .append ("<b>angles in radians</b>, <b>speeds in mm a second</b> and <b>turn rates in degrees a second</b>. ")
		 .append ("ThinkingCap works in metres and radians a second, and the bridge does the changing.</p>");

		h.append ("<h2>Contents</h2><p class=\"wire\">");
		h.append ("<a href=\"#chaos\">chaos</a> &nbsp; <a href=\"#tables\">the tables it answers</a> &nbsp; ");
		h.append ("<a href=\"#math\">math</a> &nbsp; <a href=\"#string\">string</a> &nbsp; <a href=\"#table\">table</a> &nbsp; ");
		h.append ("<a href=\"#io\">io</a> &nbsp; <a href=\"#os\">os</a> &nbsp; <a href=\"#base\">the basic ones</a>");
		h.append ("</p>");

		card (h, "chaos", "chaos", "tclib.behaviours.lua.Chaos",
			  "The robot, as a script sees it: what it is given (the objects of the LPS, where it is, what it is called on to do) "
			  + "and what it asks for (a speed, a turn, a behaviour). The controller fills it in before every cycle and reads out "
			  + "of it afterwards.",
			  chaos.table (), CHAOS_HELP);

		// the tables the bridge answers with
		h.append ("<a name=\"tables\"></a><h2>The tables it answers</h2>");
		h.append ("<p>A call of the bridge answers a table, never an object of Java:</p>");
		h.append ("<ul>");
		h.append ("<li><b>an object</b> (<span class=\"mono\">chaos.getLpo</span>): ")
		 .append ("<span class=\"mono\">index</span>, <span class=\"mono\">name</span>, ")
		 .append ("<span class=\"mono\">rho</span> (how far, mm), <span class=\"mono\">theta</span> (which way, radians), ")
		 .append ("<span class=\"mono\">x</span> and <span class=\"mono\">y</span> (the same, in front of the robot, mm), ")
		 .append ("<span class=\"mono\">anchored</span> and <span class=\"mono\">quality</span> (0 to 1: how sure, and 0 when it ")
		 .append ("has not been seen for a while), <span class=\"mono\">active</span>. ")
		 .append ("An object that is not there at all answers rho 0 and anchored 0, which is what a script is to look at first.</li>");
		h.append ("<li><b>a point</b> (<span class=\"mono\">getMyPos</span>, <span class=\"mono\">getDesiredPos</span>, ")
		 .append ("<span class=\"mono\">getBallVel</span>, <span class=\"mono\">getOptimalPose</span>, ")
		 .append ("<span class=\"mono\">getDefPose</span>): <span class=\"mono\">x</span>, <span class=\"mono\">y</span> (mm), ")
		 .append ("<span class=\"mono\">theta</span> (radians), <span class=\"mono\">quality</span>, <span class=\"mono\">anchored</span>.</li>");
		h.append ("<li><b>the behaviour</b> (<span class=\"mono\">getBehaviorInfo</span>) and <b>the part played</b> ")
		 .append ("(<span class=\"mono\">getRole</span>), as said above.</li>");
		h.append ("</ul>");

		card (h, "math", "math", "tclib.behaviours.lua.interpreter.LuaLib",
			  "The whole of the numbers. Angles are in radians, so an angle of the robot in degrees is put right with "
			  + "<span class=\"mono\">math.radians</span>.", (LuaTable) lua.get ("math"), MATH_HELP);
		card (h, "string", "string", "tclib.behaviours.lua.interpreter.LuaLib",
			  "The part of the text a behaviour needs, which is little: what it writes is for a person to read on the console.",
			  (LuaTable) lua.get ("string"), STRING_HELP);
		card (h, "table", "table", "tclib.behaviours.lua.interpreter.LuaLib",
			  "Lists, by their number from 1 up.", (LuaTable) lua.get ("table"), TABLE_HELP);
		card (h, "io", "io", "tclib.behaviours.lua.interpreter.LuaLib",
			  "What a script says out loud. Nothing of this reaches a file: it goes to the console of the simulator, and a window "
			  + "can be given it instead (<span class=\"mono\">LuaLib.output</span>).",
			  (LuaTable) lua.get ("io"), IO_HELP);
		card (h, "os", "os", "tclib.behaviours.lua.interpreter.LuaLib",
			  "The time, and no more: nothing of this asks anything of the machine the robot runs on.",
			  (LuaTable) lua.get ("os"), OS_HELP);

		// the basic functions, which are the globals of the interpreter
		h.append ("<a name=\"base\"></a>");
		h.append ("<table width=\"100%\" cellpadding=\"5\" cellspacing=\"0\" bgcolor=\"#eceff3\"><tr>");
		h.append ("<td width=\"64\" align=\"center\" bgcolor=\"#5b6b7c\">")
		 .append ("<font color=\"#ffffff\" face=\"monospaced\" size=\"2\"><b>BASIC</b></font></td>");
		h.append ("<td><span class=\"sym\">the basic ones</span></td>");
		h.append ("<td align=\"right\"><span class=\"cls\">tclib.behaviours.lua.interpreter.LuaLib</span></td></tr>");
		h.append ("<tr><td></td><td colspan=\"2\">Called by their name alone, with no table before them.</td></tr>");
		h.append ("</table>");
		rows (h, BASE_HELP);
		h.append ("<br>");

		h.append ("<p class=\"none\">Written from a bridge and an interpreter made for the occasion: what is listed is what a ")
		 .append ("script running now would find.</p>");
		h.append ("</body></html>");
		return h.toString ();
	}

	/* ------------------------------------------------------------------ */
	/* Helpers                                                             */
	/* ------------------------------------------------------------------ */

	/** One table of the library: the card of its name, and a row for every name in it. */
	static private void card (StringBuilder h, String anchor, String name, String clazz, String what, LuaTable t, String[][] help)
	{
		h.append ("<a name=\"").append (anchor).append ("\"></a>");
		h.append ("<table width=\"100%\" cellpadding=\"5\" cellspacing=\"0\" bgcolor=\"")
		 .append (name.equals ("chaos") ? "#f2f7f2" : "#eceff3").append ("\"><tr>");
		h.append ("<td width=\"64\" align=\"center\" bgcolor=\"").append (name.equals ("chaos") ? "#2f7d4f" : "#5b6b7c").append ("\">")
		 .append ("<font color=\"#ffffff\" face=\"monospaced\" size=\"2\"><b>").append (name.equals ("chaos") ? "ROBOT" : "TABLE")
		 .append ("</b></font></td>");
		h.append ("<td><span class=\"sym\">").append (name).append ("</span></td>");
		h.append ("<td align=\"right\"><span class=\"cls\">").append (esc (clazz)).append ("</span></td></tr>");
		h.append ("<tr><td></td><td colspan=\"2\">").append (what).append ("</td></tr>");
		h.append ("</table>");
		rows (h, listed (t, help));
		h.append ("<br>");
	}

	/** The rows of one table: name, arguments and what it does. */
	static private void rows (StringBuilder h, String[][] help)
	{
		h.append ("<table width=\"100%\" cellpadding=\"4\" cellspacing=\"0\">");
		for (String[] e : help)
		{
			h.append ("<tr valign=\"top\">");
			h.append ("<td width=\"38%\"><span class=\"mono\"><b>").append (esc (e[0])).append ("</b>")
			 .append ((e[1].length () > 0) ? (" (" + esc (e[1]) + ")") : "").append ("</span></td>");
			h.append ("<td>").append ((e[2].length () > 0) ? e[2]
									  : "<span class=\"none\">Nothing is written down of this one yet.</span>").append ("</td>");
			h.append ("</tr>");
		}
		h.append ("</table>");
	}

	/**
	 * What a table really holds, in the order it was written down: the ones that are
	 * there with what is said of them, and then the ones nothing is said of, which
	 * are listed all the same rather than left out.
	 */
	static private String[][] listed (LuaTable t, String[][] help)
	{
		List<String[]>	rows = new ArrayList<String[]> ();
		List<String>	said = new ArrayList<String> ();

		for (String[] e : help)
		{
			said.add (e[0]);
			if ((t == null) || (t.get (e[0]) != null))		rows.add (e);
			else											rows.add (new String[] { e[0], e[1],
																					 "<span class=\"none\">Not there any more.</span>" });
		}
		if (t != null)
			for (Object k : t.keys ())
			{
				String	name = Lua.tostring (k);

				if (said.contains (name))					continue;
				rows.add (new String[] { name, (t.get (k) instanceof LuaFunction) ? "..." : "", "" });
			}
		return rows.toArray (new String[0][]);
	}

	/** A piece of a script, as it would be written in the editor. */
	static private void code (StringBuilder h, String text)
	{
		h.append ("<table cellpadding=\"6\" cellspacing=\"0\" bgcolor=\"#f6f6f6\" width=\"100%\"><tr><td><span class=\"mono\">")
		 .append (esc (text).replace ("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
						   .replace ("  ", "&nbsp;&nbsp;").replace ("\n", "<br>"))
		 .append ("</span></td></tr></table>");
	}

	static private String esc (String s)
	{
		if (s == null)								return "";
		return s.replace ("&", "&amp;").replace ("<", "&lt;").replace (">", "&gt;");
	}
}
