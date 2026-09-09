package tc.shared.world;

import java.io.PrintWriter;
import java.util.Properties;

import wucore.utils.geom.Polygon2;

public class WMFAreas
{
	protected WMFArea[]	fareas;
	
	private String defTexture		= "./conf/3dmodels/textures/farea.jpg";
	
	// Constructors
	public WMFAreas (Properties props)
	{
		fromProperties (props);
	}
	
	// Accessors
	public final int	 	n () 		{ return fareas.length; }
	public final WMFArea[]	areas ()	{ return fareas; }
	
	public final String	 	defaultTexture () 	{ return defTexture; }
	
	// Instance methods
	public WMFArea at (int i)
	{
		if ((i < 0) || (i >= fareas.length))
			return null;
		return fareas[i];
	}
	
	public void fromProperties (Properties props)
	{
		int					i;
		String				prop;
		
		fareas	= new WMFArea[Integer.parseInt (props.getProperty ("FAREAS", "0"))];
		
		if ((prop = props.getProperty ("FAREA_DEF_TEXTURE")) != null)	
			defTexture	= prop;
		
		for (i = 0; i < fareas.length; i++)
		{
			prop		= props.getProperty ("FAREA_"+i);	
			fareas[i]	= new WMFArea (prop, defTexture);
		}
	}
	
	public void toProperties (Properties props)
	{
		int			i;
		
		props.setProperty ("FAREAS",Integer.toString (fareas.length));
		
		for (i = 0; i < fareas.length; i++)
			props.setProperty ("FAREA_"+i, fareas[i].toRawString ());
	}
	
	public void toFile (PrintWriter out)
	{
		int i;
		
		// Print Line Segments
		out.println("# ==============================");
		out.println("# NON NAVIGABLE AREAS");
		out.println("# ==============================");
		out.println ("FAREAS = " + fareas.length);	
		out.println ();
		out.println ("FAREA_DEF_TEXTURE = "+defTexture);
		out.println ();
		
		for (i = 0; i < fareas.length; i++) 
			if(fareas[i].texture.equals(defTexture))
				out.println ("FAREA_" + i + " = " + fareas[i].toRawString2 ());
			else
				out.println ("FAREA_" + i + " = " + fareas[i].toRawString ());
		out.println ();
	}
	
	public String toString()
	{
		String rname = new String();
		rname = rname.concat(" ==============================\n");
		rname = rname.concat(" NON NAVIGABLE AREAS\n");
		rname = rname.concat(" ==============================\n");
		rname = rname.concat(" FAREAS = " + fareas.length + "\n");
		
		for (int i = 0; i < fareas.length; i++)
		{
			rname = rname.concat("FAREA_" + i + " = " + fareas[i].toRawString () + "\n");
		}
		
		return rname;
	}
	
	public Polygon2[] getPolygons(){
		Polygon2[] polygons = new Polygon2[fareas.length];
		
		for(int i = 0; i < fareas.length; i++)
			polygons[i] = fareas[i].polygon;
		
		return polygons;
	}
	
}
