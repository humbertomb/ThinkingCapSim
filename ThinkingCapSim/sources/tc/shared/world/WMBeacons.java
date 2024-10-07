/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import java.io.PrintWriter;
import java.util.Properties;
import java.util.Vector;

import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.Entity;
import wucore.utils.dxf.entities.LineDxf;
import wucore.utils.dxf.entities.TextDxf;
import wucore.utils.dxf.sections.ACADColor;
import wucore.utils.dxf.sections.Layer;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMBeacons
{
	
	protected WMBeacon[]			beacons;

	// Constructors
	public WMBeacons (Properties props)
	{
		fromProperties (props);
	}
	
	public WMBeacons (DXFWorldFile dxf){
	    Vector entities = dxf.getEntities();
	    Vector beac = new Vector();
	    Entity entity;
	    for(int i = 0; i<entities.size(); i++){
	        entity = (Entity)entities.get(i);
	        if(entity.getLayer().equalsIgnoreCase("BEACONS")){
	            if(entity instanceof TextDxf) 
	                beac.add(new WMBeacon((TextDxf) entity));  
	            if(entity instanceof LineDxf)
	                beac.add(new WMBeacon((LineDxf) entity));
	        }
	    }
	    beacons = new WMBeacon[beac.size()];
	    beac.toArray(beacons);
	}
	
	// Accessors
	public final int	 		n () 				{ return beacons.length; }
	public final WMBeacon[]	edges ()			{ return beacons; }
	
	// Instance methods
	public WMBeacon at (int i)
	{
		if ((i < 0) || (i >= beacons.length)) return null;
		return beacons[i];
	}
	
	public int index (String label)
	{
		int  		i;
		
		if ((label == null) || (beacons == null))			return -1;

		for (i = 0; i < beacons.length; i++)
			if (label.equals (beacons[i].label))			return i;

		return -1;
	}
	
    public int crossline(Line2 line){
    	return crossline(line.orig ().x (), line.orig ().y (), line.dest ().x (), line.dest ().y ());
    }
    
    public int crossline (double x1, double y1, double x2, double y2){

		int				i, index;
		Point2			pt;
		double			d1;
		double			d;
		
		index=-1;	// No intersecta
		d 	= Double.MAX_VALUE;
		for (i = 0; i < n(); i++)
		{
			pt = beacons[i].getLine().intersection (x1, y1, x2, y2);
			if (pt != null)
			{
				d1 = pt.distance (x1, y1);
				if (d1 < d)
				{
					d 	= d1;
					index=i;
				}
			}
		}
		return index;
    }
	
	public void fromProperties (Properties props)
	{
		String				prop;
		beacons = new WMBeacon[Integer.parseInt (props.getProperty ("BEACONS","0"))];
		for (int i=0; i < beacons.length; i++){
			prop		= props.getProperty ("BEACON_"+i);		
			beacons[i]	= new WMBeacon (prop);
		}
	}
	
	public void toProperties (Properties props)
	{
		int			i;
		
		props.setProperty ("BEACONS",Integer.toString (beacons.length));
		
		for (i = 0; i < beacons.length; i++)
			props.setProperty ("BEACON_"+i, beacons[i].toRawString ());
	}
	
	public void toFile (PrintWriter out)
	{
		// Print Cilindrical Beacon
		out.println("# ==============================");
		out.println("# STRIP BEACONS");
		out.println("# ==============================");
		out.println ("BEACONS = " + n());
		out.println("");
		for (int i = 0; i < beacons.length; i++) 
			out.println ("BEACON_" + i + " = " + beacons[i].toRawString ());
		out.println ();
	}
	
	public void toDxfFile (DXFWorldFile dxf){
	   //	  Define una capa con un color determinado (opcional)
      dxf.addLayer(new Layer("BEACONS",ACADColor.RED));
		for(int i = 0; i < beacons.length; i++){
		    beacons[i].toDxf(dxf);
		}
	}
	
}
