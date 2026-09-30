/**
 * Created on 03-jul-2006
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.soccer.gm.data;

import java.io.*;
import java.util.*;

import wucore.utils.math.*;

public class OdometryErrors 
{
	public int			nsets;
	public Odometry[]	errors;
	
	public OdometryErrors (String name)
	{
		Properties		props = new Properties ();
		try
		{
			File f = new File (name);
			FileInputStream fd = new FileInputStream (f);
			props.load (fd);
			fd.close ();
		} catch (Exception e) { }
		
		nsets		= new Integer (props.getProperty("ODO_COUNT")).intValue ()+1;
		errors		= new Odometry[nsets];
		
		errors[0]	= new Odometry ();
		errors[0].dlin = 0; 			//This will only be used when v=0.00
		errors[0].dlat = 0; 			//Otherwise v would be ceiled and index
		errors[0].drot = 0.0f; 		//[1] would be used 
		errors[0].elin = 0; 
		errors[0].elat = 0; 
		errors[0].erot = 0.0f; 
		
		for (int i = 0; i < nsets-1; i++)
		{
			int					ndx;
			StringTokenizer		st;
			double				reqspeed, obtspeed, errspeed;
			
			ndx					= nsets - i -1;
			errors[ndx]			= new Odometry ();
			
			st					= new StringTokenizer (props.getProperty ("ODO_LIN_"+i), " ");
			reqspeed				= new Double (st.nextToken ()).doubleValue ();
			obtspeed				= new Double (st.nextToken ()).doubleValue ();
			errspeed				= new Double (st.nextToken ()).doubleValue ();
			errors[ndx].dlin		= (float) (obtspeed / reqspeed); 
			errors[ndx].elin		= (float) (errspeed * 1000.0); 

			
			st					= new StringTokenizer (props.getProperty ("ODO_LAT_"+i), " ");
			reqspeed				= new Double (st.nextToken ()).doubleValue ();
			obtspeed				= new Double (st.nextToken ()).doubleValue ();
			errspeed				= new Double (st.nextToken ()).doubleValue ();
			errors[ndx].dlat		= (float) (obtspeed / reqspeed); 
			errors[ndx].elat		= (float) (errspeed * 1000.0); 
			
			st					= new StringTokenizer (props.getProperty ("ODO_ROT_"+i), " ");
			reqspeed				= new Double (st.nextToken ()).doubleValue ();
			obtspeed				= new Double (st.nextToken ()).doubleValue ();
			errspeed				= new Double (st.nextToken ()).doubleValue ();
			errors[ndx].drot		= (float) (obtspeed / reqspeed); 
			errors[ndx].erot		= (float) (errspeed * 1.5 * Angles.DTOR); 
		}
	}
	
	public void printErrors ()
	{
		for (int i = 0; i < nsets; i++)
			System.out.println ("i="+i+" \terrors=["+(int) errors[i].elin+", "+(int) errors[i].elat+", "+(int) (errors[i].erot * Angles.RTOD)+"]");
	}
}
