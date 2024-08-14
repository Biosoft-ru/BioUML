package ru.biosoft.bsastats;

import java.io.File;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.bsastats.ProcessTasks.FastqSiteWriter;

import com.developmentontheedge.beans.annot.ExpertProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("Remove too short")
@PropertyDescription("Remove reads shorter then specified length")
@ExpertProperty
public class FilterByLength extends TaskProcessor
{
    private int minLength = 10, maxLength = 1000;
    
    private DataElementPath shortSequencesPath, longSequencesPath;
    private FileDataElement shortSequencesFile, longSequencesFile;
    private FastqSiteWriter shortSequencesWriter, longSequencesWriter;
    
    @PropertyName ( "Minimal read length" )
    @PropertyDescription ( "Reads shorter than given length will be excluded" )
    public int getMinLength()
    {
        return minLength;
    }

    public void setMinLength(int minLength)
    {
        Object oldValue = this.minLength;
        this.minLength = minLength;
        firePropertyChange( "minLength", oldValue, minLength );
    }

    @PropertyName( "Maximal read length" )
    @PropertyDescription( "Reads longer than given length will be excluded" )
    public int getMaxLength()
    {
        return maxLength;
    }

    public void setMaxLength(int maxLength)
    {
        Object oldValue = this.maxLength;
        this.maxLength = maxLength;
        firePropertyChange( "maxLength", oldValue, maxLength );
    }

    @PropertyName( "Short sequences path" )
    @PropertyDescription( "Path to save short sequences" )
    public DataElementPath getShortSequencesPath()
    {
        return shortSequencesPath;
    }

    public void setShortSequencesPath(DataElementPath shortSequencesPath)
    {
        Object oldValue = this.shortSequencesPath;
        this.shortSequencesPath = shortSequencesPath;
        firePropertyChange( "shortSeuencesPath", oldValue, shortSequencesPath );
    }

    @PropertyName( "Long sequences path" )
    @PropertyDescription( "Path to save long sequences" )
    public DataElementPath getLongSequencesPath()
    {
        return longSequencesPath;
    }

    public void setLongSequencesPath(DataElementPath longSequencesPath)
    {
        Object oldValue = this.longSequencesPath;
        this.longSequencesPath = longSequencesPath;
        firePropertyChange( "longSequencesPath", oldValue, longSequencesPath );
    }

    @Override
    public Task process(Task task)
    {
        if(task.getSequence().length < getMinLength())
        {
            if(getShortSequencesPath() != null)
            {
                try
                {
                    if( shortSequencesWriter == null )
                    {
                        File file = ProcessTasks.getFileForPath( getShortSequencesPath() );
                        shortSequencesFile = new FileDataElement( getShortSequencesPath().getName(), getShortSequencesPath().optParentCollection(), file );
                        shortSequencesWriter = new FastqSiteWriter( file, (byte)33);
                    }
                    shortSequencesWriter.write( task );
                }
                catch( Exception e )
                {
                }
            }
            return null;
        } else if(task.getSequence().length > getMaxLength())
        {
            if(getLongSequencesPath() != null)
            {
                try
                {
                    if(longSequencesWriter == null)
                    {
                        File file = ProcessTasks.getFileForPath( getLongSequencesPath() );
                        longSequencesFile = new FileDataElement( getLongSequencesPath().getName(), getLongSequencesPath().optParentCollection(), file );
                        longSequencesWriter = new FastqSiteWriter( file, (byte)33);
                    }
                    longSequencesWriter.write( task );
                }
                catch( Exception e )
                {
                }
            }
            return null;
        }
        return task;
    }
    
    @Override
    public void finalizeProcessing() throws Exception
    {
        if(shortSequencesWriter != null)
        {
            shortSequencesWriter.close();
            getShortSequencesPath().save( shortSequencesFile );
        }
        if(longSequencesWriter != null)
        {
            longSequencesWriter.close();
            getLongSequencesPath().save( longSequencesFile );
        }
        
    }

}
