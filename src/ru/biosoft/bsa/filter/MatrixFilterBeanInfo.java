/*$ Id: SiteCutOffFilterBeanInfo.java,v 1.1 2001/05/24 05:18:20 cher Exp $ */

package ru.biosoft.bsa.filter;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.IndexedPropertyDescriptorEx;

public class MatrixFilterBeanInfo extends BeanInfoEx
{
    public MatrixFilterBeanInfo()
    {
        super(MatrixFilter.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName     (getResourceString("PN_MATRIX_FILTER"));
        beanDescriptor.setShortDescription(getResourceString("PD_MATRIX_FILTER"));

        setSubstituteByChild(true);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new IndexedPropertyDescriptorEx("filter", beanClass),
            getResourceString("PN_MATRIX_FILTER"),
            getResourceString("PD_MATRIX_FILTER"));
    }
}

