/**
 * Created on 23-abr-2006
 *
 * @author Humberto Martinez Barbera
 */
package tclib.planning.sequence;

import java.io.*;
import java.util.*;

public class Sequence implements Serializable
{
	public String[] 					place;
	public String[]					action;

	public Sequence (int size)
	{
		place	= new String[size];
		action	= new String[size];
	}
	
	public Sequence (String source, String delimiter)
	{
		int				i, size;
		StringTokenizer	st;

		st		= new StringTokenizer (source, delimiter);
		size		= st.countTokens() / 2;
		place	= new String[size];
		action	= new String[size];

		for (i = 0; i < size; i++)
			set (i, st.nextToken(), st.nextToken());		
	}
	
	public int size ()
	{
		if ((place == null) || (action == null))		return 0;
		
		return Math.min (place.length, action.length);
	}
	
	public void set (int i, String p, String a)
	{
		place[i]		= p;
		action[i]	= a;
	}
	
	public String toString ()
	{
		int			i;
		String		str;
		
		str = "<";
		for (i = 0; i < place.length; i++)
		{
			if (i > 0)
				str += ", ";
			str += place[i]+"-"+action[i];
		}
		return str + ">";
	}
}
