package biouml.plugins.virtualcell.diagram;


import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;

public class SingleConnectionProperties implements Role
{
    private DataOwner from;
    private DataOwner to;

    private String nameFrom;
    private String nameTo;
    
    private Edge edge;
    
    public SingleConnectionProperties(Edge edge)
    {
        this.edge = edge;
        this.from = edge.getInput().getRole( DataOwner.class );
    }

    public String[] getavailableFrom()
    {
        return from.getNames();
    }

    public String[] getavailableTo()
    {
        return to.getNames();
    }

    @PropertyName ( "Name from" )
    public String getNameFrom()
    {
        return nameFrom;
    }
    public void setNameFrom(String nameFrom)
    {
        this.nameFrom = nameFrom;
    }

    @PropertyName ( "Name to" )
    public String getNameTo()
    {
        return nameTo;
    }

    public void setNameTo(String nameTo)
    {
        this.nameTo = nameTo;
    }

    @Override
    public DiagramElement getDiagramElement()
    {
        return edge;
    }
    
    public void setDiagramElement(Edge edge)
    {
        this.edge = edge;
    }

    @Override
    public Role clone(DiagramElement de)
    {
        return new SingleConnectionProperties( (Edge)de );
    }
}