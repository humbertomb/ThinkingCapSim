package devices.drivers.laser.LaserUDP;

import java.net.*;

import devices.drivers.laser.*;

public class LaserUDPServer extends Thread
{
	private Laser			driver;
	private DatagramSocket	sock;
	private InetAddress 	Raddress;
	private int				Rport;
	private int 			Lport;
	
	public LaserUDPServer (String laserparams,String porttolisten)
	{
		super ("LaserUDPServer");
		
		try 
		{ 
		
			driver = Laser.getLaser (laserparams);
			Lport = Integer.parseInt (porttolisten); 
			
			sock = new DatagramSocket (Lport);
		} catch (Exception e)
		{
			System.out.println ("UDPCamServer exception: "+e);
			System.exit (0);
		}
	}
	
	
	public void run ()
	{
		DatagramPacket dp;
		double[] laserdata;
		int i;
		String tosend;
		boolean initialized = false;
		
		System.out.println ("LaserServerCam initialized on port "+sock.getLocalPort());	
		
		while (true)
		{
			if (!initialized)
			{
				System.out.println ("Waiting for listener...");
				try
				{
					dp = new DatagramPacket (new byte[32],32);
					sock.receive (dp);	
					Raddress = dp.getAddress();
					Rport = dp.getPort();
					System.out.println ("Listener on "+Raddress+":"+Rport);
					initialized = true;			
				} catch (Exception e)
				{}
			}
			else
			{
				try
				{
					laserdata = driver.getLaserData();
					tosend = new String (Integer.toString(laserdata.length));
					for (i=0; i < laserdata.length; i++)
						tosend += "|"+Double.toString(laserdata[i]);
					dp = new DatagramPacket (tosend.getBytes(), tosend.length(), Raddress, Rport);
					sock.send (dp);	
				} 
				catch (java.io.IOException e)
				{
					System.out.println ("Error on listener...");
					initialized = false;
				}		
				catch (Exception e)
				{
					System.out.println ("Exception during run: "+e);
					System.exit (0);
				}
			}
			yield();			
		}		
	}
	
	public static void main (String args[])
	{
		LaserUDPServer lserver = new LaserUDPServer (args[0], args[1]);
		lserver.start();
	}

}