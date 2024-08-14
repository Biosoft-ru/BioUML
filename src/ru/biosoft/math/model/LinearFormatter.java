package ru.biosoft.math.model;

import java.util.ArrayList;
import java.util.List;

public class LinearFormatter implements Formatter
{
    protected StringBuilder result;
    protected String endl = System.getProperty("line.separator");

    @Override
    public String[] format(AstStart start)
    {
        result = new StringBuilder();

        if( start != null )
        {
            int n = start.jjtGetNumChildren();
            for( int i = 0; i < n; i++ )
                processNode( start.jjtGetChild( i ) );
        }
        return new String[] {"", String.valueOf(result)};
    }

    public List<String> format(List<AstStart> starts)
    {
        List<String> result = new ArrayList<>();
        for( AstStart start : starts )
            result.add(format(start)[1]);
        return result;
    }

    protected void processNode(Node node)
    {
        if( node instanceof AstConstant )
            processConstant((AstConstant)node);

        else if( node instanceof AstVarNode )
            processVariable((AstVarNode)node);

        else if( node instanceof AstFunNode )
            processFunction((AstFunNode)node);

        else if( node instanceof AstFunctionDeclaration )
            processFunctionDeclaration((AstFunctionDeclaration)node);

        else if( node instanceof AstPiecewise )
            processPiecewise((AstPiecewise)node);

        else
        {
            result.append("???" + node + "???");
            System.out.println("Unknown node type, node=" + node);
        }
    }

    protected void processConstant(AstConstant node)
    {
        if( node.getName() != null )
        {
            // symbolic constant
            result.append(node.getName());
        }
        else
        {
            // number, string or boolean value
            Object value = node.getValue();
            if( value instanceof String )
                result.append("\"" + value + "\"");
            else
                result.append(node.getValue());
        }
    }
    
    protected void processVariable(AstVarNode node)
    {
        result.append(node.getName());
    }

    protected void processFunction(AstFunNode node)
    {
        Function function = node.getFunction();
        String name = getFunctionName(function.getName());
        if( name.startsWith("\"") )
            name = name.substring(1, name.length() - 1);

        boolean parenthis = needParenthis(node);
        if( parenthis )
            result.append('(');

        if( function.getPriority() == Function.FUNCTION_PRIORITY )
        {
            result.append(name);
            result.append("(");

            int n = node.jjtGetNumChildren();
            for( int i = 0; i < n; i++ )
            {
                processNode(node.jjtGetChild(i));
                if( i < n - 1 )
                    result.append(", ");
            }

            result.append(")");
            return;
        }

        // unary operators
        if( function.getNumberOfParameters() == 1 )
        {
            if( name.equals("u-") )
                name = "-";

            result.append(name);
            processNode(node.jjtGetChild(0));
        }
        else
        {
            int n = node.jjtGetNumChildren();
            for( int i = 0; i < n; i++ )
            {
                processNode(node.jjtGetChild(i));

                if( i < n - 1 )
                {
                    if( function.getPriority() == Function.TIMES_PRIORITY || function.getPriority() == Function.POWER_PRIORITY )
                        result.append(name);
                    else
                        result.append(" " + name + " ");
                }
            }
        }

        if( parenthis )
            result.append(')');
    }

    protected String getFunctionName(String name)
    {
        return name;
    }

    public boolean needParenthis(AstFunNode node)
    {
        if( ! ( node.jjtGetParent() instanceof AstFunNode ) )
            return false;

        AstFunNode parent = (AstFunNode)node.jjtGetParent();
        Function f1 = parent.getFunction();
        Function f2 = node.getFunction();

        if( f1.getPriority() == Function.FUNCTION_PRIORITY || f1.getName().equals("=") )
            return false;

        if( f2.getName().equals("u-") )
        {
            if( parent.jjtGetChild(0) != node )
                return true;

            if( parent.jjtGetParent() instanceof AstFunNode )
            {
                Function f0 = ( (AstFunNode)parent.jjtGetParent() ).getFunction();
                if( f0.getPriority() == Function.PLUS_PRIORITY && parent.jjtGetParent().jjtGetChild(0) != parent )
                    return true;
            }
        }
        
        if( ( f2.getPriority() == Function.LOGICAL_PRIORITY && f1.getPriority() == Function.RELATIONAL_PRIORITY )
                || ( f2.getPriority() == Function.RELATIONAL_PRIORITY && f1.getPriority() == Function.LOGICAL_PRIORITY ) )
            return true;

        if( f1.getPriority() > f2.getPriority() )
            return true;

        if( f1.getPriority() == f2.getPriority() && f1.getPriority() != Function.FUNCTION_PRIORITY && parent.jjtGetChild(0) != node )
            return true;

        return false;
    }

    protected void processFunctionDeclaration(AstFunctionDeclaration node)
    {
        result.append("function ");
        result.append(node.getName());

        result.append("(");
        int n = node.jjtGetNumChildren() - 1;
        for( int i = 0; i < n; i++ )
        {
            processNode(node.jjtGetChild(i));
            if( i < n - 1 )
                result.append(", ");
        }
        result.append(")");

        result.append(" = ");
        processNode(node.jjtGetChild(n));
    }

    protected void processPiecewise(AstPiecewise node)
    {
        result.append("piecewise");
        result.append("( ");

        int n = node.jjtGetNumChildren();
        for( int i = 0; i < n; i++ )
        {
            if( i > 0 )
                result.append("; ");

            processPiece((AstPiece)node.jjtGetChild(i));
        }

        result.append(" )");
    }

    protected void processPiece(AstPiece node)
    {
        if( node.getCondition() != null )
        {
            processNode(node.getCondition());
            result.append(" => ");
        }

        processNode(node.getValue());
    }
    
    //utility methods
    protected void append(String str)
    {
        result.append(str);
    }
    
    protected void append(String before, Node node, String after)
    {
        result.append(before);
        processNode(node);
        result.append(after);
    }

    protected void append(String start, Node node1, String middle, Node node2, String end)
    {
        result.append(start);
        processNode(node1);
        result.append(middle);
        processNode(node2);
        result.append(end);
    }
}
