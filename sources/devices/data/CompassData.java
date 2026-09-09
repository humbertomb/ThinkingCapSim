/*
 * (c) 2000-2001 Juan Pedro Canovas
 * (c) 2004 Humberto Martinez
 */

package devices.data;

import java.io.*;

public class CompassData implements Serializable, LoggableData
{
	static public final int			DATALOG			= 4;
	
	protected double data[];
	
	public CompassData ()
	{
		data = new double[4];
		
		for (int i = 0; i < 4; i++)
			data[i]	= Double.NaN;
	}
	
	public void		setHeading (double heading)		{ data[0] = heading; }
	public void		setPitch (double pitch)			{ data[1] = pitch; }
	public void		setRoll (double roll)				{ data[2] = roll; }
	public void		setTemp (double temp)				{ data[3] = temp; }
	
	public double	getHeading ()					{ return data[0]; }
	public double	getPitch ()						{ return data[1]; }
	public double	getRoll ()						{ return data[2]; }
	public double	getTemp ()						{ return data[3]; }
	
	public double[]	toDatalog ()						{ return data; }
	
	public void fromDatalog (double[] datalog)
	{
		for (int i = 0; i < data.length; i++)
			data[i] = datalog[i];
	}
}