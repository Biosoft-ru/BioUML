package biouml.plugins.jupyter.access;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.ExceptionRegistry;

import java.io.File;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.TextDataElement;

public class IPythonElement extends TextDataElement
{
    private final String filePath;
    public IPythonElement(String name, DataCollection<?> origin, String content, String filePath)
    {
        super( name, origin, content );
        this.filePath = filePath;
    }

    public String getFilePath()
    {
        return filePath;
    }

    //TODO: maybe it will be better to use FileDataElement
    @Override
    public String getContentType()
    {
        return "text/plain";
    }

    @Override
    public DataElement clone(DataCollection parent, String name) throws CloneNotSupportedException
    {
        try
        {
            File newFile = DataCollectionUtils.getChildFile(parent, name);
            ApplicationUtils.linkOrCopyFile(newFile, new File(filePath), null);
            IPythonElement newElement = new IPythonElement(name, parent, content, newFile.getAbsolutePath());
            parent.put(newElement);
            return newElement;
        }
        catch (Exception e)
        {
            try
            {
                parent.remove(name);
            }
            catch (Exception e1)
            {
            }
            throw ExceptionRegistry.translateException(e);
        }
    }
}
