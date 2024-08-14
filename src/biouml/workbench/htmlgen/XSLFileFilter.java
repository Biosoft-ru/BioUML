package biouml.workbench.htmlgen;

import java.io.File;
import java.text.MessageFormat;

import javax.swing.filechooser.FileFilter;

import biouml.workbench.BioUMLApplication;

public class XSLFileFilter extends FileFilter
{
    public static final String EXTENTION = ".xsl";

    @Override
    public boolean accept( File f )
    {
        if ( f.isDirectory() )
        {
            return true;
        }
        else if ( f.isFile() )
        {
            return f.getName().toLowerCase().endsWith( EXTENTION );
        }
        return false;
    }

    @Override
    public String getDescription()
    {
        return MessageFormat.format(
                BioUMLApplication.getMessageBundle().getResourceString( "TEMPLATE_FILE_DESCRIPTION" ),
                new Object[]{EXTENTION} );
    }
}
