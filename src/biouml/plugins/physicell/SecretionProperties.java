package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Edge;

@PropertyName ( "Secretion" )
@PropertyDescription ( "Secretion." )
public class SecretionProperties implements PhysicellRole
{
    private DiagramElement de;
    private String substrate = PhysicellConstants.NOT_SELECTED;
    private double secretionRate;
    private double secretionTarget;
    private double uptakeRate;
    private double netExportRate;

    public SecretionProperties(DiagramElement de)
    {
        setDiagramElement( de );
    }

    public SecretionProperties()
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
                substrate = sp.getName();
        }
    }

    @Override
    public SecretionProperties clone(DiagramElement de)
    {
        SecretionProperties result = new SecretionProperties( de );
        result.setSecretionRate( secretionRate );
        result.setUptakeRate( uptakeRate );
        result.setNetExportRate( netExportRate );
        result.setSecretionTarget( secretionTarget );
        result.setTitle( substrate );
        return result;
    }

    @PropertyName ( "Secretion Rate" )
    public double getSecretionRate()
    {
        return secretionRate;
    }
    public void setSecretionRate(double secretionRate)
    {
        this.secretionRate = secretionRate;
    }

    @PropertyName ( "Secretion Target" )
    public double getSecretionTarget()
    {
        return secretionTarget;
    }
    public void setSecretionTarget(double secretionTarget)
    {
        this.secretionTarget = secretionTarget;
    }

    @PropertyName ( "Uptake Rate" )
    public double getUptakeRate()
    {
        return uptakeRate;
    }
    public void setUptakeRate(double uptakeRate)
    {
        this.uptakeRate = uptakeRate;
    }

    @PropertyName ( "Net export rate" )
    public double getNetExportRate()
    {
        return netExportRate;
    }
    public void setNetExportRate(double netExportRate)
    {
        this.netExportRate = netExportRate;
    }


    @PropertyName ( "Substrate" )
    public String getTitle()
    {
        return substrate;
    }
    public void setTitle(String substrate)
    {
        this.substrate = substrate;
    }
}