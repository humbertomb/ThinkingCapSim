/***************************************************************
 ***
 ***  LPO.java
 ***
 ***  Object description in the Local Perceptual Space
 ***
 ***  (c) 1999 A. Saffiotti
 ***  (c) 2000-2002 Humberto Martinez
 ***************************************************************/

package tc.shared.lps.lpo;

import java.io.*;
//import java.awt.*;

import wucore.widgets.*;
import wucore.utils.math.*;
import wucore.utils.color.*;

public abstract class LPO extends Object implements Serializable
{
	// Object sources
	static public final int			MAP			= 0;
	static public final int			CAMERA		= 1;
	static public final int			PERCEPT		= 2;
	static public final int			ANCHOR		= 3;
	static public final int			ARTIFACT	= 4;
	
	// Local object location
	protected double 				x;				// local euclidean coordinates (m, m, rad)
	protected double					y;
	protected double 				alpha;
	protected double 				rho;				// local polar coordinates (m, rad) 
	protected double 				phi;				
	
	// Perception related information
	protected double					anchor;			// Anchoring value
	protected int					ageing;			// How old the perception is
	
	// Object features	
	protected String					label;
	protected WColor				color;
	protected int 					source;			// where the perception came from?
	protected boolean 				active;

	// Constructor
	protected LPO ()
	{
	}
	
	public LPO (double x, double y, double alpha, String label, int source)
	{			
		locate (x, y, alpha);
		
		this.label	= label;
		this.source	= source;
		
		color	= WColor.BLACK;
		
		anchor	= 0.0;
		ageing	= 0;
		active	= false;
	}

	// Accessors
	public double			x ()						{ return x; }
	public double			y ()						{ return y; }
	public double			alpha ()					{ return alpha; }
	public double			phi ()					{ return phi; }
	public double			rho ()					{ return rho; }
	
	public double			anchor ()				{ return anchor; }
	public int				ageing ()				{ return ageing; }
	public int				source ()				{ return source; }
	public void				label (String label)		{ this.label = label; }
	public String			label ()					{ return label; }
	public boolean			active ()				{ return active; }
	public void				active (boolean active)	{ this.active = active; }
	public WColor		color ()					{ return color; }
	public void				color (WColor color)	{ this.color = color; }
	
	// Instance methods
	public void locate (double x, double y, double alpha)  
	{
		this.x		= x;
		this.y		= y;
		this.alpha	= Angles.radnorm_180 (alpha);
		
		rho			= Math.sqrt (x * x + y * y);
		phi			= Angles.radnorm_180 (Math.atan2 (y, x));	
	}

	public void locate (double x, double y)
	{
		locate (x, y, 0.0);
	}
	
	public void locate_polar (double rho, double phi, double alpha)  
	{
		this.rho	= rho;
		this.phi	= phi;
		this.alpha	= Angles.radnorm_180 (alpha);
		
		x			= rho * Math.cos (phi);
		y			= rho * Math.sin (phi);	
	}

	public void locate_polar (double rho, double phi)
	{
		locate_polar (rho, phi, 0.0);
	}
	
	public void anchor (double anchor)
	{
		this.anchor	= Math.max (Math.min (anchor, 1.0), 0.0);
	}

	public void ageing (int ageing)
	{
		this.ageing	= ageing;
	}

	/*  
	 *  Use a rotation matrix to convert the position of an LPO
	 *  Note: it is NOT a multiplication
	 *
	 *         | xx yx zx |  | x  |            | x' |
	 *  APPLY (| xy yy zy |, | y  |)  =>  PT = | y' |
	 *         | xz yz zz |  | th |            | th'|
	 */
	public void clamp (Matrix3D rm)
	{
		double		xx, yy;
		
		if (!active)	return;
		
		xx		= x;
		yy		= y;				
		x		= (rm.mat[0][0] * xx) + (rm.mat[0][1] * yy) + rm.mat[0][2];
		y		= (rm.mat[1][0] * xx) + (rm.mat[1][1] * yy) + rm.mat[1][2];	
		alpha	= Angles.radnorm_180 (alpha + rm.mat[2][2]);
		rho		= Math.sqrt (x * x + y * y);
		phi		= Angles.radnorm_180 (Math.atan2 (y, x));	
    }
    
    // Subclasses MUST implement
	public abstract void draw (Model2D model, LPOView view);
}


