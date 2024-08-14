package ru.biosoft.access.core;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.MessageBundle;
//import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.repository.DataElementPathEditor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class DataElementPathBeanInfo extends BeanInfoEx
{
    public DataElementPathBeanInfo()
    {
        super(DataElementPath.class, MessageBundle.class.getName());
        setBeanEditor( DataElementPathEditor.class );
        setHideChildren(true);
        setNoRecursionCheck(true);
    }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde;
        pde = new PropertyDescriptorEx("element", beanClass, "getDataElement", null);
        add(pde);
        
        pde = new PropertyDescriptorEx("parent", beanClass, "getParentPath", null);
        addHidden(pde);
        
        pde = new PropertyDescriptorEx("name", beanClass, "getName", null);
        add(pde);

        pde = new PropertyDescriptorEx("empty", beanClass, "isEmpty", null);
        add(pde);
        
        //        pde = new PropertyDescriptorEx("defaultSpecies", beanClass, "getDefaultSpecies", null);
        //        addHidden(pde);
    }
}
