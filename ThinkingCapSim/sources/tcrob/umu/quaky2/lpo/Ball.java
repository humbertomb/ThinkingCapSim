/*
 * (c) 2003 Humberto Martinez
 */
 
package tcrob.umu.quaky2.lpo;

import java.io.*;

import tc.shared.lps.lpo.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class Ball extends LPO implements Serializable
{
	// Object specific information
	protected double					radius;						// Radius of the ball (m)

	// Constructor
	public Ball (double radius, String label, int source)
	{			
		super (0.0, 0.0, 0.0, label, source);
		
		this.radius		= radius;
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
			model.addRawText (xx, yy, label, ColorTool.fromWColorToColor(color));
			//model.addRawText (xx, yy, label, color);
		
		model.addRawCircle (xx, yy, radius, Model2D.FILLED, ColorTool.fromWColorToColor(color));
		//model.addRawCircle (xx, yy, radius, Model2D.FILLED, color);
	}
}


