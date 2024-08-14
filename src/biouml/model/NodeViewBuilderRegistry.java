package biouml.model;

import java.util.List;

import ru.biosoft.util.ObjectExtensionRegistry;

public class NodeViewBuilderRegistry
{
    private static ObjectExtensionRegistry<NodeViewBuilder> registry;
    private static boolean pluginsMode = true;

    public static List<NodeViewBuilder> getBuilders()
    {
        if( pluginsMode )
        {
            try
            {
                if( registry == null )
                    registry = new ObjectExtensionRegistry<>( "biouml.workbench.nodeViewBuilder", "name", NodeViewBuilder.class );
                return registry.stream().toList();
            }
            catch( NoClassDefFoundError err )
            {
                pluginsMode = false;
            }
        }
        return null;
    }
}
