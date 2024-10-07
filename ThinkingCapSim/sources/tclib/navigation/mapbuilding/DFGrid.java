/*
 * (c) 2003 David Herrero Perez, Humberto Martinez 
 */
 
package tclib.navigation.mapbuilding;

import tclib.utils.fusion.*;
import tc.vrobot.*;

import wucore.utils.geom.*;
import wucore.utils.math.*;

public class DFGrid extends FGrid
{
	// Global variables for Dinamic Maps
	public static final int 			DYNAMIC_MY			= 1;		/* My method. 						*/
	public static final int 			DYNAMIC_NORMALIZED	= 2;		/* Normalized Method. 				*/
	public static final int 			DYNAMIC_WEIGHTED		= 3;		/* Normalized Method. 				*/
	
	public static final int 			EMPTY				= 0;
	public static final int 			OCCUPIED				= 1;
	
	public static final double		EPSILON				= 0.15;		/* Empty Set (laser)				*/
	public static final double		OMEGA				= 0.15;		/* Occupied Set (laser)				*/	

	// Instance variables for dynamic maps
	protected double 				threshold;
	protected double 				p;
	protected int 					dynamic_mode 		= DYNAMIC_MY;

	// Constructors
	public DFGrid (FusionDesc fdesc, RobotDesc rdesc, int nx, int ny, double h)
	{
		super (fdesc, rdesc, nx, ny, h);

		int			i;
					
		// Dinamic maps
		double ethreshold, othreshold;
		
		switch (dynamic_mode)
		{
		case DYNAMIC_MY:
			p = 3.0;
		
			// Init parameter for dynamic maps
			ethreshold = EPSILON;
			othreshold = OMEGA;
			
			for (i = 0; i < p; i++)
			{
				ethreshold = union (ethreshold, EPSILON);
				othreshold = union (othreshold, OMEGA);
			}	
			
			threshold = Math.max(ethreshold, othreshold);
			break;
		case DYNAMIC_NORMALIZED:
			threshold = 1.0;			/* Normalized condition A + B > 1 */
			break;
		case DYNAMIC_WEIGHTED:
			threshold = 1.0;			/* Normalized condition A + B > 1 */
			break;
		default:
		}			
	}
		
	// Instance methods 

	/*
	 * Update the maps (E, W and M). The M map (free cells to navigate/plan)
	 * is updated depending on which method is selected.
	 */
	protected void fuzzy_dynamic_update (int i, int j, double value, int set)
	{
		double min, max, k;
	
		switch (set)
		{
		case EMPTY:
			empty[i][j] 	= union (empty[i][j], value);
			break;
		case OCCUPIED:
			occupied[i][j]	= union (occupied[i][j], value);
			break;
		default:
		}
						
        setFree(i,j, intersection (occupied[i][j], 1.0 - empty[i][j]));	
		
		switch (dynamic_mode)
		{
		case DYNAMIC_MY:
			if (intersection(occupied[i][j] ,empty[i][j]) > threshold)	
			{	
				switch (set)
				{
				case EMPTY:
					occupied[i][j]	= intersection (occupied[i][j], threshold);
					break;
				case OCCUPIED:
					empty[i][j] 	= intersection (empty[i][j], threshold);
					break;
				default:
				}
			}
			break;
		case DYNAMIC_NORMALIZED:
			// When O + E > 1, normalize the fuzzy sets.
			if (occupied[i][j] + empty[i][j] > threshold)	
			{
				min = Math.min(occupied[i][j] ,empty[i][j]);
				max = Math.max(occupied[i][j] ,empty[i][j]);
			
				if(occupied[i][j] >= empty[i][j])
				{
					occupied[i][j]	= max - min;
					empty[i][j]		= 0.0;
				} else {
					occupied[i][j]	= 0.0;
					empty[i][j]		= max - min;					
				}
			}		
			break;
		case DYNAMIC_WEIGHTED:
			// When O + E > 1, weight the fuzzy sets.
			if (occupied[i][j] + empty[i][j] > threshold)	
			{
				min = Math.min(occupied[i][j] ,empty[i][j]);
				max = Math.max(occupied[i][j] ,empty[i][j]);
				k 	= min / max;
				
				if(occupied[i][j] >= empty[i][j])
				{
					occupied[i][j]	= k * (max - min);
					empty[i][j]		= 0.0;
				} else {
					occupied[i][j]	= 0.0;
					empty[i][j]		= k * (max - min);					
				}
			}				
			break;
		default:
		}			
	}
	
	// This method is SOMEHOW unefficient. The bounding box method evaluates
	// a lot of positions that are not really needed. In general it takes
	// in average twice the time if the triangle itself is evaluated.
	protected void riepfa_update (int k1, int k2, Point2 start)
	{
		double			xs, ys;
		double			xx, yy;
		double			x1, y1, x2, y2;
		Point2			vert1, vert2;
		Point2			intsec;
		double			dist, len;
		Line2			lna, lnb, lnc;
		double			phi, alpha, beta;
		double			Dref;
		int				i, j;
		double			xi, yi;
		int				minx, miny;
		int				maxx, maxy;
		double			rho;
		
		// Check in which quadrant the RIEPFA boundary points lay on (counter-clockwise)
		//		*- vert1 is the right-most point
		//		*- vert2 is the left-most point
		xs		= start.x ();
		ys		= start.y ();
		x1		= data[k1].x ();
		y1		= data[k1].y ();
		x2		= data[k2].x ();
		y2		= data[k2].y ();
		
		if ((y1 >= ys) && (y2 >= ys))						// Both points in upper half
		{
			if (x2 > x1)		{ vert1 = new Point2 (x2, y2);		vert2 = new Point2 (x1, y1); }
			else if (x2 < x1)	{ vert1 = new Point2 (x1, y1);		vert2 = new Point2 (x2, y2); }
			else if (y1 > y2)	{ vert1 = new Point2 (x2, y2);		vert2 = new Point2 (x1, y1); }
			else				{ vert1 = new Point2 (x1, y1);		vert2 = new Point2 (x2, y2); }
		}
		else if ((y1 < ys) && (y2 < ys))					// Both points in lower half
		{
			if (x2 < x1)		{ vert1 = new Point2 (x2, y2);		vert2 = new Point2 (x1, y1); }
			else if (x2 > x1)	{ vert1 = new Point2 (x1, y1);		vert2 = new Point2 (x2, y2); }
			else if (y1 < y2)	{ vert1 = new Point2 (x2, y2);		vert2 = new Point2 (x1, y1); }
			else				{ vert1 = new Point2 (x1, y1);		vert2 = new Point2 (x2, y2); }
		}
		else if ((y1 >= ys) && (y2 < ys))					// First point up, sencod point down
								{ vert1 = new Point2 (x2, y2);		vert2 = new Point2 (x1, y1); }
		else /* ((y1 <= ys) && (y2 > ys)) */				// First point down, second point up
								{ vert1 = new Point2 (x1, y1);		vert2 = new Point2 (x2, y2); }
		
		// Save points for debugging purposes
		lines[lines_n].set (vert1.x (), vert1.y (), vert2.x (), vert2.y ());	lines_n ++;
		lines[lines_n].set (xs, ys, vert1.x (), vert1.y ());					lines_n ++;
		lines[lines_n].set (xs, ys, vert2.x (), vert2.y ());					lines_n ++;

		// Compute triangle vertex from RIEPFA boundary points
		dist	= Math.max (start.distance (vert1), start.distance (vert2));
		
		lna		= new Line2 ();		lna.set (xs, ys, vert1.x (), vert1.y ());					// Line to first vertex
		lnb		= new Line2 ();		lnb.set (xs, ys, vert2.x (), vert2.y ());					// Line to second vertex
		lnc		= new Line2 ();		lnc.set (vert1.x (), vert1.y (), vert2.x (), vert2.y ());	// Line between segments


		alpha	= lna.angle ();
		beta	= lna.angle (lnb);
		phi		= Angles.radnorm_180 (alpha + beta * 0.5);

		xx 		= xs + dist * Math.cos (phi);						// Calculate most distant point to be detected
		yy 		= ys + dist * Math.sin (phi);
		
		lna		= new Line2 ();		lna.set (xs, ys, xx, yy);
		lnb		= new Line2 ();
		
		// Compute triangle bounding box
		minx	= ctog_x (Math.min (Math.min (vert1.x (), vert2.x ()), xs) - h);
		miny	= ctog_y (Math.min (Math.min (vert1.y (), vert2.y ()), ys) - h);
		maxx	= ctog_x (Math.max (Math.max (vert1.x (), vert2.x ()), xs) + h);
		maxy	= ctog_y (Math.max (Math.max (vert1.y (), vert2.y ()), ys) + h);
		
		Dref	= Math.max (Dp, h * 0.5);
		for (j = miny; j <= maxy; j++)
		{
			for (i = minx; i <= maxx; i++)
			{
				if ((i < 1) || (j < 1) || (i > size_x - 2) || ( j > size_y - 2))
					continue;

				// Compute current ray from start to the current cell				
				xi 	= gtoc_x (i);
				yi 	= gtoc_y (j);
				
				lnb.set (xs, ys, xi, yi);
				rho		= start.distance (xi, yi);					// Distance to current cell
				
				// Compute the intersection of the ray with the RIEPFA-generated segment
				xx		= xs + dist * Math.cos (lnb.angle ());
				yy		= ys + dist * Math.sin (lnb.angle ());
				
				lnb.set (xs, ys, xx, yy);
				intsec	= lnb.intersection (lnc);
				
				if (intsec == null)				continue;				
				len		= start.distance (intsec);					// Distance to intersected cell

				// Distance factor for occupancy
				if (Math.abs (rho - len) < 2.0 * Dref)				// Occupied area
					fuzzy_dynamic_update (i, j, OMEGA, OCCUPIED);

				// Distance factor for emptyness
				if (rho < len - 4.0 * Dref)							// Free area
					fuzzy_dynamic_update (i, j, EPSILON, EMPTY);
			}
		}
	}
}
