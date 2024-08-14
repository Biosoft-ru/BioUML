package ru.biosoft.bsastats;

public class TrimLowQualityBeanInfo extends TaskProcessorBeanInfo
{

    public TrimLowQualityBeanInfo()
    {
        super( TrimLowQuality.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add( "phredQualityThreashold" );
        add( "from3PrimeEnd" );
        add( "from5PrimeEnd" );
    }
}
