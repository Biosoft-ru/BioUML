package biouml.plugins.simulation;

import java.awt.Point;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Event;
import biouml.standard.type.Stub;

/**
 * Transforms constraints to events
 */
public class ConstraintPreprocessor extends Preprocessor
{
    public static final String CONSTRAINTS_IGNORE = "Ignore";
    public static final String CONSTRAINTS_LOG = "Log message";
    public static final String CONSTRAINTS_STOP = "Stop simulation";

    private String constraintHandling = null;
    public ConstraintPreprocessor(String type)
    {
        constraintHandling = type;
    }

    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram.getRole() instanceof EModel;
    }

    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        if (constraintHandling.equals(CONSTRAINTS_IGNORE))
            return diagram;

        boolean stop =  constraintHandling.equals(CONSTRAINTS_STOP);
        EModel emodel = diagram.getRole(EModel.class);
        for( Constraint constraint : emodel.getConstraints() )
        {
            String name = DefaultSemanticController.generateUniqueNodeName(diagram, constraint.getDiagramElement().getName() + "_event");
            biouml.model.Node node = new biouml.model.Node(diagram, new Stub(diagram, name));
            Event event = new Event(node);
            event.setTrigger("!( "+constraint.getFormula()+" )");
            event.setEventAssignment(new Assignment[] {});
            event.setTriggerMessage(constraint.getMessage());
            DiagramElement de = diagram.getType().getSemanticController().createInstance( diagram, Event.class, new Point(), event )
                    .getElement();

            if( stop )
                de.getAttributes().add(new DynamicProperty("Terminal", Boolean.class, Boolean.TRUE));
            diagram.put(de);
        }
        return diagram;
    }

    public static String[] getConstraintMethods()
    {
        return new String[] {CONSTRAINTS_STOP, CONSTRAINTS_LOG, CONSTRAINTS_IGNORE};
    }
}