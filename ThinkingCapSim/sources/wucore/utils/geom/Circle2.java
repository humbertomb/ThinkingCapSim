/*
 * Jose Antonio Marin
 */

package wucore.utils.geom;
 
public class Circle2 extends Object
{
	public Point2			center;					// Center
	public double			radius;					// Radius

	/* Constructors */
	public Circle2 ()
	{
		center	= new Point2 ();
		radius	= 0.0;
	}
	
	public Circle2 (double x, double y, double r)
	{
		this ();
		set (x, y, r);
	}
	
	public Circle2 (Point2 p1, Point2 p2, Point2 p3)
	{
		this ();
		set (p1, p2, p3);
	}
	
	/* Accessor methods */
	public final Point2 	center () 					{ return center; }
	public final double 	radius () 					{ return radius; }

	/* Instance methods */
	public void set (double x, double y, double r)
	{
		center.set (x, y);
		radius = r;
	}
	
	// [OÕRourke (C)] p. 201. Simplified by Jim Ward.
	public void set (Point2 a, Point2 b, Point2 c)
	{
		double			A, B, C, D, E, F, G;
		
		A	= b.x - a.x;
		B	= b.y - a.y;
		C	= c.x - a.x;
		D	= c.y - a.y;
		E	= A*(a.x + b.x) + B*(a.y + b.y);
		F	= C*(a.x + c.x) + D*(a.y + c.y);
		G	= 2*(A*(c.y - b.y)-B*(c.x - b.x));
		
		center.x = (D*E - B*F) / G;
		center.y = (A*F - C*E) / G;
		radius = center.distance (b);
	}
	
	public Point2[] intersectionSeg (Line2 line){
	    Point2[] points = intersection(line);
	    boolean pt0, pt1;
	    if(points != null){
	        pt0 = line.contains(points[0]);
	        
	        if(points.length>1)
	            pt1 = line.contains(points[1]);
	        else
	            pt1 = false;
	        
	        System.out.println("pt0 = "+pt0+" pt1="+pt1);
	        if(pt0 && pt1){
	            return points;
	        }
	        else if(pt0){
	            Point2[] newpoint = new Point2[1];
	            newpoint[0] = points[0];
	            return newpoint;
	        }
	        else if(pt1){
	            Point2[] newpoint = new Point2[1];
	            newpoint[1] = points[1];
	            return newpoint;
	        }
	    }
	    return null;
	}
	
	public Point2[] intersection (Line2 line)
	{
	    // x1,y1  P1 coordinates (point of line)
	    // x2,y2  P2 coordinates (point of line)
	    // x3,y3, r  P3 coordinates and radius (sphere)
	    // x,y,   intersection coordinates
	    //
	    // This function returns a pointer array which first index indicates
	    // the number of intersection point, followed by coordinate pairs.
	    
	    double x1 = line.orig.x;
	    double y1 = line.orig.y;
	    double x2 = line.dest.x;
	    double y2 = line.dest.y;
	    double x3 = center.x;
	    double y3 = center.y;
	    double r = radius;
	    
	    double x , y;
	    double a, b, c, mu, i ;
	    
	    Point2[] p;
	    
	    a =  (x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1);
	    b =  2* ( (x2 - x1)*(x1 - x3) + (y2 - y1)*(y1 - y3) ) ;
	    c =  x3*x3 + y3*y3 + x1*x1 + y1*y1 - 2* ( x3*x1 + y3*y1 ) - r*r;
	    i =   b * b - 4 * a * c ;
	    
	    if ( i < 0.0 )
	    {
	        // no intersection
	        return null;
	    }
	    else if ( i == 0.0 )
	    {
	        // one intersection
	        p = new Point2[1];
	        
	        mu = -b/(2*a) ;
	        x = x1 + mu*(x2-x1);
	        y = y1 + mu*(y2-y1);
	        p[0] = new Point2(x,y);
	        
	        return(p);
	    }
	    else if ( i > 0.0 )
	    {
	        // two intersections
	        p = new Point2[2];
	        
	        // first intersection
	        mu = (-b + Math.sqrt( b*b - 4*a*c )) / (2*a);
	        x = x1 + mu*(x2-x1);
	        y = y1 + mu*(y2-y1);
	        p[0] = new Point2(x,y);
	        
	        // second intersection
	        mu = (-b - Math.sqrt(b*b - 4*a*c )) / (2*a);
	        x = x1 + mu*(x2-x1);
	        y = y1 + mu*(y2-y1);
	        p[1] = new Point2(x,y);
	        return(p);
	    }
	    return null;
	}
	
	
	// Calcula los puntos de interseccion de dos circulos (si existen)
	public Point2[] intersection(Circle2 circle)
	{
		double a, dx, dy, d, h, rx, ry;
		double pt1x, pt1y, pt2x, pt2y;
		double x2, y2;
		
		double x0 = center.x;
		double y0 = center.y;
		double r0 = radius;
		
		double x1 = circle.center.x;
		double y1 = circle.center.y;
		double r1 = circle.radius;
		
		
		/* dx and dy are the vertical and horizontal distances between
		 * the circle centers.
		 */
		dx = x1 - x0;
		dy = y1 - y0;
		
		/* Determine the straight-line distance between the centers. */
		d = Math.sqrt((dy*dy) + (dx*dx));
		
		/* Check for solvability. */
		if (d > (r0 + r1))
		{
			/* no solution. circles do not intersect. */
			return null;
		}
		if (d < Math.abs(r0 - r1))
		{
			/* no solution. one circle is contained in the other */
			return null;
		}
		
		/* 'point 2' is the point where the line through the circle
		 * intersection points crosses the line between the circle
		 * centers.  
		 */
		
		/* Determine the distance from point 0 to point 2. */
		a = ((r0*r0) - (r1*r1) + (d*d)) / (2.0 * d) ;
		
		/* Determine the coordinates of point 2. */
		x2 = x0 + (dx * a/d);
		y2 = y0 + (dy * a/d);
		
		/* Determine the distance from point 2 to either of the
		 * intersection points.
		 */
		h = Math.sqrt((r0*r0) - (a*a));
		
		/* Now determine the offsets of the intersection points from
		 * point 2.
		 */
		rx = -dy * (h/d);
		ry = dx * (h/d);
		
		/* Determine the absolute intersection points. */
		pt1x = x2 + rx;
		pt2x = x2 - rx;
		pt1y = y2 + ry;
		pt2y = y2 - ry;
				
		Point2[] ret = {new Point2(pt1x,pt1y), new Point2(pt2x,pt2y)};
		return ret;
	}
	
	public String toString ()
	{
		return ("[" + center + ", " + radius + "]");
	}
}
