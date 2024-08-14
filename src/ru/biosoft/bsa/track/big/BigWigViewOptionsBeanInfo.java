package ru.biosoft.bsa.track.big;

import ru.biosoft.bsa.view.SiteViewOptionsBeanInfo;

public class BigWigViewOptionsBeanInfo extends SiteViewOptionsBeanInfo
{
    public BigWigViewOptionsBeanInfo()
    {
        super(BigWigViewOptions.class);
    }
    
    @Override
    public void initProperties() throws Exception
    {
        add("maxProfileHeight");
        add("autoScale");
        addHidden( "scale", "isAutoScale" );
        add("showValuesRange");
        initColorSchemeProperties();
    }
}
