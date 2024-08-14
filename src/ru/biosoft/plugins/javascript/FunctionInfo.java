package ru.biosoft.plugins.javascript;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * Provides JavaScript function description, and description for its arguments,
 * throwable exception and returned value. Generally function info extracts the information
 * from the <code>ru.biosoft.plugins.javascript.function</code> extension point.
 *
 * <p><code>FunctionInfo</code> implements <code>ru.biosoft.access.core.DataElement</code> interface,
 * so it can be shown in repository tree.
 *
 * @see JScriptContext#defineFunction
 */
public class FunctionInfo extends DataElementSupport
{
    /**
     * Creates info for the specified JavaScript function.
     *
     * @param name - JavaScript function name
     * @param parent <code>DataCollection</code> to which this function belongs.
     * Generally it is <code>plugins/Javascript/functions</code>.
     */
    public FunctionInfo(String name, DataCollection parent)
    {
        super(name, parent);
    }
    
    protected void doInit()
    {
    }
    
    private boolean initialized = false;
    private synchronized void init()
    {
        if(!initialized) doInit();
        initialized = true;
    }

    @Override
    public String toString()
    {
        init();
        StringBuffer buf = new StringBuffer("\n");

        buf.append("<h2 align=center>JavaScript function</h2>");
        buf.append(getFunctionDeclaration());
        buf.append("\n");

        if( description != null && description.length() > 0 )
        {
            buf.append("<p>");
            buf.append(description);
            buf.append("\n");
        }

        // Parameters:
        for( int j = 0; j < arguments.size(); j++ )
        {
            Argument[] args = arguments.get(j);
            startSection(buf, "Parameters" + ( arguments.size() == 1 ? "" : "(" + ( j + 1 ) + ")" ));
            buf.append("\n");

            for( int i = 0; i < args.length; i++ )
            {
                if( i > 0 )
                    buf.append("<br>");

                buf.append("<code>");

                if( !args[i].isObligatory() )
                    buf.append("[");

                buf.append(args[i].getName());
                if( !args[i].isObligatory() )
                    buf.append("]");

                buf.append("</code>");

                buf.append(" - ");
                buf.append(args[i].getDescription());
                buf.append("\n");
            }

            endSection(buf);
        }

        // Returns:
        if( returnedValue.description != null && returnedValue.description.length() > 0 )
        {
            startSection(buf, "Returns");

            buf.append(returnedValue.description);

            endSection(buf);
            buf.append("\n");
        }

        // Throws:
        if( exceptions != null && exceptions.length > 0 )
        {
            startSection(buf, "Throws");
            buf.append("\n");

            for( int i = 0; i < exceptions.length; i++ )
            {
                buf.append("<code>");
                buf.append(exceptions[i].getType());
                buf.append("</code>");

                buf.append(" - ");
                buf.append(exceptions[i].getDescription());
                buf.append("\n");
                buf.append("<br>");
            }

            endSection(buf);
        }

        // Examples:
        if( examples != null && examples.length > 0 )
        {
            startSection(buf, "Examples");
            
            buf.append("\n<ul>");
            for( int i = 0; i < examples.length; i++ )
            {
                buf.append("\n");
                buf.append("<li>");
                buf.append("<pre>");
                buf.append(examples[i].getCode());
                buf.append("</pre>");
                buf.append("\n");

                if( examples[i].getDescription() != null && examples[i].getDescription().length() > 0 )
                {
                    buf.append(examples[i].getDescription());
                    buf.append("\n");
                }
            }
            buf.append("\n</ul>");

            endSection(buf);
        }

        return buf.toString();
    }

    protected void startSection(StringBuffer buf, String section)
    {
        buf.append("<p><b>");
        buf.append(section);
        buf.append(": ");
        buf.append("</b><blockquote>");
    }

    protected void endSection(StringBuffer buf)
    {
        buf.append("</blockquote>");
        buf.append("\n");
    }

    public String getFunctionDeclaration()
    {
        init();
        StringBuffer buf = new StringBuffer();

        for( Argument[] args : arguments )
        {
            if( returnedValue.type != null && returnedValue.type.length() > 0 )
                buf.append(returnedValue.type);
            else
                buf.append("void");

            buf.append(' ');
            buf.append("<b>");
            buf.append(getName());
            buf.append("</b>");
            buf.append('(');

            if( args != null && args.length > 0 )
            {
                for( int i = 0; i < args.length; i++ )
                {
                    if( !args[i].isObligatory() )
                        buf.append("[");

                    if( i > 0 )
                        buf.append(", ");

                    buf.append(args[i].getType());
                    if( args[i].getName() != null && args[i].getName().length() > 0 )
                    {
                        buf.append(' ');
                        buf.append(args[i].getName());
                    }

                    if( !args[i].isObligatory() )
                        buf.append("]");
                }
            }

            buf.append(')');

            if( exceptions != null && exceptions.length > 0 )
            {
                buf.append(" throws ");
                for( int i = 0; i < exceptions.length; i++ )
                {
                    if( i > 0 )
                        buf.append(", ");

                    buf.append(exceptions[i].getType());
                }
            }

            buf.append("\n");
            buf.append("<br>");
        }

        return buf.toString();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    /** The function description */
    protected String description;

    public String getDescription()
    {
        init();
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    /** Function arguments description. */
    protected List<Argument[]> arguments = new ArrayList<>();

    public void addArguments(Argument[] arguments)
    {
        this.arguments.add(arguments);
    }

    /** The returned value type and description. */
    protected ReturnedValue returnedValue = new ReturnedValue();

    public ReturnedValue getReturnedValue()
    {
        init();
        return returnedValue;
    }

    public void setReturnedValue(ReturnedValue returnedValue)
    {
        this.returnedValue = returnedValue;
    }

    /** Function exception description. */
    protected ExceptionInfo[] exceptions;

    public ExceptionInfo[] getExceptions()
    {
        init();
        return exceptions;
    }

    public void setExceptions(ExceptionInfo[] exceptions)
    {
        this.exceptions = exceptions;
    }

    /** Examples of function usage. */
    protected Example[] examples;

    public Example[] getExamples()
    {
        init();
        return examples;
    }

    public void setExamples(Example[] examples)
    {
        this.examples = examples;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Meta data issues
    //

    public boolean isExceptionsHidden()
    {
        init();
        return exceptions == null || exceptions.length == 0;
    }

    public boolean isExamplesHidden()
    {
        init();
        return examples == null || examples.length == 0;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Internal classes
    //

    /**
     * This class describes the JavaScript function argument.
     */
    public static class Argument
    {
        public Argument()
        {
        }

        public Argument(String name, String type, String description)
        {
            this.name = name;
            this.type = type;
            this.description = description;
        }

        /** The argument name */
        private String name;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        /** The argument type */
        private String type;

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }

        /** Indicates whether this function argument is obligatory */
        private boolean obligatory = true;

        public boolean isObligatory()
        {
            return obligatory;
        }

        public void setObligatory(boolean obligatory)
        {
            this.obligatory = obligatory;
        }

        /** The argument description */
        private String description;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }

    public static class ArgumentBeanInfo extends BeanInfoEx
    {
        public ArgumentBeanInfo()
        {
            super(Argument.class, "ru.biosoft.plugins.javascript.MessageBundle");
            beanDescriptor.setDisplayName(getResourceString("CN_ARGUMENT"));
            beanDescriptor.setShortDescription(getResourceString("CD_ARGUMENT"));
        }

        @Override
        public void initProperties() throws java.lang.Exception
        {
            PropertyDescriptorEx pde = new PropertyDescriptorEx("type", beanClass);
            pde.setReadOnly(true);
            add(pde, getResourceString("PN_ARGUMENT_TYPE"), getResourceString("PD_ARGUMENT_TYPE"));

            pde = new PropertyDescriptorEx("name", beanClass);
            pde.setReadOnly(true);
            add(pde, getResourceString("PN_ARGUMENT_NAME"), getResourceString("PD_ARGUMENT_NAME"));

            pde = new PropertyDescriptorEx("obligatory", beanClass);
            pde.setReadOnly(true);
            add(pde, getResourceString("PN_ARGUMENT_OBLIGATORY"), getResourceString("PD_ARGUMENT_OBLIGATORY"));

            pde = new PropertyDescriptorEx("description", beanClass);
            pde.setReadOnly(true);
            add(pde, getResourceString("PN_FUNCTION_NAME"), getResourceString("PD_FUNCTION_NAME"));
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Describes the returned value.
     */
    public class ReturnedValue
    {
        /** The returned value type */
        private String type;

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }

        /** The returned value description */
        private String description;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }

    public static class ReturnedValueBeanInfo extends BeanInfoEx
    {
        public ReturnedValueBeanInfo()
        {
            super(ReturnedValue.class, "ru.biosoft.plugins.javascript.MessageBundle");
            beanDescriptor.setDisplayName(getResourceString("CN_RETURNED_VALUE"));
            beanDescriptor.setShortDescription(getResourceString("CD_RETURNED_VALUE"));
        }

        @Override
        public void initProperties() throws java.lang.Exception
        {
            PropertyDescriptorEx pde = new PropertyDescriptorEx("type", beanClass);
            pde.setReadOnly(true);
            add(pde, getResourceString("PN_RETURNED_VALUE_TYPE"), getResourceString("PD_RETURNED_VALUE_TYPE"));

            pde = new PropertyDescriptorEx("description", beanClass);
            pde.setReadOnly(true);
            add(pde, getResourceString("PN_RETURNED_VALUE_DESCRIPTION"), getResourceString("PD_RETURNED_VALUE_DESCRIPTION"));
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Describes exceptions throwable by function.
     */
    public static class ExceptionInfo
    {
        public ExceptionInfo()
        {
        }

        public ExceptionInfo(String type, String description)
        {
            this.type = type;
            this.description = description;
        }

        /** The exception type */
        private String type;

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }

        /** The exception description */
        private String description;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }

    public static class ExceptionInfoBeanInfo extends BeanInfoEx
    {
        public ExceptionInfoBeanInfo()
        {
            super(ExceptionInfo.class, "ru.biosoft.plugins.javascript.MessageBundle");
            beanDescriptor.setDisplayName(getResourceString("CN_EXCEPTION"));
            beanDescriptor.setShortDescription(getResourceString("CD_EXCEPTION"));
        }

        @Override
        public void initProperties() throws java.lang.Exception
        {
            PropertyDescriptorEx pde = new PropertyDescriptorEx("type", beanClass);
            pde.setReadOnly(true);
            add(pde, getResourceString("PN_EXCEPTION_TYPE"), getResourceString("PD_EXCEPTION_TYPE"));

            pde = new PropertyDescriptorEx("description", beanClass);
            pde.setReadOnly(true);
            add(pde, getResourceString("PN_EXCEPTION_DESCRIPTION"), getResourceString("PD_EXCEPTION_DESCRIPTION"));
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Exampe of function usage.
     */
    public static class Example
    {
        public Example()
        {
        }

        public Example(String code, String description)
        {
            this.code = code;
            this.description = description;
        }

        /** The example code */
        private String code;

        public String getCode()
        {
            return code;
        }

        public void setCode(String code)
        {
            this.code = code;
        }

        /** The example code description */
        private String description;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }

    public static class ExampleBeanInfo extends BeanInfoEx
    {
        public ExampleBeanInfo()
        {
            super(Example.class, "ru.biosoft.plugins.javascript.MessageBundle");
            beanDescriptor.setDisplayName(getResourceString("CN_EXAMPLE"));
            beanDescriptor.setShortDescription(getResourceString("CD_EXAMPLE"));
        }

        @Override
        public void initProperties() throws java.lang.Exception
        {
            PropertyDescriptorEx pde = new PropertyDescriptorEx("code", beanClass);
            pde.setReadOnly(true);
            add(pde, getResourceString("PN_EXAMPLE_CODE"), getResourceString("PD_EXAMPLE_CODE"));

            pde = new PropertyDescriptorEx("description", beanClass);
            pde.setReadOnly(true);
            add(pde, getResourceString("PN_EXAMPLE_DESCRIPTION"), getResourceString("PD_EXAMPLE_DESCRIPTION"));
        }
    }

}
