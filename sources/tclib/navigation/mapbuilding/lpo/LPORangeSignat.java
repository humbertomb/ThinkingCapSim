/*
 * (c) 2003 Humberto Martinez
 */
 
package tclib.navigation.mapbuilding.lpo;

import java.io.*;
//import java.awt.*;

import tc.shared.lps.lpo.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class LPORangeSignat extends LPORange implements Serializable
{
	public static final double			RADIUS			= 0.05;

	// Constructor
	public LPORangeSignat (String label, int source)
	{			
		super (null, label, source);
		
		locate (0.0, 0.0, 0.0, 0.0);
		color (WColor.ORANGE);
		active (true);
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
			model.addRawText (xx + RADIUS, yy + RADIUS, label, ColorTool.fromWColorToColor(color));
		//	model.addRawText (xx + RADIUS, yy + RADIUS, label, color);
		
		model.addRawCircle (xx, yy, RADIUS, ColorTool.fromWColorToColor(color));
	}
}


