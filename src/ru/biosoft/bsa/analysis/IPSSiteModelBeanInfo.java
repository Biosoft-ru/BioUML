package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class IPSSiteModelBeanInfo extends BeanInfoEx
{
    public IPSSiteModelBeanInfo()
    {
        super(IPSSiteModel.class, IPSSiteModelMessageBundle.class.getName() );
        beanDescriptor.setDisplayName     (getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("name", beanClass , "getName", null),
                getResourceString("PN_NAME"),
                getResourceString("PD_NAME"));

        add(new PropertyDescriptorEx("threshold", beanClass ),
                getResourceString("PN_CRITICAL_IPS"),
                getResourceString("PD_CRITICAL_IPS"));

        add(new PropertyDescriptorEx("distMin", beanClass ),
                getResourceString("PN_DISTMIN"),
                getResourceString("PD_DISTMIN"));

        add(new PropertyDescriptorEx("window", beanClass ),
                getResourceString("PN_WINDOW"),
                getResourceString("PD_WINDOW"));

        PropertyDescriptorEx pde = new PropertyDescriptorEx("matrices", beanClass, "getMatrixPaths", null );
        add(pde,
                getResourceString("PN_MATRICES"),
                getResourceString("PD_MATRICES"));

    }

    public static class MatrixListEditor extends GenericMultiSelectEditor
    {
        public static final DataElementPath matrixLibPath = DataElementPath.create("databases/Utils/IPS/matrices");

        @Override
        protected Object[] getAvailableValues()
        {
            try
            {
                return matrixLibPath.getDataCollection( FrequencyMatrix.class ).stream().toArray( FrequencyMatrix[]::new );
            }
            catch( Exception e )
            {
                ExceptionRegistry.log(e);
                return new Object[0];
            }
        }
    }
}
