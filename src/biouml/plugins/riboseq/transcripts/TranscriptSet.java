package biouml.plugins.riboseq.transcripts;

import java.io.File;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.JSONBean;

public class TranscriptSet extends OptionEx implements JSONBean
{
    public static final String ANNOTATION_SOURCE_ENSEMBL = "Ensembl";
    public static final String ANNOTATION_SOURCE_BED_FILE = "BED file";
    public static final String ANNOTATION_SOURCE_GTF_FILE = "GTF file";

    {
        initAnnotationSource();
    }

    private String annotationSource;
    @PropertyName("Annotation source")
    @PropertyDescription("Source of gene annotation")
    public String getAnnotationSource()
    {
        return annotationSource;
    }
    public void setAnnotationSource(String annotationSource)
    {
        String oldValue = this.annotationSource;
        this.annotationSource = annotationSource;
        firePropertyChange( "*", oldValue, annotationSource );
    }

    private DataElementPath transcriptsTrack;
    @PropertyName("Transcripts annotation track")
    @PropertyDescription("Track with transcripts annotation in BED format")
    public DataElementPath getTranscriptsTrack()
    {
        return transcriptsTrack;
    }
    public void setTranscriptsTrack(DataElementPath transcriptsTrack)
    {
        final Object oldValue = this.transcriptsTrack;
        this.transcriptsTrack = transcriptsTrack;
        firePropertyChange( "transcriptsTrack", oldValue, this.transcriptsTrack );
    }
    public boolean isTranscriptsTrackHidden()
    {
        return !annotationSource.equals( ANNOTATION_SOURCE_BED_FILE );
    }

    private EnsemblDatabase ensembl;
    @PropertyName("Ensembl")
    @PropertyDescription("Ensembl database version")
    public EnsemblDatabase getEnsembl()
    {
        return ensembl;
    }
    public void setEnsembl(EnsemblDatabase ensembl)
    {
        final Object oldValue = this.ensembl;
        this.ensembl = ensembl;
        firePropertyChange( "ensembl", oldValue, this.ensembl );
    }
    public boolean isEnsemblHidden()
    {
        return !annotationSource.equals( ANNOTATION_SOURCE_ENSEMBL );
    }
    
    private DataElementPath gtfFile;
    @PropertyName("GTF file")
    public DataElementPath getGtfFile()
    {
        return gtfFile;
    }
    public void setGtfFile(DataElementPath gtfFile)
    {
        Object oldValue = this.gtfFile;
        this.gtfFile = gtfFile;
        firePropertyChange( "gtfFile", oldValue, gtfFile );
    }
    public boolean isGtfFileHidden()
    {
        return !annotationSource.equals( ANNOTATION_SOURCE_GTF_FILE );
    }
    
    private void initAnnotationSource()
    {
        EnsemblDatabase[] availableDatabases = EnsemblDatabaseSelector.getEnsemblDatabases();
        if(availableDatabases.length > 0)
        {
            annotationSource = ANNOTATION_SOURCE_ENSEMBL;
            ensembl = availableDatabases[0];
        }
        else
        {
            annotationSource = ANNOTATION_SOURCE_BED_FILE;
        }
    }

    public TranscriptsProvider createTranscriptsProvider()
    {
        TranscriptsProvider provider = null;
        switch( getAnnotationSource() )
        {
            case ANNOTATION_SOURCE_ENSEMBL:
                if(getEnsembl() == null)
                    throw new IllegalStateException();
                provider = new EnsemblTranscriptsProvider( getEnsembl() );
                break;
            case ANNOTATION_SOURCE_BED_FILE:
                if(getTranscriptsTrack() == null)
                    throw new IllegalStateException();
                Track bedTrack = getTranscriptsTrack().getDataElement( Track.class );
                provider = new BedTrackTranscriptsProvider( bedTrack );
                break;
            case ANNOTATION_SOURCE_GTF_FILE:
                if(getGtfFile() == null)
                    throw new IllegalStateException();
                File file = getGtfFile().getDataElement(FileDataElement.class).getFile();
                provider = new GTFTranscriptsProvider( file );
                break;
            default:
                throw new AssertionError();
        }
        if(onlyProteinCoding)
            provider.setOnlyProteinCoding( true );
        return provider;
    }

    public TranscriptLoader createTranscriptLoader()
    {
        DataElementPath subsetPath = getTranscriptSubset();
        DataCollection<DataElement> subsetDC = subsetPath == null ? null : subsetPath.getDataCollection();
        return new TranscriptLoader( createTranscriptsProvider(), subsetDC == null ? null : subsetDC.getNameList() );
    }

    private DataElementPath transcriptSubset;
    @PropertyName("Transcript subset")
    @PropertyDescription("Subset of transcripts used in this analysis")
    public DataElementPath getTranscriptSubset()
    {
        return transcriptSubset;
    }
    public void setTranscriptSubset(DataElementPath transcriptSubset)
    {
        final Object oldValue = this.transcriptSubset;
        this.transcriptSubset = transcriptSubset;
        firePropertyChange( "transcriptSubset", oldValue, this.transcriptSubset );
    }
    public boolean isTranscriptSubsetHidden()
    {
        return transcriptSubsetDisabled;
    }
    private boolean transcriptSubsetDisabled = false;
    public void disableTranscriptSubset()
    {
        transcriptSubsetDisabled = true;
    }
    
    
    private DataElementPath sequencesCollection;
    @PropertyName("Genome sequence")
    @PropertyDescription("Collection of chromosomal sequences")
    public DataElementPath getSequencesCollection()
    {
        return sequencesCollection;
    }
    public void setSequencesCollection(DataElementPath sequencesCollection)
    {
        this.sequencesCollection = sequencesCollection;
    }
    public boolean isSequencesCollectionHidden()
    {
        return annotationSource.equals( ANNOTATION_SOURCE_ENSEMBL ) || !needsSequencesCollection;
    }
    
    private boolean needsSequencesCollection = true;
    public void setNeedsSequencesCollection(boolean value)
    {
        needsSequencesCollection = value;
    }

    public SequenceCollection getChromosomes()
    {
        DataElementPath path = getAnnotationSource().equals( ANNOTATION_SOURCE_ENSEMBL )
                ? getEnsembl().getPrimarySequencesPath()
                : getSequencesCollection();
        return path.getDataElement( SequenceCollection.class );
    }
    
    private boolean onlyProteinCoding;
    public void setOnlyProteinCoding(boolean value)
    {
        onlyProteinCoding = value;
    }
}
