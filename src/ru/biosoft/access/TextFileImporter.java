
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
public class TextFileImporter extends FileImporter
{
    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable(parent, TextDataElement.class) )
            return ACCEPT_UNSUPPORTED;
        if( file == null )
            return ACCEPT_HIGH_PRIORITY;
        if( isTextFile(file) )
        {
            /*if( super.accept(parent, file) == ACCEPT_UNSUPPORTED )
                return ACCEPT_UNSUPPORTED;*/
            if( file.getName().toLowerCase().endsWith(".txt") )
                return ACCEPT_LOW_PRIORITY;
            if( file.getName().endsWith(".log") || file.getName().endsWith(".info") )
                return ACCEPT_HIGH_PRIORITY;
            return ACCEPT_LOWEST_PRIORITY;
        }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return TextDataElement.class;
    }
    
    @Override
    protected DataElement createElement(DataCollection parent, String name, File file, JobControl jobControl) throws IOException
    {
        String data = ApplicationUtils.readAsString(file);
        return new TextDataElement(name, parent, data);
    }
}
