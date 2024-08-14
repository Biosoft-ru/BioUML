package biouml.plugins.obo.access;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.Entry;
import ru.biosoft.access.EntryCollection;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.Layouter;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.plugins.obo.OboDiagramType;
import biouml.plugins.obo.OboQuerySystem;
import biouml.plugins.obo.OboTransformer;
import biouml.standard.diagram.FormulaDescription;
import biouml.standard.type.Concept;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.SemanticRelation;
import biouml.workbench.graph.DiagramToGraphTransformer;

public class OboDataCollection extends VectorDataCollection<DataCollection<?>>
{
    protected DataCollection<DataCollection> data;
    protected DataCollection<Diagram> diagrams;

    protected DataCollection<Concept> typedef;
    protected DataCollection<Concept> terms;
    protected DataCollection<Concept> instances;

    protected DataCollection<SemanticRelation> relations;

    protected FileEntryCollection2 primaryCollection;
    protected OboTransformer transformer;

    protected Diagram diagram;

    protected String fileName;
    protected String filePath;
    protected String layouterClass;

    public OboDataCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
        this.filePath = properties.getProperty("filePath");
        this.fileName = properties.getProperty("file");
        this.layouterClass = properties.getProperty("layouter");
    }


    ///////////////////////////////////////////////////////////////////
    // Initilisation issues
    //

    protected boolean isInit = false;

    public void init()
    {
        this.init( true );
    }

    /**
     * Get the {@link edu.caltech.sbw.Module} instance from SBW broker
     * and load its services.
     */
    public void init(boolean layout)
    {
        if( isInit )
            return;

        isInit = true;

        this.setNotificationEnabled(false);
        try
        {
            data = new VectorDataCollection<>(Module.DATA, DataCollection.class, this);
            Properties propsDiagram = new Properties();
            propsDiagram.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Diagram.class.getName());
            diagrams = new VectorDataCollection<>(Module.DIAGRAM, Diagram.class, this);
            typedef = new VectorDataCollection<>("typedef", Concept.class, data);
            typedef.getInfo().setQuerySystem(new OboQuerySystem(typedef));
            terms = new VectorDataCollection<>("terms", Concept.class, data);
            terms.getInfo().setQuerySystem(new OboQuerySystem(terms));
            instances = new VectorDataCollection<>("instances", Concept.class, data);
            instances.getInfo().setQuerySystem(new OboQuerySystem(instances));
            relations = new VectorDataCollection<>("relations", SemanticRelation.class, data);
            relations.getInfo().setQuerySystem(new OboQuerySystem(relations));

            doPut(data, true);
            doPut(diagrams, true);
            data.put(typedef);
            data.put(terms);
            data.put(instances);
            data.put(relations);

            Properties properties = new Properties();
            properties.setProperty(EntryCollection.ENTRY_START_PROPERTY, "[");
            properties.setProperty(EntryCollection.ENTRY_ID_PROPERTY, "id:");
            properties.setProperty(EntryCollection.ENTRY_DELIMITERS_PROPERTY, " ");
            properties.setProperty(EntryCollection.ENTRY_END_PROPERTY, "\n");
            properties.setProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, filePath);
            properties.setProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, filePath);
            properties.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, fileName);
            primaryCollection = new FileEntryCollection2(null, properties);

            DiagramType diagramType = new OboDiagramType();
            DiagramInfo diagramInfo = new DiagramInfo("Main diagram");
            diagramInfo.setTitle("Main diagram");
            diagram = new Diagram(diagrams, diagramInfo, diagramType);
            diagram.setNotificationEnabled(false);

            transformer = new OboTransformer();
            transformer.init(primaryCollection, (DataCollection)this);
            for(Entry entry : primaryCollection)
            {
                Concept de = transformer.transformInput(entry);
                if( de != null )
                {
                    String type = (String)de.getAttributes().getValue("Type");
                    if( type != null )
                    {
                        if( type.equals("Term") )
                        {
                            terms.put(de);
                            Node node = new Node(diagram, de);
                            node.setTitle(de.getTitle());
                            diagram.put(node);
                        }
                        else if( type.equals("Typedef") )
                        {
                            typedef.put(de);
                        }
                        else if( type.equals("Instance") )
                        {
                            instances.put(de);
                            Node node = new Node(diagram, de);
                            node.setTitle(de.getTitle());
                            diagram.put(node);
                        }
                    }
                }
            }

            for(Concept concept : terms)
            {
                createRelation(concept, concept.getAttributes().getValue("is_a"), SemanticRelation.PARTICIPATION_DIRECT, "is-a");
                createRelation(concept, concept.getAttributes().getValue("intersection_of"), SemanticRelation.PARTICIPATION_INDIRECT,
                        "intersection_of");
                createRelation(concept, concept.getAttributes().getValue("union_of"), SemanticRelation.PARTICIPATION_INDIRECT, "union_of");
                createRelation(concept, concept.getAttributes().getValue("disjoint_from"), SemanticRelation.PARTICIPATION_INDIRECT,
                        "disjoint_from");
                createRelation(concept, concept.getAttributes().getValue("relationship"), SemanticRelation.PARTICIPATION_INDIRECT,
                        "relationship");
            }

            for( Concept concept : instances )
            {
                createRelation( concept, concept.getAttributes().getValue( "instance_of " ), SemanticRelation.PARTICIPATION_DIRECT,
                        "instance_of" );
            }

            diagram.setNotificationEnabled(true);

            if( layout )
            {
                if( layouterClass != null )
                {
                    Layouter layouter = ClassLoading.loadSubClass( layouterClass, Layouter.class ).newInstance();
                    DiagramToGraphTransformer.layout( diagram, layouter );
                }
                else if( diagram.getSize() < 1000 )
                {
                    DiagramToGraphTransformer.layout( diagram, new HierarchicLayouter() );
                }
                else
                {
                    diagram.setPathLayouter( null );
                }
            }
            diagrams.put(diagram);
        }
        catch( Throwable t )
        {
            throw ExceptionRegistry.translateException( t );
        }

        this.setNotificationEnabled(true);
    }

    ////////////////////////////////////////////////////////////////////////////
    // redefine DataCollection methods due to Module instance lazy initialisation
    //

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public @Nonnull Class getDataElementType()
    {
        return ru.biosoft.access.core.DataCollection.class;
    }

    @Override
    public int getSize()
    {
        init();
        return super.getSize();
    }

    @Override
    protected DataCollection<?> doGet(String name)
    {
        init();
        return super.doGet(name);
    }

    @Override
    public @Nonnull Iterator<DataCollection<?>> iterator()
    {
        init();
        return super.iterator();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        init();
        return super.getNameList();
    }

    private void createRelation(Concept concept, Object relations, String type, String label) throws Exception
    {
        if( relations != null )
        {
            if( relations.getClass().isArray() )
            {
                String[] parents = (String[])relations;
                for( String parent : parents )
                {
                    createEdge(concept.getOrigin().getName(), concept.getName(), "terms", parent, type, label);
                }
            }
            else
            {
                createEdge(concept.getOrigin().getName(), concept.getName(), "terms", (String)relations, type, label);
            }
        }
    }
    private void createEdge(String inputType, String inputName, String outputType, String outputName, String type, String label)
            throws Exception
    {
        String output = outputName.substring(outputName.indexOf(' ') + 1);
        SemanticRelation sr = new SemanticRelation(relations, inputName + "->" + output);
        sr.setParticipation(type);
        sr.setRelationType(label);
        sr.setInputElementName(inputType + "/" + inputName);
        sr.setOutputElementName(outputType + "/" + output);
        Node inputNode = (Node)diagram.get(inputName);
        Node outputNode = (Node)diagram.get(output);
        boolean failed = false;
        if( inputNode == null )
        {
            log.log( Level.WARNING, "Can not find node '" + inputName + "' on diagram." );
            failed = true;
        }
        if( outputNode == null )
        {
            log.log( Level.WARNING, "Can not find node '" + output + "' on diagram." );
            failed = true;
        }
        if( failed )
            return;
        diagram.put(new Edge(diagram, sr, inputNode, outputNode));
        relations.put(sr);
    }

    public List<FormulaDescription> getFormulas(String parentId)
    {
        init( false );
        List<FormulaDescription> result = new ArrayList<>();
        terms.stream().filter(c->isChildOf(c, parentId)).forEach(concept->
        {
            Object math = concept.getAttributes().getValue(OboTransformer.MATH_PROPERTY_NAME);
            if( math != null )
                result.add(new FormulaDescription(concept.getCompleteName(), concept.getDescription(), (String)math));
        });
        return result;
    }

    private boolean isChildOf(Concept concept, String parentId)
    {
        if( concept.getName().equals(parentId) )
            return true;
        Object parents = concept.getAttributes().getValue(OboTransformer.PARENT_PROPERTY);
        if( parents == null )
            return false;

        try
        {
            if( parents instanceof String[] )
            {
                for( String parent : (String[])parents )
                {
                    if( isChildOf(terms.get(parent), parentId) )
                        return true;
                }
                return false;
            }
            else if( parents instanceof String )
                return isChildOf(terms.get((String)parents), parentId);
        }
        catch( Exception e )
        {
        }
        return false;
    }

    public String getTermDescription(String termId)
    {
        try
        {
            return terms.get(termId).getCompleteName();
        }
        catch( Exception e )
        {
            //do nothing, just return null
        }
        return null;
    }
}