package ru.biosoft.bsa.importer;

import ru.biosoft.bsa.GenomeSelectorBeanInfo;

public class TrackImportPropertiesBeanInfo extends GenomeSelectorBeanInfo
{
    public TrackImportPropertiesBeanInfo()
    {
        super(TrackImportProperties.class);
    }
    
    protected TrackImportPropertiesBeanInfo(Class<? extends TrackImportProperties> beanClass)
    {
        super(beanClass);
    }
}