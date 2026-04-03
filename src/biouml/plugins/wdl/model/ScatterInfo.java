package biouml.plugins.wdl.model;

import java.util.HashSet;
import java.util.Set;

public class ScatterInfo extends ContainerInfo
{
    private String variable; //TODO: refactor expression info into declaration info
    private String expression;
    private Set<String> arguments = new HashSet<>();

    public Set<String> getArguments()
    {
        return arguments;
    }

    public void setArguments(Set<String> arguments)
    {
        this.arguments = arguments;
    }

    public String getVariable()
    {
        return variable;
    }

    public void setVariable(String variable)
    {
        this.variable = variable;
    }
    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }
}

