/******************************************************************
*
*	Copyright (C) Satoshi Konno 1999
*	Copyright (C) Humberto Martinez 2002
*
*	File : Joystick.java
*
******************************************************************/

package wucore.widgets.input;

public class Joystick implements InputDevice
{
	static 
	{
		try { System.loadLibrary ("joystick"); } 
		catch (Exception e) 
		{ 
			System.out.print ("ERROR: ");
			e.printStackTrace (); 
		}
	}		
	
	private int							joyID		= -1;

	// Constructors
	public Joystick () 
	{
		joyID = getNumDevs ();
	}

	// Native methods
	public native int		getNumDevs ();
	public native float		getXPos (int id);
	public native float		getYPos (int id);
	public native float		getZPos (int id);
	public native int		getButtons (int id);

	// Accessors
	public boolean			isAvailable ()			{ return (joyID >= 0); }
	
	// Instance methods
	public double getXPos () 
	{
		return (double) getXPos (joyID);
	}
	
	public double getYPos () 
	{
		return (double) getYPos (joyID);
	}
	
	public double getZPos () 
	{
		return (double) getZPos (joyID);
	}
	
	public int getButtons () 
	{
		return getButtons (joyID);
	}
	
	public String toString () 
	{
		return "Joystick";
	}
}
