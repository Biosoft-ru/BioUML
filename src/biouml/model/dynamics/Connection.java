package biouml.model.dynamics;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Node;
import ru.biosoft.util.bean.BeanInfoEx2;

public abstract class Connection extends EModelRoleSupport
{
    public static class Port
    {
        private String variableName;
        private String variableTitle;

        public Port(String variableName)
        {
            this.variableName = variableName;
            this.variableTitle = variableName;
        }
        
        public Port(Node node)
        {
            this(node.getName(), node.getTitle());
        }

        public Port(String variableName, String variableTitle)
        {
            this.variableName = variableName;
            this.variableTitle = variableTitle;
        }

        @PropertyName("Name")
        @PropertyDescription("Variable name.")
        public String getVariableName()
        {
            return variableName;
        }

        public void setVariableName(String variableName)
        {
            this.variableName = variableName;
        }

        @PropertyName("Title")
        @PropertyDescription("Variable title.")
        public String getVariableTitle()
        {
            return variableTitle;
        }

        public void setVariableTitle(String variableTitle)
        {
            this.variableTitle = variableTitle;
        }
    }

    public static class PortBeanInfo extends BeanInfoEx2<Port>
    {
        public PortBeanInfo()
        {
            super(Port.class);
        }

        @Override
        public void initProperties() throws Exception
        {

            property("variableName").readOnly().add();
            property("variableTitle").readOnly().add();
        }
    }

    public Connection(DiagramElement de)
    {
        super(de);
    }

    // TODO: think on how to support variable link
    private Port inputPort;
    private Port outputPort;

    @PropertyName("Input port")
    @PropertyDescription("Input port.")
    public Port getInputPort()
    {
        return inputPort;
    }

    public void setInputPort(Port inputPort)
    {
        this.inputPort = inputPort;
    }

    @PropertyName("Output port")
    @PropertyDescription("Output port.")
    public Port getOutputPort()
    {
        return outputPort;
    }

    public void setOutputPort(Port outputPort)
    {
        this.outputPort = outputPort;
    }

    public void doClone(Connection connection)
    {
        connection.inputPort = new Port(inputPort.variableName, inputPort.variableTitle);
        connection.outputPort = new Port(outputPort.variableName, outputPort.variableTitle);
        connection.comment = comment;
    }
}
