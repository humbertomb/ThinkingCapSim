/*
 * Created on 18-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tclib.utils.petrinets;

import java.util.*;

import wucore.widgets.*;
import wucore.utils.color.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class PNObject extends Object
{
	static public final String	NONAME	= "none";
	static public final double	OBJGAP	= 35.0;
	
	static private int 			factor	= 1;
	static private int 			sign		= 1;
	static private int 			scount	= 0;
	static private boolean 		even		= false;
	static private boolean 		bsign	= false;
	static private double 		xd		= 10.0;
	static private double 		yd		= 10.0;

	protected String 			name;
	protected int 				x;
	protected int 				y;
	protected WColor			color;
	
	protected double				dx		= 0.0;
	protected double				dy		= 0.0;
	
	// Constructors
	protected PNObject () 
	{
		name	= NONAME;
		color	= WColor.LIGHT_GRAY;
		
		initCoords ();
	}
	
	// Accessors
	public void			setColor(WColor col)		{ color = col; }
	public WColor	getColor() 					{ return color; }
	public boolean		isNamed ()					{ return !name.equals (NONAME); }
	public String 		getName() 					{ return name; }	
	public void 			setName(String name) 			{ this.name = name; }	
	
	public int			x ()							{ return x; }
	public int 			y ()							{ return y; }
	public void 			x (int x) 					{ this.x = x; }
	public void 			y (int y) 					{ this.y = y; }
	public void 			x (double x) 				{ this.x = (int) Math.round (x); }
	public void 			y (double y) 				{ this.y = (int) Math.round (y); }

	public double		dx ()						{ return dx; }
	public double 		dy ()						{ return dy; }
	public void 			dx (double dx) 				{ this.dx = dx; }
	public void 			dy (double dy) 				{ this.dy = dy; }

	// Abstract methods (subclasses MUST implement)
	public abstract void draw (Model2D model, PetriNet pn);
	
	// Instance methdos
	protected synchronized void initCoords ()
	{
		yd += OBJGAP*factor;
		scount++;
		if (scount == sign)
		{
			scount = 0;
			if (bsign)
				sign++;
			bsign = !bsign;				
			factor *= -1;
		}	
		if (even) 
			factor /= 2;
		else 
		{
			factor *= 2;
			xd += OBJGAP;
		}
		even = !even;
		
		x	= (int) Math.round (xd);
		y	= (int) Math.round (yd);
	}
		
	public double distance (double x, double y)
	{
		double dx = (this.x - x);
		double dy = (this.y - y);
		double dist = Math.sqrt(dx*dx + dy*dy);
		return dist;
	}
	
	public void set (PNObject o) 
	{
		setName(o.name);
		setColor(o.color);
		x(o.x);
		y(o.y);
	}
	
	public void fromPropString (StringTokenizer st)
	{
		name		= st.nextToken ();
		x		= Integer.parseInt (st.nextToken());
		y		= Integer.parseInt (st.nextToken());
//		y		= Double.parseDouble (st.nextToken());
		color	= ColorTool.getColorFromName (st.nextToken ());
//		color	= ColorTool.getColorFromName (st.nextToken ());
	}

	public String toPropString ()
	{
		return name + ", " + x + ", " + y + ", " + color;
	}
}
