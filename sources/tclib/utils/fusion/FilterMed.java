/* ----------------------------------------
	(c) 2000 Humberto Martinez Barbera
   ---------------------------------------- */

package tclib.utils.fusion;

import java.util.*;
import java.io.*;

public class FilterMed extends Filter
{
	public static final int		FILTERID		= 4;

	// Instance variables	
	protected int				n;							/* Filter memory size 				*/

	protected double[]			last;							/* Last sensor values				*/
	protected double[]			temp;						/* Temporal copy of sensor values		*/
	private int				where;						/* Position to update memory array	*/
	
	// Constructors
	public FilterMed (int memory)
	{
		this.prepare ();
		this.create (memory);
	}
	
	public FilterMed (String name)
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
		type		= FilterMed.FILTERID;
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
		temp		= new double[n];
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
		double		med, tmp;
		int			i, j;
		
		last[where] = b;
		where = (where + 1) % n;
		
        		System.arraycopy (last, 0, temp, 0, n);
		for (i = 0; i < n; i++)
			for (j = i + 1; j < n; j++)
				if (temp[j] < temp[i])
				{
					tmp		= temp[j];
					temp[j]	= temp[i];
					temp[i]	= tmp;
				}
				
		med = temp[n / 2];
		if (n % 2 == 0)
			med = (med + temp[n / 2]) / 2.0;
		
		return med;
	}
}
