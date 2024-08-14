package biouml.plugins.simulation;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class EquationTypePreprocessor extends Preprocessor
{

    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        //TODO: set correct type on creation (but we should check all type checking in analyses and else)
        for( Equation eq : diagram.getRole( EModel.class ).getEquations() )
        {
            if( isInternal( eq ) && eq.getType().equals( Equation.TYPE_SCALAR ) )
                eq.setType( Equation.TYPE_SCALAR_INTERNAL );
        //TODO: set correct type (RATE_BY_RULE) but check simulation tests and everything else
//            else if (!isInternal(eq) && eq.getType().equals( Equation.TYPE_RATE ))
//                eq.setType( Equation.TYPE_RATE_BY_RULE );
        }
        return diagram;
    }


    public static boolean isInternal(Equation eq)
    {
        return (eq.getDiagramElement() instanceof Node && ( (Node)eq.getDiagramElement() ).getKernel() instanceof Reaction) || (eq.getDiagramElement() instanceof Edge && ( (Edge)eq.getDiagramElement() ).getKernel() instanceof SpecieReference);
    }
}