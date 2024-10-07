/*
 * (c) 2003 Humberto Martinez
 */
 
package tcrob.umu.quaky2.lpo;

import java.io.*;

import tc.shared.lps.lpo.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class Net extends LPO implements Serializable
{
	// Object specific information
	protected double					width;					// Width of the docking area (m)

	// Constructor
	public Net (double width, String label, int source)
	{			
		super (0.0, 0.0, 0.0, label, source);
		
		this.width		= width;
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
			
		model.addRawTransRotLine (-width, 0 , width, 0, xx, yy, phi, ColorTool.fromWColorToColor(color));
		model.addRawTransRotLine (-width, 0, width, width, xx, yy, phi, ColorTool.fromWColorToColor(color));
		model.addRawTransRotLine (width, 0 , width, width, xx, yy, phi, ColorTool.fromWColorToColor(color));
		
		//model.addRawTransRotLine (-width, 0 , width, 0, xx, yy, phi, color);
		//model.addRawTransRotLine (-width, 0, width, width, xx, yy, phi, color);
		//model.addRawTransRotLine (width, 0 , width, width, xx, yy, phi, color);
	}
}


