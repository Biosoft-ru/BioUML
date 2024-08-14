package ru.biosoft.galaxy;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.util.ExtensionRegistrySupport;

/**
 * This registry allows to override some analyses settings
 * @author lan
 */
public class AnalysesOverridesRegistry extends ExtensionRegistrySupport<AnalysesOverridesRegistry.AnalysisProperties>
{
    private static final String ATTR_PATH = "path";
    private static final String ATTR_VISIBLE = "visible";
    private static final String ATTR_GROUP = "group";
    private static final String ATTR_NAME = "name";
    
    private static final AnalysesOverridesRegistry instance = new AnalysesOverridesRegistry();
    private static AnalysisProperties defaultProperties = new AnalysisProperties();
    
    public static class AnalysisProperties
    {
        private boolean visible = true;
        private Set<String> groups = new HashSet<>();
        
        public AnalysisProperties(boolean visible)
        {
            this.visible = visible;
        }

        /**
         * Constructor for default properties
         */
        public AnalysisProperties()
        {
        }

        public boolean isVisible()
        {
            return visible;
        }
        
        private void addGroup(String group)
        {
            groups.add( group );
        }
        
        public Set<String> getGroups()
        {
            return groups;
        }
    }
    
    private AnalysesOverridesRegistry()
    {
        super("ru.biosoft.galaxy.analyses", ATTR_PATH);
    }
    
    protected static AnalysisProperties getAnalysisOverrides(String path)
    {
        AnalysisProperties properties = instance.getExtension(path);
        return properties == null ? defaultProperties : properties;
    }
    
    protected static AnalysisProperties getAnalysisOverrides(GalaxyMethodInfo info)
    {
        return getAnalysisOverrides(info.getId());
    }

    @Override
    protected AnalysisProperties loadElement(IConfigurationElement element, String path) throws Exception
    {
        boolean visible = getBooleanAttribute(element, ATTR_VISIBLE, true);
        
        AnalysisProperties result = new AnalysisProperties(visible);
        
        String mainGroup = element.getAttribute(ATTR_GROUP);
        if(mainGroup != null)
        {
            result.addGroup( mainGroup );
        }
        
        for(IConfigurationElement groupElem : element.getChildren( ATTR_GROUP ))
        {
            String group = getStringAttribute( groupElem, ATTR_NAME );
            result.addGroup( group );
        }
        
        return result;
    }
}
