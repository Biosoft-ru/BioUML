package ru.biosoft.access.log;

import java.util.concurrent.atomic.AtomicReference;

import one.util.streamex.IntStreamEx;

public class DefaultBiosoftLogger implements BiosoftLogger
{
    private final AtomicReference<BiosoftLoggerListener[]> listeners = new AtomicReference<>();
    
    private void fireEvent(EventType type, String msg)
    {
        BiosoftLoggerListener[] listeners = this.listeners.get();
        if(listeners != null)
        {
            for(BiosoftLoggerListener listener : listeners)
            {
                listener.messageAdded( type, msg );
            }
        }
    }

    @Override
    public void info(String msg)
    {
        fireEvent( EventType.INFO, msg );
    }

    @Override
    public void warn(String msg)
    {
        fireEvent( EventType.WARN, msg );
    }

    @Override
    public void error(String msg)
    {
        fireEvent( EventType.ERROR, msg );
    }

    @Override
    public void addListener(BiosoftLoggerListener listener)
    {
        while(true)
        {
            BiosoftLoggerListener[] listeners = this.listeners.get();
            BiosoftLoggerListener[] newListeners;
            if(listeners != null)
            {
                newListeners = new BiosoftLoggerListener[listeners.length+1];
                System.arraycopy( listeners, 0, newListeners, 0, listeners.length );
                newListeners[listeners.length] = listener;
            } else
            {
                newListeners = new BiosoftLoggerListener[] {listener};
            }
            if(this.listeners.compareAndSet( listeners, newListeners ))
            {
                return;
            }
        }
    }

    @Override
    public void removeListener(BiosoftLoggerListener listener)
    {
        while(true)
        {
            BiosoftLoggerListener[] listeners = this.listeners.get();
            if(listeners == null)
            {
                return;
            }
            int pos = IntStreamEx.ofIndices( listeners, listener::equals ).findAny().orElse( -1 );
            if(pos == -1)
            {
                return;
            }
            BiosoftLoggerListener[] newListeners;
            newListeners = new BiosoftLoggerListener[listeners.length - 1];
            System.arraycopy( listeners, 0, newListeners, 0, pos );
            System.arraycopy( listeners, pos + 1, newListeners, pos, newListeners.length - pos );
            if(this.listeners.compareAndSet( listeners, newListeners ))
            {
                return;
            }
        }
    }
}
