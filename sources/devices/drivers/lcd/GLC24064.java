package devices.drivers.lcd;

import javax.comm.*;
import java.io.InputStream;
import java.io.OutputStream;

public class GLC24064 
{
	//Fonts types
	public static int	SMALL	= 0;
	public static int	FUTURA	= 1;
	public static int	GEORGIA = 2;
	
	//Graphics Bars types
	public static int 	VERTBOTTOM	= 0;
	public static int 	VERTTOP		= 2;
	public static int 	HORLEFT		= 1;
	public static int 	HORRIGHT	= 3;
	
	//Fonts stuff
	private static int 	SPACE[] 	= {0,1,1};
	private static int	LNSPCE[] 	= {1,0,1};
	private static int 	LEFTM[] 	= {0,1,5};
	private static int	TOPM[]		= {1,2,1};
	private static int	SCRROW[]	= {56,56,60};
	
	private static int 	MAXLINES[]	= {8,4,4};
	private static int  MAXCHARS[]	= {40,27,34};
		
	private int	currfont	= 0;
	private int currline	= 0;
	private int	currchar	= 0;
	private int maxlines	= 0;
	private int	maxchars	= 0;

	//LCD data
	private static final int LCD_MODELID	= 0x13;
	
	private int serial;
	private int version;
	
	private int barindex;

	//Commands codes
	private static byte	SETFNT = 0x31;
	private static byte SETMET = 0x32;
	private static byte REASER = 0x35;
	private static byte REAVER = 0x36;
	private static byte REAMOD = 0x37;
	private static byte WRP_ON = 0x43;
	private static byte WRPOFF = 0x44;
	private static byte SETTXP = 0x47;
	private static byte SETTOP = 0x48;
	private static byte	SCR_ON = 0x51;
	private static byte	SCROFF = 0x52;
	private static byte	CLRLCD = 0x58;
	private static byte BCK_ON = 0x42;
	private static byte BCKOFF = 0x46;
	private static byte SETCNT = 0x50;
	private static int 	SAVCNT = 0x91;
	private static byte DRWBMP = 0x64;
	private static byte	INISCH = 0x6A;
	private static byte SHFSCH = 0x6B;
	private static byte SETCOL = 0x63;
	private static byte DRWLIN = 0x6C;
	private static byte CNTLIN = 0x65;
	private static byte INIBAR = 0x67;
	private static byte SETBAR = 0x69;
	private static byte PUTPIX = 0x70;


	private static int timeout = 3000;

  	private SerialPort serie;
  	private InputStream is;
 	private OutputStream os;
  
  	public GLC24064 (String puerto) throws Exception
  	{
  		init(puerto);
  	} 
  	
  	private void init (String puerto) throws Exception
  	{
  		CommPortIdentifier id;

       	id = CommPortIdentifier.getPortIdentifier (puerto);
        serie = (SerialPort) id.open ("GLC24064 LCD",10000);
        serie.setSerialPortParams(115200,8,1,SerialPort.PARITY_NONE);
        serie.setFlowControlMode (SerialPort.FLOWCONTROL_NONE);
        serie.enableReceiveTimeout(timeout);
        
       	is = serie.getInputStream();
        os = serie.getOutputStream();
        
        //System.out.println ("In Buffer size: "+serie.getInputBufferSize()+" Out Buffer size:  "+serie.getOutputBufferSize());
        serie.setInputBufferSize(16);
        serie.setOutputBufferSize (128);
        
        if (readInfo () != LCD_MODELID)
        	throw new Exception ("Unknown LCD model: "+readInfo());
  		
  		barindex = 0;  	
  	}
  	
  	public void close () throws Exception
  	{
  		is.close ();
  		os.close ();
  		serie.close ();
  	}
  	
  	private void sendCommand (byte command) throws Exception
  	{
  		byte bytes[] = new byte[2];
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = command;
  		
   		os.write(bytes);
   		os.flush ();	
  		Thread.sleep (10);
  	}
  	
  	//Returns the model
  	synchronized private int readInfo () throws Exception 
  	{
  		int[] returned = new int[8];
  		int i,count;
  		
  		count = 0;
  		sendCommand (REASER);	
  		while (is.available () > 0)
  			returned[count++] = is.read();	
  		serial = 0;
  		for (i=0; i < count; i++)
  			serial = serial + returned[i]*(int)(Math.pow (2*1.0,8*i*1.0));		
  		
  		count = 0;
  		sendCommand (REAVER);
		while (is.available () > 0)
  			returned[count++] = is.read();	
  		version = 0;
  		for (i=0; i < count; i++)
  			version  = version + returned[i]*(int)(Math.pow (2*1.0,8*i*1.0));
   		
  		count = 0;
  		sendCommand (REAMOD);
 		while (is.available () > 0)
			returned[count++] = (byte)is.read();	
		
		return (returned[0]);
  	}
  	
  	public String toString ()
  	{
  		return ("Matrix Orbital GLC24064 LCD model. Serial: "+serial+". Version: "+version+"."); 	
  	}
  	
  	synchronized public void writeText (String text) throws Exception
  	{
  		os.write (text.getBytes());
   		currchar += text.length(); 
  	}
  	
  	
  	synchronized public void writeTextAt (String text, int col, int row) throws Exception
  	{
  		byte bytes[] = new byte[4];
  	
  		if ((col > maxchars) || (row > maxlines)) 
  		{
  			System.out.println ("incorrect values for col or row: col="+col+"("+maxchars+") row="+row+" ("+maxlines+")");
  			return;
  		}
  		
  		currline = row;
  		currchar = col;
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = SETTXP;
  		bytes[2] = (byte)col;
  		bytes[3] = (byte)row;
  		
  		os.write (bytes);
		  	
  		os.write (text.getBytes());
   		
   		os.flush ();
   		currchar += text.length(); 
   		
  	}
  	
  	synchronized public void clrlcd () throws Exception
  	{
  		sendCommand (CLRLCD);
  	}
  	
  	synchronized public void backlight (boolean on) throws Exception
  	{
  		if (on) sendCommand (BCK_ON);
  		else sendCommand (BCKOFF);
  	}
  	
  	synchronized public void scroll (boolean _scroll) throws Exception
  	{
  		if (_scroll) 
  		{
  			sendCommand (WRP_ON);
  			sendCommand (SCR_ON);
  		}
  		else sendCommand (SCROFF);
  	}
  	
  	synchronized public void wrap (boolean _wrap) throws Exception
  	{
  		if (_wrap) sendCommand (WRP_ON);
  		else sendCommand (WRPOFF);
  	}
  	
  	synchronized public void setContrast (byte value) throws Exception
  	{
  		byte bytes[] = new byte[3];
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = SETCNT;
  		bytes[2] = value;
  		
  		os.write (bytes);
  		os.flush ();	
  		Thread.sleep (5);
  	} 
  	
  	synchronized public void saveContrast (int value) throws Exception
  	{
  		byte bytes[] = new byte[3];
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = (byte)GLC24064.SAVCNT;
  		bytes[2] = (byte)value;
  		
  		os.write (bytes);
  		os.flush ();	
  		Thread.sleep (5);
  	}
  	
  	synchronized public void setColor (byte color) throws Exception
  	{
  		byte bytes[] = new byte[3];
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = SETCOL;
  		bytes[2] = color;
  		
  		os.write (bytes); 
  		os.flush ();	
  		Thread.sleep (5);	
  	}
  	
  	synchronized public void drawLine (int x1, int y1, int x2, int y2) throws Exception
  	{
  		byte bytes[] = new byte[6];
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = DRWLIN;
  		bytes[2] = (byte)(x1 & 0x00FF);
  		bytes[3] = (byte)(y1 & 0x00FF);
  		bytes[4] = (byte)(x2 & 0x00FF);
  		bytes[5] = (byte)(y2 & 0x00FF);
  		
  		os.write (bytes);
  		//os.flush ();
  		
	} 	
  	
  	synchronized public void addToLine (int x, int y) throws Exception
  	{	
  		byte bytes[] = new byte[4];
  	
  		bytes[0] = (byte)0xFE;
  		bytes[1] = CNTLIN;
  		bytes[2] = (byte)(x & 0x00FF);
  		bytes[3] = (byte)(y & 0x00FF);
  		
  		os.write (bytes);
  	}
  	
  	synchronized public void putPixel (int x, int y) throws Exception
  	{
  		
  		byte bytes[] = new byte[4];
  	
  		bytes[0] = (byte)0xFE;
  		bytes[1] = PUTPIX;
  		bytes[2] = (byte)(x & 0x00FF);
  		bytes[3] = (byte)(y & 0x00FF);
  		
  		os.write (bytes);
  		os.flush ();
  	}
  	
  	synchronized public void drawBitmap (int x1, int y1, int x2, int y2, byte data[]) throws Exception
  	{
  		byte bytes[] = new byte[6+data.length];
  		int i;
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = DRWBMP;
  		bytes[2] = (byte)(x1 & 0x00FF);
  		bytes[3] = (byte)(y1 & 0x00FF);
  		bytes[4] = (byte)(x2 & 0x00FF);
  		bytes[5] = (byte)(y2 & 0x00FF);
  		
  		for (i = 0; i < data.length; i++)
  			bytes[6+i] = data[i];
  			
  		os.write (bytes);  
  		os.flush ();
  		Thread.sleep (10);		
  	}
  	
  	synchronized public void setTextPos (int col, int row) throws Exception
  	{
  		byte bytes[] = new byte[4];
  	
  		if ((col > maxchars) || (row > maxlines)) 
  		{
  			System.out.println ("incorrect values for col or row: col="+col+"("+maxchars+") row="+row+" ("+maxlines+")");
  			return;
  		}
  		
  		currline = row;
  		currchar = col;
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = SETTXP;
  		bytes[2] = (byte)col;
  		bytes[3] = (byte)row;
  		
  		os.write (bytes);
  		os.flush ();	
  		Thread.sleep (5);
  	}
  	
  	synchronized public void setFont (int font) throws Exception
  	{
  		byte bytes[] = new byte[3];
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = SETFNT;
  		bytes[2] = (byte)(font+1);
  		
  		os.write (bytes); 
  		os.flush ();
  		Thread.sleep (3);
  		  		
  		currfont = font;
  		maxlines = MAXLINES[font];
  		maxchars = MAXCHARS[font];	
  		
  		//Set Font Metrics
  		bytes = new byte[7];
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = SETMET;
  		bytes[2] = (byte)LEFTM[currfont];
  		bytes[3] = (byte)TOPM[currfont];
  		bytes[4] = (byte)SPACE[currfont];
  		bytes[5] = (byte)LNSPCE[currfont];
  		bytes[6] = (byte)SCRROW[currfont];
  		
  		os.write (bytes); 
  		os.flush ();
  		Thread.sleep (3);
  		
  		//Set Top Left Position
  		bytes = new byte[3];
  		bytes[0] = (byte)0xFE;
  		bytes[1] = SETTOP;
  		
  		os.write (bytes);
  		os.flush ();
  		Thread.sleep (3);  		
  	}
  	
  	synchronized public int setBarAt (int x1, int y1, int x2, int y2, int type) throws Exception
  	{
  		byte bytes[] = new byte[8];
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = INIBAR;
  		bytes[2] = (byte)barindex;
  		bytes[3] = (byte)type;
  		bytes[4] = (byte)x1;
  		bytes[5] = (byte)y1;
  		bytes[6] = (byte)x2;
  		bytes[7] = (byte)y2;
  		
  		os.write (bytes); 
  		os.flush ();
  		Thread.sleep (5);	
  		
  		return (barindex++);	
  	}
  	
  	synchronized public void writeBar (int reference, int value) throws Exception
  	{
  		byte bytes[] = new byte[4];
  		
  		bytes[0] = (byte)0xFE;
  		bytes[1] = SETBAR;
  		bytes[2] = (byte)reference;
  		bytes[3] = (byte)value;  
  		
  		os.write (bytes); 
  		os.flush ();
  		Thread.sleep (5);	
  	}
  	
 }
  	
  	