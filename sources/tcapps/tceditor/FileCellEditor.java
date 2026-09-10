/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Cell editor (and matching renderer) for file-path properties: a text field
 * plus a "..." button that opens a file chooser. Chosen paths are stored
 * relative to the working directory ("./conf/3dmodels/foo.3ds") when possible,
 * which is how .world files reference resources.
 */
public class FileCellEditor extends AbstractCellEditor implements TableCellEditor
{
	private static final long		serialVersionUID = 1L;

	/** 3D shapes (3D Studio files) */
	static public final FileCellEditor	SHAPE	= new FileCellEditor ("Select 3D shape", "./conf/3dmodels",
																	new FileNameExtensionFilter ("3D Studio objects (*.3ds)", "3ds"));
	/** Textures (images) */
	static public final FileCellEditor	TEXTURE	= new FileCellEditor ("Select texture", "./conf/3dmodels/textures",
																	new FileNameExtensionFilter ("Images (*.jpg, *.gif, *.png)", "jpg", "jpeg", "gif", "png"));

	static private final int		BUTTON_W	= 20;		// width of the "..." button (px)

	/** A compact "..." button: no margins, thin border, fixed width, also under the macOS look and feel. */
	static JButton createButton ()
	{
		JButton		b = new JButton ("\u2026");						// single-character ellipsis
		b.setMargin (new Insets (0, 0, 0, 0));
		b.setFocusable (false);
		b.setFocusPainted (false);
		b.setContentAreaFilled (true);
		b.setBorder (javax.swing.BorderFactory.createLineBorder (new java.awt.Color (150, 150, 150)));
		b.setFont (b.getFont ().deriveFont (java.awt.Font.BOLD, 11f));
		b.setHorizontalAlignment (javax.swing.SwingConstants.CENTER);
		b.setPreferredSize (new Dimension (BUTTON_W, 18));
		b.setMinimumSize (new Dimension (BUTTON_W, 12));
		b.setMaximumSize (new Dimension (BUTTON_W, 40));
		b.putClientProperty ("JButton.buttonType", "square");			// Aqua: no rounded, oversized bezel
		b.putClientProperty ("JComponent.sizeVariant", "mini");
		return b;
	}

	protected JPanel				panel;
	protected JTextField			field;
	protected JButton				button;
	protected String				title;
	protected String				defaultDir;
	protected FileNameExtensionFilter	filter;

	public FileCellEditor (String title, String defaultDir, FileNameExtensionFilter filter)
	{
		this.title		= title;
		this.defaultDir	= defaultDir;
		this.filter		= filter;

		field	= new JTextField ();
		field.setBorder (null);
		field.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ stopCellEditing (); }
		});

		button	= createButton ();
		button.setToolTipText ("Choose a file...");
		button.addActionListener (new ActionListener ()
		{
			public void actionPerformed (ActionEvent e)		{ browse (); }
		});

		panel	= new JPanel (new BorderLayout ());
		panel.add (field, BorderLayout.CENTER);
		panel.add (button, BorderLayout.EAST);
	}

	/* --- TableCellEditor */

	public Component getTableCellEditorComponent (JTable table, Object value, boolean isSelected, int row, int column)
	{
		field.setText ((value == null) ? "" : value.toString ());
		field.setFont (table.getFont ());
		button.setPreferredSize (new Dimension (BUTTON_W, table.getRowHeight (row) - 2));
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

	/* --- file chooser */

	protected void browse ()
	{
		File			start = resolve (field.getText ());
		JFileChooser	fc = new JFileChooser ((start != null) ? start.getParentFile () : startDir ());
		fc.setDialogTitle (title);
		fc.setFileFilter (filter);
		fc.setAcceptAllFileFilterUsed (true);
		if (start != null)		fc.setSelectedFile (start);

		if (fc.showOpenDialog (panel) == JFileChooser.APPROVE_OPTION)
		{
			field.setText (relativize (fc.getSelectedFile ()));
			stopCellEditing ();
		}
	}

	private File startDir ()
	{
		File	d = new File (defaultDir);
		if (d.isDirectory ())		return d;
		d = new File ("./conf");
		return d.isDirectory () ? d : new File (".");
	}

	/** Existing file referenced by the current text, or null. */
	static private File resolve (String text)
	{
		if ((text == null) || (text.trim ().length () == 0))		return null;
		File	f = new File (text.trim ());
		return f.isFile () ? f.getAbsoluteFile () : null;
	}

	/** "./rel/path" when the file lives under the working directory, absolute path otherwise. */
	static public String relativize (File f)
	{
		try
		{
			String	base = new File (".").getCanonicalPath ();
			String	path = f.getCanonicalPath ();
			if (path.startsWith (base + File.separator))
				return "./" + path.substring (base.length () + 1).replace (File.separatorChar, '/');
			return path.replace (File.separatorChar, '/');
		} catch (Exception e)
		{
			return f.getPath ().replace (File.separatorChar, '/');
		}
	}

	/* --- renderer showing the same look (text + "..." button) when not editing */

	static public class Renderer extends JPanel implements TableCellRenderer
	{
		private static final long	serialVersionUID = 1L;
		protected JLabel	label	= new JLabel ();
		protected JButton	button	= createButton ();

		public Renderer ()
		{
			super (new BorderLayout ());
			label.setBorder (null);
			add (label, BorderLayout.CENTER);
			add (button, BorderLayout.EAST);
		}

		public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			label.setText ((value == null) ? "" : value.toString ());
			label.setFont (table.getFont ());
			button.setPreferredSize (new Dimension (BUTTON_W, table.getRowHeight (row) - 2));
			label.setToolTipText (label.getText ());
			setBackground (isSelected ? table.getSelectionBackground () : table.getBackground ());
			label.setForeground (isSelected ? table.getSelectionForeground () : table.getForeground ());
			label.setOpaque (false);
			setOpaque (true);
			return this;
		}
	}
}
