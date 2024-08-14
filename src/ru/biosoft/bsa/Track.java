package ru.biosoft.bsa;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.bsa.view.TrackViewBuilder;

/**
 * Base interface for tracks
 */
@ClassIcon("resources/track.gif")
@PropertyName("track")
public interface Track extends DataElement
{
    // Path to sequences collection
    public static final String SEQUENCES_COLLECTION_PROPERTY = "SequencesCollection";
    // Genome id like "hg18"
    public static final String GENOME_ID_PROPERTY = DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX+"genomeId";
    // Position for genome browser in "chromosome:coordinate" format 
    public static final String DEFAULT_POSITION_PROPERTY = "defaultPosition";
    //Ttrack should be viewed in the genome browser with other tracks listed in this property, separated by ';'
    public static final String OPEN_WITH_TRACKS = "openWithTracks";

    /**
     * Returns collection of Site for selected region
     * @param sequence name, usually full path to sequence in repository
     * @param from position inclusive
     * @param to position inclusive
     * @return Collection of all sites overlapping given region
     * @throws RepositoryException
     */
    public DataCollection<Site> getSites(String sequence, int from, int to);
    /**
     * Returns Site count for selected region
     * @param sequence name, usually full path to sequence in repository
     * @param from position inclusive
     * @param to position inclusive
     * @return The number of sites overlapping given region
     * @throws Exception
     */
    public int countSites(String sequence, int from, int to) throws Exception;

    /**
     * Look for site by name
     * @return first found site or null if site not found
     */
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception;
    
    /**
     * Returns collection of all sites on the sequence
     * @throws UnsupportedOperationException if this operation is not supported by this track
     */
    public @Nonnull DataCollection<Site> getAllSites() throws UnsupportedOperationException;

    /**
     * Get view builder for track
     */
    public TrackViewBuilder getViewBuilder();
    
    public default List<String> getIndexes()
    {
        return Collections.emptyList();
    }
    
    public default List<Site> queryIndex(String index, String query)
    {
        throw new UnsupportedOperationException();
    }
}
