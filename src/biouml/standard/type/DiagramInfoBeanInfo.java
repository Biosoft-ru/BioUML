package biouml.standard.type;


public class DiagramInfoBeanInfo extends ReferrerBeanInfo<DiagramInfo>
{
    public DiagramInfoBeanInfo()
    {
        this(DiagramInfo.class, "DIAGRAM_INFO");
    }

    protected DiagramInfoBeanInfo(Class beanClass, String key)
    {
        super(beanClass, key);
    }
    
    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add( "authors" );
        add( "created" );
        add( "modified" );
    }
}
