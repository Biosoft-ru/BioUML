package biouml.standard.simulation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;


/**
 * @author lan
 *
 */
public abstract class SimulationDataGeneratorSupport implements SimulationDataGenerator
{
    protected List<PropertyChangeListener> listenersList = new ArrayList<>();

    protected void notifyListeners()
    {
        PropertyChangeEvent event = new PropertyChangeEvent(this, "", null, null);
        for(PropertyChangeListener listener: listenersList)
        {
            listener.propertyChange(event);
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        listenersList.add(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        listenersList.remove(l);
    }

}
