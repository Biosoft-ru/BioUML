package biouml.plugins.simulation.java;

import biouml.plugins.simulation.ConstraintPreprocessor;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.SimulationEngineBeanInfo;

public class JavaSimulationEngineBeanInfo extends SimulationEngineBeanInfo
{
    public JavaSimulationEngineBeanInfo()
    {
        super(JavaSimulationEngine.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property("templateType").tags(JavaSimulationEngine.getTemplateMethods()).add();
        add("Threads");
        property("constraintsViolation").tags(ConstraintPreprocessor.getConstraintMethods()).add();
        property("fastReactions").tags(OdeSimulationEngine.getFastReactionsMethods()).add();
        addWithTags("algebraicSolverName", JavaSimulationEngine.availableSolvers);
        add("algebraicSolver");
    }
}
