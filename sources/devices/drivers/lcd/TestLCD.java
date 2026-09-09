package devices.drivers.lcd;

abstract class LCDWriter implements Runnable
{
	GLC24064 lcd;
	
	public LCDWriter (GLC24064 lcd)
	{
		this.lcd = lcd;
	}
}

public class TestLCD
{
	public static String port = "/dev/ttyS0";
	
	public static final byte dibujo[] = {(byte)0xFF,(byte)0xC3,(byte)0xA5,(byte)0x99,(byte)0x99,(byte)0xA5,(byte)0xC3,(byte)0xFF,(byte)0xAB,(byte)0xD5,(byte)0xFF};
	public static final byte eraser[] = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};


	public static void main (String args[])
	{
		GLC24064 lcd = null;
		
		try
		{
			lcd = new GLC24064 (port);
			System.out.println (lcd);
			lcd.clrlcd ();
			//lcd.setContrast ((byte)165);
			lcd.saveContrast((byte)165);
			
			lcd.setFont(GLC24064.FUTURA);
			lcd.setTextPos (1,1);
			lcd.writeText ("NaveTech Inc. Quaky II Data");	
			
			lcd.setFont(GLC24064.SMALL);
			lcd.setTextPos (1,4);
			lcd.writeText ("Secs.: ");
			
			lcd.setTextPos (1,6);
			lcd.writeText ("Valor1: ");
		
			lcd.setTextPos (15,5);
			lcd.writeText ("Status: ");
			
			
			LCDWriter hilo1 = new LCDWriter (lcd)
			{
				public void run ()
				{
					int countsecs = 0;
					
					while (true)
					{
						try 
						{
							this.lcd.writeTextAt (Integer.toString (countsecs++),8,4);
						 	Thread.sleep (1000); 
						 }  catch (Exception e) {}
					}
				}
			};			
			new Thread (hilo1).start();
			
			LCDWriter hilo2 = new LCDWriter (lcd)
			{
				public void run ()
				{
					while (true)
					{
						try 
						{
							this.lcd.writeTextAt ("  ",9,6);
							this.lcd.writeTextAt (Integer.toString ((int)(Math.random()*15)),9,6);
						 	Thread.sleep (300); 
						 }  catch (Exception e) {}
					}
				}
			};					
			new Thread (hilo2).start();
			
			LCDWriter hilo3 = new LCDWriter (lcd)
			{
				public void run ()
				{
					while (true)
					{
						try 
						{
							this.lcd.writeTextAt ("        ",23,5);
							switch ((int)(Math.random()*3))
							{
								case 0: this.lcd.writeTextAt ("Idle",23,5);
										break;
										
								case 1: this.lcd.writeTextAt ("Moving",23,5);
										break;
								
								case 2: this.lcd.writeTextAt ("Waiting",23,5);
										break;
								
								default: this.lcd.writeTextAt ("Error",23,5);
										break;								
							}
							
						 	Thread.sleep (1500); 
						 }  catch (Exception e) {}
					}	
				}
			};					
			new Thread (hilo3).start();		
						
			LCDWriter hilo4 = new LCDWriter (lcd)
			{
				public void run ()
				{
					int xoffset = 0;
					while (true)
					{
						try 
						{
							this.lcd.drawBitmap (1+xoffset,49,8+xoffset,59,dibujo);
							Thread.sleep (100);
							this.lcd.drawBitmap (1+xoffset,49,8+xoffset,59,eraser);
							xoffset = (xoffset+8)%240;						
						}  catch (Exception e) {}
					}	
				}
			};					
			new Thread (hilo4).start();		
				
			while (System.in.read() != 0x0A);		
												
			lcd.close ();
			System.exit(0);
		} catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		
	}
}