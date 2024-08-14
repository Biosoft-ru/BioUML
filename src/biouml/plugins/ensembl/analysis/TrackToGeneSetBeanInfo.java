package biouml.plugins.ensembl.analysis;

import com.developmentontheedge.beans.BeanInfoEx;

public class TrackToGeneSetBeanInfo extends BeanInfoEx
{
    public TrackToGeneSetBeanInfo()
    {
        super( TrackToGeneSet.class, MessageBundle.class.getName() );
        beanDescriptor.setDisplayName( getResourceString("CN_CLASS") );
        beanDescriptor.setShortDescription( getResourceString("CD_CLASS") );
    }
}
