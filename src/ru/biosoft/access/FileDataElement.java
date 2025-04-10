package ru.biosoft.access;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.TextUtil2;

import com.developmentontheedge.beans.annot.PropertyName;

import com.developmentontheedge.application.ApplicationUtils;

/**
 *  FileDataElement is ru.biosoft.access.core.DataElement which wraps the File object.
 *  This class is used for storing of objects that have file representation.
 * @author DevelopmentOnTheEdge
 * @version 1.0
 */
@PropertyName("file")
@ClassIcon("resources/leaf.gif")
public class FileDataElement extends DataElementSupport implements CloneableDataElement
{
    /** File stored in this FileDataElement */
    protected File file;

    /**
     * Constructs  FileDataElement with specified name,parent ru.biosoft.access.core.DataCollection,and parent subdirectory
     *
     * @param name name of ru.biosoft.access.core.DataElement
     * @param origin  parent ru.biosoft.access.core.DataCollection
     * @param file File object to wrap
     */
    public FileDataElement(String name, DataCollection origin, File file)
    {
        super(name,origin);
        this.file = file;
    }

    /**
     * Constructs  FileDataElement with specified name and parent ru.biosoft.access.core.DataCollection
     *
     * @param name   name of ru.biosoft.access.core.DataElement
     * @param origin parent ru.biosoft.access.core.DataCollection
     */
    public FileDataElement(String name, FileBasedCollection<?> origin)
    {
        this(name,origin,origin.getChildFile( name ));
    }
    /**
     * Return file of this ru.biosoft.access.core.DataElement
     *
     * @return file of this ru.biosoft.access.core.DataElement
     */
    public @Nonnull File getFile()
    {
        return file;
    }

    public long getContentLength()
    {
        return file.length();
    }

    public String getReadableContentLength()
    {
        return TextUtil.formatSize( getContentLength() );
    }

    /**
     * Sets new  file.
     *
     * @param file new file
     * @exception Exception If name of ru.biosoft.access.core.DataElement does not equal name of new file.
     */
    public void setFile(File file) throws  Exception
    {
        if(!this.file.getName().equals(file.getName()))
            throw new Exception("Name should be equal: \""+this.file.getName()+"\" != \""+file.getName()+"\"");

        this.file = file;
    }

    @Override
    public DataElement clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        FileDataElement result = (FileDataElement)super.clone(origin, name);
        result.file = DataCollectionUtils.getChildFile(origin, name);
        if(!result.file.equals(file))
        {
            try
            {
                ApplicationUtils.linkOrCopyFile(result.file, file, null);
            }
            catch( IOException e )
            {
                throw new RuntimeException("Cannot copy file "+file+" to "+result.file, e);
            }
        }
        return result;
    }
}
