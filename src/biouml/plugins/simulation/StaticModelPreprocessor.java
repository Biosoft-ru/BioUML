package biouml.plugins.simulation;

import java.awt.Point;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.util.EModelHelper;

/**
 * Preprocessors works with models without ODE or reactions.
 * Such models still may have events, algebraic and scalar equations depending on time thus to sumulate them correctly we add one differential equation:
 * dt/dtime = 1
 *
 * After that he model may be simulated by any ODE solver in BioUML.
 *
 * @author Ilya
 *
 */
public class StaticModelPreprocessor extends Preprocessor
{

    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        if( !accept( diagram ) )
            return diagram;

        EModel eModel = diagram.getRole( EModel.class );
        String name = EModelHelper.generateUniqueVariableName( eModel, "t" );
        eModel.getVariables().put( new Variable(name, eModel, eModel.getVariables()) );
        Equation eq = new Equation( null, Equation.TYPE_RATE, name, "1" );
        DiagramElement de = diagram.getType().getSemanticController().createInstance( diagram, Equation.class, new Point( 0, 0 ), eq )
                .getElement();
        diagram.put( de );

        return diagram;
    }

    @Override
    public boolean accept(Diagram diagram)
    {
        Role role = diagram.getRole();
        if (!(role instanceof EModel))
            return false;

        EModel eModel = (EModel)role;
        int type = eModel.getModelType();

        return !EModel.isOfType(type, EModel.ODE_TYPE);
    }

}
