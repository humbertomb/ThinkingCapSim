/**
 * Created on 15-jun-2006
 *
 * @author Humberto Martinez Barbera (2006)
 * @author David Herrero Perez (2004)
 * @author Alessandro Saffiotti (2002)
 */

package tcrob.umu.soccer.gm.fmk;

public class GridCell
{
	static public final double			FULL		= 1.0;
	static public final double			BIAS		= 0.01;
	static public final double			PI2		= (double) (1.0 * Math.PI);

	protected double center;	// Center angle, in rad
	protected double height;	// Height of trapezoid
	protected double core;		// Width of core (at height) in rad
	protected double support;	// Width of support (at zero) in rad
	protected double bias;		// Low value of trapezoid

	public GridCell ()
	{
		clear ();
	}
	
	public void setCenter (double center)		{ this.center = center; }
	public void setHeight (double height)		{ this.height = height; }
	public void setCore (double core)			{ this.core = core; }
	public void setSupport (double support)		{ this.support = support; }
	public void setBias (double bias)			{ this.bias = bias; }
	
	public double getCenter ()					{ return center; }
	public double getHeight ()					{ return height; }
	public double getCore ()					{ return core; }
	public double getSupport ()				{ return support; }
	public double getBias ()					{ return bias; }
	
	public void set (GridCell other)
	{
		center	= other.center;
		height	= other.height;
		core		= other.core;
		support	= other.support;
		bias		= other.bias;
	}

	public void set (double height, double center, double core, double support, double bias)
	{
		this.height		= height;
		this.center		= center;
		this.core		= core;
		this.support		= support;
		this.bias		= bias;
	}

	protected void checkConsistency ()
	{
		while (center < 0.0)	center += PI2;
		while (center >= PI2)	center -= PI2;
		
		if (core > PI2)			core = PI2;
		if (core < 0.0)			core = 0.0;
		
		if (support > PI2)		support = PI2;
		if (support < 0.0)		support = 0.0;
		
		if (bias > 1.0)			bias = 1.0;
		if (bias < BIAS)		bias = BIAS;
		
		if (height > 1.0)		height = 1.0;
		if (height < bias)		height = bias;
		
		if (height == bias)
		{
			core = PI2;
			support = PI2;
		}
	}

	public void normalize (double highest)
	{
		// Alternative normalization: increase all values
//		height += 1.0 - highest;
//		bias += 1.0 - highest;
		// old normalization: divide by the highest value
		height /= highest;
		bias /= highest;
	}

	/**
	 *  
	 * Compute the membership value of x to a trapezoid
	 * 
	 *            +---------------+ <-- height
	 *           /                 \
	 *          /                   \
	 * _________/                     \__________ bias
	 * --------+---+-------+-------+---+--------------------.
	 *         a   b     center    c   d                    x
	 *             |<-----core---.|
	 *         |<-------support------.|
	 * 
	 * 
	 */
	protected double getMu (double x)
	{
		double a, b, c, d;
		
		a = center - (0.5 * support);
		b = center - (0.5 * core);
		c = center + (0.5 * core);
		d = center + (0.5 * support);
		
		if (x <= a)
			return BIAS;
		
		if (x <= b)
			return (height - bias) * ((x-a) / (b-a)) + BIAS;
		
		if (x <= c)
			return height;
		
		if (x <= d)
			return (height - bias) * ((d-x) / (d-c)) + BIAS;
		
		return BIAS;
	}

	/**
	 * 
	 * Compute the union operator betwen two trapezoids
	 * 
	 * Set 1:
	 * 
	 *             +---------------+ <-- height
	 *            /                 \
	 *           /                   \
	 * _________/                     \________________bias
	 * --------+---+------- -------+---+-------------------------.
	 *         a   b               c   d                          x
	 * 
	 * Set 2:
	 * 
	 *              +-----------------------+ <-- height1
	 * ____________/                         \___________ bias1
	 * 
	 * ----------+--+-----------------------+--+-----------------.
	 *          a1  b1                     c1  d1                 x
	 * 
	 * Union resut:
	 * 
	 *             +-------------------------+ <-- height
	 *            /                           \
	 * __________/                             \__________ bias1
	 * 
	 * --------+--+--------------------------+---+-----------------.
	 *         a  b                         c1   d1                x
	 * 
	 */
	public void unionOperator (GridCell other)
	{
		double a, b, c, d;
		double a1, b1, c1, d1;
		double ar, br, cr, dr;
		
		double center1, core1, support1;
		
		if(other.height == BIAS) // If 'other' trapezoid is BIAS, union is the same set
		{
		} else if(height == BIAS) { // If this trapezoid is BIAS and 'other' is not, union is 'other' set
			set(other);
		} else {
			a = center - (0.5 * support);
			b = center - (0.5 * core);
			c = center + (0.5 * core);
			d = center + (0.5 * support);
			
			center1		= other.center;
			core1		= other.core;
			support1		= other.support;
			
			a1	= center1 - (0.5 * support1);
			b1	= center1 - (0.5 * core1);
			c1	= center1 + (0.5 * core1);
			d1	= center1 + (0.5 * support1);
			
			ar	= Math.min (a, a1);
			br 	= Math.min (b, b1);
			cr 	= Math.max (c, c1);
			dr	= Math.max (d, d1);
			
			core		= cr - br;
			support	= dr - ar;
			center	= br + core * 0.5;
			height	= Math.max (height, other.height);
			bias		= Math.max (bias, other.bias);
		}
			
		// some consistency check...
		checkConsistency();
	}

	/**
	 * 
	 * Fuzzy intersection, new method
	 * 
	 * Compute the upper trapezoidal envelope of the product intersection.
	 * New method based on the representation of the resulting fuzzy set by
	 * a piecewise linear function, and then find its outer envelope.
	 * 
	 * 1. find all the flexion point of the function
	 * 2. sort them
	 * 3. find the start and end of the initial ascent
	 * 4. find the start and end of the final descent
	 * 5. compute the trapezoid parameter from these
	 * 
	 */ 
	private double[]		xx = new double[8];
	private double[]		yy = new double[8];

	public void intersectionEnveloped (GridCell other)
	{
		double dist, threshold;
				
		double a_this, b_this, c_this, d_this;
		double a_other, b_other, c_other, d_other;
		
		double center_own;
		double center_other, core_other, support_other;
		
		center_own		= center;
		
		center_other		= other.center;
		core_other		= other.core;
		support_other	= other.support;
		
		// Move the lowest trapezoid up if we wrap around 0 degrees
		dist = center_own - center_other;
		
		if (dist < -Math.PI)
			center_own += PI2;
		else if (dist > Math.PI)
			center_other += PI2;
		
		center = center_own;
		other.center = center_other;
		
		// Find all the 8 inflexion points, and sort them
		// 
		// Note: there might be more inflexion points,
		// eg, if the two slopes intersect!
		
		a_this = center_own - (0.5 * support);
		b_this = center_own - (0.5 * core);
		c_this = center_own + (0.5 * core);
		d_this = center_own + (0.5 * support);
		
		a_other = center_other - (0.5 * support_other);
		b_other = center_other - (0.5 * core_other);
		c_other = center_other + (0.5 * core_other);
		d_other = center_other + (0.5 * support_other);
		
		// TRY MERGESORT ????
		xx[0] = a_this;
		xx[1] = b_this;
		xx[2] = c_this;
		xx[3] = d_this;
		
		pushSorted(a_other, xx, 0, 4);
		pushSorted(b_other, xx, 1, 5);
		pushSorted(c_other, xx, 2, 6);
		pushSorted(d_other, xx, 3, 7);
		
		threshold = 0.0;
		
		// Find the correspoiding y value
		for (int i=0; i<8; ++i)
			yy[i] = getMu(xx[i]) * other.getMu(xx[i]);
		
		// Find the 4 parameters of the trapezoidal envelope by looking at
		// the increase/decrease of the piecewise linear function
		
		int a, b, c, d;
		
		a = b = 0;
		c = d = 7;
		
		for (int i = 1; i < 8; ++i)
		{
			if (yy[i] - yy[i-1] > threshold) // track initial ascent
				b = i;
			
			if (yy[i] < yy[i-1]) // end of ascent
				break;
			
			for (int j = 6; j>=i; --j)
			{
				if (yy[j]- yy[j+1] > threshold) // track terminating descent
					c = j;
				
				if (yy[j] < yy[j+1]) // end of descent
					break;
			}
		}
		
		// Convert the parameters to our format. This may introduce errors.
		// Moreover, since we are modulo 360, two trapezoids with 
		//support > 180 will produce two opposite modalities, which
		// is not considered here!   
		
		center	= (xx[b] + xx[c]) * 0.5;
		core		= xx[c] - xx[b];
		support	= xx[d] - xx[a];
		height	= Math.max (yy[b], yy[c]);
		bias		= Math.max (yy[a], yy[d]);
		
		// some consistency check...
		checkConsistency();
	}

	/**
	 * 
	 * Insert new element x in sorted list, using sublist from start to end
	 * 
	 */
	protected void pushSorted (double x, double list[], int start, int end)
	{
		int i, j;
		
		list[end] = x;
		
		for (i = start; i < end; ++i)
		{
			if (x < list[i])
			{
				for (j = end; j > i; --j)
					list[j] = list[j-1];
				
				list[i] = x;
				break;
			}
		}
	}

	public void clear ()
	{
		center	= 0.0;
		core		= PI2;
		support	= PI2;
		height	= FULL;
		bias		= BIAS;
	}

	public void reset ()
	{
		center	= 0.0;
		core		= PI2;
		support	= PI2;
		height	= BIAS;
		bias		= BIAS;
	}
}
