/* ----------------------------------------
	(c) 2000 Humberto Martinez Barbera
   ---------------------------------------- */

package tclib.utils.fusion;

import java.util.*;
import java.io.*;

import tclib.behaviours.bg.interpreter.*;

public class FilterSFF extends Filter
{
	public static final int		FILTERID	= 4;
	public static final int		MAXVARS		= 10;

	// Instance variables	
	protected int				n;							/* Filter memory size 				*/

	protected double[]			last;						/* Last sensor values				*/
	private int					where;						/* Position to update memory array	*/
	protected RuleInterpreter	fuzzy;
	
	// Constructors
	public FilterSFF (String name)
	{
		this.prepare ();
		this.load (name);
	}
	
	// Accessors
	public final int 			memory ()			{ return n; }
	public final void 			memory (int n)			{ this.n = n; }
	
	// Instance methods 
	protected void prepare ()
	{ 
		type		= FilterSFF.FILTERID;
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
		create (n, name);
	}

	protected void create (int memory, String name)
	{	
		n			= memory;
		last		= new double[n];
		fuzzy		= new RuleInterpreter (name);
		
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
		double		avg, diff, alpha, output;
		int			i;
		
		// Equation I
		avg 		= 0.0;
		for (i = 0; i < n; i++)
			avg 		+= last[i];
		avg 		= avg / (double) n;
		
		// Equation II
		diff		= Math.abs (avg - b);
		
		// Equation III
		alpha 	= fuzzy.evaluate (diff);
		output 	= alpha * b + (1.0 - alpha) * avg;
		
		last[where] = output;
		where = (where + 1) % n;
		
		return output;
	}
}
