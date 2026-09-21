package biouml.plugins.wdl.nextflow.ast;

import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;

public class NextflowASTUtil
{
    public static String getMethodName(MethodCallExpression expression)
    {
        Expression method = expression.getMethod();
        if( method instanceof ConstantExpression )
            return ( (ConstantExpression)method ).getValue().toString();
        return null;
    }

    public static Statement createInclude(String first, String second, String alias)
    {
        VariableExpression varExpr = new VariableExpression( first );
        ExpressionStatement exprStmt = new ExpressionStatement( varExpr );
        BlockStatement block = new BlockStatement();
        block.addStatement( exprStmt );
        ClosureExpression closure = new ClosureExpression( null, block );
        MethodCallExpression includeCall = new MethodCallExpression( new VariableExpression( "this" ), "include",
                new ArgumentListExpression( closure ) );
        MethodCallExpression fromCall = new MethodCallExpression( includeCall, "from",
                new ArgumentListExpression( new ConstantExpression( second ) ) );
        Statement stmt = new ExpressionStatement( fromCall );
        stmt.putNodeMetaData( "alias", alias );
        return stmt;
    }
}