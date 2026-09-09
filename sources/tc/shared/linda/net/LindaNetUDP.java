/*
 * (c) 2001 Humberto Martinez, Jose Ma Garcia Nieto
 * (c) 2002-2004 Humberto Martinez
 */

package tc.shared.linda.net;

import java.net.*;

import tc.shared.linda.*;

public class LindaNetUDP extends LindaNet
{
	protected DatagramSocket		socket;
	protected DatagramPacket 		idatagram;
	protected DatagramPacket 		odatagram;
	protected byte[]				ibuffer;		// Input data buffer
	protected byte[]				obuffer;		// Output data buffer
	
	protected LindaNetUDPServer		nserver;
	
	// Constructors
	public LindaNetUDP (InetAddress server, int port, LindaNetProcessor processor)
	{
		super (server, port, processor);
		
		nserver	= new LindaNetUDPServer (this);
	}

	public LindaNetUDP (int lport, LindaNetProcessor processor)
	{
		super (lport, processor);

		nserver	= new LindaNetUDPServer (this);
	}

	class LindaNetUDPServer implements Runnable
	{
		protected LindaNetUDP	linda;
		protected boolean		running;
		
		public LindaNetUDPServer (LindaNetUDP linda)
		{
			Thread				thread;
			
			this.linda	= linda;
			
			thread		= new Thread (this);
			thread.start ();
		}
		
		public void run ()
		{
			LindaNetPacket		packet;
			
			Thread.currentThread ().setName ("TC-LindaNetServer-UDP");
			running	= true;
			while (running)
			{
				packet = receiveTuple ();
				if ((packet != null) && (processor != null))
					processor.process (linda, packet);
				
				Thread.yield ();
			}
		}
		
		public void stop ()
		{
			running	= false;
		}
	}

	// Accessors
	public InetAddress		getLocalAddress ()		{ return socket.getLocalAddress(); }
	public int  				getLocalPort ()			{ return socket.getLocalPort(); }

	// Instance methods
	protected void client_socket (InetAddress peer, int port)
	{
		try { socket		= new DatagramSocket (); } catch(Exception e) { }
		
		packet		= new LindaNetPacket (UDP);
		packet.set (peer, port);
		
		ibuffer		= new byte[LindaNetPacket.LEN_DATA];
		obuffer		= new byte[LindaNetPacket.LEN_DATA];
		idatagram	= new DatagramPacket (ibuffer, ibuffer.length);
		odatagram	= new DatagramPacket (obuffer, obuffer.length, peer, port);
	}
	
	protected void server_socket (int port)
	{
		boolean		open;
		int			tries;
		
		open		= false;
		tries		= 0;
		while (!open)
		{
			open = true;
			try { this.socket = new DatagramSocket (port + tries); } 
			catch (SocketException e)
			{
				open = false;
				tries++;
			}
		}
		
		packet		= new LindaNetPacket (UDP);
		ibuffer		= new byte[LindaNetPacket.LEN_DATA];
		obuffer		= new byte[LindaNetPacket.LEN_DATA];
		idatagram	= new DatagramPacket (ibuffer, ibuffer.length);
		odatagram	= new DatagramPacket (obuffer, obuffer.length);
	}
	
	public synchronized void close ()
	{ 
		if (socket != null) 		socket.close ();
		socket		= null;
		
		if (nserver != null)		nserver.stop ();
		nserver		= null;
	}
	
	public boolean sendTuple (LindaNetConn dconn, int command, Tuple tuple)
	{
		byte[]		tbuffer;
		byte[]		sendbuf;
		int firstbyte = 0;
		int npackets;
		int idpacket=0;
		int index;
				
		try 
		{
			packet.set (command, tuple); 
			tbuffer	= packet.encode ();
			sendbuf = new byte[LindaNetPacket.LEN_DATA];
			firstbyte = 0;

			npackets = (int)Math.ceil((double)tbuffer.length/(double)(LindaNetPacket.LEN_DATA-1));
			idpacket = npackets;
			while (idpacket!=0)
			{
				idpacket--;
				if (idpacket == (npackets-1)) // Es el primero -> Se manda la cola del mensaje
				{
					sendbuf = new byte[tbuffer.length-((LindaNetPacket.LEN_DATA-1)*(npackets-1))+1];
					firstbyte = idpacket * (LindaNetPacket.LEN_DATA-1);
					sendbuf[0] = (byte) idpacket;
					index = 1;
					for (int i = firstbyte; i<tbuffer.length;i++)
					{
						sendbuf[index] = tbuffer[i];
						index++;
					}
				}
				else 
				{
					sendbuf = new byte[LindaNetPacket.LEN_DATA];
					firstbyte = idpacket*(LindaNetPacket.LEN_DATA-1);
					sendbuf[0] = (byte) idpacket;
					index = 1;
					for (int i = firstbyte; i<firstbyte+(LindaNetPacket.LEN_DATA-1);i++)
					{
						sendbuf[index] = tbuffer[i];
						index++;
					}
				}

				odatagram.setData (sendbuf, 0, sendbuf.length);	
				if (dconn != null)
				{
					odatagram.setAddress (dconn.peer);
					odatagram.setPort (dconn.port);
				}
				
				if (socket == null)					return false;
				socket.send (odatagram);
			}
		} catch (Exception e) { e.printStackTrace(); return false; }
		
		return true;
	}
	
	protected LindaNetPacket receiveTuple ()
	{
		byte[] receivedBuf;
		byte[] completBuf;
		byte idpacket;
		int firstbyte = 0;
		int index = 0;
		
		try 
		{
			idatagram.setData (ibuffer, 0, ibuffer.length);
			if (socket == null)						return null;
			socket.receive (idatagram);
			if (idatagram.getLength() <= 0)			return null;
			
			receivedBuf = idatagram.getData();
			idpacket = receivedBuf[0];
			completBuf = new byte[idpacket*(LindaNetPacket.LEN_DATA-1)+idatagram.getLength()-1];

			firstbyte = idpacket*(LindaNetPacket.LEN_DATA-1);
			index = 1;
			for (int i=firstbyte ;i<firstbyte+(idatagram.getLength()-1);i++)
			{
				completBuf[i]=receivedBuf[index];
				index++;
			}
			while (idpacket != 0)
			{
				idatagram.setData (ibuffer, 0, ibuffer.length);
				if (socket == null)					return null;
				socket.receive (idatagram);
				if (idatagram.getLength() <= 0)		return null;
				
				receivedBuf = idatagram.getData();
				idpacket = receivedBuf[0];
				firstbyte = idpacket*(LindaNetPacket.LEN_DATA-1);
				index = 1;
				for (int i=firstbyte ;i<firstbyte+(idatagram.getLength()-1);i++)
				{
					completBuf[i]=receivedBuf[index];
					index++;
				}
			}
			
			packet.decode (completBuf,0, completBuf.length);
			packet.set (idatagram.getAddress (), idatagram.getPort ());
		} catch (Exception e) { }

		return packet;
	}
}