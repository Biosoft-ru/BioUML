package biouml.plugins.ensembl.type;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class EnsemblSequenceBeanInfo extends BeanInfoEx
{
    public EnsemblSequenceBeanInfo()
    {
        super( EnsemblSequence.class, MessageBundle.class.getName() );

        beanDescriptor.setDisplayName     (getResourceString("CN_SEQUENCE"));
        beanDescriptor.setShortDescription(getResourceString("CD_SEQUENCE"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("length", beanClass.getMethod("getLength"), null),
            getResourceString("PN_SEQUENCE_LENGTH"),
            getResourceString("PD_SEQUENCE_LENGTH") );
    }
}
