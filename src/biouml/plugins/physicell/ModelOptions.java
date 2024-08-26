package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyName;

public class ModelOptions
{
    private boolean disableAutomatedAdhesions = false;
    
    @PropertyName("Disable auotamted spring adhesion")
    public boolean isDisableAutomatedAdhesions()
    {
        return disableAutomatedAdhesions;
    }
    public void setDisableAutomatedAdhesions(boolean disableAutomatedAdhesions)
    {
        this.disableAutomatedAdhesions = disableAutomatedAdhesions;
    }
    
    public ModelOptions clone()
    {
        ModelOptions result = new ModelOptions();
        result.setDisableAutomatedAdhesions( disableAutomatedAdhesions );
        return result;
    }
}
