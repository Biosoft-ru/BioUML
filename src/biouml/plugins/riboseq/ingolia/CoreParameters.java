package biouml.plugins.riboseq.ingolia;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.transcripts.TranscriptSet;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.BAMTrack;

public class CoreParameters extends AbstractAnalysisParameters
{
    private DataElementPathSet bamFiles;
    @PropertyName("BAM files")
    @PropertyDescription("BAM files with genomic alignments")
    public DataElementPathSet getBamFiles()
    {
        return bamFiles;
    }
    public void setBamFiles(DataElementPathSet bamFiles)
    {
        final Object oldValue = bamFiles;
        this.bamFiles = bamFiles;
        firePropertyChange( "bamFiles", oldValue, this.bamFiles );
    }
    public BAMTrack[] getBAMTracks()
    {
        DataElementPathSet pathSet = getBamFiles();
        BAMTrack[] result = new BAMTrack[pathSet.size()];
        int i = 0;
        for( DataElementPath path : pathSet )
            result[i++] = path.getDataElement( BAMTrack.class );
        return result;
    }

    private TranscriptSet transcriptSet;
    {
        setTranscriptSet( new TranscriptSet() );
    }
    @PropertyName("Transcript set")
    @PropertyDescription("Transcript set")
    public TranscriptSet getTranscriptSet()
    {
        return transcriptSet;
    }
    public void setTranscriptSet(TranscriptSet transcriptSet)
    {
        TranscriptSet oldValue = this.transcriptSet;
        this.transcriptSet = withPropagation( oldValue, transcriptSet );
        firePropertyChange( "transcriptSet", oldValue, transcriptSet );
    }

    private int transcriptOverhangs = 100;
    @PropertyName("Transcript overhangs")
    @PropertyDescription("Transcript overhangs")
    public int getTranscriptOverhangs()
    {
        return transcriptOverhangs;
    }
    public void setTranscriptOverhangs(int transcriptOverhangs)
    {
        final int oldValue = this.transcriptOverhangs;
        this.transcriptOverhangs = transcriptOverhangs;
        firePropertyChange( "transcriptOverhangs", oldValue, this.transcriptOverhangs );
    }

    private boolean strandSpecific = true;
    @PropertyName("Strand specific")
    @PropertyDescription("In strand specific protocol reads can come only from positive strand of transcript")
    public boolean isStrandSpecific()
    {
        return strandSpecific;
    }
    public void setStrandSpecific(boolean strandSpecific)
    {
        final boolean oldValue = this.strandSpecific;
        this.strandSpecific = strandSpecific;
        firePropertyChange( "strandSpecific", oldValue, this.strandSpecific );
    }
}
