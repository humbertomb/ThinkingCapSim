/*
 * Created on 11-nov-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package devices.drivers.laser;

import java.io.*;
import java.util.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LaserFile extends Laser 
{
	protected double[][]					data;		// Raw laser data
	protected int						cdata;		// Current scan
	protected int						ndata;		// Maximum number of scans
	
	// Constructors
	public LaserFile ()
	{
		super ();
	}
	
	// Instance methods
	public void initialise (String param) throws LaserException
	{
		int				i, j, n;
		double[]			scan;
		BufferedReader	reader;
		StringTokenizer	st;
		
		cdata	= -1;
		try
		{
			// Read number of available scans
			reader	= new BufferedReader (new FileReader (getConnection ()));			
			for (n = 0; reader.ready (); n++)
				reader.readLine ();
			reader.close ();
			ndata	= n;
			
			// Read and store scans
			data		= new double[ndata][];
			reader	= new BufferedReader (new FileReader (getConnection ()));	
			for (i = 0; i < ndata; i++)
			{
				st		= new StringTokenizer (reader.readLine (), " ");
				n		= Integer.parseInt (st.nextToken ());
				scan		= new double[n];
				data[i]	= scan;

				for (j = 0; j < n; j++)
					scan[j]	= Double.parseDouble (st.nextToken ());
			}
			
			reader.close ();
		} catch (Exception e) { e.printStackTrace (); }
	}
	
	public double[] getLaserData () throws LaserException
	{
		cdata ++;
		if (cdata >= ndata)
			cdata = 0;
		
		return data[cdata];
	}
}
