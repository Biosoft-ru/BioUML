package biouml.plugins.gtrd;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ExperimentalFactorBeanInfo extends BeanInfoEx 
{
	public ExperimentalFactorBeanInfo()
	{
		 super( ExperimentalFactor.class, true );
	}
	protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name", beanClass, "getName", null);
        add(pde, "Factor table ID", "Factor table ID");
        
        pde = new PropertyDescriptorEx("factorId", beanClass, "getFactorId", null);
        add(pde, "Factor ID", "Factor ID");
        
        pde = new PropertyDescriptorEx("title", beanClass, "getTitle", null);
        add(pde, "Title", "Title");

        pde = new PropertyDescriptorEx("parent", beanClass, "getParent", null);
        add(pde, "parent ID", "parent ID");
        
        pde = new PropertyDescriptorEx("exRefs", beanClass, "getExRefs", null);
        add(pde, "Cell Ontology ID", "Cell Ontology ID");
        
    }
	
}
