package ru.biosoft.analysiscore;

import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.util.ExtensionRegistrySupport;

public class AnalysesGroupRegistry extends ExtensionRegistrySupport<AnalysesGroupRegistry.GroupItem>
{
    protected static final Logger log = Logger.getLogger( AnalysesGroupRegistry.class.getName() );

    public static final AnalysesGroupRegistry instance = new AnalysesGroupRegistry();

    public AnalysesGroupRegistry()
    {
        super( "ru.biosoft.analysiscore.group" );
    }

    @Override
    protected GroupItem loadElement(IConfigurationElement element, String elementName) throws Exception
    {
        String descriptionAttr = element.getAttribute( "description" );
        String description;
        if(descriptionAttr == null)
        {
            description = "";
        }
        else if(descriptionAttr.endsWith( ".html" ))
        {
            String pluginName = element.getNamespaceIdentifier();
            Bundle bundle = Platform.getBundle(pluginName);
            if( bundle == null )
            {
                log.log( Level.SEVERE, "Bundle not found: " + pluginName );
                description = "";
            }
            else
            {
                URL url = bundle.getResource( descriptionAttr );
                if( url == null )
                {
                    log.log( Level.SEVERE, "Resource not found: " + descriptionAttr );
                    description = "";
                }
                else
                {
                    InputStream stream = url.openStream();
                    description = ApplicationUtils.readAsString( stream );
                }
            }
        }
        else
        {
            description = descriptionAttr;
        }

        String relatedAttr = element.getAttribute( "related" );
        String[] relatedArray = relatedAttr == null ? new String[0] : relatedAttr.split( ";" );

        return new GroupItem( elementName, description, relatedArray );
    }

    public static class GroupItem
    {
        public final String name, description;
        public final String[] relatedGroups;

        public GroupItem(String name, String description, String[] relatedGroups)
        {
            this.name = name;
            this.description = description;
            this.relatedGroups = relatedGroups;
        }
    }
}
