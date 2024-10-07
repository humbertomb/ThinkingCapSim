/*
 * Seccion ENTITIES del archivo DXF.
 * Contiene todas las entidades insertadas en el DXF
 * como lineas, circulos, polilineas ...
 * 
 * Faltan por implementar bastantes parametros, pero estan los importantes.
 */
package wucore.utils.dxf.sections;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import wucore.utils.dxf.entities.CircleDxf;
import wucore.utils.dxf.entities.Entity;
import wucore.utils.dxf.entities.InsertDxf;
import wucore.utils.dxf.entities.LineDxf;
import wucore.utils.dxf.entities.LwPolylineDxf;
import wucore.utils.dxf.entities.PolylineDxf;
import wucore.utils.dxf.entities.TextDxf;
import wucore.utils.dxf.entities.VertexDxf;
import wucore.utils.geom.Point2;

/**
 * @author Administrador
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class EntitiesSection {

public Vector entities;

public Point2 min = new Point2(0,0);
public Point2 max = new Point2(100,100);

public EntitiesSection(){
	entities = new Vector();
}


public void add(Entity entity){
	entities.add(entity);
}

/* Formato Seccion ENTITIES: 
0								Inicio de la sección ENTITIES
SECTION
2
ENTITIES 
 
0								Una entrada por cada definición de entidad
<tipo entidad>
5
<identificador>
330
<puntero a propietario>
100
AcDbEntity
8
<capa>
100
AcDb<nombre clase>
.
. <datos>
. 
 
0									Fin de la sección ENTITIES
ENDSEC 
 
 
 
*/ 

public void write(PrintWriter out){
	out.println("  0\nSECTION");					// Inicio Seccion
	out.println("  2\nENTITIES"); 					// Seccion Entities
	for(int i = 0; i<entities.size();i++){
		((Entity)entities.get(i)).write(out);
	}
	out.println("  0\nENDSEC"); 					// Fin Seccion Entities
}

public void read(BufferedReader br) throws IOException{
	String linea;
	while((linea=br.readLine())!=null){
		if(linea.equals("LINE"))		add(new LineDxf(br));
		if(linea.equals("TEXT"))		add(new TextDxf(br));
		if(linea.equals("CIRCLE"))		add(new CircleDxf(br));
		if(linea.equals("INSERT"))		add(new InsertDxf(br));
		if(linea.equals("POLYLINE"))	add(new PolylineDxf(br));
		if(linea.equals("LWPOLYLINE")) 	add(new LwPolylineDxf(br));
		if(linea.equals("VERTEX"))		add(new VertexDxf(br));
		if(linea.equals("ENDSEC"))		return;
	}
}

public String toString(){
	String ret = "";
	ret += 	"----------------------------------\n"+
			"Entities = "+entities.size()+"\n"+
			"----------------------------------\n";
	for(int i = 0; i<entities.size();i++)	ret += entities.get(i).toString()+"\n";
	
	return ret;
}

public LineDxf[] getLines(){
    Vector lines = new Vector();
    Entity ent;
    for(int i=0; i<entities.size();i++){
        ent = (Entity)entities.get(i);
        if(ent instanceof LineDxf)
            lines.add(ent);
        if(ent instanceof PolylineDxf){	// Convierte polilineas en lineas
            LineDxf[] poly = ((PolylineDxf)ent).toDxfLines();
            if(poly != null)
                for(int j = 0; j<poly.length; j++)
                    lines.add(poly[j]);
        }
    }
    if(lines.size()==0) return null;
    return (LineDxf[]) lines.toArray(new LineDxf[0]);
}

}
