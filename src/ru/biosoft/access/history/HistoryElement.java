package ru.biosoft.access.history;

import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.Date;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementSupport;

/**
 * Element of {@link HistoryDataCollection}. Contains transaction or something else.
 */
public class HistoryElement extends DataElementSupport
{
    public static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";
    protected static final Logger log = Logger.getLogger(HistoryElement.class.getName());

    public static enum Type
    {
        CHANGES, OBJECT;

        public static Type fromString(String str)
        {
            return "changes".equals(str) ? CHANGES : OBJECT;
        }

        @Override
        public String toString()
        {
            return this == CHANGES ? "changes" : "objects";
        }
    }

    protected DataElementPath dePath;
    protected Date timestamp;
    protected int version;
    protected String author;
    protected String comment;
    protected Type type;

    protected String data;//serialized version of object

    public HistoryElement(DataCollection origin, String name)
    {
        super(name, origin);
    }

    //
    // Getters and setters
    //
    
    public DataElementPath getDePath()
    {
        return dePath;
    }
    
    public void setDePath(DataElementPath dePath)
    {
        this.dePath = dePath;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public String getTimestampFormated()
    {
        return new SimpleDateFormat(DATE_FORMAT).format(timestamp);
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public String getData()
    {
        return data;
    }

    public void setData(String data)
    {
        this.data = data;
        this.dataObj = null;
    }

    //
    // Object parsing
    //

    protected Object dataObj;//parsed version of 'data'
    public Object getDataObj(DataElement de, DiffManager diffManager)
    {
        if( dataObj == null )
        {
            try
            {
                dataObj = diffManager.parseDifference(de, this);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot parse history element", e);
            }
        }
        return dataObj;
    }
}
