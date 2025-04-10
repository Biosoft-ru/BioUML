package ru.biosoft.analysiscore.javascript;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import com.developmentontheedge.beans.editors.PropertyEditorEx;
import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.log.WriterHandler;

import ru.biosoft.jobcontrol.ClassJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.plugins.javascript.Global;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 *
 */
public class AnalysisFunction extends BaseFunction
{
    private final AnalysisMethodInfo info;
    private final String hostName;
    private final String name;
    private String help;

    public AnalysisFunction(String hostName, String name, AnalysisMethodInfo info)
    {
        this.hostName = hostName;
        this.name = name;
        this.info = info;
    }

    @Override
    public String getClassName()
    {
        return info.getAnalysisClass().getName();
    }

    @Override
    public void put(int index, Scriptable start, Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(String name, Scriptable start, Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(int index)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString()
    {
        if(this.help == null)
        {
            generateHelp();
        }
        ScriptEnvironment environment = Global.getEnvironment();
        if(environment != null)
            environment.showHtml(help);
        return help;
    }

    @Override
    public Object get(String name, Scriptable scope)
    {
        if(name.equals("help"))
        {
            String descriptionHTML = info.getDescriptionHTML();
            ScriptEnvironment environment = Global.getEnvironment();
            if(environment != null)
                environment.showHtml(descriptionHTML);
            return descriptionHTML;
        }
        return null;
    }

    protected String getAnalysisName()
    {
        return info.getName();
    }

    private synchronized void generateHelp()
    {
        help = new StringBuilder().append("String <b>").append(getQualifiedName()).append("</b>({parameters})<br>Launches analysis '").append(info.getName())
                .append("'<br><br><b>Parameters:</b><br>Parameters must be specified in {key: value} manner. The following keys are defined:<ul>")
                .append(getRecursivePropertyDescription("", ComponentFactory.getModel(info.createAnalysisMethod().getParameters())))
                .append("</ul><b>Returns:</b> Informational or error messages produced by an analysis.<br>To get analysis help use <b>")
                .append(getQualifiedName()).append(".help</b><br>").toString();
    }

    private String getRecursivePropertyDescription(String prefix, Property model)
    {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<model.getPropertyCount(); i++)
        {
            Property property = model.getPropertyAt(i);
            if(!(property.getDescriptor() instanceof PropertyDescriptor)) continue;
            PropertyDescriptor descriptor = (PropertyDescriptor)property.getDescriptor();
            if(descriptor.getWriteMethod() == null) continue;
            String subProperty = "";
            if(property instanceof CompositeProperty && !property.isHideChildren() && !property.getValueClass().getPackage().getName().startsWith("java."))
            {
                subProperty = getRecursivePropertyDescription(prefix+property.getName()+"/", property);
            }
            if(!subProperty.isEmpty())
            {
                sb.append(subProperty);
            } else
            {
                sb.append("<li><b>").append(prefix).append(property.getName()).append("</b>");
                String[] tags = null;
                try
                {
                    PropertyEditor editor = (PropertyEditor)property.getPropertyEditorClass().newInstance();
                    if(editor instanceof PropertyEditorEx)
                    {
                        ( (PropertyEditorEx)editor ).setBean(property.getOwner());
                        ( (PropertyEditorEx)editor ).setDescriptor(descriptor);
                    }
                    editor.setValue(property.getValue());
                    tags = editor.getTags();
                }
                catch(Exception e)
                {
                }
                String type = "String";
                if(tags == null)
                {
                    if(property.getValueClass().equals(Float.class) || property.getValueClass().equals(Double.class)) type="float";
                    else if(property.getValueClass().equals(Integer.class) || property.getValueClass().equals(Long.class)) type="int";
                    else if(property.getValueClass().equals(Boolean.class)) type="boolean";
                    else if(property.getValueClass().equals(DataElementPath.class))
                    {
                        if(property.getBooleanAttribute(DataElementPathEditor.ELEMENT_MUST_EXIST))
                        {
                            Object elementClassObj = descriptor.getValue(DataElementPathEditor.ELEMENT_CLASS);
                            if( elementClassObj instanceof Class<?> )
                                type = ((Class<? extends DataElement>)elementClassObj).getName();
                        }
                    }
                }
                if(property.getValueClass().isArray() || property.getValueClass().equals(DataElementPathSet.class)) type="String[]";
                sb.append(" (").append(type).append(") &ndash; ").append(property.getShortDescription());
                if(tags != null && tags.length > 0)
                {
                    sb.append("<br>Available values:<ul>");
                    for( String tag : tags )
                    {
                        sb.append("<li>").append(tag);
                    }
                    sb.append("</ul>");
                }
            }
        }
        return sb.toString();
    }

    private String getQualifiedName()
    {
        return hostName == null ? name : hostName+"."+name;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
    {
        if( args != null && args.length > 2 )
            throw new IllegalArgumentException("Too many arguments");
        final boolean verbose = args != null && args.length >= 2 && args[1] instanceof Boolean && (Boolean)args[1];
        if( args != null && args.length > 0 && ! ( args[0] instanceof Scriptable ) )
            throw new IllegalArgumentException("Argument must be a JavaScript object");
        Scriptable argument = ( args == null || args.length == 0 ) ? null : (Scriptable)args[0];
        AnalysisMethod method = info.createAnalysisMethod();
        AnalysisParameters analysisParameters = method.getParameters();
        final ScriptEnvironment environment = Global.getEnvironment(scope);
        boolean hasErrors = false;
        final Logger logger = Logger.getLogger(String.valueOf(System.currentTimeMillis()) + Math.random());
        method.setLogger(logger);
        logger.setLevel(Level.ALL);
        Handler handler = new WriterHandler( new Writer()
        {
            StringBuilder lastLine = new StringBuilder();

            @Override
            public synchronized void write(char[] cbuf, int off, int len) throws IOException
            {
                if(environment != null)
                {
                    lastLine.append(cbuf, off, len);
                    int pos = lastLine.lastIndexOf("\n");
                    if(pos >= 0)
                    {
                        String string = lastLine.toString();
                        environment.print(string.substring(0, pos).replace("\r", ""));
                        lastLine = new StringBuilder(string.substring(pos+1));
                    }
                }
            }

            @Override
            public void flush() throws IOException
            {
            }

            @Override
            public void close() throws IOException
            {

            }
        });
        handler.setLevel( Level.INFO );
        logger.addHandler( handler );
        ComponentModel model = ComponentFactory.getModel(analysisParameters);
        if(argument != null)
        {
            for( Object id : argument.getIds() )
            {
                Property property = model.findProperty(id.toString());
                if( property == null )
                {
                    hasErrors = true;
                    if(environment != null)
                        environment.error("Unknown parameter " + id);
                    continue;
                }
                try
                {
                    Object value = argument.get(id.toString(), scope);
                    if( value instanceof NativeJavaObject )
                        value = ((NativeJavaObject)value).unwrap();
                    if( value instanceof DataElement )
                        value = DataElementPath.create((DataElement)value);
                    else if( value instanceof NativeArray && DataElementPathSet.class.isAssignableFrom(property.getValueClass()) )
                    {
                        DataElementPathSet newValue = new DataElementPathSet();
                        NativeArray arrayValue = (NativeArray)value;
                        for( int i = 0; i < arrayValue.getLength(); i++ )
                        {
                            newValue.add(DataElementPath.create(arrayValue.get(i, scope).toString()));
                        }
                        value = newValue;
                    }
                    if( value instanceof NativeArray && property instanceof ArrayProperty )
                    {
                        NativeArray arrayValue = (NativeArray)value;
                        ArrayProperty arrayProperty = (ArrayProperty)property;
                        Object newValue = Array.newInstance(arrayProperty.getItemClass(), (int)arrayValue.getLength());
                        for( int i = 0; i < arrayValue.getLength(); i++ )
                        {
                            Object value2 = arrayValue.get(i, scope);
                            if( value2 instanceof DataElement )
                                value2 = DataElementPath.create((DataElement)value2);
                            Array.set(newValue, i, TextUtil2.fromString(arrayProperty.getItemClass(), value2.toString()));
                        }
                    }
                    else if(value == null)
                        property.setValue( null );
                    else
                        property.setValue(TextUtil2.fromString(property.getValueClass(), value.toString()));
                }
                catch( Exception e )
                {
                    hasErrors = true;
                    if(environment != null)
                        environment.error("Cannot set parameter " + id + ": " + ExceptionRegistry.log(e));
                }
            }
        }
        if(hasErrors) return null;
        try
        {
            method.setLogger(logger);
            ClassJobControl jobControl = method.getJobControl();
            jobControl.addListener(new JobControlListenerAdapter()
            {
                @Override
                public void jobTerminated(JobControlEvent event)
                {
                    if( event.getStatus() == JobControl.TERMINATED_BY_ERROR )
                        logger.log( Level.SEVERE, event.getMessage() );
                }

                @Override
                public void valueChanged(JobControlEvent event)
                {
                    if(verbose && environment != null)
                        environment.print("$Percent$ = "+event.getPreparedness());
                }
            });
            jobControl.run();
        }
        catch( Exception e )
        {
            logger.log( Level.SEVERE, e.getMessage() );
        }
        return null;
    }
}
