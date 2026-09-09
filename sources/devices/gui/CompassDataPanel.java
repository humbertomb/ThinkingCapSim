/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2000-2001
 * Company:      Grupo ANTS - Proyecto MIMICS
 * @author Juan Pedro Cnovas Quionero
 * @version 1.0
 */

package devices.gui;

import java.awt.*;
import javax.swing.*;

import wucore.utils.math.Angles;

import devices.data.*;


public class CompassDataPanel extends JPanel
{
  static public final int		DIGITS		= 3;

  GridBagLayout gridBagLayout1 = new GridBagLayout();
  HeadCanvas HeadInfo = new HeadCanvas();
  JPanel TextInfoPanel = new JPanel();
  JLabel RollLabel = new JLabel();
  JLabel PitchLabel = new JLabel();
  JLabel TempLabel = new JLabel();
  JLabel CompassText = new JLabel();
  JLabel RollText = new JLabel();
  JLabel PitchText = new JLabel();
  JLabel TempText = new JLabel();
  JLabel HeadLabel = new JLabel();
  JLabel HeadText = new JLabel();

  String myname;



  public CompassDataPanel (String name)
  {
    myname = name;

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
    this.setLayout(gridBagLayout1);
    this.setPreferredSize(new Dimension(300, 100));
    HeadInfo.setPreferredSize(new Dimension (100,100));
    TextInfoPanel.setPreferredSize(new Dimension(200,100));
    TextInfoPanel.setLayout(null);

    RollLabel.setText("Roll:");
    RollLabel.setBounds(new Rectangle(8, 43, 34, 13));
    PitchLabel.setText("Pitch:");
    PitchLabel.setBounds(new Rectangle(8, 61, 41, 17));
    TempLabel.setText("Temp:");
    TempLabel.setBounds(new Rectangle(6, 82, 45, 15));
    HeadLabel.setText("Heading:");
    HeadLabel.setBounds(new Rectangle(7, 23, 62, 14));

    CompassText.setText(myname);
    CompassText.setBounds(new Rectangle(5, 1, 212, 15));
    CompassText.setFont(new java.awt.Font("Dialog", 3, 12));

    RollText.setText("N/A");
    RollText.setBounds(new Rectangle(49, 43, 121, 13));
    PitchText.setText("N/A");
    PitchText.setBounds(new Rectangle(51, 63, 122, 13));
    TempText.setText("N/A");
    TempText.setBounds(new Rectangle(55, 80, 71, 19));
    TempText.setFont(new java.awt.Font("Dialog", 1, 14));
    HeadText.setText("N/A");
    HeadText.setBounds(new Rectangle(76, 23, 107, 14));

    this.add(HeadInfo, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 100, 100));
    this.add(TextInfoPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 200, 100));

    TextInfoPanel.add(CompassText, null);
    TextInfoPanel.add(RollLabel, null);
    TextInfoPanel.add(RollText, null);
    TextInfoPanel.add(HeadLabel, null);
    TextInfoPanel.add(HeadText, null);
    TextInfoPanel.add(PitchLabel, null);
    TextInfoPanel.add(PitchText, null);
    TextInfoPanel.add(TempLabel, null);
    TextInfoPanel.add(TempText, null);
  }


  public void update (CompassData data)
  {
    double head;

    try
    {
      head = data.getHeading() *Angles.RTOD;
      HeadText.setText(""+format (head, DIGITS));
      HeadInfo.setHeading(head);
      PitchText.setText(""+format (data.getPitch()*Angles.RTOD, DIGITS));
      RollText.setText("" +format (data.getRoll()*Angles.RTOD, DIGITS));
      TempText.setText(""+data.getTemp()+"");
    } catch (NullPointerException npe)
    {
      return;
    }
    catch (Exception e)
    {
        System.out.println("Compass2-Actualizer: Excepcion en la toma de datos: "+e);
    }
  }

	static private String format (double value, int dec)
	{
		int			i;
		String		str;
		double		times;
		
		// Rounding
		for (i = 0, times = 1.0; i < dec; i++)
			times /= 10.0;
		value	= value + 0.5 * times;
		if ((value > -times) && (value < times))
			value = 0.0;
		
		// String adjusting
		str		= Double.toString (value);
		while (str.length () - str.indexOf ('.') > dec + 1)
			str		= str.substring (0, str.length () - 1);
		while (str.length () - str.indexOf ('.') < dec + 1)
			str		= str + "0";

		return str;
	}

  
  public boolean isName (String str)
  {
    return (myname.equalsIgnoreCase(str));
  }

}