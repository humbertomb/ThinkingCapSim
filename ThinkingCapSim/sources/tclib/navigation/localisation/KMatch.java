/*
 * (c) 2002 Humberto Martinez
 */
 
package tclib.navigation.localisation;

public class KMatch extends Object
{	
	public static final int				MAX_MATCHES	= 100;		// Maximum number of matching segments

	// Data for storing matchings
	public int							n;						// Number of matching measures
	public int							segs;					// Number of different matching segments
	
	public double[][]					hx;						// Matched hx matrix
	public double[]						r;						// Matched r matrix
	public double[]						v;						// Matched v matrix
	public int[]						s;						// Matched segment
	
	// Local computation variables
	private int[]						ds;						// Unique matched segments
	
	// Constructors
	public KMatch ()
	{
		hx		= new double[MAX_MATCHES][3];
		r		= new double[MAX_MATCHES];
		v		= new double[MAX_MATCHES];	
		s		= new int[MAX_MATCHES];	
		ds		= new int[MAX_MATCHES];	
		
		n		= 0;
		segs	= 0;
	}
	
	// Instance methods
	public void compute_matches ()
	{
		int			i, j;
		boolean		diff;
		
		if (n == 0)
		{
			segs	= 0;
			return;
		}
		
		segs	= 1;
		ds[0]	= s[0];
		for (i = 1; i < n; i++)
		{
			diff	= true;
			for (j = 0; j < segs; j++)
				if (s[i] == ds[j])		diff = false;
				
			if (diff)
			{
				ds[segs]	= s[i];
				segs ++;
			}
		}
	}
}