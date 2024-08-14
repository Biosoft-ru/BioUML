package biouml.plugins.enrichment;

import java.util.Properties;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.biohub.BioHub;

@SuppressWarnings ( "serial" )
public class DiagramClassificationParameters extends FunctionalClassificationParameters
{
    static final String[] TYPES = {"Proteins", "Small molecules"};
    
    private String type = TYPES[0];

    @PropertyName("Type")
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        Object oldValue = this.type;
        this.type = type;
        firePropertyChange( "type", oldValue, this.type );
    }

    @Override
    public BioHub getFunctionalHub()
    {
        return new DiagramHub( new Properties(), getRepositoryHubRoot(), getReferenceCollection(), getType() );
    }
}
