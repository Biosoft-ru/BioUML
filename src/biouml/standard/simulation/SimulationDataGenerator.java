package biouml.standard.simulation;

import java.beans.PropertyChangeListener;

import ru.biosoft.access.core.DataElement;

/**
 * @author lan
 *
 */
public interface SimulationDataGenerator extends DataElement
{
    public int getPointsCount();
    public double getValue(int point) throws Exception;
    
    public void addPropertyChangeListener(PropertyChangeListener l);
    public void removePropertyChangeListener(PropertyChangeListener l);
}
