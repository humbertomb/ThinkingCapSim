/**
 * Created on 25-jun-2007
 *
 * @author Humberto Martinez Barbera
 */
package wucore.utils.math;

public class NumericalRecipes 
{
	static public final double			EPS = 3.0e-11;

	/**
		Determines the Gauss-Legendre weights (wout) and abscissas (xout) such that 
		  +1
		 /
		 | f(x) dx  =  wout  f(xout ) + ... + wout  f(xout )
	 	/               0       0              n-1     n-1
		-1
		where f(x) is any polynomial of degree 2 n - 1 or less. 
		
		The integer scalar n specifies the number of points in the quadrature. 
		The input value of aout does not matter. Its output value is a 
		double-precision column vector of length n containing the abscissas 
		for the quadrature. The input value of wout does not matter. Its output 
		value is a double-precision column vector of length n containing the 
		weights for the quadrature. 
	 */	
	static public void gauleg (double x1, double x2, double xout[], double wout[], int n)
	{
		double		z1, z, xm, xl;
		double		pp, p3, p2, p1;

		z	= 0;
		xm	= 0.5 * (x2 + x1);
		xl	= 0.5 * (x2 - x1);
		for (int i = 0; i < n/2; i++) 
		{
			z	= Math.cos (Math.PI * ((double) i + 0.75) / ((double) n + 1.5));
			do 
			{
				p1	= 1.0;
				p2	= 0.0;
				for (int j = 0; j < n; j++) 
				{
					p3	= p2;
					p2	= p1;
					p1	= (2.0 * j * z * p2 - j * p3) / (j + 1);
				}
				pp	= (n + 1) * (z * p1 - p2) / (z * z - 1.0);
				z1	= z;
				z	= z1 - p1 / pp;
			} while (Math.abs (z - z1) > EPS);
			
			xout[i]		= xm - xl * z;
			wout[i]		= 2.0 * xl / ((1.0 - z * z) * pp * pp);
			
			xout[n-i-1]	= xm + xl * z;
			wout[n-i-1]	= wout[i];
		}
	}
	
	/** Calculates the floating point remainder.
	 * 
	 * The fmod function calculates the floating-point remainder 
	 * F of X / Y such that X = i * Y  + F, where I is an integer, 
	 * F has the same sign as X, and the absolute value of F is 
	 * less than the absolute value of Y. 
	 */
	static public double fmod (double x, double y)
	{
		int i = (int) (x / y);
		return x - i * y;
	}
}
