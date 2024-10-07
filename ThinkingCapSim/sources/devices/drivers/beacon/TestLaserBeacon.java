package devices.drivers.beacon;

public class TestLaserBeacon
{
	public static String LASER_NAV200 	=	"devices.drivers.beacon.nav200.NAV200|/dev/ttyS1";
//	public static String LASER_NAV200 	=	"devices.drivers.beacon.nav200.NAV200|COM1";
	
	public static void main (String args[])
	{
		double[] measures;
		double[][] filtered;
		
		int index=0;
		int mode;		// Modo		long time;
		try{
		mode = Integer.parseInt(args[0]);
		}catch (Exception e){
		mode = 0;
		}
		System.out.println("MODE ="+mode);
	
		
		try
		{ 
			
			LaserBeacon l = LaserBeacon.getLaser (LASER_NAV200);
			if (l == null) System.out.println ("l es null");
			else System.out.println ("l="+l);
			l.debugAll(true);
			if(mode == 0){	//Modo Posicion
				
				//pause(100);
				l.activateStandby();						// Entra en modo Standby
				//pause(500);
				l.clockWise(1);								// Especifica el giro del Laser (clockwise)
				pause(500);
				l.activatePos();							// Entra en modo Posicion
				//pause(4000);
				l.selecLayer(0);							// Selecciona layer (0)
				//pause(500);
//				l.changeRad(0.5,30.0);						// Cambia el rango de donde considera las medidas del laser (de 0.5 a 30m)
//				l.selecNearest(0);							// Selecciona el numero de medidas de cada baliza que utiliza el algoritmo (0 todas las medidas)
   				//l.changePos(-0.12,0.15,0.01,0);			// Cambia la posicion inicial del NAV200
			
				while(index < 20000){							// Numero de medidas que toma
					l.activatePos();
					l.selecLayer(0);
					measures = l.getPosition ();			// Obtiene la posicion estimada
	//			    pause(10);
					//System.out.println(" Pos X,Y = ( " +measures[0]+ " , " +measures[1]+ " )  Angle = " +measures[2]*180/Math.PI+ "  G ="+measures[3]+" N="+measures[4]);
					System.out.println(measures[0]+ "  " +measures[1]+ "  " +measures[2]*180/Math.PI+ "  "+measures[3]+"  "+measures[4]);
					index ++;
				}
			}
			else if(mode == 1){		// Modo medidas
				l.activateStandby();							// Activa el modo Standby
				l.clockWise(1);									// Define el giro del Laser
				l.activateMeasures();							// Obtine medidas
				while( index < 20000){
					filtered=l.getMeasures(2);
					System.out.println("N = "+filtered.length);
					if (filtered.length>0)						// Solo se muestra la primera medida (de cada baliza se obtienen varias medidas
						for(int i=0; i<filtered.length; i++){		// Para mostrar todas las medidas
						System.out.println("        Rango["+i+"] = "+filtered[0][i]+ "   Orientacion =  " +filtered[1][i]*180/Math.PI);
						//System.out.println(" Rango["+0+"] = "+measures[0*2+1]+ "   Orientacion =  " +measures[0*2+2]*180/Math.PI);
					}
					index++;
				}
			}
			else if(mode == 2){
			   	l.activateUpload();
			   	for(int i=0;i<5;i++){
			   		measures = l.getUpload(0);
			   		if(measures[2]==-1){
			   			System.out.println("No mas beacons: "+measures[2]);
			   			break;
			   		}
			   		else									System.out.println("Beacons["+measures[2]+"] = ("+measures[0]+" , "+measures[1]+")");
				}
			}

		} catch (Exception e)
		{
			System.out.println ("Exception: "+e);
			System.exit (0);
		}
		
	}
	
	public static void pause(long time){
	long ti = System.currentTimeMillis();
	while(System.currentTimeMillis()<(ti+time));
	}
}
