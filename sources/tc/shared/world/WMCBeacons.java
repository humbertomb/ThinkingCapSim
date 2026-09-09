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
import wucore.utils.dxf.entities.CircleDxf;
import wucore.utils.dxf.entities.Entity;
import wucore.utils.dxf.entities.TextDxf;
import wucore.utils.dxf.sections.ACADColor;
import wucore.utils.dxf.sections.Layer;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMCBeacons
{
	
	protected WMCBeacon[]			beacons;

	// Constructors
	public WMCBeacons (Properties props)
	{
		fromProperties (props);
	}
	
	public WMCBeacons (DXFWorldFile dxf){
		Vector entities = dxf.getEntities();
		Vector cbeac = new Vector();
		Entity entity;
		int index = 0;
		for(int i = 0; i<entities.size(); i++){
			entity = (Entity)entities.get(i);
			if(entity.getLayer().equalsIgnoreCase("CBEACONS")){
				if(entity instanceof TextDxf) 
					cbeac.add(new WMCBeacon((TextDxf) entity));
				if(entity instanceof CircleDxf){ 
				    WMCBeacon beac = new WMCBeacon((CircleDxf) entity);	
				    if(beac.label == null)	beac.label = "CB"+(index++);
				    cbeac.add(beac);
				}
			}
		}
		beacons = new WMCBeacon[cbeac.size()];
		cbeac.toArray(beacons);
	}
	
	// Accessors
	public final int	 		n () 				{ return beacons.length; }
	public final WMCBeacon[]	edges ()			{ return beacons; }
	
	// Instance methods
	public WMCBeacon at (int i)
	{
		if ((i < 0) || (i >= beacons.length)) return null;
		return beacons[i];
	}
	
	public void fromProperties (Properties props)
	{
		String				prop;
		beacons = new WMCBeacon[Integer.parseInt (props.getProperty ("CBEACONS","0"))];
		for (int i=0; i < beacons.length; i++){
			prop		= props.getProperty ("CBEACON_"+i);		
			beacons[i]	= new WMCBeacon (prop);
		}
	}
	
	public void toProperties (Properties props)
	{
		int			i;
		
		props.setProperty ("CBEACONS",Integer.toString (beacons.length));
		
		for (i = 0; i < beacons.length; i++)
			props.setProperty ("CBEACON_"+i, beacons[i].toRawString ());
	}
	
	public void toFile (PrintWriter out)
	{
		// Print Cilindrical Beacon
		out.println("# ==============================");
		out.println("# CILINDRICAL BEACONS");
		out.println("# ==============================");
		out.println ("CBEACONS = " + n());
		out.println("");
		for (int i = 0; i < beacons.length; i++) 
			out.println ("CBEACON_" + i + " = " + beacons[i].toRawString ());
		out.println ();
	}
	
	public void toDxfFile (DXFWorldFile dxf){
	   //	  Define una capa con un color determinado (opcional)
	   dxf.addLayer(new Layer("CBEACONS",ACADColor.RED));
		for(int i = 0; i < beacons.length; i++){
			beacons[i].toDxf(dxf);
		}
	}
	
}
