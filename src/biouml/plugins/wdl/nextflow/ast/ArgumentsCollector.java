package biouml.plugins.wdl.nextflow.ast;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;

public class ArgumentsCollector extends NextflowVisitor
{
    Set<String> arguments = new HashSet<>();

    @Override
    public void doVisit(Expression expression)
    {
        if( expression instanceof VariableExpression )
        {
            arguments.add( ( (VariableExpression)expression ).getName() );
        }
        else if( expression instanceof PropertyExpression )
        {
            PropertyExpression propertyExpression = (PropertyExpression)expression;
            if( propertyExpression.getObjectExpression() instanceof VariableExpression )
            {
                String name = ( (VariableExpression)propertyExpression.getObjectExpression() ).getName();
                if( name.equals( "params" ) )
                {
                    arguments.add( new NextFlowFormatter().format( propertyExpression ) );
                }
            }
        }
    }

    public Set<String> getArguments(Expression expression)
    {
        arguments.clear();
        visit( expression );
        return new HashSet<>( arguments );
    }
}