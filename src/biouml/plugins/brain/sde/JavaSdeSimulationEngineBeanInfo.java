package biouml.plugins.brain.sde;

import biouml.plugins.simulation.ConstraintPreprocessor;
import biouml.plugins.simulation.SimulationEngineBeanInfo;

public class JavaSdeSimulationEngineBeanInfo extends SimulationEngineBeanInfo
{

    public JavaSdeSimulationEngineBeanInfo()
    {
        super(JavaSdeSimulationEngine.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        //property("templateType").tags(JavaSimulationEngine.getTemplateMethods()).add();
        //add("Threads");
        add("customSeed");
        //addHidden("seed");
        add("seed");
        property("constraintsViolation").tags(ConstraintPreprocessor.getConstraintMethods()).add();
    }
}