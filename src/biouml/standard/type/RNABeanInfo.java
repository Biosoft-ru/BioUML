package biouml.standard.type;

public class RNABeanInfo extends BiopolymerBeanInfo
{
    public RNABeanInfo()
    {
        super( RNA.class );
    }

    protected RNABeanInfo(Class<? extends RNA> beanClass)
    {
        super( beanClass );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        int index = findPropertyIndex( "comment" );
        property( "gene" ).htmlDisplayName( "GN" ).add( index );
        property( "rnaType" ).tags( RNA.rnaTypes ).htmlDisplayName( "TP" ).add( index + 1 );
    }
}
