package biouml.plugins.enrichment;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import biouml.standard.type.Gene;
import biouml.standard.type.Species;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import one.util.streamex.EntryStream;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Index;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.graphics.chart.AxisOptions;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon("resources/enrichment-analysis.gif")
public class EnrichmentAnalysis extends FunctionalAnalysisSupport<EnrichmentAnalysisParameters>
{
    private double[] nesArray;
    private double[] nesRandomArray;
    private int nesZeroPoint;
    private int nesRandomZeroPoint;

    public EnrichmentAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new EnrichmentAnalysisParameters());
    }

    protected static class GroupContainer implements Iterable<GroupInfo>
    {
        private final Map<Element, GroupInfo> data;
        private final int maxRank;
        
        public GroupContainer(int maxRank)
        {
            data = new HashMap<>();
            this.maxRank = maxRank;
        }
        
        public void put(Element group, int rank)
        {
            data.computeIfAbsent( group, k -> new GroupInfo(maxRank) ).put( rank );
        }
        
        public Set<Element> getGroups()
        {
            return data.keySet();
        }
        
        public boolean containsGroup(Element group)
        {
            return data.containsKey(group);
        }
        
        public GroupInfo get(Element group)
        {
            return data.get(group);
        }
        
        public int[] getGenes(Element group)
        {
            return get(group).getRank();
        }
        
        public void filter(int minHits)
        {
            data.entrySet().removeIf( entry -> entry.getKey().getAccession().equals( "all" ) || entry.getValue().getSize() < minHits);
        }
        
        public void remove(Element group)
        {
            data.remove(group);
        }

        @Override
        public Iterator<GroupInfo> iterator()
        {
            return data.values().iterator();
        }
    }
    
    protected List<String> getRankedList(TableDataCollection source, int columnIndex)
    {
        TableColumn col = source.getColumnModel().getColumn(columnIndex);
        source.sortTable(columnIndex, col.getValueClass() == String.class);
        return source.getNameList();
    }
    
    protected GroupContainer fetchCategories(BioHub hub, Species species, List<String> rankedList, DataElementPath destPath) throws Exception
    {
        CollectionRecord collection = new CollectionRecord(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_HITS_RECORD, true);
        CollectionRecord ensembl = new CollectionRecord( TrackUtils.getEnsemblPath( species, destPath ), true );
        TargetOptions dbOptions = new TargetOptions(collection, ensembl);
        Element[] elements = new Element[rankedList.size()];
        Arrays.setAll( elements, i -> new Element("stub/%//"+rankedList.get(i)) );
        jobControl.setPreparedness(6);
        if(jobControl.isStopped()) return null;
        Map<Element, Element[]> res = hub.getReferences(elements, dbOptions, null, 1, -1);
        jobControl.setPreparedness(45);
        if(jobControl.isStopped()) return null;
        GroupContainer result = new GroupContainer(rankedList.size());
        EntryStream.of( elements ).invert().mapKeys( res::get ).nonNullKeys().flatMapKeys( Arrays::stream ).forKeyValue( result::put );
        jobControl.setPreparedness(50);
        if(jobControl.isStopped()) return null;
        return result;
    }
    
    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        TableDataCollection source = getParameters().getSource();
        if(source == null)
            throw new IllegalArgumentException("Please specify source table");
        String columnName = getParameters().getColumnName();
        if(columnName == null || columnName.equals(""))
            throw new IllegalArgumentException("Please specify column name");
        int columnIndex = source.getColumnModel().optColumnIndex(columnName);
        if(columnIndex == -1)
            throw new IllegalArgumentException("Specified column not found");
        checkRange("permutationsCount", 10, 10000);
    }
    
    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        TableDataCollection source = getParameters().getSource();
        String columnName = getParameters().getColumnName();
        int columnIndex = source.getColumnModel().optColumnIndex(columnName);
        log.info("Ranking...");
        List<String> rankedList = getRankedList(source, columnIndex);
        jobControl.setPreparedness(5);
        if(jobControl.isStopped()) return null;
        log.info("Fetching categories...");
        GroupContainer categories = fetchCategories( getParameters().getFunctionalHub(), getParameters().getSpecies(), rankedList,
                getParameters().getOutputTable() );
        jobControl.setPreparedness(45);
        if(jobControl.isStopped()) return null;
        log.info("Gathering statistics for FDR...");
        gatherNesStats(categories);
        log.info("Filtering...");
        categories.filter(getParameters().getMinHits());
        jobControl.setPreparedness(80);
        if(jobControl.isStopped()) return null;
        try
        {
            log.info("Initializing table...");
            TableDataCollection resTable = TableDataCollectionUtils.createTableDataCollection(getParameters().getOutputTable());
            Set<Element> groups = categories.getGroups();
            int annotationColumnsCount = 0;
            int annotationTitleColumn = -1;
            
            ColumnModel columnModel = resTable.getColumnModel();
            if(!groups.isEmpty())
            {
                Element group = groups.iterator().next();
                for(String key: group.getKeys())
                {
                    if( key.equals(FunctionalHubConstants.GROUP_SIZE_PROPERTY) || key.equals(FunctionalHubConstants.INPUT_GENES_PROPERTY)
                            || key.equals(FunctionalHubConstants.TOTAL_GENES_PROPERTY) )
                        continue;
                    columnModel.addColumn(key, group.getValue(key).getClass());
                    if(annotationTitleColumn == -1 && group.getValue(key).getClass() == String.class)
                        annotationTitleColumn = annotationColumnsCount;
                    annotationColumnsCount++;
                }
            }
            ChartSeries weightSeries = getChartForWeightingColumn(source, columnIndex);
            String weightSeriesChart = weightSeries.toJSON().toString();
            columnModel.addColumn("Group size", Integer.class);
            columnModel.addColumn("Expected hits", Double.class);
            columnModel.addColumn("Nominal P-value", Double.class);
            columnModel.addColumn("ES", Double.class);
            columnModel.addColumn("Rank at max", Integer.class);
            columnModel.addColumn("NES", Double.class);
            columnModel.addColumn("FDR", Double.class);
            columnModel.addColumn("Number of hits", Integer.class);
            TableColumn column = columnModel.addColumn("Plot", Chart.class, "concat(Plot.getData().subSequence(0,2),'"+weightSeriesChart+",',Plot.getData().subSequence(2,-1))");
            column.setExpressionLocked(true);
            columnModel.addColumn("Hits", StringSet.class).setHidden(true);
            columnModel.addColumn("Hit names", StringSet.class);
            jobControl.setPreparedness(90);
            if(jobControl.isStopped())
            {
                getParameters().getOutputTable().remove();
                return null;
            }
            log.info("Calculating scores & generating output...");
            int i=0, size = categories.getGroups().size();
            DataCollection<Gene> genesCollection = TrackUtils.getGenesCollection(getParameters().getSpecies(), getParameters().getOutputTable());
            Index<String> index = null;
            try
            {
                index = genesCollection.getInfo().getQuerySystem().getIndex("title");
            }
            catch( Exception e1 )
            {
            }
            // Converting to array to prevent concurrent modification exception
            for(Element goElement: groups.toArray(new Element[groups.size()]))
            {
            	if (goElement == null)
            		continue;
                String go = goElement.getAccession();
                if (go == null || go.length() == 0 || go.equals("null")) {
                	continue;
                }
                jobControl.setPreparedness(90+10*i/size);
                if(jobControl.isStopped())
                {
                    getParameters().getOutputTable().remove();
                    return null;
                }
                i++;
                GroupInfo group = categories.get(goElement);
                categories.remove(goElement);
                if(getParameters().getOnlyOverrepresented() && group.getES() <=0) continue;
                if(group.getNP() < 0 || group.getNP() > getParameters().getPvalueThreshold()) continue;
                Chart chart = new Chart();
                ChartSeries series = new ChartSeries(group.getScorePlot());
                String label = annotationTitleColumn != -1 ? go + " " + goElement.getValue(columnModel.getColumn(annotationTitleColumn).getName()) : go;
                series.setLabel(label);
                ChartOptions options = new ChartOptions();
                
                AxisOptions xAxis = new AxisOptions();
                xAxis.setLabel("Gene rank in the set");
                options.setXAxis(xAxis);
                AxisOptions yAxis = new AxisOptions();
                yAxis.setLabel("Kolmogorov-Smirnov score");
                options.setYAxis(yAxis);
                AxisOptions yAxis2 = new AxisOptions(true);
                yAxis2.setLabel(source.getColumnModel().getColumn(columnIndex).getName());
                options.addYAxis(yAxis2);
                
                chart.setOptions(options);
                chart.addSeries(series);
                Object[] values = new Object[columnModel.getColumnCount()];
                int col;
                for(col = 0; col < annotationColumnsCount; col++)
                {
                    values[col] = goElement.getValue(columnModel.getColumn(col).getName());
                }
                col = annotationColumnsCount;
                double expected = ( (double)(Integer)goElement.getValue(FunctionalHubConstants.INPUT_GENES_PROPERTY) )
                        * ( (Integer)goElement.getValue(FunctionalHubConstants.GROUP_SIZE_PROPERTY) )
                        / ( (Integer)goElement.getValue(FunctionalHubConstants.TOTAL_GENES_PROPERTY) );
                values[col++] = goElement.getValue(FunctionalHubConstants.GROUP_SIZE_PROPERTY);
                values[col++] = expected;
                values[col++] = group.getNP();
                values[col++] = group.getES();
                values[col++] = group.getRankAtMax()+1;
                double nes = group.getNES();
                values[col++] = group.getNES();
                int nesPos = getPos(nesArray, nes);
                int nesRandomPos = getPos(nesRandomArray, nes);
                double nesRatio;
                double nesRandomRatio;
                if(nes > 0)
                {
                    nesRandomRatio = ((double)nesRandomArray.length-nesRandomPos)/(nesRandomArray.length-nesRandomZeroPoint);
                    nesRatio = ((double)nesArray.length-nesPos)/(nesArray.length-nesZeroPoint);
                } else
                {
                    nesRandomRatio = ((double)nesRandomPos+1)/nesRandomZeroPoint;
                    nesRatio = ((double)nesPos+1)/nesZeroPoint;
                }
                values[col++] = Math.min(1, nesRandomRatio/nesRatio);
                values[col++] = group.getSize();
                values[col++] = chart;
                Set<String> hits = new TreeSet<>();
                Set<String> hitNames = new TreeSet<>();
                for(int curRank: group.getRank())
                {
                    String hit = rankedList.get(curRank);
                    hits.add(hit);
                    if( index != null && index.containsKey(hit) )
                        hitNames.add(index.get(hit));
                    else
                    {
                        try
                        {
                            hitNames.add( genesCollection.get(hit).getTitle());
                        }
                        catch( Exception e )
                        {
                            hitNames.add(hit);
                        }
                    }
                }
                values[col++] = new StringSet( hits );
                values[col++] = new StringSet( hitNames );
                TableDataCollectionUtils.addRow(resTable, go, values);
            }
            TableDataCollectionUtils.setSortOrder(resTable, "NES", false);
            resTable.getInfo().setNodeImageLocation(getClass(), "resources/classify.gif");
            writeMetaData(resTable);
            return resTable;
        }
        catch(Exception e)
        {
            getParameters().getOutputTable().remove();
            throw e;
        }
    }

    public static int getPos(double[] array, double value)
    {
        int pos = Arrays.binarySearch(array, value);
        if(pos < 0) return -pos-1;
        if(value > 0)
        {
            while(pos > 0 && array[pos] == value) pos--;
            pos++;
        } else
        {
            while(pos < array.length && array[pos] == value) pos++;
            pos--;
        }
        return pos;
    }

    private void gatherNesStats(GroupContainer categories)
    {
        TDoubleList nesList = new TDoubleArrayList();
        TDoubleList nesRandomList = new TDoubleArrayList();
        for(GroupInfo group: categories)
        {
            group.calculateScore();
            group.calculatePValue(getParameters().getPermutationsCount(), nesList, nesRandomList);
        }
        nesArray = nesList.toArray();
        Arrays.sort(nesArray);
        nesZeroPoint = Arrays.binarySearch(nesArray, 0.0);
        if(nesZeroPoint < 0) nesZeroPoint = -nesZeroPoint-1;
        nesRandomArray = nesRandomList.toArray();
        Arrays.sort(nesRandomArray);
        nesRandomZeroPoint = Arrays.binarySearch(nesRandomArray, 0.0);
        if(nesRandomZeroPoint < 0) nesRandomZeroPoint = -nesRandomZeroPoint-1;
    }
    
    private ChartSeries getChartForWeightingColumn(TableDataCollection source, int columnIndex) throws Exception
    {
        List<String> rankedList = getRankedList(source, columnIndex);
        double[][] chartData = new double[rankedList.size()][2];
        for(int j = 0; j < rankedList.size(); j++)
        {
            String geneName = rankedList.get(j);
            Object val = ((source.get(geneName))).getValues()[columnIndex];
            try
            {
                chartData[j][1] = Double.parseDouble(val.toString());
            }
            catch (NumberFormatException e)
            {
                chartData[j][1] = j+1;
            }
            chartData[j][0] =  j+1;
        }
        ChartSeries series = new ChartSeries(chartData);
        series.setYAxis(2);
        series.setLabel(source.getColumnModel().getColumn(columnIndex).getName());
        series.setColor(Color.BLUE);
        return series;
    }
}
