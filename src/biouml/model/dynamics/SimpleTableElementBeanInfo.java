package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SimpleTableElementBeanInfo extends BeanInfoEx2<SimpleTableElement>
{
    public SimpleTableElementBeanInfo()
    {
        super(SimpleTableElement.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "tablePath");
        add("argColumn");
        add("columns");
//        PropertyDescriptorEx pde = new PropertyDescriptorEx( "columns" );
//        this.beanClass.
//        pde.setValue( "item-prototype", value );
//        property("columns").value( "item-prototype", this. );
    } 
}