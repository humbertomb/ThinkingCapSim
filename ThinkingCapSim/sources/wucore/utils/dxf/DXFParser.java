/**
 * Title: DXFParser
 * Description: Lector de entidades en ficheros DXF
 * Copyright: Copyright (c) 2000-2001
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

package wucore.utils.dxf;

import java.io.*;
import java.util.*;

import wucore.utils.geom.*;

public class DXFParser
{
	private LineNumberReader fichin;
	private double minX,minY,maxX,maxY;
		
	public DXFParser(String nomfich) throws IOException
	{
		fichin = new LineNumberReader(
				new InputStreamReader(
						new FileInputStream (nomfich)));
		
		minX = minY = Double.MAX_VALUE;
		maxX = maxY = Double.MIN_VALUE;
	}
	
	private void testParam (double x, double y)
	{
		if (x < minX) minX = x;
		if (x > maxX) maxX = x;
		if (y < minY) minY = y;
		if (y > maxY) maxY = y;
	}
	
	public double getminX () { return minX; }
	public double getminY () { return minY; }
	public double getmaxX () { return maxX; }
	public double getmaxY () { return maxY; }
	
	public void leeDXF (Vector lista) throws IOException
	{
		int status;	
		StringTokenizer st;
		String linea;
			
		status = 0;
		while (status != 3)
		{
			linea = fichin.readLine();
			//System.out.println ("linea es: "+linea);
			if (linea == null) status = 3;
			else
			{
				st = new StringTokenizer (linea," \t");
				switch (status)
				{
				case 0: if (st.countTokens() == 1 && (st.nextToken()).equals("0"))
					status = 1;
				break;
				
				case 1: if (linea.trim().equals("POLYLINE"))
				{
					//System.out.println ("Leo polilinea");
					//System.out.println (linea);
					leePoly(lista);
					status = 0;
				}
				else if (linea.trim().equals("LINE"))
				{
					//System.out.println ("Leo linea");
					//System.out.println (linea);
					leeLine(lista);
					status = 0;
				}
				else if (linea.trim().equals("ARC"))
				{
					//System.out.println ("Leo arco");
					//System.out.println (linea);
					leeArc(lista);
					status = 0;
				}
				else if (linea.trim().equals("CIRCLE"))
				{
					//System.out.println ("Leo circulo");
					//System.out.println (linea);
					leeCircle(lista);
					status = 0;
				}
				else if (linea.trim().equals("POINT"))
				{
					//System.out.println ("Leo punto");
					//System.out.println (linea);
					leePoint(lista);
					status = 0;
				}
				else if (linea.trim().equals("$EXTMAX"))
				{
					leeLimite ();					
					status = 0;
				}
				else if (linea.trim().equals("$EXTMIN"))
				{
					leeLimite ();								
					status = 0;
				}
				else if (linea.trim().equals("LAYER"))
				{
					//System.out.println ("\tEstoy en layer");
					status = 2;
				}
				else if (linea.trim().equals("EOF"))
				{
					//System.out.println ("Fin de fichero");
					//System.out.println (linea);
					status = 3;
				}
				break;
				
				case 2: if (linea.trim().equals("ENDSEC"))
				{
					//System.out.println ("\tAcaba layer");
					status = 0;
				}
				break;
				
				default: break;
				}
			}
		}
	}
	
	private void leePoly(Vector l) throws IOException
	{
		boolean salir;
		String linea;
		DXFPolyline poly;
		int conta;
		
		double dx, dy;
		
		linea = fichin.readLine();
		salir = false;
		poly = new DXFPolyline ();
		while (!salir)
		{
			//System.out.println ("linea es: "+linea);
			if (linea.trim().equals("VERTEX"))
			{
				//System.out.println("\tLeo vértice de polilinea");
				dx = dy = Double.NaN;
				conta = 2;
				while (conta != 0)
				{
					linea = fichin.readLine();
					if (linea.trim().equals("10"))
					{
						try { dx = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { dx = 0.0; }
						conta--;
					}
					else if (linea.trim().equals("20"))
					{
						try { dy = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { dy = 0.0; }
						conta--;
					}
				}
				//dx = Double.parseDouble(doublex);
				//dy = Double.parseDouble(doubley);
				testParam (dx,dy);
				
				poly.addPoint (dx,dy);
				//System.out.println ("\tx: "+doublex+" y: "+doubley);
			}
			else if (linea.indexOf("SEQEND") != -1) salir = true;
			linea = fichin.readLine();
		}
		l.addElement(poly);
	}
	
	
	private void leeLine(Vector l) throws IOException
	{
		String doubles;
		double x1,x2,y1,y2;
		Line2 linea;
		int conta;
		
		
		x1 = x2 = y1 = y2 = Double.NaN;
		conta = 4;
		
		while (conta != 0)
		{
			doubles = fichin.readLine();
			if (doubles.trim().equals("10"))
			{
				try { x1 = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { x1 = 0.0; }
				conta--;
			}
			if (doubles.trim().equals("20"))
			{
				try { y1 = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { y1 = 0.0; }
				conta--;
			}
			if (doubles.trim().equals("11"))
			{
				try { x2 = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { x2 = 0.0; }
				conta--;
			}
			if (doubles.trim().equals("21"))
			{
				try { y2 = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { y2 = 0.0; }
				conta--;
			}
		}
		
		testParam (x1,y1);
		testParam (x2,y2);
		
		linea = new Line2 (x1,y1,x2,y2);
		//System.out.println ("\tleida linea: "+linea.toString());
		l.addElement(linea);
	}
	
	private void leeArc(Vector l) throws IOException
	{
		String doubles;
		double x,y,radio,sangle,eangle;
		Arc2 arco;
		int conta;
		
		x = y = radio = sangle = eangle = Double.NaN;
		conta = 5;
		while (conta != 0)
		{
			doubles = fichin.readLine();
			if (doubles.trim().equals("10"))
			{
				try { x = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { x = 0.0; }
				conta--;
			}
			else if (doubles.trim().equals("20"))
			{
				try { y = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { y = 0.0; }
				conta--;
			}
			else if (doubles.trim().equals("40"))
			{
				try { radio = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { radio = 0.0; }
				conta--;
			}
			else if (doubles.trim().equals("50"))
			{
				try { sangle = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { sangle = 0.0; }
				conta--;
			}
			else if (doubles.trim().equals("51"))
			{
				try { eangle = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { eangle = 0.0; }
				conta--;
			}
			
		}
		
		testParam (x,y);
		
		arco = new Arc2 (x,y,radio,sangle,Math.abs(sangle-eangle));
		//System.out.println ("\tleido arco: "+arco.toString());
		l.addElement (arco);
	}
	
	private void leeCircle(Vector l) throws IOException
	{
		String doubles;
		double x,y,radio;
		Ellipse2 circ;
		int conta;
		
		
		x = y = radio = Double.NaN;
		conta = 3;
		while (conta != 0)
		{
			doubles = fichin.readLine();
			if (doubles.trim().equals("10"))
			{
				try { x = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { x = 0.0; }
				conta--;
			}
			else if (doubles.trim().equals("20"))
			{
				try { y = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { y = 0.0; }
				conta--;
			}
			else if (doubles.trim().equals("40"))
			{
				try { radio = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { radio = 0.0; }
				conta--;
			}
		}
		
		testParam (x,y);
		
		circ = new Ellipse2 (x,y,radio*2,radio*2);
		//System.out.println ("\tleido circulo: "+circ.toString());
		l.addElement (circ);
		
	}
	
	private void leePoint(Vector l) throws IOException
	{
		String doubles;
		double x,y;
		Ellipse2 point;
		int conta;
		
		
		x = y = Double.NaN;
		
		conta = 2;
		while (conta != 0)
		{
			doubles = fichin.readLine();
			if (doubles.trim().equals("10"))
			{
				try { x = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { x = 0.0; }
				conta--;
			}
			else if (doubles.trim().equals("20"))
			{
				try { y = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { y = 0.0; }
				conta--;
			}
		}
		
		testParam (x,y);
		
		point = new Ellipse2 (x,y,1,1);
		//System.out.println ("\tleido circulo: "+point.toString());
		l.addElement (point);
	}
		
	/**
	 * @param max
	 * @throws IOException
	 */
	private void leeLimite () throws IOException {
		
		String doubles;
		double x1,y1;
		int conta=3;
		
		x1 = y1 = Double.NaN;
		
		while(conta!=0){			
			doubles = fichin.readLine();
			if (doubles.trim().equals("10"))
			{
				try { x1 = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { x1 = 0.0; }
				conta--;
				
			}
			if (doubles.trim().equals("20"))
			{
				try { y1 = new Double (fichin.readLine()).doubleValue (); } catch (Exception e) { y1 = 0.0; }
				conta--;				
			}			
			if (doubles.trim().equals("30"))
			{
				try { fichin.readLine(); } catch (Exception e) { }
			//	try { String zona = new String (fichin.readLine()).toString(); } catch (Exception e) { }
				conta--;
			}			
		}
		
		testParam (x1,y1);
//		pos.setNorth(y1);
//		pos.setEast(x1);
//		pos.setZone(zona);
	}
}