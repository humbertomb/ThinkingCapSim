/*
 * (c) 2003 Humberto Martinez
 */

package devices.pos;

import java.io.File;

public class PathFileFilter extends javax.swing.filechooser.FileFilter 
{
	// Accessors
    public String		getDescription ()		{ return "PATH File Format"; }
	
	// Class methods
    
    public static String getExtension (File f) 
    {
    	int				i;
        String			ext = null;
        String 			name;
        
        name	= f.getName ();
        i		= name.lastIndexOf ('.');

        if ((i > 0) && (i < name.length () - 1))
            ext		= name.substring (i).toLowerCase ();

        return ext;
    }

	// Instance methods
    public boolean accept (File f) 
    {
		String			suffix; 
         
		if (f.isDirectory ()) 
        	return true;

		suffix = getExtension (f);
		if ((suffix != null) && (suffix.equals (Path.SUFFIX)))
			return true;
			
		return false;
	}
}