package biouml.standard.type;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class UnitBeanInfo extends BeanInfoEx2<Unit>
{
    public UnitBeanInfo()
    {
        super(Unit.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        property("name").htmlDisplayName("ID").add();
        property("title").htmlDisplayName("TI").add();
        property("comment").htmlDisplayName("CC").add();
        addReadOnly( "formula");
        PropertyDescriptorEx pde = new PropertyDescriptorEx("baseUnits", beanClass);
        pde.setChildDisplayName(beanClass.getMethod("calcBaseUnitName", new Class[] {Integer.class, Object.class}));
        add(pde);
    }
}
