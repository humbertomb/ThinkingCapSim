/**
 * Title: Line
 * Description: Entidad INSERT para insercion de bloques
 * @author Jose Antonio Marin
 * @version 1.0
 */

package wucore.utils.dxf.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import wucore.utils.geom.Point3;

public class InsertDxf extends Entity{

	Point3 pos;			// Posicion de insercion
	Point3 scale;		// Escala (x,y,z)
	double rotacion;	// Angulo de rotacion (rad)
	
	//BlockDxf block;
	String blockname;

	public InsertDxf(){
		this(new Point3(0,0,0),null);
	}
	public InsertDxf(Point3 pos, String block){
		this(pos,block,"0");
	}
	
	public InsertDxf(Point3 pos, String block, String layer){
		super();
		set(pos,block,layer);
		scale = new Point3(1,1,1);
		rotacion = 0.0;
	}
	
	public InsertDxf(BufferedReader br) throws IOException{
		this();
		read(br);
	}
	
	public void set(Point3 pos, String block, String layer){
		setPos(pos);
		setBlockname(block);
		setLayer(layer);
	}
	
	public void setPos(Point3 pos)		{	
		this.pos = pos;	
		checkLimits(pos);
	}

	public void setScale(Point3 scale)		{	this.scale  = scale; }
	public void setRot(double rotacion)		{	this.rotacion  = rotacion; }
	public Point3 getScale()				{	return scale; }
	public double getRot()					{	return rotacion; }

	
	public void setBlockname(String block)	{	this.blockname = block;	}
	public String getBlockname()			{		return blockname;	}
	public Point3 getPos()					{		return pos;		}
	
	//	 Read dxf line
	public void read(BufferedReader br) throws IOException{
		String line;
		
		while( !(line = br.readLine()).endsWith(" 0") ){
			if(line.endsWith(" 5"))	setID(br.readLine());
			else if(line.endsWith(" 8"))  setLayer(br.readLine());
			else if(line.endsWith(" 10")) pos.x(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 20")) pos.y(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 30")) pos.z(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 50")) rotacion = Math.toRadians(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 41")) scale.x(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 42")) scale.y(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 43")) scale.z(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 2"))  blockname = br.readLine();
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
	
	public void write(PrintWriter out){
		calculateID();
		out.println("  0\nINSERT");							// Tipo entidad
		out.println("  5\n" + id); 							// Identificador
		out.println("  8\n" + layer);						// Capa
		out.println("  2\n" + blockname);
		out.println(" 10\n" + pos.x());
		out.println(" 20\n" + pos.y()); 
		out.println(" 30\n" + pos.z());
		if(scale.x() != 1) out.println(" 41\n" + scale.x());
		if(scale.y() != 1) out.println(" 42\n" + scale.y()); 
		if(scale.z() != 1) out.println(" 43\n" + scale.z());
		if(rotacion != 0) out.println(" 50\n" + Math.toDegrees(rotacion));
		writeExt(out);
	}
	
	public String toString()	{		return "INSERT(LAYER: '"+getLayer()+"' ID: '"+getID()+"') POS: {"+pos.x()+","+pos.y()+","+pos.z()+"}  BlockName: '"+blockname+"'"+ toExtString();}
}