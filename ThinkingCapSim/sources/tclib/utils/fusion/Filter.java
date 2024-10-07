/* ----------------------------------------
	(c) 2000 Humberto Martinez Barbera
   ---------------------------------------- */

package tclib.utils.fusion;

import java.util.*;
import java.io.*;

public class Filter extends Object
{	
	public static final String		SUFFIX		= ".filter";
	public static final String		HEADER		= "Sensor Filter Preferences";	
	public static final int			FILTERID		= 0;
	
	public int						type;

	// Constructors
	public Filter ()
	{
		this.prepare ();
	}
	
	public Filter (String name)
	{
		this.prepare ();
		this.load (name);
	}
	
	// Class methods 
	public static Filter fromFile (String name)
	{ 
		Properties		props;
		File			file;
		FileInputStream	stream;
		int				type;
		Filter			filter	= null;
		
		props			= new Properties ();
		try 
		{
			file 			= new File (name);
			stream 		= new FileInputStream (file);
			props.load (stream);
			stream.close ();
		} catch (Exception e) { }

		// Read Filter Type
		try { type 			= new Integer (props.getProperty ("FILTER")).intValue (); } catch (Exception e) 		{ type		= FILTERID; }

		switch (type)
		{
			case FilterAvg.FILTERID:
				filter = new FilterAvg (name);
				break;
			case FilterANN.FILTERID:
				filter = new FilterANN (name);
				break;
			case FilterFuzzy.FILTERID:
				filter = new FilterFuzzy (name);
				break;
			case Filter.FILTERID:
			default:
				filter = new Filter (name);
		}		
		return filter;
	}

	// Accessors
	public final int 			type ()			{ return type; }

	// Instance methods 
	protected void prepare ()
	{ 
		type		= Filter.FILTERID;
	}

	protected void load (String name)
	{ 
	}

	public void initialize ()
	{ 
	}

	public void save (String name)
	{
		FilterAvg			favg;
		FilterANN			fann;
		FilterFuzzy		ffuz;
		
		Properties		props;
		File				file;
		FileOutputStream	stream;
		props			= new Properties ();
		
		switch (type)
		{
			case FilterAvg.FILTERID:
				favg		= (FilterAvg) this;
				favg.save (name);
				break;
			case FilterANN.FILTERID:
				fann		= (FilterANN) this;
				fann.save (name);
				break;
			case FilterFuzzy.FILTERID:
				ffuz		= (FilterFuzzy) this;
				ffuz.save (name);
				break;
			case Filter.FILTERID:
			default:
				props.put ("FILTER", 			new Integer (FILTERID).toString ());

				try 
				{
					file 			= new File (name);
					stream 		= new FileOutputStream (file);
					props.store (stream, HEADER);
					stream.close ();
				} catch (Exception e) { }
		}		

	}

	public double filter (double a, double b)
	{ 
		return b;
	}
}
