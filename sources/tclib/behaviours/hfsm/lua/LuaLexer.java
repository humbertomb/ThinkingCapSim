/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.lua;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cuts a Lua script into tokens: names, keywords, numbers, strings and symbols,
 * leaving out the comments (<code>--</code> to the end of the line, and the long
 * <code>--[[ ... ]]</code> ones).
 */
public class LuaLexer
{
	/* Kinds of token */
	static public final int			EOF			= 0;
	static public final int			NAME		= 1;
	static public final int			NUMBER		= 2;
	static public final int			STRING		= 3;
	static public final int			KEYWORD		= 4;
	static public final int			SYMBOL		= 5;

	static private final Set<String>	KEYWORDS = new HashSet<String> (Arrays.asList (
		"and", "break", "do", "else", "elseif", "end", "false", "for", "function", "goto", "if", "in",
		"local", "nil", "not", "or", "repeat", "return", "then", "true", "until", "while"));

	/* Symbols, longest first so that ".." is not read as "." "." */
	static private final String[]	SYMBOLS = {
		"...", "..", "==", "~=", "<=", ">=", "::", "+", "-", "*", "/", "%", "^", "#", "<", ">", "=",
		"(", ")", "{", "}", "[", "]", ";", ":", ",", "." };

	/** One token: what it is, its text, the number it holds (NUMBER) and its line. */
	static public class Token
	{
		public int					kind;
		public String				text;
		public double				number;
		public int					line;

		Token (int kind, String text, double number, int line)
		{
			this.kind	= kind;
			this.text	= text;
			this.number	= number;
			this.line	= line;
		}

		public boolean is (String t)			{ return ((kind == KEYWORD) || (kind == SYMBOL)) && text.equals (t); }
		public String toString ()				{ return text + " (" + kind + ") @" + line; }
	}

	protected String				src;
	protected String				chunk;
	protected int					pos;
	protected int					line;

	public LuaLexer (String src, String chunk)
	{
		this.src	= (src != null) ? src : "";
		this.chunk	= chunk;
		this.pos	= 0;
		this.line	= 1;
	}

	/** Every token of the script, the last one being EOF. */
	public List<Token> tokens ()
	{
		List<Token>		out = new ArrayList<Token> ();
		Token			t;

		do
		{
			t	= next ();
			out.add (t);
		} while (t.kind != EOF);
		return out;
	}

	protected LuaError error (String msg)		{ return new LuaError (chunk, line, msg); }

	private char at (int i)						{ return ((pos + i) < src.length ()) ? src.charAt (pos + i) : '\0'; }
	private boolean name0 (char c)				{ return Character.isLetter (c) || (c == '_'); }
	private boolean namec (char c)				{ return Character.isLetterOrDigit (c) || (c == '_'); }

	/** The next token, the white space and the comments before it left behind. */
	public Token next ()
	{
		skip ();
		if (pos >= src.length ())				return new Token (EOF, "<eof>", 0.0, line);

		char		c = src.charAt (pos);

		if (name0 (c))							return word ();
		if (Character.isDigit (c) || ((c == '.') && Character.isDigit (at (1))))		return number ();
		if ((c == '"') || (c == '\''))			return string (c);
		if ((c == '[') && ((at (1) == '[') || (at (1) == '=')))
		{
			int		level = longLevel ();
			if (level >= 0)						return longString (level);
		}
		return symbol ();
	}

	/** Leaves white space and comments behind. */
	private void skip ()
	{
		while (pos < src.length ())
		{
			char	c = src.charAt (pos);

			if (c == '\n')						{ line++; pos++; }
			else if (Character.isWhitespace (c))	pos++;
			else if ((c == '-') && (at (1) == '-'))
			{
				pos	+= 2;
				int		level = longLevel ();
				if (level >= 0)					longText (level);					// --[[ ... ]]
				else							while ((pos < src.length ()) && (src.charAt (pos) != '\n'))		pos++;
			}
			else								return;
		}
	}

	/** The level of a long bracket at the cursor ([[ is 0, [=[ is 1 ...), or -1 when there is none. */
	private int longLevel ()
	{
		if (at (0) != '[')						return -1;

		int		i = 1;

		while (at (i) == '=')					i++;
		if (at (i) != '[')						return -1;
		return i - 1;
	}

	/** The text of a long bracket, the cursor on its opening one. */
	private String longText (int level)
	{
		pos	+= level + 2;															// [==[
		if (at (0) == '\n')						{ line++; pos++; }					// a first line break is dropped
		StringBuffer	sb = new StringBuffer ();
		String			close = "]" + "=".repeat (level) + "]";

		while (true)
		{
			if (pos >= src.length ())			throw error ("unfinished long string or comment");
			if (src.startsWith (close, pos))	{ pos += close.length (); return sb.toString (); }
			if (src.charAt (pos) == '\n')		line++;
			sb.append (src.charAt (pos++));
		}
	}

	private Token longString (int level)
	{
		int		l = line;

		return new Token (STRING, longText (level), 0.0, l);
	}

	private Token word ()
	{
		int		start = pos;

		while ((pos < src.length ()) && namec (src.charAt (pos)))		pos++;

		String	w = src.substring (start, pos);

		return new Token (KEYWORDS.contains (w) ? KEYWORD : NAME, w, 0.0, line);
	}

	private Token number ()
	{
		int		start = pos;

		if ((at (0) == '0') && ((at (1) == 'x') || (at (1) == 'X')))						// hexadecimal
		{
			pos	+= 2;
			while ((pos < src.length ()) && (Character.digit (src.charAt (pos), 16) >= 0))		pos++;
			return new Token (NUMBER, src.substring (start, pos), Long.parseLong (src.substring (start + 2, pos), 16), line);
		}

		while ((pos < src.length ()) && Character.isDigit (src.charAt (pos)))			pos++;
		if (at (0) == '.')
		{
			pos++;
			while ((pos < src.length ()) && Character.isDigit (src.charAt (pos)))		pos++;
		}
		if ((at (0) == 'e') || (at (0) == 'E'))
		{
			pos++;
			if ((at (0) == '+') || (at (0) == '-'))		pos++;
			while ((pos < src.length ()) && Character.isDigit (src.charAt (pos)))		pos++;
		}

		String	text = src.substring (start, pos);

		try { return new Token (NUMBER, text, Double.parseDouble (text), line); }
		catch (NumberFormatException e) { throw error ("malformed number near '" + text + "'"); }
	}

	private Token string (char quote)
	{
		StringBuffer	sb = new StringBuffer ();
		int				l = line;

		pos++;
		while (true)
		{
			if (pos >= src.length ())			throw error ("unfinished string");

			char	c = src.charAt (pos++);

			if (c == quote)						break;
			if (c == '\n')						throw error ("unfinished string");
			if (c != '\\')						{ sb.append (c); continue; }

			char	e = (pos < src.length ()) ? src.charAt (pos++) : '\0';

			switch (e)
			{
			case 'n':		sb.append ('\n');	break;
			case 't':		sb.append ('\t');	break;
			case 'r':		sb.append ('\r');	break;
			case 'a':		sb.append ('\007');	break;
			case 'b':		sb.append ('\b');	break;
			case 'f':		sb.append ('\f');	break;
			case 'v':		sb.append ('\013');	break;
			case '\\':		sb.append ('\\');	break;
			case '"':		sb.append ('"');	break;
			case '\'':		sb.append ('\'');	break;
			case '\n':		sb.append ('\n');	line++;		break;
			default:
				if (Character.isDigit (e))											// \ddd
				{
					int		v = e - '0', n = 1;
					while ((n < 3) && Character.isDigit (at (0)))	{ v = v * 10 + (src.charAt (pos++) - '0'); n++; }
					sb.append ((char) v);
				}
				else					sb.append (e);
			}
		}
		return new Token (STRING, sb.toString (), 0.0, l);
	}

	private Token symbol ()
	{
		for (String s : SYMBOLS)
			if (src.startsWith (s, pos))
			{
				pos	+= s.length ();
				return new Token (SYMBOL, s, 0.0, line);
			}
		throw error ("unexpected symbol near '" + src.charAt (pos) + "'");
	}
}
