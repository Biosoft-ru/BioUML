package biouml.workbench.diagram.viewpart;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.Util;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class ConnectionSimple implements DataElement
{
    Edge connection;

    public ConnectionSimple(Edge e)
    {
        this.connection = e;
    }

    public ConnectionSimple()
    {

    }

    @Override
    @PropertyName ( "Name" )
    public String getName()
    {
        return connection == null ? null : connection.getName();
    }
    public void setName(String name)
    {

    }

    @PropertyName ( "Type" )
    public String getType()
    {
        return connection == null ? null : connection.getKernel() instanceof Stub.DirectedConnection ? "Directed" : "Undirected";
    }
    public void setType(String type)
    {

    }

    @PropertyName ( "Module From" )
    public String getModuleFrom()
    {
        if( connection == null )
            return null;
        Node input = connection.getInput();
        if( input.getOrigin() instanceof SubDiagram )
            return input.getOrigin().getName();
        return null;
    }
    public void setModuleFrom(String type)
    {

    }

    @PropertyName ( "Variable From" )
    public String getVariableFrom()
    {
        if( connection == null )
            return null;
        Node input = connection.getInput();
        if( Util.isPort( input ) )
            return Util.getPortVariable( input );
        Role role = input.getRole();
        if( role instanceof VariableRole )
            return ( (VariableRole)role ).getName();
        if( role instanceof Bus )
            return ( (Bus)role ).getName();
        return null;

    }
    public void setVariableFrom(String type)
    {

    }

    @PropertyName ( "Module To" )
    public String getModuleTo()
    {
        if( connection == null )
            return null;
        Node input = connection.getOutput();
        if( input.getOrigin() instanceof SubDiagram )
            return input.getOrigin().getName();
        return null;
    }
    public void setModuleTo(String type)
    {

    }

    @PropertyName ( "Variable To" )
    public String getVariableTo()
    {
        if( connection == null )
            return null;
        Node output = connection.getInput();
        if( Util.isPort( output ) )
            return Util.getPortVariable( output );
        Role role = output.getRole();
        if( role instanceof VariableRole )
            return ( (VariableRole)role ).getName();
        if( role instanceof Bus )
            return ( (Bus)role ).getName();
        return null;
    }
    public void setVariableTo(String type)
    {

    }

    @Override
    public DataCollection<?> getOrigin()
    {
        // TODO Auto-generated method stub
        return null;
    }
}