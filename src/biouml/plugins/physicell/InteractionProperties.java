package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;

@PropertyName ( "Interactions" )
@PropertyDescription ( "Interactions." )
public class InteractionProperties implements Role
{
    private DiagramElement de;
    private String cellType;
    private double attackRate = 0;
    private double fuseRate = 0;
    private double phagocytosisRate = 0;

    public InteractionProperties(DiagramElement de)
    {
        setDiagramElement( de );
    }

    public InteractionProperties()
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
    public InteractionProperties clone(DiagramElement de)
    {
        InteractionProperties result = new InteractionProperties( de );
        result.attackRate = attackRate;
        result.fuseRate = fuseRate;
        result.phagocytosisRate = phagocytosisRate;
        result.cellType = cellType;
        return result;
    }

    @PropertyName ( "Attack rate" )
    public double getAttackRate()
    {
        return attackRate;
    }
    public void setAttackRate(double attackRate)
    {
        this.attackRate = attackRate;
    }

    @PropertyName ( "Fuse rate" )
    public double getFuseRate()
    {
        return fuseRate;
    }
    public void setFuseRate(double fuseRate)
    {
        this.fuseRate = fuseRate;
    }

    @PropertyName ( "Phagocytosis rate" )
    public double getPhagocytosisRate()
    {
        return phagocytosisRate;
    }
    public void setPhagocytosisRate(double phagocytosisRate)
    {
        this.phagocytosisRate = phagocytosisRate;
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
}