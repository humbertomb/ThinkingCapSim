/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tclib.behaviours.lua.interpreter.LuaAst.*;

/**
 * Runs the tree a Lua script was read into, over a table of globals: the same
 * globals for every script of a machine, so that what one leaves another one
 * finds, as a Lua state does.
 *
 * Several return values are answered as an <code>Object[]</code>; wherever one
 * value is wanted, the first one is taken.
 */
public class LuaInterp
{
	/** What a block leaves behind: a place for a local, shared with the functions written inside it. */
	static protected class Cell
	{
		Object						value;
		String						wrong;						// what the call it was declared from complained of, if it did

		Cell (Object v)							{ value = v; }
	}

	/** The locals in reach, innermost first. */
	static protected class Scope
	{
		Map<String, Cell>			locals = new HashMap<String, Cell> ();
		Scope						up;

		Scope (Scope up)						{ this.up = up; }

		Cell find (String name)
		{
			for (Scope s = this; s != null; s = s.up)
			{
				Cell	c = s.locals.get (name);

				if (c != null)					return c;
			}
			return null;
		}

		Cell declare (String name, Object v)
		{
			Cell	c = new Cell (v);

			locals.put (name, c);
			return c;
		}
	}

	/* Control flow, without the cost of a stack trace */
	static protected class BreakSignal extends RuntimeException
	{
		private static final long	serialVersionUID = 1L;

		BreakSignal ()							{ super (null, null, false, false); }
	}

	static protected class ReturnSignal extends RuntimeException
	{
		private static final long	serialVersionUID = 1L;

		Object						value;

		ReturnSignal (Object v)					{ super (null, null, false, false); value = v; }
	}

	/** A function written in Lua: its tree, and the locals it was born among. */
	protected class Closure extends LuaFunction
	{
		Func						def;
		Scope						scope;

		Closure (Func def, Scope scope)
		{
			super (def.name);

			this.def	= def;
			this.scope	= scope;
		}

		public Object call (Object[] args)
		{
			Scope		s = new Scope (scope);

			for (int i = 0; i < def.params.size (); i++)
				s.declare (def.params.get (i), arg (args, i));
			if (def.varargs)
			{
				List<Object>	rest = new ArrayList<Object> ();

				for (int i = def.params.size (); (args != null) && (i < args.length); i++)
					rest.add (args[i]);
				s.declare ("...", rest.toArray ());
			}

			try { exec (def.body, s); }
			catch (ReturnSignal r) { return r.value; }
			return null;
		}
	}

	protected LuaTable				globals;
	protected String				chunk;

	/* Watching the locals, for whoever looks at a program while it runs */
	protected boolean				watch;
	protected Map<String, Map<String, Cell>>	watched = new java.util.LinkedHashMap<String, Map<String, Cell>> ();
	protected String				complaint;					// what a call of the statement being run complained of

	public LuaInterp (LuaTable globals)
	{
		this.globals	= (globals != null) ? globals : new LuaTable ();
	}

	public final LuaTable globals ()			{ return globals; }

	/**
	 * Whether the locals every script declares are kept track of, which is what a
	 * monitor of a running program looks at. It costs a note per local declared, so
	 * nobody pays for it unless they ask.
	 */
	public void watch (boolean b)				{ watch = b;	if (!b)		watched.clear (); }
	public boolean watching ()					{ return watch; }

	/** Nothing is remembered of the locals of the runs so far, watched or not. */
	public void forget ()						{ watched.clear (); }

	/**
	 * The locals of every script as its last run left them, by script and in the
	 * order they were declared. Empty unless {@link #watch} was asked for.
	 */
	public Map<String, Map<String, Object>> locals ()
	{
		Map<String, Map<String, Object>>	all = new java.util.LinkedHashMap<String, Map<String, Object>> ();

		for (Map.Entry<String, Map<String, Cell>> e : watched.entrySet ())
		{
			Map<String, Object>		one = new java.util.LinkedHashMap<String, Object> ();

			for (Map.Entry<String, Cell> c : e.getValue ().entrySet ())
				one.put (c.getKey (), c.getValue ().value);
			all.put (e.getKey (), one);
		}
		return all;
	}

	/**
	 * The locals of every script that were declared from a call that complained (see
	 * {@link LuaFunction#complain}), by script and name, and what the complaint was:
	 * a local declared as <code>chaos.getLpo (chaos.BALL_NET1)</code>, of a constant
	 * there is not, is nil, and this says why. Empty unless {@link #watch} was asked for.
	 */
	public Map<String, Map<String, String>> complaints ()
	{
		Map<String, Map<String, String>>	all = new java.util.LinkedHashMap<String, Map<String, String>> ();

		for (Map.Entry<String, Map<String, Cell>> e : watched.entrySet ())
			for (Map.Entry<String, Cell> c : e.getValue ().entrySet ())
				if (c.getValue ().wrong != null)
				{
					Map<String, String>		one = all.get (e.getKey ());

					if (one == null)			all.put (e.getKey (), one = new java.util.LinkedHashMap<String, String> ());
					one.put (c.getKey (), c.getValue ().wrong);
				}
		return all;
	}

	/** Takes note of a local that was just declared, while its script is being watched. */
	protected void noted (String name, Cell cell)
	{
		if (!watch || (cell == null))			return;

		String		where = (chunk != null) ? chunk : "?";
		Map<String, Cell>	one = watched.get (where);

		if (one == null)						watched.put (where, one = new java.util.LinkedHashMap<String, Cell> ());
		one.put (name, cell);
	}

	/** Runs a script, reading it first. */
	public Object run (String src, String chunk)
	{
		return run (LuaParser.parse (src, chunk), chunk);
	}

	/** Runs a script already read, and answers what it returns (nil when it returns nothing). */
	public Object run (Block block, String chunk)
	{
		String		old = this.chunk;

		this.chunk	= chunk;
		if (watch)						watched.remove ((chunk != null) ? chunk : "?");	// the locals of this run, not of the last
		try
		{
			exec (block, new Scope (null));
			return null;
		}
		catch (ReturnSignal r) { return first (r.value); }
		finally { this.chunk = old; }
	}

	/** The first of several values, or the value itself when it is only one. */
	static public Object first (Object v)
	{
		if (v instanceof Object[])
		{
			Object[]	a = (Object[]) v;

			return (a.length > 0) ? a[0] : null;
		}
		return v;
	}

	static private Object arg (Object[] args, int i)
	{
		return ((args != null) && (i < args.length)) ? args[i] : null;
	}

	/* ------------------------------------------------------------------ */
	/* Statements                                                          */
	/* ------------------------------------------------------------------ */

	protected void exec (Block b, Scope scope)
	{
		for (Stat s : b.stats)
			exec (s, scope);
	}

	protected void exec (Stat s, Scope scope)
	{
		complaint	= null;											// of this statement's calls, not of the last one's
		try
		{
			if (s instanceof Local)					{ local ((Local) s, scope);			return; }
			if (s instanceof Assign)				{ assign ((Assign) s, scope);		return; }
			if (s instanceof CallStat)				{ eval (((CallStat) s).call, scope);	return; }
			if (s instanceof If)					{ ifStat ((If) s, scope);			return; }
			if (s instanceof While)					{ whileStat ((While) s, scope);		return; }
			if (s instanceof Repeat)				{ repeatStat ((Repeat) s, scope);	return; }
			if (s instanceof NumFor)				{ numFor ((NumFor) s, scope);		return; }
			if (s instanceof GenFor)				{ genFor ((GenFor) s, scope);		return; }
			if (s instanceof Do)					{ exec (((Do) s).body, new Scope (scope));	return; }
			if (s instanceof Return)
			{
				Return	r = (Return) s;

				throw new ReturnSignal ((r.values.size () == 1) ? eval (r.values.get (0), scope) : values (r.values, scope));
			}
			if (s instanceof Break)					throw new BreakSignal ();
			throw new LuaError (chunk, s.line, "cannot run a " + s.getClass ().getSimpleName ());
		}
		catch (LuaError e)		{ throw e; }
		catch (BreakSignal e)	{ throw e; }
		catch (ReturnSignal e)	{ throw e; }
		catch (RuntimeException e)
		{
			throw new LuaError (chunk, s.line, (e.getMessage () != null) ? e.getMessage () : e.toString ());
		}
	}

	private void local (Local s, Scope scope)
	{
		Object[]	vals = values (s.values, scope);
		boolean		anynil = false;

		for (int i = 0; i < s.names.size (); i++)
			anynil	|= (i >= vals.length) || (vals[i] == null);
		for (int i = 0; i < s.names.size (); i++)
		{
			Object	v = (i < vals.length) ? vals[i] : null;
			Cell	c = scope.declare (s.names.get (i), v);

			// a call of the statement complained: it is the nil it answered that is to
			// blame, or every local of the statement when none of them is nil
			if ((complaint != null) && ((v == null) || !anynil))	c.wrong = complaint;
			noted (s.names.get (i), c);
		}
	}

	private void assign (Assign s, Scope scope)
	{
		Object[]	vals = values (s.values, scope);

		for (int i = 0; i < s.targets.size (); i++)
			set (s.targets.get (i), (i < vals.length) ? vals[i] : null, scope);
	}

	protected void set (Expr target, Object value, Scope scope)
	{
		if (target instanceof Name)
		{
			String	n = ((Name) target).name;
			Cell	c = scope.find (n);

			if (c != null)						c.value = value;
			else								globals.set (n, value);
			return;
		}
		if (target instanceof Index)
		{
			Index	ix = (Index) target;

			Lua.setIndex (eval (ix.obj, scope), eval (ix.key, scope), value);
			return;
		}
		throw new LuaError (chunk, 0, "cannot assign to this");
	}

	private void ifStat (If s, Scope scope)
	{
		for (int i = 0; i < s.conds.size (); i++)
			if (Lua.truth (eval (s.conds.get (i), scope)))
			{
				exec (s.blocks.get (i), new Scope (scope));
				return;
			}
		if (s.orelse != null)					exec (s.orelse, new Scope (scope));
	}

	private void whileStat (While s, Scope scope)
	{
		try
		{
			while (Lua.truth (eval (s.cond, scope)))
				exec (s.body, new Scope (scope));
		}
		catch (BreakSignal e) { }
	}

	private void repeatStat (Repeat s, Scope scope)
	{
		try
		{
			do
			{
				Scope	inner = new Scope (scope);						// the condition sees the locals of the body

				exec (s.body, inner);
				if (Lua.truth (eval (s.cond, inner)))		break;
			} while (true);
		}
		catch (BreakSignal e) { }
	}

	private void numFor (NumFor s, Scope scope)
	{
		double		from = Lua.number (eval (s.from, scope), "'for' initial value");
		double		to = Lua.number (eval (s.to, scope), "'for' limit");
		double		step = (s.step != null) ? Lua.number (eval (s.step, scope), "'for' step") : 1.0;

		if (step == 0.0)						throw new LuaError (chunk, s.line, "'for' step is zero");
		try
		{
			for (double v = from; (step > 0) ? (v <= to) : (v >= to); v += step)
			{
				Scope	inner = new Scope (scope);

				inner.declare (s.var, Double.valueOf (v));
				exec (s.body, inner);
			}
		}
		catch (BreakSignal e) { }
	}

	private void genFor (GenFor s, Scope scope)
	{
		Object[]	init = values (s.values, scope);
		Object		f = (init.length > 0) ? init[0] : null;
		Object		state = (init.length > 1) ? init[1] : null;
		Object		control = (init.length > 2) ? init[2] : null;

		if (!(f instanceof LuaFunction))			throw new LuaError (chunk, s.line, "attempt to call a " + Lua.type (f) + " value");
		try
		{
			while (true)
			{
				Object		r = ((LuaFunction) f).call (new Object[] { state, control });
				Object[]	vals = (r instanceof Object[]) ? (Object[]) r : new Object[] { r };

				if ((vals.length == 0) || (vals[0] == null))		return;
				control	= vals[0];

				Scope		inner = new Scope (scope);

				for (int i = 0; i < s.names.size (); i++)
					inner.declare (s.names.get (i), (i < vals.length) ? vals[i] : null);
				exec (s.body, inner);
			}
		}
		catch (BreakSignal e) { }
	}

	/* ------------------------------------------------------------------ */
	/* Expressions                                                         */
	/* ------------------------------------------------------------------ */

	/**
	 * The values of a list of expressions: the last one spreads when it answers
	 * several (a call or ...), as Lua does.
	 */
	protected Object[] values (List<Expr> exprs, Scope scope)
	{
		List<Object>	out = new ArrayList<Object> ();

		for (int i = 0; i < exprs.size (); i++)
		{
			Object	v = eval (exprs.get (i), scope);

			if ((i == (exprs.size () - 1)) && (v instanceof Object[]))
				for (Object o : (Object[]) v)		out.add (o);
			else									out.add (first (v));
		}
		return out.toArray ();
	}

	public Object eval (Expr e, Scope scope)
	{
		if (e instanceof Const)						return ((Const) e).value;
		if (e instanceof Name)
		{
			String	n = ((Name) e).name;
			Cell	c = scope.find (n);

			return (c != null) ? c.value : globals.get (n);
		}
		if (e instanceof Index)
		{
			Index	ix = (Index) e;

			try { return Lua.index (first (eval (ix.obj, scope)), first (eval (ix.key, scope))); }
			catch (LuaError err) { throw new LuaError (chunk, e.line, err.getMessage ().replaceFirst ("^.*?: ", "")); }
		}
		if (e instanceof Bin)						return binary ((Bin) e, scope);
		if (e instanceof Un)						return unary ((Un) e, scope);
		if (e instanceof Call)						return call ((Call) e, scope);
		if (e instanceof Func)						return new Closure ((Func) e, scope);
		if (e instanceof Table)						return table ((Table) e, scope);
		if (e instanceof Vararg)
		{
			Cell	c = scope.find ("...");

			return (c != null) ? c.value : new Object[0];
		}
		throw new LuaError (chunk, e.line, "cannot evaluate a " + e.getClass ().getSimpleName ());
	}

	private Object binary (Bin e, Scope scope)
	{
		// and / or answer one of their sides, and do not run the other one
		if ("and".equals (e.op))
		{
			Object	a = first (eval (e.a, scope));

			return Lua.truth (a) ? first (eval (e.b, scope)) : a;
		}
		if ("or".equals (e.op))
		{
			Object	a = first (eval (e.a, scope));

			return Lua.truth (a) ? a : first (eval (e.b, scope));
		}

		Object		a = first (eval (e.a, scope));
		Object		b = first (eval (e.b, scope));

		try
		{
			switch (e.op)
			{
			case "+":		return Lua.add (a, b);
			case "-":		return Lua.sub (a, b);
			case "*":		return Lua.mul (a, b);
			case "/":		return Lua.div (a, b);
			case "%":		return Lua.mod (a, b);
			case "^":		return Lua.pow (a, b);
			case "..":		return Lua.concat (a, b);
			case "==":		return Boolean.valueOf (Lua.eq (a, b));
			case "~=":		return Boolean.valueOf (!Lua.eq (a, b));
			case "<":		return Boolean.valueOf (Lua.lt (a, b));
			case "<=":		return Boolean.valueOf (Lua.le (a, b));
			case ">":		return Boolean.valueOf (Lua.lt (b, a));
			case ">=":		return Boolean.valueOf (Lua.le (b, a));
			}
		}
		catch (LuaError err) { throw new LuaError (chunk, e.line, err.getMessage ().replaceFirst ("^.*?: ", "")); }
		throw new LuaError (chunk, e.line, "unknown operator '" + e.op + "'");
	}

	private Object unary (Un e, Scope scope)
	{
		Object		a = first (eval (e.a, scope));

		try
		{
			switch (e.op)
			{
			case "-":		return Lua.neg (a);
			case "not":		return Boolean.valueOf (!Lua.truth (a));
			case "#":		return Lua.len (a);
			}
		}
		catch (LuaError err) { throw new LuaError (chunk, e.line, err.getMessage ().replaceFirst ("^.*?: ", "")); }
		throw new LuaError (chunk, e.line, "unknown operator '" + e.op + "'");
	}

	private Object call (Call e, Scope scope)
	{
		Object		fn;
		Object[]	args;

		if (e.method != null)														// obj:m (...)
		{
			Object		self = first (eval (e.fn, scope));
			Object[]	rest = values (e.args, scope);

			fn		= Lua.index (self, e.method);
			args	= new Object[rest.length + 1];
			args[0]	= self;
			System.arraycopy (rest, 0, args, 1, rest.length);
		}
		else
		{
			fn		= first (eval (e.fn, scope));
			args	= values (e.args, scope);
		}

		if (!(fn instanceof LuaFunction))
			throw new LuaError (chunk, e.line, "attempt to call a " + Lua.type (fn) + " value" + named (e));

		try
		{
			Object		r = ((LuaFunction) fn).call (args);
			String		c = ((LuaFunction) fn).complained ();

			if (c != null)						complaint = c;
			return r;
		}
		catch (LuaError err)		{ throw err; }
		catch (ReturnSignal err)	{ throw err; }
		catch (RuntimeException err)
		{
			throw new LuaError (chunk, e.line, "in " + ((LuaFunction) fn).name () + ": "
								+ ((err.getMessage () != null) ? err.getMessage () : err.toString ()));
		}
	}

	/** How the script named what it tried to call, for the error message. */
	private String named (Call e)
	{
		if (e.method != null)					return " (method '" + e.method + "')";
		if (e.fn instanceof Name)				return " (global '" + ((Name) e.fn).name + "')";
		if ((e.fn instanceof Index) && (((Index) e.fn).key instanceof Const))
			return " (field '" + Lua.tostring (((Const) ((Index) e.fn).key).value) + "')";
		return "";
	}

	private Object table (Table e, Scope scope)
	{
		LuaTable	t = new LuaTable ();
		int			n = 0;

		for (int i = 0; i < e.values.size (); i++)
		{
			Expr	k = e.keys.get (i);
			Object	v = eval (e.values.get (i), scope);

			if (k != null)						t.set (first (eval (k, scope)), first (v));
			else if ((i == (e.values.size () - 1)) && (v instanceof Object[]))		// the last one spreads
				for (Object o : (Object[]) v)	t.set (++n, o);
			else								t.set (++n, first (v));
		}
		return t;
	}
}
