/*
 * Created on 27-jul-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tclib.navigation.localisation.outdoor;

import devices.pos.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface Filter 
{
	public void			initialise (Variables vars);
	public void			step ();
	public Position		getPosition ();
	public Pose			getPose ();
	public int			getCounter ();
	public double[]		datalog ();
}
