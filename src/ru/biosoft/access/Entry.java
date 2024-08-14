package ru.biosoft.access;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import org.apache.commons.io.IOUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.util.SizeLimitInputStream;


/**
 * ru.biosoft.access.core.DataElement that store his data in text-based format.
 * Used by {@link ru.biosoft.access.FileEntryCollection2}.
 * @author DevelopmentOnTheEdge
 * @version 1.0
 */
public class Entry extends DataElementSupport
{
    /** Plain text format */
    public final static String TEXT_FORMAT = "text";

    /** Text data. */
    protected String data;
    /** Format of text data: text, html, xml.
     * TODO: remove (not used)*/
    protected String format;

    private File file;
    private long offset;
    private long size;
    protected String encoding;


    /**
     * Construct data element.
     * @param pParent DataCollection which contains this data element.
     * @param pName Name of this data element.
     * @param pData Text data of this data element.
     * @param pFormat Format of text data.
     * @see ru.biosoft.access.EntryCollection
     */
    public Entry(DataCollection<?> pParent, String pName, String pData, String pFormat)
    {
        super(pName, pParent);
        data = pData;
        format = pFormat;
    }

    /**
     * Construct data element with {@link #TEXT_FORMAT}.
     * @param pParent DataCollection which contains this data element.
     * @param pName Name of this data element.
     * @param pData Text data of this data element.
     * @see ru.biosoft.access.core.DataCollection
     */
    public Entry(DataCollection<?> pParent, String pName, String pData)
    {
        this(pParent, pName, pData, TEXT_FORMAT);
    }

    /**
     * Construct big data element with {@link #TEXT_FORMAT}.
     * File f shouldnt be deleted until this object exists.
     * @param parent DataCollection which contains this data element.
     * @param name Name of this data element.
     * @param f File contained data for this entry.
     * @param o Offset of entry's data in the file.
     * @param size Size of entry's data in the file.
     * @see ru.biosoft.access.EntryCollection
     */
    public Entry(DataCollection<?> parent, String name, File pFile, long pOffset, long pSize)
    {
        this(parent, name, pFile, System.getProperty("file.encoding"), pOffset, pSize);
    }

    public Entry(DataCollection<?> parent, String name, File file, String encoding, long offset, long size)
    {
        super(name, parent);
        format = TEXT_FORMAT;
        this.file = file;
        this.offset = offset;
        this.size = size;
        this.encoding = encoding;
    }

    public Reader getReader() throws IOException
    {
        Reader reader = null;

        if( file != null )
        {
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.skip(offset);
            reader = new InputStreamReader(new SizeLimitInputStream(fileInputStream, size), encoding);

            if( reader.markSupported() )
                reader.mark((int)size);
        }
        else if( data != null )
        {
            reader = new StringReader(data);
        }

        return reader;
    }


    /**
     * @return entry data or null if this entry stored as stream
     */
    public String getData()
    {
        return data;
    }

    public String getEntryData() throws IOException
    {
        if( data != null )
            return data;
        try (Reader reader = getReader())
        {
            String result = IOUtils.toString( reader );
            return result;
        }
    }

    /**
     * @return size of entry data in bytes
     */
    public long getSize()
    {
        return size;
    }

    /**
     * Returns format of stored text data.
     * @return Format of stored text data.
     * @see #TEXT_FORMAT
     */
    final public String getFormat()
    {
        return format;
    }

    public boolean isStoredAsStream()
    {
        return data == null;
    }

}
