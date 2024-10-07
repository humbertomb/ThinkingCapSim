/*
 * Minidome.java

 * Driver for the minidome remote camera
 *
 * @author Bernardo Cánovas Segura
 */

package devices.drivers.camera.minidome;

import javax.comm.*;
import java.io.*;
import java.util.*;

import devices.drivers.camera.*;

public class Minidome extends Camera
{

    private InputStream			inputS		= null;					/** InputStream utilizado para recibir información desde el sensor. */
    private OutputStream		outputS		= null;					/** OutputStream utilizado para enviar comandos al sensor. */
    private SerialPort			serport		= null;
	private MinidomeAllCommands commands	= null;
	private final boolean		DEBUG		= false;

	
	// Constructors
	public Minidome()
	{
		super ();
		commands= new MinidomeAllCommands();
	}

	public void initialise (String commPortName) throws CameraException
	{
		CommPortIdentifier identifier;
		try		
		{
			identifier = CommPortIdentifier.getPortIdentifier(commPortName);
			serport	= (SerialPort) identifier.open("Minidome Telecamera Driver", 1000);
			serport.setSerialPortParams(2400,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
			serport.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			inputS	= serport.getInputStream();
			outputS = serport.getOutputStream();
        }
        catch (NoSuchPortException e)				{ throw new CameraException ("No se ha pòdido encontrar el identificador del puerto.\t"+e);}
        catch (PortInUseException e)				{ throw new CameraException ("No se ha podido abrir el puerto serie. Esta ya en uso.\t"+e);}
//        catch (UnsupportedComOperationException e)	{ throw new CameraException ("Error al introduccir los parámetros de configuración del láser.\t"+e);}
        catch (IOException e)						{ throw new CameraException ("Error E/S con los Stream.\t"+e);}
        catch (Exception e) {throw new CameraException ("Error al abrir el puerto con el minidome.\t"+e); }
	}
	
    public void close () throws CameraException
    {
		try
		{
			inputS.close ();
			outputS.close ();
			serport.close ();
        } catch (Exception e) { throw new CameraException ("Error cerrando el puerto serie y/o los stremas.\t"+e);}
    }

	public boolean send (Comando comando)
	{
		MinidomeCommand mcommand;

		byte[] commandChain;
		if (!commands.isSupported(comando))
		{
			if (DEBUG) System.out.println("Minidome.java: Command "+comando.orden+" Not supported by Minidome");
			return true;
		}

		mcommand=(MinidomeCommand)commands.getCommand(comando);
		if (mcommand.getParamLength()!=0)
			if (DEBUG) System.out.println("Minidome.java: Warning! Sending a command that needs "+mcommand.getParamLength()+" params with no params ");
		
		commandChain= mcommand.com();
		try {
			if (DEBUG) 
			{
				String aux;
				aux="";
				for (int i =0; i<commandChain.length; i++)
				aux=aux+(byte)commandChain[i]+" ";
				System.out.println("Sending to camera "+comando.orden+":"+aux);
			}
			outputS.write(commandChain);
		} catch (Exception e) {System.out.println("Error en outputStream\t"+e);}
		
		return true;
	
	}
	
	/** Sends a command with one argument (only MIXED_MOVEMENT supports it) */
	public boolean send (Comando comando, int numero)
	{
		MinidomeCommand mcommand;
		byte[] commandChain;
		byte[] paramChain= new byte[2];
		if (!commands.isSupported(comando))
		{
			if (DEBUG) System.out.println("Minidome.java: Command "+comando.orden+" Not supported by Minidome");
			return true;
		}
		if (!comando.orden.equals("MIXED_MOVEMENT"))
		{
			if (DEBUG) System.out.println("Minidome.java: Warning: command "+comando.orden+" tried to be send with one integer param (only MIXED_MOVEMENT has one param). Retrying with no params");
			send(comando);
		}
		else
		{
			mcommand=(MinidomeCommand)commands.getCommand(comando);
			paramChain[0]=(byte)numero;
			paramChain[1]=0;
			commandChain= mcommand.com(paramChain);
			try {
				if (DEBUG) 
				{
					String aux;
					aux="";
					for (int i =0; i<commandChain.length; i++)
					aux=aux+(byte)commandChain[i]+" ";
					System.out.println("Sending to camera "+comando.orden+":"+aux);
				}
				outputS.write(commandChain);
			} catch (Exception e) {System.out.println("Error en outputStream\t"+e);}
		
			return true;
		}
		return false;
	}
	
	/** Sends a command with two arguments (only MOVEMENT_SPEED ) */
	public boolean send (Comando comando, int param1, int param2)
	{
		MinidomeCommand mcommand;
		byte[] commandChain;
		byte[] paramChain= new byte[2];
		if (!commands.isSupported(comando))
		{
			if (DEBUG) System.out.println("Minidome.java: Command "+comando.orden+" Not supported by Minidome");
			return true;
		}
		if (!comando.orden.equals("MOVEMENT_SPEED"))
		{
			if (DEBUG) System.out.println("Minidome.java: Warning: command "+comando.orden+" tried to be send with two integer param (only MOVEMENT_SPEED has 2 params). Retrying with no params");
			send(comando);
		}
		else
		{
			mcommand=(MinidomeCommand)commands.getCommand(comando);
			paramChain[0]=(byte)param1;
			paramChain[1]=(byte)param2;
			commandChain= mcommand.com(paramChain);
			try {
				if (DEBUG) 
				{
					String aux;
					aux="";
					for (int i =0; i<commandChain.length; i++)
					aux=aux+(byte)commandChain[i]+" ";
					System.out.println("Sending to camera "+comando.orden+":"+aux);
				}
				outputS.write(commandChain);
			} catch (Exception e) {System.out.println("Error en outputStream\t"+e);}
		
			return true;
		}
		return false;
	}

	/** Sends a command with three arguments (only SET_AZ_EL_ZOOM ) 
	  * Note: valid params values (in case of SET_AZ_EL_ZOOM command for minidome) :
	  *		param1: Azimut 0-3600 (0-> 0.0º, 3600-> 360.0º)
	  *		param2: Elevation 80-850 (46-900º resolution 0.1º??)
	  *		param3: Zoom 41-738 (4.1-73.8mm )
	  *
	*/
	public boolean send (Comando comando, int param1, int param2, int param3)
	{
		MinidomeCommand mcommand;
		byte[] commandChain;
		byte[] paramChain= new byte[6];
		if (!commands.isSupported(comando))
		{
			if (DEBUG) System.out.println("Minidome.java: Command "+comando.orden+" Not supported by Minidome");
			return true;
		}
		if (!comando.orden.equals("SET_AZ_EL_ZOOM"))
		{
			if (DEBUG) System.out.println("Minidome.java: Warning: command "+comando.orden+" tried to be send with three integer param (only SET_AZ_EL_ZOOM has 3 params). Retrying with no params");
			send(comando);
		}
		else
		{
			mcommand=(MinidomeCommand)commands.getCommand(comando);
			paramChain[0]=(byte)(param1%256);
			paramChain[1]=(byte)(param1/256);
			paramChain[2]=(byte)(param2%256);
			paramChain[3]=(byte)(param2/256);
			paramChain[4]=(byte)(param3%256);
			paramChain[5]=(byte)(param3/256);
			
			commandChain= mcommand.com(paramChain);
			try {
				if (DEBUG) 
				{
					String aux;
					aux="";
					for (int i =0; i<commandChain.length; i++)
					aux=aux+(byte)commandChain[i]+" ";
					System.out.println("Sending to camera "+comando.orden+":"+aux);
				}
				outputS.write(commandChain);
			} catch (Exception e) {System.out.println("Error en outputStream\t"+e);}
		
			return true;
		}
		return false;
	}

	public Collection getCommandGroups()
	{
		Vector groups = new Vector();
		Collection allComs;
		Comando command;
		Iterator it;
		allComs=commands.getAvaiableCommands();
		it=allComs.iterator();
		while (it.hasNext())
		{
			command=(Comando)it.next();
			if(!groups.contains(command.grupo))
				groups.add(command.grupo);		
		}
		return groups;
	}

	public boolean isAvaiable(Comando comando)
	{
		return commands.isSupported(comando);
	}

}