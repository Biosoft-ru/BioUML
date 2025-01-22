package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Mechanics;
import ru.biosoft.physicell.core.Model;

@PropertyName ( "Mechanics" )
@PropertyDescription ( "Mechanics." )
public class MechanicsProperties
{
    private Mechanics mechanics = new Mechanics();

    public MechanicsProperties()
    {

    }

    public MechanicsProperties clone()
    {
        return new MechanicsProperties( mechanics.clone() );
    }

    public void createMechanics(CellDefinition cd, Model model)
    {
        mechanics.initialize( model );
        cd.phenotype.mechanics = mechanics.clone();
    }

    public MechanicsProperties(Mechanics mechanics)
    {
        this.mechanics = mechanics;
    }

    @PropertyName ( "Cell adhesion strength" )
    @PropertyDescription ( "Cell-cell adhesion strength." )
    public double getCellCellAdhesionStrength()
    {
        return mechanics.cellCellAdhesionStrength;
    }
    public void setCellCellAdhesionStrength(double cellCellAdhesionStrength)
    {
        mechanics.cellCellAdhesionStrength = cellCellAdhesionStrength;
    }

    @PropertyName ( "Membrane adhesion strength" )
    @PropertyDescription ( "Basement Membrane adhesion strength." )
    public double getCellBMAdhesionStrength()
    {
        return mechanics.cellBMAdhesionStrength;
    }
    public void setCellBMAdhesionStrength(double cellBMAdhesionStrength)
    {
        mechanics.cellBMAdhesionStrength = cellBMAdhesionStrength;
    }

    @PropertyName ( "Cell repulsion strength" )
    @PropertyDescription ( "Cell-cell repulsion strength." )
    public double getCellCellRepulsionStrength()
    {
        return mechanics.cellCellRepulsionStrength;
    }
    public void setCellCellRepulsionStrength(double cellCellRepulsionStrength)
    {
        mechanics.cellCellRepulsionStrength = cellCellRepulsionStrength;
    }

    @PropertyName ( "Membrane repulsion strength" )
    @PropertyDescription ( "Basement membrane repulsion strength." )
    public double getCellBMRepulsionStrength()
    {
        return mechanics.cellBMRepulsionStrength;
    }
    public void setCellBMRepulsionStrength(double cellBMRepulsionStrength)
    {
        mechanics.cellBMRepulsionStrength = cellBMRepulsionStrength;
    }

    @PropertyName ( "Relative max adhesion distance" )
    @PropertyDescription ( "Relative max adhesion distance." )
    public double getRelMaxAdhesionDistance()
    {
        return mechanics.relMaxAdhesionDistance;
    }
    public void setRelMaxAdhesionDistance(double relMaxAdhesionDistance)
    {
        mechanics.relMaxAdhesionDistance = relMaxAdhesionDistance;
    }

    @PropertyName ( "Max attachments" )
    public int getMaxAttachments()
    {
        return mechanics.maxAttachments;
    }
    public void setMaxAttachments(int maxAttachments)
    {
        mechanics.maxAttachments = maxAttachments;
    }

    @PropertyName ( "Attachment elasticity" )
    @PropertyDescription ( "Attachment elastic coefficient." )
    public double getAttachmentElasticConstant()
    {
        return mechanics.attachmentElasticConstant;
    }
    public void setAttachmentElasticConstant(double attachmentElasticConstant)
    {
        mechanics.attachmentElasticConstant = attachmentElasticConstant;
    }

    @PropertyName ( "Attachment rate" )
    public double getAttachmentRate()
    {
        return mechanics.attachmentRate;
    }
    public void setAttachmentRate(double attachmentRate)
    {
        mechanics.attachmentRate = attachmentRate;
    }

    @PropertyName ( "Detachment rate" )
    public double getDetachmentRate()
    {
        return mechanics.detachmentRate;
    }
    public void setDetachmentRate(double detachmentRate)
    {
        mechanics.detachmentRate = detachmentRate;
    }
}