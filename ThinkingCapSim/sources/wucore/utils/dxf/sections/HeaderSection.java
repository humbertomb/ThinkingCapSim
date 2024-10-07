/*
 * Seccion HEADER del archivo DXF.
 * La sección HEADER de los archivos DXF contiene los parámetros de variables asociadas con el dibujo. 
 * Cada variable se precisa mediante un código de grupo 9 que proporciona el nombre de la variable 
 * seguido de grupos que suministran su valor.
 * 
 * Faltan por implementar bastantes parametros, pero estan los importantes.
 */
package wucore.utils.dxf.sections;

import java.io.PrintWriter;

import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;

/**
 * @author Administrador
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class HeaderSection {

/* Número de versión de la base de datos de dibujos de AutoCAD®: 
	AC1006 = R10; AC1009 = R11 y R12; 
	AC1012 = R13; AC1014 = R14; AC1015 = AutoCAD 2000;
	AC1018 = AutoCAD 2004
*/ 
public String ACADVER = "AC1009";  // codigo 1


/* Formato de unidades para ángulos 
 	0 = decimal degrees
    1 = degrees / minutes / seconds
    2 = grads
    3 = radians
    4 = surveyor's units 
*/
public int AUNITS = 0; // codigo 70

/* Número de color de la entidad actual:
 	0 = BYBLOCK; 256 = BYLAYER 
*/
public int CECOLOR = 0; // codigo 62

/* Nombre de la capa actual */
public String CLAYER = "0"; // codigo 8
 

/* Nombre de tipo de línea de la entidad, BYBLOCK o BYLAYER  */
public String CELTYPE = "BYLAYER"; // codigo 6

/* Valores X, Y y Z de la esquina inferior izquierda de la extensión del dibujo (en SCU)   */
public Point3 EXTMIN = new Point3(0,0,0); // codigo 10,20,30

/* Valores X, Y y Z de la esquina superior derecha de la extensión del dibujo (en SCU)   */
public Point3 EXTMAX = new Point3(100,100,0); // codigo 10,20,30

// Valores XY de la esquina inferior izquierda de los límites del dibujo (en SCU) 
public Point2 LIMMIN = new Point2(0,0); // codigo 10,20

//Valores XY de la esquina superior derecha de los límites del dibujo (en SCU) 
public Point2 LIMMAX = new Point2(100,100); // codigo 10,20

/* Modo de relleno activado si su valor es distinto de cero  */
public int FILLMODE = 1; // codigo 70

/* Escala global del tipo de línea */
public int LTSCALE = 1; // codigo 40

/* Visualización del polígono de control de Spline: 1 = Act; 0 = Des  */
public int SPLFRAME = 0; // codigo 70

/* 1 para el modo de compatibilidad con la versión anterior; 0 en el caso contrario */
public int TILEMODE = 1; // codigo 70
 


public HeaderSection(){
}


/* Formato Seccion HEADER: 
0					Inicio de la sección HEADER
SECTION
2
HEADER
  
9					Se repite para cada variable de encabezamiento
$<variable>
<código grupo>
<valor> 

0					 Fin de la sección HEADER
ENDSEC 
*/ 

public void write(PrintWriter out){
	out.println("  0\nSECTION");					// Inicio Seccion
	out.println("  2\nHEADER"); 					// Seccion Header
	
	out.println("  9\n$ACADVER\n  1\n"+ACADVER); 	
	out.println("  9\n$AUNITS\n  70\n"+AUNITS); 	
	out.println("  9\n$CECOLOR\n  62\n"+CECOLOR); 	
	out.println("  9\n$CLAYER\n  8\n"+CLAYER); 	
	out.println("  9\n$CELTYPE\n  6\n"+CELTYPE); 	
	out.println("  9\n$EXTMIN\n 10\n"+EXTMIN.x()+"\n 20\n"+EXTMIN.y()+"\n 30\n"+EXTMIN.z()); 	
	out.println("  9\n$EXTMAX\n 10\n"+EXTMAX.x()+"\n 20\n"+EXTMAX.y()+"\n 30\n"+EXTMAX.z()); 	
	out.println("  9\n$LIMMIN\n 10\n"+LIMMIN.x()+"\n 20\n"+LIMMIN.y()); 	
	out.println("  9\n$LIMMAX\n 10\n"+LIMMAX.x()+"\n 20\n"+LIMMAX.y()); 	
	out.println("  9\n$FILLMODE\n  70\n"+FILLMODE); 	
	out.println("  9\n$LTSCALE\n  40\n"+LTSCALE); 	
	out.println("  9\n$SPLFRAME\n  70\n"+SPLFRAME); 	
	out.println("  9\n$TILEMODE\n  70\n"+TILEMODE);
	
	out.println("  0\nENDSEC"); 					// Fin Seccion Header
}


}
