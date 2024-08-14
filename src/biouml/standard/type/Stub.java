package biouml.standard.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.BeanInfoEx;

@SuppressWarnings ( "serial" )
public class Stub extends BaseSupport
{
    public Stub(DataCollection<?> parent, String name)
    {
        super(parent, name, TYPE_UNKNOWN);
    }

    public Stub(DataCollection<?> parent, String name, String type)
    {
        super(parent, name, type);
    }

    @Override
    public int hashCode()
    {
        return ( getName() + getType() ).hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if( obj instanceof Base )
        {
            Base base = (Base)obj;
            return getType().equals(base.getType()) && getName().equals(base.getName());
        }

        return false;
    }

    ///////////////////////////////////////////////////////////////////
    // Special class for Note
    //

    @PropertyName("Note")
    @PropertyDescription("Arbitrary HTML text that will be shown<br>" + "on the diagram as text box.")
    public static class Note extends Stub
    {
        protected boolean backgroundVisible = true;
        
        public Note(DataCollection<?> parent, String name)
        {
            super(parent, name, TYPE_NOTE);
        }
        
        @PropertyName("Background visible")
        @PropertyDescription("Indicates whether note background should be shown.")
        public boolean isBackgroundVisible()
        {
            return backgroundVisible;
        }
        public void setBackgroundVisible(boolean backgroundVisible)
        {
            this.backgroundVisible = backgroundVisible;
        }  
    }

    public static class NoteBeanInfo extends BeanInfoEx2<Note>
    {
        public NoteBeanInfo()
        {
            super(Note.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            add("backgroundVisible");
        }
    }

    public static class NoteLink extends Stub
    {
        public NoteLink(DataCollection<?> parent, String name)
        {
            super(parent, name, TYPE_NOTE_LINK);
        }
    }

    public static class NoteLinkBeanInfo extends BeanInfoEx
    {
        public NoteLinkBeanInfo()
        {
            super(NoteLink.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_NOTE_LINK"));
            beanDescriptor.setShortDescription(getResourceString("CD_NOTE_LINK"));
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Special classes for ConnectionPorts
    //

    public static class ConnectionPort extends Stub
    {
        public static final String SUFFIX = "_port";
        
        public static final String VARIABLE_NAME_ATTR = "variableName";
        public static final String ACCESS_TYPE = "accessType";
        public static final String PORT_TYPE = "portType";
        public static final String PORT_ORIENTATION = "orientation";
        
        //is used for propagated ports
        public static final String BASE_PORT_NAME_ATTR = "basePortName";
        
        public static final String BASE_MODULE_NAME_ATTR = "baseModuleName";
        //access type
        public static final String PROPAGATED = "propagated"; //corresponds to module port (on modular diagram), not variable
        public static final String PUBLIC = "public"; //is visible from outside of the module
        public static final String PRIVATE = "private"; //is visible only inside module
        

        public static final Map<String, Class<? extends Base>> typeNameToType = new HashMap<String, Class<? extends Base>>()
        {
            {
                put( "input", InputConnectionPort.class );
                put( "output", OutputConnectionPort.class );
                put( "contact", ContactConnectionPort.class );
            }
        };

        public static final Set<String> portFullTypes = Collections.unmodifiableSet( new HashSet<>( Arrays.asList(
                ConnectionPort.TYPE_INPUT_CONNECTION_PORT, ConnectionPort.TYPE_OUTPUT_CONNECTION_PORT,
                ConnectionPort.TYPE_CONTACT_CONNECTION_PORT ) ) );


        public static final Map<String, String> shortNameToFull = new HashMap<String, String>()
        {
            {
                put("input", Type.TYPE_INPUT_CONNECTION_PORT);
                put("output", Type.TYPE_OUTPUT_CONNECTION_PORT);
                put("contact", Type.TYPE_CONTACT_CONNECTION_PORT);
            }
        };
        
        public static Class<? extends ConnectionPort> getOppositeClass(Class type) throws IllegalArgumentException
        {
            if( !ConnectionPort.class.isAssignableFrom( type ) )
                throw new IllegalArgumentException( "Can not get opposite of " + type + "Only ConnectionPort is allowed." );
            if( type.equals( InputConnectionPort.class ) )
                return OutputConnectionPort.class;
            if( type.equals( OutputConnectionPort.class ) )
                return InputConnectionPort.class;
            return ContactConnectionPort.class;
        }
                
        public ConnectionPort(String name, DataCollection<?> parent, String type)
        {
            super(parent, name, type);
        }

        public static ConnectionPort createPortByType(DataCollection<?> parent, String name, String type)
        {
            if( type.equals(Type.TYPE_INPUT_CONNECTION_PORT) || type.equals("input") )
            {
                return new Stub.InputConnectionPort(parent, name);
            }
            else if( type.equals(Type.TYPE_OUTPUT_CONNECTION_PORT) || type.equals("output") )
            {
                return new Stub.OutputConnectionPort(parent, name);
            }
            else if( type.equals(Type.TYPE_CONTACT_CONNECTION_PORT) || type.equals("contact") )
            {
                return new Stub.ContactConnectionPort(parent, name);
            }
            return new ConnectionPort(name, parent, type);
        }
    }

    public static class InputConnectionPort extends Stub.ConnectionPort
    {
        public InputConnectionPort(DataCollection<?> parent, String name)
        {
            super(name, parent, TYPE_INPUT_CONNECTION_PORT);
        }
    }

    public static class InputConnectionPortBeanInfo extends BeanInfoEx
    {
        public InputConnectionPortBeanInfo()
        {
            super(InputConnectionPort.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_INPUT_CONNECTION_PORT"));
            beanDescriptor.setShortDescription(getResourceString("CD_INPUT_CONNECTION_PORT"));
        }
    }

    public static class ContactConnectionPort extends Stub.ConnectionPort
    {
        public ContactConnectionPort(DataCollection<?> parent, String name)
        {
            super(name, parent, TYPE_CONTACT_CONNECTION_PORT);
        }
    }

    public static class ContactConnectionPortBeanInfo extends BeanInfoEx
    {
        public ContactConnectionPortBeanInfo()
        {
            super(ContactConnectionPort.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_CONTACT_CONNECTION_PORT"));
            beanDescriptor.setShortDescription(getResourceString("CD_CONTACT_CONNECTION_PORT"));
        }
    }

    public static class OutputConnectionPort extends Stub.ConnectionPort
    {
        public OutputConnectionPort(DataCollection<?> parent, String name)
        {
            super(name, parent, TYPE_OUTPUT_CONNECTION_PORT);
        }
    }

    public static class OutputConnectionPortBeanInfo extends BeanInfoEx
    {
        public OutputConnectionPortBeanInfo()
        {
            super(OutputConnectionPort.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_OUTPUT_CONNECTION_PORT"));
            beanDescriptor.setShortDescription(getResourceString("CD_OUTPUT_CONNECTION_PORT"));
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Special classes for bus stubs
    //
    public static class Bus extends Stub
    {
        public Bus(DataCollection<?> parent, String name)
        {
            super(parent, name, TYPE_CONNECTION_BUS);
        }
    }

    public static class BusBeanInfo extends BeanInfoEx
    {
        public BusBeanInfo()
        {
            super(Bus.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_CONNECTION_BUS"));
            beanDescriptor.setShortDescription(getResourceString("CD_CONNECTION_BUS"));
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Special classes for edge stubs
    //
    public static class DirectedConnection extends Stub
    {
        public DirectedConnection(DataCollection<?> parent, String name)
        {
            super(parent, name, TYPE_DIRECTED_LINK);
        }
    }

    public static class DirectedConnectionBeanInfo extends BeanInfoEx
    {
        public DirectedConnectionBeanInfo()
        {
            super(DirectedConnection.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_DIRECTED_LINK"));
            beanDescriptor.setShortDescription(getResourceString("CD_DIRECTED_LINK"));
        }
    }

    public static class UndirectedConnection extends Stub
    {
        public UndirectedConnection(DataCollection<?> parent, String name)
        {
            super(parent, name, TYPE_UNDIRECTED_LINK);
        }
    }

    public static class UndirectedConnectionBeanInfo extends BeanInfoEx
    {
        public UndirectedConnectionBeanInfo()
        {
            super(UndirectedConnection.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_UNDIRECTED_LINK"));
            beanDescriptor.setShortDescription(getResourceString("CD_UNDIRECTED_LINK"));
        }
    }

    public static class Dependency extends Stub
    {
        public Dependency(DataCollection<?> parent, String name)
        {
            super(parent, name, TYPE_DEPENDENCY);
        }
    }

    public static class DependencyBeanInfo extends BeanInfoEx
    {
        public DependencyBeanInfo()
        {
            super(Dependency.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_DEPENDENCY"));
            beanDescriptor.setShortDescription(getResourceString("CD_DEPENDENCY"));
        }
    }

    /**
    /* Special class for Plot stub
     */
    public static class PlotElement extends Stub
    {
        public PlotElement(DataCollection<?> parent, String name)
        {
            super(parent, name, TYPE_PLOT);
        }
    }

    public static class PlotElementBeanInfo extends BeanInfoEx
    {
        public PlotElementBeanInfo()
        {
            super(PlotElement.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_PLOT_ELEMENT"));
            beanDescriptor.setShortDescription(getResourceString("CD_PLOT_ELEMENT"));
        }
    }

    /**
    /* Special class for Adapter stub
     */
    public static class AveragerElement extends Stub
    {
        public AveragerElement(DataCollection<?> parent, String name)
        {
            super(parent, name, TYPE_AVERAGER);
        }
    }

    public static class AveragerElementBeanInfo extends BeanInfoEx
    {
        public AveragerElementBeanInfo()
        {
            super(AveragerElement.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_AVERAGER_ELEMENT"));
            beanDescriptor.setShortDescription(getResourceString("CD_AVERAGER_ELEMENT"));
        }
    }

    /**
    /* Special class for Switch stub
     */
    public static class SwitchElement extends Stub
    {
        public SwitchElement(DataCollection<?> parent, String name)
        {
            super(parent, name, TYPE_SWITCH);
        }
    }

    public static class SwitchElementBeanInfo extends BeanInfoEx
    {
        public SwitchElementBeanInfo()
        {
            super(SwitchElement.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_SWITCH_ELEMENT"));
            beanDescriptor.setShortDescription(getResourceString("CD_SWITCH_ELEMENT"));
        }
    }
    
    public static class Constant extends Stub
    {
        public Constant(DataCollection<?> parent, String name)
        {
            super(parent, name, TYPE_CONSTANT);
        }
  
    }

    public static class ConstantBeanInfo extends BeanInfoEx
    {
        public ConstantBeanInfo() throws Exception
        {
            super(ConstantBeanInfo.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_CONSTANT"));
            beanDescriptor.setShortDescription(getResourceString("CD_CONSTANT"));
        }
    }
    
    public static class SubDiagramKernel extends Stub
    {
        public SubDiagramKernel(DataCollection<?> parent, String name)
        {
            super(parent, name, Type.TYPE_SUBDIAGRAM);
        }
  
    }

    public static class SubDiagramKernelBeanInfo extends BeanInfoEx
    {
        public SubDiagramKernelBeanInfo() throws Exception
        {
            super(SubDiagramKernelBeanInfo.class, MessageBundle.class.getName());

            beanDescriptor.setDisplayName(getResourceString("CN_SUBDIAGRAM_ELEMENT"));
            beanDescriptor.setShortDescription(getResourceString("CD_SUBDIAGRAM_ELEMENT"));
        }
    }
}