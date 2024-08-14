package ru.biosoft.bsa;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.access.SequencesDatabaseInfo;
import ru.biosoft.bsa.access.SequencesDatabaseInfoSelector;

/**
 * Bean to select genome (sequences collection) with genomeId
 * Genome can be null
 */
public class GenomeSelector extends BasicGenomeSelector
{
    private static final long serialVersionUID = 1L;
    private String genomeId;
    
    public GenomeSelector()
    {
    }
    
    public GenomeSelector(Track t)
    {
        super(t);
    }

    public String getGenomeId()
    {
        if(getDbSelector() == null)
            return null;
        if(getDbSelector() == SequencesDatabaseInfo.NULL_SEQUENCES)
            return null;
        if(getDbSelector() == SequencesDatabaseInfo.CUSTOM_SEQUENCES)
            return genomeId;
        return getDbSelector().getGenomeBuild();
    }

    public void setGenomeId(String genomeId)
    {
        Object oldValue = this.genomeId;
        this.genomeId = genomeId;
        firePropertyChange("genomeId", oldValue, genomeId);
    }

    @Override
    protected SequencesDatabaseInfo getDefaultDbSelector()
    {
        return SequencesDatabaseInfoSelector.getNullDatabase();
    }
    
    @Override
    public void setFromTrack(Track track)
    {
        super.setFromTrack( track );
        setGenomeId( TrackUtils.getGenomeId( track ) );
    }
    
    public static class GenomeSelectorTrackUpdater implements PropertyChangeListener
    {
        private final Track element;
        private final GenomeSelector genomeSelector;
        private final Runnable action;
        public GenomeSelectorTrackUpdater(Track element, GenomeSelector genomeSelector, Runnable action)
        {
            this.element = element;
            this.genomeSelector = genomeSelector;
            this.action = action;
        }
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            String newValue = evt.getNewValue() == null ? null : evt.getNewValue().toString();
            if( "*".equals( evt.getPropertyName() ) )
            {
                DataElementPath collectionPath = genomeSelector.getSequenceCollectionPath();
                TrackUtils.addTrackProperty( element, Track.SEQUENCES_COLLECTION_PROPERTY,
                        collectionPath == null ? null : collectionPath.toString() );
                action.run();
                String genomeId = genomeSelector.getGenomeId();
                if( genomeId != null )
                    TrackUtils.addTrackProperty( element, Track.GENOME_ID_PROPERTY, genomeId );
            }
            else if( "sequencePath".equals( evt.getPropertyName() ) )
            {
                TrackUtils.addTrackProperty( element, Track.SEQUENCES_COLLECTION_PROPERTY, newValue );
                action.run();
            }
            else if( "genomeId".equals( evt.getPropertyName() ) )
            {
                TrackUtils.addTrackProperty( element, Track.GENOME_ID_PROPERTY, newValue );
            }
        }
    }

}
