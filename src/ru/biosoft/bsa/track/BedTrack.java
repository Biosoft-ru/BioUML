package ru.biosoft.bsa.track;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.*;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;

public class BedTrack extends DataElementSupport implements WritableTrack
{
    private File file;
    private BufferedWriter writer;
    private int nextId = 1;
    private boolean isInitialized = false;
    private VectorDataCollection<Site> track;
    private final Map<String, Sequence> sequenceCache = new HashMap<>();
    private TrackViewBuilder viewBuilder = new DefaultTrackViewBuilder();

    public BedTrack(DataCollection<?> origin, Properties properties)
    {
        super(properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY, "null"), origin);
        file = DataCollectionUtils.getChildFile(origin, getName());
    }

    public BedTrack(DataCollection<?> origin, String name, File file)
    {
        super(name, origin);
        this.file = file;
    }

    private void init()
    {
        if (isInitialized) return;
        synchronized (this)
        {
            if (isInitialized) return;
            track = new VectorDataCollection<>(getName(), Site.class, null);
            readFromFile(file, track);
            isInitialized = true;
        }
    }

    private void readFromFile(File trackFile, DataCollection<Site> track)
    {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(trackFile), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = input.readLine()) != null)
            {
                if (line.startsWith("#")) continue;
                Site site = parseBedLine(line);
                if (site != null) track.put(site);
            }
        }
        catch (Exception e)
        {
            // TODO: Обработать исключение
        }
    }

    private Site parseBedLine(String line)
    {
        String[] parts = line.split("\t");
        if (parts.length < 3) return null;

        String sequenceName = parts[0];
        int from = Integer.parseInt(parts[1]);
        int to = Integer.parseInt(parts[2]);
        String name = parts.length > 3 ? parts[3] : String.valueOf(nextId++);
        float score = parts.length > 4 ? Float.parseFloat(parts[4]) : 0.0f;
        char strand = parts.length > 5 ? parts[5].charAt(0) : '.';

        return new SiteImpl(null, name, "BED", 0, from, to - from, 1, strand == '+' ? 1 : strand == '-' ? -1 : 0,
                getSequence(sequenceName), "", null);
    }

    private Sequence getSequence(String name)
    {
        return sequenceCache.computeIfAbsent(name, k -> new LinearSequence(name, new byte[0], Nucleotide15LetterAlphabet.getInstance()));
    }

    public File getFile()
    {
        return file;
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        init();
        VectorDataCollection<Site> result = new VectorDataCollection<>("sites");
        track.stream().filter(s -> s.getSequence().getName().equals(sequence) && s.getFrom() >= from && s.getTo() <= to)
                .forEach(result::put);
        return result;
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        init();
        return getSites(sequence, from, to).getSize();
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        init();
        return track.get(siteName);
    }

    @Override
    public DataCollection<Site> getAllSites() throws UnsupportedOperationException
    {
        init();
        return track;
    }

    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
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

            writer.append(site.getSequence().getName())
                    .append('\t')
                    .append(String.valueOf(site.getFrom() - chr.getStart()))
                    .append('\t')
                    .append(String.valueOf(site.getTo() - chr.getStart() + 1))
                    .append('\t')
                    .append(siteName)
                    .append('\t')
                    .append(String.valueOf(site.getScore()))
                    .append('\t')
                    .append(strand)
                    .append('\n');
        }
        catch (IOException e)
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    private void initWriter() throws IOException
    {
        if (writer == null)
            writer = new BufferedWriter(new FileWriter(file, true));
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
            throw ExceptionRegistry.translateException(e);
        }
    }
}

