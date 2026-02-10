package ru.biosoft.plugins.javascript;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CancellationException;

import one.util.streamex.DoubleCollector;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntCollector;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.PolicySecurityController;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SecurityController;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.tools.ToolErrorReporter;

import ru.biosoft.access.BiosoftSecurityManager;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.script.ScriptEnvironment;

import com.developmentontheedge.application.Application;

/**
 *
 * @pending whether Context.initStandardObjects is used correctly
 */
public class JScriptContext
{
    protected static final Logger log = Logger.getLogger(JScriptContext.class.getName());

    public static final String LAST_COMMAND = "last_command";

    private static final SecurityController securityController = new PolicySecurityController();

    //private static ThreadLocal<Context> context = new ThreadLocal<>();

    private static ThreadLocal<Context> context = new ThreadLocal<Context>() {
        @Override
        public void remove() {
            Context cx = get();
            if (cx != null) {
                try {
                    // Exit the context if it's currently entered
                    if (Context.getCurrentContext() == cx) {
                        Context.exit();
                    }
                } catch (Exception e) {
                    log.log(Level.WARNING, "Error exiting context during cleanup", e);
                }
            }
            super.remove();
        }
        
        @Override
        protected Context initialValue() {
            return null; // Will be set explicitly when needed
        }
    };

    public static Context getContext()
    {
        Context threadContext = context.get();
        if( threadContext == null )
        {
            threadContext = Context.enter();
            threadContext.setApplicationClassLoader(ClassLoading.getClassLoader());
            try
            {
                threadContext.setSecurityController(securityController);
            }
            catch( Exception e )
            {
            }
            ImporterTopLevel scope = getScope();
            scope.initStandardObjects(threadContext, false);
            JScriptVisiblePlugin jsPlugin = JScriptVisiblePlugin.getInstance();
            if( jsPlugin != null )
                loadExtensions(jsPlugin, threadContext, scope);
            context.set(threadContext);
        }

        if( Context.getCurrentContext() == null )
        {
            threadContext = Context.enter();
            try
            {
                threadContext.setSecurityController(securityController);
            }
            catch( Exception e )
            {
            }
            //context.initStandardObjects(getScope());
        }

        return threadContext;
    }

    public static void setContext( Context ctx )
    {
        context.set( ctx );
    }

    private static ThreadLocal<ImporterTopLevel> scope = new ThreadLocal<>();

    public static ImporterTopLevel getScope()
    {
        ImporterTopLevel threadScope = scope.get();
        if( threadScope == null )
        {
            threadScope = new Global();
            scope.set(threadScope);
        }
        return threadScope;
    }

    public static void setScope(ImporterTopLevel sc)
    {
        scope.set(sc);
    }

    public static String evaluateString(String expr)
    {
        return evaluateString(getContext(), getScope(), expr, "", null);
    }

    public static String evaluateString(String expr, ScriptEnvironment environment)
    {
        return evaluateString(getContext(), getScope(), expr, "", environment);
    }

    public static String evaluateString(Context context, ScriptableObject scope, String expr, ScriptEnvironment environment)
    {
        return evaluateString(context, scope, expr, "", environment);
    }

    private static class SandboxRunnable implements Runnable
    {
        private Object result;
        private final Context context;
        private final Scriptable scope;
        private final Script script;

        public SandboxRunnable(Context context, Scriptable scope, Script script)
        {
            this.context = context;
            this.scope = scope;
            this.script = script;
        }

        @Override
        public void run()
        {
            //log.info( "JavaScript ClassLoader: " + script.getClass().getClassLoader() );
            result = script.exec(context, scope);
        }

        public Object getResult()
        {
            return result;
        }
    }

    private static final PerThreadStream threadOutStream = new PerThreadStream( System.out ), threadErrStream = new PerThreadStream( System.err );

    public static String evaluateString(Context context, ScriptableObject scope, String expr, String name, ScriptEnvironment environment)
    {
        //execute preprocessors
        expr = PreprocessorRegistry.preprocessors().foldLeft( expr, (code, prep) -> prep.preprocess( code ) );

        if( environment != null )
        {
            //send JSEnvironment to all objects via JavaScript scope
            scope.put(Global.ENVIRONMENT_OBJECT, scope, environment);
            scope.defineProperty( Global.ENVIRONMENT_OBJECT, environment, ScriptableObject.DONTENUM );
            Global.storeEnvironmentForThread(Thread.currentThread(), environment);
        }

        PrintStream oldOutStream = System.out;
        OutputStream outStream = new TextOutputStream();
        threadOutStream.set( outStream );
        System.setOut( new PrintStream( threadOutStream ) );
        
        PrintStream oldErrStream = System.err;
        OutputStream errStream = new TextOutputStream();
        threadErrStream.set( errStream );
        System.setErr( new PrintStream( threadErrStream ) );

        String resultString = null;

        scope.defineProperty( LAST_COMMAND, Undefined.instance, ScriptableObject.DONTENUM );

        try
        {
            Object result;
            try
            {
                Script script = context.compileString(expr, name, 1, null);
                SandboxRunnable runnable = new SandboxRunnable(context, scope, script);
                BiosoftSecurityManager.runInSandbox(runnable);
                result = runnable.getResult();

                if( result == Undefined.instance )
                    result = "undefined";

                if( result instanceof Scriptable )
                {
                    try
                    {
                        resultString = JScriptHelp.getHelpValue((Scriptable)result);
                        if( resultString != null && environment != null )
                        {
                            environment.showHtml(resultString);
                            resultString = null;
                        }
                        else if( environment != null && result instanceof Wrapper &&
                                ( resultString = environment.tryShowObject( ( ( Wrapper )result ).unwrap() ) ) != null )
                        {
                            log.info( "Got interesting object 1" );
                            resultString = null;
                        }
                        else
                        {
                            resultString = convertToString(result);
                            scope.defineProperty( LAST_COMMAND, expr, ScriptableObject.DONTENUM );
                        }
                    }
                    catch( RuntimeException exc )
                    {
                        if( environment != null && ( resultString = environment.tryShowException( exc ) ) != null )
                        {
                            resultString = null;
                        }
                        else
                        {
                            resultString = String.valueOf( result );
                        }
                    }
                }
                else if( environment != null && ( resultString = environment.tryShowObject( result ) ) != null )
                {
                     log.info( "Got interesting object 2" );
                     resultString = null;
                }
                else
                {  
                    resultString = String.valueOf(result);
                }
            }
            catch( WrappedException ex )
            {
                if( environment != null && ( resultString = environment.tryShowException( ex ) ) != null )
                {
                    resultString = null;
                }
                else
                {
                    if (ex.getCause() instanceof CancellationException) //TODO not sure if we need here to throw ex
                        throw ex;
                    resultString = ExceptionRegistry.log(ex.getCause());
                }
            }
            catch( Throwable exc )
            {
                if( environment != null )
                {
                    if( ( resultString = environment.tryShowException( exc ) ) != null )
                    {
                        resultString = null;
                    }
                    else
                    { 
                        environment.error( exc.getMessage() );
                    }  
                }   
            }

            if( resultString == null )
            {
                resultString = "";
            }
        }
        finally
        {
            if( environment != null )
            {
                String string = outStream.toString();
                if(!string.isEmpty())
                    environment.print(string);
                string = errStream.toString();
                if(!string.isEmpty())
                    environment.error(string);
                scope.delete(Global.ENVIRONMENT_OBJECT);
            }
            System.setOut(oldOutStream);
            System.setErr(oldErrStream);
        }

        return resultString;
    }

    private static final List<JSObjectConverter> converters = new ArrayList<>();

    public static void addStringConverter(JSObjectConverter converter)
    {
        SecurityManager sm = System.getSecurityManager();
        if( sm != null )
        {
            sm.checkPermission(new RuntimePermission("setupApplication"));
        }
        converters.add(converter);
    }
    public static List<JSObjectConverter> getConverters()
    {
        return Collections.unmodifiableList( converters );
    }

    public static String convertToString(Object object)
    {
        if( ( object == null ) || ( object instanceof Undefined ) )
        {
            return "";
        }
        if( object instanceof NativeJavaArray )
        {
            NativeJavaArray array = (NativeJavaArray)object;
            Object arrayObj = array.unwrap();
            if( arrayObj instanceof double[] )
            {
                return DoubleStreamEx.of((double[])arrayObj).collect( DoubleCollector.joining( ", ", "[", "]" ) );
            }
            else if( arrayObj instanceof int[] )
            {
                return IntStreamEx.of((int[])arrayObj).collect( IntCollector.joining( ", ", "[", "]" ) );
            }
            else if( arrayObj instanceof double[][] )
            {
                return StreamEx.of((double[][])arrayObj)
                    .map( row -> DoubleStreamEx.of((double[])arrayObj).collect( DoubleCollector.joining( ", ", "[", "]" ) ) )
                    .joining( "\n ", "[", "]" );
            }
            else if( arrayObj instanceof String[] )
            {
                return StreamEx.of((String[])arrayObj).joining( ",\n", "[", "]" );
            }
        }
        else if( object instanceof NativeJavaObject )
        {
            Object javaObject = ( (NativeJavaObject)object ).unwrap();
            for( JSObjectConverter converter : converters )
            {
                if( converter.canConvert(javaObject) )
                {
                    return converter.convertToString(javaObject);
                }
            }
        }
        return ScriptRuntime.toString(object);
    }

    public static Object evaluateReader(Reader in, String sourceName, int lineno, Object securityDomain)
    {
        Context cx = getContext();
        Object result = Context.getUndefinedValue();
        try
        {
            result = cx.evaluateReader( getScope(), in, sourceName, lineno, securityDomain );
        }
        catch( WrappedException we )
        {
            log.log(Level.SEVERE, we.getWrappedException().toString(), we);
        }
        catch( EcmaError ee )
        {
            String msg = ee.toString();
            //exitCode = EXITCODE_RUNTIME_ERROR;
            if( ee.getSourceName() == null )
                Context.reportError(msg);
            else
            {
                Context.reportError(msg, ee.getSourceName(), ee.getLineNumber(), ee.getLineSource(), ee.getColumnNumber());
            }
        }
        catch( EvaluatorException ee )
        {
            // Already printed message.
            //exitCode = EXITCODE_RUNTIME_ERROR;
        }
        catch( JavaScriptException jse )
        {
            // Need to propagate ThreadDeath exceptions.
            Object value = jse.getValue();
            if( value instanceof ThreadDeath )
                throw (ThreadDeath)value;
            //exitCode = EXITCODE_RUNTIME_ERROR;
            Context.reportError(jse.getMessage());
        }
        catch( IOException ioe )
        {
            log.log(Level.SEVERE, ioe.toString());
        }
        finally
        {
            try
            {
                in.close();
            }
            catch( IOException ioe )
            {
                log.log(Level.SEVERE, ioe.toString());
            }
        }
        return result;
    }

    public static Object processFile(String filename)
    {
        // if (securityImpl == null)
        return processFileSecure(filename, null);
        // else
        //    securityImpl.callProcessFileSecure(cx, scope, filename);
    }

    static Object processFileSecure(String filename, Object securityDomain)
    {
        Reader in = null;

        // Try filename first as URL
        try
        {
            URL url = new URL(filename);
            InputStream is = url.openStream();
            in = new BufferedReader(new InputStreamReader(is));
        }
        catch( MalformedURLException mfex )
        {
            // fall through to try it as a file
        }
        catch( IOException ioex )
        {
            Context.reportError("msg.couldnt.open.url"); //ToolErrorReporter.getMessage("msg.couldnt.open.url", filename, ioex.toString()));
            //exitCode = EXITCODE_FILE_NOT_FOUND;
            return null;
        }

        if( in == null )
        {
            // Try filename as file
            try
            {
                in = new PushbackReader(new FileReader(filename));
                int c = in.read();
                // Support the executable script #! syntax:  If
                // the first line begins with a '#', treat the whole
                // line as a comment.
                if( c == '#' )
                {
                    while( ( c = in.read() ) != -1 )
                    {
                        if( c == '\n' || c == '\r' )
                            break;
                    }
                    ( (PushbackReader)in ).unread(c);
                }
                else
                {
                    // No '#' line, just reopen the file and forget it
                    // ever happened.  OPT closing and reopening
                    // undoubtedly carries some cost.  Is this faster
                    // or slower than leaving the PushbackReader
                    // around?
                    in.close();
                    in = new FileReader(filename);
                }

                filename = new java.io.File(filename).getCanonicalPath();
            }
            catch( FileNotFoundException ex )
            {
                Context.reportError(ToolErrorReporter.getMessage("msg.couldnt.open" , filename));
                return null;
            }
            catch( IOException ioe )
            {
                //global.getErr().println(ioe.toString());
            }
        }

        // Here we evalute the entire contents of the file as
        // a script. Text is printed only if the print() function is called.
        return evaluateReader(in, filename, 1, securityDomain);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Function and host objects defined as extensions
    //
    public static final String NAME_ATTR = "name";
    public static final String ALIAS_ATTR = "alias";
    public static final String CLASS_ATTR = "class";
    public static final String METHOD_ATTR = "method";
    public static final String VARARGS_ATTR = "varargs";
    public static final String ARGUMENT_ELEMENT = "argument";
    public static final String DOC_ELEMENT = "doc";

    public static void loadExtensions(JScriptVisiblePlugin help)
    {
        loadExtensions( help, getContext(), getScope() );
    }

    public static void loadExtensions(JScriptVisiblePlugin help, Context context, ScriptableObject scope)
    {
        loadFunctionExtensions("ru.biosoft.plugins.javascript.function", help.getFunctions(), context, scope);
        loadHostObjectExtensions("ru.biosoft.plugins.javascript.hostObject", help.getHostObjects(), context, scope);
    }

    /**
     * Load all executable extensions for the specified extension point.
     */
    public static void loadFunctionExtensions(String extensionPointId, DataCollection functions, Context context, ScriptableObject scope)
    {
        IExtensionPoint point = Application.getExtensionRegistry().getExtensionPoint(extensionPointId);

        if( point == null )
        {
            log.log(Level.SEVERE, "Extension point '" + extensionPointId + "' can not be found.");
        }
        else
        {
            IExtension[] extensions = point.getExtensions();
            for( IExtension extension : extensions )
            {
                IConfigurationElement element = extension.getConfigurationElements()[0];
                try
                {
                    defineFunction(element, context, scope, functions);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "JavaScript function definition error, extention=" + element.getName(), t);
                }
            }
        }
    }

    /**
     *
     * @pending error processing
     */
    public static void defineFunction(IConfigurationElement element, Context context, ScriptableObject scope, DataCollection functions)
    {
        String functionName = element.getAttribute(NAME_ATTR);
        String className = element.getAttribute(CLASS_ATTR);
        String methodName = element.getAttribute(METHOD_ATTR);
        boolean varargs = "true".equals(element.getAttribute(VARARGS_ATTR));

        Class<?> c = null;
        try
        {
            c = ClassLoading.loadClass( className, "biouml.plugins.sbw;biouml.plugins.microarray;ru.biosoft.plugins.javascript;ru.biosoft.plugins.jri" );
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not load class '" + className + "' for JavaScript function '" + functionName + "'\n error: " + t.getMessage());
            return;
        }

        Method method = null;
        if( varargs )
        {
            try
            {
                Class<?>[] args = {Context.class, Scriptable.class, Object[].class, Function.class};
                method = c.getMethod(methodName, args);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not find method '" + methodName + "' for JavaScript function (varargs) '" + functionName + "'\n error: "
                        + t.getMessage());
                return;
            }
        }
        else
        {
            try
            {
                IConfigurationElement[] argElements = element.getChildren(ARGUMENT_ELEMENT);
                Class<?>[] args = null;

                if( argElements != null && argElements.length > 0 )
                {
                    args = new Class[argElements.length];
                    for( int i = 0; i < argElements.length; i++ )
                    {
                        String type = argElements[i].getAttribute(CLASS_ATTR);
                        if( primitiveTypes.containsKey(type) )
                            args[i] = primitiveTypes.get(type);
                        else
                            args[i] = Class.forName(type);
                    }
                }
                method = c.getMethod(methodName, args);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not find method '" + methodName + "' for JavaScript function '" + functionName + "'\n error: "
                        + t.getMessage(), t);
                return;
            }
        }

        try
        {
            FunctionObject function = new FunctionObject(functionName, method, scope);
            scope.defineProperty(functionName, function, ScriptableObject.DONTENUM);

            FunctionInfo info = new FunctionInfo(functionName, functions);

            JScriptHelp.readFunctionInfo(info, function, element.getChildren(DOC_ELEMENT));
            functions.put(info);
            JScriptHelp.addFunctionDescription(info.getName());

            //log.info("Function loaded: " + className + "." + functionName + ".");
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not define JavaScript function '" + functionName + "'\n error: " + t.getMessage(), t);
        }
    }

    /**
     *
     * @pending stub
     */
    public static void loadHostObjectExtensions(String extensionPointId, DataCollection hostObjects, Context context, ScriptableObject scope)
    {
        IExtensionPoint point = Application.getExtensionRegistry().getExtensionPoint(extensionPointId);

        if( point == null )
        {
            log.log(Level.SEVERE, "Extension point '" + extensionPointId + "' can not be found.");
        }
        else
        {
            IExtension[] extensions = point.getExtensions();
            for( IExtension extension : extensions )
            {
                IConfigurationElement element = extension.getConfigurationElements()[0];
                try
                {
                    defineHostObject(element, context, scope, hostObjects);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "JavaScript host object definition error, extention=" + element.getName(), t);
                }
            }
        }
    }

    public static void defineHostObject(IConfigurationElement element, Context context, ScriptableObject scope, DataCollection hostObjects)
    {
        String objectName = element.getAttribute(NAME_ATTR);
        String className = element.getAttribute(CLASS_ATTR);
        String objectAlias = element.getAttribute(ALIAS_ATTR);
        
        Class<?> c = null;
        try
        {
            c = ClassLoading.loadClass( className, element.getNamespaceIdentifier() );

            Object host = c.newInstance();
            if( host instanceof JSEnvironmentProvider )
            {
                ( (JSEnvironmentProvider)host ).setScopeObject(scope);
            }
            Scriptable scriptable = Context.toObject(host, scope);
            scope.defineProperty( objectName, scriptable, ScriptableObject.DONTENUM );
            if( objectAlias != null )
                scope.defineProperty( objectAlias, scriptable, ScriptableObject.DONTENUM );

            if(host instanceof ScriptableObject)
            {
                hostObjects.put(new ScriptableHostObjectInfo(objectName, hostObjects, c.asSubclass(ScriptableObject.class)));
                if (objectAlias != null)
                    hostObjects.put(new ScriptableHostObjectInfo(objectAlias, hostObjects, c.asSubclass(ScriptableObject.class)));
            }
            IConfigurationElement[] docElements = element.getChildren(DOC_ELEMENT);
            if( docElements == null || docElements.length == 0 )
            {
            }
            else
            {
                if( docElements.length > 1 )
                {
                    log.warning("Host object info element is duplicated, only first element will be used, host object=" + objectName);
                }

                HostObjectInfo info = new HostObjectInfo(objectName, hostObjects, c);
                if (objectAlias != null)
                    info.setAlias( objectAlias );
                hostObjects.put(info);
                JScriptHelp.readHostObjectInfo(info, scriptable, docElements[0]);
                JScriptHelp.addObjectDescription(info.getName());
            }
        }
        catch( LoggedClassNotFoundException e )
        {
            log.log(Level.SEVERE, "Can not load class '" + className + "' for JavaScript host object '" + objectName);
            return;
        }
        catch( Throwable t ) // InstantiationException, IllegalAccessException
        {
            log.log(Level.SEVERE, "Can not load host object, class=" + className + ", object name=" + objectName + ", error=" + t, t);
        }
    }

    private static final HashMap<String, Class<?>> primitiveTypes = new HashMap<>();
    static
    {
        primitiveTypes.put("boolean", boolean.class);
        primitiveTypes.put("byte", byte.class);
        primitiveTypes.put("char", char.class);
        primitiveTypes.put("short", short.class);
        primitiveTypes.put("int", int.class);
        primitiveTypes.put("long", long.class);
        primitiveTypes.put("float", float.class);
        primitiveTypes.put("double", double.class);
    }

    public static class TextOutputStream extends OutputStream
    {
        private final Writer writer;
        public TextOutputStream()
        {
            this.writer = new StringWriter();
        }
        @Override
        public void write(int b)
        {
            try
            {
                writer.write( String.valueOf( (char)b ) );
            }
            catch( IOException e )
            {

            }
        }
        @Override
        public String toString()
        {
            return writer.toString();
        }
    }

    static class PerThreadStream extends OutputStream
    {
        private final ThreadLocal<OutputStream> threadStream;
        private final OutputStream defaultStream;

        PerThreadStream(OutputStream defaultStream)
        {
            threadStream = ThreadLocal.withInitial( () -> defaultStream );
            this.defaultStream = defaultStream;
        }

        void set(OutputStream os)
        {
            threadStream.set( os );
        }

        void clear()
        {
            threadStream.set( defaultStream );
        }

        @Override
        public void write(byte[] b) throws IOException
        {
            threadStream.get().write( b );
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            threadStream.get().write( b, off, len );
        }

        @Override
        public void write(int b) throws IOException
        {
            threadStream.get().write( b );
        }
    }
}
