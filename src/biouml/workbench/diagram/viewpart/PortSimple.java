package biouml.workbench.diagram.viewpart;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Node;
import biouml.standard.diagram.Util;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class PortSimple implements DataElement
{
    private Node port;

    public PortSimple()
    {
        
    }
    
    public PortSimple(Node port)
    {
        this.port = port;
    }

    @Override
    @PropertyName ( "Name" )
    public String getName()
    {
        return port == null ? null : port.getName();
    }
    public void setName(String name)
    {

    }

    @PropertyName ( "Title" )
    public String getTitle()
    {
        return port == null ? null : port.getTitle();
    }
    public void setTitle(String title)
    {
        if( port != null )
        port.setTitle( title );
    }

    @PropertyName ( "Type" )
    public String getType()
    {
        return port == null ? null : Util.getPortType( port );
    }
    public void setType(String type)
    {

    }

    @PropertyName ( "Access Type" )
    public String getAccessType()
    {
        return port == null ? null : Util.getAccessType( port );
    }
    public void setAccessType(String type)
    {

    }

    @PropertyName ( "Variable" )
    public String getVariable()
    {
        if( port == null )
            return null;
        String var =  Util.getPortVariable( port );
        return var == null ? "" : var;
    }
    public void setVariable(String type)
    {

    }

    @PropertyName ( "Module" )
    public String getModule()
    {
        if( port == null )
            return null;
        String res = Util.getPortModule( port );
        return res == null ? "" : res;
    }
    public void setModule(String module)
    {

    }

    @Override
    public DataCollection<?> getOrigin()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Node getPort()
    {
        return port;
    }
}