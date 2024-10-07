/*
 * (c) 2002 Humberto Martinez
 */
 
package wucore.widgets.input;

public interface InputDevice 
{
	// Constants definition
	static public final int				BUTTON1		= 1;
	static public final int				BUTTON2		= 2;
	static public final int				BUTTON3		= 4;
	static public final int				BUTTON4		= 8;
	static public final int				BUTTON5		= 16;
	static public final int				BUTTON6		= 32;
	static public final int				BUTTON7		= 64;
	static public final int				BUTTON8		= 128;

	// Instance methods
	public double 		getXPos ();
	public double 		getYPos (); 
	public double 		getZPos (); 
	public int 			getButtons (); 
	public String 		toString (); 
}
