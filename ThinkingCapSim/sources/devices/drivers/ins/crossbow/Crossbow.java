
/***************************************************************************** necesito saber la trama crossbow *********/

/**
 * Title: Crossbow
 * Description: Clase para acceder a un compas Crossbow
 * Copyright: Copyright (c) 2003
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Canovas Qui–onero, Rafael Toledo Moreo
 * @author Humberto Martinez Barbera
 * @version 1.0
 */

package devices.drivers.ins.crossbow;

import javax.comm.*;
import java.io.*;

import devices.data.*;
import devices.drivers.ins.*;

import wucore.utils.math.*;

public class Crossbow extends Ins
{
	static public final byte			RQST0	= (byte) 0x53;		// serial number
	static public final byte			RQST1	= (byte) 0x52;		// reset
	static public final byte			RQST2	= (byte) 0x61;		// angle mode
	static public final byte			RQST5	= (byte) 0x47;		// get data packet
	static public final byte			RQST6	= (byte) 0x7A;		// zeroing
	static public final byte			RQST7	= (byte) 0x54;		// erection rate

	static public final double		G		= - 9.81; 			// -9.81 standard value  // -9.7867 emprirical value.
	static public final double		RR		= 100.0;				// Maximum rotation rate [deg/s]
	static public final double		GR		= 2.0;				// Maximum gravity acceleration factor [G].

	protected SerialPort				serial;
	protected InputStream				is;
	protected OutputStream			os;
	
	protected double[]				values	= new double[10];	
	protected InsData				data		= new InsData ();
	private boolean					valid;

	public Crossbow ()
	{
		super ();
	}

	protected void initialise () throws CrossbowException
	{
		initSerial ();
		initDevice ();
	}

	public void close () throws CrossbowException
	{
		try
		{
			is.close ();
			os.close ();
			serial.close ();
		} catch (Exception e) { throw new CrossbowException (e.getMessage()); }
	}

	private void initSerial () throws CrossbowException
	{
		CommPortIdentifier id;
			
		try {
			id = CommPortIdentifier.getPortIdentifier (port); 
			if (id.getPortType() != CommPortIdentifier.PORT_SERIAL) {System.out.println("Error in serial port");}
			serial = (SerialPort) id.open ("DMU-VG",1000);
			serial.setSerialPortParams(38400,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
			serial.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			
			is = serial.getInputStream();
			os = serial.getOutputStream();
			
			System.out.println("tranfer serial port: "+port);
			System.out.println("----------------------------------------");	
		}
		catch (Exception e) { e.printStackTrace (); }
	}

	private void initDevice () throws CrossbowException
	{
		int	  i;
		int   unbyte;
		byte  answ1;
		
		try
		{
			
			// serial number
			String serialnumber = ("");
			String checksumSerialnumber = ("");	
			int entero[] = new int[6];
			int suma = 0;
			os.write(RQST0);
			for (i = 0; i < 6; i++) {
				entero[i] = is.read();
				serialnumber = (serialnumber + entero[i]);
			}
			// checksum serial number
			for (i = 1; i < 5; i++) {suma = suma + entero[i];}
			suma = suma%256;
			checksumSerialnumber = serialnumber.substring(11);
			serialnumber = serialnumber.substring(3,11);
			System.out.println("Serial number: " + serialnumber);
			if (String.valueOf(suma).equals(checksumSerialnumber)) {System.out.println("checking serial number... OK");}
			else {System.out.println("checking serial number... Serial number Error");}
			System.out.println("----------------------------------------");	
			
			// conection request
			os.write(RQST1);
			unbyte	= is.read();
			answ1	= (byte)unbyte;
			if(answ1 == 72) { System.out.println("INS Crossbow in port.");}
			else {System.out.println("No INS Crossbow in port.");}
			System.out.println("----------------------------------------");	
			
			// transfer request							
			os.write(RQST2);														
			unbyte	= is.read();
			answ1	= (byte)unbyte;
			if(answ1 == 65) { System.out.println("Transfer Request granted.");}
			else {System.out.println("Transfer Request rejected.");}
			System.out.println("----------------------------------------");	
		}
		catch (IOException ioe) { ioe.printStackTrace (); }
		
		calibration();
	}
  
	protected void calibration ()
	{
		int unint;
		   	
		try
		{
			// set to zero rate sensors outputs
			System.out.print("zeroing... ");
			os.write(RQST6);
			os.write(2000);	// samples for zeroing (nº of samples = 10*value)
			 
			unint = is.read();
			if(unint == 0x5A) { System.out.println("OK");}
			else {System.out.println("Error");}
			System.out.println("----------------------------------------");	
			
			// erection rate
			System.out.println("Setting erection rate. ");
			System.out.println("----------------------------------------");	
			os.write(RQST7);
			os.write(20);	// empirical value
			/*
			adjusment of this empirical value: after zeroing:
			Take the worst rate gyro offset and multiply it by 120.
			If the DMU is going to work for long periods without
			zeroing it's recommended to increase this value.
			*/
		}
		catch (IOException ioe) { ioe.printStackTrace (); }
	}

  	protected void dataRequest () throws CrossbowException
	{
  		int			i, i1, a, unint;
  		int			buffInt[] = new int[22];
  		short		buffShort[] = new short[10];

  		// -------------------------
  		// Transfer data from device
  		// -------------------------
		try
		{
			os.write(RQST5);
			for(i = 0; i<22; i++)
			{
				unint = is.read();			
				buffInt[i] = unint;
			}
			
			for(i = 0, i1 = 0; i1 <10; i++, i1++)
			{
				i++;			
				a = buffInt[i];
				a = a << 8;
				a = buffInt[i+1] | a;
				buffShort[i1] = (short)a;
			}
		
			// checksum
			int suma = 0;
			int chcksm;
			for (i = 1; i < 21; i ++)
				suma = suma + buffInt[i];
		  	suma		= suma%256;
			chcksm	= (int)buffInt[21];
			valid	= (suma == chcksm);
		}
		catch (Exception e) { throw new CrossbowException (e.toString ()); }
	
		// ----------------------------
		// Perform data type convertion
		// ----------------------------
	
		// roll, pitch
		values[0] = buffShort[0] * 180/(Math.pow(2,15));
		values[1] = buffShort[1] * 180/(Math.pow(2,15));
		
		// rate
		values[2] = buffShort[2] * RR * 1.5/(Math.pow(2,15));
		values[3] = buffShort[3] * RR * 1.5/(Math.pow(2,15));
		values[4] = buffShort[4] * RR * 1.5/(Math.pow(2,15));
	
		// acceleration
		values[5] = buffShort[5] * GR * 1.5/(Math.pow(2,15)) / G;
		values[6] = buffShort[6] * GR * 1.5/(Math.pow(2,15)) / G;
		values[7] = buffShort[7] * GR * 1.5/(Math.pow(2,15)) / G;
	
		// temperature
		values[8] = (((double)buffShort[8] * 5/4096 )- 1.375) * 44.44;
	}
  
  	public InsData getData ()
  	{
  		try
		{
	  		dataRequest ();
	  		
			data.setRoll (values[0] * Angles.DTOR);
			data.setPitch (values[1] * Angles.DTOR);
			data.setRollRate (values[2] * Angles.DTOR);
			data.setPitchRate (values[3] * Angles.DTOR);
			data.setYawRate (values[4] * Angles.DTOR);
			data.setAccX (values[5]);
			data.setAccY (values[6]);
			data.setAccZ (values[7]);
		}
  		catch (Exception e) { e.printStackTrace (); }
		
		return data;
  	}
}