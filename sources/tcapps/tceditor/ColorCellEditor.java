/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import wucore.utils.color.WColor;

/**
 * Cell editor (and renderer) for colour properties: a colour swatch, a text
 * field with the classic hexadecimal notation (#f045a7) and a compact "..."
 * button that opens the Swing colour chooser.
 */
public class ColorCellEditor extends AbstractCellEditor implements TableCellEditor
{
	private static final long		serialVersionUID = 1L;

	static public final ColorCellEditor		INSTANCE = new ColorCellEditor ();

	protected JPanel				panel;
	protected Swatch				swatch;
	protected JTextField			field;
	protected JButton				button;

	public ColorCellEditor ()
	{
		swatch	= new Swatch ();

		field	= new JTextField ();
		field.setBorder (null);
		field.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ stopCellEditing (); }
		});
		// live preview of what is being typed
		field.getDocument ().addDocumentListener (new DocumentListener ()
		{
			public void insertUpdate (DocumentEvent e)		{ swatch.setColor (parse (field.getText ())); }
			public void removeUpdate (DocumentEvent e)		{ swatch.setColor (parse (field.getText ())); }
			public void changedUpdate (DocumentEvent e)		{ swatch.setColor (parse (field.getText ())); }
		});

		button	= FileCellEditor.createButton ();
		button.setToolTipText ("Choose a colour...");
		button.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ choose (); }
		});

		panel	= new JPanel (new BorderLayout (2, 0));
		panel.add (swatch, BorderLayout.WEST);
		panel.add (field, BorderLayout.CENTER);
		panel.add (button, BorderLayout.EAST);
	}

	/* --- TableCellEditor */

	public Component getTableCellEditorComponent (JTable table, Object value, boolean isSelected, int row, int column)
	{
		field.setText ((value == null) ? "" : value.toString ());
		field.setFont (table.getFont ());
		swatch.setColor (parse (field.getText ()));
		int		h = table.getRowHeight (row) - 2;
		button.setPreferredSize (new Dimension (button.getPreferredSize ().width, h));
		swatch.setPreferredSize (new Dimension (h, h));
		SwingUtilities.invokeLater (new Runnable ()
		{
			public void run ()		{ field.requestFocusInWindow (); field.selectAll (); }
		});
		return panel;
	}

	public Object getCellEditorValue ()
	{
		return field.getText ();
	}

	protected void choose ()
	{
		Color	initial = parse (field.getText ());
		Color	c = JColorChooser.showDialog (panel, "Select colour", (initial != null) ? initial : Color.GRAY);
		if (c != null)
		{
			field.setText (hex (c));
			stopCellEditing ();
		}
	}

	/* --- helpers */

	static public String hex (Color c)
	{
		return String.format ("#%02x%02x%02x", c.getRed (), c.getGreen (), c.getBlue ());
	}

	/** AWT colour for a property value, or null if it is not (yet) a valid colour. */
	static public Color parse (String text)
	{
		try
		{
			WColor	w = WorldEdit.parseColor (text);
			return new Color (w.getRed (), w.getGreen (), w.getBlue ());
		} catch (Exception e)
		{
			return null;
		}
	}

	/** Small square filled with the colour (hatched when the colour is invalid). */
	static public class Swatch extends JComponent
	{
		private static final long	serialVersionUID = 1L;
		protected Color		color;

		public Swatch ()
		{
			setPreferredSize (new Dimension (18, 18));
			setBorder (BorderFactory.createLineBorder (new Color (150, 150, 150)));
		}

		public void setColor (Color c)		{ color = c; repaint (); }

		protected void paintComponent (Graphics g)
		{
			int		w = getWidth (), h = getHeight ();
			if (color != null)
			{
				g.setColor (color);
				g.fillRect (1, 1, w - 2, h - 2);
			}
			else
			{
				g.setColor (Color.WHITE);
				g.fillRect (1, 1, w - 2, h - 2);
				g.setColor (Color.RED);
				g.drawLine (2, 2, w - 3, h - 3);
				g.drawLine (w - 3, 2, 2, h - 3);
			}
		}
	}

	/* --- renderer: swatch + hex text + "..." button */

	static public class Renderer extends JPanel implements TableCellRenderer
	{
		private static final long	serialVersionUID = 1L;
		protected Swatch	swatch	= new Swatch ();
		protected JLabel	label	= new JLabel ();
		protected JButton	button	= FileCellEditor.createButton ();

		public Renderer ()
		{
			super (new BorderLayout (2, 0));
			add (swatch, BorderLayout.WEST);
			add (label, BorderLayout.CENTER);
			add (button, BorderLayout.EAST);
		}

		public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			String	text = (value == null) ? "" : value.toString ();
			int		h = table.getRowHeight (row) - 2;
			label.setText (text);
			label.setFont (table.getFont ());
			swatch.setColor (parse (text));
			swatch.setPreferredSize (new Dimension (h, h));
			button.setPreferredSize (new Dimension (button.getPreferredSize ().width, h));
			setBackground (isSelected ? table.getSelectionBackground () : table.getBackground ());
			label.setForeground (isSelected ? table.getSelectionForeground () : table.getForeground ());
			setOpaque (true);
			return this;
		}
	}
}
