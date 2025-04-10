package ru.biosoft.bsa.track;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.FileTrack;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.importer.GFFTrackImporter;

public class GFFTrack extends FileTrack
{

    public GFFTrack(DataCollection<?> origin, Properties properties) throws IOException
    {
        super( origin, properties );
    }

    @Override
    protected void readFromFile(File trackFile, DataCollection<Site> track)
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
                Site site = GFFTrackImporter.parseGFFLine( line, false );
                if( site == null )
                {
                    continue;
                }
                if( site.getOriginalSequence() == null )
                {
                    Sequence seq = getSequence( site.getName() );
                    site = new SiteImpl( site.getOrigin(), i + "", site.getType(), site.getBasis(), site.getStart(), site.getLength(),
                            site.getPrecision(), site.getStrand(), seq, site.getComment(), site.getProperties() );
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

    private boolean isComment(String line)
    {
        return line.startsWith( "track " ) || line.startsWith( "browser " ) || line.startsWith( "#" );
    }

}
