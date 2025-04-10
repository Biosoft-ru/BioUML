package biouml.plugins.riboseq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.DiscontinuousCoordinateSystem;
import ru.biosoft.bsa.Interval;
import ru.biosoft.util.bean.BeanInfoEx2;

public class GTFForRiboseqAlignment extends AnalysisMethodSupport<GTFForRiboseqAlignment.Parameters>
{

    public GTFForRiboseqAlignment(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath resultPath = parameters.getResultingGTF();
        if(resultPath.exists())
            resultPath.remove();
        File gtfFile = DataCollectionUtils.getChildFile( resultPath.getParentCollection(), resultPath.getName() );
        FileDataElement result = new FileDataElement( resultPath.getName(), resultPath.getParentCollection(), gtfFile );
        try (BufferedWriter writer = new BufferedWriter( new FileWriter( gtfFile ) ))
        {
            List<Transcript> transcripts = parameters.getTranscriptSet().createTranscriptLoader().loadTranscripts( log );
            Map<String, Integer> chrLen = parameters.getTranscriptSet().getChromosomes().stream()
                    .collect( Collectors.toMap( DataElement::getName, s -> s.getSequence().getLength() ) );
            for( Transcript t : transcripts )
            {
                if( !chrLen.containsKey( t.getChromosome() ) )
                    continue;
                t = extendTranscript(t, chrLen);
                String attributes = "transcript_id \"" +t.getName() + "\";";
                for(Interval exon : t.getExonLocations())
                {
                    writer
                      .append( t.getChromosome() ).append( '\t' )//seqname
                      .append( "biouml" ).append( '\t' )//source
                      .append( "exon" ).append( '\t' )//feature
                      .append( String.valueOf( exon.getFrom() + 1 ) ).append( '\t' )//start 1-based
                      .append( String.valueOf( exon.getTo() + 1 ) ).append( '\t' )//end 1-based
                      .append( '.' ).append( '\t' )//score
                      .append( t.isOnPositiveStrand() ? '+' : '-' ).append( '\t' )//strand
                      .append( '.' ).append( '\t' )//frame
                      .append( attributes ).append( '\n' );
                }
                for(Interval cds : t.getCDSLocations())
                {
                    DiscontinuousCoordinateSystem cs = new DiscontinuousCoordinateSystem( t.getExonLocations(), !t.isOnPositiveStrand() );
                    int genomicFrom = cs.translateCoordinateBack( cds.getFrom() ) ;
                    int genomicTo = cs.translateCoordinateBack( cds.getTo() );
                    if(!t.isOnPositiveStrand())
                    {
                        int tmp = genomicFrom;
                        genomicFrom = genomicTo;
                        genomicTo = tmp;
                    }
                    Interval cdsGenomicInterval = new Interval(genomicFrom, genomicTo);
                    for(Interval exon : t.getExonLocations())
                        if( exon.intersects( cdsGenomicInterval ) )
                        {
                            Interval cdsPart = exon.intersect( cdsGenomicInterval );
                            if(cdsPart == null)
                                continue;
                            writer
                            .append( t.getChromosome() ).append( '\t' )
                            .append( "biouml" ).append( '\t' )
                            .append( "cds" ).append( '\t' )
                            .append( String.valueOf( cdsPart.getFrom() + 1 ) ).append( '\t' )
                            .append( String.valueOf( cdsPart.getTo() + 1  ) ).append( '\t' )
                            .append( '.' ).append( '\t' )
                            .append( t.isOnPositiveStrand() ? '+' : '-' ).append( '\t' )//strand
                            .append( '.' ).append( '\t' )//frame
                            .append( attributes ).append( '\n' );
                        }
                }
            }
        }
        resultPath.save( result );
        return result;
    }

    private Transcript extendTranscript(Transcript t, Map<String, Integer> chrLen)
    {
        if( !t.isCoding() )
            return t;
        Interval cds = t.getCDSLocations().get( 0 );
        int len = chrLen.get( t.getChromosome() );

        int upstream = 0;
        if( cds.getFrom() < parameters.getMinUTRLength() )
            upstream = parameters.getMinUTRLength() - cds.getFrom();
        if( t.isOnPositiveStrand() && t.getLocation().getFrom() - upstream < 0 )
            upstream = t.getLocation().getFrom();
        if( !t.isOnPositiveStrand() && t.getLocation().getTo() + upstream > len - 1 )
            upstream = len - t.getLocation().getTo() - 1;

        int downstream = 0;
        if( t.getLength() - cds.getTo() - 1 < parameters.getMinUTRLength() )
            downstream = parameters.getMinUTRLength() - ( t.getLength() - cds.getTo() - 1 );
        if( t.isOnPositiveStrand() && t.getLocation().getTo() + downstream > len - 1 )
            downstream = len - t.getLocation().getTo() - 1;
        if( !t.isOnPositiveStrand() && t.getLocation().getFrom() - downstream < 0 )
            downstream = t.getLocation().getFrom();

        List<Interval> exons = t.isOnPositiveStrand() ? growIntervals( t.getExonLocations(), upstream, downstream )
                : growIntervals( t.getExonLocations(), downstream, upstream );

        int tFrom = exons.get( 0 ).getFrom();
        int tTo = exons.get( exons.size() - 1 ).getTo();
        Interval loc = new Interval( tFrom, tTo );

        return new Transcript( t.getName(), t.getChromosome(), loc, t.isOnPositiveStrand(), exons, Collections.singletonList( cds.shift( upstream ) ) );
    }

    private static List<Interval> growIntervals(List<Interval> exons, int upstream, int downstream)
    {
        exons = new ArrayList<>( exons );
        Interval e = exons.get( 0 );
        exons.set( 0, new Interval( e.getFrom() - upstream, e.getTo() ) );

        e = exons.get( exons.size() - 1 );
        e = new Interval( e.getFrom(), e.getTo() + downstream );
        exons.set( exons.size() - 1, e );

        return exons;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private TranscriptSet transcriptSet;
        {
            setTranscriptSet( new TranscriptSet() );
        }
        @PropertyName ( "Transcript set" )
        @PropertyDescription ( "Set of transcripts to output into GTF file" )
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

        private int minUTRLength = 0;
        @PropertyName ( "Min UTR length" )
        @PropertyDescription ( "Extend untranlsated regions to this length" )
        public int getMinUTRLength()
        {
            return minUTRLength;
        }
        public void setMinUTRLength(int minUTRLength)
        {
            int oldValue = this.minUTRLength;
            this.minUTRLength = minUTRLength;
            firePropertyChange( "minUTRLength", oldValue, minUTRLength );
        }
        
        private boolean onlyProteinCoding = false;
        @PropertyName("Only protein coding")
        @PropertyDescription("Use only protein coding transcripts")
        public boolean isOnlyProteinCoding()
        {
            return onlyProteinCoding;
        }
        public void setOnlyProteinCoding(boolean onlyProteinCoding)
        {
            boolean oldValue = this.onlyProteinCoding;
            this.onlyProteinCoding = onlyProteinCoding;
            firePropertyChange( "onlyProteinCoding", oldValue, onlyProteinCoding );
            transcriptSet.setOnlyProteinCoding( onlyProteinCoding );
        }

        private DataElementPath resultingGTF;
        @PropertyName ( "Resulting GTF" )
        @PropertyDescription ( "Resulting GTF file" )
        public DataElementPath getResultingGTF()
        {
            return resultingGTF;
        }
        public void setResultingGTF(DataElementPath resultingGTF)
        {
            Object oldValue = this.resultingGTF;
            this.resultingGTF = resultingGTF;
            firePropertyChange( "resultingGTF", oldValue, resultingGTF );
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
            add( "transcriptSet" );
            add( "minUTRLength" );
            add( "onlyProteinCoding" );
            property( "resultingGTF" ).outputElement( FileDataElement.class ).add();
        }
    }
}
