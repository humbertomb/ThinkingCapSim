/***************************************************************
 ***
 ***  BPoint.java
 ***
 ***  Range data point for the LPS
 ***
 ***  (c) 1999 Alessandro Saffiotti
 ***  (c) 2000-2002 Humberto Martinez
 ***  (c) 2003 Humberto Martinez, Alessandro Saffiotti
 ***************************************************************/

package tc.shared.lps.lpo;

import java.io.*;

import wucore.widgets.*;
import wucore.utils.math.*;
import wucore.utils.color.*;

public class LPORangePoint extends LPORange implements Serializable
{
	static public final int			SONAR	= 0;
	static public final int			LRF		= 1;
	
	protected double					side;			// Quantization cell size (m)
	protected int					sensor;			// Type of sensor that produced the measurement
	
	// Constructors
	public LPORangePoint (double side, int source)
	{
		super (null, null, source);

		color (WColor.LIGHT_GRAY);
		
		this.side	= side * 0.5;
	}
	
	// Accessors
	public final int		sensor ()			{ return sensor; }
	
	// Instance methods
	public void set (double x, double y, double len, int sensor, int source)
	{
		locate (x, y, 0.0, len);
		
		this.sensor	= sensor;	
		this.source	= source;	
		this.ageing	= 0;
	}
	
	public double distance (LPORangePoint point)
	{
		double		dx, dy;
		
		dx	= x - point.x;
		dy	= y - point.y;
		return Math.sqrt (dx * dx + dy * dy);
	}
	
	public void clamp (Matrix3D rm)
	{
		ageing	++;

		super.clamp (rm);	
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
	}
}
