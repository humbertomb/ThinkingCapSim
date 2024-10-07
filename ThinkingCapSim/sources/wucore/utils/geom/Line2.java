/*
 * (c) 1997-2001 Humberto Martinez
 */
 
package wucore.utils.geom;

import java.io.Serializable;

import wucore.utils.math.Angles;

public class Line2 extends Object implements Serializable
{
	public static final double		ROFFSET		= 0.01;		// Reflection offset (from wall)

	public Point2				orig;
	public Point2				dest;

	/* Constructors */
	public Line2 ()
	{
		this.create ();
	}
	
	public Line2 (double x1, double y1, double x2, double y2)
	{
		this.create ();
		this.set (x1, y1, x2, y2);
	}
	
	/* Accessor methods */
	public final Point2 	orig () 					{ return orig; }
	public final Point2 	dest () 					{ return dest; }

	/* Instance methods */
	protected void create ()
	{
		orig	= new Point2 ();
		dest	= new Point2 ();
	}
			
	public void set (double x1, double y1, double x2, double y2)
	{
		orig.set (x1, y1);
		dest.set (x2, y2);
	}
	
	public void set (Point2 orig, Point2 dest)
	{
		this.orig.set (orig);
		this.dest.set (dest);
	}
	
	public void set (Line2 line)
	{
		orig.set (line.orig);
		dest.set (line.dest);
	}
	
	public final double theta()
	{
		return Math.atan( (orig.x - dest.x) / (dest.y - orig.y) );
	}
	
	public final double rho() 
	{
		double theta = theta();
		return dest.x*Math.cos(theta) + dest.y*Math.sin(theta); 
	}
	
	public final double lenght() 
	{
		double dx = orig.x - dest.x;
		double dy = orig.y - dest.y;
		
		return Math.sqrt(dx*dx + dy*dy);
	}

	/* ------------------------------------------------------------------
	
    	Let the point be C (Cx,Cy) and the line be AB (Ax,Ay) to (Bx,By).
		Let P be the point of perpendicular projection of C onto AB.

				L = sqrt( (Bx-Ax)^2 + (By-Ay)^2 )

				    (Ay-Cy)(Ay-By)-(Ax-Cx)(Bx-Ax)
				r = -----------------------------
				                L^2
				                
				    (Ay-Cy)(Bx-Ax)-(Ax-Cx)(By-Ay)
				s = -----------------------------
				                L^2

				Px = Ax + r(Bx-Ax)
				Py = Ay + r(By-Ay)

				|A - P| = r*L.
				|C - P| = s*L.
   
				s<0      C is left of AB
				s>0      C is right of AB
				s=0      C is on AB
				r=0      P = A
				r=1      P = B
				r<0      P is on the backward extension of AB
				r>1      P is on the forward extension of AB
				0<r<1    P is interior to AB
				
    ------------------------------------------------------------------ */

	public double distance (double x, double y)
	{
		double			r1, r2;
		double			s1, s2;
		double			r, s, l;
		
		l 	= orig.distance (dest);
		r1 	= (orig.y - y) * (orig.y - dest.y);
		r2 	= (orig.x - x) * (dest.x - orig.x);
		r 	= (r1 - r2) / (l * l);
		if ((r < 0.0) || (r > 1.0)) return Double.MAX_VALUE;
		
		s1 	= (orig.y - y) * (dest.x - orig.x);
		s2 	= (orig.x - x) * (dest.y - orig.y);
		s 	= (s1 - s2) / l;
		
		return Math.abs (s);
	}	

	// Distancia del punto (x,y) al segmento
	public double segDistance (double x, double y)
	{
		double			r1, r2;
		double			s1, s2;
		double			r, s, l;
		
		l 	= orig.distance (dest);
		r1 	= (orig.y - y) * (orig.y - dest.y);
		r2 	= (orig.x - x) * (dest.x - orig.x);
		r 	= (r1 - r2) / (l * l);
		if ((r < 0.0) || (r > 1.0)) return Math.min(orig.distance(x,y), dest.distance(x,y)); //Double.MAX_VALUE;
		
		s1 	= (orig.y - y) * (dest.x - orig.x);
		s2 	= (orig.x - x) * (dest.y - orig.y);
		s 	= (s1 - s2) / l;
		
		return Math.abs (s);
	}
	
	// Minima distancia de un segmento a otro
	public double segDistance (Line2 line)
	{
		double d1 = Math.min(
		        segDistance(line.orig.x,line.orig.y),
		        segDistance(line.dest.x,line.dest.y)
		        );
		double d2 = Math.min(
		        line.segDistance(orig.x,orig.y),
		        line.segDistance(dest.x,dest.y)
		        );
		return Math.min(d1,d2);
	}

	public double distance (Point2 pt)
	{
		return distance (pt.x, pt.y);
	}

	public Point2 center ()
	{
		double		x, y;
		double		d, a;
		
		d	= orig.distance (dest) / 2.0;
		a	= angle ();
		
		x	= orig.x + d * Math.cos (a);
		y	= orig.y + d * Math.sin (a);
		
		return new Point2 (x, y);
	}
	
	/* ------------------------------------------------------------------
	
    	Let A,B,C,D be 2-space position vectors.  Then the directed line
    	segments AB & CD are given by:

        	AB=A+r(B-A), r in [0,1]
        	CD=C+s(D-C), s in [0,1]

        	    (Ay-Cy)(Dx-Cx)-(Ax-Cx)(Dy-Cy)
        	r = -----------------------------  (eqn 1)
        	    (Bx-Ax)(Dy-Cy)-(By-Ay)(Dx-Cx)

        	    (Ay-Cy)(Bx-Ax)-(Ax-Cx)(By-Ay)
        	s = -----------------------------  (eqn 2)
        	    (Bx-Ax)(Dy-Cy)-(By-Ay)(Dx-Cx)

    	Let P be the position vector of the intersection point, then

        	Px=Ax+r(Bx-Ax)
        	Py=Ay+r(By-Ay)

    	By examining the values of r & s, you can also determine some
    	other limiting conditions:

        	If 0<=r<=1 & 0<=s<=1, intersection exists
            If r<0 or r>1 or s<0 or s>1 line segments do not intersect

        If the denominator in eqn 1 is zero, AB & CD are parallel
        If the numerator in eqn 1 is also zero, AB & CD are coincident

    ------------------------------------------------------------------ */

	public Point2 intersection (double Ax, double Ay, double Bx, double By)
	{
		Point2			p;
		double			Cx, Dx;
		double			Cy, Dy;
		double			rn, rd;
		double			sn, sd;
		double			r, s;
		
		Cx = orig.x;			Cy = orig.y;
		Dx = dest.x;			Dy = dest.y;
		
		rn = (Ay-Cy) * (Dx-Cx) - (Ax-Cx) * (Dy-Cy);
		rd = (Bx-Ax) * (Dy-Cy) - (By-Ay) * (Dx-Cx);
		if ((rd == 0.0) && (rn == 0.0))
			return new Point2 (Ax, Ay);		// Coincident

		if (rd == 0.0) return null;			// Parallel
		r  = rn / rd;
		
		sn = (Ay-Cy) * (Bx-Ax) - (Ax-Cx) * (By-Ay);
		sd = (Bx-Ax) * (Dy-Cy) - (By-Ay) * (Dx-Cx);
		if (sd == 0.0) return null;			// No intersection
		s  = sn / sd;

		if ((r < 0.0) || (r > 1.0) || (s < 0.0) || (s > 1.0)) 
			return null;						// No intersection
		
		p = new Point2 ();
		p.x (Ax + r * (Bx - Ax));
		p.y (Ay + r * (By - Ay));
		
		return p;							// Intersection
	}	

	public Point2 intersection (Point2 orig, Point2 dest)
	{
		return intersection (orig.x, orig.y, dest.x, dest.y);
	}
	
	public Point2 intersection (Line2 line)
	{
		return intersection (line.orig.x, line.orig.y, line.dest.x, line.dest.y);
	}
	
	public double angle ()
	{
		double		x1, y1;
		double		a1;
		
		x1	= dest.x - orig.x;
		y1	= dest.y - orig.y;					
		a1 	= Math.atan2 (y1, x1);

		return a1;
	}
	
	public double angle_norm ()
	{
		double		x1, y1;
		double		a1;
		
		if (dest.distance (0.0, 0.0) < orig.distance (0.0, 0.0))
		{
			x1	= orig.x - dest.x;
			y1	= orig.y - dest.y;	
		}
		else
		{
			x1	= dest.x - orig.x;
			y1	= dest.y - orig.y;					
		}
		a1 	= Math.atan2 (y1, x1);

		return a1;
	}
	
	public double angle (Line2 line)
	{
		double		a1, a2;
		
		a1 	= angle ();
		a2	= line.angle ();
		
		return Angles.radnorm_180 (a2 - a1);
	}

	public double angle_norm (Line2 line)
	{
		double		a1, a2;
		
		a1 	= angle_norm ();
		a2	= line.angle_norm ();
		
		return Angles.radnorm_180 (a2 - a1);
	}

	public Line2 reflection (Point2 point, double alpha, double len)
	{
		Line2		line;
		double		x1, y1;
		double		x2, y2;
		double		beta;
		
		beta	= angle ();
		x1		= point.x + ROFFSET * Math.cos (beta + alpha);
		y1		= point.y + ROFFSET * Math.sin (beta + alpha);
		x2		= point.x + len * Math.cos (beta + alpha);
		y2		= point.y + len * Math.sin (beta + alpha);

		line	= new Line2 ();
		line.set (x1, y1, x2, y2);
		
		return line;
	}
	
	public boolean contains (Point2 pt)
	{
	    double num = (pt.x - orig.x) * (dest.x - orig.x) + (pt.y - orig.y) * (dest.y - orig.y);
        double den = (dest.x - orig.x) * (dest.x - orig.x) + (dest.y - orig.y) * (dest.y - orig.y);
        System.out.println("num="+num+" den="+den+" u = "+num/den);
        if(den != 0.0 && num/den >= 0 && num/den <= 1)
            return true;
         return false;
	}
	
	public String toString ()
	{
		return "[" + orig + ", " + dest + "]";
	}
	
	public String toRawString ()
	{
		return orig.toRawString () + ", " + dest.toRawString ();
	}
	
	public String toRoundString ()
	{
		return orig.toRoundString () + ", " + dest.toRoundString ();
	}
}
