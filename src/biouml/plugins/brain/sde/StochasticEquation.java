package biouml.plugins.brain.sde;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.ExpressionOwner;

@PropertyName("Stochastic equation")
@PropertyDescription("Mathematical stochastic equation.")
public class StochasticEquation extends Equation
{
    /**
     * Auxiliary equation (and variable) created to support expressions in "stochastic" function. 
     * So, expressions "x = stochastic(distr_type, loc, scale)" is converted
     * into two "x = AUX_stochastic_i" and "AUX_stochastic_i = stochastic(distr_type, loc, scale)".
     */
    public static final String TYPE_SCALAR_STOCHASTIC = "scalar_stochastic";
    
    StochasticEquation(DiagramElement de, String type, String variable, String formula)
    {
        super(de, type, variable, formula);
    }
}
