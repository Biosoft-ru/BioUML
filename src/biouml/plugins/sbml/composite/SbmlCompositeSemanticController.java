package biouml.plugins.sbml.composite;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.annotation.Nonnull;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.ModelDefinition;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.MultipleConnection;
import biouml.model.dynamics.UndirectedConnection;
import biouml.plugins.sbml.SbmlEModel;
import biouml.plugins.sbml.SbmlSemanticController;
import biouml.standard.diagram.CompositeSemanticController;
import biouml.standard.diagram.CreateEdgeDialog;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.SubDiagramProperties;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.application.Application;

public class SbmlCompositeSemanticController extends SbmlSemanticController
{

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment compartment, Object type, String name, Point point, Object properties)
    {
        try
        {
            if( type.equals( ConnectionPort.class ) && properties instanceof PortProperties )
            {
                PortProperties portProperties = (PortProperties)properties;
                DiagramElementGroup elements = portProperties.createElements( compartment, point, null );
                return elements;

            }
            else
                return super.createInstance( compartment, type, name, point, properties );

        }
        catch( Throwable t )
        {

            throw ExceptionRegistry.translateException( t );
        }
    }
    
    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        if( type instanceof Class )
        {
            Class<?> typeClass = (Class<?>)type;
            if( typeClass == Stub.DirectedConnection.class || typeClass == Stub.UndirectedConnection.class )
            {
                CreateEdgeDialog dialog = CreateEdgeDialog.getConnectionDialog( Module.getModule( parent ), point, viewEditor, typeClass,
                        parent );
                dialog.setVisible( true );
                return null;
            }

            try
            {
                Object properties = getPropertiesByType( parent, type, point );
                if( properties != null )
                {
                    PropertiesDialog dialog = new PropertiesDialog( Application.getApplicationFrame(), "New element", properties );
                    if( dialog.doModal() )
                    {
                        if( properties instanceof InitialElementProperties )
                        {
                            ( (InitialElementProperties)properties ).createElements( parent, point, viewEditor );
                        }
                    }
                    return null;
                }
            }
            catch( Throwable t )
            {

                throw ExceptionRegistry.translateException( t );
            }
        }
        return super.createInstance( parent, type, point, viewEditor );
    }

    @Override
    public boolean remove(DiagramElement de) throws Exception
    {
        if( de instanceof Edge )
        {
            Edge edge = (Edge)de;
            if( Util.isDirectedConnection( edge ) || Util.isUndirectedConnection( edge ) )
            {
                if( edge.nodes().anyMatch( Util::isPropagatedPort ) )
                    return false;
                de.getOrigin().remove( de.getName() );
                return true;
            }
        }
        else if( de instanceof ModelDefinition )
        {
            Diagram diagram = Diagram.getDiagram( de );
            for( SubDiagram node : diagram.stream( SubDiagram.class ) )
            {
                DynamicProperty dp = node.getDiagram().getAttributes().getProperty( ModelDefinition.REF_MODEL_DEFINITION );
                if( dp != null && dp.getValue().equals( de ) )
                {
                    remove( node );
                }
            }
            diagram.remove( de.getName() );
            return true;
        }

        return super.remove( de );
    }

    /**
     * Moves the specified diagram element to the specified position. If neccessary
     * compartment can be changed.
     *
     * @returns the actual distance on which the diagram node was moved.
     */
    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        if( offset.width == 0 && offset.height == 0 )
            return offset;
        //        boolean isAutoLayout = Diagram.getDiagram(de).getViewOptions().isAutoLayout();
        if( Util.isModulePort( de ) )
        {
            Compartment compartment = (Compartment)de.getOrigin();
            CompositeSemanticController.movePortToEdge( (Node)de, compartment, offset, false );

            for( Edge edge : ( (Node)de ).getEdges() )
            {
                //                if( isAutoLayout )
                //                    edge.setPath(null);
                //                else
                recalculateEdgePath( edge );
            }
            return offset;
        }
        else if( de instanceof Compartment )
        {
            Compartment compartment = (Compartment)de;
            Point location = compartment.getLocation();
            location.translate( offset.width, offset.height );

            boolean notification = compartment.isNotificationEnabled();
            compartment.setNotificationEnabled( false );//to avoid updating subDiagram

            compartment.setLocation( location );

            compartment.setNotificationEnabled( notification );

            for( Node node : compartment.getNodes() )
            {
                Point nodeLocation = node.getLocation();
                nodeLocation.translate( offset.width, offset.height );
                node.setLocation( nodeLocation );
            }

            for( Edge edge : CompositeSemanticController.getEdges( compartment ) )
            {
                recalculateEdgePath( edge );
            }

            return offset;
        }

        return super.move( de, newParent, offset, oldBounds );
    }

    @Override
    public synchronized DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement) throws Exception
    {
        String stubType = de.getKernel().getType();
        if( de instanceof Diagram )
        {
            addPackage( (Diagram)de, "comp" );

            if( compartment instanceof Diagram )
            {
                SubDiagram subDiagram = new SubDiagram( compartment, (Diagram)de, de.getName() );
                subDiagram.getAttributes().add( new DynamicProperty( Util.EXTENT_FACTOR, String.class, "" ) );
                subDiagram.getAttributes().add( new DynamicProperty( Util.TIME_SCALE, String.class, "" ) );
                subDiagram.setLocation( ( (Node)de ).getLocation() );
                this.setXmlType( subDiagram, "subDiagram" );
                subDiagram.updatePorts();
                return subDiagram;
            }

            if( de.getRole() == null )
                de.setRole( new SbmlEModel( de ) );
            return de;
        }
        else if( "connection".equals( stubType ) )
        {
            if( de instanceof Edge )
                return this.createConnection( stubType, compartment, (Edge)de );
        }
        else if( newElement && "modelDefinition".equals( stubType ) )
        {
            Diagram parent = Diagram.optDiagram( compartment );
            Diagram innerDiagram = null;
            if( de instanceof ModelDefinition && ( (ModelDefinition)de ).getDiagram() != null )
                innerDiagram = ( (ModelDefinition)de ).getDiagram();
            else if( parent != null )
            {
                innerDiagram = parent.getType().clone().createDiagram( null, de.getName(), new DiagramInfo( de.getName() ) );

                ModelDefinition modelDefinition = new ModelDefinition( compartment, innerDiagram, de.getName() );

                modelDefinition.setLocation( ( (Node)de ).getLocation() );
                de = modelDefinition;
            }
            if( innerDiagram != null )
            {
                if( innerDiagram.getRole() == null )
                    innerDiagram.setRole( new SbmlEModel( innerDiagram ) );
            }
            setXmlType( de, "modelDefinition" );
            return de;
        }
        else if( newElement && "subdiagram".equals( stubType ) )
        {
            Diagram parent = Diagram.optDiagram( compartment );
            Diagram innerDiagram = null;
            if( de instanceof SubDiagram && ( (SubDiagram)de ).getDiagram() != null )
            {
                innerDiagram = ( (SubDiagram)de ).getDiagram();
            }
            else if( parent != null )
            {
                innerDiagram = parent.getType().createDiagram( parent, de.getName(), new DiagramInfo( de.getName() ) );

                SubDiagram subDiagram = new SubDiagram( compartment, innerDiagram, de.getName() );
                subDiagram.setLocation( ( (Node)de ).getLocation() );
                de = subDiagram;
            }

            if( innerDiagram != null )
            {
                if( innerDiagram.getRole() == null )
                    innerDiagram.setRole( new EModel( innerDiagram ) );
            }
            DynamicProperty timeFactor = de.getAttributes().getProperty( Util.TIME_SCALE );
            if( timeFactor == null )
                de.getAttributes().add( new DynamicProperty( Util.TIME_SCALE, String.class, "" ) );
            DynamicProperty extentFactor = de.getAttributes().getProperty( Util.EXTENT_FACTOR );
            if( extentFactor == null )
                de.getAttributes().add( new DynamicProperty( Util.EXTENT_FACTOR, String.class, "" ) );
            setXmlType( de, "subDiagram" );
            return de;
        }


        return super.validate( compartment, de, newElement );
    }

    /**
     * Recreating connection edge with appropriate kernel
     * @param xmlType
     * @param compartment
     * @param oldConnection
     * @return
     * @throws Exception
     */
    protected Edge createConnection(String xmlType, Compartment compartment, Edge oldConnection) throws Exception
    {
        Edge edge = null;

        DynamicPropertySet attributes = oldConnection.getAttributes();

        DynamicProperty typeProperty = attributes.getProperty( "connectionType" );

        String name = oldConnection.getName();
        Base kernel = new Stub.DirectedConnection( compartment, name );
        if( typeProperty != null )
        {
            String connectionType = typeProperty.getValue().toString();
            if( "directed".equals( connectionType ) )
            {
                kernel = new Stub.DirectedConnection( compartment, name );
            }
            else if( "undirected".equals( connectionType ) )
            {
                kernel = new Stub.UndirectedConnection( compartment, name );
            }
        }

        edge = new Edge( name, kernel, oldConnection.getInput(), oldConnection.getOutput() );
        Connection connection;
        if( kernel instanceof Stub.UndirectedConnection )
            connection = new UndirectedConnection( edge );
        else
            connection = new DirectedConnection( edge );
        edge.setRole( connection );
        Node input = edge.getInput();
        Node output = edge.getOutput();
        if( Util.isPort( input ) && Util.isPort( output ) )
        {
            String inputVar = Util.getPortVariable( input );
            String outputVar = Util.getPortVariable( output );
            connection.setInputPort( new Connection.Port( inputVar ) );
            connection.setOutputPort( new Connection.Port( outputVar ) );
        }

        setXmlType( edge, xmlType );

        return edge;
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        if( type instanceof Class )
        {
            if( SubDiagram.class.isAssignableFrom( (Class<?>)type ) )
            {
                return new SubDiagramProperties( Diagram.getDiagram( compartment ) );
            }
        }
        return super.getPropertiesByType( compartment, type, point );
    }

    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        if(de instanceof Edge)
        {
            Edge e = (Edge)de;
            if( e.getKernel() instanceof MultipleConnection )
                return Util.isSubDiagram(e.getInput()) && Util.isSubDiagram(e.getOutput());
            if( Util.isDirectedConnection(e) )
                return Util.isOutputPort(e.getInput()) && Util.isInputPort(e.getOutput());
            if( Util.isUndirectedConnection(e) )
                return Util.isContactPort(e.getInput()) && Util.isContactPort(e.getOutput());
        }

        if (Util.isSubDiagram(de) || Util.isPort(de) || de instanceof ModelDefinition)
            return true;

        return super.canAccept(compartment, de);
    }


}
