/**
 * Description: Clase generica para un sistema INS
 * Copyright: 2003-2004
 * @author Humberto Martinez Barbera, Rafael Toledo Moreo
 * @version 1.0
 */

package devices.drivers.ins;

import devices.data.*;

public abstract class Ins extends Object
{
	// Factory and connection variables
	protected String					clase;
	protected String 				port;

	// ------------------------------
	// CLASS LOADING (FACTORIES)
	// ------------------------------
	public static Ins getIns (String prop) throws InsException
    {
		Ins	 			tg = null;
		String 			cl;
		String 			prt;

		try
		{
			// Parse parameters and create a device instance
			cl		= prop.substring (0, prop.indexOf("|"));
			prt		= prop.substring (prop.indexOf("|")+1, prop.length());
			Class insclass = Class.forName(cl);
			tg		= (Ins) insclass.newInstance();
			
 			System.out.println ("INS: connecting "+cl+" to port "+prt+".");
 			
 			// Initialise device
 			tg.setType (cl);
			tg.setPort (prt);
			tg.initialise ();
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
		return tg;
    }

	// Accessors
	public final void			setType (String _clase)		{ clase = _clase; }
	public final void 			setPort (String _port)		{ port = _port; }
	public final String			getType ()					{ return clase; }
	public final String			getPort ()					{ return port; }
    
    // Abstract instance methods (subclasses MUST implement)
	protected abstract void		initialise () throws InsException;
	public abstract void			close () throws InsException;
	public abstract InsData		getData ();
}