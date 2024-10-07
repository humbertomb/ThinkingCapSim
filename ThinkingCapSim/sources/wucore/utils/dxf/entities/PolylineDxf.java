/**
 * Title: Line
 * Description: Entidad Polilinea (Conjunto de entidades VERTEX)
 * @author Jose Antonio Marin
 * @version 1.0
 */

package wucore.utils.dxf.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import wucore.utils.geom.Point3;

public class PolylineDxf extends Entity{
	
	public Point3 maxlimit;
	public Point3 minlimit;

	public Vector vertexs;
	
	public PolylineDxf(){
		super();
		vertexs = new Vector();
		maxlimit = new Point3(Double.MIN_VALUE,Double.MIN_VALUE,Double.MIN_VALUE);
		minlimit = new Point3(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE);
	}
	
	public PolylineDxf(BufferedReader br) throws IOException{
		this();
		read(br);
	}

	public void addVertex(VertexDxf vertex){
		vertexs.add(vertex);
		if(vertex.vertex.x()<minlimit.x()) minlimit.x(vertex.vertex.x());
		if(vertex.vertex.y()<minlimit.y()) minlimit.y(vertex.vertex.y());
		if(vertex.vertex.z()<minlimit.z()) minlimit.z(vertex.vertex.z());
		if(vertex.vertex.x()>maxlimit.x()) maxlimit.x(vertex.vertex.x());
		if(vertex.vertex.y()>maxlimit.y()) maxlimit.y(vertex.vertex.y());
		if(vertex.vertex.z()>maxlimit.z()) maxlimit.z(vertex.vertex.z());
	}
	
	public VertexDxf getVertex(int i){
		return (VertexDxf)vertexs.get(i);
	}
	
	public Point3 getPoint(int i){
	    return getVertex(i).getVertex();
	}
	
	//	 Read dxf line
	public void read(BufferedReader br) throws IOException{
		String line;
				
		while( (line = br.readLine()) != null ){
			if(line.endsWith(" 5"))	setID(br.readLine());
			else if(line.endsWith(" 8"))  setLayer(br.readLine());
			else if(line.endsWith(" 10")) line = br.readLine();
			else if(line.endsWith(" 20")) line = br.readLine();
			else if(line.endsWith(" 30")) line = br.readLine();
			else if(line.endsWith(" 0")){
				while( (line = br.readLine()) != null )
					if(line.endsWith("VERTEX"))				// Lectura de vertices
						addVertex(new VertexDxf(br));
					else if(line.endsWith("SEQEND")){		// Fin de lectura de vertices
						new SeqendDxf();
						return;								
					}
					else System.out.println("Entidad dentro de polilinea desconocida");
			}
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
		out.println("  0\nPOLYLINE");						// Tipo entidad
		out.println("  5\n" + id); 							// Identificador
		out.println("  8\n" + layer);						// Capa
		out.println(" 66\n     1");							// Indicador de que las entidades siguen
		out.println(" 10\n0.0");							// siempre 0
		out.println(" 20\n0.0"); 							// siempre 0
		out.println(" 30\n0.0");							// elevación de la polilínea 
		out.println(" 70\n" + "     1");					// Indicador de polilínea Indicador de polilínea (codificado en bits; por defecto = 0):
															//	1 = Polilínea cerrada (o malla poligonal cerrada en la dirección M)
															//	2 = Se han añadido vértices de ajuste de curva
															//	4 = Se ha añadido vértices de ajuste de spline
															//	8 = Polilínea 3D
															//	16 = Malla poligonal 3D
															//	32 = Malla poligonal cerrada en la dirección N
															//	64 = La polilínea es una malla policara
															//	128 = El patrón de tipo de línea se genera continuamente alrededor de los vértices de esta polilínea 
		writeExt(out);										// Escribe propiedades Extendidas
		for(int i = 0; i<vertexs.size(); i++)
			((VertexDxf)vertexs.get(i)).write(out);			// Vertices de la polilinea
		new SeqendDxf().write(out);							// Fin de Secuencia
		
	}
	
	public LineDxf[] toDxfLines(){
	    if(vertexs == null) return null;
	    int nlines = vertexs.size();
	    LineDxf[] lines = new LineDxf[nlines];
	    VertexDxf orig, dest;
	    for(int i=0; i<nlines; i++){
	        orig = (VertexDxf) vertexs.get(i);
	        if(i+1 < nlines) 	dest =(VertexDxf) vertexs.get(i+1);
	        else				dest = (VertexDxf) vertexs.get(0);
	        lines[i] = new LineDxf(orig.vertex, dest.vertex);
	        lines[i].setEntProp(orig);
	    }
	    return lines;
	}
	
	
	public String toString()	{		
		String ret = "POLYLINE(LAYER: '"+getLayer()+"' ID: '"+getID()+"')   "+ toExtString();
		for(int i = 0; i<vertexs.size(); i++)
			ret += "\n" + ((VertexDxf)vertexs.get(i)).toString();
		return ret;
	}
}