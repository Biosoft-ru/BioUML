package biouml.plugins.wdl;

import biouml.model.Diagram;

public class CWLGenerator extends WorkflowTextGenerator
{
    private static String TEMPLATE_PATH = "resources/cwl.vm";
    private static String TEMPLATE_NAME = "CWL template";
  
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
        return new CWLVelocityHelper( diagram );
    }
}