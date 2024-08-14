package biouml.standard.filter;

import java.awt.Rectangle;

import one.util.streamex.StreamEx;

import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.Node;

/**
 * If some diagram element satisfies to conditions of several filters simultaneously,
 * then it can be highlighted by several colors.
 */
public class CompositeHighlightAction implements Action
{
    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    private static int orientation = X_AXIS;

    /**
     * Looks for childView having given node as model
     * @param parentView parent view to look in
     * @param node node to look for
     * @return position of found child view; end position inside parentView if not found
     */
    protected static int lookupNodePosition(CompositeView parentView, Node node)
    {
        int pos=0;
        for(View view: parentView)
        {
            if(view.getModel() == node) break;
            pos++;
        }
        return pos;
    }

    public CompositeHighlightAction()
    {}

    public CompositeHighlightAction(HighlightAction[] actions)
    {
        this.actions = actions;
    }

    public CompositeHighlightAction(Action action1, Action action2)
    {
        if( action1 instanceof HighlightAction )
            add( (HighlightAction)action1 );
        else if ( action1 instanceof CompositeHighlightAction )
            add( (CompositeHighlightAction)action1 );

        if( action2 instanceof HighlightAction )
            add( (HighlightAction)action2 );
        else if ( action2 instanceof CompositeHighlightAction )
            add( (CompositeHighlightAction)action2 );
    }

    protected HighlightAction[] actions;
    public HighlightAction[] getActions()
    {
        return actions;
    }

    public void add(HighlightAction action)
    {
        if(actions == null)
        {
            actions = new HighlightAction[1];
            actions[0] = action;
            return;
        }

        int length = actions.length;
        HighlightAction[]  newActions = new HighlightAction[length + 1];
        System.arraycopy(actions, 0, newActions, 0, length);

        newActions[length] = action;
        actions = newActions;
    }

    public void add(CompositeHighlightAction cha)
    {
        if( actions == null )
            actions = cha.getActions();
        else
        {
            StreamEx.of(cha.actions).forEach(this::add);
        }
    }

    @Override
    public void apply(DiagramElement de)
    {
        if( actions == null )
            return;

        if( actions.length == 1 )
        {
            actions[0].apply(de);
            return;
        }

        if( de instanceof Node && de.getOrigin() instanceof Compartment)
        {
            Node node = (Node)de;
            Compartment parent = (Compartment)de.getOrigin();
            CompositeView parentView = (CompositeView)parent.getView();

            Rectangle bounds = (Rectangle)node.getView().getBounds().clone();
            bounds.grow(20, 20);
            CompositeView highlighter = new CompositeView();
            int n = actions.length;
            for(int i=0; i<n; i++)
            {
                int x = orientation == Y_AXIS ? bounds.x : bounds.x + bounds.width*i/n;
                int y = orientation == X_AXIS ? bounds.y : bounds.y + bounds.height*i/n;
                int width  = orientation == Y_AXIS ? bounds.width  : bounds.width/n  +1;
                int height = orientation == X_AXIS ? bounds.height : bounds.height/n +1;

                BoxView view = new BoxView(actions[i].pen, actions[i].brush, x, y, width, height);
                highlighter.add(view);
            }

            highlighter.setModel(this);
            parentView.insert(highlighter, lookupNodePosition(parentView, node));
        }
    }
}


