/*
 * (c) 2002 Humberto Martinez
 */
 
package tcrob.umu.quaky2;

import java.util.*;

import java.net.*;
import java.awt.*;

public class Quaky2Vis extends Thread
{
	public class VisionException extends Exception
	{
		public VisionException ()
		{
			super ();
		}

		public VisionException (String message)
		{
			super (message);
		}
	}
	
	static public final byte				CAPTURE			= 100;
	
	protected DatagramSocket				socket;
	protected DatagramPacket 				idatagram;
	protected DatagramPacket 				odatagram;
	protected byte[]						ibuffer;
	protected byte[]						obuffer;
	protected InetAddress					rip;
	protected int							rport;

	// Processed objects information
	protected Quaky2VisData[]				odata;
	protected int							maxobjs;
	
	// Communication parameters and instance class
	protected String						clase;
	protected int							port;
	
	// Flow control and locking
	protected boolean						updated		= false;
	protected boolean						debug		= false;

	// Constructors
	public Quaky2Vis ()
	{
		super ();
		
		setName ("Thread-Quaky2Vis");	
	}
	
	// Accessors
	public final synchronized void			setUpdated (boolean updated)	{ this.updated = updated; }	
	public final synchronized boolean		isUpdated ()					{ return updated; }	
	public final Quaky2VisData[]				getData ()						{ return odata; }	
	public final void						setDebug (boolean debug)		{ this.debug = debug; }
	public final boolean					getDebug ()						{ return debug; }

	// Class methods
	public static Quaky2Vis getVision (String param) throws VisionException
	{
		Quaky2Vis		vis;
		
		vis = new Quaky2Vis ();
		vis.initialise (param);
		
		return vis;	
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
			odata			= new Quaky2VisData[maxobjs];
			for (i = 0; i < maxobjs; i++)
				odata[i]		= new Quaky2VisData ();
				
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

	/* -------------------------------------------------------------------------------------
	 *  WARNING:
	 * 
	 * TODO: este codigo se mantine por razones historicas. Ya no hay robot para ejecutarlo.
	 * Pero si se quisiera terminar la integracion, este fragmento tendria que usarse para 
	 * meter en el LPS los objetos detectados por el modulo de vision
	 * 
	   ------------------------------------------------------------------------------------- */
	
	/*
	public void set_lpo (Quaky2VisData data)
	{
		int			i;
		double		x, y;
		double		ll, aa;
		Position	pos;

		pos		= new Position ();
		for (i = 0; i < lpos_n; i++)
			if (data.id.equals (lpos[i].label ()))
			{
				// Compute sensor absolute position (where the objects were captured)
				pos.set (cur.x (), cur.y (), cur.alpha);
				pos.untranslate (data.cpos);
				
				// Compute object absolute positions (where captured)
				x	= pos.x () + data.rho * Math.cos (pos.alpha + data.phi);
				y	= pos.y () + data.rho * Math.sin (pos.alpha + data.phi);

				// Compute object relative positions (current robot frame)
				x	= x - cur.x ();
				y	= y - cur.y ();
				ll	= Math.sqrt (x * x + y * y);
				aa	= Math.atan2 (y, x);
				x	= ll * Math.cos (aa - cur.alpha);
				y	= ll * Math.sin (aa - cur.alpha);
				
				// Update LPS data
				lpos[i].locate (x, y, 0.0);
				lpos[i].color (ColorTool.fromColorToWColor(data.color));
				//lpos[i].color (data.color);
				
				lpos[i].active (true);
				lpos[i].anchor (1.0);
				lpos[i].ageing (0);
			}
	}
	 */
	
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
