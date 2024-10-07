/*
 * (c) 2003 Humberto Martinez, Alessandro Saffiotti
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class LPOLine extends LPO implements Serializable
{
	public static final double			RADIUS			= 0.05;

	// Constructor
	public LPOLine (double x, double y, double alpha, String label, int source)
	{			
		super (x, y, alpha, label, source);
		
		color (WColor.GREEN);
	}

	// Instance methods
	public void draw (Model2D model, LPOView view)
	{
		double			xx, yy, aa;
		
		if (!active)	return;

		aa	= view.rotation + phi - alpha;
		xx 	= rho * Math.cos (aa);
		yy 	= rho * Math.sin (aa);
		
		if ((xx < view.min.x ()) || (xx > view.max.x ()) || (yy < view.min.y ()) || (yy > view.max.y ()))		return;
			
		if (label != null)
			model.addRawText (xx, yy, label, ColorTool.fromWColorToColor(WColor.BLUE));
		else
			model.addRawCircle (xx, yy, RADIUS, ColorTool.fromWColorToColor(color));
		model.addRawArrow (0, 0, rho, aa, ColorTool.fromWColorToColor(color));
	}
}


