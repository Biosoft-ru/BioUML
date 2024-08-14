package ru.biosoft.bsa.analysis;

import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.ExProperties;

@ClassIcon("resources/sitesresult.gif")
public class SiteSearchResult extends GenericDataCollection
{
    public static final String YES_PROMOTERS = "yes promoters";
    public static final String NO_PROMOTERS = "no promoters";
    public static final String YES_SITES = "yes sites";
    public static final String NO_SITES = "no sites";
    public static final String YES_SITES_OPTIMIZED = "yes sites optimized";
    public static final String NO_SITES_OPTIMIZED = "no sites optimized";
    public static final String SUMMARY = "summary";
    public static final String PROFILE_PROPERTY = DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX+"profile";

    public SiteSearchResult(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
        ExProperties.addPlugin(properties, getClass());
    }

    public static DataCollection createResult(DataElementPath path) throws Exception
    {
        return DataCollectionUtils.createSubCollection( path, DataCollectionUtils.CreateStrategy.FORCE_REMOVE, SiteSearchResult.class );
    }

    /**
     * @return Track with sites found on experiment data
     */
    public static @Nonnull Track getYesTrack(DataCollection<?> parent)
    {
        DataElementPath path = parent.getCompletePath();
        DataElementPath optPath = path.getChildPath( YES_SITES_OPTIMIZED );
        if(!optPath.exists())
            optPath = path.getChildPath( YES_SITES );
        return optPath.getDataElement( Track.class );
    }

    /**
     * @return Track with sites found on control data
     */
    public static @Nonnull Track getNoTrack(DataCollection<?> parent)
    {
        DataElementPath path = parent.getCompletePath();
        DataElementPath optPath = path.getChildPath( NO_SITES_OPTIMIZED );
        if(!optPath.exists())
            optPath = path.getChildPath( NO_SITES );
        return optPath.getDataElement( Track.class );
    }

    /**
     * @return Track with promoters from experiment data
     */
    public Track getYesIntervals()
    {
        try
        {
            return (Track) ( get(YES_PROMOTERS) );
        }
        catch( Exception e )
        {
        }
        return null;
    }

    /**
     * @return Track with promoters from control data
     */
    public Track getNoIntervals()
    {
        try
        {
            return (Track) ( get(NO_PROMOTERS) );
        }
        catch( Exception e )
        {
        }
        return null;
    }
}
