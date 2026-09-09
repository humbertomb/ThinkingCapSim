/* 
 * (c)	2002	David Herrero Perez
 */

package tclib.navigation.pathplanning;

import devices.pos.*;

public class BSpline extends Path
{
	final int MAX_LENGTH			= 15000;
  	final double STEPS				= 8.0;					// Constant that define the number of aproximates positions 
  	final double EPSILON			= 0.001;
  	
  	final int 				orderK 				= 4;					//	Order Open B Spline 
  	
	private int[] 			knots;
	
	public BSpline (Path path, double firstangle)
	{
		//super(1000);
		super (10*path.num());
		while(path.num()<orderK) path.add(path.last ());
		refine (path, firstangle);
	}	

  	/* 	Calculates the knots of BSpline, if k is the order of multiplicity of BSpline and
  		n is the number of points, the knots are:
  		
  			ti = 0			if 		0 	<= i < 	k
  			ti = i-k+1		if 	    k 	<= i <= n
  			ti = n-k+2		if 	  	 	   i > 	n	(To npoints + k)
  	 */
  	protected int[] knots (Path path)
  	{
  		int nknot, i;
  		int[] aux; 
  		
  		nknot	= orderK + path.num () + 1;
  		aux		= new int[nknot];  
		
  		aux[0] = 0;
  		for (i = 1; i < nknot; i++)
  		{
  			if ((i >= orderK) && (i <= path.num ()))	
  				aux[i]	= aux[i-1] + 1;
 			else
  				aux[i]	= aux[i-1];
  		}
  			
  		return aux;
  	};

  	protected double[] N (Path path, double t, int[] knots) 
  	{
  		int i, k;
  		double d, e;
  		double[] aux  = new double[path.num ()];
  		double[] temp = new double[knots.length];

	// Calculates basic functions of first order Ni,1
		for (i = 0 ; i < (knots.length - 1); i++){
			if (t >= knots[i] && t < knots[i+1])
				temp[i]	= 1.;
			else
				temp[i]	= 0.;
		}

	// Calculates basic functions of greater order Ni,k
		for (k = 2 ; k <= orderK; k++){
			for (i=0; i < knots.length - k; i++){ 	
				if (temp[i] != 0.) {
					if ( (knots[i+k-1] - knots[i]) == 0.0)
						d = 0.;
					else
						d = ( (t - knots[i]) * temp[i]) / (knots[i+k-1] - knots[i]);
				}
				else
					d = 0.;
					
				if (temp[i+1] != 0.){
					if ( (knots[i+k] - knots[i+1]) == 0.)
						e = 0.;
					else
						e = ( (knots[i+k] - t) * temp[i+1]) / (knots[i+k] - knots[i+1]);
				}
				else
					e = 0.;
					
				temp[i] = d + e;
			}
		}
				
		for (i=0; i < path.num (); i++)
		 	aux[i] = temp[i];

		if (((double) knots[knots.length - 1] - t) < (2. * EPSILON) )
			aux[path.num () - 1] = 1.0;

    	return aux;
  	}

	protected void refine (Path path, double firstangle)
	{
  		int i, k = 0;
  		boolean firstime;
  		double x, y, t, ta;
  		double[] Naux;
  		Position		tpos;
  		
  		tpos	= new Position ();
		reset ();
		
		if (path == null)				return;
		
    		knots = knots (path);

		if ((knots[knots.length - 1] <= 0))
			return;
		
		ta = (double) knots[knots.length - 1] / ( (path.num () - 1) * STEPS);		
		firstime = true;
		
		for (t = 0.; t <= (double) knots[knots.length - 1] ; t += ta)
		{	
			Naux = N(path, t, knots);
			x = 0.; 	y = 0.;
			for (i = 0; i < path.num (); i++)
			{
				x += Naux[i] * path.at (i).x();
				y += Naux[i] * path.at (i).y();
			}

			if (firstime)
			{
				tpos.set (x, y, firstangle);
				k = p_num;
				add (tpos);
				firstime = false;	
			} else 
			{
				//System.out.println ("tpos="+tpos+" at(k)="+at(k));
				tpos.set (x, y, Math.atan2 (y - at (k).y (), x - at (k).x ()));
				k = p_num;
				add (tpos);
			}
		}	
			
	}
}