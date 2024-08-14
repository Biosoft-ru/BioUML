package ru.biosoft.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import com.developmentontheedge.beans.PropertyChangeObservable;


/**
 * This listener redirects all the events from observable to target while target object exists.
 * @author lan
 */
public class WeakPropertyChangeForwarder implements PropertyChangeListener
{
    private Reference<PropertyChangeListener> targetRef;
    private PropertyChangeObservable observable;
    
    public WeakPropertyChangeForwarder(PropertyChangeListener target, PropertyChangeObservable observable)
    {
        this.targetRef = new WeakReference<>( target );
        this.observable = observable;
        observable.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        PropertyChangeListener target = this.targetRef.get();
        if(target == null)
        {
            observable.removePropertyChangeListener(this);
        } else
        {
            target.propertyChange(evt);
        }
    }
}
