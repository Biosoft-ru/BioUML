package biouml.plugins.wdl.nextflow.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;

public class IncludeTransformer
{

    public static void transformIncludes(ModuleNode moduleNode)
    {
        BlockStatement script = moduleNode.getStatementBlock();
        List<Statement> statements = script.getStatements();

        List<Statement> newStatements = new ArrayList<>();

        for( Statement stmt : statements )
        {
            List<IncludeData> data = extractIncludeData(stmt);
            if( data != null && !data.isEmpty() )
            {
                List<Statement> vars = createVariablesFromInclude( data );
                newStatements.addAll( vars );
            }
            else
            {
                newStatements.add( stmt );
            }
        }

        statements.clear();
        statements.addAll( newStatements );
    }

    private static boolean isIncludeNode(Expression expression)
    {
        if( expression instanceof ConstantExpression )
        {
            ConstantExpression constExpr = (ConstantExpression)expression;
            return "include".equals( constExpr.getValue() );
        }
        return false;
    }

    public static class IncludeData
    {
        public IncludeData(String name, String alias, String source)
        {
            this.name = name;
            this.alias = alias;
            this.source = source;
        }
        public String source;
        public String alias;
        public String name;
    }

    private static List<IncludeData> extractIncludeData(Statement statement )
    {
        List<IncludeData> result = new ArrayList<>();
        
        if( ! ( statement instanceof ExpressionStatement ) )
            return null;

        ExpressionStatement exprStmt = (ExpressionStatement)statement;
        Expression expr = exprStmt.getExpression();
        if( ! ( expr instanceof MethodCallExpression ) )
            return null;

        String source = null;
        String alias = null;
        String name = null;
        
        MethodCallExpression method = (MethodCallExpression)expr;
        String methodName = NextflowASTUtil.getMethodName( method );
        Expression objectExpression = method.getObjectExpression();
        Expression argumentExpression = method.getArguments();
        if( "from".equals( methodName ) && objectExpression instanceof MethodCallExpression
                && "include".equals( NextflowASTUtil.getMethodName( (MethodCallExpression)objectExpression ) ) )
        {
            if( argumentExpression instanceof ArgumentListExpression )
            {
                Expression sourceExpression = ((ArgumentListExpression)argumentExpression).getExpression(0);
                if (sourceExpression instanceof ConstantExpression)
                {
                    source = ((ConstantExpression)sourceExpression).getValue().toString();
                }      
            }
           Expression arguments = ( (MethodCallExpression)objectExpression ).getArguments();
           if (arguments instanceof ArgumentListExpression)
           {
               Expression closureExpression = ((ArgumentListExpression)arguments).getExpression(0);
               if (closureExpression instanceof ClosureExpression)
               {
                   ClosureExpression closure = ( ClosureExpression)closureExpression;
                   List<Statement> stmnts = ((BlockStatement)closure.getCode()).getStatements();
                   Expression asExpression = ((ExpressionStatement)stmnts.get( 0 )).getExpression();
                   if (asExpression instanceof CastExpression)
                   {
                       CastExpression cast = ((CastExpression)asExpression);
                       name = ((VariableExpression)cast.getExpression()).getName();
                       alias = cast.getType().getName();
                   }
               }
           }
        }
        
        if (source != null && name != null && alias != null)
        {
            IncludeData data = new IncludeData(name, alias, source);
                    result.add( data );
        }
        return result;
    }


    @SuppressWarnings ( "unchecked" )
    private static List<Statement> createVariablesFromInclude(List<IncludeData> includeData)
    {
        List<Statement> result = new ArrayList<>();
        for( IncludeData data: includeData )
        {
            String source = data.source;
            String name = data.name;
            String alias = data.alias;
            Statement statement = NextflowASTUtil.createInclude( name, source, alias );
            result.add( statement );
        }
        return result;
    }

    @SuppressWarnings ( "unchecked" )
    public static Map<String, Object> extractIncludeInfo(Statement stmt)
    {
        return (Map<String, Object>)stmt.getNodeMetaData( "_NEXTFLOW_INCLUDE" );
    }
}