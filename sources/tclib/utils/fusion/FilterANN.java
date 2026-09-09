/* ----------------------------------------
	(c) 2000 Humberto Martinez Barbera
   ---------------------------------------- */

package tclib.utils.fusion;

import java.util.*;
import java.io.*;

public class FilterANN extends Filter		// TODO: re-include ANN into ThinkingCap-II (original code in BGen)
{
	public static final int		FILTERID		= 2;

	// Instance variables	
//	protected ANNetwork			ann;	
	private double[]			input;

	// Constructors
	public FilterANN (String name)
	{
		this.prepare ();
		this.load (name);
	}
	
	// Accessors
//	public final ANNetwork 	ann ()				{ return ann; }
	
	// Instance methods 
	protected void prepare ()
	{ 
		type		= FilterANN.FILTERID;
		input	= new double[2];
	}

	protected void load (String name)
	{ 
//		ann		= new ANNetwork (name);
	}

	public void save (String name)
	{
		Properties		props;
		File				file;
		FileOutputStream	ostream;
		FileInputStream	istream;
		
		// Save ANN Preferences
//		ann.save (name);
		
		// Merge data
		props			= new Properties ();
		try 
		{
			file 			= new File (name);
			istream 		= new FileInputStream (file);
			props.load (istream);
			istream.close ();
		} catch (Exception e) { }
		
		props.put ("FILTER", 			new Integer (FILTERID).toString ());

		// Save Filter Preferences
		try 
		{
			file 			= new File (name);
			ostream 		= new FileOutputStream (file);
			props.store (ostream, HEADER);
			ostream.close ();
		} catch (Exception e) { }
	}

	public double filter (double a, double b)
	{ 
//		double[]		output;
		
//		if (ann == null)		return 0.0;
		
		input[0] 	= a;
		input[1] 	= b;
//		output	= ann.feedforward (input);

//		return output[0];
		return 0.0;
	}
}
