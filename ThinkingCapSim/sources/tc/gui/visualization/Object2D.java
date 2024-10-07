/*
 * (c) 1997-2003 Humberto Martinez
 *				 Juan Pedro Canovas Qui–onero
 */

package tc.gui.visualization;

import java.awt.*;

import tc.fleet.*;

import devices.pos.*;
import wucore.widgets.*;

public class Object2D extends Object
{
	// Icon properties
	static public final double			T_WIDTH		= 0.25;				// Trace icon width (m)
	static public final double			T_LENGHT		= 0.5;				// Trace icon lenght (m)
	
	// LPS HUD's labels
	public static final int				H_ZONE		= 0;
	
	// Window boundaries
	protected double						MAXX_BNDRY	= 100.0;				// LPS window higher horizontal boundary
	protected double						MAXY_BNDRY	= 100.0;				// LPS window higher vertical boundary
	protected double						MINX_BNDRY	= -MAXX_BNDRY;		// LPS window lower horizontal boundary
	protected double						MINY_BNDRY	= -MAXY_BNDRY;		// LPS window lower vertical boundary
	
	// World and robot features
	protected Model2D					model;
	
	// Behaviour of the widget
	protected boolean					autoscale	= true;
	protected boolean					clipping		= false;
	
	// Configuration of the layers to be drawn
	protected boolean					drawpath;						// Draw the path followed by the robot
	protected boolean					drawtrace;						// Trace the positions occupied by the robot
	
	// Configuration of the widgets drawing procedures
	protected int						T_FACTOR		= 5;					// Plot a trace out of this number
	
	/* Constructors */
	protected Object2D ()
	{
	}
	
	public Object2D (Model2D model)
	{
		this.initialise (model);
	}
	
	/* Accesors */
	public final void		drawpath (boolean b)				{ drawpath = b; }
	public final void		drawtrace (boolean b)				{ drawtrace = b; }
	
	public final void		autoscale (boolean autoscale)		{ this.autoscale = autoscale; }
	public final void		clipping (boolean clipping)		{ this.clipping = clipping; }
		
	public void				boundary (double bound)											{ MAXX_BNDRY = bound; MAXY_BNDRY = bound; MINX_BNDRY = -bound; MINY_BNDRY = -bound; }
	public void				boundary (double maxx, double maxy)								{ MAXX_BNDRY = maxx; MAXY_BNDRY = maxy; MINX_BNDRY = -maxx; MINY_BNDRY = -maxy; } 
	public void				boundary (double minx, double miny, double maxx, double maxy)		{ MAXX_BNDRY = maxx; MAXY_BNDRY = maxy; MINX_BNDRY = minx; MINY_BNDRY = miny; } 
	
	/* Instance methods */
	protected void initialise (Model2D model) 
	{
		this.model			= model;
		this.drawpath		= true;
		this.drawtrace		= true;		
	}
	
	public void drawTrace (Path path, Color color)
	{
		int				i;
		double			xx, yy, aa;
		double			x1, y1, x2, y2, x3, y3;
		double			l2, w2, h, a;
		
		if (path == null)				return;
		
		l2	= T_LENGHT * 0.5;
		w2	= T_WIDTH * 0.5;
		h	= Math.sqrt (T_LENGHT * T_LENGHT + w2 * w2);
		a	= Math.PI - Math.asin (w2 / h);
		
		i = 0;
		for (Position pos = path.first (); pos != null; pos = path.next (), i++)
		{
			if (i % T_FACTOR != 0)			continue;
			
			xx	= pos.x ();
			yy	= pos.y ();
			aa	= pos.alpha ();
			
			x1	= xx + l2 * Math.cos (aa);
			y1	= yy + l2 * Math.sin (aa);
			
			x2	= x1 + h * Math.cos (aa + a);
			y2	= y1 + h * Math.sin (aa + a);
			
			x3	= x1 + h * Math.cos (aa - a);
			y3	= y1 + h * Math.sin (aa - a);
			
			model.addRawLine (x1, y1, x2, y2, color);
			model.addRawLine (x1, y1, x3, y3, color);
			model.addRawLine (x2, y2, x3, y3, color);
		}
	}
	
	public void drawTraces (Paths paths)
	{
		if (paths == null)				return;
		
		drawTrace (paths.odometry (), Color.YELLOW);	
		drawTrace (paths.corrected (), Color.ORANGE.darker());		
		drawTrace (paths.real (), Color.BLUE);
	}
	
	public void drawPath (Path path, Color color, int mode)
	{
		Position			pos, last;
		double			xi, yi, xf, yf;
		
		if ((path == null) || (path.num () < 2))		return;
		
		last = path.first ();
		for (pos = path.next (); pos != null; pos = path.next ())
		{
			xi = last.x ();
			yi = last.y ();			
			xf = pos.x ();
			yf = pos.y ();
			
			last = pos;
			
			model.addRawLine (xi, yi, xf, yf, mode, color);
		}
	}
	
	public void drawPaths (Paths paths)
	{
		if (paths == null)				return;
		
		drawPath (paths.odometry (), Color.YELLOW, Model2D.PLAIN);	
		drawPath (paths.corrected (), Color.ORANGE.darker(), Model2D.PLAIN);		
		drawPath (paths.real (), Color.BLUE, Model2D.PLAIN);
	}  
	
	public void drawVehicle (VehicleDesc rdesc, double x, double y, double a, Color color)
	{
		int				i;
		
		if (rdesc == null)				return;
		
		// Draw the real robot
		if (rdesc.icon == null)
			model.addRawCircle (x, y, rdesc.RADIUS, color);
		else
			for (i = 0; i < rdesc.icon.length; i++)
				model.addRawTransRotLine (rdesc.icon[i], x, y, a, color);
	}
}