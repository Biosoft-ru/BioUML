package biouml.plugins.sbml.composite;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Icon;

import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;

import biouml.model.Compartment;
import biouml.model.DiagramViewOptions;
import biouml.model.ModelDefinition;
import biouml.plugins.sbml.SbmlDiagramViewBuilder;
import biouml.standard.diagram.CompositeDiagramViewBuilder;
import biouml.standard.type.Type;

public class SbmlCompositeDiagramViewBuilder extends CompositeDiagramViewBuilder
{
    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new SbmlCompositeDiagramViewOptions( null );
    }

    @Override
    public Icon getIcon(Object type)
    {
        Icon icon = getIcon( (Class<?>)type, getClass() );
        if( icon == null )
        {
            icon = getIcon( (Class<?>)type, SbmlDiagramViewBuilder.class );
        }
        if( icon == null )
        {
            icon = getIcon( (Class<?>)type, CompositeDiagramViewBuilder.class );
        }
        return icon;
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        if( Type.TYPE_MODEL_DEFINITION.equals( compartment.getKernel().getType() ) )
        {
            return createModelDefinitionView( container, (ModelDefinition)compartment, (SbmlCompositeDiagramViewOptions)options, g );
        }
        else
        {
            return super.createCompartmentCoreView( container, compartment, options, g );
        }
    }

    protected boolean createModelDefinitionView(CompositeView container, ModelDefinition compartment,
            SbmlCompositeDiagramViewOptions options, Graphics g)
    {
        String title = compartment.getTitle();
        if( title == null || title.equals( "" ) )
            title = compartment.getName();
        ComplexTextView titleView = new ComplexTextView( title, options.getCompartmentTitleFont(), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );


        Dimension size = compartment.getShapeSize();

        if( size.height == 0 && size.width == 0 )
            size = new Dimension( 250, 250 );

        Pen pen = new Pen( new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {5, 5}, 0f ), Color.black );
        BoxView boxView = new BoxView( pen, options.getModelDefBrush(), 0, 0, size.width, size.height );

        container.add( boxView );
        container.add( titleView, CompositeView.X_CC | CompositeView.Y_CC );

        container.setModel( compartment );
        container.setActive( true );

        container.setLocation( compartment.getLocation() );
        compartment.setView( container );

        return false;
    }
}
