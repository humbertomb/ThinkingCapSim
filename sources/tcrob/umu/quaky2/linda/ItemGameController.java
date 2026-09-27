/*
 * (c) 2026 Humberto Martinez
 */
 
package tcrob.umu.quaky2.linda;

import java.io.*;

import tc.shared.linda.*;

public class ItemGameController extends Item implements Serializable
{
	public enum ScanStates			{ INITIAL, READY, SET, PLAYIMNG, PENALIZED, FINISHED }

	public ScanStates				gameState = ScanStates.INITIAL;
	public int						player = 0;
	
	//Constructors
	public ItemGameController ()
	{
		this.set (0);
	}
	
	// Instance methods
	public void setState (ScanStates gameState, int player)
	{
		this.player		= player;
		this.gameState	= gameState;
	}
	
	public String toString ()
	{
		return "STATE ["+gameState+"] for player <"+player+">";
	}
}