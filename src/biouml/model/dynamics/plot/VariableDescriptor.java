package biouml.model.dynamics.plot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import one.util.streamex.StreamEx;

/**
 * Class creates textual human-readable description of variable. It is used for example in Plots curve selection.
 * @see PlotVariable
 */
public class VariableDescriptor
{
    private static final String DELIMITER = " | ";

    private String generateDescription(Variable variable)
    {
        return variable.getTitle() + DELIMITER + stripBucks( variable.getName() );
    }

    public StreamEx<String> getDescriptions(EModel emodel)
    {
        Set<Variable> duplicates = new HashSet<>();
        Set<String> variables = new HashSet<>();
        for( Variable variable : emodel.getVariables() )
        {
            String title = variable.getTitle();
            title = stripBucks( title );
            if( variables.contains( title ) )
            {
                duplicates.add( variable );
                variables.remove( title );
            }
            else
                variables.add( title );
        }

        for( Variable duplicate : duplicates )
        {
            String title = duplicate.getTitle();
            List<Variable> vars = emodel.getVariables().stream().filter( v -> v.getTitle().equals( title ) ).toList();
            for( Variable copy : vars )
                variables.add( generateDescription( copy ) );
        }
        return StreamEx.of( variables ).sorted();
    }

    public String getDescription(Variable var, EModel emodel)
    {
        String title = var.getTitle();
        long count = emodel.getVariables().stream().filter( v -> v.getTitle().equals( title ) ).count();
        if( count > 1 )
            return generateDescription( var );
        else
            return title;
    }

    public Variable getVariable(String description, EModel emodel)
    {
        if( description.contains( DELIMITER ) )
        {
            String name = description.substring( description.lastIndexOf( DELIMITER ) + DELIMITER.length(), description.length() ).strip();
            return emodel.getVariable( name );
        }
        else
        {
            return emodel.getVariables().stream().filter( v -> v.getTitle().equals( description ) ).findAny().orElse( null );
        }
    }

    public String getVariableName(String description, EModel emodel)
    {
        Variable var = getVariable( description, emodel );
        return var == null ? null : var.getName();
    }

    public String getTitle(String description, EModel emodel)
    {
        if( description.contains( DELIMITER ) )
            return description.substring( 0, description.lastIndexOf( DELIMITER ) ).strip();
        return description;
    }

    public static String stripBucks(String name)
    {
        String result = name;
        if( result.startsWith( "$$" ) )
            result = result.substring( 2 );
        else if( result.startsWith( "$" ) )
            result = result.substring( 1 );
        return result;
    }

}