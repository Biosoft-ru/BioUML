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
import ru.biosoft.bsa.importer.WiggleTrackImporter;
import ru.biosoft.bsa.importer.WiggleTrackImporter.WiggleState;

public class WiggleFileTrack extends FileTrack
{

    public WiggleFileTrack(DataCollection<?> parent, Properties properties) throws IOException
    {
        super(parent, properties);
    }

    @Override
    protected void readFromFile(File trackFile, DataCollection<Site> sites)
    {
        WiggleState ws = new WiggleState();
        Properties properties = new Properties();

        try (FileInputStream is = new FileInputStream(trackFile);
                BufferedReader input = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                FileChannel ch = is.getChannel())
        {
            String line;
            int i = 1;
            while ( (line = input.readLine()) != null )
            {
                String siteName = String.valueOf(i++);
                Site site = WiggleTrackImporter.parseLine(line, ws);
                if( site != null && site.getOriginalSequence() == null )
                {
                    Sequence seq = getSequence( site.getName() );
                    site = new SiteImpl(site.getOrigin(), siteName, site.getType(), site.getBasis(), site.getStart(), site.getLength(), site.getPrecision(), site.getStrand(), seq,
                            site.getComment(), site.getProperties());
                }
                sites.put( site );
            }
            sites.getInfo().getProperties().putAll( properties );
        }
        catch (Exception e)
        {
            //TODO: print exception
        }
    }

}
