package ru.biosoft.bsa.analysis;

import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @author lan
 *
 */
public class MatchSiteModelBeanInfo extends WeightMatrixModelBeanInfo
{
    public MatchSiteModelBeanInfo()
    {
        super(MatchSiteModel.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        
        add(findPropertyIndex("view"), new PropertyDescriptorEx("coreCutoff", beanClass, "getCoreCutoff", null));
        add(findPropertyIndex("view"), new PropertyDescriptorEx("coreStart", beanClass, "getCoreStart", null));
        add(findPropertyIndex("view"), new PropertyDescriptorEx("coreLength", beanClass, "getCoreLength", null));
    }
}
