package ru.biosoft.access.script;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import ru.biosoft.jobcontrol.AbstractJobControl;

public abstract class ScriptJobControl extends AbstractJobControl
{
    private static final Logger log = Logger.getLogger( ScriptJobControl.class.getName() );

    public static class BreakType
    {
        public static final BreakType NONE = new BreakType();
        public static final BreakType STEP_IN = new BreakType();
        public static final BreakType STEP_OVER = new BreakType();
        public static final BreakType STEP_OUT = new BreakType();

        final int line;

        private BreakType()
        {
            this(-1);
        }

        public BreakType(int line)
        {
            this.line = line;
        }

        public int getLine()
        {
            return line;
        }

        public static BreakType line(int n)
        {
            return new BreakType(n);
        }
    }

    public static class ScriptStackTraceElement
    {
        private final int line;
        private final String method;

        public ScriptStackTraceElement(int line, String method)
        {
            super();
            this.line = line;
            this.method = method;
        }

        public int getLine()
        {
            return line;
        }
        public String getMethod()
        {
            return method;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + line;
            result = prime * result + ( ( method == null ) ? 0 : method.hashCode() );
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if( this == obj )
                return true;
            if( obj == null )
                return false;
            if( getClass() != obj.getClass() )
                return false;
            ScriptStackTraceElement other = (ScriptStackTraceElement)obj;
            if( line != other.line )
                return false;
            if( method == null )
            {
                if( other.method != null )
                    return false;
            }
            else if( !method.equals( other.method ) )
                return false;
            return true;
        }

        @Override
        public String toString()
        {
            return "[at "+line+"]: "+method;
        }
    }

    public ScriptJobControl()
    {
        super(log);
    }

    public void breakOn(BreakType type)
    {
    }

    public void addBreakpoints(int... lines)
    {
    }

    public void removeBreakpoints(int... lines)
    {
    }

    public void clearBreakpoints()
    {
    }

    public List<String> getVariables()
    {
        return Collections.emptyList();
    }

    public Object calc(String expression)
    {
        return "["+expression+"]";
    }

    public Object objectToString(Object obj)
    {
        return String.valueOf(obj);
    }

    public String getObjectType(Object obj)
    {
        return obj == null ? "null" : obj.getClass().getSimpleName();
    }

    public ScriptStackTraceElement[] getStackTrace()
    {
        return new ScriptStackTraceElement[] {new ScriptStackTraceElement( getCurrentLine(), "(top)" )};
    }

    public int getCurrentLine()
    {
        return -1;
    }

    abstract public String getResult();
}