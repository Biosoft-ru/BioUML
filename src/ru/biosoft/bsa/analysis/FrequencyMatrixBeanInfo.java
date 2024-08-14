package ru.biosoft.bsa.analysis;

import ru.biosoft.bsa.BSAMessageBundle;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class FrequencyMatrixBeanInfo extends BeanInfoEx
{
    public FrequencyMatrixBeanInfo()
    {
        super(FrequencyMatrix.class, BSAMessageBundle.class.getName());
        beanDescriptor.setDisplayName     (getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }
    
    protected FrequencyMatrixBeanInfo(Class<? extends FrequencyMatrix> beanClass, String resourceBundleName)
    {
        super(beanClass, resourceBundleName);
    }
    
    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("name",
            beanClass,"getName",null );
        HtmlPropertyInspector.setDisplayName(pde, "ID");
        add(pde,
            getResourceString("PN_NAME"),
            getResourceString("PD_NAME"));
        pde = new PropertyDescriptorEx("title", beanClass, "getName", null);
        addHidden(pde);    // For pretty search results

        add(new PropertyDescriptorEx("length", beanClass.getMethod( "getLength", new Class<?>[]{} ), null),
            getResourceString("PN_LENGTH"),
            getResourceString("PD_LENGTH"));

        pde = new PropertyDescriptorEx("bindingElement", beanClass);
        HtmlPropertyInspector.setDisplayName(pde, "BE");
        pde.setHidden(true);
        add(pde,
            getResourceString("PN_BINDINGELEMENT"),
            getResourceString("PD_BINDINGELEMENT"));
        
        pde = new PropertyDescriptorEx("bindingElementName", beanClass.getMethod( "getBindingElementName", new Class<?>[]{} ), null);
        add(pde, getResourceString("PN_BINDINGELEMENT"), getResourceString("PD_BINDINGELEMENT"));
        
        pde = new PropertyDescriptorEx("view", beanClass.getMethod( "getView", new Class<?>[]{} ), null );
        pde.setPropertyEditorClass(null);
        add(pde, "Matrix logo", "Matrix logo");
        pde = new PropertyDescriptorEx("accession", beanClass, "getAccession", null);
        pde.setDisplayName(getResourceString("PN_ACCESSION"));
        addHidden(pde);
    }
}
