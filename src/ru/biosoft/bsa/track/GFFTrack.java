package ru.biosoft.bsa.track;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.importer.GFFTrackImporter;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.exception.LoggedException;

public class GFFTrack extends DataElementSupport implements WritableTrack
{
    private File file;
    private boolean isInitialized = false;
    VectorDataCollection<Site> track;

    public GFFTrack(DataCollection<?> origin, Properties properties)
    {
        super( properties.getProperty( DataCollectionConfigConstants.NAME_PROPERTY, "null" ), origin );
        file = DataCollectionUtils.getChildFile( origin, getName() );
    }

    public GFFTrack(DataCollection<?> origin, String name, File file)
    {
        super( name, origin );
        this.file = file;
    }

    private void init()
    {
        if( isInitialized )
            return;
        synchronized( this )
        {
            if( isInitialized )
                return;
            track = new VectorDataCollection<>( getName(), Site.class, null );
            readFromFile( file, track );
            isInitialized = true;
        }
    }

    private void readFromFile(File trackFile, DataCollection<Site> track)
    {
        int i = 1;
        try (FileInputStream is = new FileInputStream( trackFile );
                BufferedReader input = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) );
                FileChannel ch = is.getChannel())
        {
            String line;
            while( ( line = input.readLine() ) != null )
            {
                if( isComment( line ) )
                    continue;
                Site site = GFFTrackImporter.parseGFFLine( line );
                if( site == null )
                {
                    continue;
                }
                if( site.getOriginalSequence() == null )
                {
                    site = new SiteImpl( site.getOrigin(), i + "", site.getType(), site.getBasis(), site.getStart(), site.getLength(),
                            site.getPrecision(), site.getStrand(), getSequence( site.getName() ), site.getComment(), site.getProperties() );
                }
                track.put( site );
                i++;
            }
        }

        catch( Exception e )
        {
            //TODO:
        }
    }

    private final Map<String, Sequence> sequenceCache = new HashMap<>();

    private Sequence getSequence(String name)
    {
        Sequence result = sequenceCache.get( name );
        if( result != null )
            return result;

        result = new LinearSequence( name, new byte[0], Nucleotide15LetterAlphabet.getInstance() );
        sequenceCache.put( name, result );

        return result;
    }

    private boolean isComment(String line)
    {
        return line.startsWith( "track " ) || line.startsWith( "browser " ) || line.startsWith( "#" );
    }

    public File getFile()
    {
        return file;
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        init();
        VectorDataCollection<Site> result = new VectorDataCollection<>( "sites" );
        String sequenceName = DataElementPath.create( sequence ).getName();
        track.stream().filter( s -> s.getSequence().getName().equals( sequenceName ) && s.getFrom() >= from && s.getTo() <= to )
                .forEach( s -> result.put( s ) );
        return result;
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        init();
        return getSites( sequence, from, to ).getSize();
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        init();
        return track.get( siteName );
    }

    @Override
    public DataCollection<Site> getAllSites() throws UnsupportedOperationException
    {
        init();
        return track;
    }

    TrackViewBuilder viewBuilder = new DefaultTrackViewBuilder();
    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    @Override
    public void addSite(Site site) throws LoggedException
    {
        throw new UnsupportedOperationException();

    }

    @Override
    public void finalizeAddition() throws LoggedException
    {
        throw new UnsupportedOperationException();
    }

}
