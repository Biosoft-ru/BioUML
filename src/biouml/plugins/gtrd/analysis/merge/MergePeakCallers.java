package biouml.plugins.gtrd.analysis.merge;


import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.Option;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;
import ru.biosoft.util.bean.StaticDescriptor;

public class MergePeakCallers extends AnalysisMethodSupport<MergePeakCallers.Parameters>
{
    public MergePeakCallers(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        if( parameters.getCallerClusters().length == 0 )
            throw new IllegalArgumentException( "Add at least one peaks caller" );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {

        mergeTracks(parameters.getCallerClusters());
        return new Object[] {
                parameters.getOutputTrack().getDataElement( Track.class ),
                parameters.getMetaClusterToClusterTable().getDataElement( TableDataCollection.class ) };
    }

    private void mergeTracks(CallerClusters[] callerClusters)
    {
        Track firstTrack = StreamEx.of( callerClusters ).map( c->c.getClustersPath().getDataElement( Track.class ) ).findFirst().get();
        SqlTrack result = SqlTrack.createTrack( parameters.getOutputTrack(), firstTrack );

        TableDataCollection relationTable = TableDataCollectionUtils.createTableDataCollection( parameters.getMetaClusterToClusterTable() );
        ColumnModel cm = relationTable.getColumnModel();
        cm.addColumn( "Meta cluster id", Integer.class );
        cm.addColumn( "Cluster id", Integer.class );
        cm.addColumn( "Cluster peak caller", String.class );

        DataElementPath chromosomes = TrackUtils.getTrackSequencesPath( firstTrack );
        AtomicInteger metaClusterId = new AtomicInteger(1);
        AtomicInteger rowId = new AtomicInteger(1);
        jobControl.forCollection( chromosomes.getChildren(), chrPath -> {

            Center[] centers = StreamEx.of( callerClusters )
                    .flatMap( c->fetchCenters(c, chrPath) )
                    .sorted().toArray( Center[]::new );
            if(centers.length == 0)
                return true;

            int j = 0;
            for(int i = 1; i < centers.length; i++)
            {
                if(centers[i].pos - centers[i-1].pos > parameters.getMaxDistance())
                {
                    MetaCluster metaCluster = new MetaCluster( metaClusterId.incrementAndGet(), j, i, centers );
                    consumeMetaCluster( metaCluster, result, relationTable, rowId );
                    j = i;
                }
            }
            MetaCluster metaCluster = new MetaCluster(metaClusterId.incrementAndGet(), j, centers.length, centers);
            consumeMetaCluster( metaCluster, result, relationTable, rowId );
            return true;
        });

        relationTable.finalizeAddition();
        parameters.getMetaClusterToClusterTable().save( relationTable );

        result.finalizeAddition();
        parameters.getOutputTrack().save( result );
    }

    private void consumeMetaCluster(MetaCluster metaCluster, SqlTrack result, TableDataCollection relationTable, AtomicInteger rowId)
    {
        if(metaCluster.childs.length > 1)
        {
            result.addSite( metaCluster.createSite(parameters.isOutputCallerSpecificProperties(), parameters.getMaxPropWidth()) );

            for(Center c : metaCluster.childs)
            {
                Integer clusterId = (Integer)c.site.getProperties().getValue( "id" );
                TableDataCollectionUtils.addRow( relationTable, String.valueOf( rowId.incrementAndGet() ),
                        new Object[] {metaCluster.id, clusterId, c.caller.name()}, true );
            }
        }
    }

    private Stream<Center> fetchCenters(CallerClusters callerClusters, DataElementPath chrPath)
    {
        Track track = callerClusters.getClustersPath().getDataElement( Track.class );
        Sequence chr = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
        return track.getSites( chrPath.toString(), 0, chr.getLength() )
                .stream().map( s->new Center( s, callerClusters.getCaller() ) );
    }

    private static class MetaCluster
    {
        Center[] childs;
        int id;

        MetaCluster(int id, int from, int to, Center[] centers)
        {
            this.id = id;
            childs = new Center[to-from];
            System.arraycopy( centers, from, childs, 0, to-from );
        }

        Center bestChild()
        {
            Center res = childs[0];
            for(int i = 1; i < childs.length; i++)
            {
                Center cur = childs[i];
                if(cur.caller.ordinal() < res.caller.ordinal())
                    res = cur;
                else if(cur.caller == res.caller)
                {
                    int curSize = (Integer)cur.site.getProperties().getValue( "peak.count" );
                    int resSize = (Integer)res.site.getProperties().getValue( "peak.count" );
                    if(curSize > resSize)
                        res = cur;
                }
            }
            return res;
        }

        static final PropertyDescriptor DESCRIPTOR_META_CLUSTER_ID = StaticDescriptor.create( "id" );
        static final PropertyDescriptor DESCRIPTOR_PEAK_CALLER_LIST = StaticDescriptor.create( "peak-caller.list" );
        static final PropertyDescriptor DESCRIPTOR_PEAK_CALLER_SET = StaticDescriptor.create( "peak-caller.set" );
        static final PropertyDescriptor DESCRIPTOR_PEAK_CALLER_COUNT = StaticDescriptor.create( "peak-caller.count" );
        static final PropertyDescriptor DESCRIPTOR_PEAK_COUNT = StaticDescriptor.create( "peak.count" );
        static final Map<String, PropertyDescriptor> DESCRIPTOR_CACHE = new HashMap<>();

        Site createSite(boolean outputCallerSpecificProperties, int maxPropWidth)
        {
            Center bestChild = bestChild();
            Sequence sequence = bestChild.site.getOriginalSequence();

            DynamicPropertySet properties = new DynamicPropertySetAsMap();

            properties.add( new DynamicProperty( DESCRIPTOR_META_CLUSTER_ID, Integer.class, id ) );

            properties.add( bestChild.site.getProperties().getProperty( "summit" ) );

            List<String> callerList = StreamEx.of( childs ).map( c->c.caller.name() ).toList();
            properties.add( new DynamicProperty( DESCRIPTOR_PEAK_CALLER_LIST, String.class, strJoinTruncate( callerList, maxPropWidth ) ) );
            TreeSet<String> callerSet = new TreeSet<>(callerList);
            properties.add( new DynamicProperty( DESCRIPTOR_PEAK_CALLER_SET, String.class, strJoinTruncate( callerSet, maxPropWidth ) ) );
            properties.add( new DynamicProperty( DESCRIPTOR_PEAK_CALLER_COUNT, Integer.class, callerSet.size() ) );

            int totalPeakCount = StreamEx.of( childs ).map( c->c.site.getProperties().getValue( "peak.count" ) ).nonNull().mapToInt( x->(Integer)x ).sum();
            properties.add( new DynamicProperty( DESCRIPTOR_PEAK_COUNT, Integer.class, totalPeakCount ) );

            if( outputCallerSpecificProperties )
            {
                for( Caller caller : Caller.values() )
                    addCallerSpecificProperties( properties, caller, maxPropWidth );
            }

            String[] propertiesToMerge = new String[] {"exp", "cell", "treatment", "antibody"};
            for(String name : propertiesToMerge)
            {
                Set<String> valSet = StreamEx.of( childs ).map( c->(String)c.site.getProperties().getValue( name + ".set" ) ).nonNull()
                        .flatMap( s -> StreamEx.split(s, ';' ) ).toSet();
                String val = strJoinTruncate( valSet,  maxPropWidth );
                PropertyDescriptor descriptor = DESCRIPTOR_CACHE.computeIfAbsent( name + ".set", StaticDescriptor::create );
                properties.add( new DynamicProperty( descriptor,  String.class, val ) );
            }

            Site result = new SiteImpl( null, "", SiteType.TYPE_TRANSCRIPTION_FACTOR, Basis.BASIS_PREDICTED, bestChild.site.getStart(),
                    bestChild.site.getLength(), Precision.PRECISION_CUT_BOTH, StrandType.STRAND_NOT_KNOWN, sequence, properties );
            return result;
        }

        private void addCallerSpecificProperties(DynamicPropertySet properties, Caller caller, int maxPropWidth)
        {
            List<Center> callerChilds = StreamEx.of( childs ).filter( c->c.caller == caller ).toList();
            if(callerChilds.isEmpty())
                return;
            if(callerChilds.size() == 1)
            {
                for(DynamicProperty dp : callerChilds.get( 0 ).site.getProperties() )
                {
                    PropertyDescriptor descriptor = DESCRIPTOR_CACHE.computeIfAbsent( caller.name() + "." + dp.getName(), StaticDescriptor::create );
                    properties.add( new DynamicProperty( descriptor, dp.getType(), dp.getValue() ) );
                }
            }else
            {
                List<DynamicPropertySet> dpsList = StreamEx.of( callerChilds ).map( c->c.site.getProperties() ).toList();
                Set<String> names = new HashSet<>();
                for(DynamicPropertySet dps : dpsList)
                    for(DynamicProperty dp : dps)
                        names.add(dp.getName());
                for(String name : names)
                {
                    PropertyDescriptor descriptor = DESCRIPTOR_CACHE.computeIfAbsent( caller.name() + "." + name, StaticDescriptor::create );

                    if(name.endsWith( ".list" ))
                    {
                        Iterable<String> strList = StreamEx.of( dpsList )
                                .map( dps->dps.getValue( name ) )
                                .map( v->v==null?"":(String)v );
                        String val = strJoinTruncate(strList, maxPropWidth );
                        properties.add( new DynamicProperty( descriptor,  String.class, val ) );
                    }else if(name.endsWith( ".set" ))
                    {
                        Set<String> valSet = StreamEx.of( dpsList ).map( dps->(String)dps.getValue( name ) ).nonNull().toSet();
                        String val = strJoinTruncate( valSet, maxPropWidth );
                        properties.add( new DynamicProperty( descriptor,  String.class, val ) );
                    }else if(name.endsWith( ".count" ))
                    {
                        int val = StreamEx.of( dpsList ).map( dps->dps.getValue( name ) ).nonNull().mapToInt( x->(Integer)x ).sum();
                        properties.add( new DynamicProperty(descriptor, Integer.class, val) );
                    }else if(name.endsWith( ".min" ))
                    {
                        OptionalDouble val = StreamEx.of( dpsList ).map( dps->dps.getValue( name ) ).nonNull().mapToDouble( x->((Number)x).doubleValue() ).min();
                        if(val.isPresent())
                            properties.add( new DynamicProperty(descriptor, Double.class, val.getAsDouble()) );
                    }else if(name.endsWith( ".max" ))
                    {
                        OptionalDouble val = StreamEx.of( dpsList ).map( dps->dps.getValue( name ) ).nonNull().mapToDouble(x->((Number)x).doubleValue() ).max();
                        if(val.isPresent())
                            properties.add( new DynamicProperty(descriptor, Double.class, val.getAsDouble()) );
                    }else if(name.endsWith( ".mean" ))
                    {
                        OptionalDouble val = StreamEx.of( dpsList ).map( dps->dps.getValue( name ) ).nonNull().mapToDouble( x->((Number)x).doubleValue() ).average();
                        if(val.isPresent())
                            properties.add( new DynamicProperty(descriptor, Double.class, val.getAsDouble()) );
                    }else if(name.endsWith( ".median" ))
                    {
                        String listProp = name.replace( ".median", ".list" );
                        double[] values = StreamEx.of( dpsList ).map( dps -> dps.getValueAsString( listProp ) ).nonNull()
                                .flatMap( s -> StreamEx.split( s, ';' ) ).remove( String::isEmpty ).mapToDouble( Double::parseDouble )
                                .sorted().toArray();
                        if( values.length > 0 )
                        {
                            double median;
                            if( values.length % 2 == 1 )
                                median = values[values.length / 2];
                            else
                                median = ( values[values.length / 2] + values[values.length / 2 - 1] ) / 2;
                            properties.add( new DynamicProperty( descriptor, Double.class, median ) );
                        }
                    }else
                    {
                        descriptor = DESCRIPTOR_CACHE.computeIfAbsent( caller.name() + "." + name + ".list", StaticDescriptor::create );
                        Iterable<String> strList = StreamEx.of( dpsList ).map( dps->dps.getValueAsString( name ) ).nonNull();
                        properties.add( new DynamicProperty( descriptor,  String.class, strJoinTruncate( strList, maxPropWidth ) ) );
                    }
                }
            }
        }

        private String strJoinTruncate(Iterable<?> strList, int maxPropWidth)
        {
            return TextUtil.joinTruncate( strList, maxPropWidth, ";", "..." );
        }
    }

    public static enum Caller
    {
        GEM, PICS, MACS2, SISSRS;
    }

    private static class Center implements Comparable<Center>
    {
        final int pos;
        final Site site;
        final Caller caller;
        public Center(Site site, Caller caller)
        {
            this.pos = (Integer)site.getProperties().getValue( "summit" ) + site.getFrom();
            this.site = site;
            this.caller = caller;
        }
        @Override
        public int compareTo(Center o)
        {
            return Integer.compare( pos, o.pos );
        }
    }

    public static class CallerClusters extends Option implements JSONBean
    {
        public CallerClusters()
        {
        }
        public CallerClusters(Caller caller)
        {
            this(caller, null);
        }
        public CallerClusters(Caller caller, DataElementPath path)
        {
            this.caller = caller;
            this.clustersPath = path;
        }

        private Caller caller = Caller.values()[0];
        public Caller getCaller() { return caller; }
        public String getCallerName()
        {
            return caller.name();
        }
        public void setCallerName(String callerName)
        {
            Object oldValue = this.caller.name();
            caller = Caller.valueOf( callerName );
            firePropertyChange( "callerName", oldValue, callerName );
        }

        private DataElementPath clustersPath;
        public DataElementPath getClustersPath()
        {
            return clustersPath;
        }
        public void setClustersPath(DataElementPath clustersPath)
        {
            Object oldValue = this.clustersPath;
            this.clustersPath = clustersPath;
            firePropertyChange( "clustersPath", oldValue, clustersPath );
        }
    }
    public static class CallerClustersBeanInfo extends BeanInfoEx2<CallerClusters>
    {
        public CallerClustersBeanInfo()
        {
            super( CallerClusters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            String[] callers = StreamEx.of( Caller.values() ).map( Caller::name ).toArray( String[]::new );
            property("callerName").tags( callers ).add();
            property("clustersPath").inputElement( Track.class ).add();
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private CallerClusters[] callerClusters = StreamEx.of( Caller.values() ).map( CallerClusters::new ).toArray( CallerClusters[]::new );

        public CallerClusters[] getCallerClusters()
        {
            return callerClusters;
        }
        public void setCallerClusters(CallerClusters[] callerClusters)
        {
            Object oldValue = this.callerClusters;
            this.callerClusters = callerClusters;
            firePropertyChange( "callerClusters", oldValue, callerClusters );
        }

        private int maxDistance = 50;
        public int getMaxDistance()
        {
            return maxDistance;
        }
        public void setMaxDistance(int maxDistance)
        {
            int oldValue = this.maxDistance;
            this.maxDistance = maxDistance;
            firePropertyChange( "maxDistance", oldValue, maxDistance );
        }

        private int maxPropWidth = 100;
        public int getMaxPropWidth()
        {
            return maxPropWidth;
        }
        public void setMaxPropWidth(int maxPropWidth)
        {
            int oldValue = this.maxPropWidth;
            this.maxPropWidth = maxPropWidth;
            firePropertyChange( "maxPropWidth", oldValue, maxPropWidth );
        }

        private boolean outputCallerSpecificProperties = false;
        public boolean isOutputCallerSpecificProperties()
        {
            return outputCallerSpecificProperties;
        }
        public void setOutputCallerSpecificProperties(boolean outputCallerSpecificProperties)
        {
            boolean oldValue = this.outputCallerSpecificProperties;
            this.outputCallerSpecificProperties = outputCallerSpecificProperties;
            firePropertyChange( "outputCallerSpecificProperties", oldValue, outputCallerSpecificProperties );
        }

        private DataElementPath outputTrack;
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

        private DataElementPath metaClusterToClusterTable;
        public DataElementPath getMetaClusterToClusterTable()
        {
            return metaClusterToClusterTable;
        }
        public void setMetaClusterToClusterTable(DataElementPath metaClusterToClusterTable)
        {
            Object oldValue = this.metaClusterToClusterTable;
            this.metaClusterToClusterTable = metaClusterToClusterTable;
            firePropertyChange( "metaClusterToClusterTable", oldValue, metaClusterToClusterTable );
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
            add("callerClusters");
            add( "maxDistance" );
            add( "maxPropWidth" );
            add( "outputCallerSpecificProperties" );
            property( "outputTrack" ).outputElement( SqlTrack.class ).add();
            property( "metaClusterToClusterTable" ).outputElement( TableDataCollection.class ).add();
        }
    }
}
