package devices.drivers.laser;

public class TestLaser
{
	public static String LASER_LM200 	=	"devices.drivers.laser.LMS200.LMS200|/dev/ttyS0";
	public static String LASER_UDP 	=	"devices.drivers.laser.LaserUDP.LaserUDP|192.168.0.3:5000";
	
	public static void main (String args[])
	{
		double[] measures;
		int index;
		
		try
		{
			Laser l = Laser.getLaser (LASER_UDP);
			if (l == null) System.out.println ("l es null");
			else System.out.println ("l="+l);
			while (true)
			{
				
				measures = l.getLaserData ();
				if (measures == null) System.out.println ("measures es null!!!");
				else
				{
					System.out.println ("Received: "+measures.length);
					if (measures.length > 0)
					{
						System.out.print ("measure[0]="+measures[0]);
						index = (int)(measures.length*0.25);
						System.out.print (", measure["+index+"]="+measures[index]);
						index = (int)(measures.length*0.5);
						System.out.print (", measure["+index+"]="+measures[index]);
						index = (int)(measures.length*0.75);
						System.out.print (", measure["+index+"]="+measures[index]);
						System.out.println (", measure["+(measures.length-1)+"]="+measures[measures.length-1]);
					}
				}
				Thread.sleep (500);
			}
		} catch (Exception e)
		{
			System.out.println ("Exception: "+e);
			System.exit (0);
		}
		
	}
}