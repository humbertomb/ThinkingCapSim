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
import wucore.utils.geom.Ellipse2;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMCBeacon extends WMElement
{
	
	public Ellipse2			beacon;	// Se considerara que son siempre circulares

	public WMCBeacon(String prop) {
		StringTokenizer st = new StringTokenizer (prop,", \t");
		double px			 	= Double.parseDouble (st.nextToken()); 
		double py 				= Double.parseDouble (st.nextToken());
		double horiz			= Double.parseDouble (st.nextToken()); 				// Horizontal size
		double vert			 	= Double.parseDouble (st.nextToken());				// Vertical size		
		beacon = new Ellipse2(px,py,horiz,vert);
		label = new String (st.nextToken());
	}

	public WMCBeacon(Ellipse2 beacon, String label){
		this.beacon = beacon;
		this.label = label;
	}
	
	public WMCBeacon(TextDxf text) {
	    Point3 pos = text.getPos();
	    label = text.getText();
	    double radius = 0.0;
	    if(text.ExtendedDouble.size()>0) radius = text.getExtDouble(0);	
	    beacon = new Ellipse2(pos.x(),pos.y(),radius,radius);
	}
	
	public WMCBeacon(CircleDxf circle) {
	    Point3 pos = circle.getCenter();
	    if(circle.ExtendedText.size()>0) label = circle.getExtText(0);	
	    else label = "CBEAC";
	    beacon = new Ellipse2(pos.x(),pos.y(),circle.getRadius(),circle.getRadius());
	}

	// Lo convierte en un circulo
   public void toDxf(DXFWorldFile dxf) {
	    CircleDxf circle = new CircleDxf(new Point3(beacon.center()),beacon.horiz(),"CBEACONS");		
	    circle.addExtText(label);
	    dxf.addEntity(circle); 
	}
	
   // Lo convierte en un texto
   public void toDxf1(DXFWorldFile dxf) {
	    TextDxf text = new TextDxf(label,new Point3(beacon.center()),0.2,"CBEACONS");		
	    text.addExtDouble(beacon.horiz());
	    dxf.addEntity(text); 
	}

	public String toRawString ()
	{
		return DoubleFormat.format(beacon.center().x()) + ", " + DoubleFormat.format(beacon.center().y()) + ", " + DoubleFormat.format(beacon.horiz()) + ", " + DoubleFormat.format(beacon.vert()) + ", " + label;
	}
}
