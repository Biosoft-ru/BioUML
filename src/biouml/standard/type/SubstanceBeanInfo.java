package biouml.standard.type;

public class SubstanceBeanInfo extends MoleculeBeanInfo
{
    public SubstanceBeanInfo()
    {
        super( Substance.class );
    }
    
    public SubstanceBeanInfo(Class<? extends Substance> beanClass)
    {
        super( Substance.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        int index = findPropertyIndex( "synonyms" );
        property( "casRegistryNumber" ).htmlDisplayName( "CAS" ).add( index );
        property( "formula" ).htmlDisplayName( "FM" ).add( index + 1 );
    }
}
