package tc.shared.world;

import java.util.StringTokenizer;

import wucore.utils.geom.Polygon2;

public class WMFArea extends WMElement
{
	public Polygon2		polygon;
	public String		texture;
	
	// Constructors
	public WMFArea(){
	}
	
	public WMFArea (String prop, String dtexture)
	{
		StringTokenizer		st;
		int					npoints;
		double				x1, y1;
		
		polygon = new Polygon2();
		
		st		= new StringTokenizer (prop,", \t");
		npoints	= Integer.parseInt(st.nextToken());
		for (int i = 0; i < npoints; i++)
		{
			x1	= Double.parseDouble (st.nextToken());
			y1	= Double.parseDouble (st.nextToken());
			polygon.addPoint(x1, y1);
		}
		
		label	= st.nextToken();
		
		texture	= dtexture;
		if (st.hasMoreTokens())
			texture	= st.nextToken();
	}
	
	// Instance methods
	public String toRawString ()
	{
		String rname = new String(Integer.toString(polygon.npoints));
		for(int i = 0; i < polygon.npoints; i++)
		{
			rname = rname.concat(", " + polygon.xpoints[i] + ", " + polygon.ypoints[i]);
		}
		rname = rname.concat(", ").concat(label).concat(", ").concat(texture);
		
		return rname;
	}
	
	public String toRawString2 ()
	{
		String rname = new String(Integer.toString(polygon.npoints));
		for(int i = 0; i < polygon.npoints; i++)
		{
			rname = rname.concat(", " + polygon.xpoints[i] + ", " + polygon.ypoints[i]);
		}
		rname = rname.concat(", ").concat(label);
		
		return rname;
	}
}
