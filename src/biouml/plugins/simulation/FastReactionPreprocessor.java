package biouml.plugins.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.SemanticController;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.model.Node;
import ru.biosoft.analysis.Util;

public class FastReactionPreprocessor extends Preprocessor
{
    private static final double FAST_REACTION_RATE_MULTIPLIER = 1E8; //TODO: make a parameter
    private Map<String, List<Equation>> varToEquations;
    private final Set<String> reactantProduct = new HashSet<>();
    private List<biouml.model.Node> reactions;
    private List<biouml.model.Node> fastReactions;
    private List<AstStart> newAlgebraicEquations = new ArrayList<>();
    private EModel emodel;
    private Diagram diagram;
    private String method;

    public FastReactionPreprocessor(String method)
    {
        this.method = method;
    }
    
    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram.getRole() instanceof EModel && hasFastReactions(diagram);
    }

    public boolean hasFastReactions(Compartment compartment)
    {
        for( DiagramElement de : compartment )
        {
            if( de instanceof biouml.model.Node && ( de.getKernel() instanceof Reaction ) && ( (Reaction)de.getKernel() ).isFast() )
                return true;
            if( de instanceof Compartment && hasFastReactions((Compartment)de) )
                return true;
        }
        return false;
    }

    public Diagram simplePreprocess(Diagram diagram)
    {
        for( biouml.model.Node node : fastReactions )
        {
            if( node.getRole() instanceof Equation )
            {
                Equation eq = node.getRole(Equation.class);
                eq.unlinkKernel();
                eq.setFormula(" ( " + eq.getFormula() + " ) * " + FAST_REACTION_RATE_MULTIPLIER);
            }
        }
        return diagram;
    }

    public static Map<String, List<Equation>> initDefiningEquations(EModel emodel)
    {
        Map<String, List<Equation>> varToEquations = new HashMap<>();
        emodel.getEquations().forEach(eq -> varToEquations.computeIfAbsent(eq.getVariable(), k -> new ArrayList<>()).add(eq));
        return varToEquations;
    }

    public void initReactions(Diagram diagram) throws Exception
    {
        reactions = new ArrayList<>();
        fastReactions = new ArrayList<>();

        for( biouml.model.Node n : diagram.recursiveStream().select(biouml.model.Node.class)
                .filter(n -> n.getKernel() instanceof Reaction) )
        {
            reactions.add(n);
            Reaction r = (Reaction)n.getKernel();
            if( r.isFast() )
            {
                for( SpecieReference ref : r.getSpecieReferences() )
                    reactantProduct.add(ref.getSpecieVariableRole(diagram).getName());
                fastReactions.add(n);
            }
        }
    }

    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        this.diagram = diagram;
        this.emodel = diagram.getRole(EModel.class);
        this.newAlgebraicEquations = new ArrayList<>();
        varToEquations = initDefiningEquations(emodel);
        initReactions(diagram);

        List<String> species = getSpecies();
        List<String> rates = getRates(); // rates list begins with fast reaction rates(F) and ends with slow ones(S), for example rates = (F1,F2,S1,S2,S3)
        //TODO: change to simple dividing into two lists

        double[][] stoichiometry = getStoichiometry(emodel, species, rates);

        int fastReactionsNumber = fastReactions.size();//getFastReactionsNumber( emodel, rates );
        int slowReactionsNumber = stoichiometry[0].length - fastReactionsNumber;

        List<String> fastRates = rates.subList(0, fastReactionsNumber);
        List<String> slowRates = rates.subList(fastReactionsNumber, rates.size());

        double[][] fastRateMatrix = new double[stoichiometry.length][fastReactionsNumber];
        double[][] slowRateMatrix = new double[stoichiometry.length][slowReactionsNumber];

        for( int j = 0; j < fastReactionsNumber; j++ )
            for( int i = 0; i < stoichiometry.length; ++i )
                fastRateMatrix[i][j] = stoichiometry[i][j];

        for( int j = 0; j < slowReactionsNumber; j++ )
            for( int i = 0; i < stoichiometry.length; ++i )
                slowRateMatrix[i][j] = stoichiometry[i][fastReactionsNumber + j];
        
        // leftNullSpaceOfFast contains as it's rows basis vectors of fastRateMatrix left null space
        double[][] leftNullSpaceOfFast = Util.matrixConjugate(Util.getLeftNullSpace(fastRateMatrix));
        
        if( reactions.size() == fastReactionsNumber ) //all reactions are fast
            generateFastReactionsSystem(species, fastRates, leftNullSpaceOfFast);
        
        if (method.equals(OdeSimulationEngine.ODE_SYSTEM))
            return simplePreprocess(diagram);
        
        double[][] newSlowRateMatrix = Util.matrixMultiply(leftNullSpaceOfFast, slowRateMatrix);
      
        if( isZero(newSlowRateMatrix) )
        {
            log.warning("Couldn't handle fast reactions with Quasi-steady state approximation.");
            log.warning("Reactions will be handled by significant rate increasing.");
            return simplePreprocess(diagram);
        }
        else
            generateFastReactionsSystem(species, slowRates, fastRates, leftNullSpaceOfFast, newSlowRateMatrix);

        //remove reactions
        SemanticController controller = diagram.getType().getSemanticController();
        for( biouml.model.Node node : reactions )
        {
            controller.remove(node);
            if( ! ( (Reaction)node.getKernel() ).isFast() )
            {
                Equation eq = node.getRole(Equation.class);
                createEquation(eq.getVariable(), eq.getFormula(), Equation.TYPE_SCALAR_INTERNAL);
            }
        }

        for( AstStart start : newAlgebraicEquations )
            createEquation(null, start, Equation.TYPE_ALGEBRAIC);
        return diagram;
    }

    private void transformMath(AstStart math, EModel emodel)
    {
        int childCount = math.jjtGetNumChildren();
        for( int i = 0; i < childCount; i++ )
        {
            Node child = math.jjtGetChild(i);
            child = boundaryConditionParser(child, emodel);
            math.jjtReplaceChild(math.jjtGetChild(i), child);
        }
    }

    private Node boundaryConditionParser(Node child, EModel emodel)
    {
        if( child instanceof AstVarNode )
        {
            AstVarNode tempChild = (AstVarNode)child;
            Variable sv = emodel.getVariable(tempChild.getName());
            if( sv instanceof VariableRole && ( (VariableRole)sv ).isBoundaryCondition() )
            {
                AstConstant tempVar = Utils.createConstant(sv.getInitialValue());
                tempVar.jjtSetParent(child.jjtGetParent());
                return tempVar;
            }
        }
        else
        {
            int childCount = child.jjtGetNumChildren();
            for( int i = 0; i < childCount; i++ )
            {
                Node newChild = child.jjtGetChild(i);
                newChild = boundaryConditionParser(newChild, emodel);
                child.jjtReplaceChild(child.jjtGetChild(i), newChild);
            }
        }
        return child;
    }

    private List<String> getSpecies()
    {
        List<String> species = new ArrayList<>();
        for( biouml.model.Node node : reactions )
        {
            for( Edge e : node.getEdges() )
            {
                biouml.model.Node specieNode = e.getOtherEnd(node);
                if( specieNode.getRole() instanceof VariableRole && !specieNode.getRole(VariableRole.class).isBoundaryCondition() )
                {
                    String varName = specieNode.getRole(VariableRole.class).getName();
                    if( !species.contains(varName) )
                        species.add(varName);
                }
            }
        }
        return species;
    }

    private List<String> getRates() // fast reactions located at the beginning of list, slow at the end
    {
        List<String> result = new ArrayList<>();
        for( biouml.model.Node node : reactions )
        {
            String varName = node.getRole(Equation.class).getVariable();
            if( ( (Reaction)node.getKernel() ).isFast() )
                result.add(0, varName);
            else
                result.add(varName);
        }
        return result;
    }

    private double[][] getStoichiometry(EModel emodel, List<String> species, List<String> rates)
    {
        double[][] rateMatrix = new double[species.size()][reactions.size()];

        //        double matrixElement = 0;
        for( String varName : species ) //fill reaction matrix by rows
        {
            List<Equation> equations = varToEquations.get( varName );
            if( equations == null )
                continue;
            for( Equation eq : equations )
            {
                Edge e = (Edge)eq.getParent();
                biouml.model.Node reactionNode = biouml.standard.diagram.Util.isReaction(e.getInput()) ? e.getInput() : e.getOutput();
                String rate = reactionNode.getRole(Equation.class).getVariable();
                SpecieReference ref = (SpecieReference)e.getKernel();
                String stoichiometryStr = ref.getStoichiometry();
                double stoichiometry = 1;
                try
                {
                    stoichiometry = Double.parseDouble(stoichiometryStr);
                }
                catch( NumberFormatException ex )
                {
                    Variable var = emodel.getVariable(stoichiometryStr);
                    if( var != null )
                        stoichiometry = var.getInitialValue(); //TODO: consider case when this variable is defined by equations
                }
                if( ref.isReactant() )
                    stoichiometry *= -1;
                if( rates.contains(rate) )
                    rateMatrix[species.indexOf(varName)][rates.indexOf(rate)] = stoichiometry;
            }
        }
        return rateMatrix;
    }

    private void generateFastReactionsSystem(List<String> species, List<String> fastRates, double[][] leftNullSpaceOfFast) throws Exception
    {
        formNewLeftPartOfODE(species, leftNullSpaceOfFast);
        formFastReactionEquilibration(fastRates);
    }


    private void generateFastReactionsSystem(List<String> species, List<String> slowRates, List<String> fastRates,
            double[][] leftNullSpaceOfFast, double[][] newSlowRateMatrix) throws Exception
    {
        List<String> newLeftPart = formNewLeftPartOfODE(species, leftNullSpaceOfFast);
        List<AstStart> newRightPart = formNewRightPartOfODE(slowRates, newSlowRateMatrix);
        EntryStream.zip(newLeftPart, newRightPart).forKeyValue((left, right) -> createEquation(left, right, Equation.TYPE_RATE));
        formFastReactionEquilibration(fastRates);
    }


    private void formFastReactionEquilibration(List<String> fastRateVector) throws Exception
    {
        for( String fastRateVariableName : fastRateVector )
        {
            AstStart math = varToEquations.get(fastRateVariableName).get(0).getMath();
            transformMath(math, emodel);////////////////////////// Boundary condition!!!!!!!!!!!!!!
            newAlgebraicEquations.add(math);
        }
    }

    private List<String> formNewLeftPartOfODE(List<String> diffVector, double[][] leftNullSpace) throws Exception// here also forms equations for new conservation laws
    {
        List<String> newLeftPart = new ArrayList<>();
        for( double[] row : leftNullSpace )
        {
            List<String> vars = new ArrayList<>();
            List<Double> coeffs = new ArrayList<>();
            for( int j = 0; j < leftNullSpace[0].length; j++ )
            {
                if( row[j] != 0 )
                {
                    vars.add(diffVector.get(j));
                    coeffs.add(row[j]);
                }
            }

            if( vars.size() > 1 )
            {
                String name = generateUniqueVariable("newRate");
                emodel.declareVariable(name, 0.0);
                generateBoundaryValue(name, vars, coeffs);
                newLeftPart.add(name);
                Node fun = Utils.applyPlus(
                        EntryStream.zip(vars, coeffs).append(name, -1.0).mapKeyValue((v, c) -> createNode(v, c)).toArray(Node[]::new));
                newAlgebraicEquations.add(Utils.createStart(fun));
            }
            else
            {
                newLeftPart.add(vars.get(0));
            }
        }
        return newLeftPart;
    }

    private AstVarNode createConversionFactor(String varName)
    {
        Variable var = emodel.getVariable(varName);
        if( var instanceof VariableRole )
        {
            String factor = ( (VariableRole)var ).getConversionFactor();
            if( factor == null || factor.isEmpty() )
                factor = emodel.getConversionFactor();
            if( factor != null && !factor.isEmpty() )
                return Utils.createVariabl(factor);
        }
        return null;
    }

    private Node createNode(String var, double coeff)
    {
        AstVarNode varNode = Utils.createVariabl(var);
        Node constant = Utils.createConstant(coeff);
        Node result = Utils.applyTimes(varNode, constant);
        Node conversion = createConversionFactor(var);
        if( conversion != null )
            result = Utils.applyDivide(result, conversion);
        return result;
    }

    private void generateBoundaryValue(String name, List<String> variables, List<Double> coeffs)
    {
        Node[] termsLeft = new Node[variables.size()];

        for( int i = 0; i < variables.size(); i++ )
        {
            String varName = variables.get(i);
            Variable variable = emodel.getVariable(varName);
            termsLeft[i] = Utils.applyTimes(Utils.createConstant(variable.getInitialValue()), Utils.createConstant(coeffs.get(i)));
            Node conversionNode = createConversionFactor(varName);
            if( conversionNode != null )
                termsLeft[i] = Utils.applyDivide(termsLeft[i], conversionNode); //TODO: consider intitial assignments
        }

        createEquation(name, Utils.createStart(Utils.applyPlus(termsLeft)), Equation.TYPE_INITIAL_ASSIGNMENT);
    }

    public String generateUniqueVariable(String base)
    {
        String v = base;
        int index = 1;
        while( emodel.getVariable(v) != null )
            v = base + index++;
        return v;
    }

    private List<AstStart> formNewRightPartOfODE(List<String> slowRateVector, double[][] newSlowRateMatrix)
    {
        List<AstStart> newRightPartMath = new ArrayList<>();
        for( double[] row : newSlowRateMatrix )
        {
            List<String> vars = new ArrayList<>();
            List<Double> coeffs = new ArrayList<>();
            for( int j = 0; j < newSlowRateMatrix[0].length; j++ )
            {
                if( row[j] != 0 )
                {
                    vars.add(slowRateVector.get(j));
                    coeffs.add(row[j]);
                }
            }

            Node funNode;

            if( vars.size() >= 2 )
                funNode = Utils.applyPlus(EntryStream.zip(vars, coeffs).mapKeyValue((v, c) -> createNode(v, c)).toArray(Node[]::new));
            else if( vars.size() == 1 )
                funNode = createNode(vars.get(0), coeffs.get(0));
            else
                funNode = Utils.createConstant(0.0);
            newRightPartMath.add(Utils.createStart(funNode));
        }
        return newRightPartMath;
    }

    public static boolean isZero(double[][] matrix)
    {
        return !StreamEx.of(matrix).flatMapToDouble(Arrays::stream).anyMatch(val -> val != 0);
    }

    private void createEquation(String variable, AstStart formula, String type)
    {
        createEquation(variable, new LinearFormatter().format(formula)[1], type);
    }

    private void createEquation(String variable, String formula, String type)
    {
        Equation eq = createEquation(diagram, variable, formula, type);
        if( variable == null )
            return;
        varToEquations.computeIfAbsent(variable, k -> new ArrayList<>()).add(eq);
    }


    public static Equation createEquation(Diagram diagram, String variable, String formula, String type)
    {
        String name = DefaultSemanticController.generateUniqueNodeName(diagram, "equation");
        biouml.model.Node node = new biouml.model.Node(diagram, new Stub(null, name, Type.MATH_EQUATION));
        Equation eq = new Equation(node, type, variable, formula);
        node.setRole(eq);
        diagram.put(node);
        return eq;
    }
}