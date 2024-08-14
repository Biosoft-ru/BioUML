// $ Id: $
package biouml.plugins.biopax;

import java.io.File;
import java.text.MessageFormat;

import javax.swing.filechooser.FileFilter;
import one.util.streamex.StreamEx;

public class BioPAXFileFilter extends FileFilter
{
    public static final String OWL_EXTENTION = ".owl";

    private static final String[] exts = new String[] {OWL_EXTENTION};
    @Override
    public boolean accept(File f)
    {
        if( f.isDirectory() )
        {
            return true;
        }
        else if( f.isFile() )
        {
            return StreamEx.of( exts ).anyMatch( f.getName().toLowerCase()::endsWith );
        }
        return false;
    }

    @Override
    public String getDescription()
    {
        return MessageFormat.format("BioPAX OWL file (*{0})", (Object[])exts);
    }
}
