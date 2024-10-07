/*
 * (c) 2003 Humberto Martinez, Alessandro Saffiotti
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class LPODock extends LPO implements Serializable
{
	// Object specific information
	protected double					width;					// Width of the docking area (m)
	protected boolean				occupied;				// Does the dock contains a load?

	// Constructor
	public LPODock (double x, double y, double alpha, double width, String label, int source)
	{			
		super (x, y, alpha, label, source);
		
		color (WColor.BLUE);
		
		this.width	= width;
		occupied		= false;
	}

	// Accessors
	public boolean			occupied ()						{ return occupied; }
	public void				occupied (boolean occupied)		{ this.occupied = occupied; }
	
	// Instance methods
	public void draw (Model2D model, LPOView view)
	{
		double			xx, yy, aa;
		
		if (!active)	return;

		aa	= view.rotation + phi;
		xx 	= rho * Math.cos (aa);
		yy 	= rho * Math.sin (aa);
		
		if ((xx < view.min.x ()) || (xx > view.max.x ()) || (yy < view.min.y ()) || (yy > view.max.y ()))		return;
			
		model.addRawText (xx, yy, label, ColorTool.fromWColorToColor(color));
		model.addRawTransRotLine (-width, 0 , width, 0, xx, yy, phi, ColorTool.fromWColorToColor(color));
		model.addRawTransRotLine (-width, 0, width, width, xx, yy, phi, ColorTool.fromWColorToColor(color));
		model.addRawTransRotLine (width, 0 , width, width, xx, yy, phi, ColorTool.fromWColorToColor(color));

		if (occupied)
			model.addRawCircle (xx, yy, width, ColorTool.fromWColorToColor(color));
	}
}


