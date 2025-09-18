package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;

@PropertyName ( "Transformation" )
@PropertyDescription ( "Transformation" )
public class TransformationProperties implements PhysicellRole
{
    private DiagramElement de;
    private String cellType;
    private double rate = 0;

    public TransformationProperties(DiagramElement de)
    {
        setDiagramElement( de );
    }

    public TransformationProperties()
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
            Role sp = ( (Edge)de ).getOutput().getRole();
            if( sp instanceof CellDefinitionProperties )
                cellType = ( (CellDefinitionProperties)sp ).getName();
        }
    }

    @Override
    public TransformationProperties clone(DiagramElement de)
    {
        TransformationProperties result = new TransformationProperties( de );
        result.rate = rate;
        result.cellType = cellType;
        return result;
    }

    @PropertyName ( "Cell type" )
    public String getCellType()
    {
        return cellType;
    }
    public void setCellType(String cellType)
    {
        this.cellType = cellType;
    }

    @PropertyName ( "Transformation rate" )
    public double getRate()
    {
        return rate;
    }
    public void setRate(double rate)
    {
        this.rate = rate;
    }
}