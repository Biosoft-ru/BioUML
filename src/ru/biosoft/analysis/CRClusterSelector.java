package ru.biosoft.analysis;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon ( "resources/CRCSelector.png" )
@PropertyDescription("Select largest clusters and choose items from them.")
public class CRClusterSelector extends AnalysisMethodSupport<CRClusterSelector.Parameters>
{
    public CRClusterSelector(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();

        DataElement input = parameters.getInputClusters().getDataElement();
        if( ! ( input instanceof TableDataCollection ) )
            throw new InvalidParameterException( "Input element is not a table" );

        AnalysisParameters params = AnalysisParametersFactory.read( input );
        if( params == null || ! ( params instanceof CRClusterAnalysisParameters ) )
            throw new InvalidParameterException( "This table is not a result of CRC Analysis" );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Map<Integer, Cluster> clustersById = new HashMap<>();
        TableDataCollection input = parameters.getInputClusters().getDataElement( TableDataCollection.class );
        final int clusterColumnIndex = 0;
        final int valuesColumnOffset = 3;
        for(RowDataElement row : input)
        {
            Object[] values = row.getValues();
            Integer clusterId = (Integer)values[clusterColumnIndex];
            Cluster cluster = clustersById.computeIfAbsent( clusterId, Cluster::new );
            double[] itemValues = new double[values.length - valuesColumnOffset];
            for(int i = valuesColumnOffset; i < values.length; i++)
                itemValues[i - valuesColumnOffset] = ((Number)values[i]).doubleValue();
            cluster.addItem( new Item( row.getName(), itemValues ) );
        }

        Cluster[] clusters = clustersById.values().stream()
                .filter( c->c.size() >= parameters.getMinItemsPerCluster() )
                .sorted( Comparator.comparingInt( Cluster::size ).reversed() )
                .limit( parameters.getMaxClusters() )
                .toArray(Cluster[]::new);


        DataCollectionUtils.createFoldersForPath( parameters.getOutputFolder().getChildPath( "dummy" ) );

        int i = 0;
        for(Cluster cluster : clusters)
        {
            double[] center = cluster.getCenter();
            cluster.sortByDistance( center );
            cluster.truncate( parameters.getMaxItemsPerCluster() );

            DataElementPath tablePath = parameters.getOutputFolder().getChildPath( "cluster" + (++i) );
            TableDataCollection t = TableDataCollectionUtils.createTableLike( input, tablePath );

            for(Item item : cluster.getItems())
            {
                RowDataElement row = input.get( item.id );
                t.addRow( row );
            }
            t.finalizeAddition();
            //DataCollectionUtils.copyPersistentInfo( t, input );
            tablePath.save( t );
        }


        return parameters.getOutputFolder().getDataCollection();
    }

    private static class Cluster
    {
        private Integer clusterId;

        private List<Item> items = new ArrayList<>();

        public Cluster(Integer id)
        {
            this.clusterId = id;
        }

        public void addItem(Item item)
        {
            items.add( item );
        }

        public int size()
        {
            return items.size();
        }

        public List<Item> getItems()
        {
            return items;
        }

        public double[] getCenter()
        {
            double[] res = new double[items.get( 0 ).values.length];
            for(Item item : items)
            {
                for(int i = 0; i < res.length; i++)
                    res[i] += item.values[i];
            }
            for(int i = 0; i < res.length; i++)
                res[i] /= size();
            return res;
        }

        public void sortByDistance(double[] point)
        {
            Collections.sort( items, Comparator.comparingDouble( item->item.getDistance(point) ));
        }

        public void truncate(int n)
        {
            if(size() > n)
                items.subList( n, items.size() ).clear();
        }
    }

    private static class Item
    {
        public final String id;
        public final double[] values;

        public Item(String id, double[] values)
        {
            this.id = id;
            this.values = values;
        }

        public double getDistance(double[] point)
        {
            double res = 0;
            for(int i = 0; i < values.length; i++)
                res += (values[i] - point[i])*(values[i] - point[i]);
            return Math.sqrt( res );
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputClusters;
        @PropertyName("Input clusters")
        @PropertyDescription("Table of clusters produced by CRC Analysis")
        public DataElementPath getInputClusters()
        {
            return inputClusters;
        }

        public void setInputClusters(DataElementPath inputClusters)
        {
            Object oldValue = this.inputClusters;
            this.inputClusters = inputClusters;
            firePropertyChange( "inputClusters", oldValue, inputClusters );
        }

        private int maxClusters = 2;
        @PropertyName("Maximum number of clusters to use")
        public int getMaxClusters()
        {
            return maxClusters;
        }
        public void setMaxClusters(int maxClusters)
        {
            int oldValue = this.maxClusters;
            this.maxClusters = maxClusters;
            firePropertyChange( "maxClusters", oldValue, maxClusters );
        }

        private int maxItemsPerCluster = 300;
        @PropertyName("Max items per cluster")
        public int getMaxItemsPerCluster()
        {
            return maxItemsPerCluster;
        }
        public void setMaxItemsPerCluster(int maxItemsPerCluster)
        {
            int oldValue = this.maxItemsPerCluster;
            this.maxItemsPerCluster = maxItemsPerCluster;
            firePropertyChange( "maxItemsPerCluster", oldValue, maxItemsPerCluster );
        }

        private int minItemsPerCluster = 10;
        @PropertyName("Min items per cluster")
        public int getMinItemsPerCluster()
        {
            return minItemsPerCluster;
        }

        public void setMinItemsPerCluster(int minItemsPerCluster)
        {
            int oldValue = this.minItemsPerCluster;
            this.minItemsPerCluster = minItemsPerCluster;
            firePropertyChange( "minItemsPerCluster", oldValue, minItemsPerCluster );
        }

        private DataElementPath outputFolder;
        @PropertyName("Output folder")
        public DataElementPath getOutputFolder()
        {
            return outputFolder;
        }
        public void setOutputFolder(DataElementPath outputFolder)
        {
            Object oldValue = this.outputFolder;
            this.outputFolder = outputFolder;
            firePropertyChange( "outputFolder", oldValue, outputFolder );
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
            property("inputClusters").inputElement( TableDataCollection.class ).add();
            add("maxClusters");
            add("maxItemsPerCluster");
            add("minItemsPerCluster");
            property("outputFolder").outputElement( FolderCollection.class ).add();
        }
    }
}
