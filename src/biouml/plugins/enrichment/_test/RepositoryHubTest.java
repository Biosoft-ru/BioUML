package biouml.plugins.enrichment._test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;

import biouml.plugins.enrichment.EnrichmentAnalysis;
import biouml.plugins.enrichment.EnrichmentAnalysisParameters;
import biouml.plugins.enrichment.FunctionalClassification;
import biouml.plugins.enrichment.FunctionalClassificationParameters;
import biouml.plugins.enrichment.FunctionalHubConstants;
import biouml.plugins.enrichment.RepositoryHub;
import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import one.util.streamex.IntStreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa._test.BSATestUtils;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * @author lan
 *
 */
public class RepositoryHubTest extends AbstractBioUMLTest
{
    public void testRepositoryHub() throws Exception
    {
        List<String> nameList = DataElementPath.create("databases/Ensembl/Data/gene").getDataCollection().getNameList();
        DataCollection<DataElement> groupsRoot = createGroups(nameList);
        
        RepositoryHub hub = new RepositoryHub(new Properties(), groupsRoot.getCompletePath(), null);
        DataElementPathSet groups = hub.getGroups();
        assertEquals(11, groups.size());
        
        Element[] elements = IntStreamEx.range( 80 ).mapToObj( i -> new Element( "stub/%//" + nameList.get( i * 2 ) ) )
                .toArray( Element[]::new );
        
        CollectionRecord collection = new CollectionRecord(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_HITS_RECORD, true);
        CollectionRecord ensembl = new CollectionRecord("databases/Ensembl", true);
        TargetOptions dbOptions = new TargetOptions(collection, ensembl);
        
        Properties matchingProperties = new Properties();
        matchingProperties.setProperty(BioHub.TYPE_PROPERTY, ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class).toString());
        matchingProperties.setProperty(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD, "true");
        Properties[] supportedMatching = hub.getSupportedMatching(matchingProperties);
        assertNotNull(supportedMatching);
        assertEquals("de:databases/groups/$id$", supportedMatching[0].getProperty(DataCollectionConfigConstants.URL_TEMPLATE));
        
        Map<Element, Element[]> references = hub.getReferences(elements, dbOptions, null, 1, -1);
        assertNotNull(references);
        assertEquals(elements.length, references.size());
        for(Element element: elements)
        {
            Element[] elementGroups = references.get(element);
            assertNotNull(elementGroups);
            assertTrue(elementGroups.length ==1 || (elementGroups.length >= 2 && elementGroups.length <= 7));
            Set<String> result = new HashSet<>();
            for(Element group: elementGroups)
            {
                assertEquals(80, group.getValue(FunctionalHubConstants.INPUT_GENES_PROPERTY));
                assertEquals(200, group.getValue(FunctionalHubConstants.TOTAL_GENES_PROPERTY));
                assertEquals("databases/groups/" + group.getAccession(), group.getPath());
                if( group.getAccession().equals("group_all") )
                {
                    assertEquals(200, group.getValue(FunctionalHubConstants.GROUP_SIZE_PROPERTY));
                } else if( group.getAccession().equals("subgroups") )
                {
                    assertEquals(140, group.getValue(FunctionalHubConstants.GROUP_SIZE_PROPERTY));
                } else
                {
                    assertEquals(50, group.getValue(FunctionalHubConstants.GROUP_SIZE_PROPERTY));
                }
                result.add(group.getAccession());
            }
            assertTrue(result.contains("group_all"));
            assertTrue(result.size() == 1 || result.contains("subgroups"));
        }
    }
    
    public void testFunctionalClassification() throws Exception
    {
        List<String> nameList = DataElementPath.create("databases/Ensembl/Data/gene").getDataCollection().getNameList();
        DataCollection<DataElement> groupsRoot = createGroups(nameList);
        VectorDataCollection<DataElement> vdc = createTestCollection();
        TableDataCollection input = createInputTable(nameList, vdc);
        
        FunctionalClassification analysis = new FunctionalClassification(null, "");
        FunctionalClassificationParameters parameters = createParameters(groupsRoot, input);
        analysis.setParameters(parameters);
        // Do not call validateParameters as it fails, because we use VectorDataCollection's instead of GenericDataCollections
        TableDataCollection output = analysis.justAnalyzeAndPut();
        assertNotNull(output);
        assertEquals(parameters.getOutputTable(), output.getCompletePath());
        assertEquals(11, output.getSize());

        for(RowDataElement row: output)
        {
            if(row.getName().equals("subgroups"))
            {
                assertEquals(70, row.getValues()[0]);
                assertEquals(140, row.getValues()[1]);
                assertEquals(56.0, row.getValues()[2]);
                assertEquals(5.283748836308929E-6, (Double)row.getValues()[3], 0.0000000001);
            } else
            {
                assertTrue(row.getName().startsWith("subgroups/group_"));
                assertEquals(25, row.getValues()[0]);
                assertEquals(50, row.getValues()[1]);
                assertEquals(20.0, row.getValues()[2]);
                assertEquals(0.06744891916760958, (Double)row.getValues()[3], 0.00001);
            }
        }
    }

    public void testFunctionalClassificationWithReference() throws Exception
    {
        List<String> nameList = DataElementPath.create("databases/Ensembl/Data/gene").getDataCollection().getNameList();
        DataCollection<DataElement> groupsRoot = createGroups(nameList);
        VectorDataCollection<DataElement> vdc = createTestCollection();
        TableDataCollection input = createInputTable(nameList, vdc);
        TableDataCollection reference = createReferenceTable(nameList, vdc);
        
        FunctionalClassification analysis = new FunctionalClassification(null, "");
        FunctionalClassificationParameters parameters = createParameters(groupsRoot, input);
        parameters.setReferenceCollection(reference.getCompletePath());
        analysis.setParameters(parameters);
        // Do not call validateParameters as it fails, because we use VectorDataCollection's instead of GenericDataCollections
        TableDataCollection output = analysis.justAnalyzeAndPut();
        assertNotNull(output);
        assertEquals(parameters.getOutputTable(), output.getCompletePath());
        assertEquals(12, output.getSize());

        for(RowDataElement row: output)
        {
            if(row.getName().equals("subgroups"))
            {
                assertEquals(70, row.getValues()[0]);
                assertEquals(140, row.getValues()[1]);
                assertEquals(11.2, row.getValues()[2]);
                assertEquals(1, (Double)row.getValues()[3]/1.010998177535244E-56, 0.00000001);
            } else if(row.getName().equals("group_all"))
            {
                assertEquals(80, row.getValues()[0]);
                assertEquals(200, row.getValues()[1]);
                assertEquals(16.0, row.getValues()[2]);
                assertEquals(1, (Double)row.getValues()[3]/3.0321561562062704E-63, 0.00000001);
            } else
            {
                assertTrue(row.getName().startsWith("subgroups/group_"));
                assertEquals(25, row.getValues()[0]);
                assertEquals(50, row.getValues()[1]);
                assertEquals(4.0, row.getValues()[2]);
                assertEquals(1, (Double)row.getValues()[3]/2.354568447984144E-16, 0.00000001);
            }
        }
    }

    public void testEnrichment() throws Exception
    {
        List<String> nameList = DataElementPath.create("databases/Ensembl/Data/gene").getDataCollection().getNameList();
        DataCollection<DataElement> groupsRoot = createGroups(nameList);
        VectorDataCollection<DataElement> vdc = createTestCollection();
        TableDataCollection input = createInputTable(nameList, vdc);
        
        EnrichmentAnalysis analysis = new EnrichmentAnalysis(null, "");
        EnrichmentAnalysisParameters parameters = new EnrichmentAnalysisParameters();
        parameters.setSourcePath(input.getCompletePath());
        parameters.setColumnName("Score");
        parameters.setPvalueThreshold(0.5);
        parameters.setBioHub( BioHubRegistry.specialHubs().findFirst( bh -> bh.getName().equals( RepositoryHub.REPOSITORY_HUB_NAME ) )
                .get() );
        parameters.setPvalueThreshold(0.2);
        parameters.setOnlyOverrepresented(false);
        assertFalse(parameters.isRepositoryHubRootHidden());
        parameters.setRepositoryHubRoot(groupsRoot.getCompletePath());
        assertEquals(groupsRoot.getName(), parameters.getHubShortName());
        assertEquals(DataElementPath.create("test/input GSEA groups"), parameters.getOutputTable());
        analysis.setParameters(parameters);
        // Do not call validateParameters as it fails, because we use VectorDataCollection's instead of GenericDataCollections
        TableDataCollection output = analysis.justAnalyzeAndPut();
        assertNotNull(output);
        assertEquals(parameters.getOutputTable(), output.getCompletePath());
        assertEquals(11, output.getSize());
    
        for(RowDataElement row: output)
        {
            if(row.getName().equals("subgroups"))
            {
                assertEquals(70, row.getValue("Number of hits"));
                assertEquals(140, row.getValue("Group size"));
                assertEquals(56.0, row.getValue("Expected hits"));
                assertEquals(-1, (Double)row.getValue("ES"), 0.000001);
            } else
            {
                assertTrue(row.getName().startsWith("subgroups/group_"));
                int groupNum = Integer.parseInt(row.getName().substring("subgroups/group_".length()));
                double ES = groupNum < 6 ? (groupNum-11)/11.0 : groupNum/11.0;
                int rankAtMax = groupNum < 6 ? 55-groupNum*5 : 80-groupNum*5;
                assertEquals(25, row.getValue("Number of hits"));
                assertEquals(50, row.getValue("Group size"));
                assertEquals(20.0, row.getValue("Expected hits"));
                assertEquals(ES, (Double)row.getValue("ES"), 0.00001);
                assertEquals(rankAtMax, row.getValue("Rank at max"));
            }
        }
    }

    private @Nonnull VectorDataCollection<DataElement> createTestCollection()
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        return vdc;
    }

    private FunctionalClassificationParameters createParameters(DataCollection<DataElement> groupsRoot, TableDataCollection input)
    {
        FunctionalClassificationParameters parameters = new FunctionalClassificationParameters();
        parameters.setSourcePath(input.getCompletePath());
        parameters.setPvalueThreshold(0.5);
        parameters.setBioHub( BioHubRegistry.specialHubs().findFirst( bh -> bh.getName().equals( RepositoryHub.REPOSITORY_HUB_NAME ) )
                .get() );
        assertFalse(parameters.isRepositoryHubRootHidden());
        parameters.setRepositoryHubRoot(groupsRoot.getCompletePath());
        assertEquals(groupsRoot.getName(), parameters.getHubShortName());
        assertEquals(DataElementPath.create("test/input groups"), parameters.getOutputTable());
        return parameters;
    }

    private TableDataCollection createInputTable(List<String> nameList, @Nonnull VectorDataCollection<DataElement> vdc) throws Exception
    {
        TableDataCollection input = TableDataCollectionUtils.createTableDataCollection(vdc, "input");
        input.getColumnModel().addColumn("Score", Integer.class);
        for(int i=0; i<80; i++)
        {
            TableDataCollectionUtils.addRow(input, nameList.get(i*2), new Object[] {i});
        }
        input.setReferenceType(ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class).getDisplayName());
        vdc.put(input);
        return input;
    }

    private TableDataCollection createReferenceTable(List<String> nameList, VectorDataCollection<DataElement> vdc) throws Exception
    {
        TableDataCollection referenceTable = TableDataCollectionUtils.createTableDataCollection(vdc, "reference");
        for(String name: nameList.subList(0, 1000))
        {
            TableDataCollectionUtils.addRow(referenceTable, name, new Object[0]);
        }
        referenceTable.setReferenceType(ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class).getDisplayName());
        vdc.put(referenceTable);
        return referenceTable;
    }

    private DataCollection<DataElement> createGroups(List<String> nameList) throws Exception
    {
        DataCollection parent = CollectionFactoryUtils.getDatabases();
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("groups", parent, null);
        VectorDataCollection<DataElement> svdc = new VectorDataCollection<>("subgroups", vdc, null);
        vdc.put(svdc);
        for(int i=0; i<10; i++)
        {
            TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(vdc, "group_"+i);
            for(String name: nameList.subList(i*10, i*10+50))
            {
                TableDataCollectionUtils.addRow(tdc, name, new Object[0]);
            }
            tdc.setReferenceType(ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class).getDisplayName());
            svdc.put(tdc);
        }
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(vdc, "group_all");
        for(String name: nameList.subList(0, 200))
        {
            TableDataCollectionUtils.addRow(tdc, name, new Object[0]);
        }
        tdc.setReferenceType(ReferenceTypeRegistry.getReferenceType(EnsemblGeneTableType.class).getDisplayName());
        vdc.put(tdc);
        parent.put(vdc);
        return vdc;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        BSATestUtils.createRepository();
    }
}
