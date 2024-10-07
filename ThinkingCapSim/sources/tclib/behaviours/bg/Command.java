/* * (c) 1997-2001 Humberto Martinez */ package tclib.behaviours.bg;public class Command extends List{	public static final int			ASSIGN		= 0;	public static final int			IFTHEN		= 1;	public static final int			RULES		= 2;	public static final int			FRULE		= 3;	public static final int			FBKG		= 4;	public static final int			FSM			= 5;	public static final int			SHIFT		= 6;	public static final int			STATE		= 7;	public static final int			CALL		= 8;	public static final int			RETURN		= 9;	public static final int			IFELSE		= 10;	public static final int			PRINTF		= 12;	public static final int			HALT		= 13;		protected static int			serial		= 0;		protected int					type;	protected VSymbol				id;	protected Expresion				exp;	protected Function				func;	protected Command				block;	protected Command				other;	protected Command				parent;	protected String				uniq;	protected int					index;		/* Constructors */	public Command ()	{	}		/* Class methods */	public static String newid ()	{		serial ++;		return "fsm" + (new Integer (serial)).toString ();	}		/* Accessor methods */	public final int 			type () 			{ return type; }	public final VSymbol 		id () 				{ return id; }	public final VSymbol 		params () 			{ return id; }	public final Expresion 		exp () 				{ return exp; }	public final Command 		block () 			{ return block; }	public final Command 		other () 			{ return other; }	public final Command 		parent () 			{ return parent; }	public final String 		uniq () 			{ return uniq; }	public final int 			index () 			{ return index; }	/* Instance methods */	public Command assign (VSymbol id, Expresion exp)	{		this.type		= ASSIGN;		this.id			= id;		this.exp		= exp;				return this;	}	public Command printf (VSymbol id)	{		this.type		= PRINTF;		this.id			= id;				return this;	}	public Command halt ()	{		this.type		= HALT;				return this;	}	public Command ifthen (Expresion exp, Command block)	{		this.type		= IFTHEN;		this.exp		= exp;		this.block		= block;				return this;	}	public Command ifelse (Expresion exp, Command block1, Command block2)	{		this.type		= IFELSE;		this.exp		= exp;		this.block		= block1;		this.other		= block2;				return this;	}	public Command rules (Command block)	{		this.type		= RULES;		this.block		= block;				return this;	}	public Command frule (VSymbol id, Expresion exp)	{		this.type		= FRULE;		this.id			= id;		this.exp		= exp;				return this;	}	public Command fbkg (VSymbol id, Expresion exp)	{		this.type		= FBKG;		this.id			= id;		this.exp		= exp;				return this;	}	public Command fsm (String name, Command block)	{		this.type		= FSM;		this.uniq		= newid ();		this.name		= name;		this.block		= block; 		block.atable (this, name);				return this;	}	public Command shift (String name)	{		this.type		= SHIFT;		this.name		= name;				return this;	}	public Command call (String name, VSymbol params)	{		this.type		= CALL;		this.id			= params;		this.name		= name;				return this;	}	public Command retn (Expresion exp)	{		this.type		= RETURN;		this.exp		= exp;				return this;	}	public Command state (String name, Command block)	{		this.type		= STATE;		this.name		= name;		this.block		= block;		block.ashift (this);				return this;	}		private void atable (Command parent, String name)	{		Command			p;		int				n;				p = this;		n = 1;		while (p != null)		{			p.parent = parent;			if (p.name.equals (name))				p.index = 0;			else			{				p.index = n;				n ++;			}			p = (Command) p.next ();		}	}			private void ashift (Command parent)	{		Command			p;				p = this;		while (p != null)		{			p.parent = parent;			if (p.type == IFTHEN)				p.block.ashift (parent);			p = (Command) p.next ();		}	}		private String recurse (Command coms)	{		Command		cs;		String		str;			str 	= " ";		cs 	= coms;		while (cs != null)		{			str += cs.toString () + "\n";			cs = (Command) cs.next ();		}		return str;		}		public String toString ()	{		String		str = " ";		String		ps;		VSymbol		vs;				switch (type)		{		case ASSIGN:			str 	= id.name () + " = " + exp.toString () + ";";			break;		case PRINTF:			str 	= "printf (" + id.name () + ");\n";			break;		case HALT:			str 	= "halt;\n";			break;		case IFTHEN:			str 	= "if (" + exp.toString () + ")\n{\n" + recurse (block) + "}\n";			break;		case RULES:			str 	= "rules\n{\n" + recurse (block) + "}\n";			break;		case FRULE:			str 	= "if (" + exp.toString () + ")\t" + id.source () + " is " + id.set ().label () + ";\n";			break;		case FBKG:			str 	= "background (" + exp.toString () + ")\t" + id.source () + " is " + id.set ().label () + ";\n";			break;		case FSM:			str 	= "fsm start " + name + "\n{\n" + recurse (block) + "}\n";			break;		case SHIFT:			str 	= "shift " + name + ";";			break;		case STATE:			str 	= "state " + name + ":\n{\n" + recurse (block) + "}\n";			break;		case CALL:			vs 	= id;			ps	= " ";			while (vs != null)			{				ps += vs.name ();				if (vs.next () != null)					ps += ", ";				vs = (VSymbol) vs.next ();			}			str 	= name + " (" + ps + " )";			break;		case RETURN:			str 	= "return " + exp.toString () + ";";			break;		case IFELSE:			str 	= "if (" + exp.toString () + ")\n{\n" + recurse (block) + "}\n";			str 	+= "else\n{\n" + other.toString () + "}\n";			break;		default:			// Do nothing		}				return str;	}}