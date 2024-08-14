package biouml.plugins.sbml.extensions;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.util.ExtensionRegistrySupport;

public class SbmlAnnotationRegistry extends ExtensionRegistrySupport<SbmlAnnotationRegistry.SbmlAnnotationInfo>
{
    public static final String NAME = "name";
    public static final String NAMESPACE = "namespace";
    public static final String EXTENSION_CLASS = "extension_class";
    public static final String PRIORITY = "priority";

    private static boolean pluginsMode = true;

    public static class SbmlAnnotationInfo
    {
        public SbmlAnnotationInfo(Class<? extends SbmlExtension> extensionClass, String namespace, int priority)
        {
            this.extensionClass = extensionClass;
            this.namespace = namespace;
            this.priority = priority;
        }

        protected Class<? extends SbmlExtension> extensionClass;
        protected String namespace;

        public SbmlExtension create() throws Exception
        {
            return extensionClass.newInstance();
        }
        public String getNamespace()
        {
            return namespace;
        }

        public int priority;
        public int getPriority()
        {
            return priority;
        }
    }

    ///////////////////////////////////////////////////////////////////
    private static final SbmlAnnotationRegistry instance = new SbmlAnnotationRegistry();
    
    private SbmlAnnotationRegistry()
    {
        super("biouml.plugins.sbml.annotation", NAMESPACE);
    }
    
    public static Set<String> getNamespaces()
    {
        return instance.names().toSet();
    }

    public static List<SbmlAnnotationInfo> getAnnotations()
    {
        if( pluginsMode )
        {
            try
            {
                return instance.stream().toList();
            }
            catch( NoClassDefFoundError err )
            {
                pluginsMode = false;
            }
        }
        return null;
    }

    @Override
    protected SbmlAnnotationInfo loadElement(IConfigurationElement element, String namespace) throws Exception
    {
        int priority = getIntAttribute(element, PRIORITY, 100);
        Class<? extends SbmlExtension> clazz = getClassAttribute(element, EXTENSION_CLASS, SbmlExtension.class);
        return new SbmlAnnotationInfo(clazz, namespace, priority);
    }
}
