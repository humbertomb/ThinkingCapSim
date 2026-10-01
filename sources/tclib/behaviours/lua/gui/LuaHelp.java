/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.gui;

import java.util.ArrayList;
import java.util.List;

import tclib.behaviours.lua.Chaos;
import tclib.behaviours.lua.Tc;
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
		{ "getLpo", "index", "The object of the LPS of that number, as a table (see <i>an object</i> below). The number is "
					+ "not to be written out: every object has a constant of its own (see <i>the constants</i> below), so it is "
					+ "<span class=\"mono\">chaos.getLpo (chaos.BALL_LPO)</span>. A constant that does not exist is nil, and "
					+ "then this gives nil and says so once on the console, with the constants there are." },
		{ "setScanType", "scan", "Asks the vision to scan with the camera this way on this cycle: one of the constants "
					+ "<span class=\"mono\">chaos.SCAN_NONE</span>, <span class=\"mono\">SCAN_LOW</span>, <span class=\"mono\">SCAN_MID</span>, "
					+ "<span class=\"mono\">SCAN_HIGH</span> or <span class=\"mono\">SCAN_FULL</span>. A cycle that asks for none is "
					+ "<span class=\"mono\">SCAN_NONE</span>: the camera stays where the description points it." },
		{ "setNeeded", "index, weight", "Says that the behaviour needs to keep seeing that object, and how much (0 to 1). "
					+ "The vision of the simulation looks everywhere at once, so it is taken note of and no more." },
		{ "getCurrentPos", "", "Where the robot is now: x and y in mm, theta in degrees, in the field. "
					+ "<span class=\"mono\">quality</span> is how sure the localisation is of it [0..1], "
					+ "<span class=\"mono\">anchored</span> 1 when it is a valid position, and "
					+ "<span class=\"mono\">dx</span>, <span class=\"mono\">dy</span> (mm) and "
					+ "<span class=\"mono\">dtheta</span> (degrees) its uncertainty, when there is one." },
		{ "getStartPos", "", "Where the robot starts, the position it is put at for a kick-off: x and y in mm, theta in degrees, in the field." },
		{ "getBallVel", "", "How fast the ball is going, x and y in mm a second." },
		{ "setVlin", "mm/s", "How fast to go forward. Backwards is negative." },
		{ "setVlat", "mm/s", "How fast to go sideways, to the left of the robot. It is commanded like the other two and it is "
					+ "the platform that carries it out or not: a synchro drive steers every wheel together and goes sideways, "
					+ "and a platform whose wheels point where they are built (a differential drive, a tricycle) says so and "
					+ "makes nothing of it." },
		{ "setVrot", "deg/s", "How fast to turn. To the left is positive." },
		{ "setVelocities", "vlin, vlat, vrot", "The three at once: along, across and around, in mm/s and deg/s." },
		{ "setBehavior", "name", "The behaviour to run: the file <span class=\"mono\">&lt;name&gt;.lua</span> of the folder of the "
					+ "behaviours (BEH), which is run right after the program on the same cycle. In a machine of states it is kept "
					+ "while the state that chose it lasts, and dropped on entering another state: a state whose code chooses none runs none." },
		{ "getGameState", "", "What the referee last said: <span class=\"mono\">state</span>, one of the constants "
					+ "<span class=\"mono\">chaos.REFEREE_INITIAL</span>, <span class=\"mono\">REFEREE_READY</span>, <span class=\"mono\">REFEREE_SET</span>, "
					+ "<span class=\"mono\">REFEREE_PLAYING</span>, <span class=\"mono\">REFEREE_PENALIZED</span>, <span class=\"mono\">REFEREE_FINISHED</span> "
					+ "(and <span class=\"mono\">name</span>, the same as text); <span class=\"mono\">player</span>, the robot it is about (-1 for all); "
					+ "<span class=\"mono\">event</span> (STATE, KICKOFF, GOAL, KICKOFF_SHOT, BALL_OUT, ILLEGAL_DEFENDER, TIME_UP), "
					+ "<span class=\"mono\">team</span> and <span class=\"mono\">robot</span> it concerns, <span class=\"mono\">text</span>, "
					+ "<span class=\"mono\">score1</span>, <span class=\"mono\">score2</span> and <span class=\"mono\">time</span> (ms of match). "
					+ "REFEREE_INITIAL while no referee has spoken." },
		{ "getBehaviorInfo", "", "About the behaviour running: <span class=\"mono\">name</span>, "
					+ "<span class=\"mono\">isNew</span> (1 on its first cycle, 0 afterwards), <span class=\"mono\">timer</span> "
					+ "and <span class=\"mono\">time</span> (ms since it started), <span class=\"mono\">finished</span> and "
					+ "<span class=\"mono\">failed</span>. A behaviour that sets itself up does it when isNew is 1." },
		{ "setDesiredPos", "x, y, theta", "Where the robot is to end up: mm and degrees." },
		{ "getDesiredPos", "", "Where it was told to end up, as a point." },
		{ "setKick", "", "Kick now." },
		{ "setSynchroKick", "on", "Kick when the ball is where it should be (true or false)." },
		{ "getRole", "", "The part this robot plays in the team, in <span class=\"mono\">role</span>." },
		{ "getOptimalPose", "", "Where the team would have this robot be, as a point." },
		{ "getDefPose", "", "Where it defends from, as a point." },
		{ "bookBall", "", "Asks the team for the ball, and says whether it was given." },
		{ "haveBookedBall", "", "Whether this robot has the ball booked." },
		{ "releaseBookedBall", "", "Gives the ball back to the team." },
		{ "setGlobal", "name, value", "Keeps a value in the bridge under that name, where every script finds it." },
		{ "getGlobal", "name", "What was kept under that name, or nil." },
		{ "initializeTimer", "name", "Starts a timer: keeps the clock of the execution (ms) in the global of the bridge of that name, "
					+ "as <span class=\"mono\">setGlobal</span> would, so that every script and every state finds it." },
		{ "getTimer", "name", "How long it is (ms) since <span class=\"mono\">initializeTimer</span> was called with that name. "
					+ "A timer that was never started reads 0 and says so once on the console." },
	};

	static private final String[][]	TC_HELP		=
	{
		{ "getGroups", "", "The groups of sensors of the LPS, as a table with one field to a group: <span class=\"mono\">group0</span>, "
					+ "<span class=\"mono\">group1</span> ... <span class=\"mono\">groupN</span>, each the distance it measures in "
					+ "<b>metres</b>. It is an empty table while the LPS has no groups." },
		{ "setVlin", "m/s", "How fast to go forward. Backwards is negative." },
		{ "setVlat", "m/s", "How fast to go sideways, to the left of the robot. A platform whose wheels point where they are "
					+ "built (a differential drive, a tricycle) makes nothing of it." },
		{ "setVrot", "deg/s", "How fast to turn. To the left is positive." },
		{ "setVelocities", "vlin, vlat, vrot", "The three at once: along, across and around, in m/s and deg/s. A velocity "
					+ "not said on a cycle is 0." },
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
		{ "limit", "x, minx, maxx", "x kept between minx and maxx: minx when it is below, maxx when it is above, "
					+ "and x itself in between (the bounds the wrong way round are taken as they were meant). "
					+ "Ours, and what to do with a speed or a turn rate before commanding it: "
					+ "<span class=\"mono\">chaos.setVrot (math.limit (5.5 * ball.theta, -90, 90))</span>." },
		{ "deg", "radians", "As degrees." },
		{ "rad", "degrees", "As radians, as Lua has it: nothing is normalised." },
		{ "normdeg", "degrees", "The same angle brought into -180 .. 180, which is where an angle of the robot is read "
					+ "from: what to do with the difference of two angles before comparing it with anything. Ours." },
		{ "normrad", "radians", "The same, for an angle in radians: into -pi .. pi. Ours." },
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
		h.append ("<p class=\"lead\">Units: <b>distances in millimetres</b>, <b>angles in degrees</b>, ")
		 .append ("<b>speeds in mm a second</b> and <b>turn rates in degrees a second</b>. Every angle a script is given or ")
		 .append ("gives is in degrees and between -180 and 180, the way the turn rates always were. ThinkingCap works in ")
		 .append ("metres and radians, and the bridge does the changing.</p>");
		h.append ("<p class=\"lead\">The functions of <span class=\"mono\">math</span> are the ones of Lua and think in ")
		 .append ("radians, so an angle goes into one through <span class=\"mono\">math.rad</span> and comes out of one ")
		 .append ("through <span class=\"mono\">math.deg</span>: <span class=\"mono\">math.cos (math.rad (ball.theta))</span>, ")
		 .append ("<span class=\"mono\">math.normdeg (math.deg (math.atan2 (dy, dx)))</span>.</p>");

		h.append ("<h2>Contents</h2><p class=\"wire\">");
		h.append ("<a href=\"#chaos\">chaos</a> &nbsp; <a href=\"#tc\">tc</a> &nbsp; <a href=\"#constants\">the constants</a> &nbsp; ")
		 .append ("<a href=\"#tables\">the tables it answers</a> &nbsp; ");
		h.append ("<a href=\"#math\">math</a> &nbsp; <a href=\"#string\">string</a> &nbsp; <a href=\"#table\">table</a> &nbsp; ");
		h.append ("<a href=\"#io\">io</a> &nbsp; <a href=\"#os\">os</a> &nbsp; <a href=\"#base\">the basic ones</a>");
		h.append ("</p>");

		card (h, "chaos", "chaos", "tclib.behaviours.lua.Chaos",
			  "The robot, as a script sees it: what it is given (the objects of the LPS, where it is, what it is called on to do) "
			  + "and what it asks for (a speed, a turn, a behaviour). The controller fills it in before every cycle and reads out "
			  + "of it afterwards.",
			  chaos.table (), CHAOS_HELP);
		card (h, "tc", "tc", "tclib.behaviours.lua.Tc",
			  "The elements of a controller of ThinkingCap, for the programs of a controller that is not a soccer robot's: "
			  + "it works in metres, as ThinkingCap does, and not in the millimetres of chaos.",
			  new Tc ().table (), TC_HELP);

		// the constants of the objects of the LPS, as they are now
		h.append ("<a name=\"constants\"></a><h2>The constants</h2>");
		h.append ("<p>The objects of the LPS are asked for by number, and every one of them has its number as a constant of ")
		 .append ("the table, named after it: a script says <span class=\"mono\">chaos.getLpo (chaos.BALL_LPO)</span> and never ")
		 .append ("a number of its own, so that a program cannot fall out of step with what the module was given in LPOS. ")
		 .append ("They are put in again on every cycle, so writing over one changes nothing for long.</p>");
		h.append ("<table width=\"100%\" cellpadding=\"4\" cellspacing=\"0\">");
		for (int i = 0; i < chaos.lpoNames ().length; i++)
			h.append ("<tr valign=\"top\"><td width=\"38%\"><span class=\"mono\"><b>")
			 .append (Chaos.constant (chaos.lpoNames ()[i])).append ("</b></span></td><td>")
			 .append (i).append (" &mdash; the object the LPS calls <span class=\"mono\">")
			 .append (esc (chaos.lpoNames ()[i])).append ("</span></td></tr>");
		h.append ("</table>");
		h.append ("<p class=\"none\">These are the ones of the objects a module is given by default; a module with an LPOS of ")
		 .append ("its own has a constant for each of the objects it names.</p>");
		h.append ("<p>The kinds of scan of the camera (<span class=\"mono\">chaos.setScanType</span>) are constants too:</p>");
		h.append ("<table width=\"100%\" cellpadding=\"4\" cellspacing=\"0\">");
		for (int i = 0; i < Chaos.SCANS.length; i++)
			h.append ("<tr valign=\"top\"><td width=\"38%\"><span class=\"mono\"><b>")
			 .append (Chaos.SCANS[i].name ()).append ("</b></span></td><td>").append (i).append ("</td></tr>");
		h.append ("</table>");
		h.append ("<p>The states of the game the referee says (<span class=\"mono\">chaos.getGameState</span>) are constants too:</p>");
		h.append ("<table width=\"100%\" cellpadding=\"4\" cellspacing=\"0\">");
		for (int i = 0; i < Chaos.STATES.length; i++)
			h.append ("<tr valign=\"top\"><td width=\"38%\"><span class=\"mono\"><b>")
			 .append (Chaos.REFEREE_).append (Chaos.STATES[i].name ()).append ("</b></span></td><td>").append (i).append ("</td></tr>");
		h.append ("</table><br>");

		// the tables the bridge answers with
		h.append ("<a name=\"tables\"></a><h2>The tables it answers</h2>");
		h.append ("<p>A call of the bridge answers a table, never an object of Java. These are the fields of each, ")
		 .append ("with what they are worth: a script reads them as <span class=\"mono\">ball.rho</span> or ")
		 .append ("<span class=\"mono\">chaos.getGameState ().state</span>.</p>");
		fields (h, "An object", "chaos.getLpo (index)", OBJECT_FIELDS);
		fields (h, "A point", "getCurrentPos, getStartPos, getDesiredPos, getBallVel, getOptimalPose, getDefPose", POINT_FIELDS);
		fields (h, "The behaviour", "chaos.getBehaviorInfo ()", BEHAVIOUR_FIELDS);
		fields (h, "The game", "chaos.getGameState ()", GAME_FIELDS);
		fields (h, "The part played", "chaos.getRole ()", ROLE_FIELDS);

		card (h, "math", "math", "tclib.behaviours.lua.interpreter.LuaLib",
			  "The whole of the numbers. These are the functions of Lua, so they think in radians while everything of the "
			  + "robot is in degrees: <span class=\"mono\">math.rad</span> going in, <span class=\"mono\">math.deg</span> "
			  + "coming out, and <span class=\"mono\">math.normdeg</span> on whatever comes of adding or subtracting angles.", (LuaTable) lua.get ("math"), MATH_HELP);
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
	/* The fields of the tables the bridge answers: name, type and what it is worth */
	static private final String[][]	OBJECT_FIELDS =
	{
		{ "index", "number", "The number of the object, the one it was asked for by (<span class=\"mono\">chaos.BALL_LPO</span> and so on)." },
		{ "name", "string", "What the LPS calls it: Ball, Net1, Net2, Align, Looka, Landmark1 (yellow on top), Landmark2 (sky-blue on top)." },
		{ "rho", "number", "How far it is from the robot, in mm; 0 when it is not there at all." },
		{ "theta", "number", "Which way it is, in degrees, from the heading of the robot: positive to the left. Of the robot, not of the camera." },
		{ "x", "number", "Where it is in front of the robot, in mm (x ahead, y to the left): rho and theta as coordinates." },
		{ "y", "number", "" },
		{ "anchored", "number", "How sure the robot is of it, 0 to 1: 1 when it has just been seen, fading to 0 when it has not been seen for a while. What a script looks at first." },
		{ "quality", "number", "The same as anchored, kept for the scripts of the Chaos robots." },
		{ "active", "boolean", "Whether the LPS is keeping it up to date at all." },
	};

	static private final String[][]	POINT_FIELDS =
	{
		{ "x", "number", "In mm: a position on the field (getCurrentPos, getDesiredPos, the poses) or a speed in mm a second (getBallVel)." },
		{ "y", "number", "" },
		{ "theta", "number", "The heading, in degrees; 0 for a speed." },
		{ "quality", "number", "How good the position is, 0 to 1: 1 in the simulation, which knows where the robot is." },
		{ "anchored", "number", "The same as quality." },
	};

	static private final String[][]	BEHAVIOUR_FIELDS =
	{
		{ "name", "string", "The behaviour running: the one the state chose with setBehavior, or the program itself." },
		{ "isNew", "number", "1 on the first cycle the behaviour runs, 0 afterwards: where a behaviour sets itself up." },
		{ "timer", "number", "How long it has been running, in ms." },
		{ "time", "number", "The same as timer." },
		{ "finished", "number", "Always 0: a behaviour of a state never ends on its own, the machine leaves the state." },
		{ "failed", "number", "Always 0, for the same reason." },
	};

	static private final String[][]	GAME_FIELDS =
	{
		{ "state", "number", "The state of the game, as one of the constants <span class=\"mono\">chaos.REFEREE_INITIAL</span>, "
					+ "<span class=\"mono\">REFEREE_READY</span>, <span class=\"mono\">REFEREE_SET</span>, <span class=\"mono\">REFEREE_PLAYING</span>, "
					+ "<span class=\"mono\">REFEREE_PENALIZED</span>, <span class=\"mono\">REFEREE_FINISHED</span>; REFEREE_INITIAL while no referee has spoken. "
					+ "It is the state as it stands for this robot: PENALIZED while it is sent off (its machine is held meanwhile, so the scripts do not see it), "
					+ "whatever is said of the other players in the meantime." },
		{ "name", "string", "The same state, by its name: INITIAL, READY, SET, PLAYING, PENALIZED, FINISHED." },
		{ "player", "number", "The robot the state is about (its number in the simulation), -1 for all of them." },
		{ "event", "string", "The last thing the referee decided: STATE (a change of state), KICKOFF, GOAL, KICKOFF_SHOT, BALL_OUT, ILLEGAL_DEFENDER, TIME_UP; empty while it has said nothing." },
		{ "team", "number", "The team the decision concerns (0 the red, 1 the blue), -1 for none." },
		{ "robot", "string", "The robot named in the decision (the one that touched the ball last, the one in the area), with its team; empty for none." },
		{ "text", "string", "What the referee said, word for word, as the ticker of its window shows it." },
		{ "score1", "number", "The goals of the red team when it was said." },
		{ "score2", "number", "The goals of the blue team." },
		{ "time", "number", "How long the match had been running, in ms." },
	};

	static private final String[][]	ROLE_FIELDS =
	{
		{ "role", "string", "The part the robot plays: player, goalie, ... as the settings say." },
	};


	/** The fields of one of the tables the bridge answers, as a table of the help: name, type and what it is worth. */
	static private void fields (StringBuilder h, String what, String from, String[][] fields)
	{
		h.append ("<p><b>").append (what).append ("</b> &mdash; <span class=\"mono\">").append (esc (from)).append ("</span></p>");
		h.append ("<table width=\"100%\" cellpadding=\"4\" cellspacing=\"0\">");
		for (String[] f : fields)
		{
			h.append ("<tr valign=\"top\">");
			h.append ("<td width=\"22%\"><span class=\"mono\"><b>").append (esc (f[0])).append ("</b></span></td>");
			h.append ("<td width=\"12%\"><span class=\"none\">").append (esc (f[1])).append ("</span></td>");
			h.append ("<td>").append ((f[2].length () > 0) ? f[2] : "<span class=\"none\">(with the one above)</span>").append ("</td>");
			h.append ("</tr>");
		}
		h.append ("</table><br>");
	}

	/** Whether a table is the robot itself (chaos, tc), which is shown apart from the library of Lua. */
	static private boolean robot (String name)
	{
		return name.equals ("chaos") || name.equals ("tc");
	}

	static private void card (StringBuilder h, String anchor, String name, String clazz, String what, LuaTable t, String[][] help)
	{
		h.append ("<a name=\"").append (anchor).append ("\"></a>");
		h.append ("<table width=\"100%\" cellpadding=\"5\" cellspacing=\"0\" bgcolor=\"")
		 .append (robot (name) ? "#f2f7f2" : "#eceff3").append ("\"><tr>");
		h.append ("<td width=\"64\" align=\"center\" bgcolor=\"").append (robot (name) ? "#2f7d4f" : "#5b6b7c").append ("\">")
		 .append ("<font color=\"#ffffff\" face=\"monospaced\" size=\"2\"><b>").append (robot (name) ? "ROBOT" : "TABLE")
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
