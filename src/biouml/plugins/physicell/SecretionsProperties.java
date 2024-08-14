package biouml.plugins.physicell;


import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.Node;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Secretion;

@PropertyName ( "Secretion/Consumption" )
public class SecretionsProperties extends Option
{
    SecretionProperties[] secretions = new SecretionProperties[0];
    private Node node;

    public SecretionsProperties()
    {
    }

    public SecretionsProperties(DiagramElement de)
    {
        setDiagramElement( de );
    }

    public void setDiagramElement(DiagramElement de)
    {
        if( de instanceof Node )
        {
            this.node = (Node)de;
            setSecretion( node.edges().map( e -> e.getRole() ).select( SecretionProperties.class ).toArray( SecretionProperties[]::new ) );
        }
        else
            this.setSecretion( new SecretionProperties[0] );
    }

    public SecretionsProperties clone(DiagramElement de)
    {
        return new SecretionsProperties( de );
    }

    public void createSecretion(CellDefinition cd)
    {
        Secretion secretion = cd.phenotype.secretion;
        Microenvironment m = cd.getMicroenvironment();
        for( SecretionProperties secretionProperties : secretions )
        {
            String substrate = secretionProperties.getTitle();
            int index = m.findDensityIndex( substrate );
            secretion.netExportRates[index] = secretionProperties.getNetExportRate();
            secretion.saturationDensities[index] = secretionProperties.getSecretionTarget();
            secretion.secretionRates[index] = secretionProperties.getSecretionRate();
            secretion.uptakeRates[index] = secretionProperties.getUptakeRate();
        }
    }

    public void addSecretion(SecretionProperties secretion)
    {
        SecretionProperties[] newSecretions = new SecretionProperties[this.secretions.length + 1];
        System.arraycopy( secretions, 0, newSecretions, 0, secretions.length );
        newSecretions[secretions.length] = secretion;
        this.setSecretion( newSecretions );
    }

    public void update()
    {
        setDiagramElement( node );
    }

    @PropertyName ( "Substrates" )
    public SecretionProperties[] getSecretion()
    {
        return secretions;
    }

    public void setSecretion(SecretionProperties[] secretion)
    {
        Object oldValue = this.secretions;
        this.secretions = secretion;
        firePropertyChange( "secretion", oldValue, secretion );
        firePropertyChange( "*", null, null );
    }

    public String getSecretionName(Integer i, Object obj)
    {
        return ( (SecretionProperties)obj ).getTitle();
    }
}