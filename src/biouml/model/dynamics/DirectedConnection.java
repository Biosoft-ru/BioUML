package biouml.model.dynamics;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;

public class DirectedConnection extends Connection
{
    public DirectedConnection(Edge edge)
    {
        super(edge);
    }

    private String function;

    @PropertyName("Transforming fucntion")
    @PropertyDescription("Transforming fucntion.")
    public String getFunction()
    {
        return function;
    }

    public void setFunction(String function)
    {
        this.function = function;
    }

    @Override
    public Role clone(DiagramElement de)
    {
        DirectedConnection connection = new DirectedConnection((Edge)de);
        doClone(connection);
        return connection;
    }

    public void doClone(DirectedConnection connection)
    {
        super.doClone(connection);
        connection.function = function;
    }
}
