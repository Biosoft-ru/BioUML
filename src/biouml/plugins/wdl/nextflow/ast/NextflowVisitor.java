package biouml.plugins.wdl.nextflow.ast;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.GStringExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.Statement;

public abstract class NextflowVisitor
{
    public abstract void doVisit(Expression expression);

    public void visit(Expression expression)
    {
        if( expression == null )
            return;

        doVisit( expression );

        if( expression instanceof BinaryExpression )
        {
            visit( ( (BinaryExpression)expression ).getLeftExpression() );
            visit( ( (BinaryExpression)expression ).getRightExpression() );
        }
        else if (expression instanceof BooleanExpression)
        {
            visit(((BooleanExpression)expression).getExpression());
        }
        else if (expression instanceof GStringExpression)
        {
            for (Expression values: ((GStringExpression)expression).getValues())
                visit(values);
        }
        else if( expression instanceof TernaryExpression )
        {
            visit( ( (TernaryExpression)expression ).getBooleanExpression() );
            visit( ( (TernaryExpression)expression ).getFalseExpression() );
            visit( ( (TernaryExpression)expression ).getTrueExpression() );
        }
        else if( expression instanceof PropertyExpression )
        {
            visit( ( (PropertyExpression)expression ).getProperty() );
            visit( ( (PropertyExpression)expression ).getObjectExpression() );
        }
        else if( expression instanceof MethodCallExpression )
        {
            //            String formatted = new NextFlowFormatter().format( ( (MethodCallExpression)expression ).getMethod() );
            visit( ( (MethodCallExpression)expression ).getMethod() );
            visit( ( (MethodCallExpression)expression ).getObjectExpression() );
            visit( ( (MethodCallExpression)expression ).getArguments() );
        }
        else if( expression instanceof DeclarationExpression )
        {
            visit( ( (DeclarationExpression)expression ).getLeftExpression() );
            visit( ( (DeclarationExpression)expression ).getRightExpression() );
            visit( ( (DeclarationExpression)expression ).getTupleExpression() );
            visit( ( (DeclarationExpression)expression ).getVariableExpression() );
        }
        else if( expression instanceof ClosureExpression )
        {
            visit ( ((ClosureExpression)expression).getCode());
        }
        else if( expression instanceof ArgumentListExpression )
        {
            for( Expression argument : ( (ArgumentListExpression)expression ).getExpressions() )
            {
                visit( argument );
            }
        }
        else if( expression instanceof ListExpression )
        {
            for( Expression argument : ( (ListExpression)expression ).getExpressions() )
            {
                visit( argument );
            }
        }
        else if( expression instanceof NamedArgumentListExpression )
        {
            for( Expression argument : ( (NamedArgumentListExpression)expression ).getMapEntryExpressions() )
            {
                visit( argument );
            }
        }
        else if( expression instanceof MapEntryExpression )
        {
            visit( ( (MapEntryExpression)expression ).getKeyExpression() );
            visit( ( (MapEntryExpression)expression ).getValueExpression() );
        }
    }

    public void visit(Statement statement)
    {
        if( statement instanceof BlockStatement )
        {
            for( Statement inner : ( (BlockStatement)statement ).getStatements() )
            {
                visit( inner );
            }
        }
        else if( statement instanceof ExpressionStatement )
        {
            visit( ( (ExpressionStatement)statement ).getExpression() );
        }
        else if( statement instanceof IfStatement )
        {
            visit( ( (IfStatement)statement ).getBooleanExpression() );
            visit (( (IfStatement)statement ).getIfBlock());
            visit (( (IfStatement)statement ).getElseBlock());
        }
    }

    public void visit(List<ASTNode> nodes)
    {
        for( Expression expression : getExpressions( nodes ) )
        {
            visit( expression );
        }
    }

    public static List<Expression> getExpressions(List<ASTNode> nodes)
    {
        List<Expression> result = new ArrayList<>();
        for( ASTNode node : nodes )
        {
            if( node instanceof BlockStatement )
            {
                for( Statement statement : ( (BlockStatement)node ).getStatements() )
                {
                    if( statement instanceof ExpressionStatement )
                    {
                        result.add( ( (ExpressionStatement)statement ).getExpression() );
                    }
                }
            }
        }
        return result;
    }
}
