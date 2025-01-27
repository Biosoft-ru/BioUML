package biouml.plugins.sbol;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.standard.diagram.CreateEdgeAction;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;

public class SbolDiagramSemanticController extends DefaultSemanticController
{

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point pt, ViewEditorPane viewEditor)
    {
        boolean isNotificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled( isNotificationEnabled );

        try
        {
            if( type.equals( ParticipationProperties.class ) )
            {
                new CreateEdgeAction().createEdge( pt, viewEditor, new ParticipationEdgeCreator() );
                return DiagramElementGroup.EMPTY_EG;
            }

            Object properties = getPropertiesByType( parent, type, pt );
            if( properties instanceof InitialElementProperties )
            {
                if( new PropertiesDialog( Application.getApplicationFrame(), "New element", properties ).doModal() )
                    ( (InitialElementProperties)properties ).createElements( parent, pt, viewEditor );
                return DiagramElementGroup.EMPTY_EG;
            }
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, "Error during element creation", ex );
            return DiagramElementGroup.EMPTY_EG;
        }
        finally
        {
            parent.setNotificationEnabled( isNotificationEnabled );
        }
        return super.createInstance( parent, type, pt, viewEditor );
    }

    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        Base deBase = de.getKernel();
        Base compartmentBase = compartment.getKernel();

        if( deBase instanceof SequenceFeature )
        {
            return compartmentBase instanceof Backbone
                    || ( compartmentBase instanceof SequenceFeature && compartment.getOrigin() == de.getOrigin() );
        }
        else if( deBase instanceof Backbone )
        {
            return compartment instanceof Diagram;
        }
        else if( deBase instanceof MolecularSpecies )
        {
            return compartment instanceof Diagram;
        }
        else if( deBase instanceof InteractionProperties )
        {
            return compartment instanceof Diagram;
        }

        if( de instanceof Edge )
            return true;
        return false;
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        try
        {
            if( type.equals( Backbone.class ) || type.equals( Backbone.class.getName() ) )
            {
                return new Backbone( DefaultSemanticController.generateUniqueName( Diagram.getDiagram( compartment ), "Backbone" ) , false);
            }
            else if( type.equals( SequenceFeature.class ) || type.equals( SequenceFeature.class.getName() ) )
            {
                return new SequenceFeature( DefaultSemanticController.generateUniqueName( Diagram.getDiagram( compartment ), "Promoter" ) , false );
            }
            else if( type.equals( MolecularSpecies.class ) || type.equals( SequenceFeature.class.getName() ) )
            {
                return new MolecularSpecies( DefaultSemanticController.generateUniqueName( Diagram.getDiagram( compartment ), "Complex" ) , false );
            }
            else if( type.equals( InteractionProperties.class ) || type.equals( InteractionProperties.class.getName() ) )
            {
                return new InteractionProperties(
                        DefaultSemanticController.generateUniqueName( Diagram.getDiagram( compartment ), "Process" ) , false );
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isResizable(DiagramElement diagramElement)
    {
        return diagramElement.getKernel() instanceof MolecularSpecies;
    }

    @Override
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement) throws Exception
    {
        return de;
    }

    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        if( de instanceof Node )
        {
            if( de.getKernel() instanceof Backbone )
                return super.move( de, newParent, offset, oldBounds );
            Node node = (Node)de;
            Compartment parent = node.getCompartment();
            if( newParent.getKernel() instanceof SequenceFeature )
                newParent = newParent.getCompartment();
            if( parent.getKernel() instanceof Backbone )
            {
                //do not move outside parent backbone
                if( newParent != parent )
                {
                    return new Dimension( 0, 0 );
                }
                Point location = node.getLocation();
                int oldX = location.x;
                //TODO: discuss how to get size, from view or from shape size
                int width = (int) ( Math.ceil( ( (Node)de ).getShapeSize().getWidth() ) );

                location.translate( offset.width, 0 );
                int newX = location.x;
                int dx = parent.stream().mapToInt( n -> {
                    if( n instanceof Node )
                    {
                        Point nlocation = ( (Node)n ).getLocation();
                        int nX = nlocation.x;
                        int nW = (int) ( Math.ceil( ( (Node)n ).getShapeSize().getWidth() ) );
                        if( nX == oldX )
                            return 0;
                        if( oldX > newX && nX <= newX && nX + nW > newX )
                            return oldX - nX - nW;
                        else if( oldX < newX && nX <= newX && nX + nW > newX )
                            return nX + nW - oldX - width;
                        return 0;
                    }
                    return 0;
                } ).max().orElse( 0 );
                int shift = oldX > newX ? -dx : dx;
                if( dx == 0 )
                    return new Dimension( 0, 0 );
                Set<Edge> edges = new HashSet<>();
                parent.stream().forEach( n -> {
                    if( n instanceof Node )
                    {
                        Point nlocation = ( (Node)n ).getLocation();
                        int nX = nlocation.x;

                        if( nX == oldX )
                        {
                            nlocation.translate( shift, 0 );
                            ( (Node)n ).setLocation( nlocation );
                            edges.addAll( ( (Node)n ).edges().collect( Collectors.toSet() ) );
                        }
                        else if( oldX > newX && nX > newX && nX < oldX + width )
                        {
                            nlocation.translate( width, 0 );
                            ( (Node)n ).setLocation( nlocation );
                            edges.addAll( ( (Node)n ).edges().collect( Collectors.toSet() ) );
                        }
                        else if( oldX < newX && nX > oldX && nX <= newX )
                        {
                            nlocation.translate( -width, 0 );
                            ( (Node)n ).setLocation( nlocation );
                            edges.addAll( ( (Node)n ).edges().collect( Collectors.toSet() ) );
                        }
                    }
                } );
                edges.stream().forEach( e -> {
                    e.setPath( null );
                    recalculateEdgePath( e );
                    return;
                } );
                return new Dimension( dx, 0 );
            }
            else
            {
                if( newParent != parent )
                {
                    return new Dimension( 0, 0 );
                }
                return super.move( de, newParent, offset, oldBounds );
            }
        }
        else
            return super.move( de, newParent, offset, oldBounds );
    }

    @Override
    public void recalculateEdgePath(Edge edge)
    {
        Diagram diagram = Diagram.getDiagram( edge );
        DiagramType diagramType = diagram.getType();
        if( diagramType == null )
            return;
        Node inNode = edge.getInput();
        Node outNode = edge.getOutput();
        //edge inside compartment
        if( edge.getPath() == null && edge.getKernel() instanceof Stub && inNode.getCompartment().getKernel() instanceof Backbone
                && inNode.getOrigin() == outNode.getOrigin() )
        {
            Point in = new Point();
            Point out = new Point();
            diagramType.getDiagramViewBuilder().calculateInOut( edge, in, out );
            edge.setPath( new Path( new int[] {in.x, in.x, out.x, out.x}, new int[] {in.y, in.y - 20, out.y - 20, out.y}, 4 ) );
        }
        else
            super.recalculateEdgePath( edge );
    }

    @Override
    public boolean remove(DiagramElement de) throws Exception
    {
        if( de instanceof Diagram )
            return false;
        Compartment parent = de.getCompartment();
        //remove one of the sequence elements from backbone
        if( de instanceof Node && parent.getKernel() instanceof Backbone )
        {
            //TODO: discuss how to get size, from view or from shape size
            int width = (int) ( Math.ceil( ( (Node)de ).getShapeSize().getWidth() ) );
            Point location = ( (Node)de ).getLocation();
            Set<Edge> edges = new HashSet<>();
            parent.stream().forEach( n -> {
                if( n instanceof Node )
                {
                    Point nlocation = ( (Node)n ).getLocation();
                    int nX = nlocation.x;
                    //move all elements after the removing node
                    if( nX > location.x )
                    {
                        nlocation.translate( -width, 0 );
                        ( (Node)n ).setLocation( nlocation );
                        edges.addAll( ( (Node)n ).edges().collect( Collectors.toSet() ) );
                    }
                }
            } );
            edges.stream().forEach( e -> {
                e.setPath( null );
                recalculateEdgePath( e );
                return;
            } );
            for( Edge edge : Util.getEdges( (Node)de ) )
            {
                removeEdge( edge );
            }

            if( de instanceof Compartment )
                ( (Compartment)de ).clear();

            SbolUtil.removeSbolObjectFromDiagram( de );

            // remove diagramElement
            parent.remove( de.getName() );
            Dimension d = new Dimension( parent.getShapeSize().width - width, parent.getShapeSize().height );
            parent.setShapeSize( d );

            return true;
        }
        //remove one of nodes on diagram (not backbone)
        else if( de instanceof Node && ! ( de.getKernel() instanceof Backbone ) )
        {
            Node node = (Node)de;


            if( node.getKernel() instanceof InteractionProperties && node.getKernel().getType().equals( SbolConstants.DEGRADATION ) )
            {
                Node empty = node.edges().map( e -> e.getOtherEnd( node ) )
                        .filter( n -> n.getKernel().getType().equals( SbolConstants.DEGRADATION_PRODUCT ) ).findAny().orElse( null );
                if( empty != null )
                    remove( empty );
            }

            for( Edge edge : Util.getEdges( node ) )
                removeEdge( edge );
            
            if( de instanceof Compartment )
                ( (Compartment)de ).clear();
            SbolUtil.removeSbolObjectFromDiagram( de );
            de.getOrigin().remove( de.getName() );
            return true;
        }
        //remove edge
        else if( de instanceof Edge )
        {
            removeEdge( (Edge)de );
            return true;
        }
        //remove backbone
        else
        {
            SbolUtil.removeSbolObjectFromDiagram( de );
            return super.remove( de );
        }

    }

    private void removeEdge(Edge edge) throws Exception
    {
        if( SbolConstants.DEGRADATION.equals( edge.getKernel().getType() ) )
        {
            //remove degradation stub node
            Node degradationNode = ( (Edge)edge ).getOutput();
            if( degradationNode.getKernel().getType().equals( SbolUtil.TYPE_DEGRADATION_PRODUCT ) )
                try
                {
                    degradationNode.getCompartment().remove( degradationNode.getName() );
                }
                catch( Exception e )
                {
                    //TODO: error handling
                }
        }
        SbolUtil.removeSbolObjectFromDiagram( edge );
        edge.getOrigin().remove( edge.getName() );
    }

    @Override
    public Edge createEdge(@Nonnull Node fromNode, @Nonnull Node toNode, String edgeType, Compartment compartment)
            throws IllegalArgumentException
    {
        if( edgeType.equals( ParticipationProperties.class.getName() ) )
        {
            return new ParticipationEdgeCreator().createEdge( fromNode, toNode, false );
        }
        return null;
    }
}
