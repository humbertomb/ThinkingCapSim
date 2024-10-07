package devices.drivers.captors.serial;

import devices.drivers.captors.*;
import javax.comm.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Copyright: Copyright (c) 2001
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero (juanpe@dif.um.es)
 * @version 1.0
 */
 
public class SerialCaptors extends Captors
{
	private static final int	baudrate	=	9600;
	private static final int	databits	= 	8;
	private static final int	stopbits	= 	1;
	private static final int	parity		= 	SerialPort.PARITY_NONE;
	
	private static final int	timeout		= 	3000;

 	private SerialPort 			serie;
  	private InputStream 		is;
  	private OutputStream 		os;
  	private String 				Sport;
  	
  	private int 				captors[];


	Runnable SerialReceiveThread = new Runnable ()
		{
			public void run ()
			{
		    	byte byteL, byteH;
    			int unbyte;
    			int temp;

				System.out.println ("SerialCaptors: Thread Started.");
				while (true)
				{
					byteH = byteL = 0;
					try
    				{
      					//i = 0;
      					while ((unbyte = is.read()) != 0x5);
      					while ((unbyte = is.read()) != 0x5);
      					unbyte = is.read();
      					byteL = (byte)unbyte;
      					unbyte = is.read();
      					byteH = (byte)unbyte;
      					if (((unbyte = is.read()) != 0xA))
      					{
      						//throw (new SerialCaptorsException ("SerialCaptors: Falloo en la recepcion de la trama"));
							System.out.println ("No recibido valor 0xAA cuande se esperaba");
							byteH = byteL = 0;      						
      					}
      					if (((unbyte = is.read()) != 0xA))
      					{
      						//throw (new SerialCaptorsException ("SerialCaptors: Falloo en la recepcion de la trama"));
							System.out.println ("No recibido valor 0xAA cuande se esperaba");
							byteH = byteL = 0;      						
      					}
      					
      				} catch (Exception e)
    				{
      					System.err.println ("SerialCaptors: Excepción al acceder al puerto:"+e);;
      					byteL = byteH = 0;
       				}
       				temp = 0;
       				temp = byteH;
       				temp = temp << 8;
       				temp = temp & 0xFF00;
       				temp = temp + byteL;
    System.out.println ("Reconstruido valor de captores. valor="+temp);
       				captors[0] = temp;
      			}
   			}
         };



	public void init (String p) throws SerialCaptorsException
	{
		CommPortIdentifier id;
		
		/* 
			la cadena del fichero de properties debe ser:
			CAPTORSX=devices.drivers.captors.UDP.UDPCaptors|<puertoserie>
			La clase la ha recogido la clase padre y aqui sólo pasa el 
			puerto serie por donde recibirá los datos
		*/
		Sport = p;
		
    	try
    	{
        	id = CommPortIdentifier.getPortIdentifier (p);
    	} catch (NoSuchPortException npe)
    	{
        	throw (new SerialCaptorsException ("El puerto especificado ("+p+") no existe."));
    	}

    	try
    	{
        	serie = (SerialPort) id.open ("ANTS Serial Captors",5000);
    	} catch (PortInUseException pie)
    	{
        	throw (new SerialCaptorsException ("El puerto especificado ("+p+") esta en uso."));
    	}

    	try
    	{
        	serie.setSerialPortParams(baudrate,databits,stopbits,parity);
        	serie.setFlowControlMode (SerialPort.FLOWCONTROL_NONE);
        	//serie.enableReceiveTimeout(timeout);
    	} catch (UnsupportedCommOperationException ucoe)
    	{
        	throw (new SerialCaptorsException ("No se pudo configurar el puerto "+p+"."));
    	}

    	try
    	{
        	is = serie.getInputStream();
        	os = serie.getOutputStream();
    	} catch (IOException ioe)
    	{
        	throw (new SerialCaptorsException ("No se pudo obtener los Streams del puerto."));
    	}
    	
    	captors = new int[1];
    	captors[0] = 0;
		
		(new Thread (SerialReceiveThread)).start();
		System.out.println ("Serial Captors waiting in port "+Sport);

	}
	
  	
	/*private void init_serialCaptors () throws SerialCaptorsException
	{
		String response;
		
		send ("V");
		serie.enableReceiveTimeout(timeout);
		response = get ();
		if (r.startsWith("ANTSSerialCaptors"))
    	{
			System.out.println ("SerialCaptors detected.");	    
		}
		else
		{
			throw (new SerialCaptorsException ("[init]: No serial captors"));
		}
	}*/
	
	public int[] getValues () throws SerialCaptorsException
	{
		return (captors);
	}

}