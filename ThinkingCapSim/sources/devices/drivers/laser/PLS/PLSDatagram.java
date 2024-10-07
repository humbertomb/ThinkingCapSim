package devices.drivers.laser.PLS;

import java.io.*;

import devices.drivers.laser.*;

public class PLSDatagram implements Serializable
{
	// LASER COMMANDS TO LMS2XX - RESPONSE FROM LMS2XX used for communications
	public static byte CMD_INITIALISE_AND_RESET								= (byte) 0x10;
	public static byte CMD_CHOOSE_OPERATING_MODE								= (byte) 0x20;
	public static byte CMD_REQUEST_MEASURED_VALUES								= (byte) 0x30;
	public static byte CMD_REQUEST_LMS_STATUS									= (byte) 0x31;
	public static byte CMD_REQUEST_ERROR_TEST_MESSAGE							= (byte) 0x32;
	public static byte CMD_REQUEST_OPERATING_DATA_COUNTER						= (byte) 0x35;
	public static byte CMD_REQUEST_MEAN_MEASURED_VALUES						= (byte) 0x36;
	public static byte CMD_REQUEST_MEASURED_VALUES_SUB_RANGE					= (byte) 0x37;
	public static byte CMD_REQUEST_LMS_TYPE									= (byte) 0x3A;
	public static byte CMD_SWITCH_VARIANT_IN_LMS2XX							= (byte) 0x3B;
	public static byte CMD_REQUEST_MEASURED_VALUES_WITH_FIELD_VALUES 			= (byte) 0x3E;
	public static byte CMD_REQUEST_MEAN_MEASURED_VALUES_SUB_RANGE 				= (byte) 0x3F;
	public static byte CMD_CONFIGURE_FIELDS									= (byte) 0x40; // A, B OR C
	public static byte CMD_SWITCH_ACTIVE_FIELD_SET								= (byte) 0x41;
	public static byte CMD_CHANGE_PASSWORD										= (byte) 0x42;
	public static byte CMD_REQUEST_MEASURED_VALUES_AND_REFLECTIVITY_SUB_RANGE	= (byte) 0x44;
	public static byte CMD_REQUEST_CONFIGURED_FIELDS							= (byte) 0x45;
	public static byte CMD_START_TEACH_MODE_FOR_FIELD_CONFIGURATION 			= (byte) 0x46;
	public static byte CMD_REQUEST_STATUS_OF_THE_FIELD_OUTPUTS					= (byte) 0x4A;
	public static byte CMD_DEFINE_BAUD_RATE_OR_LMS_TYPE						= (byte) 0x66;
	public static byte CMD_ANGULAR_RANGE_FOR_POSITIONING_AID					= (byte) 0x69;
	public static byte CMD_REQUEST_LMS_CONFIGURATION							= (byte) 0x74;
	public static byte CMD_REQUEST_MEASURED_VALUE_WITH_REFLECTIVITY_DATA 		= (byte) 0x75;
	public static byte CMD_REQUEST_MEASURED_VALUE_IN_CARTESIAN_COORDINATES 		= (byte) 0x76;
	public static byte CMD_CONFIGURE_LMS2XX									= (byte) 0x77;
	public static byte CMD_CONFIGURE_LMS2XX_CONTINUED							= (byte) 0x7C;
	
	public static byte RESP_INITIALISE_AND_RESET								= (byte) 0x90;
	public static byte RESP_CHOOSE_OPERATING_MODE								= (byte) 0xA0;
	public static byte RESP_REQUEST_MEASURED_VALUES							= (byte) 0xB0;
	public static byte RESP_REQUEST_LMS_STATUS									= (byte) 0xB1;
	public static byte RESP_REQUEST_ERROR_TEST_MESSAGE							= (byte) 0xB2;
	public static byte RESP_REQUEST_OPERATING_DATA_COUNTER						= (byte) 0xB5;
	public static byte RESP_REQUEST_MEAN_MEASURED_VALUES						= (byte) 0xB6;
	public static byte RESP_REQUEST_MEASURED_VALUES_SUB_RANGE					= (byte) 0xB7;
	public static byte RESP_REQUEST_LMS_TYPE									= (byte) 0xBA;
	public static byte RESP_SWITCH_VARIANT_IN_LMS2XX							= (byte) 0xBB;
	public static byte RESP_REQUEST_MEASURED_VALUES_WITH_FIELD_VALUES 			= (byte) 0xBE;
	public static byte RESP_REQUEST_MEAN_MEASURED_VALUES_SUB_RANGE 				= (byte) 0xBF;
	public static byte RESP_CONFIGURE_FIELDS									= (byte) 0xC0; // A, B OR C
	public static byte RESP_SWITCH_ACTIVE_FIELD_SET							= (byte) 0xC1;
	public static byte RESP_CHANGE_PASSWORD									= (byte) 0xC2;
	public static byte RESP_REQUEST_MEASURED_VALUES_AND_REFLECTIVITY_SUB_RANGE	= (byte) 0xC4;
	public static byte RESP_REQUEST_CONFIGURED_FIELDS							= (byte) 0xC5;
	public static byte RESP_START_TEACH_MODE_FOR_FIELD_CONFIGURATION 			= (byte) 0xC6;
	public static byte RESP_REQUEST_STATUS_OF_THE_FIELD_OUTPUTS					= (byte) 0xCA;
	public static byte RESP_DEFINE_BAUD_RATE_OR_LMS_TYPE						= (byte) 0xE6;
	public static byte RESP_ANGULAR_RANGE_FOR_POSITIONING_AID					= (byte) 0xE9;
	public static byte RESP_REQUEST_LMS_CONFIGURATION							= (byte) 0xF4;
	public static byte RESP_REQUEST_MEASURED_VALUE_WITH_REFLECTIVITY_DATA  		= (byte) 0xF5;
	public static byte RESP_REQUEST_MEASURED_VALUE_IN_CARTESIAN_COORDINATES 		= (byte) 0xF6;
	public static byte RESP_CONFIGURE_LMS2XX									= (byte) 0xF7;
	public static byte RESP_CONFIGURE_LMS2XX_CONTINUED							= (byte) 0xFC;
	
	public static final byte 		STX 				= (byte) 0x02;	// Start byte
	public static final byte 		ACK 				= (byte) 0x06;	// Acknowledge (correct mnemonic, i.e. correct telegram sent)
	public static final byte 		NACK 			= (byte) 0x15;	// Not Acknowledge (incorrect mnemonic, i.e. incorrect telegram sent)
	public static final byte 		ADDRBROADCAST	= (byte) 0x00;	// Addresses for communication with laser
	public static final int		CRC16_POL_GEN	= 0x8005;
	public static final int		ACK_DELAY		= 60; 			// Time to wait for the ACK (ms)
	public static final int		MAXDATA			= 2048;			// Maximum number of bytes in a telegram
	
	protected InputStream			istream;	
	protected OutputStream		ostream;
	
	protected byte				address;							/** Address. Byte de direcci�n de la trama. */
	protected short				length;							/** Length. Longitud de la trama. Incluye el tama�o del campo de comando y el de datos. */
	protected byte				comando;							/** CMD. Byte de comando de la trama del HOST->LMS200. Cuando LMS200 responde a HOST a�ade 0x80 a la orden original CMD. */
	protected byte[]				data;							/** Array de bytes conteniendo los datos de la trama (Es opcional). */
	protected short				checkSum;						/** CRC16. */
	
	private double[]				ranges;
	private byte[]				data_t			= new byte[MAXDATA];
	
	//Constructores.	
	public PLSDatagram (InputStream istream, OutputStream ostream)
	{
		this.istream = istream;
		this.ostream = ostream;
	}
	
	public static short makeshort(byte low, byte high) {
		short value;	
		value = (short )(((short )(low & 0x00FF)) | (((short )(high << 8)) & 0xFF00));
		return (value);
	}
	
	public static byte lowbyte(short value) {
		return((byte )(value & 0x00FF));
	}
	
	public static byte hibyte(short value) {
		return((byte )((value & 0xFF00) >> 8));
	}
	
	// Metodos para ver valores.
	public byte getAddress() {
		return(this.address);
	}
	
	public short getLength() {
		return(this.length);
	}
	
	public byte getCmd() {
		return(this.comando);
	}
	
	public byte[] getData() {
		return(this.data);
	}
	
	public short getCheckSum() {
		return(this.checkSum);
	}
	
	public byte getByte(int position) {		//Coge uno de los data recibidos.
		return(this.data[position]);
	}
	
	public void createTelegram (byte addr, byte command, byte[] payload)
	{
		address	= addr;
		length	= (short)(payload.length + 1);
		comando	= command;
		data		= payload;
		checkSum	= calculateCheckSum();
	}
	
	/** Envia y espera el ACK */
	public void sendTelegram () throws LaserException, IOException
	{
		// Send telegram
		ostream.write (STX);
		ostream.write (address);
		ostream.write (lowbyte (length));
		ostream.write (hibyte (length));
		ostream.write (comando);
		ostream.write (data);
		ostream.write (lowbyte (checkSum));	
		ostream.write (hibyte (checkSum));
		//ostream.flush();
		
		// Give the laser some time to process the command
		try { Thread.sleep (ACK_DELAY); }catch (Exception e) { }
		
		// Read ACK
		byte			read;
		
		if (istream.available() == 0) throw new LaserException("ACK timeout");
		read = (byte) istream.read();
		if (read != ACK)
		{
			// Clear input buffer
			istream.skip (istream.available());
			throw new LaserException("ACK wrong data ("+read+")");
		}
	}
	
	/** Envia y espera el ACK */
	public void sendTelegram (int retries) throws LaserException, IOException
	{
		while (retries > 0)
		{
			try
			{
				sendTelegram ();
				return;
			} catch (LaserException e) { retries--; }
		}
		
		throw new LaserException("Max. retries exceded");
	}
	
	/** Lee un datagrama generico del Laser (sin ACK)*/
	public void readTelegram () throws LaserException
	{
		int readbyte;
		int readbyte1;
		
		try
		{
			// Espera hasta recibir el STARTBYTE o salte el timeout
			while ((readbyte = istream.read()) != STX)
				if(readbyte == -1)
					throw new LaserException("PLS timeout reading startbyte");
			
			address = (byte) istream.read();
			if(address == -1) 	
				throw new LaserException ("PLS timeout reading address");
			
			readbyte = istream.read();
			if(readbyte == -1) 	throw new LaserException("PLS timeout reading length1");
			readbyte1 = istream.read();
			if(readbyte1 == -1) 	throw new LaserException("PLS timeout reading length2");
			length = makeshort ((byte)readbyte, (byte) readbyte1);
			
			comando = (byte)istream.read();
			if(comando == -1) 	throw new LaserException("PLS timeout reading command");
			
			data = new byte[this.length - 1];
			for (int j = 0; j < data.length; j++){
				readbyte = istream.read();
				if(readbyte == -1) 	throw new LaserException("PLS timeout reading data");
				data[j] = (byte)readbyte;
			}
			
			readbyte = istream.read();
			if(readbyte == -1) 	throw new LaserException("PLS timeout reading checksum1");
			readbyte1 = istream.read();
			if(readbyte1 == -1) 	throw new LaserException("PLS timeout reading checksum2");
			checkSum =	makeshort((byte) readbyte, (byte) readbyte1);
		}
		catch (Exception e) { e.printStackTrace (); }
		if (checkSum != calculateCheckSum ()) throw new LaserException("PLS checksum error");
	}
	
	// Compute CRC16 checksum
	protected short calculateCheckSum() 
	{
		int			i; 
		short		temp;
		short		uCRC16;
		int			count = 4 + length;
		byte[]		abData = new byte[2];
		
		data_t[0] = STX;
		data_t[1] = address;
		data_t[2] = lowbyte(length);
		data_t[3] = hibyte(length);
		data_t[4] = comando;
		if(data != null && data.length>0)
			System.arraycopy(data,0,data_t,5,data.length);
		
		i = 0;
		uCRC16 = 0;
		abData[0] =  0;
		while (count > 0)
		{	
			count--;
			abData[1] = abData[0];
			abData[0] = data_t[i];
			i++;
			if ((uCRC16 & 0x8000) != 0) {
				uCRC16	= (short)((uCRC16 & 0x7FFF) << 1); 
				uCRC16  = (short)((uCRC16 & 0xFFFF) ^ CRC16_POL_GEN);
			} else
				uCRC16 <<= 1;
			temp = (short)((abData[0] & 0xFF) | ((abData[1] << 8) & 0xFF00));
			uCRC16 ^= temp;	
		}
		return uCRC16;		
	}
	
	// Convierte los Datos a medidas de rango del laser
	protected double[] toRange()
	{	
		int			len;
		
		if (data == null)			return null;
		
		len = (data.length-2)/2;
		if ((ranges == null) || (ranges.length != len))
			ranges = new double[len];
		
		for (int i = 0; i < ranges.length; i++)
			ranges[i] = (double)((makeshort((byte)data[2*i+2], (byte)data[2*i+3]))&0x3FFF) * 0.01;
		
		return ranges;
	}
	
	public String toString(){
		String datagram = "DATAGRAM: ADDR["+(Integer.toHexString(0x80000000 | address).substring(8-2).toUpperCase())
		+ "h] LENGH["+length+"] COMMAND["+(Integer.toHexString(0x80000000 | comando).substring(8-2).toUpperCase())+"h]\n"+
		"DATA[ ";
		if(data!=null){
			for(int i=0;i<data.length; i++)
				datagram += (Integer.toHexString(0x80000000 | data[i]).substring(8-2).toUpperCase())+"h ";
		}
		datagram += "]\nCHECKSUM["+(Integer.toHexString(0x80000000 | checkSum).substring(8-4).toUpperCase())+"h]\n";
		return datagram;
		
	}
}

