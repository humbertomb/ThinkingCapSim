/*
 * (c) 2001 Humberto Martinez, Jose Ma Garcia Nieto
 * (c) 2002-2004 Humberto Martinez
 */

package tc.shared.linda.net;

import java.io.*;
import java.net.*;
import java.util.zip.*;

import tc.shared.linda.*;

public class LindaNetPacket implements Serializable
{
	public static final int 				LEN_DATA			= 1024 * 8;	// Maximum packet length

	// Linda protocol: data commands
	public static final int 				CMD_NULL			= 0;
	public static final int 				CMD_WRITE		= 1; 		// Write a tuple into the space
	public static final int 				CMD_READ			= 2; 		// Read a tuple from the space
	public static final int 				CMD_TAKE			= 3; 		// Read and remove a tuple from the space

	// Linda protocol: control commands
	public static final int 				CMD_REGISTER		= 4; 		// Start receiving tuple events																	
	public static final int				CMD_UNREGISTER 	= 5;			// Stop receiving tuple events
	public static final int				CMD_DODEBUG 		= 6;			// Start debugging actions
	public static final int				CMD_NODEBUG 		= 7;			// Stop debugging actions
	public static final int				CMD_DUMP	 		= 8;			// Dump current space state
	public static final int				CMD_ANSWER	 	= 9;			// Answer from a READ/TAKE command
	public static final int				CMD_EVENT	 	= 10;		// Event from a REGISTER command
	
	// Action internal variables
	protected int						command;
	protected Tuple						tuple;
	protected LindaNetConn				connection;
	
	private boolean						debug			= false;
	
	// Constructors
	public LindaNetPacket (int mode)
	{
		this (mode, false);
	}

	public LindaNetPacket (int mode, boolean debug)
	{
		this.command		= CMD_NULL;
		this.tuple		= null;
		this.connection	= new LindaNetConn (mode);
		this.debug		= debug;
	}
	
	// Class methods
	static public String cmdToString (int cmd)
	{
		switch (cmd)
		{
		case CMD_NULL:			return "NULL";
		case CMD_WRITE:			return "WRITE";
		case CMD_READ:			return "READ";
		case CMD_TAKE:			return "TAKE";
		case CMD_REGISTER:		return "REGISTER";
		case CMD_UNREGISTER:		return "UNREGISTER";
		case CMD_DODEBUG:		return "DO_DEBUG";
		case CMD_NODEBUG:		return "NO_DEBUG";
		case CMD_DUMP:			return "DUMP";
		case CMD_ANSWER:			return "ANSWER";
		case CMD_EVENT:			return "EVENT";
		}
		
		return "ERROR["+cmd+"]";
	}
	
	// Accessors
	public final int				getCommand ()		{ return command; }
	public final Tuple			getTuple ()			{ return tuple; }
	public final LindaNetConn	getConnection ()		{ return connection; }
	
	// Instance methods
	public void set (int command, Tuple tuple)
	{
		this.command		= command;
		this.tuple		= tuple;
	}
	
	public void set (InetAddress peer, int port)
	{
		connection.set (peer, port);	
	}
	
	public void decode (byte[] data,int offset, int dlen) 
	{
		ByteArrayInputStream		in;
		GZIPInputStream			gzip;
		ObjectInputStream		objs;
		Object					obj = null;
		String					space;
		String					key;

		command	= CMD_NULL;
		space	= new String ();
		key		= new String ();
		
		try 
		{
			in		= new ByteArrayInputStream (data, offset, dlen);
			gzip		= new GZIPInputStream (in);
			objs		= new ObjectInputStream (gzip);

			if (debug)	System.out.println ("decode.step A => rcvd.len=" + dlen + ", byt.len=" + in.available ()+ ", objs.len=" + objs.available ());

			command	= objs.readInt ();
			space	= (String) objs.readObject ();	
			key		= (String) objs.readObject ();	
			obj		= objs.readObject ();	

			objs.close ();
			gzip.close ();
			in.close ();		

			if (debug)	System.out.println ("decode.step B => obj=[" + obj + "], byt.len=" + in.available ()+ ", objs.len=" + objs.available ());
		} 
		catch (Exception e) { System.out.println("### ERROR decode: "+e.getMessage()+" key = "+key+" space = "+space+" command = "+command); e.printStackTrace();}

		tuple	= new Tuple (space, key, (Item) obj);	
	}

	public byte[] encode () 
	{
		ByteArrayOutputStream 		out;
		GZIPOutputStream				gzip;
		ObjectOutputStream    		objs;
		byte[]						bytes	= null;
		Object						obj;

		if (debug)	System.out.println ("encode.step A => comm=" + command + ", " + tuple);
		try 
		{
			out		= new ByteArrayOutputStream ();
			gzip		= new GZIPOutputStream (out);
			objs		= new ObjectOutputStream (gzip);
			
			obj		= tuple.value;
			if (obj == null)
				obj		= new Item ();		

			objs.writeInt (command);
			objs.writeObject (tuple.space);
			objs.writeObject (tuple.key);
			objs.writeObject (obj);
			
			objs.flush ();
			gzip.finish ();

			objs.close ();
			gzip.close ();
			out.close ();

			bytes = out.toByteArray ();

			if (debug)	System.out.println ("encode.step B => comm=" + command + ", obj=[" + obj + "], len=" +  bytes.length);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("### ERROR encode: "+e.getMessage()+" ["+tuple.key+"]"); 
			System.out.println("comm=" + command + " tuple = " + tuple);
		}
    	
		return bytes;
	}
}