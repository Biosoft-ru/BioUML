package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.StaticDescriptor;

@ClassIcon ( "resources/clustertrack.gif" )
public class ClusterTrack extends AnalysisMethodSupport<ClusterTrack.Parameters>
{
    private static final PropertyDescriptor CLUSTER_SIZE_DESCRIPTOR = StaticDescriptor.create( "Cluster size" );
    private static final PropertyDescriptor SITE_NAME_DESCRIPTOR = StaticDescriptor.create( "name" );
    private static final PropertyDescriptor CLUSTER_SITES_DESCRIPTOR = StaticDescriptor.create( "Clustered" );
    private static final PropertyDescriptor CLUSTER_SITE_NAMES_DESCRIPTOR = StaticDescriptor.create( "Clustered sites" );

    public ClusterTrack(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final Track inputTrack = parameters.getInputTrack().getDataElement( Track.class );
        final SqlTrack result = SqlTrack.createTrack( parameters.getClusterTrack(), inputTrack  );

        final AtomicInteger clusterCounter = new AtomicInteger( 0 );
        jobControl.forCollection( parameters.getGenome().getSequenceCollectionPath().getChildren(), new Iteration<ru.biosoft.access.core.DataElementPath>()
        {
            @Override
            public boolean run(DataElementPath chrPath)
            {
                try
                {
                    Sequence chr = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
                    DataCollection<Site> sites = inputTrack.getSites( chrPath.toString(), 0, chr.getLength() );
                    SortedMap<Interval, String> intervalsMap = new TreeMap<>();
                    SortedMap<Interval, Set<String>> siteNamesMap = new TreeMap<>();
                    boolean siteModelFound = false;
                    for(Site s : sites)
                    {
                        String siteModel = s.getProperties().getValueAsString( "siteModel" );
                        intervalsMap.put( s.getInterval(), siteModel );
                        if(siteModel != null)
                            siteModelFound = true;
                        String siteName = s.getProperties().getValueAsString( "name" );
                        if( siteName == null )
                            siteName = s.getName();
                        siteNamesMap.computeIfAbsent( s.getInterval(), v -> new HashSet<>() ).add( siteName );
                    }
                    if( intervalsMap.isEmpty() )
                        return true;
                    
                    Interval curCluster = intervalsMap.firstKey();
                    int curSize = 0;
                    Set<String> siteSet = new TreeSet<>();
                    Set<String> addedSites = new TreeSet<>();
                    for( Map.Entry<Interval, String> entry : intervalsMap.entrySet() )
                    {
                        Interval site = entry.getKey();
                        if( site.getFrom() > curCluster.getTo() + parameters.getMaxDistance() )
                        {
                            addCluster( curCluster, curSize, chr, siteModelFound ? siteSet : null, siteModelFound ? null : addedSites );
                            curCluster = site;
                            curSize = 0;
                            siteSet = new TreeSet<>();
                            addedSites = new TreeSet<>();
                        }
                        curCluster = new Interval( curCluster.getFrom(), Math.max( curCluster.getTo(), site.getTo() ) );
                        if( entry.getValue() != null )
                        {
                            siteSet.add( entry.getValue()  );
                        }
                        addedSites.addAll( siteNamesMap.get( entry.getKey() ) );
                        curSize++;
                    }
                    addCluster( curCluster, curSize, chr, siteModelFound ? siteSet : null, siteModelFound ? null : addedSites );
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException( e );
                }
                return true;
            }

            private void addCluster(Interval cluster, int size, Sequence seq, Set<String> siteModels, Set<String> siteNames)
            {
                if(size >= parameters.getMinClusterSize())
                {
                    DynamicPropertySet prop = new DynamicPropertySetAsMap();
                    prop.add( new DynamicProperty( CLUSTER_SIZE_DESCRIPTOR, Integer.class, size ) );
                    prop.add( new DynamicProperty( SITE_NAME_DESCRIPTOR, String.class, "Cluster " + clusterCounter.incrementAndGet() ) );
                    if( siteModels != null )
                        prop.add( new DynamicProperty( CLUSTER_SITES_DESCRIPTOR, String.class, String.join( "; ", siteModels ) ) );
                    if( siteNames != null )
                        prop.add( new DynamicProperty( CLUSTER_SITE_NAMES_DESCRIPTOR, String.class, String.join( "; ", siteNames ) ) );
                    Site site = new SiteImpl( result, null, SiteType.TYPE_MISC_FEATURE, Basis.BASIS_PREDICTED, cluster
                            .getFrom(), cluster.getLength(), Precision.PRECISION_EXACTLY, StrandType.STRAND_BOTH, seq, prop );
                    result.addSite( site );
                }
            }
        } );

        result.finalizeAddition();
        parameters.getClusterTrack().save( result );
        log.info( clusterCounter + " clusters were found." );
        return result;
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTrack;
        private BasicGenomeSelector genome;
        private int maxDistance = 1000;
        private int minClusterSize = 3;
        private DataElementPath clusterTrack;

        public Parameters()
        {
            setGenome( new BasicGenomeSelector() );
        }

        @PropertyName("Input track")
        @PropertyDescription("Input track")
        public DataElementPath getInputTrack()
        {
            return inputTrack;
        }
        public void setInputTrack(DataElementPath inputTrack)
        {
            Object oldValue = this.inputTrack;
            this.inputTrack = inputTrack;
            if(!Objects.equals( inputTrack, oldValue ))
            {
                Track t = inputTrack.getDataElement(Track.class);
                getGenome().setFromTrack( t );
            }
            firePropertyChange( "inputTrack", oldValue, inputTrack );
        }


        @PropertyName("Genome")
        @PropertyDescription("Reference genome")
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

        @PropertyName("Max distance")
        @PropertyDescription("Maximum allowed distance between adjacent sites in cluster")
        public int getMaxDistance()
        {
            return maxDistance;
        }
        public void setMaxDistance(int maxDistance)
        {
            Object oldValue = this.maxDistance;
            this.maxDistance = maxDistance;
            firePropertyChange( "maxDistance", oldValue, maxDistance );
        }


        @PropertyName("Cluster size")
        @PropertyDescription("Minimal size of cluster")
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

        @PropertyName("Cluster track")
        @PropertyDescription("Resulting track with cluster sites")
        public DataElementPath getClusterTrack()
        {
            return clusterTrack;
        }
        public void setClusterTrack(DataElementPath clusterTrack)
        {
            Object oldValue = this.clusterTrack;
            this.clusterTrack = clusterTrack;
            firePropertyChange( "clusterTrack", oldValue, clusterTrack );
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
            property( "inputTrack" ).inputElement( Track.class ).add();
            add("genome");
            add("maxDistance");
            add("minClusterSize");

            property( "clusterTrack" ).outputElement( SqlTrack.class ).auto( "$inputTrack$ clusters" ).add();
        }
    }
}
