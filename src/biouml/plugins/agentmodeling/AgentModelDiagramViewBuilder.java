package biouml.plugins.agentmodeling;

import biouml.model.Compartment;
import biouml.model.DiagramViewOptions;
import biouml.standard.diagram.CompositeDiagramViewBuilder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;

import org.apache.commons.text.StringEscapeUtils;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;

import biouml.standard.type.Stub;

public class AgentModelDiagramViewBuilder extends CompositeDiagramViewBuilder
{
    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return new AgentModelDiagramViewOptions(null);
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        if( compartment.getKernel() instanceof Stub.AveragerElement )
        {
            return createAdapterCoreView(container, compartment, (AgentModelDiagramViewOptions)options, g);
        }
        else if (compartment.getKernel() != null && compartment.getKernel().getType().equals("ScriptAgent"))
        {
            return createScriptAgentCoreView(container, compartment, (AgentModelDiagramViewOptions)options, g);
        }
        else if (compartment.getKernel() != null && compartment.getKernel().getType().equals("PythontAgent"))
        {
            return createScriptAgentCoreView(container, compartment, (AgentModelDiagramViewOptions)options, g);
        }
        else
        {
            return super.createCompartmentCoreView(container, compartment, options, g);
        }
    }

    public boolean createAdapterCoreView(CompositeView container, Compartment compartment, AgentModelDiagramViewOptions options, Graphics g)
    {
        String title = compartment.getTitle();
        ComplexTextView titleView = new ComplexTextView(title, getTitleFont(compartment, options.getCompartmentTitleFont()),
                options.getFontRegistry(), ComplexTextView.TEXT_ALIGN_CENTER, options.getMaxTitleSize(), g );

        Dimension size = compartment.getShapeSize();
        if( size == null )
            size = new Dimension(100, 80);
        BoxView boxView = new BoxView(getBorderPen(compartment, options.getDefaultPen()), getBrush(compartment, new Brush(Color.white)), 0,
                0, size.width, size.height);
        container.add(boxView);
        container.add(titleView, CompositeView.X_CC | CompositeView.Y_CC);
        container.setModel(compartment);
        container.setActive(true);
        container.setLocation(compartment.getLocation());
        compartment.setView(container);
        return false;
    }

    public boolean createScriptAgentCoreView(CompositeView container, Compartment compartment, AgentModelDiagramViewOptions options, Graphics g)
    {
        String script = compartment.getAttributes().getValueAsString("Script");
        ComplexTextView text = new ComplexTextView(StringEscapeUtils.escapeHtml4( script), getTitleFont(compartment, options.getScriptFont()), options.getFontRegistry(),
                ComplexTextView.TEXT_ALIGN_LEFT, 40, g);

        Dimension size = compartment.getShapeSize();
        if( text.getBounds().height > size.height )
            size.height = text.getBounds().height + 5;
        RectangularShape roundRect = new RoundRectangle2D.Float(0, 0, size.width, size.height, 5, 5);
        BoxView view = new BoxView(getBorderPen(compartment, options.getScriptPen()), getBrush(compartment, options.getScriptBrush()), roundRect);
        container.add(view);
        container.add(text, CompositeView.X_CC | CompositeView.Y_TT);
        container.setLocation(compartment.getLocation());
        container.setModel(compartment);
        container.setActive(true);
        compartment.setView( container );
        return false;
    }
}
