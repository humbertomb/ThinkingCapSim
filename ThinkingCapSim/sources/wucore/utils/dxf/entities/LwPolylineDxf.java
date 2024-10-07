/**
 * Title: Line
 * Description: Entidad Polilinea para DXF version 2000, 2004
 * @author Jose Antonio Marin
 * @version 1.0
 */

package wucore.utils.dxf.entities;

import java.io.BufferedReader;
import java.io.IOException;
import wucore.utils.geom.Point3;

public class LwPolylineDxf extends PolylineDxf{
	
	public LwPolylineDxf(){
		super();
	}
	
	public LwPolylineDxf(BufferedReader br) throws IOException{
		super(br);
	}
	
	//	 Read dxf line
	public void read(BufferedReader br) throws IOException{
		String line;
		Point3 vertex = null;
		int cont = -1;
		while( (line = br.readLine()) != null && !line.endsWith(" 0")){
			if(line.endsWith(" 5"))	setID(br.readLine());
			else if(line.endsWith(" 8"))  setLayer(br.readLine());
			else if(line.endsWith(" 90")){
				line = br.readLine();
				//nvertex = Integer.parseInt(line.trim());
			}
			else if(line.endsWith(" 10")){
				line = br.readLine();
				if(vertex != null)
					addVertex(new VertexDxf(vertex));
				vertex = new Point3();
				vertex.x(Double.parseDouble(line));
				cont ++;
			}
			else if(line.endsWith(" 20")){
				line = br.readLine();
				vertex.y(Double.parseDouble(line));
			}
			else if(line.endsWith(" 30")){
				line = br.readLine();
				vertex.z(Double.parseDouble(line));
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
		if(vertex != null)
			addVertex(new VertexDxf(vertex));
	
	}
	
	public String toString()	{		
		String ret = "LWPOLYLINE(LAYER: '"+getLayer()+"' ID: '"+getID()+"')   "+ toExtString();
		for(int i = 0; i<vertexs.size(); i++)
			ret += "\n" + ((VertexDxf)vertexs.get(i)).toString();
		return ret;
	}
}