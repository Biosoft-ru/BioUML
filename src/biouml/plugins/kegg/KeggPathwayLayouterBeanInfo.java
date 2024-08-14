package biouml.plugins.kegg;

import com.developmentontheedge.beans.BeanInfoEx;

public class KeggPathwayLayouterBeanInfo extends BeanInfoEx
{
    public KeggPathwayLayouterBeanInfo()
    {
        super(KeggPathwayLayouter.class, ru.biosoft.graph.MessageBundle.class.getName() );
        
        beanDescriptor.setDisplayName( getResourceString("CN_LAYOUTER"));
        beanDescriptor.setShortDescription( getResourceString("CD_LAYOUTER") );
    }
    
    @Override
    public void initProperties() throws Exception
    {
        
    }
}
