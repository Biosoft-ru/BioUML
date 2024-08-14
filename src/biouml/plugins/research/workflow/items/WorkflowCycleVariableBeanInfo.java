package biouml.plugins.research.workflow.items;

import biouml.plugins.research.workflow.items.WorkflowCycleVariable.CycleTypeSelector;

import ru.biosoft.access.security.SecurityManager;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class WorkflowCycleVariableBeanInfo extends WorkflowVariableBeanInfo
{
    public WorkflowCycleVariableBeanInfo()
    {
        this(WorkflowCycleVariable.class, "CYCLE VARIABLE");
    }

    public WorkflowCycleVariableBeanInfo(Class type, String name)
    {
        super(type, name);
        beanDescriptor.setDisplayName( getResourceString("CN_CYCLE_VARIABLE") );
        beanDescriptor.setShortDescription( getResourceString("CD_CYCLE_VARIABLE") );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        
        PropertyDescriptorEx pde = new PropertyDescriptorEx("cycleType", beanClass);
        pde.setPropertyEditorClass(CycleTypeSelector.class);
        pde.setSimple(true);
        add(pde, getResourceString("PN_CYCLE_TYPE"), getResourceString("PD_CYCLE_TYPE"));
        
        pde = new PropertyDescriptorEx("parallel", beanClass);
        pde.setHidden(SecurityManager.isExperimentalFeatureHiddenMethod());
        add(pde, getResourceString("PN_PARALLEL"), getResourceString("PD_PARALLEL"));
        
        pde = new PropertyDescriptorEx("expression", beanClass);
        pde.setPropertyEditorClass(ExpressionEditor.class);
        add(pde, getResourceString("PN_EXPRESSION"), getResourceString("PD_EXPRESSION"));
    }
}
