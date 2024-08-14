package biouml.plugins.gtrd;


import com.developmentontheedge.beans.BeanInfoEx;

public class ExperimentBeanInfo extends BeanInfoEx
{
    public ExperimentBeanInfo()
    {
        super(Experiment.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName("Abstract experiment");
        beanDescriptor.setShortDescription("Abstract experiment");
    }

}
