/*
 * (c) 2003 Humberto Martinez, Alessandro Saffiotti
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class LPOArrow extends LPORange implements Serializable
{
	// Constructor
	public LPOArrow (double x, double y, double alpha, String label, int source)
	{			
		super (null, label, source);
		
		locate (x, y, alpha, 0.0);
		color (WColor.GREEN.darker());
	}

	// Instance methods
	public void draw (Model2D model, LPOView view)
	{
		double			xx, yy, aa;
		
		if (!active)	return;

		aa	= view.rotation + phi;
		xx 	= rho * Math.cos (aa);
		yy 	= rho * Math.sin (aa);
		
		if ((xx < view.min.x ()) || (xx > view.max.x ()) || (yy < view.min.y ()) || (yy > view.max.y ()))		return;
			
		if (label != null)
			model.addRawText (xx, yy, label, ColorTool.fromWColorToColor(WColor.CYAN));
		
		model.addRawArrow (xx, yy, len, aa, ColorTool.fromWColorToColor(color));
		//model.addRawArrow (xx, yy, len, aa, color);
	}
}


