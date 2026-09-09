/*
 * Created on 10-dic-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import java.util.StringTokenizer;
//import java.awt.*;
import wucore.utils.color.*;

import wucore.utils.dxf.*;
import wucore.utils.dxf.entities.*;
import wucore.utils.geom.*;
import wucore.utils.math.*;
import wucore.utils.color.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMObject extends WMElement
{
	// 2D components (mandatory variables)
	public Line2[]				icon;		// Physical 2D bounds of the object
	public WColor				color;		// Color of the object
		
	// 3D components
	public Point3				pos;			// Position of the 3D object (m, m, m)
	public double				a;			// Heading of the 3D object, XY plane (rad)
	public String				shape;		// 3D object representation	
	public boolean				usecolor;	// Replace 3D object color

	public boolean				visible		= true;
	
	public WMObject (){
	}
	
	// Constructors
	public WMObject (String prop)
	{
		int					j, num;
		StringTokenizer		st;
		double				x1, x2, y1, y2, z1;
		st		= new StringTokenizer (prop,", \t");
		
		// Read line based icon (for 2D displaying and simulation)
		num		= Integer.parseInt (st.nextToken());
		icon		= new Line2[num];
		for (j = 0; j < num; j++)
		{
			x1		= Double.parseDouble (st.nextToken());
			y1		= Double.parseDouble (st.nextToken());
			x2		= Double.parseDouble (st.nextToken());
			y2		= Double.parseDouble (st.nextToken());
			
			icon[j]	= new Line2 (x1, y1, x2, y2);
		}
			
		// Read object color
		if (st.hasMoreTokens())
		{
			//r		= Integer.parseInt (st.nextToken());
			//g		= Integer.parseInt (st.nextToken());
			//b		= Integer.parseInt (st.nextToken());
			//color	= new Color (r, g, b);
			color	= ColorTool.getColorFromName (st.nextToken ());
		}
		else
			color	= WColor.BLACK;
		
		// Read object 3D shape and properties
		if (st.hasMoreTokens ())
		{
			shape	= st.nextToken ();
			x1		= Double.parseDouble (st.nextToken());
			y1		= Double.parseDouble (st.nextToken());
			z1		= Double.parseDouble (st.nextToken());
			pos		= new Point3 (x1, y1, z1);
			a		= Double.parseDouble (st.nextToken()) * Angles.DTOR;
			usecolor	= new Boolean (st.nextToken()).booleanValue ();
		}
		else
			shape	= null;
		
		AbsIcon();
	}
	
	
	public WMObject (InsertDxf insert, BlockDxf block){
			
			LineDxf line;
			int size = 0;
			pos = insert.getPos();
			a = insert.getRot();
			
			if(insert.ExtTextSize()>0)
				color = ColorTool.getColorFromName (insert.getExtText(0));
			else
				color	= WColor.BLACK;
			
			if(insert.ExtTextSize()>1)
				shape = insert.getExtText(1);
			else
				shape	= null;
			
			if(insert.ExtTextSize()>2)
				usecolor = Boolean.getBoolean(insert.getExtText(2));
			else
				usecolor	= false;
			
			for(int i = 0; i<block.entities.size(); i++)
				if(block.entities.get(i) instanceof LineDxf) size ++;
			
			icon = new Line2[size];
			
			for(int i = 0; i<block.entities.size(); i++){
				if(block.entities.get(i) instanceof LineDxf){
					line = (LineDxf)block.entities.get(i);
					icon[i] = new Line2(line.getStart().x(),line.getStart().y(),line.getEnd().x(),line.getEnd().y());
				}
			}
			label = "OBJECT";
			AbsIcon();
		}
	
	public void toDxf(DXFWorldFile dxf){
			InsertDxf insert = new InsertDxf(pos,shape,"OBJECTS");
			insert.setRot(a);
						
			String name = shape.replace('/','_').replace('.','_');
			insert.setBlockname(name);
			BlockDxf block = new BlockDxf(name);
			Line2[] 	icon = getLocalIcon();	// Se escribe en coordenadas locales
			for(int i=0; i<icon.length;i++){
				block.entities.add(
					new LineDxf(new Point3(icon[i].orig()),new Point3(icon[i].dest()))
				);
			}
			insert.addExtText(0,color.toString());
			insert.addExtText(1,shape);
			insert.addExtText(2,Boolean.toString(usecolor));
			dxf.addBlock(block);
			dxf.insertBlock(insert);
	}
	
	// Cambia el icono a coordenadas absolutas (rotacion+translacion)
	protected void AbsIcon(){
		double x1,y1,x2,y2;
		for(int i = 0; i<icon.length; i++){
			x1 = icon[i].orig().x() * Math.cos(a) - icon[i].orig().y() * Math.sin(a)+ pos.x();
			y1 = icon[i].orig().x() * Math.sin(a) + icon[i].orig().y() * Math.cos(a)+ pos.y();
			x2 = icon[i].dest().x() * Math.cos(a) - icon[i].dest().y() * Math.sin(a)+ pos.x();
			y2 = icon[i].dest().x() * Math.sin(a) + icon[i].dest().y() * Math.cos(a)+ pos.y();
			icon[i].set(x1,y1,x2,y2);
		}
	}
	
	protected Line2[] getLocalIcon(){
		double x1,y1,x2,y2;
		Line2[] lines = new Line2[icon.length];
		for(int i = 0; i<icon.length; i++){
			x1 = (icon[i].orig().x()-pos.x()) * Math.cos(-a) - (icon[i].orig().y()-pos.y()) * Math.sin(-a);
			y1 = (icon[i].orig().x()-pos.x()) * Math.sin(-a) + (icon[i].orig().y()-pos.y()) * Math.cos(-a);
			x2 = (icon[i].dest().x()-pos.x()) * Math.cos(-a) - (icon[i].dest().y()-pos.y()) * Math.sin(-a);
			y2 = (icon[i].dest().x()-pos.x()) * Math.sin(-a) + (icon[i].dest().y()-pos.y()) * Math.cos(-a);
			lines[i] = new Line2(x1,y1,x2,y2);
		}
		return lines;
	}
	
	// Instance methods
	public String toRawString ()
	{
		int			i;
		String		out;
		Line2[] 	icon = getLocalIcon();	// Se escribe en coordenadas locales
		out		= new Integer (icon.length).toString () + ", ";
		for (i = 0; i < icon.length; i ++)
			out		+= DoubleFormat.format(icon[i].orig ().x ()) + ", " + DoubleFormat.format(icon[i].orig ().y ()) + ", " + DoubleFormat.format(icon[i].dest ().x ()) + ", " + DoubleFormat.format(icon[i].dest ().y ()) + ", ";		
		out		+= color.toString ();
		
		if (shape != null)
			out		+= ", " + shape + ", " + DoubleFormat.format(pos.x()) + ", " + DoubleFormat.format(pos.y()) + ", " + DoubleFormat.format(pos.z ()) + ", " + DoubleFormat.format(a * Angles.RTOD) + ", " + usecolor;
		
		return  out;
	}
	
	
}
