/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2000-2001
 * Company:      Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cánovas Quiñonero
 * @version 1.0
 */

package wucore.utils.dxf;

import java.util.ArrayList;

public class DXFPolyline
{
  private ArrayList<Integer> xs;
  private ArrayList<Integer> ys;

  public DXFPolyline()
  {
    xs = new ArrayList<Integer> ();
    ys = new ArrayList<Integer> ();
  }

  public void addPoint (int x, int y)
  {
    xs.add (Integer.valueOf (x));
    ys.add (Integer.valueOf (y));
  }

  public void addPoint (double x, double y)
  {
    addPoint ((int)Math.round (x),(int)Math.round (y));
  }

  public void addPoint (float x, float y)
  {
    addPoint (Math.round(x), Math.round(y));
  }

  public int[] getXS ()
  {
    int i;
    int arr[];

    arr = new int[xs.size()];
    for (i=0; i < xs.size(); i++)
    {
      arr[i] = xs.get (i).intValue();
    }
    return (arr);

  }

  public int[] getYS ()
  {
    int i;
    int arr[];

    arr = new int[ys.size()];
    for (i=0; i < ys.size(); i++)
    {
      arr[i] = ys.get (i).intValue();
    }
    return (arr);
  }

  public int nroVertex ()
  {
    return (xs.size());
  }


}