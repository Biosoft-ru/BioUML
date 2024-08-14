package biouml.plugins.research.workflow.items;


import com.developmentontheedge.beans.BeanInfoEx;

public class VariableTypeBeanInfo extends BeanInfoEx
{
    public VariableTypeBeanInfo()
    {
        super( VariableType.class, MessageBundle.class.getName() );
        setHideChildren(true);
        setBeanEditor(VariableTypeEditor.class);
        beanDescriptor.setDisplayName( getResourceString("PN_TYPE") );
        beanDescriptor.setShortDescription( getResourceString("PD_TYPE") );
    }
}