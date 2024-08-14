package ru.biosoft.bsa;

import com.developmentontheedge.beans.Option;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.access.SequencesDatabaseInfo;
import ru.biosoft.bsa.access.SequencesDatabaseInfoSelector;
import ru.biosoft.util.bean.JSONBean;

/**
 * Bean to select genome (sequences collection)
 * Genome must always be selected
 */
public class BasicGenomeSelector extends Option implements JSONBean
{
    private static final long serialVersionUID = 1L;
    private DataElementPath sequencePath;
    private SequencesDatabaseInfo dbSelector;
    
    public BasicGenomeSelector()
    {
        setDbSelector(getDefaultDbSelector());
    }
    
    public void setFromTrack(Track track)
    {
        DataElementPath path = null;
        try
        {
            path = TrackUtils.getTrackSequencesPath(track);
        }
        catch( Exception e )
        {
        }
        if( path != null )
        {
            setDbSelector(SequencesDatabaseInfoSelector.getDatabase(path));
            setSequenceCollectionPath(path);
        } else
        {
            setDbSelector(getDefaultDbSelector());
        }
    }
    
    public BasicGenomeSelector(Track track)
    {
        setFromTrack(track);
    }

    protected SequencesDatabaseInfo getDefaultDbSelector()
    {
        return SequencesDatabaseInfoSelector.getDefaultDatabase();
    }

    public DataElementPath getSequenceCollectionPath()
    {
        if( dbSelector == null || dbSelector == SequencesDatabaseInfo.NULL_SEQUENCES )
            return null;
        if(dbSelector == SequencesDatabaseInfo.CUSTOM_SEQUENCES)
            return sequencePath;
        return dbSelector.getChromosomePath();
    }

    public void setSequenceCollectionPath(DataElementPath sequencePath)
    {
        Object oldValue = this.sequencePath;
        this.sequencePath = sequencePath;
        firePropertyChange("sequencePath", oldValue, sequencePath);
    }

    public boolean isSequenceCollectionPathHidden()
    {
        return dbSelector != SequencesDatabaseInfo.CUSTOM_SEQUENCES;
    }

    public SequencesDatabaseInfo getDbSelector()
    {
        return dbSelector;
    }

    public void setDbSelector(SequencesDatabaseInfo dbSelector)
    {
        this.dbSelector = dbSelector == null ? SequencesDatabaseInfo.CUSTOM_SEQUENCES : dbSelector;
        firePropertyChange("*", null, null);
    }
    
    public DataCollection<AnnotatedSequence> getSequenceCollection()
    {
        return getSequenceCollectionPath() == null ? null : getSequenceCollectionPath().getDataCollection(AnnotatedSequence.class);
    }
}
