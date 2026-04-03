package biouml.plugins.wdl.nextflow.ast;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;

import biouml.plugins.wdl.nextflow.NextFlowImporter;

public class ArgumentsCollector extends NextflowVisitor
{
    Set<String> arguments = new HashSet<>();

    @Override
    public boolean doVisit(Expression expression)
    {
        if ( NextFlowImporter.isTaskCall( expression ))
        {
            arguments.add( getCallName( (MethodCallExpression)expression )  );
            return false;
        }
        if( expression instanceof VariableExpression )
        {
            arguments.add( ( (VariableExpression)expression ).getName() );
        }
        else if( expression instanceof PropertyExpression )
        {
            PropertyExpression propertyExpression = (PropertyExpression)expression;
            if( propertyExpression.getObjectExpression() instanceof VariableExpression )
            {
                String name =  ((VariableExpression)propertyExpression.getObjectExpression()).getName();
                if( name.equals( "params" ) )
                {

                    arguments.add( new NextFlowFormatter().format( propertyExpression ) );
                }
                else if( propertyExpression.getProperty() instanceof ConstantExpression )
                {
                    ConstantExpression constant = (ConstantExpression)propertyExpression.getProperty();
                    if( constant.getValue().toString().equals( "out" ) )
                    {
                        arguments.add( name );
                    }
                }
            }
        }
        return true;
    }

    public Set<String> getArguments(Expression expression)
    {
        arguments.clear();
        visit( expression );
        return new HashSet<>( arguments );
    }
    
    public String getCallName(MethodCallExpression methodCall)
    {
        return ( (ConstantExpression) methodCall.getMethod() ).getValue().toString();
    }
}