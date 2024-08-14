package biouml.plugins.research.research;

import biouml.plugins.research.workflow.MessageBundle;

import com.developmentontheedge.beans.BeanInfoEx;

public class ResearchDiagramTypeBeanInfo extends BeanInfoEx
{

    public ResearchDiagramTypeBeanInfo()
    {
        super(ResearchDiagramType.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName     (getResourceString("CN_RESEARCH_DIAGRAM"));
        beanDescriptor.setShortDescription(getResourceString("CD_RESEARCH_DIAGRAM"));
    }

}
