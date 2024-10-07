

package devices.drivers.beacon.nav200;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;

import devices.drivers.beacon.LaserBeacon;
import devices.drivers.beacon.LaserBeaconException;

/** 
 * Driver for Positioning System NAV200 
 * 
 * @autor			Jose Antonio Marin Meseguer
 */

public class NAV200 extends LaserBeacon
{

	protected InputStream	inputS		= null;					/** InputStream used to receive information from sensor*/
	protected OutputStream	outputS		= null;					/** OutputStream used to send information from sensor. */
	
	protected SerialPort	port		= null;					/** Serial Port comunication. */
	
	int length = 0;
	
	boolean	debugError 				= 		true;				// Debugging error
	boolean debugAll 				= 		false;				// Debuggin All

	static public final int			TIMEOUT 	= 10000;
	
	/** Default constructor
	*
	*/
	public NAV200 () throws LaserBeaconException 
	{
		super();	
	}
	
	/** Change debug of the transmition
	*
	*/
	public void debugAll(boolean debug){
	debugAll = debug;
	}
	
	
	/** Change debug for error
	*
	*/
	public void debugError(boolean debug){
	debugError = debug;
	}
	
	
	/** Initialized Serial Port
	*	Open serial port with 19200 baud, 8 data bits , 1 start byte, 1 stop byte, parity even
	*
	*	@param	param	Serial port name (COM1,COM2,/dev/ttyS0,/dev/ttyS1,...)
	*
	*/
	public void initialise (String param) throws LaserBeaconException
	{
		CommPortIdentifier identifier;
		
		System.out.println("NAV200.initialise: Triying "+param+"...");
		try 
		{
			identifier = CommPortIdentifier.getPortIdentifier(param);
			System.out.println("Identificador del puerto obtenido");
			if (identifier.getPortType() != CommPortIdentifier.PORT_SERIAL) 
				throw (new LaserBeaconException ("It isn't a serial port!!!"));
			
			port = (SerialPort )identifier.open("NAV200", 1000);			
			port.setSerialPortParams(19200,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_EVEN);
			port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			port.enableReceiveTimeout(TIMEOUT);	// Activado timeout

			inputS = port.getInputStream();
			outputS = port.getOutputStream();
			clearBuffer();
		}
		catch (NoSuchPortException e)	
		{
			System.out.println("No such serial port"); 
			throw new LaserBeaconException(e.getMessage());
		}
		catch (PortInUseException e)	
		{
			System.out.println("Error opening serial port"); 
			throw new LaserBeaconException(e.getMessage());
		}
		catch (UnsupportedCommOperationException e)	
		{
			System.out.println("Unsuport command operation");	
			throw new LaserBeaconException(e.getMessage());
		}
		catch (IOException e)	
		{
			System.out.println(" I/O stream error"); 
			throw (new LaserBeaconException(e.getMessage()));
		}
		catch (Exception e) 
		{
			port.close(); 
			throw (new LaserBeaconException(e.getMessage()));
		}
		
		System.out.println("NAV200.initialise: "+param+" OK.");
	}
	
	////////////////////  P O S I T I O N   M O D E /////////////////////////////	


	/** Activate Positioning Mode (PA command). In this mode, the measuremen of the reflectors is evalued.
	*   and the position an orientation in the layer is determined. The layer can be chosen during positioning mode 
	*
	* 	@return	true if positioning mode has been activated.
	*/
	public boolean activatePos () throws LaserBeaconException {
				
		boolean ok = false;
		NAV200Datagram datagram = new NAV200Datagram('P','A');
		NAV200Datagram dtrequest = new NAV200Datagram('P','A');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Activate Positioning Mode (PA) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println("Position Activated");
		
		return(ok);
		
	}
	


	/**  Activate Positioning Mode (PN command). Activate positioning mode with depth of smoothing input
	*	
	*
	*	@param	number	Number of measurement for sliding mean (default setting = 4)
	*
	* 	@return	true if positioning mode has been activated.
	*/
	public boolean activatePos (int number) throws LaserBeaconException {
		
		boolean ok = false;
		byte[] data = {(byte)number};
		NAV200Datagram datagram = new NAV200Datagram('P','N',data);
		NAV200Datagram dtrequest = new NAV200Datagram('P','A');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Activate Positioning Mode with depth of smoothing input (PN) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println(" Position Activated with depth of smoothing input");
		
		return(ok);

	}



	/** Layer selection (PL command). Selection of a new layer. It is possible to change layer during active Position Mode 
	*
	*	@param	layer	layer number
	*
	* 	@return	true if layer has been changed.
	*/ 
	public boolean selecLayer (int layer) throws LaserBeaconException {
		
		boolean ok = false;
		byte[] data = {(byte)layer};
		NAV200Datagram datagram = new NAV200Datagram('P','L',data);
		NAV200Datagram dtrequest = new NAV200Datagram('P','L',data);
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Layer Selection (PL) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println(" layer changed: "+layer);
		
		return(ok);
	
	}

	public int findLayer() throws LaserBeaconException{
		int maxlayer;
		int maxN = 0;
		int maxQ = 0;
		int layer = -1;
		double[] data;
		activateStandby();
		for(maxlayer = 0; maxlayer<50; maxlayer++)
			if(getReflector(maxlayer,0) == null) break;
		//System.out.println("Layer found "+maxlayer);
		for(int i = 0; i<maxlayer; i++){
			activatePos();
			selecLayer(i);
			data = getPosition();
			System.out.println("LAYER_"+i+": data["+data[0]+", "+data[1]+", "+Math.toDegrees(data[2])+"] Q="+(int)data[3]+" N="+(int)data[4]);			
			if(data[3]>maxN || (data[3] == maxN && data[4] > maxQ)){
				maxN = (int)data[4];
				maxQ = (int)data[3];
				layer = i;
			}
		}
		System.out.println("La mejor layer es "+layer+" con N = "+maxN+" y Q ="+maxQ);
		return layer;
	}

	/** Selection of number of N nearest (densest) (PC command).  
	*	The parameter N neaerst allow the user to search for a particular number of valid reflectors, located closest to 
	*   the laser scanner, in Positioning Mode
	*
	*	@param	N	new number of N nearest.
	*
	* 	@return	true if number of N nearest is changed.
	*/  
	public boolean selecNearest (int N) throws LaserBeaconException {
		
		boolean ok = false;
		byte[] data = {(byte)N};
		NAV200Datagram datagram = new NAV200Datagram('P','C',data);
		NAV200Datagram dtrequest = new NAV200Datagram('P','C',data);
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in selection of number of N nearest (PC) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println(" N nearest: "+N);
		
		return(ok);
	
	}

	/**  Request for position with automatic speed determination (PP command).  
	*
	*	
	* 	@return	array with x-position,y-position (meter), orientation (rad), quality (0-100,-1,-2), number of reflector used
	*/ 
	public double[] getPosition() throws LaserBeaconException{
		double[] pos;
		boolean ok = false;
		NAV200Datagram datagram = new NAV200Datagram('P','P');
		NAV200Datagram dtrequest = new NAV200Datagram('P','P');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in request for position (PP) ");
				printError(datagram);
			}
			throw new LaserBeaconException("NAV200: Posicion incorrecta (PP)"); 
		}
		//System.out.println("hola="+datagram.toString());
		pos = datagram.getPosition();
		if(pos == null) throw new LaserBeaconException("NAV200: Posicion incorrecta (PP)");
		if (debugAll == true)
			System.out.println("Pos("+pos[0]+", "+pos[1]+", "+Math.toDegrees(pos[2])+") Q="+pos[3]+" N="+pos[4]);
		
		return(pos);
	
	}
	
	
	/**  Request for position with input of speed in the laser scanner co-ordinate system(Pv command).   
	*
	*	@param	Vx	X-component of speed in laser scanner co-ordinate system in m/s
	*	@param	Vy	Y-component of speed in laser scanner co-ordinate system in m/s
	*
	* 	@return	array with x-position,y-position (meter), orientation (rad), quality (0-100,-1,-2), number of reflector used
	*/
	public double[] getPosition(double Vx, double Vy) throws LaserBeaconException{
	
		double[] pos;
		boolean ok = false;
		byte[]	byteVx = NAV200Datagram.Vel2Byte(Vx);
		byte[]	byteVy = NAV200Datagram.Vel2Byte(Vy);
		byte[] data = {byteVx[0], byteVx[1], byteVy[0], byteVy[1]};
		NAV200Datagram datagram = new NAV200Datagram('P','v',data);
		NAV200Datagram dtrequest = new NAV200Datagram('P','P');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in request for position with input of speed (Pv) ");
				System.out.println(" Vx = "+Vx+"("+Integer.toHexString(byteVx[1])+"h "+Integer.toHexString(byteVx[0])+"h)"+
						" Vy = "+Vy+"("+Integer.toHexString(byteVy[1])+"h "+Integer.toHexString(byteVy[0])+"h)");
				printError(datagram);
			}
			throw new LaserBeaconException("NAV200: Posicion incorrecta (Pv)"); 
		}
		pos = datagram.getPosition();
		if(pos == null) throw new LaserBeaconException("NAV200: Posicion incorrecta (Pv)");
		if (debugAll == true)
			System.out.println("Pos("+pos[0]+", "+pos[1]+", "+Math.toDegrees(pos[2])+") Q="+pos[3]+" N="+pos[4]);
		
		return(pos);
	
	}
	
	
	
	/**  Request for position with input of speed and angular velocity in the laser scanner co-ordinate system(Pw command).   
	*    
	*
	*	@param	Vx	X-component of speed in laser scanner co-ordinate system in m/s
	*	@param	Vy	Y-component of speed in laser scanner co-ordinate system in m/s
	*	@param	Va	positioning system angular velocity in rad/s
	*
	* 	@return	array with x-position,y-position (meter), orientation (rad), quality (0-100,-1,-2), number of reflector used
	*/
	public double[] getPosition(double Vx, double Vy, double Va) throws LaserBeaconException{

		double[] pos;
		boolean ok = false;
		byte[]	byteVx = NAV200Datagram.Vel2Byte(Vx);
		byte[]	byteVy = NAV200Datagram.Vel2Byte(Vy);
		byte[]	byteVa = NAV200Datagram.Angle2Byte(Va);
		byte[] data = {byteVx[0], byteVx[1], byteVy[0], byteVy[1], byteVa[0], byteVa[1]};
		NAV200Datagram datagram = new NAV200Datagram('P','w',data);
		NAV200Datagram dtrequest = new NAV200Datagram('P','P');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in request for position with input of speed an angular velocity (Pw)");
				System.out.println(" Vx = "+Vx+"("+Integer.toHexString(byteVx[1])+"h "+Integer.toHexString(byteVx[0])+"h)"+
						" Vy = "+Vy+"("+Integer.toHexString(byteVy[1])+"h "+Integer.toHexString(byteVy[0])+"h)"+
						" Va = "+Math.toDegrees(Va)+"deg ("+Integer.toHexString(byteVa[1])+"h "+Integer.toHexString(byteVa[0])+"h)");
				printError(datagram);
			}
			throw new LaserBeaconException("NAV200: Posicion incorrecta (Pw)"); 
		}
		pos = datagram.getPosition();
		if(pos == null) throw new LaserBeaconException("NAV200: Posicion incorrecta");
		if (debugAll == true)
			System.out.println("Pos("+pos[0]+", "+pos[1]+", "+Math.toDegrees(pos[2])+") Q="+pos[3]+" N="+pos[4]);
		
		return(pos);
			
	}
	
	
	/** Request for position with input of vehicle speed (PV command).  
	*
	*	@param	Vx	X-component of speed in laser scanner co-ordinate system in m/s
	*	@param	Vy	Y-component of speed in laser scanner co-ordinate system in m/s
	*	@param	Va	positioning system angular velocity in rad/s
	*
	* 	@return	array with x-position,y-position (meter), orientation (rad), quality (0-100,-1,-2), number of reflector used
	*/
	public double[] getPositionAbs(double Vx, double Vy, double Va) throws LaserBeaconException{
	
		double[] pos;
		boolean ok = false;
		byte[]	byteVx = NAV200Datagram.Vel2Byte(Vx);
		byte[]	byteVy = NAV200Datagram.Vel2Byte(Vy);
		byte[]	byteVa = NAV200Datagram.Angle2Byte(Va);
		byte[] data = {byteVx[0], byteVx[1], byteVy[0], byteVy[1], byteVa[0], byteVa[1]};
		NAV200Datagram datagram = new NAV200Datagram('P','V',data);
		NAV200Datagram dtrequest = new NAV200Datagram('P','P');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in request for position with input of vehicule speed (PV) ");
				printError(datagram);
				System.out.println(" Vx = "+Vx+"("+Integer.toHexString(byteVx[1])+"h "+Integer.toHexString(byteVx[0])+"h)"+
						" Vy = "+Vy+"("+Integer.toHexString(byteVy[1])+"h "+Integer.toHexString(byteVy[0])+"h)"+
						" Va = "+Math.toDegrees(Va)+"deg ("+Integer.toHexString(byteVa[1])+"h "+Integer.toHexString(byteVa[0])+"h)");

			}
			throw new LaserBeaconException("NAV200: Posicion incorrecta (PV)"); 
		}
		pos = datagram.getPosition();
		if(pos == null) throw new LaserBeaconException("NAV200: Posicion incorrecta");
		if (debugAll == true)
			System.out.println("Pos("+pos[0]+", "+pos[1]+", "+Math.toDegrees(pos[2])+") Q="+pos[3]+" N="+pos[4]);
		
		return(pos);
	
	}
	
	
	/**  Selection of layer with definition of position (PM command).
	*	 Selection of a new layer wih position definition allows rapid changes of layer during active Positioning mode.    
	*
	*	@param	Vx	X-component of speed in laser scanner co-ordinate system in m/s
	*	@param	Vy	Y-component of speed in laser scanner co-ordinate system in m/s
	*	@param	Va	positioning system angular velocity in rad/s
	*	@param	layer	new layer.
	*
	* 	@return	true if layer changed.
	*/ 
	public boolean changePos (double posx,double posy, double angle, int layer) throws LaserBeaconException {

		boolean ok = false;
		byte[] posXbyte = NAV200Datagram.Pos2Byte(posx);
		byte[] posYbyte = NAV200Datagram.Pos2Byte(posy);
		byte[] angbyte = NAV200Datagram.Angle2Byte(angle);
		byte[] data = {(byte)layer, posXbyte[0], posXbyte[1], posYbyte[0], posYbyte[1], angbyte[0], angbyte[1]};
		NAV200Datagram datagram = new NAV200Datagram('P','M',data);
		NAV200Datagram dtrequest = new NAV200Datagram('P','M');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error of selection of layer with definition of position(PM) ");
				printError(datagram);
			}
		}
		if (debugAll == true)
			System.out.println(" Position changed ");
		
		return(ok);
		
	
	}
	
	
	/**  Selection of action radii (PO command). Selection of the radius for determining position. The parameter Rfr and Rto define
	*	 a circular band within which the relevant reflector measurement are used.
	*
	*	@param	Rfr		Action radius of meter.
	*	@param	Rto		Action radius until meter.
	*
	* 	@return	true if selection is changed
	*/ 	
	public boolean changeRad (double Rfr,double Rto) throws LaserBeaconException {

		boolean ok = false;
		byte[] byteRfr = NAV200Datagram.Pos2Byte(Rfr);
		byte[] byteRto = NAV200Datagram.Pos2Byte(Rto);
		byte[] data = {byteRfr[0], byteRfr[1], byteRfr[2], byteRfr[3],byteRto[0], byteRto[1], byteRto[2], byteRto[3]};
		NAV200Datagram datagram = new NAV200Datagram('P', 'O',data);
		NAV200Datagram dtrequest = new NAV200Datagram('P', 'O');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in selection of action radii (PO) ");
				printError(datagram);
			}
		}
		if (debugAll == true)
			System.out.println(" Radii changed");
		
		return(ok);

	}
	
	
	
	
	////////////////////   S T A N D B Y    M O D E  /////////////////////////////	
	
	/**	Activate standby mode (SA command). The help function: request for hardware and software version, and the output
	*	and changing of individual reflector position, can only be carrier out in Standby mode.
	*
	* 	@return	true if standby mode is activated
	*/ 	 
	public boolean activateStandby () throws LaserBeaconException {
	    
		boolean ok = false;
		NAV200Datagram datagram = new NAV200Datagram('S','A');
		NAV200Datagram dtrequest = new NAV200Datagram('S','A');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Activate Standby Mode (SA) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println("Standby Activated");
		
		return(ok);
	    	
	}
	
	/**	Define direction of scanner rotation (SU command). Depending on the mounting position, the scanning head.
	*
	*	@param	number	1 clockwise (default), 0 anti-clockwise.
	*		
	* 	@return	true if command respose is correct.
	*/ 	
	public boolean clockWise (int number) throws LaserBeaconException {

		boolean ok = false;
		byte[] data = {(byte)number};
		NAV200Datagram datagram = new NAV200Datagram('S','U',data);
		NAV200Datagram dtrequest = new NAV200Datagram('S','U');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in defined direction of scanner rotation (SU) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println("Rotation defined");
		
		return(ok);
	
	}
	

	/**  Request for reflector position (SR command).
	*
	*	@param	layer	layer number (0 a Emax)
	*	@param	nbeacon reflector number (null if reflector not exist)
	*
	* 	@return	array with x-position,y-position (meter) of reflector and reflector number (-1 if error)
	*/ 	
	public double[] getReflector(int layer, int nbeacon) throws LaserBeaconException{
	
		double[] pos = new double[3];
		boolean ok = false;
		byte[] data = {(byte)layer, (byte)nbeacon};
		NAV200Datagram datagram = new NAV200Datagram('S','R',data);
		NAV200Datagram dtrequest = new NAV200Datagram('S','R');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			return null; // no existe ese reflector
		}
		pos[0] = NAV200Datagram.Byte2Pos(datagram.data[2],datagram.data[3],datagram.data[4],datagram.data[5]);  	// Pos x(meter)
		pos[1] = NAV200Datagram.Byte2Pos(datagram.data[6],datagram.data[7],datagram.data[8],datagram.data[9]);		// Pos y(meter)
		pos[2] = datagram.data[1];
		if (debugAll == true)
			System.out.println("Pos("+pos[0]+", "+pos[1]+", "+Math.toDegrees(pos[2])+") N="+pos[3]);
		
		return(pos);
	    
	}
	
	
	/**  Change a reflector position (SC command).
	*
	*	@param	posX	x-position of reflector in meter.
	*	@param	posY    y-position of reflector in meter.
	*	@param	layer   layer number.
	*	@param	nbeacon reflector number.

	*
	* 	@return	true if reflector has been changed
	*/ 	
	public boolean changeReflector(double posX, double posY, int layer, int nbeacon) throws LaserBeaconException{
	
		boolean ok = false;
		byte[] byteX = NAV200Datagram.Pos2Byte(posX);
		byte[] byteY = NAV200Datagram.Pos2Byte(posY);
		byte[] data = {(byte)layer, (byte)nbeacon, byteX[0], byteX[1], byteX[2], byteX[3], byteY[0], byteY[1], byteY[2], byteY[3]};
		NAV200Datagram datagram = new NAV200Datagram('S','C',data);
		NAV200Datagram dtrequest = new NAV200Datagram('S','C');
		NAV200Datagram dtrequest1= new NAV200Datagram('S','R');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0) || datagram.isEquals(dtrequest1,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in change Reflector (SC) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println(" Error in change Reflector (SC) ");
		
		return(ok);
			
	}
	
	/**  Insert a reflector position (comando SI). Insert a particular reflector position. 
	*
	*	@param	posX	x-position of reflector in meter.
	*	@param	posY    y-position of reflector in meter.
	*	@param	layer   layer number.
	*	@param	nbeacon reflector number.
	*
	* 	@return	true if reflector has been inserted.
	*/
	public boolean insertReflector(double posX, double posY, int layer, int nbeacon) throws LaserBeaconException{

		boolean ok = false;
		byte[] byteX = NAV200Datagram.Pos2Byte(posX);
		byte[] byteY = NAV200Datagram.Pos2Byte(posY);
		byte[] data = {(byte)layer, (byte)nbeacon, byteX[0], byteX[1], byteX[2], byteX[3], byteY[0], byteY[1], byteY[2], byteY[3]};
		NAV200Datagram datagram = new NAV200Datagram('S','I',data);
		NAV200Datagram dtrequest = new NAV200Datagram('S','I');
		NAV200Datagram dtrequest1= new NAV200Datagram('S','R');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0) || datagram.isEquals(dtrequest1,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Insert Reflector (SI) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println(" Reflector Inserted ");
		
		return(ok);	
	}
	

	/** Delete a reflector position (SD command).
	*
	*	@param	posX	x-position of reflector.
	*	@param	posY    y-position of reflector.
	*	@param	layer   layer number.
	*	@param	nbeacon reflector number.
	*
	* 	@return	true if reflector has been deleted.
	*/	
	public boolean deleteReflector(int layer, int nbeacon) throws LaserBeaconException{

		boolean ok = false;
		byte[] data = {(byte)layer, (byte)nbeacon};
		NAV200Datagram datagram = new NAV200Datagram('S','D',data);
		NAV200Datagram dtrequest = new NAV200Datagram('S','D');
		NAV200Datagram dtrequest1= new NAV200Datagram('S','R');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0) || datagram.isEquals(dtrequest1,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Delete Reflector (SD) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println(" Reflector "+ datagram.data[0] +" Deleted ");
		
		return(ok);	
	
	}
	
	
	/**  Read the reflector radius of layer (RG command).
	*
	*	@param	layer   layer number (0 to Emax)
	*
	* 	@return	reflector radius
	*/
	public int getRadius (int layer) throws LaserBeaconException {

		boolean ok = false;
		byte[] data = {(byte)layer};
		NAV200Datagram datagram = new NAV200Datagram('R','G',data);
		NAV200Datagram dtrequest = new NAV200Datagram('R','G');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Read Radius Reflector (RG) ");
				printError(datagram);
			}
			throw new LaserBeaconException("Incorrect datagram received");
		}
		if (debugAll == true) System.out.println("Radius Reflector is "+datagram.data[1]+"mm in layer "+datagram.data[0]);
		
		return(datagram.data[1]);	

	}
	
	
	/**  Set the reflector radius for a layer (RS command).
	*
	*	@param	layer   layer number
	*	@param	radius  reflector radius in milimeter.
	*
	* 	@return	true if reflector radius has been changed
	*/
	public boolean setRadius (int layer, int radius) throws LaserBeaconException {

		boolean ok = false;
		byte[] data = {(byte)layer, (byte)radius};
		NAV200Datagram datagram = new NAV200Datagram('R','S',data);
		NAV200Datagram dtrequest = new NAV200Datagram('R','G');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Set Radius Reflector (RS) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println("Radius Reflector changed ("+radius+" mm");
		
		return(ok);	

	}
	


	
	//////////////////// M A P P I N G    M O D E  /////////////////////////////	
 
	/**	Activated Mapping mode (Comando MA).
	*	Reflector position are measured in mapping mode. For this process the positioning system must be informed
	*   of its own orientation and position within the absolute co-ordinate system
	* 	@return	true if mapping mode has been activated
	*/ 
	public boolean activateMapping () throws LaserBeaconException {

		boolean ok = false;
		NAV200Datagram datagram = new NAV200Datagram('M','A');
		NAV200Datagram dtrequest = new NAV200Datagram('M','A');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Activate Mapping Mode (MA) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println("Mapping Activated");
		
		return(ok);	

	}
	
	
	/**	Start Mapping (MS command).
	*	This activates recording of the reflector positions at a defined location withn the plant
	*	When the positioning system has finished recording positions it responds with the number of reflectors scanned
	*
	*	@param	posx   x-position of the laser scanner in meter
	*	@param	posy   y-position of the laser scanner in meter
	*	@param	angle  prientation of the laser scanner in rad.
	*	@param	refRad  reflector radius (milimeter).
	*	@param	layer   layer number.
	*
	* 	@return	number or reflection scanned.
	*/
	public int startMapping (double posx, double posy, double angle, int refRad, int layer) throws LaserBeaconException {

		boolean ok = false;
		byte[]	byteX = NAV200Datagram.Pos2Byte(posx);
		byte[]	byteY = NAV200Datagram.Pos2Byte(posy);
		byte[]	byteA = NAV200Datagram.Angle2Byte(angle);
		byte[] data = {(byte)layer, byteX[0], byteX[1], byteX[2], byteX[3], byteY[0], byteY[1], byteY[2], byteY[3], byteA[0], byteA[1],(byte)refRad};
		NAV200Datagram datagram = new NAV200Datagram('M','S',data);
		NAV200Datagram dtrequest = new NAV200Datagram('M','S');
		
		datagram.send(outputS);
		waitData(30000);
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error de respuesta en comando Start Mapping (MS) ");
				printError(datagram);
			}
		}
		if (debugAll == true)
			System.out.println("Start Mapping");
		
		return(datagram.data[1]);
	   
	}
	
	/**	Start Mapping measurement (Comando MM). This activates recording of the reflector positions at a defined location
	*	withn the plant. The command corresponds to th MS Mapping command but with the additional possibility of setting the
	*   number of scans.
	*	
	*
	*	@param	posx   x-position of the laser scanner in meter
	*	@param	posy   y-position of the laser scanner in meter
	*	@param	angle  prientation of the laser scanner in rad.
	*	@param	refRad  reflector radius (milimeter).
	*	@param	layer   layer number.
	*	@param	mean   number of scans for mean formation
	*
	* 	@return	number or reflection scanned.
	*/	
	public int startMapping (double posx, double posy, double angle, int refRad, int layer, int mean) throws LaserBeaconException {

		boolean ok = false;
		byte[]	byteX = NAV200Datagram.Pos2Byte(posx);
		byte[]	byteY = NAV200Datagram.Pos2Byte(posy);
		byte[]	byteA = NAV200Datagram.Angle2Byte(angle);
		byte[] data = {(byte)layer, (byte)mean, byteX[0], byteX[1], byteX[2], byteX[3], byteY[0], byteY[1], byteY[2], byteY[3], byteA[0], byteA[1],(byte)refRad};
		NAV200Datagram datagram = new NAV200Datagram('M','M',data);
		NAV200Datagram dtrequest = new NAV200Datagram('M','S');
		
		datagram.send(outputS);
		waitData(30000);
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Start Mapping Measurement (MM) ");
				printError(datagram);
			}
		}
		if (debugAll == true)
			System.out.println("Start Mapping Measurement");
		
		return(datagram.data[1]);
	    
	}

	
	/**	Start negative Mapping measurement (Comando MN).
	*	This activates recording or the reflector positions at a defined location within the plant. The measures are compared
	*   to the existing measurement for this layer.
	*
	*	@param	posx   x-position of the laser scanner in meter
	*	@param	posy   y-position of the laser scanner in meter
	*	@param	angle  prientation of the laser scanner in rad.
	*	@param	refRad  reflector radius (milimeter).
	*	@param	layer   layer number.
	*	@param	mean   number of scans for mean formation
	*
	* 	@return	number or reflection scanned.
	*/	
	public int startMappingNeg (double posx, double posy, double angle, int refRad, int layer, int mean) throws LaserBeaconException {

		boolean ok = false;
		byte[]	byteX = NAV200Datagram.Pos2Byte(posx);
		byte[]	byteY = NAV200Datagram.Pos2Byte(posy);
		byte[]	byteA = NAV200Datagram.Angle2Byte(angle);
		byte[] data = {(byte)layer, (byte)mean, byteX[0], byteX[1], byteX[2], byteX[3], byteY[0], byteY[1], byteY[2], byteY[3], byteA[0], byteA[1],(byte)refRad};
		NAV200Datagram datagram = new NAV200Datagram('M','N',data);
		NAV200Datagram dtrequest = new NAV200Datagram('M','S');
		
		datagram.send(outputS);
		waitData(30000);
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Start Mapping Measurement (MM) ");
				printError(datagram);
			}
		}
		if (debugAll == true)
			System.out.println("Start Mapping Measurement");
		
		return(datagram.data[1]);

	}

	
	
	/**  Request for Mapping position (MR command).
	*	Request for output of the position of reflector in layer.
	*
	*	@param	layer	layer number
	*	@param	nbeacon Reflector number..
	*
	* 	@return	array with x-position,y-position (meter) of reflector and reflector number (-1 if error)	*/ 	
	public double[] getMapping(int layer, int nbeacon) throws LaserBeaconException{
	
		double[] pos = new double[3];
		boolean ok = false;
		byte[] data = {(byte)layer, (byte)nbeacon};
		NAV200Datagram datagram = new NAV200Datagram('M','R',data);
		NAV200Datagram dtrequest = new NAV200Datagram('M','R');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Request of Mapping (MR) ");
				printError(datagram);
			}
			throw new LaserBeaconException("NAV200: Posicion incorrecta");
		}
		pos[0] = NAV200Datagram.Byte2Pos(datagram.data[2],datagram.data[3],datagram.data[4],datagram.data[5]);  	// Pos x(meter)
		pos[1] = NAV200Datagram.Byte2Pos(datagram.data[6],datagram.data[7],datagram.data[8],datagram.data[9]);		// Pos y(meter)
		pos[2] = datagram.data[1];
		if (debugAll == true)
			System.out.println("Pos("+pos[0]+", "+pos[1]+", "+Math.toDegrees(pos[2])+") N="+pos[3]);
		
		return(pos);

	}
	
	
	
	
	
	/////////////////////    U P L O A D  M O D E   ///////////////////////////

	/**	Activate Upload (UA command).
	*	In upload every reflector position requested individually to ensure correct processing and transfer of the data
	*
	* 	@return	trueif upload mode has been activated
	*/	
	public boolean activateUpload () throws LaserBeaconException {
	    
		boolean ok = false;
		NAV200Datagram datagram = new NAV200Datagram('U','A');
		NAV200Datagram dtrequest = new NAV200Datagram('U','A');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Activate Upload Mode (UA) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println("Upload Activated");
		
		return(ok);	

	}
	

	/** Request upload transmition ( UR command). Request for output of position of reflector in a layer. All reflector in a layer have
	*	been transferred when the reflector number is -1
	*	@param	layer	layer numbre (0 to Emax)
	*
	* 	@return	array with the output of reflector position (x-position,y-position, reflector number)
	*/
	public double[] getUpload(int layer) throws LaserBeaconException{
	
		double[] pos = new double[3];
		boolean ok = false;
		byte[] data = {(byte)layer};
		NAV200Datagram datagram = new NAV200Datagram('U','R',data);
		NAV200Datagram dtrequest = new NAV200Datagram('U','R');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Upload Transmition (UR) ");
				printError(datagram);
			}
			throw new LaserBeaconException("NAV200: Posicion incorrecta");
		}
		pos[0] = NAV200Datagram.Byte2Pos(datagram.data[2],datagram.data[3],datagram.data[4],datagram.data[5]);  	// Pos x(meter)
		pos[1] = NAV200Datagram.Byte2Pos(datagram.data[6],datagram.data[7],datagram.data[8],datagram.data[9]);		// Pos y(meter)
		pos[2] = datagram.data[1];
		if (debugAll == true)
			System.out.println("Pos("+pos[0]+", "+pos[1]+", "+Math.toDegrees(pos[2])+") N="+pos[3]);
		
		return(pos);
	
	}
	
	
	
	
		/////////////////////   D O W N L O A D   M O D E  ///////////////////////////

	/**	Activated Download (DA command).
	*	Download mode activated the transfer of reflector data sers for saving in the positioning system.
	*
	* 	@return	true if download mode has been activated
	*/		
	public boolean activateDownload () throws LaserBeaconException {

		boolean ok = false;
		NAV200Datagram datagram = new NAV200Datagram('D','A');
		NAV200Datagram dtrequest = new NAV200Datagram('D','A');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Activate Download Mode (DA) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println("Download Activated");
		
		return(ok);	

	}
	
	

	/**	Download reflector position ( DR command). All reflector in layer have been transferred when the reflector number is -1.
	*	
	*	@param	posX     x-position of the laser scanner in meter
	*	@param	posY     y-position of the laser scanner in meter
	*	@param	layer    layer number.
	*	@param	nbeacon  beacon number
	*
	* 	@return	true reflector position is download
	*/
	public boolean setDownload(double posX, double posY, int layer, int nbeacon) throws LaserBeaconException{

		boolean ok = false;
		byte[]	byteX = NAV200Datagram.Pos2Byte(posX);
		byte[]	byteY = NAV200Datagram.Pos2Byte(posY);
		byte[] data = {(byte)layer, (byte)nbeacon, byteX[0], byteX[1], byteX[2], byteX[3], byteY[0], byteY[1], byteY[2], byteY[3]};
		NAV200Datagram datagram = new NAV200Datagram('D','R',data);
		NAV200Datagram dtrequest = new NAV200Datagram('D','R');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Download Reflector position (DR) ");
				printError(datagram);
			}
		}
		if (debugAll == true)
			System.out.println(" Reflector Download ");
		
		return ok;

	}
	
	
	
	////////////////////  M E A S U R E M E N T    M O D E /////////////////////////////	

	/**	Activate Measurement mode (TA command). In this mode, the measurement of laser scanner (range,bearing) is request. 
	*
	* 	@return	true if measurement mode has been activated .
	*/	
	public boolean activateMeasures () throws LaserBeaconException {

		boolean ok = false;
		NAV200Datagram datagram = new NAV200Datagram('T','A');
		NAV200Datagram dtrequest = new NAV200Datagram('T','A');
		
		datagram.send(outputS);		
		datagram.read(inputS);
		ok = datagram.isEquals(dtrequest,0);
		if(!ok){
			if(debugError || debugAll){
				System.out.println(" Error in Activate Measurement (TA) ");
				printError(datagram);
			}
		}
		if (debugAll == true) System.out.println("Measurement Activated");
		
		return(ok);	

	}
	


	/**	Get the measures of laser scanner ( TS command). 
	*	
	* 	@return	array with measures number, and range(meter), bearing(rad) of all measures
	*
	*/
	public double[] getMeasures() throws LaserBeaconException{
	
		double[] measures;		
		int nMeasures;
		NAV200Datagram datagram = new NAV200Datagram('T','S');
		
		datagram.send(outputS);		
		datagram.read(inputS);

		if (debugAll == true) System.out.println("Measurement Activated");
		nMeasures = datagram.data[0];
		measures = new double[nMeasures*2+1];
		measures[0]= nMeasures; 
		for (int i=0;i<nMeasures;i++){
		    measures[2*i+1]=Math.abs(NAV200Datagram.Byte2Pos(datagram.data[1+i*7],datagram.data[2+i*7],datagram.data[3+i*7],datagram.data[4+i*7]));  	
		    measures[2*i+2]=NAV200Datagram.Byte2Angle(datagram.data[5+i*7],datagram.data[6+i*7]);							
		}
		return measures;

	}
	

	/**	Get the measures of laser scanner filtered
	*
	*	@param	Nmin   minimum measures for reflector
	*
	* 	@return	array Nx3  with range(meter), bearing(rad) and number of scan of all measures
	*/
	public double[][] getMeasures(int Nmin){
	    double angle, meanAngle=0,meanRange=0;
	    int count = 0;
	    int num = 0;				// Indice del numero de medidas filtradas
	    double [] measures;
	    double[][] measure = new double [20][3];
	    double [][] ret;
	    double angMax = 1.5;					// Grado minimo de separacion que debe tener las medidas para considerarse
	    
	    try{
	        measures = getMeasures();	// Obtiene las medidas del laser								// de una misma baliza
	    }
	    catch (Exception e){
	        double[][]Null = {{0.0,0.0,0.0}};
	        return Null;
	    }
	    if(measures[0] == 0) {
	        double[][]Null = {{0.0,0.0,0.0}};
	        return Null;
	    }
	    
	    angle=measures[2];
	    meanAngle = measures[2];
	    meanRange = Math.abs(measures[1]);
	    count ++;
	    
	    for(int i=1;i<measures[0];i++){
	        if (Math.abs(angle - measures[i*2+2]) < (angMax*Math.PI/180)){
	            meanAngle += measures[i*2+2];
	            meanRange += Math.abs(measures[i*2+1]);
	            count++;
	        }
	        else{
	            if(count >= Nmin){ 	
	                measure[num][0] = meanRange/count;
	                measure[num][1] = meanAngle/count;
	                measure[num][2] = count;
	                num++;
	            }
	            meanRange = measures[i*2+1];
	            meanAngle = measures[i*2+2];
	            count = 1;
	        }
	        angle = measures[i*2+2];
	    }
	    
	    if(count >= Nmin){
	        measure[num][0] = meanRange/count;
	        measure[num][1] = meanAngle/count;			
	        measure[num][2] = count;			
	        num++;
	    }
	    ret = new double[num][3];
	    for(int i = 0; i<num; i++){
	        ret[i][0] = measure[i][0];
	        ret[i][1] = measure[i][1];
	        ret[i][2] = measure[i][2];
	    }
	    return ret;
	}	
	







/////////// Auxiliary methods	 /////////////


			


	// Print error in command
	private void printError(NAV200Datagram datagram){
	    if(datagram==null) return;
	    System.out.println("Trama recibida:\n "+datagram);
	    
	    if(datagram.data.length==0) return;
	    
	    System.out.println("Mode: "+datagram.mode);
	    System.out.println("Type: "+datagram.function);
	    if(datagram.data.length < 1) return;
	    System.out.println("funcion "+(char)datagram.data[0]);
	    
	    if(datagram.data.length < 2) return;
	    switch(datagram.data[1]){
	    case 1:
	        System.out.println("1- Software: Error regarding the content of the user's sotware protocol");
	        break;
	    case 2:
	        System.out.println("2- Transputer: Unused");
	        break;
	    case 3:
	        System.out.println("3- Sensor link: Error in the connection between TPU and sensor");
	        break;
	    case 4:
	        System.out.println("4- Distance measurement: Spurious measurement");
	        break;
	    case 5:
	        System.out.println("5- Rotation and angular measurement: Spurious angular measurement");
	        break;
	    case 6:
	        System.out.println("6- EEPROM link: Error in connection to reflector memory");
	        break;
	    case 7:
	        System.out.println("7- User link: Error in connection between user computer and TPU (RS232 Interface)");
	        break;
	    default:
	        System.out.println(" Unknown Respose");
	    }
	    
	    if(datagram.data.length < 3) return;
	    switch(datagram.data[2]){
	    case 1:
	        System.out.println("1-  Input/ output, telegram trafic");
	        break;
	    case 2:
	        System.out.println("2-  Standby Mode");
	        break;
	    case 3:
	        System.out.println("3-  Handling reference reflectors");
	        break;
	    case 4:
	        System.out.println("4-  Download Mode");
	        break;
	    case 5:
	        System.out.println("5-  Upload Mode");
	        break;
	    case 6:
	        System.out.println("6-  Mapping Mode");
	        break;
	    case 7:
	        System.out.println("7-  Position Mode");
	        break;
	    case 8:
	        System.out.println("8-  Test Mode");
	        break;
	    case 9:
	        System.out.println("7-  Navigation operation, general");
	        break;
	        
	    default:
	        System.out.println(" Unknown Respose");
	    }
	    
	    if(datagram.data.length < 4) return;
	    switch(datagram.data[3]){
	    case 1:
	        System.out.println("1-   Unknown command");
	        break;
	    case 2:
	        System.out.println("2-   Command (function) not implemented");
	        break;
	    case 3:
	        System.out.println("3-   Wrong command");
	        break;
	    case 4:
	        System.out.println("4-   Reflector not available");
	        break;
	    case 5:
	        System.out.println("5-   Wrong reflector number");
	        break;
	    case 6:
	        System.out.println("6-   Wrong data block");
	        break;
	    case 7:
	        System.out.println("7-   Input not possible");
	        break;
	    case 8:
	        System.out.println("8-   Invalid Layer");
	        break;
	    case 9:
	        System.out.println("9-   No reflectors in layer");
	        break;
	    case 10:
	        System.out.println("10-  All reflectors tranferred");
	        break;
	    case 11:
	        System.out.println("11-  Communication error");
	        break;
	    case 12:
	        System.out.println("12-   Error in the initialisation of reference reflectors");
	        break;
	    case 13:
	        System.out.println("13-   Wrong radii during input of effective range");
	        break;
	    case 14:
	        System.out.println("14-   Flash EPROM not functional");
	        break;
	    case 15:
	        System.out.println("15-   Wrong reflector radius");
	        break;
	    default:
	        System.out.println(" Unknown Respose");
	    }
	    
	    
	}
	
	public void close(){
	    try {
            inputS.close();
            outputS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	    port.close();
	}
	
	public void clearBuffer(){
		try {
			int n = inputS.available();
			if(n>0){
				System.out.println("  [NAV200] Eliminando "+n+" datos de entrada del Puerto Serie");
				inputS.skip(n);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Espera hasta un tiempo maximo hasta que haya un dato disponible
	private boolean waitData(int time){
		long st = System.currentTimeMillis();
		while((System.currentTimeMillis()-st)<time){
			try{
				if(inputS.available()>0) return true;
				Thread.sleep(200);
			}catch(Exception e){e.printStackTrace(); return false;}
		}
		return false;
	}

}
