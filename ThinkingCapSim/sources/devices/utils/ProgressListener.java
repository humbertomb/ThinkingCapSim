/*
 * Created on 18-ene-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package devices.utils;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface ProgressListener
{
	public void setTotalSteps (int nsteps);
	public void incrementSteps (int steps);
}
