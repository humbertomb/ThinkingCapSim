/* ----------------------------------------
	(c) 2000 Humberto Martinez Barbera
   ---------------------------------------- */

package tc.vrobot;

import java.util.*;

import wucore.utils.math.*;

public class FeaturePos extends SensorPos
{
	protected int[]						ndx;
	protected double[]					wgt;
	protected int						n;
	
	protected double					base;
	protected double					cone;
	protected double					range;
    
	/* Constructors */
	public FeaturePos ()
	{
		super ();
		
		this.n	= 0;
	}
	
	/* Accessor methods */
	public final int		 	n () 				{ return n; }
	public final int		 	ndx (int i) 		{ return ndx[i]; }
	public final double		 	wgt (int i) 		{ return wgt[i]; }

	public final double		 	base () 			{ return base; }
	public final double		 	cone () 			{ return cone; }
	public final double		 	range () 			{ return range; }

	public void set_equ (String buff)
	{
		StringTokenizer	st;
		int				i;
		
		if (buff == null)			return;
		
   		st	= new StringTokenizer (buff, ",");
 		try { n 	= new Integer (st.nextToken ()).intValue (); } catch (Exception e) 		{ n = 0; }
  			
  		ndx	= new int[n];
  		wgt	= new double[n];
		for (i = 0; i < n; i++)
		{
 			try { ndx[i] 	= new Integer (st.nextToken ()).intValue (); } catch (Exception e) 		{ ndx[i] = 0; }
 			try { wgt[i] 	= new Double (st.nextToken ()).doubleValue (); } catch (Exception e) 	{ wgt[i] = 0.0; }
		}
	}	

	public void set_shape (double base, double cone, double range)
	{
		this.base	= base;
		this.cone	= cone;
		this.range	= range;
	}
	
	public String toString ()
	{
		String		tmp;
		int			i;
		
		tmp = "sensor " + (phi*Angles.DTOR) + " := ";
		for (i = 0; i < n; i++)
		{
			tmp += wgt[i] + " * virtu[" + ndx[i] + "]";
			if (i < (n - 1))	
				tmp += " + ";
		}
			
		return tmp;
	}
} 