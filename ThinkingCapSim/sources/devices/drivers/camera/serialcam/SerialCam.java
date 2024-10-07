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
import devices.utils.CRC16;

public class SerialCam extends Camera
{
    public static final byte STX   = (byte) 0xFF;
    public static final byte ACK   = (byte) 0xFE;
    public static final byte NAK   = (byte) 0xFD;
    public static final byte ERR   = (byte) 0xFC;
                                                            
    private CommPortIdentifier portid;
    private SerialPort sport;
    private OutputStream os;
    private InputStream is;

    private boolean bidir;

    // Constructors
    public SerialCam ()
    {
    	super ();
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

      //Open port
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
        sport.enableReceiveTimeout (500);
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

      System.out.println ("SerialCam on port "+puerto+" with params: "+baudios+", "+databits+", "+parity+", "+stopbits);
    }

    public void close () throws CameraException
    {
      sport.close();
      return;
    }

    /*
      We send: STX|datalength|<command bytes>|CRC16
      length is one byte, so datalength must be less or equal than 255
      CRC16 is two bytes.
    */
    public boolean send (Comando command)
    {
      byte[] combytes = command.toBytes();
      byte[] sendbytes = new byte[combytes.length+4];
      byte response = 0;
      int i;
      short crc;

      if (combytes.length > 255)
      {
        System.out.println ("Command length must be less or equal than 255.");
        return (false);
      }
      sendbytes[0] = STX;
      sendbytes[1] = (byte)combytes.length;

      for (i=0; i < combytes.length; i++)
        sendbytes[i+2] = combytes[i];

       
      crc = CRC16.calculate (combytes);

      sendbytes[i+2] = (byte)((crc & 0xFF00) >> 8);
      sendbytes[i+3] = (byte)((crc & 0x00FF));

      try
      {
        os.write (sendbytes);
        if (bidir)
          response = (byte)is.read ();
        else response = SerialCam.ACK;         
      } catch (Exception e)
      {
        System.out.println ("SerialCam.send: exception "+e);
    		return (false);
      }

      return (response == SerialCam.ACK);
    }


    public boolean send (Comando comando, int numero)
    {
    	String strconc = new String (comando.toBytes()) + "|" + Integer.toString (numero);
      byte[] combytes = strconc.getBytes();
      byte[] sendbytes = new byte[combytes.length+4];
      byte response = 0;
      int i;
      short crc;

      if (combytes.length > 255)
      {
        System.out.println ("Command length must be less or equal than 255.");
        return (false);
      }
      sendbytes[0] = STX;
      sendbytes[1] = (byte)combytes.length;

      for (i=0; i < combytes.length; i++)
        sendbytes[i+2] = combytes[i];


      crc = CRC16.calculate (combytes);

      sendbytes[i+2] = (byte)((crc & 0xFF00) >> 8);
      sendbytes[i+3] = (byte)((crc & 0x00FF));

      try
      {
        os.write (sendbytes);
        if (bidir)
          response = (byte)is.read ();
        else response = SerialCam.ACK;
      } catch (Exception e)
      {
        System.out.println ("SerialCam.send: exception "+e);
    		return (false);
      }

      return (response == SerialCam.ACK);
    }

    public static String codeString (byte code)
    {
      switch (code)
      {
        case STX: return ("Start of transmision");

        case ACK: return ("Acknowledge");

        case NAK: return ("Not ACK");

        case ERR: return ("CRC error");

        default: return ("Unknown code"); 
      }   
    }
}
