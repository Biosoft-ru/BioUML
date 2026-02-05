package biouml.plugins.wdl.cwl;

import biouml.model.Diagram;
import biouml.plugins.wdl.WorkflowTextGenerator;
import biouml.plugins.wdl.WorkflowVelocityHelper;

public class CWLGenerator extends WorkflowTextGenerator
{
    private static String TEMPLATE_PATH = "resources/cwl.vm";
    private static String TEMPLATE_NAME = "CWL template";
  
    public CWLGenerator()
    {
        
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
        return new CWLVelocityHelper( diagram );
    }
}