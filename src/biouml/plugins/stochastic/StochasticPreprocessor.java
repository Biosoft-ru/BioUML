package biouml.plugins.stochastic;

import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;

import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.DefaultParserContext;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.parser.ParserTreeConstants;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.simulation.Preprocessor;
import biouml.standard.diagram.Util;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class StochasticPreprocessor extends Preprocessor
{
    private static final double AVOGADRO = 6.02214179E23;
    private EModel emodel;
    private AstFunNode functionMult;
    private int reactantInd = 0;
    protected static final Logger log = Logger.getLogger(StochasticPreprocessor.class.getName());

    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram.getRole() instanceof EModel;
    }
    
    @Override
    public Diagram preprocess(Diagram diagram)
    {
        emodel = diagram.getRole( EModel.class );
        for( Equation eq : emodel.getEquations() )
                stochasticEquationTransform(eq, emodel);

            //        //transforming concentration to molecules
            //        for( VariableRole var : emodel.getVariableRoles() )
            //        {
            //            if( isMolar( var.getName() ) )
            //                var.setInitialValue( var.getInitialValue() * AVOGADRO );
            //        }
        return diagram;
    }
    public boolean isMolar(String name)
    {
        Variable var = emodel.getVariable( name );
        if( var.getUnits().contains( "mole" ) )
        {
            return true;
        }
        return false;
    }


    private void stochasticEquationTransform(Equation eq, EModel emodel)
    {
        try
        {
            if( ! ( eq.getParent() instanceof Node ) || !Util.isReaction( eq.getParent() ) )
                return;
            
            AstStart math = eq.getMath();
            if( ! ( math.jjtGetChild(0) instanceof AstFunNode ) )
                return;
            
            eq.unlinkKernel();
            AstStart newMath = new AstStart(math.getId());
            functionMult = new AstFunNode(ParserTreeConstants.JJTFUNNODE);
            functionMult.setFunction(new PredefinedFunction(DefaultParserContext.TIMES, Function.TIMES_PRIORITY, -1));
            newMath.jjtAddChild(functionMult, 0);
            reactantInd = 0;
            
            HashMap<String, SpecieReference> reactants = new HashMap<>();
            for( SpecieReference sr : (Reaction) ( (Node)eq.getParent() ).getKernel() )
            {
                if( sr.isReactant() )
                    reactants.put(sr.getSpecieVariable(), sr);
            }
            
            HashMap<String, Integer> specieAppearence = new HashMap<>();
            boolean mathKinetic = fillKineticLawSpecies((AstFunNode)math.jjtGetChild(0), specieAppearence, reactants);

            if( !mathKinetic )
                return;
            
            for( SpecieReference sr : reactants.values() )
            {
                int stoich = Integer.parseInt(sr.getStoichiometry());
                String reactantName = sr.getSpecieVariable();
                if( !specieAppearence.containsKey(reactantName) )
                    continue; //do not add to kinetik law if it was not there originally

                functionMult.jjtAddChild(Utils.createVariabl(reactantName), reactantInd);
                for( int f = 0; f < stoich - 1; f++ )
                {
                    functionMult.jjtAddChild(Utils.applyFunction(Utils.createVariabl(reactantName), Utils.createConstant(f + 1),
                            new PredefinedFunction(DefaultParserContext.MINUS, Function.PLUS_PRIORITY, 2)), f + 1 + reactantInd);
                }

                reactantInd += stoich;
                if( stoich != 1 )
                    functionMult.jjtAddChild(Utils.createConstant(1.0 / factorial( stoich )), reactantInd++);
            }
            eq.setFormula(new LinearFormatter().format(newMath)[1]);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Can not process reaction equation " + eq.getNameInParent() + ", error " + ex.getMessage());
        }
    }

    private long factorial(int n)
    {
        return n <= 1 ? 1 : n * factorial(n - 1);
    }

    private boolean fillKineticLawSpecies(AstFunNode fun, Map<String, Integer> kineticLawSpecies, HashMap<String, SpecieReference> reactants)
    {
        String funName = fun.getFunction().getName();
        if( funName.equals(DefaultParserContext.TIMES) || ( ( funName.equals(DefaultParserContext.DIVIDE) ) && ( ! ( fun.jjtGetChild(1) instanceof AstVarNode ) ) ) )
        {
            for( int i = 0; i < fun.jjtGetNumChildren(); i++ )
            {
                ru.biosoft.math.model.Node nextNode = fun.jjtGetChild(i);
                if( nextNode instanceof AstVarNode && funName.equals(DefaultParserContext.TIMES) )
                {
                    AstVarNode var = (AstVarNode)nextNode;
                    String varName = var.getName();

                    Variable variable = emodel.getVariable(varName);

                    if( reactants.containsKey(variable.getName()) )
                    {
                        Integer val = kineticLawSpecies.get(varName);
                        val = ( val == null ) ? 1 : val + 1;
                        kineticLawSpecies.put(varName, val);
                    }
                    else
                    {
                        functionMult.jjtAddChild(var, reactantInd);
                        reactantInd++;
                    }
                }
                else if( nextNode instanceof AstConstant && funName.equals(DefaultParserContext.TIMES) )
                {
                    functionMult.jjtAddChild( nextNode, reactantInd );
                    reactantInd++;
                }
                else if( nextNode instanceof AstConstant && funName.equals(DefaultParserContext.DIVIDE) )
                {
                    Object object = ( (AstConstant)nextNode ).getValue();
                    if( object instanceof Double )
                        functionMult.jjtAddChild(Utils.createConstant(1 / (Double)object), reactantInd);
                    else if( object instanceof Integer )
                        functionMult.jjtAddChild(Utils.createConstant(1.0 / (Integer)object), reactantInd);
                    reactantInd++;
                }
                else if( nextNode instanceof AstFunNode )
                {
                   if (!fillKineticLawSpecies((AstFunNode)nextNode, kineticLawSpecies, reactants))
                        return false;
                }
            }
            return true;
        }
        return false;
    }
}
