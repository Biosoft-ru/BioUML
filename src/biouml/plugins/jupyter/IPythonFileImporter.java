package biouml.plugins.jupyter;

import java.io.File;
import java.io.IOException;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.jupyter.access.IPythonElement;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.TextFileImporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.jobcontrol.JobControl;

public class IPythonFileImporter extends TextFileImporter
{
    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable( parent, TextDataElement.class ) )
            return ACCEPT_UNSUPPORTED;
        if( file == null )
            return ACCEPT_LOW_PRIORITY;
        if( isTextFile( file ) && file.getName().toLowerCase().endsWith( ".ipynb" ) )
            return ACCEPT_HIGH_PRIORITY;
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public Class<? extends IPythonElement> getResultType()
    {
        return IPythonElement.class;
    }

    @Override
    protected DataElement createElement(DataCollection parent, String name, File file, JobControl jobControl) throws IOException
    {
        File result = copyFileToRepository( parent, name, file, jobControl );
        String data = ApplicationUtils.readAsString( file );
        return new IPythonElement( name, parent, data, result.getAbsolutePath() );
    }
}
