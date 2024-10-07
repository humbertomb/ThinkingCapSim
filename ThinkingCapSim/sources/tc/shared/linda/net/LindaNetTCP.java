/*
 * (c) 2001 Humberto Martinez, Jose Ma Garcia Nieto
 * (c) 2002-2004 Humberto Martinez
 * 	   2004 Sergio Aleman
 */

package tc.shared.linda.net;

import java.io.*;
import java.net.*;
import java.util.*;

import tc.shared.linda.*;

public class LindaNetTCP extends LindaNet
{
	static public final int			CLIENT		= 0;
	static public final int			SERVER		= 1;
	
	static public final int			MAX_CLIENT	= 30;
	static public final int			HDR_LEN		= 4;
	static public final int			BUF_LEN		= 1000 * 1024;
	
	// TCP client stuff 
	protected Socket					csocket;
	protected LindaNetTCPClient		nclient;
	protected byte[]					cbuffer;
	
	// TCP server stuff
	protected ServerSocket			ssocket;
	protected LindaNetTCPServer		nserver;
	protected Hashtable				pserver;
	
	// Local states and synchronization
	private int						mode;
	private byte[]					header		= new byte[HDR_LEN];
	protected int					wcount		= 0;
	protected int					rcount		= 0;
	
	// Constructors
	public LindaNetTCP (InetAddress server, int port, LindaNetProcessor processor)
	{
		super (server, port, processor);
		
		nclient		= new LindaNetTCPClient (csocket, this);
	}

	public LindaNetTCP (int port, LindaNetProcessor processor)
	{
		super (port, processor);
		
		pserver		= new Hashtable (MAX_CLIENT);
		nserver		= new LindaNetTCPServer (this);
	}

	// Class methods
	static public int getHeader (byte[] header)
	{
		return (int) ((int) ((header[3] << 24) & 0xFF000000) | (int) ((header[2] << 16) & 0xFF0000) | (int) ((header[1] << 8) & 0xFF00) | (int) (header[0] & 0xFF));
	}
	
	static public void setHeader (byte[] header, int value)
	{
		int			uval;
		
		uval			= value;
		header[3]	= (byte) (uval >> 24);
		uval			= uval % 0x1000000;
		header[2]	= (byte) (uval >> 16);
		uval			= uval % 0x10000;
		header[1]	= (byte) (uval >> 8);
		uval			= uval % 0x100;
		header[0]	= (byte) uval;
	}
	
	// Inner classes
	class LindaNetTCPClient implements Runnable
	{
		protected LindaNetTCP		linda;
		protected Socket				socket;
		protected InputStream		istream;
		
		protected LindaNetPacket		packet;
		protected byte[]				buffer;
		protected boolean			running;
		protected boolean			abort;
				
		public LindaNetTCPClient (Socket socket, LindaNetTCP linda)
		{
			Thread				thread;
			
			this.linda	= linda;
			this.socket	= socket;
			
			packet		= new LindaNetPacket (TCP);
			buffer		= new byte[BUF_LEN];

			try
			{
				socket.setSoTimeout(1000);
				istream		= socket.getInputStream ();
			} catch (Exception e) { e.printStackTrace(); }

			thread		= new Thread (this);
			thread.start ();
		}
		
		public void run ()
		{
			boolean			inHdr;
			int				inLen, inPos;
			int				inRcvd, inNext;
		
			Thread.currentThread ().setName ("TC-LindaNetClient-TCP");
			running	= true;
			abort	= false;
			
			inHdr	= true;
			inLen	= inNext = HDR_LEN;
			inPos	= 0;
			while (running)
			{
				try
				{
					inRcvd	= istream.read (buffer, inPos, inNext);
					
					if (inRcvd < 0)
					{
						running	= false;
						break;
					}
					
					if (inRcvd < inNext)
					{
						inPos	= inPos + inRcvd;
						inNext	= inNext - inRcvd;

						continue;
					}

					if (inHdr)
					{
						inLen	= inNext = getHeader (buffer);
						inHdr	= false;
					}
					else
					{
						packet.decode (buffer, 0, inLen);
						packet.set (socket.getInetAddress(), socket.getPort());

						if (processor != null)
							processor.process (linda, packet);
			
						rcount ++;
						
						inLen	= inNext = HDR_LEN;
						inPos	= 0;
						inHdr	= true;
					}
				}
				catch (SocketTimeoutException ste){
//					System.out.println("LindaNetTCPClient run() No ha recibido datos por el socket");
				}
				catch (SocketException e)	{
					if(!abort && nclient!=null && nclient.socket!=null)
						System.out.println("--[LindaNetTCP]:run exception local="+nclient.socket.getLocalAddress()+":"+nclient.socket.getPort()+" remote="+nclient.socket.getInetAddress()+":"+nclient.socket.getPort()+" "+e.toString());
					else if(!abort){
						System.out.println("--[LindaNetTCP]:run exception "+e.toString());
						e.printStackTrace();
					}
//					e.printStackTrace();
					running = false; 
					}
				catch (Exception e)			{ e.printStackTrace (); running = false; }
				
				Thread.yield ();
			}
		}		

		public void stop ()
		{
			abort = true;	

			try { Thread.sleep (1000); } catch (Exception e) {e.printStackTrace(); }
			try { socket.close (); } catch (Exception e) {e.printStackTrace(); }
			try { istream.close (); } catch (Exception e) {e.printStackTrace(); }
			
			while (running)
				try { Thread.sleep (50); } catch (Exception e) {e.printStackTrace(); }
		}
	}
	
	class LindaNetTCPServer implements Runnable
	{
		protected LindaNetTCP	linda;
		protected boolean		running;
		
		public LindaNetTCPServer (LindaNetTCP linda)
		{
			Thread				thread;
			
			this.linda	= linda;
			
			thread		= new Thread (this);
			thread.start ();
		}
		
		public void run ()
		{
			Socket				socket;
			LindaNetTCPClient	client;
			
			Thread.currentThread ().setName ("TC-LindaNetServer-TCP");
			running	= true;
			while (running)
			{
				try 
				{
					socket	= ssocket.accept ();
					socket.setReceiveBufferSize (BUF_LEN);
					socket.setSendBufferSize (BUF_LEN);
					
					client	= new LindaNetTCPClient (socket, linda);
					pserver.put (new String (socket.getInetAddress()+" "+socket.getPort ()), client);
				} catch (Exception e) { e.printStackTrace(); }
	
				Thread.yield ();
			}
		}
				
		public void stop ()
		{
			Enumeration			enu;
			Integer				key;
			LindaNetTCPClient	client;
			
			running	= false;
			
			enu		= pserver.keys ();
			while (enu.hasMoreElements ())
			{
				key		= (Integer) enu.nextElement ();
				client	= (LindaNetTCPClient) pserver.get (key);
				pserver.remove (key);
				client.stop ();
			}
			pserver.clear ();
			
			try { ssocket.close (); } catch (Exception e) { e.printStackTrace(); }
		}
	}

	// Accessors
	public InetAddress	getLocalAddress ()	{ if (mode == SERVER) return ssocket.getInetAddress(); else return csocket.getLocalAddress(); }
	public int  			getLocalPort ()		{ if (mode == SERVER) return ssocket.getLocalPort(); else return csocket.getLocalPort(); }

	// Instance methods
	protected void client_socket (InetAddress peer, int port)
	{
		mode		= CLIENT;
		
		try 
		{ 
			csocket		= new Socket (peer, port); 
			csocket.setReceiveBufferSize (BUF_LEN);
			csocket.setSendBufferSize (BUF_LEN);
		} catch (Exception e) { e.printStackTrace (); }
		
		cbuffer		= new byte[BUF_LEN];
		packet		= new LindaNetPacket (TCP);
		packet.set (peer, port);
	}
	
	protected void server_socket (int port)
	{
		boolean		open;
		int			tries;
		
		mode		= SERVER;
		
		open		= false;
		tries	= 0;
		while (!open)
		{
			open = true;
			try { ssocket = new ServerSocket (port + tries); } 
			catch (IOException e)
			{
				open = false;
				tries++;
			}
		}
		
		packet	= new LindaNetPacket (TCP);
	}
	
	public synchronized void close ()
	{ 
		if (nserver != null)		nserver.stop ();
		nserver		= null;
		ssocket		= null;
		
		if (nclient != null)		nclient.stop ();
		nclient		= null;
		csocket		= null;
	}

	public synchronized boolean sendTuple (LindaNetConn dconn, int command, Tuple tuple)
	{		
		OutputStream			ostream;
		LindaNetTCPClient	client;
		byte[]				buffer;
		
		if (mode == CLIENT)
		{
			if (csocket == null)					return false;
			
			try { ostream	= csocket.getOutputStream (); } catch (Exception e) { 
				System.err.print("--[LindaNetTCP]sendTuple mode=CLIENT Error al obtener socket "+tuple+" connection="+dconn+" command="+command);
				if(e.toString().indexOf("Broken pipe")!=-1){
					System.err.println(" Broken pipe.");
				}else{
					System.err.println(e.toString());
				}
				return false; 
			}
		}
		else
		{
			client	= (LindaNetTCPClient) pserver.get (dconn.peer+" "+dconn.port); 
			if (client == null)						return false;
			
			try { ostream	= client.socket.getOutputStream (); } catch (Exception e) { 
				System.err.print("--[LindaNetTCP]sendTuple mode=SERVER Error al obtener socket "+tuple+" connection="+dconn+" command="+command);
				if(e.toString().indexOf("Broken pipe")!=-1){
					System.err.println(" Broken pipe.");
				}else{
					System.err.println(e.toString());
				}
				return false; 
			}
		}
			
		packet.set (command, tuple); 
		buffer	= packet.encode ();

		try 
		{
			setHeader (header, buffer.length);
			ostream.write (header);
			ostream.write (buffer);
			ostream.flush ();
			wcount ++;
		} catch (Exception e) { 
			
/*			if(e.toString().indexOf("Broken pipe")!=-1){
				System.err.print("--[LindaNetTCP]sendTuple mode="+(mode==0?"CLIENT":(mode==1?"SERVER":"Desconocido:"+mode))+" Error al enviar tupla "+tuple+" connection="+dconn+" command="+command+"buffer.length="+buffer.length+" header.length="+header.length+"  Broken pipe.");
			}else if(e.toString().indexOf("Connection reset by peer")!=-1){
//				Si un cliente de linda se cierra sin cerra el socket, se produce esta excepcion
//				Eliminar de la linda todas sus conexiones
				processor.manage(LindaNetProcessor.DELETE,new LindaNetListener(null,dconn));
			}else{
				System.err.println(e.toString());
				e.printStackTrace();
			}*/
			System.err.println("--[LindaNetTCP]sendTuple mode="+(mode==0?"CLIENT":(mode==1?"SERVER":"Desconocido:"+mode))+" Error al enviar tupla "+tuple+" connection="+dconn+" command="+command+"buffer.length="+buffer.length+" header.length="+header.length);
			System.err.println(e.toString());
			processor.manage(LindaNetProcessor.DELETE,new LindaNetListener(null,dconn),tuple.space);
			return false; 
		}
		return true;
	}
}