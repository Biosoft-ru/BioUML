package biouml.plugins.brain.sde;

import java.util.ArrayList;
import java.util.List;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.DAEModelUtilities;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.ExpressionOwner;
import biouml.model.dynamics.State;
import biouml.model.dynamics.Transition;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.EModel.ExtendedAssignmentsFilter;
import biouml.model.dynamics.EModel.InitialEquationsFilter;
import biouml.model.dynamics.EModel.NodeFilter;
import biouml.standard.diagram.CompositeDiagramType;
import one.util.streamex.StreamEx;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.model.Utils;

@SuppressWarnings ("serial")
public class SdeEModel extends EModel
{
    //EModel types 
    public static final int SDE_TYPE = 64; // stochastic differential equations
    public static final int STOCHASTIC_TYPE = 128; // non-differential equations with stochastics
    
    // Auxiliary names created to support expressions in "stochastic" function. 
    public static final String STOCHASTIC_FUNCTION_NAME = "stochastic";
    public static final String STOCHASTIUC_AUX_VARIABLE_NAME = "AUX_" + STOCHASTIC_FUNCTION_NAME;

    public static final String SDE_EMODEL_TYPE_STRING = "SDE EModel";
    
    public SdeEModel(DiagramElement diagramElement)
    {
        super(diagramElement);
        declareFunction(new PredefinedFunction(STOCHASTIC_FUNCTION_NAME, ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 3)); // main stochastic function
        declareFunction(new PredefinedFunction(STOCHASTIC_FUNCTION_NAME + "4", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 4)); // auxiliary function for parser warnings suppression
        
        // Override of standard stochastic functions to fix the seed
        declareFunction(new PredefinedFunction("normal", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 2));
        declareFunction(new PredefinedFunction("uniform", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 2));
        declareFunction(new PredefinedFunction("poisson", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1));
        declareFunction(new PredefinedFunction("exponential", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1));
    }
    
    @Override
    public String getType()
    {
        return SDE_EMODEL_TYPE_STRING;
    }
    
    @Override
    public Role clone(DiagramElement de)
    {
        SdeEModel emodel = new SdeEModel(de);
        doClone(emodel);
        return emodel;
    }

    /**
     * returns model
     * @return
     */
    @Override
    public int getModelType()
    {
        int compoundType = STATIC_TYPE;
        for (Equation eq : getEquations().filter(eq -> !Equation.TYPE_INITIAL_ASSIGNMENT.equals(eq.getType())))
        {
            if (eq.getType().equals(Equation.TYPE_RATE) && !eq.isFast())
            {
                Variable variable = getVariable(eq.getVariable());
                // skip constants and boundaryConditions
                if (variable != null && !(variable.isConstant()
                        || (variable instanceof VariableRole && ((VariableRole)variable).isBoundaryCondition())))
                {
                    if (eq.getFormula().indexOf(STOCHASTIC_FUNCTION_NAME) != -1)
                    {
                        compoundType |= SDE_TYPE;
                    }
                    else 
                    {
                        compoundType |= ODE_TYPE;
                    }
                }
            }
            else if (!eq.isAlgebraic() && !eq.isFast())
            {
                Variable variable = getVariable(eq.getVariable());
                // skip internal variables
                if (variable != null)
                {
                    String varName = variable.getName();
                    if (!(varName.length() > 2 && varName.charAt(0) == '$' && varName.charAt(1) == '$'))
                    {
                        compoundType |= STATIC_EQUATIONS_TYPE;
                    }
                }
            }
            else
            {
                compoundType |= ALGEBRAIC_TYPE;
            }
        }


        Event[] events = getEvents();

        if (events != null && events.length > 0)
        {
            compoundType |= EVENT_TYPE;
        }

        if (hasStochasticNonDifferentialVariables(getDiagramElement()))
        {
            compoundType |= STOCHASTIC_TYPE; 
        }
        
        if (hasDelayedVariables(getDiagramElement()))
        {
            compoundType |= ODE_DELAY_TYPE;
        }

        State[] states = getStates();
        Transition[] transitions = getTransitions();
        if (states != null && states.length > 0 && transitions != null && transitions.length > 0)
        {
            compoundType |= STATE_TRANSITION_TYPE;
        }

        Diagram parentDiag = this.getParent();
        if (parentDiag.getType() instanceof CompositeDiagramType)
        {
            for (DiagramElement de : parentDiag)
            {
                if (de instanceof SubDiagram)
                {
                    compoundType |= ((SubDiagram)de).getDiagram().getRole(EModel.class).getModelType();   
                }
            }
        }

        return compoundType;
    }

    public String getModelTypeName()
    {
        int modelType = getModelType();
        if (modelType == STATIC_TYPE)
        {
            return "Static model"; 
        }

        String str = "";

        if (isOfType(modelType, SDE_TYPE)) 
        {
            str = "SDE model";
            if (isOfType(modelType, STATIC_EQUATIONS_TYPE))
            {
                str += " with static equations";
            }
        }
        else if (isOfType(modelType, ODE_TYPE))
        {
            str = "ODE model";
            if (isOfType(modelType, STATIC_EQUATIONS_TYPE))
            {
                str += " with static equations";
            }
        }
        else
        {
            if (isOfType(modelType, STATIC_EQUATIONS_TYPE))
            {
                str = "Model with static equations";
            }
        }

        if (isOfType(modelType, STOCHASTIC_TYPE))
        {
            str += ", with stochastics";
        }
        
        if (isOfType(modelType, ODE_DELAY_TYPE))
        {
            str += ", with delays";
        }

        if (isOfType(modelType, ALGEBRAIC_TYPE))
        {
            str += ", with algebraic rules";
        }

        if (isOfType(modelType, EVENT_TYPE))
        {
            str += ", with events";   
        }

        if (isOfType(modelType, STATE_TRANSITION_TYPE))
        {
            str += ", with states and transitions";
        }

        return str;
    }
    
    public boolean hasStochasticNonDifferentialVariables(DiagramElement de)
    {
        if (de instanceof Node || de instanceof Edge)
        {
            Role role = de.getRole();
            if (role instanceof ExpressionOwner)
            {   
                if (role instanceof Equation)
                {
                    Equation eq = (Equation)role;
                    // need to check only non-differential equations
                    if (eq.getType().equals(Equation.TYPE_RATE) && !eq.isFast())
                    {
                        return false;
                    }
                }
                
                for (String expression : ((ExpressionOwner)role).getExpressions())
                {
                    if (expression != null && expression.indexOf(STOCHASTIC_FUNCTION_NAME) != -1)
                    {
                        return true;
                    }
                }
            }

            if (de instanceof Compartment)
            {
                for (DiagramElement next : (Compartment)de)
                {
                    //We do not check the elements of subdiagrams of composite diagrams.
                    if (!(next instanceof Diagram) && hasStochasticNonDifferentialVariables(next))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static class StochasticExpression
    {
        public String varName;
        public Role role;

        public StochasticExpression(String varName, Role role)
        {
            this.varName = varName;
            this.role = role;
        }
    }

    public List<StochasticExpression> getStochasticExpressionsList()
    {
        List<StochasticExpression> varList = new ArrayList<>();
        fillStochasticVariablesList(getDiagramElement(), varList);
        return varList;
    }

    public void fillStochasticVariablesList(DiagramElement de, List<StochasticExpression> list)
    {
        if (de instanceof Node || de instanceof Edge)
        {
            Role role = de.getRole();
            if (role instanceof ExpressionOwner)
            {
                if (role instanceof Equation)
                {
                    Equation eq = (Equation)role;
                    
                    //eq.getMath();
                    
                    //if (eq.getFormula().indexOf(STOCHASTIC_FUNCTION_NAME) != -1)
                    
                    //if (eq.getVariable().indexOf(STOCHASTIUC_AUX_VARIABLE_NAME) != -1)
                    if (eq.getVariable().indexOf(STOCHASTIUC_AUX_VARIABLE_NAME) != -1)
                    {
                        list.add(new StochasticExpression(eq.getVariable(), role)); 
                    }
                }
                else if (role instanceof Event)
                {
                    Event ev = (Event)role;
//                    StreamEx.of(ev.getEventAssignment())
//                        .filter(as -> as.getExpressions()[1].indexOf(STOCHASTIC_FUNCTION_NAME) != -1)
//                        .forEach(as -> list.add(new StochasticExpression(as.getVariable(), role)));
                    
                    StreamEx.of(ev.getEventAssignment())
                    	.filter(as -> as.getExpressions()[0].indexOf(STOCHASTIUC_AUX_VARIABLE_NAME) != -1)
                    	.forEach(as -> list.add(new StochasticExpression(as.getVariable(), role)));
                }
            }

            //Stochastic variables of subdiagrams are not included into composite diagrams
            if (de instanceof Compartment)
            {
                for (DiagramElement innerDe : (Compartment)de)
                {
                    fillStochasticVariablesList(innerDe, list);   
                }
            }
        }
    }
    
//    public static class ExtendedAssignmentsFilter extends EModel.ExtendedAssignmentsFilter
//    {
//        EModel emodel;
//        public ExtendedAssignmentsFilter(EModel emodel)
//        {
//            super(emodel);
//        }
//        @Override
//        protected boolean isNodeAcceptable(Node de)
//        {
//            Equation role = de.getRole(Equation.class);
//            String type = role.getType();
//            String varName = role.getVariable();
//            if( varName == null ) //algebraic equation
//                return false;
//            Variable var = emodel.getVariable(varName);
//            if( var == null || var.isConstant() )
//                return false;
//            return Equation.TYPE_SCALAR.equals(type) || Equation.TYPE_SCALAR_INTERNAL.equals(type)
//                    || Equation.TYPE_SCALAR_DELAYED.equals(type) || StochasticEquation.TYPE_SCALAR_STOCHASTIC.equals(type);
//        }
//    }
//    
//    public static class InitialEquationsFilter extends EModel.InitialEquationsFilter
//    {
//        EModel emodel;
//        public InitialEquationsFilter(EModel emodel)
//        {
//            super(emodel);
//        }
//
//        @Override
//        protected boolean isNodeAcceptable(Node de)
//        {
//            Equation role = de.getRole(Equation.class);
//            String type = role.getType();
//
//            if( !Equation.TYPE_INITIAL_ASSIGNMENT.equals(type) && !Equation.TYPE_INITIAL_VALUE.equals(type) )//initial assignment still should be applied even if variable is constant
//            {
//                String varName = role.getVariable();
//                Variable var = emodel.getVariable(varName);
//                if( var == null || var.isConstant() )
//                    return false;
//            }
//            return Equation.TYPE_SCALAR.equals(type) || Equation.TYPE_INITIAL_ASSIGNMENT.equals(type)
//                    || Equation.TYPE_SCALAR_INTERNAL.equals(type) || Equation.TYPE_SCALAR_DELAYED.equals(type)
//                    || StochasticEquation.TYPE_SCALAR_STOCHASTIC.equals(type)
//                    || Equation.TYPE_INITIAL_VALUE.equals(type);
//        }
//    }
//    
//    public void orderScalarEquations(List<Equation> cycledEquations) throws Exception
//    {
//        orderedScalarEquations = new ArrayList<>();
//        DAEModelUtilities.reorderAssignmentRules(getEquations(new ExtendedAssignmentsFilter(this)).toSet(), orderedScalarEquations,
//                cycledEquations);
//    }
//    
//    public void orderInitialAssignments(List<Equation> cycledEquations) throws Exception
//    {
//        orderedInitialAssignments = new ArrayList<>();
//        DAEModelUtilities.reorderAssignmentRules(getEquations(new InitialEquationsFilter(this)).toSet(), orderedInitialAssignments,
//                cycledEquations);
//    }
}
