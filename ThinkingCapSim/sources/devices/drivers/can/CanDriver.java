/*
 * (c) 2002 Humberto Martinez
 */

package devices.drivers.can;

import java.util.*;


public abstract class CanDriver extends Object
{
	// CAN driver structures
  	protected Thread			thread;
  	private volatile boolean	running			= false;
  	protected CanDevice			can0;
	protected CanFrame			frame 			= new CanFrame ();

	// CAN configuration parameters
	protected int 				canBrate;
	protected String			canDev;
	private int					ocr, cdr;
	private boolean				singleFilter;

    // Debug information
  	protected boolean 			messageDebug;
  	protected boolean 			canDebug;

	protected Runnable receiveThread = new Runnable ()
	{
		int				i, j, k;
		int				id;
		byte[]			rcvd;
		boolean			valid;

		public void run ()
		{
			CanFrame[]		frames = null;

			Thread.currentThread ().setName ("Thread-CanDrv");
			System.out.println ("  [CanDrv] PC104-CAN driver (listener thread) ready");

			running		= true;
			while (running)
			{
				frames = can0.receiveFrames ();
				if (frames != null)
				{
					parseFrames (frames);

					if (canDebug)
					{
						System.out.print ("  [CanDrv] CAN frame received: "+"canDebug"+canDebug);
						frames[i].dumpFrame ();
					}
				}

				Thread.yield ();
           	}
			System.out.println("CAN closed");
		}
	};

	// Constructors
	public CanDriver (Properties prop)
	{
		// Open CAN driver and start reception thread
		try
		{
			parseProperties (prop);
			can0 = new CanDevice (canDev);
			can0.canConfig (canBrate, ocr, cdr);
			initialise ();
			System.out.println ("  [CanDrv] PC104-CAN driver ready");

			thread = new Thread (this.receiveThread);
			thread.start ();
		} catch (Exception e) { System.err.println("--[CanDrv] Exception in creation: "+e); }
	}

	// Class methods
	static public int bytesToInt (byte val)
	{
		int			out;

		out		= (int) (val & 0xFF);
		if (val < 0)
		{
			out--;
			out = out ^ 0xFFFF;
			out = -out;
		}

	  	return out;
	}

	static public int bytesToInt (byte hi, byte lo)
	{
		int			out;

		out		= (int) ((int) ((hi << 8) & 0xFF00) | (int) (lo & 0xFF));
		if (hi < 0)
		{
			out--;
			out = out ^ 0xFFFF;
			out = -out;
		}

	  	return out;
	}

	static public int bytesToInt (byte hi, byte medh, byte medl, byte lo)
	{
		int			out;

		out		= (int) ((int) ((hi << 24) & 0xFF000000) | (int) ((medh << 16) & 0xFF0000) | (int) ((medl << 8) & 0xFF00) | (int) (lo & 0xFF));
		if (hi < 0)
		{
			out--;
			out = out ^ 0xFFFFFFFF;
			out = -out;
		}

	  	return out;
	}

	static public int bytesToUInt (byte val)
	{
		return (int) (val & 0xFF);
	}

	static public int bytesToUInt (byte hi, byte lo)
	{
		return (int) ((int) ((hi << 8) & 0xFF00) | (int) (lo & 0xFF));
	}

	static public byte intToByte (int val)
	{
	  	return 	(byte) (val % 0x100);
	}

	static public byte intToByteHi (int val)
	{
		byte		out;
		int			uval;

		if (val < 0)
		{
			uval	= 0xffff - Math.abs (val) + 1;
			out		= (byte) (uval / 0x100);
		}
		else
			out		= (byte) (val / 0x100);

		return out;
	}

	static public byte intToByteLo (int val)
	{
		byte		out;
		int			uval;

		if (val < 0)
		{
			uval	= 0xffff - Math.abs (val) + 1;
			out		= (byte) (uval % 0x100);
		}
		else
			out		= (byte) (val % 0x100);

		return out;
	}

	// Instance methods
	public boolean reset ()
	{
		return can0.canReset ();
	}

	public void stop ()
	{
		if (thread != null)
		{
			running		= false;
			try { thread.join (); } catch (Exception e) {e.printStackTrace(); }
			thread		= null;
		}

		can0.close ();
	}

	protected void parseProperties (Properties p)
	{
		try
		{
			canDev			= p.getProperty ("CAN_DEV");
			canBrate		= Integer.parseInt (p.getProperty ("CAN_BRATE"));

			messageDebug	= new Boolean(p.getProperty ("DEBUG")).booleanValue();
			canDebug		= new Boolean(p.getProperty ("DEBUG_CAN")).booleanValue();

			ocr				= Integer.parseInt (p.getProperty ("CAN_OCR"));
			cdr				= Integer.parseInt (p.getProperty ("CAN_CDR"));
			singleFilter	= new Boolean(p.getProperty ("CAN_SINGLE_MODE")).booleanValue();
		} catch (Exception e) { e.printStackTrace (); }

		System.out.println ("  [CanDrv] Using "+canDev+" at "+canBrate+" kbps");

		parseIdentifiers (p);
	}

	protected abstract void initialise ();
	protected abstract void parseIdentifiers (Properties p);
	protected abstract void parseFrames (CanFrame[] frames);
}