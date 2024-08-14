
package ru.biosoft.bsa;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUnion;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.view.TrackViewBuilder;

/**
 * @author yura
 * Make union of all tracks contained in List(Track>
 */
public class UnionTrack extends DataElementSupport implements Track
{
    private List<Track> tracks;
    /**
     * @param name
     * @param origin
     */
    public UnionTrack(String name, DataCollection origin, List<Track> tracks)
    {
        super(name, origin);
        this.tracks = tracks;
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nonnull DataCollection<Site> getAllSites()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        DataCollection vectorDC = new VectorDataCollection("none", null, new Properties());
        for (Track t : tracks)
        {
            vectorDC.put(t.getSites(sequence, from, to));
        }
        DataCollection<Site> result = new DataCollectionUnion("none", vectorDC, new Properties());
        return result;
    }
    
    public List<Site> getAllSitesList()
    {
    	List<Site> allSites = new ArrayList<>();
    	for(Track track : tracks)
    		for(Site site : track.getAllSites())
    			allSites.add(site);
    	return allSites;
    }
    @Override
    public TrackViewBuilder getViewBuilder()
    {
        throw new UnsupportedOperationException();
    }
}
