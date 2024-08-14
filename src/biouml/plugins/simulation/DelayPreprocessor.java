package biouml.plugins.simulation;

import java.util.List;
import java.util.logging.Level;

import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Formatter;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.parser.ParserTreeConstants;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.ExpressionOwner;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

public class DelayPreprocessor extends Preprocessor
{
    @Override
    public Diagram preprocess(Diagram diagram)
    {
        try
        {
            EModel eModel = diagram.getRole(EModel.class);
            Formatter formatter = new LinearFormatter();
            List<ExpressionOwner> expressionOwners = getExpressionOwners(diagram);
            for( ExpressionOwner owner : expressionOwners )
            {
                String[] oldExpressions = owner.getExpressions();
                String[] newExpressions = new String[oldExpressions.length];

                boolean changedAny = false;

                for( int i = 0; i < oldExpressions.length; i++ )
                {
                    AstStart start = eModel.readMath(oldExpressions[i], owner.getRole());
                    if( start == null )
                        continue;
                    boolean changed = process(start, diagram);
                    changedAny |= changed;
                    newExpressions[i] = ( changed ) ? formatter.format(start)[1] : oldExpressions[i];
                }
                if( changedAny )
                    owner.setExpressions(newExpressions);
            }
            return diagram;
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error during static model preprocessing " + ex);
        }
        return null;

    }

    @Override
    public boolean accept(Diagram diagram)
    {
        Role role = diagram.getRole();
        if( ! ( role instanceof EModel ) )
            return false;
        return EModel.isOfType(( (EModel)role ).getModelType(), EModel.ODE_DELAY_TYPE);
    }

    private boolean process(Node node, Diagram diagram) throws Exception
    {
        boolean changed = false;
        if( node instanceof AstFunNode && "delay".equals( ( (AstFunNode)node ).getFunction().getName()) )
        {
            Node parent = node.jjtGetParent();
            Node delayedNode = node.jjtGetChild(0);

            if( delayedNode instanceof AstVarNode )
            {
                return false;
            }
            else if( delayedNode instanceof AstConstant ) //process: "x = delay(c, d)" => "x = c", where c = constant
            {
                delayedNode.jjtSetParent(parent);
                parent.jjtReplaceChild(node, delayedNode);
                changed = true;
            }
            else
            //process: "x = delay(f, d)" => "x = delay(delayed, d) , delayed = f", where f - expression
            {
                process(delayedNode, diagram); //recursive delay
                delayedNode = node.jjtGetChild(0);
                Formatter formatter = new LinearFormatter();
                AstStart start = Utils.createStart(delayedNode);
                process(start, diagram);
                String formula = formatter.format(start)[1];
                String varName = generateAuxVariable(diagram, "delayed");
                generateAuxEquation(diagram, varName, formula);
                AstVarNode newVarNode = new AstVarNode(ParserTreeConstants.JJTVARNODE);
                newVarNode.setName(varName);
                newVarNode.jjtSetParent(node);
                node.jjtReplaceChild(delayedNode, newVarNode);
                changed = true;
            }
            Node delayNode = node.jjtGetChild(1);
            changed |= process(delayNode, diagram);
        }
        else
        {
            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
            {
                Node childNode = node.jjtGetChild(i);
                changed |= process(childNode, diagram);
            }
        }
        return changed;
    }

    public static String generateAuxVariable(Diagram diagram, String baseName) throws Exception
    {
        EModel emodel = diagram.getRole(EModel.class);
        int suffix = 1;
        String name = baseName;
        while( emodel.containsVariable(name) )
        {
            name = baseName + "_" + suffix;
            suffix++;
        }

        emodel.declareVariable(name, 0.0);
        return name;
    }

    public static void generateAuxEquation(Diagram diagram, String varName, String formula) throws Exception
    {
        String eqName = DefaultSemanticController.generateUniqueNodeName(diagram, "equation");
        biouml.model.Node node = new biouml.model.Node(diagram, new Stub(diagram, eqName, Type.MATH_EQUATION));
        Equation equation = new Equation(node, Equation.TYPE_SCALAR_DELAYED, varName);
        equation.setFormula(formula);
        node.setRole(equation);
        diagram.put(node);
    }

    public static List<ExpressionOwner> getExpressionOwners(Diagram diagram)
    {
        return diagram.recursiveStream().map(DiagramElement::getRole).filter(ExpressionOwner.class::isInstance)
                .map(ExpressionOwner.class::cast).toList();
    }

}
