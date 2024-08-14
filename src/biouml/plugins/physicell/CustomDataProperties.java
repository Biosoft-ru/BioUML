package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CustomCellData;
import ru.biosoft.physicell.core.Variable;

public class CustomDataProperties
{
    private VariableProperties[] variables = new VariableProperties[0];

    public CustomDataProperties()
    {

    }

    public CustomDataProperties(CustomCellData data)
    {
        int size = data.variables.size();
        variables = new VariableProperties[size];
        for( int i = 0; i < size; i++ )
            variables[i] = new VariableProperties( data.variables.get( i ) );
    }

    public CustomDataProperties clone()
    {
        CustomDataProperties result = new CustomDataProperties();
        result.variables = new VariableProperties[variables.length];
        for( int i = 0; i < variables.length; i++ )
            result.variables[i] = variables[i].clone();
        return result;
    }

    public void createCustomData(CellDefinition cd)
    {
        cd.custom_data = new CustomCellData();
        for (int i=0; i<variables.length; i++)
        {
            VariableProperties customVar = variables[i];
            Variable var = new Variable();
            var.setName( customVar.getName() );
            var.setValue( customVar.getValue() );
            var.setUnits( customVar.getUnits() );
            var.setConserved( customVar.isConserved() );
            cd.custom_data.addVariable( customVar.getName(), customVar.getUnits(), customVar.getValue() );
        }
    }

    @PropertyName ( "Variables" )
    public VariableProperties[] getVariables()
    {
        return variables;
    }
    public void setVariables(VariableProperties[] variables)
    {
        this.variables = variables;
    }
}