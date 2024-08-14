package biouml.standard.type;

import com.developmentontheedge.beans.BeanInfoEx;

import biouml.standard.type.Unit.BaseUnitTypeEditor;

public class BaseUnitBeanInfo extends BeanInfoEx
{
    public BaseUnitBeanInfo()
    {
        super(BaseUnit.class, true);
        setCompositeEditor("type;multiplier;scale;exponent", new java.awt.GridLayout(1, 4));
        setHideChildren(true);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("type", BaseUnitTypeEditor.class);
        add("multiplier");
        add("scale");
        add("exponent");
    }
}