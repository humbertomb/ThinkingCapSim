/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import java.util.StringTokenizer;

import wucore.utils.dxf.DXFWorldFile;
import wucore.utils.dxf.DoubleFormat;
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

	public WMCBeacon(String prop) {
		StringTokenizer st = new StringTokenizer (prop,", \t");
		double px			 	= Double.parseDouble (st.nextToken());
		double py 				= Double.parseDouble (st.nextToken());
		double pz				= Double.parseDouble (st.nextToken());
		pos						= new Point3 (px, py, pz);
		diameter				= Double.parseDouble (st.nextToken());
		height				 	= Double.parseDouble (st.nextToken());
		label = new String (st.nextToken());
	}

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

	public String toRawString ()
	{
		return DoubleFormat.format(pos.x()) + ", " + DoubleFormat.format(pos.y()) + ", " + DoubleFormat.format(pos.z()) + ", " + DoubleFormat.format(diameter) + ", " + DoubleFormat.format(height) + ", " + label;
	}
}
