/*
 * (c) 2004 Humberto Martinez
 */
 
package tc.fleet;

import java.io.*;

public class PayloadData extends Object implements Serializable 
{
	// Vehicle data
	public double[]				data;					// Raw payload data
	
	/* Constructors */
	public PayloadData ()
	{
	}
	
	public PayloadData (int n)
	{
		data	= new double[n];
	}
	
	// Instance methods
	public void set (PayloadData payload)
	{
		this.data	= payload.data;
	}

	public void set (double[] data)
	{
		this.data	= data;
	}
} 