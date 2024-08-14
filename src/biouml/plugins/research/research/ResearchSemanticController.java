package biouml.plugins.research.research;

import java.awt.Dimension;
import java.awt.Point;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.ImportAnalysisParameters;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.util.DPSUtils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.plugins.research.BaseResearchSemanticController;
import biouml.standard.diagram.CreateEdgeDialog;
import biouml.standard.diagram.SimpleEdgePane;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * Semantic controller for research diagrams
 */
public class ResearchSemanticController extends BaseResearchSemanticController
{
    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        DiagramElementGroup result = super.createInstance( parent, type, point, viewEditor );
        if(result != null) return result;

        String typeString = (String)type;

        if( Type.TYPE_DATA_ELEMENT.equals(typeString) )
        {
            DataElementPathDialog dialog = new DataElementPathDialog("Select data element");
            DataElementPath dePath = null;
            dialog.setValue(dePath);
            if( dialog.doModal() )
            {
                dePath = dialog.getValue();
            }
            if( dePath != null )
            {
                try
                {
                    if(getDataElementNode(parent, dePath) == null)
                        return new DiagramElementGroup( createDataElementNode( parent, dePath ) );
                }
                catch( Exception e )
                {
                }
            }
        }

        if( Base.TYPE_DIRECTED_LINK.equals(typeString) || Base.TYPE_UNDIRECTED_LINK.equals(typeString) )
        {
            String name = generateUniqueNodeName(parent, typeString);
            CreateEdgeDialog dialog = new CreateEdgeDialog(point, "New edge", new SimpleEdgePane(Module.getModule(parent),
                    viewEditor, name, typeString, null));
            dialog.setVisible(true);
            return null;
        }
        return null;
    }
    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        if(super.canAccept(compartment, de)) return true;
        String type = de.getKernel().getType();
        if( compartment instanceof Diagram )
        {
            if( Base.TYPE_DIRECTED_LINK.equals(type) || Base.TYPE_UNDIRECTED_LINK.equals(type) )
            {
                if(de instanceof Edge && ( (Edge)de ).getInput() == ( (Edge)de ).getOutput()) return false;
                return true;
            }
            if(Type.TYPE_DATA_ELEMENT.equals(type) )
            {
                try
                {
                    // Check whether such element is already exists
                    String path = de.getAttributes().getProperty(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY).toString();
                    return getDataElementNode(compartment, DataElementPath.create(path)) == null;
                }
                catch(Exception e)
                {
                    log.log(Level.SEVERE, e.getMessage(), e);
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Create node for data element
     */
    @Override
    public @Nonnull Node createDataElementNode(Compartment parent, DataElementPath dePath)
    {
        String name = dePath.getName().replace(".", "_");
        if( parent.contains(name) )
        {
            int i = 2;
            while( parent.contains(name + "(" + i + ")") )
            {
                i++;
            }
            name = name + "(" + i + ")";
        }
        Stub kernel = new Stub(null, name, Type.TYPE_DATA_ELEMENT);
        Node node = new Node(parent, kernel);
        try
        {
            node.getAttributes().add(new DynamicProperty(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY, String.class, dePath.toString()));
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not set attributes for node", e);
        }
        return node;
    }

    @Override
    public Node getDataElementNode(Compartment compartment, DataElementPath path) throws Exception
    {
        for(Node node: compartment.getNodes())
        {
            if(Type.TYPE_DATA_ELEMENT.equals(node.getKernel().getType()) && node.getAttributes().getProperty(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY).getValue().toString().equals(path.toString()))
            {
                return node;
            }
        }
        return null;
    }

    /**
     * Create node for analysis element
     */
    @Override
    public @Nonnull Compartment createAnalysisNode(Compartment parent, String analysisName, DynamicPropertySet attributes) throws Exception
    {
        AnalysisMethod analysisMethod = AnalysisMethodRegistry.getAnalysisMethod(analysisName);
        String name = analysisMethod.getName();
        int ind = name.lastIndexOf('/');
        if( ind != -1 )
        {
            name = name.substring(ind + 1);
        }
        name = name.replaceFirst("\\s*\\(.*\\..*\\)$", "");
        if( parent.contains(name) )
        {
            int i = 2;
            while( parent.contains(name + "(" + i + ")") )
            {
                i++;
            }
            name = name + "(" + i + ")";
        }
        Stub kernel = new Stub(null, name, Type.ANALYSIS_METHOD);
        Compartment anNode = new Compartment(parent, kernel);
        anNode.setNotificationEnabled(false);
        anNode.setShapeSize(new Dimension(150, 70));
        anNode.setNotificationEnabled(true);
        return anNode;
    }
    @Override
    public DiagramElement[] addTaskInfoItemsToDiagram(Compartment parent, Iterator<TaskInfo> iter, Point inPoint, ViewEditorPane viewPane) throws Exception
    {
        List<DiagramElement> createdElements = new ArrayList<>();
        while( iter.hasNext() )
        {
            TaskInfo action = iter.next();
            if( action.getType().equals(TaskInfo.ANALYSIS) || action.getType().equals(TaskInfo.IMPORT))
            {
                Compartment node;
                DynamicPropertySet attributes = action.getAttributes();
                if(action.getType().equals(TaskInfo.IMPORT))    // Convert import into analysis node
                {
                    String format = (String)action.getAttributes().getValue(TaskInfo.IMPORT_FORMAT_PROPERTY);
                    String output = (String)action.getAttributes().getValue(TaskInfo.IMPORT_OUTPUT_PROPERTY);
                    AnalysisMethod importer = AnalysisMethodRegistry.getAnalysisMethod(format);
                    ImportAnalysisParameters parameters = (ImportAnalysisParameters)importer.getParameters();
                    Object properties = parameters.getProperties();
                    if(properties != null)
                    {
                        DPSUtils.readBeanFromDPS(properties, action.getAttributes(), DPSUtils.PARAMETER_ANALYSIS_PARAMETER+".");
                    }
                    parameters.setResultPath(DataElementPath.create(output));
                    attributes = new DynamicPropertySetAsMap();
                    AnalysisDPSUtils.writeParametersToNodeAttributes(format, parameters, attributes);
                    node = createAnalysisNode(parent, format, attributes);
                } else
                {
                    node = createAnalysisNode(parent, action.getSource().getName(), action.getAttributes());
                }
                viewPane.add(node, inPoint);
                createdElements.add(node);
                AnalysisParameters parameters = AnalysisDPSUtils.readParametersFromAttributes(attributes);

                int i = 0;
                String[] inputNames = parameters.getInputNames();
                for( String pName : inputNames )
                {
                    DataElementPath dePath = getParameterDataElement( parameters, pName );

                    if( dePath != null )
                    {
                        Node inNode = getDataElementNode( parent, dePath );
                        if( inNode == null )
                        {
                            inNode = createDataElementNode( parent, dePath );
                            int pointX = ( inputNames.length == 1 ) ? inPoint.x
                                    : inPoint.x - 25 + (int) ( 200.0 * i / inputNames.length - 1 );
                            Point point = new Point( pointX, inPoint.y - 50 );
                            viewPane.add( inNode, point );
                        }
                        Edge edge = createDirectedLink( parent, inNode, node );
                        viewPane.add( edge, new Point( 0, 0 ) );
                    }
                    i++;
                }

                i = 0;
                String[] outputNames = parameters.getOutputNames();
                for( String pName : outputNames )
                {
                    DataElementPath dePath = getParameterDataElement( parameters, pName );

                    if( dePath != null )
                    {
                        Node outNode = getDataElementNode( parent, dePath );
                        if( outNode == null )
                        {
                            outNode = createDataElementNode( parent, dePath );
                            int pointX = ( outputNames.length == 1 ) ? inPoint.x
                                    : inPoint.x - 25 + (int) ( 200.0 * i / outputNames.length - 1 );
                            Point point = new Point( pointX, inPoint.y + 100 );
                            viewPane.add( outNode, point );
                        }
                        Edge edge = createDirectedLink( parent, node, outNode );
                        viewPane.add( edge, new Point( 0, 0 ) );
                    }
                    i++;
                }
            }
            else if( action.getType().equals(TaskInfo.SCRIPT) )
            {
                // TODO: support non-JS scripts
                Compartment node = createScriptNode(parent, action.getData(), "js");
                viewPane.add(node, inPoint);
                createdElements.add(node);
            }
            else if( action.getType().equals(TaskInfo.SCRIPT_DOCUMENT) )
            {
                Compartment node = createScriptNode(parent, action.getSource());
                viewPane.add(node, inPoint);
                createdElements.add(node);
            }
            else if( action.getType().equals(TaskInfo.SQL) )
            {
                String host = (String)action.getAttributes().getValue("host");
                Compartment node = createSQLNode(parent, action.getData(), host);
                viewPane.add(node, inPoint);
                createdElements.add(node);
            }
        }
        return createdElements.toArray(new DiagramElement[createdElements.size()]);
    }
}
