/*
 * @(#)ItemBehInformation.java		1.0 2004/02/07
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tc.shared.linda;

/**
 * This class implements a specific item that contains the informations used to
 * specify if the behaviour debug process has to be started or stopped.
 * 
 * @version	1.0		07 Feb 2004
 * @author 	Denis Remondini
 */
public class ItemBehDebug extends Item {

	/** START debug command */
	public final static int START	= 0;
	/** STOP debug command */
	public final static int STOP	= 1;
	/* Stores the debug command (START or STOP) */
	private int command;
	
	/**
	 * Constructs the item with the default debug command (STOP).
	 */
	public ItemBehDebug() {
		command = STOP;
	}
	
	/**
	 * Sets the debug command
	 * @param comm the command: it can be START or STOP.
	 */
	public void setCommand(int comm) {
		command = comm;
	}
	
	/**
	 * Returns the debug command
	 * @return the command (it can be START or STOP).
	 */
	public int getCommand() {
		return command;
	}
}
