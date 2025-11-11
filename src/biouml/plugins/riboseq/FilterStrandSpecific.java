package biouml.plugins.riboseq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AlignmentUtils;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.ChrIntervalMap;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.importer.SAMBAMTrackImporter;
import ru.biosoft.bsa.importer.SAMBAMTrackImporter.ImporterProperties;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;

public class FilterStrandSpecific extends AnalysisMethodSupport<FilterStrandSpecific.Parameters>
{
    public FilterStrandSpecific(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        List<Transcript> transcripts = parameters.getTranscriptSet().createTranscriptLoader().loadTranscripts( log );

        ChrIntervalMap<Transcript> transcriptIndex = new ChrIntervalMap<>();
        for( Transcript t : transcripts )
            transcriptIndex.add( t.getChromosome(), t.getLocation().getFrom(), t.getLocation().getTo(), t );

        BAMTrack inTrack = parameters.getInputBAMTrack().getDataElement( BAMTrack.class );

        DataElementPath outPath = parameters.getOutputBAMTrack();
        if(!parameters.isEnabled())
            return inTrack.clone( outPath.getParentCollection(), outPath.getName() );

        try (TempFile outBamFile = TempFiles.file( ".bam" ); SamReader bamReader = SamReaderFactory.makeDefault().open( inTrack.getBAMFile() ))
        {
            SAMFileHeader header = bamReader.getFileHeader();
            boolean sorted = header.getSortOrder() == SortOrder.coordinate;
            SAMFileWriterFactory factory = new SAMFileWriterFactory();
            SAMFileWriter bamWriter = factory.makeBAMWriter( header, sorted, outBamFile );
            
            try
            {
                List<String> wrongAlignmnets = new ArrayList<>();
                for( SAMRecord r : bamReader )
                {
                    Collection<Transcript> overlappingTranscripts = transcriptIndex.getIntervals( r.getReferenceName(),
                            r.getAlignmentStart() - 1, r.getAlignmentEnd() - 1 );
                    boolean hasForwardAlignment = false;
                    boolean hasAlignment = false;
                    Interval[] alignment = AlignmentUtils.getMatchedIntervals( r );
                    for( Transcript t : overlappingTranscripts )
                    {
                        Interval[] exons = t.getExonLocations().toArray( new Interval[0] );
                        if( AlignmentUtils.isContinuousAlignment( exons, alignment ) )
                        {
                            hasAlignment = true;
                            if( !r.getReadNegativeStrandFlag() == t.isOnPositiveStrand() )
                            {
                                hasForwardAlignment = true;
                                break;
                            }
                        }
                    }
                    if( !hasAlignment )
                        wrongAlignmnets.add( r.getReadName() );
                    if( hasForwardAlignment )
                    {
                        SAMRecord rCopy = (SAMRecord)r.clone();
                        rCopy.setAttribute( "NM", null );
                        bamWriter.addAlignment( r );
                    }
                }
                if(!wrongAlignmnets.isEmpty())
                {
                    log.warning( "Some reads has no matching transcripts, total " + wrongAlignmnets.size() );
                    for(int i = 0; i < Math.min( 100, wrongAlignmnets.size()); i++)
                        log.warning( "No matching transcript for " + wrongAlignmnets.get( i ) );
                }
            }
            finally
            {
                bamWriter.close();
            }

            SAMBAMTrackImporter importer = new SAMBAMTrackImporter();
            ImporterProperties properties = importer.getProperties( outPath.getParentCollection(), outBamFile, outPath.getName() );
            properties.setCreateIndex( sorted );
            return importer.doImport( outPath.getParentCollection(), outBamFile, outPath.getName(), null, log );
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputBAMTrack;
        @PropertyName ( "Input BAM track" )
        @PropertyDescription ( "BAM track to filter" )
        public DataElementPath getInputBAMTrack()
        {
            return inputBAMTrack;
        }
        public void setInputBAMTrack(DataElementPath inputBAMTrack)
        {
            Object oldValue = this.inputBAMTrack;
            this.inputBAMTrack = inputBAMTrack;
            firePropertyChange( "inputBAMTrack", oldValue, inputBAMTrack );
        }

        private TranscriptSet transcriptSet;
        {
            setTranscriptSet( new TranscriptSet() );
        }
        @PropertyName ( "Transcript set" )
        @PropertyDescription ( "Transcript set" )
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
        
        private boolean enabled = true;
        @PropertyName("Enabled")
        public boolean isEnabled()
        {
            return enabled;
        }
        public void setEnabled(boolean enabled)
        {
            boolean oldValue = this.enabled;
            this.enabled = enabled;
            firePropertyChange( "enabled", oldValue, enabled );
        }

        private DataElementPath outputBAMTrack;
        @PropertyName ( "Output BAM track" )
        public DataElementPath getOutputBAMTrack()
        {
            return outputBAMTrack;
        }
        public void setOutputBAMTrack(DataElementPath outputBAMTrack)
        {
            Object oldValue = this.outputBAMTrack;
            this.outputBAMTrack = outputBAMTrack;
            firePropertyChange( "outputBAMTrack", oldValue, outputBAMTrack );
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "inputBAMTrack" ).inputElement( BAMTrack.class ).add();
            add( "transcriptSet" );
            addExpert( "enabled" );
            property( "outputBAMTrack" ).outputElement( BAMTrack.class ).add();
        }
    }
}
