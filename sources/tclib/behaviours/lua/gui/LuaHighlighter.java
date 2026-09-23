/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.lua.gui;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Colours Lua as it is typed: the keywords, the strings, the numbers, the
 * comments, the library a behaviour speaks through (<code>chaos</code>,
 * <code>math</code>, <code>io</code>) and the names of the functions that are
 * called.
 *
 * It reads the text the way the lexer of the interpreter does, but forgivingly:
 * a string or a comment that is not finished yet -- which is what half-typed
 * code looks like -- is coloured to the end of the text instead of being an
 * error.
 */
public class LuaHighlighter
{
	/* The colours */
	static public final Color		C_PLAIN		= new Color (30, 30, 30);
	static public final Color		C_KEYWORD	= new Color (0, 0, 190);
	static public final Color		C_STRING	= new Color (150, 60, 0);
	static public final Color		C_NUMBER	= new Color (150, 0, 130);
	static public final Color		C_COMMENT	= new Color (0, 130, 0);
	static public final Color		C_LIBRARY	= new Color (140, 80, 0);
	static public final Color		C_CALL		= new Color (0, 100, 140);

	static private final Set<String>	KEYWORDS = new HashSet<String> (Arrays.asList (
		"and", "break", "do", "else", "elseif", "end", "false", "for", "function", "goto", "if", "in",
		"local", "nil", "not", "or", "repeat", "return", "then", "true", "until", "while"));

	/** What a behaviour of the robot speaks to the world through. */
	static private final Set<String>	LIBRARIES = new HashSet<String> (Arrays.asList (
		"chaos", "math", "io", "string", "table", "os"));

	protected AttributeSet			plain, keyword, string, number, comment, library, call;

	public LuaHighlighter ()
	{
		plain	= style (C_PLAIN, false);
		keyword	= style (C_KEYWORD, true);
		string	= style (C_STRING, false);
		number	= style (C_NUMBER, false);
		comment	= style (C_COMMENT, false);
		library	= style (C_LIBRARY, true);
		call	= style (C_CALL, false);
	}

	static private AttributeSet style (Color c, boolean bold)
	{
		SimpleAttributeSet	a = new SimpleAttributeSet ();

		StyleConstants.setForeground (a, c);
		StyleConstants.setBold (a, bold);
		return a;
	}

	/** Colours the whole document. */
	public void apply (StyledDocument doc, String text)
	{
		if ((doc == null) || (text == null))		return;

		doc.setCharacterAttributes (0, Math.max (1, text.length ()), plain, true);

		int			i = 0;
		int			n = text.length ();

		while (i < n)
		{
			char	c = text.charAt (i);

			/* ---- comments ---- */
			if ((c == '-') && next (text, i, '-'))
			{
				int		level = longLevel (text, i + 2);
				int		end = (level >= 0) ? longEnd (text, i + 2, level) : line (text, i);

				doc.setCharacterAttributes (i, end - i, comment, true);
				i	= end;
				continue;
			}

			/* ---- long strings ---- */
			if (c == '[')
			{
				int		level = longLevel (text, i);

				if (level >= 0)
				{
					int	end = longEnd (text, i, level);

					doc.setCharacterAttributes (i, end - i, string, true);
					i	= end;
					continue;
				}
			}

			/* ---- strings ---- */
			if ((c == '"') || (c == '\''))
			{
				int		end = quoted (text, i, c);

				doc.setCharacterAttributes (i, end - i, string, true);
				i	= end;
				continue;
			}

			/* ---- numbers ---- */
			if (Character.isDigit (c) || ((c == '.') && (i + 1 < n) && Character.isDigit (text.charAt (i + 1))))
			{
				int		end = i;

				while ((end < n) && (Character.isLetterOrDigit (text.charAt (end)) || (text.charAt (end) == '.')
									 || (((text.charAt (end) == '+') || (text.charAt (end) == '-'))
										 && (end > i) && ((text.charAt (end - 1) == 'e') || (text.charAt (end - 1) == 'E')))))
					end++;
				doc.setCharacterAttributes (i, end - i, number, true);
				i	= end;
				continue;
			}

			/* ---- names ---- */
			if (Character.isLetter (c) || (c == '_'))
			{
				int		end = i;

				while ((end < n) && (Character.isLetterOrDigit (text.charAt (end)) || (text.charAt (end) == '_')))
					end++;

				String	word = text.substring (i, end);

				if (KEYWORDS.contains (word))
					doc.setCharacterAttributes (i, end - i, keyword, true);
				else if (LIBRARIES.contains (word))
					doc.setCharacterAttributes (i, end - i, library, true);
				else if (called (text, end))
					doc.setCharacterAttributes (i, end - i, call, true);
				i	= end;
				continue;
			}
			i++;
		}
	}

	/* ---------------- reading ---------------- */

	static private boolean next (String s, int i, char c)
	{
		return ((i + 1) < s.length ()) && (s.charAt (i + 1) == c);
	}

	/** Where the line that starts at <code>i</code> ends (the break included). */
	static private int line (String s, int i)
	{
		int			j = s.indexOf ('\n', i);

		return (j < 0) ? s.length () : (j + 1);
	}

	/** The level of a long bracket at <code>i</code> ([[ is 0, [=[ is 1 ...), or -1. */
	static private int longLevel (String s, int i)
	{
		if ((i >= s.length ()) || (s.charAt (i) != '['))			return -1;

		int			j = i + 1;

		while ((j < s.length ()) && (s.charAt (j) == '='))		j++;
		if ((j >= s.length ()) || (s.charAt (j) != '['))		return -1;
		return j - i - 1;
	}

	/** Where a long bracket ends, or the end of the text when it is not finished. */
	static private int longEnd (String s, int i, int level)
	{
		String		close = "]" + "=".repeat (level) + "]";
		int			j = s.indexOf (close, i + level + 2);

		return (j < 0) ? s.length () : (j + close.length ());
	}

	/** Where a quoted string ends, or the end of the line when it is not finished. */
	static private int quoted (String s, int i, char quote)
	{
		int			j = i + 1;

		while (j < s.length ())
		{
			char	c = s.charAt (j);

			if (c == '\\')						{ j += 2;	continue; }
			if (c == '\n')						return j;
			j++;
			if (c == quote)						return j;
		}
		return s.length ();
	}

	/** Whether what comes after a name makes it a call: a bracket, a string or a table. */
	static private boolean called (String s, int i)
	{
		while ((i < s.length ()) && ((s.charAt (i) == ' ') || (s.charAt (i) == '\t')))		i++;
		if (i >= s.length ())					return false;

		char		c = s.charAt (i);

		return (c == '(') || (c == '{') || (c == '"') || (c == '\'');
	}
}
