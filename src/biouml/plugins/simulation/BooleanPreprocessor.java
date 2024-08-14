package biouml.plugins.simulation;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.ExpressionOwner;
import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstPiece;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.DefaultParserContext;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.model.Utils;

/**
 * @author Ilya
 * Preprocessor transforming boolean functions to numeric
 * 
 * It is needed because in l3v2 of SBML logic and numeric expressions have equal rights, one can write something like "(true + 5) AND 0" or "if (7) then"
 * Mapping is as follows: "true" -> "1", "false" -> "0", "0" -> "false", "X" -> "true" when X is a number other than "0"
 * 
 * Full list of processings:
 * 
 * 1. Transform constants "true" --> "1", "false" --> "0" (except for the case when constant is an event trigger (i.e. event have trigger given as "false" or "true"))
 * 2. Transform logic functions to numeric:
 * A & B -> AND(A,B) which returns 1 if A == 1 and B == 1 and 0 otherwise
 * A || B -> OR(A,B) which returns 1 if A == 1 or B == 1 and 0 otherwise
 * A < B -> LT(A,B) which returns 1 if A < B and 0 otherwise
 * and so on...
 * 3. Transform event trigger from "A" to "A != 0" because if A is a logic expression it would be transformed to numeric by processFunction or processConstant
 * 4. Transform piecewise from "if A then B" to "if A != 0 then B" because if A is a logic expression it would be transformed to numeric by processFunction or processConstant
 */
public class BooleanPreprocessor extends Preprocessor
{
    private EModel emodel;

    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram.getRole() instanceof EModel;
    }

    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        emodel = diagram.getRole(EModel.class);
        List<ExpressionOwner> owners = diagram.recursiveStream().map(de -> de.getRole()).select(ExpressionOwner.class).toList();
        for( ExpressionOwner owner : owners )
        {
            String[] expressions = owner.getExpressions();
            for( int i = 0; i < expressions.length; i++ )
            {
                AstStart start = emodel.readMath(expressions[i], owner.getRole());
                if( start == null )
                    continue;
                processConstants(start);
                processFunctions(start);
                processPiecewise(start);
                expressions[i] = new LinearFormatter().format(start)[1];
            }
            
            owner.setExpressions(expressions);
            if( owner.getRole() instanceof Event )
                processEvent((Event)owner.getRole());  
        }
        return diagram;
    }

    /**
     * Transform constants "true" --> "1", "false" --> "0"
     * Except for the case when constant is an event trigger (i.e. event have trigger given as "false" or "true")
     */
    public void processConstants(Node node)
    {
        if( node instanceof AstConstant )
        {
            Node parent = node.jjtGetParent();
//            if( parent instanceof AstPiece && node.equals( ( (AstPiece)parent ).getCondition()) )
//                return;
            if( ( (AstConstant)node ).getValue().equals(true) )
                node.jjtGetParent().jjtReplaceChild(node, Utils.createConstant(1.0));
            else if( ( (AstConstant)node ).getValue().equals(false) )
                node.jjtGetParent().jjtReplaceChild(node, Utils.createConstant(0.0));
        }

        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            processConstants(node.jjtGetChild(i));
    }

    /**
     * Transforms logic functions to numeric:
     * A & B -> AND(A,B) which returns 1 if A == 1 and B == 1 and 0 otherwise
     * A || B -> OR(A,B) which returns 1 if A == 1 or B == 1 and 0 otherwise
     * A < B -> LT(A,B) which returns 1 if A < B and 0 otherwise
     * and so on...
     */
    public void processFunctions(Node node)
    {
        if( node instanceof AstFunNode )
        {
            Function f = ( (AstFunNode)node ).getFunction();
            if( DefaultParserContext.isRelationalOperator(f) || DefaultParserContext.isLogicalOperator(f) )
            {
                Node parent = node.jjtGetParent();
                PredefinedFunction function = new PredefinedFunction(nameToNumeric.get(f.getName()), Function.FUNCTION_PRIORITY, 2);
                Node newNode = node.jjtGetNumChildren() == 2 ? Utils.applyFunction(node.jjtGetChild(0), node.jjtGetChild(1), function)
                        : Utils.applyFunction(node.jjtGetChild(0), function);
                parent.jjtReplaceChild(node, newNode);
            }
        }
        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            processFunctions(node.jjtGetChild(i));
    }

    /**
     * Transforms event trigger from "A" to "A != 0" because if A is a logic expression it would be transformed to numeric by processFunction or processConstant
     */
    public void processEvent(Event event)
    {
        event.setTrigger("(" + event.getTrigger() + ") != 0");
    }

    /**
     * Transforms piecewise from "if A then B" to "if A != 0 then B" because if A is a logic expression it would be transformed to numeric by processFunction or processConstant
     */
    public void processPiecewise(Node node)
    {
        if( node instanceof AstPiece && ( (AstPiece)node ).jjtGetNumChildren() == 2 )
        {
            Node condition = node.jjtGetChild(0);
            PredefinedFunction f = new PredefinedFunction("!=", Function.RELATIONAL_PRIORITY, 2);
            node.jjtReplaceChild(condition, Utils.applyFunction(condition, Utils.createConstant(0.0), f));
        }

        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            processPiecewise(node.jjtGetChild(i));
    }

    private static Map<String, String> nameToNumeric = new HashMap()
    {
        {
            put(DefaultParserContext.LT, "NUMERIC_LT");
            put(DefaultParserContext.GT, "NUMERIC_GT");
            put(DefaultParserContext.EQ, "NUMERIC_EQ");
            put(DefaultParserContext.GEQ, "NUMERIC_GEQ");
            put(DefaultParserContext.LEQ, "NUMERIC_LEQ");
            put(DefaultParserContext.NEQ, "NUMERIC_NEQ");

            put(DefaultParserContext.AND, "NUMERIC_AND");
            put(DefaultParserContext.OR, "NUMERIC_OR");
            put(DefaultParserContext.XOR, "NUMERIC_XOR");
            put(DefaultParserContext.NOT, "NUMERIC_NOT");
        }
    };
}