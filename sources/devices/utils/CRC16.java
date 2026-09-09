/**
 * Copyright: Copyright (c) 2002
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Canovas Quionero (juanpe@dif.um.es)
 * @version 1.0
 */
 
package devices.utils;

public class CRC16
{
  public static final int POL = 0x8005;

  public static short calculate (byte[] data)
  {
    int length = data.length;
		short uCRC16;
		byte[] abData = new byte[2];
		int i = 0;
    
		uCRC16 = 0;
		abData[0] =  0;

    for (i=0; i < length; i++)
    {
      abData[1] = abData[0];
      abData[0] = data[i];
      if ((uCRC16 & 0x8000) != 0)
      {
				uCRC16	= (short)((uCRC16 & 0x7FFF) << 1);
				uCRC16  = (short)((uCRC16 & 0xFFFF) ^ POL);
			}
      else uCRC16 <<= 1;
      
      uCRC16 ^= (short)((abData[0] & 0xFF) | ((abData[1] << 8) & 0xFF00));
    }
    return (uCRC16);
    
		/*while (length > 0)
    {
			length--;
			abData[1] = abData[0];
			abData[0] = data[i];
			i++;
			if ((uCRC16 & 0x8000) != 0)
      {
				uCRC16	= (short )((uCRC16 & 0x7FFF) << 1);
				uCRC16  = (short )((uCRC16 & 0xFFFF) ^ CRC16_POL_GEN);
			} else
      {
				uCRC16 <<= 1;
			}
			temp = (short )((abData[0] & 0xFF) | ((abData[1] << 8) & 0xFF00));
			uCRC16 ^= temp;
		}
		return(uCRC16);*/ 
  }

  public static boolean check (byte data[], short crc)
  {
    return (crc == calculate (data)); 
  }
}
