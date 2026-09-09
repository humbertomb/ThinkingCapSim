/** 
 * Setup.java
 *
 * Description:	    Setup for positioning System NAV200
 * @author			Jose Antonio Marin Meseguer
 * @version			
 */
package devices.drivers.beacon.nav200;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import devices.drivers.beacon.*;





/**	Setup for positioning System NAV200 */
public class Setup {
	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	int opcion;
	int layer;
	int radRef,mean;
	double posX,posY,angle;
	double[] data;
	int nbeacon;									
	double[][] pos = new double[2][50];				
	LaserBeacon l;
	boolean Ok;
	int modo = -1;
	long st=System.currentTimeMillis();
	double RTOD = (180.0/Math.PI);
	double len = 0.623;
	double alpha = 0.0;
	double rho = 0.0;
	
	public Setup(String NAV200, String pathfile) {


	try{ 
		l = LaserBeacon.getLaser (NAV200);
		l.debugAll(false);
		l.debugError(true);
		if(l.activateStandby()==false){
			System.out.println("Comunication error");
			return;
		}
		l.clockWise(1);
		if(new File(pathfile).exists()){
			Properties props = new Properties();
			props.load (new FileInputStream (new File (pathfile)));
			try { alpha		= Math.toRadians(new Double (props.getProperty ("lsbfeat0")).doubleValue ()); } catch (Exception e) 	{ alpha		= 0.0; }
			try { len		= new Double (props.getProperty ("lsblen0")).doubleValue (); } catch (Exception e) 		{ len		= 0.623; }
			try { rho		= Math.toRadians(new Double (props.getProperty ("lsbrho0")).doubleValue ()); } catch (Exception e) 		{ rho		= 0.0; }
			System.out.println("Cargado el archivo de configuracion del Robot:");
			System.out.println("lenght = "+len+"\nrho = "+Math.toDegrees(rho)+"\nalpha = "+Math.toDegrees(alpha));
		}
		else{
			System.out.println("Error al cargar el archivo de configuracion del Robot.");
			System.out.println("Parametros por defecto:\n"
					+"lenght = "+len+"\nrho = "+Math.toDegrees(rho)+"\nalpha = "+Math.toDegrees(alpha));
		}
		//l.activatePos();
		//l.selecLayer(0);
		//l.changeRad(0.5,30.0);  
	}catch(Exception e){e.printStackTrace ();
						return;}
	
	

	do{
		opcion=menu();
		switch(opcion){
		case 1:	  menuStandby(); break;
		case 2:   menuMapping(); break;
		case 3:   menuUpload(); break;
		case 4:   menuDownload(); break;		
		case 5:   menuPosition(); break;
		case 6:   menuMeasurement(); break;
		case 8:   break;
		case 9:   menuDebug(); break;
		}
	}while(opcion!=8);

	System.out.println("\nBye Bye !!!");
	}

	
	
	
	
	
	public int menu(){
	int comand=0;
	
	System.out.println("\n\n");
	System.out.println(" 	------------------------------------------ ");
	System.out.println(" 	  N A V 2 0 0 	   S E T U P");
	System.out.println(" 	------------------------------------------	");
	System.out.println(" 											");
	System.out.println(" 	1) Edition Reflector  (STANDBY)   		");
	System.out.println(" 	2) Reflector Map      (MAPPING)  		");
	System.out.println(" 	3) Show Reflector     (UPLOAD)  		");
	System.out.println(" 	4) Save Reflector     (DOWNLOAD)  		");
	System.out.println(" 	5) Get Position       (POSITION)   		");
	System.out.println(" 	6) Get Laser Measures (MEASUREMENT)  	");
	System.out.println(" 									");
	System.out.println(" 	8) EXIT									");
	System.out.println(" 											");
	System.out.print(" 	select option: ");		
	
	while(comand < 1 || comand > 9){
	comand = leerInt();
	if (comand < 1 || comand > 9) System.out.println("invalid option!");
	}
	
	return comand;
	}
	


    public void menuStandby(){
   	int comand;
	int radius = 0;
	int layer = 0;
	int maxlayer = 0;
	int cont = 0;
	ArrayList[] position;
	double[][] posit;
	do{
		comand = 0;
		System.out.println("\n\n");
		System.out.println(" 	-----------------------------------		");
		System.out.println(" 	     S T A N D B Y				    	");
		System.out.println(" 	-----------------------------------		");
		System.out.println(" 											");
		System.out.println(" 	1) Show reflector						");
		System.out.println(" 	2) Insert reflector  					");
		System.out.println(" 	3) Delete reflector   					");
		System.out.println(" 	4) Edit reflector  						");
		System.out.println(" 	5) Reflector Radius						");
		System.out.println(" 	6) Save reflector in file  				");
		System.out.println(" 	7) Load reflector in file  				");
		System.out.println(" 	8) MENU									");
		System.out.println(" 										 	");
		System.out.print(" 	select option: ");		
	
		while(comand < 1 || comand > 8){
		
		comand=leerInt();
		if (comand < 1 || comand > 8) System.out.println("invalid option!");
		}
		
		
		try{
			switch(comand){
			case 1:
				do	layer = leerInt("Layer number? ");
				while(layer<0 || layer>20); 
				nbeacon = leerInt("Number reflector (All)? ");	
				l.activateStandby();
				if (nbeacon >= 0 && nbeacon <100){
					data=l.getReflector(layer,nbeacon);
					if(data!=null)
						System.out.println(" Position of reflector "+(int)data[2]+" is ["+data[0]+" , "+data[1]+"]");
					else
						System.out.println(" Invalid Reflector ");
				}
				else{
					for(int i=0;i<50;i++){
						data=l.getReflector(layer,i);
						if(data!=null)
							System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meter");				
						else break;
					}	
				}	
				leer("Press any key to continue");
				break;
			case 2:
				layer = leerInt("Layer number? ");
				l.activateStandby();
				for(int i=0;i<50;i++){
					data=l.getReflector(layer,i);
					if(data!=null)
						System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meter");				
					else break;
				}
				nbeacon = leerInt("Reflector number? ");	
				posX =  leerDouble("X-position of reflector (meter)? ");
				posY =  leerDouble("Y-position of reflector (meter)? ");			
				l.insertReflector(posX,posY,layer,nbeacon);
				for(int i=0;i<50;i++){
					data=l.getReflector(layer,i);
					if(data!=null)
						System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meter");				
					else break;
				}
				leer("Press any key to continue");
				break;
			case 3:
				layer = leerInt("Layer number? ");
				l.activateStandby();
				cont = 0;
				for(int i=0;i<50;i++){
					data=l.getReflector(layer,i);
					if(data!=null)
						System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meter");				
					else break;
					cont++;
				}
				nbeacon = leerInt("Reflector number (-1 All)? ");	
				if(nbeacon == -1){
					for(int i = 0; i<cont; i++){
						l.deleteReflector(layer,cont-1-i);
						System.out.println("Reflector "+(cont-1-i)+" deleted");
					}
				}
				else{
					l.deleteReflector(layer,nbeacon);
				}
				for(int i=0;i<50;i++){
					data=l.getReflector(layer,i);
					if(data!=null)
						System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meter");				
					else break;
				}
				leer("Press any key to continue");
				break;
			case 4:
			    layer = leerInt("Layer number? ");
			    l.activateStandby();
			    for(int i=0;i<50;i++){
					data=l.getReflector(layer,i);
					if(data!=null)
						System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meter");				
					else break;
				}
				nbeacon = leerInt("Reflector number? ");	
				posX =  leerDouble("X-position of reflector (meter)? ");
				posY =  leerDouble("Y-position of reflector (meter)? ");
				l.changeReflector(posX,posY,layer,nbeacon);
				for(int i=0;i<50;i++){
					data=l.getReflector(layer,i);
					if(data!=null)
						System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meter");				
					else break;
				}
				leer("Press any key to continue");
				break;
			case 5:
			    layer = leerInt("Layer number? ");
			    radius = l.getRadius(layer);
				System.out.println("Radius of Layer "+layer+": "+radius+"mm");
				if(leer("Change reflector radius? ").toLowerCase().equals("y")){
					radius = leerInt("New Radius (0 to 127mm)? ");
					if(radius>0 && radius <= 127){
						l.activateStandby();
						l.setRadius(layer,radius);
						radius = l.getRadius(layer);
						System.out.println("New radius is "+radius+ "mm");
					}
					else{
						System.out.println("Radius incorrect");
					}
				}
				
				leer("Press any key to continue");
				break;
			case 6:
				
				//layer = leerInt("Layer number? ");
				l.activateStandby();
				for(layer = 0; layer<50; layer++)
					if( l.getReflector(layer,0) == null) break;
				maxlayer = layer;
				System.out.println("Found "+maxlayer + " layer");
				position = new ArrayList[maxlayer];
				for(layer = 0; layer<position.length ; layer++){
					System.out.println("\nLAYER "+layer);
					position[layer] = new ArrayList();
					for(int i=0;i<50;i++){
						data=l.getReflector(layer,i);
						if(data!=null)
							System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meter");				
						else break;
						position[layer].add(new Point2D.Double(data[0],data[1]));
					}
					
					radius = l.getRadius(layer);
					System.out.println("Radius of Reflector: "+radius);
				}
				

				if(leer("Save reflector in file (y/n)? ").toLowerCase().equals("y")){
					String file=leer("Name of file? ");
					BufferedWriter bw = new BufferedWriter(new FileWriter(file));
					bw.write("#######################################\n");
					bw.write("#          Reflector position         #\n");
					bw.write("#######################################\n\n");
					bw.write("MAXLAYER = "+maxlayer+"\n\n");
					
					for(int j = 0; j<maxlayer; j++){
						bw.write("#######################################\n");
						bw.write("##     LAYER "+j+"\n");
						bw.write("##\n");
						bw.write("LAYER_"+j+"_MAXBEACON = "+position[j].size()+"\n");
						bw.write("LAYER_"+j+"_RADIUS = "+radius+"\n");
						Point2D.Double pos;
						for(int i = 0; i<position[j].size(); i++){
							pos = (Point2D.Double)position[j].get(i);
							bw.write("LAYER_"+j+"_BEACON_"+i+" = "+pos.getX()+", "+pos.getY()+"\n");
						}
						bw.write("##\n");
						bw.write("#######################################\n\n");
					}
					bw.close();
				}
				leer("Press any key to continue");
				break;	
				case 7:
					StringTokenizer token;
					String file;
					layer = 0;
					l.activateStandby();
					System.out.println("q - Cancel      l - List");
					while(true){
						file=leer("Name of File? ");
						if(file.equals("l")){
							String[] list = new File(".").list();
							for(int i = 0; i<list.length; i++)
								System.out.println(list[i]);
						}
						else if(file.equals("q")) break;
						else if(new File(file).exists()){
							break;
						}
						else
							System.out.println("Archivo no existe.");
					}
					if(file.equals("q")){
						leer("Press any key to continue");
						break;
					}
					try{
						Properties prop = new Properties();
						prop.load(new FileInputStream(file));
						maxlayer = Integer.parseInt(prop.getProperty("MAXLAYER","0"));	
						System.out.println("MAX. LAYER: "+maxlayer);
						for(int i = 0; i<maxlayer; i++){
							int maxbeac = Integer.parseInt(prop.getProperty("LAYER_"+i+"_MAXBEACON","0"));	
							System.out.println("\nLAYER: "+i);	
							System.out.println("MAX. REFLECTOR: "+maxbeac);
							radius = Integer.parseInt(prop.getProperty("LAYER_"+i+"_RADIUS","0"));
							System.out.println("RADIUS: "+radius);
							posit = new double[maxbeac][2];						
							for(int j = 0; j<maxbeac; j++){
								token = new StringTokenizer( 
										prop.getProperty("LAYER_"+i+"_BEACON_"+j)
										," ,");
								posit[j][0] = Double.parseDouble(token.nextToken());
								posit[j][1] = Double.parseDouble(token.nextToken());
								System.out.println("Reflector_"+j+": "+posit[j][0]+", "+posit[j][1]);
							}
							
							
							
							if(leer("Do you want write this reflector in layer "+i+" (y/n)? ").toLowerCase().equals("y")){
								System.out.println("REFLECTORS IN LAYER "+i);
								cont = 0;
								for(int ref=0;ref<50;ref++){
									data=l.getReflector(i,ref);
									if(data!=null)
										System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meter");				
									else break;
									cont++;
								}
								
								if(leer("All reflector of layer "+i+" was erased, are you sure? ").toLowerCase().equals("y")){
																		
									for(int ref = 0; ref<cont; ref++){
										l.deleteReflector(i,cont-1-ref);
										System.out.println("Reflector "+(cont-1-ref)+" deleted");
									}
									
									for(int j = 0; j<maxbeac; j++){
										System.out.println("Insert Reflector "+j+": "+posit[j][0]+", "+posit[j][1]);
										l.insertReflector(posit[j][0],posit[j][1],i,j);
									}
									l.setRadius(i,radius);
									System.out.println("Set radius to "+radius);
									
									System.out.println("Reading new reflector in layer "+i);
									for(int j=0;j<50;j++){
										data=l.getReflector(i,j);
										if(data!=null)
											System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meter");				
										else break;
									}
									
									
								}
							}
							
						}
					}catch(Exception e){
						e.printStackTrace();
					}
					leer("Press any key to continue");
					break;
			default:
				break;			
			}
		}catch(Exception e){e.printStackTrace ();}
	
	}while(comand != 8);
    
    }
   
    public void menuMapping(){
	int comand;
	try{
	if(l.activateStandby()==false) return;
	if(l.activateMapping()==false) return;
	}catch(Exception e){e.printStackTrace ();}
		
	do{
	comand=0;
	System.out.println("\n\n");
	System.out.println(" 	-------------------------------		");
	System.out.println(" 	       M A P P I N G		    	");
	System.out.println(" 	-------------------------------		");
	System.out.println(" 										");
	System.out.println(" 	1) Mapping							");
	System.out.println(" 	2) Inverser Mapping		   			");
	System.out.println(" 	3) Show Reflector / NAV200 Save		");
	System.out.println(" 	4) 										");
	System.out.println(" 	5) MENU									");
	System.out.println(" 										 	");
	System.out.print(" 	select option: ");		
	
	while(comand < 1 || comand > 5){
		comand = leerInt();
		if (comand < 1 || comand > 5) System.out.println("invalid option!");
	}
	
	
		try{
			switch(comand){
			case 1:
				do	layer = leerInt("Layer number? ");
				while(layer<0 || layer>20);
				posX = leerDouble("X-position of NAV200 (meters)? ");
				posY = leerDouble("Y-position of NAV200 (meters)? ");
				angle = leerDouble("Orientation of NAV200 (degree)? ")*Math.PI/180;
				radRef = leerInt("Radius of the Reflector (milimeters)? ");
				mean = leerInt("Number of scans for mean formation (0 All)? ");
				if(mean==0)
				nbeacon=l.startMapping(posX,posY,angle,radRef,layer);
				else
				nbeacon=l.startMapping(posX,posY,angle,radRef,layer,mean);
				System.out.println(nbeacon + " Reflector scanned");
				leer("Press any key to continue");
				break;
			case 2:
				do	layer = leerInt("numero de Layer? ");
				while(layer<0 || layer>20);
				posX = leerDouble("X-position of NAV200 (meters)? ");
				posY = leerDouble("Y-position of NAV200 (meters)? ");
				angle = leerDouble("Orientation of NAV200 (degree)? ")*Math.PI/180;
				radRef = leerInt("Radius of the Reflector (milimeters)? ");
				mean = leerInt("Number of scans for mean formation (0 All)? ");
				nbeacon=l.startMappingNeg(posX,posY,angle,radRef,layer,mean);
				//l.setRadius(layer,radRef);
				System.out.println(nbeacon + " new reflector scanned");
				for(int i=0;i<100;i++){
					data=l.getMapping(layer,i);
					if(data[2]>=0.0 && data[2]<100.0){
						pos[0][i]= data[0];
						pos[1][i]= data[1];					
						System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meters");				
					}else {
						System.out.println(" --- ");
						break;
					}
				}
				if(leer("Add new reflector in TPU? ").toLowerCase().equals("y")){
					if(l.activateStandby()==false) break;
					int cont = 0;
					while(l.getReflector(layer,cont)!=null) cont++;
					for(int i=0;i<nbeacon;i++){
						l.insertReflector(pos[0][i],pos[1][i],layer,i+cont);
						System.out.println("Reflector("+(i+cont)+") = ["+pos[0][i]+" , "+pos[1][i]+"]  download");
					}
					l.setRadius(layer,radRef);
					System.out.println("Set radius to "+radRef);
					System.out.println("Reading new reflector in layer "+layer);
					for(int j=0;j<50;j++){
						data=l.getReflector(layer,j);
						if(data!=null)
							System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meter");				
						else break;
					}
				}
				
				leer("Press any key to continue");
				break;
			case 3:
				do	layer = leerInt("Layer number? ");
				while(layer<0 || layer>20);
				//for(layer=0;layer<200;layer++)
				for(int i=0;i<100;i++){
					data=l.getMapping(layer,i);
					if(data[2]>=0.0 && data[2]<100.0){
						pos[0][i]= data[0];
						pos[1][i]= data[1];
						nbeacon = i+1;					
						System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meters");				
					}else {
						System.out.println(" --- ");
						break;
					}
				}
				if(leer("Download reflector position in TPU (y/n)? ").equals("y")){
					System.out.println("position downloading");
					if(l.activateStandby()==false) break;					
					System.out.println("Set radius to "+radRef);
					l.setRadius(layer,radRef);
					if(l.activateDownload()==false) break;
					for(int i=0;i<nbeacon;i++){
					l.setDownload(pos[0][i],pos[1][i],layer,i);
					System.out.println("Reflector("+i+") = ["+pos[0][i]+" , "+pos[1][i]+"]  download");
					}
					l.setDownload(0.0,0.0,layer,-1); // La ultima debe ser -1 para indicarle el final
					System.out.println("All reflector had been downloaded"); 
				} 
				leer("Press any key to continue");
				break;
			default:
				break;			
			}
		}catch(Exception e){e.printStackTrace ();}
	
		
	}while(comand != 5);
    }
   
   
   
   
   
   
   
   
	public void menuPosition() {
		double[] pos = { 0.0, 0.0, 0.0, 0.0, 0.0 };
		double[] pos1 = { 0.0, 0.0, 0.0, 0.0, 0.0 };
		double w;
		long st = System.currentTimeMillis();
		double dt;
		long time, time1;
		int comand;
		String name;
		BufferedWriter bw;
		PrintWriter salida;

		try {
			if (l.activateStandby() == false)
				return;
			if (l.activatePos() == false)
				return;
		} catch (Exception e) {
			e.printStackTrace();
		}

		do {
			comand = 0;
			System.out.println("\n\n");
			System.out.println(
				" 	-----------------------------------		");
			System.out.println(
				" 		    P O S I T I O N	    	    	");
			System.out.println(
				" 	-----------------------------------		");
			System.out.println(" 											");
			System.out.println(
				" 	1) Get Laser Position					");
			System.out.println(
				" 	2) Get Robot Position    				");
			System.out.println(" 	3) Save position in archive 			");
			System.out.println(
				" 	4) Save position and speed in archive	");
			System.out.println(
				" 	5) Get Mean Positions					");
			System.out.println(
				" 	6) Get Laser Offset Parameter			");
			System.out.println(
				" 	7) MENU									");
			System.out.println(
				" 										 	");
			System.out.print("    	select option: ");

			while (comand < 1 || comand > 7) {
				comand = leerInt();
				if (comand < 1 || comand > 7)
					System.out.println("invalid option!");
			}

			try {
				double [][] p = {{0,0,0},{0,0,0}};
				int cont = 0;
				switch (comand) {	
					case 1 :
						layer = leerInt("Layer number? ");
						l.selecLayer(layer);
						layer = leerInt("Nearest? ");
						if(layer>0)
							l.selecNearest(layer);
						while (System.in.available() == 0) {
							l.activatePos();
							data = l.getPosition();
							for (int j = 0; j < 5; j++)
								pos[j] = data[j];
							System.out.println(
								"Pos = ["
									+ data[0]
									+ " , "
									+ data[1]
									+ "]   Angle = "
									+ data[2] * (180.0 / Math.PI)
									+ "  Q = "
									+ (int) data[3]
									+ " N = "
									+ (int) data[4]);
							System.out.print(
								"Time = "
									+ (System.currentTimeMillis() - st) * .001);
							System.out.print(
								"    Vel = "
									+ Math.sqrt(
										Math.pow(pos[0] - pos1[0], 2)
											+ Math.pow(pos[1] - pos1[1], 2))
										/ ((System.currentTimeMillis() - st)
											* 0.001)
									+ "m/s");
							System.out.println(
								"    Rot = "
									+ (180.0 / Math.PI)
										* (pos[2] - pos1[2])
										/ ((System.currentTimeMillis() - st)
											* 0.001)
									+ "deg/s");
							st = System.currentTimeMillis();
							for (int j = 0; j < 5; j++)
								pos1[j] = pos[j];
						}
						leer();
						leer("Press any key to continue");
						break;
					case 2 :
						layer = leerInt("Layer number? ");
						l.selecLayer(layer);
						l.activatePos();
						double px, py, pa;
						while (System.in.available() == 0) {
							data = l.getPosition();
							px = data[0] - len * Math.cos(data[2] - alpha);
							py = data[1] - len * Math.sin(data[2] - alpha);
							pa = data[2] - alpha - rho;

							System.out.println(
								"LaserPos = ["
									+ data[0]
									+ " , "
									+ data[1]
									+ "]   Angle = "
									+ data[2] * (180.0 / Math.PI)
									+ "  Q = "
									+ (int) data[3]
									+ " N = "
									+ (int) data[4]);
							System.out.println(
								"RobotPos = ["
									+ px
									+ " , "
									+ py
									+ "]   Angle = "
									+ pa * (180.0 / Math.PI)
									+ "  Q = "
									+ (int) data[3]
									+ " N = "
									+ (int) data[4]);
							System.out.println("Time = "+ (System.currentTimeMillis() - st) * .001);
							st = System.currentTimeMillis();
						}
						leer();
						leer("Press any key to continue");
						break;

					case 3 :
						layer = leerInt("Layer number? ");
						l.selecLayer(layer);
						name = leer("Archive name? ");
						l.activatePos();
						pause(5000);
						bw = new BufferedWriter(new FileWriter(name));
						salida = new PrintWriter(bw);
						if (leer("Show data in screen (y/n)? ").equals("y")) {
							System.out.println(
								"N \tTime \tX(m) \tY(m) \talpha(deg)");
							salida.println(
								"N \tTime \tX(m) \tY(m) \talpha(deg)");
							st = System.currentTimeMillis();
							while (System.in.available() == 0) {
								data = l.getPosition();
								data[2] *= RTOD;
								time = System.currentTimeMillis();
								salida.println(
									(int) data[4]
										+ "\t"
										+ (time - st)
										+ "\t"
										+ data[0]
										+ "\t"
										+ data[1]
										+ "\t"
										+ data[2]);
								//+"  Q = "+(int)data[3]+" N = "+(int)data[4]);
								System.out.println(
									(int) data[4]
										+ "\t"
										+ (time - st)
										+ "\t"
										+ data[0]
										+ "\t"
										+ data[1]
										+ "\t"
										+ data[2]);
							}
						} else {
							salida.println(
								"N \tTime \tX(m) \tY(m) \talpha(deg)");
							st = System.currentTimeMillis();
							while (System.in.available() == 0) {
								data = l.getPosition();
								salida.println(
									(int) data[4]
										+ "\t"
										+ (System.currentTimeMillis() - st)
										+ "\t"
										+ data[0]
										+ "\t"
										+ data[1]
										+ "\t"
										+ data[2] * RTOD);
								//+"  Q = "+(int)data[3]+" N = "+(int)data[4]);
							}

						}
						salida.close();
						leer();
						leer("Press any key to continue");
						break;

					case 4 :

						layer = leerInt("Layer number? ");
						l.selecLayer(layer);
						name = leer("Archive name? ");
						l.activatePos();
						pause(5000);
						bw = new BufferedWriter(new FileWriter(name));
						salida = new PrintWriter(bw);
						if (leer("Show data in screen (y/n)? ").equals("y")) {
							System.out.println(
								"N \tTime \tX(m) \tY(m) \talpha(deg) \tVx(m/s) \tVy(m/s) \tW(deg/s)");
							salida.println(
								"N \tTime \tX(m) \tY(m) \talpha(deg) \tVx(m/s) \tVy(m/s) \tW(deg/s)");
							st = System.currentTimeMillis();

							pos1 = l.getPosition();
							pos1[2] *= RTOD;
							time1 = System.currentTimeMillis();

							while (System.in.available() == 0) {
								data = l.getPosition();
								time = System.currentTimeMillis();
								dt = (time - time1) / 1000.0;
								data[2] *= RTOD;
								w = (data[2] - pos1[2]);
								if (w > 180.0)
									w -= 360;
								if (w < -180.0)
									w += 360;
								salida.println(
									(int) data[4]
										+ "\t"
										+ (time - st)
										+ "\t"
										+ data[0]
										+ "\t"
										+ data[1]
										+ "\t"
										+ data[2]
										+ "\t"
										+ (data[0] - pos1[0]) / dt
										+ "\t"
										+ (data[1] - pos1[1]) / dt
										+ "\t"
										+ w / dt);
								System.out.println(
									"-- N = "
										+ (int) data[4]
										+ " Time = "
										+ (time - st)
										+ "ms   X = "
										+ data[0]
										+ "m   Y = "
										+ data[1]
										+ "m   fi = "
										+ data[2]
										+ "deg");
								System.out.println(
									"   Vx = "
										+ (data[0] - pos1[0]) / dt
										+ "m/s    Vy = "
										+ (data[1] - pos1[1]) / dt
										+ "m/s    W = "
										+ w / dt
										+ "deg/s");
								time1 = time;
								for (int j = 0; j < 5; j++)
									pos1[j] = data[j];
							}
						} else {
							salida.println(
								"N \tTime \tX(m) \tY(m) \talpha(deg) \tVx(m/s) \tVy(m/s) \tW(deg/s)");
							st = System.currentTimeMillis();

							pos1 = l.getPosition();
							pos1[2] *= RTOD;
							time1 = System.currentTimeMillis();

							while (System.in.available() == 0) {
								data = l.getPosition();
								time = System.currentTimeMillis();
								dt = (time - time1) / 1000.0;
								data[2] *= RTOD;
								w = (data[2] - pos1[2]);
								if (w > 180.0)
									w -= 360;
								if (w < -180.0)
									w += 360;
								salida.println(
									(int) data[4]
										+ "\t"
										+ (time - st)
										+ "\t"
										+ data[0]
										+ "\t"
										+ data[1]
										+ "\t"
										+ data[2]
										+ "\t"
										+ (data[0] - pos1[0]) / dt
										+ "\t"
										+ (data[1] - pos1[1]) / dt
										+ "\t"
										+ w / dt);
								time1 = time;
								for (int j = 0; j < 5; j++)
									pos1[j] = data[j];
							}

						}
						salida.close();
						leer();
						leer("Press any key to continue");
						break;
					case 5:
						layer=leerInt("Layer number? ");
						l.selecLayer(layer);
						l.activatePos();
						cont = 0;
						while(System.in.available()==0){
							data=l.getPosition();
							if(data==null || (int)data[3]<60 || (int)data[4]<4) continue;
							px = data[0] - len * Math.cos (data[2] - alpha);
							py = data[1] - len * Math.sin (data[2] - alpha);
							pa = data[2] - alpha - rho;
							p[0][0] += px;
							p[0][1] += py;
							p[0][2] += pa;
							p[1][0] += data[0];
							p[1][1] += data[1];
							p[1][2] += data[2];
							cont++;
							System.out.println("LaserPos = ["+p[1][0]/cont+" , "+p[1][1]/cont+"]   Angle = "+p[1][2]*(180.0/Math.PI)/cont+"  Q = "+(int)data[3]+" N = "+(int)data[4]);
							System.out.println("RobotPos = ["+p[0][0]/cont+" , "+p[0][1]/cont+"]   Angle = "+p[0][2]*(180.0/Math.PI)/cont+"  Q = "+(int)data[3]+" N = "+(int)data[4]);
							System.out.println("Time = "+(System.currentTimeMillis()-st)*.001);
							st=System.currentTimeMillis();
						}
						leer(); 
						leer("Press any key to continue");
						break;
					case 6:
						layer=leerInt("Layer number? ");
						l.selecLayer(layer);
						l.activatePos();
						cont = 0;
						System.out.println("Posicion Exacta de la Carretilla");
						px = leerDouble("px? ");
						py = leerDouble("py? ");
						pa = Math.toRadians(leerDouble("pa(deg)? "));
						while(System.in.available()==0){
							data=l.getPosition();
							if(data==null || (int)data[3]<60 || (int)data[4]<4) continue;
							p[1][0] += data[0];
							p[1][1] += data[1];
							p[1][2] += data[2];
							cont++;
							data[0] = p[1][0]/cont;
							data[1] = p[1][1]/cont;
							data[2] = p[1][2]/cont;
							len = Math.sqrt((data[0]-px)*(data[0]-px)+(data[1]-py)*(data[1]-py));
							alpha = data[2] - (Math.atan2((data[1]-py),(data[0]-px)));
							rho = data[2] - alpha - pa;
							
							System.out.println("LaserPos = ["+p[1][0]/cont+" , "+p[1][1]/cont+"]   Angle = "+p[1][2]*(180.0/Math.PI)/cont+"  Q = "+(int)data[3]+" N = "+(int)data[4]);
							System.out.println("len = "+len+" rho = "+Math.toDegrees(rho)+"deg alpha(feat)="+Math.toDegrees(alpha)+"deg");
							System.out.println("Time = "+(System.currentTimeMillis()-st)*.001);
							st=System.currentTimeMillis();
						}
						System.out.println("PosRobot Antigua = ["+px+", "+py+", "+Math.toDegrees(pa)+"]");
						px = data[0] - len * Math.cos (data[2] - alpha);
						py = data[1] - len * Math.sin (data[2] - alpha);
						pa = data[2] - alpha - rho;
						System.out.println("PosRobot Nueva = ["+px+", "+py+", "+Math.toDegrees(pa)+"]");
						leer(); 
						leer("Press any key to continue");
						break;
						
				}

			} catch (IOException e) {
				System.out.println("Error in create archive");
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		while (comand != 7);

	}
   

    public void menuUpload(){
	String name;
	BufferedWriter bw;
	PrintWriter salida;
   	int comand;
	
	try{
		l.activateStandby();
		l.activateUpload();
	}catch(Exception e){e.printStackTrace ();}
	
	do{
		comand = 0;
		System.out.println("\n\n");
		System.out.println(" 	-----------------------------------		");
		System.out.println(" 			     U P L O A D 		    	");
		System.out.println(" 	-----------------------------------		");
		System.out.println(" 											");
		System.out.println(" 	1) Show reflector in Screen				");
		System.out.println(" 	2) Save reflector in Archive			");
		System.out.println(" 	  										");
		System.out.println(" 	  										");
		System.out.println(" 	5) MENU									");
		System.out.println(" 										 	");
		System.out.print(" 	  select option: ");		
	
		while(comand < 1 || comand > 5){
		comand = leerInt();		
		if (comand < 1 || comand > 5) System.out.println("invalid option");
		}
		
		
		try{
			switch(comand){
			case 1:
				do	layer = leerInt("Layer number? ");
				while(layer<0 || layer>20); 
				for(int i=0;i<100;i++){
					data=l.getUpload(layer);
					if(data[2]>=0.0 && data[2]<127.0)
						System.out.println(" Reflector("+(int)data[2]+")  =    ["+data[0]+" , "+data[1]+"] meters");				
					else{
						System.out.println(" ---");
						break;
					}
				}
				leer("Press any key to continue");
				break;
			case 2:
				layer = leerInt("Layer number? ");
				name = leer("Archive name? ");
				bw = new BufferedWriter(new FileWriter(name));
				salida = new PrintWriter(bw);  
				while(layer<0 || layer>20); 
				for(int i=0;i<100;i++){
					data=l.getUpload(layer);
					salida.println("layer "+layer);
					if(data[2]>=0.0 && data[2]<127.0){
						System.out.println("Reflector_"+(int)data[2]+"\t"+data[0]+"\t"+data[1]);
						salida.println("Reflector_"+(int)data[2]+"\t"+data[0]+"\t"+data[1]);
					}
					else{
						System.out.println("---");
						salida.println("---");
						break;
					}
				}
				leer("Press any key to continue");
				break;
			default:
				break;			
			}
		}catch(Exception e){e.printStackTrace ();}
	
	}while(comand != 5);
    
    }



    public void menuDownload(){
   	int comand;
	
	try{
		l.activateStandby();
		l.activateDownload();
	}catch(Exception e){e.printStackTrace ();}
		
	do{
		comand = 0;
		System.out.println("\n\n");
		System.out.println(" 	-----------------------------------		");
		System.out.println(" 		     D O W N L O A D		    	");
		System.out.println(" 	-----------------------------------		");
		System.out.println(" 											");
		System.out.println(" 	1) Save position of Reflector in NAV200 ");
		System.out.println(" 	2) Save positions sequency in NAV200  	");
		System.out.println(" 	3) Save position from archive in NAV200	");
		System.out.println(" 	  										");
		System.out.println(" 	  										");
		System.out.println(" 	5) MENU									");
		System.out.println(" 										 	");
		System.out.print(" 	select option: ");		
	
		while(comand < 1 || comand > 5){
		comand = leerInt();
		if (comand < 1 || comand > 5) System.out.println("invalid option");
		}
		
		
		try{
			switch(comand){
			case 1:
				do{
					do	layer = leerInt("Layer number? ");
					while(layer<0 || layer>20); 
					nbeacon=leerInt("Reflector number? "); 
					posX=leerDouble("X-position (meters)? ");				
					posY=leerDouble("Y-position (meters)? ");
					l.setDownload(posX,posY,layer,nbeacon);
				}while(leer("Save more reflector (y/n)? ").equals("y"));				
				leer("Press any key to continue");
				break;
			case 2:
				do	layer = leerInt("Layer number? ");
				while(layer<0 || layer>20); 
				for(int i = 0; i<127; i++){
					if(leer("Reflector "+i+" (y/n)? ").equals("n")) break;
					posX=leerDouble("X-position (meters)? ");				
					posY=leerDouble("X-position (meters)? ");
					l.setDownload(posX,posY,layer,i);
				}
				leer("Press any key to continue");
				break;
			case 3:
				System.out.println("option not implemented");
				leer("Press any key to continue");
			default:
				break;			
			}
		}catch(Exception e){e.printStackTrace ();}
		
	}while(comand != 5);
    
    }




    public void menuMeasurement(){
	int comand;
	double[] p = {0,0,0,0};
	double[][] filtered;
	try{
		l.activateStandby();
		l.clockWise(1);
		l.activateMeasures();
	}catch(Exception e){e.printStackTrace ();}
	do{
	comand=0;
	System.out.println("\n\n");
	System.out.println(" 	-----------------------------------		");
	System.out.println(" 		L A S E R    M E A S U R E S		");
	System.out.println(" 	-----------------------------------		");
	System.out.println(" 											");
	System.out.println(" 	1) Show laser measures 					");
	System.out.println(" 	2) Show laser measures filtered			");
	System.out.println(" 	3) Show laser measures XY				");
	System.out.println(" 	4) 				");
	System.out.println(" 	5) MENU									");
	System.out.println(" 										 	");
	System.out.print(" 	select option: ");		
	
	while(comand < 1 || comand > 5){
	
		comand = leerInt();
		if (comand < 1 || comand > 5) System.out.println("invalid option");
	}

	try{
		switch(comand){
		case 1:
			while(System.in.available()==0){
				data=l.getMeasures();
				if(data[0]>0.0)
					for(int j=0; j<(int)data[0];j++)	System.out.println("Measure("+j+") Range = "+data[2*j+1]+" Angle ="+data[2*j+2]*180/Math.PI);
			}
			leer();
			leer("Press any key to continue");
			break;
		case 2:
			nbeacon=leerInt("Number minimus of measures for reflector?");
			while(System.in.available()==0){
				filtered=l.getMeasures(nbeacon);
				if(filtered.length>0)
					for(int j=0; j<filtered.length;j++)	System.out.println("Measure("+j+") Range = "+filtered[j][0]+" Angle ="+Math.toDegrees(filtered[j][1])+" N = "+(int)filtered[j][2]);
			}
			leer();
			leer("Press any key to continue");
			break;	
		case 3:
			nbeacon=0;
			p[3] = leerDouble("Laser X position ("+p[0]+"meter) ? ");
			if(!Double.isNaN(p[3]))	p[0] = p[3];
			p[3] = leerDouble("Laser Y position ("+p[1]+"meter) ? ");
			if(!Double.isNaN(p[3])) p[1] = p[3]; 
			p[3] = leerDouble("Laser Angle ("+p[2]+"deg) ? ");
			if(!Double.isNaN(p[3])) p[2] = p[3]; 
			System.out.println("Offset Position = ["+p[0]+", "+p[1]+", "+p[2]+"]");
			while(System.in.available()==0){
				filtered=l.getMeasures(nbeacon);
				System.out.println("////////////////////////////////////////////////////");
				for(int j=0; j<filtered.length;j++)	System.out.println("Measure("+j+") = ["+(p[0]+filtered[j][0]*Math.cos(filtered[j][1]+Math.toRadians(p[2])))+" ,"+(p[1]+filtered[j][0]*Math.sin(filtered[j][1]+Math.toRadians(p[2])))+"]  N="+(int)filtered[j][2]);
			}
			leer();
			leer("Press any key to continue");
			break;	
		
		}	

	}catch(Exception e){e.printStackTrace ();}
		
	}while(comand!=5);
    }


    public void menuDebug(){
	try{	
		if(leer("Show all messages (DEBUGALL) y/n ? ").equals("y")) l.debugAll(true);
		else l.debugAll(false);
		if(leer("Show messages error  y/n ? ").equals("y")) l.debugError(true);
		else l.debugError(false);
	}catch(Exception e){e.printStackTrace ();}
	
	}










// Metods for reading keyboard	

	private	String leer(){
	String comando = "-1";
	try{	
		comando = br.readLine();
	}
	catch(IOException ioe){
	}
	return comando;
	}
	
	private	String leer(String texto){
	System.out.print(texto);
	return leer();
	}	
	
	private int leerInt(){
	int comando = -1;
	try{	
		comando = Integer.parseInt(leer());
	}
	catch(NumberFormatException nfe){
	}
	return comando;
	}
	
	private int leerInt(String texto){
	System.out.print(texto);
	return leerInt();
	}
	
	private double leerDouble(){
	double comando = Double.NaN;
	try{	
		comando = Double.parseDouble(leer());
	}
	catch(NumberFormatException nfe){
	}
	return comando;	
	}
	
	private double leerDouble(String texto){
	System.out.print(texto);
	return leerDouble();	
	}
	
	
	private void pause(long time){
	long t = System.currentTimeMillis();
	while(System.currentTimeMillis()<(t+time));
	} 
 
	
	
	
	// Main entry point
	static public void main(String[] args) {
	// Inicializa por defecto el puerto Serie ttyS1 (com2 en linux)
	String LASER_NAV200 = "devices.drivers.beacon.nav200.NAV200|/dev/ttyS1";
	String pathfile = "./conf/custom/ifork.cust";
	System.setProperty("java.class.path",".");
	if(args.length > 0){
		if(args[0].toLowerCase().equals("--help")){
			System.out.println("java Setup [fileconfig [serialport]]");
			System.out.println("fileconfig: ruta del fichero config del NAV200. defecto: ./conf/custom/ifork.cust");
			System.out.println("serialport: driver y nombre del serial port. defecto: devices.drivers.beacon.nav200.NAV200|/dev/ttyS1");	
			System.exit(0);
		}
		pathfile = args[0];
		if(args.length > 1)	LASER_NAV200 = args[1];
	}
	System.out.println("Iniciando programa.");
	System.out.println("Config file: "+pathfile);
	System.out.println("ClassFile: "+ LASER_NAV200);
	new Setup(LASER_NAV200, pathfile);
	System.exit(0);
	}
	
}
