/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2000-2001
 * Company:      Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

package devices.gui;

import javax.swing.*;
import java.awt.*;

public class HeadCanvas extends JPanel
{

  static double G2R = Math.PI/180.0;

  double head = 0*G2R;

  public HeadCanvas()
  {
    try
    {
      jbInit();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception
  {
    this.setPreferredSize(new Dimension(110, 110));
    this.setBackground(Color.white);
  }

  public void setHeading (double h)
  {
    head = h*G2R;
    repaint();
  }

  public void paint(Graphics g)
  {
    int h = getHeight();
    int w = getWidth ();
    int mh = h/2;
    int mw = w/2;
    int r = (h-10)/2;


    g.setColor(Color.white);
    g.fillRect(0,0,w,h);
    g.setColor(Color.black);
    g.drawOval(5,5,h-10,h-10);
    g.fillOval(mw-2,mh-2,5,5);
    g.drawString("N",mw-2,15);
    g.drawString("S",mw-2,h-8);
    g.drawString("E",h-15,mh);
    g.drawString("W",8,mh);

    g.setColor (Color.red);


    int xlimit = (int)(mw-(r*Math.sin(head)));
    int ylimit = (int)(mh-(r*Math.cos(head)));

    g.drawLine (mw-2,mh-2,xlimit,ylimit);
    g.drawLine (mw+2,mh+2,xlimit,ylimit);
  }
}