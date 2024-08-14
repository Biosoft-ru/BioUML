package biouml.model.util;

import ru.biosoft.workbench.editors.GenericComboBoxEditor;


import com.developmentontheedge.beans.awt.infos.ColorMessageBundle;
import com.developmentontheedge.beans.BeanInfoEx;

public class VariableNameBeanInfo extends BeanInfoEx
{
    public VariableNameBeanInfo()
    {
        super( VariableName.class, ColorMessageBundle.class.getName());
        beanDescriptor.setDisplayName("DISPLAY_NAME");
        beanDescriptor.setShortDescription( "SHORT_DESCRIPTION" );
        setSimple( true );

        setBeanEditor( VariableNameEditor.class );
    }

    public static class VariableNameEditor extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ((VariableName)super.getValue()).getAvailableNames();
        }
        
        @Override
        public void setValue(Object value)
        {
            if (value instanceof VariableName)
                super.setValue( value );
            else if (value instanceof String)
            ((VariableName)super.getValue()).setValue( (String)value );
        }
        
        @Override
        public Object getValue()
        {
            return (super.getValue());
        }
    }
}