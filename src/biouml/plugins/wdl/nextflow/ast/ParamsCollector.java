package biouml.plugins.wdl.nextflow.ast;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;

public class ParamsCollector extends NextflowVisitor
{
    Set<String> parameters = new HashSet<>();

    public Set<String> getParameters()
    {
        return parameters;
    }
    
    @Override
    public void doVisit(Expression expression)
    {
        if( expression instanceof PropertyExpression )
        {
            PropertyExpression propertyExpression = (PropertyExpression)expression;
            if( propertyExpression.getObjectExpression() instanceof VariableExpression )
            {
                String name = ( (VariableExpression)propertyExpression.getObjectExpression() ).getName();
                if( name.equals( "params" ) )
                {
                    parameters.add( new NextFlowFormatter().format( propertyExpression ) );
                }
            }
        }
    }
    
    public Set<String> getParameters(Expression expression)
    {
        parameters.clear();
        visit(expression);
        return parameters;
    }
}