package devices.drivers.gps.Garmin;
import java.io.*;

/**
* This class provides the functionality of automatically removing the double DLEs from the GPS-inputstream.
* The double-DLEs can be found in the size-,data-, and checksum-fields. 
* The only method providing the filtering-functionality is read().
*/
public class GarminInputStream extends FilterInputStream 
{
	/*
	* Last value read.
	*/
	private int prev;
	
	/**
	* Takes the stream to the GPS-unit as an argument.
	*/
	public GarminInputStream(InputStream i) 
	{
		super(i);
		in = i;
		prev = 0;
	}		
			
	/**
	* Reads a packet from the stream. <br/>
	* <b> Note: </b> Method assumes that it's called between packets, ie. when the first byte of a packet is the
	* next in the stream. If this condition is met, the method will leave the stream in the same state.
	*/	
	public int[] readPacket() throws GarminException 
	{
		int c;
		int[] packet;
		int id, size;
		
		try
		{	
			// Wait until a valid header is received (blocking IO)
			c = read();					
			while (c != Garmin.DLE)
				c = read();					

			// Continue processing the received packet	
			id = read();
			size = read();
			packet = new int[size + 6];
			packet[0] = Garmin.DLE;
			packet[1] = id;
			packet[2] = size;
			for (int i = 0 ; i < size + 3 ; i++) 
				packet[3 + i] = read();
				
			if (packet[packet.length - 2] != Garmin.DLE)
				throw new GarminException ("[read] tail DLE not received");
			
			if (packet[packet.length - 1] != Garmin.ETX)
				throw new GarminException ("[read] tail ETX not received");
		}
		catch (IOException e)
		{
			throw new GarminException ("[read] IO error: " + e.toString ());
		}
		
		return packet;
	}
	
	/**
	* Returns the next byte from the stream. Makes sure to remove DLE stuffing.	
	*/	
	public int read() throws IOException
	{
		int c = in.read();
		if (prev == Garmin.DLE && c == Garmin.DLE)
			return prev = in.read();
		else 
			return prev = c;
	}
}