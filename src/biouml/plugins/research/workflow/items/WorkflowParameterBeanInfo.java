package biouml.plugins.research.workflow.items;

import one.util.streamex.StreamEx;

import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import ru.biosoft.workbench.editors.ReferenceTypeSelector;
import biouml.plugins.research.workflow.items.DataElementType.DataElementTypeSelector;
import biouml.plugins.research.workflow.items.WorkflowParameter.WorkflowParameterRoleSelector;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class WorkflowParameterBeanInfo extends WorkflowVariableBeanInfo
{
    public WorkflowParameterBeanInfo()
    {
        this(WorkflowParameter.class, "PARAMETER");
    }

    public WorkflowParameterBeanInfo(Class type, String name)
    {
        super(type, name);
        beanDescriptor.setDisplayName( getResourceString("CN_PARAMETER") );
        beanDescriptor.setShortDescription( getResourceString("CD_PARAMETER") );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add(1, new PropertyDescriptorEx("description", beanClass), getResourceString("PN_DESCRIPTION"), getResourceString("PD_DESCRIPTION"));
        add(new PropertyDescriptorEx("defaultValueString", beanClass), ExpressionEditor.class, getResourceString("PN_DEFAULT_VALUE"), getResourceString("PD_DEFAULT_VALUE"));
        PropertyDescriptorEx pde = new PropertyDescriptorEx("role", beanClass);
        pde.setHidden(beanClass.getMethod("isDataElementTypeHidden"));
        add(pde, WorkflowParameterRoleSelector.class, getResourceString("PN_ROLE"), getResourceString("PD_ROLE"));
        pde = new PropertyDescriptorEx("dataElementType", beanClass);
        pde.setHidden(beanClass.getMethod("isDataElementTypeHidden"));
        pde.setSimple(true);
        add(pde, DataElementTypeSelector.class, getResourceString("PN_DATA_ELEMENT_TYPE"), getResourceString("PD_DATA_ELEMENT_TYPE"));
        pde = new PropertyDescriptorEx("referenceType", beanClass);
        pde.setHidden(beanClass.getMethod("isReferenceTypeHidden"));
        add(pde, ReferenceTypeSelector.class, getResourceString("PN_REFERENCE_TYPE"), getResourceString("PD_REFERENCE_TYPE"));
        pde = new PropertyDescriptorEx("dropDownOptions", beanClass);
        pde.setHidden(beanClass.getMethod("isDropDownOptionsHidden"));
        pde.setPropertyEditorClass(DropDownOptionsSelector.class);
        add(pde, getResourceString("PN_DROP_DOWN_OPTIONS"), getResourceString("PD_DROP_DOWN_OPTIONS"));
        pde = new PropertyDescriptorEx("dropDownOptionsExpression", beanClass);
        pde.setHidden(beanClass.getMethod("isDropDownOptionsExpressionHidden"));
        add(pde, ExpressionEditor.class, getResourceString("PN_DROP_DOWN_OPTIONS_EXPRESSION"), getResourceString("PD_DROP_DOWN_OPTIONS_EXPRESSION"));
        add(new PropertyDescriptorEx("rank", beanClass), getResourceString("PN_RANK"), getResourceString("PD_RANK"));
    }
    
    public static class DropDownOptionsSelector extends GenericComboBoxEditor
    {
        private static String[] tags;
        
        static
        {
            tags = StreamEx.of( WorkflowCycleVariable.CYCLE_TYPES ).map( CycleType::getName ).prepend( "(none)", "(auto)" )
                    .toArray( String[]::new );
        }
        
        @Override
        protected Object[] getAvailableValues()
        {
            return tags.clone();
        }
    }
}
