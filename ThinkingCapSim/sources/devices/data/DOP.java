package devices.data;

/**
 * Title: DOP
 * Description: Información sobre Dilution of Precision
 * Copyright: Copyright (c) 2001
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

import java.io.*;

public class DOP implements Serializable
{
	
	private int numsat;
	private double gdop; //DOP geométrica
	private double pdop; //DOP de la posición
	private double htdop; //DOP horizontal y del tiempo
	private double hdop; //DOP horizontal
	private double tdop; //dop del tiempo
	
	public DOP(double g, double p, double ht, double h, double t, int ns)
	{
		gdop = g;
		pdop = p;
		htdop = ht;
		hdop = h;
		tdop = t;
		numsat = ns;
	}
	
	public DOP()
	{
		gdop = Double.NaN;
		pdop = Double.NaN;
		htdop = Double.NaN;
		hdop = Double.NaN;
		tdop = Double.NaN;
	}
	
	public boolean isValid()
	{
		return (pdop != Double.NaN && hdop != Double.NaN && tdop != Double.NaN);
	}
	
	public void setGDOP(double g)
	{
		gdop = g;
	}
	
	public void setPDOP(double p)
	{
		pdop = p;
	}
	
	public void setHTDOP(double ht)
	{
		htdop = ht;
	}
	
	public void setHDOP(double h)
	{
		hdop = h;
	}
	
	public void setTDOP(double t)
	{
		tdop = t;
	}
	
	public void setNumSat (int ns)
	{
		numsat = ns;
	}
	
	public double getGDOP () {return (gdop); }
	
	public double getPDOP () {return (pdop); }
	
	public double getHTDOP () {return (htdop); }
	
	public double getHDOP () {return (hdop); }
	
	public double getTDOP () {return (tdop); }
	
	public double getNumSat () { return (numsat); }
	
	public String toString ()
	{
		return (new String ("DOP: g="+gdop+", p="+pdop+", h="+hdop+", ht="+htdop+
				", t="+tdop+" NUMSAT: "+numsat));
	}
	
	public String toString2 ()
	{
		return (new String ("DOP: "+gdop+","+pdop+","+hdop+","+htdop+
				","+tdop+" ns="+numsat));
	}
	
	
}