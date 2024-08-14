package biouml.plugins.simulation;

import java.util.ArrayList;
import java.util.List;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import one.util.streamex.StreamEx;

public class ScalarCyclesPreprocessor extends Preprocessor
{

    private final static int OUTPUT_LIMIT = 10;
    
    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        List<Equation> cycledEquations = new ArrayList<>();
        diagram.getRole( EModel.class ).orderScalarEquations( cycledEquations );

        if (cycledEquations.size() > 0)
        {
            log.info("Assignments for next variables form cycle and were transformed to algebraic equations:");
            log.info(StreamEx.of(cycledEquations).limit(OUTPUT_LIMIT).map(eq -> eq.getVariable()).joining(", "));
            if( cycledEquations.size() > OUTPUT_LIMIT )
                log.info("and " + ( cycledEquations.size() - OUTPUT_LIMIT ) + " more...");
            log.info("To avoid this message please transform equations manually.");
        }
        //TODO: do something more clever then just transforming all equation into algebraic
        for( Equation eq : cycledEquations )
        {
            eq.setFormula( eq.getFormula() + " - " + eq.getVariable() );
            eq.setType( Equation.TYPE_ALGEBRAIC );
        }

        return diagram;
    }

}