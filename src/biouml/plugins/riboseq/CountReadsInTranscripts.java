package biouml.plugins.riboseq;

import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AlignmentUtils;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.BAMTrack.SitesCollection.BAMSite;
import ru.biosoft.bsa.DiscontinuousCoordinateSystem;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CountReadsInTranscripts extends AnalysisMethodSupport<CountReadsInTranscripts.Parameters>
{

    public CountReadsInTranscripts(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        result.getColumnModel().addColumn( "Count", DataType.Integer );
        
        BAMTrack bamTrack = parameters.getBamTrack().getDataElement( BAMTrack.class );
        
        List<Transcript> transcripts = parameters.getTranscriptSet().createTranscriptLoader().loadTranscripts( log );
        for(Transcript t : transcripts)
        {
            int count = computeReadCount( t, bamTrack );
            TableDataCollectionUtils.addRow( result, t.getName(), new Object[] {count}, true );
        }
        result.finalizeAddition();
        
        parameters.getOutputTable().save( result );
        return new Object[] {result};
    }
    
    private int computeReadCount(Transcript t, BAMTrack bamTrack) throws Exception
    {
        String chrPath = getChrPath(t);
        DataCollection<Site> sites = bamTrack.getSites( chrPath, t.getLocation().getFrom() + 1, t.getLocation().getTo() + 1 );
        int count = 0;
        Interval[] exons = t.getExonLocations().toArray( new Interval[0] );
        for(Site s : sites)
        {
            boolean siteOnPositiveStrand = s.getStrand() == StrandType.STRAND_PLUS;
            if( parameters.isStrandSpecific() && siteOnPositiveStrand != t.isOnPositiveStrand() )
                continue;
            Interval[] intervals = AlignmentUtils.getMatchedIntervals( (BAMSite)s );
            if( AlignmentUtils.isContinuousAlignment( exons, intervals ) )
            {
                if( parameters.isOnlyOverlappingCDS() )
                {
                    if( !t.isCoding()  )
                        continue;
                    int len = getCDSOverlapLength( (BAMSite)s, t );
                    if( len < parameters.getMinOverlap() )
                        continue;
                }
                count++;
            }
        }
        return count;
    }

    private String getChrPath(Transcript t)
    {
        return parameters.getTranscriptSet().getChromosomes().getCompletePath().getChildPath( t.getChromosome() ).toString();
    }
    
    private int getCDSOverlapLength(BAMSite s, Transcript t)
    {
        DiscontinuousCoordinateSystem cs = new DiscontinuousCoordinateSystem( t.getExonLocations(), !t.isOnPositiveStrand() );
        int from = cs.translateCoordinate( s.getFrom() - 1 );
        int to = cs.translateCoordinate( s.getTo() - 1 );
        Interval siteInterval = new Interval( Math.min( from, to ), Math.max( from, to ) );
        Interval cds = t.getCDSLocations().get( 0 );
        if(cds.intersects( siteInterval ))
            return cds.intersect( siteInterval ).getLength();
        return 0;
    }
    
    public static class Parameters extends AbstractAnalysisParameters
    {
        
        private DataElementPath bamTrack;
        private TranscriptSet transcriptSet;
        {
            setTranscriptSet( new TranscriptSet() );
        }
        private boolean strandSpecific;
        private boolean onlyOverlappingCDS;
        private int minOverlap = 1;
        private DataElementPath outputTable;
        
        @PropertyName("BAM track")
        @PropertyDescription("BAM track with read alignments")
        public DataElementPath getBamTrack()
        {
            return bamTrack;
        }
        public void setBamTrack(DataElementPath bamTrack)
        {
            this.bamTrack = bamTrack;
        }

        @PropertyName("Transcript set")
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
        
        @PropertyName("Strand specific")
        @PropertyDescription("Count only reads on the same strand as transcript")
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

        @PropertyName("Only overlapping cds")
        @PropertyDescription("Count only reads that overlap coding sequence")
        public boolean isOnlyOverlappingCDS()
        {
            return onlyOverlappingCDS;
        }
        public void setOnlyOverlappingCDS(boolean onlyOverlappingCDS)
        {
            boolean oldValue = this.onlyOverlappingCDS;
            this.onlyOverlappingCDS = onlyOverlappingCDS;
            firePropertyChange( "onlyOverlappingCDS", oldValue, onlyOverlappingCDS );
            transcriptSet.setOnlyProteinCoding( onlyOverlappingCDS );
        }

        @PropertyName("Min overlap")
        @PropertyDescription("Minimal overlap between read and CDS in nucleotides")
        public int getMinOverlap()
        {
            return minOverlap;
        }
        public void setMinOverlap(int minOverlap)
        {
            int oldValue = this.minOverlap;
            this.minOverlap = minOverlap;
            firePropertyChange( "minOverlap", oldValue, minOverlap );
        }
        public boolean isMinOverlapHidden()
        {
            return !isOnlyOverlappingCDS();
        }
        
        @PropertyName("Output table")
        @PropertyDescription("Output table with read counts for each transcript")
        public DataElementPath getOutputTable()
        {
            return outputTable;
        }
        public void setOutputTable(DataElementPath outputTable)
        {
            this.outputTable = outputTable;
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
            property( "bamTrack" ).inputElement( BAMTrack.class ).add();
            add( "transcriptSet" );
            add( "strandSpecific" );
            add( "onlyOverlappingCDS" );
            addHidden( "minOverlap", "isMinOverlapHidden" );
            property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$bamTrack$ counts in transcripts" ).add();
        }
    }
}
