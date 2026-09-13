/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonObject;


import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.entities.CircleDxf;
import wucore.utils.dxf.entities.TextDxf;
import wucore.utils.geom.Point3;

/**
 * Cylindrical (reflector) beacon: a vertical cylinder of a given diameter and
 * height whose base is at pos (x, y, z).
 *
 * @author Humberto Martinez Barbera
 */
public class WMCBeacon extends WMElement
{
	static public final double	DEF_DIAMETER	= 0.036;	// default diameter (m)
	static public final double	DEF_HEIGHT		= 0.5;		// default height (m)

	public Point3			pos;						// Centre of the base
	public double			diameter	= DEF_DIAMETER;
	public double			height		= DEF_HEIGHT;

	public WMCBeacon(double x, double y, double z, double diameter, double height, String label){
		this.pos		= new Point3 (x, y, z);
		this.diameter	= diameter;
		this.height		= height;
		this.label		= label;
	}

	public WMCBeacon(TextDxf text) {
	    Point3 p = text.getPos();
	    label = text.getText();
	    if(text.ExtendedDouble.size()>0) diameter = text.getExtDouble(0);
	    if(text.ExtendedDouble.size()>1) height = text.getExtDouble(1);
	    pos = new Point3 (p.x(), p.y(), 0.0);
	}

	public WMCBeacon(CircleDxf circle) {
	    Point3 p = circle.getCenter();
	    if(circle.ExtendedText.size()>0) label = circle.getExtText(0);
	    else label = "CBEAC";
	    diameter = 2.0 * circle.getRadius();
	    pos = new Point3 (p.x(), p.y(), 0.0);
	}

	public double x ()			{ return pos.x (); }
	public double y ()			{ return pos.y (); }
	public double z ()			{ return pos.z (); }
	public double radius ()		{ return diameter / 2.0; }

	// Lo convierte en un circulo
   public void toDxf(DXFWorldFile dxf) {
	    CircleDxf circle = new CircleDxf(new Point3(pos.x(),pos.y(),0.0),radius(),"CBEACONS");
	    circle.addExtText(label);
	    dxf.addEntity(circle);
	}

   // Lo convierte en un texto
   public void toDxf1(DXFWorldFile dxf) {
	    TextDxf text = new TextDxf(label,new Point3(pos.x(),pos.y(),0.0),0.2,"CBEACONS");
	    text.addExtDouble(diameter);
	    text.addExtDouble(height);
	    dxf.addEntity(text);
	}

	/* JSON: {label, x, y, z, diameter, height} */

	public WMCBeacon (JsonObject o)
	{
		label		= World.getString (o, "label", "cb");
		pos			= World.toPoint (o);
		diameter	= World.getDouble (o, "diameter", DEF_DIAMETER);
		height		= World.getDouble (o, "height", DEF_HEIGHT);
	}

	public JsonObject toJson ()
	{
		JsonObject	o = new JsonObject ();
		o.addProperty ("label", label);
		World.putPoint (o, pos.x (), pos.y (), pos.z ());
		o.addProperty ("diameter", World.num (diameter));
		o.addProperty ("height", World.num (height));
		return o;
	}
}
