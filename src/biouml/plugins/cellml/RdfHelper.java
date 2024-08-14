package biouml.plugins.cellml;

/**
 * This class should help to DomElementTransformer process RDF elements.
 *
 * @pending spike solution
 */
public class RdfHelper extends DomHelperSupport
{
    public static final String PARSE_TYPE = "rdf:parseType";
    private static RdfHelper instance = new RdfHelper();

    private RdfHelper()
    {
    }

    public static RdfHelper getInstance()
    {
        return instance;
    }

    /**
     * Marks "rdf:parseType" as an expert property.
     */
    @Override
    protected boolean isExpert(String name)
    {
        if( name.equals(PARSE_TYPE) )
            return true;

        return super.isExpert(name);
    }
}


