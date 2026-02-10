package biouml.plugins.wdl.nextflow;

import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.GStringExpression;
import org.codehaus.groovy.ast.expr.LambdaExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;

import one.util.streamex.StreamEx;

public class NextFlowFormatter
{
    private static String INDENT = "  ";

    public static String format(Expression expression)
    {
        StringBuilder sb = new StringBuilder();
        format( expression, sb );
        return sb.toString();
    }

    public static void format(Expression expression, StringBuilder sb)
    {
        if( expression instanceof MethodCallExpression )
        {
            formatMethodCall( (MethodCallExpression)expression, sb );
        }
        else if( expression instanceof ConstantExpression )
        {
            formatConstant( (ConstantExpression)expression, sb );
        }
        else if( expression instanceof VariableExpression )
        {
            sb.append( ( (VariableExpression)expression ).getName() );
        }
        else if( expression instanceof LambdaExpression )
        {
            formatLambda( (LambdaExpression)expression, sb );
        }
        else if( expression instanceof ClosureExpression )
        {
            formatClosure( (ClosureExpression)expression, sb );
        }
        else if( expression instanceof DeclarationExpression )
        {
            formatDeclaration( (DeclarationExpression)expression, sb );
        }
        else if( expression instanceof GStringExpression )
        {
            formatGString( (GStringExpression)expression, sb );
        }
        else if( expression instanceof BinaryExpression )
        {
            formatBinary( (BinaryExpression)expression, sb );
        }
        else
        {
            formatUnknown( expression, sb );
        }
    }

    private static void formatConstant(ConstantExpression expression, StringBuilder sb)
    {
        Object value = ( (ConstantExpression)expression ).getValue();
        if( value instanceof String )
        {
            quote( sb );
            sb.append( value.toString() );
            quote( sb );
        }
        else
        {
            sb.append( value.toString() );
        }
    }

    private static void formatUnknown(Expression expression, StringBuilder sb)
    {
        sb.append( "ERROR " + expression.getClass() );
    }

    private static void formatBinary(BinaryExpression binary, StringBuilder sb)
    {
        format( binary.getLeftExpression(), sb );
        sb.append( " " );
        sb.append( binary.getOperation().getText() );
        sb.append( " " );
        format( binary.getRightExpression(), sb );
    }

    private static void formatGString(GStringExpression gString, StringBuilder sb)
    {
        quote( sb );
        String text = gString.getText();
        for( Expression expression : gString.getValues() )
        {
            String variableName = ( (VariableExpression)expression ).getName();
            text = text.replace( "$" + variableName, "${" + variableName + "}" );
        }
        sb.append( text );
        quote( sb );
    }

    private static void formatLambda(LambdaExpression lambda, StringBuilder sb)
    {
        sb.append( "ERROR " + lambda.getClass() );
    }

    private static void formatDeclaration(DeclarationExpression declaration, StringBuilder sb)
    {
        sb.append( "def " );
        format( declaration.getLeftExpression(), sb );
        sb.append( " " );
        sb.append( declaration.getOperation().getText() );
        sb.append( " " );
        format( declaration.getRightExpression(), sb );
    }

    private static void formatMethodCall(MethodCallExpression methodCall, StringBuilder sb)
    {
        Expression methodExpression = methodCall.getMethod();
        Expression objectExpression = methodCall.getObjectExpression();
        ArgumentListExpression argumentsExpression = (ArgumentListExpression)methodCall.getArguments();

        String object = format( objectExpression );
        String method = null;
        if( methodExpression instanceof ConstantExpression )
        {
            method = ( (ConstantExpression)methodExpression ).getValue().toString();
        }
        else
        {
            method = format( methodExpression );
        }
        boolean isMap = method.equals( "map" );

        String arguments = StreamEx.of( argumentsExpression.getExpressions() ).map( arg -> format( arg ) ).joining( "," );

        if( ! ( "this".equals( object ) ) )
        {
            sb.append( object );
            sb.append( "." );
        }
        sb.append( method );
        if( isMap )
        {
            sb.append( " { " );
            sb.append( arguments );
            br( sb );
            sb.append( "}" );
        }
        else
        {
            sb.append( " ( " );
            sb.append( arguments );
            sb.append( " ) " );
        }
    }

    private static void formatClosure(ClosureExpression closureExpression, StringBuilder sb)
    {
        sb.append( StreamEx.of( closureExpression.getParameters() ).map( p -> p.getName() ).joining( "," ) );
        sb.append( " -> " );

        Statement codeStatement = closureExpression.getCode();
        if( codeStatement instanceof BlockStatement )
        {
            for( Statement statement : ( (BlockStatement)codeStatement ).getStatements() )
            {
                if( statement instanceof ExpressionStatement )
                {
                    br( sb );
                    indent(sb);
                    format( ( (ExpressionStatement)statement ).getExpression(), sb );
                }
                else if( statement instanceof ReturnStatement )
                {
                    br( sb );
                    indent(sb);
                    sb.append( "return " );
                    format( ( (ReturnStatement)statement ).getExpression(), sb );
                }
            }
        }
    }


    private static void quote(StringBuilder sb)
    {
        sb.append( "\"" );
    }

    private static void indent(StringBuilder sb)
    {
        sb.append( INDENT );
    }

    private static void br(StringBuilder sb)
    {
        sb.append( System.lineSeparator() );
        sb.append( INDENT );
    }
}
