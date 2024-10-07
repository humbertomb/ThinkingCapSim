/**
 * Copyright: Copyright (c) 2002
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Diego Alonso (dalonso@um.es)
 * @author Humberto Martinez Barbera (humberto@um.es)
 * @author Juan Pedro Canovas Qui–onero (juanpe@dif.um.es)
 * @version 1.0
 */

package devices.drivers.camera.ccd400e;

import javax.comm.*;
import java.io.*;
import java.util.*;

import devices.drivers.camera.*;

public class CCD400E extends Camera
{  
    private InputStream						inputS		= null;					/** InputStream utilizado para recibir información desde el sensor. */
    private OutputStream					outputS		= null;					/** OutputStream utilizado para enviar comandos al sensor. */
    private SerialPort						serport		= null;
    private byte[]							hola		= new byte[20];
    private String							com;
	private final boolean 					DEBUG		= false;
    
  	// Constructors  
    public CCD400E () 
    {
    	super ();
		ComandosCCD400E.feedHash();
    }
    
    public void initialise (String commPortName) throws CameraException
    {    
        CommPortIdentifier		identifier;
        
        try 
        {
            identifier	= CommPortIdentifier.getPortIdentifier(commPortName);
            serport		= (SerialPort )identifier.open("Mitsubishi Telecamera Driver", 1000);
            serport.setSerialPortParams(9600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
            serport.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
            inputS		= serport.getInputStream();
            outputS		= serport.getOutputStream();
        }
        catch (NoSuchPortException e)				{ throw new CameraException ("No se ha pòdido encontrar el identificador del puerto.\t"+e);}
        catch (PortInUseException e)				{ throw new CameraException ("No se ha podido abrir el puerto serie. Esta ya en uso.\t"+e);}
        catch (UnsupportedCommOperationException e)	{ throw new CameraException ("Error al introduccir los parámetros de configuración del láser.\t"+e);}
        catch (IOException e)						{ throw new CameraException ("Error E/S con los Stream.\t"+e);}
//        catch (Exception e)						{ throw new CameraException ("Error inicializando la cámara.\t"+e);}
 
    }
    
    public void close () throws CameraException
    {
		try
		{
			inputS.close ();
			outputS.close ();
			serport.close ();
        } catch (Exception e) { throw new CameraException ("Error cerrando el puerto serie y/o los stremas.\t"+e);}
    }
    
    public boolean send (Comando comando) 
    {       
  		ComandoCCD400E c;
  		
  		c = (ComandoCCD400E)ComandosCCD400E.comandos.get (comando.orden);
		if (c==null)
		{
			if (DEBUG) System.out.println("Camera CCD400E does not supports command"+comando.orden);
			return true;
		}
  		return (sendCCD400E (c));
    }
    
       
    private boolean sendCCD400E (ComandoCCD400E comando) 
    {       
        try
        {
            outputS.write(("\n").getBytes());
            outputS.write(comando.Com());
        } catch (Exception e) {System.out.println("Error en outputStream\t"+e);}
        try { inputS.read(hola);}catch (Exception e) {System.out.println("Error en inputStream\t"+e);}
        if((comando.getGrupo()).equals("INQUIRY")) {
//            System.out.println("inquiry command "+new String(hola));
            comando.setStatus(new String(hola));
            return(false);
        }
        else {
            StringTokenizer strT = new StringTokenizer(new String(hola),"\r\n");
            //System.out.println("sendCCD400E: "+comando+" command ->\""+new String(hola)+"\"");
            com = strT.nextToken();
            if ( com.equals((String)ComandoCCD400E.RC) ){
                com = strT.nextToken();
                if ( com.equals(comando.getStatus()) )
                    return(false);
            }
        }
        return(true);
    }
    
    public boolean send (Comando comando, int numero) 
    {
    	ComandoCCD400E c;
  		
  		c = (ComandoCCD400E)ComandosCCD400E.comandos.get (comando.orden);
		if (c==null)
		{
			if (DEBUG) System.out.println("Camera CCD400E does not supports command \""+comando.orden+"\"");
			return true;
		}
  		return (sendCCD400E (c,numero));
        
    }
    
    public boolean sendCCD400E (ComandoCCD400E comando, int numero)
    {
        try{
            outputS.write(("\n").getBytes());
            outputS.write(comando.Com(numero));
        } catch (Exception e) {System.out.println("Error en outputStream\t"+e);}
        
        try{ inputS.read(hola);}catch (Exception e) {System.out.println("Error en inputStream\t"+e); }
        
        StringTokenizer strT = new StringTokenizer(new String(hola),"\r\n");
        com = strT.nextToken();
        if ( com.equals((String)ComandoCCD400E.RC) ){
            com = strT.nextToken();
            if ( com.equals(comando.getStatus()) )
                return(false);
        }
        return(true);
    }
    
    public Collection getCommandGroups()
    {
		Vector groups= new Vector();
	
		Enumeration en;
		ComandoCCD400E comando;
		en = ComandosCCD400E.comandos.elements();
		while (en.hasMoreElements())
		{
			comando = (ComandoCCD400E)en.nextElement();
			if (!groups.contains(comando.grupo))
			{
				groups.add(comando.grupo);
			}
		}
		return groups;
    }
    
    public boolean isAvaiable(Comando comando)
    {
		return ComandosCCD400E.comandos.containsKey(comando.orden);
    }
}