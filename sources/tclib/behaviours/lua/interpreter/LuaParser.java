/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.interpreter;

import java.util.List;

import tclib.behaviours.lua.interpreter.LuaAst.*;
import tclib.behaviours.lua.interpreter.LuaLexer.Token;

/**
 * Reads a Lua script into the tree of {@link LuaAst}: a recursive descent
 * parser of the whole language but for what a script of a state machine never
 * needs (goto and labels, and metatables, which are a matter of the runtime).
 */
public class LuaParser
{
	protected List<Token>			tks;
	protected int					at;
	protected String				chunk;

	public LuaParser (String src, String chunk)
	{
		this.chunk	= chunk;
		this.tks	= new LuaLexer (src, chunk).tokens ();
		this.at		= 0;
	}

	/** Reads a whole script. */
	static public Block parse (String src, String chunk)
	{
		LuaParser		p = new LuaParser (src, chunk);
		Block			b = p.block ();

		if (p.peek ().kind != LuaLexer.EOF)
			throw p.error ("'<eof>' expected near '" + p.peek ().text + "'");
		return b;
	}

	/* ---------------- tokens ---------------- */

	protected Token peek ()						{ return tks.get (at); }
	protected Token peek (int n)				{ return tks.get (Math.min (at + n, tks.size () - 1)); }
	protected Token take ()						{ return tks.get (at++); }

	protected boolean opt (String t)
	{
		if (peek ().is (t))						{ at++; return true; }
		return false;
	}

	protected Token expect (String t)
	{
		if (!peek ().is (t))					throw error ("'" + t + "' expected near '" + peek ().text + "'");
		return take ();
	}

	protected String name ()
	{
		if (peek ().kind != LuaLexer.NAME)		throw error ("<name> expected near '" + peek ().text + "'");
		return take ().text;
	}

	protected LuaError error (String msg)		{ return new LuaError (chunk, peek ().line, msg); }

	private <T> T at (T node, Token t)
	{
		if (node instanceof Expr)				((Expr) node).line = t.line;
		else if (node instanceof Stat)			((Stat) node).line = t.line;
		return node;
	}

	/* ---------------- statements ---------------- */

	/** A run of statements, up to whatever closes the block (end, else, until, eof). */
	public Block block ()
	{
		Block		b = new Block ();

		while (true)
		{
			Token	t = peek ();

			if ((t.kind == LuaLexer.EOF) || t.is ("end") || t.is ("else") || t.is ("elseif") || t.is ("until"))
				return b;

			if (opt (";"))						continue;
			if (t.is ("return"))
			{
				b.stats.add (returnStat ());
				opt (";");
				return b;															// return closes its block
			}
			b.stats.add (statement ());
		}
	}

	protected Stat statement ()
	{
		Token		t = peek ();

		if (t.is ("if"))					return ifStat ();
		if (t.is ("while"))					return whileStat ();
		if (t.is ("repeat"))				return repeatStat ();
		if (t.is ("for"))					return forStat ();
		if (t.is ("do"))					{ take (); Do d = at (new Do (block ()), t); expect ("end"); return d; }
		if (t.is ("local"))					return localStat ();
		if (t.is ("function"))				return functionStat ();
		if (t.is ("break"))					{ take (); return at (new Break (), t); }
		if (t.is ("goto"))					{ take (); name (); return at (new Do (new Block ()), t); }		// ignored
		if (t.is ("::"))					{ take (); name (); expect ("::"); return at (new Do (new Block ()), t); }
		return exprStat ();
	}

	protected Stat returnStat ()
	{
		Token		t = expect ("return");
		Return		r = at (new Return (), t);

		if (!blockEnd () && !peek ().is (";"))
			explist (r.values);
		return r;
	}

	private boolean blockEnd ()
	{
		Token		t = peek ();

		return (t.kind == LuaLexer.EOF) || t.is ("end") || t.is ("else") || t.is ("elseif") || t.is ("until");
	}

	protected Stat ifStat ()
	{
		Token		t = expect ("if");
		If			s = at (new If (), t);

		s.conds.add (expr ());
		expect ("then");
		s.blocks.add (block ());
		while (peek ().is ("elseif"))
		{
			take ();
			s.conds.add (expr ());
			expect ("then");
			s.blocks.add (block ());
		}
		if (opt ("else"))					s.orelse = block ();
		expect ("end");
		return s;
	}

	protected Stat whileStat ()
	{
		Token		t = expect ("while");
		While		s = at (new While (), t);

		s.cond	= expr ();
		expect ("do");
		s.body	= block ();
		expect ("end");
		return s;
	}

	protected Stat repeatStat ()
	{
		Token		t = expect ("repeat");
		Repeat		s = at (new Repeat (), t);

		s.body	= block ();
		expect ("until");
		s.cond	= expr ();
		return s;
	}

	protected Stat forStat ()
	{
		Token		t = expect ("for");
		String		first = name ();

		if (opt ("="))																// for v = a, b [, c]
		{
			NumFor	s = at (new NumFor (), t);

			s.var	= first;
			s.from	= expr ();
			expect (",");
			s.to	= expr ();
			if (opt (","))					s.step = expr ();
			expect ("do");
			s.body	= block ();
			expect ("end");
			return s;
		}

		GenFor		s = at (new GenFor (), t);										// for a, b in ...

		s.names.add (first);
		while (opt (","))					s.names.add (name ());
		expect ("in");
		explist (s.values);
		expect ("do");
		s.body	= block ();
		expect ("end");
		return s;
	}

	protected Stat localStat ()
	{
		Token		t = expect ("local");

		if (peek ().is ("function"))												// local function f () ... end
		{
			take ();

			String	n = name ();
			Local	s = at (new Local (), t);
			Func	f = funcBody (n);

			s.names.add (n);
			s.values.add (f);
			return s;
		}

		Local		s = at (new Local (), t);

		s.names.add (name ());
		while (opt (","))					s.names.add (name ());
		if (opt ("="))						explist (s.values);
		return s;
	}

	/** <code>function a.b.c:d () ... end</code>, which is an assignment of a function. */
	protected Stat functionStat ()
	{
		Token		t = expect ("function");
		Assign		s = at (new Assign (), t);
		StringBuffer	fname = new StringBuffer (name ());
		Expr		target = at (new Name (fname.toString ()), t);
		boolean		method = false;

		while (peek ().is (".") || peek ().is (":"))
		{
			method	= peek ().is (":");
			take ();

			String	f = name ();

			fname.append (method ? ":" : ".").append (f);
			target	= at (new Index (target, at (new Const (f), t)), t);
			if (method)						break;
		}

		Func		f = funcBody (fname.toString ());

		if (method)							f.params.add (0, "self");
		s.targets.add (target);
		s.values.add (f);
		return s;
	}

	/** The parameters and the body of a function, the cursor on its '('. */
	protected Func funcBody (String name)
	{
		Token		t = peek ();
		Func		f = at (new Func (), t);

		f.name	= name;
		expect ("(");
		if (!peek ().is (")"))
			do
			{
				if (peek ().is ("..."))		{ take (); f.varargs = true; break; }
				f.params.add (name ());
			} while (opt (","));
		expect (")");
		f.body	= block ();
		expect ("end");
		return f;
	}

	/** A call or an assignment, which both start with a suffixed expression. */
	protected Stat exprStat ()
	{
		Token		t = peek ();
		Expr		e = suffixed ();

		if (peek ().is ("=") || peek ().is (","))
		{
			Assign	s = at (new Assign (), t);

			s.targets.add (e);
			while (opt (","))				s.targets.add (suffixed ());
			expect ("=");
			explist (s.values);
			for (Expr target : s.targets)
				if (!(target instanceof Name) && !(target instanceof Index))
					throw error ("syntax error near '" + peek ().text + "'");
			return s;
		}

		if (!(e instanceof Call))			throw error ("syntax error near '" + peek ().text + "'");
		return at (new CallStat ((Call) e), t);
	}

	protected void explist (List<Expr> out)
	{
		do
			out.add (expr ());
		while (opt (","));
	}

	/* ---------------- expressions ---------------- */

	/* Binary operators, by how tightly they bind (left, right); 'or' loosest. */
	static private String[][]		LEVELS = {
		{ "or" }, { "and" }, { "<", ">", "<=", ">=", "~=", "==" }, { ".." }, { "+", "-" }, { "*", "/", "%" } };

	public Expr expr ()						{ return binary (0); }

	private Expr binary (int level)
	{
		if (level >= LEVELS.length)			return unary ();

		Expr		e = binary (level + 1);

		while (true)
		{
			Token	t = peek ();
			String	op = null;

			for (String cand : LEVELS[level])
				if (t.is (cand))			{ op = cand; break; }
			if (op == null)					return e;
			take ();

			Expr	rhs = "..".equals (op) ? binary (level) : binary (level + 1);	// .. is right associative

			e	= at (new Bin (op, e, rhs), t);
			if ("..".equals (op))			return e;
		}
	}

	private Expr unary ()
	{
		Token		t = peek ();

		if (t.is ("not") || t.is ("-") || t.is ("#"))
		{
			take ();
			return at (new Un (t.text, unary ()), t);
		}
		return power ();
	}

	/** <code>^</code> binds tighter than the unary operators and is right associative. */
	private Expr power ()
	{
		Token		t = peek ();
		Expr		e = simple ();

		if (peek ().is ("^"))
		{
			take ();
			return at (new Bin ("^", e, unary ()), t);
		}
		return e;
	}

	private Expr simple ()
	{
		Token		t = peek ();

		switch (t.kind)
		{
		case LuaLexer.NUMBER:		take ();	return at (new Const (Double.valueOf (t.number)), t);
		case LuaLexer.STRING:		take ();	return at (new Const (t.text), t);
		}
		if (t.is ("nil"))			{ take ();	return at (new Const (null), t); }
		if (t.is ("true"))			{ take ();	return at (new Const (Boolean.TRUE), t); }
		if (t.is ("false"))			{ take ();	return at (new Const (Boolean.FALSE), t); }
		if (t.is ("..."))			{ take ();	return at (new Vararg (), t); }
		if (t.is ("{"))							return table ();
		if (t.is ("function"))		{ take ();	return funcBody ("?"); }
		return suffixed ();
	}

	/** A name or a parenthesized expression, with any number of calls, fields and indexes after it. */
	protected Expr suffixed ()
	{
		Token		t = peek ();
		Expr		e;

		if (opt ("("))
		{
			e	= expr ();
			expect (")");
		}
		else								e = at (new Name (name ()), t);

		while (true)
		{
			Token	s = peek ();

			if (opt ("."))					e = at (new Index (e, at (new Const (name ()), s)), s);
			else if (opt ("["))				{ e = at (new Index (e, expr ()), s); expect ("]"); }
			else if (peek ().is (":"))
			{
				take ();

				String	m = name ();
				Call	c = at (new Call (), s);

				c.fn		= e;
				c.method	= m;
				args (c);
				e	= c;
			}
			else if (peek ().is ("(") || peek ().is ("{") || (peek ().kind == LuaLexer.STRING))
			{
				Call	c = at (new Call (), s);

				c.fn	= e;
				args (c);
				e	= c;
			}
			else							return e;
		}
	}

	/** The arguments of a call: (a, b), a string, or a table. */
	protected void args (Call c)
	{
		Token		t = peek ();

		if (t.kind == LuaLexer.STRING)		{ take (); c.args.add (at (new Const (t.text), t)); return; }
		if (t.is ("{"))						{ c.args.add (table ()); return; }
		expect ("(");
		if (!peek ().is (")"))				explist (c.args);
		expect (")");
	}

	protected Expr table ()
	{
		Token		t = expect ("{");
		Table		tb = at (new Table (), t);

		while (!peek ().is ("}"))
		{
			Token	s = peek ();

			if (opt ("["))															// [k] = v
			{
				Expr	k = expr ();

				expect ("]");
				expect ("=");
				tb.keys.add (k);
				tb.values.add (expr ());
			}
			else if ((s.kind == LuaLexer.NAME) && peek (1).is ("="))				// k = v
			{
				take ();
				take ();
				tb.keys.add (at (new Const (s.text), s));
				tb.values.add (expr ());
			}
			else																	// v
			{
				tb.keys.add (null);
				tb.values.add (expr ());
			}
			if (!opt (",") && !opt (";"))	break;
		}
		expect ("}");
		return tb;
	}
}
