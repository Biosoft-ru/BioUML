package ru.biosoft.bsa.analysis;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.GenomeSelector;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.SiteModelUtils;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackOnSequences;
import ru.biosoft.bsa.access.SequencesDatabaseInfo;
import ru.biosoft.bsa.access.SequencesDatabaseInfoSelector;

@SuppressWarnings ( "serial" )
public class SiteSearchAnalysisParameters extends AbstractAnalysisParameters
{
    protected SequencesDatabaseInfo dbSelector;
    protected DataElementPath trackPath, profilePath, output, seqCollectionPath;

    public SiteSearchAnalysisParameters()
    {
        setSeqCollectionPath(DataElementPath.create("databases/Ensembl/Sequences/chromosomes GRCh37"));
        setDbSelector(SequencesDatabaseInfoSelector.getDefaultDatabase());
        setTrackPath(null);

        setProfilePath(getDefaultProfile());
    }

    public DataElementPath getDefaultProfile()
    {
        return SiteModelUtils.getDefaultProfile();
    }

    public void setTrack(Track track)
    {
        Object oldValue = this.trackPath;
        if( track == null )
        {
            this.trackPath = null;
        }
        else
        {
            this.trackPath = DataElementPath.create(track);
        }
        firePropertyChange("track", oldValue, this.trackPath);
    }

    public void setSeqCollection(DataCollection<?> seqCollection)
    {
        setSeqCollectionPath(DataElementPath.create(seqCollection));
    }

    public void setProfile(DataCollection<?> profile)
    {
        setProfilePath(profile==null?null:DataElementPath.create(profile));
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[] {"trackPath"};
    }

    @Override
    public @Nonnull String[] getOutputNames()
    {
        return new String[] {"output"};
    }

    public DataElementPath getTrackPath()
    {
        return trackPath;
    }

    public void setTrackPath(DataElementPath trackPath)
    {
        Object oldValue = this.trackPath;
        this.trackPath = trackPath;
        firePropertyChange("trackPath", oldValue, this.trackPath);
        if(trackPath != null)
        {
            Track track = trackPath.optDataElement( Track.class );
            if(track != null)
            {
                if(track instanceof TrackOnSequences)
                {
                    setSeqCollectionPath( trackPath );
                }
                else
                {
                GenomeSelector genomeSelector = new GenomeSelector( track );
                SequencesDatabaseInfo databaseInfo = genomeSelector.getDbSelector();
                if(databaseInfo != null && databaseInfo != SequencesDatabaseInfo.NULL_SEQUENCES)
                    setDbSelector( databaseInfo );
                DataElementPath seqCollectionPath = genomeSelector.getSequenceCollectionPath();
                if(seqCollectionPath != null)
                    setSeqCollectionPath( seqCollectionPath );
                }
            }
        }
    }

    public DataElementPath getProfilePath()
    {
        return profilePath;
    }

    public void setProfilePath(DataElementPath profilePath)
    {
        Object oldValue = this.profilePath;
        this.profilePath = profilePath;
        firePropertyChange("profilePath", oldValue, this.profilePath);
    }

    public DataElementPath getOutput()
    {
        return output;
    }

    public void setOutput(DataElementPath output)
    {
        Object oldValue = this.output;
        this.output = output;
        firePropertyChange("output", oldValue, this.output);
    }

    public DataElementPath getSeqCollectionPath()
    {
        //HACK to get correct value for workflow
        //TODO: try to use BasicGenomeSelector
        if( this.dbSelector != null && this.dbSelector != SequencesDatabaseInfo.CUSTOM_SEQUENCES )
            return this.dbSelector.getChromosomePath();
        return seqCollectionPath;
    }
    
    public boolean isSeqCollectionPathHidden()
    {
        return dbSelector != SequencesDatabaseInfo.CUSTOM_SEQUENCES;
    }

    public void setSeqCollectionPath(DataElementPath seqCollectionPath)
    {
        Object oldValue = this.seqCollectionPath;
        this.seqCollectionPath = seqCollectionPath;
        firePropertyChange("seqCollectionPath", oldValue, this.seqCollectionPath);
    }

    public SequencesDatabaseInfo getDbSelector()
    {
        return dbSelector;
    }

    public void setDbSelector(SequencesDatabaseInfo dbSelector)
    {
        if(dbSelector == null) dbSelector = SequencesDatabaseInfo.CUSTOM_SEQUENCES;
        Object oldValue = this.dbSelector;
        this.dbSelector = dbSelector;
        firePropertyChange("dbSelector", oldValue, dbSelector);
        if(this.dbSelector != SequencesDatabaseInfo.CUSTOM_SEQUENCES)
            setSeqCollectionPath(this.dbSelector.getChromosomePath());
    }
}
