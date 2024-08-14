package ru.biosoft.bpmn;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.TextFileImporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

public class BPMNDiagramImporter extends TextFileImporter
{

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable( parent, BPMNDataElement.class ) )
            return ACCEPT_UNSUPPORTED;
        if( file == null )
            return ACCEPT_HIGH_PRIORITY;
        if( isTextFile( file ) )
        {
            if( file.getName().toLowerCase().endsWith( ".bpmn" ) )
                return ACCEPT_HIGH_PRIORITY;
            return ACCEPT_LOWEST_PRIORITY;
        }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return BPMNDataElement.class;
    }

    @Override
    protected DataElement createElement(DataCollection parent, String name, File file, JobControl jobControl) throws IOException
    {
        copyFileToRepository( parent, name, file, jobControl );
        String data = ApplicationUtils.readAsString( file );
        return new BPMNDataElement( name, parent, data );
    }
}
