package biouml.plugins.sbml;

import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.standard.type.Compartment;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;

public class SbmlDiagramType_L2 extends SbmlDiagramType
{   
    @Override
    public Class[] getNodeTypes()
    {
        return new Class[] {Compartment.class, Specie.class, Reaction.class, Event.class, Equation.class, Function.class};
    }
}