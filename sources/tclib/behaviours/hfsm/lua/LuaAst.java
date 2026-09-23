/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.lua;

import java.util.ArrayList;
import java.util.List;

/**
 * The tree a Lua script is read into: what {@link LuaParser} builds and
 * {@link LuaInterp} walks. Every node keeps the line it came from, so that an
 * error names it.
 */
public class LuaAst
{
	/* ------------------------------------------------------------------ */
	/* Expressions                                                         */
	/* ------------------------------------------------------------------ */

	static public abstract class Expr
	{
		public int					line;
	}

	/** A literal: a number, a string, a boolean or nil. */
	static public class Const extends Expr
	{
		public Object				value;

		public Const (Object v)					{ value = v; }
	}

	/** A name: a local one when the block declares it, a field of the globals otherwise. */
	static public class Name extends Expr
	{
		public String				name;

		public Name (String n)					{ name = n; }
	}

	/** <code>obj[key]</code>, which is also what <code>obj.key</code> is. */
	static public class Index extends Expr
	{
		public Expr					obj;
		public Expr					key;

		public Index (Expr o, Expr k)			{ obj = o; key = k; }
	}

	/** <code>fn(args)</code>, or <code>obj:method(args)</code> when a method is named. */
	static public class Call extends Expr
	{
		public Expr					fn;
		public String				method;
		public List<Expr>			args = new ArrayList<Expr> ();
	}

	/** A function, as written in the script. */
	static public class Func extends Expr
	{
		public List<String>			params = new ArrayList<String> ();
		public boolean				varargs;
		public Block				body;
		public String				name = "?";
	}

	/** <code>a op b</code>. */
	static public class Bin extends Expr
	{
		public String				op;
		public Expr					a;
		public Expr					b;

		public Bin (String op, Expr a, Expr b)	{ this.op = op; this.a = a; this.b = b; }
	}

	/** <code>op a</code>: <code>-</code>, <code>not</code> or <code>#</code>. */
	static public class Un extends Expr
	{
		public String				op;
		public Expr					a;

		public Un (String op, Expr a)			{ this.op = op; this.a = a; }
	}

	/** <code>...</code> */
	static public class Vararg extends Expr
	{
	}

	/** <code>{ 1, 2, x = 3, [k] = v }</code>: the key is null for the ones that go in the array part. */
	static public class Table extends Expr
	{
		public List<Expr>			keys = new ArrayList<Expr> ();
		public List<Expr>			values = new ArrayList<Expr> ();
	}

	/* ------------------------------------------------------------------ */
	/* Statements                                                          */
	/* ------------------------------------------------------------------ */

	static public abstract class Stat
	{
		public int					line;
	}

	static public class Block
	{
		public List<Stat>			stats = new ArrayList<Stat> ();
	}

	static public class Local extends Stat
	{
		public List<String>			names = new ArrayList<String> ();
		public List<Expr>			values = new ArrayList<Expr> ();
	}

	static public class Assign extends Stat
	{
		public List<Expr>			targets = new ArrayList<Expr> ();
		public List<Expr>			values = new ArrayList<Expr> ();
	}

	static public class CallStat extends Stat
	{
		public Call					call;

		public CallStat (Call c)				{ call = c; }
	}

	static public class If extends Stat
	{
		public List<Expr>			conds = new ArrayList<Expr> ();
		public List<Block>			blocks = new ArrayList<Block> ();
		public Block				orelse;
	}

	static public class While extends Stat
	{
		public Expr					cond;
		public Block				body;
	}

	static public class Repeat extends Stat
	{
		public Block				body;
		public Expr					cond;
	}

	/** <code>for v = from, to, step do ... end</code> */
	static public class NumFor extends Stat
	{
		public String				var;
		public Expr					from;
		public Expr					to;
		public Expr					step;
		public Block				body;
	}

	/** <code>for a, b in explist do ... end</code> */
	static public class GenFor extends Stat
	{
		public List<String>			names = new ArrayList<String> ();
		public List<Expr>			values = new ArrayList<Expr> ();
		public Block				body;
	}

	static public class Do extends Stat
	{
		public Block				body;

		public Do (Block b)						{ body = b; }
	}

	static public class Return extends Stat
	{
		public List<Expr>			values = new ArrayList<Expr> ();
	}

	static public class Break extends Stat
	{
	}
}
