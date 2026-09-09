/**
 * Title: Text
 * Description: Entidad Text
 * @author Jose Antonio Marin
 * @version 1.0
 */

package wucore.utils.dxf.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import wucore.utils.geom.Point3;

public class TextDxf extends Entity{

	Point3 pos;
	double heigth = 0.2;
	String txt;
	
	public TextDxf(){
		this("text",new Point3(0,0,0),0.2);
	}
	public TextDxf(String txt, Point3 pos, double heigth){
		this(txt, pos, heigth,"0");
	}
	
	public TextDxf(String txt, Point3 pos, double heigth, String layer){
		super();
		set(txt, pos,heigth,layer);
	}
	
	public TextDxf(BufferedReader br) throws IOException{
		this();
		read(br);
	}
	
	public void set(String txt, Point3 pos, double heigth, String layer){
		setText(txt);
		setPos(pos);
		setHeigth(heigth);
		setLayer(layer);
	}
	
	
	public void setPos(Point3 pos){
		this.pos = pos;
		checkLimits(pos);
	}
	public void setHeigth(double heigth){	this.heigth = heigth;	}
	public void setText(String txt)		{	this.txt = txt;			}

	
	public Point3 getPos()		{		return pos;			}
	public double getHeigth()	{		return heigth;		}
	public String getText()		{		return txt;			}
	
	public void write(PrintWriter out){
		calculateID();
		out.println("  0\nTEXT");							// Tipo entidad
		out.println("  5\n" + id); 			// Identificador
		out.println("  8\n" + layer);						// Capa
		out.println(" 10\n" + pos.x());
		out.println(" 20\n" + pos.y()); 
		out.println(" 30\n" + pos.z());
		out.println(" 40\n" + heigth);
		out.println("  1\n" + txt);
		writeExt(out);
	}
	
	public void read(BufferedReader br) throws IOException{
		String line;
		
		while( !(line = br.readLine()).endsWith(" 0") ){
			if(line.endsWith(" 5"))	setID(br.readLine());
			else if(line.endsWith(" 8"))  setLayer(br.readLine());
			else if(line.endsWith(" 10")) pos.x(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 20")) pos.y(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 30")) pos.z(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 40")) setHeigth(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 1")) setText(br.readLine());
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
	
	public String toString()	{		return "TEXT(LAYER: '"+getLayer()+"' ID: '"+getID()+"') POSITION: {"+pos.x()+","+pos.y()+","+pos.z()+"} HEIGTH: " + heigth+" TEXT: '"+txt+"'"+ toExtString();}
	
}