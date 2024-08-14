
package ru.biosoft.access;

import java.io.File;
import java.io.IOException;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.jobcontrol.JobControl;

/**
 * @author anna
 *
 */
public class HtmlFileImporter extends FileImporter
{
    @Override
    public int accept(DataCollection parent, File file)
    {
        if( file == null || isTextFile( file ) && ( file.getName().toLowerCase().endsWith( ".html" )
                || file.getName().toLowerCase().endsWith( ".htm" ) || file.getName().toLowerCase().endsWith( ".svg" ) ) )
            return super.accept(parent, file) == ACCEPT_UNSUPPORTED ? ACCEPT_UNSUPPORTED : ACCEPT_HIGH_PRIORITY;
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return HtmlDataElement.class;
    }

    @Override
    protected DataElement createElement(DataCollection parent, String name, File file, JobControl jobControl) throws IOException
    {
        String data = ApplicationUtils.readAsString( file );
        return new HtmlDataElement( name, parent, data );
    }
}
