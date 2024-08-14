package biouml.plugins.stochastic;

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
import biouml.plugins.simulation.EquationTypePreprocessor;
import biouml.plugins.simulation.FastReactionPreprocessor;
import biouml.plugins.simulation.Preprocessor;
import biouml.standard.diagram.Util;
import biouml.standard.type.BaseUnit;
import biouml.standard.type.Unit;

public class StochasticAssignmentPreprocessor extends Preprocessor
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
            if( !EquationTypePreprocessor.isInternal( eq ) && ( Equation.TYPE_INITIAL_ASSIGNMENT.equals( eq.getType() )
                    || Equation.TYPE_SCALAR.equals( eq.getType() ) || Equation.TYPE_RATE.equals( eq.getType() ) ) )

            {
                String newFormula = applyCompartment( eq.getVariable(), eq.getFormula(), emodel );
                if( newFormula != null )
                    eq.setFormula( newFormula );
                if( Equation.TYPE_INITIAL_ASSIGNMENT.equals( eq.getType() ) || Equation.TYPE_SCALAR.equals( eq.getType() ) )
                    initialEquations.put( eq.getVariable(), eq );
            }
            applyConversionFactor( eq );

            if( !Equation.TYPE_INITIAL_VALUE.equals( eq.getType() ) )
                eq.setFormula( divideByCompartment( eq.getMath() ) );
        }

        for( Variable var : emodel.getVariables() )
        {
            String compartmentFactor = "";
            if( var.getInitialValue() == 0.0 )
                continue;
            if( var instanceof VariableRole )
            {
                int initialType = ( (VariableRole)var ).getInitialQuantityType();

                if( initialType == VariableRole.CONCENTRATION_TYPE )
                {
                    Compartment c = ( (VariableRole)var ).getDiagramElement().getCompartment();
                    if( c.getRole() instanceof VariableRole )
                        compartmentFactor = " * " + c.getRole( VariableRole.class ).getName();
                }
            }
            double molarFactor = transformToMolar( emodel, var );
            String factor = compartmentFactor + "*" + molarFactor;
            if( compartmentFactor.isEmpty() && molarFactor == 1.0 )
                continue;

            if( !initialEquations.containsKey( var.getName() ) )
            {
                String formula = "(" + var.getName() + ")" + factor;
                if( var instanceof VariableRole )
                    formula = "round(" + formula + ")";
                FastReactionPreprocessor.createEquation( diagram, var.getName(), formula, Equation.TYPE_INITIAL_VALUE );
            }
            else
            {
                Equation eq = initialEquations.get( var.getName() );
                String formula = "(" + eq.getFormula() + ")" + factor;
                if( var instanceof VariableRole )
                    formula = "round(" + formula + ")";
                eq.setFormula( formula );
            }
        }


        for( Event eq : emodel.getEvents() )
        {
            for( Assignment sa : eq.getEventAssignment() )
            {
                String newFormula = applyCompartment(sa.getVariable(), sa.getMath(), emodel);
                if( newFormula != null )
                    sa.setMath(newFormula);
                sa.setMath( divideByCompartment( emodel.readMath( sa.getMath(), eq ) ) );
            }
            eq.setTrigger( divideByCompartment( emodel.readMath( eq.getTrigger(), eq ) ) );
            if( eq.getPriority() != null && !eq.getPriority().isEmpty() )
                eq.setPriority( divideByCompartment( emodel.readMath( eq.getPriority(), eq ) ) );
        }

        return diagram;
    }

    public static double getMultiplier(String varName, EModel emodel)
    {
        Variable var = emodel.getVariable( varName );
        double result = 1.0;
        if( var instanceof VariableRole )
        {
            DiagramElement de = ( (VariableRole)var ).getDiagramElement();
            if( de.getKernel() instanceof biouml.standard.type.Compartment )
                return result;
            Compartment comp = de.getCompartment();
            if( Util.isVariable( comp ) && comp.getRole( VariableRole.class ).getInitialValue() != 1.0 )
            {
                if( ( (VariableRole)var ).getInitialQuantityType() == VariableRole.CONCENTRATION_TYPE )
                {
                    result *= comp.getRole( VariableRole.class ).getInitialValue();
                }
            }
            result *= 6.02214179E23;
        }
        return result;
    }

    public static double transformToMolar(EModel emodel, Variable var) throws Exception
    {
        String units = var.getUnits();
        if( units.isEmpty() )
            return 1.0;
        if( units.equals( "mole" ) )
            return 6.02214179E23;
        Unit unit = emodel.getUnits().get( units );
        if( unit == null )
            return 1.0;

        for( BaseUnit baseUnit : unit.getBaseUnits() )
        {
            String type = baseUnit.getType();
            if( type.equals( "mole" ) )
            {
                double multiplier = transformToMolecules( baseUnit );
                return multiplier;
            }
        }
        return 1.0;
    }

    public static double transformToMolecules(BaseUnit unit) throws Exception
    {
        int scale = unit.getScale();
        double multiplier = unit.getMultiplier();
        double m = multiplier;
        double exponent = unit.getExponent();
        m *= Math.pow( 10, scale );
        double avogadro = 6.02214179E23;
        m *= avogadro;
        if( exponent != 1 )
            m = Math.pow( m, exponent );
        return m;
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
    
    private String applyCompartment(String varName, String formula, EModel emodel) // transform to concentration in rate rule if need
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

            if( !definingEquations.containsKey( compartment.getName() ) )
                return "( " + formula + " ) * " + ( (Variable)compRole ).getInitialValue();
            else
                return "( " + formula + " ) * " + ( (Variable)compRole ).getName();
        }
        return null;
    }

    private String divideByCompartment(AstStart start) throws Exception
    {
        Utils.visitAST( start, visitor );
        return formatter.format( start )[1];
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

                Variable var = emodel.getVariable( varName );
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

                Node compartmentNode = Utils.createVariabl( compartment.getRole( VariableRole.class ).getName() );
                Node parent = variable.jjtGetParent();
                if( parent instanceof AstFunNode && ( "delay".equals( ( (AstFunNode)parent ).getFunction().getName() ) ) )
                {
                    Function f = ( (AstFunNode)parent ).getFunction();
                    Node delayValue = parent.jjtGetChild( 1 );
                    node = parent;
                    parent = parent.jjtGetParent();
                    compartmentNode = Utils.applyFunction( compartmentNode, delayValue.cloneAST(), f );
                }
                parent.jjtReplaceChild( node, Utils.applyDivide( node, compartmentNode ) );
            }
        }
    }
}