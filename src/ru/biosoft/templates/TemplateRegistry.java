package ru.biosoft.templates;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.util.ExtensionRegistrySupport;
import ru.biosoft.util.ObjectExtensionRegistry;
import ru.biosoft.util.TextUtil;


/**
 * Facade for templates operations.
 */
public class TemplateRegistry extends ExtensionRegistrySupport<TemplateInfo>
{
    public static final String FILTER_ELEMENT = "filter";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String NAME_ATTR = "name";
    public static final String FILE_ATTR = "file";
    public static final String DESCRIPTION_ATTR = "description";
    public static final String ISBRIEF_ATTR = "isBrief";
    public static final String ORDER_ATTR = "order";
    public static final String CLASS_ATTR = "class";
    public static final String SUBCLASSES_ATTR = "subclasses";
    public static final String METHOD_ATTR = "method";
    public static final String JAVASCRIPT_ATTR = "javascripts";
    public static final String VALUE_ATTR = "value";
    public static final String ISREGEXP_ATTR = "isRegexp";

    private static Logger log = Logger.getLogger(TemplateRegistry.class.getName());
    private static final TemplateRegistry instance = new TemplateRegistry();
    
    private static final ExtensionRegistrySupport<Object> contextItems = new ObjectExtensionRegistry<>(
            "ru.biosoft.templates.contextItem", NAME_ATTR, Object.class);
    
    static
    {
        try
        {
            Properties props = new Properties();
            props.setProperty("velocimacro.context.localscope", "true");
        
            props.setProperty("resource.loader", "class");
            props.setProperty("class.resource.loader.class", "ru.biosoft.templates.ClasspathResourceLoader");
            props.setProperty("class.resource.loader.cache", "false");
        
            props.setProperty("velocimacro.library", "resources/displayMacros.vm, resources/processMacros.vm");

            // experimental, possble fix for 
            // Runtime : ran out of parsers. Creating a new one.  Please increment the parser.pool.size property. The current value is too small.
            props.setProperty( "parser.pool.size", "50" );
        
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(ClassLoading.getClassLoader());
            synchronized( TemplateRegistry.class )
            {
                Velocity.init(props);
            }
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to initialize Velocity engine", e);
        }
    }
    
    public static void initialize()
    {
        // Just to perform static initialization
    }
    
    private TemplateRegistry()
    {
        super("ru.biosoft.templates.template", NAME_ATTR);
    }
    
    @Override
    protected TemplateInfo loadElement(IConfigurationElement element, String name) throws Exception
    {
        String filePath = getStringAttribute(element, FILE_ATTR);
        String description = element.getAttribute(DESCRIPTION_ATTR);
        boolean isBrief = getBooleanAttribute(element, ISBRIEF_ATTR);
        int order = getIntAttribute(element, ORDER_ATTR);
        TemplateFilter filter = null;
    
        IConfigurationElement[] filterElements = element.getChildren(FILTER_ELEMENT);
        if( filterElements.length > 0 )
        {
            String filterClass = getStringAttribute(filterElements[0], CLASS_ATTR);
            boolean filterSubclasses = getBooleanAttribute(filterElements[0], SUBCLASSES_ATTR);
    
            List<PropertyFilter> properties = new ArrayList<>();
            for( IConfigurationElement propertyElement : filterElements[0].getChildren(PROPERTY_ELEMENT) )
            {
                String propertyName = getStringAttribute(propertyElement, NAME_ATTR);
                String propertyValue = propertyElement.getAttribute(VALUE_ATTR);
                String propertyClass = propertyElement.getAttribute(CLASS_ATTR);
                boolean propertyIsRegexp = getBooleanAttribute(propertyElement, ISREGEXP_ATTR);
    
                properties.add(new PropertyFilter(propertyName, propertyValue, propertyClass, propertyIsRegexp));
            }
            String methodName = filterElements[0].getAttribute( METHOD_ATTR );
            filter = new TemplateFilter( filterClass, filterSubclasses, properties, methodName );
        }
    
        return new TemplateInfo(name, description, isBrief, element.getNamespaceIdentifier(), filePath, filter, order);
    }

    /**
     * Returns template info objects that are suitable
     * for the specified object.
     */
    public static @Nonnull TemplateInfo[] getSuitableTemplates(Object obj)
    {
        return instance.stream().filter( info -> info.isSuitable( obj ) ).sortedByInt( TemplateInfo::getOrder )
                .toArray( TemplateInfo[]::new );
    }

    /**
     * Apply template to data element
     */
    public static @Nonnull StringBuffer mergeTemplate(Object de, String templateName)
    {
        try
        {
            TemplateInfo templateInfo = instance.getExtension(templateName);

            if( templateInfo.isSuitable(de) )
            {
                Template template = templateInfo.getTemplate();
                return mergeTemplate(de, template);
            }
            return new StringBuffer("Template '"+templateInfo.getName()+"' is not suitable for "+de);
        }
        catch( Throwable t )
        {
            BiosoftVelocityException ex = new BiosoftVelocityException(t, templateName, de);
            ex.log();
            String[] message = TextUtil.split(ex.getMessage(), '\n');
            StringBuffer result = new StringBuffer();
            result.append("<div class='log_error'>").append(message[0]).append("</div>");
            for(int i=1; i<message.length; i++)
            {
                result.append("<div class='log_warning'>").append(message[i]).append("</div>");
            }
            return result;
        }
    }

    /**
     * Apply template to data element
     */
    public static @Nonnull StringBuffer mergeTemplate(Object de, Template template) throws Exception, IOException
    {
        VelocityContext context = new VelocityContext();
        
        contextItems.entries().prepend( "de", de ).forKeyValue( context::put );

        StringWriter sw = new StringWriter();
        template.merge( context, sw );
        return sw.getBuffer();
    }
}
