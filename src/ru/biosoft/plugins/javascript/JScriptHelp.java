package ru.biosoft.plugins.javascript;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.application.ApplicationUtils;

public class JScriptHelp
{
    protected static final Logger log = Logger.getLogger(JScriptHelp.class.getName());

    public static final String PROPERTY_ELEMENT = "property";
    public static final String FUNCTION_ELEMENT = "function";
    public static final String ARGUMENT_ELEMENT = "argument";
    public static final String RETURNS_ELEMENT = "returns";
    public static final String THROWS_ELEMENT = "throws";
    public static final String EXAMPLE_ELEMENT = "example";
    public static final String SIGNATURE_ELEMENT = "signature";

    public static final String NAME_ATTR = "name";
    public static final String TYPE_ATTR = "type";
    public static final String READ_ONLY_ATTR = "readOnly";
    public static final String DESCRIPTION_ATTR = "description";
    public static final String OBLIGATORY_ATTR = "obligatory";
    public static final String CODE_ATTR = "code";

    ////////////////////////////////////////////////////////////////////////////
    // Help map issues
    //

    private static final Map<Scriptable, Object> infoElementsMap = new WeakHashMap<>();

    public static String getHelpValue(Scriptable scriptable)
    {
        synchronized(infoElementsMap)
        {
            Object infoElement = infoElementsMap.get(scriptable);
            return infoElement == null ? null : infoElement.toString();
        }
    }

    public static void setInfoElement(Scriptable scriptable, Object help)
    {
        synchronized(infoElementsMap)
        {
            infoElementsMap.put(scriptable, help);
        }
    }

    private static final List<String> functionDescriptionList = new ArrayList<>();
    public static String getFunctionDescriptionList()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<h2 align='center'>Functions</h2><ul>\n");
        for( String str : functionDescriptionList )
        {
            buffer.append("<li>");
            buffer.append(str);
            buffer.append("\n");
        }
        buffer.append("</ul>");
        return buffer.toString();
    }
    public static void addFunctionDescription(String str)
    {
        if( !functionDescriptionList.contains(str) )
        {
            functionDescriptionList.add(str);
        }
    }

    private static final List<String> objectDescriptionList = new ArrayList<>();
    public static String getObjectDescriptionList()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<h2 align='center'>Objects</h2><ul>\n");
        for( String str : objectDescriptionList )
        {
            buffer.append("<li>");
            buffer.append(str);
            buffer.append("\n");
        }
        buffer.append("</ul>");
        return buffer.toString();
    }
    public static void addObjectDescription(String str)
    {
        if( !objectDescriptionList.contains(str) )
        {
            objectDescriptionList.add(str);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Read info from plugin.xml
    //

    public static void readHostObjectInfo(HostObjectInfo info, Scriptable host, IConfigurationElement element)
    {
        try
        {
            info.setDescription(element.getAttribute(TYPE_ATTR));
            info.setDescription(element.getAttribute(DESCRIPTION_ATTR));
            if( info.getType() == null || info.getType().length() == 0 )
                info.setType("ScriptableObject");

            // read properties
            IConfigurationElement[] elements = element.getChildren(PROPERTY_ELEMENT);
            for( IConfigurationElement propertyElement : elements )
            {

                String name = propertyElement.getAttribute(NAME_ATTR);
                if( name == null || name.length() == 0 )
                    log.log(Level.SEVERE, "Property name is not specified, host object=" + info.getName() + ".");
                else
                {
                    PropertyInfo property = new PropertyInfo(name, info);
                    Scriptable scope = (Scriptable)host.get(name, host);
                    if( scope == null )
                    {
                        log.log(Level.SEVERE, "Undefined host object property '" + name + "', host object=" + info.getName() + ".");
                    }
                    else
                    {
                        readPropertyInfo(property, scope, propertyElement);
                        info.put(property);
                    }
                }
            }

            // read functions
            elements = element.getChildren(FUNCTION_ELEMENT);
            for( int i = 0; i < elements.length; i++ )
            {
                final IConfigurationElement functionElement = elements[i];

                String name = functionElement.getAttribute(NAME_ATTR);
                if( name == null || name.length() == 0 )
                    log.log(Level.SEVERE, "Function name is not specified, host object=" + info.getName() + ".");
                else
                {
                    Method method = null;
                    int nParam = -1;
                    for( Method hostObjectMethod : info.getObjectClass().getMethods() )
                    {
                        if( hostObjectMethod.getName().equals(name) && hostObjectMethod.getParameterTypes().length > nParam )
                        {
                            method = hostObjectMethod;
                            nParam = hostObjectMethod.getParameterTypes().length;
                        }
                    }
                    final Method finalMethod = method;
                    Object scope = host.get( name, host );
                    if( !(scope instanceof Scriptable) )
                    {
                        log.log(Level.SEVERE, "Undefined host object function '" + name + "', host object=" + info.getName() + ".");
                    }
                    else
                    {
                        FunctionInfo function = new FunctionInfo(name, info)
                        {
                            @Override
                            protected void doInit()
                            {
                                readFunctionInfo( this, null, functionElement, finalMethod );
                            }
                        };
                        setInfoElement((Scriptable)scope, function);
                        info.put(function);
                    }
                }
            }

            setInfoElement(host, info);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not read info for host object " + info.getName() + ", error: " + t.getMessage(), t);
        }
    }

    public static void readPropertyInfo(PropertyInfo info, Scriptable scope, IConfigurationElement element)
    {
        try
        {
            info.setDescription(element.getAttribute(TYPE_ATTR));
            if( info.getType() == null || info.getType().length() == 0 )
                info.setType("Object");

            info.setDescription(element.getAttribute(DESCRIPTION_ATTR));

            if( "true".equals(element.getAttribute(READ_ONLY_ATTR)) )
                info.setReadOnly(true);

            setInfoElement(scope, info);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not read info for property " + info.getName() + ", error: " + t.getMessage(), t);
        }
    }

    public static void readFunctionInfo(FunctionInfo info, Scriptable scope, IConfigurationElement[] elements)
    {
        if( elements == null || elements.length == 0 )
            return;

        if( elements.length > 1 )
            log.warning("Function info element is duplicated, only first element will be used, function '" + info.getName());

        readFunctionInfo(info, scope, elements[0], null);
    }

    public static void readFunctionInfo(FunctionInfo info, Scriptable scope, IConfigurationElement docElement, Method method)
    {
        try
        {
            info.setDescription(docElement.getAttribute(DESCRIPTION_ATTR));

            IConfigurationElement[] elements = docElement.getChildren(SIGNATURE_ELEMENT);
            if( elements == null || elements.length == 0 )
            {
                elements = new IConfigurationElement[] {docElement};
            }

            Object parameters = null;
            ComponentModel parametersModel = null;
            if( method != null )
            {
                try
                {
                    JSAnalysis annotation = method.getAnnotation(JSAnalysis.class);
                    if( annotation != null )
                    {
                        Object analysis = annotation.value().getConstructor(DataCollection.class, String.class).newInstance(null, "");
                        Method getParametersMethod = analysis.getClass().getMethod("getParameters", new Class[0]);
                        parameters = getParametersMethod.invoke(analysis, new Object[0]);
                        parametersModel = ComponentFactory.getModel(parameters);
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            // load arguments description
            for( IConfigurationElement element : elements )
            {
                IConfigurationElement[] argElements = element.getChildren(ARGUMENT_ELEMENT);
                if( argElements != null && argElements.length > 0 )
                {
                    FunctionInfo.Argument[] args = new FunctionInfo.Argument[argElements.length];
                    Set<String> usedProperties = new HashSet<>();
                    for( int i = 0; i < argElements.length; i++ )
                    {
                        boolean obligatory = argElements[i].getAttribute(OBLIGATORY_ATTR) == null
                                || !argElements[i].getAttribute(OBLIGATORY_ATTR).equals("false");
                        String type = argElements[i].getAttribute(TYPE_ATTR);
                        if( type == null && method != null )
                            type = method.getParameterTypes()[i].getSimpleName();
                        String description = argElements[i].getAttribute(DESCRIPTION_ATTR);
                        if( description == null && method != null )
                        {
                            for( Annotation annotation : method.getParameterAnnotations()[i] )
                            {
                                if( annotation instanceof JSDescription )
                                {
                                    description = ( (JSDescription)annotation ).value();
                                    break;
                                }
                            }
                        }
                        if( description == null && parametersModel != null )
                        {
                            for( Annotation annotation : method.getParameterAnnotations()[i] )
                            {
                                if( annotation instanceof JSProperty )
                                {
                                    String propertyName = ( (JSProperty)annotation ).value();
                                    usedProperties.add(propertyName);
                                    Property property = parametersModel.findProperty(propertyName);
                                    if( property != null )
                                    {
                                        description = property.getDescriptor().getShortDescription();
                                    }
                                    break;
                                }
                            }
                        }
                        if( method != null && method.getParameterTypes()[i].equals(Scriptable.class) )
                        {
                            StringBuilder desc = new StringBuilder(
                                    "Object containing one or several optional parameters listed below.\n<ul>");
                            for( int j = 0; j < parametersModel.getPropertyCount(); j++ )
                            {
                                Property property = parametersModel.getPropertyAt(j);
                                if( !property.isVisible(Property.SHOW_EXPERT) || usedProperties.contains(property.getName()) )
                                    continue;
                                desc.append("<li>").append(property.getName()).append(": ").append(property.getShortDescription());
                                if( !property.getShortDescription().endsWith(".") )
                                    desc.append(".");
                                Object value = property.getValue();
                                desc.append(" Default: ").append(value == null ? "null" : value.toString());
                                desc.append("</li>\n");
                            }
                            desc.append("</ul>");
                            description = desc.toString();
                        }
                        args[i] = new FunctionInfo.Argument(argElements[i].getAttribute(NAME_ATTR), type, description);

                        args[i].setObligatory(obligatory);
                    }

                    info.addArguments(args);
                }
                else
                {
                    info.addArguments(new FunctionInfo.Argument[0]);
                }
            }

            // returned type and value
            elements = docElement.getChildren(RETURNS_ELEMENT);
            if( elements != null && elements.length > 0 )
            {
                if( elements.length > 1 )
                    log.warning("Function info error: function can return only one object, function '" + info.getName());

                IConfigurationElement element = elements[0];
                info.returnedValue.setType(element.getAttribute(TYPE_ATTR));
                info.returnedValue.setDescription(element.getAttribute(DESCRIPTION_ATTR));
            }

            // exceptions
            IConfigurationElement[] throwsElements = docElement.getChildren(THROWS_ELEMENT);
            if( throwsElements != null && throwsElements.length > 0 )
            {
                FunctionInfo.ExceptionInfo[] exceptions = new FunctionInfo.ExceptionInfo[throwsElements.length];
                for( int i = 0; i < throwsElements.length; i++ )
                {
                    exceptions[i] = new FunctionInfo.ExceptionInfo(throwsElements[i].getAttribute(TYPE_ATTR),
                            throwsElements[i].getAttribute(DESCRIPTION_ATTR));
                }

                info.setExceptions(exceptions);
            }

            // code examples
            IConfigurationElement[] exampleElements = docElement.getChildren(EXAMPLE_ELEMENT);
            if( exampleElements != null && exampleElements.length > 0 )
            {
                FunctionInfo.Example[] examples = new FunctionInfo.Example[exampleElements.length];
                for( int i = 0; i < exampleElements.length; i++ )
                {
                    examples[i] = new FunctionInfo.Example(exampleElements[i].getAttribute(CODE_ATTR),
                            exampleElements[i].getAttribute(DESCRIPTION_ATTR));
                }

                info.setExamples(examples);
            }
            if( scope != null )
                setInfoElement( scope, info );
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not read info for function " + info.getName() + ", error: " + t.getMessage(), t);
        }
    }

    public static String getJavaDoc(Object scriptable)
    {
        if( ! ( scriptable instanceof NativeJavaObject ) )
        {
            return null;
        }
        Object obj = ( (NativeJavaObject)scriptable ).unwrap();
        Class<?> clazz = obj instanceof Class?(Class<?>)obj:obj.getClass();
        String className = clazz.getName();
        String applicationPath = System.getProperty("biouml.server.path");
        if( applicationPath == null )
        {
            applicationPath = System.getProperty("user.dir");
        }
        File javaDocPath = new File(applicationPath, "javadoc");
        File docFile = new File(javaDocPath, className.replace('.', '/') + ".html");
        if(!docFile.exists())
        {
            String pluginName = ClassLoading.getPluginForClass( className );
            javaDocPath = new File(Platform.getBundle(pluginName).getLocation(), "javadoc");
            docFile = new File(javaDocPath, className.replace('.', '/') + ".html");
        }
        if(!docFile.exists()) return null;
        try
        {
            return ApplicationUtils.readAsString(docFile);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not load javadoc file", e);
        }
        return null;
    }
}
