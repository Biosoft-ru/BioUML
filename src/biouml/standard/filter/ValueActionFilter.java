package biouml.standard.filter;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataElement;
import biouml.model.DiagramElement;

import com.developmentontheedge.beans.Option;

/**
 * Filter condition is specified as a set of {@link ActionValues}s.
 *
 * Virtual method <code>getValue</code> returns some string value for dataElement that is kernel
 * of corresponding diagram element. The filter looks to what valueActions corresponds
 * this value and returns some action that is composition of satisfying valueActions.
 *
 * The actions composition is defined as following:
 * <ul>
 *   <li>empty set - null is returned</li>
 *   <li>one action - this action will be returned</li>
 *   <li>if set contains one or more {@link Hide} actions, then <code>Hide.instance</hide>
 *       will be returned</li>
 *   <li>otherwise {@link ComplexHighlightAction} will be generated and returned.</li>
 *  </ul>
 *
 * Special empty value action is defined to process empty values.
 */
abstract public class ValueActionFilter extends Option implements ActionFilter
{
    public static final String EMPTY_VALUE = "_empty_";

    /**
     * Creates ValueActionFilter instance.
     *
     * @param parent - usually DiagramFilter instance
     * @param values - list of values for which corresponding value actions will be generated
     * @param useEmptyValue - indicates whether empty value should be used.
     * If true - empty value action will be inserted first in list of value actions.
     */
    public ValueActionFilter(Option parent, String[] values, boolean useEmptyValue)
    {
        super(parent);

        this.useEmptyValue = useEmptyValue;
        StreamEx<ValueAction> stream = StreamEx.of(values).map( val -> new ValueAction(this, val) );
        if(useEmptyValue)
            stream = stream.prepend( new ValueAction(this, EMPTY_VALUE) );
        valueActions = stream.toArray( ValueAction[]::new );
    }

    private final boolean useEmptyValue;
    public boolean isUseEmptyValue()
    {
        return useEmptyValue;
    }

    public abstract String getValue(DataElement de);

    private boolean enabled = false;
    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        boolean oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
    }

    private ValueAction[] valueActions;
    public ValueAction[] getValueAction()
    {
        return valueActions;
    }
    public ValueAction getValueAction(int i)
    {
        return valueActions[i];
    }

    public void setValueAction(ValueAction[] valueActions)
    {
        ValueAction[] oldValue = valueActions;
        this.valueActions = valueActions;

        firePropertyChange("valueAction", oldValue, valueActions);
    }

    public void setValueAction(int i, ValueAction valueAction)
    {
        ValueAction oldValue = valueActions[i];
        valueActions[i] = valueAction;

        firePropertyChange("valueAction", oldValue, valueAction);
    }

    /** used to calculate display name for child filters. */
    public String getItemDisplayName(Integer index, Object obj)
    {
        return ((ValueAction)obj).getValue();
    }

    @Override
    public Action getAction(DiagramElement de)
    {
        String value = getValue(de.getKernel());

        // empty value processing
        if( value==null || value.length() == 0 )
        {
            if( useEmptyValue && valueActions[0].isEnabled() )
                return valueActions[0].getAction();

            return null;
        }

        CompositeHighlightAction composite = null;
        Action action;
        for( ValueAction valueAction : valueActions )
        {
            if( valueAction.isEnabled() && value.indexOf(valueAction.getValue()) != -1 )
            {
                action = valueAction.getAction();
                if( action == HideAction.instance )
                    return HideAction.instance;

                if( action instanceof HighlightAction )
                {
                    if( composite == null )
                        composite = new CompositeHighlightAction();

                    composite.add((HighlightAction)action);
                }
            }
        }

        return composite;
    }
}
