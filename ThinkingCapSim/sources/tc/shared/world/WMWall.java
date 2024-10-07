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
import wucore.utils.dxf.entities.LineDxf;
import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMWall extends WMElement
{
	// 2D components
	public Line2						edge;
	
	// 2 1/2 D components
	public double					width;
	public double					height;
	
	// Visualization components
	public String					texture;					
	
	public WMWall(){
	}
	
	// Constructors
	public WMWall (String prop, double dwidth, double dheight, String dtexture)
	{
		StringTokenizer		st;
		double				x1, x2, y1, y2;
		
		st		= new StringTokenizer (prop,", \t");
		x1		= Double.parseDouble (st.nextToken());
		y1		= Double.parseDouble (st.nextToken());
		x2		= Double.parseDouble (st.nextToken());
		y2		= Double.parseDouble (st.nextToken());
		edge		= new Line2 (x1, y1, x2, y2);
		
		height	= dheight;
		width	= dwidth;
		texture	= dtexture;
		
		if (st.hasMoreTokens())
			width	= Double.parseDouble (st.nextToken());
		
		if (st.hasMoreTokens())
			height	= Double.parseDouble (st.nextToken());
		
		if (st.hasMoreTokens())
			texture	= st.nextToken();
	}
	
	public WMWall (LineDxf line, double dwidth, double dheight, String dtexture){
	      edge = new Line2(line.getStart().x(),line.getStart().y(),line.getEnd().x(),line.getEnd().y());
			if(line.ExtendedDouble.size()>0) 
			    height = line.getExtDouble(0);
			else
			    height = dheight;
			if(line.ExtendedDouble.size()>1) 
			    width = line.getExtDouble(1);
			else										
			    width = dwidth;
			if(line.ExtendedText.size()>0) 
			    texture = line.getExtText(0);
			else
			    texture = dtexture;
			label = "LINE_?";
	}
	
	public void toDxf(DXFWorldFile dxf){
	    LineDxf line = new LineDxf(
               new Point3(edge.orig()),
               new Point3(edge.dest()),
               "0"
       );
       line.addExtDouble(0,height);
       line.addExtDouble(1,width);
       line.addExtText(0,texture);
       dxf.addEntity(line);
	}
	
	// Instance methods
	public String toRawString ()
	{
		return DoubleFormat.format(edge.orig().x())+", "+DoubleFormat.format(edge.orig().y())+", "+DoubleFormat.format(edge.dest().x())+", "+DoubleFormat.format(edge.dest().y())+", "+DoubleFormat.format(width)+", "+DoubleFormat.format(height)+", "+texture;
	}
}
