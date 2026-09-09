package devices.drivers.laser.LaserUDP;

import java.net.*;
import java.util.*;

import devices.drivers.laser.*;

public class LaserUDP extends Laser
{
	private InetAddress 	servadd;
	private int 			servport;
	private DatagramSocket	sock;
	private double[]		laserdata = null;
	
	Runnable UDPReceiveThread = new Runnable ()
		{
			public void run ()
			{
				byte udpbuff[];
				DatagramPacket dp;
				
				System.out.println ("LaserUDP: Receive Thread Started ("+servadd+":"+servport+")");
				//Se envia una trama para comunicarle al servidor que quiere recibir
				dp = new DatagramPacket (new byte[1],1,servadd,servport);
				try
				{
					sock.send (dp);
				} catch (Exception e)
				{}
				
				while (true)
				{
					udpbuff = new byte[4096];
					dp = new DatagramPacket (udpbuff,4096);
					try
					{
						sock.receive(dp);
					} catch (Exception e)
					{
						System.err.println ("LaserUDP: Exception reading socket: "+e);
						dp.setLength(0);
					}
					if (dp.getLength() != 0)
					{
						StringTokenizer st = new StringTokenizer (new String (udpbuff),"|");
						int cuantos = Integer.parseInt (st.nextToken());
						if (laserdata == null) laserdata = new double[cuantos];
						for (int i=0; i < cuantos; i++)
							laserdata[i] = Double.parseDouble (st.nextToken());	
					}
				}
			}
		};
	

	public LaserUDP ()
	{
		super();
	}

	// Instance methods
	public void initialise (String param) throws LaserException
	{
		StringTokenizer st = new StringTokenizer (param,":");
		
		String server = st.nextToken();
		String port = st.nextToken();
				
		try { servadd = InetAddress.getByName (server); }
		catch (Exception e) { throw (new LaserException ("Server "+server+" not valid"));}
		
		try { servport = Integer.parseInt (port); } 
		catch (Exception e) { throw (new LaserException ("Port "+port+" not valid"));}
		
		try { sock = new DatagramSocket (); }
		catch (Exception e) { throw (new LaserException ("can't open the socket!!->"+e.toString()));}
		
		(new Thread (UDPReceiveThread)).start ();
   	}
	
	public double[] getLaserData () throws LaserException
	{
		
		//StringTokenizer st = new StringTokenizer ();
		return (laserdata == null ? new double[0]:laserdata);
	}

}