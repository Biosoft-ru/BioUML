package biouml.model.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import org.w3c.dom.Element;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graph.Layouter;
import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.util.DiagramXmlReader;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Base;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.editors.StringTagEditor;
import com.developmentontheedge.application.Application;

/**
 * Diagram type that formally defines graphic notation via XML file.
 */
public class XmlDiagramType extends DiagramTypeSupport implements Base
{
    public static final String DEFAULT_NOTATION_PATH = "databases/Utils/Diagrams/graphic notations";

    public static final String NOTATION_PATH_PROPERTY = "NotationPath";

    /**
     * Used for Java scripts processing by xmlDiagramViewBuilder and xmlDiagramSemanticController
     */
    static final String[] IMPORT_PACKAGES = new String[] {"ru.biosoft.graphics", "ru.biosoft.graphics.font", "ru.biosoft.graph",
            "java.awt", "java.util", "java.lang", "java.awt.geom", "biouml.model.xml", "biouml.standard.type", "biouml.model",
            "biouml.model.dynamics"};

    protected String name;
    protected String title;
    protected String requiredPlugins;
    protected String description = null;
    private Class<? extends Layouter> pathLayouter = null;

    private DataCollection<?> origin;

    public XmlDiagramType()
    {

    }

    public XmlDiagramType(DataCollection<?> origin, String name)
    {
        this.origin = origin;
        this.name = name;
    }

    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram(origin, diagramName, kernel);
        PathwaySimulationDiagramType.DiagramPropertyChangeListener listener = new PathwaySimulationDiagramType.DiagramPropertyChangeListener(diagram);
        diagram.getViewOptions().addPropertyChangeListener(listener);
        SemanticController semanticController = getSemanticController();
        if(getPathLayouter() != null)
        {
            diagram.setPathLayouter( getPathLayouter().getConstructor().newInstance() );
            diagram.getViewOptions().setAutoLayout( true );
        }
        if( semanticController instanceof XmlDiagramSemanticController )
        {
            return (Diagram) ( (XmlDiagramSemanticController)semanticController ).getPrototype().validate(null, diagram);
        }
        return diagram;
    }

    @Override
    public String getType()
    {
        return Type.TYPE_XML_NOTATION;
    }

    @Override
    public DynamicPropertySet getAttributes()
    {
        return null;
    }

    public Class<? extends Layouter> getPathLayouter()
    {
        return pathLayouter;
    }

    public void setPathLayouter(Class<? extends Layouter> pathLayouter)
    {
        this.pathLayouter = pathLayouter;
    }

    @Override
    public String getTitle()
    {
        if( title == null )
            return name;
        return title;
    }

    @Override
    public String toString()
    {
        return getTitle();
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @Override
    public String getDescription()
    {
        if( description == null )
            return "XML Diagram ("+getTitle()+")";
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        return origin;
    }

    protected Map<String, DynamicPropertySet> typeMap = new HashMap<>();
    protected Map<String, Class<?>> kernelTypeMap = new HashMap<>();
    protected Map<String, String> idFormatMap = new HashMap<>();
    protected Map<String, Class<?>> beanMap = new HashMap<>();

    public void addType(String type, DynamicPropertySet dps, Class<?> kernelType)
    {
        typeMap.put(type, dps);
        putKernelType(type, kernelType);
    }

    public void putKernelType(String type, Class<?> kernelType)
    {
        if( kernelType != null )
        {
            kernelTypeMap.put(type, kernelType);
        }
    }

    public void addPropertiesBeanClass(String type, Class<?> beanClass)
    {
        beanMap.put(type, beanClass);
    }

    public Object getPropertiesBean(String type)
    {
        try
        {
            Class<?> clazz = beanMap.get(type);
            return clazz == null ? null : clazz.newInstance();
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    public DynamicPropertySet getType(String type)
    {
        return typeMap.get(type);
    }

    public void removeType(String type)
    {
        typeMap.remove(type);
    }

    ///////////////////////////////////////////////////////////////////
    // DiagramType interface methods
    //

    protected String[] nodeTypes = new String[0];

    /**
     * This method is used to generate buttons at the top of diagram. Combines nodes and reactions
     */
    @Override
    public @Nonnull Object[] getNodeTypes()
    {
        return StreamEx.of(nodeTypes).append( reactionTypes ).map( this::getKernelType ).toArray();
    }

    public void setNodeTypes(String[] nodeTypes)
    {
        this.nodeTypes = nodeTypes;
    }

    /**
     * Returns type for object
     */
    public Object getKernelType(String typeName)
    {
        if( kernelTypeMap.containsKey(typeName) )
        {
            return kernelTypeMap.get(typeName);
        }
        return typeName;
    }

    /**
     * Returns type name by type object
     */
    public String getKernelTypeName(Object type)
    {
        if( type instanceof String )
            return (String)type;

        return StreamEx.ofKeys( kernelTypeMap, cls -> cls.getName().equals( ( (Class<?>)type ).getName() ) )
            .findAny().orElse( null );
    }

    protected String defaultTypeName = null;

    /**
     * Get default type for elements
     */
    public String getDefaultTypeName()
    {
        return defaultTypeName;
    }

    /**
     * Set default type for elements
     */
    protected void setDefaultTypeName(String typeName)
    {
        this.defaultTypeName = typeName;
    }

    /*
     * DataCollectionConfigConstants.ID_FORMAT support methods
     */

    public String getIdFormat(String typeName)
    {
        return idFormatMap.get(typeName);
    }

    public void setIdFormat(String typeName, String idFormat)
    {
        if( idFormat == null )
        {
            idFormatMap.remove(typeName);
        }
        else
        {
            idFormatMap.put(typeName, idFormat);
        }
    }

    /**
     *  This method is used from DCNodesAdapter and other classes to get only nodes list (without reactions)
     */
    public String[] getNodes()
    {
        return nodeTypes;
    }

    protected String[] edgeTypes = new String[0];
    @Override
    public @Nonnull Object[] getEdgeTypes()
    {
        return StreamEx.of(edgeTypes).map( this::getKernelType ).toArray();
    }
    public String[] getEdges()
    {
        return edgeTypes;
    }
    public void setEdgeTypes(String[] edgeTypes)
    {
        this.edgeTypes = edgeTypes;
    }

    protected String[] reactionTypes = new String[0];
    public String[] getReactionTypes()
    {
        return reactionTypes;
    }
    public void setReactionTypes(String[] reactionTypes)
    {
        this.reactionTypes = reactionTypes;
    }

    protected XmlDiagramViewBuilder xmlDiagramViewBuilder;
    public XmlDiagramViewBuilder getXmlDiagramViewBuilder()
    {
        if( xmlDiagramViewBuilder == null )
            xmlDiagramViewBuilder = new XmlDiagramViewBuilder(this);
        return xmlDiagramViewBuilder;
    }
    public void setXmlDiagramViewBuilder(XmlDiagramViewBuilder diagramViewBuilder)
    {
        this.xmlDiagramViewBuilder = diagramViewBuilder;
        setDiagramViewBuilder(diagramViewBuilder);
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = getXmlDiagramViewBuilder();
        return diagramViewBuilder;
    }

    @Override
    public void setDiagramViewBuilder(DiagramViewBuilder viewBuilder)
    {
        super.setDiagramViewBuilder(viewBuilder);
        if(viewBuilder instanceof XmlDiagramViewBuilder)
            this.xmlDiagramViewBuilder = (XmlDiagramViewBuilder)viewBuilder;

    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new XmlDiagramSemanticController(this);

        return semanticController;
    }
    public void setSemanticController(SemanticController sc)
    {
        this.semanticController = sc;
    }

    private Map<String, Element> exampleElements;

    private Map<String, Diagram> examples;

    public void addExampleElement(String name, Element element)
    {
        if( exampleElements == null )
        {
            exampleElements = new HashMap<>();
        }
        exampleElements.put(name, element);
    }

    public void addExample(Diagram example)
    {
        if( examples == null )
        {
            examples = new HashMap<>();
        }
        examples.put(example.getName(), example);
    }
    public Set<String> getExampleNameList()
    {
        if( examples == null )
        {
            examples = new HashMap<>();
        }
        Set<String> nameList = new HashSet<>();
        nameList.addAll( examples.keySet() );

        if (exampleElements != null)
        {
            nameList.addAll( exampleElements.keySet() );
        }
        return nameList;
    }
    public Diagram getExample(String name)
    {
        if( examples == null )
        {
            examples = new HashMap<>();
        }

        if( examples.containsKey( name ) )
        {
            return examples.get( name );
        }
        else if( exampleElements != null && exampleElements.containsKey( name ) )
        {
            Element element = exampleElements.get( name );
            Diagram diagram = DiagramXmlReader.parseDiagram( name, element, null, this );
            examples.put( diagram.getName(), diagram );
            exampleElements.remove( name );
            return diagram;
        }

        return null;
    }
    public void removeExample(String name)
    {
        if( examples == null )
        {
            examples = new HashMap<>();
        }
        if( examples.containsKey( name ) )
        {
            examples.remove( name );
        }
        else if( exampleElements.containsKey( name ) )
        {
            exampleElements.remove( name );
        }
    }

    private final List<String> compartmentList = new ArrayList<>();
    public boolean checkCompartment(String type)
    {
        return compartmentList.contains(type);
    }
    public void addCompartment(String type)
    {
        compartmentList.add(type);
    }
    public void removeCompartment(String type)
    {
        compartmentList.remove(type);
    }

    private final List<String> needLayoutList = new ArrayList<>();
    public void addNeedLayout(String type)
    {
        needLayoutList.add(type);
    }
    public boolean checkNeedLayout(String type)
    {
        return needLayoutList.contains(type);
    }
    public void removeNeedLayout(String type)
    {
        needLayoutList.remove(type);
    }
    private final List<String> createByPrototype = new ArrayList<>();
    public void addCreateByPrototype(String type)
    {
        createByPrototype.add(type);
    }
    public boolean isCreateByPrototype(String type)
    {
        return createByPrototype.contains(type);
    }

    //utility functions
    public void fillPropertyEditorParameters(DynamicPropertySet dps)
    {
        Iterator<String> iter = dps.nameIterator();
        while( iter.hasNext() )
        {
            String pName = iter.next();
            DynamicProperty templateProperty = this.properties.getProperty(pName);
            if( templateProperty != null )
            {
                Object tags = templateProperty.getDescriptor().getValue(StringTagEditor.TAGS_KEY);
                if( tags instanceof String[] )
                {
                    DynamicProperty property = dps.getProperty(pName);
                    property.getDescriptor().setValue(StringTagEditor.TAGS_KEY, tags);
                    property.getDescriptor().setPropertyEditorClass(TestTagEditor.class);
                }
            }
        }
    }

    public static class TestTagEditor extends com.developmentontheedge.beans.editors.StringTagEditor
    {
        public TestTagEditor()
        {
            super();
        }
    }

    public String getRequiredPlugins()
    {
        return requiredPlugins;
    }

    public void setRequiredPlugins(String requiredPlugins)
    {
        this.requiredPlugins = requiredPlugins;
    }

    /**
     *
     * TODO: get needLayout info from notation XML file
     */
    @Override
    public boolean needLayout(Node node)
    {
        DynamicPropertySet dps = node.getAttributes();
        String type;
        if( dps != null && dps.getValue("xmlElementType") != null)
            type = dps.getValue("xmlElementType").toString();
        else
            type = node.getKernel().getType();
        if(checkNeedLayout(type))
            return true;

        return false;
    }

    @Override
    public boolean needAutoLayout(Edge edge)
    {
        return true;
    }

    /**
     * Returns default XmlDiagramType object for given xml notation
     * @param xml xml notation name (e.g. "sbgn_simulation.xml")
     * @return XmlDiagramType object if such notation exists, null otherwise
     */
    public static XmlDiagramType getTypeObject(String xml)
    {
        try
        {
            return getTypesCollection().get(xml);
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    /**
     * Returns default collection of XmlDiagramType's or empty collection
     */
    public static @Nonnull DataCollection<XmlDiagramType> getTypesCollection()
    {
        try
        {
            return DataElementPath.create( Application.getGlobalValue( NOTATION_PATH_PROPERTY, DEFAULT_NOTATION_PATH ) )
                    .getDataCollection( XmlDiagramType.class );
        }
        catch( Exception ex )
        {
            return new VectorDataCollection<>( "" );
        }
    }

    @Override
    public boolean isCompartment(Base kernel)
    {
        String typeStr = getKernelTypeName( kernel.getClass() );
        return super.isCompartment( kernel ) || checkCompartment( typeStr );
    }
}
