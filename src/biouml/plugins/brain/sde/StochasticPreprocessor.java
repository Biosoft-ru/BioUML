package biouml.plugins.brain.sde;

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
import biouml.model.dynamics.Function;
import biouml.plugins.simulation.Preprocessor;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

public class StochasticPreprocessor extends Preprocessor
{
    private final String STOCHASTIC_FUNCTION_NAME = SdeEModel.STOCHASTIC_FUNCTION_NAME;
    
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
        if(!(role instanceof EModel))
        {
            return false;
        }
        int modelType = ((EModel)role).getModelType();
        return EModel.isOfType(modelType, SdeEModel.SDE_TYPE)
                || EModel.isOfType(modelType, SdeEModel.STOCHASTIC_TYPE);
    }

    private boolean process(Node node, Diagram diagram) throws Exception
    {
        boolean changed = false;
        
        /**
         * StochasticPreprocessor convert expressions "x = stochastic(distr_type, loc, scale)" 
         * into two "x = AUX_stochastic_i" and "AUX_stochastic_i = stochastic4(distr_type, loc, scale, AUX_stochastic_i)".
         * 
         * Digit 4 in stochastic4 added for parser warnings suppression.
         */
        if (node instanceof AstFunNode && STOCHASTIC_FUNCTION_NAME.equals(((AstFunNode)node).getFunction().getName()))
        {	
            Node parent = node.jjtGetParent();
            
            String auxVarName = generateAuxVariable(diagram, STOCHASTIC_FUNCTION_NAME);
            AstConstant auxArgument = new AstConstant(ParserTreeConstants.JJTCONSTANT);
            auxArgument.setValue(auxVarName);
            
            node.jjtAddChild(auxArgument, node.jjtGetNumChildren()); // add auxVarName as last argument to stochastic function
            
            Formatter formatter = new LinearFormatter();
            AstStart start = Utils.createStart(node);
            String formula = formatter.format(start)[1];
            formula = formula.replaceFirst(STOCHASTIC_FUNCTION_NAME, STOCHASTIC_FUNCTION_NAME + "4"); // suppress parser warnings about 3 arguments in stochastic function
            
            generateAuxEquation(diagram, auxVarName, formula);
            
            AstVarNode auxVarNode = new AstVarNode(ParserTreeConstants.JJTVARNODE);
            auxVarNode.setName(auxVarName);
            //auxVarNode.jjtSetParent(parent);
            
            parent.jjtReplaceChild(node, auxVarNode);

            changed = true; 
        }
        else
        {
            for (int i = 0; i < node.jjtGetNumChildren(); i++)
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
        String name = "AUX_" + baseName + "_0";
        while( emodel.containsVariable(name) )
        {
            name = "AUX_" + baseName + "_" + suffix;
            suffix++;
        }

        emodel.declareVariable(name, 0.0);
        return name;
    }

    public static void generateAuxEquation(Diagram diagram, String varName, String formula) throws Exception
    {
        String eqName = DefaultSemanticController.generateUniqueNodeName(diagram, "equation");
        biouml.model.Node node = new biouml.model.Node(diagram, new Stub(diagram, eqName, Type.MATH_EQUATION));
        //Equation equation = new Equation(node, Equation.TYPE_SCALAR_DELAYED, varName);
        //Equation equation = new Equation(node, StochasticEquation.TYPE_SCALAR_STOCHASTIC, varName);
        Equation equation = new Equation(node, Equation.TYPE_SCALAR, varName);
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
