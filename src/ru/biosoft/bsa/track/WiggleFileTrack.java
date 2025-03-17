package ru.biosoft.bsa.track;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.ChrCache;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.importer.WiggleTrackImporter;
import ru.biosoft.bsa.importer.WiggleTrackImporter.WiggleState;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;

public class WiggleFileTrack extends AbstractDataCollection<DataElement> implements Track
{
    private File file;
    private ChrCache chrCache;

    public WiggleFileTrack(DataCollection<?> parent, Properties properties) throws IOException
    {
        super(parent, properties);

        String filePath = properties.getProperty(DataCollectionConfigConstants.FILE_PROPERTY);
        if( filePath == null )
            throw new IllegalArgumentException();
        file = new File(filePath);
        if( !file.exists() )
            throw new FileNotFoundException();
        DataElementPath seqBase = DataElementPath.create(properties.getProperty(Track.SEQUENCES_COLLECTION_PROPERTY));
        chrCache = new ChrCache(seqBase);
    }

    private Object readerLock = new Object();
    @Override public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        String chrName = DataElementPath.create(sequence).getName();

        synchronized (readerLock)
        {
            DataCollection<Site> allSites = getAllSites();
            VectorDataCollection<Site> result = new VectorDataCollection<>("sites");
            allSites.stream().filter(site -> {
                return (site.getOriginalSequence().getName().equals(chrName) && site.getFrom() >= from && site.getTo() <= to);
            }).forEach(result::put);
            return result;
        }
    }

    @Override public int countSites(String sequence, int from, int to) throws Exception
    {
        return getSites(sequence, from, to).getSize();
    }

    @Override public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        return getSites(sequence, from, to).get(siteName);
    }

    @Override public DataCollection<Site> getAllSites() throws UnsupportedOperationException
    {
        synchronized (readerLock)
        {
            return convertToDC(file);
        }
    }

    private VectorDataCollection<Site> convertToDC(File trackFile)
    {
        VectorDataCollection<Site> result = new VectorDataCollection<>("sites");
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
                    Sequence seq = chrCache.getSequence(site.getName());
                    site = new SiteImpl(site.getOrigin(), siteName, site.getType(), site.getBasis(), site.getStart(), site.getLength(), site.getPrecision(), site.getStrand(), seq,
                            site.getComment(), site.getProperties());
                }
                result.put(site);
            }
            result.getInfo().getProperties().putAll(properties);
        }
        catch (Exception e)
        {
            log.log(Level.SEVERE, "Can not create Wiggle track from file", e);
        }
        return result;
    }

    private TrackViewBuilder viewBuilder = new DefaultTrackViewBuilder();

    @Override public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    @Override public List<String> getNameList()
    {
        return Collections.emptyList();
    }

    @Override protected DataElement doGet(String name) throws Exception
    {
        return null;
    }

}
