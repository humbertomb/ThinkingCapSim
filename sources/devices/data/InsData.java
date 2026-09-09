/*
 * (c) 2003 Juan Pedro Canovas, Rafael Toledo Moreo
 * (c) 2004,2006 Humberto Martinez
 */

package devices.data;

import java.io.*;

public class InsData implements Serializable, LoggableData
{
	static public final int			DATALOG			= 13;
	
	protected double data[];
	
	public InsData ()
	{
		data = new double[DATALOG];
		
		for (int i = 0; i < DATALOG; i++)
			data[i]	= Double.NaN;
	}
	
	public void		setRoll (double roll)					{ data[0] = roll; }
	public void		setPitch (double pitch)				{ data[1] = pitch; }
	public void		setYaw (double yaw)					{ data[2] = yaw; }
	public void		setRollRate (double rollRate)			{ data[3] = rollRate; }
	public void		setPitchRate (double pitchRate)		{ data[4] = pitchRate; }
	public void		setYawRate (double yawRate)			{ data[5] = yawRate; }
	public void		setAccX (double accx)					{ data[6] = accx; }
	public void		setAccY (double accy)					{ data[7] = accy; }
	public void		setAccZ (double accz)					{ data[8] = accz; }
	public void		setMagX (double magx)					{ data[9] = magx; }
	public void		setMagY (double magy)					{ data[10] = magy; }
	public void		setMagZ (double magz)					{ data[11] = magz; }
	public void		setTemp (double temp)					{ data[12] = temp; }
	
	public double	getRoll ()							{ return data[0]; }
	public double	getPitch ()							{ return data[1]; }
	public double	getYaw ()							{ return data[2]; }
	public double	getRollRate ()						{ return data[3]; }
	public double	getPitchRate ()						{ return data[4]; }
	public double	getYawRate ()						{ return data[5]; }
	public double	getAccX ()							{ return data[6]; }
	public double	getAccY ()							{ return data[7]; }
	public double	getAccZ ()							{ return data[8]; }
	public double	getMagX ()							{ return data[9]; }
	public double	getMagY ()							{ return data[10]; }
	public double	getMagZ ()							{ return data[11]; }
	public double	getTemp ()							{ return data[12]; }
	
	public double[]	toDatalog ()							{ return data; }

	public void fromDatalog (double[] datalog)
	{
		for (int i = 0; i < data.length; i++)
			data[i] = datalog[i];
	}
}