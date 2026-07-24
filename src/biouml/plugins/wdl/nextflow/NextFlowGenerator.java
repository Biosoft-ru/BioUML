package biouml.plugins.wdl.nextflow;

import biouml.model.Diagram;
import biouml.plugins.wdl.WorkflowTextGenerator;
import biouml.plugins.wdl.WorkflowVelocityHelper;

public class NextFlowGenerator extends WorkflowTextGenerator
{
    private static String TEMPLATE_PATH = "resources/nextflow.vm";
    private static String TEMPLATE_NAME = "Nextflow template";
    private boolean isEntryWorkflow = true;
    private String publishDir = "";
    private boolean publishOutput = false;
    
    public NextFlowGenerator()
    {
        this.isEntryWorkflow = true;
    }

    public NextFlowGenerator(boolean isEntryWorkflow)
    {
        this.isEntryWorkflow = isEntryWorkflow;
    }
    
    public void setPublishOutput(boolean publishOutput)
    {
        this.publishOutput = publishOutput;
    }
    
    public void setPublishDir(String publishDir)
    {
        this.publishDir = publishDir;
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
        NextFlowVelocityHelper helper = new NextFlowVelocityHelper( diagram, isEntryWorkflow );
        helper.setPublishOutput( publishOutput );
        return helper;
    }

    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        NextFlowPreprocessor preprocessor = new NextFlowPreprocessor();
        preprocessor.setPublishDir( publishDir );
        return preprocessor.preprocess( diagram );
    }
}