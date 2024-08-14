package biouml.plugins.gtrd.analysis.merge;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import gnu.trove.list.array.TDoubleArrayList;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.AnnotatedSequence;
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
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.StaticDescriptor;


public class MergePeaks extends AnalysisMethodSupport<MergePeaks.Parameters>
{
    public MergePeaks(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Track inputTrack = parameters.getInputTrack().getDataElement( Track.class );

        jobControl.pushProgress( 0, 80 );
        log.info( "Fetching track data" );
        Map<String, int[]> allCenters = fetchCenters( inputTrack );
        jobControl.popProgress();

        jobControl.pushProgress( 80, 85 );
        log.info( "Clustering site centers" );
        Map<String, Interval[]> allClusters = getClusters(allCenters);
        jobControl.popProgress();

        jobControl.pushProgress( 85, 90 );
        log.info( "Estimating peak center standard deviation" );
        double sd = estimateSD(allCenters, allClusters);
        allCenters = null;
        log.info( "Global SD = " + sd );
        jobControl.popProgress();

        jobControl.pushProgress( 90, 100 );
        log.info( "Computing center and width of clusters" );
        computeClusters(allClusters, sd, inputTrack);
        jobControl.popProgress();

        return new Object[] {
                parameters.getOutputTrack().getDataElement( Track.class ),
                parameters.getClusterToPeakTable().getDataElement( TableDataCollection.class )};
    }


    private void computeClusters(Map<String, Interval[]> allClusters, double globalSD, Track inputTrack)
    {
        SqlTrack outputTrack = SqlTrack.createTrack( parameters.getOutputTrack(), inputTrack );

        TableDataCollection clusterToPeakTable = TableDataCollectionUtils.createTableDataCollection( parameters.getClusterToPeakTable() );
        ColumnModel cm = clusterToPeakTable.getColumnModel();
        cm.addColumn( "Cluster id", DataType.Integer);
        cm.addColumn( "Peak id", DataType.Text );

        int clusterId = 1;
        int rowId = 1;
        for( Map.Entry<String, Interval[]> e : allClusters.entrySet() )
        {
            String chr = e.getKey();
            Interval[] chrClusters = e.getValue();
            Peak[] peaks = fetchPeaksSorted( chr, inputTrack );
            for( Interval cluster : chrClusters )
            {
                double median;
                if(cluster.getLength() % 2 == 0)
                    median = (peaks[cluster.getCenter()].center + peaks[cluster.getCenter() + 1].center)/2.0;
                else
                    median = peaks[cluster.getCenter()].center;
                double sd = globalSD;
                if( !parameters.isUseGlobalSD() && cluster.getLength() > 1 )
                {
                    double mean = 0;
                    for( int i = cluster.getFrom(); i <= cluster.getTo(); i++ )
                        mean += peaks[i].center;
                    mean /= cluster.getLength();
                    sd = 0;
                    for( int i = cluster.getFrom(); i <= cluster.getTo(); i++ )
                        sd += ( peaks[i].center - mean ) * ( peaks[i].center - mean );
                    sd = Math.sqrt( sd / ( cluster.getLength() - 1 ) );
                }

                double w = parameters.getBindingSiteWidth()/2.0 + 2*sd/Math.sqrt( cluster.getLength() );

                int from = (int)Math.floor( median - w );
                int to = (int) Math.ceil( median + w );
                int summit = (int)Math.round( median ) - from;

                List<DynamicPropertySet> dpsList = StreamEx.of( peaks, cluster.getFrom(), cluster.getTo() + 1 ).map( p -> p.site.getProperties() ).toList();
                DynamicPropertySet mergedProps = mergeProperties( dpsList );

                addCluster( outputTrack, chr, from, to, summit, cluster.getLength(), mergedProps, clusterId );

                for(int i = cluster.getFrom(); i <= cluster.getTo(); i++)
                {
                    Peak peak = peaks[i];
                    TableDataCollectionUtils.addRow( clusterToPeakTable, String.valueOf( rowId++ ), new Object[]{clusterId, peak.getId()}, true );
                }

                clusterId++;
            }
        }

        clusterToPeakTable.finalizeAddition();
        parameters.getClusterToPeakTable().save( clusterToPeakTable );

        outputTrack.finalizeAddition();
        parameters.getOutputTrack().save( outputTrack );

    }


    private Map<String, PropertyDescriptor> mergedPD = new HashMap<>();

    private DynamicPropertySet mergeProperties(List<DynamicPropertySet> dpsList)
    {
        DynamicPropertySet res = new DynamicPropertySetAsMap();
        Set<String> stringProps = new HashSet<>();
        Set<String> numProps = new HashSet<>();
        for(DynamicPropertySet dps : dpsList)
            for(DynamicProperty dp : dps)
            {
                if(dp.getType().equals( String.class ))
                    stringProps.add( dp.getName() );
                else if(Number.class.isAssignableFrom( dp.getType()))
                    numProps.add( dp.getName() );
            }
        for(String pName : stringProps)
        {
            List<String> valueList = new ArrayList<>();
            Set<String> valueSet = new LinkedHashSet<>();

            for(DynamicPropertySet dps : dpsList)
            {
                String value = (String)dps.getValue( pName );
                if(value == null)
                    value = "";
                valueList.add( value );
                valueSet.add( value );
            }
            PropertyDescriptor pdList = mergedPD.computeIfAbsent( pName + ".list", StaticDescriptor::create );
            res.add( new DynamicProperty( pdList, String.class, strJoinTruncate( valueList ) ) );
            PropertyDescriptor pdSet = mergedPD.computeIfAbsent( pName + ".set", StaticDescriptor::create );
            res.add( new DynamicProperty( pdSet, String.class, strJoinTruncate( valueSet ) ) );
            PropertyDescriptor pdCount = mergedPD.computeIfAbsent( pName + ".count", StaticDescriptor::create );
            res.add( new DynamicProperty( pdCount, Integer.class, valueSet.size() ) );
        }

        for(String pName : numProps)
        {
            List<Double> valueList = new ArrayList<>();
            List<String> strList = new ArrayList<>();
            for(DynamicPropertySet dps : dpsList)
            {
                Number value = (Number)dps.getValue( pName );
                if(value != null)
                    valueList.add( value.doubleValue() );
                strList.add( value == null ? "" : value.toString() );
            }
            PropertyDescriptor pdList = mergedPD.computeIfAbsent( pName + ".list", StaticDescriptor::create );
            res.add( new DynamicProperty( pdList, String.class, strJoinTruncate( valueList ) ) );

            if(valueList.isEmpty())
                continue;

            PropertyDescriptor pdMedian = mergedPD.computeIfAbsent( pName + ".median", StaticDescriptor::create );
            Collections.sort( valueList );
            double median;
            if(valueList.size() % 2 == 0)
                median = valueList.get( valueList.size() / 2 ) + valueList.get( valueList.size() / 2 - 1 );
            else
                median = valueList.get( valueList.size() / 2 );
            res.add( new DynamicProperty( pdMedian, Double.class, median ) );

            DoubleSummaryStatistics summaryStatistics = valueList.stream().mapToDouble( Double::doubleValue ).summaryStatistics();
            PropertyDescriptor pdMean = mergedPD.computeIfAbsent( pName + ".mean", StaticDescriptor::create );
            PropertyDescriptor pdMin = mergedPD.computeIfAbsent( pName + ".min", StaticDescriptor::create );
            PropertyDescriptor pdMax = mergedPD.computeIfAbsent( pName + ".max", StaticDescriptor::create );
            res.add( new DynamicProperty( pdMean, Double.class, summaryStatistics.getAverage() ) );
            res.add( new DynamicProperty( pdMin, Double.class, summaryStatistics.getMin() ) );
            res.add( new DynamicProperty( pdMax, Double.class, summaryStatistics.getMax() ) );
        }
        return res;
    }

    private String strJoinTruncate(Iterable<?> strList)
    {
        return TextUtil.joinTruncate( strList, parameters.getMaxPropWidth(), ";", "..." );
    }


    public static final PropertyDescriptor CLUSTER_SIZE_DESCRIPTOR = StaticDescriptor.create( "peak.count" );
    public static final PropertyDescriptor CLUSTER_SUMMIT_DESCRIPTOR = StaticDescriptor.create( "summit" );
    public static final PropertyDescriptor CLUSTER_ID_DESCRIPTOR = StaticDescriptor.create( "id" );
    private static final double DEFAULT_SD = 20;

    private void addCluster(SqlTrack outputTrack, String chr, int from, int to, int summit, int size, DynamicPropertySet mergedProps, int clusterId)
    {
        DynamicPropertySet prop = new DynamicPropertySetAsMap();
        prop.add( new DynamicProperty( CLUSTER_SIZE_DESCRIPTOR, Integer.class, size ) );
        prop.add( new DynamicProperty( CLUSTER_SUMMIT_DESCRIPTOR, Integer.class, summit ) );
        prop.add( new DynamicProperty( CLUSTER_ID_DESCRIPTOR, Integer.class, clusterId ) );
        for(DynamicProperty dp : mergedProps)
            prop.add( dp );
        Site site = new SiteImpl( outputTrack, chr, SiteType.TYPE_MISC_FEATURE, Basis.BASIS_PREDICTED, from, to - from + 1,
                Precision.PRECISION_EXACTLY, StrandType.STRAND_NOT_KNOWN, null, prop );
        outputTrack.addSite( site );
    }

    private Map<String, Interval[]> getClusters(Map<String, int[]> allCenters)
    {
        return EntryStream.of( allCenters ).mapValues( centers -> {
            List<Interval> res;
            if( parameters.isUseDensityClusterization() )
                res = findDensityClusters( centers );
            else
                res = findClusters( centers );
            return res.toArray( new Interval[0] );
        } ).toMap();
    }

    private static class Peak implements Comparable<Peak>{
        public final int center;
        public final Site site;

        public Peak(Site s)
        {
            this.site = s;
            Integer summit = (Integer)s.getProperties().getValue( "summit" );
            center = summit != null ? s.getFrom() + summit : s.getInterval().getCenter();
        }

        public String getId()
        {
            String id = site.getProperties().getValueAsString( "id" );
            if(id != null)
                return id;
            return site.getName();
        }

        @Override
        public int compareTo(Peak o)
        {
            return Integer.compare( center, o.center );
        }
    }

    private Map<String, int[]> fetchCenters(Track inputTrack)
    {
        DataElementPath chromosomes = TrackUtils.getTrackSequencesPath( inputTrack );
        Map<String, int[]> result = new HashMap<>();
        jobControl.forCollection( chromosomes.getChildren(), chrPath -> {
            try
            {
                Sequence chr = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
                DataCollection<Site> sites = inputTrack.getSites( chrPath.toString(), 0, chr.getLength() );
                int[] centers = new int[sites.getSize()];
                int i = 0;
                for(Site s : sites)
                {
                    Peak peak = new Peak( s );
                    centers[i++] = peak.center;
                }
                Arrays.sort( centers );
                result.put( chr.getName(), centers );
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            return true;
        });
        return result;
    }

    private Peak[] fetchPeaksSorted(String chrName, Track inputTrack) {
        DataElementPath chromosomes = TrackUtils.getTrackSequencesPath( inputTrack );
        DataElementPath chrPath = chromosomes.getChildPath( chrName );
        Sequence chr = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
        DataCollection<Site> sites = inputTrack.getSites( chrPath.toString(), 0, chr.getLength() );
        return sites.stream().map( Peak::new ).sorted().toArray( Peak[]::new );
    }

    private double estimateSD(Map<String, int[]> allCenters, Map<String, Interval[]> allClusters)
    {
        TableDataCollection sdTable = TableDataCollectionUtils.createTableDataCollection( parameters.getSdTable() );
        sdTable.getColumnModel().addColumn( "sd", DataType.Float );
        sdTable.getColumnModel().addColumn( "n", DataType.Integer );
        sdTable.getColumnModel().addColumn( "mean", DataType.Float );

        TDoubleArrayList sdList = new TDoubleArrayList();
        for( Map.Entry<String, Interval[]> e : allClusters.entrySet() )
        {
            String chr = e.getKey();
            Interval[] chrClusters = e.getValue();
            int[] centers = allCenters.get( chr );
            for( Interval cluster : chrClusters )
            {
                if(cluster.getLength() <= 1)
                    continue;
                double mean = 0;
                for(int i = cluster.getFrom(); i <= cluster.getTo(); i++)
                    mean += centers[i];
                mean /= cluster.getLength();
                double sd = 0;
                for(int i = cluster.getFrom(); i <= cluster.getTo(); i++)
                    sd += (centers[i] - mean) * (centers[i] - mean);
                sd = Math.sqrt( sd / (cluster.getLength() - 1) );
                sdList.add( sd );
                TableDataCollectionUtils.addRow( sdTable, chr + cluster, new Object[]{sd, cluster.getLength(), mean}, true );
            }
        }

        sdTable.finalizeAddition();
        parameters.getSdTable().save( sdTable );

        if(sdList.isEmpty())
            return DEFAULT_SD;
        sdList.sort();
        double median = sdList.get( sdList.size()/2 );

        return median;
    }

    private List<Interval> findClusters(int[] centers)
    {
        if( centers.length == 0 )
            return Collections.emptyList();
        List<Interval> result = new ArrayList<>();
        int i = 0;
        for( int j = 1; j < centers.length; j++ )
            if( centers[j] - centers[j - 1] > parameters.getMaxDistance() )
            {
                result.add( new Interval(i, j - 1) );
                i = j;
            }
        result.add( new Interval( i, centers.length - 1 ) );
        return result;
    }

    private List<Interval> findDensityClusters(int[] centers)
    {
        if(centers.length == 0)
            return Collections.emptyList();

        int[] dens = computeDensity( centers );
        int[] delta = computeDelta( dens, centers );

        List<Integer> clusterCenters = new ArrayList<>();
        for( int i = 0; i < centers.length; i++ )
            if( dens[i] >= parameters.getMinDensity() && delta[i] >= parameters.getMinDelta() )
                clusterCenters.add( i );

        List<Interval> result = new ArrayList<>();
        int from = 0;
        for( int i = 0; i < clusterCenters.size() - 1; i++ )
        {
            int curCluster = clusterCenters.get( i );
            int nextCluster = clusterCenters.get( i + 1 );
            int to = curCluster;
            while( centers[to] - centers[curCluster] < centers[nextCluster] - centers[to] )
                to++;
            result.add( new Interval( from, to - 1 ) );
            from = to;
        }
        result.add( new Interval( from, centers.length - 1 ) );

        return result;
    }

    private int[] computeDensity(int[] points)
    {
        int[] dens = new int[points.length];
        int l = 0;
        int r = 0;
        for( int i = 0; i < points.length; i++ )
        {
            while( points[l] + parameters.getMaxDistance() < points[i] )
                l++;
            while( r < points.length && points[r] - parameters.getMaxDistance() < points[i] )
                r++;
            dens[i] = r - l;
        }
        return dens;
    }

    private static int[] computeDelta(int[] dens, int[] points)
    {
        int[] lg = new int[dens.length];
        for( int i = 0; i < dens.length; i++ )
        {
            int j = i - 1;
            while( j >= 0 && dens[j] <= dens[i] )
                j = lg[j];
            lg[i] = j;
        }
        int[] rg = new int[dens.length];
        for( int i = dens.length - 1; i >= 0; i-- )
        {
            int j = i + 1;
            while( j < dens.length && dens[j] < dens[i] )
                j = rg[j];
            rg[i] = j;
        }
        int[] delta = new int[dens.length];
        for( int i = 0; i < dens.length; i++ )
        {
            int l = lg[i];
            int r = rg[i];
            if( l == -1 && r == dens.length )
                delta[i] = Math.max( points[i] - points[0], points[points.length - 1] - points[i] );
            else if( l == -1 )
                delta[i] = points[r] - points[i];
            else if( r == dens.length )
                delta[i] = points[i] - points[l];
            else
                delta[i] = Math.min( points[i] - points[l], points[r] - points[i] );
        }
        return delta;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTrack;
        public DataElementPath getInputTrack()
        {
            return inputTrack;
        }
        public void setInputTrack(DataElementPath inputTrack)
        {
            Object oldValue = this.inputTrack;
            this.inputTrack = inputTrack;
            firePropertyChange( "inputTrack", oldValue, inputTrack );
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

        private boolean useDensityClusterization;
        public boolean isUseDensityClusterization()
        {
            return useDensityClusterization;
        }
        public void setUseDensityClusterization(boolean useDensityClusterization)
        {
            boolean oldValue = this.useDensityClusterization;
            this.useDensityClusterization = useDensityClusterization;
            firePropertyChange( "useDensityClusterization", oldValue, useDensityClusterization );
        }
        public boolean isNotUseDensityClusterization()
        {
            return !isUseDensityClusterization();
        }

        private int minDensity = 0;
        public int getMinDensity()
        {
            return minDensity;
        }
        public void setMinDensity(int minDensity)
        {
            int oldValue = this.minDensity;
            this.minDensity = minDensity;
            firePropertyChange( "minDensity", oldValue, minDensity );
        }

        private int minDelta = 100;
        public int getMinDelta()
        {
            return minDelta;
        }
        public void setMinDelta(int minDelta)
        {
            int oldValue = this.minDelta;
            this.minDelta = minDelta;
            firePropertyChange( "minDelta", oldValue, minDelta );
        }

        private boolean useGlobalSD = false;
        public boolean isUseGlobalSD()
        {
            return useGlobalSD;
        }
        public void setUseGlobalSD(boolean useGlobalSD)
        {
            boolean oldValue = this.useGlobalSD;
            this.useGlobalSD = useGlobalSD;
            firePropertyChange( "useGlobalSD", oldValue, useGlobalSD );
        }

        private int bindingSiteWidth = 0;
        public int getBindingSiteWidth()
        {
            return bindingSiteWidth;
        }
        public void setBindingSiteWidth(int bindingSiteWidth)
        {
            int oldValue = this.bindingSiteWidth;
            this.bindingSiteWidth = bindingSiteWidth;
            firePropertyChange( "bindingSiteWidth", oldValue, bindingSiteWidth );
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

        private DataElementPath sdTable;
        public DataElementPath getSdTable()
        {
            return sdTable;
        }
        public void setSdTable(DataElementPath sdTable)
        {
            Object oldValue = this.sdTable;
            this.sdTable = sdTable;
            firePropertyChange( "sdTable", oldValue, sdTable );
        }

        private DataElementPath clusterToPeakTable;
        public DataElementPath getClusterToPeakTable()
        {
            return clusterToPeakTable;
        }
        public void setClusterToPeakTable(DataElementPath clusterToPeakTable)
        {
            Object oldValue = this.clusterToPeakTable;
            this.clusterToPeakTable = clusterToPeakTable;
            firePropertyChange( "clusterToPeakTable", oldValue, clusterToPeakTable );
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
            property( "inputTrack" ).inputElement( Track.class ).add();
            property( "maxDistance" ).add();
            property( "useDensityClusterization" ).add();
            property( "minDensity" ).hidden( "isNotUseDensityClusterization" ).add();
            property( "minDelta" ).hidden( "isNotUseDensityClusterization" ).add();
            property( "useGlobalSD" ).add();
            property( "bindingSiteWidth" ).add();
            add( "maxPropWidth" );
            property( "outputTrack" ).outputElement( SqlTrack.class ).add();
            property( "sdTable" ).outputElement( TableDataCollection.class ).add();
            property( "clusterToPeakTable" ).outputElement( TableDataCollection.class ).add();
        }
    }
}
