package ru.biosoft.bsa.view.colorscheme;

import com.developmentontheedge.beans.BeanInfoEx;

public class ConstantColorSchemeBeanInfo extends BeanInfoEx
{
    public ConstantColorSchemeBeanInfo()
    {
        super( ConstantColorScheme.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("color");
    }
}
