/*
 * Seccion TABLES del archivo DXF.
 * La sección TABLES contiene varias tablas, las cuales a su 
 * vez pueden contener un número variable de entradas. 
 * 
 * En este caso solo se utiliza para registrar una aplicacion
 * que se utilizara para generar Datos Extendidos
 * 
 */
package wucore.utils.dxf.sections;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

/**
 * @author Administrador
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TablesSection {

public Vector layers;

public TablesSection(){
    layers = new Vector();
}

public void addLayer(Layer layer){
    layers.add(layer);
}

public void write(PrintWriter out){
	out.println("  0\nSECTION");					// Inicio Seccion
	out.println("  2\nTABLES"); 					// Seccion Blocks
	
	out.println("  0\nTABLE");						// Inicio de la tabla APPID		
	out.println("  2\nAPPID");
	out.println(" 70\n     2");					// Longitud tabla = 2
	out.println("  0\nAPPID");
	out.println("  2\nACAD");						// Nombre de la Aplicacion
	out.println(" 70\n     0");						// Valores de indicador estándar 
	out.println("  0\nAPPID");
	out.println("  2\nTCII");						// Nombre de la Aplicacion
	out.println(" 70\n     0");						// Valores de indicador estándar 
	out.println("  0\nENDTAB");					// Final de la tabla
	
	if(layers.size()>0){
	    Layer layer;
	    out.println("  0\nTABLE");
	    out.println("  2\nLAYER");
	    out.println(" 70\n     "+layers.size());		// Longitud tabla = 2
	    for(int i = 0; i<layers.size();i++){
	        layer = (Layer)layers.get(i);
	        out.println("  0\nLAYER");
	        out.println("  2\n"+layer.name);				// Nombre capa
	        out.println(" 70\n     0");					// Valores de indicador estándar
	        out.println(" 62\n     "+layer.color); 	// Color (negativo desactivada)
	        out.println("  6\n"+layer.linestile);		// Nombre de tipo de linea
	    }
       out.println("  0\nENDTAB");					// Final de la tabla

	}
	out.println("  0\nENDSEC"); 					// Fin Seccion Blocks
}

public void read(BufferedReader br) throws IOException{
}

public String toString(){
	return super.toString();
}

}
