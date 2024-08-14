package ru.biosoft.math.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import ru.biosoft.math.MathRoutines;
import ru.biosoft.math.parser.ParserTreeConstants;

public class Utils
{
    
    public static AstConstant createConstant(Object value)
    {
        AstConstant result = new AstConstant(ParserTreeConstants.JJTCONSTANT);
        result.setValue(value);
        return result;
    }
    
    public static AstVarNode createVariabl(String name)
    {
        AstVarNode result = new AstVarNode(ParserTreeConstants.JJTVARNODE);
        result.setName(name);
        return result;
    }
    
    public static AstFunNode applyFunction(Node node1, Node node2, Function function)
    {
        AstFunNode astFunNode = new AstFunNode(ParserTreeConstants.JJTFUNNODE);
        astFunNode.setFunction(function);
        astFunNode.jjtAddChild(node1, 0);
        astFunNode.jjtAddChild(node2, 1);
        return astFunNode;
    }

    public static AstFunNode applyFunction(Node node1, Function function)
    {
        AstFunNode astFunNode = new AstFunNode(ParserTreeConstants.JJTFUNNODE);
        astFunNode.setFunction(function);
        astFunNode.jjtAddChild(node1, 0);
        return astFunNode;
    }
    
    /**
     * creates Ast tree representing sequence f(...f(f(x0, x1), x2),...xn)
     * E.g.: x1 && x2 && x3 && ... && xn 
     */
    public static Node applyFunction(Node[] nodes, Function function)
    {
        Node firstArgument = nodes[0];      
        for (int i=1; i<nodes.length; i++)
            firstArgument = applyFunction(firstArgument, nodes[i], function);
        return firstArgument;
    }

    public static Node applyFunction(AstStart node1, AstStart node2, Function function)
    {
        Node n1 = cloneAST(node1.jjtGetChild(0));
        Node n2 = cloneAST(node2.jjtGetChild(0));
        return applyFunction(n1, n2, function);
    }

    public static Node applyPlus(AstStart node1, AstStart node2)
    {
        return applyFunction(node1, node2, new PredefinedFunction(DefaultParserContext.PLUS, Function.PLUS_PRIORITY, -1));
    }
    
    public static Node applyMinus(Node node1, Node node2)
    {
        return applyFunction(node1, node2, new PredefinedFunction(DefaultParserContext.MINUS, Function.PLUS_PRIORITY, -1));
    }
    
    public static Node applyPlus(Node[] nodes)
    {
        return applyFunction(nodes, new PredefinedFunction(DefaultParserContext.PLUS, Function.PLUS_PRIORITY, -1));
    }
    
    public static AstFunNode applyTimes(Node node1, Node node2)
    {
        return applyFunction(node1, node2, new PredefinedFunction(DefaultParserContext.TIMES, Function.TIMES_PRIORITY, -1));
    }
    
    public static AstFunNode applyDivide(Node node1, Node node2)
    {
        return applyFunction(node1, node2, new PredefinedFunction(DefaultParserContext.DIVIDE, Function.TIMES_PRIORITY, -1));
    }



    public static Node cloneAST(Node node)
    {
        Node destNode = node.cloneAST();
        copyChildren(node, destNode);
        return destNode;
    }

    private static void copyChildren(Node srcNode, Node destNode)
    {
        for( int i = 0; i < srcNode.jjtGetNumChildren(); i++ )
        {
            destNode.jjtAddChild(cloneAST(srcNode.jjtGetChild(i)), i);
        }
    }

    public static boolean equalsAST(Node node1, Node node2)
    {
        if( node1 == null )
        {
            return node2 == null;
        }

        if( !node1.equals(node2) )
        {
            return false;
        }

        if( node1.jjtGetNumChildren() != node2.jjtGetNumChildren() )
        {
            return false;
        }

        for( int i = 0; i < node1.jjtGetNumChildren(); i++ )
        {
            if( !equalsAST(node1.jjtGetChild(i), node2.jjtGetChild(i)) )
            {
                return false;
            }
        }

        return true;
    }

    private static void substituteVarsInPlace(Node node, Map<String, String> mapping)
    {
        if( node instanceof AstVarNode )
        {
            AstVarNode varNode = (AstVarNode)node;
            String substName = mapping.get(varNode.getName());
            if( substName != null )
                varNode.setName(substName);
        }
        else
        {
            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            {
                substituteVarsInPlace(node.jjtGetChild(i), mapping);
            }
        }
    }

    public static AstStart substituteVars(AstStart math, Map<String, String> srcToDestVars)
    {
        Node node = cloneAST(math);
        substituteVarsInPlace(node, srcToDestVars);
        return (AstStart)node;
    }

    public static AstStart createStart(Node node)
    {
        AstStart astStart = new AstStart(ParserTreeConstants.JJTSTART);
        astStart.jjtAddChild(node, 0);
        return astStart;
    }

    /**
     * Substitute variables with their values (that's useful
     * when calculating initial values for dependent variables
     * in simulation engines).
     * 
     * @param node AST
     * @param values - values of variables and constants
     * @return pruned tree with constants and variables calculated
     */
    public static void calculateVariables(Node node, Map<String, ? extends Object> values)
    {
        if( node instanceof AstVarNode )
        {
            AstVarNode varNode = (AstVarNode)node;
            final String varName = varNode.getName();
            final Object value = values.get(varName);
            if( value != null )
            {
                final Node parent = node.jjtGetParent();
                if( parent != null )
                {
                    AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
                    constant.setValue(value);
                    parent.jjtReplaceChild(node, constant);
                }
            }
        }
        else
        {
            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            {
                calculateVariables(node.jjtGetChild(i), values);
            }
        }
    }


    /**
     * Basic optimization tools should be added here.
     */
    public static Node optimizeAST(Node node)
    {
        final Node optimized = cloneAST(node);
        pruneFunctions(optimized);
        optimizeDummyExpressions(optimized);
        return optimized;
    }

    /**
     * Processes optimizations of
     *  "x + 0", "x * 1", "x ^ 0", "x ^ 1".
     *  "x/x", "x*0" and similar cases
     * 
     *  It's suggested to be used after "pruneFunctions", so there
     *  must not be any constant-valued expressions like "1+0".
     */
    public static void optimizeDummyExpressions(Node node)
    {
        if( node instanceof AstFunNode || node instanceof AstStart )
        {
            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            {
                optimizeDummyExpressions(node.jjtGetChild(i));
            }
        }

        if( node instanceof AstFunNode )
        {
            AstFunNode funNode = (AstFunNode)node;
            Function function = funNode.getFunction();
            final String functionName = function.getName();
            if( DefaultParserContext.PLUS.equals(functionName) )
            {
                final Node arg1 = node.jjtGetChild(0);
                final Node arg2 = node.jjtGetChild(1);
                if( arg1 instanceof AstConstant )
                {
                    if( getAsDouble((AstConstant)arg1) == 0. )
                    {
                        // replace "0+x" with "x"
                        replaceNode(node, arg2);
                    }
                }
                if( arg2 instanceof AstConstant )
                {
                    if( getAsDouble((AstConstant)arg2) == 0. )
                    {
                        // replace "x+0" with "x"
                        replaceNode(node, arg1);
                    }
                }
            }
            else if( DefaultParserContext.TIMES.equals(functionName) )
            {
                final Node arg1 = node.jjtGetChild(0);
                final Node arg2 = node.jjtGetChild(1);
                if( arg1 instanceof AstConstant )
                {
                    final Double v1 = getAsDouble((AstConstant)arg1);
                    if( v1 == 1. )
                    {
                        // replace "1*x" with "x"
                        replaceNode(node, arg2);
                    }
                    else if( v1 == 0. )
                    {
                        // replace "0*x" with "0"
                        replaceNode(node, arg1);
                    }
                }
                if( arg2 instanceof AstConstant )
                {
                    final Double v2 = getAsDouble((AstConstant)arg2);
                    if( v2 == 1. )
                    {
                        // replace "x*1" with "x"
                        replaceNode(node, arg1);
                    }
                    else if( v2 == 0. )
                    {
                        // replace "x*0" with "0"
                        replaceNode(node, arg2);
                    }
                }
            }
            else if( DefaultParserContext.POWER.equals(functionName) )
            {
                final Node arg1 = node.jjtGetChild(0);
                final Node arg2 = node.jjtGetChild(1);
                if( arg1 instanceof AstConstant )
                {
                    if( getAsDouble((AstConstant)arg1) == 1. )
                    {
                        // replace "1^x" with "1"
                        replaceNode(node, arg1);
                    }
                }
                if( arg2 instanceof AstConstant )
                {
                    final Double v2 = getAsDouble((AstConstant)arg2);
                    if( v2 == 1. )
                    {
                        // replace "x^1" with "x"
                        replaceNode(node, arg1);
                    }
                    if( v2 == 0. )
                    {
                        // replace "x^0" with "1"
                        AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
                        constant.setValue(1.);
                        node.jjtGetParent().jjtReplaceChild(node, constant);
                    }
                }
            }
            else if( DefaultParserContext.DIVIDE.equals(functionName) )
            {
                final Node arg1 = node.jjtGetChild(0);
                final Node arg2 = node.jjtGetChild(1);

                if( equalsAST(arg1, arg2) )
                {
                    // replace "x/x" with "1"
                    AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
                    constant.setValue(1.);
                    node.jjtGetParent().jjtReplaceChild(node, constant);
                }
                else
                {
                    if( arg1 instanceof AstConstant )
                    {
                        final Double v1 = getAsDouble((AstConstant)arg1);
                        if( v1 == 0. )
                        {
                            // replace "0/x" with "0"
                            replaceNode(node, arg1);
                        }
                    }
                    if( arg2 instanceof AstConstant )
                    {
                        final Double v2 = getAsDouble((AstConstant)arg2);
                        if( v2 == 1. )
                        {
                            // replace "x/1" with "x"
                            replaceNode(node, arg1);
                        }
                    }
                }
            }
        }
    }

    private static void replaceNode(Node node, final Node arg1)
    {
        final Node parent = node.jjtGetParent();
        if( parent != null )
        {
            parent.jjtReplaceChild(node, arg1);
        }
    }


    /**
     * Generic AST visiting
     * 
     * @param node
     * @param astVisitor
     * @throws Exception
     */
    public static void visitAST(Node node, ASTVisitor astVisitor) throws Exception
    {
        visitAST(node, new ASTVisitor[] {astVisitor});
    }

    public static void visitAST(Node node, ASTVisitor[] astVisitors) throws Exception
    {
        if( node == null )
            return;

        if( node instanceof AstStart )
        {
            for( ASTVisitor visitor : astVisitors )
                visitor.visitStart((AstStart)node);
        }
        else
        {
            for( ASTVisitor visitor : astVisitors )
                visitor.visitNode(node);
        }

        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            visitAST(node.jjtGetChild(i), astVisitors);
        }
    }

    public static void visitAST(Node node, Collection<ASTVisitor> astVisitors) throws Exception
    {
        if( node == null )
            return;

        if( node instanceof AstStart )
        {
            for( ASTVisitor visitor : astVisitors )
                visitor.visitStart((AstStart)node);
        }
        else
        {
            for( ASTVisitor visitor : astVisitors )
                visitor.visitNode(node);
        }

        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            visitAST(node.jjtGetChild(i), astVisitors);
        }
    }


    /**
     * Calculate function value for the case of
     * only constant arguments
     */
    public static void pruneFunctions(Node node)
    {
        if( node == null )
            return;

        if( node instanceof AstFunNode || node instanceof AstStart || node instanceof AstPiecewise || node instanceof AstPiece )
        {
            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            {
                pruneFunctions(node.jjtGetChild(i));
            }
        }

        if( node instanceof AstFunNode )
        {
            boolean allChildrenAreConstants = true;
            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            {
                if( ! ( node.jjtGetChild(i) instanceof AstConstant ) )
                {
                    allChildrenAreConstants = false;
                    break;
                }
            }

            if( allChildrenAreConstants )
            {
                AstFunNode funNode = (AstFunNode)node;
                Node result = calculateFunctionApplicationResult(funNode);
                Node parent = node.jjtGetParent();
                parent.jjtReplaceChild(node, result);
            }
        }
        else if( node instanceof AstPiecewise )
        {
            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            {
                AstPiece piece = (AstPiece)node.jjtGetChild( i );
                if( piece.jjtGetNumChildren() == 1 )
                {
                    Node otherwise = piece.jjtGetChild( 0 );
                    node.jjtGetParent().jjtReplaceChild( node, otherwise );
                    return;
                }
                else
                {
                    Node condition = piece.getCondition();
                    if( condition instanceof AstConstant )
                    {
                        Object condValue = ( (AstConstant)condition ).getValue();
                        if( ! ( condValue instanceof Boolean ) )
                            return;
                        if( (Boolean)condValue )
                        {
                            Node result = piece.getValue();
                            node.jjtGetParent().jjtReplaceChild( node, result );
                            return;
                        }
                    }
                    else
                        return;
                }
            }
        }
    }


    /**
     * 
     * @param funNode
     * @return
     */
    private static Node calculateFunctionApplicationResult(AstFunNode funNode)
    {
        Function function = funNode.getFunction();
        final String functionName = function.getName();
        if( DefaultParserContext.PLUS.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsDouble(arg1) + getAsDouble(arg2));
            return constant;
        }
        if( DefaultParserContext.MINUS.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsDouble(arg1) - getAsDouble(arg2));
            return constant;
        }
        if( DefaultParserContext.TIMES.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsDouble(arg1) * getAsDouble(arg2));
            return constant;
        }
        if( DefaultParserContext.DIVIDE.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsDouble(arg1) / getAsDouble(arg2));
            return constant;
        }
        if( DefaultParserContext.POWER.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(Math.pow(getAsDouble(arg1), getAsDouble(arg2)));
            return constant;
        }
        if( DefaultParserContext.ROOT.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(Math.pow(getAsDouble(arg1), 1. / getAsDouble(arg2)));
            return constant;
        }
        if( DefaultParserContext.UMINUS.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue( -getAsDouble(arg1));
            return constant;
        }
        if( DefaultParserContext.ABS.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(Math.abs(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.EXP.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(Math.exp(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.LN.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(Math.log(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.LOG.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            if( funNode.jjtGetNumChildren() == 1 )
            {
                AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
                constant.setValue(Math.log10(getAsDouble(arg1)));
                return constant;
            }
            else if( funNode.jjtGetNumChildren() == 2 )
            {
                AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
                AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
                constant.setValue(Math.log(getAsDouble(arg1)) / Math.log(getAsDouble(arg2)));
                return constant;
            }
            return null;
        }
        if( DefaultParserContext.SIN.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(Math.sin(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.COS.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(Math.cos(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.TAN.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(Math.tan(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.COT.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(1. / Math.tan(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.ASIN.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(Math.asin(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.ACOS.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(Math.acos(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.ARCCOSH.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.ach(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.ARCSINH.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.ash(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.ARCCOTH.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.actgh(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.ARCTANH.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.atgh(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.SEC.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.sec(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.SECH.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.sech(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.CSC.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.csec(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.CSCH.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.csech(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.ARCSEC.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.asec(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.ARCSECH.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.asech(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.ARCSCS.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.acsec(getAsDouble(arg1)));
            return constant;
        }
        if( DefaultParserContext.ARCCOTH.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue(MathRoutines.actgh(getAsDouble(arg1)));
            return constant;
        }

        // boolean values
        if( DefaultParserContext.OR.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsBoolean(arg1) || getAsBoolean(arg2));
            return constant;
        }
        if( DefaultParserContext.AND.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsBoolean(arg1) && getAsBoolean(arg2));
            return constant;
        }
        if( DefaultParserContext.NOT.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            constant.setValue( !getAsBoolean(arg1));
            return constant;
        }
        if( DefaultParserContext.XOR.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsBoolean(arg1) ^ getAsBoolean(arg2));
            return constant;
        }
        if( DefaultParserContext.GT.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsDouble(arg1) > getAsDouble(arg2));
            return constant;
        }
        if( DefaultParserContext.LT.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsDouble(arg1) < getAsDouble(arg2));
            return constant;
        }
        if( DefaultParserContext.GEQ.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsDouble(arg1) >= getAsDouble(arg2));
            return constant;
        }
        if( DefaultParserContext.LEQ.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsDouble(arg1) <= getAsDouble(arg2));
            return constant;
        }
        if( DefaultParserContext.EQ.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(getAsDouble(arg1).equals(getAsDouble(arg2)));
            return constant;
        }
        if( DefaultParserContext.NEQ.equals(functionName) )
        {
            AstConstant constant = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            AstConstant arg1 = (AstConstant)funNode.jjtGetChild(0);
            AstConstant arg2 = (AstConstant)funNode.jjtGetChild(1);
            constant.setValue(!getAsDouble(arg1).equals(getAsDouble(arg2)));
            return constant;
        }

        return funNode;
    }

    private static Double getAsDouble(AstConstant arg)
    {
        final Object value = arg.getValue();
        if( value instanceof Double )
            return (Double)value;
        if( value instanceof Number )
            return ( (Number)value ).doubleValue();
        return 0.;
    }

    private static Boolean getAsBoolean(AstConstant arg)
    {
        final Object value = arg.getValue();
        if( value instanceof Boolean )
            return (Boolean)value;
        if( value instanceof Number )
            return ( (Number)value ).doubleValue() != 0.;
        return false;
    }
    
    public static StreamEx<Node> children(Node start)
    {
        return IntStreamEx.range( 0, start.jjtGetNumChildren() ).mapToObj( start::jjtGetChild );
    }
    
    public static StreamEx<Node> deepChildren(Node start)
    {
        return StreamEx.ofTree( start,
                n -> n.jjtGetNumChildren() == 0 ? null : IntStream.range( 0, n.jjtGetNumChildren() ).mapToObj( n::jjtGetChild ) );
    }

    /**
     * Get all variables from AST.
     * 
     * @param node start AST node
     * @return list of variable names
     */
    public static List<String> getVariables(Node node)
    {
        return variables(node).toList();
    }
    
    /**
     * Get all variables from AST.
     * 
     * @param node start AST node
     * @return stream of variable names
     */
    public static StreamEx<String> variables(Node node)
    {
        return Utils.deepChildren( node ).select( AstVarNode.class ).map( AstVarNode::getName ).distinct();
    }

    /**
     * Get all variables from string
     * @param formula
     * @return stream of variable names
     */
    public static Stream<String> variables(String formula)
    {
        DefaultParserContext context = new DefaultParserContext();
        ru.biosoft.math.parser.Parser parser = new ru.biosoft.math.parser.Parser();
        parser.setContext( context );
        parser.setVariableResolver( new SimpleVariableResolver( context ) );
        parser.parse( formula );
        return context.getVariableNames().stream();
    }

    public static Stream<String> functions(String formula)
    {
        ru.biosoft.math.parser.Parser parser = new ru.biosoft.math.parser.Parser();                     
        parser.parse( formula );
        return deepChildren( parser.getStartNode() ).select(AstFunNode.class ).map( n->n.getFunction().getName() );
    }
    
    public static String formatErrors(Parser p)
    {
        List<String> errorList = p.getMessages();
        StringBuilder msg = new StringBuilder();
        for( String error: errorList )
        {
            msg.append("    ");
            msg.append(error);
            msg.append("\n");
        }
        return msg.toString();
    }
    
    public static AstStart parseExpression(String expression)
    {
        Parser parser = new ru.biosoft.math.parser.Parser();
        parser.setContext( new DefaultParserContext() );
        int status = parser.parse( expression );
        if( status != 0 )
            throw new ParseException( expression, status );
        return parser.getStartNode();
    }

    public static Object evaluateExpression(AstStart ast, Map<String, Object> scope)
    {
        Utils.calculateVariables( ast, scope );
        List<String> otherVariables = Utils.getVariables( ast );
        if( !otherVariables.isEmpty() )
            throw new UnresolvedVariablesException( otherVariables );
        Utils.pruneFunctions( ast );
        Node lastNode = ast.jjtGetChild( ast.jjtGetNumChildren() - 1 );
        return ( (AstConstant)lastNode ).getValue();
    }

    public static Object evaluateExpression(String expression, Map<String, Object> scope)
    {
        return evaluateExpression( parseExpression( expression ), scope );
    }

    public static class UnresolvedVariablesException extends RuntimeException
    {
        public UnresolvedVariablesException(List<String> variables)
        {
            super( "Unresolved variales: " + variables );
        }
    }

    public static class ParseException extends RuntimeException
    {
        public ParseException(String expression, int errorCode)
        {
            super( "Can not parse '" + expression + "': " + errorCode );
        }
    }

    public static class SimpleVariableResolver implements VariableResolver
    {
        private DefaultParserContext context;

        SimpleVariableResolver(DefaultParserContext context)
        {
            this.context = context;
        }

        @Override
        public String getVariableName(String variableTitle)
        {
            return context.containsVariable( variableTitle ) ? variableTitle : null;
        }

        @Override
        public String resolveVariable(String variableName)
        {
            return context.containsVariable( variableName ) ? variableName : null;

        }
    }
    
    /**
     * Method replaces any AstVarNodes within given <b>node</b> which names are listed in <b>replaces</b> keyset with corresponding value
     */
    public static void renameVariableInAST(Node node, Map<String, String> replaces)
    {
        if( node == null )
            return;

        Utils.deepChildren( node ).select( AstVarNode.class ).filter( var -> replaces.containsKey( var.getName() ) ).forEach( var -> {
            String replacement = replaces.get( var.getName() );
            var.setName( replacement );
            var.setTitle( replacement );
        } );
    }
}
