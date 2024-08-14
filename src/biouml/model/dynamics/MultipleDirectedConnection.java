package biouml.model.dynamics;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;

public class MultipleDirectedConnection extends MultipleConnection
{
    public MultipleDirectedConnection(Edge edge)
    {
        super(edge);
    }

    @Override
    public void addConnection(Connection c)
    {
        if( ! ( c instanceof DirectedConnection ) )
            throw new IllegalArgumentException("Only directed connections allowed for MultipleDirectedConnection");
        super.addConnection(c);
    }

    @Override
    public Role clone(DiagramElement de)
    {
        MultipleDirectedConnection connection = new MultipleDirectedConnection((Edge)de);
        super.doClone(connection, de);
        return connection;
    }
}
