/*
 * ComandoMinidome.java Minidome command class
 *
 * @author Bernardo Cánovas Segura
 */

package devices.drivers.camera.minidome;

import devices.drivers.camera.*;

public class MinidomeCommand extends Comando
{
	/** Bytecode that will be sent for this command */
	protected byte[] bytecode;
	/** Number of param elements that accept */
	protected int expectedParamLength;
	
	// Constructors
	/** Complete constructor */
	public MinidomeCommand (String group,String order,byte[] bytecode,String description, int plength)
	{
		super(group,order,description);
		this.bytecode = bytecode;
		this.expectedParamLength = plength;	
	}

	/** Constructor for no argument's commands */ 
	public MinidomeCommand (String group,String order,byte[] bytecode,String description)
	{
		super(group,order,description);
		this.bytecode = bytecode;
		this.expectedParamLength = 0;
	}
	
	// Instance methods 
	/** Generates a byte array to transmit the command (with no arguments) to the camera */
	public byte[] com()
	{
		return this.com((byte[])null);		
	}

	/** Generates a byte array to transmit the command (with one argument) to the camera */
	public byte[] com(byte param)
	{
		return this.com(new byte[] { param });
	}

	/** Generates a byte array to transmit the command to the camera */
	public byte[] com(byte[] params)
	{
		byte[] chain;
		int checksum=0;
		int chainIndex=0;
		if (params==null)
			chain= new byte[bytecode.length+1];
		else
			chain= new byte[bytecode.length+params.length+1];
		for (int i=0;i<bytecode.length;i++)
		{
			chain[chainIndex]=bytecode[i];
			checksum=checksum+bytecode[i];
			chainIndex++;
		}
		if (params!=null)
		{
			for (int i=0;i<params.length;i++)
			{
				chain[chainIndex]=params[i];
				checksum=checksum+params[i];
				chainIndex++;
			}
		}
		chain[chainIndex]=(byte)(checksum%256);
		return chain;
	}

	public int getParamLength()
	{
		return this.expectedParamLength;
	}
}