package biouml.standard.state;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.undo.UndoableEdit;

import ru.biosoft.access.core.undo.DataCollectionAddUndo;
import ru.biosoft.access.core.undo.DataCollectionRemoveUndo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;
import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.PathwayDiagramViewBuilder;
import biouml.standard.diagram.PathwaySimulationDiagramViewBuilder;

import com.developmentontheedge.beans.Option;

public class StateDiagramViewBuilder extends PathwaySimulationDiagramViewBuilder
{
    protected State state;
    protected Diagram diagram;

    protected ColorFont titleFont = new ColorFont("Arial", Font.BOLD, 18, Color.red);
    protected int titleAlignment = CompositeView.X_CC | CompositeView.Y_TT;
    protected Pen redPen = new Pen(2, Color.red);
    protected Pen greenPen = new Pen(2, Color.green);
    protected Pen bigRedPen = new Pen(7, Color.red);
    protected Pen orangePen = new Pen(2, Color.orange);

    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        return baseViewBuilder.createDefaultDiagramViewOptions();
    }
    
    public StateDiagramViewBuilder(DiagramViewBuilder baseViewBuilder, State state, Diagram diagram)
    {
        this.baseViewBuilder = baseViewBuilder;
        this.state = state;
        this.diagram = diagram;
    }

    public DiagramViewBuilder getBaseViewBuilder()
    {
        return baseViewBuilder;
    }

    @Override
    public void setBaseViewBuilder(DiagramViewBuilder baseViewBuilder)
    {
        this.baseViewBuilder.setBaseViewBuilder(baseViewBuilder);
    }

    @Override
    public void setTypeMapping(Map<Object, String> typeMapping)
    {
        this.baseViewBuilder.setTypeMapping(typeMapping);
    }

    @Override
    public @Nonnull CompositeView createDiagramView(Diagram diagram, Graphics g)
    {
        boolean notificationEnabled = diagram.isNotificationEnabled();
        boolean propagationEnabled = diagram.isPropagationEnabled();
        diagram.setNotificationEnabled(false);
        diagram.setPropagationEnabled(false);
        state.getStateUndoManager().undoDeleted();

        CompositeView result = super.createDiagramView(diagram, g);
        if( baseViewBuilder instanceof PathwayDiagramViewBuilder )
        {
            ( (PathwayDiagramViewBuilder)baseViewBuilder ).createReactionTitles(diagram, result, diagram.getViewOptions(), g);
        }

        View title = new TextView(state.getName(), titleFont, g);
        result.add(title, titleAlignment, new Point(5, -60));

        diagram.setNotificationEnabled(false);
        diagram.setPropagationEnabled(false);

        state.getStateUndoManager().redoDeleted();
        diagram.setNotificationEnabled(notificationEnabled);
        diagram.setPropagationEnabled(propagationEnabled);

        return result;
    }

    @Override
    public @Nonnull CompositeView createNodeView(Node node, DiagramViewOptions options, Graphics g)
    {
        CompositeView result = baseViewBuilder.createNodeView(node, options, g);

        decorateNodeView(node, result);

        return result;
    }

    private void decorateNodeView(Node node, CompositeView result)
    {
        if (node instanceof Diagram)
            return;
        int elementStatus = getStatus(node);

        if( elementStatus == STATUS_REMOVED )
        {
            result.setPen(redPen);
            result.add(new LineView(bigRedPen, -10, -10, 10, 10), CompositeView.X_CC | CompositeView.Y_CC);
            result.add(new LineView(bigRedPen, -10, 10, 10, -10), CompositeView.X_CC | CompositeView.Y_CC);
        } else if( elementStatus == STATUS_ADDED )
        {
            result.setPen(greenPen);
            CompositeView plusView = new CompositeView();
            plusView.add(new LineView(redPen, 0, 5, 10, 5));
            plusView.add(new LineView(redPen, 5, 0, 5, 10));
            result.add(plusView, CompositeView.X_RR|CompositeView.Y_TT);
        } else if( elementStatus == STATUS_CHANGED )
        {
            result.setPen(orangePen);
        }
    }

    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {
        int elementStatus = getStatus(edge);

        CompositeView result = baseViewBuilder.createEdgeView(edge, options, g);

        if( elementStatus == STATUS_ADDED )
        {
            result.setPen(greenPen);
        }
        else if( elementStatus == STATUS_CHANGED )
        {
            result.setPen(orangePen);
        }
        else if( elementStatus == STATUS_REMOVED )
        {
            result.setPen(redPen);
        }

        return result;
    }

    @Override
    public @Nonnull CompositeView createCompartmentView(Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        //TODO: check if we should call baseViewBuilder
//        CompositeView result = baseViewBuilder.createCompartmentView(compartment, options, g);
//        CompositeView result;
//        if( compartment instanceof SubDiagram && options instanceof CompositeDiagramViewOptions
//                && ( (CompositeDiagramViewOptions)options ).isCollapsed() )
//        {
//            result = baseViewBuilder.createCompartmentView(compartment, options, g);
//        }
//        else
//        {
        CompositeView result = super.createCompartmentView(compartment, options, g);
//        }
        decorateNodeView(compartment, result);
        return result;
    }

    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        if( baseViewBuilder instanceof DefaultDiagramViewBuilder )
        {
            return ( (DefaultDiagramViewBuilder)baseViewBuilder ).createCompartmentCoreView(container, compartment, options, g);
        }
        return super.createCompartmentCoreView(container, compartment, options, g);
    }

    private final static int STATUS_NONE = 0;
    private final static int STATUS_ADDED = 1;
    private final static int STATUS_REMOVED = 2;
    private final static int STATUS_CHANGED = 3;

    protected int getStatus(DiagramElement de)
    {
        boolean isChanged = false;
        List<UndoableEdit> edits = state.getStateUndoManager().getEditsFlat();
        for( int i = edits.size() - 1; i >= 0; i-- )
        {
            UndoableEdit ce = edits.get(i);
            if( ce instanceof DataCollectionAddUndo )
            {
                if( ( (DataCollectionAddUndo)ce ).getDataElement() == de )
                    return STATUS_ADDED;
            }
            else if( ce instanceof DataCollectionRemoveUndo )
            {
                if( ( (DataCollectionRemoveUndo)ce ).getDataElement() == de )
                    return STATUS_REMOVED;
            }
            else if( ce instanceof StatePropertyChangeUndo )
            {
                if( ( (StatePropertyChangeUndo)ce ).getSource() == de || ( (StatePropertyChangeUndo)ce ).getSource() == de.getRole() )
                    isChanged = true;
            }
        }
        if( isChanged )
        {
            return STATUS_CHANGED;
        }
        return STATUS_NONE;
    }

    protected DiagramElement getDiagramElement(String elementName)
    {
        try
        {
            DataElement element = (DataElement)diagram.findObject(elementName);
            while( ! ( element instanceof DiagramElement ) && element != null )
            {
                if( element instanceof Option )
                {
                    element = (DataElement) ( (Option)element ).getParent();
                }
                else
                {
                    element = element.getOrigin();
                }
            }
            if( element != null )
            {
                return (DiagramElement)element;
            }
        }
        catch( Exception e )
        {
            //nothing to do
        }
        return null;
    }

    @Override
    public Icon getIcon(Object type)
    {
        return baseViewBuilder.getIcon(type);
    }
}
