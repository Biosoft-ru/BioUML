package ru.biosoft.plugins.javascript;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

import ru.biosoft.access.script.ScriptJobControl;
import ru.biosoft.access.script.ScriptJobControl.BreakType;
import ru.biosoft.access.script.ScriptJobControl.ScriptStackTraceElement;

public class JScriptDebugger implements Debugger
{
    private final JSStackFrame debugFrame = new JSStackFrame();
    boolean isDisconnected;
    volatile boolean paused = false;
    int currentLine;
    BreakType type = ScriptJobControl.BreakType.NONE;
    int depth = 0;
    Runnable pauseHandler;
    TIntSet breakpoints = new TIntHashSet();
    List<ScriptStackTraceElement> stack = new ArrayList<>();
    Set<String> startVars = Collections.emptySet();

    public boolean isDisconnected()
    {
        return isDisconnected;
    }

    public void setDisconnected(boolean isDisconnected)
    {
        this.isDisconnected = isDisconnected;
    }

    public void setBreakType(ScriptJobControl.BreakType type)
    {
        this.type = type;
        this.depth = 0;
    }
    
    public void init()
    {
        this.depth = -1;
    }

    public void setPauseHandler(Runnable runnable)
    {
        pauseHandler = runnable;
    }

    @Override
    public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript)
    {
        String name = "";
        if(fnOrScript.isTopLevel())
        {
            name = "(top)";
        } else if(fnOrScript.isFunction())
        {
            name = IntStreamEx.range( fnOrScript.getParamCount() ).mapToObj( fnOrScript::getParamOrVarName )
                    .joining( ", ", String.valueOf( fnOrScript.getFunctionName() ) + "(", ")" );
        }
        stack.add( new ScriptStackTraceElement( currentLine, name ) );
        return debugFrame;
    }
    
    public int getCurrentLine()
    {
        return currentLine;
    }

    @Override
    public void handleCompilationDone(Context ctx, DebuggableScript script, String arg2)
    {
    }

    public void addBreakpoints(int... lines)
    {
        breakpoints.addAll( lines );
    }

    public void removeBreakpoints(int... lines)
    {
        breakpoints.removeAll( lines );
    }

    public void clearBreakpoints()
    {
        breakpoints.clear();
    }

    public ScriptStackTraceElement[] getStackTrace()
    {
        return StreamEx.of( stack ).append( new ScriptStackTraceElement( currentLine, null ) )
            .pairMap( (prev, cur) -> new ScriptStackTraceElement( cur.getLine(), prev.getMethod() ) )
            .toArray( ScriptStackTraceElement[]::new );
    }

    public List<String> getVariables()
    {
        return StreamEx.of( debugFrame.scope.getIds() ).map( Object::toString ).remove( startVars::contains ).sorted().toList();
    }

    public Object calc(String expr)
    {
        if(!paused)
        {
            throw new IllegalStateException();
        }
        Context cx = Context.enter();
        Debugger saved_debugger = cx.getDebugger();
        Object saved_data = cx.getDebuggerContextData();
        int saved_level = cx.getOptimizationLevel();

        cx.setDebugger(null, null);
        cx.setOptimizationLevel(-1);
        cx.setGeneratingDebug(false);
        try {
            Callable script = (Callable)cx.compileString(expr, "", 0, null);
            Object result = script.call(cx, debugFrame.scope, debugFrame.thisObj,
                                        ScriptRuntime.emptyArgs);
            return result;
        } catch (Exception exc) {
            return exc;
        } finally {
            cx.setGeneratingDebug(true);
            cx.setOptimizationLevel(saved_level);
            cx.setDebugger(saved_debugger, saved_data);
        }
    }

    class JSStackFrame implements DebugFrame
    {
        public Scriptable thisObj;
        public Scriptable scope;

        @Override
        public void onEnter(Context cx, Scriptable activation, Scriptable thisObj, Object[] args)
        {
            this.thisObj = thisObj;
            this.scope = activation;
            depth++;
        }

        private void pause()
        {
            type = ScriptJobControl.BreakType.NONE;
            if(pauseHandler != null)
            {
                paused = true;
                try
                {
                    pauseHandler.run();
                }
                finally
                {
                    paused = false;
                }
            }
        }

        @Override
        public void onLineChange(Context cx, int lineNumber)
        {
            currentLine = lineNumber;
            if( isDisconnected )
            {
                throw new CancellationException("Cancelled by user request");
            }
            if( breakpoints.contains( lineNumber ) || type == ScriptJobControl.BreakType.STEP_IN
                    || ( type == ScriptJobControl.BreakType.STEP_OVER && depth <= 0 ) || type.getLine() == lineNumber )
            {
                pause();
            }
        }

        @Override
        public void onExceptionThrown(Context cx, Throwable ex)
        {
        }

        @Override
        public void onExit(Context cx, boolean byThrow, Object resultOrException)
        {
            stack.remove( stack.size()-1 );
            depth--;
            if( type == ScriptJobControl.BreakType.STEP_OUT )
            {
                type = ScriptJobControl.BreakType.STEP_IN;
            }
        }

        @Override
        public void onDebuggerStatement(Context cx)
        {
            pause();
        }
    }
}
