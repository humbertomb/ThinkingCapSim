/**
 * Title: VertexDxf
 * Description: Entidad VERTEX (punto de un vertice para la polilinea)
 * @author Jose Antonio Marin
 * @version 1.0
 */

package wucore.utils.dxf.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import wucore.utils.geom.Point3;

public class VertexDxf extends Entity{

	Point3 vertex;
	public double curvature;			// radio de curvatura (negativo en sentido horario)
	public double startwidth;			// Grosor inicial 
	public double endwidth;				// Grosor final 
	
	
	public VertexDxf(Point3 vertex){
		super();
		setVertex(vertex);
	}
	
	public VertexDxf(BufferedReader br) throws IOException{
		this(new Point3());
		read(br);
	}
	
	public void setVertex(Point3 vertex){	
		this.vertex = vertex;	
		checkLimits(vertex);
	}
	public Point3 getVertex()	{		return vertex;		}

	//	 Read dxf line
	public void read(BufferedReader br) throws IOException{
		String line;
				
		while( !(line = br.readLine()).endsWith(" 0") ){
			if(line.endsWith(" 5"))	setID(br.readLine());
			else if(line.endsWith(" 8"))  setLayer(br.readLine());
			else if(line.endsWith(" 10")) vertex.x(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 20")) vertex.y(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 30")) vertex.z(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 40")) startwidth = Double.parseDouble(br.readLine());
			else if(line.endsWith(" 41")) endwidth = Double.parseDouble(br.readLine());
			else if(line.endsWith(" 42")) curvature = Double.parseDouble(br.readLine());
			
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
		out.println("  0\nVERTEX");							// Tipo entidad
		out.println("  5\n" + id); 							// Identificador
		out.println("  8\n" + layer);						// Capa
		out.println(" 10\n" + vertex.x());
		out.println(" 20\n" + vertex.y()); 
		out.println(" 30\n" + vertex.z());
		if(startwidth != 0) out.println(" 40\n" + startwidth);
		if(endwidth != 0) out.println(" 41\n" + endwidth);
		if(curvature != 0) out.println(" 42\n" + curvature);
		writeExt(out);		
	}
	

	
	
	public String toString()	{		return "VERTEX(LAYER: '"+getLayer()+"' ID: '"+getID()+"') POS: {"+vertex.x()+","+vertex.y()+","+vertex.z()+"}  "+ toExtString();}
}