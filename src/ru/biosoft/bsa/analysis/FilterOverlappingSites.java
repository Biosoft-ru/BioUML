package ru.biosoft.bsa.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.MoreCollectors;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SitePropertySelector;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon ( "resources/RemoveOverlappingSites.gif" )
public class FilterOverlappingSites extends AnalysisMethodSupport<FilterOverlappingSites.Parameters>
{

    public FilterOverlappingSites(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final Track input = parameters.getInputTrack().getDataElement( Track.class );
        final SqlTrack output = SqlTrack.createTrack( parameters.getOutputTrack(), input, parameters.getGenome()
                .getSequenceCollectionPath() );
        final Random rnd = new Random();

        jobControl.forCollection( parameters.getGenome().getSequenceCollectionPath().getChildren(), new Iteration<ru.biosoft.access.core.DataElementPath>()
        {
            @Override
            public boolean run(DataElementPath chromosome)
            {
                Sequence chrSeq = chromosome.getDataElement( AnnotatedSequence.class ).getSequence();
                try
                {
                    DataCollection<Site> siteCollection = input.getSites( chromosome.toString(), 0, chrSeq.getLength() );

                    if( parameters.isIndependentStrands() )
                    {
                        Map<Boolean, Site[]> strandSites = siteCollection.stream().collect( MoreCollectors
                                .partitioningBy( s -> s.getStrand() != StrandType.STRAND_MINUS, MoreCollectors.toArray( Site[]::new ) ) );
                        processSites( strandSites.get( true ) );
                        processSites( strandSites.get( false ) );
                    }
                    else
                    {
                        processSites( siteCollection.stream().toArray( Site[]::new ) );
                    }
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException( e );
                }
                return true;
            }

            private void processSites(Site[] sites) throws Exception
            {
                if(sites.length == 0)
                    return;
                Arrays.sort( sites, Comparator.comparing( Site::getInterval ) );
                int firstSiteInCluster = 0;
                int clusterTo = sites[0].getTo();
                for( int i = 1; i < sites.length; i++ )
                {
                    if( sites[i].getFrom() > clusterTo )
                    {
                        handleCluster( sites, firstSiteInCluster, i - firstSiteInCluster );
                        firstSiteInCluster = i;
                    }
                    clusterTo = Math.max( clusterTo, sites[i].getTo() );
                }
                handleCluster( sites, firstSiteInCluster, sites.length - firstSiteInCluster );
            }

            private void handleCluster(Site[] sites, int offset, int size) throws Exception
            {
                if( size == 1 )
                {
                    output.addSite( sites[offset] );
                    return;
                }
                switch( parameters.getSiteSelectMode() )
                {
                    case Parameters.ONE_LONGEST:
                    {
                        int maxLength = sites[offset].getLength();
                        int maxIndex = offset;
                        for( int i = 1; i < size; i++ )
                        {
                            int curLength = sites[offset + i].getLength();
                            if( curLength > maxLength )
                            {
                                maxLength = curLength;
                                maxIndex = offset + i;
                            }
                        }
                        output.addSite( sites[maxIndex] );
                    }
                        break;
                    case Parameters.ONE_SHORTEST:
                    {
                        int minLength = sites[offset].getLength();
                        int minIndex = offset;
                        for( int i = 1; i < size; i++ )
                        {
                            int curLength = sites[offset + i].getLength();
                            if( curLength < minLength )
                            {
                                minLength = curLength;
                                minIndex = offset + i;
                            }
                        }
                        output.addSite( sites[minIndex] );
                    }
                        break;
                    case Parameters.ONE_WITH_BEST_VALUE:
                    {
                        int bestIndex = offset;
                        for( int i = 1; i < size; i++ )
                            if( isBetter( sites[offset + i], sites[bestIndex] ) )
                                bestIndex = offset + i;
                        output.addSite( sites[bestIndex] );
                    }
                        break;
                    case Parameters.ONE_RANDOM:
                    {
                        output.addSite( sites[offset + rnd.nextInt( size )] );
                    }
                        break;
                    case Parameters.LARGEST_SET:
                    {
                        int end = sites[offset + size - 1].getTo() + 1;
                        for( int i = size - 1; i >= 0; i-- )
                        {
                            Site site = sites[i + offset];
                            if( site.getTo() >= end )
                                continue;
                            end = site.getFrom();
                            output.addSite( site );
                        }
                    }
                        break;
                    case Parameters.LONGEST_SET:
                    {
                        int[] next = new int[size];
                        NavigableMap<Integer, Integer> fromToIndex = new TreeMap<>();
                        for( int i = size - 1; i >= 0; i-- )
                        {
                            Site site = sites[offset + i];
                            Entry<Integer, Integer> higherEntry = fromToIndex.higherEntry( site.getTo() );
                            next[i] = higherEntry == null ? size : higherEntry.getValue();
                            fromToIndex.put( site.getFrom(), i );
                        }
                        int[] bestLength = new int[size + 1];
                        bestLength[size] = 0;
                        for( int i = size - 1; i >= 0; i-- )
                        {
                            Site site = sites[offset + i];
                            bestLength[i] = Math.max( bestLength[next[i]] + site.getLength(), bestLength[i + 1] );
                        }
                        int i = 0;
                        while( i < size )
                        {
                            if( bestLength[i] == bestLength[i + 1] )
                                i++;
                            else
                            {
                                output.addSite( sites[offset + i] );
                                i = next[i];
                            }
                        }
                    }
                        break;
                    case Parameters.SET_OF_BEST_SITES:
                    {
                        List<Site> cluster = new ArrayList<>();
                        for( int i = 0; i < size; i++ )
                            cluster.add( sites[offset + i] );
                        while( !cluster.isEmpty() )
                        {
                            Site best = cluster.get( 0 );
                            for( int i = 1; i < cluster.size(); i++ )
                            {
                                Site site = cluster.get( i );
                                if( isBetter( site, best ) )
                                    best = site;
                            }
                            output.addSite( best );
                            List<Site> filtered = new ArrayList<>();
                            for( Site s : cluster )
                                if( s != best && !s.getInterval().intersects( best.getInterval() ) )
                                    filtered.add( s );
                            cluster = filtered;
                        }
                    }
                        break;
                    case Parameters.FIVE_PRIME_MOST:
                    {
                        if( sites[offset].getStrand() != StrandType.STRAND_MINUS )
                        {
                            output.addSite( sites[offset] );
                        }
                        else
                        {
                            int rightMost = offset;
                            for( int i = offset; i < offset + size; i++ )
                                if( sites[i].getTo() > sites[rightMost].getTo() )
                                    rightMost = i;
                            output.addSite( sites[rightMost] );
                        }
                    }
                        break;
                    case Parameters.THREE_PRIME_MOST:
                    {
                        if( sites[offset].getStrand() == StrandType.STRAND_MINUS )
                        {
                            output.addSite( sites[offset] );
                        }
                        else
                        {
                            int rightMost = offset;
                            for( int i = offset; i < offset + size; i++ )
                                if( sites[i].getTo() > sites[rightMost].getTo() )
                                    rightMost = i;
                            output.addSite( sites[rightMost] );
                        }
                    }
                        break;
                    case Parameters.JACCARD_SIMILARITY:
                    {

                        double[][] jacDistance = new double[size][size];
                        int[][] overlaps = new int[size][size];
                        int maxOverlap = Integer.MIN_VALUE;
                        double maxJac = Double.MIN_VALUE;
                        for( int i = 0; i < size; i++ )
                        {
                            Site s1 = sites[offset + i];
                            for( int j = i + 1; j < size; j++ )
                            {
                                Site s2 = sites[offset + j];
                                int overlap = s1.getTo() < s2.getFrom() ? 0
                                        : s1.getTo() <= s2.getTo() ? s1.getTo() - s2.getFrom() + 1 : s2.getTo() - s2.getFrom() + 1;

                                //double union = ( s1.getTo() <= s2.getTo() ? s2.getTo() : s1.getTo() ) - s1.getFrom() + 1;
                                double union = Math.max( s1.getLength(), s2.getLength() );
                                jacDistance[i][j] = overlap / union;
                                overlaps[i][j] = overlap;
                                maxOverlap = Math.max( maxOverlap, overlap );
                                maxJac = Math.max( maxJac, jacDistance[i][j] );
                            }
                        }
                        List<Integer> indexes = new LinkedList<>();
                        for( int i = 0; i < size; i++ )
                        {
                            indexes.add( i );
                        }

                        while( true )
                        {
                            if( maxJac < parameters.getJaccardDistance() && maxOverlap < parameters.getMaxIntersectionLength() )
                                break;
                            maxOverlap = Integer.MIN_VALUE;
                            maxJac = Double.MIN_VALUE;
                            int mi = -1, mj = -1;
                            for( int i = 0; i < indexes.size(); i++ )
                            {
                                for( int j = i + 1; j < indexes.size(); j++ )
                                {
                                    double curJac = jacDistance[indexes.get( i )][indexes.get( j )];
                                    if( curJac > maxJac )
                                    {
                                        mi = i;
                                        mj = j;
                                        maxJac = curJac;
                                    }
                                    maxOverlap = Math.max( maxOverlap, overlaps[indexes.get( i )][indexes.get( j )] );
                                }
                            }
                            if( mi == -1 || mj == -1 )
                                break;

                            if( indexes.size() < 2 )
                                break;

                            Site si = sites[offset + mi];
                            Site sj = sites[offset + mj];
                            if( si.getLength() < sj.getLength() )
                            {
                                //remove first
                                indexes.remove( mi );
                            }
                            else
                            {
                                //remove second
                                indexes.remove( mj );
                            }
                        }

                        for( int i = 0; i < indexes.size(); i++ )
                        {
                            output.addSite( sites[offset + indexes.get( i )] );
                        }
                    }
                        break;
                    default:
                        throw new IllegalArgumentException( "Invalid siteSelectMode" );

                }
            }

            private double getPropertyValue(Site site)
            {
                return ( (Number)site.getProperties().getValue( parameters.getLeadingProperty() ) ).doubleValue();
            }
            private boolean isBetter(Site a, Site b)
            {
                double aValue = getPropertyValue( a );
                double bValue = getPropertyValue( b );
                if( parameters.getBestValueType().equals( Parameters.LARGEST_IS_BEST ) )
                    return aValue > bValue;
                if( parameters.getBestValueType().equals( Parameters.SMALLEST_IS_BEST ) )
                    return aValue < bValue;
                if( parameters.getBestValueType().equals( Parameters.EXTREME_IS_BEST ) )
                    return Math.abs( aValue ) > Math.abs( bValue );
                throw new IllegalArgumentException( "Invalid bestValueType parameter" );
            }
        } );

        output.finalizeAddition();
        parameters.getOutputTrack().save( output );
        return output;
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private static final String SET_OF_BEST_SITES = "Set of best sites";
        private static final String LONGEST_SET = "Longest set";
        private static final String LARGEST_SET = "Largest set";
        private static final String ONE_RANDOM = "One random";
        private static final String ONE_WITH_BEST_VALUE = "One with best value";
        private static final String ONE_SHORTEST = "One shortest";
        private static final String ONE_LONGEST = "One longest";
        private static final String FIVE_PRIME_MOST = "Most 5'";
        private static final String THREE_PRIME_MOST = "Most 3'";
        private static final String JACCARD_SIMILARITY = "Jaccard-like similarity";
        private static final String[] SITE_SELECT_MODES = {ONE_LONGEST, ONE_SHORTEST, ONE_WITH_BEST_VALUE, ONE_RANDOM, LARGEST_SET,
                LONGEST_SET, SET_OF_BEST_SITES, FIVE_PRIME_MOST, THREE_PRIME_MOST, JACCARD_SIMILARITY};

        private static final String LARGEST_IS_BEST = "Largest";
        private static final String EXTREME_IS_BEST = "Extreme";
        private static final String SMALLEST_IS_BEST = "Smallest";
        private static final String[] BEST_VALUE_TYPES = {LARGEST_IS_BEST, SMALLEST_IS_BEST, EXTREME_IS_BEST};

        private DataElementPath inputTrack;
        private BasicGenomeSelector genome;
        private DataElementPath outputTrack;
        private String siteSelectMode = SITE_SELECT_MODES[0];
        private String bestValueType = BEST_VALUE_TYPES[0];
        private String leadingProperty;
        private boolean independentStrands = false;

        private double jaccardDistance = 0.5;
        private int maxIntersectionLength = 0;

        public Parameters()
        {
            setGenome( new BasicGenomeSelector() );
        }

        @PropertyName ( "Input track" )
        @PropertyDescription ( "Track to filter" )
        public DataElementPath getInputTrack()
        {
            return inputTrack;
        }
        public void setInputTrack(DataElementPath inputTrack)
        {
            Object oldValue = this.inputTrack;
            this.inputTrack = inputTrack;
            if( inputTrack != null )
            {
                Track track = inputTrack.getDataElement( Track.class );
                genome.setFromTrack( track );
            }
            firePropertyChange( "inputTrack", oldValue, inputTrack );
        }

        @PropertyName ( "Genome" )
        @PropertyDescription ( "Reference genome" )
        public BasicGenomeSelector getGenome()
        {
            return genome;
        }
        public void setGenome(BasicGenomeSelector genome)
        {
            Object oldValue = this.genome;
            this.genome = genome;
            genome.setParent( this );
            firePropertyChange( "genome", oldValue, genome );
        }

        @PropertyName( "Independent strands" )
        @PropertyDescription( "Handle sites from different strands independently" )
        public boolean isIndependentStrands()
        {
            return independentStrands;
        }
        public void setIndependentStrands(boolean independentStrands)
        {
            Object oldValue = this.independentStrands;
            this.independentStrands = independentStrands;
            firePropertyChange( "independentStrands", oldValue, independentStrands );
        }
        public boolean isIndependentStrandsHidden()
        {
            return FIVE_PRIME_MOST.equals( siteSelectMode) || THREE_PRIME_MOST.equals( siteSelectMode );
        }

        @PropertyName ( "Overlapping site selection mode" )
        @PropertyDescription ( "How to select sites from the set overlapping sites" )
        public String getSiteSelectMode()
        {
            return siteSelectMode;
        }
        public void setSiteSelectMode(String siteSelectMode)
        {
            if(FIVE_PRIME_MOST.equals( siteSelectMode) || THREE_PRIME_MOST.equals( siteSelectMode ) )
                setIndependentStrands( true );
            Object oldValue = this.siteSelectMode;
            this.siteSelectMode = siteSelectMode;
            firePropertyChange( "siteSelectMode", oldValue, siteSelectMode );
        }

        @PropertyName ( "Leading property" )
        @PropertyDescription ( "Site property to be used for site selection" )
        public String getLeadingProperty()
        {
            return leadingProperty;
        }
        public void setLeadingProperty(String leadingProperty)
        {
            Object oldValue = this.leadingProperty;
            this.leadingProperty = leadingProperty;
            firePropertyChange( "leadingProperty", oldValue, leadingProperty );
        }

        public boolean isLeadingPropertyHidden()
        {
            return !siteSelectMode.equals( SET_OF_BEST_SITES ) && !siteSelectMode.equals( ONE_WITH_BEST_VALUE );
        }

        @PropertyName ( "Best value type" )
        @PropertyDescription ( "What is the best value of leading property?" )
        public String getBestValueType()
        {
            return bestValueType;
        }
        public void setBestValueType(String bestValueType)
        {
            Object oldValue = this.bestValueType;
            this.bestValueType = bestValueType;
            firePropertyChange( "bestValueType", oldValue, bestValueType );
        }

        @PropertyName ( "Output track" )
        @PropertyDescription ( "Resulting track" )
        public DataElementPath getOutputTrack()
        {
            return outputTrack;
        }
        public void setOutputTrack(DataElementPath outputTrack)
        {
            Object oldValue = this.outputTrack;
            this.outputTrack = outputTrack;
            firePropertyChange( "outputTrack", oldValue, outputTrack );
        }

        @PropertyName ( "Similarity cutoff" )
        @PropertyDescription ( "Cutoff for similarity coefficient: for each pair of sites intersection length is divided to minimal site length" )
        public double getJaccardDistance()
        {
            return jaccardDistance;
        }

        public void setJaccardDistance(double jaccardDistance)
        {
            Object oldValue = this.jaccardDistance;
            this.jaccardDistance = jaccardDistance;
            firePropertyChange( "jaccardDistance", oldValue, jaccardDistance );
        }

        @PropertyName ( "Allowed intersection" )
        @PropertyDescription ( "Maximal allowed intersection for all sites, is checked before similarity cutoff " )
        public int getMaxIntersectionLength()
        {
            return maxIntersectionLength;
        }

        public void setMaxIntersectionLength(int maxIntersectionLength)
        {
            Object oldValue = this.maxIntersectionLength;
            this.maxIntersectionLength = maxIntersectionLength;
            firePropertyChange( "maxIntersectionLength", oldValue, maxIntersectionLength );
        }

        public boolean isJaccardPropertyHidden()
        {
            return !siteSelectMode.equals( JACCARD_SIMILARITY );
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
            add( DataElementPathEditor.registerInput( "inputTrack", beanClass, Track.class ) );
            add( "genome" );
            add( "independentStrands" );
            property( "siteSelectMode" ).tags( Parameters.SITE_SELECT_MODES ).add();

            property(SitePropertySelector.registerSelector( new PropertyDescriptorEx( "leadingProperty", beanClass ), "inputTrack", false,
                            true ) ).hidden( "isLeadingPropertyHidden" ).add();

            property("bestValueType").hidden("isLeadingPropertyHidden").tags( Parameters.BEST_VALUE_TYPES ).add();
            property( "jaccardDistance" ).hidden( "isJaccardPropertyHidden" ).add();
            property( "maxIntersectionLength" ).hidden( "isJaccardPropertyHidden" ).add();

            property( "outputTrack" ).outputElement( Track.class ).auto( "$inputTrack$ filtered" ).add();
        }
    }
}
