package ru.biosoft.bsa.track;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;

public class BedTrack extends DataElementSupport implements WritableTrack
{
    private File file;
    private BufferedWriter writer;
    private int nextId = 1;

    public BedTrack(DataCollection<?> origin, Properties properties)
    {
        super( properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY, "null"), origin );
        file = DataCollectionUtils.getChildFile( origin, getName() );
    }
    
    public BedTrack(DataCollection<?> origin, String name, File file)
    {
        super( name, origin );
        this.file = file;
    }
    
    public File getFile()
    {
        return file;
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataCollection<Site> getAllSites() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return null;
    }

    @Override
    public void addSite(Site site) throws LoggedException
    {
        try
        {
            initWriter();
            Sequence chr = site.getOriginalSequence();
            
            char strand;
            switch (site.getStrand())
            {
                case StrandType.STRAND_PLUS: strand = '+'; break;
                case StrandType.STRAND_MINUS: strand = '-'; break;
                default: strand = '.';
            }
            String siteName = site.getName();
            if(siteName == null)
                siteName = String.valueOf(nextId++); 

            writer.append( site.getSequence().getName() )
            .append( '\t' )
            .append( String.valueOf(site.getFrom() - chr.getStart()) )//zero based inclusive
            .append('\t')
            .append( String.valueOf(site.getTo() - chr.getStart() + 1) )//zero based exclusive;
            .append('\t')
            .append( siteName )
            .append( '\t' )
            .append( String.valueOf(site.getScore()) )
            .append('\t')
            .append(strand)
            .append( '\n' );
        }
        catch( IOException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    private void initWriter() throws IOException
    {
        if(writer == null)
            writer = new BufferedWriter( new FileWriter( file, true ) );

    }

    @Override
    public void finalizeAddition() throws LoggedException
    {
        try
        {
            initWriter();
            writer.flush();
        }
        catch( IOException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }
}
