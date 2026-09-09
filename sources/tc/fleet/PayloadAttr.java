/*
 * (c) 2004 Humberto Martinez
 */
 
package tc.fleet;

public class PayloadAttr
{
	public String					name;			// Payload description
	public String					unit;			// Payload measurement unit
	    
	/* Constructors */
	public PayloadAttr ()
	{
	}
	
	/* Accessor methods */
	public void set (String name, String unit)
	{
		this.name	= name;
		this.unit	= unit;
	}
	
	public String toString ()
	{
		return "<" + name + ", " + unit + ">";
	}
} 