/*
 * (c) 2003 Humberto Martinez, Alessandro Saffiotti
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import devices.pos.*;
import wucore.widgets.*;
import wucore.utils.color.*;

public class LPOPoint extends LPO implements Serializable
{
	public static final double		RADIUS			= 0.05;

	protected double					last_x;
	protected double					last_y;
	
	// Constructor
	public LPOPoint (double x, double y, double alpha, String label, int source)
	{			
		super (x, y, alpha, label, source);
		
		last_x		= Double.MAX_VALUE;
		last_y		= Double.MAX_VALUE;
		
		if (label != null)
			color (WColor.BLUE);
		else
			color (WColor.GREEN);
	}

	// Instance methods
	public void update (Position cur, Position point)
	{
		double		ll, aa;
		double		cx, cy;

		if ((point.x () == last_x) && (point.y () == last_y))				return;
		
		cx	= point.x () - cur.x ();
		cy	= point.y () - cur.y ();
		ll	= Math.sqrt (cx * cx + cy * cy);
		aa	= Math.atan2 (cy, cx);
		cx	= ll * Math.cos (aa - cur.alpha ());
		cy	= ll * Math.sin (aa - cur.alpha ());
		
		locate (cx, cy, 0.0);
		active (true);		
		
		last_x	= point.x ();
		last_y	= point.y ();
	}
		
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

		model.addRawCircle (xx, yy, RADIUS, ColorTool.fromWColorToColor(color));
	}
}


