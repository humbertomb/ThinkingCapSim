package devices.drivers.beacon.nav200;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import wucore.utils.math.ByteUtil;
import devices.drivers.beacon.LaserBeaconException;

/** 
 * Send or received command in serial port with the BCC code 
 * 
 * @autor			Jose Antonio Marin Meseguer
 */
public class NAV200Datagram {

	protected final	byte	startByte	= 0x02; 		/** STX. Start bite of command */
	protected		byte	length;						// length of datagram
	protected		byte	mode;
	protected		byte	function;
	protected		byte[]	data;						// Data
	protected		byte	BCC;						// Block check (XOR of commad with STX)

	
	/** Default constructor */
	
	public NAV200Datagram() {
		this((byte)0,(byte)0);
	}
	
	public NAV200Datagram(char mode, char function) {
		this((byte)mode,(byte)function,new byte[0]);
	}
	public NAV200Datagram(byte mode, byte function) {
		this(mode,function,new byte[0]);
	}
	
	public NAV200Datagram(char mode, char function, byte[] data) {
		this((byte)mode,(byte)function,data);
	}
	public NAV200Datagram(byte mode, byte function, byte[] data) {
		this.mode = mode;
		this.function = function;
		setData(data);
	}
		
	public void setData(byte[] data){
		if(data == null) data = new byte[0];
		this.data = data;
		this.length = (byte)(data.length + 5); // stx + length + mode+ function data + BBC
		this.BCC = calculateBCC();
	}
			
	/** Send command (tramaEnv) in serial port */
	public void send(OutputStream  o) throws LaserBeaconException 
	{
		try{
			o.write(startByte);
			o.write(length);
			o.write(mode);
			o.write(function);
			if(data!=null && data.length>0)
				o.write(data);
			o.write(BCC);
		}catch(IOException e){
			e.printStackTrace();
			throw new LaserBeaconException("NAV200: No se ha podido escribir en el puerto");
		}
	}
	
	
	/** Read command received for NAV200 and check BCC */
	public void read(InputStream i) throws LaserBeaconException 
	{
	    int read;
	    int len;
	    long time = System.currentTimeMillis();
	    
	    try{
	        // Espera hasta recibir el STARTBYTE o salte el timeout
	        while( (read = i.read()) != startByte )
	            if(read == -1) 	throw new LaserBeaconException("NAV200: timeout reading startbyte"+". time="+(System.currentTimeMillis()-time)+"ms");
	            else	            	System.out.println("NAV200 error: Incorrect startbyte "+read);
	        
	        len = i.read();
	        if(read == -1) 	throw new LaserBeaconException("NAV200: timeout reading length"+". time="+(System.currentTimeMillis()-time)+"ms");
	        this.length = (byte)len;
	        len = len - 5;
	        
            read = i.read();
            if(read == -1) 	throw new LaserBeaconException("NAV200: timeout reading Mode"+". time="+(System.currentTimeMillis()-time)+"ms");
            this.mode = (byte)read;

            read = i.read();
            if(read == -1) 	throw new LaserBeaconException("NAV200: timeout reading Function"+". time="+(System.currentTimeMillis()-time)+"ms");
            this.function = (byte)read;
            
	        this.data = new byte[len];
	        
	        for(int j = 0; j<len; j++){
	            read = i.read();
	            if(read == -1) 	throw new LaserBeaconException("NAV200: timeout reading data("+j+")"+". time="+(System.currentTimeMillis()-time)+"ms");
	            data[j] = (byte)read;
	        }
	        
            read = i.read();
            if(read == -1) 	throw new LaserBeaconException("NAV200: timeout reading BCC"+". time="+(System.currentTimeMillis()-time)+"ms");
            this.BCC = (byte)read;
	        
            checkBCC();
            	        
	    }catch(IOException e){
	    	e.printStackTrace();
	    }
	}
	 
	
	public String toString(){
		String ret = "Command: "+(char)mode+(char)function+"\nLength: "+length+"\nData:{ ";
		if(data!=null)
			for(int i=0; i<data.length; i++){
				ret += Integer.toHexString(data[i])+"h ";
			}
		ret += "}\nChecksum: "+Integer.toHexString(BCC)+"h";
		return ret;
	}
	
	// Tools
	
	/** BCC generate */
	
	// Calcula el Checksum del datagrama
	protected byte calculateBCC() 	{
		int BCC = startByte ^ length ^ mode ^ function;
		for(int i=0;i<data.length;i++) 		
			BCC = BCC ^ data[i];
		return (byte)BCC;		
	}
	
	// Comprueba que el Checksum del datagrama es Correcto
	public void checkBCC() throws LaserBeaconException{
		if(calculateBCC() != this.BCC) throw new LaserBeaconException("NAV200: Incorrect BBC");
	}

	



//	Conversion bytes to Position and Oriention
	
	public double[] getPosition(){
		double[] position= new double[5];
		position[0] = Byte2Pos(data[0],data[1],data[2],data[3]);  	//pos x(meter)
		position[1] = Byte2Pos(data[4],data[5],data[6],data[7]);	//pos y(meter)
		position[2] = Byte2Angle(data[8],data[9]);					//angle (rad)
		position[3] = data[10];										//Quality(0 - 100) 																					//					 el numero de balizas necesarias)
		position[4] = data[11];										//Reflector used
		return position;
	}

	/** Converted the 4bytes(INT32) of position a meter (LSB = b0) */
	public static double Byte2Pos (byte b0, byte b1, byte b2,byte b3){
	    return((double)ByteUtil.BYTEtoINT32(b0,b1,b2,b3) / 1000.0); 
	}

	/** Converted the position meter to 4bytes (INT32). LSB = b[0], MSB = b[3] */
	public static byte[] Pos2Byte (double pos){
	    return ByteUtil.INT32toBYTE((int)Math.round(pos*1000));
	}


	/** Converted the 2bytes (INT16) of velocity to meter/second  (LSB = b0)*/
	public static double Byte2Vel (byte b0, byte b1){
	    return((double)ByteUtil.BYTEtoINT16(b0,b1) / 1000.0); 
	}
    
    
	/* Converted velocity in meter/second to 2bytes */
	public static byte[] Vel2Byte (double vel){
	    return ByteUtil.INT16toBYTE((short)Math.round(vel*1000));
	}
    


	/** Converted angles in radian to 2bytes */
	public static byte[] Angle2Byte (double ang){
	    //	  0º => 0, 90º => 0x4000, 180º => 0x8000, -90º => 0xC000
	    return ByteUtil.INT16toBYTE((short)Math.round(ang * 0x8000 / Math.PI));
	}


	/** Converted 2bytes of angle to radian */
	public static double Byte2Angle (byte b0, byte b1){
	    // 0º => 0, 90º => 0x4000, 180º => 0x8000, -90º => 0xC000
		return ByteUtil.BYTEtoINT16(b0,b1) * Math.PI / 0x8000;
	}
	
	   // compare received command 

	public boolean isEquals(NAV200Datagram datagram2){
		if(this==null || datagram2==null) return false;

		if(length != datagram2.length) 	return false;
		if(mode != datagram2.mode) 		return false;
		if(function != datagram2.function) 		return false;
		if(BCC != datagram2.BCC) 		return false;
		
		for(int i = 0; i< length-1; i++)
			if (data[i] != datagram2.data[i]) 	return false;	
		
		return (true);
	}
	
	public boolean isEquals(NAV200Datagram datagram2, int n){
		if(this==null || datagram2==null) return false;
		if(mode != datagram2.mode) 		return false;
		if(function != datagram2.function) 		return false;
		
		if(data.length<n || datagram2.data.length<n) return false;
		
		for(int i = 0; i < n; i++)
			if (data[i] != datagram2.data[i]) 	return false;
	    return true;
	}
	
	
}

