package ru.biosoft.plugins.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.script.DerivedScriptEnvironment;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCacheManager;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

@ClassIcon("resources/script.gif")
@PropertyName("script")
public class JSElement extends ScriptDataElement
{
    private static final String JAVA_SCRIPT_SCOPE_PROPERTY = "properties/JavaScript/scope";

    public JSElement(DataCollection<?> parent, String name, String data)
    {
        super(name, parent, data);
    }

    @Override
    public String getContentType()
    {
        return "text/javascript";
    }

    @Override
    protected ScriptJobControl createJobControl(String content, ScriptEnvironment env, Map<String, Object> scope, Map<String, Object> outVars,
            boolean sessionContext)
    {
        return new JSElementJobControl(content, sessionContext, scope, env, outVars);
    }

    @Override
    protected void handleException(ScriptEnvironment env, Throwable ex)
    {
        if( ex instanceof RhinoException )
        {
            env.error(ex.getMessage());
        }
        else
        {
            super.handleException( env, ex );
        }
    }

    private static Global createTopLevelScope(Context context)
    {
        Global topLevelScope = new Global(context);
        JScriptVisiblePlugin jsPlugin = JScriptVisiblePlugin.getInstance();
        if( jsPlugin != null )
            JScriptContext.loadExtensions(jsPlugin, context, topLevelScope);
        return topLevelScope;
    }

    class JSElementJobControl extends ScriptJobControl
    {
        private final JScriptDebugger debugger;
        private final ScriptEnvironment env;
        private String result;
        private final boolean sessionContext;
        private final Map<String, Object> scope;
        private final Map<String, Object> outVars;
        private final String content;

        public JSElementJobControl(String content, boolean sessionContext, Map<String, Object> scope, ScriptEnvironment env,
                Map<String, Object> outVars)
        {
            this.debugger = new JScriptDebugger();
            this.sessionContext = sessionContext;
            this.scope = scope;
            this.env = env;
            this.outVars = outVars;
            this.content = content;
        }

        @Override
        public void terminate()
        {
            super.terminate();
            end();
            if(debugger != null)
                debugger.setDisconnected(true);
        }

        @Override
        public void addBreakpoints(int ... lines)
        {
            debugger.addBreakpoints( lines );
        }

        @Override
        public void removeBreakpoints(int ... lines)
        {
            debugger.removeBreakpoints( lines );
        }

        @Override
        public void clearBreakpoints()
        {
            debugger.clearBreakpoints();
        }

        @Override
        public void breakOn(BreakType type)
        {
            debugger.setBreakType( type );
        }

        @Override
        public Object calc(String expression)
        {
            return debugger.calc(expression);
        }

        @Override
        public String objectToString(Object obj)
        {
            int maxStringLength = 500;
            StringBuilder sb = new StringBuilder();
            objectToString( obj, maxStringLength, sb );
            return maxStringLength < sb.length() ? sb.substring( 0, maxStringLength ) + "..." : sb.toString();
        }

        private void objectToString(Object obj, int maxLength, StringBuilder out)
        {
            if( obj == null )
            {
                out.append( "null" );
            }
            else if( obj instanceof Undefined )
            {
                out.append( "undefined" );
            }
            else if( obj instanceof String )
            {
                out.append( "\"" + (String)obj + "\"" );
            }
            else if( obj instanceof NativeArray || obj instanceof NativeJavaArray )
            {
                out.append( "[" );
                nativeValue( obj, maxLength, out );
                out.append( "]" );
            }
            else if( obj instanceof NativeObject )
            {
                out.append( "{" );
                nativeValue( obj, maxLength, out );
                out.append( "}" );
            }
            else if( obj instanceof NativeJavaObject )
            {
                out.append( ( (NativeJavaObject)obj ).unwrap().toString() );
            }
            else if( obj instanceof RhinoException )
            {
                out.append( "Error: " + ( (RhinoException)obj ).getMessage() );
            }
            else
            {
                out.append( obj.toString() );
            }
        }

        private void nativeValue(Object obj, int maxLength, StringBuilder out)
        {
            if( out.length() >= maxLength )
            {
                out.append( "..." );
                return;
            }
            for( Object id : ( (Scriptable)obj ).getIds() )
            {
                if( id instanceof String )
                {
                    objectToString( id, maxLength, out );
                    if( out.length() >= maxLength )
                    {
                        out.append( "..." );
                        return;
                    }
                    out.append( ": " );
                }
                Object value = get( obj, id );
                objectToString( value, maxLength, out );
                if( out.length() >= maxLength )
                {
                    out.append( "..." );
                    return;
                }
                out.append( ", " );
            }
            if (out.lastIndexOf( ", " ) > 0)
                {
                    out.delete( out.lastIndexOf( ", " ), out.length() );
                }
        }

        private Object get(Object obj, Object key)
        {
            Scriptable so = (Scriptable)obj;
            if( key instanceof String )
            {
                return so.get( (String)key, so );
            }
            else if( key instanceof Integer )
            {
                return so.get( (Integer)key, so );
            }
            throw new AssertionError();
        }

        @Override
        public String getObjectType(Object obj)
        {
            if( obj == null )
            {
                return "null";
            }
            if( obj instanceof Undefined )
            {
                return "undefined";
            }
            if( obj instanceof Double )
            {
                return "Number";
            }
            if( obj instanceof RhinoException )
            {
                return "Error";
            }
            String objectType = obj.getClass().getSimpleName();
            if( obj instanceof NativeJavaObject )
            {
                return objectType.replace( "Native", "" ) + ": " + ( (NativeJavaObject)obj ).unwrap().getClass().getSimpleName();
            }
            if( objectType.startsWith( "Native" ) )
            {
                return objectType.replace( "Native", "" );
            }
            return objectType;
        }

        @Override
        public List<String> getVariables()
        {
            return debugger.getVariables();
        }

        @Override
        public ScriptStackTraceElement[] getStackTrace()
        {
            return debugger.getStackTrace();
        }

        @Override
        public int getCurrentLine()
        {
            return debugger.getCurrentLine();
        }

        @Override
        protected void doRun() throws JobControlException
        {
            debugger.setPauseHandler( () -> {
                pause();
                try
                {
                    checkStatus();
                }
                catch( JobControlException e )
                {
                    if( e.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    {
                        env.warn( "Cancelled by user" );
                        return;
                    }
                    throw ExceptionRegistry.translateException( e );
                }
            } );
            debugger.init();
            Context context = JScriptContext.getContext();
            ScriptableObject topLevelScope;
            if(sessionContext)
            {
                SessionCache sessionCache = SessionCacheManager.getSessionCache();
                synchronized(sessionCache)
                {
                    Object scopeObj = sessionCache.getObject(JAVA_SCRIPT_SCOPE_PROPERTY);
                    if(scopeObj instanceof ScriptableObject)
                    {
                        topLevelScope = (ScriptableObject)scopeObj;
                    } else
                    {
                        topLevelScope = createTopLevelScope( context );
                        sessionCache.addObject(JAVA_SCRIPT_SCOPE_PROPERTY, topLevelScope, true);
                    }
                }
            } else
            {
                topLevelScope = createTopLevelScope( context );
            }
            try
            {
                Scriptable buck = JavaScriptUtils.defineVariables( topLevelScope, scope );
                context.setDebugger(debugger, 0);
                context.setGeneratingDebug(true);
                context.setOptimizationLevel( -1);


                DerivedScriptEnvironment trackingEnv = new DerivedScriptEnvironment( env );
                result = JScriptContext.evaluateString( context, topLevelScope, content, getName(), trackingEnv );
                if( !trackingEnv.hasData() && result != null )
                    env.print( result );

                for( String outVarName : new ArrayList<>( outVars.keySet() ) )
                {
                    Object newValue = buck.get(outVarName, buck);
                    if(newValue instanceof Wrapper)
                        newValue = ( (Wrapper)newValue ).unwrap();
                    outVars.put( outVarName, newValue );
                }
            }
            finally
            {
                for( Entry<String, Object> var : scope.entrySet() )
                    topLevelScope.delete(JavaScriptUtils.getValidName(var.getKey()));
                topLevelScope.delete("$");
            }
        }

        @Override
        public String getResult()
        {
            return result;
        }
    }
}
