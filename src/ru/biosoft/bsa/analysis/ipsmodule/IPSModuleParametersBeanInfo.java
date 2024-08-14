package ru.biosoft.bsa.analysis.ipsmodule;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.analysis.ipsmodule.IPSModuleParameters.SiteModelsSelector;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class IPSModuleParametersBeanInfo extends BeanInfoEx
{
    public IPSModuleParametersBeanInfo()
    {
        super(IPSModuleParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(MessageBundle.CN_IPS_MODULE);
        beanDescriptor.setShortDescription(MessageBundle.CD_IPS_MODULE);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("siteTrackPath", beanClass, SqlTrack.class), MessageBundle.PN_SITE_TRACK, MessageBundle.PD_SITE_TRACK);
        add(new PropertyDescriptorEx("siteModels", beanClass), SiteModelsSelector.class, MessageBundle.PN_SITE_MODELS, MessageBundle.PD_SITE_MODELS);
        add(new PropertyDescriptorEx("windowSize", beanClass), MessageBundle.PN_WINDOW_SIZE, MessageBundle.PD_WINDOW_SIZE);
        add(new PropertyDescriptorEx("minSites", beanClass), MessageBundle.PN_MIN_SITES, MessageBundle.PD_MIN_SITES);
        add(new PropertyDescriptorEx("minAverageScore", beanClass), MessageBundle.PN_MIN_AVERAGE_SCORE, MessageBundle.PD_MIN_AVERAGE_SCORE);
        add(DataElementPathEditor.registerOutput("moduleTrack", beanClass, SqlTrack.class), MessageBundle.PN_MODULE_TRACK, MessageBundle.PD_MODULE_TRACK);
    }
    
}
