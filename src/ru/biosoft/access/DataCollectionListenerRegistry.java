package ru.biosoft.access;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.Environment;
import ru.biosoft.util.ClassExtensionRegistry;

public class DataCollectionListenerRegistry
{
    protected static final ClassExtensionRegistry<DataCollectionListener> possibleListeners = new ClassExtensionRegistry<>(
            "ru.biosoft.access.dataCollectionListener", DataCollectionListener.class );

    public static void initDataCollectionListeners()
    {
        Map<String, Class<? extends DataCollectionListener>> listeners = new HashMap<>();
        possibleListeners.names().forEach( lClass -> {
            listeners.put( lClass, possibleListeners.getExtension( lClass ) );
        } );
        Environment.setDataCollectionListenersRegistry( listeners );

    }
}
