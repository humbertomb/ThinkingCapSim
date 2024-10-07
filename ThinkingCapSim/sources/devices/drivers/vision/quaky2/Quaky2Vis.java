/*
 * (c) 2002 Humberto Martinez
 */
 
package devices.drivers.vision.quaky2;

import java.util.*;
import java.net.*;
import java.awt.*;

import devices.data.*;
import devices.drivers.vision.*;

public class Quaky2Vis extends Vision
{
	static public final byte				CAPTURE			= 100;
	
	protected DatagramSocket				socket;
	protected DatagramPacket 				idatagram;
	protected DatagramPacket 				odatagram;
	protected byte[]						ibuffer;
	protected byte[]						obuffer;
	protected InetAddress					rip;
	protected int							rport;

	// Constructors
	public Quaky2Vis ()
	{
		super ();
		
		setName ("Thread-Quaky2Vis");	
	}

	// Instance methods
	public void initialise (String props) throws VisionException
	{
		int					i;
		StringTokenizer		st;

		try
		{   	
			// Parse configuration parameters
			st		= new StringTokenizer (props,", :\t");
			maxobjs	= Integer.parseInt (st.nextToken ());
			port	= Integer.parseInt (st.nextToken ());
			rip		= InetAddress.getByName (st.nextToken ());
			rport	= Integer.parseInt (st.nextToken ());

			// Create and initialise object structures
			odata			= new VisionData[maxobjs];
			for (i = 0; i < maxobjs; i++)
				odata[i]		= new VisionData ();
				
			// Communication stuff initialisation
			ibuffer			= new byte[1500];
			idatagram		= new DatagramPacket (ibuffer, ibuffer.length);

			obuffer			= new byte[1];
			odatagram		= new DatagramPacket (obuffer, 0, obuffer.length);
		
		} catch (Exception e) { throw new VisionException ("(initialise) "+e.toString ()); }
	}

	public void acquire_frame ()
	{
		try 
		{
			obuffer[0]	= CAPTURE;
			
			odatagram.setAddress (rip);
			odatagram.setPort (rport);

			socket.send (odatagram);
		} catch (Exception e) { e.printStackTrace (); }
	}
	
	protected void process_frame ()
	{
		int					i, k;
		int					type;
		int 				objs;     
		double				pos3_x, pos3_y, pos3_z;
		int					pos2_x, pos2_y;
		int					width, height;
		String				rcvd;
		StringTokenizer		st;
            
        // Wait until consumer cleans the lock variable
        while (isUpdated ())
        	try { Thread.sleep (10); } catch (Exception e) { }
        
        // Clean up previous perceptions
        for (i = 0; i < maxobjs; i++)
        	odata[i].valid = false;
        	
        // Receive UDP data
        rcvd	= " ";
        
        try
        {
			socket.receive (idatagram);
			rcvd	= new String (idatagram.getData (), 0, idatagram.getLength ());
		}
		catch (Exception e) { e.printStackTrace (); return; }

		try
		{
			st		= new StringTokenizer (rcvd,"<, :");

			objs	= Integer.parseInt (st.nextToken ());
			Integer.parseInt (st.nextToken ());		  				// TODO: The nTipos parameter should be removed from protocol

			for (k = 0; (k < objs) && (k < maxobjs); k++)
			{
				type	= Integer.parseInt (st.nextToken ());

				pos3_x	= (double) Float.parseFloat (st.nextToken ());		    
				pos3_y	= (double) Float.parseFloat (st.nextToken ());
				pos3_z	= (double) Float.parseFloat (st.nextToken ());
		    
				pos2_x	= Integer.parseInt (st.nextToken ());
				pos2_y	= Integer.parseInt (st.nextToken ());
		    
				width	= Integer.parseInt (st.nextToken ());
				height	= Integer.parseInt (st.nextToken ());

				switch (type)
				{
				case 0:
					odata[k].set_blob ("Ball", pos2_x, pos2_y, width, height, Color.YELLOW);
					odata[k].percept_pos (-pos3_x / 1000.0, pos3_y / 1000.0, pos3_z / 1000.0);
					break;
					
				case 1:
					odata[k].set_blob ("Net1", pos2_x, pos2_y, width, height, Color.BLUE);
					odata[k].percept_pos (-pos3_x / 1000.0, pos3_y / 1000.0, pos3_z / 1000.0);
					break;
					
				case 2:
					odata[k].set_blob ("Net2", pos2_x, pos2_y, width, height, Color.BLUE);
					odata[k].percept_pos (-pos3_x / 1000.0, pos3_y / 1000.0, pos3_z / 1000.0);
					break;					
					
				default:
				}
        		odata[k].valid = true;
			}
			
//			if (debug) System.out.println ("  [Quaky2Vis] Data received [" + rcvd + "]");
			if (debug) System.out.println ("  [Quaky2Vis] Data received [Temporarily N/A]");

			// Set update flag
			setUpdated (true);
		}
		catch (Exception e) 
		{ 
			// Clear update flag
			setUpdated (false);
			
			System.out.println ("--[Quaky2Vis] ERROR converting camera data");
		}
	}		

	public void run () 
	{
    	long		ct, tk;
    	    	 
		if (debug)		System.out.println ("  [Quaky2Vis] Starting vision processing thread");		

		try { socket = new DatagramSocket (port); } catch (Exception e) { e.printStackTrace (); }
		
    	while (true)
    	{
			tk		= System.currentTimeMillis ();
					
			process_frame ();
				
			// Show processing information
			ct		= System.currentTimeMillis () - tk;
			if (debug)		System.out.println ("  [Quaky2Vis] CPU time=" + ct + "ms");		
			
			Thread.yield ();
		}
	}	
}
