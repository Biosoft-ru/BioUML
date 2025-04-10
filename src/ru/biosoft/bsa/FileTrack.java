package ru.biosoft.bsa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;

public abstract class FileTrack extends AbstractDataCollection<DataElement> implements Track
{
    private File file;
    private VectorDataCollection<Site> sites;
    private boolean isInitialized = false;
    private TrackOptions trackOptions;
    private TrackViewBuilder viewBuilder = new DefaultTrackViewBuilder();

    public FileTrack(DataCollection<?> origin, Properties properties) throws IOException
    {
        super(origin, properties);
        String filePath = properties.getProperty(DataCollectionConfigConstants.FILE_PROPERTY);
        if( filePath != null )
        {
            file = new File(filePath);
        }
        else
            file = DataCollectionUtils.getChildFile(origin, getName());

        if( !file.exists() )
            throw new FileNotFoundException("File " + file.getAbsolutePath() + " not found in track constructor");

        trackOptions = new TrackOptions(this, properties);
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        return super.getOrigin();
    }

    private Object readerLock = new Object();
    private void init()
    {
        if( isInitialized )
            return;
        synchronized (readerLock)
        {
            if( isInitialized )
                return;
            sites = new VectorDataCollection<>(getName(), Site.class, null);
            readFromFile(file, sites);
            isInitialized = true;
        }
    }

    protected abstract void readFromFile(File file, DataCollection<Site> sites);

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        init();
        VectorDataCollection<Site> result = new VectorDataCollection<>("sites");
        String sequenceName = DataElementPath.create(sequence).getName();
        Interval fromTo = new Interval(from, to);
        sites.stream().filter(s -> s.getSequence().getName().equals(sequenceName) && s.getInterval().intersects(fromTo)).forEach(s -> result.put(s));
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
        return sites.get(siteName);
    }

    @Override
    public DataCollection<Site> getAllSites() throws UnsupportedOperationException
    {
        init();
        return sites;
    }

    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }

    public File getFile()
    {
        return file;
    }

    public TrackOptions getTrackOptions()
    {
        return trackOptions;
    }

    @Override
    public List<String> getNameList()
    {
        return getAllSites().getNameList();
    }

    @Override
    protected DataElement doGet(String name) throws Exception
    {
        return getAllSites().get(name);
    }

    protected Sequence getSequence(String name)
    {
        return trackOptions.getChromosomeSequence(trackOptions.internalToExternal(name));
    }

}
