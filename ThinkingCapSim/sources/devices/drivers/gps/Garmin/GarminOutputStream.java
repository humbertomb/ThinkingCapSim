package devices.drivers.gps.Garmin;

import java.io.*;

/**
* This class take care of adding DLE-stuffing to all packets sent to the GPS.
* <b> NOTE: </b> Only the method write(GarminPacket) performs addition of DLE-stuffing. The remaining 
* methods write directly to the GPS without format-control.
*/

public class GarminOutputStream extends FilterOutputStream 
{	
	public GarminOutputStream(OutputStream o) 
	{
		super(o);
	}
	    
	public synchronized void write (int[] packet) throws IOException 
	{
		int			i, c;
		
		// Write header
		write (packet[0]);
		write (packet[1]);
		
		// Iterate through size-field, data-field and checksum-field. Add stuffing where necessary.
		for (i = 2 ; i < packet[2] + 4 ; i++) 
		{
			c = packet[i];			
			write(c);
			if (c == Garmin.DLE)
				write (c);			
		}
		
		// Write tail
		for ( ; i < packet.length; i++)
			write (packet[i]);
		
		flush();
	}	
}