/**
 * Copyright: Copyright (c) 2002
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Canovas Qui–onero (juanpe@dif.um.es)
 * @version 1.0
 */

package devices.drivers.camera.UDPCam;

import java.net.*;
import java.util.*;

import devices.drivers.camera.*;

public class UDPCamServer extends Thread
{
	private Camera			driver;
	private DatagramSocket 	sock;
	private final boolean 	DEBUG = true;	
	
	public UDPCamServer (Camera cam, int porttolisten)
	{
		super ("UDPCamServer");
		
		driver = cam;
		
		try 
		{
			sock = new DatagramSocket (porttolisten);
		} catch (Exception e)
		{
			System.out.println ("UDPCamServer exception: "+e);
			System.exit (0);
		}		
	}
	
	public void run ()
	{
		DatagramPacket dp; 
		
		System.out.println ("UDPServerCam initialized on port "+sock.getLocalPort());
		String token;
		while (true)
		{
			dp = new DatagramPacket (new byte[256],256);
			
			try
			{
				sock.receive (dp);
			}
			catch (Exception e)
			{
				System.out.println ("UDPCamServer.run exception: "+e);
			}
			
			if (dp.getLength() > 0)
			{
				StringTokenizer st = new StringTokenizer (new String (dp.getData(),0,dp.getLength()),"|");
				
				if (st.countTokens () == 1)
				{
					String className;
					token = st.nextToken();
					if (token.equals("SEND_CAM_CLASS"))
					{
						try {
							className=driver.getClass().getName();
							if (DEBUG) System.out.println("Sending camera model: \""+className+"\""+" to "+dp.getAddress()+":"+dp.getPort());
							sock.send(new DatagramPacket(className.getBytes(),className.getBytes().length,dp.getAddress(),dp.getPort()));
						} catch (Exception e) {
							System.out.println("UDPCamServer: Error sending camera class name to client");
							e.printStackTrace();
						}
					}
				}
				else if (st.countTokens () == 3)
				{
					Comando c = new Comando (st.nextToken(),st.nextToken(),st.nextToken());
					
					driver.send (c);									
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
					
					driver.send (c,n);
				}
				else if (st.countTokens () == 5)
				{
					Comando c = new Comando (st.nextToken(),st.nextToken(),st.nextToken());
					int param1,param2;
					String stnum = st.nextToken();
					try { 
						param1 = Integer.parseInt (stnum); 
						stnum = st.nextToken();
						param2 = Integer.parseInt (stnum); 
					} 
					catch (Exception e)
					{
						
						System.out.println ("Excepcion: "+e);
						e.printStackTrace();
						System.out.println ("stnum es: \""+stnum+"\"");
						param1=0;
						param2=0;
					}
					driver.send(c,param1,param2);					
				}
				else if (st.countTokens () == 6)
				{
					Comando c = new Comando (st.nextToken(),st.nextToken(),st.nextToken());
					int param1,param2,param3;
					String stnum = st.nextToken();
					try { 
						param1 = Integer.parseInt (stnum); 
						stnum = st.nextToken();
						param2 = Integer.parseInt (stnum); 
						stnum = st.nextToken();
						param3 = Integer.parseInt (stnum); 
					} 
					catch (Exception e)
					{
						
						System.out.println ("Excepcion: "+e);
						e.printStackTrace();
						System.out.println ("stnum es: \""+stnum+"\"");
						param1=0;
						param2=0;
						param3=0;
					}
					driver.send(c,param1,param2,param3);
				}
				else 
					System.out.println ("UDPCamServer.run: Non expected bytes received");				
			} 
		}
	}

/*
					byte[] bytes = ((new Boolean (b)).toString()).getBytes();
					
		
					dp2 = new DatagramPacket (bytes, bytes.length,dp.getAddress(),dp.getPort());
					
					try { sock.send (dp2); } catch (Exception e) 
					{ 
						System.out.println ("Excepcion en envio para devolver");
					}			
*/
	
	public static void main (String args[])
	{
		Camera cam = null;
		int servport = 0;
		try
		{
			cam = Camera.getCamera (args[0]);
			servport = Integer.parseInt (args[1]);
		} catch (Exception e)
		{
			System.out.println ("Exception: "+e);
			e.printStackTrace();
			System.exit (0);
		}
		
		UDPCamServer serv = new UDPCamServer (cam,servport);
		serv.start ();		
	}
}