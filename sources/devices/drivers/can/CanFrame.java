/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:
 * @author		Juan Pedro Canovas, Jose Antonio Marin
 * @version 1.0		PC104-CAN
 */

package devices.drivers.can;

public class CanFrame
{
	private int id;
	private byte[] datos;
	
	// 	FLAG PARA EL NUEVO MODULO
	// MSG_RTR         (1<<0)          
	// MSG_OVR         (1<<1)          
	// MSG_EXT         (1<<2)          
	// MSG_PASSIVE     (1<<4)          
	// MSG_BUSOFF      (1<<5)          
	// MSG_            (1<<6)          
	// MSG_BOVR        (1<<7)          
	
	private byte fi;
	private long tiempo;
	
	public final static int CAN0 = 0;
	public final static int CAN1 = 1;	
		
	public int getID() {return(this.id);}
	
	public byte[] getData() {return(this.datos);}

	public int sizeDatos() {return(datos.length);}

	public long getTiempo () {return this.tiempo;}
	
	public CanFrame()
	{
		this.id = 0;
		this.fi = 0;
		this.datos = null;
	}
	
	public CanFrame(int _id,int _fi,byte[] _datos)
	{
		this.id = _id;
		this.fi = (byte)_fi;
		this.datos = _datos;
	}
	
	public CanFrame(int _id,int _fi,byte[] _datos,long _time)
	{
		this.id = _id;
		this.fi = (byte)_fi;
		this.datos = _datos;
		this.tiempo = _time;
	}
	
	public CanFrame(int _id,boolean eff,boolean rtr, byte[] _datos)
	{
		byte cabecera = 0; 
		
		if(eff) cabecera += (byte)(1<<2);
		if(rtr) cabecera += (byte)(1<<0);
			
		this.id = _id;
		this.datos = _datos;
		//this.fi = (byte)(cabecera + _datos.length); Cambiado para el nuevo modulo 
		this.fi = (byte)(cabecera);	
	}
	
	public byte[] toByteStream()
	{
		int size = sizeDatos()+1;
		int i;
		byte[] mensaje = null;
		if(fi!=0) System.out.println("fi = "+fi);
		if (isEFF())
		{
			mensaje = new byte[size+4];
			mensaje[0] = this.fi;
			mensaje[1] = (byte) ((id>>24) & 0xFF);
			mensaje[2] = (byte) ((id>>16) & 0xFF);
			mensaje[3] = (byte) ((id>>8) & 0xFF);
			mensaje[4] = (byte) (id & 0xFF);
			size = 5;
		}
		else
		{
			mensaje = new byte[size+2];
			mensaje[0] = this.fi;
			mensaje[1] = (byte) ((id>>8) & 0xFF);
			mensaje[2] = (byte) (id & 0xFF);
			size = 3;
		}
		
		for(i=0;i<sizeDatos();i++)
			mensaje[size+i] = this.datos[i];
			
		return(mensaje);	
	}
	
	public boolean isEFF()
	{
		if((this.fi & (1<<2)) > 1) return(true);
		else return (false);
	}
	
	public boolean isRTR()
	{
		if((this.fi & (1<<0)) > 1) return(true);
		else return (false);
	}
	
	public void setData(byte[] _datos)
	{
		this.datos = _datos;
		//this.fi = (byte) ((this.fi & 0xF0) + _datos.length); Cambiado para el nuevo modulo
		this.fi = (byte) ((this.fi & 0xF0));
	}
	
	public void setID(int _id) {this.id = _id;}
	
	public void setTiempo(long _time) {this.tiempo = _time;}
		
	public void setFI(boolean EFF, boolean RTR)
	{
		byte cabecera = 0; 
		
		if(EFF) cabecera += (byte)(1<<2);
		if(RTR) cabecera += (byte)(1<<0);

		//this.fi = (byte)(cabecera + (byte)sizeDatos()); Cambiado para el nuevo modulo
		this.fi = (byte)(cabecera);
	}
	
	public synchronized void dumpFrame()
	{
		int i;
		byte[] datos = null;

		System.out.print("ID-> "+this.id);
		if(isRTR()) 
			System.out.print("\tR ");
		if(isEFF()) 
			System.out.print("\tE ");
		System.out.print("\tDATOS: "+sizeDatos()+"\n");
		datos = getData();
		for(i=0;(i<sizeDatos()) && (i < datos.length);i++)
			System.out.println("Dato "+i+":\t"+datos[i]);
		System.out.println("==============================================");
	}
}
