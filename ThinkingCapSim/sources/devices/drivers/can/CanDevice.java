package devices.drivers.can;

public class CanDevice
{
	private String dev;

	static
	{
		System.loadLibrary("Can4Linux_lib");
	}

	public CanDevice (String _dev)
	{
		System.out.println("CanDevice version can4linux");
		this.dev = _dev;
		initialise ();
	}

	private native void initialise ();

	public native boolean canConfig (int bRate, int OCR,int CDR);

	/** Cuando se hace un reset se debe volver a configurar o no funcionara el CAN */
	public native boolean canReset ();

	public native void close ();

	/** Por compatibilidades se recibe un array de CanFrames
	 * pero el nuevo modulo del Can solo devuelve una trama CanFrame
	 * @return
	 */
	public CanFrame[] receiveFrames ()
	{
		byte canbytes[];
		byte candata[];
		int id, fi;
		CanFrame[] frame = new CanFrame[1];

		canbytes = receive ();

		if (canbytes.length == 0) return (null);

		//System.out.println ("----java DEBUG: received "+canbytes.length+" bytes -->"+canbytes[0]+" "+canbytes[1]+" "+canbytes[2]+" "+canbytes[3]+" "+canbytes[4]+" "+canbytes[5]+" "+canbytes[6]);

		id = canbytes[0] & 0x00FF;
		id = (id << 8) + (canbytes[1] & 0x00FF);
		id = (id << 8) + (canbytes[2] & 0x00FF);
		id = (id << 8) + (canbytes[3] & 0x00FF);

		fi = (canbytes[4] << 8);
		fi = (fi & 0xFF00) + canbytes[5];

		candata = new byte[canbytes[6]];
		System.arraycopy(canbytes,7,candata,0,canbytes[6]);
		frame[0] = new CanFrame (id, fi, candata);
		return (frame);
	}

	private native byte[] receive ();

	public void sendFrame (CanFrame frame)
	{
		byte canbytes[] = frame.toByteStream();

//		System.out.print ("----java debug: ");
//		for (int i=0; i < canbytes.length; i++)
//			System.out.print (Integer.toHexString(canbytes[i])+" ");
//		System.out.println ();

		send (canbytes);
	}


	/* Formato del los bytes a transmitir
	 * byte[0] flag
	 *
	 * Extended Flag (byte[0]&0x80 > 1)
	 * byte[1]-byte[4]    Extended ID(MSB-LSB)
	 * byte[5]--byte[...] data
	 *
	 * Normal Flag
	 * byte[1]-byte[2]   Normal ID(MSB-LSB)
	 * byte[2]-byte[...] data
	 * */
	private native void send (byte bytes[]);
}
