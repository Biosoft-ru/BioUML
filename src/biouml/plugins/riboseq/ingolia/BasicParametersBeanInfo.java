package biouml.plugins.riboseq.ingolia;

public class BasicParametersBeanInfo extends CoreParametersWithASiteTableBeanInfo
{
    protected BasicParametersBeanInfo(Class<? extends BasicParameters> beanClass)
    {
        super( beanClass );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add( "minWindowFootprints" );
        add( "minASiteFootprints" );
        add( "windowOverhangs" );
    }
}
