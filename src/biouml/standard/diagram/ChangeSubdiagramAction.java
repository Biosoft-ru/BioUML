package biouml.standard.diagram;

import java.util.List;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.util.bean.BeanInfoEx2;

@SuppressWarnings ( "serial" )
public class ChangeSubdiagramAction extends BackgroundDynamicAction
{
    @Override
    public boolean isApplicable(Object object)
    {
        return object instanceof Diagram && ( (Diagram)object ).getType().getSemanticController() instanceof CompositeSemanticController;
    }

    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        if( !isApplicable( selectedItems ) )
            return null;

        return new ChangeSubdiagramActionParameters( selectedItems );
    }

    boolean isApplicable(List<DataElement> selectedItems)
    {
        return selectedItems.size() == 1 && selectedItems.get( 0 ) instanceof SubDiagram;
    }

    @Override
    public JobControl getJobControl(final Object model, final List<DataElement> selectedItems, final Object properties) throws Exception
    {
        return new AbstractJobControl(log)
        {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                    if( !isApplicable(selectedItems) )
                        return;

                    ChangeSubdiagramActionParameters parameters = (ChangeSubdiagramActionParameters)properties;
                    SubDiagram subdiagram = (SubDiagram)selectedItems.get(0);
                    Diagram diagram = Diagram.getDiagram(subdiagram);
                    SemanticController controller = diagram.getType().getSemanticController();

                    DataElementPath path = parameters.getDataElementPath();
                   
                    SubDiagramProperties properties = new SubDiagramProperties( diagram );
                    properties.setDiagramPath( path );
                    properties.setExternal( true );
                    properties.setName( subdiagram.getName() );
                    DiagramElementGroup deg = controller.createInstance( subdiagram.getCompartment(), SubDiagram.class, subdiagram.getLocation(), properties );
                    SubDiagram newSubdiagram = (SubDiagram)deg.get( 0 );

                    newSubdiagram.setShapeSize( subdiagram.getShapeSize());
                    
                    for( Node node : subdiagram.getNodes() )
                    {
                        Node newNode = findPort( newSubdiagram, Util.getPortVariable( node ) );
                        if( newNode != null )
                        {
                            for( Edge e : node.getEdges() )
                                ChangeSubdiagramAction.swapNode( node, newNode, e );
                        }
                        else
                            for( Edge e : node.getEdges() )
                                diagram.remove( e.getName() );
                    }

                    diagram.getType().getSemanticController().remove( subdiagram );         
                    resultsAreReady( new Object[] {diagram} );
                }
                catch( Exception e )
                {
                    throw new JobControlException(e);
                }
            }
        };
    }
    
    private Node findPort(SubDiagram subdiagram, String varName)
    {
        return subdiagram.stream(Node.class).filter( n->Util.getPortVariable( n ).equals( varName ) ).findAny().orElse( null );        
    }

    /**
     * Redirects edge e from oldNode to newNode
     * @throws IllegalArgumentException if e is not connected to oldNode
     */
    public static void swapNode(@Nonnull Node oldNode, @Nonnull Node newNode, @Nonnull Edge e)
    {
        if( e.getInput() == oldNode )
        {
            oldNode.removeEdge( e );
            e.setInput( newNode );
            newNode.addEdge( e );
        }
        else if( e.getOutput() == oldNode )
        {
            oldNode.removeEdge( e );
            e.setOutput( newNode );
            newNode.addEdge( e );
        }
        else
            throw new IllegalArgumentException( e + ": Supplied node " + oldNode + " is not my input or output" );
    }

    public static class ChangeSubdiagramActionParameters
    {
        private DataElementPath path;

        @PropertyName ( "Subdiagram path" )
        @PropertyDescription ( "Subdiagram path." )
        public DataElementPath getDataElementPath()
        {
            return path;
        }
        public void setDataElementPath(DataElementPath path)
        {
            this.path = path;
        }

        public ChangeSubdiagramActionParameters(List<DataElement> des)
        {
        }
    }

    public static class ChangeSubdiagramActionParametersParametersBeanInfo extends BeanInfoEx2<ChangeSubdiagramActionParameters>
    {
        public ChangeSubdiagramActionParametersParametersBeanInfo()
        {
            super( ChangeSubdiagramActionParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "path" );
        }
    }
}
