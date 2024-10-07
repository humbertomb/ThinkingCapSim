/**
 * Copyright: Copyright (c) 2002
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Canovas Quionero (juanpe@dif.um.es)
 * @version 1.0
 */

package devices.drivers.camera.serialcam;

import java.io.*;
import java.util.*;
import javax.comm.*;

import devices.drivers.camera.*;
import devices.utils.*;

public class SerialCamServer extends Thread
{
	private Camera			driver;

  private CommPortIdentifier portid;
  private SerialPort sport;
  private OutputStream os;
  private InputStream is;

  private boolean bidir;


	public SerialCamServer (Camera cam, String params)
	{
		super ("SerialCamServer");

		driver = cam;

		try
		{
			initialise (params);
		} catch (Exception e)
		{
			System.out.println ("SerialCamServer exception: "+e);
			System.exit (0);
		}
	}

  public void initialise (String portparams) throws CameraException
  {
       String puerto, parity, mode;
       int baudios, databits, stopbits, parityval;

      /*=========================================================
        = portparams is:                                        =
        =   port, baudios, nrodatabits, parity, stopbits, mode  =
        = with:                                                 =
        =       parity = odd | even | none                      =
        =       mode = bidir | unidir                           =
        =========================================================*/

      //Parse portparams
      try
      {
        StringTokenizer st = new StringTokenizer (portparams,", ");
		    puerto = st.nextToken ();
        baudios = Integer.parseInt (st.nextToken());
        databits = Integer.parseInt (st.nextToken());
        parity = st.nextToken ();
        if (parity.equalsIgnoreCase ("odd")) parityval = SerialPort.PARITY_ODD;
        else if (parity.equalsIgnoreCase ("even")) parityval = SerialPort.PARITY_EVEN;
        else parityval = SerialPort.PARITY_NONE;
        stopbits = Integer.parseInt (st.nextToken());
        mode = st.nextToken ();
        bidir = mode.equalsIgnoreCase ("bidir");
      } catch (Exception E)
      {
        throw (new CameraException (E.toString()));
      }

      //Check values
      switch (baudios)
      {
        case 2400:
        case 9600:
        case 19200:
        case 38400:
        case 57600:
        case 115200: break;

        default: throw (new CameraException ("Invalid value for baudios: "+baudios));
      }

      switch (databits)
      {
        case 7:
        case 8: break;

        default: throw (new CameraException ("Invalid value for databits: "+databits));
      }
      
      switch (stopbits)
      {
        case 0:
        case 1:
        case 2: break;

        default: throw (new CameraException ("Invalid value for stopbits: "+stopbits));
      }

      //Open & configure port
      try
      {
      	portid = CommPortIdentifier.getPortIdentifier (puerto);
      } catch (NoSuchPortException NSPE)
      {
       	throw (new CameraException (NSPE.toString()));
      }

      try
      {
   		  sport = (SerialPort)portid.open("SerialCam",10000);
      } catch (PortInUseException PIUE)
      {
       	throw (new CameraException (PIUE.toString()));
      }

      try
      {
	   	  sport.setSerialPortParams(baudios, databits,stopbits, parityval);
        sport.disableReceiveTimeout ();
        sport.disableReceiveThreshold ();
        sport.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
      } catch (UnsupportedCommOperationException UCOE)
      {
       	sport.close();
       	throw (new CameraException (UCOE.toString()));
      }

      try
      {
        os = sport.getOutputStream();
        is = sport.getInputStream();
      } catch (IOException IOE)
      {
       	sport.close();
        throw (new CameraException (IOE.toString()));
      }

      System.out.println ("SerialCamServer on port "+puerto+" with params: "+baudios+", "+databits+", "+parity+", "+stopbits);
  }


	public void run ()
	{
    byte onebyte = 0;
    byte twobytes[] = new byte[2];
    byte bytes[] = null;
    boolean receivedok, crcok;

    short crcval;

    
		while (true)
		{
      receivedok = false;
      crcok = true;
      try
			{
        //wait for STX
        onebyte	= (byte) is.read ();
        if (onebyte == SerialCam.STX)
        {
          //read length
          onebyte = (byte) is.read ();
          bytes = new byte[onebyte];
          //read command;
          is.read (bytes);
          //read CRC
          is.read (twobytes);
          crcval = (short)twobytes[0];
          crcval = (short) ((crcval << 8) & 0xFF00);
          crcval = (short) (crcval + twobytes[1]);
          receivedok = true;
          crcok = CRC16.check (bytes,crcval);
        }
			}
			catch (Exception e)
			{
				System.out.println ("SerialCamServer.run exception: "+e);
			}
			if (receivedok && crcok)
			{
				//System.out.print ("Recibo: \""+new String (bytes)+"\"");
				StringTokenizer st = new StringTokenizer (new String (bytes),"|");
				//System.out.println ("("+st.countTokens()+" tokens)");

				if (st.countTokens () == 3)
				{
					Comando c = new Comando (st.nextToken(),st.nextToken(),st.nextToken());

					boolean b = driver.send (c);
					//System.out.println ("Envio: "+c.orden+". Driver devuelve: "+b);

          if (bidir)
          {
            if (b) onebyte = SerialCam.ACK;
            else onebyte = SerialCam.NAK;
            try
            {
              os.write (onebyte);
            } catch (Exception e)
					  {
						  System.out.println ("SerialCamServer: Exception sending response: "+e);
					  }
          }
				}
				else if (st.countTokens () == 4)
				{
					Comando c = new Comando (st.nextToken(),st.nextToken(),st.nextToken());
					int n;
					String stnum = st.nextToken();
					try { n = Integer.parseInt (stnum); }
					catch (Exception e)
					{
						System.out.println ("Excepcion: "+e);
						e.printStackTrace();
						System.out.println ("stnum es: \""+stnum+"\"");
						n = 0;
					}
					boolean b = driver.send (c,n);

          if (bidir)
          {
            if (b) onebyte = SerialCam.ACK;
            else onebyte = SerialCam.NAK;
            try
            {
              os.write (onebyte);
            } catch (Exception e)
					  {
						  System.out.println ("SerialCamServer: Exception sending response: "+e);
					  }
          }
					//System.out.println ("Comando long. 4. Devuelvo: "+new String (bytes));
				}
				else System.out.println ("UDPCamServer.run: Unknown bytes Received");
			}
      else if (!crcok)//CRC error
      {
        System.out.println ("SerialCamServer: data received with CRC error."); 
        if (bidir)
        {
          try
          {
            os.write (SerialCam.ERR);
          } catch (Exception e)
				  {
				    System.out.println ("SerialCamServer: Exception sending response: "+e);
				  }
        } 
      }
		}
	}

	public static void main (String args[])
	{
		Camera cam = null;
		try
		{
			cam = Camera.getCamera (args[0]);
		} catch (Exception e)
		{
			System.out.println ("Exception: "+e);
			e.printStackTrace();
			System.exit (0);
		}

		SerialCamServer serv = new SerialCamServer (cam,args[1]);
		serv.start ();
	}
}
