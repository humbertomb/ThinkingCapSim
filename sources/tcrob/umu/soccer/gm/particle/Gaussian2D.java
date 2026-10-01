/**
 * Created on 26-jun-2006
 *
 * @author Humberto Martinez
 */
package tcrob.umu.soccer.gm.particle;

public class Gaussian2D
{
	static public final double			DEFAULT_VAR	= 1E8;
	static public final double			EPS			= .1/(DEFAULT_VAR*DEFAULT_VAR);
	static public final double			MERGE_OFFSET	= 20;

	// Representation of Gaussian:
	// exp(a0 -1/2*((v[0]-x,v[1]-y)'*inv([cxx cxy; cxy cyy])(v[0]-x,v[1]-y)
	//      -1/2*log(cDet)-log(2*pi)+1/2*a0) + 1/L^2
	
	
	protected double			x, y;
	protected double			cxx, cyy, cxy, cDet;
	protected double			a0;
	
	private double[]			output = new double[3];

	public Gaussian2D ()
	{
		clear ();
		setLogAmplitude (0);
	}

	public Gaussian2D (int x, int y)
	{
		clear ();
		setMean (x, y);
		setLogAmplitude (0);
	}

	public void clear ()
	{
		x = y = 0;
		cxx = cyy = DEFAULT_VAR;
		cxy = 0;
		cDet = cxx*cyy-cxy*cxy;
		a0 = 0;
	}
	
	public double[] getMean ()
	{
		output[0] = x;
		output[1] = y;
		
		return output;
	}

	public double[] getCovariance ()
	{
		output[0] = cxx;
		output[1] = cyy;
		output[2] = cxy;
		
		return output;
	}

	public double[] getCovarianceAxis()
	{
		double s1, s2, alpha;
		double cTrace = cxx+cyy;
		double qFactor = Math.sqrt(cTrace*cTrace-4*cDet);
		
		s1 = .5*(cTrace-qFactor);
		s2 = .5*(cTrace+qFactor);
		
		if ((cxy == 0) && (cxx == cyy))
			alpha = 0;
		else
			alpha = Math.atan2(cxx-cxy-s1, cyy-cxy-s1);
		
		s1 = Math.sqrt(s1);
		s2 = Math.sqrt(s2);

		output[0] = s1;
		output[1] = s2;
		output[2] = alpha;

		return output;
	}

	public double		getLogAmplitude () 					{ return a0; }
	public void			setLogAmplitude (double a) 			{ a0 = a; }
	public double		getX() 								{ return x; }
	public double		getY() 								{ return y; }

	public double getLogLikelihood (double vx, double vy)
	{
		double dx = vx-x, dy = vy-y;
		
		double axx = cyy/cDet;
		double ayy = cxx/cDet;
		double axy = -cxy/cDet;
		
		return -.5*(axx*dx*dx+ayy*dy*dy+2*axy*dx*dy);
	}

	public double getError ()
	{
		double cTrace = cxx+cyy;
		return Math.sqrt(cTrace);
	}

	public void setMean (double vx, double vy)
	{
		x = vx;
		y = vy;
	}

	public void checkCovariance ()
	{
		if ((cxx <= 0) || (cxx > DEFAULT_VAR) || (cyy <= 0) || (cyy > DEFAULT_VAR))
		{
			cxx = DEFAULT_VAR;
			cyy = DEFAULT_VAR;
			cxy = 0;
		}
		
		cDet = cxx*cyy-cxy*cxy;
		if (cDet < EPS)
		{
			double t = (1-cDet)/(cxx+cyy);
			cxx += t;
			cyy += t;
			cDet = cxx*cyy-cxy*cxy;
		}
	}

	public void setCovariance(double vxx, double vyy, double vxy)
	{
		cxx = vxx;
		cyy = vyy;
		cxy = vxy;
		
		checkCovariance();
	}

	public void setCovarianceAxis(double s1, double s2, double alpha)
	{
		s1 = s1*s1;
		s2 = s2*s2;
		
		double cosa = Math.cos(alpha);
		double sina = Math.sin(alpha);
		
		cxx = s1*cosa*cosa+s2*sina*sina;
		cyy = s1*sina*sina+s2*cosa*cosa;
		cxy = (s1-s2)*cosa*sina;
		
		checkCovariance();
	}

	public void addToCovariance(double sigma)
	{
		cxx += sigma*sigma;
		cyy += sigma*sigma;
		
		checkCovariance();
	}

	public void translate(double dx, double dy)
	{
		x += dx;
		y += dy;
	}

	public void invertMean()
	{
		x = -x;
		y = -y;
	}

	public void rotate(double a)
	{
		double xt = x, yt = y;
		double cxxt = cxx, cyyt = cyy, cxyt = cxy;
		
		x = xt*Math.cos(a)-yt*Math.sin(a);
		y = xt*Math.sin(a)+yt*Math.cos(a);
		
		cxx = cxxt*Math.cos(a)*Math.cos(a)-2*cxyt*Math.cos(a)*Math.sin(a)+cyyt*Math.sin(a)*Math.sin(a);
		cyy = cxxt*Math.sin(a)*Math.sin(a)+2*cxyt*Math.cos(a)*Math.sin(a)+cyyt*Math.cos(a)*Math.cos(a);
		cxy = (cxxt-cyyt)*Math.cos(a)*Math.sin(a)+cxyt*(Math.cos(a)*Math.cos(a)-Math.sin(a)*Math.sin(a));
	}

	public void set (Gaussian2D other)
	{
		x		= other.x;
		y		= other.y;
		cxx		= other.cxx;
		cyy		= other.cyy;
		cxy		= other.cxy;
		cDet		= other.cDet;
		a0		= other.a0;
	}

	public void add (Gaussian2D rhs)
	{
		x += rhs.x;
		y += rhs.y;
		
		cxx += rhs.cxx;
		cyy += rhs.cyy;
		cxy += rhs.cxy;
		
		checkCovariance();
	}

	public void substract (Gaussian2D rhs)
	{
		x -= rhs.x;
		y -= rhs.y;
		
		cxx += rhs.cxx;
		cyy += rhs.cyy;
		cxy += rhs.cxy;
		
		checkCovariance();
	}

	/**
	 * How well two Gaussians agree: the log of the density of the difference of
	 * their means under the sum of their covariances, which is the likelihood of
	 * one given the other (the log of the integral of their product).
	 */
	public double logOverlap (Gaussian2D rhs)
	{
		double		sxx = cxx + rhs.cxx, syy = cyy + rhs.cyy, sxy = cxy + rhs.cxy;
		double		det = sxx * syy - sxy * sxy;

		if (det <= 0.0)			return Double.NEGATIVE_INFINITY;
		return -0.5 * distance2 (rhs) - 0.5 * Math.log (det) - Math.log (2.0 * Math.PI);
	}

	/**
	 * How far apart two Gaussians are, in deviations: the squared Mahalanobis
	 * distance of the difference of their means under the sum of their
	 * covariances (around 2 when they agree, much more when they do not).
	 */
	public double distance2 (Gaussian2D rhs)
	{
		double		sxx = cxx + rhs.cxx, syy = cyy + rhs.cyy, sxy = cxy + rhs.cxy;
		double		det = sxx * syy - sxy * sxy;
		double		dx = x - rhs.x, dy = y - rhs.y;

		if (det <= 0.0)			return Double.POSITIVE_INFINITY;
		return (syy * dx * dx - 2.0 * sxy * dx * dy + sxx * dy * dy) / det;
	}

	/**
	 * This Gaussian times another (the fusion of two estimates of the same
	 * position): the mean and covariance of their product, and the log amplitude
	 * added up with how well they agreed ({@link #logOverlap}).
	 */
	public void multiply (Gaussian2D rhs)
	{
		double x0, y0, cxx0, cyy0, cxy0, cDet0;
		double overlap = logOverlap (rhs);
		x0 = x;
		y0 = y;
		cxx0 = cxx;
		cyy0 = cyy;
		cxy0 = cxy;
		cDet0 = cDet;
		
		double ax, ay, axx, ayy, axy;
		axx = cyy0/cDet0;
		ayy = cxx0/cDet0;
		axy = -cxy0/cDet0;
		ax = axx*x0 + axy*y0;
		ay = axy*x0 + ayy*y0;
		
		double bx, by, bxx, byy, bxy;
		bxx = rhs.cyy/rhs.cDet;
		byy = rhs.cxx/rhs.cDet;
		bxy = -rhs.cxy/rhs.cDet;
		bx = bxx*rhs.x + bxy*rhs.y;
		by = bxy*rhs.x + byy*rhs.y;
		
		double invDet = (axx+bxx)*(ayy+byy)-(axy+bxy)*(axy+bxy);
		//if (invDet < 1) invDet = 1;
		cxx = (ayy+byy)/invDet;
		cyy = (axx+bxx)/invDet;
		cxy = -(axy+bxy)/invDet;
		
		checkCovariance();
		
		x = cxx*(ax+bx) + cxy*(ay+by);
		y = cxy*(ax+bx) + cyy*(ay+by);
		
		a0 += rhs.a0 + overlap;
	}

	public void merge (Gaussian2D rhs, double factor) // = 0.5)
	{
		Gaussian2D product = new Gaussian2D ();
		
		product.set (this);
		product.multiply (rhs);
		
		double aCurrent = getLogAmplitude();
		double aRhs = rhs.getLogAmplitude();
		double aProduct = product.getLogAmplitude() + MERGE_OFFSET;
		
		if (aCurrent > 0) aCurrent *= factor;
		
		if ((aProduct > aCurrent) && (aProduct > aRhs))
		{
			set (product);
			setLogAmplitude(aProduct);
		} 
		else if (aRhs > aCurrent) 
		{
			set (rhs);
			setLogAmplitude(aRhs);
		} else
			setLogAmplitude(aCurrent);
	}

	public void limitLogAmplitude(double maxLogAmplitude)
	{
		if (getLogAmplitude() > maxLogAmplitude)
			setLogAmplitude(maxLogAmplitude);
	}

	public void addLogAmplitude(double c)
	{
		setLogAmplitude(getLogAmplitude()+c);
	}
	
	public String toString ()
	{
		return "mean="+(int)x+","+(int)y+" cov="+(int)cxx+","+(int)cyy+" log="+a0;
	}
}
