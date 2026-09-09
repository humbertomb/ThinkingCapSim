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
import wucore.utils.dxf.entities.TextDxf;
import wucore.utils.dxf.sections.ACADColor;
import wucore.utils.dxf.sections.Layer;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMDocks
{
	protected WMDock[] docks;
	

	// Constructors
	public WMDocks (Properties props)
	{
		fromProperties (props);
	}
	
	public WMDocks (DXFWorldFile dxf){
		Vector entities = dxf.getEntities();
		Vector dk = new Vector();
		Entity entity;
		for(int i = 0; i<entities.size(); i++){
			entity = (Entity)entities.get(i);
			if(entity.getLayer().equalsIgnoreCase("DOCKINGS")){
				if(entity instanceof TextDxf){ 
				    dk.add(new WMDock((TextDxf)entity)); 
				}
			}
		}
		docks = new WMDock[dk.size()];
		dk.toArray(docks);
	}
	
	// Accessors	
	public final int	 		n () 				{ return docks.length; }
	public final WMDock[]	waypoints ()		{ return docks; }

	// Instance methods
	public WMDock at (int i)
	{
		if ((i < 0) || (i >= docks.length)) return null;
		return docks[i];
	}
	
	public WMDock at (String label)
	{
		return at(index(label));
	}
	
	public int index (String label)
	{
		int  		i;
		
		if ((label == null) || (docks == null))			return -1;

		for (i = 0; i < docks.length; i++)
			if (label.equals (docks[i].label))			return i;

		return -1;
	}
	


	
	public void fromProperties (Properties props)
	{
		String				prop;
		docks = new WMDock[Integer.parseInt (props.getProperty ("DOCKINGS","0"))];
		for (int i=0; i < docks.length; i++){
			prop		= props.getProperty ("DOCKING_"+i);		
			docks[i] = new WMDock (prop);
		}
	}
	
	public void toProperties (Properties props)
	{
		int			i;
		
		props.setProperty ("DOCKINGS",Integer.toString (docks.length));
		
		for (i = 0; i < docks.length; i++)
			props.setProperty ("DOCKING_"+i, docks[i].toRawString ());
	}
	
	public void toFile (PrintWriter out)
	{
		// Print Cilindrical Beacon
		out.println("# ==============================");
		out.println("# DOCKS");
		out.println("# ==============================");
		out.println ("DOCKINGS = " + n());
		out.println("");
		for (int i = 0; i < docks.length; i++)
			out.println ("DOCKING_" + i + " = " + docks[i].toRawString ());
		out.println ();
	}
	
	public void toDxfFile (DXFWorldFile dxf){
	    
	    // Define una capa con un color determinado (opcional)
	    dxf.addLayer(new Layer("DOCKINGS",ACADColor.CYAN));
	    for (int i = 0; i < docks.length; i++){
	        docks[i].toDxf(dxf);
	    }
	}
	
}
