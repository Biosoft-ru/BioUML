package ru.biosoft.galaxy.javascript;

import java.util.logging.Level;
import java.util.Map.Entry;


import java.util.logging.Logger;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.galaxy.GalaxyAnalysisParameters;
import ru.biosoft.galaxy.GalaxyAnalysisParameters.GalaxyParameter;
import ru.biosoft.galaxy.GalaxyMethod;
import ru.biosoft.galaxy.GalaxyMethodInfo;
import ru.biosoft.galaxy.parameters.ArrayParameter;
import ru.biosoft.galaxy.parameters.Parameter;

public class GalaxyAnalysisRecord extends BaseFunction
{
    private static final long serialVersionUID = 1L;
    private GalaxyMethodInfo info;

    public GalaxyAnalysisRecord(GalaxyMethodInfo info)
    {
        this.info = info;
    }

    @Override
    public String getClassName()
    {
        return "GalaxyAnalysisRecord";
    }

    public String getName()
    {
        return info.getName();
    }

    public String getTitle()
    {
        return info.getDisplayName();
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
        return info.getDescriptionHTML();
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
    {
        if( args != null && args.length > 1 )
            throw new IllegalArgumentException("Too many arguments");
        if( args != null && args.length > 0 && ! ( args[0] instanceof Scriptable ) )
            throw new IllegalArgumentException("Argument must be a JavaScript object");
        Scriptable argument = ( args == null || args.length == 0 ) ? null : (Scriptable)args[0];
        GalaxyMethod method = (GalaxyMethod)info.createAnalysisMethod();
        GalaxyAnalysisParameters analysisParameters = (GalaxyAnalysisParameters)method.getParameters();
        final StringBuilder output = new StringBuilder();
        Logger logger = Logger.getLogger(String.valueOf(System.currentTimeMillis()) + Math.random());
        method.setLogger(logger);
        logger.setLevel(Level.ALL);
        //TODO:APPENDER
        /*WriterAppender appender = new WriterAppender(new SimpleLayout(), new Writer()
        {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException
            {
                output.append(cbuf, off, len);
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
        appender.setThreshold(Level.INFO);
        logger.addAppender(appender);*/
        if(argument != null)
        {
            for( Object id : argument.getIds() )
            {
                GalaxyParameter property = analysisParameters.findPropertyBySimpleName(id.toString());
                if( property == null )
                {
                    output.append("Unknown property " + id + "\n");
                    continue;
                }
                Object value = argument.get(id.toString(), scope);
                if(value instanceof NativeJavaObject)
                    value = ((NativeJavaObject)value).unwrap();
                if( value instanceof DataElement )
                    value = DataElementPath.create((DataElement)value);
                if( value instanceof NativeArray && property.getParameter() instanceof ArrayParameter )
                {
                    NativeArray arrayValue = (NativeArray)value;
                    ArrayParameter parameter = (ArrayParameter)property.getParameter();
                    parameter.setEntriesCount((int)arrayValue.getLength());
                    for( int i = 0; i < arrayValue.getLength(); i++ )
                    {
                        Scriptable scriptable = (Scriptable)arrayValue.get(i, scope);
                        for( Entry<String, Parameter> entry : parameter.getValues().get(i).entrySet() )
                        {
                            if( scriptable.has(entry.getKey(), scope) )
                            {
                                Object value2 = scriptable.get(entry.getKey(), scope);
                                if( value2 instanceof DataElement )
                                    value2 = DataElementPath.create((DataElement)value2);
                                entry.getValue().setValue(value2.toString());
                            }
                        }
                    }
                }
                else
                    property.setValueString(value.toString());
            }
        }
        method.validateParameters();
        try
        {
            method.justAnalyzeAndPut();
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }
        return output.toString();
    }
}
