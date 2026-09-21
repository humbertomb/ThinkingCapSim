/**
 * Created on 05-oct-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.blobs;

public class Blob
{
	protected int			x, y;								// center in image coordinates (pix)
	protected int			xmin, xmax, ymin, ymax;				// bounding rectangle (pix)
	protected int			area;								// blob surface (pix*pix)
	protected int			sizex, sizey;						// x and y size (pix)
	protected int			m01, m10;							// first order momentum
	protected int			npixel;								// number of pixels in blob
	protected boolean		valid;

	public final int 		getXMax () 				{ return xmax;	}
	public final int 		getXMin () 				{ return xmin;	}
	public final int 		getYMax () 				{ return ymax;	}
	public final int 		getYMin ()				{ return ymin;	}
	public final int 		getNumPixels () 			{ return npixel; }
	public final int		getArea ()				{ return area; }
	public final double		getDensity ()				{ return npixel / (double) area; }
	public final int 		getSizeX () 				{ return sizex;	}
	public final int 		getSizeY () 				{ return sizey;	}
	public final int 		getX () 					{ return x; }
	public final int 		getY () 					{ return y; }

	public final void 		setXMax (int xmax) 		{ this.xmax = xmax; }
	public final void 		setXMin (int ymin) 		{ this.xmin = ymin;	}
	public final void 		setYMax (int ymax) 		{ this.ymax = ymax;	}
	public final void 		setYMin (int ymin)		{ this.ymin = ymin;	}
	public final void 		setSizeX (int sizex) 		{ this.sizex = sizex;	}
	public final void 		setSizeY (int sizey) 		{ this.sizey = sizey;	}
	public final void 		setX (int x) 			{ this.x = x; }
	public final void 		setY (int y) 			{ this.y = y; }

	public void initialise ()
	{
		xmax	= -Integer.MAX_VALUE;
		xmin	= Integer.MAX_VALUE;
		ymax	= -Integer.MAX_VALUE;
		ymin	= Integer.MAX_VALUE;
		
		x = 0;
		y = 0;
		
		sizex = sizey = 0;				// Initialize size
		m01 = m10 = 0;					// Initialize momentums
		npixel = 0;						// Initialize the number of pixels in this blob
		valid = false;
	}

	public void update (int px, int py)
	{
		npixel ++;
		
		m01 += py;
		m10 += px;
		
		if (ymin > py)		ymin	 = py;
		if (ymax < py)		ymax = py;
		if (xmin > px)		xmin	 = px;
		if (xmax < px)		xmax	 = px;
	}

	public void set (Blob other)
	{
		x		= other.x;
		y		= other.y;
		xmin	= other.xmin;
		xmax	= other.xmax;
		ymin	= other.ymin;
		ymax	= other.ymax;
		area	= other.area;
		sizex	= other.sizex;
		sizey	= other.sizey;
		m01		= other.m01;
		m10		= other.m10;
		npixel	= other.npixel;
	}

	public void merge (Blob other)
	{
		if (xmin > other.xmin)	xmin = other.xmin;
		if (xmax < other.xmax)	xmax = other.xmax;
		if (ymin > other.ymin)	ymin = other.ymin;
		if (ymax < other.ymax)	ymax = other.ymax;
		
		m10		+= other.m10;
		m01		+= other.m01;		
		npixel	+= other.npixel;
		
		computeShape ();
	}
	
	public double distance (Blob other)
	{
		return Math.sqrt ((other.x-x)*(other.x-x) + (other.y-y)*(other.y-y));
	}
	
	public boolean contains (Blob other)
	{
		return (other.xmin >= xmin) && (other.xmax <= xmax) && (other.ymin >= ymin) && (other.ymax <= ymax);
	}
	
	public boolean contains (int x, int y, int gap)
	{
		return (x >= xmin-gap) && (x <= xmax+gap) && (y >= ymin-gap) && (y <= ymax+gap);
	}
	
	public boolean overlaps (Blob other, int gap)
	{
		return contains (other.xmin, other.ymin, gap) || contains (other.xmin, other.ymax, gap) || contains (other.xmax, other.ymin, gap) || contains (other.xmax, other.ymax, gap);
	}
	
	//	 Compute the parameters of a blob
	public void computeShape ()
	{
		// Compute center of blob
		x		= (int) ((double) m10 / (double)npixel);
		y		= (int) ((double) m01 / (double)npixel);
		
		sizex	= xmax - xmin + 1;
		sizey	= ymax - ymin + 1;
		area	= sizex * sizey;
		valid	= true;
	}
	
	public boolean hasWidth ()
	{
		return (xmax != xmin);
	}

	public boolean hasHeight ()
	{
		return (ymax != ymin);
	}
	
	public String toString ()
	{
		return "[BLOB] cs="+x+" cy="+y+" npix="+npixel+" sizx="+sizex+" sizy="+sizey+" dens="+getDensity();
	}
}
