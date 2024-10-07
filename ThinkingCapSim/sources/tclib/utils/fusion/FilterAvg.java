/* ----------------------------------------
	(c) 2000 Humberto Martinez Barbera
   ---------------------------------------- */

package tclib.utils.fusion;

import java.util.*;
import java.io.*;

public class FilterAvg extends Filter
{
	public static final int		FILTERID		= 1;

	// Instance variables	
	protected int				n;							/* Filter memory size 				*/

	protected double[]			last;							/* Last sensor values				*/
	private int				where;						/* Position to update memory array	*/
	
	// Constructors
	public FilterAvg (int memory)
	{
		this.prepare ();
		this.create (memory);
	}
	
	public FilterAvg (String name)
	{
		this.prepare ();
		this.load (name);
	}
	
	// Accessors
	public final int 			memory ()			{ return n; }
	public final void 		memory (int n)			{ this.n = n; }
	
	// Instance methods 
	protected void prepare ()
	{ 
		type		= FilterAvg.FILTERID;
	}

	protected void load (String name)
	{ 
		Properties		props;
		File				file;
		FileInputStream	stream;
		
		props			= new Properties ();
		try 
		{
			file 			= new File (name);
			stream 		= new FileInputStream (file);
			props.load (stream);
			stream.close ();
		} catch (Exception e) { }

		// Read Filter parameters
		try { n 			= new Integer (props.getProperty ("MEMORY")).intValue (); } catch (Exception e) 		{ n		= 4; }

		// Create data structures
		create (n);
	}

	protected void create (int memory)
	{	
		n			= memory;
		last			= new double[n];
		initialize ();
	}

	public void save (String name)
	{
		Properties		props;
		File				file;
		FileOutputStream	stream;
		props			= new Properties ();
		
		props.put ("FILTER", 			new Integer (FILTERID).toString ());
		props.put ("MEMORY", 		new Integer (n).toString ());

		try 
		{
			file 			= new File (name);
			stream 		= new FileOutputStream (file);
			props.store (stream, HEADER);
			stream.close ();
		} catch (Exception e) { }
	}

	public void initialize ()
	{ 
		int			i;
		
		for (i = 0; i < n; i++)
			last[i] = 0.0;
		where = 0;
	}

	public double filter (double a, double b)
	{ 
		double		avg;
		int			i;
		
		last[where] = b;
		where = (where + 1) % n;
		
		avg = 0.0;
		for (i = 0; i < n; i++)
			avg += last[i];
		avg = avg / (double) n;
		
		return avg;
	}
}
