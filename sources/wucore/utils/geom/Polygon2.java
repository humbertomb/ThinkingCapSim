package wucore.utils.geom;

// This class encapsulates a description of a closed, two-dimensional region
// within a coordinate space. This region is bounded by an arbitrary number of
// line segments, each of which is one side of the polygon. Internally, a
// polygon comprises of a list of (x,y) coordinate pairs, where each pair
// defines a vertex of the polygon, and two successive pairs are the endpoints
// of a line that is a side of the polygon. The first and final pairs of (x,y)
// points are joined by a line segment that closes the polygon.
// Such a class includes the contains, intersects and inside methods.

public class Polygon2 {
	
	// The total number of points. The value of npoints represents the number
	// of valid points in this Polygon and might be less than the number of
	// elements in xpoints or ypoints.
	// This value can be NULL.
	public int npoints;
	
	// The array of X coordinates. The number of elements in this array might
	// be more than the number of X coordinates in this Polygon. The extra
	// elements allow new points to be added to this Polygon without
	// re-creating this array. The value of npoints is equal to the number of
	// valid points in this Polygon.
	public double xpoints[];
	
	// The array of Y coordinates.  The number of elements in this array might
	// be more than the number of Y coordinates in this Polygon. The extra
	// elements allow new points to be added to this Polygon without
	// re-creating this array. The value of npoints is equal to the number of
	// valid points in this Polygon. 
	public double ypoints[];
	
	// JDK 1.1 serialVersionUID 
	private static final long serialVersionUID = -6460261437900069969L;
	
	// Default length for xpoints and ypoints.
	private static final int MIN_LENGTH = 3;
	
	// Creates an empty polygon.
	public Polygon2()
	{
		xpoints = new double[MIN_LENGTH];
		ypoints = new double[MIN_LENGTH];
	}
	
	// Constructs and initializes a Polygon from the specified parameters.
	public Polygon2(double xpoints[], double ypoints[], int npoints)
	{
		if (npoints > xpoints.length || npoints > ypoints.length)
		{
			throw new IndexOutOfBoundsException("npoints > xpoints.length || " + "npoints > ypoints.length");
		}
		
		if (npoints < 0)
		{
			throw new NegativeArraySizeException("npoints < 0");
		}
		
		this.npoints = npoints;
		this.xpoints = new double[npoints];
		this.ypoints = new double[npoints];
		System.arraycopy (xpoints, 0, this.xpoints, 0, npoints);
		System.arraycopy (ypoints, 0, this.ypoints, 0, npoints);
//		this.xpoints = Arrays.copyOf(xpoints, npoints);
//		this.ypoints = Arrays.copyOf(ypoints, npoints);
	}
	
	// Resets this Polygon object to an empty polygon. The coordinate arrays
	// and the data in them are left untouched but the number of points is reset
	// to zero to mark the old vertex data as invalid and to start accumulating
	// new vertex data at the beginning.
	// All internally-cached data relating to the old vertices are discarded.
	// Note that since the coordinate arrays from before the reset are reused,
	// creating a new empty Polygon might be more memory efficient than
	// resetting the current one if the number of vertices in the new polygon
	// data is significantly smaller than the number of vertices in the data
	// from before the reset.
	public void reset()
	{
		npoints = 0;
	}
	
	// Translates the vertices of the Polygon by deltaX along the x axis and by
	// deltaY along the y axis.
	public void translate(double deltaX, double deltaY)
	{
		for (int i = 0; i < npoints; i++)
		{
			xpoints[i] += deltaX;
			ypoints[i] += deltaY;
		}
	}
	
	// Appends the specified coordinates to this Polygon.
	public void addPoint(double x, double y)
	{
		if (npoints >= xpoints.length || npoints >= ypoints.length)
		{
			int newLength = npoints * 2;
			// Make sure that newLength will be greater than MIN_LENGTH and
			// aligned to the power of 2
			if (newLength < MIN_LENGTH)
			{
				newLength = MIN_LENGTH;
			}
			//else if ((newLength & (newLength - 1)) != 0) {
			//	newLength = Integer.highestOneBit(newLength);
			//}
			
			double cxpoints[] = new double[newLength];
			double cypoints[] = new double[newLength];
			
			int lastlength = xpoints.length;
			for(int j = 0; j < lastlength; j++)
			{
				cxpoints[j] = xpoints[j];
				cypoints[j] = ypoints[j];
			}
			
			xpoints = new double[newLength];
			ypoints = new double[newLength];
			
			for(int i = 0; i < lastlength; i++)
			{
				xpoints[i] = cxpoints[i];
				ypoints[i] = cypoints[i];
			}
			
			//xpoints = Arrays.copyOf(xpoints, newLength);
			//ypoints = Arrays.copyOf(ypoints, newLength);
		}
		
		xpoints[npoints] = x;
		ypoints[npoints] = y;
		npoints++;
	}
	
	public double getCoGX() {
		double cogx = 0;
		for(int i = 0; i < npoints; i++)
			cogx += xpoints[i];
		
		return cogx/npoints;
	}
	
	public double getCoGY() {
		double cogy = 0;
		for(int i = 0; i < npoints; i++)
			cogy += ypoints[i];
		
		return cogy/npoints;
	}
	
	public boolean contains(double x, double y)
	{
		if (npoints < 3)	return false;
		
		int hits = 0;
		
		double lastx = xpoints[npoints - 1];
		double lasty = ypoints[npoints - 1];
		double curx, cury;
		
		// Walk the edges of the polygon
		for (int i = 0; i < npoints; lastx = curx, lasty = cury, i++)
		{
			curx = xpoints[i];
			cury = ypoints[i];
			
			if (cury == lasty)	continue;
			
			double leftx;
			
			if (curx < lastx)
			{
				if (x >= lastx)		continue;
				leftx = curx;
			} else {
				if (x >= curx)		continue;
				leftx = lastx;
			}
			
			double test1, test2;
			
			if (cury < lasty)
			{
				if (y < cury || y >= lasty)		continue;
				if (x < leftx)
				{
					hits++;
					continue;
				}
				test1 = x - curx;
				test2 = y - cury;
			} else {
				if (y < lasty || y >= cury)		continue;
				if (x < leftx)
				{
					hits++;
					continue;
				}
				test1 = x - lastx;
				test2 = y - lasty;
			}
			
			if (test1 < (test2 / (lasty - cury) * (lastx - curx)))	hits++;
		}
		
		return ((hits & 1) != 0);
	}
	
	// Determines whether the specified Point is inside this Polygon.
	public boolean contains(Point2 p)
	{
		return contains(p.x, p.y);
	}
	
	// Determines whether the specified coordinates are inside this Polygon.
	public boolean contains(int x, int y)
	{
		return contains((double) x, (double) y);
	}
	
	// Returns a string representation of this Polygon.
	public String toString() {
		String rname = new String(getClass().getName());
		
		rname = rname.concat(" num_edges: ").concat(Integer.toString(npoints)).concat(" edges:");
		
		for(int i = 0; i < npoints; i++)
		{
			rname = rname.concat(" <" + xpoints[i] + "," + ypoints[i] + ">");
		}
		
		return rname;
	}
	
	public static void main(String [ ] args)
	{
		Polygon2 pol1 = new Polygon2();
		
		pol1.addPoint(0.0, 0.0);
		pol1.addPoint(1.0, 0.0);
		pol1.addPoint(2.0, 1.0);
		//pol1.addPoint(1.0, 2.0);
		//pol1.addPoint(0.0, 2.0);
		//pol1.addPoint(-1.0, 1.0);
		
		System.out.println(" pol1: " + pol1.toString());
		double xt = 3.9;
		double yt = 0.999;
		System.out.println(" test:<" + xt + "," + yt + ">: " + pol1.contains(xt, yt));
		
		double xp[] = new double[100];
		double yp[] = new double[100];
		
		xp[0] = 0.0;
		yp[0] = 0.0;
		xp[1] = 1.0;
		yp[1] = 0.0;
		xp[2] = 2.0;
		yp[2] = 1.0;
		xp[3] = 1.0;
		yp[3] = 2.0;
		xp[4] = 0.0;
		yp[4] = 2.0;
		xp[5] = -1.0;
		yp[5] = 1.0;
		
		Polygon2 pol2 = new Polygon2(xp,yp,6);
		
		System.out.println(" pol2: " + pol2.toString());
		System.out.println(" test:<" + xt + "," + yt + ">: " + pol2.contains(xt, yt));
	}
}
