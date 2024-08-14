package ru.biosoft.bsastats;

public class FilterByQualityBeanInfo extends TaskProcessorBeanInfo
{

    public FilterByQualityBeanInfo()
    {
        super( FilterByQuality.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add("minQuality");
        add("maxLowQualityPercentage");
    }
}
