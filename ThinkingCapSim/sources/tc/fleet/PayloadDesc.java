/*
 * (c) 2004 Humberto Martinez
 */

package tc.fleet;

import java.util.*;
import java.io.*;

public class PayloadDesc extends Object implements Serializable
{
	// Payload parameters
	public int				MAXPAYLOAD;				// Number of payload attributes

	public PayloadAttr[]		attrs;					// Payload attributes
	
	/* Constructors */
	public PayloadDesc (String name)
	{
		Properties		props;
		File			file;
		FileInputStream	stream;

		props			= new Properties ();
		try
		{
			file 		= new File (name);
			stream 		= new FileInputStream (file);
			props.load (stream);
			stream.close ();
		} catch (Exception e) { e.printStackTrace (); }

		update (props);
	}

	public PayloadDesc (Properties props)
	{
		update (props);
	}

	/* Instance methods */
	public void update (Properties props)
	{
		int					i;
		StringTokenizer		st;
		String				prop;
		String				name, unit;
		boolean				modified = true;

		// Load payload properties
		try { MAXPAYLOAD 	= new Integer (props.getProperty ("MAXPAYLOAD")).intValue (); } 	catch (Exception e) 	{ modified = false; }

		// TODO the new properties MUST include all the attributes. Could be done better
		if ((MAXPAYLOAD != 0) && modified)
		{
			// Initialise data arrays
			attrs		= new PayloadAttr[MAXPAYLOAD];
			
			// Load data properties
			for (i = 0; i < MAXPAYLOAD; i++)
			{
				prop		= props.getProperty ("PAYLOAD" + i);
				st		= new StringTokenizer (prop,", \t");
				name		= st.nextToken ();
				unit		= st.nextToken ();
						
				attrs[i]	= new PayloadAttr ();
				attrs[i].set (name, unit);
			}
		}
	}

	public String toString ()
	{
		int				i;
		String			str;
		
		str		= "PLDS=" + MAXPAYLOAD + "\n";
		for (i = 0; i < MAXPAYLOAD; i++)
			str += "\t" + attrs[i] + "\n";
		
		return str;
	}
}

