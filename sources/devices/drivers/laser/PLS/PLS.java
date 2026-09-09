package devices.drivers.laser.PLS;

import devices.drivers.laser.*;

import java.io.*;
import java.util.StringTokenizer;

import javax.comm.*;

/**
 * @author Administrador
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PLS extends Laser
{
	static public final int			TIMEOUT 		= 2000;		// Timeout al leer un dato (en ms)
	static public final int			RETRIES		= 5;
	
	protected InputStream				istream;	
	protected OutputStream			ostream;
	protected SerialPort				port;
	
	protected String					model;
	protected PLSDatagram				datagram;

	public PLS () throws LaserException 
	{
		super ();
	}
	
	public void initialise (String param) throws LaserException
	{
		openLaserPort (param, "PLS", SerialPort.PARITY_EVEN);
	}

	protected void openLaserPort (String param, String model, int parity) throws LaserException
	{
		CommPortIdentifier 		id;

		this.model = model;
		System.out.println("  ["+model+"] Initialise: Triying "+param+"...");
		try 
		{
			id		= CommPortIdentifier.getPortIdentifier(param);
			port	= (SerialPort) id.open (model, 1000);	
			port.setSerialPortParams (38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, parity);
			port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			port.enableReceiveTimeout(TIMEOUT);
			istream	= port.getInputStream();
			ostream	= port.getOutputStream();
			datagram	= new PLSDatagram (istream, ostream);

			clearBuffer();
			changeToMonitoringMode ();
			Thread.sleep(50);
			clearBuffer();
		}
		catch (Exception e)	
		{
			e.printStackTrace();
			System.out.println("--["+model+"] "+ e.getMessage ()); 
			throw new LaserException (e.getMessage());
		}		
		System.out.println("  ["+model+"] Initialise: "+param+" OK.");
	}

	protected boolean waitForBuffer (int maxtime)
	{
		long			stime;
		
		stime = System.currentTimeMillis ();
	    try
	    {
	    		while ((istream.available() <= 0) && (System.currentTimeMillis() - stime < maxtime))
	    			Thread.yield ();
	    		return istream.available() > 0;
		} catch (Exception e) { e.printStackTrace(); }

		return false;
	}
	
	protected void clearBuffer()
	{
		try 
		{
			istream.skip (istream.available ());
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	/** Cambia al modo monitorizacion 0x25 (Solo se reciben medidas cuando se solicitan)
	*  El modo por defecto cuando arranca es 0x21 (Modo monitorizacion, se reciben medidas cuando se solicitan 
	*  pero si se detecta un objeto dentro del campo de proteccion se manda automaticamente).
	*/
	public void changeToMonitoringMode()
	{
		byte[] comand = {0x25};
		
		try
		{
			datagram.createTelegram (PLSDatagram.ADDRBROADCAST, PLSDatagram.CMD_CHOOSE_OPERATING_MODE, comand);
			datagram.sendTelegram ();
		} catch (Exception e) { }
	}
	
	/** 
	 * Obtiene las medidas del laser (rangos en metros).
	*/
	public double[] getLaserData () throws LaserException
	{
		byte[] comand = {0x1};
		
		try 
		{
			datagram.createTelegram (PLSDatagram.ADDRBROADCAST, PLSDatagram.CMD_REQUEST_MEASURED_VALUES, comand);
			datagram.sendTelegram (RETRIES);
			datagram.readTelegram ();	
        } catch (IOException e) { e.printStackTrace(); }
        
		if (datagram.length == 726)
			return datagram.toRange ();
		return null;
	}

	/** Configura el campo de proteccion del laser (rectangular)
	 * @param heigth Largo del Campo de proteccion
	 * @param width Ancho del campo de proteccion
	 * @return
	 */
	public boolean setProtectiveField (int heigth, int width)
	{
		byte[] password = {0,'S','I','C','K','_','P','L','S'};	// Password por defecto
		
		try {
            //	Cambiando a modo Seguridad (introduciendo password)
		    datagram.createTelegram (PLSDatagram.ADDRBROADCAST, PLSDatagram.CMD_CHOOSE_OPERATING_MODE, password);
		    System.out.print("Send Changing the operating mode (Setup): ");
		    datagram.sendTelegram (RETRIES);
		    System.out.println("ok!");
		    // Espera respuesta 5 segundos maximo (tarda sobre 3sg)
		    if(!waitForBuffer(5000)){
		        System.out.println("time exceded");
		        return false;
		    }
            datagram.readTelegram ();
            System.out.println("PLS response: \n"+datagram);
            
                        
            // Enviando telegrama de informacion del campo
            byte[] data = { 0 , 0 };
            datagram.createTelegram (PLSDatagram.ADDRBROADCAST, PLSDatagram.CMD_REQUEST_CONFIGURED_FIELDS, data);
            System.out.print("Send Configured protective fields request: ");
            datagram.sendTelegram (RETRIES);
            System.out.println("ok!");
            datagram.readTelegram ();
            System.out.println("PLS response: \n"+datagram);
            
            if( (datagram.getCmd()!= PLSDatagram.RESP_REQUEST_CONFIGURED_FIELDS) || datagram.getData().length<14){
            	System.out.println("Error recibiendo configuracion del campo de proteccion");
            	return false;
            }
            
            // Enviando campo (hay que enviar la fecha (posterior a la que tiene) y calcular un checksum especial EXP)
            // Se manda la misma fecha leida incrementando un minuto
            short left = (short)(width / 2);;
            short rigth = (short)(width / 2);
            byte minutos = datagram.getByte(1);
            byte hora = datagram.getByte(2);
            byte dia = datagram.getByte(3);
            byte mes = datagram.getByte(4);
            byte yearlow = datagram.getByte(5);
            byte yearhi = 0; //datagram.getByte(6);		
            minutos ++;
            if(minutos>=60){
            	minutos = 0;
            	hora ++;
            	if(hora >= 24){
            		hora = 0;
            		dia ++;
            		if(dia >= 30){
            			dia = 0;
            			mes ++;
            			if(mes >= 13)
            				mes = 0;
            				yearlow++;
            		}
            	}	
            }

            byte[] config = {0, minutos, hora, dia, mes, yearlow, yearhi,
            	PLSDatagram.lowbyte(left), PLSDatagram.hibyte(left),
            	PLSDatagram.lowbyte(rigth), PLSDatagram.hibyte(rigth),
            	PLSDatagram.lowbyte((short)heigth), PLSDatagram.hibyte((short)heigth),
            	PLSDatagram.lowbyte((short)heigth), PLSDatagram.hibyte((short)heigth)
            };
            
            datagram.createTelegram (PLSDatagram.ADDRBROADCAST, PLSDatagram.CMD_CONFIGURE_FIELDS, config);
            System.out.print("Send Configure the safe protective field: ");
            datagram.sendTelegram(RETRIES);
            System.out.println("ok!");
            datagram.readTelegram ();
            System.out.println("PLS response: \n"+datagram);

            //		Iniciando Confirmacion
            data = new byte[1];
            data[0] = 1;
            datagram.createTelegram (PLSDatagram.ADDRBROADCAST, PLSDatagram.CMD_SWITCH_ACTIVE_FIELD_SET, data);
            System.out.print("Send Confirmation of the configured safe protective field (Init): ");
            datagram.sendTelegram(RETRIES);
            System.out.println("ok!");
            datagram.readTelegram ();
            System.out.println("PLS response: \n"+datagram);
            
            if(datagram.getByte(0)!=0x3){
            	System.out.println("PLS|setProtectiveField: Error iniciando confirmacion");
            	if(datagram!=null) System.out.println("DATAGRAMA RECIBIDO: "+datagram);
            	return false;
            } 	    
        
            // Finalizando Confirmacion	
            data[0] = 2;
            datagram.createTelegram (PLSDatagram.ADDRBROADCAST,(byte)0x41, data);
            System.out.print("Send Confirmation of the configured safe protective field (End): ");
            datagram.sendTelegram(RETRIES);
            System.out.println("ok!");
            datagram.readTelegram ();
            System.out.println("PLS response: \n"+datagram);
            if(datagram.getByte(0)!=0x1){
            	System.out.println("PLS|setProtectiveField: Error finalizando confirmacion");
            	if(datagram!=null) System.out.println("DATAGRAMA RECIBIDO: "+datagram);
            	return false;
            }  	
            
            // Cambiando a Modo Monitorizacion (tarda otros 2-3sg)
            data[0] = 0x21;
            datagram.createTelegram (PLSDatagram.ADDRBROADCAST,(byte)0x20, data);
            System.out.print("Send Changing th operating mode (Monitoring): ");
            datagram.sendTelegram(RETRIES);
            System.out.println("ok!");
            // Espera respuesta 5 segundos maximo (tarda sobre 3sg)
            if(!waitForBuffer(5000)){
		        System.out.println("time exceded");
		        return false;
		    }
            datagram.readTelegram ();
            System.out.println("PLS response: \n"+datagram);
            	
            if(datagram.getByte(0)!=0x0){
            	System.out.println("PLS|setProtectiveField: Error cambiando modo monitorizacion");
            	return false;
            } 	
         		
            return true;
            
        } catch (Exception e) { e.printStackTrace(); }
        
        return false;
	}
	
	public void close(){
		try {
			port.close();
			istream.close();
			ostream.close();
		} catch (IOException e) { e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		String LASER =	"devices.drivers.laser.PLS.PLS|";
		String defport = "/dev/ttyS0";
		int read;
		PLS pls = null;
		long st;
		int delay = 50;
		int cont = 0;
		double[] data;
		if(args !=null){
		    if(args.length>0)	defport = args[0];
		    if(args.length>1)	delay = Integer.parseInt(args[1]);
		}
		else{
		    System.out.println("Test del PLS. Arrancando con puerto por defecto: "+defport);
		    System.out.println("Para utilizar otro puerto, es necesario especificarlo en el argumento.");
		    System.out.println("Pulsar 'c' para entrar en Modo Consola (envio manual de comandos al PLS)");
		    System.out.println("INTRO PARA SALIR");
		    System.out.println("'+' y '-' Incrementa o decrementa el retardo entre medidas (por defecto = 0) ");
		}
		try {
			pls = (PLS) Laser.getLaser(LASER+defport);
			while(true){
				try{
							    
				    if(delay>0) Thread.sleep(delay);
							    
					st = System.currentTimeMillis();
					data = pls.getLaserData();
					st = System.currentTimeMillis()-st;
					
					if(data!=null)
						System.out.println("0deg = "+(int)(data[data.length/2]*100)/100.0+ "m ("+data.length+" barridos en "+st+"ms) delay="+delay);
					else
						System.out.println("No se ha obtenido ningun barrido");
					if(System.in.available()>0){
					    read = System.in.read();
					    if(read=='+'){
					        delay += 50;
					        System.out.println("delay set to "+delay);
					        while(System.in.available()>0) System.in.read();
					    }
					    else if(read=='-'){
					        delay -= 50;
					        System.out.println("delay set to "+delay);
					        while(System.in.available()>0) System.in.read();
					    }
					    else break;
					}
				}catch(Exception e){
				    e.printStackTrace();
				}
			}
			
			if(System.in.read() == 'c'){
			    // Modo consola
			    while(System.in.available()>0) System.in.read();
			    String str;
			    StringTokenizer tk;
			    String word;
			    int wordint;
			    byte command;
			    byte[] databyte;
			    BufferedReader keybd = new BufferedReader(new InputStreamReader(System.in));
			    PLSDatagram datagram = new PLSDatagram (pls.istream, pls.ostream);
			    System.out.println("Modo Consola Activo ('q' salir). Envio de comandos al PLS (address = 0) ");
			    System.out.println("Formato para enviar:\n Comando Dato0 Dato1 Dato2 ...");
			    System.out.println("Para valores hexadecimales utilizar el prefijo 0x\n\n");
			    while(true){
			        try{
					    System.out.print("Send data: ");
					    str = keybd.readLine();
					    if(str.equalsIgnoreCase("q")) break;
					    if(str.equalsIgnoreCase("reset")){pls.istream.reset(); continue;}
					    if(str.equalsIgnoreCase("skip")){pls.istream.skip(pls.istream.available()); System.out.println("avalaible = "+pls.istream.available());continue;}
					    if(str.equalsIgnoreCase("available")){System.out.println("avalaible = "+pls.istream.available()); continue;}
					    if(str.equalsIgnoreCase("showbuffer")){
					    	for(int i = 0; i<1000; i++){
					    		if(pls.istream.available()==0){
					    			System.out.println("No mas datos en buffer");
					    			break;
					    		}
					    		else{
					    			read = pls.istream.read();
					    			if(read==0x02) System.out.print("\n\t"+Integer.toHexString(read)+"h ");
					    			else System.out.print(Integer.toHexString(read)+"h ");
					    		}
					    	}
					    	if(pls.istream.available()>0) System.out.println("Todavia quedan "+pls.istream.available()+"!!!");
					    	continue;
					    }
					    tk = new StringTokenizer(str);
					    cont = 0;
					    command = 0;
					    if(tk.countTokens()>0)
					        databyte = new byte[tk.countTokens()-1];
					    else
					        databyte = null;
					    while(tk.hasMoreTokens()){
					        word = tk.nextToken();
					        if(word.startsWith("0x"))
					            wordint = Integer.parseInt(word.substring(2),16);
					        else
					            wordint = Integer.parseInt(word);
					        if(cont==0)
					            command = (byte)wordint;
					        else
					            databyte[cont-1] = (byte)wordint;
					        cont++;
					    }
					    datagram.createTelegram ((byte)0, command, databyte);
				        System.out.println("sending:\n"+datagram);
				        datagram.sendTelegram();
				        datagram.readTelegram();
				    	System.out.println("reading:\n"+datagram);
				    }catch(Exception e){e.printStackTrace();}
				}
			    
			}
									
			pls.close();
			System.exit(0);
		} catch (Exception e) {
			System.out.println("Error en los parametros.");
			e.printStackTrace();
		}
	}	
			
}
