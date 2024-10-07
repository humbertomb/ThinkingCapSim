/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2000-2001
 * Company:      Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

package devices.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

import devices.data.*;

public class DesktopDevices extends JDesktopPane
{
  static public final int			GPS_DEVICE			= 0;
  static public final int			COMPASS_DEVICE		= 1;
  
  private CompassDataPanel[] compass;
  private GPSDataPanel[] gps;
  private int compassindex, maxcompass;
  private int gpsindex, maxgps;


  public DesktopDevices(int numGPS, int numcompass)
  {
    super();
    compassindex = 0;
    gpsindex = 0;
//    setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    compass = new CompassDataPanel[numcompass];
    gps = new GPSDataPanel[numGPS];
    maxcompass = numcompass;
    maxgps = numGPS;
  }

  public DesktopDevices ()
  {
    this(5,5);
  }

  public void purgeDesktop ()
  {
    JInternalFrame[] frames = this.getAllFrames();
    int i;
    
    for (i=0; i < frames.length; i++)
    {
      frames[i].dispose();
    }
    compassindex = 0;
    gpsindex=0;
  }

  public void addDevice (int type, String name)
  {
    if (type == COMPASS_DEVICE)
    {
      if (compassindex < maxcompass)
      {
        compass[compassindex] = new CompassDataPanel (name);
        addIntFrame (compass[compassindex], "COMPASS: "+name);
        compassindex++;
      }
      else
      {
        System.err.println ("Imposible añadir dispositivo. Se superaría el maximo.");
      }
    }
    else if (type == GPS_DEVICE)
    {
      if (gpsindex < maxgps)
      {
        gps[gpsindex] = new GPSDataPanel ();
		gps[gpsindex].setName (name);
        addIntFrame (gps[gpsindex], "GPS: "+name);
        gpsindex++;
      }
      else
      {
        System.err.println ("Imposible añadir dispositivo. Se superaría el maximo.");
      }

    }
  }


  protected void addIntFrame (JPanel p, String title)
  {
    JInternalFrame intframe = new JInternalFrame (title,true,true,true,true);

    intframe.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    (intframe.getContentPane()).setLayout(new BorderLayout());
    (intframe.getContentPane()).add(p,BorderLayout.CENTER);
    intframe.addInternalFrameListener (new InternalFrameAdapter ()
      {
        public void internalFrameClosed (InternalFrameEvent e)
        {
          //(e.getInternalFrame()).setVisible(false);
          ((JInternalFrame)(e.getSource())).setVisible(false);
        }

      });

    add(intframe);
    intframe.pack();
    intframe.setVisible(true);
  }

  public void updateWindows (GPSData[] gpsdata, CompassData[] compassdata)
  {
    int i;

    for (i = 0; i < gpsdata.length; i++)
       gps[i].update (gpsdata[i], null);
 
    for (i = 0; i < compassdata.length; i++)
      compass[i].update (compassdata[i]);
  }
}