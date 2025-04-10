package biouml.plugins.riboseq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.ingolia.AlignmentConverter;
import biouml.plugins.riboseq.ingolia.AlignmentOnTranscript;
import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.BAMTrack.SitesCollection.BAMSite;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

public class EstimateReadDensity extends AnalysisMethodSupport<EstimateReadDensity.Parameters>
{

    public EstimateReadDensity(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection densityTable = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputDensityTable() );
        densityTable.getColumnModel().addColumn( "Density", DataType.Float );
        densityTable.getColumnModel().addColumn( "Count", DataType.Integer );

        List<Transcript> transcripts = parameters.getTranscriptSet().createTranscriptLoader().loadTranscripts( log );
        Map<String, Transcript> transcriptsByName = StreamEx.of( transcripts ).mapToEntry( Transcript::getName, t -> t ).toMap();

        BAMTrack inTrack = parameters.getInputBAMTrack().getDataElement( BAMTrack.class );
        TIntIntMap countsByLength = new TIntIntHashMap();
        for(Site s : inTrack.getAllSites())
        {
            int refLen = ((BAMSite)s).getCigar().getReferenceLength();
            countsByLength.adjustOrPutValue( refLen, 1, 1 );
        }
        
        File inputWigFile = parameters.getMinimalUniqueLength().getDataElement( FileDataElement.class ).getFile();

        AlignmentConverter alignmentConverter = new AlignmentConverter();
        alignmentConverter.setTranscriptOverhangs( 0 );

        try (BufferedReader reader = new BufferedReader( new FileReader( inputWigFile ) ))
        {
            String line = reader.readLine();
            while( line != null )
            {
                String[] fields = line.split( " " );
                if( fields.length != 4 || !fields[0].equals( "fixedStep" ) || !fields[1].startsWith( "chrom=" )
                        || !fields[2].equals( "start=1" ) || !fields[3].equals( "step=1" ) )
                    throw new Exception( "Unexpected line '" + line + "' in " + parameters.getMinimalUniqueLength() );
                String transcriptName = fields[1].substring( "chrom=".length() );

                Transcript t = transcriptsByName.get( transcriptName );
                if(t == null || (parameters.isInCDS() && !t.isCoding()))
                {
                    while( ( line = reader.readLine() ) != null && !line.startsWith( "fixedStep" ) );
                    continue;
                }

                int[] mul = new int[t.getLength()];
                {
                    int i = 0;
                    while( ( line = reader.readLine() ) != null && !line.startsWith( "fixedStep" ) )
                    {
                        if( i >=  mul.length )
                            incompatibleTranscript( t );
                        mul[i++] = Integer.parseInt( line );
                    }
                    if( i != t.getLength() )
                        incompatibleTranscript( t );
                }

                List<AlignmentOnTranscript> alignsOnTranscript = alignmentConverter.getTranscriptAlignments( t, inTrack );
                if( parameters.isStrandSpecific() )
                    alignsOnTranscript = StreamEx.of( alignsOnTranscript ).filter( a->a.isPositiveStrand() ).toList();

                Result result = computeDensity( mul, alignsOnTranscript, t, countsByLength );

                TableDataCollectionUtils.addRow( densityTable, transcriptName, new Object[] {result.density, result.count}, true );
            }
        }

        densityTable.finalizeAddition();
        parameters.getOutputDensityTable().save( densityTable );
        return densityTable;
    }
    
    private static void incompatibleTranscript(Transcript t) throws Exception
    {
        throw new Exception( "Incompatible transcript " + t.getName() );
    }

    private static class Result
    {
        final int count;
        final double density;
        Result(int count, double density)
        {
            this.count = count;
            this.density = density;
        }
    }

    private Result computeDensity(int[] mul, List<AlignmentOnTranscript> alignsOnTranscript, Transcript t, TIntIntMap totalCountsByLen)
    {
        NavigableMap<Integer, Integer> lengthIndex = getCountsByLength( alignsOnTranscript, t );
        if(lengthIndex.isEmpty())
        {
            int[] lens = totalCountsByLen.keys();
            for(int j = 0; j < lens.length; j++)
            {
                int len = lens[j];
                Interval bounds = getBounds5Prime( t, len );
                for( int i = bounds.getFrom(); i <= bounds.getTo(); i++ )
                    if( len >= mul[i] )
                        return new Result( 0, 0 );
            }
            return new Result( 0, Double.NaN );
        }
        double density = 0;
        int totalCount = 0;
        for( Map.Entry<Integer, Integer> entry : lengthIndex.entrySet() )
        {
            int len = entry.getKey();
            
            int count = entry.getValue();
            totalCount += count;
            
            Interval bounds = getBounds5Prime( t, len );

            int mappableLen = 0;
            for( int i = bounds.getFrom(); i <= bounds.getTo(); i++ )
                if( len >= mul[i] )
                    mappableLen++;
            if( mappableLen > 0 )
                density += (double)count / mappableLen;
        }
        if(density == 0.0)
            density = Double.NaN;
            
        return new Result( totalCount, density );
    }
    
    private Interval getBounds5Prime(Transcript t, int readLen)
    {
        int from = 0;
        int to = t.getLength() - readLen;
        if( parameters.isInCDS() )
        {
            Interval cds = t.getCDSLocations().get( 0 );
            from = cds.getFrom() - readLen + parameters.getMinCDSOverlap();
            if( from < 0 )
                from = 0;
            to = cds.getTo() - parameters.getMinCDSOverlap() + 1;
            if( to + readLen > t.getLength())
                to = t.getLength() - readLen;
        }
        return new Interval(from, to);
    }

    private NavigableMap<Integer, Integer> getCountsByLength(List<AlignmentOnTranscript> alignsOnTranscript, Transcript t)
    {
        Interval cds = null;
        if(parameters.isInCDS())
            cds = t.getCDSLocations().get( 0 );
        NavigableMap<Integer, Integer> lengthIndex = new TreeMap<>();
        for( AlignmentOnTranscript aot : alignsOnTranscript )
        {
            if(cds != null)
            {
                Interval overlap = cds.intersect( aot );
                if(overlap == null)
                    continue;
                int overlapLength = overlap.getLength();
                if(overlapLength < parameters.getMinCDSOverlap())
                    continue;
            }
            int len = aot.getLength();
            Integer count = lengthIndex.getOrDefault( len, 0 );
            lengthIndex.put( len, count + 1 );
        }
        return lengthIndex;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputBAMTrack;
        @PropertyName ( "Alignments" )
        @PropertyDescription ( "Genomic alignments of reads to transcriptome" )
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

        private DataElementPath minimalUniqueLength;
        @PropertyName ( "Minimal unique length" )
        @PropertyDescription ( "Minimal unique length file, produced by 'Transcriptome minimal unique length' analysis" )
        public DataElementPath getMinimalUniqueLength()
        {
            return minimalUniqueLength;
        }
        public void setMinimalUniqueLength(DataElementPath minimalUniqueLength)
        {
            Object oldValue = this.minimalUniqueLength;
            this.minimalUniqueLength = minimalUniqueLength;
            firePropertyChange( "minimalUniqueLength", oldValue, minimalUniqueLength );
        }

        private TranscriptSet transcriptSet;
        {
            setTranscriptSet( new TranscriptSet() );
        }
        @PropertyName ( "Transcript set" )
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
        
        private boolean strandSpecific;
        @PropertyName("Strand specific")
        @PropertyDescription("Count only reads mapped to forward strand of transcript")
        public boolean isStrandSpecific()
        {
            return strandSpecific;
        }
        public void setStrandSpecific(boolean strandSpecific)
        {
            boolean oldValue = this.strandSpecific;
            this.strandSpecific = strandSpecific;
            firePropertyChange( "strandSpecific", oldValue, strandSpecific );
        }

        private boolean inCDS;
        @PropertyName ( "In CDS" )
        @PropertyDescription ( "Compute density in CDS" )
        public boolean isInCDS()
        {
            return inCDS;
        }
        public void setInCDS(boolean inCDS)
        {
            boolean oldValue = this.inCDS;
            this.inCDS = inCDS;
            transcriptSet.setOnlyProteinCoding( inCDS );
            firePropertyChange( "inCDS", oldValue, inCDS );
        }

        private int minCDSOverlap = 15;
        @PropertyName ( "Min CDS overlap" )
        @PropertyDescription ( "Include only reads overlapping this number of nucleotides with CDS" )
        public int getMinCDSOverlap()
        {
            return minCDSOverlap;
        }
        public void setMinCDSOverlap(int minCDSOverlap)
        {
            int oldValue = this.minCDSOverlap;
            this.minCDSOverlap = minCDSOverlap;
            firePropertyChange( "minCDSOverlap", oldValue, minCDSOverlap );
        }
        public boolean isMinCDSOverlapHidden()
        {
            return !isInCDS();
        }

        private DataElementPath outputDensityTable;
        @PropertyName ( "Output density table" )
        public DataElementPath getOutputDensityTable()
        {
            return outputDensityTable;
        }
        public void setOutputDensityTable(DataElementPath outputDensityTable)
        {
            Object oldValue = this.outputDensityTable;
            this.outputDensityTable = outputDensityTable;
            firePropertyChange( "outputDensityTable", oldValue, outputDensityTable );
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
            property( "minimalUniqueLength" ).inputElement( FileDataElement.class ).add();
            add( "transcriptSet" );
            add( "strandSpecific" );
            add( "inCDS" );
            addHidden( "minCDSOverlap", "isMinCDSOverlapHidden" );
            property( "outputDensityTable" ).outputElement( TableDataCollection.class ).add();
        }
    }
}
