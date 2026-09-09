package wucore.utils.math;

/**
 * 
 * Utilidades para la conversion de Enteros a bytes y viceversa.
 * 
 * @author Jose Antonio Marin
 *(0 a 4294967295) (0 a 65536) (-2147483648 a 2147483647) (-32768 a 32767)
 */
public class ByteUtil {
	// Byte utilities Menor Peso = b0 //
    
    
    /** Construye un Entero con signo (32bits) a partir de sus 4 bytes
     * 
     * @param b Bytes para generar el Entero (LSB = b[0], MSB = b[3])
     * @return Entero generado (-2147483648 a 2147483647)
     */
	static public int BYTEtoINT32(byte[] b){
		return BYTEtoINT32(b[0], b[1], b[2], b[3]);
	}
	
    /** Construye un Entero con signo (32bits) a partir de sus 4 bytes
     * 
     * @param b0 Byte menos significativo (LSB)
     * @param b1 Byte intermedio
     * @param b2 Byte intermedio
     * @param b3 Byte mas significativo (MSB)
     * @return Entero generado (-2147483648 a 2147483647)
     */
	static public int BYTEtoINT32(byte b0, byte b1, byte b2, byte b3){
		return
			( ((int)b3&0xFF) << 24) |
			( ((int)b2&0xFF) << 16) |
			( ((int)b1&0xFF) << 8)  |
			( (int)b0&0xFF );
	}
	
	static public long BYTEtoINT64(byte[] b){
		return
			( ((long)b[7]&0xFF) << 56) |
			( ((long)b[6]&0xFF) << 48) |
			( ((long)b[5]&0xFF) << 40) |
			( ((long)b[4]&0xFF) << 32) |
			( ((long)b[3]&0xFF) << 24) |
			( ((long)b[2]&0xFF) << 16) |
			( ((long)b[1]&0xFF) << 8)  |
			( (long)b[0]&0xFF );
	}	
	
		
    /** Construye un Entero sin signo (32bits) a partir de sus 2 bytes
     * 
     * @param b Bytes para generar el Entero (LSB = b[0], MSB = b[3])
     * @return Entero generado (0 a 4294967295)
     */
	static public long BYTEtoUINT32(byte[] b){
		return BYTEtoUINT32(b[0], b[1], b[2], b[3]);
	}
	
    /** Construye un Entero sin signo (32bits) a partir de sus 2 bytes
     * 
     * @param b0 Byte menos significativo (LSB)
     * @param b1 Byte intermedio
     * @param b2 Byte intermedio
     * @param b3 Byte mas significativo (MSB)
     * @return Entero generado (0 a 4294967295)
     */
	static public long BYTEtoUINT32(byte b0, byte b1, byte b2, byte b3){
		return
			( ((long)b3&0xFF) << 24) |
			( ((long)b2&0xFF) << 16) |
			( ((long)b1&0xFF) << 8)  |
			( (long)b0&0xFF );
	}

	
	
	
	
	
    /** Construye un Entero con signo (16bits) a partir de sus 2 bytes
     * 
     * @param b Bytes para generar el Entero (LSB = b[0], MSB = b[1])
     * @return Entero generado (-32768 a 32767)
     */
	static public short BYTEtoINT16(byte[] b){
		return BYTEtoINT16(b[0], b[1]);
	}
	
    /** Construye un Entero con signo (16bits) a partir de sus 2 bytes
     * 
     * @param b0 Byte menos significativo (LSB)
     * @param b1 Byte mas significativo (MSB)
     * @return Entero generado (-32768 a 32767)
     */
	static public short BYTEtoINT16(byte b0, byte b1){
		return (short)(
			( ((short)b1&0xFF) << 8)  |
			( (short)b0&0xFF ));
	}

	
	
	
	
	
    /** Construye un Entero sin signo (16bits) a partir de sus 2 bytes
     * 
     * @param b Bytes para generar el Entero (LSB = b[0], MSB = b[1])
     * @return Entero generado (0 a 65536)
     */
	static public int BYTEtoUINT16(byte[] b){
		return BYTEtoUINT16(b[0], b[1]);
	}
	
    /** Construye un Entero sin signo (16bits) a partir de sus 2 bytes
     * 
     * @param b0 Byte menos significativo (LSB)
     * @param b1 Byte mas significativo (MSB)
     * @return Entero generado (0 a 65536)
     */
	static public int BYTEtoUINT16(byte b0, byte b1){
		return (int)(
			( ((int)b1&0xFF) << 8)  |
			( (int)b0&0xFF ));
	}
	
	
	
	
	
    /** Devuelve los 4 bytes que representan el Entero con signo de 32bits
     * 
     * @param i Entero de 32bits con signo (-2147483648 a 2147483647)
     * @return Array de 4 bytes que forman el entero (LSB = b[0], MSB = b[3])
     */
	static public byte[] INT32toBYTE(int i){
		byte[] bytes = new byte[4];
		bytes[3] = (byte)((i>>24) & 0xFF);
		bytes[2] = (byte)((i>>16) & 0xFF);
		bytes[1] = (byte)((i>>8) & 0xFF);
		bytes[0] = (byte)(i & 0xFF);
		return bytes;
	}
	
    /** Devuelve los 8 bytes que representan el Entero con signo de 64bits
     * 
     * @param i Entero de 64bits con signo (-9223372036854775808 a 9223372036854775807)
     * @return Array de 8 bytes que forman el entero (LSB = b[0], MSB = b[7])
     */
	static public byte[] INT64toBYTE(long i){
		byte[] bytes = new byte[8];
		bytes[7] = (byte)((i>>56) & 0xFF);
		bytes[6] = (byte)((i>>48) & 0xFF);
		bytes[5] = (byte)((i>>40) & 0xFF);
		bytes[4] = (byte)((i>>32) & 0xFF);
		bytes[3] = (byte)((i>>24) & 0xFF);
		bytes[2] = (byte)((i>>16) & 0xFF);
		bytes[1] = (byte)((i>>8) & 0xFF);
		bytes[0] = (byte)(i & 0xFF);
		return bytes;
	}
	
    /** Devuelve los 4 bytes que representan el Entero sin signo de 32bits
     * 
     * @param l Entero de 32bits sin signo (0 a 4294967295)
     * @return Array de 4 bytes que forman el entero (LSB = b[0], MSB = b[3])
     */
	static public byte[] UINT32toBYTE(long l){
		byte[] bytes = new byte[4];
		bytes[3] = (byte)((l>>24) & 0xFF);
		bytes[2] = (byte)((l>>16) & 0xFF);
		bytes[1] = (byte)((l>>8) & 0xFF);
		bytes[0] = (byte)(l & 0xFF);
		return bytes;
	}

	
	
	
	
	
    /** Devuelve los 2 bytes que representan el Entero con signo de 16bits
     * 
     * @param i Entero de 16bits con signo (-32768 a 32767)
     * @return Array de 2 bytes que forman el entero (LSB = b[0], MSB = b[1])
     */
	static public byte[] INT16toBYTE(short i){
		byte[] bytes = new byte[2];
		bytes[1] = (byte)((i & 0xFF00) >> 8);
		bytes[0] = (byte)((i & 0x00FF) 	);
		return bytes;
	}
	
    /** Devuelve los 2 bytes que representan el Entero sin signo de 16bits
     * 
     * @param i Entero de 16bits sin signo (0 a 65536)
     * @return Array de 2 bytes que forman el entero (LSB = b[0], MSB = b[1])
     */
	static public byte[] UINT16toBYTE(int i){
		byte[] bytes = new byte[2];
		bytes[1] = (byte)((i & 0xFF00) >> 8);
		bytes[0] = (byte)((i & 0x00FF) 	);
		return bytes;
	}
	
    /** Transforma un float a 4 bytes (segun norma IEEE 754)
     * 
     * @param f Valor float para convertir
     * @return Array de 4 bytes que forman el float (LSB = b[0], MSB = b[3])
     */
	static public byte[] FLOATtoBYTE(float f){
		int i = Float.floatToIntBits(f);
		return INT32toBYTE(i);
	}
	
    /** Transforma un array de 4 bytes a float (segun norma IEEE 754)
     * 
     * @param b Array de 4 bytes con la representacion del float
     * @return float equivalente.
     */
	static public float BYTEtoFLOAT(byte[] b){
		int i = BYTEtoINT32(b);
		return Float.intBitsToFloat(i);
	}
	
	
    /** Transforma un double a 8 bytes (segun norma IEEE 754)
     * 
     * @param d Valor double para convertir
     * @return Array de 8 bytes que forman el double (LSB = b[0], MSB = b[1])
     */
	static public byte[] DOUBLEtoBYTE(double d){
		long l = Double.doubleToLongBits(d);
		return INT64toBYTE(l);
	}
	
    /** Transforma un array de 8 bytes a double (segun norma IEEE 754)
     * 
     * @param b Array de 8 bytes con la representacion del float
     * @return valor del double equivalente
     */
	static public double BYTEtoDOUBLE(byte[] b){
		long l = BYTEtoINT64(b);
		return Double.longBitsToDouble(l);
	}
	
    /** Representacion hexadecimal de un array de bytes
     * 
     * @param b Array de bytes
     * @return Cadena con la representacion del arrat de bytes en Hexadecimal
     */
	static public String toHexString(byte[] b){
		String ret = "";
	    for(int i = 0; i<b.length; i++){
		    ret += toHexString(b[i])+"h ";
		}
	    return ret;
	}
	
	/** Representacion hexadecimal de un byte
     * 
     * @param b byte
     * @return Cadena con la representacion del byte en Hexadecimal
     */
	static public String toHexString(byte b){
		return Integer.toHexString(((int)b)|0xFFFFFF00).substring(6).toUpperCase();
	}
	
	static public void main(String[] argv){
	    double d = 3756.1234;
	    byte[] b1 = DOUBLEtoBYTE(d);
	    byte[] b2 = FLOATtoBYTE((float)d);
	    
	    System.out.println("d = "+d);
	    System.out.println("b1 = "+toHexString(b1)+" => "+BYTEtoDOUBLE(b1));
	    System.out.println("f = "+(float)d);
	    System.out.println("b2 = "+toHexString(b2)+" => "+BYTEtoFLOAT(b2));
	    
	}
}
