/** 
 * Utils.Filters.DataFilter
 *
 * Description:	
 * @author			David Herrero Pérez
 * @version			1.0		
 */
 
package  wucore.utils.filters;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class DataFilter extends FileFilter {

    private final static String dat = "dat";

    // Accept all directories and dat files.
    public boolean accept(File f) {
    	if (f.isDirectory()) 
        	return true;

        String extension = getExtension(f);
        
		if (extension != null)
            if (extension.equals(dat))
               return true;
            else
               return false;

        return false;
    }
    
    // The description of this filter
    public String getDescription() {
        return "*.dat";
    }

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
}