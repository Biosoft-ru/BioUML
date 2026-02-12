package biouml.plugins.wdl.nextflow;

import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.GStringExpression;
import org.codehaus.groovy.ast.expr.LambdaExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;

import one.util.streamex.StreamEx;

public class NextFlowFormatter
{
    private static String INDENT = "  ";
    private boolean addQuote = true;

    public String format(Expression expression, boolean addQuote)
    {
        this.addQuote = addQuote;
        StringBuilder sb = new StringBuilder();
        format( expression, sb );
        return sb.toString();
    }

    public String format(Expression expression)
    {
        return format( expression, false );
    }

    public void format(Expression expression, StringBuilder sb)
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
        else if( expression instanceof BooleanExpression )
        {
            formatBoolean( (BooleanExpression)expression, sb );
        }
        else if( expression instanceof PropertyExpression )
        {
            formatPropertyExpression( (PropertyExpression)expression, sb );
        }
        else if( expression instanceof TernaryExpression )
        {
            formatTernary( (TernaryExpression)expression, sb );
        }
        else if( expression instanceof ListExpression )
        {
            formatList( (ListExpression)expression, sb );
        }
        else if( expression instanceof ClassExpression )
        {
            formatClass( (ClassExpression)expression, sb );
        }
        else if( expression instanceof ArgumentListExpression )
        {
            formatArgumentList( (ArgumentListExpression)expression, sb );
        }
        else if( expression instanceof TupleExpression )
        {
            formatTuple( (TupleExpression)expression, sb );
        }
        else if( expression instanceof NamedArgumentListExpression )
        {
            formatNamedArgumentList( (NamedArgumentListExpression)expression, sb );
        }
        else if( expression instanceof MapEntryExpression )
        {
            formatMapEntry( (MapEntryExpression)expression, sb );
        }
        else
        {
            formatUnknown( expression, sb );
        }
    }

    private void formatMapEntry(MapEntryExpression mapEntry, StringBuilder sb)
    {
        sb.append(format( mapEntry.getKeyExpression(), false ));
        sb.append( " : " );
        sb.append(format( mapEntry.getValueExpression(), true ));
    }

    private void formatNamedArgumentList(NamedArgumentListExpression argumentExpression, StringBuilder sb)
    {
        sb.append( StreamEx.of( argumentExpression.getMapEntryExpressions() ).map( arg -> format( arg ) ).joining( ", " ) );
    }

    private void formatArgumentList(ArgumentListExpression argumentExpression, StringBuilder sb)
    {
        sb.append( StreamEx.of( argumentExpression.getExpressions() ).map( arg -> format( arg ) ).joining( ", " ) );
    }

    private void formatTuple(TupleExpression tupleExpression, StringBuilder sb)
    {
        format( tupleExpression.getExpression( 0 ), sb );//TODO: check
    }

    private void formatClass(ClassExpression classExpression, StringBuilder sb)//TODO: check
    {
        sb.append( classExpression.getType().getName() );
    }

    private void formatList(ListExpression listExpression, StringBuilder sb)//TODO: check
    {
        sb.append( "[" );
        for( int i = 0; i < listExpression.getExpressions().size(); i++ )
        {
            format( listExpression.getExpressions().get( i ), sb );
            if( i < listExpression.getExpressions().size() )
                ;
            sb.append( "," );
        }
        sb.append( "]" );
    }

    private void formatPropertyExpression(PropertyExpression propertyExpression, StringBuilder sb)
    {
        Expression objectExpression = propertyExpression.getObjectExpression();
        Expression property = propertyExpression.getProperty();
        String source = null;
        String propertyString = null;
        if( objectExpression instanceof VariableExpression )
            source = ( (VariableExpression)objectExpression ).getName();
        if( property instanceof ConstantExpression )
            propertyString = ( (ConstantExpression)property ).getValue().toString();

        //        CallInfo callInfo = findCallByResult( workflowInfo, source );
        //        String callName = callInfo.getAlias();
        sb.append( source );
        sb.append( "." );
        sb.append( propertyString );
    }


    private void formatTernary(TernaryExpression expression, StringBuilder sb)
    {
        Expression trueExpression = expression.getTrueExpression();
        Expression falseExpression = expression.getFalseExpression();
        BooleanExpression booleanExpression = expression.getBooleanExpression();
        sb.append( "if (" );
        sb.append( format( booleanExpression ) );
        sb.append( ") then " );
        sb.append( format( trueExpression ) );
        sb.append( " else " );
        sb.append( format( falseExpression ) );
    }

    private void formatConstant(ConstantExpression expression, StringBuilder sb)
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

    private void formatUnknown(Expression expression, StringBuilder sb)
    {
        sb.append( "ERROR " + expression.getClass() );
        System.out.println( "ERROR " + expression.getClass() );
    }

    private void formatBoolean(BooleanExpression booleanExpr, StringBuilder sb)
    {
        if( booleanExpr instanceof NotExpression )
        {
            sb.append( "!( " );
            format( booleanExpr.getExpression(), sb );
            sb.append( " )" );
        }
        else
        {
            format( booleanExpr.getExpression(), sb );
        }
    }

    private void formatBinary(BinaryExpression binary, StringBuilder sb)
    {
        format( binary.getLeftExpression(), sb );
        sb.append( " " );
        sb.append( binary.getOperation().getText() );
        sb.append( " " );
        format( binary.getRightExpression(), sb );
    }

    private void formatGString(GStringExpression gString, StringBuilder sb)
    {
        quote( sb );
        String text = gString.getText();
        for( Expression expression : gString.getValues() )
        {
            if( expression instanceof VariableExpression )
            {
                String variableName = ( (VariableExpression)expression ).getName();
                text = text.replace( "$" + variableName, "${" + variableName + "}" );
            }
            else
            {
                String variableName = format( expression );
                text = text.replace( "$" + variableName, "${" + variableName + "}" );
            }
        }
        sb.append( text );
        quote( sb );
    }

    private void formatLambda(LambdaExpression lambda, StringBuilder sb)
    {
        sb.append( "ERROR " + lambda.getClass() );
    }

    private void formatDeclaration(DeclarationExpression declaration, StringBuilder sb)
    {
        sb.append( "def " );
        format( declaration.getLeftExpression(), sb );
        sb.append( " " );
        sb.append( declaration.getOperation().getText() );
        sb.append( " " );
        format( declaration.getRightExpression(), sb );
    }

    private void formatMethodCall(MethodCallExpression methodCall, StringBuilder sb)
    {
        String arguments = format( methodCall.getArguments() );
        String object = format( methodCall.getObjectExpression() );
        String method = format( methodCall.getMethod(), false );

        boolean isMap = method.equals( "map" );

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

    private void formatClosure(ClosureExpression closureExpression, StringBuilder sb)
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
                    indent( sb );
                    format( ( (ExpressionStatement)statement ).getExpression(), sb );
                }
                else if( statement instanceof ReturnStatement )
                {
                    br( sb );
                    indent( sb );
                    sb.append( "return " );
                    format( ( (ReturnStatement)statement ).getExpression(), sb );
                }
                else
                {
                    System.out.println( "" );
                }

            }
        }
    }


    private void quote(StringBuilder sb)
    {
        if( addQuote )
            sb.append( "\"" );
    }

    private void indent(StringBuilder sb)
    {
        sb.append( INDENT );
    }

    private void br(StringBuilder sb)
    {
        sb.append( System.lineSeparator() );
        sb.append( INDENT );
    }
}
