package devices.drivers.captors.UDP;

import devices.drivers.captors.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

/**
 * Copyright: Copyright (c) 2001
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero (juanpe@dif.um.es)
 * @version 1.0
 */

public class UDPCaptors extends Captors 
{
	private DatagramSocket 	sock;
	private int 			port;
	private int				captors[];
	
	Runnable UDPReceiveThread = new Runnable ()
		{
			public void run ()
			{
				byte udpbuff[];
				DatagramPacket dp;
				String cadena,subcommand;
				
				Thread.currentThread ().setName ("Thread-UDPCaptors");	
				System.out.println ("  [UDPCaptors] UDP Captors waiting in port "+port);
     
				while (true)
				{
					udpbuff = new byte[256];
					dp = new DatagramPacket (udpbuff,256);
					try
					{
						sock.receive(dp);
					} catch (Exception e)
					{
						System.err.println ("--[UDPCaptors] Exception reading socket: "+e);
						dp.setLength(0);
					}
					if (dp.getLength() != 0)
					{
						cadena = new String (dp.getData(),0,dp.getLength());
						//System.out.println ("UDPCaptors recibido: "+cadena);
            			subcommand = cadena.substring(0,cadena.indexOf('|'));
            			if (subcommand.equalsIgnoreCase("cap"))
            			{
              				//la cadena recibida será:
              				// cap|c1|c2|c3|c4
              				int pipe1, pipe2, pipe3, pipe4;

              				pipe1 = cadena.indexOf('|');
              				pipe2 = cadena.indexOf('|',pipe1+1);
              				pipe3 = cadena.indexOf('|',pipe2+1);
              				pipe4 = cadena.indexOf('|',pipe3+1);

              				captors[0] = Integer.parseInt(cadena.substring(pipe1+1,pipe2));
              				captors[1] = Integer.parseInt(cadena.substring(pipe2+1,pipe3));
              				captors[2] = Integer.parseInt(cadena.substring(pipe3+1,pipe4));
              				captors[3] = Integer.parseInt(cadena.substring(pipe4+1,cadena.length()));
              			}
            		}
            	}
            }
         };

	public void init (String p) throws UDPCaptorsException
	{
		/* 
			la cadena del fichero de properties debe ser:
			CAPTORSX=devices.drivers.captors.UDP.UDPCaptors|<nropuerto>
			La clase la ha recogido la clase padre y aqui sólo pasa el puerto
			donde quedará a la escucha
		*/
		try
		{
			port = Integer.parseInt (p);
			sock = new DatagramSocket (port);
			captors = new int[4];
			captors[0] = captors[1] = captors[2] = captors[3] = 0;
		} catch (Exception e)
		{
			throw (new UDPCaptorsException (e.toString()));
		}
		(new Thread (UDPReceiveThread)).start();
	}
	
	public int[] getValues () throws UDPCaptorsException
	{
		return (captors);
	}
}
