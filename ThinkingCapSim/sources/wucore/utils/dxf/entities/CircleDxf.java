/**
 * Title: Circle
 * Description: Entidad Circulo
 * @author Jose Antonio Marin
 * @version 1.0
 */

package wucore.utils.dxf.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import wucore.utils.geom.Point3;

public class CircleDxf extends Entity{
	
	Point3 center;
	double radius;
	
	public CircleDxf(){
		this(new Point3(0,0,0),1.0);
	}
	public CircleDxf(Point3 center, double radius){
		this(center,radius,"0");
	}
	
	public CircleDxf(Point3 center, double radius, String layer){
		super();
		set(center,radius,layer);
	}
	
	public CircleDxf(BufferedReader br) throws IOException{
		this();
		read(br);
	}
	
	
	public void set(Point3 center, double radius, String layer){
		setCenter(center);
		setRadius(radius);
		setLayer(layer);
	}
	
	
	public void setCenter(Point3 center){
		this.center = center;	
		checkLimits(center);
	}
	public void setRadius(double radius){	this.radius = radius; }
	
	public Point3 getCenter()	{	return center;	}
	public double getRadius()	{	return radius; }
	
	public void write(PrintWriter out){
		calculateID();										// Se calcula una id valida
		out.println("  0\nCIRCLE");							// Tipo entidad
		out.println("  5\n" + id); 							// Identificador
		out.println("  8\n" + layer);						// Capa
		out.println(" 10\n" + center.x());
		out.println(" 20\n" + center.y()); 
		out.println(" 30\n" + center.z());
		out.println(" 40\n" + radius);
		writeExt(out);
	}
	
	public void read(BufferedReader br) throws IOException{
		String line;
		
		while( !(line = br.readLine()).endsWith(" 0") ){
			if(line.endsWith(" 5"))	setID(br.readLine());
			else if(line.endsWith(" 8"))  setLayer(br.readLine());
			else if(line.endsWith(" 10")) center.x(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 20")) center.y(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 30")) center.z(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 40")) setRadius(Double.parseDouble(br.readLine()));
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
			else br.readLine();
		}
	
	}
	
	public String toString()	{		return "CIRCLE(LAYER: '"+getLayer()+"' ID:"+getID()+") CENTER: {"+center.x()+","+center.y()+","+center.z()+"} RADIUS: " + radius + toExtString();}
}