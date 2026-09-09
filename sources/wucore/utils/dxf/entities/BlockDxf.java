/**
 * Title: BlockDxf
 * Description: Bloque (No es una entidad). Representa un conjunto de entidades. 
 * Debe definirse en la seccion BLOCKS para poder insertarse mendiante la entidad INSERT.
 * @author Jose Antonio Marin
 * @version 1.0
 */

package wucore.utils.dxf.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import wucore.utils.geom.Point3;

public class BlockDxf extends Entity{
	
	public String name;
	Point3 pos;
	
	public Vector entities;
	
	public BlockDxf(String name){
		this(name,new Point3(),"0");
	}
	
	public BlockDxf(String name, Point3 pos){
		this(name,pos,"0");
	}	
	
	public BlockDxf(BufferedReader br) throws IOException{
		this("Bloque");
		read(br);
	}
	
	public BlockDxf(String name, Point3 pos, String layer){
		super();
		entities = new Vector();
		this.pos = pos;
		this.layer = layer;
		this.name = name;
	}
	
	public void setName(String name){this.name = name;}
	public void setPos(Point3 pos){this.pos = pos;}
	
	public String getName(){return name;}
	public Point3 getPos(){return pos;}

	public void add(Entity entity){
		entities.add(entity);
	}
	
	public void write(PrintWriter out){
		calculateID();
		out.println("  0\nBLOCK");							// Tipo entidad
//		out.println("  5\n" + String.valueOf(id)); 			// Identificador
		out.println("  8\n" + layer);						// Capa
		out.println("  70\n" + "     0");
		out.println("  2\n" + name);						// Nombre del Bloque
		out.println(" 10\n" + pos.x());
		out.println(" 20\n" + pos.y()); 
		out.println(" 30\n" + pos.z());
		out.println("  3\n" + name);
		out.println("  1\n" + "   ");
		for(int i = 0; i < entities.size(); i++){
			((Entity)entities.get(i)).write(out);
		}
		out.println("  0\nENDBLK");
		out.println("  5\n" + getID());								
		out.println("  8\n" + layer);
	}

	public void read(BufferedReader br) throws IOException {
		String line;
		
		while( (line = br.readLine()) != null ){
			if(line.endsWith(" 5"))	setID(br.readLine());
			else if(line.endsWith(" 2"))  setName(br.readLine());
			else if(line.endsWith(" 3"))  setName(br.readLine());
			else if(line.endsWith(" 8"))  setLayer(br.readLine());
			else if(line.endsWith(" 10")) pos.x(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 20")) pos.y(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 30")) pos.z(Double.parseDouble(br.readLine()));
			else if(line.endsWith(" 0")){
				while( (line = br.readLine()) != null ){
					if (line.endsWith("ENDBLK")){
						while( (line = br.readLine()) != null ){
							if(line.endsWith(" 5"))	setID(br.readLine());
							else if(line.endsWith(" 8"))  setLayer(br.readLine());
							else if(line.endsWith(" 0"))  return;		// Fin de Bloque
							else br.readLine();
						}
					}
					else if (line.equals("LINE"))	add(new LineDxf(br));
					else if (line.equals("TEXT"))	add(new TextDxf(br));
					else if (line.equals("CIRCLE"))	add(new CircleDxf(br));
					else	break;
				}
			}
			else br.readLine();
		}
	}


	public String toString() {
		String ret = "";
		ret += "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n";
		ret += "BLOCK(LAYER: '"+getLayer()+"' ID: '"+getID()+"') POSREF: {"+pos.x()+","+pos.y()+","+pos.z()+"} NAME: "+name+"\n";
		ret += "\n";
		ret += "Entities = "+entities.size()+"\n";
		for(int i = 0; i<entities.size();i++)	ret += entities.get(i).toString()+"\n";
		ret += "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n";
		
		return ret;
	}

}