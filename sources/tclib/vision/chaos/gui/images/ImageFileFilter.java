package tclib.vision.chaos.gui.images;

import java.io.*;

/* ImageFilter.java is a 1.4 example used by FileChooserDemo2.java. */
public class ImageFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter 
{
	   public final static String		PNG		= "png";
	   public final static String		JPG		= "jpg";
	   public final static String		JPEG		= "jpeg";

    //Accept all directories and all gif, jpg, tiff, or png files.
    public boolean accept(File f)
    {
        if (f.isDirectory())
        {
            return true;
        }

        String extension = getExtension(f);
        if (extension != null)
        {
            if (extension.equals(PNG) || extension.equals(JPG) || extension.equals(JPEG))
                return true;
            else
                return false;
         }

        return false;
    }

    public String getExtension (File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

    //The description of this filter
    public String getDescription() {
        return "PNG/JPEG image files";
    }
}
