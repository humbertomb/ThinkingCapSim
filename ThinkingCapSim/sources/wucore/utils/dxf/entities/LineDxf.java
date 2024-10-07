/**
 * Title: Line
 * Description: Entidad Linea
 * @author Jose Antonio Marin
 * @version 1.0
 */

package wucore.utils.dxf.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import wucore.utils.geom.Line2;
import wucore.utils.geom.Point3;

public class LineDxf extends Entity{

	Point3 start;
	Point3 end;
	
	public LineDxf(){
		this(new Point3(0,0,0),new Point3(0,0,0));
	}
	public LineDxf(Point3 start, Point3 end){
		this(start,end,"0");
	}
	public LineDxf(Line2 line){
		this(new Point3(line.orig()),new Point3(line.dest()),"0");
	}
	
	public LineDxf(Point3 start, Point3 end, String layer){
		super();
		set(start,end,layer);
	}
	
	public LineDxf(BufferedReader br) throws IOException{
		this();
		read(br);
	}
	
	public void set(Point3 start, Point3 end, String layer){
		setStart(start);
		setEnd(end);
		setLayer(layer);
	}
	
	public void setStart(Point3 start)		{	
		this.start = start;	
		checkLimits(start);
	}
	public void setEnd(Point3 end)		{
		this.end = end;	
		checkLimits(end);
	}

	public Point3 getStart()	{		return start;		}
	public Point3 getEnd()		{		return end;		}

	//	 Read dxf line
	public void read(BufferedReader br) throws IOException{
		String line;
				
		while( !(line = br.readLine()).endsWith(" 0") ){
			if(line.endsWith(" 5"))	setID(br.readLine());
			else if(line.endsWith(" 8"))  setLayer(br.readLine());
			else if(line.endsWith(" 10")) start.x(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 20")) start.y(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 30")) start.z(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 11")) end.x(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 21")) end.y(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 31")) end.z(Double.parseDouble(br.readLine()));
			
			// Extended data
			else if(line.endsWith("1071")){
				addExtInt(Integer.parseInt(br.readLine()));
			}
			else if(line.endsWith("1040")){
				addExtDouble(Double.parseDouble(br.readLine()));
			}
			else if(line.endsWith("1000")){
				addExtText(br.readLine());
			}
			else line = br.readLine();
		}
	
	}
	
	public void write(PrintWriter out){
		calculateID();
		out.println("  0\nLINE");							// Tipo entidad
		out.println("  5\n" + id); 							// Identificador
		out.println("  8\n" + layer);						// Capa
		out.println(" 10\n" + start.x());
		out.println(" 20\n" + start.y()); 
		out.println(" 30\n" + start.z());
		out.println(" 11\n" + end.x());
		out.println(" 21\n" + end.y());
		out.println(" 31\n" + end.z());
		writeExt(out);		
	}
	

	
	
	public String toString()	{		return "LINE(LAYER: '"+getLayer()+"' ID: '"+getID()+"') START: {"+start.x()+","+start.y()+","+start.z()+"}  END: {"+end.x()+","+end.y()+","+end.z()+"}"+ toExtString();}
}