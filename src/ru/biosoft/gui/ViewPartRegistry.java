package ru.biosoft.gui;

import java.util.List;

import javax.swing.Action;

import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.util.ExtensionRegistrySupport;

public class ViewPartRegistry extends ExtensionRegistrySupport<ViewPart>
{
    private static final ViewPartRegistry instance = new ViewPartRegistry();
    
    private ViewPartRegistry()
    {
        super( "biouml.workbench.diagramViewPart", "id" );
    }

    @Override
    protected ViewPart loadElement(IConfigurationElement element, String elementName) throws Exception
    {
        ViewPart viewPart = getClassAttribute( element, "class", ViewPart.class ).newInstance();
        // configure view part action
        String[] attributes = element.getAttributeNames();
        Action action = viewPart.getAction();
        for( String attribute : attributes )
            action.putValue(attribute, element.getAttribute(attribute));

        // post process some settings
        if( action.getValue(ViewPart.PRIORITY) != null )
        {
            String priority = (String)action.getValue(ViewPart.PRIORITY);
            action.putValue(ViewPart.PRIORITY, Float.valueOf(priority));
        }
        
        return viewPart;
    }
    
    public static List<ViewPart> getViewParts()
    {
        return instance.stream().toList();
    }
    
    public static ViewPart getViewPart(String id)
    {
        return instance.getExtension(id);
    }
}
