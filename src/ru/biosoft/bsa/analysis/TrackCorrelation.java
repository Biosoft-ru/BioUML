package ru.biosoft.bsa.analysis;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.jobcontrol.Iteration;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;

public class TrackCorrelation extends AnalysisMethodSupport<TrackCorrelation.Parameters>
{
    public TrackCorrelation(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Track refTrack = parameters.getReferenceTrack().getDataElement( Track.class );
        Track queryTrack = parameters.getQueryTrack().getDataElement( Track.class );
        Computation corrComputation = new Computation( refTrack, queryTrack );
        jobControl.forCollection( parameters.getGenome().getChildren(), corrComputation );
        GenomeResult result = corrComputation.merge();
        log.info( result.toString() );
        return result.getAsTable( parameters.getOutputTable() );
    }

    private static class ChrResult
    {
        int referenceCount, queryCount;
        int referenceCommon, queryCommon;
        @Override
        public String toString()
        {
            return "referenceCount=" + referenceCount + ", queryCount=" + queryCount + ", referenceCommon=" + referenceCommon
                    + ", queryCommon=" + queryCommon;
        }
    }

    private static class GenomeResult extends ChrResult
    {
        public TableDataCollection getAsTable(DataElementPath tablePath)
        {
            TableDataCollection res = TableDataCollectionUtils.createTableDataCollection( tablePath );
            res.getColumnModel().addColumn( "ReferenceCount", DataType.Integer );
            res.getColumnModel().addColumn( "QueryCount", DataType.Integer );
            res.getColumnModel().addColumn( "ReferenceCommon100", DataType.Integer );
            res.getColumnModel().addColumn( "QueryCommon100", DataType.Integer );
            res.getColumnModel().addColumn( "ReferenceCommonPercent", DataType.Float );
            res.getColumnModel().addColumn( "QueryCommonPercent", DataType.Float );
            TableDataCollectionUtils.addRow( res, "1", new Object[] {referenceCount, queryCount, referenceCommon, queryCommon,
                    referenceCommon * 100.0 / referenceCount, queryCommon * 100.0 / queryCount} );
            tablePath.save( res );
            return res;
        }
    }

    private static class Computation implements Iteration<ru.biosoft.access.core.DataElementPath>
    {
        private Track refTrack;
        private Track queryTrack;

        private List<ChrResult> chrResults = new ArrayList<>();

        public Computation(Track refTrack, Track queryTrack)
        {
            this.refTrack = refTrack;
            this.queryTrack = queryTrack;
        }

        @Override
        public boolean run(DataElementPath chrPath)
        {
            try
            {
                Sequence chrSeq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
                DataCollection<Site> refSites = refTrack.getSites( chrPath.toString(), 0, chrSeq.getStart() + chrSeq.getLength() );
                DataCollection<Site> querySites = queryTrack.getSites( chrPath.toString(), 0, chrSeq.getStart() + chrSeq.getLength() );
                chrResults.add( computeChr( refSites, querySites, chrSeq.getStart(), chrSeq.getLength() ) );
                return true;
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
        }

        private ChrResult computeChr(DataCollection<Site> refSites, DataCollection<Site> querySites, int chrStart, int chrLenght)
        {
            ChrResult res = new ChrResult();

            int[] refPoints = getMidPoints( refSites );
            int[] queryPoints = getMidPoints( querySites );

            res.referenceCount = refPoints.length;
            res.referenceCommon = countClosePoints( refPoints, queryPoints, 100 );
            res.queryCount = queryPoints.length;
            res.queryCommon = countClosePoints( queryPoints, refPoints, 100 );

            return res;
        }

        /** Count the number of query points that are near (<=maxDist) reference points. */
        private int countClosePoints(int[] query, int[] ref, int maxDist)
        {
            int res = 0;
            int j = 0;
            for( int element : ref )
            {
                while( j < query.length && query[j] < element - maxDist )
                    j++;
                while( j < query.length && query[j] <= element + maxDist )
                {
                    res++;
                    j++;
                }
            }
            return res;
        }

        private int[] getMidPoints(DataCollection<Site> sites)
        {
            int[] result = new int[sites.getSize()];
            int i = 0;
            for( Site s : sites )
                result[i++] = s.getInterval().getCenter();
            Arrays.sort( result );
            return result;
        }

        public GenomeResult merge()
        {
            GenomeResult res = new GenomeResult();
            for( ChrResult cr : chrResults )
            {
                res.referenceCount += cr.referenceCount;
                res.queryCount += cr.queryCount;
                res.referenceCommon += cr.referenceCommon;
                res.queryCommon += cr.queryCommon;
            }
            return res;
        }

    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath referenceTrack;
        @PropertyName ( "Reference track" )
        public DataElementPath getReferenceTrack()
        {
            return referenceTrack;
        }
        public void setReferenceTrack(DataElementPath referenceTrack)
        {
            Object oldValue = this.referenceTrack;
            this.referenceTrack = referenceTrack;

            try
            {
                DataElementPath genome = TrackUtils.getTrackSequencesPath( referenceTrack.getDataElement( Track.class ) );
                if( genome != null && genome.exists() )
                    setGenome( genome );
            }
            catch( Exception e )
            {
            }

            referenceTrack.getDataElement( Track.class );
            firePropertyChange( "referenceTrack", oldValue, referenceTrack );
        }

        private DataElementPath queryTrack;
        @PropertyName ( "Query track" )
        public DataElementPath getQueryTrack()
        {
            return queryTrack;
        }
        public void setQueryTrack(DataElementPath queryTrack)
        {
            Object oldValue = this.queryTrack;
            this.queryTrack = queryTrack;
            firePropertyChange( "queryTrack", oldValue, queryTrack );
        }

        private DataElementPath genome;
        @PropertyName ( "Genome" )
        public DataElementPath getGenome()
        {
            return genome;
        }
        public void setGenome(DataElementPath genome)
        {
            Object oldValue = this.genome;
            this.genome = genome;
            firePropertyChange( "genome", oldValue, genome );
        }

        private DataElementPath outputTable;
        public DataElementPath getOutputTable()
        {
            return outputTable;
        }
        public void setOutputTable(DataElementPath outputTable)
        {
            Object oldValue = this.outputTable;
            this.outputTable = outputTable;
            firePropertyChange( "outputTable", oldValue, outputTable );
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
            property( "referenceTrack" ).inputElement( Track.class ).add();
            property( "queryTrack" ).inputElement( Track.class ).add();
            property( "genome" ).inputElement( SequenceCollection.class ).add();
            property( "outputTable" ).outputElement( TableDataCollection.class ).add();
        }
    }
}
