package biouml.model.util;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;

public class VariableName implements DiagramDependentProperty
{

    EModel emodel = null;
    String value = null;

    public VariableName(String value)
    {
        this.value = value;
    }
    
    public String[] getAvailableNames()
    {
        if (emodel == null)
            return new String[]{};

        //TODO: implement filter properly
        return emodel.getVariables().stream().map( Variable::getName )
                .filter( name -> ! ( name.startsWith( "$$" ) || name.equals( "time" ) ) )
            .toArray( String[]::new );
    }
    
    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    public void setDiagram(Diagram diagram)
    {
       if (diagram != null && diagram.getRole() != null && diagram.getRole() instanceof EModel)
       {
           this.emodel = (EModel)diagram.getRole();
           String[] vals = getAvailableNames();
           if (vals.length > 0)
               this.setValue( vals[0] );
       }
    }

    @Override
    public String toString()
    {
        return this.value;
    }
}
