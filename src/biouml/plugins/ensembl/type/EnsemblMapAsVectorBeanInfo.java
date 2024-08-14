package biouml.plugins.ensembl.type;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;

/**
 * @author lan
 *
 */
public class EnsemblMapAsVectorBeanInfo extends BeanInfoEx
{
    public EnsemblMapAsVectorBeanInfo()
    {
        super(EnsemblMapAsVector.class, MessageBundle.class.getName());
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptor("name", beanClass, "getCompletePath", null), getResourceString("PN_NAME"), getResourceString("PD_NAME"));
        add(new PropertyDescriptor("length", beanClass, "getLength", null), getResourceString("PN_SEQUENCE_LENGTH"), getResourceString("PD_SEQUENCE_LENGTH"));
        add(new PropertyDescriptor("karyotype", beanClass, "getKaryotype", null), getResourceString("PN_KARYOTYPE"), getResourceString("PD_KARYOTYPE"));
    }
}
