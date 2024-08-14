package biouml.model.dynamics;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;

public class MultipleUndirectedConnection extends MultipleConnection
{
    public MultipleUndirectedConnection(Edge edge)
    {
        super(edge);
    }

    @Override
    public void addConnection(Connection c)
    {
        if( ! ( c instanceof UndirectedConnection ) )
            throw new IllegalArgumentException("Only undirected connections allowed for MultipleUndirectedConnection");
       super.addConnection(c);
    }

    @Override
    public Role clone(DiagramElement de)
    {
        MultipleUndirectedConnection connection = new MultipleUndirectedConnection((Edge)de);
        super.doClone(connection, de);
        return connection;
    }
}
