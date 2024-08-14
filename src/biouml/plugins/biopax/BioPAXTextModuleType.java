package biouml.plugins.biopax;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.model.util.DiagramXmlTransformer;
import biouml.plugins.biopax.access.BioPaxOwlDataCollection;
import biouml.plugins.biopax.model.BioSource;
import biouml.plugins.biopax.model.OpenControlledVocabulary;
import biouml.plugins.lucene.LuceneInitListener;
import biouml.plugins.lucene.LuceneQuerySystem;
import biouml.plugins.lucene.LuceneQuerySystemImpl;
import biouml.plugins.lucene.LuceneUtils;
import biouml.plugins.sbgn.SbgnDiagramType;
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
import biouml.standard.type.access.ReactionTransformer;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.EntryCollection;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.ExProperties;

public class BioPAXTextModuleType extends DataElementSupport implements ModuleType
{
    protected FunctionJobControl jobControl;

    public static final String VERSION = "0.7.7";

    protected String filenames[];
    private boolean stop = false;

    public BioPAXTextModuleType()
    {
        super("BioPAX text", null);
    }

    public void setFileNames(String[] filenames)
    {
        this.filenames = filenames;
    }

    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        return new Class[] {SbgnDiagramType.class};
    }

    @Override
    public String[] getXmlDiagramTypes()
    {
        return new String[]{};
    }
    
    @Override
    public boolean isCategorySupported()
    {
        return true;
    }

    @Override
    public String getCategory(Class<? extends DataElement> c)
    {
        if( Complex.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.COMPLEX;
        if( SemanticRelation.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.CONTROL;
        if( Reaction.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.CONVERSION;
        if( DNA.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.DNA;
        if( Protein.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.PROTEIN;
        if( RNA.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.RNA;
        if( Substance.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.SMALL_MOLECULE;
        if( SpecieReference.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.PARTICIPANT;
        if( Publication.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.PUBLICATION;
        if( DatabaseInfo.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.DATA_SOURCE;
        if( OpenControlledVocabulary.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.VOCABULARY;
        if( BioSource.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.ORGANISM;
        if( Concept.class.isAssignableFrom(c) )
            return Module.DATA + "/" + BioPAXSupport.PHYSICAL_ENTITY;

        throw new IllegalArgumentException("Unknown kernel class in BioPAX categoriser: " + c.getName());
    }

    @Override
    public Module createModule(Repository parent, String name) throws Exception
    {
        // get BioPaxOwlDataCollection
        Properties owlproperties = new Properties();
        owlproperties.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, filenames[0] );
        BioPaxOwlDataCollection bodc = new BioPaxOwlDataCollection(null, owlproperties);
        // Create Module data collection (root)
        Properties primary = new Properties();
        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName());

        Properties transformed = new Properties();
        transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, Module.class.getName());
        transformed.setProperty(Module.TYPE_PROPERTY, BioPAXTextModuleType.class.getName());
        transformed.setProperty(QuerySystem.QUERY_SYSTEM_CLASS, LuceneQuerySystemImpl.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.DATA_COLLECTION_LISTENER, LuceneInitListener.class.getName());
        transformed.setProperty(LuceneQuerySystem.LUCENE_INDEX_DIRECTORY, LuceneUtils.INDEX_FOLDER_NAME);
        transformed.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.biopax");

        Module module = (Module)CollectionFactoryUtils.createDerivedCollection(parent, name, primary, transformed, name);
        LocalRepository moduleLR = (LocalRepository)module.getPrimaryCollection();

        // init Data data collection
        Properties props = new Properties();
        props.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, Module.DATA);
        props.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName());

        Map<String, DataCollection<?>> collections = new HashMap<>();

        Repository dataDC = CollectionFactoryUtils.createLocalRepository(moduleLR, Module.DATA);

        collections.put(Module.DATA, dataDC);

        // init data collections
        DataCollection<?> dc;

        createDataCollection(BioPAXSupport.PHYSICAL_ENTITY, dataDC, Concept.class);
        dc = dataDC.get(BioPAXSupport.PHYSICAL_ENTITY);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        dc.getInfo().writeProperty(LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;synonyms;comment;");
        collections.put(BioPAXSupport.PHYSICAL_ENTITY, dc);

        createDataCollection(BioPAXSupport.COMPLEX, dataDC, Complex.class);
        dc = dataDC.get(BioPAXSupport.COMPLEX);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        dc.getInfo().writeProperty(LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;synonyms;components;comment;");
        collections.put(BioPAXSupport.COMPLEX, dc);

        createDataCollection(BioPAXSupport.CONTROL, dataDC, SemanticRelation.class);
        dc = dataDC.get(BioPAXSupport.CONTROL);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        dc.getInfo().writeProperty(LuceneQuerySystem.LUCENE_INDEXES,
                "name;inputElementName;outputElementName;relationType;participation;title;comment;");
        collections.put(BioPAXSupport.CONTROL, dc);

        createDataCollection(BioPAXSupport.CONVERSION, dataDC, Reaction.class, ReactionTransformer.class);
        dc = dataDC.get(BioPAXSupport.CONVERSION);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        dc.getInfo().writeProperty(LuceneQuerySystem.LUCENE_INDEXES, "name;title;comment;");
        collections.put(BioPAXSupport.CONVERSION, dc);

        createDataCollection(BioPAXSupport.DNA, dataDC, DNA.class);
        dc = dataDC.get(BioPAXSupport.DNA);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        dc.getInfo().writeProperty(LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;synonyms;comment;");
        collections.put(BioPAXSupport.DNA, dc);

        createDataCollection(BioPAXSupport.PROTEIN, dataDC, Protein.class);
        dc = dataDC.get(BioPAXSupport.PROTEIN);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        dc.getInfo().writeProperty(LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;synonyms;comment;");
        collections.put(BioPAXSupport.PROTEIN, dc);

        createDataCollection(BioPAXSupport.RNA, dataDC, RNA.class);
        dc = dataDC.get(BioPAXSupport.RNA);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        dc.getInfo().writeProperty(LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;synonyms;comment;");
        collections.put(BioPAXSupport.RNA, dc);

        createDataCollection(BioPAXSupport.SMALL_MOLECULE, dataDC, Substance.class);
        dc = dataDC.get(BioPAXSupport.SMALL_MOLECULE);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        dc.getInfo().writeProperty(LuceneQuerySystem.LUCENE_INDEXES, "name;title;completeName;formula;synonyms;comment;");
        collections.put(BioPAXSupport.SMALL_MOLECULE, dc);

        createDataCollection(BioPAXSupport.PARTICIPANT, dataDC, SpecieReference.class);
        dc = dataDC.get(BioPAXSupport.PARTICIPANT);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        dc.getInfo().writeProperty(LuceneQuerySystem.LUCENE_INDEXES, "name;specie;role;stoichiometry;title;comment;");
        collections.put(BioPAXSupport.PARTICIPANT, dc);

        /*createDataCollection(BioPAXSupport.PATHWAY, dataDC, DiagramInfo.class);
        dc = (DataCollection)dataDC.get(BioPAXSupport.PATHWAY);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        dc.getInfo().writeProperty(LuceneQuerySystem.LUCENE_INDEXES, "name;title;comment;");
        collections.put(BioPAXSupport.PATHWAY, dc);*/

        createDataCollection(BioPAXSupport.PUBLICATION, dataDC, Publication.class);
        dc = dataDC.get(BioPAXSupport.PUBLICATION);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        dc.getInfo().writeProperty(LuceneQuerySystem.LUCENE_INDEXES,
                "name;authors;title;journalTitle;pageFrom;pageTo;month;language;publicationType;db;dbVersion;idName;idVersion;comment;");
        collections.put(BioPAXSupport.PUBLICATION, dc);

        props = new Properties();
        props.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, Module.METADATA);
        props.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName());

        Repository metadataDC = CollectionFactoryUtils.createLocalRepository(moduleLR, Module.METADATA);

        collections.put(Module.METADATA, metadataDC);

        createDataCollection(BioPAXSupport.VOCABULARY, metadataDC, OpenControlledVocabulary.class);
        dc = metadataDC.get(BioPAXSupport.VOCABULARY);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        collections.put(BioPAXSupport.VOCABULARY, dc);

        createDataCollection(BioPAXSupport.DATA_SOURCE, metadataDC, DatabaseInfo.class);
        dc = metadataDC.get(BioPAXSupport.DATA_SOURCE);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        collections.put(BioPAXSupport.DATA_SOURCE, dc);

        createDataCollection(BioPAXSupport.ORGANISM, metadataDC, BioSource.class);
        dc = metadataDC.get(BioPAXSupport.ORGANISM);
        dc.getInfo().setQuerySystem(new BioPAXQuerySystem(dc));
        dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, BioPAXQuerySystem.class.getName());
        collections.put(BioPAXSupport.ORGANISM, dc);

        CollectionFactoryUtils.createTransformedFileCollection(moduleLR, Module.DIAGRAM, "", DiagramXmlTransformer.class);
        dc = moduleLR.get(Module.DIAGRAM);
        collections.put(Module.DIAGRAM, dc);

        if( !bodc.initWithCollections(collections, jobControl, filenames.length, 1) )
            stop = true;

        module.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, LuceneQuerySystemImpl.class.getName());

        return module;
    }

    public void addPathways(String[] files, DataCollection<?> primaryCollection) throws Exception
    {
        if( files == null )
            return;

        if( stop )
            return;

        Map<String, DataCollection<?>> collections = new HashMap<>();

        DataCollection<?> dataDC = (DataCollection<?>)primaryCollection.get(Module.DATA);

        collections.put(Module.DATA, dataDC);

        // init data collections
        DataCollection<?> dc;

        dc = (DataCollection<?>)dataDC.get(BioPAXSupport.PHYSICAL_ENTITY);
        collections.put(BioPAXSupport.PHYSICAL_ENTITY, dc);

        dc = (DataCollection<?>)dataDC.get(BioPAXSupport.COMPLEX);
        collections.put(BioPAXSupport.COMPLEX, dc);

        dc = (DataCollection<?>)dataDC.get(BioPAXSupport.CONTROL);
        collections.put(BioPAXSupport.CONTROL, dc);

        dc = (DataCollection<?>)dataDC.get(BioPAXSupport.CONVERSION);
        collections.put(BioPAXSupport.CONVERSION, dc);

        dc = (DataCollection<?>)dataDC.get(BioPAXSupport.DNA);
        collections.put(BioPAXSupport.DNA, dc);

        dc = (DataCollection<?>)dataDC.get(BioPAXSupport.PROTEIN);
        collections.put(BioPAXSupport.PROTEIN, dc);

        dc = (DataCollection<?>)dataDC.get(BioPAXSupport.RNA);
        collections.put(BioPAXSupport.RNA, dc);

        dc = (DataCollection<?>)dataDC.get(BioPAXSupport.SMALL_MOLECULE);
        collections.put(BioPAXSupport.SMALL_MOLECULE, dc);

        dc = (DataCollection<?>)dataDC.get(BioPAXSupport.PARTICIPANT);
        collections.put(BioPAXSupport.PARTICIPANT, dc);

        dc = (DataCollection<?>)dataDC.get(BioPAXSupport.PUBLICATION);
        collections.put(BioPAXSupport.PUBLICATION, dc);

        DataCollection<?> metadataDC = (DataCollection<?>)primaryCollection.get(Module.METADATA);

        collections.put(Module.METADATA, metadataDC);

        dc = (DataCollection<?>)metadataDC.get(BioPAXSupport.VOCABULARY);
        collections.put(BioPAXSupport.VOCABULARY, dc);

        dc = (DataCollection<?>)metadataDC.get(BioPAXSupport.DATA_SOURCE);
        collections.put(BioPAXSupport.DATA_SOURCE, dc);

        dc = (DataCollection<?>)metadataDC.get(BioPAXSupport.ORGANISM);
        collections.put(BioPAXSupport.ORGANISM, dc);

        DataCollection<?> diagrams = (DataCollection<?>)primaryCollection.get(Module.DIAGRAM);
        collections.put(Module.DIAGRAM, diagrams);

        int currentNumber = 1;
        int length = files.length;
        if( filenames != null )
        {
            currentNumber++;
            length++;
        }
        for( int i = 0; i < files.length; i++ )
        {
            if( files[i] != null && !files[i].equals("") )
            {
                Properties owlproperties = new Properties();
                owlproperties.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, files[i] );
                BioPaxOwlDataCollection bodc = new BioPaxOwlDataCollection(null, owlproperties);
                if( !bodc.initWithCollections(collections, jobControl, length, currentNumber) )
                    break;
            }
            currentNumber++;
        }
    }
    protected void createDataCollection(String name, Repository dataDC, Class<?> dataElementType) throws Exception
    {
        createDataCollection(name, dataDC, dataElementType, BeanInfoEntryTransformer.class);
    }

    protected void createDataCollection(String name, Repository dataDC, Class<?> dataElementType, Class<?> transformerClass)
            throws Exception
    {
        Properties primary = new ExProperties();
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileEntryCollection2.class.getName());
        primary.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, name + ".dat");
        primary.setProperty(EntryCollection.ENTRY_START_PROPERTY, "ID");
        primary.setProperty(EntryCollection.ENTRY_ID_PROPERTY, "ID");
        primary.setProperty(EntryCollection.ENTRY_END_PROPERTY, "//");
        primary.setProperty(EntryCollection.ENTRY_DELIMITERS_PROPERTY, "\"; \"");
        primary.setProperty(EntryCollection.ENTRY_KEY_FULL, "true");
        
        Properties transformed = new ExProperties();
        transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, TransformedDataCollection.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.TRANSFORMER_CLASS, transformerClass.getName());
        transformed.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.biopax");
        
        if( dataElementType != null )
            transformed.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, dataElementType.getName());

        CollectionFactoryUtils.createDerivedCollection(dataDC, name, primary, transformed, null);
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

    public void setJobControl(FunctionJobControl jobControl)
    {
        this.jobControl = jobControl;
    }
}
