/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:
 * @author
 * @version 1.0
 */

package devices.drivers.gps.Trimble;

import java.io.*;

import devices.data.*;
import devices.drivers.gps.*;

import devices.pos. *;
import wucore.utils.geom.*;

public class TrimbleInterpreter
{

  private static byte NOID = 0x00;
  private static byte DLE = 0x10;
  private static byte ETX = 0x03;
  private static byte RVER = 0x1F;
  private static byte RPOS = 0x37;
  private static byte RDOP = 0x24;
  private static byte RSAT = 0x6E;
  private static byte RSEC = 0x21;
  private static byte VER = 0x45;
  //En Java los bytes van de -127 a 127, con lo que valores mayores de 128 (0x80)
  //son realmente números negativos.
  private static int tmp1 = 0x84;
  private static int tmp2 = 0x83;
  private static int tmp3 = 0x8F;
  //private static byte PLLA = (byte)tmp1; //0x84;
  private static byte SPACKET = (byte)tmp3; //0x8F
  private static byte PLLA = 0x23; //PLLA = superpacket 0x8F-23
  private static byte PXYZ = (byte)tmp2; //0x83;
  private static byte VENU = 0x56;
  private static byte DOP = 0x6D;
  private static byte SAT = 0x6F;
  private static byte SECS = 0x41;

  private static byte ASKVER[] = {DLE,RVER,DLE,ETX};
  private static byte ASKPOS[] = {DLE,RPOS,DLE,ETX};
  private static byte ASKDOP[] = {DLE,RDOP,DLE,ETX};
  private static byte ASKSAT[] = {DLE,RSAT,0x01,0x01,0x01,DLE,ETX};
  private static byte ASKSEC[] = {DLE,RSEC,DLE,ETX};
  
  private boolean satrequested = false;
  private InputStream is;
  private OutputStream os;
  protected double height;

	// Constructor
  public TrimbleInterpreter (InputStream input, OutputStream output)
  {
    is = input;
    os = output;
  }

  // Class methods
  static public double getDoubleValue (byte barray[], int l, int h) throws IOException
  {
	long valor;
	long aux;
	long mask;
	int i;


	if (l < 0 || h >= barray.length || l >= barray.length) return (Double.NaN);

	try
	{
	  valor = 0;
	  aux = 0;
	  for (i=0; i < 8; i++)
	  {
		aux = barray[l+i];
		aux = aux << (8*(7-i));
		switch (7-i)
		{
		  case 0: mask = 0x00000000000000ffL;
				  break;
		  case 1: mask = 0x000000000000ff00L;
				  break;
		  case 2: mask = 0x0000000000ff0000L;
				  break;
		  case 3: mask = 0x00000000ff000000L;
				  break;
		  case 4: mask = 0x000000ff00000000L;
				  break;
		  case 5: mask = 0x0000ff0000000000L;
				  break;
		  case 6: mask = 0x00ff000000000000L;
				  break;
		  case 7: mask = 0xff00000000000000L;
				  break;
		  default:mask = 0x0000000000000000L;
		}
		aux = aux&mask;
		valor = valor+aux;
		aux = aux&0;
	  }
	} catch (ArrayIndexOutOfBoundsException aobe)
	{
	  return (Double.NaN);
	}
	//System.out.println (" ==> "+Double.longBitsToDouble (valor));
	return (Double.longBitsToDouble (valor));
  }

  static public float getFloatValue (byte barray[], int l, int h) throws IOException
  {
	int valor;
	int aux;
	int mask;
	int i;


	if (l < 0 || h >= barray.length || l >= barray.length) return (Float.NaN);
	valor = 0;
	aux = 0;
	try
	{
	  for (i=0; i < 4; i++)
	  {
		aux = barray[l+i];
		aux = aux << (8*(3-i));
		switch (3-i)
		{
		  case 0: mask = 0x000000ff;
				  break;
		  case 1: mask = 0x0000ff00;
				  break;
		  case 2: mask = 0x00ff0000;
				  break;
		  case 3: mask = 0xff000000;
				  break;
		  default:mask = 0x00000000;
		}
		aux = aux&mask;
		valor = valor+aux;
		aux = aux&0;
	  }
	} catch (ArrayIndexOutOfBoundsException aobe)
	{
	  return (Float.NaN);
	}
	//System.out.println (" ==> "+Float.intBitsToFloat (valor));
	return (Float.intBitsToFloat (valor));
  }

  static public int getINT32Value (byte barray[], int l, int h) throws IOException
  {
	int valor;
	int aux;
	int mask;
	int i;

	if (l < 0 || h >= barray.length || l >= barray.length) return (Integer.MAX_VALUE);

	valor = 0;
	aux = 0;
	try
	{
	  for (i=0; i < 4; i++)
	  {
		aux = barray[l+i];
		aux = aux << (8*(3-i));
		switch (3-i)
		{
		  case 0: mask = 0x000000ff;
				  break;
		  case 1: mask = 0x0000ff00;
				  break;
		  case 2: mask = 0x00ff0000;
				  break;
		  case 3: mask = 0xff000000;
				  break;
		  default:mask = 0x00000000;
		}
		aux = aux&mask;
		valor = valor+aux;
		aux = aux&0;
	  }
	} catch (ArrayIndexOutOfBoundsException aobe)
	{
	  return (Integer.MAX_VALUE);
	}
	return (valor);
  }

  // Instance methods
  protected void initTrimble () throws IOException
  {

    byte rec[];
    int		a1, a2;

    sendPacket (ASKVER);
    rec = recPacket (VER,NOID).getBytes();

    if (rec == null || rec.length <= 0 )
    {
      //System.err.println("initTrimble: rec es null·");
      throw (new IOException ("No se encontró un GPS Trimble - TSIP."));

    }
    else
    {
      System.out.println ("\tTRIMBLE TSIP GPS detected");

		if (rec[5] > 50)
			a1 = 1900 + rec[5];
		else
			a1 = 2000 + rec[5];
		
		if (rec[10] > 50)
			a2 = 1900 + rec[10];
		else
			a2 = 2000 + rec[10];
		
      System.out.print ("\tNavigation Processor version "+rec[1]+"."+rec[2]+". ");
      System.out.println (rec[3]+"/"+rec[4]+"/"+a1);
      System.out.print ("\tSignal Processor version "+rec[6]+"."+rec[7]+". ");
      System.out.println (rec[8]+"/"+rec[9]+"/"+a2);
    }
  }


  private void sendPacket (byte paq[]) throws IOException
  {
     os.write(paq);
  }

  private String recPacket (byte id, byte subid) throws IOException
  {
    byte buf[];
    int unbyte,i,status;
    /*status: 0 = Waiting for DLE
              1 = DLE received
              2 = ID received
              3 = Retrieving packet
              4 = Packet complete
    */

    //System.out.println ("esperando id:"+Integer.toHexString(id));

    unbyte = i = status = 0;
    buf = new byte[1024];
    while (status != 4)
    {
      unbyte = is.read();
      //System.out.println ("\tunbyte = "+Integer.toHexString(unbyte));
      if (unbyte == -1)
      {
        //System.out.println ("recibido unbyte = -1");
        i = 0;
        status = 4;
      }
      switch (status)
      {
        //waiting for DLE
        case 0: if ((byte)unbyte == DLE)
                {
                  status = 1;
                  //System.out.println ("\tDLE Received.");
                }
                break;

        //DLE received
        case 1: if ((byte)unbyte == id)
                {
                  if (id == SPACKET)
                  {
                    //System.out.println ("\tSPACKET Received "+Integer.toHexString(unbyte));
                    status = 2;
                  }
                  else
                  {
                    status = 3;
                    //System.out.println ("\tID Received "+Integer.toHexString(unbyte));
                    buf[i++] = (byte)unbyte;
                  }
                }
                else
                {
                  //System.out.println ("\tReceived "+Integer.toHexString(unbyte)+
                  //                    " instead of id "+Integer.toHexString(id));
                  status = 0;
                }
                break;

        //ID received
        case 2: if ((byte)unbyte == subid)
                {
                  //System.out.println ("\tSUBID Received: "+Integer.toHexString(unbyte));
                  status = 3;
                  buf[i++] = (byte)unbyte;
                }
                else status = 0;
                break;

        //Retrieving packet
        case 3: if ((byte)unbyte == DLE)
                {
                  unbyte = is.read();
                  if ((byte)unbyte == ETX)
                  {
                    //System.out.println ("\tETX Received.");
                    status = 4; //Packet complete
                  }
                  else buf[i++] = (byte)unbyte;
                }
                else buf[i++] = (byte)unbyte;
                break;

        default: status = 4;
                 break;
      }
    }
    return (new String (buf,0,i));
  }

  public LLAPos getPos () throws IOException
  {
    byte rec[];
    int tlat;
    int tlong;
    int talt;
     double lat;
    double longi;

    sendPacket (ASKPOS);
    rec = recPacket (SPACKET,PLLA).getBytes();

    if (rec == null || rec.length <= 0)
      return (new LLAPos (-Double.NaN,-Double.NaN));

    tlat = getINT32Value (rec,9,12);
    tlong = getINT32Value (rec,13,16);
    talt = getINT32Value (rec,17,20);

    lat = tlat * GPS.SEMIC_UNIT;
    longi = tlong * GPS.SEMIC_UNIT;
    height = talt/1000.0;

    //System.out.println ("\tFIX slnt. vale: "+Integer.toHexString(rec[8]));
 /*   if ((rec[8] & 0x01) != 0) tfix = GeoPos.INVALID_FIX;
    else if ((rec[8] & 0x02) == 0x02) tfix = GeoPos.DGPS_FIX;
         else tfix = GeoPos.GPS_FIX;
*/
    return new LLAPos (lat,longi);
  }

  public Point3 getVel () throws IOException
  {
  	Point3 vel;
    byte rec[];

    sendPacket (ASKPOS);
    rec = recPacket (VENU,NOID).getBytes();
    if (rec == null || rec.length <= 0)			return null;
    
    vel	= new Point3 ();
    vel.x (getFloatValue (rec,1,4));		// Vel east
    vel.y (getFloatValue (rec,5,8));		// Vel north
    vel.z (getFloatValue (rec,9,12));		// Vel up
    
    return vel;
  }

  public int getNumSat () throws IOException
  {
    byte rec[];
    int val;

    sendPacket (ASKDOP);
    rec = recPacket (DOP,NOID).getBytes();
    if (rec == null || rec.length <= 0)
      return (0);

    if (rec.length < 16)
    {
      //System.out.println("\tLa respuesta para DOP es de "+rec.length+" bytes");
      return (0);
    }
    val = rec[1] & 0xF0;
    val = val >> 4;

    return (val);
  }

  public SatelliteData[] getSV () throws IOException
  {
    int i;
    int ns;
    int tprn;
    int telev;
    int tazim;
    int tsnr;
    byte rec[];
    SatelliteData[] tlista;

    //System.out.println ("TrimbleInterpreter-getSV: pido satelites.");
    sendPacket (ASKSAT);
    //satrequested = true;
    //System.out.println ("TrimbleInterpreter-getSV: espero satelites.");
    rec = recPacket(SAT,NOID).getBytes();
    //System.out.println ("TrimbleInterpreter-getSV: paquete recibido.");
    if (rec == null || rec.length <= 0)
      return (new SatelliteData[0]);

    ns = (int)rec[21];
    //System.out.println ("TrimbleInterpreter-getSV: "+ns+" sats");

    tlista = new SatelliteData[ns];
    for (i=0; i < ns; i++)
    {
        tprn = (int)rec[22+(27*i)];
        //System.out.println ("Trimble getSV: tprn="+tprn);
        telev = (int)rec[25+(27*i)];
        //System.out.println ("Trimble getSV: telev="+telev);
        tazim = (int)rec[26+(27*i)];
        tazim = tazim & 0x000000ff;
        //System.out.println ("Trimble getSV: tazim(byte 1)="+Integer.toHexString(tazim));
        tazim = tazim << 8;
        tazim = tazim + (rec[27+(27*i)]&0x000000ff);
        //System.out.println ("Trimble getSV: tazim="+Integer.toHexString(tazim));
        tsnr = (int)rec[28+(27*i)];
        //System.out.println ("Trimble getSV: tsnr="+tsnr);
        //tsnr = tsnr*4; //cuando devuelve SNR en AMUs (configuración del receptor)
        tlista[i] = new SatelliteData (tprn,telev, tazim, tsnr);
    }

    return (tlista);
  }

  public DOP getDOP () throws IOException
  {
    byte rec[];
    DOP dop = new DOP();
    int i,tsat;

    //System.out.println ("pido DOP");
    sendPacket (ASKDOP);
    rec = recPacket (DOP,NOID).getBytes();
    if (rec == null || rec.length <= 0)
      return (new DOP());

    if (rec.length < 16)
    {
      System.out.println("La respuesta para DOP es de "+rec.length+" bytes");
      for (i=0; i < rec.length; i++)
      {
        System.out.println ("\t\tbyte "+i+": "+Integer.toHexString(rec[i]));
      }

      return (new DOP(0.0,0.0,0.0,0.0,0.0,0));
    }

    dop.setPDOP((double)getFloatValue(rec,2,5));
    dop.setHDOP((double)getFloatValue(rec,6,9));
    dop.setGDOP((double)getFloatValue(rec,10,13));
    dop.setTDOP((double)getFloatValue(rec,14,17));
    tsat = rec[1] & 0xF0;
    tsat = tsat >> 4;
    dop.setNumSat (tsat);

    return (dop);
  }

  public float getWeekSeconds () throws IOException
  {
    byte rec[];
    float secs;

    //System.out.println ("pido DOP");
    sendPacket (ASKSEC);
    rec = recPacket (SECS,NOID).getBytes();
    if (rec == null || rec.length <= 0) return (Float.NaN);

    secs = getFloatValue(rec,1,4);

    if (secs < 0) return (Float.NaN);
    else return (secs);
  }


}