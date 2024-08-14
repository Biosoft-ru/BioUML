package biouml.model.dynamics;

import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import one.util.streamex.StreamEx;

public abstract class MultipleConnection extends Connection
{
    protected MultipleConnection(Edge edge)
    {
        super(edge);
    }

    private List<Connection> connections = new ArrayList<>();

    @PropertyName("Connections")
    @PropertyDescription("Inner connections.")
    public Connection[] getConnections()
    {
        return connections.toArray( new Connection[connections.size()] );
    }

    public void setConnections(Connection[] pc)
    {
        connections.clear();
        StreamEx.of(pc).forEach(c -> addConnection(c));
    }

    public void addConnection(Connection c)
    {
        connections.add(c);
    }

    protected void removeConnection(Connection c)
    {
        if( !connections.contains(c) )
            throw new IllegalArgumentException("Directed connection was not found");
        this.connections.remove(c);
    }

    protected void doClone(MultipleConnection connection, DiagramElement de)
    {
        super.doClone(connection);
        for( Connection c : connections )
            connection.addConnection((Connection)c.clone(de));
    }
    
    public static StreamEx<Connection> getBasicConnections(Connection connection)
    {
        if (connection instanceof MultipleConnection)
            return StreamEx.of(((MultipleConnection)connection).getConnections());
        else return StreamEx.of(connection);
    }
}
