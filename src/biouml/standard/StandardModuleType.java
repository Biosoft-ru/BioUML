package biouml.standard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.Application;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.model.util.DiagramXmlTransformer;
import biouml.model.xml.XmlDiagramType;
import biouml.standard.diagram.GeneNetworkDiagramType;
import biouml.standard.diagram.MetabolicPathwayDiagramType;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.diagram.SemanticNetworkDiagramType;
import biouml.standard.type.Base;
import biouml.standard.type.Cell;
import biouml.standard.type.Compartment;
import biouml.standard.type.Complex;
import biouml.standard.type.Concept;
import biouml.standard.type.Constant;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.Gene;
import biouml.standard.type.Protein;
import biouml.standard.type.Publication;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.RelationType;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Species;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;
import biouml.standard.type.Unit;
import biouml.standard.type.access.ReactionTransformer;
import biouml.standard.type.access.TitleIndex;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.FileCollection;
import ru.biosoft.access.FileImagesDataCollection;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.util.ObjectExtensionRegistry;

/**
 * Definition of standard BioUML module. The module defines:
 * <ul>
 * <li>standard BioUML data types</li>
 * <li>mapping of standard BioUML data types into database. <br>
 * Just now we are using flat files with tag fields.</li>
 * <li>standard DiagramViewBuilders and SemanticControllers.</li>
 * </ul>
 */
public class StandardModuleType extends DataElementSupport implements ModuleType
{
    private static final ObjectExtensionRegistry<DiagramType> diagramTypes = new ObjectExtensionRegistry<>( "biouml.workbench.diagramType",
            DiagramType.class );

    //these types are deprecated so user can not create them but we keep them to preserve ability to load old diagrams 
    private static final List<String> OBSOLETE_TYPES = Arrays.asList("sbgn_simulation_ext.xml", "sbgn_simulation.xml", "arterialTree.xml");
    
    protected Logger log = Logger.getLogger(StandardModuleType.class.getName());

    public static final String VERSION = "0.7.7";

    public static final String DATABASE_INFO = "database info";
    public static final String RELATION_TYPE = "relation type";
    public static final String SPECIES = "species";
    public static final String UNIT = "unit";

    /** Hashmap to optimise mapping of data type into category. */
    protected HashMap<Class<? extends DataElement>, String> types = new HashMap<>();

    /** Constructor for derived classes. */
    protected StandardModuleType(String name)
    {
        super(name, null);
        initCategories();
    }

    public StandardModuleType()
    {
        super(Application.getGlobalValue("ApplicationName") + " standard", null);
        initCategories();
    }

    protected void initCategories()
    {
        // Data
        types.put(Cell.class, Module.DATA + "/cell");
        types.put(Compartment.class, Module.DATA + "/compartment");
        types.put(Diagram.class, Module.DIAGRAM);

        types.put(Concept.class, Module.DATA + "/concept");

        types.put(Complex.class, Module.DATA + "/complex");

        types.put(Substance.class, Module.DATA + "/substance");
        types.put(Gene.class, Module.DATA + "/gene");
        types.put(RNA.class, Module.DATA + "/rna");
        types.put(Protein.class, Module.DATA + "/protein");

        types.put(SemanticRelation.class, Module.DATA + "/relation");
        types.put(Reaction.class, Module.DATA + "/reaction");
        types.put(Constant.class, Module.DATA + "/constant");

        types.put(Publication.class, Module.DATA + "/literature");

        // Metadata types
        types.put(DatabaseInfo.class, Module.METADATA + "/database info");
        types.put(Species.class, Module.METADATA + "/species");
        types.put(Unit.class, Module.METADATA + "/unit");
        types.put(RelationType.class, Module.METADATA + "/relation type");
        // types.put(KineticType.class, Module.METADATA + "/kinetic type");

    }
    
    public static StreamEx<Class<? extends DiagramType>> getGeneralPurposeTypes()
    {
        return diagramTypes.stream().filter( DiagramType::isGeneralPurpose ).map( DiagramType::getClass );
    }

    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return getGeneralPurposeTypes().append( PathwayDiagramType.class, PathwaySimulationDiagramType.class,
                SemanticNetworkDiagramType.class, MetabolicPathwayDiagramType.class, GeneNetworkDiagramType.class ).toArray( Class[]::new );
    }

    @Override
    public String[] getXmlDiagramTypes()
    {
        List<String> nameList = XmlDiagramType.getTypesCollection().getNameList();
        nameList.removeAll(OBSOLETE_TYPES);
        return nameList.toArray(new String[nameList.size()]);
    }

    @Override
    public boolean isCategorySupported()
    {
        return true;
    }

    @Override
    public String getCategory(Class<? extends DataElement> c)
    {
        try
        {
            ClassLoading.loadClass( c.getName(), null );
        }
        catch( LoggedClassNotFoundException e )
        {
            throw new IllegalArgumentException("Unknown kernel class: " + c.getName());
        }

        String category = types.get(c);
        if( category == null && !Stub.class.isAssignableFrom(c) )
        {
            try
            {
                for( Entry<Class<? extends DataElement>, String> entry : types.entrySet() )
                {
                    Class<? extends DataElement> type = entry.getKey();
                    try
                    {
                        DataElement obj = type.getConstructor(DataCollection.class, String.class).newInstance(null, "test");
                        if( ! ( obj instanceof Base ) )
                            continue;

                        DataElement de = c.getConstructor(DataCollection.class, String.class).newInstance(null, "test");
                        if( ! ( de instanceof Base ) )
                            continue;
                        String kernelType = ( (Base)de ).getType();
                        String baseType = ( (Base)obj ).getType();
                        if( kernelType.startsWith(baseType) && obj.getClass().isInstance(de) )
                            return entry.getValue();
                    }
                    catch( NoSuchMethodException e )
                    {
                        log.info("Incorrect type: " + type.getName());
                        continue;
                    }
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Unexpected error", t);
                return null;
            }
        }

        return category;
    }

    @Override
    public Module createModule(Repository parent, String name) throws Exception
    {
        // Create Module data collection (root)
        Properties primary = new Properties();
        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName());

        Properties transformed = new Properties();
        transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, Module.class.getName());
        transformed.setProperty(Module.TYPE_PROPERTY, StandardModuleType.class.getName());

        Module module = (Module)CollectionFactoryUtils.createDerivedCollection(parent, name, primary, transformed, name);
        LocalRepository moduleLR = (LocalRepository)module.getPrimaryCollection();

        // init Data data collection
        Repository dataDC = CollectionFactoryUtils.createLocalRepository(moduleLR, Module.DATA);

        // init Metadata data collection
        Repository metadataDC = CollectionFactoryUtils.createLocalRepository(moduleLR, Module.METADATA);
        module.put(metadataDC);

        // init Images data collection
        Properties props = new Properties();
        props.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, Module.IMAGES);
        props.setProperty(FileCollection.FILE_FILTER, FileImagesDataCollection.FILTER);
        props.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, ru.biosoft.access.FileImagesDataCollection.class.getName());
        FileImagesDataCollection imagesDC = (FileImagesDataCollection)CollectionFactoryUtils.createSubDirCollection(moduleLR, Module.IMAGES, props);
        module.put(imagesDC);

        // init data collections
        createDataCollection("cell", dataDC, Cell.class);
        DataCollection<?> dc = dataDC.get("cell");
        dc.getInfo().writeProperty( DataCollectionConfigConstants.ID_FORMAT, "CEL0000" );
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName());
        dc.getInfo().writeProperty(QuerySystem.INDEX_LIST, "title");
        dc.getInfo().writeProperty(TitleIndex.INDEX_TITLE, TitleIndex.class.getName());

        createDataCollection("compartment", dataDC, Compartment.class);
        dc = dataDC.get("compartment");
        dc.getInfo().writeProperty( DataCollectionConfigConstants.ID_FORMAT, "CMP0000" );
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName());
        dc.getInfo().writeProperty(QuerySystem.INDEX_LIST, "title");
        dc.getInfo().writeProperty(TitleIndex.INDEX_TITLE, TitleIndex.class.getName());

        createDataCollection("concept", dataDC, Concept.class);
        dc = dataDC.get("concept");
        dc.getInfo().writeProperty( DataCollectionConfigConstants.ID_FORMAT, "CPT000000" );
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName());
        dc.getInfo().writeProperty(QuerySystem.INDEX_LIST, "title");
        dc.getInfo().writeProperty(TitleIndex.INDEX_TITLE, TitleIndex.class.getName());

        createDataCollection("complex", dataDC, Complex.class);
        dc = dataDC.get("complex");
        dc.getInfo().writeProperty( DataCollectionConfigConstants.ID_FORMAT, "CPX000000" );
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName());
        dc.getInfo().writeProperty(QuerySystem.INDEX_LIST, "title");
        dc.getInfo().writeProperty(TitleIndex.INDEX_TITLE, TitleIndex.class.getName());
        
        createDataCollection("substance", dataDC, Substance.class);
        dc = dataDC.get("substance");
        dc.getInfo().writeProperty( DataCollectionConfigConstants.ID_FORMAT, "SBS000000" );
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName());
        dc.getInfo().writeProperty(QuerySystem.INDEX_LIST, "title");
        dc.getInfo().writeProperty(TitleIndex.INDEX_TITLE, TitleIndex.class.getName());

        createDataCollection("gene", dataDC, Gene.class);
        dc = dataDC.get("gene");
        dc.getInfo().writeProperty( DataCollectionConfigConstants.ID_FORMAT, "GEN000000" );
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName());
        dc.getInfo().writeProperty(QuerySystem.INDEX_LIST, "title");
        dc.getInfo().writeProperty(TitleIndex.INDEX_TITLE, TitleIndex.class.getName());

        createDataCollection("rna", dataDC, RNA.class);
        dc = dataDC.get("rna");
        dc.getInfo().writeProperty( DataCollectionConfigConstants.ID_FORMAT, "RNA000000" );
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName());
        dc.getInfo().writeProperty(QuerySystem.INDEX_LIST, "title");
        dc.getInfo().writeProperty(TitleIndex.INDEX_TITLE, TitleIndex.class.getName());

        createDataCollection("protein", dataDC, Protein.class);
        dc = dataDC.get("protein");
        dc.getInfo().writeProperty( DataCollectionConfigConstants.ID_FORMAT, "PRT000000" );
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName());
        dc.getInfo().writeProperty(QuerySystem.INDEX_LIST, "title");
        dc.getInfo().writeProperty(TitleIndex.INDEX_TITLE, TitleIndex.class.getName());

        createDataCollection("relation", dataDC, SemanticRelation.class);
        dc = dataDC.get("relation");
        dc.getInfo().writeProperty( DataCollectionConfigConstants.ID_FORMAT, "RLT000000" );

        createDataCollection("reaction", dataDC, Reaction.class, ReactionTransformer.class);
        dc = dataDC.get("reaction");
        dc.getInfo().writeProperty( DataCollectionConfigConstants.ID_FORMAT, "RCT000000" );
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName());
        dc.getInfo().writeProperty(QuerySystem.INDEX_LIST, "title");
        dc.getInfo().writeProperty(TitleIndex.INDEX_TITLE, TitleIndex.class.getName());

        createDataCollection("literature", dataDC, Publication.class);
        dc = dataDC.get("literature");
        dc.getInfo().writeProperty( DataCollectionConfigConstants.ID_FORMAT, "LIT000000" );
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName());
        dc.getInfo().writeProperty(QuerySystem.INDEX_LIST, "title");
        dc.getInfo().writeProperty(TitleIndex.INDEX_TITLE, TitleIndex.class.getName());

        createDataCollection(DATABASE_INFO, metadataDC, DatabaseInfo.class);
        createDataCollection(RELATION_TYPE, metadataDC, RelationType.class);
        createDataCollection(SPECIES, metadataDC, Species.class);
        createDataCollection(UNIT, metadataDC, Unit.class);

        // create file collections
        Properties additional = new Properties();
        CollectionFactoryUtils.createTransformedFileCollection(moduleLR, Module.DIAGRAM, "", DiagramXmlTransformer.class, additional);

        return module;
    }

    protected void createDataCollection(String name, Repository dataDC, Class<? extends DataElement> dataElementType) throws Exception
    {
        createDataCollection(name, dataDC, dataElementType, BeanInfoEntryTransformer.class);
    }

    protected void createDataCollection(String name, Repository dataDC, Class<? extends DataElement> dataElementType, Class<?> transformerClass)
            throws Exception
    {
        CollectionFactoryUtils.createTransformedCollection(dataDC, name, transformerClass, dataElementType, null, null, ".dat", "ID", "ID",
                "//", null);
    }

    @Override
    public String getVersion()
    {
        return VERSION;
    }

    @Override
    public boolean canCreateEmptyModule()
    {
        return true;
    }
}
