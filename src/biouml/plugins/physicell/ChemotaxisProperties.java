package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;

@PropertyName ( "Chemotaxis" )
@PropertyDescription ( "Chemotaxis." )
public class ChemotaxisProperties implements Role
{
    private DiagramElement de;
    private int direction = 1;
    private double sensitivity = 1;
    private String title;

    public ChemotaxisProperties(DiagramElement de)
    {
        setDiagramElement( de );
    }

    public ChemotaxisProperties()
    {
    }

    @Override
    public DiagramElement getDiagramElement()
    {
        return de;
    }
    public void setDiagramElement(DiagramElement de)
    {
        this.de = de;

        if( de instanceof Edge )
        {
            SubstrateProperties sp = ( (Edge)de ).nodes().map( n -> n.getRole() ).select( SubstrateProperties.class ).findAny()
                    .orElse( null );
            if( sp != null )
                title = sp.getName();
        }
    }

    @Override
    public Role clone(DiagramElement de)
    {
        ChemotaxisProperties result = new ChemotaxisProperties( de );
        result.sensitivity = sensitivity;
        result.direction = direction;
        result.title = title;
        return result;
    }

    @PropertyName ( "Direction" )
    public int getDirection()
    {
        return direction;
    }
    public void setDirection(int direction)
    {
        this.direction = direction;
    }

    @PropertyName ( "Sensitivity" )
    public double getSensitivity()
    {
        return sensitivity;
    }

    public void setSensitivity(double sensitivity)
    {
        this.sensitivity = sensitivity;
    }

    @PropertyName ( "Substrate" )
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
}