/* 
 * (c) 2002 Alessandro Saffiotti
 * (c) 2004 David Herrero Perez
 * 
 * Fuzzy Markov 2.5 Grid
 * 
 */
 
package tclib.navigation.localisation.fmarkov;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */
public class F2_5Cell extends Object implements Cloneable {
	public static final double PI2 = 2*Math.PI;
	public static final double PIq = 0.25*Math.PI;
	
	public static final double	EMPTY	= 0.0;
	public static final double	FULL	= 1.0;
	
	public static final double	ANGLE_SLOPE = 0.5*Math.PI;
	
	public static final double	BIAS		= 0.01; // bias to account for mis-identification
	
	//public static final double FUZZY_CORE_WIDTH = 0.05;	// width of entire core part (% of measured distance)
	//public static final double FUZZY_SLOPE_WIDTH = 0.6;	// width of each slope part  (% of measured distance)
	public static final double DEFAULT_ANGLE_WIDTH = Math.toRadians(20.0); // width of core part for orientation trapezoid
	public static final double DEFAULT_ANGLE_SLOPE = Math.toRadians(90.0); // width of slope part for all orientation trapezoids
	
	double center;		// Center angle, in rad
	double height;		// Height of trapezoid
	double core;		// Width of core (at height) in rad
	double support;	// Width of support (at zero) in rad
	double bias;		// Low value of trapezoid
	
	// Constructors
	public F2_5Cell()
	{
		clear();
	}
	
	public F2_5Cell(double height, double center, double core, double support, double bias)
	{
		set(height, center, core, support, bias);
	}
	
	// Accessor methods
	
	public void setHeight(double height)
	{
		this.height = height;
	}
	
	public double getHeight()
	{
		return height;
	}
	
	public void setCenter(double center)
	{
		this.center = center;
	}
	
	public double getCenter()
	{
		return center;
	}
	
	public void setCore(double core)
	{
		this.core = core;
	}
	
	public double getCore()
	{
		return core;
	}
	
	public void setSupport(double support)
	{
		this.support = support;
	}
	
	public double getSupport()
	{
		return support;
	}
	
	public void setBias(double bias)
	{
		this.bias = bias;
	}
	
	public double getBias()
	{
		return bias;
	}
	
	public void set(double height, double center, double core, double support, double bias)
	{
		this.height	= height;
		this.center	= center;
		this.core		= core;
		this.support	= support;
		this.bias		= bias;
	}
	
	public void set(F2_5Cell other)
	{
		center = other.getCenter();
		height = other.getHeight();
		core = other.getCore();
		support = other.getSupport();
		bias = other.getBias();
	}
	
	// Instance methods
	
	/**
	 * 
	 *  Compute the membership value of x to a trapezoid
	 * 
	 *             +---------------+ <-- height
	 *            /                 \
	 *           /                   \
	 * _________/                     \__________ bias
	 * --------+---+-------+-------+---+--------------------->
	 *         a   b     center    c   d                    x
	 *             |<-----core---->|
	 *         |<-------support------->|
	 * 
	 *
	 */
	public double getTrapezoidMu(double x)
	{
		double a, b, c, d;
		
		a	= center - (0.5 * support);
		b	= center - (0.5 * core);
		c	= center + (0.5 * core);
		d	= center + (0.5 * support);
		
		if (x <= a)
			return F2_5Cell.BIAS;
		
		if (x <= b)
			return (height - bias) * ((x-a) / (b-a)) + F2_5Cell.BIAS;
		
		if (x <= c)
			return height;
		
		if (x <= d)
			return (height - bias) * ((d-x) / (d-c)) + F2_5Cell.BIAS;
		
		return F2_5Cell.BIAS;
	}
	
	/**
	 * 
	 * @author dherrero
	 * 
	 *	Compute the union operator betwen two trapezoids
	 *
	 *	Set 1:
	 *  
	 *             +---------------+ <-- height
	 *            /                 \
	 *           /                   \
	 * _________/                     \________________bias
	 * --------+---+------- -------+---+-------------------------->
	 *         a   b               c   d                          x
	 * 
	 *	Set 2:
	 * 
	 *              +-----------------------+ <-- height1
	 * ____________/                         \___________ bias1
	 * 
	 * ----------+--+-----------------------+--+------------------>
	 *          a1  b1                     c1  d1                 x
	 * 
	 *	Union resut:
	 * 
	 *             +-------------------------+ <-- height
	 *            /                           \
	 * __________/                             \__________ bias1
	 * 
	 * --------+--+--------------------------+---+------------------>
	 *         a  b                         c1   d1                x
	 *
	 */
	public void union(F2_5Cell other)
	{
		double a, b, c, d;
		double a1, b1, c1, d1;
		double ar, br, cr, dr;
		
		double center1, core1, support1;
		
		if(other.getHeight() == BIAS) // If 'other' trapezoid is BIAS, union is the same set
		{
			
		} else if(height == BIAS) { // If this trapezoid is BIAS and 'other' is not, union is 'other' set
			set(other);
		} else {
			a	= center - (0.5 * support);
			b	= center - (0.5 * core);
			c	= center + (0.5 * core);
			d	= center + (0.5 * support);
			
			center1		= other.getCenter();
			core1		= other.getCore();
			support1	= other.getSupport();
			
			a1	= center1 - (0.5 * support1);
			b1	= center1 - (0.5 * core1);
			c1	= center1 + (0.5 * core1);
			d1	= center1 + (0.5 * support1);
			
			ar	= Math.min(a, a1);
			br 	= Math.min(b, b1);
			cr 	= Math.max(c, c1);
			dr	= Math.max(d, d1);
			
			core	= cr - br;
			support	= dr - ar;
			center	= br + core * 0.5;
			height	= Math.max(height, other.getHeight());
			bias	= Math.max(bias, other.getBias());
		}
		
		// some consistency check...
		CheckConsistency();
	}
	
	/**
	 * 
	 * @author asaffiotti
	 * @since 030619
	 * 
	 * Fuzzy intersection, new method
	 * 
	 * Compute the upper trapezoidal envelope of the product
	 * intersection.
	 * New method based on the representation of the resulting
	 * fuzzy set by a piecewise linear function, and then find its
	 * outer envelope.
	 * 
	 *    1. find all the flexion point of the function
	 *    2. sort them
	 *    3. find the start and end of the initial ascent
	 *    4. find the start and end of the final descent
	 *    5. compute the trapezoid parameter from these
	 * 
	 */
	public void intersectionEnveloped(F2_5Cell other)
	{
		double dist, threshold;
		double[] x, y;
		
		double a_this, b_this, c_this, d_this;
		double a_other, b_other, c_other, d_other;
		
		double center_own;
		double center_other, core_other, support_other;
		
		x = new double[8];
		y = new double[8];
		
		center_own		= center;
		
		center_other	= other.getCenter();
		core_other		= other.getCore();
		support_other	= other.getSupport();
		
		// Move the lowest trapezoid up if we wrap around 0 degrees
		dist = center_own - center_other;
		
		if (dist < -Math.PI)
			center_own += PI2;
		else if (dist > Math.PI)
			center_other += PI2;
		
		setCenter(center_own);
		other.setCenter(center_other);
		
		// Find all the 8 inflexion points, and sort them
		// 
		// Note: there might be more inflexion points,
		// eg, if the two slopes intersect!
		
		a_this	= center_own - (0.5 * support);
		b_this	= center_own - (0.5 * core);
		c_this	= center_own + (0.5 * core);
		d_this	= center_own + (0.5 * support);
		
		a_other		= center_other - (0.5 * support_other);
		b_other	= center_other - (0.5 * core_other);
		c_other		= center_other + (0.5 * core_other);
		d_other	= center_other + (0.5 * support_other);
		
		// TRY MERGESORT ????
		x[0] = a_this;
		x[1] = b_this;
		x[2] = c_this;
		x[3] = d_this;
		
		PushSorted(a_other, x, 0, 4);
		PushSorted(b_other, x, 1, 5);
		PushSorted(c_other, x, 2, 6);
		PushSorted(d_other, x, 3, 7);
		
		threshold = 0.0;
		
		// Find the correspoiding y value
		for (int i=0; i<8; ++i)
			y[i] = this.getTrapezoidMu(x[i]) * other.getTrapezoidMu(x[i]);
		
		// Find the 4 parameters of the trapezoidal envelope
		// by looking at the increase/decrease of the piecewise
		// linear function
		
		int a, b, c, d;
		
		a = b = 0;
		c = d = 7;
		
		for (int i=1; i<8; ++i)
		{
			if (y[i] - y[i-1] > threshold) // track initial ascent
				b = i;
			
			if (y[i] < y[i-1]) // end of ascent
				break;
			
			for (int j = 6; j>=i; --j)
			{
				if (y[j]- y[j+1] > threshold) // track terminating descent
					c = j;
				
				if (y[j] < y[j+1]) // end of descent
					break;
			}
		}
		
		// Convert the parameters to our format
		// This may introduce errors. Moreover, since we are
		// modulo 360, two trapezoids with support > 180 will
		// produce two opposite modalities, which is not considered here!   
		
		center = (x[b] + x[c]) * 0.5;
		core = x[c] - x[b];
		support = x[d] - x[a];
		height = Math.max(y[b], y[c]);
		bias = Math.max(y[a], y[d]);
		
		// some consistency check...
		CheckConsistency();
	}
	
	/**
	 * 
	 * @author asaffiotti
	 *
	 *	Insert new element x in sorted list, using sublist from start to end
	 */
	public void PushSorted (double x, double list[], int start, int end)
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
	
	// some consistency check...
	public void CheckConsistency()
	{
		while (center < 0.0)	center += PI2;
		while (center >= PI2)	center -= PI2;
		
		if (core > PI2)			core = PI2;
		if (core < 0.0)			core = 0.0;
		
		if (support > PI2)		support = PI2;
		if (support < 0.0)		support = 0.0;
		
		if (bias > 1.0)			bias = 1.0;
		//if (bias < 0.0)			bias = 0.0;
		if (bias < BIAS)		bias = BIAS;
		
		if (height > 1.0)		height = 1.0;
		if (height < bias)		height = bias;
		
		if (height == bias)
		{
			core = PI2;
			support = PI2;
		}
	}
	
	public void clear()
	{
		center	= 0.0;
		core	= PI2;
		support	= PI2;
		height	= F2_5Cell.FULL;
		bias	= F2_5Cell.BIAS;
	}
	
	public void reset()
	{
		center	= 0.0;
		core	= PI2;
		support	= PI2;
		height	= F2_5Cell.BIAS;
		bias	= F2_5Cell.BIAS;
	}
	
	public void normalize(double highest)
	{
		// Alternative normalization: increase all values
		height += 1.0 - highest;
		bias += 1.0 - highest;
		
		// old normalization: divide by the highest value
		//height /= highest;
		//bias /= highest;
	}
	
	public Object clone()
	{
		try {
			// This clone() from Object makes a bitwise copy
			// of the properties (here just "flock")
			return super.clone();
		} catch (CloneNotSupportedException e ) {
			throw new Error("This should never happen!");
		}
	}
	
	public String toString()
	{
		return "[ height: " + height + ", center: " + Math.toDegrees(center) + ", core:" + Math.toDegrees(core) + ", support:" + Math.toDegrees(support) + ", bias:" + bias + "]";
	}
	
	public String toTrapezoidString()
	{
		double a = center - (0.5 * support);
		double b = center - (0.5 * core);
		double c = center + (0.5 * core);
		double d = center + (0.5 * support);
		
		return " FTrapezoid <" + Math.toDegrees(a) + " " + Math.toDegrees(b) + " " + Math.toDegrees(c) + " " + Math.toDegrees(d) + "> [" + height + ", " + bias + "]";
	}
}
