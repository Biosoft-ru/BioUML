package biouml.plugins.sbgn;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import javax.annotation.Nonnull;

import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.standard.type.Stub;

/**
 * @author Ilya
 * For the most part code is ported form "sbml-sbgn.xml" notation
 */
public class SbgnCompositeDiagramViewBuilder extends SbgnDiagramViewBuilder
{
    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        if( node.getKernel().getType().equals( biouml.standard.type.Type.TYPE_CONNECTION_BUS ) )
        {
            return this.createBusView( container, node, (SbgnDiagramViewOptions)options, g );
        }
        return super.createNodeCoreView( container, node, options, g );
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        if( compartment instanceof ModelDefinition )
        {
            return createModelDefinitionView(container, (ModelDefinition)compartment, (SbgnCompositeDiagramViewOptions)options, g);
        }
        else if (compartment instanceof SubDiagram)
        {
            return createSubDiagramView(container, (SubDiagram)compartment, (SbgnCompositeDiagramViewOptions)options, g);
        }
        return super.createCompartmentCoreView(container, compartment, options, g);
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions viewOptions, Graphics g)
    {
        if (edge.getKernel() instanceof Stub.UndirectedConnection)
        {
            if( edge.getPath() == null )
                Diagram.getDiagram(edge).getType().getSemanticController().recalculateEdgePath(edge);

            SbgnCompositeDiagramViewOptions sbgnOptions = (SbgnCompositeDiagramViewOptions)viewOptions;
            CompositeView view = new ArrowView(sbgnOptions.getConnectionPen(), sbgnOptions.getConnectionBrush(), edge.getSimplePath(), 0, 0);
            SbgnUtil.setView(view, edge);
            return view;
        }
        return super.createEdgeView(edge, viewOptions, g);
    }

    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new SbgnCompositeDiagramViewOptions();
    }

    protected boolean createModelDefinitionView(CompositeView container, ModelDefinition compartment, SbgnCompositeDiagramViewOptions options, Graphics g)
    {
        String title = compartment.getTitle();
        if( title == null || title.isEmpty() )
            title = compartment.getName();
        ComplexTextView titleView = new ComplexTextView(title, getTitleFont(compartment, options.getCompartmentTitleFont()),
                options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );

        Dimension size = compartment.getShapeSize();
        if( size == null )
            size = new Dimension(150, 150);

        container.add( new BoxView( getBorderPen( compartment, options.getModelDefinitionPen() ),
                DefaultDiagramViewBuilder.getBrush( compartment, options.getModelDefBrush() ), 0, 0, size.width, size.height ) );
        container.add(titleView, CompositeView.X_CC | CompositeView.Y_CC);
        SbgnUtil.setView(container, compartment);
        return false;
    }

    protected boolean createSubDiagramView(CompositeView container, SubDiagram compartment, SbgnDiagramViewOptions options, Graphics g)
    {
        ComplexTextView titleView = null;
        ComplexTextView expView = null;
        
        if( compartment.isShowTitle() )
        {
            String title = compartment.getTitle();
            if( title == null || title.isEmpty() )
                title = compartment.getName();

            titleView = new ComplexTextView( title, getTitleFont( compartment, options.getCompartmentTitleFont() ),
                    options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
            Diagram diagram = compartment.getDiagram();
            
            if( diagram.getCurrentState() != null )
            {
                expView = new ComplexTextView(diagram.getCurrentStateName(), options.getStateTitleFont(), options.getFontRegistry(),
                        ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );
            } 
        }

        Dimension size = compartment.getShapeSize();
        if( size == null )
            size = new Dimension( 250, 250 );      
        
        int minimalWidth = 50;
        int minimalHeight = 50;

        if( titleView != null )
        {
            minimalWidth = Math.max( minimalWidth, titleView.getBounds().width );

            if( expView != null )
                minimalHeight = Math.max( minimalHeight, titleView.getBounds().height + expView.getBounds().height );
            else
                minimalHeight = Math.max( minimalHeight, titleView.getBounds().height );
        }

        if( expView != null )
        {
            minimalWidth = Math.max( minimalWidth, expView.getBounds().width );
        }

        size.height = Math.max( size.height, minimalHeight);
        size.width = Math.max( size.width, minimalWidth );

        container.add( new BoxView( getBorderPen( compartment, options.getModulePen() ),
                DefaultDiagramViewBuilder.getBrush( compartment, options.getModuleBrush() ), 0, 0, size.width, size.height ) );
        
        if( titleView != null )
        {
            container.add( titleView, CompositeView.X_CC | CompositeView.Y_CC );
            if( expView != null )
                container.add( expView, CompositeView.X_CC | CompositeView.Y_CC, new Point( 0, titleView.getBounds().height + 3 ) );
        }
        
        compartment.setShapeSize( size );
        SbgnUtil.setView(container, compartment);
        return false;
    }
}