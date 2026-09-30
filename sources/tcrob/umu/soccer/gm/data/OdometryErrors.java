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
		
		nsets		= Integer.valueOf (props.getProperty("ODO_COUNT"))+1;
		errors		= new Odometry[nsets];
		
		errors[0]	= new Odometry ();
		errors[0].dlin = 0; 			//This will only be used when v=0.00
		errors[0].dlat = 0; 			//Otherwise v would be ceiled and index
		errors[0].drot = 0.0; 		//[1] would be used 
		errors[0].elin = 0; 
		errors[0].elat = 0; 
		errors[0].erot = 0.0; 
		
		for (int i = 0; i < nsets-1; i++)
		{
			int					ndx;
			StringTokenizer		st;
			double				reqspeed, obtspeed, errspeed;
			
			ndx					= nsets - i -1;
			errors[ndx]			= new Odometry ();
			
			st					= new StringTokenizer (props.getProperty ("ODO_LIN_"+i), " ");
			reqspeed				= Double.valueOf (st.nextToken ());
			obtspeed				= Double.valueOf (st.nextToken ());
			errspeed				= Double.valueOf (st.nextToken ());
			errors[ndx].dlin		= (double) (obtspeed / reqspeed); 
			errors[ndx].elin		= (double) (errspeed * 1000.0); 

			
			st					= new StringTokenizer (props.getProperty ("ODO_LAT_"+i), " ");
			reqspeed				= Double.valueOf (st.nextToken ());
			obtspeed				= Double.valueOf (st.nextToken ());
			errspeed				= Double.valueOf (st.nextToken ());
			errors[ndx].dlat		= (double) (obtspeed / reqspeed); 
			errors[ndx].elat		= (double) (errspeed * 1000.0); 
			
			st					= new StringTokenizer (props.getProperty ("ODO_ROT_"+i), " ");
			reqspeed				= Double.valueOf (st.nextToken ());
			obtspeed				= Double.valueOf (st.nextToken ());
			errspeed				= Double.valueOf (st.nextToken ());
			errors[ndx].drot		= (double) (obtspeed / reqspeed); 
			errors[ndx].erot		= (double) (errspeed * 1.5 * Angles.DTOR); 
		}
	}
	
	public void printErrors ()
	{
		for (int i = 0; i < nsets; i++)
			System.out.println ("i="+i+" \terrors=["+(int) errors[i].elin+", "+(int) errors[i].elat+", "+(int) (errors[i].erot * Angles.RTOD)+"]");
	}
}
