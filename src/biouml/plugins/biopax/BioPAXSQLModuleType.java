
package biouml.plugins.biopax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.plugins.biopax.access.BioPaxOwlDataCollection;
import biouml.plugins.biopax.access.BioSourceSqlTransformer;
import biouml.plugins.biopax.access.ComplexSqlTransformer;
import biouml.plugins.biopax.access.OpenControlledVocabularySqlTransfromer;
import biouml.plugins.biopax.access.SpecieReferenceSqlTransformer;
import biouml.plugins.biopax.biohub.BioHubBuilder;
import biouml.plugins.biopax.biohub.BioPAXSQLHubBuilder;
import biouml.plugins.biopax.model.BioSource;
import biouml.plugins.biopax.model.OpenControlledVocabulary;
import biouml.plugins.lucene.LuceneQuerySystem;
import biouml.plugins.lucene.LuceneQuerySystemImpl;
import biouml.plugins.lucene.LuceneUtils;
import biouml.plugins.server.SqlModule;
import biouml.plugins.server.access.SQLRegistry.SQLInfo;
import biouml.standard.type.Complex;
import biouml.standard.type.Concept;
import biouml.standard.type.DNA;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.Protein;
import biouml.standard.type.Publication;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Substance;
import biouml.standard.type.access.ConceptSqlTransformer;
import biouml.standard.type.access.DatabaseInfoSqlTransformer;
import biouml.standard.type.access.DiagramSqlTransformer;
import biouml.standard.type.access.ProteinSqlTransformer;
import biouml.standard.type.access.PublicationSqlTransformer;
import biouml.standard.type.access.RNASqlTransformer;
import biouml.standard.type.access.ReactionSqlTransformer;
import biouml.standard.type.access.RelationSqlTransformer;
import biouml.standard.type.access.SubstanceSqlTransformer;
import biouml.standard.type.access.TitleSqlNoHtmlIndex;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.biohub.CollectionSpecificReferenceType;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.ExProperties;

/**
 * @author anna
 *
 */
public class BioPAXSQLModuleType extends DataElementSupport implements ModuleType
{

    private static final String VERSION = "0.9.2";
    private Properties databaseProperties = new Properties();

    private String filenames[];
    private FunctionJobControl jobControl;

    private boolean isInitialized = true;
    private static final String KEYNODES_HUB_SUFFIX = " collection";
    private static final String MATCHING_HUB_SUFFIX = " matching";
    private static final String FUNCTIONAL_HUB_SUFFIX = " pathways";
    private static final String SEARCH_HUB_SUFFIX = " search";

    public void setFileNames(String[] filenames)
    {
        this.filenames = filenames;
    }

    public BioPAXSQLModuleType()
    {
        super( "BioPAX SQL module", null );
    }

    public BioPAXSQLModuleType(String name, DataCollection<?> origin)
    {
        super( name, origin );
    }

    @Override
    public boolean canCreateEmptyModule()
    {
        return false;
    }

    @Override
    public String getCategory(Class<? extends DataElement> c)
    {
        if( Complex.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.COMPLEX;
        if( SemanticRelation.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.CONTROL;
        if( Reaction.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.CONVERSION;
        if( DNA.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.DNA;
        if( Protein.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.PROTEIN;
        if( RNA.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.RNA;
        if( Substance.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.SMALL_MOLECULE;
        if( SpecieReference.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.PARTICIPANT;
        if( Publication.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.PUBLICATION;
        if( DatabaseInfo.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.DATA_SOURCE;
        if( OpenControlledVocabulary.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.VOCABULARY;
        if( BioSource.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.ORGANISM;
        if( Concept.class.isAssignableFrom( c ) )
            return Module.DATA + "/" + BioPAXSupport.PHYSICAL_ENTITY;

        throw new IllegalArgumentException( "Unknown kernel class in BioPAX categoriser: " + c.getName() );
    }

    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return new Class[] {BioPAXDiagramType.class};
    }

    @Override
    public String getVersion()
    {
        return VERSION;
    }

    @Override
    public String[] getXmlDiagramTypes()
    {
        return new String[] {"sbgn_simulation.xml"};
    }

    @Override
    public boolean isCategorySupported()
    {
        return true;
    }

    public void setDatabaseProperties(Properties properties)
    {
        this.databaseProperties = properties;
    }

    public void setDatabaseProperties(SQLInfo sqlInfo)
    {
        databaseProperties.setProperty( SqlDataCollection.JDBC_DRIVER_PROPERTY, "com.mysql.jdbc.Driver" );
        databaseProperties.setProperty( SqlDataCollection.JDBC_URL_PROPERTY, sqlInfo.getJdbcUrl() );
        databaseProperties.setProperty( SqlDataCollection.JDBC_USER_PROPERTY, sqlInfo.getUsername() );
        databaseProperties.setProperty( SqlDataCollection.JDBC_PASSWORD_PROPERTY, sqlInfo.getPassword() );
    }

    public void copyDatabaseProperties(Properties properties)
    {
        databaseProperties.setProperty( SqlDataCollection.JDBC_DRIVER_PROPERTY, "com.mysql.jdbc.Driver" );
        databaseProperties.setProperty( SqlDataCollection.JDBC_URL_PROPERTY, properties.getProperty( SqlDataCollection.JDBC_URL_PROPERTY ) );
        databaseProperties
                .setProperty( SqlDataCollection.JDBC_USER_PROPERTY, properties.getProperty( SqlDataCollection.JDBC_USER_PROPERTY ) );
        databaseProperties.setProperty( SqlDataCollection.JDBC_PASSWORD_PROPERTY,
                properties.getProperty( SqlDataCollection.JDBC_PASSWORD_PROPERTY ) );
    }

    public Properties getDatabaseProperties()
    {
        return databaseProperties;
    }

    public void setJobControl(FunctionJobControl jobControl)
    {
        this.jobControl = jobControl;
    }

    private static boolean classExists(@Nonnull String className)
    {
        try
        {
            ClassLoading.loadClass( className );
            return true;
        }
        catch( Exception e )
        {
        }
        return false;
    }

    @Override
    public Module createModule(Repository parent, String name) throws Exception
    {
        // get BioPaxOwlDataCollection
        Properties owlproperties = new Properties();
        owlproperties.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, filenames[0] );
        BioPaxOwlDataCollection bodc = new BioPaxOwlDataCollection( null, owlproperties );

        String hubTableName = name.replaceAll( "[^\\w]", "_" ) + "_biohub";

        // Create Module data collection (root)
        Properties primary = new ExProperties();
        primary.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        primary.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );

        Properties transformed = new ExProperties();
        for( Entry<Object, Object> entry : databaseProperties.entrySet() )
        {
            transformed.setProperty( entry.getKey().toString(), entry.getValue().toString() );
        }
        transformed.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, SqlModule.class.getName() );
        transformed.setProperty( Module.TYPE_PROPERTY, BioPAXSQLModuleType.class.getName() );
        //        transformed.setProperty(DataCollectionConfigConstants.DATA_COLLECTION_LISTENER, LuceneInitListener.class.getName());
        transformed.setProperty( LuceneQuerySystem.LUCENE_INDEX_DIRECTORY, LuceneUtils.INDEX_FOLDER_NAME );
        transformed.setProperty( QuerySystem.QUERY_SYSTEM_CLASS, LuceneQuerySystemImpl.class.getName() );
        transformed.setProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.biopax" );
        transformed.setProperty( BioPAXSQLHubBuilder.HUB_TABLE_PROPERTY, hubTableName );

        //PENDING: this way to prepare hub list is not completely correct
        transformed.setProperty( "bioHub.matching", "biouml.plugins.biopax.biohub.BioPAXSQLMatchingHub;name=" + name + MATCHING_HUB_SUFFIX );
        if( classExists( "biouml.plugins.keynodes.biohub.biopax.BioPAXSQLBioHub" ) )
        {
            transformed.setProperty( "bioHub.keynodes", "biouml.plugins.keynodes.biohub.biopax.BioPAXSQLBioHub;name=" + name
                    + KEYNODES_HUB_SUFFIX );
        }
        if( classExists( "biouml.plugins.enrichment.biopax.BioPaxFunctionalHub" ) )
        {
            transformed.setProperty( "bioHub.enrichment", "biouml.plugins.enrichment.biopax.BioPaxFunctionalHub;name=" + name
                    + FUNCTIONAL_HUB_SUFFIX );
        }
        transformed.setProperty( ReferenceType.REFERENCE_TYPE_PROPERTY, CollectionSpecificReferenceType.class.getSimpleName() );

        Module module = (Module)CollectionFactoryUtils.createDerivedCollection( parent, name, primary, transformed, name );
        LocalRepository moduleLR = (LocalRepository)module.getPrimaryCollection();

        // init Data data collection
        Properties props = new Properties();
        props.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, Module.DATA );
        props.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );

        Map<String, DataCollection<?>> collections = new HashMap<>();

        Repository dataDC = CollectionFactoryUtils.createLocalRepository( moduleLR, Module.DATA );

        collections.put( Module.DATA, dataDC );

        // init data collections
        DataCollection<?> dc;

        createDataCollection( BioPAXSupport.PHYSICAL_ENTITY, dataDC, Concept.class, ConceptSqlTransformer.class );
        dc = dataDC.get( BioPAXSupport.PHYSICAL_ENTITY );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        dc.getInfo().writeProperty( LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;synonyms;comment;" );
        collections.put( BioPAXSupport.PHYSICAL_ENTITY, dc );

        createDataCollection( BioPAXSupport.COMPLEX, dataDC, Complex.class, ComplexSqlTransformer.class );
        dc = dataDC.get( BioPAXSupport.COMPLEX );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        dc.getInfo().writeProperty( LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;synonyms;components;comment;" );
        collections.put( BioPAXSupport.COMPLEX, dc );

        createDataCollection( BioPAXSupport.CONTROL, dataDC, SemanticRelation.class, RelationSqlTransformer.class );
        dc = dataDC.get( BioPAXSupport.CONTROL );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        dc.getInfo().writeProperty( LuceneQuerySystem.LUCENE_INDEXES,
                "name;inputElementName;outputElementName;relationType;participation;title;comment;" );
        collections.put( BioPAXSupport.CONTROL, dc );

        createDataCollection( BioPAXSupport.CONVERSION, dataDC, Reaction.class, ReactionSqlTransformer.class );
        dc = dataDC.get( BioPAXSupport.CONVERSION );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        dc.getInfo().writeProperty( LuceneQuerySystem.LUCENE_INDEXES, "name;title;comment;" );
        collections.put( BioPAXSupport.CONVERSION, dc );

        createDataCollection( BioPAXSupport.DNA, dataDC, DNA.class, RNASqlTransformer.class );
        dc = dataDC.get( BioPAXSupport.DNA );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        dc.getInfo().writeProperty( LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;synonyms;comment;" );
        collections.put( BioPAXSupport.DNA, dc );

        createDataCollection( BioPAXSupport.PROTEIN, dataDC, Protein.class, ProteinSqlTransformer.class );
        dc = dataDC.get( BioPAXSupport.PROTEIN );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        dc.getInfo().writeProperty( LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;synonyms;comment;" );
        collections.put( BioPAXSupport.PROTEIN, dc );

        createDataCollection( BioPAXSupport.RNA, dataDC, RNA.class, RNASqlTransformer.class );
        dc = dataDC.get( BioPAXSupport.RNA );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        dc.getInfo().writeProperty( LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;synonyms;comment;" );
        collections.put( BioPAXSupport.RNA, dc );

        createDataCollection( BioPAXSupport.SMALL_MOLECULE, dataDC, Substance.class, SubstanceSqlTransformer.class );
        dc = dataDC.get( BioPAXSupport.SMALL_MOLECULE );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        dc.getInfo().writeProperty( LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;formula;synonyms;comment;" );
        collections.put( BioPAXSupport.SMALL_MOLECULE, dc );

        createDataCollection( BioPAXSupport.PARTICIPANT, dataDC, SpecieReference.class, SpecieReferenceSqlTransformer.class );
        dc = dataDC.get( BioPAXSupport.PARTICIPANT );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        dc.getInfo().writeProperty( LuceneQuerySystem.LUCENE_INDEXES, "name;specie;role;stoichiometry;title;comment;" );
        collections.put( BioPAXSupport.PARTICIPANT, dc );

        createDataCollection( BioPAXSupport.PUBLICATION, dataDC, Publication.class, PublicationSqlTransformer.class );
        dc = dataDC.get( BioPAXSupport.PUBLICATION );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        dc.getInfo().writeProperty( LuceneQuerySystem.LUCENE_INDEXES,
                "name;authors;title;journalTitle;pageFrom;pageTo;month;language;publicationType;db;dbVersion;idName;idVersion;comment;" );
        collections.put( BioPAXSupport.PUBLICATION, dc );

        Repository metadataDC = CollectionFactoryUtils.createLocalRepository( moduleLR, Module.METADATA );
        collections.put( Module.METADATA, metadataDC );

        createDataCollection( BioPAXSupport.VOCABULARY, metadataDC, OpenControlledVocabulary.class,
                OpenControlledVocabularySqlTransfromer.class );
        dc = metadataDC.get( BioPAXSupport.VOCABULARY );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        collections.put( BioPAXSupport.VOCABULARY, dc );

        createDataCollection( BioPAXSupport.DATA_SOURCE, metadataDC, DatabaseInfo.class, DatabaseInfoSqlTransformer.class );
        dc = metadataDC.get( BioPAXSupport.DATA_SOURCE );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        collections.put( BioPAXSupport.DATA_SOURCE, dc );

        createDataCollection( BioPAXSupport.ORGANISM, metadataDC, BioSource.class, BioSourceSqlTransformer.class );
        dc = metadataDC.get( BioPAXSupport.ORGANISM );
        dc.getInfo().setQuerySystem( new BioPAXQuerySystem( dc ) );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName() );
        collections.put( BioPAXSupport.ORGANISM, dc );

        CollectionFactoryUtils.createTransformedSqlCollection( moduleLR, Module.DIAGRAM, DiagramSqlTransformer.class, Diagram.class,
                new Properties() );
        dc = moduleLR.get( Module.DIAGRAM );
        dc.getInfo().writeProperty( "kernelReferenceType", new CollectionSpecificReferenceType( module ).toString() );
        dc.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName() );
        dc.getInfo().writeProperty( QuerySystem.INDEX_LIST, "title" );
        dc.getInfo().writeProperty( "index.title", TitleSqlNoHtmlIndex.class.getName() );
        dc.getInfo().writeProperty( "index.title.query", "SELECT ID,title FROM diagrams" );
        collections.put( Module.DIAGRAM, dc );

        Properties properties = new Properties( databaseProperties );
        properties.setProperty( BioPAXSQLHubBuilder.HUB_TABLE_PROPERTY, hubTableName );
        BioHubBuilder builder = new BioPAXSQLHubBuilder( properties );
        bodc.setBioHubBuilder( builder );
        isInitialized = bodc.initWithCollections( collections, jobControl, filenames.length, 1 );
        ReferenceType[] matchingTypes = builder.getMatchingTypes();
        if( matchingTypes != null && matchingTypes.length > 0 )
        {
            module.getInfo().writeProperty( ReferenceType.MATCHING_TYPE_PROPERTY, getMatchingTypesProperty( matchingTypes ) );
        }
        module.getInfo().writeProperty( QuerySystem.QUERY_SYSTEM_CLASS, LuceneQuerySystemImpl.class.getName() );
        return module;
    }

    protected <T extends DataElement> void createDataCollection(String name, Repository dataDC, Class<T> dataElementType,
            Class transformerClass) throws Exception
    {
        Properties properties = new ExProperties();
        properties.setProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.biopax" );
        CollectionFactoryUtils.createTransformedSqlCollection( dataDC, name, transformerClass, dataElementType, properties );
    }

    public void addPathways(String[] files, Module module) throws Exception
    {
        if( files == null )
            return;
        if( !isInitialized )
            return;

        DataCollection<?> primaryCollection = module.getPrimaryCollection();

        Map<String, DataCollection<?>> collections = new HashMap<>();

        DataCollection<?> dataDC = (DataCollection<?>)primaryCollection.get( Module.DATA );

        collections.put( Module.DATA, dataDC );

        // init data collections
        DataCollection<?> dc;

        dc = (DataCollection<?>)dataDC.get( BioPAXSupport.PHYSICAL_ENTITY );
        collections.put( BioPAXSupport.PHYSICAL_ENTITY, dc );

        dc = (DataCollection<?>)dataDC.get( BioPAXSupport.COMPLEX );
        collections.put( BioPAXSupport.COMPLEX, dc );

        dc = (DataCollection<?>)dataDC.get( BioPAXSupport.CONTROL );
        collections.put( BioPAXSupport.CONTROL, dc );

        dc = (DataCollection<?>)dataDC.get( BioPAXSupport.CONVERSION );
        collections.put( BioPAXSupport.CONVERSION, dc );

        dc = (DataCollection<?>)dataDC.get( BioPAXSupport.DNA );
        collections.put( BioPAXSupport.DNA, dc );

        dc = (DataCollection<?>)dataDC.get( BioPAXSupport.PROTEIN );
        collections.put( BioPAXSupport.PROTEIN, dc );

        dc = (DataCollection<?>)dataDC.get( BioPAXSupport.RNA );
        collections.put( BioPAXSupport.RNA, dc );

        dc = (DataCollection<?>)dataDC.get( BioPAXSupport.SMALL_MOLECULE );
        collections.put( BioPAXSupport.SMALL_MOLECULE, dc );

        dc = (DataCollection<?>)dataDC.get( BioPAXSupport.PARTICIPANT );
        collections.put( BioPAXSupport.PARTICIPANT, dc );

        dc = (DataCollection<?>)dataDC.get( BioPAXSupport.PUBLICATION );
        collections.put( BioPAXSupport.PUBLICATION, dc );

        DataCollection<?> metadataDC = (DataCollection<?>)primaryCollection.get( Module.METADATA );

        collections.put( Module.METADATA, metadataDC );

        dc = (DataCollection<?>)metadataDC.get( BioPAXSupport.VOCABULARY );
        collections.put( BioPAXSupport.VOCABULARY, dc );

        dc = (DataCollection<?>)metadataDC.get( BioPAXSupport.DATA_SOURCE );
        collections.put( BioPAXSupport.DATA_SOURCE, dc );

        dc = (DataCollection<?>)metadataDC.get( BioPAXSupport.ORGANISM );
        collections.put( BioPAXSupport.ORGANISM, dc );

        DataCollection<?> diagrams = (DataCollection<?>)primaryCollection.get( Module.DIAGRAM );
        collections.put( Module.DIAGRAM, diagrams );

        int currentNumber = 1;
        int length = files.length;
        if( filenames != null )
        {
            currentNumber++;
            length++;
        }

        copyDatabaseProperties( module.getInfo().getProperties() );
        Properties hubBuilderProperties = new Properties( databaseProperties );
        hubBuilderProperties.setProperty( BioPAXSQLHubBuilder.HUB_TABLE_PROPERTY,
                module.getInfo().getProperty( BioPAXSQLHubBuilder.HUB_TABLE_PROPERTY ) );
        hubBuilderProperties.setProperty( ReferenceType.MATCHING_TYPE_PROPERTY,
                module.getInfo().getProperty( ReferenceType.MATCHING_TYPE_PROPERTY ) );
        BioHubBuilder builder = new BioPAXSQLHubBuilder( hubBuilderProperties );
        List<String> notLoaded = new ArrayList<>();
        for( int i = 0; i < files.length; i++ )
        {
            if( files[i] != null && !files[i].equals( "" ) )
            {
                Properties owlproperties = new Properties();
                owlproperties.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, files[i] );
                BioPaxOwlDataCollection bodc = new BioPaxOwlDataCollection( null, owlproperties );
                bodc.setBioHubBuilder( builder );
                boolean loaded = false;
                try
                {
                    loaded = bodc.initWithCollections( collections, jobControl, length, currentNumber );
                }
                catch( Exception e )
                {
                }
                if( !loaded )
                    notLoaded.add( files[i] );
            }
            currentNumber++;
        }
        ReferenceType[] matchingTypes = builder.getMatchingTypes();
        if( matchingTypes != null && matchingTypes.length > 0 )
        {
            module.getInfo().writeProperty( ReferenceType.MATCHING_TYPE_PROPERTY, getMatchingTypesProperty( matchingTypes ) );
        }
        builder.finalizeBuilding();
        if( notLoaded.size() > 0 )
            throw new Exception( "Files " + String.join( ", ", notLoaded ) + " were loaded with errors." );
    }

    private String getMatchingTypesProperty(ReferenceType[] types)
    {
        return StreamEx.of( types ).map( ReferenceType::getStableName ).joining( ";" );
    }
}
