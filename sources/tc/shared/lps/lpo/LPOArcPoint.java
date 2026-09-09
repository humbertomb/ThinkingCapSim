/*
 * (c) 2003 Humberto Martinez
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class LPOArcPoint extends LPORangePoint implements Serializable
{
	protected double				speed;				// Object speed (m/s)
	
	// Constructors
	public LPOArcPoint (double side, int source)
	{
		super (side, source);
	}
	
	// Instance methods
	public void set (double x, double y, double speed, int source)
	{
		double			spd;
		
		locate (x, y, 0.0, source);
		
		spd	= Math.log (Math.abs (1.0 + speed));
		if (speed < 0.0)
			spd = -spd;

		this.speed	= spd;
	}
	
	public void draw (Model2D model, LPOView view)
	{
		double			xx, yy, aa;
				
		if (!active)	return;

		aa	= view.rotation + phi;
		xx 	= rho * Math.cos (aa);
		yy 	= rho * Math.sin (aa);
		
		if ((xx < view.min.x ()) || (xx > view.max.x ()) || (yy < view.min.y ()) || (yy > view.max.y ()))		return;
							
		model.addRawBox (xx - side, yy - side, xx + side, yy + side, Model2D.FILLED, ColorTool.fromWColorToColor(color));
		//model.addRawBox (xx - side, yy - side, xx + side, yy + side, Model2D.FILLED, color);
		model.addRawArrow (xx - side, yy - side, speed, aa, ColorTool.fromWColorToColor(WColor.YELLOW));
	}
}
