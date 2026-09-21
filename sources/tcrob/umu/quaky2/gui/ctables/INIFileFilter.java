package tcrob.umu.quaky2.gui.ctables;

import java.io.File;
import javax.swing.filechooser.*;

/* ImageFilter.java is a 1.4 example used by FileChooserDemo2.java. */
public class INIFileFilter extends FileFilter {

    public final static String CONFIG = "ini";

    //Accept all directories and all gif, jpg, tiff, or png files.
    public boolean accept(File f)
    {
        if (f.isDirectory())
        {
            return true;
        }

        String extension = getExtension(f);
        if (extension != null) {
            if (extension.equals(CONFIG))
            {
                    return true;
            } else {
                return false;
            }
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
        return "INI files only";
    }
}
