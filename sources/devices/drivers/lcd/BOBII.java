package devices.drivers.lcd;

import javax.comm.*;
import java.io.InputStream;
import java.io.OutputStream;

public class BOBII
{
  // Special chars
  public static final String RIGHTARR = "z";
  public static final String LEFTARR = "{";
  public static final String UPARR = "|";
  public static final String DOWNARR = "}";

	//Commands
  private static String CLR = "{A";
  private static String EBL = "{BE";
  private static String DBL = "{BD";
  private static String PXY = "{C";
  private static String COL = "{E";
  private static String SCOL = "{F";
  private static String BLE = "{GE";
  private static String BLD = "{GD";
  private static String LOM = "{MF";
  private static String OVM = "{MM";
  private static String NTR = "{T";

  private int maxlines	= 10;
	private int	maxchars	= 27;

	private static int timeout = 3000;

  private boolean localmode;

  private SerialPort serie;
  private InputStream is;
 	private OutputStream os;

  private String port;

  public BOBII (String puerto) throws Exception
  {
  	init(puerto);
    localmode = false;
  }

  	private void init (String puerto) throws Exception
  	{
  		CommPortIdentifier id;

       	id = CommPortIdentifier.getPortIdentifier (puerto);
        serie = (SerialPort) id.open ("BOB II",10000);
        serie.setSerialPortParams(9600,8,1,SerialPort.PARITY_NONE);
        serie.setFlowControlMode (SerialPort.FLOWCONTROL_NONE);
        serie.enableReceiveTimeout(timeout);

       	is = serie.getInputStream();
        os = serie.getOutputStream();

        //System.out.println ("In Buffer size: "+serie.getInputBufferSize()+" Out Buffer size:  "+serie.getOutputBufferSize());
        //serie.setInputBufferSize(16);
        //serie.setOutputBufferSize (128);

        System.out.println ("Opened port "+puerto+" for BOB II tittle generator.");
        port = puerto;
  	}

  	public void close () throws Exception
  	{
  		is.close ();
  		os.close ();
  		serie.close ();
  	}

  	public String toString ()
  	{
  		return ("BOB II Video Title Generator in port "+port);
  	}

  	synchronized public void writeText (String text) throws Exception
  	{
  		os.write (text.getBytes());
  	}

    synchronized public void setTextPos (int col, int row) throws Exception
  	{
      String command;

  		if ((col > maxchars) || (row > maxlines))
  		{
  			System.out.println ("incorrect values for col or row: col="+col+"("+maxchars+") row="+row+" ("+maxlines+")");
  			return;
  		}

      command = PXY;
      if (col < 10)
        command = command+"0";
      command = command+Integer.toString(col);

      if (row < 10)
        command = command+"0";
      command = command+Integer.toString(row);

      os.write (command.getBytes());
  	}

  	synchronized public void writeTextAt (String text, int col, int row) throws Exception
  	{
      String command;

  		if ((col > maxchars) || (row > maxlines))
  		{
  			System.out.println ("incorrect values for col or row: col="+col+"("+maxchars+") row="+row+" ("+maxlines+")");
  			return;
  		}

      command = PXY;
      if (col < 10)
        command = command+"0";  
      command = command+Integer.toString(col);

      if (row < 10)
        command = command+"0";
      command = command+Integer.toString(row);

      command = command + text;
      os.write (command.getBytes());
  	}

  	synchronized public void clrlcd () throws Exception
  	{
  		os.write (CLR.getBytes ());
      os.flush ();
      Thread.sleep (5);
  	}

    synchronized public void blinking_text (String text) throws Exception
    {
      String command;

      command = BLE+text+BLD;
      os.write (command.getBytes());
    }

    synchronized public void send_special (String specials) throws Exception
    {
      String command;

      command = NTR+specials;

      os.write (command.getBytes());
      os.write ((byte)27);     
    }
    
    synchronized public void enable_blink () throws Exception
    {
      os.write (BLE.getBytes ());
    }

    synchronized public void disable_blink () throws Exception
    {
      os.write (BLD.getBytes ());
    }

     synchronized public void localmode (boolean local) throws Exception
     {
      if (local)
      {
        os.write (LOM.getBytes ());
        os.flush ();
        Thread.sleep (10);
      }
      else
      {
        os.write (OVM.getBytes ());
        os.flush ();
        Thread.sleep (10);
      }
     }

  	synchronized public void setColor (int color) throws Exception
  	{
      String command;

      if (!localmode)
      {
  			System.out.println ("BOB II must be in local mode to use setColor.");
  			return;
  		}
      if (color > 7)
      {
  			System.out.println ("incorrect values for color: "+color+ " (0-7)");
  			return;
  		}

      command = SCOL+Integer.toString(color);
      os.write (command.getBytes());
  	} 
 }