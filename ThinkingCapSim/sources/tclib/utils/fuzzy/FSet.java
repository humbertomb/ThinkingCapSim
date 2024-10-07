/*
 * @(#)FSet.java	1.1 2003/12/08
 * 
 * (c) 1997-2001 Humberto Martinez
 * (c) 2003 Denis Remondini
 * (c) 2004 Humberto Martinez
 */

package tclib.utils.fuzzy;

import java.io.*;

import wucore.utils.geom.*;

/**
 * This abstract class serves as a basis for implementing various kind of fuzzy set.
 * At this level there are only basic methods and the possibility to choose from
 * different type of T-NORM and T-CONORM.
 * 
 * @version	1.1 	08 Dic 2003
 * @author 	Denis Remondini
 */
public abstract class FSet implements Serializable
{
	/* Different type of T-conorms */
	
	/**
	 * Specifies you want to use the Lukasiewicz  t-conorm.
	 */
	static public final int				TCO_LUKASIEWICZ	= 0;

	/**
	 * Specifies you want to use the Maximum t-conorm.
	 */
	static public final int				TCO_MAX		= 1;

	/**
	 * Specifies you want to use the Probabilistic Sum t-conorm.
	 */
	static public final int				TCO_PSUM	= 2;
	
	/**
	 * Specifies you want to use the Sugeno t-conorm.
	 */
	static public final int				TCO_SUGENO	= 3;
	
	
	/* Different type of T-norms */
	/**
	 * Specifies you want to use the Product t-norm.
	 */
	static public final int				T_PROD		= 0;
	
	/**
	 * Specifies you want to use the Lukasiewicz t-norm.
	 */
	static public final int				T_LUKASIEWICZ	= 1;

	/**
	 * Specifies you want to use the Minimum t-norm.
	 */
	static public final int				T_MIN		= 2;
	
	/* Different type of defuzzification */
	
	/**
	 * Specifies you want to use the Center of Gravity method 
	 * in the defuzzification process 
	 */
	static public final int				DE_COG		= 0;
	
	/**
	 * Specifies you want to use the maximum method 
	 * in the defuzzification process 
	 */
	static public final int 			DE_MAX		= 1;
	
	// Points that define the set (pointwise sets only)
	static public final int				A			= 0;
	static public final int				B			= 1;
	static public final int				C			= 2;
	static public final int				D			= 3;
	
	static protected final int			MAXPOINTS	= 10;		// Max number of points in a fuzzy set
	static protected final double		LAMBDA		= 1.0;		// Sugeno's lambda parameter

	/* Current tnorm and tconorm operators */
	static protected int				tnorm		= T_MIN;
	static protected int				tconorm		= TCO_MAX;

	/* Current defuzzification operator */
	static protected int				defuz		= DE_COG;

	/* Points that define the set */
	protected Point2[]					shape;
	protected double					mean;					// Mean value (pointwise sets only)

	/**
	 * Constructs a new fuzzy set initializing the points that define it.
	 */
	protected FSet() {
		this.shape 	= new Point2[MAXPOINTS];
		for (int i = 0; i < MAXPOINTS; i++)
			this.shape[i] = new Point2();	
	}

	/**
	 * Specifies the T-NORM that you want to use working with the fuzzy set.
	 * @param tn one of the following values: <br>
	 * 			<ul>
	 * 				<li> T_PROD </li>
	 * 				<li> T_MIN	</li>
	 * 			</ul>
	 */
	static public void set_tnorm(int tn) { 
		tnorm = tn; 
	}
	
	/**
	 * Specifies the T-CONORM that you want to use working with the fuzzy set.
	 * @param tco one of the following values: <br>
	 * 			<ul>
	 * 				<li> TCO_LUKASIEWICZ	</li>
	 * 				<li> TCO_MAX	</li>
	 * 				<li> TCO_PSUM	</li>
	 * 				<li> TCO_SUGENO	</li>
	 * 			</ul>
	 */
	static public void set_tconorm(int tco)	{
		tconorm = tco; 
	}
	
	/**
	 * Specifies the defuzzification that you want to use working with the fuzzy set.
	 * @param def one of the following values: <br>
	 * 			<ul>
	 * 				<li> DE_COG	</li>
	 * 				<li> DE_MAX	</li>
	 * 			</ul>
	 */
	static public void set_defuz(int def)	{
		defuz = def; 
	}
	
	/**
	 * Returns the associated crisp truth value
	 * @param x fuzzy truth value
	 * @return crisp truth value
	 */
	static public boolean fif(double x)  {
		return ((x >= 0.5) ? true : false);
	}

	/**
	 * Applies the T-NORM operator
	 * @param x a fuzzy predicate
	 * @param y a fuzzy predicate
	 * @return a fuzzy predicate that is the result of the T-NORM operation
	 */
	static public double tnorm(double x, double y)	{
		double t;

		switch (tnorm) {
			case T_PROD:
				t = x * y;
				break;
			case T_LUKASIEWICZ:
				t = Math.max(x + y - 1, 0);
				break;
			case T_MIN:
			default:
				t = Math.min(x, y);
		}
		return t;
	}

	/**
	 * Applies the T-CONORM operator
	 * @param x a fuzzy predicate
	 * @param y a fuzzy predicate
	 * @return a fuzzy predicate that is the result of the T-CONORM operation
	 */
	static public double tconorm(double x, double y) {
		double tco;
		
		switch (tconorm) {
			case TCO_LUKASIEWICZ:
				tco = Math.min(1.0, x + y);
				break;
			case TCO_PSUM:
				tco = x + y - (x * y);
				break;
			case TCO_SUGENO:
				tco = Math.min(1.0, x + y + (LAMBDA * x * y));
				break;
			case TCO_MAX:
			default:
				tco = Math.max(x, y);
		}
		return tco;
	}

	/**
	 * Applies the NOT operator
	 * @param x a fuzzy predicate
	 * @return a fuzzy predicate that is the result of the NOT operation
	 */
	static public double not(double x) {
		return 1.0 - x;
	}
	
	/**
	 * Applies the AND operator. The AND operator is defined as the T-NORM that
	 * uses the minimum operator.
	 * @param x a fuzzy predicate
	 * @return a fuzzy predicate that is the result of the AND operation
	 */
	static public double and(double x, double y) {
		tnorm = T_MIN;
		return tnorm(x,y);
	}
	
	/**
	 * Applies the OR operator. The OR operator is defined as the T-CONORM that
	 * uses the maximum operator.
	 * @param x a fuzzy predicate
	 * @return a fuzzy predicate that is the result of the OR operation
	 */
	static public double or(double x, double y) {
		tconorm = TCO_MAX;
		return tconorm(x,y);
	}
	
	/**
	 * Returns the mean value of the set (only pointwise sets)
	 * @return the mean value of the set
	 */
	public final double mean ()
	{
		return mean;
	}

	/**
	 * Specifies the coordinates of a fuzzy set point.
	 * @param i number of the point 
	 * @param x X coordinate
	 * @param y Y coordinate
	 */
	public void at(int i, double x, double y) {
		if ((i < 0) || (i >= MAXPOINTS)) return;
		
		shape[i].x(x);
		shape[i].y(y);
	}	
	

	/**
	 * Duplicates the fuzzy set
	 * @return another fuzzy set that is a copy of this one.
	 */
	public abstract FSet dupset();
	
	
	/**
	 * Calculates the membership degree of the fuzzy predicate
	 * @param x fuzzy predicate 
	 * @return membership degree
	 */
	public abstract double dmember(double x);  
	
	/**
	 * Discounts the fuzzy set (alpha-cut)
	 * @param alpha discount value
	 * @return a new fuzzy set discounted
	 */
	public abstract FSet alphacut(double alpha) ;
	
	/**
	 * Discounts the fuzzy set (alpha-cut)
	 * @param set the fuzzy set to discount
	 * @param alpha discount value
	 */
	public abstract void alphacut(FSet set, double alpha) ;
	
	/**
	 * Returns the integral of the set (only pointwise sets) for inference
	 * calculations
	 * @return the integral of the set
	 */
	public abstract double integral (); 
	/**
	 * Returns the momnet of the set (only pointwise sets) for inference
	 * calculations
	 * @return the moment of the set
	 */
	public abstract double moment (); 
	
	/**
	 * Returns a string representation of this fuzzy set
	 */
	public abstract String toString();
}
