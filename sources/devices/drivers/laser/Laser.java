/**
 * Copyright: Copyright (c) 2002
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Humberto Martinez Barbera (humberto@um.es)
 * @version 1.0
 */

package devices.drivers.laser;

public abstract class Laser 
{
	private String				clase;
	private String				port;
	
	// Constructors
	public static Laser getLaser (String props) throws LaserException
	{
		Laser			laser;
		String			cname;
		String			param;
		Class<?>			clase;

		try
		{   	
			cname	= props.substring (0,props.indexOf("|"));
			param	= props.substring (props.indexOf("|")+1,props.length());
			clase	= Class.forName (cname);
        
			System.out.println ("Laser: initialising "+cname+" with "+param+".");
        
			laser		= (Laser) clase.getDeclaredConstructor ().newInstance ();
			laser.setType (cname);
			laser.setConnection (param);
			laser.initialise (param);
        
		} catch (Exception e) { throw new LaserException ("(getLaser) "+e.toString ()); }
		
		return laser;	
	}
	
	// Accessors
	public void			setConnection (String port)		{ this.port = port; }	
	public String		getConnection ()					{ return port; }
	public void			setType (String clase)			{ this.clase = clase; }
	public String		getType ()						{ return clase; }
	
	// Instance methods
	public abstract void initialise (String param) throws LaserException;	
	public abstract double[] getLaserData () throws LaserException;
	
	public void close(){}
}