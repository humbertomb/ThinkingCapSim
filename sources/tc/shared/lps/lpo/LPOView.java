/*
 * (c) 2003 Humberto Martinez, Alessandro Saffiotti
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import wucore.utils.math.*;
import wucore.utils.geom.*;

public class LPOView extends Object implements Serializable
{
	// Rotation of LPS objects
	public static final double			VERTICAL		= Angles.PI05;		
	public static final double			HORIZONTAL		= 0.0;

	public double						rotation;									// Orientation of LPS objects
	public Point3						min;										// Minimum boundary of LPS view
	public Point3						max;										// Maximum boundary of LPS view
	public boolean						verbose;									// Do more graphical output

	// Constructor
	public LPOView ()
	{			
		rotation	= VERTICAL;
    	verbose		= false;
		
    	min			= new Point3 ();
    	max			= new Point3 ();	
	}
}


