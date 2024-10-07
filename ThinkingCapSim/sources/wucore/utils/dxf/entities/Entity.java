/**
 * Title: Line
 * Description: Clase abstracta de una Entidad.
 * Se le han añadido la posibilidad de definir propiedades 
 * extendidas de tipo String, Int, Double que no son visibles o editables
 * directamente mediante Autocad (solo utilizando una aplicacion AUTOLISP).  
 * @author Jose Antonio Marin
 * @version 1.0
 */

package wucore.utils.dxf.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import wucore.utils.geom.Point2;
import wucore.utils.geom.Point3;

public abstract class Entity{

	// Para definir unos limites maximos y minimo (no es imprescindible)
	static public Point2 LIMMIN;
	static public Point2 LIMMAX;
	
	// Contador id para las entidades
	static int idcont = 30;
	
	//	 Identificador (unico para cada entidad)
	protected String id;
	
	// Capa (por defecto "0")
	protected String layer;
	
	// Indice de color 1-255 (256 por capa)
	protected int color = 256;
	
	// Para poder añadir propiedades Extendidas
	public Vector ExtendedInt;
	public Vector ExtendedDouble;
	public Vector ExtendedText;

	
	public Entity(){
		layer = "0";
		id = "0";
		LIMMIN = new Point2();
		LIMMAX = new Point2();
		ExtendedInt = new Vector();
		ExtendedDouble = new Vector();
		ExtendedText = new Vector();
	}
	
	
	public void setLayer(String layer)	{	this.layer = layer;}
	public String getLayer()			{		return layer;	}
	
	public void checkLimits(Point3 pos){
		if(pos.x()<LIMMIN.x())  LIMMIN.x(pos.x());
		if(pos.y()<LIMMIN.y())  LIMMIN.y(pos.y());
		if(pos.x()>LIMMAX.x())  LIMMAX.x(pos.x());
		if(pos.y()>LIMMAX.y())  LIMMAX.y(pos.y());
	}
	
	// Calcula una ID valida que no coincida con ninguna otra entidad
	public void calculateID()	    {		id = Integer.toHexString(idcont++).toUpperCase();		}
	
	public void setID(String id)	{		this.id = id; 	}
	public String getID()	   		{		return id;		}
	
	public void addExtInt(int data)			{ExtendedInt.add(new Integer(data));}
	public void addExtText(String data)		{ExtendedText.add(data);}
	public void addExtDouble(double data)	{ExtendedDouble.add(new Double(data));}
	
	public void addExtInt(int index, int data)			{ExtendedInt.add(index, new Integer(data));}
	public void addExtText(int index, String data)		{ExtendedText.add(index, data);}
	public void addExtDouble(int index, double data)	{ExtendedDouble.add(index, new Double(data));}
	
	public int getExtInt(int index)			{return ((Integer)ExtendedInt.get(index)).intValue();}
	public String getExtText(int index)		{return (String)ExtendedText.get(index);}
	public double getExtDouble(int index)	{return ((Double)ExtendedDouble.get(index)).doubleValue();}

	public int ExtIntSize()		{return ExtendedInt.size();}
	public int ExtTextSize()	{return ExtendedText.size();}
	public int ExtDoubleSize()	{return ExtendedDouble.size();}

	public void setEntProp(Entity ent){
	    this.layer = ent.layer;
	    this.color = ent.color;
	    this.ExtendedDouble = ent.ExtendedDouble;
	    this.ExtendedInt = ent.ExtendedDouble;
	    this.ExtendedText = ent.ExtendedDouble;
	    this.id = ent.id;  
	}
	
	protected String toExtString(){
		if(ExtendedInt.size()==0 && ExtendedDouble.size()==0 && ExtendedText.size()==0) return "";
		String ret = "\tExtendedData: ";
		if(ExtendedInt.size()>0){
			ret += "INT{ ";
			for(int i = 0;i<ExtendedInt.size();i++)
				ret += "'"+ExtendedInt.get(i)+"' ";
			ret += "} ";
		}
		if(ExtendedDouble.size()>0){
			ret += "DOUBLE{ ";
			for(int i = 0;i<ExtendedDouble.size();i++)
				ret += "'"+ExtendedDouble.get(i)+"' ";
			ret += "} ";
		}
		if(ExtendedText.size()>0){
			ret += "TEXT{ ";
			for(int i = 0;i<ExtendedText.size();i++)
				ret += "'"+ExtendedText.get(i)+"' ";
			ret += "} ";
		}
		return ret;
	}
	
	public void writeExt(PrintWriter out){
		if(ExtendedInt.size()==0 && ExtendedDouble.size()==0 && ExtendedText.size()==0) return;
		
		out.println(" 1001\n"+"TCII");	// Nombre de la aplicacion
		out.println(" 1002\n"+"{");
		
		if(ExtendedInt.size()>0)
			for(int i = 0;i<ExtendedInt.size();i++)
				out.println(" 1071\n"+ExtendedInt.get(i));
		
		if(ExtendedDouble.size()>0)
			for(int i = 0;i<ExtendedDouble.size();i++)
				out.println(" 1040\n"+ExtendedDouble.get(i));
		
		if(ExtendedText.size()>0)
			for(int i = 0;i<ExtendedText.size();i++)
				out.println(" 1000\n"+ExtendedText.get(i));
		out.println(" 1002\n"+"}");
	}
	
	// Metodos Abstractos
	
	//	Lee una entidad a partir de un Reader
	public abstract void read(BufferedReader br) throws IOException;
	
	// 	Escribe en un Writer la Entidad (asegurarse de calcular la ID antes de escribir la entidad)
	public abstract void write(PrintWriter out);
	
	// 	Representacion de la linea
	public abstract String toString();
	
}