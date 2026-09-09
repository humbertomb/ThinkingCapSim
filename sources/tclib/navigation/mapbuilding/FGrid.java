/*
 * (c) 1999-2002 Humberto Martinez 
 * (c) 2004 Jose Antonio Marin 
 */
 
package tclib.navigation.mapbuilding;

import tclib.navigation.mapbuilding.lpo.*;
import tclib.utils.fusion.*;

import tc.vrobot.*;
import tc.shared.lps.lpo.*;

import devices.pos.*;
import wucore.utils.geom.*;
import wucore.utils.math.*;
import wucore.utils.math.jama.*;

public class FGrid extends Grid
{
	// Global variables
	public static final int			SAFE_MOTION		= 0;			/* Safe motion map mode			*/
	public static final int			SAFE_PLANNING	= 1;			/* Safe planning map mode			*/

	public static final double		LAMBDA			= 0.8;		/* Dombi accumulation parameter 	*/
	public static final double		Kw				= 0.5;		/* Occupancy constant (sonar)		*/
	public static final double		Ke				= 0.03;		/* Emptyness constant (sonar)		*/
	public static final double		Klw				= 0.75;		/* Occupancy constant (laser)		*/
	public static final double		Kle				= 0.2;		/* Emptyness constant (laser)		*/
	public static final double		H1				= 1.2;		/* Higher interpolation value		*/
	public static final double		H2				= 1.0;		/* Lower interpolation value		*/
	public static final double		H3				= 0.1;		/* Minimum degree of belief		*/
	public static final double		Dp				= 0.15;		/* Sensor overall accuracy		*/
	
	// Parameters of the RIEPFA division method
	public static final int			RIEPFA_MAXSEGS	= 100;		// Maximum number of segments to be generated
	public static final int			RIEPFA_POINTS	= 6;			// Minimum number of points to form a segment
	public static final double		RIEPFA_DIST		= 0.35;		// Maximum distance to constitute a segment (m)
	public static final double		RIEPFA_BREAK		= 0.1;		// Minimum distance to split into two segments (m)
																// This reference is assumed at a distance of 1 m from the sensor.			
	
	// Parametros para la limpiar las zonas entre los segmentos riepfa
	public boolean					updateclean		= true;				// Activa o desactiva la funcion
	public static final double		UPDATE_MAXDIST	= Double.MAX_VALUE;	// Rango maximo del triangulo generado
	public static final int			MAXPOINT		= 25; 				// Puntos maximos de cada triangulo (si excede se divide en varios triangulos)

	
	
//	public static final int			RIEPFA_MAXSEGS	= 100;		// Maximum number of segments to be generated
//	public static final int			RIEPFA_POINTS	= 9;			// Minimum number of points to form a segment
//	public static final double		RIEPFA_DIST		= 0.275;		// Maximum distance to constitute a segment (m)
//	public static final double		RIEPFA_BREAK		= 0.1;		// Minimum distance to split into two segments (m)
	
	// Instance variables								
	protected double					lambda;						// Dombi accumulation operator value

	// Constructors
	public FGrid (FusionDesc fdesc, RobotDesc rdesc, int nx, int ny, double h)
	{
		super (fdesc, rdesc, nx, ny, h);

		int			i;
			
		// Create data structures
		data			= new Point2[rdesc.RAYLRF];		
		lines		= new Line2[RIEPFA_MAXSEGS];	
		
		// Initialise data structures
		data_n		= 0;	
		lines_n		= 0;
		for (i = 0; i < RIEPFA_MAXSEGS; i++)
			lines[i] = new Line2 ();
		
		// Initialise default operation modes
		mode			= SAFE_MOTION;
		lambda		= LAMBDA;
	}
	
	// Accessors
	public final double 		lambda ()				{ return lambda; }
	public final void 		lambda (double l)		{ lambda = l; }
	
	// Instance methods 
	public void set_occupied (int i, int j)
	{
		empty[i][j] 	= EMPTY;
		occupied[i][j]	= FULL;				
		setFree(i, j, FULL);						
	}
	
	public void set_empty (int i, int j)
	{
		empty[i][j] 	= FULL;
		occupied[i][j]	= EMPTY;
        setFree(i, j, EMPTY);
	}

	public void set_unknown (int i, int j)
	{
		empty[i][j] 	= EMPTY;
		occupied[i][j]	= EMPTY;
        setFree(i, j, 0.35);
	}

	/*
	  * Update the maps (E, W and M). The M map (free cells to navigate/plan)
	  * is updated depending on which method is selected.
	  */
	protected void fuzzy_update (int i, int j, double epsilon, double omega)
	{
		double			ambig;
		
		empty[i][j] 		= dombi (empty[i][j], epsilon, lambda);
		occupied[i][j]	= dombi (occupied[i][j], omega, lambda);
		
		switch (mode)
		{
		case SAFE_MOTION:
			setFree(i,j, intersection (1.0 - empty[i][j], occupied[i][j] * occupied[i][j]) );
			break;
		case SAFE_PLANNING:
		default:
			ambig 		= intersection (empty[i][j], occupied[i][j]);
            setFree(i,j, intersection (intersection (1.0 - empty[i][j], occupied[i][j]), 1.0 - ambig));
		}				
	}
	
	/*
	  * Poloni-Ulivi-Vendittelli method
	  * -----------------------------
	  *
	  *  - Define a triangle (p1, p2, p3) based on sensor measurement
	  *  - Compute its 2D bounding box
	  *  - Compute the free area based on previous value and current measure
	  */
	public void update (Position pos, int s, double dist)
	{
		int				i, j;
		double			xx, yy;
		double			xx1, yy1;
		double			xx2, yy2;
		double			a, xs, ys;
		double			xi, yi;
		int				minx, miny;
		int				maxx, maxy;
		double			ga, gb, gc, gd;
		double			rho, nabla;
		double			epsilon, omega;
		double			Aref, Dref;
		double			len, phi, alpha;
		Point2			start;
		Line2			lna, lnb;
		
		if ((dist <= 0.0) || (dist > MAX_RANGE_SON))		return;
		
		// Compute triangle vertex from sonar measure
		len 		= fdesc.virtufeat[s].rho ();
		phi 		= pos.alpha () + fdesc.virtufeat[s].phi ();
		alpha 	= pos.alpha () + fdesc.virtufeat[s].alpha ();
		
		xs 		= pos.x () + len * Math.cos (phi);			// Calculate sensor starting location
		ys 		= pos.y () + len * Math.sin (phi);
		start	= new Point2 (xs, ys);
		
		a		= (fdesc.CONEVIRTU * 1.1  / 2.0);
		xx 		= xs + dist * Math.cos (alpha);				// Calculate sensor detection location
		yy 		= ys + dist * Math.sin (alpha);
		xx1 		= xs + dist * Math.cos (alpha - a);			// Calculate first sensor ending location
		yy1 		= ys + dist * Math.sin (alpha - a);
		xx2 		= xs + dist * Math.cos (alpha + a);			// Calculate second sensor ending location
		yy2 		= ys + dist * Math.sin (alpha + a);
		lna		= new Line2 ();		lna.set (xs, ys, xx, yy);
		lnb		= new Line2 ();
		
		// Compute triangle bounding box
		minx		= ctog_x (Math.min (Math.min (xx1, xx2), xs) - h);
		miny		= ctog_y (Math.min (Math.min (yy1, yy2), ys) - h);
		maxx		= ctog_x (Math.max (Math.max (xx1, xx2), xs) + h);
		maxy		= ctog_y (Math.max (Math.max (yy1, yy2), ys) + h);
		
		Aref		= fdesc.CONEVIRTU * 0.5;
		Dref		= Math.max (Dp, h * 0.5);
		for (j = miny; j <= maxy; j++)
		{
			for (i = minx; i <= maxx; i++)
			{
				if ((i < 1) || (j < 1) || (i > size_x - 2) || ( j > size_y - 2))
					continue;

				// Compute current ray from start to the current cell				
				xi 	= gtoc_x (i);
				yi 	= gtoc_y (j);
					
				lnb.set (start.x (), start.y (), xi, yi);
				rho		= start.distance (xi, yi);
				nabla	= lna.angle (lnb);
				
				if (Math.abs (rho - dist) < Dref)
					ga = 1.0 - ((rho - dist) / Dref) * ((rho - dist) / Dref);
				else
					ga = 0.0;
					
				if (rho < dist - 2.0 * Dref)
					gb = 1.0;
				else
					gb = 0.0;
					
				gc = Math.min (1.0, H1 * Math.exp (rho * (-H2)) + H3);

				if (Math.abs (nabla) < Aref)
					gd = 1.0 - (nabla / Aref) * (nabla / Aref);
				else
					gd = 0.0;
					
				epsilon	= Ke * gb * gc * gd; 
				omega	= Kw * ga * gc * gd;

				fuzzy_update (i, j, epsilon, omega);
			}
		}
	}

	public void update (Position pos, LPOSensorScanner scan)
	{	
		Point2			start;
		SensorPos		s;
		double			cone, delta, len;
		int				j;
		double			k;
		int				k1, k2, kt;
		int				kd1, kd2;
		int				test;		
		double			xs, ys;
		double			xm, ym;
		
		int 			mem = 0;
		double 			angle;
		Point2			pt1, pt2;
		int				cont = 0;
		int 			steps;
		int 			div;
		
		// Check if the sensor has been updated
		if (!scan.active ())							return;
		
		// Prepare variables for debugging purposes
		lines_n	= 0;
			
		// Set up sensor origin location
		s		= fdesc.scanfeat;
		xs 		= pos.x () + s.rho () * Math.cos (pos.alpha () + s.phi ());			
		ys 		= pos.y () + s.rho () * Math.sin (pos.alpha () + s.phi ());	
		start 	= new Point2 (xs, ys);	
					
		// Fill up structures with the laser data in Cartesian coordinates (global)
		cone		= fdesc.CONESCAN * 0.5;		
		delta	= fdesc.CONESCAN / ((double) fdesc.RAYSCAN - 1.0);		
       	k		= -cone;
		for (k = -cone, j = 0; j < fdesc.RAYSCAN; k += delta, j++)
		{
			len	= scan.range[j];
			if (len < 0.0)				len = 0.0;
			if (len > MAX_RANGE_LRF)		len = MAX_RANGE_LRF;
			
       		xm 	= xs + len * Math.cos (pos.alpha () + (s.alpha () + k));
       		ym 	= ys + len * Math.sin (pos.alpha () + (s.alpha () + k)); 
        				
			data[j]	= new Point2 (xm, ym);
		}
		data_n	= fdesc.RAYSCAN;

		// Aply the RIEPFA method to the new data set	
		k1		= 0;
		k2		= fdesc.RAYSCAN - 1;
		while ((k2 - k1) > RIEPFA_POINTS)
		{
			// Find the points that could constitute a segment (between k1 and k2)
			kt		= k2;
			for (j = k1 + 1; j <= k2; j++)
				if (data[j].distance (data[j-1]) > (start.distance (data[j-1]) * RIEPFA_BREAK))	
				{
					kt		= j - 1;
					break;			
				}
				
			// Divide the subset of points into the appropriate segments (between k1 and kt)
			kd1		= k1;
			kd2		= kt;
			while ((kt - kd1) > RIEPFA_POINTS)
			{	
				if ((kd2 - kd1) > RIEPFA_POINTS)
				{
					test 	= riepfa_test (kd1, kd2);
					if (test < 0)
					{
					    if(updateclean && mem < kd1){
					        // Generate intermediate triangles
					    	steps = (int)Math.ceil((double)(kd1-mem)/(double)MAXPOINT);
					    	div =  (kd1 - mem) / steps;
					    	len = UPDATE_MAXDIST;
					        cont = 0;
					    	for(int i = mem; i <= kd1; i++){
					    	    if(scan.range[i]<len)
					    	        len = scan.range[i];
					    	    if(cont>div || i == kd1){
					    	    	angle = Math.atan2(data[mem].y()-start.y(), data[mem].x()-start.x());
							    	pt1 = new Point2(start.x()+len*Math.cos(angle),start.y()+len*Math.sin(angle));
							    	angle = Math.atan2(data[i].y()-start.y(), data[i].x()-start.x());
							    	pt2 = new Point2(start.x()+len*Math.cos(angle),start.y()+len*Math.sin(angle));
							    	// Only update empty map
							    	riepfa_update (pt1, pt2, start, true);
							    	mem = i;
							    	cont = 0;
							    	len = UPDATE_MAXDIST;
					    	    }
					    	    else
					    	    	cont++;
					    	}
					    }
						mem = kd2;
						//riepfa_update (kd1, kd2, start);
						riepfa_update (data[kd1], data[kd2], start, false);
						kd1 = kd2 + 1;
						kd2 = kt;
					} 
					else
						kd2 = test;
				} 
				else
				{
					kd1 = kd2 + 1;
					kd2 = kt;			
				}
			}

			// Continue with the new subset (between kt and k2)
			k1 	= kt + 1;
		}
		
		kd1 = (fdesc.RAYSCAN - 1);
		
		if(updateclean && mem < kd1){
		    //		  Generate intermediate triangles
	    	steps = (int)Math.ceil((double)(kd1-mem)/(double)MAXPOINT);
	    	div =  (kd1 - mem) / steps;
	    	
			len = UPDATE_MAXDIST;
	        cont = 0;
	    	for(int i = mem; i <= kd1; i++){
	    	    if(scan.range[i]<len)
	    	        len = scan.range[i];
	    	    if(cont>div || i == kd1){
	    	    	angle = Math.atan2(data[mem].y()-start.y(), data[mem].x()-start.x());
			    	pt1 = new Point2(start.x()+len*Math.cos(angle),start.y()+len*Math.sin(angle));
			    	angle = Math.atan2(data[i].y()-start.y(), data[i].x()-start.x());
			    	pt2 = new Point2(start.x()+len*Math.cos(angle),start.y()+len*Math.sin(angle));
 			    	// Only update empty map
			    	riepfa_update (pt1, pt2, start, true);
			    	mem = i;
			    	cont = 0;
			    	len = UPDATE_MAXDIST;	
	    	    }
	    	    else
	    	    	cont++;
	    	}
		}
	}

	protected int riepfa_test (int ini, int fin)
	{
		int			i, kmax=0; 
		double		ro, phi, rop, phip;
		double		X1,X2,Y1,Y2,X3,Y3,Xint,Yint;
		double 		dis, max = 0.0;	
		double[][] 	a = new double[2][2], b = new double[2][1]; 
		Matrix		A, B, X;

		X1		=	data[ini].x();
		Y1		=	data[ini].y();
		X2		=	data[fin].x();
		Y2		=	data[fin].y();
							
		phi		=	Math.atan((X2-X1)/(Y1-Y2));
		
		phi 	=	Angles.radnorm_360 (phi);

		if((X1 != 0.0) || (Y1 != 0.0))
			ro	=	X1 * Math.cos(phi) + Y1 * Math.sin(phi);
		else
			ro	=	X2 * Math.cos(phi) + Y2 * Math.sin(phi);

		if (ro<0.){
			ro 		= Math.abs (ro);
			phi 	= Angles.radnorm_360 (phi - Math.PI);
		}

		for (i = ini+1; i < fin-1; i++){
			X3 		= data[i].x();
			Y3 		= data[i].y();
			
			phip		=	Angles.radnorm_360 (phi + .5 * Math.PI);
			rop			=	X3 * Math.cos(phip) + Y3 * Math.sin(phip);
			
			if (rop<0.){
				rop 	= Math.abs (rop);
				phip 	= Angles.radnorm_360(phip - Math.PI);
			}
			
			a[0][0] = Math.cos(phip);
			a[0][1] = Math.sin(phip);	
			a[1][0] = Math.cos(phi);	
			a[1][1] = Math.sin(phi);
			b[0][0]	= rop;				
			b[1][0]	= ro;

			A 		= new Matrix(a);
			B		= new Matrix(b);
			X		= A.solve(B);
		
			Xint	= X.get(0,0);
			Yint	= X.get(1,0);
			dis		= LPOFSegment.dist(X3,Y3,Xint,Yint);
				
			if (dis>max)
			{
				max 	= dis;
				kmax 	= i;
			}
		}			

		if (max < RIEPFA_DIST)
			return -1;
		return kmax;
	}

	// This method is SOMEHOW unefficient. The dounding box method evaluates
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
		double			ga, gb, gc;
		double			epsilon, omega;
		Line2			lna, lnb, lnc;
		double			phi, alpha, beta;
		double			Dref, Aref;
		int				i, j;
		double			xi, yi;
		int				minx, miny;
		int				maxx, maxy;
		double			rho, nabla;
		
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
			if (x2 > x1)			{ vert1 = new Point2 (x2, y2);		vert2 = new Point2 (x1, y1); }
			else if (x2 < x1)	{ vert1 = new Point2 (x1, y1);		vert2 = new Point2 (x2, y2); }
			else if (y1 > y2)	{ vert1 = new Point2 (x2, y2);		vert2 = new Point2 (x1, y1); }
			else					{ vert1 = new Point2 (x1, y1);		vert2 = new Point2 (x2, y2); }
		}
		else if ((y1 < ys) && (y2 < ys))					// Both points in lower half
		{
			if (x2 < x1)			{ vert1 = new Point2 (x2, y2);		vert2 = new Point2 (x1, y1); }
			else if (x2 > x1)	{ vert1 = new Point2 (x1, y1);		vert2 = new Point2 (x2, y2); }
			else if (y1 < y2)	{ vert1 = new Point2 (x2, y2);		vert2 = new Point2 (x1, y1); }
			else					{ vert1 = new Point2 (x1, y1);		vert2 = new Point2 (x2, y2); }
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
		beta		= lna.angle (lnb);
		phi		= Angles.radnorm_180 (alpha + beta * 0.5);

		xx 		= xs + dist * Math.cos (phi);						// Calculate most distant point to be detected
		yy 		= ys + dist * Math.sin (phi);
		
		lna		= new Line2 ();		lna.set (xs, ys, xx, yy);
		lnb		= new Line2 ();
		
		// Compute triangle bounding box
		minx		= ctog_x (Math.min (Math.min (vert1.x (), vert2.x ()), xs) - h);
		miny		= ctog_y (Math.min (Math.min (vert1.y (), vert2.y ()), ys) - h);
		maxx		= ctog_x (Math.max (Math.max (vert1.x (), vert2.x ()), xs) + h);
		maxy		= ctog_y (Math.max (Math.max (vert1.y (), vert2.y ()), ys) + h);
		
		Aref		= beta * 0.5;
		Dref		= Math.max (Dp, h * 0.5);
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
				nabla	= lna.angle (lnb);
				
				// Compute the intersection of the ray with the RIEPFA-generated segment
				xx		= xs + dist * Math.cos (lnb.angle ());
				yy		= ys + dist * Math.sin (lnb.angle ());
				
				lnb.set (xs, ys, xx, yy);
				intsec	= lnb.intersection (lnc);
				
				if (intsec == null)				continue;				
				len		= start.distance (intsec);					// Distance to intersected cell

				// Distance factor for occupancy
				if (Math.abs (rho - len) < 2.0 * Dref)				// Occupied area
					ga = 1.0;
				else if (Math.abs (rho - len) < 3.0 * Dref)			// Borderline area
					ga = 0.5;
				else if (rho < len)									// Unaffected area
				    ga = 0.0;
				else
				    continue;										// No valid point
				
				// Distance factor for emptyness
				if (rho < len - 4.0 * Dref)							// Free area
					gb = 1.0;
				else if (rho < len - 2.5 * Dref)						// Borderline area
					gb = 0.5;
				else													// Unaffected area
					gb = 0.0;
					
				// Angular factor for both emptyness and occupancy
				if (Math.abs (nabla) < Math.abs (Aref))							// Sensor detection cone
					gc = 1.0;
				else													// Unaffected area
					gc = 0.0;
					
				epsilon	= Kle * gb * gc; 
				omega	= Klw * ga * gc;
				
				fuzzy_update (i, j, epsilon, omega);
			}
		}
	}

	protected void riepfa_update (Point2 pt1, Point2 pt2, Point2 start, boolean onlyempty){

	    int				minx, miny;
		int				maxx, maxy;
		int 			i,j;
		int				pt1x, pt2x, pt3x, pt1y, pt2y, pt3y;
		boolean			orient;
		boolean			orient1;
		double			epsilon, omega;
		double			Dref;
		double			ga, gb;
		double 			dist = 0;
		double			x,y;
		Line2			line;
		boolean			near;
		
		// Si construye un segmento con el maximo rango, solo debe limpiar el grid empty, y no ocupar nada
        if(!onlyempty && Math.max(start.distance(pt1),start.distance(pt2)) > (MAX_RANGE_LRF * 0.99)){
            //System.out.println("Segmento" + pt1+ ", "+ pt2 + " supera distancia maxima d1="+start.distance(pt1)+" d2="+start.distance(pt2)+" MAX="+MAX_RANGE_LRF);
            onlyempty = true;
        }
        
		pt1x = ctog_x(pt1.x());
		pt2x = ctog_x(pt2.x());
		pt3x = ctog_x(start.x());
		pt1y = ctog_y(pt1.y());
		pt2y = ctog_y(pt2.y());
		pt3y = ctog_y(start.y());
		
		line		= new Line2(pt1.x(),pt1.y(),pt2.x(),pt2.y());
		Dref		= Math.max (Dp, h * 0.5);
		// Save points for debugging purposes
		lines[lines_n++].set (pt1.x (), pt1.y (), pt2.x (), pt2.y ());
		lines[lines_n++].set (start.x (), start.y (), pt1.x (), pt1.y ());
		lines[lines_n++].set (start.x (), start.y (), pt2.x (), pt2.y ());

		
		// Compute triangle bounding box
		minx		= Math.min (Math.min (pt1x, pt2x), pt3x);
		miny		= Math.min (Math.min (pt1y, pt2y), pt3y);
		maxx		= Math.max (Math.max (pt1x, pt2x), pt3x);
		maxy		= Math.max (Math.max (pt1y, pt2y), pt3y);

		// Check grid limits
		if(minx < 0) minx = 0;
		if(miny < 0) miny = 0;
		if(maxx > size_x - 1) maxx = size_x - 1;
		if(maxy > size_y - 1) maxy = size_y - 1;
		
		/* Algoritmo para calcular si un punto esta dentro de un triangulo 
		 * 
		 * Def: Dado un triangulo ABC y un punto P del plano, P esta en el 
		 * interior de este triangulo si la orientacion de los triangulos ABP, 
		 * BCP y CAP es la misma que la orientacion del triangulo ABC.
		 * 
		 * La orientacion de cada triangulo se determina de acuerdo a la direccion
		 * del movimiento cuando se visitan los vertices en el orden especificado.
		 * Orientacion =   (A.x - C.x) * (B.y - C.y) - (A.y - C.y) * (B.x - C.x)
		 * (positiva si es mayor o igual a 0) 
		 */
				
		// Orientacion del triangulo ABC
		orient = ((pt1x-pt3x)*(pt2y-pt3y) - (pt1y-pt3y)*(pt2x-pt3x)) >= 0;
		
		for (j = miny; j <= maxy; j++){
		    for (i = minx; i <= maxx; i++){
				
		        ga = 0;
		        
		        if(!onlyempty){
				    x = gtoc_x(i);
					y = gtoc_y(j);
				
					// Distance to line
					dist = line.segDistance(x,y);
					
					//	 Distance factor for occupancy
					if (dist < 2.0 * Dref)		// Occupied area
						ga = 1.0;
					else if (dist < 3.0 * Dref)	// Borderline area
						ga = 0.5;
					else						// Unaffected area
						ga = 0.0;
		        }
		        
		        near = true;
		        if(ga == 0){
					// Comprobacion si esta dentro del triangulo //

		            // Orientacion del triangulo (ABP)
					orient1 = ((pt1x-i)*(pt2y-j) - (pt1y-j)*(pt2x-i)) >= 0;
					if(orient != orient1) continue; // Fuera
	
					// Orientacion del triangulo (BCP)
					orient1 = ((pt2x-i)*(pt3y-j) - (pt2y-j)*(pt3x-i)) >= 0;
					if(orient != orient1) continue; // Fuera
					
					// Orientacion del triangulo (CAP)
					orient1 = ((pt3x-i)*(pt1y-j) - (pt3y-j)*(pt1x-i)) >= 0;
					if(orient != orient1) continue; // Fuera
					near = false;
		        }
								
				if(onlyempty){
				    x = gtoc_x(i);
					y = gtoc_y(j);
					// Distance to line
					dist = line.distance(x,y);
				}
					
				if(near){
				    // Orientacion de p1 a p2
				    orient1 = ((pt1x-i)*(pt2y-j) - (pt1y-j)*(pt2x-i)) >= 0;
				    // Si esta cerca pero fuera del triangulo solo actualiza 
				    // el grid de ocupados, no el de vacios
				    if(orient != orient1) { 
				        gb = 0.0;
				        epsilon	= Kle * gb; 
						omega	= Klw * ga;
						fuzzy_update (i, j, epsilon, omega);
						continue;
				    } 				
				}
				
				if (dist > 4.0 * Dref)			// Free area
					gb = 1.0;
				else if (dist > 2.5 * Dref)		// Borderline area
					gb = 0.5;
				else							// Unaffected area
					gb = 0.0;
					
				epsilon	= Kle * gb; 
				omega	= Klw * ga;
				
				fuzzy_update (i, j, epsilon, omega);
				        
		    }
		}
				
	}
	
	/*
	 * Dombi accumulation method
	 */
	protected double dombi (double a, double b, double lambda)
	{
		double			fa, fb;

		fa	= Math.pow ((1.0 / a) - 1.0, -lambda);
		fb	= Math.pow ((1.0 / b) - 1.0, -lambda);
		return 1.0 / (1.0 + Math.pow (fa + fb, -1.0 / lambda));
	}

	/*
	 * Yager accumulation method
	 */
	protected double yager (double a, double b, double p)
	{
		double			fa, fb;

		fa	= Math.pow (a, p);
		fb	= Math.pow (b, p);
		return Math.min (1.0, Math.pow (fa + fb, 1.0 / p));
	}

	/*
	 * T-conorm
	 */
	protected double union (double a, double b)
	{
//		return Math.max (a, b);						// Max operator
//		return a + b - a * b;							// Algebraic sum operator
		return Math.min (1.0, a + b);					// Bounded sum operator
	}

	/*
	 * T-norm
	 */
	protected double intersection (double a, double b)
	{
//		return Math.min (a, b);						// Min operator
		return a * b;								// Algebraic product operator
//		return Math.max (0.0, a + b - 1.0);			// Bounded product operator
	}
}
