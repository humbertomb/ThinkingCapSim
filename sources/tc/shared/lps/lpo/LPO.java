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
	static public final double		ANCHOR_FADE	= 5.0;		// Time an anchoring takes to run out, from 1 to 0 (s)

	// Object sources
	static public final int			MAP			= 0;
	static public final int			CAMERA		= 1;
	static public final int			PERCEPT		= 2;
	static public final int			ANCHOR		= 3;
	static public final int			ARTIFACT	= 4;
	
	// Local object location
	public double 					x;				// local euclidean coordinates (m, m, rad)
	public double					y;
	public double 					phi;
	public double 					rho;			// local polar coordinates (m, rad) 
	public double 					theta;				
	
	// Perception related information
	public double					anchor;			// Anchoring value
	public double					anchor_time;
	public double					anchor_fade;
	public int						ageing;			// How old the perception is
	public boolean					anchored;		// Has it ever been anchored (seen)? The sensor percepts never are
	
	// Object features	
	public String					label;
	public WColor					color;
	public int 						source;			// where the perception came from?
	public boolean 					active;

	// Constructor
	protected LPO ()
	{
	}
	
	public LPO (double x, double y, double alpha, String label, int source)
	{			
		locate (x, y, alpha);
		
		this.label		= label;
		this.source		= source;
		
		color			= WColor.BLACK;
		
		anchor			= 0.0;
		anchor_time 	= -1.0;
		anchor_fade 	= ANCHOR_FADE;
		ageing			= 0;
		active			= false;
	}

	// Accessors
	public double			x ()					{ return x; }
	public double			y ()					{ return y; }
	public double			phi ()					{ return phi; }
	public double			theta ()				{ return theta; }
	public double			rho ()					{ return rho; }
	
	public double			anchor ()				{ return anchor; }
	public int				ageing ()				{ return ageing; }
	public int				source ()				{ return source; }
	public void				label (String label)	{ this.label = label; }
	public String			label ()				{ return label; }
	public boolean			active ()				{ return active; }
	public void				active (boolean active)	{ this.active = active; }
	public WColor			color ()				{ return color; }
	public void				color (WColor color)	{ this.color = color; }
	
	// Instance methods
	public void locate (double x, double y, double alpha)  
	{
		this.x		= x;
		this.y		= y;
		this.phi	= Angles.radnorm_180 (alpha);
		
		rho			= Math.sqrt (x * x + y * y);
		theta		= Angles.radnorm_180 (Math.atan2 (y, x));	
	}

	public void locate (double x, double y)
	{
		locate (x, y, 0.0);
	}
	
	public void locate_polar (double rho, double phi, double alpha)  
	{
		this.rho	= rho;
		this.theta	= phi;
		this.phi	= Angles.radnorm_180 (alpha);
		
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
		if (this.anchor > 0.0)		anchored = true;
	}

	/** Whether it has ever been anchored: it is an object the robot perceives, not a sensor reading. */
	public boolean			anchored ()				{ return anchored; }

	/** An object once anchored whose anchoring has run out: the LPS no longer knows where it is. */
	public boolean			lost ()					{ return anchored && (anchor <= 0.0); }

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
		phi	= Angles.radnorm_180 (phi + rm.mat[2][2]);
		rho		= Math.sqrt (x * x + y * y);
		theta		= Angles.radnorm_180 (Math.atan2 (y, x));	
    }
    
    // Subclasses MUST implement
	public abstract void draw (Model2D model, LPOView view);
}


