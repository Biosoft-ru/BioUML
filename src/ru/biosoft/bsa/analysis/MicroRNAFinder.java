package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.MergedTrack.IntervalSet;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import ru.biosoft.jobcontrol.Iteration;

public class MicroRNAFinder extends AnalysisMethodSupport<MicroRNAFinder.Parameters>
{
    private static final PropertyDescriptor READ_COUNT_DESCRIPTOR = StaticDescriptor.create( "Read count" );

    public MicroRNAFinder(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final Track alignments = parameters.getAlignment().getDataElement(Track.class);
        DataElementPath seqCollectionPath = parameters.getGenome().getSequenceCollectionPath();
        final SqlTrack clusterTrack = SqlTrack.createTrack( parameters.getClustersTrack(), alignments, seqCollectionPath );
        final SqlTrack primaryIsoformsTrack = SqlTrack.createTrack( parameters.getPrimaryIsoformsTrack(), alignments, seqCollectionPath );
        final SqlTrack longestIsoformsTrack = SqlTrack.createTrack( parameters.getLongestIsoformsTrack(), alignments, seqCollectionPath );

        jobControl.forCollection( seqCollectionPath.getChildren(), new Iteration<ru.biosoft.access.core.DataElementPath>()
        {
            private int nextId = 1;

            @Override
            public boolean run(DataElementPath chrPath)
            {
                try
                {
                    for( int strand : new int[] {StrandType.STRAND_PLUS, StrandType.STRAND_MINUS} )
                    {
                        IntervalSet clusters = new IntervalSet();
                        Sequence chr = chrPath.getDataElement(AnnotatedSequence.class).getSequence();
                        for( Site s : alignments.getSites( chrPath.toString(), 0, chr.getLength() ) )
                            if( s.getStrand() == strand )
                                clusters.add( new Interval( s.getStart(), s.getStart() + parameters.getMaxReadDistance() ) );

                        for( Interval cluster : clusters )
                        {
                            DataCollection<Site> clusterAligns = alignments.getSites( chrPath.toString(), cluster.getFrom(),
                                    cluster.getTo() );

                            int clusterSize = 0;
                            int clusterFrom = Integer.MAX_VALUE;
                            int clusterTo = Integer.MIN_VALUE;
                            Map<Interval, Integer> isoformCounts = new HashMap<>();
                            for( Site alignment : clusterAligns )
                                if( alignment.getStrand() == strand && cluster.inside( alignment.getStart() ) )
                                {
                                    clusterSize++;
                                    Interval isoformInterval = alignment.getInterval();
                                    clusterFrom = Math.min( isoformInterval.getFrom(), clusterFrom );
                                    clusterTo = Math.max( isoformInterval.getTo(), clusterTo );
                                    Integer isoformCount = isoformCounts.get( isoformInterval );
                                    isoformCounts.put( isoformInterval, isoformCount == null ? 1 : ( isoformCount + 1 ) );
                                }
                            if( clusterSize < parameters.getMinClusterSize() )
                                continue;

                            int currentId = nextId++;

                            DynamicPropertySet properties = new DynamicPropertySetSupport();
                            properties.add( new DynamicProperty( READ_COUNT_DESCRIPTOR, Integer.class, clusterSize ) );
                            Site clusterSite = new SiteImpl( null, "cluster-" + currentId, Site.TYPE_MISC_RNA, Basis.BASIS_PREDICTED,
                                    strand == StrandType.STRAND_MINUS ? clusterTo : clusterFrom, clusterTo - clusterFrom + 1,
                                    Precision.PRECISION_CUT_BOTH, strand, chr, properties );
                            clusterTrack.addSite( clusterSite );

                            Interval primaryIsoform = null;
                            int primaryCount = 0;
                            for( Map.Entry<Interval, Integer> e : isoformCounts.entrySet() )
                                if( e.getValue() > primaryCount )
                                {
                                    primaryIsoform = e.getKey();
                                    primaryCount = e.getValue();
                                }

                            properties = new DynamicPropertySetSupport();
                            properties.add( new DynamicProperty( READ_COUNT_DESCRIPTOR, Integer.class, primaryCount ) );
                            Site primaryIsoformSite = new SiteImpl( null, "isoform-primary-" + currentId, Site.TYPE_MISC_RNA,
                                    Basis.BASIS_PREDICTED, strand == StrandType.STRAND_MINUS ? primaryIsoform.getTo() : primaryIsoform
                                            .getFrom(), primaryIsoform.getLength(), Precision.PRECISION_CUT_BOTH, strand, chr, properties );
                            primaryIsoformsTrack.addSite( primaryIsoformSite );

                            Interval longestIsoform = null;
                            for( Interval e : isoformCounts.keySet() )
                                if( longestIsoform == null || e.getLength() > longestIsoform.getLength() )
                                    longestIsoform = e;

                            properties = new DynamicPropertySetSupport();
                            properties.add( new DynamicProperty( READ_COUNT_DESCRIPTOR, Integer.class, isoformCounts.get( longestIsoform ) ) );
                            Site longestIsoformSite = new SiteImpl( null, "isoform-longest-" + currentId, Site.TYPE_MISC_RNA,
                                    Basis.BASIS_PREDICTED, strand == StrandType.STRAND_MINUS ? longestIsoform.getTo() : longestIsoform
                                            .getFrom(), longestIsoform.getLength(), Precision.PRECISION_CUT_BOTH, strand, chr, properties );
                            longestIsoformsTrack.addSite( longestIsoformSite );
                        }
                    }
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException(e);
                }
                return true;
            }
        } );

        clusterTrack.finalizeAddition();
        parameters.getClustersTrack().save( clusterTrack );

        primaryIsoformsTrack.finalizeAddition();
        parameters.getPrimaryIsoformsTrack().save( primaryIsoformsTrack );

        longestIsoformsTrack.finalizeAddition();
        parameters.getLongestIsoformsTrack().save( longestIsoformsTrack );

        return new Object[] {clusterTrack, primaryIsoformsTrack, longestIsoformsTrack};
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath alignment, clustersTrack, primaryIsoformsTrack, longestIsoformsTrack;
        private int minClusterSize = 100;
        private int maxReadDistance = 10;
        private BasicGenomeSelector genome = new BasicGenomeSelector();

        @PropertyName ( "Alignments" )
        @PropertyDescription ( "Genomic alignments of microRNA reads" )
        public DataElementPath getAlignment()
        {
            return alignment;
        }

        public void setAlignment(DataElementPath alignment)
        {
            Object oldValue = this.alignment;
            this.alignment = alignment;
            firePropertyChange( "alignment", oldValue, alignment );
            setGenome( new BasicGenomeSelector( alignment.getDataElement(Track.class) ) );
        }

        @PropertyName ( "Genome" )
        @PropertyDescription ( "Reference genome used for alignment" )
        public BasicGenomeSelector getGenome()
        {
            return genome;
        }

        public void setGenome(BasicGenomeSelector genome)
        {
            Object oldValue = this.genome;
            this.genome = genome;
            firePropertyChange( "genome", oldValue, genome );
        }

        @PropertyName ( "Minimal cluster size" )
        @PropertyDescription ( "Minimal number of reads in cluster" )
        public int getMinClusterSize()
        {
            return minClusterSize;
        }

        public void setMinClusterSize(int minClusterSize)
        {
            Object oldValue = this.minClusterSize;
            this.minClusterSize = minClusterSize;
            firePropertyChange( "minClusterSize", oldValue, minClusterSize );
        }

        @PropertyName ( "Maximum read distance" )
        @PropertyDescription ( "Maxim distance between adjacent reads in cluster" )
        public int getMaxReadDistance()
        {
            return maxReadDistance;
        }

        public void setMaxReadDistance(int maxReadDistance)
        {
            Object oldValue = this.maxReadDistance;
            this.maxReadDistance = maxReadDistance;
            firePropertyChange( "maxReadDistance", oldValue, maxReadDistance );
        }

        @PropertyName ( "Clusters" )
        @PropertyDescription ( "Resulting read clusters" )
        public DataElementPath getClustersTrack()
        {
            return clustersTrack;
        }

        public void setClustersTrack(DataElementPath clustersTrack)
        {
            Object oldValue = this.clustersTrack;
            this.clustersTrack = clustersTrack;
            firePropertyChange( "clustersTrack", oldValue, clustersTrack );
        }

        @PropertyName ( "Primary isoforms" )
        @PropertyDescription ( "Resulting primary microRNA isoforms" )
        public DataElementPath getPrimaryIsoformsTrack()
        {
            return primaryIsoformsTrack;
        }

        public void setPrimaryIsoformsTrack(DataElementPath primaryIsoformsTrack)
        {
            Object oldValue = this.primaryIsoformsTrack;
            this.primaryIsoformsTrack = primaryIsoformsTrack;
            firePropertyChange( "primaryIsoformsTrack", oldValue, primaryIsoformsTrack );
        }

        @PropertyName( "Longest isoforms" )
        @PropertyDescription( "Resulting longest microRNA isoforms" )
        public DataElementPath getLongestIsoformsTrack()
        {
            return longestIsoformsTrack;
        }

        public void setLongestIsoformsTrack(DataElementPath longestIsoformsTrack)
        {
            Object oldValue = this.longestIsoformsTrack;
            this.longestIsoformsTrack = longestIsoformsTrack;
            firePropertyChange( "longestIsoformsTrack", oldValue, longestIsoformsTrack );
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
            super.initProperties();
            property( "alignment" ).inputElement( Track.class ).add();
            add( "genome" );
            add( "minClusterSize" );
            add( "maxReadDistance" );

            property( "clustersTrack" ).outputElement( SqlTrack.class ).auto( "$alignment$ clusters" ).add();
            property( "primaryIsoformsTrack" ).outputElement( SqlTrack.class ).auto( "$alignment$ primary isoforms" ).add();
            property( "longestIsoformsTrack" ).outputElement( SqlTrack.class ).auto( "$alignment$ longest isoforms" ).add();
        }
    }

}
