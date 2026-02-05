package biouml.plugins.wdl.nextflow;

import biouml.model.Diagram;
import biouml.plugins.wdl.WorkflowTextGenerator;
import biouml.plugins.wdl.WorkflowVelocityHelper;

public class NextFlowGenerator extends WorkflowTextGenerator
{
    private static String TEMPLATE_PATH = "resources/nextflow.vm";
    private static String TEMPLATE_NAME = "Nextflow template";
    private boolean isEntryWorkflow = true;

    public NextFlowGenerator()
    {
        this.isEntryWorkflow = true;
    }

    public NextFlowGenerator(boolean isEntryWorkflow)
    {
        this.isEntryWorkflow = isEntryWorkflow;
    }

    @Override
    public String getTemplateName()
    {
        return TEMPLATE_NAME;
    }

    @Override
    public String getTemplatePath()
    {
        return TEMPLATE_PATH;
    }

    @Override
    public WorkflowVelocityHelper getVelocityHelper(Diagram diagram)
    {
        return new NextFlowVelocityHelper( diagram, isEntryWorkflow );
    }

    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        return new NextFlowPreprocessor().preprocess( diagram );
    }
}