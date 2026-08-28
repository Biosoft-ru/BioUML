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
import org.apache.velocity.runtime.RuntimeSingleton;
import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.util.ExtensionRegistrySupport;
import ru.biosoft.util.ObjectExtensionRegistry;
import ru.biosoft.util.TextUtil2;


/**
 * Facade for templates operations.
 */
public class TemplateRegistry extends ExtensionRegistrySupport<TemplateInfo>
{
    /**
     * Number of parsers kept in the Velocity parser pool.
     * <p>
     * Applied both to the shared singleton engine (see
     * {@link #initVelocity()}) and to the independent
     * <code>VelocityEngine</code> instances created by the simulation
     * engines — keep those in sync via this constant.
     */
    public static final int VELOCITY_PARSER_POOL_SIZE = 200;

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
        initVelocity();
    }

    /**
     * Initializes the shared Velocity engine.
     * <p>
     * The singleton engine's parser pool (default size 25,
     * <code>parser.pool.size</code>) is used by every
     * <code>RuntimeSingleton.parse()</code> call. When more templates are
     * parsed concurrently than the pool can hold, Velocity logs
     * "Runtime : ran out of parsers. Creating a new one. Please increment the
     * parser.pool.size property." for each excess thread, so the pool size is
     * raised here to accommodate the observed level of concurrent template
     * parsing.
     * <p>
     * Note: in Velocity 1.7 <code>RuntimeSingleton.init()</code> re-runs the
     * full runtime initialization on every call, which would discard these
     * properties. Callers that already parsed a template via the singleton
     * therefore only need to call this before their first parse, and repeated
     * calls must not reset a correctly configured engine — see the
     * <code>isInitialized()</code> guard.
     */
    public static synchronized void initVelocity()
    {
        if( RuntimeSingleton.isInitialized() )
        {
            return;
        }
        try
        {
            Properties props = new Properties();
            props.setProperty("velocimacro.context.localscope", "true");

            props.setProperty("resource.loader", "class");
            props.setProperty("class.resource.loader.class", "ru.biosoft.templates.ClasspathResourceLoader");
            props.setProperty("class.resource.loader.cache", "false");

            props.setProperty("velocimacro.library", "resources/displayMacros.vm, resources/processMacros.vm");

            props.setProperty("parser.pool.size", String.valueOf(VELOCITY_PARSER_POOL_SIZE));

            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(ClassLoading.getClassLoader());
            try
            {
                Velocity.init(props);
            }
            finally
            {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to initialize Velocity engine", e);
        }
    }

    public static void initialize()
    {
        initVelocity();
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
            String[] message = TextUtil2.split(ex.getMessage(), '\n');
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
