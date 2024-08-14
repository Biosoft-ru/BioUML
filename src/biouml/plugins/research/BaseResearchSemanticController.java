package biouml.plugins.research;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.swing.JOptionPane;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.tasks.TaskInfo;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.workflow.engine.SQLElement;
import biouml.plugins.research.workflow.engine.ScriptElement;
import biouml.standard.diagram.CreateEdgeAction;
import biouml.standard.diagram.NoteLinkEdgeCreator;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.application.Application;

public abstract class BaseResearchSemanticController extends DefaultSemanticController
{
    private static final Dimension NULL_OFFSET = new Dimension(0,0);

    protected static final Logger log = Logger.getLogger( BaseResearchSemanticController.class.getName() );

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        String typeString = (String)type;

        if( Type.TYPE_NOTE.equals(typeString) )
        {
            String id = generateUniqueNodeName(parent, Type.TYPE_NOTE);
            return new DiagramElementGroup( new Node( parent, new Stub.Note( null, id ) ) );
        }

        if( Type.TYPE_NOTE_LINK.equals(typeString) )
        {
            new CreateEdgeAction().createEdge(point, viewEditor, new NoteLinkEdgeCreator());
            return DiagramElementGroup.EMPTY_EG;
        }

        if( Type.ANALYSIS_METHOD.equals(typeString) )
        {
            String[] anList = AnalysisMethodRegistry.getAnalysisNamesWithGroup().toArray(String[]::new);
            if( anList.length == 0 )
            {
                log.warning("No available analysis methods");
                return null;
            }
            Object analysisName = JOptionPane.showInputDialog(Application.getApplicationFrame(), "Select analysis", "Analysis",
                    JOptionPane.QUESTION_MESSAGE, null, anList, anList[0]);
            if( analysisName == null )
                return null;
            try
            {
                DynamicPropertySet attributes = new DynamicPropertySetAsMap();
                attributes.add(new DynamicProperty(AnalysisDPSUtils.PARAMETER_ANALYSIS_FULLNAME, String.class, analysisName));
                return new DiagramElementGroup( createAnalysisNode( parent, (String)analysisName, attributes ) );
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not find analysis: " + analysisName, e);
            }
        }
        return DiagramElementGroup.EMPTY_EG;
    }

    abstract public DiagramElement createAnalysisNode(Compartment parent, String analysisName, DynamicPropertySet attributes)
            throws Exception;

    /**
     * Create node for script element
     * @param scriptType type of the script ('js', 'R')
     */
    public Compartment createScriptNode(Compartment parent, String source, String scriptType) throws Exception
    {
        String id = generateUniqueNodeName(parent, Type.ANALYSIS_SCRIPT);
        Stub kernel = new Stub(null, id, Type.ANALYSIS_SCRIPT);
        Compartment anNode = new Compartment(parent, kernel);
        anNode.getAttributes().add(new DynamicProperty(ScriptElement.SCRIPT_SOURCE, String.class, source));
        anNode.getAttributes().add(new DynamicProperty(ScriptElement.SCRIPT_TYPE, String.class, scriptType ) );
        anNode.setNotificationEnabled(false);
        anNode.setShapeSize( new Dimension( 200, 70 ) );
        anNode.setNotificationEnabled(true);
        return anNode;
    }

    /**
     * Create node for script element
     */
    public Compartment createScriptNode(Compartment parent, DataElementPath sourcePath) throws Exception
    {
        String id = generateUniqueNodeName(parent, Type.ANALYSIS_SCRIPT);
        Stub kernel = new Stub(null, id, Type.ANALYSIS_SCRIPT);
        Compartment anNode = new Compartment(parent, kernel);
        anNode.getAttributes().add(new DynamicProperty(ScriptElement.SCRIPT_PATH, String.class, sourcePath.toString()));
        anNode.setNotificationEnabled(false);
        anNode.setShapeSize( new Dimension( 200, 20 ) );
        anNode.setNotificationEnabled(true);
        return anNode;
    }

    /**
     * Create node for SQL element
     */
    public Compartment createSQLNode(Compartment parent, String source, String host) throws Exception
    {
        String id = generateUniqueNodeName(parent, Type.ANALYSIS_QUERY);
        Stub kernel = new Stub(null, id, Type.ANALYSIS_QUERY);
        Compartment anNode = new Compartment(parent, kernel);
        anNode.getAttributes().add(new DynamicProperty(SQLElement.SQL_SOURCE, String.class, source));
        anNode.getAttributes().add(new DynamicProperty(SQLElement.SQL_HOST, String.class, host));
        anNode.setNotificationEnabled(false);
        anNode.setShapeSize(new Dimension(150, 70));
        anNode.setNotificationEnabled(true);
        return anNode;
    }

    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        String type = de.getKernel().getType();
        if(de instanceof Edge)
        {
            if( de.getKernel() instanceof Stub.NoteLink && ( (Edge)de ).nodes().anyMatch( n -> n.getKernel() instanceof Stub.Note ) )
                return true;
        }
        if( compartment instanceof Diagram || compartment.getKernel().getType().equals(Type.ANALYSIS_CYCLE) )
        {
            if( Type.ANALYSIS_METHOD.equals(type) || Type.ANALYSIS_SCRIPT.equals(type) || Type.ANALYSIS_QUERY.equals(type)
                    || Type.ANALYSIS_CYCLE.equals(type) || Type.TYPE_NOTE.equals(type) )
            {
                return true;
            }
        }
        return false;
    }

    protected abstract Node createDataElementNode(Compartment parent, DataElementPath dePath);

    protected abstract Node getDataElementNode(Compartment compartment, DataElementPath path) throws Exception;

    public abstract DiagramElement[] addTaskInfoItemsToDiagram(Compartment parent, Iterator<TaskInfo> iter, Point inPoint, ViewEditorPane viewPane)
            throws Exception;

    public DiagramElement addTaskInfoItemToDiagram(Compartment compartment, TaskInfo de, Point point, ViewEditorPane viewEditor)
            throws Exception
    {
        return addTaskInfoItemsToDiagram(compartment, Collections.singletonList(de).iterator(), point, viewEditor)[0];
    }

    @Override
    public DiagramElementGroup addInstanceFromElement(Compartment compartment, DataElement de, Point point, ViewEditorPane viewEditor)
            throws Exception
    {
        if( de instanceof TaskInfo )
        {
            return new DiagramElementGroup( addTaskInfoItemToDiagram( compartment, (TaskInfo)de, point, viewEditor ) );
        }
        else if( de instanceof AnalysisMethodInfo )
        {
            return new DiagramElementGroup( addAnalysis( compartment, de.getName(), point, viewEditor ) );
        }
        else if( de instanceof ScriptDataElement )
        {
            Compartment node = createScriptNode(compartment, DataElementPath.create(de));
            viewEditor.add(node, point);
            return new DiagramElementGroup( node );
        }
        else
        {
            Node node = getDataElementNode( compartment, de.getCompletePath() );
            if( node != null )
                throw new Exception("Such node is already present on diagram: " + node.getName());
            return new DiagramElementGroup( addDataElement( compartment, DataElementPath.create( de ), point, viewEditor ) );
        }
    }

    public Node addDataElement(Compartment parent, DataElementPath path, Point inPoint, ViewEditorPane viewPane) throws Exception
    {
        Node node = createDataElementNode(parent, path);
        if( canAccept(parent, node) )
        {
            viewPane.add(node, inPoint);
        }
        else
        {
            throw new Exception("Unable to add node");
        }
        return node;
    }

    public DiagramElement addAnalysis(Compartment parent, String analysisName, Point inPoint, ViewEditorPane viewPane) throws Exception
    {
        DynamicPropertySet attributes = new DynamicPropertySetAsMap();
        attributes.add(new DynamicProperty(AnalysisDPSUtils.PARAMETER_ANALYSIS_FULLNAME, String.class, analysisName));
        DiagramElement analysis = createAnalysisNode(parent, analysisName, attributes);
        viewPane.add(analysis, inPoint);
        return analysis;
    }

    public Edge createDirectedLink(Compartment parent, @Nonnull Node inNode, @Nonnull Node outNode)
    {
        String name = generateUniqueNodeName(parent, Base.TYPE_DIRECTED_LINK);
        Stub edgeStub = new Stub(null, name, Base.TYPE_DIRECTED_LINK);
        return new Edge(edgeStub, inNode, outNode);
    }

    protected static DataElementPath getParameterDataElement(AnalysisParameters parameters, String pName)
    {
        try
        {
            ComponentModel model = ComponentFactory.getModel(parameters);
            Object result = model.findProperty(pName).getValue();
            if( result != null )
            {
                if( result instanceof DataElementPath )
                {
                    return (DataElementPath)result;
                }
                else if( result instanceof DataElement )
                {
                    return DataElementPath.create((DataElement)result);
                }
                else if( result instanceof String )
                {
                    return DataElementPath.create((String)result);
                }
            }
        }
        catch( Exception e )
        {
            //log.log(Level.SEVERE, "Can not get parameter value: " + pName, e);
        }
        return null;
    }

    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        if(de.getParent() == newParent && offset.equals(NULL_OFFSET)) return new Dimension(NULL_OFFSET);
        boolean isAutoLayout = Diagram.getDiagram(de).getViewOptions().isAutoLayout();
        if( de instanceof Node )
        {
            for( Edge edge : Util.getEdges((Node)de) )
            {
                if( isAutoLayout )
                {
                    edge.setPath(null);
                    //Workaround for different node and view location bug in Web
                    //View shoud be recreated for both edge nodes
                    edge.nodes().forEach( n -> n.setView( null ) );
                }
                else
                {
                    recalculateEdgePath(edge);
                }
            }
        }

        return super.move(de, newParent, offset, oldBounds);
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        if( type.equals(Type.ANALYSIS_METHOD) )
        {
            return new AnalysisMethodRef();
        }
        if( type.equals(Type.TYPE_DATA_ELEMENT))
        {
            return new DataElementRef();
        }
        if( type.equals(Type.TYPE_NOTE) )
        {
            return new NoteProperties();
        }
        return super.getPropertiesByType(compartment, type, point);
    }
}
