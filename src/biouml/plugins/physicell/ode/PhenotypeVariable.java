package biouml.plugins.physicell.ode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public class PhenotypeVariable extends Option
{
    public static String INPUT_TYPE = "Input";
    public static String OUTPUT_TYPE = "Output";
    public static String CONTACT_TYPE = "Contact";

    private String varName;
    private String phenotypeName;
    private String type = CONTACT_TYPE;

    private List<String> phenotypeNames = new ArrayList<String>();
    private List<String> varNames = new ArrayList<String>();

    public String[] getTypes()
    {
        return new String[] {INPUT_TYPE, OUTPUT_TYPE, CONTACT_TYPE};
    }

    public void setPhenotypeNames(String[] names)
    {
        this.phenotypeNames = Arrays.asList( names );
    }
    public List<String> getPhenotypeNames()
    {
        return phenotypeNames;
    }

    public void setVariableNames(String[] names)
    {
        this.varNames = Arrays.asList( names );
    }
    public List<String> getVariableNames()
    {
        return varNames;
    }

    @PropertyName ( "Type" )
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        Object oldValue = this.type;
        this.type = type;
        firePropertyChange( "type", oldValue, type );
    }

    @PropertyName ( "Variable" )
    public String getVarName()
    {
        return varName;
    }
    public void setVarName(String varName)
    {
        Object oldValue = this.varName;
        this.varName = varName;
        firePropertyChange( "varName", oldValue, type );
    }

    @PropertyName ( "Phenotype property" )
    public String getPhenotypeName()
    {
        return phenotypeName;
    }
    public void setPhenotypeName(String phenotypeName)
    {
        Object oldValue = this.phenotypeName;
        this.phenotypeName = phenotypeName;
        firePropertyChange( "phenotypeName", oldValue, phenotypeName );
    }

    public PhenotypeVariable clone()
    {
        PhenotypeVariable result = new PhenotypeVariable();
        result.phenotypeName = this.phenotypeName;
        result.type = this.type;
        result.phenotypeNames = new ArrayList<>( phenotypeNames );
        result.varNames = new ArrayList<>( varNames );
        return result;
    }
}