package biouml.workbench.module.xml;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;

import biouml.model.CollectionDescription;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.util.DiagramXmlTransformer;
import biouml.model.xml.XmlDiagramType;
import biouml.standard.simulation.access.SimulationResultTransformer;
import biouml.standard.simulation.plot.access.PlotTransformer;
import biouml.standard.type.Stub;
import biouml.standard.type.access.DiagramSqlTransformer;
import biouml.workbench.module.xml.editor.IndexPropertyEditor;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.DerivedDataCollection;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.LazyValue;

public class XmlModule extends Module
{
    public static final String XML_DATABASE_FILE = "moduleXml";

    protected static final Logger log = Logger.getLogger(XmlModule.class.getName());

    protected Properties properties;
    protected Properties databaseProperties = new Properties();
    protected String moduleType = XmlModuleConstants.TYPE_TEXT;

    public XmlModule(DataCollection<?> primaryCollection, Properties properties) throws Exception
    {
        super(primaryCollection, properties);
        this.properties = properties;
    }

    public void setReadingAvailable(boolean available)
    {
        isInit = !available;
    }

    protected boolean isInit = false;
    public void initXmlModule()
    {
        if( !isInit )
        {
            isInit = true;

            String name = properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY);
            String filePath = properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
            String moduleXml = properties.getProperty(XML_DATABASE_FILE);
            if( name != null && filePath != null && moduleXml != null )
            {
                try
                {
                    File xmlFile = new File(filePath, moduleXml);
                    if( !xmlFile.exists() )
                    {
                        isInit = false;
                        return;
                    }

                    type = new XmlModuleType();
                    internalTypes = new ArrayList<>();
                    diagramTypeList = new ArrayList<>();
                    externalTypes = new ArrayList<>();

                    try(FileInputStream fis = new FileInputStream(xmlFile))
                    {
                        new XmlModuleReader(name, fis).read(this);
                    }
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "can't read module xml properties", t);
                }
            }
        }
    }

    @Override
    protected void applyType(String className) throws Exception
    {
        type = new XmlModuleType();
    }

    public void setDatabaseProperties(Properties properties)
    {
        this.databaseProperties = properties;
    }
    public Properties getDatabaseProperties()
    {
        return databaseProperties;
    }

    public String getModuleType()
    {
        return moduleType;
    }

    public void setModuleType(String moduleType)
    {
        this.moduleType = moduleType;
    }

    public void initSections()
    {
        initStandardSection(Module.DATA);
        LocalRepository moduleLR = (LocalRepository)getPrimaryCollection();

        //init daigram section
        if( !contains(Module.DIAGRAM) )
        {
            try
            {
                if( moduleType.equals(XmlModuleConstants.TYPE_TEXT) )
                {
                    CollectionFactoryUtils.createTransformedFileCollection(moduleLR, Module.DIAGRAM, "", DiagramXmlTransformer.class);
                }
                else if( moduleType.equals(XmlModuleConstants.TYPE_SQL) )
                {
                    CollectionFactoryUtils.createTransformedSqlCollection(moduleLR, Module.DIAGRAM, DiagramSqlTransformer.class, Diagram.class,
                            databaseProperties);
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not create " + Module.DIAGRAM + " section.", t);
            }
        }
        ( (XmlModuleType)type ).addType(Diagram.class, Module.DIAGRAM);

        //init simulation section
        if( !contains(Module.SIMULATION) )
        {
            try
            {
                if( moduleType.equals(XmlModuleConstants.TYPE_TEXT) )
                {
                    Repository simulationDC = CollectionFactoryUtils.createLocalRepository(moduleLR, Module.SIMULATION);

                    CollectionFactoryUtils.createTransformedFileCollection(simulationDC, "result", "", SimulationResultTransformer.class);
                    CollectionFactoryUtils.createTransformedFileCollection(simulationDC, "plot", "", PlotTransformer.class);
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not create " + Module.SIMULATION + " section.", t);
            }
        }
    }

    public void initStandardSection(String sectionName)
    {
        if( !contains(sectionName) )
        {
            // init Data data collection
            try
            {
                LocalRepository moduleLR = getPrimaryCollection().cast( LocalRepository.class );
                CollectionFactoryUtils.createLocalRepository(moduleLR, sectionName);
            }
            catch( Throwable t )
            {
                throw ExceptionRegistry.translateException(t);
            }
        }
    }

    protected List<InternalType> internalTypes = new ArrayList<>();

    public List<InternalType> getInternalTypes()
    {
        return internalTypes;
    }

    public void setInternalTypes(List<InternalType> internalTypes)
    {
        this.internalTypes = internalTypes;
    }

    public void addInternalType(InternalType iType)
    {
        try
        {
            internalTypes.add(iType);
            if( !this.contains(iType.section) )
            {
                initStandardSection(iType.section);
            }

            DataCollection<?> parent = (DataCollection<?>)this.get(iType.section);
            if( parent != null )
            {
                Class<? extends DataElement> typeClass = ClassLoading.loadSubClass( iType.getTypeClass(), pluginNames, DataElement.class );
                ( (XmlModuleType)type ).addType(typeClass, DataElementPath.escapeName(iType.section) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(iType.getName()));
                if( !parent.contains(iType.getName()) )
                {
                    Class transformer = ClassLoading.loadClass( iType.getTypeTransformer(), pluginNames );

                    DataCollection repository = parent;
                    while( ! ( repository instanceof LocalRepository ) && ( repository instanceof DerivedDataCollection ) )
                    {
                        repository = ( (DerivedDataCollection)repository ).getPrimaryCollection();
                    }

                    if( moduleType.equals(XmlModuleConstants.TYPE_TEXT) )
                    {
                        CollectionFactoryUtils.createTransformedCollection((LocalRepository)repository, iType.getName(), transformer, typeClass,
                                null, null, ".dat", "ID", "ID", "//", null);

                        DataCollection<?> dc = (DataCollection<?>)parent.get(iType.getName());
                        if( dc != null )
                        {
                            if( iType.getIdFormat() != null && iType.getIdFormat().length() > 0 )
                                dc.getInfo().writeProperty(DataCollectionConfigConstants.ID_FORMAT, iType.getIdFormat());
                            if( iType.getQuerySystemClass() != null && iType.getQuerySystemClass().length() > 0 )
                                dc.getInfo().writeProperty(QuerySystem.QUERY_SYSTEM_CLASS, iType.getQuerySystemClass());
                            if( iType.getLuceneIndexes() != null && iType.getLuceneIndexes().length() > 0 )
                                dc.getInfo().writeProperty(QuerySystem.INDEX_LIST, iType.getLuceneIndexes());

                            for( biouml.workbench.module.xml.XmlModule.InternalType.IndexDescription indexDescription : iType.getIndexes() )
                            {
                                dc.getInfo().writeProperty("index." + indexDescription.getName(), indexDescription.getIndexClass());
                                if( indexDescription.getTable() != null )
                                {
                                    dc.getInfo().writeProperty("index." + indexDescription.getName() + ".table",
                                            indexDescription.getTable());
                                }
                            }
                        }
                    }
                    else if( moduleType.equals(XmlModuleConstants.TYPE_SQL) )
                    {
                        Properties sqlTypeProperties = new Properties();
                        sqlTypeProperties.putAll( databaseProperties );
                        if( iType.getIdFormat() != null )
                            sqlTypeProperties.put(DataCollectionConfigConstants.ID_FORMAT, iType.getIdFormat());
                        if( iType.getQuerySystemClass() != null )
                            sqlTypeProperties.put(QuerySystem.QUERY_SYSTEM_CLASS, iType.getQuerySystemClass());
                        if( iType.getLuceneIndexes() != null )
                            sqlTypeProperties.put(QuerySystem.INDEX_LIST, iType.getLuceneIndexes());

                        for( biouml.workbench.module.xml.XmlModule.InternalType.IndexDescription indexDescription : iType.getIndexes() )
                        {
                            sqlTypeProperties.put("index." + indexDescription.getName(), indexDescription.getIndexClass());
                            if( indexDescription.getTable() != null )
                            {
                                sqlTypeProperties.put("index." + indexDescription.getName() + ".table", indexDescription.getTable());
                            }
                        }

                        CollectionFactoryUtils.createTransformedSqlCollection((LocalRepository)repository, iType.getName(), transformer,
                                typeClass, sqlTypeProperties);
                    }
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not create data collection " + iType.getName(), t);
        }
    }

    public void addExternalType(CollectionDescription eType)
    {
        if( externalTypes == null )
            externalTypes = new ArrayList<>();
        externalTypes.add(eType);
    }

    public void setExternalType(List<CollectionDescription> externalTypes)
    {
        this.externalTypes = externalTypes;
    }

    protected List<DiagramTypeDescription> diagramTypeList = new ArrayList<>();

    public List<DiagramTypeDescription> getDiagramTypes()
    {
        return diagramTypeList;
    }

    public void setDiagramTypes(List<DiagramTypeDescription> diagramTypes)
    {
        this.diagramTypeList = diagramTypes;
    }

    public void addDiagramType(DiagramTypeDescription dtd)
    {
        if( diagramTypeList == null )
            diagramTypeList = new ArrayList<>();

        diagramTypeList.add(dtd);

        if( dtd.getType().equals(XmlModuleConstants.GRAPHIC_NOTATION_TYPE_JAVA) )
        {
            try
            {
                ( (XmlModuleType)type ).addDiagramType(ClassLoading.loadSubClass( dtd.getClassName(), pluginNames, DiagramType.class ));
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, new DataElementReadException(e, this, "XMLModuleType#"+dtd.getName()).log());
            }
        }
        else if( dtd.getType().equals(XmlModuleConstants.GRAPHIC_NOTATION_TYPE_XML) )
        {
            ( (XmlModuleType)type ).addXmlDiagramType(dtd.path);
        }
    }

    @Override
    public <T extends DataElement> DataCollection<T> getCategory(Class<T> c)
    {
        if( Stub.class.isAssignableFrom(c) )
            return null;

        String name = type.getCategory(c);
        if( name == null )
            return null;

        DataElement de = getDataElement(name);
        if( de instanceof DataCollection )
            return (DataCollection<T>)de;
        return null;
    }

    private DataElement getDataElement(String relativeName)
    {
        DataCollection ancestor = this;
        StringTokenizer st = new StringTokenizer(relativeName, DataElementPath.PATH_SEPARATOR);
        DataElement de = null;
        StringBuilder name;
        while( st.hasMoreTokens() )
        {
            try
            {
                name = new StringBuilder(st.nextToken());
                de = ancestor.getFromCache(name.toString());
                if( de == null )
                    de = ancestor.get(name.toString());

                if( de == null )
                {
                    while( st.hasMoreTokens() )
                    {
                        name.append(DataElementPath.PATH_SEPARATOR).append(st.nextToken());
                        de = ancestor.getFromCache(name.toString());
                        if( de == null )
                            de = ancestor.get(name.toString());
                        if( de != null )
                            break;
                    }

                    if( de == null )
                        return null;
                }

                if( st.hasMoreTokens() )
                {
                    if( de instanceof DataCollection )
                        ancestor = (DataCollection)de;
                    else
                    {
                        // one more try
                        name.append(DataElementPath.PATH_SEPARATOR).append(st.nextToken());
                        de = ancestor.getFromCache(name.toString());
                        if( de == null )
                            de = ancestor.get(name.toString());
                        if( de == null || !st.hasMoreTokens() )
                            return de;

                        if( de instanceof DataCollection )
                            ancestor = (DataCollection)de;
                        else
                            return null;
                    }
                }
            }
            catch( Exception e )
            {
                throw new RuntimeException(e);
            }
        }

        return de;
    }

    ////////////////////////////////////////////////////////////////////////////
    // redefine DataCollection methods due to Module instance lazy initialisation
    //

    @Override
    public int getSize()
    {
        initXmlModule();
        return super.getSize();
    }

    @Override
    public DataElement doGet(String name) throws Exception
    {
        initXmlModule();
        return super.doGet(name);
    }

    @Override
    public @Nonnull Iterator iterator()
    {
        initXmlModule();
        return super.iterator();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        initXmlModule();
        return super.getNameList();
    }

    @Override
    public List<CollectionDescription> getExternalTypes()
    {
        initXmlModule();
        return super.getExternalTypes();
    }

    ////////////////////////////////////////////////////////////////////////////

    // classes definitions
    public static class InternalType
    {
        protected String section;
        protected String name;
        protected String typeClass;
        protected String typeTransformer;
        protected String idFormat;
        protected String querySystemClass;
        protected String luceneIndexes;
        protected List<IndexDescription> indexes = new ArrayList<>();

        public InternalType()
        {
            name = "";
            section = "";
            typeClass = "";
            typeTransformer = "";
            idFormat = "";
            querySystemClass = "";
            luceneIndexes = "";
        }

        public InternalType(String name, String section, String typeClass, String typeTransformer, String idFormat,
                String querySystemClass, String luceneIndexes)
        {
            this.name = name;
            this.section = section;
            this.typeClass = typeClass;
            this.typeTransformer = typeTransformer;
            this.idFormat = idFormat;
            this.querySystemClass = querySystemClass;
            this.luceneIndexes = luceneIndexes;
        }

        @Override
        public InternalType clone()
        {
            InternalType result = new InternalType();
            result.setName(name);
            result.setSection(section);
            result.setTypeClass(typeClass);
            result.setTypeTransformer(typeTransformer);
            result.setIdFormat(idFormat);
            result.setQuerySystemClass(querySystemClass);
            result.setLuceneIndexes(luceneIndexes);

            List<IndexDescription> newIndexes = new ArrayList<>();
            for( IndexDescription id : indexes )
            {
                newIndexes.add(id.clone());
            }
            result.setIndexes(newIndexes);

            return result;
        }
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }
        public String getSection()
        {
            return section;
        }
        public void setSection(String section)
        {
            this.section = section;
        }
        public String getTypeClass()
        {
            return typeClass;
        }
        public void setTypeClass(String typeClass)
        {
            this.typeClass = typeClass;
        }
        public String getTypeTransformer()
        {
            return typeTransformer;
        }
        public void setTypeTransformer(String typeTransformer)
        {
            this.typeTransformer = typeTransformer;
        }
        public String getIdFormat()
        {
            return idFormat;
        }
        public void setIdFormat(String idFormat)
        {
            this.idFormat = idFormat;
        }
        public String getLuceneIndexes()
        {
            return luceneIndexes;
        }
        public void setLuceneIndexes(String luceneIndexes)
        {
            this.luceneIndexes = luceneIndexes;
        }
        public String getQuerySystemClass()
        {
            return querySystemClass;
        }
        public void setQuerySystemClass(String querySystemClass)
        {
            this.querySystemClass = querySystemClass;
        }
        public List<IndexDescription> getIndexes()
        {
            return indexes;
        }
        public void setIndexes(List<IndexDescription> indexes)
        {
            this.indexes = indexes;
        }

        public static class IndexDescription
        {
            protected String name;
            protected String indexClass;
            protected String table;

            public IndexDescription()
            {
            }

            public IndexDescription(String name, String indexClass)
            {
                this.name = name;
                this.indexClass = indexClass;
            }

            @Override
            public IndexDescription clone()
            {
                IndexDescription result = new IndexDescription();
                result.setName(name);
                result.setIndexClass(indexClass);
                result.setTable(table);

                return result;
            }

            public String getIndexClass()
            {
                return indexClass;
            }
            public void setIndexClass(String indexClass)
            {
                this.indexClass = indexClass;
            }
            public String getName()
            {
                return name;
            }
            public void setName(String name)
            {
                this.name = name;
            }
            public String getTable()
            {
                return table;
            }
            public void setTable(String table)
            {
                this.table = table;
            }
        }
        public static class IndexDescriptionBeanInfo extends BeanInfoEx
        {
            public IndexDescriptionBeanInfo()
            {
                super(IndexDescription.class, null);
            }

            @Override
            public void initProperties() throws Exception
            {
                super.initProperties();

                property( "name" ).titleRaw( "Name" ).descriptionRaw( "Field name" );
                property( "indexClass" ).titleRaw( "Class" ).descriptionRaw( "Index class" );
                property( "table" ).titleRaw( "Table" ).descriptionRaw( "DB table" );
            }
        }
    }

    public static class InternalTypeBeanInfo extends BeanInfoEx
    {
        public InternalTypeBeanInfo()
        {
            super(InternalType.class, null);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();

            property( "name" ).titleRaw( "Name" ).descriptionRaw( "Collection name" ).add();
            property( "section" ).titleRaw( "Section" ).descriptionRaw( "Section" ).add();
            property( "typeClass" ).titleRaw( "Class" ).descriptionRaw( "Collection elements type" ).add();
            property( "typeTransformer" ).titleRaw( "Transformer" ).descriptionRaw( "Transformer" ).add();
            property( "idFormat" ).titleRaw( "ID format" ).descriptionRaw( "ID format" ).add();
            property( "querySystemClass" ).titleRaw( "Query system" ).descriptionRaw( "Query system class" ).add();
            property( "luceneIndexes" ).titleRaw( "Indexes" ).descriptionRaw( "Indexes" ).add();
            property( "indexes" ).editor( IndexPropertyEditor.class ).titleRaw( "Index list" ).descriptionRaw( "Index list" ).add();
        }
    }

    public static class DiagramTypeDescription
    {
        protected String name;
        protected String type;
        protected String className;
        protected String path;

        @Override
        public DiagramTypeDescription clone()
        {
            DiagramTypeDescription result = new DiagramTypeDescription();
            result.setName(name);
            result.setType(type);
            result.setClassName(className);
            result.setPath(path);

            return result;
        }

        public String getClassName()
        {
            return className;
        }
        public void setClassName(String className)
        {
            this.className = className;
        }
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }
        public String getPath()
        {
            return path;
        }
        public void setPath(String path)
        {
            this.path = path;
        }
        public String getType()
        {
            return type;
        }
        public void setType(String type)
        {
            this.type = type;
        }
    }

    public static class DiagramTypeDescriptionBeanInfo extends BeanInfoEx
    {
        public DiagramTypeDescriptionBeanInfo()
        {
            super(DiagramTypeDescription.class, null);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();

            property( "name" ).titleRaw( "Name" ).descriptionRaw( "Diagram type name" ).add();

            property( "type" ).editor( TypeEditor.class ).titleRaw( "Type" ).descriptionRaw( "Type" ).add();

            property( "className" ).titleRaw( "Class name" ).descriptionRaw( "Name of class for Java types" ).add();

            property( "path" ).editor( PathEditor.class ).titleRaw( "Path" ).descriptionRaw( "Path for XML types" ).add();
        }

        public static class TypeEditor extends StringTagEditorSupport
        {
            public TypeEditor()
            {
                super(MessageBundle.class.getName(), MessageBundle.class, "COLLECTIONDESCRIPTION_TYPE_STATES");
            }
        }

        public static class PathEditor extends StringTagEditorSupport
        {
            public PathEditor()
            {
                super(getNames());
            }

            private static final LazyValue<String[]> names = new LazyValue<>( "XML diagram types",
                    () -> StreamEx.of( XmlDiagramType.getTypesCollection().names() ).prepend( "" ).toArray( String[]::new ) );

            public static String[] getNames()
            {
                return names.get().clone();
            }
        }
    }

    public static List<DiagramTypeDescription> getDefaultDiagramTypes()
    {
        List<DiagramTypeDescription> result = new ArrayList<>();

        DiagramTypeDescription dt1 = new DiagramTypeDescription();
        dt1.setName("SemanticNetworkDiagramType");
        dt1.setType(XmlModuleConstants.GRAPHIC_NOTATION_TYPE_JAVA);
        dt1.setClassName("biouml.standard.diagram.SemanticNetworkDiagramType");
        result.add(dt1);

        DiagramTypeDescription dt2 = new DiagramTypeDescription();
        dt2.setName("PathwayDiagramType");
        dt2.setType(XmlModuleConstants.GRAPHIC_NOTATION_TYPE_JAVA);
        dt2.setClassName("biouml.standard.diagram.PathwayDiagramType");
        result.add(dt2);

        DiagramTypeDescription dt3 = new DiagramTypeDescription();
        dt3.setName("PathwaySimulationDiagramType");
        dt3.setType(XmlModuleConstants.GRAPHIC_NOTATION_TYPE_JAVA);
        dt3.setClassName("biouml.standard.diagram.PathwaySimulationDiagramType");
        result.add(dt3);

        DiagramTypeDescription dt4 = new DiagramTypeDescription();
        dt4.setName("CompositeDiagramType");
        dt4.setType(XmlModuleConstants.GRAPHIC_NOTATION_TYPE_JAVA);
        dt4.setClassName("biouml.standard.diagram.CompositeDiagramType");
        result.add(dt4);

        return result;
    }

    public static List<InternalType> getDefaultInternalTypes()
    {
        List<InternalType> result = new ArrayList<>();

        InternalType data1 = new InternalType("cell", "Data", "biouml.standard.type.Cell",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "CEL0000", "ru.biosoft.access.DefaultQuerySystem", "");
        data1.getIndexes().add(new InternalType.IndexDescription("title", "biouml.standard.type.access.TitleIndex"));
        result.add(data1);
        InternalType data2 = new InternalType("compartment", "Data", "biouml.standard.type.Compartment",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "CMP0000", "ru.biosoft.access.DefaultQuerySystem", "");
        data2.getIndexes().add(new InternalType.IndexDescription("title", "biouml.standard.type.access.TitleIndex"));
        result.add(data2);
        InternalType data3 = new InternalType("concept", "Data", "biouml.standard.type.Concept",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "CPT000000", "ru.biosoft.access.DefaultQuerySystem", "");
        data3.getIndexes().add(new InternalType.IndexDescription("title", "biouml.standard.type.access.TitleIndex"));
        result.add(data3);
        InternalType data5 = new InternalType("gene", "Data", "biouml.standard.type.Gene",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "GEN000000", "ru.biosoft.access.DefaultQuerySystem", "");
        data5.getIndexes().add(new InternalType.IndexDescription("title", "biouml.standard.type.access.TitleIndex"));
        result.add(data5);
        InternalType data6 = new InternalType("literature", "Data", "biouml.standard.type.Publication",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "LIT000000", "ru.biosoft.access.DefaultQuerySystem", "");
        data6.getIndexes().add(new InternalType.IndexDescription("title", "biouml.standard.type.access.TitleIndex"));
        result.add(data6);
        InternalType data7 = new InternalType("protein", "Data", "biouml.standard.type.Protein",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "PRT000000", "ru.biosoft.access.DefaultQuerySystem", "");
        data7.getIndexes().add(new InternalType.IndexDescription("title", "biouml.standard.type.access.TitleIndex"));
        result.add(data7);
        InternalType data8 = new InternalType("reaction", "Data", "biouml.standard.type.Reaction",
                "biouml.standard.type.access.ReactionTransformer", "RCT000000", "ru.biosoft.access.DefaultQuerySystem", "");
        data8.getIndexes().add(new InternalType.IndexDescription("title", "biouml.standard.type.access.TitleIndex"));
        result.add(data8);
        InternalType data9 = new InternalType("relation", "Data", "biouml.standard.type.SemanticRelation",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "RLT000000", "ru.biosoft.access.DefaultQuerySystem", "");
        data9.getIndexes().add(new InternalType.IndexDescription("title", "biouml.standard.type.access.TitleIndex"));
        result.add(data9);
        InternalType data10 = new InternalType("rna", "Data", "biouml.standard.type.RNA",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "RNA000000", "ru.biosoft.access.DefaultQuerySystem", "");
        data10.getIndexes().add(new InternalType.IndexDescription("title", "biouml.standard.type.access.TitleIndex"));
        result.add(data10);
        InternalType data11 = new InternalType("substance", "Data", "biouml.standard.type.Substance",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "SBS000000", "ru.biosoft.access.DefaultQuerySystem", "");
        data11.getIndexes().add(new InternalType.IndexDescription("title", "biouml.standard.type.access.TitleIndex"));
        result.add(data11);

        InternalType dict1 = new InternalType("database info", "Dictionaries", "biouml.standard.type.DatabaseInfo",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "", "", "");
        result.add(dict1);
        InternalType dict2 = new InternalType("relation type", "Dictionaries", "biouml.standard.type.RelationType",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "", "", "");
        result.add(dict2);
        InternalType dict3 = new InternalType("species", "Dictionaries", "biouml.standard.type.Species",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "", "", "");
        result.add(dict3);
        InternalType dict4 = new InternalType("unit", "Dictionaries", "biouml.standard.type.Unit",
                "ru.biosoft.access.support.BeanInfoEntryTransformer", "", "", "");
        result.add(dict4);

        return result;
    }
}
