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
	static public final double			DEPTH		= 0.1;		// How deep it is drawn (m)

	// Object specific information
	protected double					width;					// Half the width of the net (m)

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

		aa	= view.rotation + theta;
		xx 	= rho * Math.cos (aa);
		yy 	= rho * Math.sin (aa);
		
		if ((xx < view.min.x ()) || (xx > view.max.x ()) || (yy < view.min.y ()) || (yy > view.max.y ()))		return;
			
		if (label != null)
			model.addRawText (xx, yy, label, ColorTool.fromWColorToColor(color));

		// the net: a filled rectangle across the line of sight, from where it stands on the floor backwards
		double[]		px = new double[4], py = new double[4];
		double[]		lx = { 0.0, 0.0, DEPTH, DEPTH };			// along the line of sight
		double[]		ly = { -width, width, width, -width };		// across it
		for (int i = 0; i < 4; i++)
		{
			px[i]	= xx + lx[i] * Math.cos (aa) - ly[i] * Math.sin (aa);
			py[i]	= yy + lx[i] * Math.sin (aa) + ly[i] * Math.cos (aa);
		}
		model.addRawPoly (px, py, Model2D.FILLED, ColorTool.fromWColorToColor(color));
	}
}


