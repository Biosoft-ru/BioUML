package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;

public class TransitionProperties implements Role
{
    private Edge edge;
    private boolean isFixed;
    private double rate;
    private String title;
    private String from;
    private String to;

    public TransitionProperties()
    {

    }

    public TransitionProperties(String title)
    {
        this.title = title;
    }

    public TransitionProperties(DiagramElement de)
    {
        setDiagramElement( de );
    }

    @Override
    public DiagramElement getDiagramElement()
    {
        return edge;
    }
    public void setDiagramElement(DiagramElement de)
    {
        edge = (Edge)de;
        from = edge.getInput().getRole( PhaseProperties.class ).getName();
        to = edge.getOutput().getRole( PhaseProperties.class ).getName();
        this.title = from + " -> " + to;
    }

    @PropertyName ( "Fixed" )
    public boolean isFixed()
    {
        return isFixed;
    }
    public void setFixed(boolean isFixed)
    {
        this.isFixed = isFixed;
    }

    @PropertyName ( "Rate" )
    public double getRate()
    {
        return rate;
    }
    public void setRate(double rate)
    {
        this.rate = rate;
    }

    public double getDuration()
    {
        return 1/rate;
    }
    
    @PropertyName ( "Title" )
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @Override
    public TransitionProperties clone(DiagramElement de)
    {
        TransitionProperties result = new TransitionProperties( de );
        result.rate = rate;
        result.isFixed = isFixed;
        result.from = from;
        result.to = to;
        return result;
    }

    public TransitionProperties clone()
    {
        TransitionProperties result = new TransitionProperties();
        result.rate = rate;
        result.isFixed = isFixed;
        result.from = from;
        result.to = to;
        return result;
    }

    public String getFrom()
    {
        return from;
    }
    public void setFrom(String from)
    {
        this.from = from;
    }

    public String getTo()
    {
        return to;
    }
    public void setTo(String to)
    {
        this.to = to;
    }
}