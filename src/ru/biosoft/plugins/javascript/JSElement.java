package ru.biosoft.plugins.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
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
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCache.ConsoleKey;
import ru.biosoft.access.security.SessionCache.ConsoleState;
import ru.biosoft.access.security.SessionCacheManager;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

@ClassIcon("resources/script.gif")
@PropertyName("script")
public class JSElement extends ScriptDataElement
{
    private static final int CONSOLE_WAIT_SECONDS = 10;
    private static final String JAVA_SCRIPT_SCOPE_PROPERTY = "properties/JavaScript/scope";

    private static final ConsoleKey<ConsoleScope> CONSOLE = new ConsoleKey<>();

    private record ConsoleScope(Permission authorization, ScriptableObject scope)
    {
    }

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
            ScriptClassShutter.runRestricted( () -> {
                runScript();
                return null;
            } );
        }

        private void runScript() throws JobControlException
        {
            Context context = JScriptContext.getContext();
            boolean nested = ScriptRuntime.hasTopCall( context );
            String session = SecurityManager.getSession();
            SessionCache cache = nested ? null : SessionCacheManager.getSessionCache( session );
            if( !sessionContext || cache == null )
            {
                ScriptableObject scope = createTopLevelScope( context );
                Permission authorization = nested ? null : administratorPermission();
                Global scratch = authorization == null ? null : createTopLevelScope( context );
                if( authorization != null && administratorPermission() != authorization )
                    throw authorizationChanged();
                executeJob( context, scope, scratch );
                return;
            }

            ConsoleState<ConsoleScope> console = CONSOLE.forSession( cache );
            // Cross-worker dependencies must not wait indefinitely for a console that is waiting on them.
            var execution = console.executionLock();
            try
            {
                if( !execution.tryLock( CONSOLE_WAIT_SECONDS, TimeUnit.SECONDS ) )
                    throw new JobControlException( JobControl.TERMINATED_BY_ERROR, "The JavaScript console is busy" );
            }
            catch( InterruptedException e )
            {
                Thread.currentThread().interrupt();
                throw new JobControlException( JobControl.TERMINATED_BY_REQUEST, "Interrupted while waiting for the JavaScript console" );
            }
            try
            {
                checkStatus();
                checkConsoleSession( session, cache );
                runConsoleCommand( context, session, cache, console );
            }
            finally
            {
                execution.unlock();
            }
        }

        private void checkConsoleSession(String session, SessionCache cache) throws JobControlException
        {
            if( !session.equals( SecurityManager.getSession() ) || cache != SessionCacheManager.getSessionCache( session ) )
                throw new JobControlException( JobControl.TERMINATED_BY_ERROR, "The JavaScript console session changed" );
        }

        private void runConsoleCommand(Context context, String session, SessionCache cache, ConsoleState<ConsoleScope> console)
                throws JobControlException
        {
            // Capture authorization after waiting. A started script may already hold Java objects when it is demoted.
            var snapshot = console.administrator();
            Permission authorization = administratorPermission();
            if( authorization == null )
            {
                if( snapshot.scope() != null )
                    console.publish( snapshot, null );
                Object existing = cache.getObject( JAVA_SCRIPT_SCOPE_PROPERTY );
                ScriptableObject scope;
                if( existing instanceof ScriptableObject ordinary )
                    scope = ordinary;
                else
                {
                    scope = createTopLevelScope( context );
                    cache.addObject( JAVA_SCRIPT_SCOPE_PROPERTY, scope, true );
                }
                executeJob( context, scope, null );
                return;
            }

            ConsoleScope entry = snapshot.scope();
            boolean fresh = entry == null || entry.authorization() != authorization;
            if( fresh )
                entry = new ConsoleScope( authorization, createTopLevelScope( context ) );
            Global scratch = createTopLevelScope( context );
            checkConsoleSession( session, cache );
            if( administratorPermission() != authorization
                    || ( fresh ? !console.publish( snapshot, entry ) : console.administrator() != snapshot ) )
                throw authorizationChanged();
            executeJob( context, entry.scope(), scratch );
        }

        private JobControlException authorizationChanged()
        {
            return new JobControlException( JobControl.TERMINATED_BY_ERROR, "Administrator authorization changed before script execution" );
        }

        private Permission administratorPermission()
        {
            return ScriptClassShutter.getInstance().getMode() == ScriptClassShutter.Mode.OFF
                    ? null : SecurityManager.getSessionAdministratorPermission();
        }

        private void executeJob(Context context, ScriptableObject topLevelScope, Global scratch)
        {
            var previousScope = JScriptContext.getScope();
            var previousDebugger = context.getDebugger();
            Object previousDebuggerData = context.getDebuggerContextData();
            if( scratch != null )
                JScriptContext.setScope( scratch );
            try
            {
                executeInScope( context, topLevelScope, scratch != null );
            }
            finally
            {
                if( scratch != null )
                    JScriptContext.setScope( previousScope );
                context.setDebugger( previousDebugger, previousDebuggerData );
            }
        }

        private void executeInScope(Context context, ScriptableObject topLevelScope, boolean administrator)
        {
            try
            {
                Scriptable buck = JavaScriptUtils.defineVariables( topLevelScope, scope, administrator );
                context.setDebugger(debugger, 0);
                context.setGeneratingDebug(true);
                context.setOptimizationLevel( -1);


                DerivedScriptEnvironment trackingEnv = new DerivedScriptEnvironment( env );
                result = JScriptContext.evaluateJob( context, topLevelScope, content, getName(), trackingEnv, administrator );
                // echo the value like a console does, but not a script document's "undefined" (a workflow's script steps
                // logged nothing else)
                if( !trackingEnv.hasData() && result != null && ( sessionContext || !"undefined".equals( result ) ) )
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
