/**
 * Copyright: Copyright (c) 2002
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Canovas Qui–onero (juanpe@dif.um.es)
 * @version 1.0
 */

package devices.drivers.camera.UDPCam;

import java.net.*;
import java.util.*;

import devices.drivers.camera.*;

public class UDPCam extends Camera
{  
	private InetAddress 	servadd;
	private int 			servport;
	private DatagramSocket	sock;
	private Camera driver;
	private final boolean DEBUG = false;

	private static final int TIMEOUT=3000;
   	// Constructors  
    public UDPCam () 
    {
    	super ();
    }
    
    public void initialise (String serverLocation) throws CameraException
    {    
		//Abrir DatagramSocket
		StringTokenizer st = new StringTokenizer (serverLocation,":");
		String server = st.nextToken ();
		String port = st.nextToken ();
		String driverClass;
		
		try { servadd = InetAddress.getByName (server); } 
		catch (Exception e) { throw (new CameraException ("Host "+server+" unknown."));}
		
		try { servport = Integer.parseInt (port); } 
		catch (Exception e) { throw (new CameraException ("Port "+port+" not valid"));}
		
		try { sock = new DatagramSocket (); }
		catch (Exception e) { throw (new CameraException ("can't open the socket!!->"+e.toString()));}
		
		DatagramPacket dp = new DatagramPacket (new byte[256],256);
		try {
			String  sendChain;
			sendChain = "SEND_CAM_CLASS";
        	DatagramPacket dp2 = new DatagramPacket (sendChain.getBytes(),sendChain.length(),servadd, servport);
			if (DEBUG) System.out.println("Sending cam class solicitation: \""+sendChain+"\"");
			sock.send(dp2);
			if (DEBUG) System.out.println("Receiving cam class... ");
			sock.setSoTimeout(TIMEOUT);
			sock.receive(dp);
			driverClass=new String(dp.getData(),0,dp.getLength());
			if (DEBUG) System.out.println("Cam class received: \""+driverClass+"\"");
			driver=(Camera)Class.forName(driverClass).newInstance();
		} catch (java.io.IOException e) {
			throw (new CameraException ("Error receiving camera class: "+e.getMessage()));
		} catch (Exception e) {
			throw (new CameraException ("Error instantiaing camera class: "+e.getMessage()));
		}
    }
    
    public void close () throws CameraException
    {
    	return;
    }
    
    public boolean send (Comando comando) 
    {   
    	byte[] combytes = comando.toBytes();
        DatagramPacket dp = new DatagramPacket (combytes,combytes.length,servadd, servport);
        
    	try
    	{
    		sock.send (dp);
     	} 
    	catch (Exception e) 
    	{
    		System.out.println ("UDPCam.send: exception "+e);
    		return false;
    	}
    	
    	return true;
    }
    
       
    public boolean sendrecv (Comando comando) 
    {   
    	byte[] combytes = comando.toBytes();
        DatagramPacket dp = new DatagramPacket (combytes,combytes.length,servadd, servport);
        DatagramPacket dprec = new DatagramPacket(new byte[32],32);
        
    	try
    	{
    		sock.send (dp);
    		sock.setSoTimeout (500);
    		sock.receive (dprec);
    		sock.setSoTimeout (0);
    	} 
    	catch (Exception e) 
    	{
    		System.out.println ("UDPCam.send: exception "+e);
    		return (false);
    	}
        
        return (Boolean.getBoolean(new String (dprec.getData())));
    }
    
       
    public boolean send (Comando comando, int numero) 
    {
    	String strconc = new String (comando.toBytes()) + "|" + Integer.toString (numero); 
    	byte[] combytes = strconc.getBytes();    
    	
    	DatagramPacket dp = new DatagramPacket (combytes,combytes.length,servadd, servport);
        
    	try
    	{
    		sock.send (dp);
    	} 
    	catch (Exception e) 
      	{
    		System.out.println ("UDPCam.send: exception "+e);
    		return false;
    	}
       	        
        return true;
    }

    public boolean send (Comando comando, int param1, int param2) 
    {
    	String strconc = new String (comando.toBytes()) + "|" + Integer.toString (param1) + "|" + Integer.toString (param2); 
    	byte[] combytes = strconc.getBytes();    
    	
    	DatagramPacket dp = new DatagramPacket (combytes,combytes.length,servadd, servport);
        
    	try
    	{
    		sock.send (dp);
    	} 
    	catch (Exception e) 
      	{
    		System.out.println ("UDPCam.send: exception "+e);
    		return false;
    	}
       	        
        return true;
    }

    public boolean send (Comando comando, int param1, int param2, int param3) 
    {
    	String strconc = new String (comando.toBytes()) + "|" + Integer.toString (param1) + "|" + Integer.toString (param2) + "|" + Integer.toString (param3);
    	byte[] combytes = strconc.getBytes();    
    	
    	DatagramPacket dp = new DatagramPacket (combytes,combytes.length,servadd, servport);
        
    	try
    	{
    		sock.send (dp);
    	} 
    	catch (Exception e) 
      	{
    		System.out.println ("UDPCam.send: exception "+e);
    		return false;
    	}
       	        
        return true;
    }
    
    public boolean sendrecv (Comando comando, int numero) 
    {
    	String strconc = new String (comando.toBytes()) + "|" + Integer.toString (numero); 
    	byte[] combytes = strconc.getBytes();    
    	
    	DatagramPacket dp = new DatagramPacket (combytes,combytes.length,servadd, servport);
        DatagramPacket dprec = new DatagramPacket(new byte[32],32);
        
    	try
    	{
    		sock.send (dp);
    		sock.setSoTimeout (1500);
    		sock.receive (dprec);
    		sock.setSoTimeout (0);
    	} 
    	catch (Exception e) 
      	{
    		System.out.println ("UDPCam.send: exception "+e);
    		return (false);
    	}
       	        
        return (Boolean.getBoolean(new String (dprec.getData())));
    }
    
	public Collection getCommandGroups()
    {
		return driver.getCommandGroups();
    }
    
	public boolean isAvaiable(Comando comando)
    {
		return driver.isAvaiable(comando);
    }
}