package biouml.plugins.wdl;

import biouml.model.Diagram;

public class WDLGenerator extends WorkflowTextGenerator
{
    private static String TEMPLATE_PATH = "resources/wdl.vm";
    private static String TEMPLATE_NAME = "WDL template";
  
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
        return new WDLVelocityHelper( diagram );
    }
}