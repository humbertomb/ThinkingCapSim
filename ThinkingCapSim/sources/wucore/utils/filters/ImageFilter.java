/** 
 * Utils.Filters.ImageFilter
 *
 * Description:	
 * @author			David Herrero Pérez
 * @version			1.0		
 */
 
package  wucore.utils.filters;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class ImageFilter extends FileFilter {

    private final static String jpeg = "jpeg";
    private final static String jpg = "jpg";
    private final static String gif = "gif";
    private final static String tiff = "tiff";
    private final static String tif = "tif";

    
    // Accept all directories and all gif, jpg, jpeg, or tiff files.
    public boolean accept(File f) {
    	if (f.isDirectory()) 
        	return true;

        String extension = getExtension(f);
        
		if (extension != null) {
            if (extension.equals(tiff) || 
            	extension.equals(tif)  || 
            	extension.equals(gif)  ||
                extension.equals(jpeg) ||
                extension.equals(jpg)) 
            {
                    return true;
            } else {
                return false;
            }
    	}

        return false;
    }
    
    // The description of this filter
    public String getDescription() {
        return "Image Files";
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