/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino JavaScript Debugger code, released
 * November 21, 2000.
 *
 * The Initial Developer of the Original Code is
 * SeeBeyond Corporation.
 * Portions created by the Initial Developer are Copyright (C) 2000
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Igor Bukanov
 *   Matt Gould
 *   Christopher Oliver
 *   Cameron McCormack
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */
package ru.biosoft.plugins.javascript.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.NativeCall;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ObjArray;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SecurityUtilities;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableObject;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

import ru.biosoft.access.BiosoftSecurityManager;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.security.SessionThread;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.plugins.javascript.Global;
import ru.biosoft.plugins.javascript.JScriptContext;
import ru.biosoft.plugins.javascript.JScriptVisiblePlugin;
import ru.biosoft.plugins.javascript.PreprocessorRegistry;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.util.TextUtil2;

/**
 * Dim or Debugger Implementation for Rhino.
 */
public class Dim
{

    // Constants for instructing the debugger what action to perform
    // to end interruption.  Used by 'returnValue'.
    public static final int STEP_OVER = 0;
    public static final int STEP_INTO = 1;
    public static final int STEP_OUT = 2;
    public static final int GO = 3;
    public static final int BREAK = 4;
    public static final int EXIT = 5;

    // Constants for the DimIProxy interface implementation class.
    public static final int IPROXY_DEBUG = 0;
    public static final int IPROXY_LISTEN = 1;
    public static final int IPROXY_COMPILE_SCRIPT = 2;
    public static final int IPROXY_EVAL_SCRIPT = 3;
    public static final int IPROXY_STRING_IS_COMPILABLE = 4;
    public static final int IPROXY_OBJECT_TO_STRING = 5;
    public static final int IPROXY_OBJECT_PROPERTY = 6;
    public static final int IPROXY_OBJECT_IDS = 7;

    /**
     * Interface to the debugger GUI.
     */
    private GuiCallback callback;

    /**
     * Whether the debugger should break.
     */
    private boolean breakFlag;


    /**
     * The index of the current stack frame.
     */
    private int frameIndex = -1;

    /**
     * Information about the current stack at the point of interruption.
     */
    private volatile ContextData interruptedContextData;

    /**
     * The ContextFactory to listen to for debugging information.
     */
    private ContextFactory contextFactory;

    /**
     * Synchronization object used to allow script evaluations to
     * happen when a thread is resumed.
     */
    private final Object monitor = new Object();

    /**
     * Synchronization object used to wait for valid
     * {@link #interruptedContextData}.
     */
    private final Object eventThreadMonitor = new Object();

    /**
     * The action to perform to end the interruption loop.
     */
    private volatile int returnValue = -1;

    /**
     * Whether the debugger is inside the interruption loop.
     */
    private boolean insideInterruptLoop;

    /**
     * The requested script string to be evaluated when the thread
     * has been resumed.
     */
    private volatile String evalRequest;

    /**
     * The stack frame in which to evaluate {@link #evalRequest}.
     */
    private StackFrame evalFrame;

    /**
     * The result of evaluating {@link #evalRequest}.
     */
    private String evalResult;

    /**
     * Whether the debugger should break when a script exception is thrown.
     */
    private boolean breakOnExceptions;

    /**
     * Whether the debugger should break when a script function is entered.
     */
    private boolean breakOnEnter;

    /**
     * Whether the debugger should break when a script function is returned
     * from.
     */
    private boolean breakOnReturn;

    /**
     * Document scope
     */
    private Scriptable scope;

    /**
     * Table mapping URLs to information about the script source.
     */
    private final Hashtable urlToSourceInfo = new Hashtable();

    /**
     * Table mapping function names to information about the function.
     */
    private final Hashtable functionNames = new Hashtable();

    /**
     * Table mapping functions to information about the function.
     */
    private final Hashtable functionToSource = new Hashtable();

    /**
     * ContextFactory.Listener instance attached to {@link #contextFactory}.
     */
    private DimIProxy listener;

    /**
     * Sets the GuiCallback object to use.
     */
    public void setGuiCallback(GuiCallback callback)
    {
        this.callback = callback;
    }

    /**
     * Tells the debugger to break at the next opportunity.
     */
    public void setBreak()
    {
        this.breakFlag = true;
    }

    /**
     * Switches context to the stack frame with the given index.
     */
    public void contextSwitch(int frameIndex)
    {
        this.frameIndex = frameIndex;
    }

    /**
     * Sets whether the debugger should break on exceptions.
     */
    public void setBreakOnExceptions(boolean breakOnExceptions)
    {
        this.breakOnExceptions = breakOnExceptions;
    }

    /**
     * Sets whether the debugger should break on function entering.
     */
    public void setBreakOnEnter(boolean breakOnEnter)
    {
        this.breakOnEnter = breakOnEnter;
    }

    /**
     * Sets whether the debugger should break on function return.
     */
    public void setBreakOnReturn(boolean breakOnReturn)
    {
        this.breakOnReturn = breakOnReturn;
    }

    /**
     * Attaches the debugger to the given ContextFactory.
     */
    public void attachTo(ContextFactory factory)
    {
        detach();
        this.contextFactory = factory;
        this.listener = new DimIProxy(this, IPROXY_LISTEN);
        factory.addListener(this.listener);
    }

    /**
     * Detaches the debugger from the current ContextFactory.
     */
    public void detach()
    {
        if( listener != null )
        {
            contextFactory.removeListener(listener);
            contextFactory = null;
            listener = null;
        }
    }

    /**
     * Releases resources associated with this debugger.
     */
    public void dispose()
    {
        detach();
    }

    /**
     * Returns the FunctionSource object for the given script or function.
     */
    private FunctionSource getFunctionSource(DebuggableScript fnOrScript)
    {
        FunctionSource fsource = functionSource(fnOrScript);
        if( fsource == null )
        {
            String url = getNormalizedUrl(fnOrScript);
            SourceInfo si = sourceInfo(url);
            if( si == null )
            {
                if( !fnOrScript.isGeneratedScript() )
                {
                    // Not eval or Function, try to load it from URL
                    String source = loadSource(url);
                    if( source != null )
                    {
                        DebuggableScript top = fnOrScript;
                        for( ;; )
                        {
                            DebuggableScript parent = top.getParent();
                            if( parent == null )
                            {
                                break;
                            }
                            top = parent;
                        }
                        registerTopScript(top, source);
                        fsource = functionSource(fnOrScript);
                    }
                }
            }
        }
        return fsource;
    }

    /**
     * Loads the script at the given URL.
     */
    private String loadSource(String sourceUrl)
    {
        String source = null;
        int hash = sourceUrl.indexOf('#');
        if( hash >= 0 )
        {
            sourceUrl = sourceUrl.substring(0, hash);
        }
        try
        {
            InputStream is;
            openStream:
            {
                if( sourceUrl.indexOf(':') < 0 )
                {
                    // Can be a file name
                    try
                    {
                        if( sourceUrl.startsWith("~/") )
                        {
                            String home = SecurityUtilities.getSystemProperty("user.home");
                            if( home != null )
                            {
                                String pathFromHome = sourceUrl.substring(2);
                                File f = new File(new File(home), pathFromHome);
                                if( f.exists() )
                                {
                                    is = new FileInputStream(f);
                                    break openStream;
                                }
                            }
                        }
                        File f = new File(sourceUrl);
                        if( f.exists() )
                        {
                            is = new FileInputStream(f);
                            break openStream;
                        }
                    }
                    catch( SecurityException ex )
                    {
                    }
                    // No existing file, assume missed http://
                    if( sourceUrl.startsWith("//") )
                    {
                        sourceUrl = "http:" + sourceUrl;
                    }
                    else if( sourceUrl.startsWith("/") )
                    {
                        sourceUrl = "http://127.0.0.1" + sourceUrl;
                    }
                    else
                    {
                        sourceUrl = "http://" + sourceUrl;
                    }
                }

                is = ( new URL(sourceUrl) ).openStream();
            }

            try
            {
                source = Kit.readReader(new InputStreamReader(is));
            }
            finally
            {
                is.close();
            }
        }
        catch( IOException ex )
        {
            System.err.println("Failed to load source from " + sourceUrl + ": " + ex);
        }
        return source;
    }

    /**
     * Registers the given script as a top-level script in the debugger.
     */
    private void registerTopScript(DebuggableScript topScript, String source)
    {
        if( !topScript.isTopLevel() )
        {
            throw new IllegalArgumentException();
        }
        String url = getNormalizedUrl(topScript);
        DebuggableScript[] functions = getAllFunctions(topScript);
        final SourceInfo sourceInfo = new SourceInfo(source, functions);

        synchronized( urlToSourceInfo )
        {
            SourceInfo old = (SourceInfo)urlToSourceInfo.get(url);
            if( old != null )
            {
                if( sourceInfo.breakpoints.length > old.breakpoints.length )
                {
                    for( int i = 0; i < old.breakpoints.length; i++ )
                    {
                        sourceInfo.breakpoints[i] = old.breakpoints[i];
                    }
                    old.breakpoints = sourceInfo.breakpoints;
                }
                else
                {
                    sourceInfo.breakpoints = old.breakpoints;
                }
            }
            urlToSourceInfo.put(url, sourceInfo);
            for( int i = 0; i != sourceInfo.functionSourcesTop(); ++i )
            {
                FunctionSource fsource = sourceInfo.functionSource(i);
                String name = fsource.name();
                if( name.length() != 0 )
                {
                    functionNames.put(name, fsource);
                }
            }
        }

        synchronized( functionToSource )
        {
            for( int i = 0; i != functions.length; ++i )
            {
                FunctionSource fsource = sourceInfo.functionSource(i);
                functionToSource.put(functions[i], fsource);
            }
        }

        //callback.updateSourceText(sourceInfo);
    }

    /**
     * Returns the FunctionSource object for the given function or script.
     */
    private FunctionSource functionSource(DebuggableScript fnOrScript)
    {
        return (FunctionSource)functionToSource.get(fnOrScript);
    }

    /**
     * Returns an array of all function names.
     */
    public String[] functionNames()
    {
        String[] a;
        synchronized( urlToSourceInfo )
        {
            Enumeration e = functionNames.keys();
            a = new String[functionNames.size()];
            int i = 0;
            while( e.hasMoreElements() )
            {
                a[i++] = (String)e.nextElement();
            }
        }
        return a;
    }

    /**
     * Returns the FunctionSource object for the function with the given name.
     */
    public FunctionSource functionSourceByName(String functionName)
    {
        return (FunctionSource)functionNames.get(functionName);
    }

    /**
     * Returns the SourceInfo object for the given URL.
     */
    public SourceInfo sourceInfo(String url)
    {
        return (SourceInfo)urlToSourceInfo.get(url);
    }

    /**
     * Returns the source URL for the given script or function.
     */
    private String getNormalizedUrl(DebuggableScript fnOrScript)
    {
        String url = fnOrScript.getSourceName();
        if( url == null )
        {
            url = "<stdin>";
        }
        else
        {
            // Not to produce window for eval from different lines,
            // strip line numbers, i.e. replace all #[0-9]+\(eval\) by
            // (eval)
            // Option: similar teatment for Function?
            char evalSeparator = '#';
            StringBuffer sb = null;
            int urlLength = url.length();
            int cursor = 0;
            for( ;; )
            {
                int searchStart = url.indexOf(evalSeparator, cursor);
                if( searchStart < 0 )
                {
                    break;
                }
                String replace = null;
                int i = searchStart + 1;
                while( i != urlLength )
                {
                    int c = url.charAt(i);
                    if( ! ( '0' <= c && c <= '9' ) )
                    {
                        break;
                    }
                    ++i;
                }
                if( i != searchStart + 1 )
                {
                    // i points after #[0-9]+
                    if( "(eval)".regionMatches(0, url, i, 6) )
                    {
                        cursor = i + 6;
                        replace = "(eval)";
                    }
                }
                if( replace == null )
                {
                    break;
                }
                if( sb == null )
                {
                    sb = new StringBuffer();
                    sb.append(url.substring(0, searchStart));
                }
                sb.append(replace);
            }
            if( sb != null )
            {
                if( cursor != urlLength )
                {
                    sb.append(url.substring(cursor));
                }
                url = sb.toString();
            }
        }
        return url;
    }

    /**
     * Returns an array of all functions in the given script.
     */
    private static DebuggableScript[] getAllFunctions(DebuggableScript function)
    {
        ObjArray functions = new ObjArray();
        collectFunctions_r(function, functions);
        DebuggableScript[] result = new DebuggableScript[functions.size()];
        functions.toArray(result);
        return result;
    }

    /**
     * Helper function for {@link #getAllFunctions(DebuggableScript)}.
     */
    private static void collectFunctions_r(DebuggableScript function, ObjArray array)
    {
        array.add(function);
        for( int i = 0; i != function.getFunctionCount(); ++i )
        {
            collectFunctions_r(function.getFunction(i), array);
        }
    }

    /**
     * Clears all breakpoints.
     */
    public void clearAllBreakpoints()
    {
        Enumeration e = urlToSourceInfo.elements();
        while( e.hasMoreElements() )
        {
            SourceInfo si = (SourceInfo)e.nextElement();
            si.removeAllBreakpoints();
        }
    }

    /**
     * Called when a breakpoint has been hit.
     */
    private void handleBreakpointHit(StackFrame frame, Context cx)
    {
        breakFlag = false;
        interrupted(cx, frame, null);
    }

    /**
     * Called when a script exception has been thrown.
     */
    private void handleExceptionThrown(Context cx, Throwable ex, StackFrame frame)
    {
        if( breakOnExceptions )
        {
            ContextData cd = frame.contextData();
            if( cd.lastProcessedException != ex )
            {
                interrupted(cx, frame, ex);
                cd.lastProcessedException = ex;
            }
        }
    }

    /**
     * Returns the current ContextData object.
     */
    public ContextData currentContextData()
    {
        return interruptedContextData;
    }

    /**
     * Sets the action to perform to end interruption.
     */
    public void setReturnValue(int returnValue)
    {
        synchronized( monitor )
        {
            this.returnValue = returnValue;
            monitor.notify();
        }
    }

    /**
     * Clear scope
     */
    public void clearScope()
    {
        scope = null;
    }

    /**
     * Resumes execution of script.
     */
    public void go()
    {
        synchronized( monitor )
        {
            this.returnValue = GO;
            monitor.notifyAll();
        }
    }

    /**
     * Evaluates the given script.
     */
    public String eval(String expr)
    {
        String result = "undefined";
        if( expr == null )
        {
            return result;
        }
        ContextData contextData = currentContextData();
        if( contextData == null || frameIndex >= contextData.frameCount() )
        {
            return result;
        }
        StackFrame frame = contextData.getFrame(frameIndex);
        if( contextData.eventThreadFlag )
        {
            Context cx = Context.getCurrentContext();
            result = do_eval(cx, frame, expr);
        }
        else
        {
            synchronized( monitor )
            {
                if( insideInterruptLoop )
                {
                    evalRequest = expr;
                    evalFrame = frame;
                    monitor.notify();
                    do
                    {
                        try
                        {
                            monitor.wait();
                        }
                        catch( InterruptedException exc )
                        {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    while( evalRequest != null );
                    result = evalResult;
                }
            }
        }
        return result;
    }

    /**
     * Compiles the given script.
     */
    public void compileScript(String url, String text)
    {
        DimIProxy action = new DimIProxy(this, IPROXY_COMPILE_SCRIPT);
        action.url = url;
        action.text = text;

        action.withContext();
    }

    /**
     * Evaluates the given script.
     */
    public void evalScript(final String url, final String text, ScriptEnvironment environment)
    {
        DimIProxy action = new DimIProxy(this, IPROXY_EVAL_SCRIPT, environment);
        action.url = url;
        action.text = text;

        Thread thread = new SessionThread(new WithContextRunable(action));
        Global.storeEnvironmentForThread(thread, environment);
        thread.start();
    }

    protected class WithContextRunable implements Runnable
    {
        protected DimIProxy action;

        public WithContextRunable(DimIProxy action)
        {
            this.action = action;
        }

        @Override
        public void run()
        {
            BiosoftSecurityManager.runInSandbox(() -> {
                try
                {
                    action.withContext();
                }
                catch( RhinoException r )
                {
                    Global.getEnvironment().error("JavaScript error: "+r.getMessage());
                }
                catch( Throwable t )
                {
                    Global.getEnvironment().error(t.toString());
                }
                finally
                {
                    synchronized( Dim.this )
                    {
                        Dim.this.notifyAll();
                    }
                }
            });
        }
    }

    /**
     * Converts the given script object to a string.
     */
    public String objectToString(Object object)
    {
        DimIProxy action = new DimIProxy(this, IPROXY_OBJECT_TO_STRING);
        action.object = object;
        action.withContext();
        return action.stringResult;
    }

    /**
     * Returns whether the given string is syntactically valid script.
     */
    public boolean stringIsCompilableUnit(String str)
    {
        DimIProxy action = new DimIProxy(this, IPROXY_STRING_IS_COMPILABLE);
        action.text = str;
        action.withContext();
        return action.booleanResult;
    }

    /**
     * Returns the value of a property on the given script object.
     */
    public Object getObjectProperty(Object object, Object id)
    {
        DimIProxy action = new DimIProxy(this, IPROXY_OBJECT_PROPERTY);
        action.object = object;
        action.id = id;
        action.withContext();
        return action.objectResult;
    }

    /**
     * Returns an array of the property names on the given script object.
     */
    public Object[] getObjectIds(Object object)
    {
        DimIProxy action = new DimIProxy(this, IPROXY_OBJECT_IDS);
        action.object = object;
        action.withContext();
        return action.objectArrayResult;
    }

    /**
     * Returns the value of a property on the given script object.
     */
    private Object getObjectPropertyImpl(Context cx, Object object, Object id)
    {
        Scriptable scriptable = (Scriptable)object;
        Object result;
        if( id instanceof String )
        {
            String name = (String)id;
            if( name.equals("this") )
            {
                result = scriptable;
            }
            else if( name.equals("__proto__") )
            {
                result = scriptable.getPrototype();
            }
            else if( name.equals("__parent__") )
            {
                result = scriptable.getParentScope();
            }
            else
            {
                result = ScriptableObject.getProperty(scriptable, name);
                if( result == ScriptableObject.NOT_FOUND )
                {
                    result = Undefined.instance;
                }
            }
        }
        else
        {
            int index = ( (Integer)id ).intValue();
            result = ScriptableObject.getProperty(scriptable, index);
            if( result == ScriptableObject.NOT_FOUND )
            {
                result = Undefined.instance;
            }
        }
        return result;
    }

    /**
     * Returns an array of the property names on the given script object.
     */
    private Object[] getObjectIdsImpl(Context cx, Object object)
    {
        if( ! ( object instanceof Scriptable ) || object == Undefined.instance )
        {
            return Context.emptyArgs;
        }

        Object[] ids;
        Scriptable scriptable = (Scriptable)object;
        if( scriptable instanceof DebuggableObject )
        {
            ids = ( (DebuggableObject)scriptable ).getAllIds();
        }
        else
        {
            ids = scriptable.getIds();
        }

        int extra = 0;

        ids = filterContext(ids, object);

        if( extra != 0 )
        {
            Object[] tmp = new Object[extra + ids.length];
            System.arraycopy(ids, 0, tmp, extra, ids.length);
            ids = tmp;
            extra = 0;
        }

        return ids;
    }

    /**
     * Filter context variable list
     */
    private Object[] filterContext(Object[] ids, Object object)
    {
        List<Object> result = new ArrayList<>();
        for( Object id : ids )
        {
            Object obj = getObjectProperty(object, id);
            if( ! ( obj instanceof Function ) && ! ( obj instanceof Undefined ) )
            {
                if( obj instanceof NativeObject && ( (NativeObject)obj ).getClassName().equals("StopIteration") )
                {
                    continue;
                }
                result.add(id);
            }
        }
        return result.toArray(new Object[result.size()]);
    }

    /**
     * Interrupts script execution.
     */
    private void interrupted(Context cx, final StackFrame frame, Throwable scriptException)
    {
        ContextData contextData = frame.contextData();
        boolean eventThreadFlag = callback.isGuiEventThread();
        contextData.eventThreadFlag = eventThreadFlag;

        boolean recursiveEventThreadCall = false;

        interruptedCheck: synchronized( eventThreadMonitor )
        {
            if( eventThreadFlag )
            {
                if( interruptedContextData != null )
                {
                    recursiveEventThreadCall = true;
                    break interruptedCheck;
                }
            }
            else
            {
                while( interruptedContextData != null )
                {
                    try
                    {
                        eventThreadMonitor.wait();
                    }
                    catch( InterruptedException exc )
                    {
                        return;
                    }
                }
            }
            interruptedContextData = contextData;
        }

        if( recursiveEventThreadCall )
        {
            // XXX: For now the following is commented out as on Linux
            // too deep recursion of dispatchNextGuiEvent causes GUI lockout.
            // Note: it can make GUI unresponsive if long-running script
            // will be called on GUI thread while processing another interrupt
            if( false )
            {
                // Run event dispatch until gui sets a flag to exit the initial
                // call to interrupted.
                while( this.returnValue == -1 )
                {
                    try
                    {
                        callback.dispatchNextGuiEvent();
                    }
                    catch( InterruptedException exc )
                    {
                    }
                }
            }
            return;
        }

        if( interruptedContextData == null )
            Kit.codeBug();

        try
        {
            do
            {
                int frameCount = contextData.frameCount();
                this.frameIndex = frameCount - 1;

                final String threadTitle = Thread.currentThread().toString();
                final String alertMessage;
                if( scriptException == null )
                {
                    alertMessage = null;
                }
                else
                {
                    alertMessage = scriptException.toString();
                }

                int returnValue = -1;
                if( !eventThreadFlag )
                {
                    synchronized( monitor )
                    {
                        if( insideInterruptLoop )
                            Kit.codeBug();
                        this.insideInterruptLoop = true;
                        this.evalRequest = null;
                        this.returnValue = -1;
                        callback.enterInterrupt(frame, threadTitle, alertMessage);
                        try
                        {
                            for( ;; )
                            {
                                try
                                {
                                    monitor.wait();
                                }
                                catch( InterruptedException exc )
                                {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                                if( evalRequest != null )
                                {
                                    this.evalResult = null;
                                    try
                                    {
                                        evalResult = do_eval(cx, evalFrame, evalRequest);
                                    }
                                    finally
                                    {
                                        evalRequest = null;
                                        evalFrame = null;
                                        monitor.notify();
                                    }
                                    continue;
                                }
                                if( this.returnValue != -1 )
                                {
                                    returnValue = this.returnValue;
                                    break;
                                }
                            }
                        }
                        finally
                        {
                            insideInterruptLoop = false;
                        }
                    }
                }
                else
                {
                    this.returnValue = -1;
                    callback.enterInterrupt(frame, threadTitle, alertMessage);
                    while( this.returnValue == -1 )
                    {
                        try
                        {
                            callback.dispatchNextGuiEvent();
                        }
                        catch( InterruptedException exc )
                        {
                        }
                    }
                    returnValue = this.returnValue;
                }
                switch( returnValue )
                {
                    case STEP_OVER:
                        contextData.breakNextLine = true;
                        contextData.stopAtFrameDepth = contextData.frameCount();
                        break;
                    case STEP_INTO:
                        contextData.breakNextLine = true;
                        contextData.stopAtFrameDepth = -1;
                        break;
                    case STEP_OUT:
                        if( contextData.frameCount() > 1 )
                        {
                            contextData.breakNextLine = true;
                            contextData.stopAtFrameDepth = contextData.frameCount() - 1;
                        }
                        break;
                }
            }
            while( false );
        }
        finally
        {
            synchronized( eventThreadMonitor )
            {
                interruptedContextData = null;
                eventThreadMonitor.notifyAll();
            }
        }

    }

    /**
     * Evaluates script in the given stack frame.
     */
    private static String do_eval(Context cx, StackFrame frame, String expr)
    {
        String resultString;
        Debugger saved_debugger = cx.getDebugger();
        Object saved_data = cx.getDebuggerContextData();
        int saved_level = cx.getOptimizationLevel();

        cx.setDebugger(null, null);
        cx.setOptimizationLevel( -1);
        cx.setGeneratingDebug(false);
        try
        {
            Callable script = (Callable)cx.compileString(expr, "", 0, null);
            Object result = script.call(cx, frame.scope, frame.thisObj, ScriptRuntime.emptyArgs);
            if( result == Undefined.instance )
            {
                resultString = "";
            }
            else
            {
                resultString = ScriptRuntime.toString(result);
            }
        }
        catch( Exception exc )
        {
            resultString = exc.getMessage();
        }
        finally
        {
            cx.setGeneratingDebug(true);
            cx.setOptimizationLevel(saved_level);
            cx.setDebugger(saved_debugger, saved_data);
        }
        if( resultString == null )
        {
            resultString = "null";
        }
        return resultString;
    }

    /**
     * Proxy class to implement debug interfaces without bloat of class
     * files.
     */
    private static class DimIProxy implements ContextAction, ContextFactory.Listener, Debugger
    {

        /**
         * The debugger.
         */
        private final Dim dim;

        private Dim getDim()
        {
            return dim;
        }

        /**
         * The interface implementation type.  One of the IPROXY_* constants
         * defined in {@link Dim}.
         */
        private final int type;

        /**
         * The URL origin of the script to compile or evaluate.
         */
        private String url;

        /**
         * The text of the script to compile, evaluate or test for compilation.
         */
        private String text;

        /**
         * The object to convert, get a property from or enumerate.
         */
        private Object object;

        /**
         * The property to look up in {@link #object}.
         */
        private Object id;

        /**
         * The boolean result of the action.
         */
        private boolean booleanResult;

        /**
         * The String result of the action.
         */
        private String stringResult;

        /**
         * The Object result of the action.
         */
        private Object objectResult;

        /**
         * Script environment
         */
        private final ScriptEnvironment environment;

        /**
         * The Object[] result of the action.
         */
        private Object[] objectArrayResult;

        /**
         * Creates a new DimIProxy.
         */
        private DimIProxy(Dim dim, int type)
        {
            this(dim, type, null);
        }
        private DimIProxy(Dim dim, int type, ScriptEnvironment environment)
        {
            this.dim = dim;
            this.type = type;
            this.environment = environment;
        }

        protected String preprocess(String text)
        {
            return PreprocessorRegistry.preprocessors().foldLeft( text, (code, prep) -> prep.preprocess( code ) );
        }

        // ContextAction

        /**
         * Performs the action given by {@link #type}.
         */
        @Override
        public Object run(Context cx)
        {
            switch( type )
            {
                case IPROXY_COMPILE_SCRIPT:
                    cx.compileString(preprocess(text), url, 1, null);
                    break;

                case IPROXY_EVAL_SCRIPT:
                {
                    if( dim.scope == null )
                    {
                        dim.scope = new ImporterTopLevel(cx);
                        DataCollection dc = CollectionFactory.getDataCollection("analyses/JavaScript");
                        if( dc != null && ( dc instanceof JScriptVisiblePlugin ) )
                        {
                            JScriptContext.loadExtensions((JScriptVisiblePlugin)dc, ( (JSDocumentContextFactory)dim.contextFactory )
                                    .getContext(), (ScriptableObject)dim.scope);
                        }
                    }

                    if( environment != null )
                    {
                        //send JSEnvironment to all objects via JavaScript scope
                        dim.scope.put(Global.ENVIRONMENT_OBJECT, dim.scope, environment);
                    }

                    Object result = cx.evaluateString(dim.scope, preprocess(text), url, 1, null);
                    if( result != null )
                    {
                        String resultString = JScriptContext.convertToString(result);
                        if( resultString != null )
                        {
                            environment.print(resultString);
                        }
                    }

                    //write to journal
                    Journal journal = JournalRegistry.getCurrentJournal();
                    if( journal != null )
                    {
                        TaskInfo action = journal.getEmptyAction();
                        action.setType(TaskInfo.SCRIPT);
                        action.setData(text);
                        journal.addAction(action);
                    }
                }
                    break;

                case IPROXY_STRING_IS_COMPILABLE:
                    booleanResult = cx.stringIsCompilableUnit(preprocess(text));
                    break;

                case IPROXY_OBJECT_TO_STRING:
                    if( object == Undefined.instance )
                    {
                        stringResult = "undefined";
                    }
                    else if( object == null )
                    {
                        stringResult = "null";
                    }
                    else if( object instanceof NativeCall )
                    {
                        stringResult = "[object Call]";
                    }
                    else
                    {
                        stringResult = Context.toString(object);
                    }
                    break;

                case IPROXY_OBJECT_PROPERTY:
                    objectResult = dim.getObjectPropertyImpl(cx, object, id);
                    break;

                case IPROXY_OBJECT_IDS:
                    objectArrayResult = dim.getObjectIdsImpl(cx, object);
                    break;

                default:
                    throw Kit.codeBug();
            }
            return null;
        }
        /**
         * Performs the action given by {@link #type} with the attached
         * {@link ContextFactory}.
         */
        private void withContext()
        {
            ( (JSDocumentContextFactory)dim.contextFactory ).callInOwnContext(this);
        }

        // ContextFactory.Listener

        /**
         * Called when a Context is created.
         */
        @Override
        public void contextCreated(Context cx)
        {
            if( type != IPROXY_LISTEN )
                Kit.codeBug();
            ContextData contextData = new ContextData();
            Debugger debugger = new DimIProxy(dim, IPROXY_DEBUG, environment);
            cx.setDebugger(debugger, contextData);
            cx.setGeneratingDebug(true);
            cx.setOptimizationLevel( -1);
        }

        /**
         * Called when a Context is destroyed.
         */
        @Override
        public void contextReleased(Context cx)
        {
            if( type != IPROXY_LISTEN )
                Kit.codeBug();
        }

        // Debugger

        /**
         * Returns a StackFrame for the given function or script.
         */
        @Override
        public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript)
        {
            if( type != IPROXY_DEBUG )
                Kit.codeBug();

            FunctionSource item = dim.getFunctionSource(fnOrScript);
            if( item == null )
            {
                // Can not debug if source is not available
                return null;
            }
            return new StackFrame(cx, dim, item);
        }

        /**
         * Called when compilation is finished.
         */
        @Override
        public void handleCompilationDone(Context cx, DebuggableScript fnOrScript, String source)
        {
            if( type != IPROXY_DEBUG )
                Kit.codeBug();

            if( !fnOrScript.isTopLevel() )
            {
                return;
            }
            dim.registerTopScript(fnOrScript, source);
        }
    }

    /**
     * Class to store information about a stack.
     */
    public static class ContextData
    {

        /**
         * The stack frames.
         */
        private final ObjArray frameStack = new ObjArray();

        /**
         * Whether the debugger should break at the next line in this context.
         */
        private boolean breakNextLine;

        /**
         * The frame depth the debugger should stop at.  Used to implement
         * "step over" and "step out".
         */
        private int stopAtFrameDepth = -1;

        /**
         * Whether this context is in the event thread.
         */
        private boolean eventThreadFlag;

        /**
         * The last exception that was processed.
         */
        private Throwable lastProcessedException;

        /**
         * Returns the ContextData for the given Context.
         */
        public static ContextData get(Context cx)
        {
            return (ContextData)cx.getDebuggerContextData();
        }

        /**
         * Returns the number of stack frames.
         */
        public int frameCount()
        {
            return frameStack.size();
        }

        /**
         * Returns the stack frame with the given index.
         */
        public StackFrame getFrame(int frameNumber)
        {
            int num = frameStack.size() - frameNumber - 1;
            return (StackFrame)frameStack.get(num);
        }

        /**
         * Pushes a stack frame on to the stack.
         */
        private void pushFrame(StackFrame frame)
        {
            frameStack.push(frame);
        }

        /**
         * Pops a stack frame from the stack.
         */
        private void popFrame()
        {
            frameStack.pop();
        }
    }

    /**
     * Object to represent one stack frame.
     */
    public static class StackFrame implements DebugFrame
    {

        /**
         * The debugger.
         */
        private final Dim dim;

        /**
         * The ContextData for the Context being debugged.
         */
        private final ContextData contextData;

        /**
         * The scope.
         */
        private Scriptable scope;

        /**
         * The 'this' object.
         */
        private Scriptable thisObj;

        /**
         * Information about the function.
         */
        private final FunctionSource fsource;

        /**
         * Array of breakpoint state for each source line.
         */
        private final boolean[] breakpoints;

        /**
         * Current line number.
         */
        private int lineNumber;

        /**
         * Creates a new StackFrame.
         */
        private StackFrame(Context cx, Dim dim, FunctionSource fsource)
        {
            this.dim = dim;
            this.contextData = ContextData.get(cx);
            this.fsource = fsource;
            this.breakpoints = fsource.sourceInfo().breakpoints;
            this.lineNumber = fsource.firstLine();
        }

        /**
         * Called when the stack frame is entered.
         */
        @Override
        public void onEnter(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
        {
            contextData.pushFrame(this);
            this.scope = scope;
            this.thisObj = thisObj;
            if( dim.breakOnEnter )
            {
                dim.handleBreakpointHit(this, cx);
            }
        }

        /**
         * Called when the current position has changed.
         */
        @Override
        public void onLineChange(Context cx, int lineno)
        {
            this.lineNumber = lineno;

            if( !breakpoints[lineno] && !dim.breakFlag )
            {
                boolean lineBreak = contextData.breakNextLine;
                if( lineBreak && contextData.stopAtFrameDepth >= 0 )
                {
                    lineBreak = ( contextData.frameCount() <= contextData.stopAtFrameDepth );
                }
                if( !lineBreak )
                {
                    return;
                }
                contextData.stopAtFrameDepth = -1;
                contextData.breakNextLine = false;
            }

            dim.handleBreakpointHit(this, cx);
        }

        /**
         * Called when an exception has been thrown.
         */
        @Override
        public void onExceptionThrown(Context cx, Throwable exception)
        {
            dim.handleExceptionThrown(cx, exception, this);
        }

        /**
         * Called when the stack frame has been left.
         */
        @Override
        public void onExit(Context cx, boolean byThrow, Object resultOrException)
        {
            if( dim.breakOnReturn && !byThrow )
            {
                dim.handleBreakpointHit(this, cx);
            }
            contextData.popFrame();
        }

        /**
         * Called when a 'debugger' statement is executed.
         */
        @Override
        public void onDebuggerStatement(Context cx)
        {
            dim.handleBreakpointHit(this, cx);
        }

        /**
         * Returns the SourceInfo object for the function.
         */
        public SourceInfo sourceInfo()
        {
            return fsource.sourceInfo();
        }

        /**
         * Returns the ContextData object for the Context.
         */
        public ContextData contextData()
        {
            return contextData;
        }

        /**
         * Returns the scope object for this frame.
         */
        public Object scope()
        {
            return scope;
        }

        /**
         * Returns the 'this' object for this frame.
         */
        public Object thisObj()
        {
            return thisObj;
        }

        /**
         * Returns the current line number.
         */
        public int getLineNumber()
        {
            return lineNumber;
        }
    }

    /**
     * Class to store information about a function.
     */
    public static class FunctionSource
    {

        /**
         * Information about the source of the function.
         */
        private final SourceInfo sourceInfo;

        /**
         * Line number of the first line of the function.
         */
        private final int firstLine;

        /**
         * The function name.
         */
        private final String name;

        /**
         * Creates a new FunctionSource.
         */
        private FunctionSource(SourceInfo sourceInfo, int firstLine, String name)
        {
            if( name == null )
                throw new IllegalArgumentException();
            this.sourceInfo = sourceInfo;
            this.firstLine = firstLine;
            this.name = name;
        }

        /**
         * Returns the SourceInfo object that describes the source of the
         * function.
         */
        public SourceInfo sourceInfo()
        {
            return sourceInfo;
        }

        /**
         * Returns the line number of the first line of the function.
         */
        public int firstLine()
        {
            return firstLine;
        }

        /**
         * Returns the name of the function.
         */
        public String name()
        {
            return name;
        }
    }

    /**
     * Class to store information about a script source.
     */
    public static class SourceInfo
    {

        /**
         * An empty array of booleans.
         */
        private static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];

        /**
         * The script.
         */
        private final String source;

        /**
         * Array indicating which lines can have breakpoints set.
         */
        private boolean[] breakableLines;

        /**
         * Array indicating whether a breakpoint is set on the line.
         */
        private boolean[] breakpoints;

        /**
         * Array of FunctionSource objects for the functions in the script.
         */
        private final FunctionSource[] functionSources;

        /**
         * Creates a new SourceInfo object.
         */
        public SourceInfo(String source, DebuggableScript[] functions)
        {
            this.source = source;

            int N = functions.length;
            int[][] lineArrays = new int[N][];
            for( int i = 0; i != N; ++i )
            {
                lineArrays[i] = functions[i].getLineNumbers();
            }

            int minAll = 0, maxAll = -1;
            int[] firstLines = new int[N];
            for( int i = 0; i != N; ++i )
            {
                int[] lines = lineArrays[i];
                if( lines == null || lines.length == 0 )
                {
                    firstLines[i] = -1;
                }
                else
                {
                    int min, max;
                    min = max = lines[0];
                    for( int j = 1; j != lines.length; ++j )
                    {
                        int line = lines[j];
                        if( line < min )
                        {
                            min = line;
                        }
                        else if( line > max )
                        {
                            max = line;
                        }
                    }
                    firstLines[i] = min;
                    if( minAll > maxAll )
                    {
                        minAll = min;
                        maxAll = max;
                    }
                    else
                    {
                        if( min < minAll )
                        {
                            minAll = min;
                        }
                        if( max > maxAll )
                        {
                            maxAll = max;
                        }
                    }
                }
            }

            if( minAll > maxAll )
            {
                // No line information
                this.breakableLines = EMPTY_BOOLEAN_ARRAY;
                this.breakpoints = EMPTY_BOOLEAN_ARRAY;
            }
            else
            {
                if( minAll < 0 )
                {
                    // Line numbers can not be negative
                    throw new IllegalStateException(String.valueOf(minAll));
                }
                int linesTop = maxAll + 1;
                this.breakableLines = new boolean[linesTop];
                this.breakpoints = new boolean[linesTop];
                for( int i = 0; i != N; ++i )
                {
                    int[] lines = lineArrays[i];
                    if( lines != null && lines.length != 0 )
                    {
                        for( int j = 0; j != lines.length; ++j )
                        {
                            int line = lines[j];
                            this.breakableLines[line] = true;
                        }
                    }
                }
            }
            this.functionSources = new FunctionSource[N];
            for( int i = 0; i != N; ++i )
            {
                String name = TextUtil2.nullToEmpty( functions[i].getFunctionName() );
                this.functionSources[i] = new FunctionSource(this, firstLines[i], name);
            }
        }

        /**
         * Returns the source text.
         */
        public String source()
        {
            return this.source;
        }

        /**
         * Returns the number of FunctionSource objects stored in this object.
         */
        public int functionSourcesTop()
        {
            return functionSources.length;
        }

        /**
         * Returns the FunctionSource object with the given index.
         */
        public FunctionSource functionSource(int i)
        {
            return functionSources[i];
        }

        /**
         * Returns whether the given line number can have a breakpoint set on
         * it.
         */
        public boolean breakableLine(int line)
        {
            return ( line < this.breakableLines.length ) && this.breakableLines[line];
        }

        /**
         * Returns whether there is a breakpoint set on the given line.
         */
        public boolean breakpoint(int line)
        {
            if( !breakableLine(line) )
            {
                throw new IllegalArgumentException(String.valueOf(line));
            }
            return line < this.breakpoints.length && this.breakpoints[line];
        }

        /**
         * Sets or clears the breakpoint flag for the given line.
         */
        public boolean breakpoint(int line, boolean value)
        {
            if( !breakableLine(line) )
            {
                throw new IllegalArgumentException(String.valueOf(line));
            }
            boolean changed;
            synchronized( breakpoints )
            {
                if( breakpoints[line] != value )
                {
                    breakpoints[line] = value;
                    changed = true;
                }
                else
                {
                    changed = false;
                }
            }
            return changed;
        }

        /**
         * Removes all breakpoints from the script.
         */
        public void removeAllBreakpoints()
        {
            synchronized( breakpoints )
            {
                for( int line = 0; line != breakpoints.length; ++line )
                {
                    breakpoints[line] = false;
                }
            }
        }
    }
}
