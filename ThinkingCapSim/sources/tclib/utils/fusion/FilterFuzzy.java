/* ----------------------------------------
	(c) 2000 Humberto Martinez Barbera
   ---------------------------------------- */

package tclib.utils.fusion;

import java.util.*;
import java.io.*;

import tclib.behaviours.bg.interpreter.*;

public class FilterFuzzy extends Filter
{
	public static final int		FILTERID		= 3;

	// Instance variables	
	protected RuleInterpreter	fuzzy;
	private double[]			input;
	
	// Constructors
	public FilterFuzzy (String name)
	{
		this.prepare ();
		this.load (name);
	}
	
	// Accessors
	public final RuleInterpreter 	fuzzy ()				{ return fuzzy; }
	
	// Instance methods 
	protected void prepare ()
	{ 
		type			= FilterFuzzy.FILTERID;
		input		= new double[2];
	}

	protected void load (String name)
	{ 
//		fuzzy		= new RuleInterpreter (name);
	}

	public void save (String name)
	{
		Properties		props;
		File				file;
		FileOutputStream	ostream;
		FileInputStream	istream;
		
		// Save Fuzzy Preferences
//		fuzzy.save (name);
		
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
		double[]		output;
		
		if (fuzzy == null)		return 0.0;
		
		input[0] 	= a;
		input[1] 	= b;
		output 	= fuzzy.evaluate (input);
		
		return  output[0];
	}
}
