package biouml.model;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.util.DPSUtils;

import com.developmentontheedge.beans.DynamicProperty;

/**
 * If extension point exists this interface must be implemented.
 */
public abstract class NodeViewBuilder implements DataCollectionListener, PropertyChangeListener
{
    protected static final String NODE_VIEW_BUILDER = "nodeViewBuilder";

    public @Nonnull abstract CompositeView createNodeView(Node node, DiagramViewOptions options, Graphics g);

    public abstract boolean isApplicable(Node node);

    public abstract boolean isApplicable(Diagram diagram);

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        if( e.getDataElement() instanceof Node )
        {
            Node node = (Node)e.getDataElement();
            if( isApplicable(node) )
                applyNodeViewBuilder(node);
        }
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
    }

    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
    }

    public void applyNodeViewBuilder(Compartment compartment)
    {
        compartment.recursiveStream().select( Node.class ).filter( this::isApplicable ).forEach( this::applyNodeViewBuilder );
    }

    public void applyNodeViewBuilder(Node node)
    {
        DynamicProperty dp = new DynamicProperty(NODE_VIEW_BUILDER, NodeViewBuilder.class, this);
        dp.setHidden(true);
        DPSUtils.makeTransient(dp);
        node.getAttributes().add(dp);
    }
}
