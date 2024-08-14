package biouml.plugins.pharm;

import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.PropertyDescriptorEx;


public class PopulationVariableBeanInfo extends BeanInfoEx2<PopulationVariable>
{
    public PopulationVariableBeanInfo()
    {
        super(PopulationVariable.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name", beanClass);
        pde.setReadOnly( beanClass.getMethod( "isCreated") );
        add(pde);
        
        pde = new PropertyDescriptorEx("initialValue", beanClass);
        pde.setNumberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE);
        add(pde);
        
        property( "type" ).tags( Type.TYPE_STOCHASTIC, Type.TYPE_FUNCTION, Type.TYPE_CONSTANT ).add();

        property("distribution").hidden( "isNotRandom" ).readOnly( "isNotRandom" ).tags( PopulationVariable.DISTRIBUTION_NORMAL ).add();
        
        property("transformation").tags(PopulationVariable.TRANSFORMATION_LOG, PopulationVariable.TRANSFORMATION_NONE).add();
        
        add("comment");
    }
}