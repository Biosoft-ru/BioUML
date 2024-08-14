package ru.biosoft.table;

import java.beans.PropertyChangeEvent;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElementSupport;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * Sample is set of measerements for some individ, cell line/tissue or experimental condition
 * im microarray experiment. All mocroarray measerements belong to samples.
 *
 * Samples can be groupped using SamplesGroup class.
 *
 * @see SampleGroup
 */
public class Sample extends MutableDataElementSupport
{
    protected DynamicPropertySetProxy attributes = new DynamicPropertySetProxy();
    
    public Sample(DataCollection origin, String name)
    {
        super(origin, name);
    }
    
    public DynamicPropertySet getAttributes()
    {
        return attributes;
    }
    
    private static class DynamicPropertySetProxy extends DynamicPropertySetAsMap
    {
        @Override
        public void add(DynamicProperty property)
        {
            super.add(property);
            firePropertyChange(new PropertyChangeEvent(this, "", null, ""));
        }

        @Override
        public Object remove(String name)
        {
            Object result = super.remove(name);
            firePropertyChange(new PropertyChangeEvent(this, "", null, ""));
            return result;
        }
    }
}


