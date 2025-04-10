package ru.biosoft.bsa.track;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.importer.BEDTrackImporter;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;

public class BedTrack extends FileTrack implements WritableTrack
{
    private File file;
    private BufferedWriter writer;
    private int nextId = 1;

    public BedTrack(DataCollection<?> origin, Properties properties) throws IOException
    {
        super( origin, properties );
    }

    //    public BedTrack(DataCollection<?> origin, String name, File file)
    //    {
    //        super(origin, name, file);
    //    }

    public File getFile()
    {
        return file;
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

            writer.append( getTrackOptions().externalToInternalName( site.getSequence().getName() ) ).append( '\t' ).append( String.valueOf( site.getFrom() - chr.getStart() ) )//zero based inclusive
                    .append( '\t' ).append( String.valueOf( site.getTo() - chr.getStart() + 1 ) )//zero based exclusive;
                    .append( '\t' ).append( siteName ).append( '\t' ).append( String.valueOf( site.getScore() ) ).append( '\t' ).append( strand ).append( '\n' );
        }
        catch (IOException e)
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    private void initWriter() throws IOException
    {
        if( writer == null )
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
        catch (IOException e)
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    @Override
    protected void readFromFile(File file, DataCollection<Site> sites)
    {
        int i = 1;
        try ( FileInputStream is = new FileInputStream( file );
                BufferedReader input = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) );
                FileChannel ch = is.getChannel() )
        {
            String line;
            while ( (line = input.readLine()) != null )
            {
                if( isComment( line ) )
                    continue;
                Site site = BEDTrackImporter.parseBEDLine( line, false );
                if( site == null )
                {
                    continue;
                }
                if( site.getOriginalSequence() == null )
                {
                    Sequence seq = getSequence( site.getName() );
                    site = new SiteImpl( site.getOrigin(), i + "", site.getType(), site.getBasis(), site.getStart(), site.getLength(), site.getPrecision(), site.getStrand(), seq,
                            site.getComment(), site.getProperties() );
                }
                sites.put( site );
                i++;
            }
        }

        catch (Exception e)
        {
            //TODO:
        }

    }

    private boolean isComment(String line)
    {
        return line.startsWith( "track " ) || line.startsWith( "browser " ) || line.startsWith( "#" );
    }
}
