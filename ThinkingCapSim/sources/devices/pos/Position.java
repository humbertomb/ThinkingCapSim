/* ----------------------------------------
	(c) 2001-2003 Humberto Martinez Barbera
   ---------------------------------------- */

package devices.pos;

import java.io.*;

import wucore.utils.geom.*;
import wucore.utils.math.*;
import wucore.utils.math.jama.*;

public class Position extends Object implements Serializable
{	
	protected Point3				pt;				// Position 3D coordinates (m, m, m)
	protected double				alpha;			// Position heading (rad)
	protected double[][]			uncert;			// Position uncertainty

	protected boolean				valid;

	/* Constructors */
	public Position ()
	{
		pt		= new Point3 ();
		uncert	= null;
	}
	
	public Position (double x, double y, double alpha)
	{
		this ();

		set (x, y, alpha, true);
	}
	
	public Position (double x, double y, double z, double alpha)
	{
		this ();

		set (x, y, z, alpha, true);
	}
	
	public Position (Position pos)
	{
		this ();

		set (pos);
	}
	
	/* Accessors */
	public final double 		x ()					{ return pt.x (); }
	public final double 		y ()					{ return pt.y (); }
	public final double 		z ()					{ return pt.z (); }

	public final void 			x (double x)			{ pt.x (x); }
	public final void 			y (double y)			{ pt.y (y); }
	public final void 			z (double z)			{ pt.z (z); }
	public final void 			alpha (double alpha)	{ this.alpha = alpha; }

	public final double 		alpha ()				{ return alpha; }
	public final double[][] 	uncert ()				{ return uncert; }
	public final double 		phi ()					{ return Math.atan2 (pt.y (), pt.x ()); }
	public final double 		rho ()					{ return Math.sqrt ((pt.x () * pt.x ()) + (pt.y () * pt.y ())); }

	public final void	 		valid (boolean valid)	{ this.valid = valid; }
	public final boolean	 	valid ()				{ return valid; }

	/* Instance methods */

	// Position-related operations
	public void set (Position pos)
	{
		set (pos.pt.x (), pos.pt.y (), pos.pt.z (), pos.alpha, pos.valid);
		
		// Update uncertainty only if a new estimate is available
		if (pos.uncert != null)
			set (pos.uncert);
	}
	
	public void set (double x, double y)
	{
		set (x, y, 0.0, true);
	}
	
	public void set (double x, double y, double alpha)
	{
		set (x, y, alpha, true);
	}
	
	public void set (double x, double y, double z, double alpha)
	{
		set (x, y, z, alpha, true);
	}
	
	public void set (double x, double y, double alpha, boolean valid)
	{
		set (x, y, 0.0, alpha, true);
	}
	
	public void set (double x, double y, double z, double alpha, boolean valid)
	{
		pt.set (x, y, z);
		this.alpha	= Angles.radnorm_180 (alpha);
		
		this.valid	= valid;
	}
	
	// Polar coordinates management
	public void set_polar (double rho, double phi)
	{
		set (rho * Math.cos (phi), rho * Math.sin (phi));
	}
	
	public void set_polar (double rho, double phi, double alpha)
	{
		set (rho * Math.cos (phi), rho * Math.sin (phi), alpha, true);
	}
	
	public void add_polar (double rho, double phi, double alpha)
	{
		pt.x (pt.x () + rho * Math.cos (phi));
		pt.y (pt.y () + rho * Math.sin (phi));
		
		this.alpha	= Angles.radnorm_180 (this.alpha + alpha);
	}	
	
	// Uncertainty-related operations
	public void set (double[][] p)
	{
		uncert	= p;	
	}
		
	public void set (Matrix p)
	{
		uncert	= p.getArrayCopy ();
	}

	public Matrix getMatrix ()
	{
		Matrix		mat;
		
		if (uncert == null)					return null;
		
		mat	= new Matrix (uncert);
		
		return mat.getMatrix (0, 1, 0, 1);
	}

	// Utility functions
	public void delta (Position pos1, Position pos2)
	{
		set (pos1);
		delta (pos2);
	}
	
	public void delta (Position pos)
	{
		delta (pos.pt.x (), pos.pt.y (), pos.alpha);
	}
	
	public void delta (double x, double y, double alpha)
	{
		double			Ax, Ay, Aa;				// Absolute increments
		double			rho, phi;				// Absolute module and argument
		
		Ax = pt.x () - x;
		Ay = pt.y () - y;
		Aa = this.alpha - alpha;
		
		rho = Math.sqrt (Ax * Ax + Ay * Ay);
		phi = Math.atan2 (Ay, Ax);
		
		pt.x (rho * Math.cos (phi - alpha));
		pt.y (rho * Math.sin (phi - alpha));
		this.alpha = Angles.radnorm_180 (Aa); 	
	}
	
	public void translate (Position pos)
	{
		translate (pos.pt.x (), pos.pt.y (), pos.alpha);
	}	
	
	public void translate (double x, double y, double alpha)
	{
		double			rho, phi;
		
		// Convert local displacement to polar coordinates (Cartesian translation can not be applied)
		rho			= Math.sqrt (x * x + y * y);
		phi			= Math.atan2 (y, x);
		
		pt.x (pt.x () + rho * Math.cos (this.alpha + phi));
		pt.y (pt.y () + rho * Math.sin (this.alpha + phi));
		
		this.alpha	= Angles.radnorm_180 (this.alpha + alpha);
	}	
	
	public void untranslate (Position pos)
	{
		untranslate (pos.pt.x (), pos.pt.y (), pos.alpha);
	}	
	
	public void untranslate (double x, double y, double alpha)
	{
		double			rho, phi;
		
		// Convert back local displacement to polar coordinates (Cartesian translation can not be applied)
		rho			= Math.sqrt (x * x + y * y);
		phi			= Math.atan2 (y, x);
		
		this.alpha	= Angles.radnorm_180 (this.alpha - alpha);

		pt.x (pt.x () - rho * Math.cos (this.alpha + phi));
		pt.y (pt.y () - rho * Math.sin (this.alpha + phi));
	}	
	
	public double distance (Position p)
	{
		return pt.distance ((Point2) p.pt);
	}
	
	public double distance (Point2 p)
	{
		return pt.distance (p);
	}
	
	public double distance (double x, double y)
	{
		return pt.distance (x, y);
	}
	
	public String toString ()
	{
		return ("[" + pt + ", " + alpha*Angles.RTOD + "]");
	}
}

