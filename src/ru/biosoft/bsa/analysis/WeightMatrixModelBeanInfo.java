package ru.biosoft.bsa.analysis;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 *
 */
public class WeightMatrixModelBeanInfo extends BeanInfoEx2<WeightMatrixModel>
{
    public WeightMatrixModelBeanInfo()
    {
        this(WeightMatrixModel.class);
    }

    public WeightMatrixModelBeanInfo(Class<? extends WeightMatrixModel> clazz)
    {
        super(clazz);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("name", beanClass , "getName", null));
        add(new PropertyDescriptorEx("matrixPath", beanClass , "getMatrixPath", null));
        
        add(new PropertyDescriptorEx("threshold", beanClass, "getThreshold", null));

        PropertyDescriptorEx pde = new PropertyDescriptorEx("view", beanClass, "getView", null );
        pde.setPropertyEditorClass(null);
        add(pde);
    }
}
