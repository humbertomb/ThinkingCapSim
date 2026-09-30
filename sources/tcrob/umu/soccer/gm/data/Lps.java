/*
 * Created on 16-jun-2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.umu.soccer.gm.data;

import java.util.*;

public class Lps
{
	public final static int			INIT_LMS		= 1;
	public final static int			NUM_LMS			= 2;
	public final static int			INIT_NETS		= 3;
	public final static int			NUM_NETS		= 2;
	public final static int			LPS_SIZE		= (1 + NUM_LMS + NUM_NETS);

	static public final short[]		TYPE			= { Lpo.BALL, Lpo.LANDMARK, Lpo.LANDMARK, Lpo.NET, Lpo.NET };
	static public final String[]	NAME			= {"BALL","LM1","LM2","NET1","NET2" };
	static public final String[][]	ICON			= { {"ball.gif","lm1.gif", "lm2.gif","net1.gif","net2.gif" },//Blue Team
													{"ball.gif","lm2.gif", "lm1.gif","net2.gif","net1.gif" }};//Red Team

	protected int 					astray;			// flag: we are in an undesirable position
	protected Lpo 					lpo[];			// the standard LPS objects


	
	public Lps ()
	{
		lpo = new Lpo[LPS_SIZE];
		for (int i = 0; i < LPS_SIZE; i++)
			lpo[i] = new Lpo ((short) i, TYPE[i]);
		
	
	}
	
	public Lpo getLpo (int index)			{ return lpo[index]; }
	
	public void fromLog (StringTokenizer st)
	{
		for (int i = 0; i < 5; i++)		// ONLY main objects
			lpo[i].fromLog (st, (short) i);
	}
	
	public void evaporate (double evaporate)
	{
		for (int i = 0; i < LPS_SIZE; i++)
			lpo[i].evaporate(evaporate);
	}

	public void clamp (Odometry odometry, double dt)
	{
		for (int i = 0; i < LPS_SIZE; i++)
		{
			if (lpo[i].needClamping () && (lpo[i].getAnchored()>0.0))
				lpo[i].clamp (odometry, dt);
			
			// Next time do clamping unless Lpo has been seen
			lpo[i].doClamping (true);
		}
	}
	public void print() {
		for (int i = 0; i < LPS_SIZE; i++)
		{
			System.out.println(lpo[i].toString());
		}
		System.out.println("----------------------------");
	}
	
	
}
