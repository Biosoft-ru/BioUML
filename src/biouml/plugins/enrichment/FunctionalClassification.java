package biouml.plugins.enrichment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import biouml.standard.type.Gene;
import biouml.standard.type.Species;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.analysis.Stat;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.Cache;

@ClassIcon ( "resources/functional-classification.gif" )
public class FunctionalClassification extends FunctionalAnalysisSupport<FunctionalClassificationParameters>
{
    static final String HITS_COLUMN = "Hits";
    static final String GROUP_SIZE_COLUMN = "Group size";
    static final String NUMBER_OF_HITS_COLUMN = "Number of hits";
    static final String ADJUSTED_P_VALUE_COLUMN = "Adjusted P-value";

    public FunctionalClassification(DataCollection<?> origin, String name)
    {
        super(origin, name, new FunctionalClassificationParameters());
    }

    protected FunctionalClassification(DataCollection<?> origin, String name, FunctionalClassificationParameters params)
    {
        super(origin, name, params);
    }
    
    protected Map<Element, Set<String>> fetchCategories(BioHub hub, Species species, List<String> list, DataElementPath destPath) throws Exception
    {
        CollectionRecord collection = new CollectionRecord(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_HITS_RECORD, true);
        CollectionRecord ensembl = new CollectionRecord(TrackUtils.getEnsemblPath(species, destPath), true);
        TargetOptions dbOptions = new TargetOptions(collection, ensembl);
        Element[] elements = new Element[list.size()];
        Arrays.setAll( elements, i -> new Element("stub/%//"+list.get(i)) );
        jobControl.setPreparedness(5);
        if( jobControl.isStopped() )
            return null;
        Map<Element, Element[]> res = hub.getReferences(elements, dbOptions, null, 1, -1);
        jobControl.setPreparedness(50);
        if( jobControl.isStopped() )
            return null;
        Map<Element, Set<String>> result = EntryStream.of(res).nonNullValues().flatMapValues(Arrays::stream)
                .mapKeys(Element::getAccession).invert().groupingTo(HashSet::new);
        jobControl.setPreparedness(55);

        return result;
    }

    protected void filter(Map<Element, Set<String>> categories, int minHits, boolean overRepresented)
    {
        for( Element group : categories.keySet().toArray(new Element[categories.size()]) )
        {
            if( group.getAccession().equals("all") || categories.get(group).size() < minHits )
                categories.remove(group);
            /*else if( overRepresented )
            {
                double expected = ( (double)(Integer)group.getValue(FunctionalHubConstants.INPUT_GENES_PROPERTY) )
                        * ( (Integer)group.getValue(FunctionalHubConstants.GROUP_SIZE_PROPERTY) )
                        / ( (Integer)group.getValue(FunctionalHubConstants.TOTAL_GENES_PROPERTY) );
                if( categories.get(group).size() < expected )
                    categories.remove(group);
            }*/
        }
    }
    
    protected Function<String, String> getNameFunction()
    {
        DataCollection<Gene> genesCollection = TrackUtils.getGenesCollection( getParameters().getSpecies(), parameters.getOutputTable() );
        QuerySystem qs = genesCollection.getInfo().getQuerySystem();
        Index<String> index = qs == null ? null : qs.getIndex("title");
        return Cache.hard( hit -> {
            if( index != null && index.containsKey(hit) )
                return index.get(hit);
            else
            {
                try
                {
                    String title = genesCollection.get( hit ).getTitle();
                    if( title != null )
                        return title;
                    else
                        return hit;
                }
                catch( Exception e )
                {
                    return hit;
                }
            }
        } );
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        TableDataCollection source = parameters.getSource();
        log.info("Fetching categories...");
        List<String> names = source.getNameList();
        Map<Element, Set<String>> categories = fetchCategories(parameters.getFunctionalHub(), parameters.getSpecies(), names, parameters.getOutputTable());
        if( jobControl.isStopped() )
            return null;
        log.info("Filtering...");
        filter(categories, parameters.getMinHits(), parameters.getOnlyOverrepresented());
        jobControl.setPreparedness(60);
        if( jobControl.isStopped() )
            return null;
        try
        {
            log.info("Initializing table...");
            TableDataCollection resTable = TableDataCollectionUtils.createTableDataCollection(parameters.getOutputTable());
            Set<Element> groups = categories.keySet();
            int annotationColumnsCount = 0;
            ColumnModel columnModel = resTable.getColumnModel();
            if( !groups.isEmpty() )
            {
                Element group = groups.iterator().next();
                for( String key : group.getKeys() )
                {
                    if( key.equals(FunctionalHubConstants.GROUP_SIZE_PROPERTY) || key.equals(FunctionalHubConstants.INPUT_GENES_PROPERTY)
                            || key.equals(FunctionalHubConstants.TOTAL_GENES_PROPERTY) )
                        continue;
                    columnModel.addColumn(key, group.getValue(key).getClass());
                    annotationColumnsCount++;
                }
            }
            columnModel.addColumn(NUMBER_OF_HITS_COLUMN, Integer.class);
            columnModel.addColumn(GROUP_SIZE_COLUMN, Integer.class);
            columnModel.addColumn("Expected hits", Double.class);
            columnModel.addColumn("P-value", Double.class);
            columnModel.addColumn(ADJUSTED_P_VALUE_COLUMN, Double.class);
            columnModel.addColumn(HITS_COLUMN, StringSet.class).setHidden(true);
            columnModel.addColumn("Hit names", StringSet.class);
            jobControl.setPreparedness(60);
            if( jobControl.isStopped() )
            {
                parameters.getOutputTable().remove();
                return null;
            }
            log.info("Calculating scores & generating output...");
            int i = 0, size = categories.size();
            boolean statsDisplayed = false;
            int totalTests = 0;
            Function<String, String> nameFunction = getNameFunction();
            TDoubleList sigPvals = new TDoubleArrayList();
            TObjectIntMap<String> rowMap = new TObjectIntHashMap<>();
            // Converting to array to prevent concurrent modification exception
            for( Element categoryElement : groups.toArray(new Element[groups.size()]) )
            {
            	if (categoryElement.getAccession() == null || 
            	    categoryElement.getValue(FunctionalHubConstants.GROUP_SIZE_PROPERTY) == null ||
            	    categoryElement.getValue(FunctionalHubConstants.INPUT_GENES_PROPERTY) == null ||
            	    categoryElement.getValue(FunctionalHubConstants.TOTAL_GENES_PROPERTY) == null)
            	{
            		continue;
            	}
                if( !statsDisplayed )
                {
                    log.info("Total genes in the input set: " + names.size());
                    log.info("Number of genes matched to the classification: "
                            + categoryElement.getValue(FunctionalHubConstants.INPUT_GENES_PROPERTY));
                    log.info("Total genes in the classification: " + categoryElement.getValue(FunctionalHubConstants.TOTAL_GENES_PROPERTY));
                    statsDisplayed = true;
                }
                String categoryAccession = categoryElement.getAccession();
                jobControl.setPreparedness(60 + 40 * i / size);
                if( jobControl.isStopped() )
                {
                    parameters.getOutputTable().remove();
                    return null;
                }
                i++;
                StringSet hits = StreamEx.of( categories.get( categoryElement ) ).sorted( String::compareTo ).toCollection( StringSet::new );
                StringSet hitNames = hits.stream().map( nameFunction ).sorted( String::compareTo ).toCollection( StringSet::new );
                categories.remove(categoryElement);
                double expected = ( (double)(Integer)categoryElement.getValue(FunctionalHubConstants.INPUT_GENES_PROPERTY) )
                        * ( (Integer)categoryElement.getValue(FunctionalHubConstants.GROUP_SIZE_PROPERTY) )
                        / ( (Integer)categoryElement.getValue(FunctionalHubConstants.TOTAL_GENES_PROPERTY) );
                double[] pvalues = Stat.cumulativeHypergeometric(
                        (Integer)categoryElement.getValue(FunctionalHubConstants.TOTAL_GENES_PROPERTY),
                        (Integer)categoryElement.getValue(FunctionalHubConstants.GROUP_SIZE_PROPERTY),
                        (Integer)categoryElement.getValue(FunctionalHubConstants.INPUT_GENES_PROPERTY), hits.size());
                double pvalue = hits.size() > expected ? pvalues[1] : pvalues[0];
                if (parameters.getOnlyOverrepresented()) {
                    pvalue = pvalues[1];
                }
                totalTests++;
                if( pvalue > parameters.getPvalueThreshold() )
                    continue;
                rowMap.put(categoryAccession, sigPvals.size());
                sigPvals.add(pvalue);
                Object[] values = new Object[columnModel.getColumnCount()];
                int col;
                for( col = 0; col < annotationColumnsCount; col++ )
                {
                    values[col] = categoryElement.getValue(columnModel.getColumn(col).getName());
                }
                col = annotationColumnsCount;
                values[col++] = hits.size();
                values[col++] = categoryElement.getValue(FunctionalHubConstants.GROUP_SIZE_PROPERTY);
                values[col++] = expected;
                values[col++] = pvalue;
                col++;
                values[col++] = hits;
                values[col++] = hitNames;
                TableDataCollectionUtils.addRow(resTable, categoryAccession, values, true);
            }
            resTable.finalizeAddition();
            if( resTable.getSize() > 0 )
            {
                log.info("Calculate FDRs");
                double[] adjPvals = Stat.adjustPvalues(sigPvals.toArray(), totalTests);
                for( String name : resTable.names().toArray( String[]::new ) )
                {
                    RowDataElement row = resTable.get(name);
                    int rix = rowMap.get(name);
                    row.setValue(ADJUSTED_P_VALUE_COLUMN, adjPvals[rix]);
                    row.getOrigin().put(row);
                }
            }
            TableDataCollectionUtils.setSortOrder(resTable, "P-value", true);
            resTable.getInfo().setNodeImageLocation(getClass(), "resources/classify.gif");
            writeMetaData(resTable);
            return resTable;
        }
        catch( Exception e )
        {
            parameters.getOutputTable().remove();
            throw e;
        }
    }
}
