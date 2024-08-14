package ru.biosoft.access;

import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access.core.Environment;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.util.ClassExtensionRegistry;

public class QuerySystemRegistry
{
    protected static final ClassExtensionRegistry<QuerySystem> possibleQuerySystemsRegistry = new ClassExtensionRegistry<>(
            "ru.biosoft.access.querySystem", QuerySystem.class );
    public static void initQuerySystems()
    {
        Map<String, Class<? extends QuerySystem>> possibleQuerySystems = new HashMap<>();
        possibleQuerySystemsRegistry.names().forEach( qsClass -> {
            possibleQuerySystems.put( qsClass, possibleQuerySystemsRegistry.getExtension( qsClass ) );
        } );
        Environment.setQuerySystemRegistry( possibleQuerySystems );

    }
}
