package biouml.plugins.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.math.model.ASTVisitorSupport;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.Utils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Role;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.util.EModelHelper;
import biouml.standard.diagram.Util;

public class RateAssignmentRulePreprocessor extends Preprocessor
{

    private CompartmentDivisionASTVisitor visitor;
    private LinearFormatter formatter;
    private EModel emodel;
    private Map<String, List<Equation>> definingEquations;
    
    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram.getRole() instanceof EModel;
    }

    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        emodel = diagram.getRole( EModel.class );
        formatter = new LinearFormatter();
        visitor = new CompartmentDivisionASTVisitor();

        definingEquations = EModelHelper.findDefiningEquations(emodel);

        //initial assignments + scalar equations
        HashMap<String, Equation> initialEquations = new HashMap<>();

        for( Equation eq : emodel.getEquations() )
        {
            eq.unlinkKernel();
            if( !EquationTypePreprocessor.isInternal(eq)
                    && ( Equation.TYPE_INITIAL_ASSIGNMENT.equals(eq.getType()) || Equation.TYPE_SCALAR.equals(eq.getType())
                            || Equation.TYPE_RATE_BY_RULE.equals(eq.getType()) || Equation.TYPE_RATE.equals(eq.getType()) ) )

            {
                String newFormula = applyCompartmentRateRule(eq);
                if( newFormula == null )
                    newFormula = applyCompartment(eq.getVariable(), eq.getFormula(), emodel);
                if( newFormula != null )
                    eq.setFormula(newFormula);
                if( Equation.TYPE_INITIAL_ASSIGNMENT.equals(eq.getType()) || Equation.TYPE_SCALAR.equals(eq.getType()) )
                    initialEquations.put(eq.getVariable(), eq);
            }
            applyConversionFactor(eq);
            eq.setFormula(divideByCompartment(eq.getMath()));
        }

        for( Event eq : emodel.getEvents() )
        {
            for( Assignment sa : eq.getEventAssignment() )
            {
                String newFormula = applyCompartment(sa.getVariable(), sa.getMath(), emodel);
                if( newFormula != null )
                    sa.setMath(newFormula);
                sa.setMath(divideByCompartment(emodel.readMath(sa.getMath(), eq)));
            }
            eq.setTrigger(divideByCompartment(emodel.readMath(eq.getTrigger(), eq)));
            if( eq.getPriority() != null && !eq.getPriority().isEmpty() )
                eq.setPriority(divideByCompartment(emodel.readMath(eq.getPriority(), eq)));
        }

        for( VariableRole var : emodel.getVariableRoles() )
        {
            int initialType = var.getInitialQuantityType();

            if( initialType == VariableRole.CONCENTRATION_TYPE )
            {
                //search for initial assignments
                if( !initialEquations.containsKey(var.getName()) )//not any)
                {
                    Compartment compartment = var.getDiagramElement().getCompartment();
                    if( compartment.getRole() instanceof VariableRole )
                    {
                        FastReactionPreprocessor.createEquation( diagram, var.getName(), "(" + var.getName() + ")*"
                                + compartment.getRole( VariableRole.class ).getName(), Equation.TYPE_INITIAL_VALUE );
                    }
                }
            }
        }
        return diagram;
    }

    public void applyConversionFactor(Equation eq)
    {
        if( Equation.TYPE_RATE.equals(eq.getType()) && Util.isSpecieReference(eq.getParent())) //TODO: use RATE_BY_RULE type
        {
            String varName = eq.getVariable();
            Variable var = emodel.getVariable(varName);
            String conversionFactor = emodel.getConversionFactor(var);
            if( conversionFactor != null )
                eq.setFormula("(" + eq.getFormula() + ") *" + conversionFactor);
        }
    }
    
    /**
     * Transform differential equation for species when species is given in concentration and compartment also have differential equation:<br>
     * Given:<br>
     * d[S]/dt = k,<br>
     * dC/dt = p,<br>
     * [S] = S/C - concentration fo species S in compartment C.<br>
     * Result:<br>
     * dS/dt = k*C+p*S,<br> NOTE that S here will be automatically divided by C later
     * dC/dt = p.<br>
     *Result is differential equation for amount of species instead of concentration.
     */
    public String applyCompartmentRateRule(Equation eq)
    {
        if( Equation.TYPE_RATE_BY_RULE.equals(eq.getType()) || Equation.TYPE_RATE.equals(eq.getType()) )
        {
            String variableName = eq.getVariable();
            Variable variable = emodel.getVariable(variableName);
            if( ! ( variable instanceof VariableRole ) )
                return null;
            if( ( (VariableRole)variable ).getQuantityType() == VariableRole.AMOUNT_TYPE )
                return null;
            DiagramElement de = ( ( (VariableRole)variable ).getDiagramElement() );
            if( ! ( de instanceof biouml.model.Node ) )
                return null;
            Compartment compartment = ( (biouml.model.Node)de ).getCompartment();
            if( compartment instanceof Diagram )
                return null;

            VariableRole compartmentVariable = compartment.getRole( VariableRole.class );
            if( compartmentVariable.isConstant() || !definingEquations.containsKey(compartmentVariable.getName()) )
                return null;

            for( Equation compartmentEq : definingEquations.get(compartmentVariable.getName()) )
            {
                if( Equation.TYPE_RATE_BY_RULE.equals(compartmentEq.getType()) || Equation.TYPE_RATE.equals(compartmentEq.getType()) )
                    return "("+eq.getFormula() + ")*" + compartmentEq.getVariable() + "+" + compartmentEq.getFormula() + "*" + eq.getVariable();
            }
        }
        return null;
    }
    
    private static String applyCompartment(String varName, String formula, EModel emodel) // transform to concentration in rate rule if need
    {

        if( varName.length() > 1 && varName.charAt(0) == '$' && varName.charAt(1) != '$' )
        {


            Variable var = emodel.getVariable(varName);

            if( ! ( var instanceof VariableRole ) || ( (VariableRole)var ).getQuantityType() == VariableRole.AMOUNT_TYPE )
                return null;

            VariableRole varRole = (VariableRole)var;

            if( varRole.getDiagramElement().getKernel() instanceof biouml.standard.type.Compartment )
                return null;

            Compartment compartment = varRole.getDiagramElement().getCompartment();

            Role compRole = compartment.getRole();
            if( ! ( compRole instanceof Variable ) )
                return null;

            return "( " + formula + " ) * " + ( (Variable)compRole ).getName();
        }
        return null;
    }

    private String divideByCompartment(AstStart start) throws Exception
    {
        Utils.visitAST(start, visitor);
        return formatter.format(start)[1];
    }

    public class CompartmentDivisionASTVisitor extends ASTVisitorSupport
    {
        @Override
        public void visitNode(Node node) throws Exception
        {
            if( node instanceof AstVarNode )
            {
                AstVarNode variable = (AstVarNode)node;
                String varName = variable.getName();

                Variable var = emodel.getVariable(varName);
                if( ! ( var instanceof VariableRole ) )
                    return;

                if( ( (VariableRole)var ).getQuantityType() == VariableRole.AMOUNT_TYPE )
                    return;

                DiagramElement de = ( (VariableRole)var ).getDiagramElement();
                if( de.getKernel() instanceof biouml.standard.type.Compartment )
                    return;

                Compartment compartment = (Compartment)de.getParent();
                if( compartment == null || compartment instanceof Diagram )
                    return;
                Role role = compartment.getRole();
                if( role == null || ! ( role instanceof VariableRole ) )
                    return;
              
                Node compartmentNode = Utils.createVariabl(compartment.getRole( VariableRole.class ).getName());
                Node parent = variable.jjtGetParent();
                if( parent instanceof AstFunNode && ( "delay".equals( ( (AstFunNode)parent ).getFunction().getName()) ) )
                {
                    Function f = ((AstFunNode)parent).getFunction();
                    Node delayValue = parent.jjtGetChild(1);
                    node = parent;
                    parent = parent.jjtGetParent();
                    compartmentNode = Utils.applyFunction(compartmentNode, delayValue.cloneAST(), f);
                }
                parent.jjtReplaceChild(node, Utils.applyDivide(node, compartmentNode));
            }
        }
    }
}