package devices.drivers.lcd;

abstract class BOBWriter implements Runnable
{
	BOBII lcd;

	public BOBWriter (BOBII lcd)
	{
		this.lcd = lcd;
	}
}

public class TestBOB
{
	public static String port = "/dev/ttyS0";

	public static void main (String args[])
	{
		BOBII lcd = null;

		try
		{
			lcd = new BOBII (port);
			System.out.println (lcd);
			lcd.clrlcd ();

			lcd.writeText ("NaveTech Inc. Petrel Info");

			lcd.setTextPos (1,4);
			lcd.writeText ("Secs.: ");

			lcd.setTextPos (1,5);
			lcd.writeText ("Status: ");

      lcd.setTextPos (1,6);
			lcd.writeText ("Valor1: ");

      lcd.setTextPos (8,10);
      lcd.blinking_text ("Copyright Er Juanpe");

      lcd.setTextPos (1,10);
      lcd.enable_blink ();
      lcd.send_special (BOBII.RIGHTARR+BOBII.LEFTARR+BOBII.UPARR+BOBII.DOWNARR);
      lcd.disable_blink ();
      
			BOBWriter hilo1 = new BOBWriter (lcd)
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

			BOBWriter hilo2 = new BOBWriter (lcd)
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

			BOBWriter hilo3 = new BOBWriter (lcd)
			{
				public void run ()
				{
					while (true)
					{
						try 
						{
							this.lcd.writeTextAt ("        ",9,5);
							switch ((int)(Math.random()*3))
							{
								case 0: this.lcd.writeTextAt ("Idle",9,5);
										break;
										
								case 1: this.lcd.writeTextAt ("Moving",9,5);
										break;
								
								case 2: this.lcd.writeTextAt ("Waiting",9,5);
										break;
								
								default: this.lcd.writeTextAt ("Error",9,5);
										break;								
							}
							
						 	Thread.sleep (1500); 
						 }  catch (Exception e) {}
					}	
				}
			};					
			new Thread (hilo3).start();		
						
				
			while (System.in.read() != 0x0A);		
												

      lcd.clrlcd ();
      lcd.close ();
			System.exit(0);
		} catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		
	}
}